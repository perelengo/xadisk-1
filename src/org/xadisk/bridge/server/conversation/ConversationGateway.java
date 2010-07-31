package org.xadisk.bridge.server.conversation;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.resource.spi.work.Work;
import org.xadisk.filesystem.NativeXAFileSystem;

public class ConversationGateway implements Work {

    private volatile boolean enabled = true;
    private final Selector selector;
    private ConcurrentLinkedQueue<SocketChannel> channelsToRegister = new ConcurrentLinkedQueue<SocketChannel>();

    public ConversationGateway() throws IOException {
        this.selector = Selector.open();
    }

    public void delegateConversation(SocketChannel clientChannel) throws IOException, InterruptedException {
        clientChannel.configureBlocking(false);
        channelsToRegister.add(clientChannel);
        selector.wakeup();
    }

    public void run() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1000);
            while (enabled) {
                int n = selector.select();
                while (true) {
                    SocketChannel newConversationChannel = channelsToRegister.poll();
                    if (newConversationChannel == null) {
                        break;
                    }
                    ConversationContext context = new ConversationContext(newConversationChannel);
                    newConversationChannel.register(selector, SelectionKey.OP_READ, context);
                }
                if (n == 0) {
                    continue;
                }
                Set<SelectionKey> selectedReadable = selector.selectedKeys();
                for (SelectionKey selectionKey : selectedReadable) {
                    ConversationContext context = (ConversationContext) selectionKey.attachment();
                    SocketChannel channel = context.getConversationChannel();

                    buffer.clear();
                    try {
                        int numRead = channel.read(buffer);
                        if (numRead == -1) {
                            throw new EOFException();
                        }
                        buffer.flip();
                        context.updateWithConversation(buffer);
                    } catch (IOException ioe) {
                        selectionKey.cancel();
                    }
                }
                selectedReadable.clear();
            }
            informDisconnectionToClients();
        } catch (Throwable t) {
            NativeXAFileSystem.getXAFileSystem().notifySystemFailure(t);
        }
    }

    public void release() {
        this.enabled = false;
        this.selector.wakeup();
    }

    private void informDisconnectionToClients() {
        Set<SelectionKey> connectedClientKeys = selector.keys();
        for (SelectionKey key : connectedClientKeys) {
            try {
                ((SocketChannel) key.channel()).socket().close();
            } catch (Throwable t) {
                //no-op.
            }
        }
    }
}

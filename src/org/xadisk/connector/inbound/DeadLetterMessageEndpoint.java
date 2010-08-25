package org.xadisk.connector.inbound;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import org.xadisk.filesystem.FileStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.TransactionLogEntry;
import org.xadisk.filesystem.XidImpl;

public class DeadLetterMessageEndpoint {

    private final File deadLetterDir;
    private FileChannel deadLetterChannel;
    private int currentLetterIndex;

    public DeadLetterMessageEndpoint(File deadLetterDir) throws FileNotFoundException {
        this.deadLetterDir = deadLetterDir;
        this.currentLetterIndex = deadLetterDir.listFiles().length;
        this.deadLetterChannel = new FileOutputStream(new File(deadLetterDir, "letter_" + currentLetterIndex)).getChannel();
    }

    public void dumpAndCommitMessage(FileStateChangeEvent event, XAResource xar) throws IOException,
            XAException {
        synchronized (this) {
            ensureDeadLetterCapacity();
            byte[] content = event.toString().getBytes(TransactionLogEntry.UTF8Charset);
            deadLetterChannel.write(ByteBuffer.wrap(content));
        }
        xar.commit(XidImpl.getXidInstanceForLocalTransaction(NativeXAFileSystem.getXAFileSystem().getNextLocalTransactionId()), true);
    }

    private void ensureDeadLetterCapacity() throws IOException {
        if (deadLetterChannel.size() > 10000) {
            File nextLetter = null;
            for (int i = currentLetterIndex + 1; i < Integer.MAX_VALUE; i++) {
                nextLetter = new File(deadLetterDir, "letter_" + i);
                if (nextLetter.exists()) {
                    continue;
                }
                deadLetterChannel.close();
                deadLetterChannel =
                        new FileOutputStream(nextLetter).getChannel();
                currentLetterIndex = i;
                break;
            }
            if (nextLetter == null) {
                throw new IOException("No more dead letters can be created...cannot proceed.");
            }
        }
    }

    public void release() {
        try {
            deadLetterChannel.close();
        } catch (IOException ioe) {
        }
    }
}

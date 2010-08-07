package org.xadisk.filesystem;

import org.xadisk.filesystem.utilities.FileIOUtility;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TransactionLogEntry {

    public static final byte FILE_APPEND = 2;
    public static final byte FILE_MOVE = 3;
    public static final byte FILE_COPY = 4;
    public static final byte FILE_DELETE = 5;
    public static final byte FILE_CREATE = 6;
    public static final byte DIR_CREATE = 7;
    public static final byte FILE_TRUNCATE = 8;
    public static final byte FILE_SPECIAL_MOVE = 9;
    public static final byte EVENT_ENQUEUE = 10;
    public static final byte FILES_ALREADY_ONDISK = 11;
    public static final byte COMMIT_BEGINS = 12;
    public static final byte TXN_COMMIT_DONE = 13;
    public static final byte TXN_ROLLBACK_DONE = 14;
    public static final byte PREPARE_COMPLETES = 15;
    public static final byte UNDOABLE_FILE_APPEND = 16;
    public static final byte UNDOABLE_FILE_TRUNCATE = 17;
    public static final byte TXN_USES_UNDO_LOGS = 18;
    public static final byte EVENT_DEQUEUE = 19;
    public static final byte PREPARE_COMPLETES_FOR_EVENT_DEQUEUE = 20;
    public static final byte CHECKPOINT_AVOIDING_COPY_OR_MOVE_REDO = 21;
    private static final String UTF8Charset = "UTF8";
    private XidImpl xid;
    private byte operationType;
    private String fileName;
    private long filePosition;
    private int fileContentLength;
    private int headerLength;
    private String destFileName;
    private long newLength;
    private HashSet<File> fileList;
    private ArrayList<FileStateChangeEvent> eventList;
    private int checkPointPosition = -1;

    static byte[] getUTF8Bytes(String str) {
        try {
            return str.getBytes(UTF8Charset);
        } catch (UnsupportedEncodingException uee) {
            return null;
            //won't arise.
        }
    }

    static String getUTF8AssumedString(byte[] bytes) {
        try {
            return new String(bytes, UTF8Charset);
        } catch (UnsupportedEncodingException uee) {
            return null;
            //won't arise.
        }
    }

    public static byte[] getLogEntry(XidImpl xid, String file, long filePosition, int fileContentLength,
            byte appendOrUndoTruncate) {
        byte filePathBytes[] = getUTF8Bytes(file);
        int filePathLength = filePathBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(200 + filePathLength);

        buffer.putInt(0);
        buffer.putInt((int) fileContentLength);
        buffer.put(serializeXid(xid));
        buffer.put(appendOrUndoTruncate);

        buffer.putInt(filePathLength);
        buffer.put(filePathBytes);

        buffer.putLong(filePosition);
        buffer.putInt(0, buffer.position());

        buffer.flip();
        byte temp[] = new byte[buffer.limit()];
        buffer.get(temp);
        return temp;
    }

    static byte[] getLogEntry(XidImpl xid, String file, byte createFileOrDirOrDeleteOrUndoCreate) {
        byte filePathBytes[] = getUTF8Bytes(file);
        int filePathLength = filePathBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(200 + filePathLength);

        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put(serializeXid(xid));
        buffer.put(createFileOrDirOrDeleteOrUndoCreate);

        buffer.putInt(filePathLength);
        buffer.put(filePathBytes);

        buffer.putInt(0, buffer.position());

        buffer.flip();
        byte temp[] = new byte[buffer.limit()];
        buffer.get(temp);
        return temp;
    }

    public static byte[] getLogEntry(XidImpl xid, String file, long newLength, byte truncateOrUndoAppend) {
        byte filePathBytes[] = getUTF8Bytes(file);
        int filePathLength = filePathBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(200 + filePathLength);

        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put(serializeXid(xid));
        buffer.put(truncateOrUndoAppend);

        buffer.putInt(filePathLength);
        buffer.put(filePathBytes);

        buffer.putLong(newLength);

        buffer.putInt(0, buffer.position());

        buffer.flip();
        byte temp[] = new byte[buffer.limit()];
        buffer.get(temp);
        return temp;
    }

    public static byte[] getLogEntry(XidImpl xid, String sourceFile, String destinationFile, byte moveOrCopyOrUndoDelete) {
        byte sourceFilePathBytes[] = getUTF8Bytes(sourceFile);
        byte destFilePathBytes[] = getUTF8Bytes(destinationFile);
        int srcFilePathLength = sourceFilePathBytes.length;
        int destFilePathLength = destFilePathBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(200 + srcFilePathLength + destFilePathLength);

        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put(serializeXid(xid));
        buffer.put(moveOrCopyOrUndoDelete);
        buffer.putInt(srcFilePathLength);
        buffer.put(sourceFilePathBytes);

        buffer.putInt(destFilePathLength);
        buffer.put(destFilePathBytes);

        buffer.putInt(0, buffer.position());

        buffer.flip();
        byte temp[] = new byte[buffer.limit()];
        buffer.get(temp);
        return temp;
    }

    public static byte[] getLogEntry(XidImpl xid, byte commitStatus) {
        ByteBuffer buffer = ByteBuffer.allocate(200);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put(serializeXid(xid));
        buffer.put(commitStatus);

        buffer.putInt(0, buffer.position());

        buffer.flip();
        byte temp[] = new byte[buffer.limit()];
        buffer.get(temp);
        return temp;
    }

    static byte[] getLogEntry(XidImpl xid, int checkPointPosition) {
        ByteBuffer buffer = ByteBuffer.allocate(200);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put(serializeXid(xid));
        buffer.put(CHECKPOINT_AVOIDING_COPY_OR_MOVE_REDO);
        buffer.putInt(checkPointPosition);

        buffer.putInt(0, buffer.position());

        buffer.flip();
        byte temp[] = new byte[buffer.limit()];
        buffer.get(temp);
        return temp;
    }

    public static byte[] getLogEntry(XidImpl xid, Set<File> files) {
        byte filePathsBytes[][] = new byte[files.size()][];
        int totalFilePathsLength = 0;
        int i = 0;
        Iterator<File> iter = files.iterator();
        while (iter.hasNext()) {
            filePathsBytes[i] = getUTF8Bytes(iter.next().getAbsolutePath());
            totalFilePathsLength += filePathsBytes[i].length;
            i++;
        }
        //a bug was identified by Janos, for a large number of files. We need the "4* .. " segment below.
        ByteBuffer buffer = ByteBuffer.allocate(200 + totalFilePathsLength + 4 * filePathsBytes.length);

        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put(serializeXid(xid));
        buffer.put(FILES_ALREADY_ONDISK);
        buffer.putInt(files.size());
        iter = files.iterator();
        for (i = 0; i < filePathsBytes.length; i++) {
            buffer.putInt(filePathsBytes[i].length);
            buffer.put(filePathsBytes[i]);
        }
        buffer.putInt(0, buffer.position());

        buffer.flip();
        byte temp[] = new byte[buffer.limit()];
        buffer.get(temp);
        return temp;
    }

    public static byte[] getLogEntry(XidImpl xid, ArrayList<FileStateChangeEvent> events, byte enQ_deQ_prepareDequeue) {
        int totalEventsLength = 0;
        byte[][] eventsBytes = new byte[events.size()][];
        for (int i = 0; i < events.size(); i++) {
            eventsBytes[i] = getBytesFromEvent(events.get(i));
            totalEventsLength += eventsBytes[i].length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(200 + totalEventsLength);

        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put(serializeXid(xid));
        buffer.put(enQ_deQ_prepareDequeue);
        buffer.putInt(events.size());
        for (int i = 0; i < eventsBytes.length; i++) {
            buffer.put(eventsBytes[i]);
        }
        buffer.putInt(0, buffer.position());

        buffer.flip();
        byte temp[] = new byte[buffer.limit()];
        buffer.get(temp);
        return temp;
    }

    static TransactionLogEntry parseLogEntry(ByteBuffer buffer) {
        TransactionLogEntry temp = new TransactionLogEntry();
        int position = buffer.position();
        buffer.position(0);
        temp.headerLength = buffer.getInt();
        buffer.getInt();
        temp.xid = deSerializeXid(buffer);
        temp.operationType = buffer.get();

        if (temp.operationType == FILE_APPEND || temp.operationType == UNDOABLE_FILE_TRUNCATE) {
            temp.fileName = readFileName(buffer);
            temp.filePosition = buffer.getLong();
            temp.fileContentLength = buffer.getInt(4);
        } else if (temp.operationType == FILE_DELETE || temp.operationType == FILE_CREATE
                || temp.operationType == DIR_CREATE) {
            temp.fileName = readFileName(buffer);
        } else if (temp.operationType == FILE_COPY || temp.operationType == FILE_MOVE
                || temp.operationType == FILE_SPECIAL_MOVE) {
            temp.fileName = readFileName(buffer);
            temp.destFileName = readFileName(buffer);
        } else if (temp.operationType == FILE_TRUNCATE || temp.operationType == UNDOABLE_FILE_APPEND) {
            temp.fileName = readFileName(buffer);
            temp.newLength = buffer.getLong();
        } else if (temp.operationType == FILES_ALREADY_ONDISK) {
            int numFiles = buffer.getInt();
            temp.fileList = new HashSet<File>(numFiles);
            for (int i = 0; i < numFiles; i++) {
                temp.fileList.add(new File(readFileName(buffer)));
            }
        } else if (temp.operationType == EVENT_ENQUEUE || temp.operationType == EVENT_DEQUEUE
                || temp.operationType == PREPARE_COMPLETES_FOR_EVENT_DEQUEUE) {
            int numEvents = buffer.getInt();
            temp.eventList = new ArrayList<FileStateChangeEvent>(numEvents);
            for (int i = 0; i < numEvents; i++) {
                temp.eventList.add(readEvent(buffer, temp.xid));
            }
        } else if (temp.operationType == CHECKPOINT_AVOIDING_COPY_OR_MOVE_REDO) {
            temp.checkPointPosition = buffer.getInt();
        }

        buffer.position(position);
        return temp;
    }

    private static String readFileName(ByteBuffer buffer) {
        int fileNameLength = buffer.getInt();
        byte fileName[] = new byte[fileNameLength];
        buffer.get(fileName);
        return getUTF8AssumedString(fileName);
    }

    private static byte[] getBytesFromEvent(FileStateChangeEvent event) {
        byte fileNameBytes[] = getUTF8Bytes(event.getFile().getAbsolutePath());
        ByteBuffer buffer = ByteBuffer.allocate(fileNameBytes.length + 10);
        buffer.put(event.getEventType());
        buffer.putInt(fileNameBytes.length);
        buffer.put(fileNameBytes);
        buffer.put((byte) (event.isDirectory() ? 1 : 0));
        buffer.flip();
        byte temp[] = new byte[buffer.limit()];
        buffer.get(temp);
        return temp;
    }

    private static FileStateChangeEvent readEvent(ByteBuffer buffer, XidImpl enqueuingTransaction) {
        byte eventType = buffer.get();
        int fileNameLength = buffer.getInt();
        byte fileNameBytes[] = new byte[fileNameLength];
        buffer.get(fileNameBytes);
        String fileName = getUTF8AssumedString(fileNameBytes);
        File file = new File(fileName);
        boolean isDirectory = buffer.get() == 1 ? true : false;
        return new FileStateChangeEvent(file, isDirectory, eventType, enqueuingTransaction);
    }

    public byte getOperationType() {
        return operationType;
    }

    int getFileContentLength() {
        return fileContentLength;
    }

    String getFileName() {
        return fileName;
    }

    long getFilePosition() {
        return filePosition;
    }

    public XidImpl getXid() {
        return xid;
    }

    int getHeaderLength() {
        return headerLength;
    }

    String getDestFileName() {
        return destFileName;
    }

    long getNewLength() {
        return newLength;
    }

    public int getCheckPointPosition() {
        return checkPointPosition;
    }

    static byte[] serializeXid(XidImpl xid) {
        byte[] gid = xid.getGlobalTransactionId();
        byte[] bqual = xid.getBranchQualifier();
        ByteBuffer temp = ByteBuffer.allocate(1 + 1 + 4 + gid.length + bqual.length);
        temp.put((byte) gid.length);
        temp.put((byte) bqual.length);
        temp.putInt(xid.getFormatId());
        temp.put(gid);
        temp.put(bqual);
        byte bytes[] = new byte[temp.capacity()];
        temp.flip();
        temp.get(bytes);
        return bytes;
    }

    static XidImpl deSerializeXid(ByteBuffer buffer) {
        return new XidImpl(buffer);
    }

    public static void updateContentLength(ByteBuffer buffer, int contentLength) {
        buffer.putInt(4, contentLength);
    }

    public static TransactionLogEntry getNextTransactionLogEntry(FileChannel logChannel, long position,
            boolean onlyCompletionEntry) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(500);
        logChannel.position(position);

        FileIOUtility.readFromChannel(logChannel, header, 0, 4);

        int logEntryHeaderLength = header.getInt(0);
        if (logEntryHeaderLength > header.capacity()) {
            header = ByteBuffer.allocate(logEntryHeaderLength);
        }
        logChannel.position(position);
        header.clear();
        FileIOUtility.readFromChannel(logChannel, header, 0, logEntryHeaderLength);
        header.flip();

        if (onlyCompletionEntry) {
            TransactionLogEntry logEntry = new TransactionLogEntry();
            logEntry.headerLength = header.getInt();
            logEntry.fileContentLength = header.getInt();
            logEntry.xid = deSerializeXid(header);
            logEntry.operationType = header.get();

            logChannel.position(position + logEntry.headerLength + logEntry.fileContentLength);

            if (logEntry.operationType == COMMIT_BEGINS || logEntry.operationType == PREPARE_COMPLETES
                    || logEntry.operationType == TXN_COMMIT_DONE || logEntry.operationType == TXN_ROLLBACK_DONE
                    || logEntry.operationType == PREPARE_COMPLETES_FOR_EVENT_DEQUEUE) {
                return logEntry;
            }
            return null;
        } else {
            TransactionLogEntry logEntry = parseLogEntry(header);
            logChannel.position(position + logEntry.headerLength + logEntry.fileContentLength);
            return logEntry;
        }
    }

    public HashSet<File> getFileList() {
        return fileList;
    }

    public ArrayList<FileStateChangeEvent> getEventList() {
        return eventList;
    }

    public boolean isUndoLogEntry() {
        return (operationType == TransactionLogEntry.UNDOABLE_FILE_APPEND
                || operationType == TransactionLogEntry.UNDOABLE_FILE_TRUNCATE);
    }

    public boolean isRedoLogEntry() {
        return operationType < 12;
    }
}

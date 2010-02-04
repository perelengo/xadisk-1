package org.xadisk.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Logger {

    private final PrintStream logFile;
    private final byte logLevel;
    public static final byte ERROR = 1;
    public static final byte WARNING = 2;
    public static final byte INFORMATIONAL = 3;
    public static final byte DEBUG = 4;

    public Logger(File logFileName, byte logLevel) throws IOException {
        this.logFile = new PrintStream(new FileOutputStream(logFileName), true);
        this.logLevel = logLevel;
    }

    public void logError(String msg) {
        if (logLevel >= ERROR) {
            writeMessageToLogFile(msg);
        }
    }

    public void logError(Throwable t) {
        if (logLevel >= ERROR) {
            writeMessageToLogFile(t.getMessage());
            t.printStackTrace(logFile);
        }
    }

    public void logWarning(String msg) {
        if (logLevel >= WARNING) {
            writeMessageToLogFile(msg);
        }
    }

    public void logInfo(String msg) {
        if (logLevel >= INFORMATIONAL) {
            writeMessageToLogFile(msg);
        }
    }

    public void logDebug(String msg) {
        if (logLevel >= DEBUG) {
            writeMessageToLogFile(msg);
        }
    }

    private void writeMessageToLogFile(String msg) {
        logFile.println(msg);
    }

    public void close() {
        logFile.close();
    }

    void releaseLogFile() throws IOException {
        logFile.close();
    }
}

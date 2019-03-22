package org.dataconservancy.pass.loader.journal.nih;

public class JournalFinderException extends Exception {

    public static final String JFE_INSUFFICIENT_INFORMATION_ERROR_EXCEPTION = "Not Enough Information supplied to" +
            "locate or create a journal";

    JournalFinderException(String message) {
        super(message);
    }

    JournalFinderException(String message, Throwable cause) {
        super(message, cause);
    }
}

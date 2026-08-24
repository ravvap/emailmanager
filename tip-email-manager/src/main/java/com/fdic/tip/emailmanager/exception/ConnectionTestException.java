package com.fdic.tip.emailmanager.exception;

/**
 * Wraps any low-level failure from the Test Connection action. Callers must catch this
 * and surface only Messages.TEST_CONNECTION_FAILURE / TEST_CONNECTION_TIMEOUT to the
 * client - never the wrapped cause's message - per the acceptance criteria that failures
 * "show a plain, safe message without exposing technical internals."
 */
public class ConnectionTestException extends RuntimeException {
    public ConnectionTestException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionTestException(String message) {
        super(message);
    }
}

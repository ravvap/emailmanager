package com.fdic.tip.emailmanager.exception;

/**
 * Thrown when a delete is attempted on a connection that is either still Active
 * or has usage history from a template - both block permanent delete per EM-1.
 */
public class ConnectionInUseException extends RuntimeException {
    public ConnectionInUseException(String message) {
        super(message);
    }
}

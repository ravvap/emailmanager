package com.fdic.tip.emailmanager.exception;

/** Thrown when a connection name collides with an existing one (case-insensitive). */
public class DuplicateNameException extends RuntimeException {
    public DuplicateNameException(String message) {
        super(message);
    }
}

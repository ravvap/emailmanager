package com.fdic.tip.emailmanager.exception;

/** Thrown when a requested data connection (or related resource) does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

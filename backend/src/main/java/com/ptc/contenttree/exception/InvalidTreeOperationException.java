package com.ptc.contenttree.exception;

/** The requested change would break the tree structure (mapped to 400). */
public class InvalidTreeOperationException extends RuntimeException {

    public InvalidTreeOperationException(String message) {
        super(message);
    }
}

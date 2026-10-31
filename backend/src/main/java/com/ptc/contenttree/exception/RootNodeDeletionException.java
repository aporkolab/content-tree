package com.ptc.contenttree.exception;

/** The root node cannot be deleted (mapped to 409). */
public class RootNodeDeletionException extends RuntimeException {

    public RootNodeDeletionException() {
        super("Cannot delete root node");
    }
}

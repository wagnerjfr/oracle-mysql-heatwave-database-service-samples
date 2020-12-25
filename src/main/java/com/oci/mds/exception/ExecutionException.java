package com.oci.mds.exception;

public class ExecutionException extends RuntimeException {

    private final boolean timeout;

    public ExecutionException(String message) {
        super(message);
        this.timeout = false;
    }

    public ExecutionException(String message, boolean timeout) {
        super(message);
        this.timeout = timeout;
    }

    public boolean isTimeout() {
        return this.timeout;
    }
}

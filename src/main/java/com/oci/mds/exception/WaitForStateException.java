package com.oci.mds.exception;

public class WaitForStateException extends RuntimeException {

    private final boolean timeout;

    public WaitForStateException(String message) {
        super(message);
        this.timeout = false;
    }

    public WaitForStateException(String message, boolean timeout) {
        super(message);
        this.timeout = timeout;
    }

    public boolean isTimeout() {
        return this.timeout;
    }
}

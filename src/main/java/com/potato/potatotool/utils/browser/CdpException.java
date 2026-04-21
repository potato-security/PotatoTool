package com.potato.potatotool.utils.browser;

/**
 * Checked exception for all CDP transport and protocol failures.
 */
public class CdpException extends Exception {
    private final CdpErrorType errorType;

    public CdpException(CdpErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public CdpException(CdpErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public CdpErrorType getErrorType() {
        return errorType;
    }

    public boolean isTimeout() {
        return errorType == CdpErrorType.REQUEST_TIMEOUT;
    }

    public static CdpException timeout(String message) {
        return new CdpException(CdpErrorType.REQUEST_TIMEOUT, message);
    }
}

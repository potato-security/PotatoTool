package com.potato.potatotool.utils.browser;

/**
 * Structured CDP error categories used by browser automation features.
 */
public enum CdpErrorType {
    PORT_UNAVAILABLE,
    NO_PAGE_TARGET,
    WEBSOCKET_HANDSHAKE_FAILED,
    CONNECTION_CLOSED,
    REQUEST_TIMEOUT,
    INVALID_RESPONSE,
    PROTOCOL_ERROR,
    INTERRUPTED,
    BROWSER_CLOSED
}

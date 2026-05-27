package com.potato.potatotool.content.redTeam.portScanner.event;

public class PortScanErrorEvent extends PortScanEvent {
    private final String message;
    private final Throwable throwable;

    public PortScanErrorEvent(String scanId, String message, Throwable throwable) {
        super(scanId);
        this.message = message;
        this.throwable = throwable;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}

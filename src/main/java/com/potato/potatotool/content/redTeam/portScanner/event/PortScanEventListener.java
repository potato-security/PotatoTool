package com.potato.potatotool.content.redTeam.portScanner.event;

public interface PortScanEventListener {
    void onScanStarted(PortScanStartedEvent event);

    void onScanProgress(PortScanProgressEvent event);

    void onPortFound(PortFoundEvent event);

    void onScanCompleted(PortScanCompletedEvent event);

    void onScanError(PortScanErrorEvent event);
}

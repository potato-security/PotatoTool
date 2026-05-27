package com.potato.potatotool.content.redTeam.portScanner.event;

import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;

public class PortScanCompletedEvent extends PortScanEvent {
    private final PortScanResult result;

    public PortScanCompletedEvent(String scanId, PortScanResult result) {
        super(scanId);
        this.result = result;
    }

    public PortScanResult getResult() {
        return result;
    }
}

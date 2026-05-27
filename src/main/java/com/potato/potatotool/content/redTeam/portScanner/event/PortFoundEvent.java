package com.potato.potatotool.content.redTeam.portScanner.event;

import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;

public class PortFoundEvent extends PortScanEvent {
    private final PortResult result;

    public PortFoundEvent(String scanId, PortResult result) {
        super(scanId);
        this.result = result;
    }

    public PortResult getResult() {
        return result;
    }
}

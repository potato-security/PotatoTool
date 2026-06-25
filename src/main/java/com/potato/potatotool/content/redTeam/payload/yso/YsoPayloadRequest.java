package com.potato.potatotool.content.redTeam.payload.yso;

public class YsoPayloadRequest {
    private final String gadget;
    private final String command;

    public YsoPayloadRequest(String gadget, String command) {
        this.gadget = gadget;
        this.command = command;
    }

    public String getGadget() {
        return gadget;
    }

    public String getCommand() {
        return command;
    }
}

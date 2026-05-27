package com.potato.potatotool.content.redTeam.portScanner.model;

public class ScanTask {
    private final String host;
    private final int port;

    public ScanTask(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getKey() {
        return host + ":" + port;
    }
}

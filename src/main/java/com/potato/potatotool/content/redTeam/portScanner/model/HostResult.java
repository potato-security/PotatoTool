package com.potato.potatotool.content.redTeam.portScanner.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HostResult {
    private String host;
    private final List<PortResult> ports = new ArrayList<>();

    public HostResult(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public synchronized void addOrUpdate(PortResult result) {
        for (int i = 0; i < ports.size(); i++) {
            PortResult current = ports.get(i);
            if (current.getPort() == result.getPort()) {
                ports.set(i, result);
                return;
            }
        }
        ports.add(result);
    }

    public synchronized List<PortResult> getPorts() {
        return Collections.unmodifiableList(new ArrayList<>(ports));
    }

    public synchronized int getOpenCount() {
        int count = 0;
        for (PortResult port : ports) {
            if (port.getState() == PortState.OPEN) {
                count++;
            }
        }
        return count;
    }
}

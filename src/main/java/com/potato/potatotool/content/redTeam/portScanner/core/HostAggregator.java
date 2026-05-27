package com.potato.potatotool.content.redTeam.portScanner.core;

import com.potato.potatotool.content.redTeam.portScanner.model.HostResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HostAggregator {
    private final Map<String, HostResult> hostMap = new LinkedHashMap<>();

    public synchronized void addOrUpdate(PortResult result) {
        HostResult hostResult = hostMap.get(result.getHost());
        if (hostResult == null) {
            hostResult = new HostResult(result.getHost());
            hostMap.put(result.getHost(), hostResult);
        }
        hostResult.addOrUpdate(result);
    }

    public synchronized List<HostResult> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(hostMap.values()));
    }

    public synchronized void clear() {
        hostMap.clear();
    }
}

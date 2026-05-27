package com.potato.potatotool.content.redTeam.portScanner.targets;

import com.potato.potatotool.content.redTeam.portScanner.model.ScanTask;

import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public final class PortPlanner {
    private PortPlanner() {
    }

    public static Iterator<ScanTask> plan(final Iterator<String> hosts, final int[] ports) {
        return new Iterator<ScanTask>() {
            private String currentHost;
            private int portIndex;

            @Override
            public boolean hasNext() {
                if (ports == null || ports.length == 0) {
                    return false;
                }
                if (currentHost != null && portIndex < ports.length) {
                    return true;
                }
                while (hosts.hasNext()) {
                    currentHost = hosts.next();
                    portIndex = 0;
                    if (currentHost != null && !currentHost.trim().isEmpty()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public ScanTask next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                ScanTask task = new ScanTask(currentHost, ports[portIndex]);
                portIndex++;
                if (portIndex >= ports.length) {
                    currentHost = null;
                }
                return task;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static int[] parsePorts(String text, int[] fallback) {
        Set<Integer> ports = new LinkedHashSet<>();
        if (text == null || text.trim().isEmpty()) {
            return fallback == null ? new int[0] : fallback;
        }
        String[] parts = text.split(",");
        for (String part : parts) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (value.contains("-")) {
                String[] range = value.split("-", 2);
                int start = parsePort(range[0]);
                int end = parsePort(range[1]);
                if (end < start) {
                    throw new IllegalArgumentException("Invalid port range: " + value);
                }
                for (int port = start; port <= end; port++) {
                    ports.add(port);
                }
            } else {
                ports.add(parsePort(value));
            }
        }
        int[] result = new int[ports.size()];
        int i = 0;
        for (Integer port : ports) {
            result[i++] = port;
        }
        return result;
    }

    private static int parsePort(String value) {
        int port = Integer.parseInt(value.trim());
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        return port;
    }
}

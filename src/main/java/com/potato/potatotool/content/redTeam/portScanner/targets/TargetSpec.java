package com.potato.potatotool.content.redTeam.portScanner.targets;

public class TargetSpec {
    private final String raw;
    private final long estimatedHosts;

    public TargetSpec(String raw, long estimatedHosts) {
        this.raw = raw;
        this.estimatedHosts = estimatedHosts;
    }

    public String getRaw() {
        return raw;
    }

    public long getEstimatedHosts() {
        return estimatedHosts;
    }
}

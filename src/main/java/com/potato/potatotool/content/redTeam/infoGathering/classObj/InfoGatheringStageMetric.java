package com.potato.potatotool.content.redTeam.infoGathering.classObj;

/**
 * @author Potato
 * @date 2026/4/7 14:03
 */
public class InfoGatheringStageMetric {
    private final String stageName;
    private final long durationMs;
    private final Integer itemCount;
    private final String summary;

    public InfoGatheringStageMetric(String stageName, long durationMs, Integer itemCount, String summary) {
        this.stageName = stageName;
        this.durationMs = durationMs;
        this.itemCount = itemCount;
        this.summary = summary;
    }

    public String getStageName() {
        return stageName;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public String getSummary() {
        return summary;
    }
}

package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.potato.potatotool.content.redTeam.infoGathering.classObj.InfoGatheringStageMetric;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Potato
 * @date 2026/4/7 14:04
 */
public class InfoGatheringStageRecorder {
    private final List<InfoGatheringStageMetric> metrics = new ArrayList<>();

    public StageToken start(String stageName) {
        return new StageToken(stageName, System.nanoTime());
    }

    public void finish(StageToken token, Integer itemCount, String summary) {
        if (token == null) {
            return;
        }
        long durationMs = Math.max(0L, (System.nanoTime() - token.startedAtNanos) / 1_000_000L);
        metrics.add(new InfoGatheringStageMetric(token.stageName, durationMs, itemCount, summary));
    }

    public void addMetric(String stageName, long durationMs, Integer itemCount, String summary) {
        metrics.add(new InfoGatheringStageMetric(stageName, Math.max(0L, durationMs), itemCount, summary));
    }

    public List<InfoGatheringStageMetric> snapshot() {
        return new ArrayList<>(metrics);
    }

    public boolean isEmpty() {
        return metrics.isEmpty();
    }

    public static class StageToken {
        private final String stageName;
        private final long startedAtNanos;

        private StageToken(String stageName, long startedAtNanos) {
            this.stageName = stageName;
            this.startedAtNanos = startedAtNanos;
        }
    }
}

package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventDispatcher;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanPhase;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanProgressEvent;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 统一扫描进度跟踪器。
 * 以阶段权重的方式聚合总体进度，避免指纹识别和漏洞扫描的任务数语义不一致。
 */
public class ScanProgressTracker {

    private static final Map<ScanPhase, Double> PHASE_WEIGHTS;
    private static final Map<ScanPhase, Double> PHASE_STARTS;

    static {
        EnumMap<ScanPhase, Double> weights = new EnumMap<ScanPhase, Double>(ScanPhase.class);
        weights.put(ScanPhase.PREPARING, 0.02d);
        weights.put(ScanPhase.FINGERPRINTING, 0.18d);
        weights.put(ScanPhase.SELECTING_POCS, 0.05d);
        weights.put(ScanPhase.SCANNING, 0.70d);
        weights.put(ScanPhase.FINALIZING, 0.05d);
        PHASE_WEIGHTS = Collections.unmodifiableMap(weights);

        EnumMap<ScanPhase, Double> starts = new EnumMap<ScanPhase, Double>(ScanPhase.class);
        double cursor = 0.0d;
        for (Map.Entry<ScanPhase, Double> entry : weights.entrySet()) {
            starts.put(entry.getKey(), cursor);
            cursor += entry.getValue();
        }
        PHASE_STARTS = Collections.unmodifiableMap(starts);
    }

    private final Object eventSource;
    private final ScanEventDispatcher dispatcher;

    private String scanId;
    private ScanPhase phase;
    private int phaseCompleted;
    private int phaseTotal;
    private int vulnerabilitiesFound;
    private double lastOverallProgress;
    private boolean closed;

    public ScanProgressTracker(Object eventSource, ScanEventDispatcher dispatcher) {
        this.eventSource = eventSource;
        this.dispatcher = dispatcher;
        this.phase = ScanPhase.PREPARING;
    }

    public synchronized void reset(String scanId) {
        this.scanId = scanId;
        this.phase = ScanPhase.PREPARING;
        this.phaseCompleted = 0;
        this.phaseTotal = 0;
        this.vulnerabilitiesFound = 0;
        this.lastOverallProgress = 0.0d;
        this.closed = false;
    }

    public synchronized void startPhase(ScanPhase phase, int total, String message) {
        if (closed || scanId == null || phase == null) {
            return;
        }
        this.phase = phase;
        this.phaseCompleted = 0;
        this.phaseTotal = Math.max(0, total);
        dispatchProgress(message, false);
    }

    public synchronized void advance(int delta, String message) {
        if (closed) {
            return;
        }
        int next = phaseCompleted + Math.max(0, delta);
        setPhaseProgress(next, phaseTotal, message);
    }

    public synchronized void setPhaseProgress(int completed, int total, String message) {
        if (closed) {
            return;
        }
        this.phaseCompleted = Math.max(0, completed);
        this.phaseTotal = Math.max(0, total);
        if (phaseTotal > 0 && phaseCompleted > phaseTotal) {
            this.phaseCompleted = phaseTotal;
        }
        dispatchProgress(message, false);
    }

    public synchronized void setVulnerabilitiesFound(int vulnerabilitiesFound) {
        this.vulnerabilitiesFound = Math.max(0, vulnerabilitiesFound);
    }

    public synchronized void finishSuccess() {
        if (closed || scanId == null) {
            return;
        }
        this.phase = ScanPhase.COMPLETED;
        this.phaseCompleted = this.phaseTotal > 0 ? this.phaseTotal : this.phaseCompleted;
        this.lastOverallProgress = 1.0d;
        dispatchProgress("扫描完成", true);
        closed = true;
    }

    public synchronized void finishStopped(String message) {
        if (closed || scanId == null) {
            return;
        }
        this.phase = ScanPhase.STOPPED;
        dispatchProgress(message, false);
        closed = true;
    }

    public synchronized void finishFailed(String message) {
        if (closed || scanId == null) {
            return;
        }
        this.phase = ScanPhase.FAILED;
        dispatchProgress(message, false);
        closed = true;
    }

    public synchronized void closeWithoutFinalEvent() {
        closed = true;
    }

    private void dispatchProgress(String message, boolean forceComplete) {
        if (dispatcher == null || scanId == null || closed && !forceComplete) {
            return;
        }
        double overall = forceComplete ? 1.0d : calculateOverallProgress();
        if (!forceComplete && overall < lastOverallProgress) {
            overall = lastOverallProgress;
        }
        overall = clamp(overall);
        lastOverallProgress = overall;

        dispatcher.dispatchScanProgress(new ScanProgressEvent(
                eventSource,
                scanId,
                phaseCompleted,
                phaseTotal,
                vulnerabilitiesFound,
                phase,
                phaseCompleted,
                phaseTotal,
                overall,
                message
        ));
    }

    private double calculateOverallProgress() {
        if (phase == ScanPhase.COMPLETED) {
            return 1.0d;
        }
        Double phaseStart = PHASE_STARTS.get(phase);
        Double phaseWeight = PHASE_WEIGHTS.get(phase);
        if (phaseStart == null || phaseWeight == null) {
            return lastOverallProgress;
        }
        if (phaseTotal <= 0) {
            return phaseStart;
        }
        return phaseStart + phaseWeight * safeRatio(phaseCompleted, phaseTotal);
    }

    private double safeRatio(int completed, int total) {
        if (total <= 0) {
            return 0.0d;
        }
        return clamp(completed / (double) total);
    }

    private double clamp(double value) {
        if (value < 0.0d) {
            return 0.0d;
        }
        if (value > 1.0d) {
            return 1.0d;
        }
        return value;
    }
}

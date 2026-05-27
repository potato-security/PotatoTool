package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventListener;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventDispatcher;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanPhase;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanProgressEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ScanProgressTracker 测试")
class ScanProgressTrackerTest {

    @Test
    @DisplayName("阶段进度应单调递增且仅成功完成时为100%")
    void shouldAdvanceMonotonicallyAndReachHundredOnlyOnSuccess() {
        ScanEventDispatcher dispatcher = new ScanEventDispatcher(false);
        List<ScanProgressEvent> events = new ArrayList<ScanProgressEvent>();
        dispatcher.addEventListener(new ScanEventListener() {
            @Override
            public void onScanProgress(ScanProgressEvent event) {
                events.add(event);
            }
        });

        ScanProgressTracker tracker = new ScanProgressTracker(this, dispatcher);
        tracker.reset("scan-progress-test");
        tracker.startPhase(ScanPhase.PREPARING, 1, "准备中");
        tracker.setPhaseProgress(1, 1, "准备完成");
        tracker.startPhase(ScanPhase.FINGERPRINTING, 2, "指纹中");
        tracker.advance(1, "指纹中");
        tracker.advance(1, "指纹完成");
        tracker.startPhase(ScanPhase.SCANNING, 3, "扫描中");
        tracker.advance(1, "扫描中");
        tracker.advance(1, "扫描中");
        tracker.finishSuccess();

        double last = -1.0d;
        for (ScanProgressEvent event : events) {
            assertTrue(event.getOverallProgress() >= last, "进度不应倒退");
            last = event.getOverallProgress();
        }
        assertTrue(!events.isEmpty());
        assertEquals(ScanPhase.COMPLETED, events.get(events.size() - 1).getPhase());
        assertEquals(1.0d, events.get(events.size() - 1).getOverallProgress(), 0.0001d);
    }

    @Test
    @DisplayName("停止和失败不应强制100%")
    void shouldNotForceHundredForStoppedOrFailed() {
        ScanEventDispatcher dispatcher = new ScanEventDispatcher(false);
        List<ScanProgressEvent> events = new ArrayList<ScanProgressEvent>();
        dispatcher.addEventListener(new ScanEventListener() {
            @Override
            public void onScanProgress(ScanProgressEvent event) {
                events.add(event);
            }
        });

        ScanProgressTracker tracker = new ScanProgressTracker(this, dispatcher);
        tracker.reset("scan-stopped-test");
        tracker.startPhase(ScanPhase.SCANNING, 10, "扫描中");
        tracker.advance(2, "扫描中");
        tracker.finishStopped("已停止");

        ScanProgressEvent stopped = events.get(events.size() - 1);
        assertEquals(ScanPhase.STOPPED, stopped.getPhase());
        assertTrue(stopped.getOverallProgress() < 1.0d);
    }
}

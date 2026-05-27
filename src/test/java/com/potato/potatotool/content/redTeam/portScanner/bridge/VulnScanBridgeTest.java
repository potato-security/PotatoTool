package com.potato.potatotool.content.redTeam.portScanner.bridge;

import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanCompletedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VulnScanBridgeTest {
    @Test
    public void handoff_shouldImportImmediatelyWhenVulnScanIsIdle() {
        FakeGateway gateway = new FakeGateway();
        gateway.scanning = false;
        VulnScanBridge bridge = new VulnScanBridge(gateway, Runnable::run);

        HandoffResult result = bridge.handoff(Arrays.asList("http://127.0.0.1:80"), HandoffPolicy.CANCEL);

        assertEquals(HandoffResult.STARTED, result);
        assertEquals(Arrays.asList("http://127.0.0.1:80"), gateway.imported.get(0));
        assertEquals(1, gateway.selectCount);
        assertFalse(gateway.stopCalled);
    }

    @Test
    public void handoff_shouldRejectBusyScanWhenPolicyIsCancel() {
        FakeGateway gateway = new FakeGateway();
        gateway.scanning = true;
        VulnScanBridge bridge = new VulnScanBridge(gateway, Runnable::run);

        HandoffResult result = bridge.handoff(Arrays.asList("http://127.0.0.1:80"), HandoffPolicy.CANCEL);

        assertEquals(HandoffResult.REJECTED_BUSY, result);
        assertTrue(gateway.imported.isEmpty());
        assertFalse(gateway.stopCalled);
        assertEquals(0, gateway.listenerCount);
    }

    @Test
    public void handoff_shouldQueueTargetsAndImportAfterCompletionWhenPolicyIsAppendAfter() {
        FakeGateway gateway = new FakeGateway();
        gateway.scanning = true;
        VulnScanBridge bridge = new VulnScanBridge(gateway, Runnable::run);

        HandoffResult result = bridge.handoff(Arrays.asList("https://example.com:443"), HandoffPolicy.APPEND_AFTER);

        assertEquals(HandoffResult.QUEUED, result);
        assertTrue(gateway.imported.isEmpty());
        assertEquals(1, gateway.listenerCount);

        gateway.completeScan();

        assertEquals(1, gateway.imported.size());
        assertEquals(Arrays.asList("https://example.com:443"), gateway.imported.get(0));
        assertEquals(0, gateway.selectCount);
    }

    @Test
    public void handoff_shouldStopCurrentScanAndImportWhenPolicyIsForceReplace() {
        FakeGateway gateway = new FakeGateway();
        gateway.scanning = true;
        VulnScanBridge bridge = new VulnScanBridge(gateway, Runnable::run);

        HandoffResult result = bridge.handoff(Arrays.asList("http://example.com:8080"), HandoffPolicy.FORCE_REPLACE);

        assertEquals(HandoffResult.STARTED, result);
        assertTrue(gateway.stopCalled);
        assertEquals(Arrays.asList("http://example.com:8080"), gateway.imported.get(0));
        assertEquals(1, gateway.selectCount);
    }

    @Test
    public void handoff_shouldFailWhenGatewayIsNotReady() {
        FakeGateway gateway = new FakeGateway();
        gateway.ready = false;
        VulnScanBridge bridge = new VulnScanBridge(gateway, Runnable::run);

        HandoffResult result = bridge.handoff(Arrays.asList("http://127.0.0.1:80"), HandoffPolicy.CANCEL);

        assertEquals(HandoffResult.FAILED, result);
        assertTrue(gateway.imported.isEmpty());
    }

    private static class FakeGateway implements VulnScanBridge.Gateway {
        private boolean ready = true;
        private boolean scanning;
        private boolean stopCalled;
        private int selectCount;
        private int listenerCount;
        private ScanEventListener listener;
        private final List<List<String>> imported = new ArrayList<>();

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public boolean isScanning() {
            return scanning;
        }

        @Override
        public void stopScan() {
            stopCalled = true;
            scanning = false;
        }

        @Override
        public void addEventListener(ScanEventListener listener) {
            this.listener = listener;
            listenerCount++;
        }

        @Override
        public void importTargets(List<String> targets) {
            imported.add(new ArrayList<>(targets));
        }

        @Override
        public void selectVulnScanPane() {
            selectCount++;
        }

        private void completeScan() {
            scanning = false;
            listener.onScanCompleted(new ScanCompletedEvent(this, "test-scan", Collections.emptyList(), 0L, true));
        }
    }
}

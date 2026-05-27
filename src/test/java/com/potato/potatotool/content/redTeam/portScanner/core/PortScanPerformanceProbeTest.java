package com.potato.potatotool.content.redTeam.portScanner.core;

import com.potato.potatotool.content.redTeam.portScanner.event.PortScanCompletedEvent;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanEventAdapter;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanConfig;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;
import com.potato.potatotool.content.redTeam.portScanner.port.PortPresets;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PortScanPerformanceProbeTest {
    @Test
    public void localhostFullPortScan_shouldFinishWithinThirtySeconds() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("portscan.performance"));
        PortScanService service = PortScanService.getInstance();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<PortScanResult> finishedResult = new AtomicReference<>();
        PortScanEventAdapter listener = new PortScanEventAdapter() {
            @Override
            public void onScanCompleted(PortScanCompletedEvent event) {
                finishedResult.set(event.getResult());
                completed.countDown();
            }
        };
        service.addEventListener(listener);
        PortScanConfig config = new PortScanConfig();
        config.setPorts(PortPresets.all());
        config.setConnectTimeoutMs(1500);
        config.setProbeTimeoutMs(3000);
        config.setBatchSize(1024);
        config.setServiceProbe(true);
        config.setSaveClosed(false);
        config.setPublicMode(false);

        long start = System.currentTimeMillis();
        try {
            service.startScan(Collections.singletonList("127.0.0.1"), config);
            assertTrue(completed.await(120, TimeUnit.SECONDS));
            long elapsed = System.currentTimeMillis() - start;
            PortScanResult result = finishedResult.get();
            assertNotNull(result);
            System.out.println("PORTSCAN_PERF elapsedMs=" + elapsed
                    + ", totalTasks=" + result.getTotalTasks()
                    + ", completedTasks=" + result.getCompletedTasks()
                    + ", openCount=" + result.getOpenResults().size()
                    + ", timeoutMs=" + config.getConnectTimeoutMs()
                    + ", batchSize=" + config.getBatchSize()
                    + ", serviceProbe=" + config.isServiceProbe());
            assertTrue(elapsed <= 30000L, "elapsedMs=" + elapsed);
        } finally {
            service.removeEventListener(listener);
            service.stopScan();
        }
    }
}

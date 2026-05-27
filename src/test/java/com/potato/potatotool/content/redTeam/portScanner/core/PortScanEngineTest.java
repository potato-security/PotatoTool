package com.potato.potatotool.content.redTeam.portScanner.core;

import com.potato.potatotool.content.redTeam.portScanner.event.PortScanEventAdapter;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanEventDispatcher;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanStartedEvent;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanConfig;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanState;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PortScanEngineTest {
    @Test
    public void pauseResumeStop_shouldUpdateScanState() throws Exception {
        PortScanEventDispatcher dispatcher = new PortScanEventDispatcher();
        PortScanEngine engine = new PortScanEngine(dispatcher);
        CountDownLatch started = new CountDownLatch(1);
        dispatcher.addListener(new PortScanEventAdapter() {
            @Override
            public void onScanStarted(PortScanStartedEvent event) {
                started.countDown();
            }
        });
        PortScanConfig config = new PortScanConfig();
        config.setPorts(new int[]{81, 82, 83});
        config.setBatchSize(4);
        config.setConnectTimeoutMs(1000);
        config.setServiceProbe(false);
        config.setPublicMode(true);
        config.setPublicRateLimit(1);

        engine.startScan(Collections.singletonList("2001:db8::1"), config);
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertEquals(PortScanState.RUNNING, engine.getState());

        engine.pauseScan();
        assertEquals(PortScanState.PAUSED, engine.getState());
        assertTrue(engine.isPaused());

        engine.resumeScan();
        assertEquals(PortScanState.RUNNING, engine.getState());
        assertTrue(engine.isScanning());

        engine.stopScan();
        assertEquals(PortScanState.STOPPED, engine.getState());
        dispatcher.shutdown();
    }
}

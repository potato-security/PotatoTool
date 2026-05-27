package com.potato.potatotool.content.redTeam.portScanner.core;

import com.potato.potatotool.content.redTeam.portScanner.event.PortScanEventDispatcher;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanEventListener;
import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanConfig;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanState;

import java.util.List;

public class PortScanService {
    private static PortScanService instance;
    private final PortScanEventDispatcher dispatcher;
    private final PortScanEngine engine;

    private PortScanService() {
        this.dispatcher = new PortScanEventDispatcher();
        this.engine = new PortScanEngine(dispatcher);
    }

    public static synchronized PortScanService getInstance() {
        if (instance == null) {
            instance = new PortScanService();
        }
        return instance;
    }

    public void startScan(List<String> targets, PortScanConfig config) {
        engine.startScan(targets, config);
    }

    public void pauseScan() {
        engine.pauseScan();
    }

    public void resumeScan() {
        engine.resumeScan();
    }

    public void stopScan() {
        engine.stopScan();
    }

    public boolean isScanning() {
        return engine.isScanning();
    }

    public boolean isPaused() {
        return engine.isPaused();
    }

    public PortScanState getState() {
        return engine.getState();
    }

    public PortScanResult getResult() {
        return engine.getResult();
    }

    public List<PortResult> getOpenResults() {
        return engine.getOpenResults();
    }

    public void addEventListener(PortScanEventListener listener) {
        dispatcher.addListener(listener);
    }

    public void removeEventListener(PortScanEventListener listener) {
        dispatcher.removeListener(listener);
    }

    public void shutdown() {
        engine.shutdown();
        dispatcher.shutdown();
    }
}

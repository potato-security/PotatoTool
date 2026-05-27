package com.potato.potatotool.content.redTeam.portScanner.core;

import com.potato.potatotool.content.redTeam.portScanner.event.PortFoundEvent;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanCompletedEvent;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanErrorEvent;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanEventDispatcher;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanProgressEvent;
import com.potato.potatotool.content.redTeam.portScanner.event.PortScanStartedEvent;
import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanConfig;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanState;
import com.potato.potatotool.content.redTeam.portScanner.model.PortState;
import com.potato.potatotool.content.redTeam.portScanner.model.ScanTask;
import com.potato.potatotool.content.redTeam.portScanner.nio.SelectorWorker;
import com.potato.potatotool.content.redTeam.portScanner.port.ServiceCatalog;
import com.potato.potatotool.content.redTeam.portScanner.targets.PortPlanner;
import com.potato.potatotool.content.redTeam.portScanner.targets.TargetExpander;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PortScanEngine implements SelectorWorker.ScanCallback, ServiceProbe.ProbeCallback {
    private final PortScanEventDispatcher dispatcher;
    private final HostAggregator aggregator = new HostAggregator();
    private final ExecutorService probePool = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r, "portscan-service-probe");
        thread.setDaemon(true);
        return thread;
    });
    private volatile SelectorWorker worker;
    private volatile PortScanState state = PortScanState.READY;
    private volatile PortScanConfig config;
    private volatile PortScanResult result;
    private volatile String scanId;
    private final AtomicLong completedTasks = new AtomicLong();
    private final AtomicInteger openCount = new AtomicInteger();
    private long totalTasks;

    public PortScanEngine(PortScanEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public synchronized void startScan(List<String> targets, PortScanConfig config) {
        if (state == PortScanState.RUNNING || state == PortScanState.PAUSED) {
            throw new IllegalStateException("Port scan is already running");
        }
        this.config = config;
        this.scanId = UUID.randomUUID().toString();
        long estimatedHosts = TargetExpander.estimateHosts(targets);
        int portCount = config.getPorts() == null ? 0 : config.getPorts().length;
        this.totalTasks = estimatedHosts > 0 && portCount > 0 && estimatedHosts > Long.MAX_VALUE / portCount
                ? Long.MAX_VALUE
                : estimatedHosts * portCount;
        this.completedTasks.set(0);
        this.openCount.set(0);
        this.aggregator.clear();
        this.result = new PortScanResult();
        this.result.setScanId(scanId);
        this.result.setStartTime(System.currentTimeMillis());
        this.result.setTotalTasks(totalTasks);
        Iterator<String> hosts = TargetExpander.expand(targets);
        Iterator<ScanTask> tasks = PortPlanner.plan(hosts, config.getPorts());
        this.worker = new SelectorWorker(tasks, config, this);
        this.state = PortScanState.RUNNING;
        dispatcher.dispatchStarted(new PortScanStartedEvent(scanId, totalTasks));
        Thread thread = new Thread(worker, "portscan-selector-worker");
        thread.setDaemon(true);
        thread.start();
    }

    public void pauseScan() {
        SelectorWorker current = worker;
        if (current != null && state == PortScanState.RUNNING) {
            state = PortScanState.PAUSED;
            current.pause();
        }
    }

    public void resumeScan() {
        SelectorWorker current = worker;
        if (current != null && state == PortScanState.PAUSED) {
            state = PortScanState.RUNNING;
            current.resume();
        }
    }

    public void stopScan() {
        SelectorWorker current = worker;
        if (current != null) {
            state = PortScanState.STOPPED;
            current.stop();
        }
    }

    public boolean isScanning() {
        return state == PortScanState.RUNNING || state == PortScanState.PAUSED;
    }

    public boolean isPaused() {
        return state == PortScanState.PAUSED;
    }

    public PortScanState getState() {
        return state;
    }

    public PortScanResult getResult() {
        return result;
    }

    public List<PortResult> getOpenResults() {
        if (result == null) {
            return java.util.Collections.emptyList();
        }
        return result.getOpenResults();
    }

    @Override
    public void onOpen(PortResult portResult) {
        portResult.setService(ServiceCatalog.infer(portResult.getPort(), (String) null, false));
        aggregator.addOrUpdate(portResult);
        result.addOrUpdate(portResult);
        openCount.incrementAndGet();
        dispatcher.dispatchPortFound(new PortFoundEvent(scanId, portResult));
        if (config.isServiceProbe()) {
            probePool.submit(new ServiceProbe(portResult, config, this));
        }
    }

    @Override
    public void onClosedOrFiltered(ScanTask task, PortState state) {
        if (config.isSaveClosed()) {
            PortResult portResult = new PortResult(task.getHost(), task.getPort(), state);
            result.addOrUpdate(portResult);
        }
    }

    @Override
    public void onTaskDone() {
        long done = completedTasks.incrementAndGet();
        result.setCompletedTasks(done);
        if (done % 20 == 0 || done == totalTasks) {
            dispatcher.dispatchProgress(new PortScanProgressEvent(scanId, done, totalTasks, openCount.get()));
        }
    }

    @Override
    public void onError(ScanTask task, Throwable throwable) {
        dispatcher.dispatchError(new PortScanErrorEvent(scanId, throwable == null ? "Unknown error" : throwable.getMessage(), throwable));
    }

    @Override
    public void onWorkerFinished() {
        if (state != PortScanState.STOPPED) {
            state = PortScanState.COMPLETED;
        }
        if (result != null) {
            result.setEndTime(System.currentTimeMillis());
        }
        dispatcher.dispatchProgress(new PortScanProgressEvent(scanId, completedTasks.get(), totalTasks, openCount.get()));
        dispatcher.dispatchCompleted(new PortScanCompletedEvent(scanId, result));
    }

    @Override
    public void onProbeDone(PortResult portResult) {
        aggregator.addOrUpdate(portResult);
        result.addOrUpdate(portResult);
        dispatcher.dispatchPortFound(new PortFoundEvent(scanId, portResult));
    }

    public void shutdown() {
        stopScan();
        probePool.shutdownNow();
    }
}

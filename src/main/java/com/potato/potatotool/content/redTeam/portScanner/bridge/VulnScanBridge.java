package com.potato.potatotool.content.redTeam.portScanner.bridge;

import com.potato.potatotool.MainApplication;
import com.potato.potatotool.ToStart;
import com.potato.potatotool.controller.MainController;
import com.potato.potatotool.controller.redTeam.PaneVulScan;
import com.potato.potatotool.content.redTeam.vulnScanner.core.VulnScanService;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanCompletedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventListener;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanErrorEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanInfoEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanProgressEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanStartedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.VulnerabilityFoundEvent;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class VulnScanBridge {
    private static VulnScanBridge instance;
    private final Deque<List<String>> pending = new ConcurrentLinkedDeque<>();
    private final Gateway gateway;
    private final UiExecutor uiExecutor;
    private volatile boolean listenerInstalled;

    private VulnScanBridge() {
        this(new DefaultGateway(), Platform::runLater);
    }

    VulnScanBridge(Gateway gateway, UiExecutor uiExecutor) {
        this.gateway = gateway;
        this.uiExecutor = uiExecutor;
    }

    public static synchronized VulnScanBridge getInstance() {
        if (instance == null) {
            instance = new VulnScanBridge();
        }
        return instance;
    }

    public HandoffResult handoff(List<String> targets, HandoffPolicy policy) {
        if (targets == null || targets.isEmpty()) {
            return HandoffResult.FAILED;
        }
        if (!gateway.isReady()) {
            return HandoffResult.FAILED;
        }
        if (!gateway.isScanning()) {
            importAndSwitch(targets);
            return HandoffResult.STARTED;
        }
        HandoffPolicy effectivePolicy = policy == null ? HandoffPolicy.CANCEL : policy;
        switch (effectivePolicy) {
            case APPEND_AFTER:
                pending.offer(new ArrayList<>(targets));
                installCompletionHook();
                return HandoffResult.QUEUED;
            case FORCE_REPLACE:
                gateway.stopScan();
                waitForStop(3000L);
                importAndSwitch(targets);
                return HandoffResult.STARTED;
            case CANCEL:
            default:
                return HandoffResult.REJECTED_BUSY;
        }
    }

    private void importAndSwitch(List<String> targets) {
        uiExecutor.execute(() -> {
            gateway.importTargets(targets);
            gateway.selectVulnScanPane();
        });
    }

    private void installCompletionHook() {
        if (listenerInstalled) {
            return;
        }
        gateway.addEventListener(new ScanEventListener() {
            @Override
            public void onScanStarted(ScanStartedEvent event) {
            }

            @Override
            public void onScanProgress(ScanProgressEvent event) {
            }

            @Override
            public void onVulnerabilityFound(VulnerabilityFoundEvent event) {
            }

            @Override
            public void onScanCompleted(ScanCompletedEvent event) {
                List<String> next = pending.poll();
                if (next != null) {
                    uiExecutor.execute(() -> gateway.importTargets(next));
                }
            }

            @Override
            public void onScanError(ScanErrorEvent event) {
            }

            @Override
            public void onScanInfo(ScanInfoEvent event) {
            }
        });
        listenerInstalled = true;
    }

    private void waitForStop(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (gateway.isScanning() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    interface Gateway {
        boolean isReady();

        boolean isScanning();

        void stopScan();

        void addEventListener(ScanEventListener listener);

        void importTargets(List<String> targets);

        void selectVulnScanPane();
    }

    interface UiExecutor {
        void execute(Runnable runnable);
    }

    private static class DefaultGateway implements Gateway {
        @Override
        public boolean isReady() {
            MainController main = MainApplication.getMainController();
            return main != null && main.getVulScanPane() != null;
        }

        @Override
        public boolean isScanning() {
            return VulnScanService.getInstance().isScanning();
        }

        @Override
        public void stopScan() {
            VulnScanService.getInstance().stopScan();
        }

        @Override
        public void addEventListener(ScanEventListener listener) {
            VulnScanService.getInstance().addEventListener(listener);
        }

        @Override
        public void importTargets(List<String> targets) {
            PaneVulScan pane = getPane();
            if (pane != null) {
                pane.importTargets(targets);
            }
        }

        @Override
        public void selectVulnScanPane() {
            MainController main = MainApplication.getMainController();
            if (main != null) {
                main.selectNavIndex(ToStart.StartupPage.RED_VUL_SCAN.getNavIndex());
            }
        }

        private PaneVulScan getPane() {
            MainController main = MainApplication.getMainController();
            return main == null ? null : main.getVulScanPane();
        }
    }
}

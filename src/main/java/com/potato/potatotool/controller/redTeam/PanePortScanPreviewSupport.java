package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortState;
import com.potato.potatotool.content.redTeam.portScanner.storage.PortScanDatabase;

import java.util.ArrayList;
import java.util.List;

final class PanePortScanPreviewSupport {

    private PanePortScanPreviewSupport() {
    }

    static PreviewState buildPreviewState(ToStart.StartupPage startupPage) {
        if (startupPage == null) {
            return null;
        }
        switch (startupPage) {
            case RED_PORT_SCAN_CONFIG:
                return buildConfigState();
            case RED_PORT_SCAN_RESULTS:
                return buildResultsState();
            case RED_PORT_SCAN_HISTORY:
                return buildHistoryState();
            case RED_PORT_SCAN_RUNNING:
                return buildRunningState();
            case RED_PORT_SCAN_EMPTY:
                return buildEmptyState();
            default:
                return null;
        }
    }

    private static PreviewState buildConfigState() {
        PreviewState state = buildBaseState();
        state.configExpanded = true;
        state.targetText = "demo.potato.example\n203.0.113.10\n198.51.100.0/30";
        state.portPreset = "Custom";
        state.customPorts = "22,80,443,8080,8443,9001-9003";
        state.timeout = "2200";
        state.batchSize = "768";
        state.publicMode = true;
        state.autoHandoff = true;
        return state;
    }

    private static PreviewState buildResultsState() {
        PreviewState state = buildConfigState();
        state.scanState = "Completed";
        state.progressText = "128 / 128";
        state.progressValue = 1.0;
        state.openCount = "6";
        state.resultItems.add(createPort("demo.potato.example", 80, "http", "nginx/1.24.0"));
        state.resultItems.add(createPort("demo.potato.example", 443, "https", "nginx/1.24.0 TLS"));
        state.resultItems.add(createPort("demo.potato.example", 8080, "http-proxy", "Spring Boot actuator"));
        state.resultItems.add(createPort("203.0.113.10", 22, "ssh", "OpenSSH_8.9p1 Debian"));
        state.resultItems.add(createPort("203.0.113.10", 3306, "mysql", "MySQL 8.0 preview"));
        state.resultItems.add(createPort("198.51.100.2", 8443, "https-alt", "Jetty admin"));
        return state;
    }

    private static PreviewState buildHistoryState() {
        PreviewState state = buildResultsState();
        state.historyVisible = true;
        state.historyItems.add(createHistory("scan-preview-20260624-001", 1719165000000L, 1719165064000L, 128, 128, 6));
        state.historyItems.add(createHistory("scan-preview-20260623-002", 1719078600000L, 1719078657000L, 96, 96, 4));
        state.historyItems.add(createHistory("scan-preview-20260622-003", 1718992200000L, 1718992280000L, 256, 256, 9));
        return state;
    }

    private static PreviewState buildRunningState() {
        PreviewState state = buildConfigState();
        state.scanState = "Scanning";
        state.progressText = "73 / 128";
        state.progressValue = 73.0 / 128.0;
        state.openCount = "3";
        state.scanning = true;
        state.resultItems.add(createPort("demo.potato.example", 80, "http", "nginx/1.24.0"));
        state.resultItems.add(createPort("203.0.113.10", 22, "ssh", "OpenSSH_8.9p1 Debian"));
        state.resultItems.add(createPort("198.51.100.2", 8443, "https-alt", "Jetty admin"));
        return state;
    }

    private static PreviewState buildEmptyState() {
        PreviewState state = buildBaseState();
        state.targetText = "198.51.100.200\n198.51.100.201";
        state.scanState = "Completed";
        state.progressText = "64 / 64";
        state.progressValue = 1.0;
        state.openCount = "0";
        return state;
    }

    private static PreviewState buildBaseState() {
        PreviewState state = new PreviewState();
        state.targetText = "demo.potato.example";
        state.portPreset = "Top1000";
        state.customPorts = "";
        state.timeout = "1500";
        state.batchSize = "1024";
        state.serviceProbe = true;
        state.publicMode = false;
        state.autoHandoff = false;
        state.scanState = "Ready";
        state.progressText = "0 / 0";
        state.progressValue = 0;
        state.openCount = "0";
        return state;
    }

    private static PortResult createPort(String host, int port, String service, String banner) {
        PortResult result = new PortResult(host, port, PortState.OPEN);
        result.setService(service);
        result.setBanner(banner);
        result.setRttMs(36 + port % 11);
        result.setTls(service != null && service.toLowerCase().contains("https"));
        return result;
    }

    private static PortScanDatabase.HistoryItem createHistory(String scanId, long startTime, long endTime,
                                                              long totalTasks, long completedTasks, int openCount) {
        return new PortScanDatabase.HistoryItem(scanId, startTime, endTime, totalTasks, completedTasks, openCount);
    }

    static final class PreviewState {
        private String targetText;
        private String portPreset;
        private String customPorts;
        private String timeout;
        private String batchSize;
        private boolean serviceProbe;
        private boolean publicMode;
        private boolean autoHandoff;
        private boolean configExpanded;
        private boolean historyVisible;
        private boolean scanning;
        private String scanState;
        private String progressText;
        private double progressValue;
        private String openCount;
        private final List<PortResult> resultItems = new ArrayList<PortResult>();
        private final List<PortScanDatabase.HistoryItem> historyItems = new ArrayList<PortScanDatabase.HistoryItem>();

        String getTargetText() {
            return targetText;
        }

        String getPortPreset() {
            return portPreset;
        }

        String getCustomPorts() {
            return customPorts;
        }

        String getTimeout() {
            return timeout;
        }

        String getBatchSize() {
            return batchSize;
        }

        boolean isServiceProbe() {
            return serviceProbe;
        }

        boolean isPublicMode() {
            return publicMode;
        }

        boolean isAutoHandoff() {
            return autoHandoff;
        }

        boolean isConfigExpanded() {
            return configExpanded;
        }

        boolean isHistoryVisible() {
            return historyVisible;
        }

        boolean isScanning() {
            return scanning;
        }

        String getScanState() {
            return scanState;
        }

        String getProgressText() {
            return progressText;
        }

        double getProgressValue() {
            return progressValue;
        }

        String getOpenCount() {
            return openCount;
        }

        List<PortResult> getResultItems() {
            return resultItems;
        }

        List<PortScanDatabase.HistoryItem> getHistoryItems() {
            return historyItems;
        }

        PortScanResult toResult() {
            PortScanResult result = new PortScanResult();
            result.setScanId("preview-portscan");
            result.setStartTime(1719165000000L);
            result.setEndTime((long) (1719165000000L + (getProgressValue() >= 1 ? 64000 : 36000)));
            String[] taskParts = getProgressText() == null ? null : getProgressText().split("/");
            if (taskParts != null && taskParts.length == 2) {
                result.setCompletedTasks(parseLong(taskParts[0]));
                result.setTotalTasks(parseLong(taskParts[1]));
            }
            for (PortResult item : resultItems) {
                result.addOrUpdate(item);
            }
            return result;
        }

        private long parseLong(String raw) {
            try {
                return Long.parseLong(raw.trim());
            } catch (Exception e) {
                return 0L;
            }
        }
    }
}

package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class PaneVulScanPreviewSupport {

    private PaneVulScanPreviewSupport() {
    }

    static PreviewState buildPreviewState(ToStart.StartupPage startupPage) {
        if (startupPage == null) {
            return null;
        }
        switch (startupPage) {
            case RED_VUL_SCAN_CONFIG:
                return buildConfigState();
            case RED_VUL_SCAN_ADVANCED:
                return buildAdvancedState();
            case RED_VUL_SCAN_PROGRESS_STATS:
                return buildProgressState();
            case RED_VUL_SCAN_RUNNING:
                return buildRunningState();
            case RED_VUL_SCAN_EMPTY:
                return buildEmptyState();
            case RED_VUL_SCAN_EXPORT:
                return buildExportState();
            case RED_VUL_SCAN_RESULTS:
                return buildResultsState();
            case RED_VUL_SCAN_POC_MANAGE:
                return buildPocManageState();
            case RED_VUL_SCAN_POC_DETAIL:
                return buildPocDetailState();
            case RED_VUL_SCAN_LOGS:
                return buildLogsState();
            case RED_VUL_SCAN_HISTORY:
                return buildHistoryState();
            default:
                return null;
        }
    }

    private static PreviewState buildBaseState() {
        PreviewState state = new PreviewState();
        state.targetText = "https://demo.potato.example\nhttps://console.potato.example\n198.51.100.32:8443";
        state.scanModeIndex = 2;
        state.inputTypeIndex = 0;
        state.exportFormatIndex = 0;
        state.nucleiSelected = true;
        state.gobySelected = true;
        state.xraySelected = true;
        state.pocsuiteSelected = true;
        state.verboseLogSelected = true;
        state.autoSaveSelected = true;
        state.enableHeadlessSelected = false;
        state.enableCodeSelected = false;
        state.enableFuzzSelected = false;
        state.includeOsintSelected = false;
        state.includeTokenSpraySelected = false;
        state.tagsText = "rce,auth-bypass";
        state.scanStateText = "Ready";
        state.progressText = "0 / 0";
        state.timeText = "00:00:00";
        state.summaryMessage = "Preview ready";
        state.summaryComplete = true;
        state.pocTotal = "2480";
        state.pocNuclei = "1880";
        state.pocGoby = "240";
        state.pocXray = "210";
        state.pocPocsuite = "150";
        state.vulnCount = "0";
        return state;
    }

    private static PreviewState buildConfigState() {
        PreviewState state = buildBaseState();
        state.configExpanded = true;
        state.targetText = "https://demo.potato.example\nhttps://api.potato.example\n198.51.100.32:8443\n203.0.113.77";
        state.summaryMessage = "4 targets loaded";
        return state;
    }

    private static PreviewState buildAdvancedState() {
        PreviewState state = buildConfigState();
        state.scanModeIndex = 3;
        state.tagsText = "rce,auth-bypass,java,spring";
        state.enableHeadlessSelected = true;
        state.enableCodeSelected = true;
        state.enableFuzzSelected = true;
        state.includeOsintSelected = true;
        state.includeTokenSpraySelected = true;
        state.inputTypeIndex = 1;
        state.summaryMessage = "Advanced options prepared";
        return state;
    }

    private static PreviewState buildProgressState() {
        PreviewState state = buildAdvancedState();
        state.showProgress = true;
        state.showStats = true;
        state.progressValue = 0.64;
        state.progressText = "64 / 100";
        state.timeText = "00:03:41";
        state.scanStateText = "Analyzing matched responses";
        state.summaryMessage = "64 tasks finished, 2 vulnerabilities confirmed";
        state.criticalCount = "0";
        state.highCount = "1";
        state.mediumCount = "1";
        state.lowCount = "0";
        state.infoCount = "0";
        state.vulnCount = "2";
        state.resultItems.add(createResult(
                "https://demo.potato.example",
                "nuclei-cve-2025-1001",
                "Spring Gateway RCE",
                PocObj.Severity.HIGH,
                "nuclei",
                "http",
                "/actuator/gateway/routes",
                "{{interactsh-url}}",
                "GET /actuator/gateway/routes HTTP/1.1\nHost: demo.potato.example",
                "HTTP/1.1 200 OK\nServer: nginx\nX-Preview: matched",
                "Spring Gateway unauthenticated RCE preview"));
        state.resultItems.add(createResult(
                "https://console.potato.example",
                "xray-auth-bypass-2025",
                "Admin Console Auth Bypass",
                PocObj.Severity.MEDIUM,
                "xray",
                "http",
                "/admin/login",
                "rememberMe=1",
                "POST /admin/login HTTP/1.1\nHost: console.potato.example",
                "HTTP/1.1 302 Found\nLocation: /admin/index",
                "Auth bypass via weak remember-me token"));
        return state;
    }

    private static PreviewState buildRunningState() {
        PreviewState state = buildProgressState();
        state.progressValue = 0.42;
        state.progressText = "42 / 100";
        state.timeText = "00:02:06";
        state.scanStateText = "Running";
        state.summaryMessage = "Scanning 3 active targets";
        state.summaryComplete = false;
        state.buttonStatus = "RUNNING";
        state.criticalCount = "0";
        state.highCount = "1";
        state.mediumCount = "0";
        state.lowCount = "0";
        state.infoCount = "0";
        state.vulnCount = "1";
        while (state.resultItems.size() > 1) {
            state.resultItems.remove(state.resultItems.size() - 1);
        }
        return state;
    }

    private static PreviewState buildEmptyState() {
        PreviewState state = buildConfigState();
        state.showProgress = true;
        state.progressValue = 1.0;
        state.progressText = "24 / 24";
        state.timeText = "00:01:19";
        state.scanStateText = "Completed";
        state.summaryMessage = "Completed, no vulnerability found";
        state.summaryComplete = true;
        state.buttonStatus = "COMPLETED";
        return state;
    }

    private static PreviewState buildExportState() {
        PreviewState state = buildResultsState();
        state.exportFormatIndex = 1;
        state.summaryMessage = "Ready to export report";
        return state;
    }

    private static PreviewState buildResultsState() {
        PreviewState state = buildProgressState();
        state.progressValue = 1.0;
        state.progressText = "100 / 100";
        state.timeText = "00:05:12";
        state.scanStateText = "Completed";
        state.summaryMessage = "3 vulnerabilities confirmed";
        state.summaryComplete = true;
        state.buttonStatus = "COMPLETED";
        state.criticalCount = "1";
        state.highCount = "1";
        state.mediumCount = "1";
        state.lowCount = "0";
        state.infoCount = "0";
        state.vulnCount = "3";
        state.resultItems.add(createResult(
                "198.51.100.32:8443",
                "goby-apache-ofbiz-rce",
                "Apache OFBiz XML-RPC RCE",
                PocObj.Severity.CRITICAL,
                "goby",
                "http",
                "/webtools/control/xmlrpc",
                "${jndi:ldap://demo}",
                "POST /webtools/control/xmlrpc HTTP/1.1\nHost: 198.51.100.32:8443",
                "HTTP/1.1 200 OK\nContent-Type: text/xml",
                "Unauthenticated RCE in XML-RPC service"));
        return state;
    }

    private static PreviewState buildPocManageState() {
        PreviewState state = buildResultsState();
        state.pocManageVisible = true;
        state.pocItems.add(createPocItem("nuclei-cve-2025-1001", "Spring Gateway RCE", "nuclei",
                PocObj.Severity.HIGH, "http", true, "spring,gateway,rce"));
        state.pocItems.add(createPocItem("xray-auth-bypass-2025", "Admin Console Auth Bypass", "xray",
                PocObj.Severity.MEDIUM, "http", true, "auth-bypass,console"));
        state.pocItems.add(createPocItem("goby-apache-ofbiz-rce", "Apache OFBiz XML-RPC RCE", "goby",
                PocObj.Severity.CRITICAL, "http", false, "apache,ofbiz,rce"));
        state.selectedPocCount = 2;
        return state;
    }

    private static PreviewState buildPocDetailState() {
        PreviewState state = buildPocManageState();
        state.pocDetailVisible = true;
        state.pocDetailItem = createPocItem("nuclei-cve-2025-1001", "Spring Gateway RCE", "nuclei",
                PocObj.Severity.HIGH, "http", true, "spring,gateway,rce");
        state.pocDetailFilename = "spring-gateway-rce.yaml";
        state.pocDetailContentText =
                "id: nuclei-cve-2025-1001\n" +
                "info:\n" +
                "  name: Spring Gateway RCE\n" +
                "  severity: high\n" +
                "http:\n" +
                "  - method: GET\n" +
                "    path:\n" +
                "      - \"{{BaseURL}}/actuator/gateway/routes\"";
        return state;
    }

    private static PreviewState buildLogsState() {
        PreviewState state = buildResultsState();
        state.logViewerVisible = true;
        state.logText =
                "[INFO] [02:24:10] loaded 2480 poc entries\n" +
                "[SUCCESS] [02:24:14] Spring Gateway RCE matched https://demo.potato.example\n" +
                "[WARN] [02:24:17] Headless template skipped for 198.51.100.32:8443\n" +
                "[INFO] [02:24:22] report generator ready";
        return state;
    }

    private static PreviewState buildHistoryState() {
        PreviewState state = buildResultsState();
        state.historyVisible = true;
        state.historyItems.add(createHistory("scan-preview-20260624-001", "2026-06-24 02:21:10", "4", "64", "3", "00:05:12", "Completed"));
        state.historyItems.add(createHistory("scan-preview-20260623-002", "2026-06-23 18:02:44", "3", "32", "1", "00:03:07", "Completed"));
        state.historyItems.add(createHistory("scan-preview-20260622-003", "2026-06-22 11:14:09", "9", "128", "0", "00:08:35", "Stopped"));
        return state;
    }

    private static ScanResult createResult(String target, String id, String name, PocObj.Severity severity,
                                           String format, String protocol, String matchedPath, String payload,
                                           String request, String response, String description) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(name);
        poc.setSeverity(severity);
        poc.setOriginalFormat(format);
        poc.setProtocol(protocol);
        poc.setVulType("RCE");
        poc.setDescription(description);
        poc.setTags(Arrays.asList("preview", severity.name().toLowerCase(), format));

        ScanResult result = new ScanResult();
        result.setTarget(target);
        result.setPoc(poc);
        result.setVulnerable(true);
        result.setMatchedPath(matchedPath);
        result.setMatchedPayload(payload);
        result.setRawRequest(request);
        result.setRawResponseSnippet(response);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    private static PaneVulScan.PocItem createPocItem(String id, String name, String format, PocObj.Severity severity,
                                                     String protocol, boolean enabled, String tags) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(name);
        poc.setOriginalFormat(format);
        poc.setSeverity(severity);
        poc.setProtocol(protocol);
        poc.setTags(Arrays.asList(tags.split(",")));
        return new PaneVulScan.PocItem(poc, enabled);
    }

    private static PaneVulScan.HistoryItem createHistory(String id, String scanTime, String targets, String pocs,
                                                         String vulns, String duration, String status) {
        PaneVulScan.HistoryItem item = new PaneVulScan.HistoryItem();
        item.setId(id);
        item.setScanTime(scanTime);
        item.setTargets(targets);
        item.setPocs(pocs);
        item.setVulns(vulns);
        item.setDuration(duration);
        item.setStatus(status);
        return item;
    }

    static final class PreviewState {
        private String targetText;
        private boolean configExpanded;
        private int scanModeIndex;
        private int inputTypeIndex;
        private int exportFormatIndex;
        private boolean nucleiSelected;
        private boolean gobySelected;
        private boolean xraySelected;
        private boolean pocsuiteSelected;
        private boolean verboseLogSelected;
        private boolean autoSaveSelected;
        private boolean enableHeadlessSelected;
        private boolean enableCodeSelected;
        private boolean enableFuzzSelected;
        private boolean includeOsintSelected;
        private boolean includeTokenSpraySelected;
        private boolean showProgress;
        private double progressValue;
        private String progressText;
        private String timeText;
        private String scanStateText;
        private String tagsText;
        private String summaryMessage;
        private boolean summaryComplete;
        private boolean showStats;
        private String criticalCount = "0";
        private String highCount = "0";
        private String mediumCount = "0";
        private String lowCount = "0";
        private String infoCount = "0";
        private String vulnCount = "0";
        private String pocTotal = "0";
        private String pocNuclei = "0";
        private String pocGoby = "0";
        private String pocXray = "0";
        private String pocPocsuite = "0";
        private String buttonStatus = "STOPPED";
        private boolean pocManageVisible;
        private boolean pocDetailVisible;
        private boolean logViewerVisible;
        private boolean historyVisible;
        private String logText = "";
        private PaneVulScan.PocItem pocDetailItem;
        private String pocDetailFilename;
        private String pocDetailContentText;
        private int selectedPocCount;
        private final List<ScanResult> resultItems = new ArrayList<ScanResult>();
        private final List<PaneVulScan.PocItem> pocItems = new ArrayList<PaneVulScan.PocItem>();
        private final List<PaneVulScan.HistoryItem> historyItems = new ArrayList<PaneVulScan.HistoryItem>();

        String getTargetText() {
            return targetText;
        }

        boolean isConfigExpanded() {
            return configExpanded;
        }

        int getScanModeIndex() {
            return scanModeIndex;
        }

        int getInputTypeIndex() {
            return inputTypeIndex;
        }

        int getExportFormatIndex() {
            return exportFormatIndex;
        }

        boolean isNucleiSelected() {
            return nucleiSelected;
        }

        boolean isGobySelected() {
            return gobySelected;
        }

        boolean isXraySelected() {
            return xraySelected;
        }

        boolean isPocsuiteSelected() {
            return pocsuiteSelected;
        }

        boolean isVerboseLogSelected() {
            return verboseLogSelected;
        }

        boolean isAutoSaveSelected() {
            return autoSaveSelected;
        }

        boolean isEnableHeadlessSelected() {
            return enableHeadlessSelected;
        }

        boolean isEnableCodeSelected() {
            return enableCodeSelected;
        }

        boolean isEnableFuzzSelected() {
            return enableFuzzSelected;
        }

        boolean isIncludeOsintSelected() {
            return includeOsintSelected;
        }

        boolean isIncludeTokenSpraySelected() {
            return includeTokenSpraySelected;
        }

        boolean isShowProgress() {
            return showProgress;
        }

        double getProgressValue() {
            return progressValue;
        }

        String getProgressText() {
            return progressText;
        }

        String getTimeText() {
            return timeText;
        }

        String getScanStateText() {
            return scanStateText;
        }

        String getTagsText() {
            return tagsText;
        }

        String getSummaryMessage() {
            return summaryMessage;
        }

        boolean isSummaryComplete() {
            return summaryComplete;
        }

        boolean isShowStats() {
            return showStats;
        }

        String getCriticalCount() {
            return criticalCount;
        }

        String getHighCount() {
            return highCount;
        }

        String getMediumCount() {
            return mediumCount;
        }

        String getLowCount() {
            return lowCount;
        }

        String getInfoCount() {
            return infoCount;
        }

        String getVulnCount() {
            return vulnCount;
        }

        String getPocTotal() {
            return pocTotal;
        }

        String getPocNuclei() {
            return pocNuclei;
        }

        String getPocGoby() {
            return pocGoby;
        }

        String getPocXray() {
            return pocXray;
        }

        String getPocPocsuite() {
            return pocPocsuite;
        }

        String getButtonStatus() {
            return buttonStatus;
        }

        boolean isPocManageVisible() {
            return pocManageVisible;
        }

        boolean isPocDetailVisible() {
            return pocDetailVisible;
        }

        boolean isLogViewerVisible() {
            return logViewerVisible;
        }

        boolean isHistoryVisible() {
            return historyVisible;
        }

        String getLogText() {
            return logText;
        }

        PaneVulScan.PocItem getPocDetailItem() {
            return pocDetailItem;
        }

        String getPocDetailFilename() {
            return pocDetailFilename;
        }

        String getPocDetailContentText() {
            return pocDetailContentText;
        }

        int getSelectedPocCount() {
            return selectedPocCount;
        }

        List<ScanResult> getResultItems() {
            return resultItems;
        }

        List<PaneVulScan.PocItem> getPocItems() {
            return pocItems;
        }

        List<PaneVulScan.HistoryItem> getHistoryItems() {
            return historyItems;
        }
    }
}

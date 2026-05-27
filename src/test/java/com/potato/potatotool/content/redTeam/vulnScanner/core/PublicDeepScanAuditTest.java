package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.SmartPocSelector.PocSelectionResult;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanCompletedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanErrorEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventListener;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanInfoEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanProgressEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanStartedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.VulnerabilityFoundEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ScanLogger;
import com.potato.potatotool.storage.PathManager;
import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("公网深度扫描审计测试")
class PublicDeepScanAuditTest {

    private static final Path EVIDENCE_ROOT = Paths.get("target/vulnscan-public-audit");
    private static final String ALL_POC_ROOT = "src/main/resources/poc";
    private static final long TARGET_TIMEOUT_SECONDS = 180L;
    private static final List<PublicTarget> TARGETS = Arrays.asList(
            new PublicTarget("owner-blog", "https://potato.gold", "用户声明拥有的博客网站"),
            new PublicTarget("vulnweb-php", "http://testphp.vulnweb.com", "Acunetix Vulnweb intentionally vulnerable test site"),
            new PublicTarget("vulnweb-html5", "http://testhtml5.vulnweb.com", "Acunetix Vulnweb intentionally vulnerable test site")
    );

    private String originalUserHome;

    @AfterEach
    void tearDown() throws Exception {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
            originalUserHome = null;
        }
        Constants.cachedConfig = null;
        resetSingleton(com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase.class, "instance", true);
        resetSingleton(PathManager.class, "instance", false);
        resetSingleton(com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig.class, "instance", false);
        resetSingleton(ScanLogger.class, "instance", false);
        resetSingleton(VulnScanService.class, "instance", false);
    }

    @Test
    @DisplayName("应对授权公网目标执行深度扫描并导出完整输出")
    @EnabledIfSystemProperty(named = "vulnscan.public.audit", matches = "true")
    void shouldRunDeepScanAgainstAuthorizedPublicTargets(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        Files.createDirectories(EVIDENCE_ROOT);
        cleanEvidenceFiles();
        ScanLogger.getInstance().clearMemoryLogs();

        ScanConfig config = createDeepConfig();
        VulnScanService service = VulnScanService.getInstance();
        for (PocObj.Poc poc : loadPublicAuditPocs()) {
            service.getPocRepository().addPoc(poc);
        }
        service.applyScanConfig(config);

        List<String> targetRows = new ArrayList<String>();
        List<String> selectedRows = new ArrayList<String>();
        List<String> eventRows = Collections.synchronizedList(new ArrayList<String>());
        List<String> vulnRows = Collections.synchronizedList(new ArrayList<String>());
        List<String> errorRows = Collections.synchronizedList(new ArrayList<String>());
        selectedRows.add("target\tselected_poc_count\tpoc_ids");
        targetRows.add("name\turl\tauthorization_note");
        for (PublicTarget target : TARGETS) {
            targetRows.add(target.name + "\t" + target.url + "\t" + target.authorizationNote);
        }

        service.addEventListener(new ScanEventListener() {
            @Override
            public void onScanStarted(ScanStartedEvent event) {
                eventRows.add("SCAN_STARTED\t" + nullSafe(event.getScanId()) + "\t"
                        + event.getTotalTargets() + "\t" + event.getTotalPocs() + "\t" + event.getTotalTasks());
            }

            @Override
            public void onScanInfo(ScanInfoEvent event) {
                eventRows.add("SCAN_INFO\t" + nullSafe(event.getScanId()) + "\t"
                        + event.getInfoType().name() + "\t" + sanitize(event.getMessage()));
            }

            @Override
            public void onScanProgress(ScanProgressEvent event) {
                eventRows.add("SCAN_PROGRESS\t" + nullSafe(event.getScanId()) + "\t"
                        + event.getCompletedTasks() + "/" + event.getTotalTasks() + "\t"
                        + event.getVulnerabilitiesFound());
            }

            @Override
            public void onVulnerabilityFound(VulnerabilityFoundEvent event) {
                ScanResult result = event.getResult();
                String pocId = result.getPoc() == null ? "" : result.getPoc().getId();
                String pocName = result.getPoc() == null ? "" : result.getPoc().getName();
                vulnRows.add(sanitize(result.getTarget()) + "\t" + sanitize(pocId) + "\t"
                        + sanitize(pocName) + "\t" + sanitize(result.getMatchedPath()));
                eventRows.add("VULNERABILITY_FOUND\t" + nullSafe(event.getScanId()) + "\t"
                        + sanitize(result.getTarget()) + "\t" + sanitize(pocId));
            }

            @Override
            public void onScanError(ScanErrorEvent event) {
                errorRows.add(nullSafe(event.getScanId()) + "\t" + sanitize(event.getTarget()) + "\t"
                        + sanitize(event.getPocId()) + "\t" + sanitize(event.getErrorMessage()));
                eventRows.add("SCAN_ERROR\t" + nullSafe(event.getScanId()) + "\t"
                        + sanitize(event.getTarget()) + "\t" + sanitize(event.getPocId()) + "\t"
                        + sanitize(event.getErrorMessage()));
            }
        });

        List<String> completedRows = new ArrayList<String>();
        completedRows.add("target\tscan_id\tstatus\tselected_pocs\tresult_count\tvulnerability_count\tduration_ms\tnote");
        writeEvidence(targetRows, selectedRows, eventRows, vulnRows, errorRows, completedRows);

        for (PublicTarget target : TARGETS) {
            String activeScanId = "";
            int selectedCount = 0;
            try {
                service.clearTargets();
                service.addTarget(target.url);

                PocSelectionResult selection = service.selectAndFilterPocs(target.url, config);
                selectedCount = selection.getPocs().size();
                selectedRows.add(target.url + "\t" + selectedCount + "\t" + joinPocIds(selection.getPocs()));
                if (selection.getPocs().isEmpty()) {
                    completedRows.add(target.url + "\t\tSKIPPED_EMPTY_SELECTION\t0\t0\t0\t0\t未筛选出可执行 POC");
                    continue;
                }

                CountDownLatch completed = new CountDownLatch(1);
                AtomicReference<String> scanIdRef = new AtomicReference<String>("");
                AtomicReference<ScanCompletedEvent> completedRef = new AtomicReference<ScanCompletedEvent>();
                ScanEventListener completionListener = new ScanEventListener() {
                    @Override
                    public void onScanStarted(ScanStartedEvent event) {
                        scanIdRef.set(nullSafe(event.getScanId()));
                    }

                    @Override
                    public void onScanCompleted(ScanCompletedEvent event) {
                        completedRef.set(event);
                        completed.countDown();
                    }
                };
                service.addEventListener(completionListener);
                service.startScan(Collections.singletonList(target.url), selection.getPocs(), selection.getFingerprint());
                boolean finished = completed.await(TARGET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                activeScanId = scanIdRef.get();
                if (!finished) {
                    errorRows.add(activeScanId + "\t" + sanitize(target.url) + "\t\t"
                            + "HARNESS_TIMEOUT_AFTER_" + TARGET_TIMEOUT_SECONDS + "_SECONDS");
                    eventRows.add("HARNESS_TIMEOUT\t" + activeScanId + "\t" + sanitize(target.url)
                            + "\t" + TARGET_TIMEOUT_SECONDS + "s");
                    service.stopScan();
                    waitForStop(service, 15);
                    completedRows.add(target.url + "\t" + activeScanId + "\tTIMEOUT_STOPPED\t"
                            + selectedCount + "\t\t\t" + (TARGET_TIMEOUT_SECONDS * 1000L)
                            + "\t超过审计夹具单目标等待时间，已调用 stopScan()");
                } else {
                    ScanCompletedEvent event = completedRef.get();
                    if (event == null) {
                        completedRows.add(target.url + "\t" + activeScanId + "\tCOMPLETED_EVENT_MISSING\t"
                                + selectedCount + "\t\t\t\tCountDownLatch 已释放但事件对象为空");
                    } else {
                        completedRows.add(target.url + "\t" + event.getScanId() + "\t"
                                + (event.isSuccess() ? "COMPLETED_SUCCESS" : "COMPLETED_FAILURE") + "\t"
                                + selectedCount + "\t" + event.getResults().size() + "\t"
                                + event.getVulnerabilityCount() + "\t" + event.getDuration() + "\t");
                    }
                }
            } finally {
                if (service.isScanning()) {
                    service.stopScan();
                    waitForStop(service, 15);
                    completedRows.add(target.url + "\t" + activeScanId + "\tSTOPPED_IN_FINALLY\t"
                            + selectedCount + "\t\t\t\t目标扫描结束清理时仍处于运行状态");
                }
                writeEvidence(targetRows, selectedRows, eventRows, vulnRows, errorRows, completedRows);
            }
        }

        service.shutdown();
        writeEvidence(targetRows, selectedRows, eventRows, vulnRows, errorRows, completedRows);
    }

    private ScanConfig createDeepConfig() {
        ScanConfig config = new ScanConfig();
        config.setThreads(3);
        config.setTimeout(8);
        config.setRetries(0);
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setEnableHeadless(true);
        config.setEnableCode(true);
        config.setEnableFuzz(true);
        config.setOobInteractionWaitSeconds(0);
        config.setSkipFingerprint(false);
        config.setFingerprintTimeout(3);
        config.setDebug(true);
        config.setScanMode(ScanConfig.ScanMode.DEEP);
        config.getEnabledCategories().clear();
        config.getExcludedCategories().clear();
        return config;
    }

    private List<PocObj.Poc> loadPublicAuditPocs() throws Exception {
        PocLoader loader = new PocLoader();
        loader.setVerbose(false);
        loader.setSkipInvalidPocs(true);
        List<PocObj.Poc> pocs = loader.loadFromDirectory(ALL_POC_ROOT);
        assertFalse(pocs.isEmpty(), "全量 POC 加载结果不应为空: " + ALL_POC_ROOT);
        return pocs;
    }

    private void configureIsolatedHome(Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path dbPath = tempHome.resolve("db").resolve("public-deep-scan-audit.sqlite");
        Files.createDirectories(dbPath.getParent());

        String json = "{\n" +
                "  \"" + ConfigConstants.PROXY + "\": {\n" +
                "    \"" + ConfigConstants.PROXY_ENABLE + "\": false,\n" +
                "    \"" + ConfigConstants.PROXY_ADDRESS + "\": \"\",\n" +
                "    \"" + ConfigConstants.PROXY_SERVICES + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_SERVICE + "\": false\n" +
                "    }\n" +
                "  },\n" +
                "  \"VulnScan\": {\n" +
                "    \"dbPath\": \"" + escapeForJson(dbPath.toString()) + "\"\n" +
                "  }\n" +
                "}";
        Files.write(configDir.resolve("config.json"), json.getBytes(StandardCharsets.UTF_8));
        Constants.cachedConfig = null;
        resetSingleton(com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase.class, "instance", true);
        resetSingleton(PathManager.class, "instance", false);
        resetSingleton(com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig.class, "instance", false);
        resetSingleton(ScanLogger.class, "instance", false);
        resetSingleton(VulnScanService.class, "instance", false);
    }

    private List<String> buildMemoryLogRows(List<ScanLogger.LogEntry> logs) {
        List<String> rows = new ArrayList<String>();
        rows.add("time\tlevel\tcategory\tscan_id\tmessage");
        for (ScanLogger.LogEntry entry : logs) {
            rows.add(entry.getFormattedTime() + "\t" + entry.getLevel().name() + "\t"
                    + sanitize(entry.getCategory()) + "\t" + sanitize(entry.getScanId()) + "\t"
                    + sanitize(entry.getMessage()));
        }
        return rows;
    }

    private void writeEvidence(List<String> targetRows,
                               List<String> selectedRows,
                               List<String> eventRows,
                               List<String> vulnRows,
                               List<String> errorRows,
                               List<String> completedRows) throws IOException {
        Files.write(EVIDENCE_ROOT.resolve("targets.tsv"), snapshot(targetRows), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("selected-pocs.tsv"), snapshot(selectedRows), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("events.tsv"), withHeader(
                "event\tscan_id\tcol_1\tcol_2\tcol_3\tcol_4", eventRows), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("vulnerabilities.tsv"), withHeader(
                "target\tpoc_id\tpoc_name\tmatched_path", vulnRows), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("errors.tsv"), withHeader(
                "scan_id\ttarget\tpoc_id\terror", errorRows), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("completed.tsv"), snapshot(completedRows), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("memory-logs.tsv"),
                buildMemoryLogRows(ScanLogger.getInstance().getMemoryLogs()), StandardCharsets.UTF_8);
    }

    private void cleanEvidenceFiles() throws IOException {
        for (String name : Arrays.asList("targets.tsv", "selected-pocs.tsv", "events.tsv",
                "vulnerabilities.tsv", "errors.tsv", "completed.tsv", "memory-logs.tsv")) {
            Files.deleteIfExists(EVIDENCE_ROOT.resolve(name));
        }
    }

    private void waitForStop(VulnScanService service, int seconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (service.isScanning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200L);
        }
    }

    private List<String> withHeader(String header, List<String> rows) {
        List<String> result = new ArrayList<String>();
        result.add(header);
        result.addAll(snapshot(rows));
        return result;
    }

    private List<String> snapshot(List<String> rows) {
        synchronized (rows) {
            return new ArrayList<String>(rows);
        }
    }

    private String joinPocIds(List<PocObj.Poc> pocs) {
        Set<String> ids = new LinkedHashSet<String>();
        for (PocObj.Poc poc : pocs) {
            ids.add(poc.getId());
        }
        return String.join(",", ids);
    }

    private void resetSingleton(Class<?> type, String fieldName, boolean closeDatabase) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object existing = field.get(null);
        if (closeDatabase && existing instanceof com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase) {
            ((com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase) existing).close();
        }
        field.set(null, null);
    }

    private String escapeForJson(String value) {
        return value.replace("\\", "\\\\");
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\t", " ").replace("\n", " ").replace("\r", " ");
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static class PublicTarget {
        private final String name;
        private final String url;
        private final String authorizationNote;

        private PublicTarget(String name, String url, String authorizationNote) {
            this.name = name;
            this.url = url;
            this.authorizationNote = authorizationNote;
        }
    }
}

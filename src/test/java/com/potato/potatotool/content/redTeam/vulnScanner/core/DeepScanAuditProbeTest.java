package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.SmartPocSelector.MultiTargetSelectionResult;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanCompletedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanErrorEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventListener;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanInfoEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanProgressEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanStartedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.VulnerabilityFoundEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ScanLogger;
import com.potato.potatotool.storage.PathManager;
import com.potato.potatotool.utils.core.Constants;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("深度扫描本地审计探针测试")
class DeepScanAuditProbeTest {

    private static final Path EVIDENCE_ROOT = Paths.get("target/vulnscan-deep-audit");
    private static final String ALL_POC_ROOT = "src/main/resources/poc";

    private final List<HttpServer> servers = new ArrayList<HttpServer>();
    private String originalUserHome;

    @AfterEach
    void tearDown() throws Exception {
        for (HttpServer server : servers) {
            try {
                server.stop(0);
            } catch (Exception ignore) {
            }
        }
        servers.clear();
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
            originalUserHome = null;
        }
        DnsLogService.setMockMode(false);
        DnsLogService.clearCache();
        Constants.cachedConfig = null;
        resetSingleton(com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase.class, "instance", true);
        resetSingleton(PathManager.class, "instance", false);
        resetSingleton(com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig.class, "instance", false);
        resetSingleton(ScanLogger.class, "instance", false);
        resetSingleton(VulnScanService.class, "instance", false);
    }

    @Test
    @DisplayName("深度扫描应导出完整输出并避免多目标 POC 串扰")
    void shouldCaptureDeepScanOutputsWithoutCrossTargetPocBleed(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        Files.createDirectories(EVIDENCE_ROOT);
        ScanLogger.getInstance().clearMemoryLogs();

        AuditTarget safeBaseline = startTarget("safe-baseline",
                html("Safe Control Page", "safe page"),
                Collections.<String, String>emptyMap());

        AuditTarget grafanaAuth = startTarget("grafana-auth",
                html("Safe Control Page", "safe page"),
                Collections.<String, String>emptyMap());
        grafanaAuth.methodPathResponses.put("GET /login",
                htmlResponse("<html><head><title>Grafana</title></head><body>{\"subTitle\":\"Grafana v10.4.1\"}</body></html>"));
        grafanaAuth.dynamicResponders.put("POST /login", new AuditResponder() {
            @Override
            public AuditResponse respond(HttpExchange exchange) throws IOException {
                String requestBody = readRequestBody(exchange);
                if (requestBody.contains("\"user\":\"admin\"")
                        && requestBody.contains("\"password\":\"admin\"")) {
                    AuditResponse response = jsonResponse(200, "{\"message\":\"Logged in\"}");
                    response.headers.put("Set-Cookie", "grafana_session=golden-session; Path=/; HttpOnly");
                    return response;
                }
                return jsonResponse(401, "{\"message\":\"Invalid username or password\"}");
            }
        });
        grafanaAuth.methodPathResponses.put("POST /api/user/signup/step2",
                jsonResponse(403, "{\"message\":\"Sign up is disabled\"}"));

        AuditTarget h2Console = startTarget("h2-console",
                html("Safe Control Page", "safe page"),
                Collections.<String, String>emptyMap());
        h2Console.pathBodies.put("/h2-console/login.jsp",
                "<html><head><title>H2 Console</title></head><body>login</body></html>");

        List<String> targets = Arrays.asList(safeBaseline.baseUrl, grafanaAuth.baseUrl, h2Console.baseUrl);

        ScanConfig auditConfig = createDeepConfig(true);
        ScanConfig executionConfig = createDeepConfig(false);
        VulnScanService service = VulnScanService.getInstance();
        for (PocObj.Poc poc : loadAuditPocs()) {
            service.getPocRepository().addPoc(poc);
        }
        service.applyScanConfig(executionConfig);
        service.clearTargets();
        for (String target : targets) {
            service.addTarget(target);
        }

        List<String> eventRows = Collections.synchronizedList(new ArrayList<String>());
        List<String> vulnRows = Collections.synchronizedList(new ArrayList<String>());
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<ScanCompletedEvent> completedRef = new AtomicReference<ScanCompletedEvent>();
        AtomicReference<ScanErrorEvent> topLevelErrorRef = new AtomicReference<ScanErrorEvent>();

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
                eventRows.add("VULNERABILITY_FOUND\t" + nullSafe(event.getScanId()) + "\t"
                        + sanitize(result.getTarget()) + "\t" + sanitize(pocId));
                vulnRows.add(sanitize(result.getTarget()) + "\t" + sanitize(pocId));
            }

            @Override
            public void onScanError(ScanErrorEvent event) {
                eventRows.add("SCAN_ERROR\t" + nullSafe(event.getScanId()) + "\t"
                        + sanitize(event.getTarget()) + "\t" + sanitize(event.getPocId()) + "\t"
                        + sanitize(event.getErrorMessage()));
                if (event.getTarget() == null && event.getPocId() == null) {
                    topLevelErrorRef.set(event);
                }
            }

            @Override
            public void onScanCompleted(ScanCompletedEvent event) {
                eventRows.add("SCAN_COMPLETED\t" + nullSafe(event.getScanId()) + "\t"
                        + event.isSuccess() + "\t" + event.getResults().size() + "\t"
                        + event.getVulnerabilityCount() + "\t" + event.getDuration());
                completedRef.set(event);
                completed.countDown();
            }
        });

        MultiTargetSelectionResult selection;
        try {
            MultiTargetSelectionResult auditSelection = service.selectAndFilterPocsMultiTarget(targets, auditConfig);
            Files.write(EVIDENCE_ROOT.resolve("all-selected-pocs.tsv"),
                    buildSelectedPocRows(auditSelection.getPocs()), StandardCharsets.UTF_8);
            selection = service.selectAndFilterPocsMultiTarget(targets, executionConfig);
            Files.write(EVIDENCE_ROOT.resolve("selected-pocs.tsv"),
                    buildSelectedPocRows(selection.getPocs()), StandardCharsets.UTF_8);
            Files.write(EVIDENCE_ROOT.resolve("fingerprint-map.tsv"),
                    buildFingerprintRows(selection, targets), StandardCharsets.UTF_8);
            Files.write(EVIDENCE_ROOT.resolve("selection-requests.tsv"),
                    buildRequestRows(Arrays.asList(safeBaseline, grafanaAuth, h2Console)), StandardCharsets.UTF_8);
            resetRequestCounters(safeBaseline, grafanaAuth, h2Console);

            service.startScan(new ArrayList<String>(targets), selection);

            assertTrue(completed.await(60, TimeUnit.SECONDS), "深度扫描未在预期时间内完成");
        } finally {
            service.shutdown();
        }

        ScanCompletedEvent completedEvent = completedRef.get();
        assertNotNull(completedEvent, "未收到扫描完成事件");
        assertTrue(completedEvent.isSuccess(), "扫描不应失败");
        assertFalse(selection.getPocs().isEmpty(), "深度扫描不应筛选出空 POC 集");
        assertTrue(selection.getPocsByTarget().size() == targets.size(),
                "多目标选择结果应为每个目标保留一个 POC 子集");

        Files.write(EVIDENCE_ROOT.resolve("events.tsv"), withHeader(
                "event\tscan_id\tcol_1\tcol_2\tcol_3\tcol_4", eventRows), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("vulnerabilities.tsv"), withHeader(
                "target\tpoc_id", vulnRows), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("requests.tsv"),
                buildRequestRows(Arrays.asList(safeBaseline, grafanaAuth, h2Console)), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("memory-logs.tsv"),
                buildMemoryLogRows(ScanLogger.getInstance().getMemoryLogs()), StandardCharsets.UTF_8);
        Files.write(EVIDENCE_ROOT.resolve("summary.tsv"),
                buildSummaryRows(selection, completedEvent, topLevelErrorRef.get(),
                        Arrays.asList(safeBaseline, grafanaAuth, h2Console), eventRows, tempHome),
                StandardCharsets.UTF_8);

        assertFalse(hasSelectionInfoWithoutScanId(eventRows),
                "筛选阶段 ScanInfoEvent 不应缺失 scanId");
        assertFalse(logFileWasNotCreated(tempHome),
                "扫描日志文件应随扫描会话创建");
    }

    @Test
    @DisplayName("停止扫描时应派发完成事件释放调用方等待")
    void shouldDispatchCompletedEventWhenStopped(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        ScanLogger.getInstance().clearMemoryLogs();

        AuditTarget slowTarget = startTarget("slow-target",
                html("Slow Page", "slow page"),
                Collections.<String, String>emptyMap());
        slowTarget.dynamicResponders.put("GET /", new AuditResponder() {
            @Override
            public AuditResponse respond(HttpExchange exchange) throws IOException {
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return htmlResponse(html("Slow Page", "slow page"));
            }
        });

        ScanConfig config = createDeepConfig(false);
        config.setThreads(1);
        config.setTimeout(10);

        VulnScanService service = VulnScanService.getInstance();
        PocObj.Poc poc = loadAuditPocs().get(0);
        service.getPocRepository().addPoc(poc);
        service.applyScanConfig(config);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<ScanCompletedEvent> completedRef = new AtomicReference<ScanCompletedEvent>();
        service.addEventListener(new ScanEventListener() {
            @Override
            public void onScanStarted(ScanStartedEvent event) {
                started.countDown();
            }

            @Override
            public void onScanCompleted(ScanCompletedEvent event) {
                completedRef.set(event);
                completed.countDown();
            }
        });

        try {
            service.startScan(Collections.singletonList(slowTarget.baseUrl),
                    Collections.singletonList(poc));
            assertTrue(started.await(5, TimeUnit.SECONDS), "扫描未启动");
            service.stopScan();
            assertTrue(completed.await(5, TimeUnit.SECONDS), "停止扫描应派发完成事件");
            assertNotNull(completedRef.get(), "停止扫描完成事件不能为空");
            assertFalse(completedRef.get().isSuccess(), "停止扫描完成事件应标记为失败/未完整完成");
        } finally {
            service.shutdown();
        }
    }

    private ScanConfig createDeepConfig(boolean fullAudit) {
        ScanConfig config = new ScanConfig();
        config.setThreads(4);
        config.setTimeout(3);
        config.setRetries(0);
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setEnableHeadless(fullAudit);
        config.setEnableCode(fullAudit);
        config.setEnableFuzz(fullAudit);
        config.setOobInteractionWaitSeconds(0);
        config.setRestrictOutboundRequestsToTargetHost(true);
        config.setSkipFingerprint(false);
        config.setFingerprintTimeout(2);
        config.setDebug(true);
        config.setScanMode(ScanConfig.ScanMode.DEEP);
        config.getEnabledCategories().clear();
        config.getExcludedCategories().clear();
        if (!fullAudit) {
            config.getEnabledCategories().add(PocObj.PocCategory.TECHNOLOGIES);
            config.getEnabledCategories().add(PocObj.PocCategory.EXPOSED_PANELS);
            config.getEnabledCategories().add(PocObj.PocCategory.MISCONFIGURATION);
            config.getEnabledCategories().add(PocObj.PocCategory.DEFAULT_LOGINS);
        }
        return config;
    }

    private List<PocObj.Poc> loadAuditPocs() throws Exception {
        PocLoader loader = new PocLoader();
        loader.setVerbose(false);
        loader.setSkipInvalidPocs(true);
        List<PocObj.Poc> pocs = loader.loadFromDirectory(ALL_POC_ROOT);
        assertFalse(pocs.isEmpty(), "全量 POC 加载结果不应为空: " + ALL_POC_ROOT);
        return pocs;
    }

    private AuditTarget startTarget(String name, String rootBody, Map<String, String> rootHeaders) throws IOException {
        final AuditTarget target = new AuditTarget();
        target.name = name;
        target.rootBody = rootBody;
        target.rootHeaders.putAll(rootHeaders);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> handleRequest(target, exchange));
        server.start();
        target.baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        servers.add(server);
        return target;
    }

    private void handleRequest(AuditTarget target, HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String routeKey = exchange.getRequestMethod() + " " + path;
        target.requestCounters.merge(routeKey, 1, Integer::sum);

        AuditResponse response = null;
        AuditResponder responder = target.dynamicResponders.get(routeKey);
        if (responder != null) {
            response = responder.respond(exchange);
        }
        if (response == null && target.methodPathResponses.containsKey(routeKey)) {
            response = target.methodPathResponses.get(routeKey);
        }
        if (response == null) {
            String body = target.pathBodies.containsKey(path) ? target.pathBodies.get(path) : target.rootBody;
            response = htmlResponse(body);
        }

        for (Map.Entry<String, String> header : target.rootHeaders.entrySet()) {
            exchange.getResponseHeaders().set(header.getKey(), header.getValue());
        }
        for (Map.Entry<String, String> header : response.headers.entrySet()) {
            exchange.getResponseHeaders().set(header.getKey(), header.getValue());
        }
        exchange.getResponseHeaders().set("Content-Type", response.contentType);
        byte[] bytes = response.body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(response.status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } finally {
            exchange.close();
        }
    }

    private void configureIsolatedHome(Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());
        DnsLogService.setMockMode(true);
        DnsLogService.clearCache();

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path dbPath = tempHome.resolve("db").resolve("deep-scan-audit.sqlite");
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
    }

    private boolean hasSelectionInfoWithoutScanId(List<String> eventRows) {
        for (String row : eventRows) {
            if (row.startsWith("SCAN_INFO\t\t")) {
                return true;
            }
        }
        return false;
    }

    private boolean logFileWasNotCreated(Path tempHome) throws IOException {
        Path logDir = tempHome.resolve(".PotatoTool").resolve("resources").resolve("vulnscan").resolve("logs");
        if (!Files.exists(logDir)) {
            return true;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(logDir)) {
            return !stream.findAny().isPresent();
        }
    }

    private boolean crossTargetRequestsObserved(AuditTarget safeBaseline,
                                                AuditTarget grafanaAuth,
                                                AuditTarget h2Console) {
        return safeBaseline.requestCounters.containsKey("GET /login")
                || safeBaseline.requestCounters.containsKey("GET /h2-console/login.jsp")
                || safeBaseline.requestCounters.containsKey("GET /whoAmI/")
                || safeBaseline.requestCounters.containsKey("GET /graph/login")
                || safeBaseline.requestCounters.containsKey("POST /login")
                || grafanaAuth.requestCounters.containsKey("GET /h2-console/login.jsp")
                || grafanaAuth.requestCounters.containsKey("GET /whoAmI/")
                || grafanaAuth.requestCounters.containsKey("GET /graph/login")
                || grafanaAuth.requestCounters.containsKey("POST /login")
                || h2Console.requestCounters.containsKey("GET /login")
                || h2Console.requestCounters.containsKey("GET /whoAmI/")
                || h2Console.requestCounters.containsKey("GET /graph/login")
                || h2Console.requestCounters.containsKey("POST /login");
    }

    private List<String> buildSelectedPocRows(List<PocObj.Poc> pocs) {
        List<String> rows = new ArrayList<String>();
        rows.add("poc_id\tname\tcategory\tprotocol\trequires_headless\trequires_code\trequires_fuzz");
        for (PocObj.Poc poc : pocs) {
            rows.add(sanitize(poc.getId()) + "\t"
                    + sanitize(poc.getName()) + "\t"
                    + (poc.getCategory() == null ? "" : poc.getCategory().name()) + "\t"
                    + sanitize(poc.getProtocol()) + "\t"
                    + poc.isRequiresHeadless() + "\t"
                    + poc.isRequiresCode() + "\t"
                    + poc.isRequiresFuzz());
        }
        return rows;
    }

    private List<String> buildFingerprintRows(MultiTargetSelectionResult selection, List<String> targets) {
        List<String> rows = new ArrayList<String>();
        rows.add("target\thas_fingerprint\ttag_count\ttags");
        for (String target : targets) {
            FingerprintService.FingerprintResult result = selection.getFingerprintMap().get(target);
            if (result == null) {
                rows.add(sanitize(target) + "\tfalse\t0\t");
                continue;
            }
            Set<String> tags = new LinkedHashSet<String>(result.getNormalizedTags());
            rows.add(sanitize(target) + "\t" + result.hasFingerprint() + "\t" + tags.size() + "\t"
                    + sanitize(String.join(",", tags)));
        }
        return rows;
    }

    private List<String> buildRequestRows(List<AuditTarget> targets) {
        List<String> rows = new ArrayList<String>();
        rows.add("target\troute\thits");
        for (AuditTarget target : targets) {
            for (Map.Entry<String, Integer> entry : target.requestCounters.entrySet()) {
                rows.add(target.name + "\t" + entry.getKey() + "\t" + entry.getValue());
            }
        }
        return rows;
    }

    private void resetRequestCounters(AuditTarget... targets) {
        for (AuditTarget target : targets) {
            if (target != null) {
                target.requestCounters.clear();
            }
        }
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

    private List<String> buildSummaryRows(MultiTargetSelectionResult selection,
                                          ScanCompletedEvent completedEvent,
                                          ScanErrorEvent topLevelError,
                                          List<AuditTarget> targets,
                                          List<String> eventRows,
                                          Path tempHome) throws IOException {
        List<String> rows = new ArrayList<String>();
        rows.add("key\tvalue");
        rows.add("selected_poc_count\t" + selection.getPocs().size());
        rows.add("scan_success\t" + completedEvent.isSuccess());
        rows.add("scan_results\t" + completedEvent.getResults().size());
        rows.add("vulnerability_count\t" + completedEvent.getVulnerabilityCount());
        rows.add("duration_ms\t" + completedEvent.getDuration());
        rows.add("top_level_error\t" + sanitize(topLevelError == null ? "" : topLevelError.getErrorMessage()));
        rows.add("selection_info_missing_scan_id\t" + hasSelectionInfoWithoutScanId(eventRows));
        rows.add("log_file_created\t" + (!logFileWasNotCreated(tempHome)));
        rows.add("cross_target_requests_observed\t"
                + crossTargetRequestsObserved(targets.get(0), targets.get(1), targets.get(2)));
        return rows;
    }

    private List<String> withHeader(String header, List<String> rows) {
        List<String> result = new ArrayList<String>();
        result.add(header);
        result.addAll(rows);
        return result;
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        StringBuilder builder = new StringBuilder();
        while ((read = exchange.getRequestBody().read(buffer)) != -1) {
            builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private static AuditResponse htmlResponse(String body) {
        AuditResponse response = new AuditResponse();
        response.contentType = "text/html; charset=utf-8";
        response.body = body;
        return response;
    }

    private static AuditResponse jsonResponse(int status, String body) {
        AuditResponse response = new AuditResponse();
        response.status = status;
        response.contentType = "application/json; charset=utf-8";
        response.body = body;
        return response;
    }

    private static String html(String title, String body) {
        return "<html><head><title>" + title + "</title></head><body>" + body + "</body></html>";
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

    private interface AuditResponder {
        AuditResponse respond(HttpExchange exchange) throws IOException;
    }

    private static class AuditTarget {
        private String name;
        private String baseUrl;
        private String rootBody;
        private final Map<String, String> rootHeaders = new LinkedHashMap<String, String>();
        private final Map<String, String> pathBodies = new LinkedHashMap<String, String>();
        private final Map<String, AuditResponse> methodPathResponses = new LinkedHashMap<String, AuditResponse>();
        private final Map<String, AuditResponder> dynamicResponders = new LinkedHashMap<String, AuditResponder>();
        private final Map<String, Integer> requestCounters = new ConcurrentHashMap<String, Integer>();
    }

    private static class AuditResponse {
        private int status = 200;
        private String contentType = "text/html; charset=utf-8";
        private String body = "";
        private final Map<String, String> headers = new LinkedHashMap<String, String>();
    }
}

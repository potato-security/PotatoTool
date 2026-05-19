package com.potato.potatotool.content.redTeam.vulnScanner.config;

import com.potato.potatotool.MainApplication;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.report.ReportGenerator;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.network.HeaderManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("楼栋/资产组轻量基线执行测试")
class VulnScanBuildingProfileBaselineExecutionTest {

    private static final String RUN_DATE = new SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(new Date());

    private String originalUserHome;
    private HttpServer httpServer;
    private ServerSocket proxyServer;
    private Thread proxyThread;

    @AfterEach
    void tearDown() throws Exception {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        if (proxyServer != null && !proxyServer.isClosed()) {
            proxyServer.close();
        }
        if (proxyThread != null) {
            proxyThread.interrupt();
            proxyThread.join(1000);
        }
        proxyServer = null;
        proxyThread = null;
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        resetRuntimeState();
    }

    @Test
    @DisplayName("A/B/C 楼应完成单目标低风险轻量基线并按楼栋分别归档")
    void shouldExecuteLightweightBaselineForEachBuildingAndArchiveReports(@TempDir Path tempHome) throws Exception {
        File artifactRoot = new File("target/vulnscan-building-artifacts");
        assertTrue(artifactRoot.exists() || artifactRoot.mkdirs());

        List<String> summaryLines = new ArrayList<String>();
        summaryLines.add("building\tprofile\tvulnerable\tproxy_hits\ttarget_hits\treport_file");

        BaselineEvidence buildingA = executeBuildingABaseline(tempHome.resolve("building-a-home"), artifactRoot);
        summaryLines.add(buildingA.toSummaryLine());

        BaselineEvidence buildingB = executeBuildingBBaseline(tempHome.resolve("building-b-home"), artifactRoot);
        summaryLines.add(buildingB.toSummaryLine());

        BaselineEvidence buildingC = executeBuildingCBaseline(tempHome.resolve("building-c-home"), artifactRoot);
        summaryLines.add(buildingC.toSummaryLine());

        Files.write(new File(artifactRoot, "building-baseline-summary.tsv").toPath(),
                joinLines(summaryLines).getBytes(StandardCharsets.UTF_8));
    }

    private BaselineEvidence executeBuildingABaseline(Path tempHome, File artifactRoot) throws Exception {
        stopHttpServer();
        AtomicInteger targetHits = new AtomicInteger();
        final Map<String, String> observedHeaders = new LinkedHashMap<String, String>();
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/building-a", exchange -> {
            targetHits.incrementAndGet();
            observedHeaders.put("X-Building-Profile", exchange.getRequestHeaders().getFirst("X-Building-Profile"));
            observedHeaders.put("X-Scan-Window", exchange.getRequestHeaders().getFirst("X-Scan-Window"));
            write(exchange, 200, "building-a-ok");
        });
        httpServer.start();

        File buildingDir = new File(artifactRoot, "building-a");
        assertTrue(buildingDir.exists() || buildingDir.mkdirs());
        configureProfile(tempHome, profileAJson(tempHome, buildingDir));

        ScanResult result = new PocExecutor(new ScanConfig()).execute(
                "http://127.0.0.1:" + httpServer.getAddress().getPort(),
                buildHttpPoc("building-a-baseline", "/building-a", "building-a-ok"));

        assertTrue(result.isVulnerable());
        assertEquals(1, targetHits.get());
        assertEquals("A-Building", observedHeaders.get("X-Building-Profile"));
        assertEquals("light-baseline", observedHeaders.get("X-Scan-Window"));

        File reportFile = exportBuildingReport(buildingDir, "building-a", "low-risk", "light-baseline", result);
        return new BaselineEvidence("building-a", "light-baseline", result.isVulnerable(), 0, targetHits.get(), reportFile);
    }

    private BaselineEvidence executeBuildingBBaseline(Path tempHome, File artifactRoot) throws Exception {
        stopHttpServer();
        stopProxyServer();

        AtomicInteger targetHits = new AtomicInteger();
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/building-b", exchange -> {
            targetHits.incrementAndGet();
            write(exchange, 200, "target-should-not-be-hit");
        });
        httpServer.start();

        final AtomicInteger proxyHits = new AtomicInteger();
        final Map<String, String> observedHeaders = new LinkedHashMap<String, String>();
        proxyServer = new ServerSocket(0);
        proxyThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Socket socket = proxyServer.accept()) {
                    proxyHits.incrementAndGet();
                    handleProxyRequest(socket, observedHeaders, "building-b-ok");
                } catch (IOException e) {
                    if (proxyServer == null || proxyServer.isClosed()) {
                        return;
                    }
                }
            }
        }, "building-b-baseline-proxy");
        proxyThread.setDaemon(true);
        proxyThread.start();

        File buildingDir = new File(artifactRoot, "building-b");
        assertTrue(buildingDir.exists() || buildingDir.mkdirs());
        configureProfile(tempHome, profileBJson(tempHome, buildingDir, proxyServer.getLocalPort()));

        ScanResult result = new PocExecutor(new ScanConfig()).execute(
                "http://127.0.0.1:" + httpServer.getAddress().getPort(),
                buildHttpPoc("building-b-baseline", "/building-b", "building-b-ok"));

        assertTrue(result.isVulnerable());
        assertEquals(1, proxyHits.get());
        assertEquals(0, targetHits.get());
        assertEquals("Bearer building-b-token", observedHeaders.get("Authorization"));
        assertEquals("session=b-profile; tenant=ops", observedHeaders.get("Cookie"));
        assertEquals("B-Building", observedHeaders.get("X-Building-Profile"));

        File reportFile = exportBuildingReport(buildingDir, "building-b", "low-risk", "proxy-regression", result);
        return new BaselineEvidence("building-b", "proxy-regression", result.isVulnerable(), proxyHits.get(), targetHits.get(), reportFile);
    }

    private BaselineEvidence executeBuildingCBaseline(Path tempHome, File artifactRoot) throws Exception {
        stopHttpServer();
        AtomicInteger targetHits = new AtomicInteger();
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/building-c", exchange -> {
            targetHits.incrementAndGet();
            observedHeader(exchange, "X-Building-Profile");
            write(exchange, 200, "building-c-ok");
        });
        httpServer.start();

        File buildingDir = new File(artifactRoot, "building-c");
        assertTrue(buildingDir.exists() || buildingDir.mkdirs());
        configureProfile(tempHome, profileCJson(tempHome, buildingDir));
        invokeApplyOobSettings();

        ScanResult result = new PocExecutor(new ScanConfig()).execute(
                "http://127.0.0.1:" + httpServer.getAddress().getPort(),
                buildHttpPoc("building-c-baseline", "/building-c", "building-c-ok"));

        assertTrue(result.isVulnerable());
        assertEquals(1, targetHits.get());
        assertFalse(VulnScanConfig.getInstance().isProxyEnabled());
        assertEquals(HttpLogService.Platform.CUSTOM, HttpLogService.getCurrentPlatform());
        assertEquals(DnsLogService.Platform.CEYE_IO, DnsLogService.getCurrentPlatform());

        File reportFile = exportBuildingReport(buildingDir, "building-c", "low-risk", "oob-regression", result);
        return new BaselineEvidence("building-c", "oob-regression", result.isVulnerable(), 0, targetHits.get(), reportFile);
    }

    private File exportBuildingReport(File buildingDir, String buildingId, String pocScope,
                                      String profileName, ScanResult result) throws Exception {
        Map<String, Object> outputData = new LinkedHashMap<String, Object>();
        outputData.put("buildingProfile", buildingId);
        outputData.put("profileName", profileName);
        outputData.put("pocScope", pocScope);
        result.setOutputData(outputData);

        String fileName = RUN_DATE + "-" + buildingId + "-" + pocScope + "-" + profileName + ".txt";
        File reportFile = new File(buildingDir, fileName);
        new ReportGenerator().generateReport("TXT", Collections.singletonList(result), reportFile.getAbsolutePath(), "00:00:01");
        assertTrue(reportFile.isFile());
        return reportFile;
    }

    private PocObj.Poc buildHttpPoc(String id, String path, String expectedBody) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol("http");
        poc.setOriginalFormat("nuclei");
        poc.setSeverity(PocObj.Severity.LOW);

        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId("http_1");
        step.setMethod("GET");
        step.setPath(path);
        step.setTimeout(2);

        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList(expectedBody));
        step.setMatchers(Collections.singletonList(matcher));

        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private void configureProfile(Path tempHome, String json) throws Exception {
        if (originalUserHome == null) {
            originalUserHome = System.getProperty("user.home");
        }
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Files.write(configDir.resolve("config.json"), json.getBytes(StandardCharsets.UTF_8));
        resetRuntimeState();
    }

    private void resetRuntimeState() throws Exception {
        Constants.cachedConfig = null;
        resetSingleton(HeaderManager.class, "instance");
        resetSingleton(VulnScanConfig.class, "instance");
        HttpLogService.clearCache();
        HttpLogService.closeClient();
        HttpLogService.configureInteractsh("oast.pro", null);
        HttpLogService.setPlatform(HttpLogService.Platform.INTERACTSH);
        DnsLogService.clearCache();
        DnsLogService.setMockMode(false);
        DnsLogService.configureCeye(null, null);
        DnsLogService.setPlatform(DnsLogService.Platform.DNSLOG_CN);
    }

    private void invokeApplyOobSettings() throws Exception {
        Method method = MainApplication.class.getDeclaredMethod("applyOobSettings");
        method.setAccessible(true);
        method.invoke(new MainApplication());
    }

    private void resetSingleton(Class<?> type, String fieldName) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, null);
    }

    private void handleProxyRequest(Socket socket, Map<String, String> observedHeaders, String bodyText) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
        String line = reader.readLine();
        if (line == null) {
            return;
        }

        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex > 0) {
                observedHeaders.put(line.substring(0, separatorIndex), line.substring(separatorIndex + 1).trim());
            }
        }

        byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
        OutputStream output = socket.getOutputStream();
        output.write(("HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n").getBytes(StandardCharsets.ISO_8859_1));
        output.write(body);
        output.flush();
    }

    private void write(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String profileAJson(Path tempHome, File buildingDir) {
        return "{\n" +
                "  \"" + ConfigConstants.PROXY + "\": {\n" +
                "    \"" + ConfigConstants.PROXY_ENABLE + "\": false,\n" +
                "    \"" + ConfigConstants.PROXY_ADDRESS + "\": \"\",\n" +
                "    \"" + ConfigConstants.PROXY_SERVICES + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_SERVICE + "\": false\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.HTTP_HEADERS + "\": {\n" +
                "    \"" + ConfigConstants.HTTP_HEADERS_GLOBAL + "\": {\n" +
                "      \"X-Building-Profile\": \"A-Building\",\n" +
                "      \"X-Scan-Window\": \"light-baseline\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.VULNSCAN + "\": {\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT_DIR + "\": \"" + escape(buildingDir.getAbsolutePath()) + "\",\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR + "\": \"" + escape(buildingDir.getAbsolutePath()) + "\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.OOB + "\": {\n" +
                "    \"" + ConfigConstants.OOB_HTTP + "\": {\n" +
                "      \"" + ConfigConstants.OOB_HTTP_PLATFORM + "\": \"INTERACTSH\",\n" +
                "      \"" + ConfigConstants.OOB_HTTP_INTERACTSH_SERVER + "\": \"oast.pro\"\n" +
                "    },\n" +
                "    \"" + ConfigConstants.OOB_DNS + "\": {\n" +
                "      \"" + ConfigConstants.OOB_DNS_PLATFORM + "\": \"DNSLOG_CN\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private String profileBJson(Path tempHome, File buildingDir, int proxyPort) {
        return "{\n" +
                "  \"" + ConfigConstants.PROXY + "\": {\n" +
                "    \"" + ConfigConstants.PROXY_ENABLE + "\": true,\n" +
                "    \"" + ConfigConstants.PROXY_ADDRESS + "\": \"http://127.0.0.1:" + proxyPort + "\",\n" +
                "    \"" + ConfigConstants.PROXY_SERVICES + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_SERVICE + "\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.HTTP_HEADERS + "\": {\n" +
                "    \"" + ConfigConstants.HTTP_HEADERS_GLOBAL + "\": {\n" +
                "      \"Authorization\": \"Bearer building-b-token\",\n" +
                "      \"Cookie\": \"session=b-profile; tenant=ops\",\n" +
                "      \"X-Building-Profile\": \"B-Building\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.VULNSCAN + "\": {\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT_DIR + "\": \"" + escape(buildingDir.getAbsolutePath()) + "\",\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR + "\": \"" + escape(buildingDir.getAbsolutePath()) + "\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private String profileCJson(Path tempHome, File buildingDir) {
        return "{\n" +
                "  \"" + ConfigConstants.PROXY + "\": {\n" +
                "    \"" + ConfigConstants.PROXY_ENABLE + "\": false,\n" +
                "    \"" + ConfigConstants.PROXY_ADDRESS + "\": \"\",\n" +
                "    \"" + ConfigConstants.PROXY_SERVICES + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_SERVICE + "\": false\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.VULNSCAN + "\": {\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT_DIR + "\": \"" + escape(buildingDir.getAbsolutePath()) + "\",\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR + "\": \"" + escape(buildingDir.getAbsolutePath()) + "\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.OOB + "\": {\n" +
                "    \"" + ConfigConstants.OOB_HTTP + "\": {\n" +
                "      \"" + ConfigConstants.OOB_HTTP_PLATFORM + "\": \"CUSTOM\",\n" +
                "      \"" + ConfigConstants.OOB_HTTP_CUSTOM_SERVER + "\": \"https://oob.local.test\",\n" +
                "      \"" + ConfigConstants.OOB_HTTP_CUSTOM_TOKEN + "\": \"local-oob-token\"\n" +
                "    },\n" +
                "    \"" + ConfigConstants.OOB_DNS + "\": {\n" +
                "      \"" + ConfigConstants.OOB_DNS_PLATFORM + "\": \"CEYE_IO\",\n" +
                "      \"" + ConfigConstants.OOB_DNS_CEYE_IDENTIFIER + "\": \"building-c\",\n" +
                "      \"" + ConfigConstants.OOB_DNS_CEYE_TOKEN + "\": \"ceye-token\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    private void stopProxyServer() throws Exception {
        if (proxyServer != null && !proxyServer.isClosed()) {
            proxyServer.close();
        }
        if (proxyThread != null) {
            proxyThread.interrupt();
            proxyThread.join(1000);
        }
        proxyServer = null;
        proxyThread = null;
    }

    private void observedHeader(HttpExchange exchange, String headerName) {
        exchange.getRequestHeaders().getFirst(headerName);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\");
    }

    private String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append('\n');
        }
        return builder.toString();
    }

    private static final class BaselineEvidence {
        private final String building;
        private final String profile;
        private final boolean vulnerable;
        private final int proxyHits;
        private final int targetHits;
        private final File reportFile;

        private BaselineEvidence(String building, String profile, boolean vulnerable,
                                 int proxyHits, int targetHits, File reportFile) {
            this.building = building;
            this.profile = profile;
            this.vulnerable = vulnerable;
            this.proxyHits = proxyHits;
            this.targetHits = targetHits;
            this.reportFile = reportFile;
        }

        private String toSummaryLine() {
            return building + "\t" + profile + "\t" + vulnerable + "\t" + proxyHits + "\t" +
                    targetHits + "\t" + reportFile.getAbsolutePath();
        }
    }
}

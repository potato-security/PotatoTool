package com.potato.potatotool.content.redTeam.vulnScanner.docker;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import com.potato.potatotool.content.redTeam.vulnScanner.report.ReportGenerator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WrongSecrets 官方在线授权环境检测测试")
class WrongSecretsOnlineDetectionIntegrationTest {

    private static final String ORIGIN = "https://www.wrongsecrets.com";

    private HttpServer safeServer;

    @AfterEach
    void tearDown() {
        if (safeServer != null) {
            safeServer.stop(0);
            safeServer = null;
        }
    }

    @Test
    @DisplayName("应在 WrongSecrets 官方 demo 命中首页与错误页特征，并对本地反样本不误报")
    void shouldDetectWrongSecretsOfficialDemoAndAvoidFalsePositive() throws Exception {
        Assumptions.assumeTrue(waitForReady(ORIGIN + "/", 20), "WrongSecrets 官方 demo 当前不可达，跳过线上授权环境验证");

        HttpResult home = get(ORIGIN + "/");
        HttpResult errorDemo = get(ORIGIN + "/error-demo/database-connection");
        assertEquals(200, home.code);
        assertEquals(200, errorDemo.code);
        assertTrue(home.body.contains("<title>OWASP WrongSecrets</title>"));
        assertTrue(home.body.contains("test your tool of choice detects all the secrets available in this project"));
        assertTrue(errorDemo.body.contains("Database connection failed with connection string:"));
        assertTrue(errorDemo.body.contains("SuperSecretDB2024!"));

        int safePort = findFreePort();
        safeServer = startSafeServer(safePort);
        String safeOrigin = "http://127.0.0.1:" + safePort;

        ScanResult positiveHome = execute(buildHomeFingerprintPoc(), ORIGIN);
        ScanResult positiveErrorDemo = execute(buildErrorDemoPoc(), ORIGIN);
        ScanResult negativeHome = execute(buildHomeFingerprintPoc(), safeOrigin);
        ScanResult negativeErrorDemo = execute(buildErrorDemoPoc(), safeOrigin);

        assertTrue(positiveHome.isVulnerable(), "WrongSecrets 首页指纹应命中");
        assertTrue(positiveErrorDemo.isVulnerable(), "WrongSecrets 错误页特征应命中");
        assertFalse(negativeHome.isVulnerable(), "本地安全页不应命中 WrongSecrets 首页指纹");
        assertFalse(negativeErrorDemo.isVulnerable(), "本地安全页不应命中 WrongSecrets 错误页特征");
        assertEquals(200, firstRecord(positiveHome).getResponseCode());
        assertEquals(200, firstRecord(positiveErrorDemo).getResponseCode());

        Path evidenceDir = Paths.get("target", "wrongsecrets-online-evidence");
        Files.createDirectories(evidenceDir);

        positiveHome.populateFromPoc();
        positiveErrorDemo.populateFromPoc();
        negativeHome.populateFromPoc();
        negativeErrorDemo.populateFromPoc();
        new ReportGenerator().generateAllFormats(
                Arrays.asList(positiveHome, positiveErrorDemo, negativeHome, negativeErrorDemo),
                evidenceDir.toString());

        Files.write(evidenceDir.resolve("home-response.html"), home.body.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("home-headers.txt"), home.headers.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("error-demo-response.txt"), errorDemo.body.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("error-demo-headers.txt"), errorDemo.headers.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("positive-home-request.txt"), safeBytes(positiveHome.getRawRequest()));
        Files.write(evidenceDir.resolve("positive-home-response.txt"), safeBytes(positiveHome.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("positive-error-demo-request.txt"), safeBytes(positiveErrorDemo.getRawRequest()));
        Files.write(evidenceDir.resolve("positive-error-demo-response.txt"), safeBytes(positiveErrorDemo.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("negative-home-response.txt"), safeBytes(negativeHome.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("negative-error-demo-response.txt"), safeBytes(negativeErrorDemo.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("summary.tsv"),
                buildSummary(positiveHome, positiveErrorDemo, negativeHome, negativeErrorDemo)
                        .getBytes(StandardCharsets.UTF_8));

        assertTrue(evidenceDir.resolve("summary.tsv").toFile().exists());
        assertTrue(evidenceDir.resolve("report.json").toFile().exists());
    }

    private PocObj.Poc buildHomeFingerprintPoc() {
        PocObj.Poc poc = basePoc("wrongsecrets-home-fingerprint", "WrongSecrets Home Fingerprint");
        poc.getVerifySteps().add(step("http_1", "/",
                Arrays.asList(
                        statusMatcher(200),
                        wordMatcher("body", "<title>OWASP WrongSecrets</title>"),
                        wordMatcher("body", "test your tool of choice detects all the secrets available in this project")
                )));
        return poc;
    }

    private PocObj.Poc buildErrorDemoPoc() {
        PocObj.Poc poc = basePoc("wrongsecrets-error-demo", "WrongSecrets Error Demo Secret Leak");
        poc.getVerifySteps().add(step("http_1", "/error-demo/database-connection",
                Arrays.asList(
                        statusMatcher(200),
                        wordMatcher("body", "Database connection failed with connection string:"),
                        wordMatcher("body", "SuperSecretDB2024!")
                )));
        return poc;
    }

    private PocObj.Poc basePoc(String id, String name) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(name);
        poc.setProtocol("http");
        poc.setOriginalFormat("custom");
        poc.setSeverity(PocObj.Severity.INFO);
        poc.setVulType("fingerprint");
        poc.setDescription("Public, read-only WrongSecrets online demo verification.");
        poc.setStepsCondition(PocObj.MatchersCondition.AND);
        return poc;
    }

    private PocObj.PocStep step(String stepId, String path, List<PocObj.Matcher> matchers) {
        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId(stepId);
        step.setMethod("GET");
        step.setPath(path);
        step.setMatchers(matchers);
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        return step;
    }

    private PocObj.Matcher statusMatcher(int statusCode) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.STATUS);
        matcher.setValues(Collections.singletonList(String.valueOf(statusCode)));
        return matcher;
    }

    private PocObj.Matcher wordMatcher(String part, String value) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart(part);
        matcher.setValues(Collections.singletonList(value));
        return matcher;
    }

    private ScanResult execute(PocObj.Poc poc, String target) throws Exception {
        ScanConfig config = new ScanConfig();
        config.setThreads(1);
        config.setTimeout(12);
        config.setRetries(0);
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setDebug(false);
        return new PocExecutor(config).execute(target, poc);
    }

    private StepExecutionRecord firstRecord(ScanResult result) {
        assertTrue(result.getStepRecords() != null && !result.getStepRecords().isEmpty(),
                "应记录至少一个步骤执行详情");
        return result.getStepRecords().get(0);
    }

    private HttpServer startSafeServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", exchange -> write(exchange,
                "<html><head><title>Safe Control Page</title></head><body>ok</body></html>"));
        server.createContext("/error-demo/database-connection", exchange -> write(exchange,
                "Database connection ok."));
        server.start();
        return server;
    }

    private void write(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private boolean waitForReady(String url, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (get(url).code == 200) {
                    return true;
                }
            } catch (AssertionError ignored) {
            }
            Thread.sleep(1000L);
        }
        return false;
    }

    private HttpResult get(String url) throws Exception {
        Process process = new ProcessBuilder(
                "curl", "-sS", "-L", "-D", "-", "--max-time", "15", url
        ).redirectErrorStream(true).start();
        boolean finished = process.waitFor(20, TimeUnit.SECONDS);
        String output;
        try (InputStream inputStream = process.getInputStream()) {
            output = readStream(inputStream);
        }
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("curl 请求超时: " + url);
        }
        if (process.exitValue() != 0) {
            throw new AssertionError("curl 请求失败(" + process.exitValue() + "): " + url + "\n" + output);
        }

        int split = output.indexOf("\r\n\r\n");
        int separatorLength = 4;
        if (split < 0) {
            split = output.indexOf("\n\n");
            separatorLength = 2;
        }
        if (split < 0) {
            throw new AssertionError("无法解析 HTTP 响应: " + url + "\n" + output);
        }

        String headers = output.substring(0, split);
        String body = output.substring(split + separatorLength);
        String[] headerLines = headers.split("\\r?\\n");
        String statusLine = headerLines.length == 0 ? "" : headerLines[0];
        String[] parts = statusLine.split(" ");
        int code = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return new HttpResult(code, headers, body);
    }

    private String readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private byte[] safeBytes(String value) {
        return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    }

    private String buildSummary(ScanResult positiveHome,
                                ScanResult positiveErrorDemo,
                                ScanResult negativeHome,
                                ScanResult negativeErrorDemo) {
        return "case\tvulnerable\tstatus\tmarker\n"
                + "home-positive\t" + positiveHome.isVulnerable() + "\t" + firstRecord(positiveHome).getResponseCode() + "\tOWASP WrongSecrets\n"
                + "error-demo-positive\t" + positiveErrorDemo.isVulnerable() + "\t" + firstRecord(positiveErrorDemo).getResponseCode() + "\tSuperSecretDB2024!\n"
                + "home-negative\t" + negativeHome.isVulnerable() + "\t" + firstRecord(negativeHome).getResponseCode() + "\tSafe Control Page\n"
                + "error-demo-negative\t" + negativeErrorDemo.isVulnerable() + "\t" + firstRecord(negativeErrorDemo).getResponseCode() + "\tDatabase connection ok.\n";
    }

    private static class HttpResult {
        private final int code;
        private final String headers;
        private final String body;

        private HttpResult(int code, String headers, String body) {
            this.code = code;
            this.headers = headers;
            this.body = body;
        }
    }
}

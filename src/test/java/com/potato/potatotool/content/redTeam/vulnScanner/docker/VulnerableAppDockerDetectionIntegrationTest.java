package com.potato.potatotool.content.redTeam.vulnScanner.docker;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import com.potato.potatotool.content.redTeam.vulnScanner.report.ReportGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
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

@DisplayName("VulnerableApp Docker 点击劫持检测闭环测试")
class VulnerableAppDockerDetectionIntegrationTest {

    private static final String IMAGE = "sasanlabs/owasp-vulnerableapp:latest";

    private String containerName;
    private Integer appPort;

    @AfterEach
    void tearDown() {
        if (containerName != null) {
            stopContainerQuietly(containerName);
            containerName = null;
        }
    }

    @Test
    @DisplayName("应在 VulnerableApp 点击劫持不安全页面命中，并对安全页面不误报")
    void shouldDetectClickjackingAndExportEvidence() throws Exception {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker 不可用，跳过 VulnerableApp Docker 实测");
        Assumptions.assumeTrue(hasImage(IMAGE), "缺少本地镜像 " + IMAGE + "，跳过 VulnerableApp Docker 实测");

        appPort = findFreePort();
        containerName = "vulnerableapp-it-" + System.currentTimeMillis();

        startContainer(containerName, appPort);
        waitForHttpReady("http://127.0.0.1:" + appPort + "/VulnerableApp/", 120);

        HttpResult vulnerableHttp = get("http://127.0.0.1:" + appPort + "/VulnerableApp/ClickjackingVulnerability/LEVEL_1");
        HttpResult secureHttp = get("http://127.0.0.1:" + appPort + "/VulnerableApp/ClickjackingVulnerability/LEVEL_5");
        assertEquals(200, vulnerableHttp.code);
        assertEquals(200, secureHttp.code);
        assertTrue(vulnerableHttp.body.contains("Page loaded without framing protection"));
        assertTrue(secureHttp.body.contains("Page loaded with framing protection header set."));
        assertFalse(containsIgnoreCase(vulnerableHttp.headers, "X-Frame-Options:"));
        assertFalse(containsIgnoreCase(vulnerableHttp.headers, "Content-Security-Policy:"));
        assertTrue(containsIgnoreCase(secureHttp.headers, "Content-Security-Policy: frame-ancestors 'none'"));

        PocObj.Poc positivePoc = buildClickjackingPoc("/VulnerableApp/ClickjackingVulnerability/LEVEL_1");
        PocObj.Poc negativePoc = buildClickjackingPoc("/VulnerableApp/ClickjackingVulnerability/LEVEL_5");
        ScanResult positive = execute(positivePoc, "http://127.0.0.1:" + appPort);
        ScanResult negative = execute(negativePoc, "http://127.0.0.1:" + appPort);

        assertTrue(positive.isVulnerable(), "LEVEL_1 应被点击劫持检测命中");
        assertFalse(negative.isVulnerable(), "LEVEL_5 不应被点击劫持检测误报");
        assertTrue(positive.getRawRequest().contains("/VulnerableApp/ClickjackingVulnerability/LEVEL_1"));
        assertTrue(positive.getRawResponseSnippet().contains("Page loaded without framing protection"));
        StepExecutionRecord positiveRecord = firstRecord(positive);
        StepExecutionRecord negativeRecord = firstRecord(negative);
        assertEquals(200, positiveRecord.getResponseCode());
        assertEquals(200, negativeRecord.getResponseCode());

        String scannerCatalog = get("http://127.0.0.1:" + appPort + "/VulnerableApp/scanner").body;
        assertTrue(scannerCatalog.contains("ClickjackingVulnerability/LEVEL_1"));
        assertTrue(scannerCatalog.contains("\"variant\":\"SECURE\""));

        String containerLogs = getContainerLogs(containerName);
        stopContainer(containerName);
        waitForPortReleased(appPort, 20);
        containerName = null;

        Path evidenceDir = Paths.get("target", "vulnerableapp-docker-evidence");
        Files.createDirectories(evidenceDir);

        positive.populateFromPoc();
        negative.populateFromPoc();
        new ReportGenerator().generateAllFormats(Arrays.asList(positive, negative), evidenceDir.toString());

        Files.write(evidenceDir.resolve("level1-response-headers.txt"),
                vulnerableHttp.headers.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("level1-response-body.json"),
                vulnerableHttp.body.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("level5-response-headers.txt"),
                secureHttp.headers.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("level5-response-body.json"),
                secureHttp.body.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("positive-request.txt"),
                safeBytes(positive.getRawRequest()));
        Files.write(evidenceDir.resolve("positive-response.txt"),
                safeBytes(positive.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("negative-request.txt"),
                safeBytes(negative.getRawRequest()));
        Files.write(evidenceDir.resolve("negative-response.txt"),
                safeBytes(negative.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("scanner-catalog.json"),
                scannerCatalog.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("vulnerableapp-container.log"),
                containerLogs.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("summary.tsv"),
                buildSummary(positive, negative).getBytes(StandardCharsets.UTF_8));

        assertTrue(evidenceDir.resolve("report.json").toFile().exists());
        assertTrue(evidenceDir.resolve("summary.tsv").toFile().exists());
        assertTrue(evidenceDir.resolve("scanner-catalog.json").toFile().exists());
    }

    private PocObj.Poc buildClickjackingPoc(String path) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("vulnerableapp-clickjacking-" + path.substring(path.lastIndexOf('/') + 1).toLowerCase());
        poc.setName("VulnerableApp Clickjacking Detection " + path);
        poc.setProtocol("http");
        poc.setOriginalFormat("custom");
        poc.setSeverity(PocObj.Severity.LOW);
        poc.setVulType("clickjacking");
        poc.setDescription("Detect missing frame protection headers on VulnerableApp clickjacking exercises.");
        poc.setStepsCondition(PocObj.MatchersCondition.AND);

        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId("http_1");
        step.setMethod("GET");
        step.setPath(path);
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        step.setMatchers(Arrays.asList(
                statusMatcher(200),
                wordMatcher("body", "Page loaded without framing protection"),
                negativeHeaderWordMatcher("X-Frame-Options:"),
                negativeHeaderWordMatcher("Content-Security-Policy:")
        ));
        poc.getVerifySteps().add(step);
        return poc;
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

    private PocObj.Matcher negativeHeaderWordMatcher(String value) {
        PocObj.Matcher matcher = wordMatcher("header", value);
        matcher.setNegative(true);
        return matcher;
    }

    private ScanResult execute(PocObj.Poc poc, String target) throws Exception {
        ScanConfig config = new ScanConfig();
        config.setThreads(1);
        config.setTimeout(10);
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

    private void startContainer(String name, int port) throws Exception {
        CommandResult result = runCommand(Arrays.asList(
                "docker", "run", "-d", "--rm",
                "--name", name,
                "-p", "127.0.0.1:" + port + ":9090",
                IMAGE
        ), 30);
        assertEquals(0, result.exitCode, "VulnerableApp 容器启动失败: " + result.output);
    }

    private void stopContainer(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "stop", name), 30);
        assertEquals(0, result.exitCode, "VulnerableApp 容器停止失败: " + result.output);
    }

    private void stopContainerQuietly(String name) {
        try {
            runCommand(Arrays.asList("docker", "rm", "-f", name), 30);
        } catch (Exception ignored) {
        }
    }

    private String getContainerLogs(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "logs", name), 30);
        assertEquals(0, result.exitCode, "读取 VulnerableApp 容器日志失败: " + result.output);
        return result.output;
    }

    private boolean isDockerAvailable() {
        try {
            CommandResult result = runCommand(Arrays.asList("docker", "version", "--format", "{{.Server.Version}}"), 15);
            return result.exitCode == 0 && hasText(result.output);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasImage(String image) {
        try {
            CommandResult result = runCommand(Arrays.asList("docker", "image", "inspect", image), 15);
            return result.exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void waitForHttpReady(String url, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        IOException lastException = null;
        while (System.currentTimeMillis() < deadline) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);
                connection.setRequestMethod("GET");
                int code = connection.getResponseCode();
                if (code == 200) {
                    String body = readStream(connection.getInputStream());
                    if (body.contains("<title>Vulnerable App</title>")) {
                        return;
                    }
                }
            } catch (IOException e) {
                lastException = e;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            Thread.sleep(1000L);
        }
        throw new AssertionError("VulnerableApp 在超时前未就绪: " + url, lastException);
    }

    private HttpResult get(String targetUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");

        int code = connection.getResponseCode();
        String headers = readHeaders(connection);
        String body = readStream(connection.getInputStream());
        connection.disconnect();
        return new HttpResult(code, headers, body);
    }

    private String readHeaders(HttpURLConnection connection) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        while (true) {
            String key = connection.getHeaderFieldKey(index);
            String value = connection.getHeaderField(index);
            if (key == null && value == null) {
                break;
            }
            if (key == null) {
                builder.append(value).append('\n');
            } else {
                builder.append(key).append(": ").append(value).append('\n');
            }
            index++;
        }
        return builder.toString();
    }

    private void waitForPortReleased(int port, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (isPortFree(port)) {
                return;
            }
            Thread.sleep(500L);
        }
        throw new AssertionError("端口未按预期释放: " + port);
    }

    private boolean isPortFree(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private CommandResult runCommand(List<String> command, int timeoutSeconds) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        String output;
        try (InputStream inputStream = process.getInputStream()) {
            output = readStream(inputStream);
        }
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("命令执行超时: " + command + "\n输出:\n" + output);
        }
        return new CommandResult(process.exitValue(), output.trim());
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && needle != null
                && value.toLowerCase().contains(needle.toLowerCase());
    }

    private byte[] safeBytes(String value) {
        return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    }

    private String buildSummary(ScanResult positive, ScanResult negative) {
        return "case\tvulnerable\tstatus\tmarker\n"
                + "level1\t" + positive.isVulnerable() + "\t" + firstRecord(positive).getResponseCode()
                + "\tno-frame-protection\n"
                + "level5\t" + negative.isVulnerable() + "\t" + firstRecord(negative).getResponseCode()
                + "\tframe-ancestors-none\n";
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

    private static class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}

package com.potato.potatotool.content.redTeam.vulnScanner.docker;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.report.ReportGenerator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Vulhub 轻量环境正反样本闭环测试")
class VulhubLightweightEnvironmentIntegrationTest {

    private HttpServer safeServer;
    private final List<String> containerNames = new ArrayList<String>();

    @AfterEach
    void tearDown() {
        if (safeServer != null) {
            safeServer.stop(0);
            safeServer = null;
        }
        for (String containerName : containerNames) {
            stopContainerQuietly(containerName);
        }
        containerNames.clear();
    }

    @Test
    @DisplayName("应完成 Vulhub 轻量环境子集的低风险正反样本验证")
    void shouldVerifyVulhubLightweightSubsetWithPositiveAndNegativeControls() throws Exception {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker 不可用，跳过 Vulhub 轻量环境实测");

        List<VulhubCase> cases = Arrays.asList(
                new VulhubCase(
                        "h2-console-unacc",
                        "vulhub/spring-with-h2database:1.4.200",
                        8080,
                        "http://127.0.0.1:%d",
                        "src/main/resources/poc/nucleiPoc/http/exposed-panels/h2console-panel.yaml",
                        "compose:h2database/h2-console-unacc/docker-compose.yml",
                        "GET",
                        "/h2-console/login.jsp",
                        200,
                        "<title>H2 Console</title>",
                        Collections.<String>emptyList(),
                        Collections.<String>emptyList(),
                        null
                ),
                new VulhubCase(
                        "influxdb-unacc",
                        "vulhub/influxdb:1.6.6",
                        8086,
                        "http://127.0.0.1:%d",
                        "src/main/resources/poc/nucleiPoc/http/technologies/influxdb-version-detect.yaml",
                        "compose:influxdb/unacc/docker-compose.yml",
                        "GET",
                        "/ping",
                        204,
                        null,
                        Arrays.asList(
                                "INFLUXDB_HTTP_AUTH_ENABLED=true",
                                "INFLUXDB_ADMIN_USER=admin",
                                "INFLUXDB_ADMIN_PASSWORD=admin",
                                "INFLUXDB_DB=sample"
                        ),
                        Collections.<String>emptyList(),
                        "X-Influxdb-Version:"
                ),
                new VulhubCase(
                        "gitea-install-panel",
                        "vulhub/gitea:1.4.0",
                        3000,
                        "http://127.0.0.1:%d",
                        null,
                        "compose:gitea/1.4-rce/docker-compose.yml",
                        "GET",
                        "/install",
                        200,
                        "Installation - Gitea",
                        Collections.<String>emptyList(),
                        Collections.<String>emptyList(),
                        null
                )
        );

        for (VulhubCase testCase : cases) {
            Assumptions.assumeTrue(hasImage(testCase.image), "缺少本地镜像 " + testCase.image + "，跳过 Vulhub 轻量环境实测");
        }

        int safePort = findFreePort();
        safeServer = startSafeServer(safePort);
        PocLoader loader = new PocLoader();
        loader.setVerbose(false);

        List<ScanResult> aggregatedResults = new ArrayList<ScanResult>();
        List<String> summaryRows = new ArrayList<String>();
        summaryRows.add("case\tsource\tpositive_vulnerable\tnegative_vulnerable\tpositive_marker\tnegative_status");

        Path evidenceDir = Paths.get("target", "vulhub-lightweight-evidence");
        Files.createDirectories(evidenceDir);

        for (VulhubCase testCase : cases) {
            String containerName = "vulhub-it-" + testCase.name + "-" + System.currentTimeMillis();
            int hostPort = findFreePort();
            containerNames.add(containerName);

            startContainer(containerName, hostPort, testCase);
            String positiveBaseUrl = String.format(testCase.baseUrlPattern, hostPort);
            String negativeBaseUrl = String.format(testCase.baseUrlPattern, safePort);
            waitForHttpReady(positiveBaseUrl + testCase.readinessPath, testCase.readinessMethod,
                    testCase.readyStatus, testCase.readyMarker, testCase.readyHeaderMarker, 180);

            PocObj.Poc poc = loadPoc(loader, testCase);
            ScanResult positive = execute(poc, positiveBaseUrl);
            ScanResult negative = execute(poc, negativeBaseUrl);

            assertTrue(positive.isVulnerable(), testCase.name + " 正样本应被命中");
            assertFalse(negative.isVulnerable(), testCase.name + " 反样本不应误报");
            assertTrue(hasText(positive.getRawRequest()), testCase.name + " 应记录请求");
            assertTrue(hasText(positive.getRawResponseSnippet()), testCase.name + " 正样本响应应包含可审计响应片段");

            String containerLogs = getContainerLogs(containerName);

            Files.write(evidenceDir.resolve(testCase.name + "-positive-request.txt"),
                    safeBytes(positive.getRawRequest()));
            Files.write(evidenceDir.resolve(testCase.name + "-positive-response.txt"),
                    safeBytes(positive.getRawResponseSnippet()));
            Files.write(evidenceDir.resolve(testCase.name + "-negative-request.txt"),
                    safeBytes(negative.getRawRequest()));
            Files.write(evidenceDir.resolve(testCase.name + "-negative-response.txt"),
                    safeBytes(negative.getRawResponseSnippet()));
            Files.write(evidenceDir.resolve(testCase.name + "-container.log"),
                    containerLogs.getBytes(StandardCharsets.UTF_8));

            positive.populateFromPoc();
            negative.populateFromPoc();
            aggregatedResults.add(positive);
            aggregatedResults.add(negative);
            summaryRows.add(testCase.name + "\t"
                    + testCase.sourceLabel + "\t"
                    + positive.isVulnerable() + "\t"
                    + negative.isVulnerable() + "\t"
                    + sanitizeTab(testCase.readyMarker) + "\t"
                    + sanitizeTab(extractStatusLine(negative.getRawResponseSnippet())));

            stopContainerQuietly(containerName);
            waitForPortReleased(hostPort, 20);
            containerNames.remove(containerName);
        }

        new ReportGenerator().generateAllFormats(aggregatedResults, evidenceDir.toString());
        Files.write(evidenceDir.resolve("summary.tsv"),
                String.join("\n", summaryRows).getBytes(StandardCharsets.UTF_8));

        assertTrue(evidenceDir.resolve("summary.tsv").toFile().exists());
        assertTrue(evidenceDir.resolve("report.json").toFile().exists());
    }

    private PocObj.Poc loadPoc(PocLoader loader, String path) throws Exception {
        PocObj.Poc poc = loader.loadFromFile(path);
        assertNotNull(poc, "POC 加载失败: " + path);
        return poc;
    }

    private PocObj.Poc loadPoc(PocLoader loader, VulhubCase testCase) throws Exception {
        if (hasText(testCase.pocPath)) {
            return loadPoc(loader, testCase.pocPath);
        }
        if ("gitea-install-panel".equals(testCase.name)) {
            return buildGiteaInstallPoc();
        }
        throw new AssertionError("未配置可执行 POC: " + testCase.name);
    }

    private PocObj.Poc buildGiteaInstallPoc() {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("gitea-install-panel");
        poc.setName("Gitea Installation Panel");
        poc.setProtocol("http");
        poc.setOriginalFormat("custom");
        poc.setSeverity(PocObj.Severity.INFO);
        poc.setVulType("fingerprint");
        poc.setDescription("Detect the unauthenticated Gitea installation page exposed by the Vulhub sample.");
        poc.setStepsCondition(PocObj.MatchersCondition.AND);
        poc.getVerifySteps().add(step("http_1", "/install",
                Arrays.asList(
                        statusMatcher(200),
                        wordMatcher("body", "Installation - Gitea"),
                        wordMatcher("body", "Database Settings"),
                        wordMatcher("body", "Admin Account Settings")
                )));
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
        config.setTimeout(10);
        config.setRetries(0);
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setDebug(false);
        return new PocExecutor(config).execute(target, poc);
    }

    private HttpServer startSafeServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", exchange -> writeHtml(exchange,
                "<html><head><title>Safe Control Page</title></head><body>ok</body></html>"));
        server.start();
        return server;
    }

    private void startContainer(String name, int hostPort, VulhubCase testCase) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add("docker");
        command.add("run");
        command.add("-d");
        command.add("--rm");
        command.add("--name");
        command.add(name);
        command.add("-p");
        command.add("127.0.0.1:" + hostPort + ":" + testCase.containerPort);
        for (String env : testCase.envs) {
            command.add("-e");
            command.add(env);
        }
        command.add(testCase.image);
        command.addAll(testCase.commandArgs);

        CommandResult result = runCommand(command, 60);
        assertTrue(result.exitCode == 0, "容器启动失败: " + testCase.name + " -> " + result.output);
    }

    private void stopContainerQuietly(String name) {
        try {
            runCommand(Arrays.asList("docker", "rm", "-f", name), 30);
        } catch (Exception ignored) {
        }
    }

    private String getContainerLogs(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "logs", name), 30);
        assertTrue(result.exitCode == 0, "读取容器日志失败: " + name + " -> " + result.output);
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

    private void waitForHttpReady(String url, String method, int expectedStatus,
                                  String bodyMarker, String headerMarker, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        IOException lastException = null;
        while (System.currentTimeMillis() < deadline) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                connection.setRequestMethod(method);
                int code = connection.getResponseCode();
                String headers = readHeaders(connection);
                String body = "";
                if (!"HEAD".equalsIgnoreCase(method)) {
                    InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
                    if (stream != null) {
                        body = readStream(stream);
                    }
                }
                if (code == expectedStatus
                        && (containsIgnoreCase(body, bodyMarker) || containsIgnoreCase(headers, headerMarker))) {
                    return;
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
        throw new AssertionError("环境未在超时前就绪: " + url, lastException);
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

    private void writeHtml(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
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

    private byte[] safeBytes(String value) {
        return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean containsIgnoreCase(String text, String needle) {
        return text != null && needle != null
                && !needle.trim().isEmpty()
                && text.toLowerCase().contains(needle.toLowerCase());
    }

    private String extractStatusLine(String responseSnippet) {
        if (!hasText(responseSnippet)) {
            return "";
        }
        int lineBreak = responseSnippet.indexOf('\n');
        return lineBreak >= 0 ? responseSnippet.substring(0, lineBreak).trim() : responseSnippet.trim();
    }

    private String sanitizeTab(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').trim();
    }

    private static class VulhubCase {
        private final String name;
        private final String image;
        private final int containerPort;
        private final String baseUrlPattern;
        private final String pocPath;
        private final String sourceLabel;
        private final String readinessMethod;
        private final String readinessPath;
        private final int readyStatus;
        private final String readyMarker;
        private final List<String> envs;
        private final List<String> commandArgs;
        private final String readyHeaderMarker;

        private VulhubCase(String name, String image, int containerPort,
                           String baseUrlPattern, String pocPath, String sourceLabel,
                           String readinessMethod, String readinessPath, int readyStatus,
                           String readyMarker, List<String> envs,
                           List<String> commandArgs, String readyHeaderMarker) {
            this.name = name;
            this.image = image;
            this.containerPort = containerPort;
            this.baseUrlPattern = baseUrlPattern;
            this.pocPath = pocPath;
            this.sourceLabel = sourceLabel;
            this.readinessMethod = readinessMethod;
            this.readinessPath = readinessPath;
            this.readyStatus = readyStatus;
            this.readyMarker = readyMarker;
            this.envs = envs;
            this.commandArgs = commandArgs;
            this.readyHeaderMarker = readyHeaderMarker;
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

package com.potato.potatotool.content.redTeam.vulnScanner.docker;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Grafana Docker 真实登录态对齐测试")
class GrafanaDockerAuthenticationIntegrationTest {

    private static final int REPORT_TEXT_LIMIT = 8000;
    private static final String IMAGE = "grafana/grafana:10.4.1";
    private static final String DETECT_POC_PATH =
            "src/main/resources/poc/nucleiPoc/http/exposed-panels/grafana-detect.yaml";
    private static final String DEFAULT_LOGIN_POC_PATH =
            "src/main/resources/poc/nucleiPoc/http/default-logins/grafana/grafana-default-login.yaml";
    private static final String PUBLIC_SIGNUP_POC_PATH =
            "src/main/resources/poc/nucleiPoc/http/misconfiguration/grafana-public-signup.yaml";

    private HttpServer safeServer;
    private String containerName;
    private Integer grafanaPort;

    @AfterEach
    void tearDown() {
        if (safeServer != null) {
            safeServer.stop(0);
            safeServer = null;
        }
        if (containerName != null) {
            stopContainerQuietly(containerName);
            containerName = null;
        }
    }

    @Test
    @DisplayName("应在官方 Grafana Docker 中命中登录页和默认口令，且不误报公开注册")
    void shouldAlignOfficialGrafanaDockerWithBuiltinAuthPocs() throws Exception {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker 不可用，跳过 Grafana Docker 实测");
        Assumptions.assumeTrue(hasImage(IMAGE), "缺少本地镜像 " + IMAGE + "，跳过 Grafana Docker 实测");

        grafanaPort = findFreePort();
        int safePort = findFreePort();
        safeServer = startSafeServer(safePort);

        containerName = "grafana-it-" + System.currentTimeMillis();
        startGrafanaContainer(containerName, grafanaPort);
        String positiveBaseUrl = "http://127.0.0.1:" + grafanaPort;
        String negativeBaseUrl = "http://127.0.0.1:" + safePort;
        waitForLoginReady(positiveBaseUrl + "/login", 120);

        HttpResult loginPage = get(positiveBaseUrl + "/login");
        assertEquals(200, loginPage.code);
        assertTrue(loginPage.body.contains("<title>Grafana</title>"));
        assertTrue(loginPage.body.contains("Grafana v"));

        HttpResult defaultLoginProbe = postJson(positiveBaseUrl + "/login",
                "{\"user\":\"admin\",\"password\":\"admin\"}");
        assertEquals(200, defaultLoginProbe.code);
        assertTrue(defaultLoginProbe.body.contains("Logged in"));
        assertTrue(defaultLoginProbe.setCookie.contains("grafana_session"));

        HttpResult publicSignupProbe = postJson(positiveBaseUrl + "/api/user/signup/step2",
                "{\"username\":\"ptuser\",\"password\":\"ptpass\"}");
        assertEquals(401, publicSignupProbe.code);
        assertTrue(publicSignupProbe.body.contains("User signup is disabled"));

        PocLoader loader = new PocLoader();
        loader.setVerbose(false);
        PocObj.Poc detectPoc = loadPoc(loader, DETECT_POC_PATH);
        PocObj.Poc defaultLoginPoc = loadPoc(loader, DEFAULT_LOGIN_POC_PATH);
        PocObj.Poc publicSignupPoc = loadPoc(loader, PUBLIC_SIGNUP_POC_PATH);

        ScanResult detectPositive = execute(detectPoc, positiveBaseUrl);
        ScanResult detectNegative = execute(detectPoc, negativeBaseUrl);
        ScanResult defaultLoginPositive = execute(defaultLoginPoc, positiveBaseUrl);
        ScanResult defaultLoginNegative = execute(defaultLoginPoc, negativeBaseUrl);
        ScanResult publicSignupPositive = execute(publicSignupPoc, positiveBaseUrl);
        ScanResult publicSignupNegative = execute(publicSignupPoc, negativeBaseUrl);

        assertTrue(detectPositive.isVulnerable(), "Grafana 登录页指纹应命中");
        assertFalse(detectNegative.isVulnerable(), "安全对照页不应误报 Grafana 登录页");

        assertTrue(defaultLoginPositive.isVulnerable(), "Grafana 默认口令应命中");
        assertFalse(defaultLoginNegative.isVulnerable(), "安全对照页不应误报 Grafana 默认口令");
        assertTrue(hasText(defaultLoginPositive.getRawResponseSnippet()));
        assertTrue(defaultLoginPositive.getRawResponseSnippet().contains("Logged in"));

        assertFalse(publicSignupPositive.isVulnerable(), "Grafana 默认不应命中公开注册");
        assertFalse(publicSignupNegative.isVulnerable(), "安全对照页不应误报 Grafana 公开注册");

        String containerLogs = getContainerLogs(containerName);
        stopContainer(containerName);
        waitForPortReleased(grafanaPort, 20);
        containerName = null;

        Path evidenceDir = Paths.get("target", "grafana-docker-evidence");
        Files.createDirectories(evidenceDir);

        List<ScanResult> aggregatedResults = Arrays.asList(
                detectPositive, detectNegative,
                defaultLoginPositive, defaultLoginNegative,
                publicSignupPositive, publicSignupNegative
        );
        for (ScanResult result : aggregatedResults) {
            truncateForReport(result);
            result.populateFromPoc();
        }
        new ReportGenerator().generateAllFormats(aggregatedResults, evidenceDir.toString());

        Files.write(evidenceDir.resolve("login-page.html"),
                safeBytes(loginPage.body));
        Files.write(evidenceDir.resolve("default-login-probe.txt"),
                safeBytes(formatHttpProbe(defaultLoginProbe)));
        Files.write(evidenceDir.resolve("public-signup-probe.txt"),
                safeBytes(formatHttpProbe(publicSignupProbe)));
        Files.write(evidenceDir.resolve("grafana-container.log"),
                containerLogs.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("detect-positive-request.txt"),
                safeBytes(detectPositive.getRawRequest()));
        Files.write(evidenceDir.resolve("detect-positive-response.txt"),
                safeBytes(detectPositive.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("default-login-positive-request.txt"),
                safeBytes(defaultLoginPositive.getRawRequest()));
        Files.write(evidenceDir.resolve("default-login-positive-response.txt"),
                safeBytes(defaultLoginPositive.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("public-signup-positive-request.txt"),
                safeBytes(publicSignupPositive.getRawRequest()));
        Files.write(evidenceDir.resolve("public-signup-positive-response.txt"),
                safeBytes(publicSignupPositive.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("summary.tsv"),
                buildSummary(detectPositive, detectNegative,
                        defaultLoginPositive, defaultLoginNegative,
                        publicSignupPositive, publicSignupNegative,
                        loginPage, defaultLoginProbe, publicSignupProbe).getBytes(StandardCharsets.UTF_8));

        assertTrue(evidenceDir.resolve("summary.tsv").toFile().exists());
        assertTrue(evidenceDir.resolve("report.json").toFile().exists());
        assertTrue(evidenceDir.resolve("grafana-container.log").toFile().exists());
    }

    private PocObj.Poc loadPoc(PocLoader loader, String path) throws Exception {
        PocObj.Poc poc = loader.loadFromFile(path);
        assertNotNull(poc, "POC 加载失败: " + path);
        return poc;
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

    private void startGrafanaContainer(String name, int port) throws Exception {
        CommandResult result = runCommand(Arrays.asList(
                "docker", "run", "-d", "--rm",
                "--name", name,
                "-p", "127.0.0.1:" + port + ":3000",
                "-e", "GF_SECURITY_ADMIN_USER=admin",
                "-e", "GF_SECURITY_ADMIN_PASSWORD=admin",
                "-e", "GF_USERS_ALLOW_SIGN_UP=false",
                IMAGE
        ), 30);
        assertEquals(0, result.exitCode, "Grafana 容器启动失败: " + result.output);
    }

    private void stopContainer(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "stop", name), 30);
        assertEquals(0, result.exitCode, "Grafana 容器停止失败: " + result.output);
    }

    private void stopContainerQuietly(String name) {
        try {
            runCommand(Arrays.asList("docker", "rm", "-f", name), 30);
        } catch (Exception ignored) {
        }
    }

    private String getContainerLogs(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "logs", name), 30);
        assertEquals(0, result.exitCode, "读取 Grafana 容器日志失败: " + result.output);
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

    private void waitForLoginReady(String url, int timeoutSeconds) throws Exception {
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
                    if (body.contains("<title>Grafana</title>")) {
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
        throw new AssertionError("Grafana 在超时前未就绪: " + url, lastException);
    }

    private HttpResult get(String url) throws Exception {
        return request("GET", url, "text/html", null);
    }

    private HttpResult postJson(String url, String body) throws Exception {
        return request("POST", url, "application/json", body);
    }

    private HttpResult request(String method, String targetUrl, String contentType, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod(method);
        if (hasText(contentType)) {
            connection.setRequestProperty("Content-Type", contentType);
        }
        if (body != null) {
            connection.setDoOutput(true);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
        }

        int code = connection.getResponseCode();
        String location = connection.getHeaderField("Location");
        String setCookie = flattenHeader(connection.getHeaderFields().get("Set-Cookie"));
        String headers = connection.getHeaderFields().toString();
        String responseBody = "";
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream != null) {
            responseBody = readStream(stream);
        }
        connection.disconnect();
        return new HttpResult(code, location, setCookie, responseBody, headers);
    }

    private String flattenHeader(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values);
    }

    private void truncateForReport(ScanResult result) {
        if (result == null) {
            return;
        }
        result.setRawRequest(limitText(result.getRawRequest()));
        result.setRawResponseSnippet(limitText(result.getRawResponseSnippet()));
        if (result.getStepRecords() == null) {
            return;
        }
        for (StepExecutionRecord record : result.getStepRecords()) {
            if (record == null) {
                continue;
            }
            record.setRawRequest(limitText(record.getRawRequest()));
            record.setRawResponse(limitText(record.getRawResponse()));
            record.setRequestBody(limitText(record.getRequestBody()));
            record.setResponseBody(limitText(record.getResponseBody()));
        }
    }

    private String limitText(String text) {
        if (text == null || text.length() <= REPORT_TEXT_LIMIT) {
            return text;
        }
        return text.substring(0, REPORT_TEXT_LIMIT) + "...";
    }

    private void waitForPortReleased(int port, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (isPortFree(port)) {
                return;
            }
            Thread.sleep(500L);
        }
        throw new AssertionError("端口未释放: " + port);
    }

    private boolean isPortFree(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress("127.0.0.1", port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private int findFreePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            serverSocket.setReuseAddress(true);
            return serverSocket.getLocalPort();
        }
    }

    private CommandResult runCommand(List<String> command, int timeoutSeconds) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread pump = new Thread(() -> copy(process.getInputStream(), output),
                "grafana-docker-command-" + System.nanoTime());
        pump.setDaemon(true);
        pump.start();

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            pump.join(1000L);
            throw new AssertionError("命令执行超时: " + String.join(" ", command));
        }
        pump.join(1000L);
        return new CommandResult(process.exitValue(),
                new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    private void copy(InputStream inputStream, OutputStream outputStream) {
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException ignored) {
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        try (InputStream stream = inputStream;
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void writeHtml(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        } finally {
            exchange.close();
        }
    }

    private byte[] safeBytes(String text) {
        return (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String formatHttpProbe(HttpResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("status=").append(result.code).append('\n');
        builder.append("location=").append(valueOrEmpty(result.location)).append('\n');
        builder.append("set_cookie=").append(valueOrEmpty(result.setCookie)).append('\n');
        builder.append("headers=").append(valueOrEmpty(result.headers)).append('\n');
        builder.append("body=").append(valueOrEmpty(result.body)).append('\n');
        return builder.toString();
    }

    private String buildSummary(ScanResult detectPositive,
                                ScanResult detectNegative,
                                ScanResult defaultLoginPositive,
                                ScanResult defaultLoginNegative,
                                ScanResult publicSignupPositive,
                                ScanResult publicSignupNegative,
                                HttpResult loginPage,
                                HttpResult defaultLoginProbe,
                                HttpResult publicSignupProbe) {
        List<String> rows = new ArrayList<String>();
        rows.add("case\tprobe_status\tprobe_marker\tscan_positive\tscan_negative\tmatched_path\tpoc_id");
        rows.add(buildRow("grafana-detect",
                loginPage.code,
                loginPage.body.contains("<title>Grafana</title>") ? "login-title" : "",
                detectPositive,
                detectNegative));
        rows.add(buildRow("grafana-default-login",
                defaultLoginProbe.code,
                defaultLoginProbe.body.contains("Logged in") ? "logged-in" : "",
                defaultLoginPositive,
                defaultLoginNegative));
        rows.add(buildRow("grafana-public-signup",
                publicSignupProbe.code,
                publicSignupProbe.body.contains("User signup is disabled") ? "signup-disabled" : "",
                publicSignupPositive,
                publicSignupNegative));
        return String.join("\n", rows) + "\n";
    }

    private String buildRow(String caseName,
                            int probeStatus,
                            String probeMarker,
                            ScanResult positive,
                            ScanResult negative) {
        return caseName + "\t"
                + probeStatus + "\t"
                + valueOrEmpty(probeMarker) + "\t"
                + positive.isVulnerable() + "\t"
                + negative.isVulnerable() + "\t"
                + valueOrEmpty(positive.getMatchedPath()) + "\t"
                + (positive.getPoc() == null ? "" : valueOrEmpty(positive.getPoc().getId()));
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ');
    }

    private static final class HttpResult {
        private final int code;
        private final String location;
        private final String setCookie;
        private final String body;
        private final String headers;

        private HttpResult(int code, String location, String setCookie, String body, String headers) {
            this.code = code;
            this.location = location;
            this.setCookie = setCookie;
            this.body = body;
            this.headers = headers;
        }
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}

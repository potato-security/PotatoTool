package com.potato.potatotool.content.redTeam.vulnScanner.docker;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WebGoat Docker 登录态闭环测试")
class WebGoatDockerAuthenticationIntegrationTest {

    private static final String IMAGE = "webgoat/webgoat:latest";
    private static final Pattern SESSION_PATTERN = Pattern.compile("JSESSIONID=([^;]+)");

    private String containerName;
    private Integer webGoatPort;
    private Integer webWolfPort;

    @AfterEach
    void tearDown() {
        if (containerName != null) {
            stopContainerQuietly(containerName);
            containerName = null;
        }
    }

    @Test
    @DisplayName("应可在 Docker WebGoat 中注册测试账号并验证未登录/已登录门禁")
    void shouldRegisterUserAndVerifyAuthenticatedAccess() throws Exception {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker 不可用，跳过 WebGoat Docker 实测");
        Assumptions.assumeTrue(hasImage(IMAGE), "缺少本地镜像 " + IMAGE + "，跳过 WebGoat Docker 实测");

        webGoatPort = findFreePort();
        webWolfPort = findFreePort();
        containerName = "webgoat-it-" + System.currentTimeMillis();

        startWebGoatContainer(containerName, webGoatPort, webWolfPort);
        waitForHttpReady("http://127.0.0.1:" + webGoatPort + "/WebGoat/", 120);

        HttpResult landingPage = get("http://127.0.0.1:" + webGoatPort + "/WebGoat/");
        HttpResult loginPage = landingPage.code == 302 && hasText(landingPage.location)
                ? get(landingPage.location)
                : landingPage;
        assertEquals(200, loginPage.code);
        assertTrue(loginPage.body.contains("or register yourself as a new user"));
        assertTrue(loginPage.body.contains("action=\"/WebGoat/login\""));

        HttpResult anonymousAttack = head("http://127.0.0.1:" + webGoatPort + "/WebGoat/attack", null);
        assertEquals(302, anonymousAttack.code);
        assertTrue(anonymousAttack.location.contains("/WebGoat/login"));

        String username = "pt" + System.currentTimeMillis();
        String password = "Potato123!";
        HttpResult registration = registerUser(username, password);
        assertEquals(302, registration.code);
        assertTrue(registration.location.contains("/WebGoat/attack?username=" + username));
        String sessionCookie = extractSessionCookie(registration.setCookie);
        assertTrue(hasText(sessionCookie));

        HttpResult authenticatedAttack = head("http://127.0.0.1:" + webGoatPort + "/WebGoat/attack",
                "JSESSIONID=" + sessionCookie);
        assertEquals(302, authenticatedAttack.code);
        assertTrue(authenticatedAttack.location.contains("/WebGoat/start.mvc?username=" + username));

        HttpResult startPage = getWithCookie(authenticatedAttack.location, "JSESSIONID=" + sessionCookie);
        assertEquals(200, startPage.code);
        assertTrue(startPage.body.contains("User: <span>" + username + "</span>"));
        assertTrue(startPage.body.contains("Logout"));
        assertFalse(startPage.body.contains("or register yourself as a new user"));

        String containerLogs = getContainerLogs(containerName);
        stopContainer(containerName);
        waitForPortReleased(webGoatPort, 20);
        waitForPortReleased(webWolfPort, 20);
        containerName = null;

        Path evidenceDir = Paths.get("target", "webgoat-auth-evidence");
        Files.createDirectories(evidenceDir);
        Files.write(evidenceDir.resolve("register-response-headers.txt"),
                registration.headers.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("login-page.html"),
                loginPage.body.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("authenticated-start-page.html"),
                startPage.body.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("webgoat-container.log"),
                containerLogs.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("summary.tsv"),
                buildSummary(username, anonymousAttack, authenticatedAttack, sessionCookie).getBytes(StandardCharsets.UTF_8));

        assertTrue(evidenceDir.resolve("summary.tsv").toFile().exists());
        assertTrue(evidenceDir.resolve("webgoat-container.log").toFile().exists());
    }

    private HttpResult registerUser(String username, String password) throws Exception {
        String body = "username=" + urlEncode(username)
                + "&password=" + urlEncode(password)
                + "&matchingPassword=" + urlEncode(password)
                + "&agree=agree";
        return postForm("http://127.0.0.1:" + webGoatPort + "/WebGoat/register.mvc", body, null);
    }

    private HttpResult get(String url) throws Exception {
        return request("GET", url, null, null);
    }

    private HttpResult getWithCookie(String url, String cookie) throws Exception {
        return request("GET", url, cookie, null);
    }

    private HttpResult head(String url, String cookie) throws Exception {
        return request("HEAD", url, cookie, null);
    }

    private HttpResult postForm(String url, String body, String cookie) throws Exception {
        return request("POST", url, cookie, body);
    }

    private HttpResult request(String method, String targetUrl, String cookie, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod(method);
        if (hasText(cookie)) {
            connection.setRequestProperty("Cookie", cookie);
        }
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
        }

        int code = connection.getResponseCode();
        String headers = connection.getHeaderFields().toString();
        String location = connection.getHeaderField("Location");
        String setCookie = connection.getHeaderField("Set-Cookie");
        String responseBody = "";
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream != null && !"HEAD".equalsIgnoreCase(method)) {
            responseBody = readStream(stream);
        }
        connection.disconnect();
        return new HttpResult(code, location, setCookie, responseBody, headers);
    }

    private String extractSessionCookie(String setCookie) {
        if (!hasText(setCookie)) {
            return "";
        }
        Matcher matcher = SESSION_PATTERN.matcher(setCookie);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String urlEncode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private void startWebGoatContainer(String name, int goatPort, int wolfPort) throws Exception {
        CommandResult result = runCommand(Arrays.asList(
                "docker", "run", "-d", "--rm",
                "--name", name,
                "-p", "127.0.0.1:" + goatPort + ":8080",
                "-p", "127.0.0.1:" + wolfPort + ":9090",
                "-e", "TZ=Asia/Shanghai",
                IMAGE
        ), 30);
        assertEquals(0, result.exitCode, "WebGoat 容器启动失败: " + result.output);
    }

    private void stopContainer(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "stop", name), 30);
        assertEquals(0, result.exitCode, "WebGoat 容器停止失败: " + result.output);
    }

    private void stopContainerQuietly(String name) {
        try {
            runCommand(Arrays.asList("docker", "rm", "-f", name), 30);
        } catch (Exception ignored) {
        }
    }

    private String getContainerLogs(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "logs", name), 30);
        assertEquals(0, result.exitCode, "读取 WebGoat 容器日志失败: " + result.output);
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
                    if (body.contains("Login Page") && body.contains("/WebGoat/login")) {
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
        throw new AssertionError("WebGoat 在超时前未就绪: " + url, lastException);
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
                "webgoat-docker-command-" + System.nanoTime());
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

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String buildSummary(String username, HttpResult anonymousAttack,
                                HttpResult authenticatedAttack, String sessionCookie) {
        return "case\tstatus\tlocation_or_marker\n"
                + "register\t302\t" + username + "\n"
                + "anonymous_attack\t" + anonymousAttack.code + "\t" + valueOrEmpty(anonymousAttack.location) + "\n"
                + "authenticated_attack\t" + authenticatedAttack.code + "\t" + valueOrEmpty(authenticatedAttack.location) + "\n"
                + "session_cookie\tpresent\t" + sessionCookie + "\n";
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.replace('\t', ' ');
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

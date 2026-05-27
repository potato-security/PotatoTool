package com.potato.potatotool.content.redTeam.vulnScanner.docker;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Gitea Installer 与 Vulhub 真实环境对齐测试")
class GiteaInstallerVulhubAlignmentTest {

    private static final String IMAGE = "vulhub/gitea:1.4.0";
    private static final String BUILTIN_POC_PATH =
            "src/main/resources/poc/nucleiPoc/http/misconfiguration/installer/gitea-installer.yaml";

    private String containerName;
    private Integer giteaPort;

    @AfterEach
    void tearDown() {
        if (containerName != null) {
            stopContainerQuietly(containerName);
            containerName = null;
        }
    }

    @Test
    @DisplayName("更新后的内置 gitea-installer 应命中真实 Vulhub 安装页")
    void shouldAlignBuiltinGiteaInstallerWithRealVulhub() throws Exception {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker 不可用，跳过 Gitea Vulhub 实测");
        Assumptions.assumeTrue(hasImage(IMAGE), "缺少本地镜像 " + IMAGE + "，跳过 Gitea Vulhub 实测");

        giteaPort = findFreePort();
        containerName = "gitea-align-it-" + System.currentTimeMillis();
        startGiteaContainer(containerName, giteaPort);

        String baseUrl = "http://127.0.0.1:" + giteaPort;
        waitForHttpReady(baseUrl + "/install", 180);

        HttpResult root = get(baseUrl + "/");
        HttpResult install = get(baseUrl + "/install");

        assertEquals(302, root.code, "真实根路径应先重定向到 /install");
        assertTrue(root.location.contains("/install"));
        assertEquals(200, install.code, "真实安装页应返回 200");
        assertTrue(install.body.contains("Installation - Gitea: Git with a cup of tea"));
        assertTrue(install.body.contains("Database Name"));

        PocLoader loader = new PocLoader();
        loader.setVerbose(false);
        PocObj.Poc builtin = loadPoc(loader, BUILTIN_POC_PATH);
        ScanResult builtinResult = execute(builtin, baseUrl);

        PocObj.Poc directInstall = loadPoc(loader, BUILTIN_POC_PATH);
        directInstall.getVerifySteps().get(0).setPath("/install");
        ScanResult directInstallResult = execute(directInstall, baseUrl);

        String containerLogs = getContainerLogs(containerName);
        stopContainer(containerName);
        waitForPortReleased(giteaPort, 20);
        containerName = null;

        Path evidenceDir = Paths.get("target", "gitea-vulhub-alignment-evidence");
        Files.createDirectories(evidenceDir);
        Files.write(evidenceDir.resolve("root-response.txt"), safeBytes(formatHttpResult(root)));
        Files.write(evidenceDir.resolve("install-response.txt"), safeBytes(formatHttpResult(install)));
        Files.write(evidenceDir.resolve("builtin-result.txt"), safeBytes(formatScanResult(builtinResult)));
        Files.write(evidenceDir.resolve("direct-install-result.txt"), safeBytes(formatScanResult(directInstallResult)));
        Files.write(evidenceDir.resolve("gitea-container.log"), containerLogs.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("summary.tsv"),
                buildSummary(root, install, builtinResult, directInstallResult)
                        .getBytes(StandardCharsets.UTF_8));

        assertTrue(evidenceDir.resolve("summary.tsv").toFile().exists());
        assertTrue(evidenceDir.resolve("install-response.txt").toFile().exists());
        assertTrue(builtin.getVerifySteps().get(0).isFollowRedirect(), "更新后的内置 gitea-installer 应开启 followRedirect");
        assertTrue(directInstallResult.isVulnerable(), "直接访问 /install 也应命中真实 Vulhub 安装页");
        assertTrue(builtinResult.isVulnerable(), "更新后的内置 gitea-installer 应命中真实 Vulhub 安装页");
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

    private void startGiteaContainer(String name, int port) throws Exception {
        CommandResult result = runCommand(Arrays.asList(
                "docker", "run", "-d", "--rm",
                "--name", name,
                "-p", "127.0.0.1:" + port + ":3000",
                IMAGE
        ), 30);
        assertEquals(0, result.exitCode, "Gitea 容器启动失败: " + result.output);
    }

    private void stopContainer(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "stop", name), 30);
        assertEquals(0, result.exitCode, "Gitea 容器停止失败: " + result.output);
    }

    private void stopContainerQuietly(String name) {
        try {
            runCommand(Arrays.asList("docker", "rm", "-f", name), 30);
        } catch (Exception ignored) {
        }
    }

    private String getContainerLogs(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "logs", name), 30);
        assertEquals(0, result.exitCode, "读取 Gitea 容器日志失败: " + result.output);
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
                    if (body.contains("Installation - Gitea: Git with a cup of tea")) {
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
        throw new AssertionError("Gitea 在超时前未就绪: " + url, lastException);
    }

    private HttpResult get(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");

        int code = connection.getResponseCode();
        String location = connection.getHeaderField("Location");
        String headers = connection.getHeaderFields().toString();
        String body = "";
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream != null) {
            body = readStream(stream);
        }
        connection.disconnect();
        return new HttpResult(code, location, headers, body);
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
                "gitea-align-command-" + System.nanoTime());
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

    private String formatHttpResult(HttpResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("status=").append(result.code).append('\n');
        builder.append("location=").append(valueOrEmpty(result.location)).append('\n');
        builder.append("headers=").append(valueOrEmpty(result.headers)).append('\n');
        builder.append("body=").append(valueOrEmpty(result.body)).append('\n');
        return builder.toString();
    }

    private String formatScanResult(ScanResult result) {
        return "vulnerable=" + result.isVulnerable() + "\n"
                + "matched_path=" + valueOrEmpty(result.getMatchedPath()) + "\n"
                + "raw_request=" + valueOrEmpty(result.getRawRequest()) + "\n"
                + "raw_response=" + valueOrEmpty(result.getRawResponseSnippet()) + "\n";
    }

    private String buildSummary(HttpResult root,
                                HttpResult install,
                                ScanResult builtinResult,
                                ScanResult directInstallResult) {
        List<String> rows = Arrays.asList(
                "case\tstatus\tmarker\tvulnerable\tmatched_path",
                "root\t" + root.code + "\t" + summarizeMarker(root.location) + "\tfalse\t",
                "install\t" + install.code + "\t" + summarizeMarker(findInstallMarker(install.body)) + "\tfalse\t",
                "builtin-updated\t-\tredirect+title-aligned\t" + builtinResult.isVulnerable() + "\t" + valueOrEmpty(builtinResult.getMatchedPath()),
                "builtin-direct-install\t-\tinstall-path-direct\t" + directInstallResult.isVulnerable() + "\t" + valueOrEmpty(directInstallResult.getMatchedPath())
        );
        return String.join("\n", rows) + "\n";
    }

    private String summarizeMarker(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ');
    }

    private String findInstallMarker(String body) {
        if (body == null) {
            return "";
        }
        if (body.contains("Installation - Gitea: Git with a cup of tea")) {
            return "install-title-single-space";
        }
        return "";
    }

    private byte[] safeBytes(String text) {
        return (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ');
    }

    private static final class HttpResult {
        private final int code;
        private final String location;
        private final String headers;
        private final String body;

        private HttpResult(int code, String location, String headers, String body) {
            this.code = code;
            this.location = location;
            this.headers = headers;
            this.body = body;
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

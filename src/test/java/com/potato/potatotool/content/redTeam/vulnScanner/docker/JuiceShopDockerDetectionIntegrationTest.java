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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Juice Shop Docker 技术识别闭环测试")
class JuiceShopDockerDetectionIntegrationTest {

    private static final String IMAGE = "bkimminich/juice-shop:latest";
    private static final String POC_PATH =
            "src/main/resources/poc/nucleiPoc/http/technologies/owasp-juice-shop-detected.yaml";

    private HttpServer safeServer;
    private String containerName;
    private Integer juiceShopPort;

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
    @DisplayName("内置 Juice Shop 检测模板应在 Docker 靶场命中且对本地安全页不误报")
    void shouldDetectJuiceShopAndExportEvidence() throws Exception {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker 不可用，跳过 Juice Shop Docker 实测");
        Assumptions.assumeTrue(hasImage(IMAGE), "缺少本地镜像 " + IMAGE + "，跳过 Juice Shop Docker 实测");

        juiceShopPort = findFreePort();
        int safePort = findFreePort();
        safeServer = startSafeServer(safePort);

        containerName = "juice-shop-it-" + System.currentTimeMillis();
        startJuiceShopContainer(containerName, juiceShopPort);
        waitForHttpReady("http://127.0.0.1:" + juiceShopPort + "/", 120);

        PocObj.Poc poc = loadPoc();
        ScanResult positive = execute(poc, "http://127.0.0.1:" + juiceShopPort);
        ScanResult negative = execute(poc, "http://127.0.0.1:" + safePort);

        assertTrue(positive.isVulnerable(), "Juice Shop 靶场应被检测模板命中");
        assertFalse(negative.isVulnerable(), "本地安全对照页不应误报为 Juice Shop");
        assertTrue(hasText(positive.getRawResponseSnippet()));
        assertTrue(positive.getRawResponseSnippet().contains("<title>OWASP Juice Shop</title>"));
        assertTrue(hasText(positive.getRawRequest()));

        String containerLogs = getContainerLogs(containerName);
        stopContainer(containerName);
        waitForPortReleased(juiceShopPort, 20);
        containerName = null;

        Path evidenceDir = Paths.get("target", "juice-shop-docker-evidence");
        Files.createDirectories(evidenceDir);

        positive.populateFromPoc();
        negative.populateFromPoc();
        new ReportGenerator().generateAllFormats(Arrays.asList(positive, negative), evidenceDir.toString());

        Files.write(evidenceDir.resolve("positive-request.txt"),
                safeBytes(positive.getRawRequest()));
        Files.write(evidenceDir.resolve("positive-response.txt"),
                safeBytes(positive.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("negative-request.txt"),
                safeBytes(negative.getRawRequest()));
        Files.write(evidenceDir.resolve("negative-response.txt"),
                safeBytes(negative.getRawResponseSnippet()));
        Files.write(evidenceDir.resolve("juice-shop-container.log"),
                containerLogs.getBytes(StandardCharsets.UTF_8));
        Files.write(evidenceDir.resolve("summary.tsv"),
                buildSummary(positive, negative).getBytes(StandardCharsets.UTF_8));

        assertTrue(evidenceDir.resolve("report.html").toFile().exists());
        assertTrue(evidenceDir.resolve("report.json").toFile().exists());
        assertTrue(evidenceDir.resolve("report.txt").toFile().exists());
        assertTrue(evidenceDir.resolve("summary.tsv").toFile().exists());
        assertTrue(evidenceDir.resolve("juice-shop-container.log").toFile().exists());
    }

    private PocObj.Poc loadPoc() throws Exception {
        PocLoader loader = new PocLoader();
        loader.setVerbose(false);
        PocObj.Poc poc = loader.loadFromFile(POC_PATH);
        assertNotNull(poc, "Juice Shop 检测模板加载失败");
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

    private void startJuiceShopContainer(String name, int port) throws Exception {
        CommandResult result = runCommand(Arrays.asList(
                "docker", "run", "-d", "--rm",
                "--name", name,
                "-p", "127.0.0.1:" + port + ":3000",
                IMAGE
        ), 30);
        assertEquals(0, result.exitCode, "Juice Shop 容器启动失败: " + result.output);
    }

    private void stopContainer(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "stop", name), 30);
        assertEquals(0, result.exitCode, "Juice Shop 容器停止失败: " + result.output);
    }

    private void stopContainerQuietly(String name) {
        try {
            runCommand(Arrays.asList("docker", "rm", "-f", name), 30);
        } catch (Exception ignored) {
        }
    }

    private String getContainerLogs(String name) throws Exception {
        CommandResult result = runCommand(Arrays.asList("docker", "logs", name), 30);
        assertEquals(0, result.exitCode, "读取 Juice Shop 容器日志失败: " + result.output);
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
                    if (body.contains("OWASP Juice Shop")) {
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
        throw new AssertionError("Juice Shop 在超时前未就绪: " + url, lastException);
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
                "juice-shop-docker-command-" + System.nanoTime());
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

    private String buildSummary(ScanResult positive, ScanResult negative) {
        StringBuilder builder = new StringBuilder();
        builder.append("case\ttarget\tvulnerable\tfalse_positive\tfalse_negative\tmatched_path\tpoc_id\tpoc_name\n");
        builder.append("positive").append('\t')
                .append(positive.getTarget()).append('\t')
                .append(positive.isVulnerable()).append('\t')
                .append(false).append('\t')
                .append(!positive.isVulnerable()).append('\t')
                .append(valueOrEmpty(positive.getMatchedPath())).append('\t')
                .append(positive.getPoc() == null ? "" : valueOrEmpty(positive.getPoc().getId())).append('\t')
                .append(positive.getPoc() == null ? "" : valueOrEmpty(positive.getPoc().getName())).append('\n');
        builder.append("negative").append('\t')
                .append(negative.getTarget()).append('\t')
                .append(negative.isVulnerable()).append('\t')
                .append(negative.isVulnerable()).append('\t')
                .append(false).append('\t')
                .append(valueOrEmpty(negative.getMatchedPath())).append('\t')
                .append(negative.getPoc() == null ? "" : valueOrEmpty(negative.getPoc().getId())).append('\t')
                .append(negative.getPoc() == null ? "" : valueOrEmpty(negative.getPoc().getName())).append('\n');
        return builder.toString();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.replace('\t', ' ');
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

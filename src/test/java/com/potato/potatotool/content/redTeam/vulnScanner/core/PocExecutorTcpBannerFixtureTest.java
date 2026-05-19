package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TCP Banner 最小正反样本闭环")
public class PocExecutorTcpBannerFixtureTest {

    private static final String BUILTIN_POC =
            "src/main/resources/poc/nucleiPoc/network/exposures/exposed-redis.yaml";

    private ServerSocket positiveServer;
    private ServerSocket negativeServer;
    private Thread positiveThread;
    private Thread negativeThread;

    @AfterEach
    void tearDown() throws Exception {
        closeQuietly(positiveServer);
        closeQuietly(negativeServer);
        joinQuietly(positiveThread);
        joinQuietly(negativeThread);
        positiveServer = null;
        negativeServer = null;
        positiveThread = null;
        negativeThread = null;
    }

    @Test
    @DisplayName("内置 exposed-redis 应命中正样本且不误报反样本")
    public void testBuiltinRedisTcpPocAgainstLocalPositiveAndNegativeFixture() throws Exception {
        PocLoader loader = new PocLoader();
        loader.setVerbose(false);
        PocObj.Poc poc = loader.loadFromFile(BUILTIN_POC);

        assertNotNull(poc, "内置 tcp POC 加载失败");

        positiveServer = new ServerSocket(0);
        negativeServer = new ServerSocket(0);

        positiveThread = startRedisLikeServer(positiveServer, true);
        negativeThread = startRedisLikeServer(negativeServer, false);

        ScanConfig config = new ScanConfig();
        config.setThreads(1);
        config.setTimeout(3);
        config.setRetries(0);
        config.setEnableResponseCache(false);
        config.setEnableClustering(false);

        PocExecutor executor = new PocExecutor(config);

        ScanResult positiveResult = executor.execute("127.0.0.1:" + positiveServer.getLocalPort(), poc);
        ScanResult negativeResult = executor.execute("127.0.0.1:" + negativeServer.getLocalPort(), poc);

        assertTrue(positiveResult.isVulnerable(), "Redis 正样本应命中 exposed-redis");
        assertFalse(negativeResult.isVulnerable(), "近邻反样本不应误报 exposed-redis");
    }

    private Thread startRedisLikeServer(ServerSocket serverSocket, boolean positive) {
        Thread thread = new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                socket.setSoTimeout(2000);
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();
                readRequest(inputStream);
                String response = positive
                        ? "$90\r\n# Server\r\nredis_version:7.2.0\r\nredis_mode:standalone\r\n# Stats\r\nuptime_in_seconds:1\r\n+OK\r\n"
                        : "$85\r\n# Server\r\nredis_version:7.2.0\r\nredis_mode:sentinel\r\n# Stats\r\nuptime_in_seconds:1\r\n+OK\r\n";
                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (Exception ignored) {
            }
        }, positive ? "tcp-redis-positive-fixture" : "tcp-redis-negative-fixture");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void readRequest(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
            String text = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            if (text.contains("quit")) {
                return;
            }
        }
    }

    private void closeQuietly(ServerSocket serverSocket) {
        if (serverSocket == null || serverSocket.isClosed()) {
            return;
        }
        try {
            serverSocket.close();
        } catch (Exception ignored) {
        }
    }

    private void joinQuietly(Thread thread) {
        if (thread == null) {
            return;
        }
        try {
            thread.join(1000);
        } catch (Exception ignored) {
        }
    }
}

package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanCompletedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanErrorEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventListener;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ScanEngine 本地命中探针测试")
class ScanEngineSoakProbeTest {

    private HttpServer server;
    private String originalUserHome;

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
            originalUserHome = null;
        }
        Constants.cachedConfig = null;
        resetSingleton(VulnScanDatabase.class, "instance", true);
        resetSingleton(com.potato.potatotool.storage.PathManager.class, "instance", false);
        resetSingleton(com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig.class, "instance", false);
    }

    @Test
    @DisplayName("单目标单 POC 扫描应命中本地 HTTPServer")
    void shouldHitLocalHttpServerThroughScanEngine(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);

        AtomicInteger hitCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/probe", exchange -> {
            hitCount.incrementAndGet();
            write(exchange, 200, "probe-ok");
        });
        server.start();

        String target = "http://127.0.0.1:" + server.getAddress().getPort();
        PocObj.Poc poc = buildHttpPoc("probe", "/probe", "probe-ok");

        ScanConfig config = new ScanConfig();
        config.setThreads(2);
        config.setEnableClustering(false);
        config.setTimeout(2);
        config.setRetries(0);

        ScanEngine engine = new ScanEngine(config);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<ScanCompletedEvent> completedEventRef = new AtomicReference<ScanCompletedEvent>();
        AtomicReference<ScanErrorEvent> errorEventRef = new AtomicReference<ScanErrorEvent>();
        try {
            engine.addEventListener(new ScanEventListener() {
                @Override
                public void onScanCompleted(ScanCompletedEvent event) {
                    completedEventRef.set(event);
                    completed.countDown();
                }

                @Override
                public void onScanError(ScanErrorEvent event) {
                    errorEventRef.set(event);
                }
            });

            engine.startScan(Collections.singletonList(target), Collections.singletonList(poc));

            assertTrue(completed.await(10, TimeUnit.SECONDS), "扫描未按预期完成");
            assertEquals(1, hitCount.get(), "本地 HTTPServer 未被实际命中");
            ScanCompletedEvent event = completedEventRef.get();
            assertNotNull(event, "未收到完成事件");
            assertTrue(event.isSuccess(), "完成事件不应为失败");
            assertEquals(1, event.getResults().size(), "应返回 1 条结果");
            assertEquals(1, event.getVulnerabilityCount(), "应命中 1 条漏洞");
            assertTrue(errorEventRef.get() == null, "不应出现扫描错误: " + errorEventRef.get());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("多轮短周期扫描应持续完成且不在数据库状态写入处阻塞")
    void shouldCompleteRepeatedShortCyclesWithoutBlockingOnScanState(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);

        AtomicInteger hitCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/probe-repeat", exchange -> {
            hitCount.incrementAndGet();
            write(exchange, 200, "probe-repeat-ok");
        });
        server.start();

        String target = "http://127.0.0.1:" + server.getAddress().getPort();
        PocObj.Poc poc = buildHttpPoc("probe-repeat", "/probe-repeat", "probe-repeat-ok");
        List<String> targets = Collections.singletonList(target);
        List<PocObj.Poc> pocs = Collections.singletonList(poc);

        for (int i = 0; i < 6; i++) {
            ScanConfig config = new ScanConfig();
            config.setThreads(4);
            config.setEnableClustering(false);
            config.setEnableResponseCache(false);
            config.setTimeout(2);
            config.setRetries(0);

            ScanEngine engine = new ScanEngine(config);
            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<ScanCompletedEvent> completedEventRef = new AtomicReference<ScanCompletedEvent>();
            AtomicReference<ScanErrorEvent> errorEventRef = new AtomicReference<ScanErrorEvent>();
            try {
                engine.addEventListener(new ScanEventListener() {
                    @Override
                    public void onScanCompleted(ScanCompletedEvent event) {
                        completedEventRef.set(event);
                        completed.countDown();
                    }

                    @Override
                    public void onScanError(ScanErrorEvent event) {
                        errorEventRef.set(event);
                    }
                });

                engine.startScan(targets, pocs);

                assertTrue(completed.await(15, TimeUnit.SECONDS), "第 " + i + " 轮扫描未按预期完成");
                ScanCompletedEvent event = completedEventRef.get();
                assertNotNull(event, "第 " + i + " 轮未收到完成事件");
                assertTrue(event.isSuccess(), "第 " + i + " 轮完成事件不应失败");
                assertEquals(1, event.getVulnerabilityCount(), "第 " + i + " 轮应命中 1 条漏洞");
                assertTrue(errorEventRef.get() == null, "第 " + i + " 轮不应出现扫描错误: " + errorEventRef.get());
            } finally {
                engine.shutdown();
            }
        }

        assertEquals(6, hitCount.get(), "多轮扫描命中次数不符合预期");
    }

    private void configureIsolatedHome(Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path dbPath = tempHome.resolve("db").resolve("scan-engine-soak-probe.sqlite");
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
        resetSingleton(VulnScanDatabase.class, "instance", true);
        resetSingleton(com.potato.potatotool.storage.PathManager.class, "instance", false);
        resetSingleton(com.potato.potatotool.content.redTeam.vulnScanner.config.VulnScanConfig.class, "instance", false);
    }

    private PocObj.Poc buildHttpPoc(String id, String path, String expectedBody) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol("http");
        poc.setSeverity(PocObj.Severity.MEDIUM);

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

    private void write(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void resetSingleton(Class<?> type, String fieldName, boolean closeDatabase) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object existing = field.get(null);
        if (closeDatabase && existing instanceof VulnScanDatabase) {
            ((VulnScanDatabase) existing).close();
        }
        field.set(null, null);
    }

    private String escapeForJson(String value) {
        return value.replace("\\", "\\\\");
    }
}

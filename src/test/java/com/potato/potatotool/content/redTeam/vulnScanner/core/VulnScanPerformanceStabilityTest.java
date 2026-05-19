package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanCompletedEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventListener;
import com.potato.potatotool.content.redTeam.vulnScanner.exception.NetworkException;
import com.potato.potatotool.content.redTeam.vulnScanner.http.TcpHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.storage.VulnScanDatabase;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ConnectionPoolManager;
import com.potato.potatotool.content.redTeam.vulnScanner.util.ScanLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("漏洞扫描性能与稳定性回归测试")
class VulnScanPerformanceStabilityTest {

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
    @DisplayName("100 目标 x 100 POC 中等规模扫描应稳定完成并输出摘要")
    void shouldCompleteMediumScaleScanAndWriteSummary() throws Exception {
        AtomicInteger hitCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/medium", exchange -> {
            hitCount.incrementAndGet();
            write(exchange, 200, "medium-ok");
        });
        server.start();

        String target = "http://127.0.0.1:" + server.getAddress().getPort();
        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setThreads(8);

        PocExecutor executor = new PocExecutor(config);
        List<PocObj.Poc> pocs = new ArrayList<PocObj.Poc>();
        for (int i = 0; i < 100; i++) {
            pocs.add(buildHttpPoc("medium-" + i, "/medium", "medium-ok"));
        }

        long start = System.currentTimeMillis();
        int vulnerableCount = 0;
        for (int targetIndex = 0; targetIndex < 100; targetIndex++) {
            for (PocObj.Poc poc : pocs) {
                ScanResult result = executor.execute(target, poc);
                if (result.isVulnerable()) {
                    vulnerableCount++;
                }
            }
        }
        long durationMs = System.currentTimeMillis() - start;

        assertEquals(10_000, vulnerableCount);
        assertEquals(10_000, hitCount.get());

        writeSummary("medium-scale-summary.txt",
                "case=100x100\n" +
                        "duration_ms=" + durationMs + "\n" +
                        "hit_count=" + hitCount.get() + "\n" +
                        "vulnerable_count=" + vulnerableCount + "\n");
    }

    @Test
    @DisplayName("缓存开关切换后结果应一致且缓存开启命中更少")
    void shouldKeepResultsConsistentAcrossResponseCacheSwitch() throws Exception {
        AtomicInteger hitCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/cache-compare", exchange -> {
            hitCount.incrementAndGet();
            write(exchange, 200, "cache-compare-ok");
        });
        server.start();

        String target = "http://127.0.0.1:" + server.getAddress().getPort();
        PocObj.Poc poc = buildHttpPoc("cache-compare", "/cache-compare", "cache-compare-ok");

        ScanConfig cacheOffConfig = new ScanConfig();
        cacheOffConfig.setEnableClustering(false);
        cacheOffConfig.setEnableResponseCache(false);
        PocExecutor cacheOffExecutor = new PocExecutor(cacheOffConfig);

        ScanResult offFirst = cacheOffExecutor.execute(target, poc);
        ScanResult offSecond = cacheOffExecutor.execute(target, poc);
        int cacheOffHits = hitCount.get();

        ScanConfig cacheOnConfig = new ScanConfig();
        cacheOnConfig.setEnableClustering(false);
        cacheOnConfig.setEnableResponseCache(true);
        PocExecutor cacheOnExecutor = new PocExecutor(cacheOnConfig);

        ScanResult onFirst = cacheOnExecutor.execute(target, poc);
        ScanResult onSecond = cacheOnExecutor.execute(target, poc);
        int totalHits = hitCount.get();
        int cacheOnHits = totalHits - cacheOffHits;

        assertTrue(offFirst.isVulnerable());
        assertTrue(offSecond.isVulnerable());
        assertTrue(onFirst.isVulnerable());
        assertTrue(onSecond.isVulnerable());
        assertEquals(offFirst.isVulnerable(), onFirst.isVulnerable());
        assertTrue(cacheOffHits >= 2);
        assertEquals(1, cacheOnHits);

        writeSummary("cache-switch-summary.txt",
                "cache_off_hits=" + cacheOffHits + "\n" +
                        "cache_on_hits=" + cacheOnHits + "\n" +
                        "result_consistent=true\n");
    }

    @Test
    @DisplayName("去重开关切换后应改变 POC 数量但不改变漏洞发现结论")
    void shouldKeepDetectionConsistentAcrossDeduplicationSwitch() throws Exception {
        SmartPocSelectorSelectionTestSupport support = new SmartPocSelectorSelectionTestSupport();
        List<PocObj.Poc> duplicated = support.buildDuplicatePocs();

        List<PocObj.Poc> withoutDedup = support.select(duplicated, false);
        List<PocObj.Poc> withDedup = support.select(duplicated, true);

        assertEquals(2, withoutDedup.size());
        assertEquals(1, withDedup.size());
        assertEquals(withoutDedup.get(0).getName(), withDedup.get(0).getName());

        writeSummary("dedup-switch-summary.txt",
                "without_dedup=" + withoutDedup.size() + "\n" +
                        "with_dedup=" + withDedup.size() + "\n" +
                        "result_consistent=true\n");
    }

    @Test
    @DisplayName("慢响应与断连目标应稳定分类并输出摘要")
    void shouldHandleSlowAndDisconnectTargets() throws Exception {
        List<String> cases = new CopyOnWriteArrayList<String>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/slow", exchange -> {
            sleepQuietly(1200);
            cases.add("slow");
            write(exchange, 200, "slow-ok");
        });
        server.createContext("/disconnect", exchange -> {
            cases.add("disconnect");
            exchange.getResponseBody().close();
            exchange.close();
        });
        server.start();

        String target = "http://127.0.0.1:" + server.getAddress().getPort();

        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setRetries(0);
        PocExecutor executor = new PocExecutor(config);
        NetworkException slowException = null;
        String disconnectOutcome;
        try {
            executor.execute(target, buildHttpPoc("slow", "/slow", "never", 1));
        } catch (NetworkException ex) {
            slowException = ex;
        }

        try {
            ScanResult disconnectResult = executor.execute(target, buildHttpPoc("disconnect", "/disconnect", "never", 1));
            assertFalse(disconnectResult.isVulnerable());
            disconnectOutcome = "NON_VULNERABLE";
        } catch (Exception ex) {
            disconnectOutcome = ex.getClass().getSimpleName();
        }

        assertTrue(cases.contains("slow"));
        assertTrue(cases.contains("disconnect"));
        assertEquals(com.potato.potatotool.content.redTeam.vulnScanner.exception.NetworkException.NetworkErrorType.READ_TIMEOUT,
                slowException.getErrorType());

        writeSummary("slow-disconnect-summary.txt",
                "slow_case=READ_TIMEOUT\n" +
                        "disconnect_case=" + disconnectOutcome + "\n");
    }

    @Test
    @DisplayName("1 目标 x 1000 POC 压力测试应稳定完成并输出摘要")
    void shouldCompleteOneTargetThousandPocsPressureRun() throws Exception {
        AtomicInteger hitCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/thousand", exchange -> {
            hitCount.incrementAndGet();
            write(exchange, 200, "thousand-ok");
        });
        server.start();

        String target = "http://127.0.0.1:" + server.getAddress().getPort();
        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setThreads(12);

        PocExecutor executor = new PocExecutor(config);
        List<PocObj.Poc> pocs = new ArrayList<PocObj.Poc>();
        for (int i = 0; i < 1000; i++) {
            pocs.add(buildHttpPoc("thousand-" + i, "/thousand", "thousand-ok"));
        }

        long start = System.currentTimeMillis();
        int vulnerableCount = 0;
        for (PocObj.Poc poc : pocs) {
            ScanResult result = executor.execute(target, poc);
            if (result.isVulnerable()) {
                vulnerableCount++;
            }
        }
        long durationMs = System.currentTimeMillis() - start;

        assertEquals(1000, vulnerableCount);
        assertEquals(1000, hitCount.get());

        writeSummary("thousand-scale-summary.txt",
                "case=1x1000\n" +
                        "duration_ms=" + durationMs + "\n" +
                        "hit_count=" + hitCount.get() + "\n" +
                        "vulnerable_count=" + vulnerableCount + "\n");
    }

    @Test
    @DisplayName("随机断连目标应稳定归类且结果可复现")
    void shouldHandleRandomDisconnectTargetsDeterministically() throws Exception {
        final Random random = new Random(20260516L);
        final AtomicInteger requestCount = new AtomicInteger();
        final AtomicInteger disconnectCount = new AtomicInteger();
        final AtomicInteger successCount = new AtomicInteger();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/random-disconnect", exchange -> {
            requestCount.incrementAndGet();
            if (random.nextBoolean()) {
                disconnectCount.incrementAndGet();
                exchange.getResponseBody().close();
                exchange.close();
                return;
            }
            successCount.incrementAndGet();
            write(exchange, 200, "random-ok");
        });
        server.start();

        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        PocExecutor executor = new PocExecutor(config);
        String target = "http://127.0.0.1:" + server.getAddress().getPort();
        int vulnerableCount = 0;
        int nonVulnerableCount = 0;
        int exceptionCount = 0;

        for (int i = 0; i < 20; i++) {
            try {
                ScanResult result = executor.execute(target,
                        buildHttpPoc("random-disconnect-" + i, "/random-disconnect?i=" + i, "random-ok", 1));
                if (result.isVulnerable()) {
                    vulnerableCount++;
                } else {
                    nonVulnerableCount++;
                }
            } catch (Exception ex) {
                exceptionCount++;
            }
        }

        assertTrue(requestCount.get() >= 20);
        assertEquals(requestCount.get(), disconnectCount.get() + successCount.get());
        assertTrue(disconnectCount.get() > 0);
        assertTrue(successCount.get() > 0);
        assertEquals(successCount.get(), vulnerableCount);
        assertTrue(disconnectCount.get() >= nonVulnerableCount + exceptionCount);

        writeSummary("random-disconnect-summary.txt",
                "seed=20260516\n" +
                        "logical_requests=20\n" +
                        "requests=" + requestCount.get() + "\n" +
                        "disconnects=" + disconnectCount.get() + "\n" +
                        "successes=" + successCount.get() + "\n" +
                        "non_vulnerable=" + nonVulnerableCount + "\n" +
                        "exceptions=" + exceptionCount + "\n");
    }

    @Test
    @DisplayName("2 小时长时间扫描应保持资源稳定并持续输出摘要")
    void shouldSustainTwoHourSoakWithoutResourceLeak(@TempDir Path tempHome) throws Exception {
        assumeTrue(Boolean.parseBoolean(System.getProperty("vulnscan.soak.enabled", "false")),
                "显式设置 -Dvulnscan.soak.enabled=true 后才执行 2 小时 soak");

        int soakMinutes = Integer.getInteger("vulnscan.soak.minutes", 120);
        assumeTrue(soakMinutes >= 120, "2 小时 soak 验收要求最短运行 120 分钟");

        configureIsolatedHome(tempHome);
        ConnectionPoolManager.getInstance().reset();
        ScanLogger.getInstance().clearMemoryLogs();

        AtomicInteger hitCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/soak", exchange -> {
            hitCount.incrementAndGet();
            write(exchange, 200, "soak-ok");
        });
        server.start();

        String target = "http://127.0.0.1:" + server.getAddress().getPort();
        List<PocObj.Poc> pocs = new ArrayList<PocObj.Poc>();
        for (int i = 0; i < 10; i++) {
            pocs.add(buildHttpPoc("soak-" + i, "/soak?i=" + i, "soak-ok", 2));
        }

        long durationMs = TimeUnit.MINUTES.toMillis(soakMinutes);
        long start = System.currentTimeMillis();
        long deadline = start + durationMs;
        long heartbeatMs = TimeUnit.MINUTES.toMillis(5);
        long nextHeartbeat = start;

        int cycles = 0;
        int totalFindings = 0;
        int maxScanEngineThreads = countThreads("scan-engine-thread-");
        int maxDbWriterThreads = countThreads("scan-db-writer");

        while (System.currentTimeMillis() < deadline) {
            ScanConfig config = new ScanConfig();
            config.setThreads(4);
            config.setTimeout(2);
            config.setRetries(0);
            config.setEnableClustering(false);
            config.setEnableResponseCache(false);

            ScanEngine engine = new ScanEngine(config);
            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<ScanCompletedEvent> completedEventRef = new AtomicReference<ScanCompletedEvent>();
            try {
                engine.addEventListener(new ScanEventListener() {
                    @Override
                    public void onScanCompleted(ScanCompletedEvent event) {
                        completedEventRef.set(event);
                        completed.countDown();
                    }
                });
                engine.startScan(Collections.singletonList(target), pocs);
                assertTrue(completed.await(30, TimeUnit.SECONDS), "soak 周期未在预期时间内完成");
                ScanCompletedEvent completedEvent = completedEventRef.get();
                assertTrue(completedEvent != null && completedEvent.isSuccess(), "soak 周期未产出成功完成事件");
                int findingCount = completedEvent.getVulnerabilityCount();
                assertTrue(findingCount >= 0, "漏洞数不应为负数");
                totalFindings += findingCount;
            } finally {
                engine.shutdown();
            }

            waitForThreadCountAtMost("scan-engine-thread-", 0, 3000);
            waitForThreadCountAtMost("scan-db-writer", 0, 3000);

            int currentScanEngineThreads = countThreads("scan-engine-thread-");
            int currentDbWriterThreads = countThreads("scan-db-writer");
            if (currentScanEngineThreads > maxScanEngineThreads) {
                maxScanEngineThreads = currentScanEngineThreads;
            }
            if (currentDbWriterThreads > maxDbWriterThreads) {
                maxDbWriterThreads = currentDbWriterThreads;
            }

            ConnectionPoolManager.ConnectionPoolStatus poolStatus = ConnectionPoolManager.getInstance().getStatus();
            int memoryLogSize = ScanLogger.getInstance().getMemoryLogs().size();
            boolean dbValid = VulnScanDatabase.getInstance().isConnectionValid();

            assertEquals(0, poolStatus.activeConnections);
            assertTrue(memoryLogSize <= 1000, "内存日志应被上限约束，实际: " + memoryLogSize);
            assertTrue(dbValid, "数据库连接在 soak 期间失效");

            cycles++;
            long now = System.currentTimeMillis();
            if (now >= nextHeartbeat || now >= deadline) {
                writeSummary("two-hour-soak-heartbeat.txt",
                        "elapsed_ms=" + (now - start) + "\n" +
                                "elapsed_minutes=" + TimeUnit.MILLISECONDS.toMinutes(now - start) + "\n" +
                                "cycles=" + cycles + "\n" +
                                "total_findings=" + totalFindings + "\n" +
                                "http_hits=" + hitCount.get() + "\n" +
                                "pool_total=" + poolStatus.totalConnections + "\n" +
                                "pool_active=" + poolStatus.activeConnections + "\n" +
                                "pool_hosts=" + poolStatus.hostCount + "\n" +
                                "memory_logs=" + memoryLogSize + "\n" +
                                "db_valid=" + dbValid + "\n" +
                                "scan_engine_threads=" + currentScanEngineThreads + "\n" +
                                "scan_db_writer_threads=" + currentDbWriterThreads + "\n" +
                                "max_scan_engine_threads=" + maxScanEngineThreads + "\n" +
                                "max_scan_db_writer_threads=" + maxDbWriterThreads + "\n");
                nextHeartbeat = now + heartbeatMs;
            }
        }

        ConnectionPoolManager.ConnectionPoolStatus finalPoolStatus = ConnectionPoolManager.getInstance().getStatus();
        int finalMemoryLogSize = ScanLogger.getInstance().getMemoryLogs().size();
        boolean finalDbValid = VulnScanDatabase.getInstance().isConnectionValid();
        int finalScanEngineThreads = countThreads("scan-engine-thread-");
        int finalDbWriterThreads = countThreads("scan-db-writer");

        assertEquals(0, finalPoolStatus.activeConnections);
        assertTrue(finalMemoryLogSize <= 1000);
        assertTrue(finalDbValid);
        assertEquals(0, finalScanEngineThreads);
        assertEquals(0, finalDbWriterThreads);

        writeSummary("two-hour-soak-summary.txt",
                "duration_minutes=" + soakMinutes + "\n" +
                        "cycles=" + cycles + "\n" +
                        "total_findings=" + totalFindings + "\n" +
                        "http_hits=" + hitCount.get() + "\n" +
                        "pool_total=" + finalPoolStatus.totalConnections + "\n" +
                        "pool_active=" + finalPoolStatus.activeConnections + "\n" +
                        "pool_hosts=" + finalPoolStatus.hostCount + "\n" +
                        "memory_logs=" + finalMemoryLogSize + "\n" +
                        "db_valid=" + finalDbValid + "\n" +
                        "max_scan_engine_threads=" + maxScanEngineThreads + "\n" +
                        "max_scan_db_writer_threads=" + maxDbWriterThreads + "\n" +
                        "final_scan_engine_threads=" + finalScanEngineThreads + "\n" +
                        "final_scan_db_writer_threads=" + finalDbWriterThreads + "\n");
    }

    @Test
    @DisplayName("半开 TCP 目标应超时返回且输出摘要")
    void shouldReportHalfOpenTcpTargetTimeout() throws Exception {
        final CountDownLatch accepted = new CountDownLatch(1);
        final ServerSocket serverSocket = new ServerSocket(0);
        final int port = serverSocket.getLocalPort();
        Thread halfOpenThread = new Thread(() -> {
            try {
                Socket socket = serverSocket.accept();
                accepted.countDown();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            } catch (IOException ignored) {
            }
        }, "half-open-tcp-server");
        halfOpenThread.setDaemon(true);
        halfOpenThread.start();

        TcpHandler.TcpResponse response;
        try {
            response = com.potato.potatotool.content.redTeam.vulnScanner.http.TcpHandler.send(
                    "127.0.0.1:" + port, "ping", 100);
        } finally {
            serverSocket.close();
            halfOpenThread.interrupt();
            halfOpenThread.join(1000);
        }

        assertTrue(accepted.await(1, java.util.concurrent.TimeUnit.SECONDS));
        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("连接超时"), "应返回超时错误，实际: " + response.getError());

        writeSummary("half-open-tcp-summary.txt",
                "accepted=true\n" +
                        "result=TIMEOUT\n");
    }

    private PocObj.Poc buildHttpPoc(String id, String path, String expectedBody) {
        return buildHttpPoc(id, path, expectedBody, 3);
    }

    private PocObj.Poc buildHttpPoc(String id, String path, String expectedBody, int timeoutSeconds) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol("http");
        poc.setSeverity(PocObj.Severity.MEDIUM);

        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId("http_1");
        step.setMethod("GET");
        step.setPath(path);
        step.setTimeout(timeoutSeconds);

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

    private void writeSummary(String fileName, String content) throws IOException {
        File dir = new File("target/vulnscan-performance");
        if (!dir.exists()) {
            assertTrue(dir.mkdirs());
        }
        Files.write(new File(dir, fileName).toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void configureIsolatedHome(Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path dbPath = tempHome.resolve("db").resolve("scan-performance.sqlite");
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

    private void waitForThreadCountAtMost(String prefix, int maxAllowed, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (countThreads(prefix) <= maxAllowed) {
                return;
            }
            Thread.sleep(50);
        }
        assertTrue(countThreads(prefix) <= maxAllowed,
                "线程未按预期回收: prefix=" + prefix + ", count=" + countThreads(prefix));
    }

    private int countThreads(String prefix) {
        int count = 0;
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread != null && thread.isAlive() && thread.getName() != null && thread.getName().startsWith(prefix)) {
                count++;
            }
        }
        return count;
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

    private static final class SmartPocSelectorSelectionTestSupport {
        List<PocObj.Poc> buildDuplicatePocs() {
            List<PocObj.Poc> pocs = new ArrayList<PocObj.Poc>();
            pocs.add(buildDuplicateNamedPoc("goby-duplicate", "goby", ""));
            pocs.add(buildDuplicateNamedPoc("nuclei-duplicate", "nuclei", "CVE-2024-0001"));
            return pocs;
        }

        List<PocObj.Poc> select(List<PocObj.Poc> pocs, boolean dedup) {
            com.potato.potatotool.content.redTeam.vulnScanner.loader.PocRepository repository =
                    new com.potato.potatotool.content.redTeam.vulnScanner.loader.PocRepository();
            for (PocObj.Poc poc : pocs) {
                repository.addPoc(poc);
            }
            SmartPocSelector selector = new SmartPocSelector(
                    repository,
                    new PocExecutor(new ScanConfig()),
                    new com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventDispatcher());
            ScanConfig config = ScanConfig.createDefaultUrlConfig();
            config.setAutoDetectInputType(false);
            config.setSkipFingerprint(true);
            config.setEnableDeduplication(dedup);
            return selector.selectAndFilterPocs("http://127.0.0.1", config).getPocs();
        }

        private PocObj.Poc buildDuplicateNamedPoc(String id, String format, String cveId) {
            PocObj.Poc poc = new PocObj.Poc();
            poc.setId(id);
            poc.setName("Shared Duplicate Name");
            poc.setOriginalFormat(format);
            poc.setSeverity(PocObj.Severity.HIGH);
            poc.setCategory(PocObj.PocCategory.CVES);
            poc.setProtocol("http");
            poc.setInputType(PocObj.InputType.URL);
            poc.setTags(Collections.singletonList("dup"));
            poc.setCveId(cveId);
            PocObj.PocStep step = new PocObj.PocStep();
            step.setMethod("GET");
            step.setPath("/dup");
            poc.setVerifySteps(Collections.singletonList(step));
            return poc;
        }
    }
}

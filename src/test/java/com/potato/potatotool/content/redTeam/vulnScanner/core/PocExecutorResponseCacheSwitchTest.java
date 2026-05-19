package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PocExecutor 响应缓存开关测试")
class PocExecutorResponseCacheSwitchTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("enableResponseCache=true 时重复相同请求应复用响应")
    void shouldReuseResponseWhenGlobalCacheEnabled() throws Exception {
        AtomicInteger hitCount = startServer();
        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        config.setEnableResponseCache(true);

        PocExecutor executor = new PocExecutor(config);
        PocObj.Poc poc = buildHttpPoc("cache-on", "/cache", "cache-hit");
        String target = "http://127.0.0.1:" + server.getAddress().getPort();

        ScanResult first = executor.execute(target, poc);
        ScanResult second = executor.execute(target, poc);

        assertTrue(first.isVulnerable());
        assertTrue(second.isVulnerable());
        assertEquals(1, hitCount.get(), "缓存开启时第二次应直接命中缓存");
    }

    @Test
    @DisplayName("enableResponseCache=false 时重复相同请求应重复发包")
    void shouldSendRequestAgainWhenGlobalCacheDisabled() throws Exception {
        AtomicInteger hitCount = startServer();
        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);

        PocExecutor executor = new PocExecutor(config);
        PocObj.Poc poc = buildHttpPoc("cache-off", "/cache", "cache-hit");
        String target = "http://127.0.0.1:" + server.getAddress().getPort();

        ScanResult first = executor.execute(target, poc);
        ScanResult second = executor.execute(target, poc);

        assertTrue(first.isVulnerable());
        assertTrue(second.isVulnerable());
        assertEquals(2, hitCount.get(), "缓存关闭时两次都应真实访问目标");
    }

    @Test
    @DisplayName("响应缓存不应跨不同认证头复用")
    void shouldNotReuseCachedResponseAcrossDifferentAuthorizationHeaders() throws Exception {
        AtomicInteger hitCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/auth-cache", exchange -> {
            int current = hitCount.incrementAndGet();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            write(exchange, 200, (auth == null ? "none" : auth) + "-" + current);
        });
        server.start();

        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        config.setEnableResponseCache(true);

        String target = "http://127.0.0.1:" + server.getAddress().getPort();
        PocExecutor executor = new PocExecutor(config);

        ScanResult first = executor.execute(target, buildHeaderAwarePoc("auth-a", "/auth-cache", "Bearer token-a"));
        ScanResult second = executor.execute(target, buildHeaderAwarePoc("auth-b", "/auth-cache", "Bearer token-b"));

        assertTrue(first.isVulnerable());
        assertTrue(second.isVulnerable());
        assertEquals(2, hitCount.get(), "不同 Authorization 头不应命中同一个缓存项");
    }

    @Test
    @DisplayName("requestsPerSecond>0 时应限制连续请求节奏")
    void shouldThrottleRequestsWhenRequestsPerSecondConfigured() throws Exception {
        List<Long> hitTimes = new CopyOnWriteArrayList<Long>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rate-limit", exchange -> {
            hitTimes.add(System.nanoTime());
            write(exchange, 200, "rate-limit-hit");
        });
        server.start();

        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setRequestsPerSecond(2);

        PocExecutor executor = new PocExecutor(config);
        PocObj.Poc poc = buildHttpPoc("rate-limit", "/rate-limit", "rate-limit-hit");
        String target = "http://127.0.0.1:" + server.getAddress().getPort();

        executor.execute(target, poc);
        executor.execute(target, poc);
        executor.execute(target, poc);

        assertEquals(3, hitTimes.size(), "限速开启后仍应完成全部请求");

        long firstGapMs = nanosToMillis(hitTimes.get(1) - hitTimes.get(0));
        long secondGapMs = nanosToMillis(hitTimes.get(2) - hitTimes.get(1));

        assertTrue(firstGapMs >= 430, "2 rps 下首个间隔应接近 500ms，实际: " + firstGapMs + "ms");
        assertTrue(secondGapMs >= 430, "2 rps 下第二个间隔应接近 500ms，实际: " + secondGapMs + "ms");
    }

    @Test
    @DisplayName("requestsPerSecond=0 时不应额外限制连续请求")
    void shouldNotThrottleRequestsWhenRequestsPerSecondDisabled() throws Exception {
        List<Long> hitTimes = new CopyOnWriteArrayList<Long>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rate-unlimited", exchange -> {
            hitTimes.add(System.nanoTime());
            write(exchange, 200, "rate-unlimited-hit");
        });
        server.start();

        ScanConfig config = new ScanConfig();
        config.setEnableClustering(false);
        config.setEnableResponseCache(false);
        config.setRequestsPerSecond(0);

        PocExecutor executor = new PocExecutor(config);
        PocObj.Poc poc = buildHttpPoc("rate-unlimited", "/rate-unlimited", "rate-unlimited-hit");
        String target = "http://127.0.0.1:" + server.getAddress().getPort();

        executor.execute(target, poc);
        executor.execute(target, poc);
        executor.execute(target, poc);

        assertEquals(3, hitTimes.size(), "不限速时仍应完成全部请求");

        long firstGapMs = nanosToMillis(hitTimes.get(1) - hitTimes.get(0));
        long secondGapMs = nanosToMillis(hitTimes.get(2) - hitTimes.get(1));

        assertTrue(firstGapMs < 350, "不限速时请求间隔不应被人为拉长，实际: " + firstGapMs + "ms");
        assertTrue(secondGapMs < 350, "不限速时请求间隔不应被人为拉长，实际: " + secondGapMs + "ms");
    }

    private AtomicInteger startServer() throws IOException {
        AtomicInteger hitCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/cache", exchange -> {
            int current = hitCount.incrementAndGet();
            write(exchange, 200, "cache-hit-" + current);
        });
        server.start();
        return hitCount;
    }

    private PocObj.Poc buildHttpPoc(String id, String path, String expectedBodyKeyword) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol("http");
        poc.setSeverity(PocObj.Severity.MEDIUM);

        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId("http_1");
        step.setMethod("GET");
        step.setPath(path);

        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList(expectedBodyKeyword));
        step.setMatchers(Collections.singletonList(matcher));

        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private PocObj.Poc buildHeaderAwarePoc(String id, String path, String authorizationValue) {
        PocObj.Poc poc = buildHttpPoc(id, path, authorizationValue);
        poc.getVerifySteps().get(0).setHeaders(Collections.singletonMap("Authorization", authorizationValue));
        return poc;
    }

    private void write(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }
}

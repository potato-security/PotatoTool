package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Utils 网页采集缓存测试")
class UtilsWebCollectionCacheTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        Utils.clearCachesForTest();
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("completeUrl 应缓存探活结果避免重复 HEAD")
    void completeUrlShouldCacheResolvedResult() throws Exception {
        AtomicInteger headRequests = new AtomicInteger();
        AtomicInteger pageRequests = new AtomicInteger();
        AtomicInteger iconRequests = new AtomicInteger();
        startServer(headRequests, pageRequests, iconRequests);

        String host = "127.0.0.1:" + server.getAddress().getPort();
        String first = Utils.completeUrl(host, false);
        String second = Utils.completeUrl(host, false);

        assertEquals("http://" + host, first);
        assertEquals(first, second);
        assertEquals(1, headRequests.get());
        assertEquals(0, pageRequests.get());
        assertEquals(0, iconRequests.get());
    }

    @Test
    @DisplayName("getWebBaseInfo 应复用页面与图标缓存")
    void getWebBaseInfoShouldReusePageAndIconCache() throws Exception {
        AtomicInteger headRequests = new AtomicInteger();
        AtomicInteger pageRequests = new AtomicInteger();
        AtomicInteger iconRequests = new AtomicInteger();
        startServer(headRequests, pageRequests, iconRequests);

        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        Map<String, Object> first = Utils.getWebBaseInfo(url, true, false);
        Map<String, Object> second = Utils.getWebBaseInfo(url, true, false);

        assertEquals("Cache Title", first.get("title"));
        assertEquals(first.get("title"), second.get("title"));
        assertTrue(first.containsKey("iconMd5"));
        assertTrue(second.containsKey("iconMd5"));
        assertEquals(1, pageRequests.get());
        assertEquals(1, iconRequests.get());
        assertEquals(0, headRequests.get());
    }

    @Test
    @DisplayName("getWebInfo 无深度采集时应走快速路径并复用缓存")
    void getWebInfoWithoutDeepCollectionShouldReuseBaseCache() throws Exception {
        AtomicInteger headRequests = new AtomicInteger();
        AtomicInteger pageRequests = new AtomicInteger();
        AtomicInteger iconRequests = new AtomicInteger();
        startServer(headRequests, pageRequests, iconRequests);

        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        Utils.getWebBaseInfo(url, true, false);

        Map<String, Object> webInfo = Utils.getWebInfo(url, false, false, 2, 2, false);

        assertEquals("Cache Title", webInfo.get("title"));
        assertTrue(webInfo.containsKey("iconBase64"));
        assertEquals(1, pageRequests.get());
        assertEquals(1, iconRequests.get());

        Object sensitive = webInfo.get("sensitive");
        assertNotNull(sensitive);
        assertTrue(sensitive instanceof List);
        assertTrue(((List<?>) sensitive).isEmpty());

        Object internalLinks = webInfo.get("internalLinks");
        assertNotNull(internalLinks);
        assertTrue(internalLinks instanceof Set);
        assertTrue(((Set<?>) internalLinks).isEmpty());
        assertFalse(webInfo.isEmpty());
    }

    @Test
    @DisplayName("getWebInfo 深度采集应复用页面快照缓存")
    void getWebInfoWithDeepCollectionShouldReusePageSnapshotCache() throws Exception {
        DeepServerCounters counters = startDeepServer("/a.html", "/b.html");

        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        Map<String, Object> first = Utils.getWebInfo(url, true, true, 2, 5, false);
        Map<String, Object> second = Utils.getWebInfo(url, true, true, 2, 5, false);

        assertEquals("Deep Root", first.get("title"));
        assertEquals(first.get("title"), second.get("title"));
        assertEquals(1, counters.rootRequests.get());
        assertEquals(1, counters.childRequests.get("/a.html").get());
        assertEquals(1, counters.childRequests.get("/b.html").get());
        assertEquals(1, counters.iconRequests.get());
        assertEquals(2, ((Set<?>) first.get("internalLinks")).size());
        assertEquals(3, ((List<?>) first.get("sensitive")).size());
    }

    @Test
    @DisplayName("getWebInfo 深度采集应遵守子链接预算")
    void getWebInfoShouldRespectInternalLinkBudget() throws Exception {
        DeepServerCounters counters = startDeepServer("/a.html", "/b.html", "/c.html", "/d.html");

        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        Map<String, Object> webInfo = Utils.getWebInfo(url, true, false, 2, 2, false);

        assertEquals("Deep Root", webInfo.get("title"));
        assertEquals(1, counters.rootRequests.get());
        assertEquals(1, counters.iconRequests.get());
        assertEquals(2, ((Set<?>) webInfo.get("internalLinks")).size());
        assertEquals(2, counters.getTotalChildRequests());
    }

    private void startServer(final AtomicInteger headRequests,
                             final AtomicInteger pageRequests,
                             final AtomicInteger iconRequests) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        final byte[] htmlBytes = ("<html><head><title>Cache Title</title>"
                + "<link rel=\"icon\" href=\"/favicon.ico\" /></head>"
                + "<body><h1>cache-body</h1><p>hello cache</p></body></html>").getBytes(StandardCharsets.UTF_8);
        final byte[] iconBytes = "fake-ico-binary".getBytes(StandardCharsets.UTF_8);

        server.createContext("/", exchange -> {
            try {
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    headRequests.incrementAndGet();
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                pageRequests.incrementAndGet();
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, htmlBytes.length);
                writeResponse(exchange, htmlBytes);
            } finally {
                exchange.close();
            }
        });

        server.createContext("/favicon.ico", exchange -> {
            try {
                iconRequests.incrementAndGet();
                exchange.getResponseHeaders().add("Content-Type", "image/x-icon");
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                exchange.sendResponseHeaders(200, iconBytes.length);
                writeResponse(exchange, iconBytes);
            } finally {
                exchange.close();
            }
        });

        server.start();
    }

    private DeepServerCounters startDeepServer(String... childPaths) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        DeepServerCounters counters = new DeepServerCounters();

        final byte[] rootHtmlBytes = buildRootHtml(childPaths).getBytes(StandardCharsets.UTF_8);
        final byte[] iconBytes = "deep-fake-ico".getBytes(StandardCharsets.UTF_8);

        server.createContext("/", exchange -> {
            try {
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    counters.headRequests.incrementAndGet();
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                counters.rootRequests.incrementAndGet();
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, rootHtmlBytes.length);
                writeResponse(exchange, rootHtmlBytes);
            } finally {
                exchange.close();
            }
        });

        for (String childPath : childPaths) {
            final AtomicInteger childCounter = new AtomicInteger();
            counters.childRequests.put(childPath, childCounter);
            final byte[] childHtmlBytes = ("<html><head><title>" + childPath + "</title></head>"
                    + "<body><p>phone 18666677777 from " + childPath + "</p></body></html>").getBytes(StandardCharsets.UTF_8);
            server.createContext(childPath, exchange -> {
                try {
                    childCounter.incrementAndGet();
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(200, -1);
                        return;
                    }
                    exchange.sendResponseHeaders(200, childHtmlBytes.length);
                    writeResponse(exchange, childHtmlBytes);
                } finally {
                    exchange.close();
                }
            });
        }

        server.createContext("/favicon.ico", exchange -> {
            try {
                counters.iconRequests.incrementAndGet();
                exchange.getResponseHeaders().add("Content-Type", "image/x-icon");
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, -1);
                    return;
                }
                exchange.sendResponseHeaders(200, iconBytes.length);
                writeResponse(exchange, iconBytes);
            } finally {
                exchange.close();
            }
        });

        server.start();
        return counters;
    }

    private String buildRootHtml(String... childPaths) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>Deep Root</title><link rel=\"icon\" href=\"/favicon.ico\" /></head><body>");
        sb.append("<p>root phone 18600001111</p>");
        for (String childPath : childPaths) {
            sb.append("<a href=\"").append(childPath).append("\">").append(childPath).append("</a>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private void writeResponse(HttpExchange exchange, byte[] bytes) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(bytes);
        outputStream.flush();
    }

    private static final class DeepServerCounters {
        private final AtomicInteger headRequests = new AtomicInteger();
        private final AtomicInteger rootRequests = new AtomicInteger();
        private final AtomicInteger iconRequests = new AtomicInteger();
        private final Map<String, AtomicInteger> childRequests = new LinkedHashMap<>();

        private int getTotalChildRequests() {
            int total = 0;
            for (AtomicInteger value : childRequests.values()) {
                total += value.get();
            }
            return total;
        }
    }
}

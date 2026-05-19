package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PocExecutor WebSocket 执行链测试")
class PocExecutorWebSocketExecutionTest {

    private EchoWebSocketServer server;

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop(1000);
            server = null;
        }
    }

    @Test
    @DisplayName("应完成本机 WebSocket 握手、消息发送与响应 matcher")
    void shouldExecuteWebSocketStepAgainstLocalServer() throws Exception {
        server = new EchoWebSocketServer();
        server.start();
        assertTrue(server.awaitStart(), "本机 WebSocket 服务未及时启动");

        PocObj.WebSocketStep step = new PocObj.WebSocketStep();
        step.setAddress("ws://127.0.0.1:" + server.getPort() + "/echo");
        step.setMessages(Arrays.asList("hello", "scan"));
        step.setMatchers(Collections.singletonList(wordMatcher("body", "echo:hello\necho:scan")));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);

        boolean matched = invokeExecute(step, new HashMap<String, Object>());

        assertTrue(matched);
        assertTrue(server.getOpenCount() >= 1, "应完成握手");
        assertTrue(server.awaitMessages(2), "应向本机服务发送两条消息");
    }

    @Test
    @DisplayName("响应不匹配时应返回 false 而不是报成功")
    void shouldReturnFalseWhenWebSocketMatcherMisses() throws Exception {
        server = new EchoWebSocketServer();
        server.start();
        assertTrue(server.awaitStart(), "本机 WebSocket 服务未及时启动");

        PocObj.WebSocketStep step = new PocObj.WebSocketStep();
        step.setAddress("ws://127.0.0.1:" + server.getPort() + "/echo");
        step.setMessages(Collections.singletonList("hello"));
        step.setMatchers(Collections.singletonList(wordMatcher("body", "missing-token")));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);

        boolean matched = invokeExecute(step, new HashMap<String, Object>());

        assertFalse(matched);
        assertTrue(server.awaitMessages(1), "即使 matcher 不命中也应完成真实发送");
    }

    @Test
    @DisplayName("连接失败时应返回 false")
    void shouldReturnFalseWhenWebSocketConnectionFails() throws Exception {
        PocObj.WebSocketStep step = new PocObj.WebSocketStep();
        step.setAddress("ws://127.0.0.1:1/unreachable");
        step.setMessages(Collections.singletonList("hello"));
        step.setMatchers(Collections.singletonList(wordMatcher("body", "hello")));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);

        boolean matched = invokeExecute(step, new HashMap<String, Object>());

        assertFalse(matched);
    }

    private boolean invokeExecute(PocObj.WebSocketStep step, Map<String, Object> variables) throws Exception {
        PocExecutor executor = new PocExecutor(new ScanConfig());
        Method method = PocExecutor.class.getDeclaredMethod("executeWebSocketStep", PocObj.WebSocketStep.class, Map.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(executor, step, variables);
    }

    private PocObj.Matcher wordMatcher(String part, String value) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart(part);
        matcher.setValues(Collections.singletonList(value));
        matcher.setCondition("AND");
        return matcher;
    }

    private static final class EchoWebSocketServer extends WebSocketServer {
        private final CountDownLatch startLatch = new CountDownLatch(1);
        private final AtomicInteger openCount = new AtomicInteger();
        private final AtomicInteger messageCount = new AtomicInteger();

        private EchoWebSocketServer() {
            super(new InetSocketAddress("127.0.0.1", 0));
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            openCount.incrementAndGet();
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            conn.send("echo:" + message);
            messageCount.incrementAndGet();
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
        }

        @Override
        public void onStart() {
            setConnectionLostTimeout(0);
            setConnectionLostTimeout(1);
            startLatch.countDown();
        }

        private boolean awaitStart() throws InterruptedException {
            return startLatch.await(3, TimeUnit.SECONDS);
        }

        private boolean awaitMessages(int expected) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                if (messageCount.get() >= expected) {
                    return true;
                }
                Thread.sleep(50L);
            }
            return messageCount.get() >= expected;
        }

        private int boundPort() {
            return getAddress().getPort();
        }

        private int getOpenCount() {
            return openCount.get();
        }
    }
}

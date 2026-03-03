package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PocExecutor 流程与步骤语义测试")
public class PocExecutorFlowExecutionTest {

    private static HttpServer server;
    private static String baseUrl;
    private static AtomicInteger continueCounter;
    private static AtomicInteger pitchforkCounter;
    private static AtomicInteger batteringramCounter;
    private static AtomicInteger shortCircuitCounter;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/flow/s1", exchange -> write(exchange, 200, "step-1-ok"));
        server.createContext("/flow/s2", exchange -> write(exchange, 200, "step-2-ok"));

        server.createContext("/fallback/s1", exchange -> write(exchange, 404, "fallback-step-1-fail"));
        server.createContext("/fallback/s2", exchange -> write(exchange, 500, "fallback-step-2-fail"));
        server.createContext("/fallback/s3", exchange -> write(exchange, 200, "fallback-step-3-pass"));

        server.createContext("/or/1/fail", exchange -> write(exchange, 404, "fail"));
        server.createContext("/or/2/fail", exchange -> write(exchange, 404, "fail"));
        server.createContext("/or/1/pass", exchange -> write(exchange, 200, "pass"));
        server.createContext("/or/2/pass", exchange -> write(exchange, 200, "pass"));

        continueCounter = new AtomicInteger(0);
        server.createContext("/continue/check", exchange -> {
            int count = continueCounter.incrementAndGet();
            write(exchange, 200, "continue-ok-" + count);
        });

        pitchforkCounter = new AtomicInteger(0);
        server.createContext("/pitchfork", exchange -> {
            int count = pitchforkCounter.incrementAndGet();
            write(exchange, 200, "pitchfork-hit-" + count);
        });

        batteringramCounter = new AtomicInteger(0);
        server.createContext("/batteringram", exchange -> {
            int count = batteringramCounter.incrementAndGet();
            write(exchange, 200, "batteringram-hit-" + count);
        });

        shortCircuitCounter = new AtomicInteger(0);
        server.createContext("/flow/shortcircuit", exchange -> {
            int count = shortCircuitCounter.incrementAndGet();
            write(exchange, 200, "shortcircuit-hit-" + count);
        });

        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("简单 flow 应按 flow 模式执行")
    void testSimpleFlowExecutionMode() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-simple-test");
        poc.setName("flow-simple-test");
        poc.setProtocol("http");
        poc.setFlow("http(1) && http(2)");

        PocObj.PocStep s1 = step("http_1", "/flow/s1", statusMatcher(200));
        PocObj.PocStep s2 = step("http_2", "/flow/s2", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(s1, s2));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "简单 flow 应命中漏洞");
        assertEquals("flow", result.getDetails().get("flowExecutionMode"), "应标记为 flow 执行模式");
    }

    @Test
    @DisplayName("复杂 flow 应回退并标记 flow-fallback")
    void testComplexFlowFallbackMode() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-fallback-test");
        poc.setName("flow-fallback-test");
        poc.setProtocol("http");
        poc.setFlow("(http(1) && http(2)) || http(3)");
        poc.setStepsCondition(PocObj.MatchersCondition.OR);

        PocObj.PocStep s1 = step("http_1", "/fallback/s1", statusMatcher(200));
        PocObj.PocStep s2 = step("http_2", "/fallback/s2", statusMatcher(200));
        PocObj.PocStep s3 = step("http_3", "/fallback/s3", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(s1, s2, s3));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "复杂 flow 回退后应按 OR 命中第三步");
        assertEquals("flow-fallback", result.getDetails().get("flowExecutionMode"), "应标记为 flow-fallback");
    }

    @Test
    @DisplayName("payload 组合路径应遵循 stepsCondition=OR")
    void testPayloadCombinationRespectsStepsConditionOr() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("payload-or-condition-test");
        poc.setName("payload-or-condition-test");
        poc.setProtocol("http");
        poc.setStepsCondition(PocObj.MatchersCondition.OR);
        poc.setVariablesType(PocObj.VariablesType.clusterbomb);

        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("v", Arrays.asList("1", "2"));
        poc.setVariables(variables);

        PocObj.PocStep failStep = step("http_1", "/or/{{v}}/fail", statusMatcher(200));
        PocObj.PocStep passStep = step("http_2", "/or/{{v}}/pass", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(failStep, passStep));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "变量组合执行时应保留 OR 语义，任一步命中即可");
        assertEquals("stepsCondition", result.getDetails().get("flowExecutionMode"), "无 flow 时应标记 stepsCondition 模式");
    }

    @Test
    @DisplayName("continueOnMatch=true 时 clusterbomb 应继续尝试后续组合")
    void testContinueOnMatchForClusterbomb() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("continue-on-match-clusterbomb-test");
        poc.setName("continue-on-match-clusterbomb-test");
        poc.setProtocol("http");
        poc.setVariablesType(PocObj.VariablesType.clusterbomb);
        poc.setContinueOnMatch(true);

        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("a", Arrays.asList("1", "2"));
        variables.put("b", Arrays.asList("x", "y"));
        poc.setVariables(variables);

        PocObj.PocStep step = step("http_1", "/continue/check?a={{a}}&b={{b}}", statusMatcher(200));
        poc.setVerifySteps(Collections.singletonList(step));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "应至少命中一次");
        assertEquals("stepsCondition", result.getDetails().get("flowExecutionMode"));
        assertEquals(4, continueCounter.get(), "continueOnMatch=true 时 clusterbomb 应执行全部组合");
    }

    @Test
    @DisplayName("pitchfork 组合应可执行并命中")
    void testPitchforkCombinationExecution() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("pitchfork-execution-test");
        poc.setName("pitchfork-execution-test");
        poc.setProtocol("http");
        poc.setVariablesType(PocObj.VariablesType.pitchfork);

        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("u", Arrays.asList("u1", "u2"));
        variables.put("p", Arrays.asList("p1", "p2"));
        poc.setVariables(variables);

        PocObj.PocStep step = step("http_1", "/pitchfork?u={{u}}&p={{p}}", statusMatcher(200));
        poc.setVerifySteps(Collections.singletonList(step));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "pitchfork 模式应命中");
        assertEquals(1, pitchforkCounter.get(), "默认命中即停时应在首个组合即停止");
    }

    @Test
    @DisplayName("batteringram 组合应可执行并命中")
    void testBatteringramCombinationExecution() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("batteringram-execution-test");
        poc.setName("batteringram-execution-test");
        poc.setProtocol("http");
        poc.setVariablesType(PocObj.VariablesType.batteringram);

        Map<String, java.util.List<String>> variables = new HashMap<>();
        variables.put("k1", Arrays.asList("v1", "v2"));
        variables.put("k2", Arrays.asList("v1", "v2"));
        poc.setVariables(variables);

        PocObj.PocStep step = step("http_1", "/batteringram?k1={{k1}}&k2={{k2}}", statusMatcher(200));
        poc.setVerifySteps(Collections.singletonList(step));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "batteringram 模式应命中");
        assertEquals(1, batteringramCounter.get(), "默认命中即停时应在首个组合即停止");
    }

    @Test
    @DisplayName("flow OR 组内 AND 语义应正确执行")
    void testFlowOrWithAndGroupSemantics() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-or-and-group-test");
        poc.setName("flow-or-and-group-test");
        poc.setProtocol("http");
        poc.setFlow("http(1) && http(2) || http(3)");

        PocObj.PocStep s1 = step("http_1", "/fallback/s1", statusMatcher(200));
        PocObj.PocStep s2 = step("http_2", "/flow/s2", statusMatcher(200));
        PocObj.PocStep s3 = step("http_3", "/flow/s1", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(s1, s2, s3));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "应按 (s1&&s2)||s3 语义命中 s3");
        assertEquals("flow", result.getDetails().get("flowExecutionMode"), "该表达式应走 flow 模式而不是 fallback");
    }

    @Test
    @DisplayName("flow OR 命中首组后应短路后续组")
    void testFlowOrShortCircuitAfterFirstGroupMatch() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-or-shortcircuit-test");
        poc.setName("flow-or-shortcircuit-test");
        poc.setProtocol("http");
        poc.setFlow("http(1) || http(2)");

        PocObj.PocStep s1 = step("http_1", "/flow/s1", statusMatcher(200));
        PocObj.PocStep s2 = step("http_2", "/flow/shortcircuit", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(s1, s2));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "首组命中时应整体命中");
        assertEquals(0, shortCircuitCounter.get(), "OR 短路应避免执行后续组");
    }

    @Test
    @DisplayName("flow token 大小写混合应可归一化匹配 stepId")
    void testFlowTokenNormalizationWithMixedCase() throws Exception {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("flow-token-normalize-test");
        poc.setName("flow-token-normalize-test");
        poc.setProtocol("http");
        poc.setFlow("HTTP(1) && HTTP(2)");

        PocObj.PocStep s1 = step("http_1", "/flow/s1", statusMatcher(200));
        PocObj.PocStep s2 = step("http_2", "/flow/s2", statusMatcher(200));
        poc.setVerifySteps(Arrays.asList(s1, s2));

        PocExecutor executor = new PocExecutor(new ScanConfig());
        ScanResult result = executor.execute(baseUrl, poc);

        assertTrue(result.isVulnerable(), "大小写混合 token 应能命中对应步骤");
        assertEquals("flow", result.getDetails().get("flowExecutionMode"));
    }

    private static PocObj.PocStep step(String stepId, String path, PocObj.Matcher matcher) {
        PocObj.PocStep step = new PocObj.PocStep();
        step.setStepId(stepId);
        step.setMethod("GET");
        step.setPath(path);
        step.setMatchers(Collections.singletonList(matcher));
        step.setMatchersCondition(PocObj.MatchersCondition.AND);
        return step;
    }

    private static PocObj.Matcher statusMatcher(int code) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.STATUS);
        matcher.setValues(Collections.singletonList(String.valueOf(code)));
        return matcher;
    }

    private static void write(com.sun.net.httpserver.HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

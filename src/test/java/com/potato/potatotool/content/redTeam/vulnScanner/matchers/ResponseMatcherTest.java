package com.potato.potatotool.content.redTeam.vulnScanner.matchers;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.ResponseCache;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ResponseMatcher 索引变量测试")
public class ResponseMatcherTest {

    private static class MockCustomHttpResponse extends CustomHttpResponse {
        private final int responseCode;
        private final int initialResponseCode;
        private final String body;
        private final String headers;
        private final long responseTime;

        MockCustomHttpResponse(int responseCode, String body) {
            super(null);
            this.responseCode = responseCode;
            this.initialResponseCode = responseCode;
            this.body = body;
            this.headers = "Content-Type: text/plain";
            this.responseTime = 50L;
        }

        MockCustomHttpResponse(int initialResponseCode, int responseCode, String body) {
            super(null);
            this.responseCode = responseCode;
            this.initialResponseCode = initialResponseCode;
            this.body = body;
            this.headers = "Content-Type: text/plain";
            this.responseTime = 50L;
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public int getInitialResponseCode() {
            return initialResponseCode;
        }

        @Override
        public String getTextStr() {
            return body;
        }

        @Override
        public String getHeaderFieldsText() {
            return headers;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            Map<String, List<String>> headerMap = new HashMap<>();
            headerMap.put("Content-Type", Collections.singletonList("text/plain"));
            return headerMap;
        }

        @Override
        public URL getURL() {
            try {
                return new URL("http://127.0.0.1/mock");
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public byte[] getByteArray() {
            return body != null ? body.getBytes() : new byte[0];
        }

        @Override
        public long getResponseTime() {
            return responseTime;
        }
    }

    @Test
    @DisplayName("跟随重定向后状态 matcher 应按最终响应码判定")
    public void testStatusMatcherShouldUseFinalResponseCodeAfterRedirect() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(302, 200, "redirected-body");

        PocObj.Matcher statusMatcher = new PocObj.Matcher();
        statusMatcher.setType(PocObj.MatcherType.STATUS);
        statusMatcher.setValues(Collections.singletonList("200"));

        boolean matched = ResponseMatcher.matchResponse(
                response,
                Collections.singletonList(statusMatcher),
                PocObj.MatchersCondition.AND
        );

        assertTrue(matched, "开启重定向后的状态匹配应按最终 200 判定，而不是初始 302");
    }

    @Test
    @DisplayName("DSL 应支持 body_1/body_2 索引变量")
    public void testDslIndexedVariablesWithStepId() {
        ResponseCache cache = new ResponseCache();
        MockCustomHttpResponse response1 = new MockCustomHttpResponse(200, "first-token");
        MockCustomHttpResponse response2 = new MockCustomHttpResponse(201, "second-token");
        cache.putMultipleResponses("http_1", Arrays.asList(response1, response2));

        PocObj.Matcher dslMatcher = new PocObj.Matcher();
        dslMatcher.setType(PocObj.MatcherType.DSL);
        dslMatcher.setValues(Collections.singletonList("contains(body_1, 'first-token') && contains(body_2, 'second-token')"));

        boolean matched = ResponseMatcher.matchResponse(
                response2,
                Collections.singletonList(dslMatcher),
                PocObj.MatchersCondition.AND,
                cache,
                "http_1"
        );

        assertTrue(matched, "应能通过 stepId 命中多响应索引变量");
    }

    @Test
    @DisplayName("错误 stepId 不应命中索引变量")
    public void testDslIndexedVariablesWithWrongStepId() {
        ResponseCache cache = new ResponseCache();
        MockCustomHttpResponse response1 = new MockCustomHttpResponse(200, "first-token");
        MockCustomHttpResponse response2 = new MockCustomHttpResponse(201, "second-token");
        cache.putMultipleResponses("http_1", Arrays.asList(response1, response2));

        PocObj.Matcher dslMatcher = new PocObj.Matcher();
        dslMatcher.setType(PocObj.MatcherType.DSL);
        dslMatcher.setValues(Collections.singletonList("contains(body_1, 'first-token') && contains(body_2, 'second-token')"));

        boolean matched = ResponseMatcher.matchResponse(
                response2,
                Collections.singletonList(dslMatcher),
                PocObj.MatchersCondition.AND,
                cache,
                "http_2"
        );

        assertFalse(matched, "错误 stepId 不应命中索引缓存变量");
    }

    @Test
    @DisplayName("DSL 应支持运行期变量上下文")
    public void testDslVariablesFromPocRuntimeContext() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(200, "ok");

        PocObj.Matcher dslMatcher = new PocObj.Matcher();
        dslMatcher.setType(PocObj.MatcherType.DSL);
        dslMatcher.setValues(Collections.singletonList(
                "contains(interactsh_protocol, 'dns') && contains(interactsh_request, 'lookup.example')"
        ));

        Map<String, Object> variables = new HashMap<>();
        variables.put("interactsh_protocol", "dns");
        variables.put("interactsh_request", "lookup.example");

        boolean matched = ResponseMatcher.matchResponse(
                response,
                Collections.singletonList(dslMatcher),
                PocObj.MatchersCondition.AND,
                null,
                "http_1",
                variables
        );

        assertTrue(matched, "Nuclei DSL matcher 应能读取运行期/OOB 变量上下文");
    }

    @Test
    @DisplayName("Goby 三花括号变量应可参与 body contains 匹配")
    public void testGobyTripleBraceVariableInWordMatcher() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(200, "052cf7e6dbf1cbce57073beca1b792d5");

        PocObj.Matcher wordMatcher = new PocObj.Matcher();
        wordMatcher.setType(PocObj.MatcherType.WORD);
        wordMatcher.setPart("body");
        wordMatcher.setOperation(PocObj.OperationType.CONTAINS);
        wordMatcher.setValues(Collections.singletonList("{{{md1}}}"));

        Map<String, Object> variables = new HashMap<>();
        variables.put("md1", "@@md5(r1)");
        variables.put("r1", "y8AKBK2a");

        boolean matched = ResponseMatcher.matchResponse(
                response,
                Collections.singletonList(wordMatcher),
                PocObj.MatchersCondition.AND,
                null,
                "http_1",
                variables
        );

        assertTrue(matched, "Goby 三花括号变量应在 matcher 中完成替换和函数求值");
    }

    @Test
    @DisplayName("单个 matcher 内 condition=AND 时多值必须全部命中")
    public void testSingleMatcherAndConditionRequiresAllValues() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(200, "alpha beta");

        PocObj.Matcher wordMatcher = new PocObj.Matcher();
        wordMatcher.setType(PocObj.MatcherType.WORD);
        wordMatcher.setPart("body");
        wordMatcher.setValues(Arrays.asList("alpha", "gamma"));
        wordMatcher.setCondition("AND");

        boolean matched = ResponseMatcher.matchResponse(
                response,
                Collections.singletonList(wordMatcher),
                PocObj.MatchersCondition.AND
        );

        assertFalse(matched, "Nuclei condition=and 应要求同一 matcher 的所有值都命中");
    }

    @Test
    @DisplayName("单个 matcher 默认/OR 时多值任一命中即可")
    public void testSingleMatcherDefaultConditionKeepsOrSemantics() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(200, "alpha beta");

        PocObj.Matcher wordMatcher = new PocObj.Matcher();
        wordMatcher.setType(PocObj.MatcherType.WORD);
        wordMatcher.setPart("body");
        wordMatcher.setValues(Arrays.asList("alpha", "gamma"));

        boolean matched = ResponseMatcher.matchResponse(
                response,
                Collections.singletonList(wordMatcher),
                PocObj.MatchersCondition.AND
        );

        assertTrue(matched, "默认多值 matcher 应保持 OR 语义，兼容 Goby/Pocsuite 单值路径");
    }

    @Test
    @DisplayName("Nuclei HTTP xpath matcher 应按 XPath 查询结果命中")
    public void testNucleiXpathMatcherShouldMatchHtmlDocument() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(
                200,
                "<html><head><title>Meduza Stealer</title></head><body></body></html>"
        );

        PocObj.Matcher xpathMatcher = new PocObj.Matcher();
        xpathMatcher.setType(PocObj.MatcherType.XPATH);
        xpathMatcher.setPart("body");
        xpathMatcher.setValues(Collections.singletonList(
                "/html/head/title[contains(text(), 'Meduza Stealer')]"
        ));

        boolean matched = ResponseMatcher.matchResponse(
                response,
                Collections.singletonList(xpathMatcher),
                PocObj.MatchersCondition.AND
        );

        assertTrue(matched, "Nuclei xpath matcher 查询到节点时应命中");
    }

    @Test
    @DisplayName("Nuclei HTTP xpath matcher condition=AND 应要求所有 XPath 命中")
    public void testNucleiXpathMatcherAndConditionRequiresAllValues() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(
                200,
                "<html><head><title>Meduza Stealer</title></head><body></body></html>"
        );

        PocObj.Matcher xpathMatcher = new PocObj.Matcher();
        xpathMatcher.setType(PocObj.MatcherType.XPATH);
        xpathMatcher.setPart("body");
        xpathMatcher.setCondition("AND");
        xpathMatcher.setValues(Arrays.asList(
                "/html/head/title[contains(text(), 'Meduza Stealer')]",
                "//script[contains(@src, '//')]"
        ));

        boolean matched = ResponseMatcher.matchResponse(
                response,
                Collections.singletonList(xpathMatcher),
                PocObj.MatchersCondition.AND
        );

        assertFalse(matched, "Nuclei xpath matcher condition=and 应要求所有 XPath 表达式都命中");
    }

    @Test
    @DisplayName("状态码 matcher 应支持 Goby 的不等于比较")
    public void testStatusMatcherNotEqualOperation() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(200, "uid=0(root)");

        PocObj.Matcher statusMatcher = new PocObj.Matcher();
        statusMatcher.setType(PocObj.MatcherType.STATUS);
        statusMatcher.setPart("status");
        statusMatcher.setOperation(PocObj.OperationType.NOT_EQUAL);
        statusMatcher.setValues(Collections.singletonList("204"));

        boolean matched = ResponseMatcher.matchResponse(
                response,
                Collections.singletonList(statusMatcher),
                PocObj.MatchersCondition.AND
        );

        assertTrue(matched, "状态码不等于 204 时应命中 Goby NOT_EQUAL 语义");
    }

    @Test
    @DisplayName("Xray CEL 应支持 response.body_string.contains 链式方法调用")
    public void testXrayCelBodyStringContainsMethodCall() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(
                200,
                "<?xml version=\"1.0\"?><web-app><display-name>htoa</display-name></web-app>"
        );

        PocObj.Matcher celMatcher = new PocObj.Matcher();
        celMatcher.setType(PocObj.MatcherType.CEL);
        celMatcher.setValues(Collections.singletonList(
                "response.status == 200 && response.body_string.contains(\"web-app\") && response.body_string.contains(\"xml\")"
        ));

        boolean matched = ResponseMatcher.matchResponse(
                response,
                Collections.singletonList(celMatcher),
                PocObj.MatchersCondition.AND
        );

        assertTrue(matched, "Xray response.body_string.contains(...) 应能正确求值");
    }

    @Test
    @DisplayName("Xray CEL 应支持字符串字面量 bmatches 调用")
    public void testXrayCelStringLiteralBmatchesMethodCall() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(
                200,
                "root:x:0:0:root:/root:/bin/bash\ndaemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin"
        );

        PocObj.Matcher celMatcher = new PocObj.Matcher();
        celMatcher.setType(PocObj.MatcherType.CEL);
        celMatcher.setValues(Collections.singletonList(
                "response.status == 200 && \"root:.*?:[0-9]*:[0-9]*:\".bmatches(response.body)"
        ));

        boolean matched = ResponseMatcher.matchResponse(
                response,
                Collections.singletonList(celMatcher),
                PocObj.MatchersCondition.AND
        );

        assertTrue(matched, "Xray \"regex\".bmatches(response.body) 应能正确求值");
    }
}

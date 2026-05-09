package com.potato.potatotool.content.redTeam.vulnScanner.matchers;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.ResponseCache;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ResponseMatcher 索引变量测试")
public class ResponseMatcherTest {

    private static class MockCustomHttpResponse extends CustomHttpResponse {
        private final int responseCode;
        private final String body;
        private final String headers;
        private final long responseTime;

        MockCustomHttpResponse(int responseCode, String body) {
            super(null);
            this.responseCode = responseCode;
            this.body = body;
            this.headers = "Content-Type: text/plain";
            this.responseTime = 50L;
        }

        @Override
        public int getResponseCode() {
            return responseCode;
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
        public byte[] getByteArray() {
            return body != null ? body.getBytes() : new byte[0];
        }

        @Override
        public long getResponseTime() {
            return responseTime;
        }
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
}

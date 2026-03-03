package com.potato.potatotool.content.redTeam.vulnScanner.matchers;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.ResponseCache;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

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
}

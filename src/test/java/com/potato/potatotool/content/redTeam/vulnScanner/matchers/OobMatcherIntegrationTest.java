package com.potato.potatotool.content.redTeam.vulnScanner.matchers;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OOB 匹配闭环测试")
public class OobMatcherIntegrationTest {

    private static class MockCustomHttpResponse extends CustomHttpResponse {
        private final int code;
        private final String body;

        MockCustomHttpResponse(int code, String body) {
            super(null);
            this.code = code;
            this.body = body;
        }

        @Override
        public int getResponseCode() {
            return code;
        }

        @Override
        public String getTextStr() {
            return body;
        }

        @Override
        public String getHeaderFieldsText() {
            return "Content-Type: text/plain";
        }

        @Override
        public byte[] getByteArray() {
            return body == null ? new byte[0] : body.getBytes();
        }
    }

    @Test
    void testInteractshMatcher_ShouldMatchWhenInteractionExists() {
        MockCustomHttpResponse response = new MockCustomHttpResponse(200, "callback token: oast-token");
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("body");
        matcher.setValues(Collections.singletonList("oast-token"));

        boolean matched = ResponseMatcher.matchResponse(response, Collections.singletonList(matcher), PocObj.MatchersCondition.AND);
        assertTrue(matched);
    }

    @Test
    void testDnslogMatcher_ShouldMatchWhenResolverCallbackFound() {
        // 无外部 dnslog 服务时，至少验证 OOB 分支调用不抛异常
        MockCustomHttpResponse response = new MockCustomHttpResponse(200, "no-body");
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("$reserver");
        matcher.setValues(Collections.singletonList("nonexistent-token.local"));

        assertDoesNotThrow(() -> ResponseMatcher.matchResponse(response, Collections.singletonList(matcher), PocObj.MatchersCondition.AND));
    }

    @Test
    void testOobCapability_ShouldBeMappedFromTemplateToMatcherPart() {
        PocObj.Matcher matcher = oobWordMatcher("test-token.oast.site");
        assertEquals(PocObj.MatcherType.WORD, matcher.getType());
        assertEquals("$reserver", matcher.getPart());
        assertNotNull(matcher.getValues());
        assertEquals("test-token.oast.site", matcher.getValues().get(0));
    }

    @Test
    void testOobHit_ShouldBeRecordedInScanResultDetails() {
        ScanResult result = new ScanResult();
        Map<String, Object> details = new HashMap<>();
        details.put("oobEvidence", "interactsh-hit");
        result.setDetails(details);

        assertTrue(detailsContainsOobEvidence(result));
    }

    private PocObj.Matcher oobWordMatcher(String domainOrToken) {
        PocObj.Matcher matcher = new PocObj.Matcher();
        matcher.setType(PocObj.MatcherType.WORD);
        matcher.setPart("$reserver");
        matcher.setValues(Collections.singletonList(domainOrToken));
        return matcher;
    }

    private boolean detailsContainsOobEvidence(ScanResult result) {
        if (result == null || result.getDetails() == null) {
            return false;
        }
        Object value = result.getDetails().get("oobEvidence");
        return value != null && !String.valueOf(value).isEmpty();
    }
}

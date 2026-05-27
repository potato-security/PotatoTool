package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("请求聚合签名测试")
class ClusteredPocExecutorSignatureTest {

    @Test
    @DisplayName("显式 UA 不同的请求不应聚合")
    void shouldKeepExplicitUserAgentInClusterSignature() throws Exception {
        String keyA = clusterKey(buildPoc("ua-a", "/same", "Agent-A"));
        String keyB = clusterKey(buildPoc("ua-b", "/same", "Agent-B"));

        assertNotNull(keyA);
        assertNotNull(keyB);
        assertNotEquals(keyA, keyB);
    }

    @Test
    @DisplayName("内置动态 UA/UUID 不应导致同构请求无法聚合")
    void shouldNormalizeBuiltinDynamicHeadersInClusterSignature() throws Exception {
        String keyA = clusterKey(buildPoc("builtin-a", "/same", null));
        String keyB = clusterKey(buildPoc("builtin-b", "/same", null));

        assertNotNull(keyA);
        assertNotNull(keyB);
        assertEquals(keyA, keyB);
    }

    @Test
    @DisplayName("查询参数不同的请求不应聚合")
    void shouldIncludeQueryStringInClusterSignature() throws Exception {
        String keyA = clusterKey(buildPoc("query-a", "/same?a=1", null));
        String keyB = clusterKey(buildPoc("query-b", "/same?a=2", null));

        assertNotNull(keyA);
        assertNotNull(keyB);
        assertNotEquals(keyA, keyB);
    }

    @Test
    @DisplayName("Cookie 差异的请求不应聚合")
    void shouldSplitClusterSignatureWhenCookieDiffers() throws Exception {
        String keyA = clusterKey(buildPocWithCookie("cookie-a", "/same", "a=1"));
        String keyB = clusterKey(buildPocWithCookie("cookie-b", "/same", null));

        assertNotNull(keyA);
        assertNotNull(keyB);
        assertNotEquals(keyA, keyB);
    }

    private String clusterKey(PocObj.Poc poc) throws Exception {
        ClusteredPocExecutor executor = new ClusteredPocExecutor(new ScanConfig(), new PocExecutor(new ScanConfig()));
        Method method = ClusteredPocExecutor.class.getDeclaredMethod("generateClusterKey", PocObj.Poc.class);
        method.setAccessible(true);
        return (String) method.invoke(executor, poc);
    }

    private PocObj.Poc buildPoc(String id, String path, String userAgent) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setProtocol("http");
        poc.setSeverity(PocObj.Severity.INFO);

        PocObj.PocStep step = new PocObj.PocStep();
        step.setMethod("GET");
        step.setPath(path);
        if (userAgent != null) {
            step.setHeaders(Collections.singletonMap("User-Agent", userAgent));
        }

        poc.setVerifySteps(Collections.singletonList(step));
        return poc;
    }

    private PocObj.Poc buildPocWithCookie(String id, String path, String cookie) {
        PocObj.Poc poc = buildPoc(id, path, null);
        poc.getVerifySteps().get(0).setCookie(cookie);
        return poc;
    }
}

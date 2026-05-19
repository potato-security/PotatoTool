package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.utils.network.RequestObj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("PocExecutor 认证配置注入测试")
class PocExecutorAuthenticationConfigTest {

    @Test
    @DisplayName("Basic Auth 应写入 Authorization 请求头并支持变量替换")
    void shouldApplyBasicAuthHeader() throws Exception {
        RequestObj requestObj = new RequestObj();
        requestObj.setHeaders(new LinkedHashMap<String, String>());

        Map<String, String> authConfig = new HashMap<String, String>();
        authConfig.put("type", "basic");
        authConfig.put("username", "{{{user}}}");
        authConfig.put("password", "{{{pass}}}");

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("user", "admin");
        variables.put("pass", "p@ssw0rd");

        invokeApplyAuthenticationConfig(requestObj, authConfig, variables);

        assertEquals("Basic YWRtaW46cEBzc3cwcmQ=", requestObj.getHeaders().get("Authorization"));
    }

    @Test
    @DisplayName("Bearer Token 应覆盖 Authorization 请求头")
    void shouldApplyBearerAuthHeader() throws Exception {
        RequestObj requestObj = new RequestObj();
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Basic stale");
        requestObj.setHeaders(headers);

        Map<String, String> authConfig = new HashMap<String, String>();
        authConfig.put("type", "bearer");
        authConfig.put("token", "{{{token}}}");

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("token", "jwt-123");

        invokeApplyAuthenticationConfig(requestObj, authConfig, variables);

        assertEquals("Bearer jwt-123", requestObj.getHeaders().get("Authorization"));
    }

    @Test
    @DisplayName("Cookie 认证应与已有 Cookie 头合并")
    void shouldMergeCookieAuthIntoExistingCookieHeader() throws Exception {
        RequestObj requestObj = new RequestObj();
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Cookie", "session=base");
        requestObj.setHeaders(headers);

        Map<String, String> authConfig = new HashMap<String, String>();
        authConfig.put("type", "cookie");
        authConfig.put("cookies", "{\"tenant\":\"blue\",\"token\":\"{{{cookieToken}}}\"}");

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("cookieToken", "abc123");

        invokeApplyAuthenticationConfig(requestObj, authConfig, variables);

        assertEquals("session=base; tenant=blue; token=abc123", requestObj.getHeaders().get("Cookie"));
    }

    @Test
    @DisplayName("认证配置在请求头为空时也应初始化 Header 容器")
    void shouldCreateHeadersWhenRequestHeadersAreMissing() throws Exception {
        RequestObj requestObj = new RequestObj();
        requestObj.setHeaders(null);

        Map<String, String> authConfig = new HashMap<String, String>();
        authConfig.put("type", "bearer");
        authConfig.put("token", "plain-token");

        invokeApplyAuthenticationConfig(requestObj, authConfig, new HashMap<String, Object>());

        assertNotNull(requestObj.getHeaders());
        assertEquals("Bearer plain-token", requestObj.getHeaders().get("Authorization"));
    }

    private void invokeApplyAuthenticationConfig(RequestObj requestObj,
                                                 Map<String, String> authConfig,
                                                 Map<String, Object> variables) throws Exception {
        PocExecutor executor = new PocExecutor(new ScanConfig());
        Method method = PocExecutor.class.getDeclaredMethod(
                "applyAuthenticationConfig", RequestObj.class, Map.class, Map.class);
        method.setAccessible(true);
        method.invoke(executor, requestObj, authConfig, variables);
    }
}

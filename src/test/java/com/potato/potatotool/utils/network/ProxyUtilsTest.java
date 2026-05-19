package com.potato.potatotool.utils.network;

import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProxyUtils 代理治理测试")
class ProxyUtilsTest {

    private String originalUserHome;

    @AfterEach
    void tearDown() {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
            originalUserHome = null;
        }
        Constants.cachedConfig = null;
    }

    @Test
    @DisplayName("空地址返回格式错误")
    void shouldReturnInvalidAddressWhenProxyAddressIsBlank() {
        ProxyUtils.ProxyReachabilityResult result = ProxyUtils.checkProxyAddressReachability("   ", 1000);

        assertFalse(result.isReachable());
        assertEquals("setting.proxy.unavailable.invalid.address", result.getReasonKey());
    }

    @Test
    @DisplayName("无端口地址返回格式错误")
    void shouldReturnInvalidAddressWhenProxyAddressHasNoPort() {
        ProxyUtils.ProxyReachabilityResult result = ProxyUtils.checkProxyAddressReachability("http://127.0.0.1", 1000);

        assertFalse(result.isReachable());
        assertEquals("setting.proxy.unavailable.invalid.address", result.getReasonKey());
    }

    @Test
    @DisplayName("非法端口返回格式错误")
    void shouldReturnInvalidAddressWhenProxyPortIsIllegal() {
        ProxyUtils.ProxyReachabilityResult result = ProxyUtils.checkProxyAddressReachability("http://127.0.0.1:70000", 1000);

        assertFalse(result.isReachable());
        assertEquals("setting.proxy.unavailable.invalid.address", result.getReasonKey());
    }

    @Test
    @DisplayName("原因键为空时回退为通用错误")
    void shouldFallbackToGenericReasonKey() {
        String reasonKey = ProxyUtils.getReasonKey(new ProxyUtils.ProxyReachabilityResult(false, null));
        assertEquals("setting.proxy.unavailable.generic", reasonKey);
    }

    @Test
    @DisplayName("可达结果保留空原因键")
    void shouldKeepReachableResultWithoutReason() {
        ProxyUtils.ProxyReachabilityResult result = new ProxyUtils.ProxyReachabilityResult(true, null);

        assertTrue(result.isReachable());
        assertEquals("setting.proxy.unavailable.generic", ProxyUtils.getReasonKey(null));
    }

    @Test
    @DisplayName("不可解析主机返回 host 原因键")
    void shouldReturnHostReasonWhenProxyHostCannotResolve() {
        ProxyUtils.ProxyReachabilityResult result = ProxyUtils.checkProxyAddressReachability("http://not-exists-host.invalid:8080", 1000);

        assertFalse(result.isReachable());
        assertEquals("setting.proxy.unavailable.host", result.getReasonKey());
    }

    @Test
    @DisplayName("带认证信息的代理地址应允许通过格式校验")
    void shouldAllowProxyAddressWithCredentials() {
        ProxyUtils.ProxyReachabilityResult result = ProxyUtils.checkProxyAddressReachability("http://user:pass@127.0.0.1:1", 10);

        assertFalse(result.isReachable());
        assertTrue(!"setting.proxy.unavailable.invalid.address".equals(result.getReasonKey()));
    }

    @Test
    @DisplayName("合并单服务代理状态时保留其他服务")
    void shouldKeepOtherServiceStatesWhenMergingSingleService() {
        java.util.LinkedHashMap<String, Object> current = new java.util.LinkedHashMap<String, Object>();
        current.put("AI", true);
        current.put("Fofa_Key", false);

        java.util.LinkedHashMap<String, Object> merged = ProxyUtils.mergeServiceProxyStates(current, "VulnScan", true);

        assertEquals(3, merged.size());
        assertEquals(true, merged.get("AI"));
        assertEquals(false, merged.get("Fofa_Key"));
        assertEquals(true, merged.get("VulnScan"));
    }

    @Test
    @DisplayName("合并服务状态时允许空当前状态")
    void shouldCreateNewServiceStateMapWhenCurrentStatesIsNull() {
        java.util.LinkedHashMap<String, Object> merged = ProxyUtils.mergeServiceProxyStates(null, "VulnScan", true);

        assertEquals(1, merged.size());
        assertEquals(true, merged.get("VulnScan"));
    }

    @Test
    @DisplayName("空结果文案构造回退为通用错误")
    void shouldBuildGenericUnavailableMessageWhenResultIsNull() {
        String message = ProxyUtils.buildUnavailableMessage(null);

        assertTrue(message.contains("代理不可用"));
        assertTrue(message.contains("网络异常"));
    }

    @Test
    @DisplayName("服务代理关闭时清空请求代理")
    void shouldClearRequestProxyWhenServiceProxyDisabled() {
        RequestObj requestObj = new RequestObj().setProxies("http://127.0.0.1:8080");

        ProxyUtils.applyProxy(requestObj, false);

        assertNull(requestObj.getProxies());
    }

    @Test
    @DisplayName("缺失 Proxy 节点时 RequestObj 默认构造不应抛异常")
    void shouldAllowRequestObjWithoutProxyNode(@TempDir Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        String json = "{\n" +
                "  \"" + ConfigConstants.HTTP_HEADERS + "\": {\n" +
                "    \"" + ConfigConstants.HTTP_HEADERS_GLOBAL + "\": {\n" +
                "      \"X-Test\": \"true\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Files.write(configDir.resolve("config.json"), json.getBytes(StandardCharsets.UTF_8));
        Constants.cachedConfig = null;

        RequestObj requestObj = new RequestObj();

        assertNotNull(requestObj);
        assertNull(requestObj.getProxies());
    }
}

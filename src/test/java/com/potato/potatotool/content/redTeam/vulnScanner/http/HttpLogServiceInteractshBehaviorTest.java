package com.potato.potatotool.content.redTeam.vulnScanner.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HttpLogService Interactsh 行为测试")
class HttpLogServiceInteractshBehaviorTest {

    @AfterEach
    void tearDown() throws Exception {
        HttpLogService.clearCache();
        HttpLogService.closeClient();
        setStaticField(HttpLogService.class, "interactshServer", "oast.pro");
        setStaticField(HttpLogService.class, "interactshToken", null);
        setStaticField(HttpLogService.class, "interactshClient", null);
    }

    @Test
    @DisplayName("注册失败时 testInteractshConnectivity 应返回失败")
    void shouldReportFailureWhenClientRegistrationFails() throws Exception {
        FakeInteractshClient fakeClient = new FakeInteractshClient(true, false, null);
        setStaticField(HttpLogService.class, "interactshClient", fakeClient);

        HttpLogService.TestResult result = HttpLogService.testInteractshConnectivity();

        assertFalse(result.success);
        assertTrue(result.message.contains("连接失败") || result.message.contains("注册失败"));
    }

    @Test
    @DisplayName("已注册客户端应返回成功和可用URL")
    void shouldReportSuccessWhenClientAlreadyRegistered() throws Exception {
        FakeInteractshClient fakeClient = new FakeInteractshClient(false, true, "fixed-correlation.oast.test");
        setStaticField(HttpLogService.class, "interactshClient", fakeClient);
        setStaticField(HttpLogService.class, "interactshServer", "oast.test");

        HttpLogService.TestResult result = HttpLogService.testInteractshConnectivity();

        assertTrue(result.success);
        assertTrue(result.url.contains("fixed-correlation.oast.test"));
    }

    @Test
    @DisplayName("callback host 提取应支持带协议和裸域名")
    void shouldExtractBareCallbackHost() {
        assertEquals("abc.oast.test", HttpLogService.toBareCallbackHost("http://abc.oast.test/path"));
        assertEquals("abc.oast.test", HttpLogService.toBareCallbackHost("https://abc.oast.test:443/path"));
        assertEquals("abc.oast.test", HttpLogService.toBareCallbackHost("abc.oast.test"));
    }

    @Test
    @DisplayName("Interactsh 空轮询数据不应作为错误输出")
    void shouldTreatEmptyPollDataAsNoInteraction() throws Exception {
        FakeInteractshClient fakeClient = new FakeInteractshClient(false, true, "fixed-correlation.oast.test");
        Method method = InteractshClient.class.getDeclaredMethod("parseInteractionsArray", String.class);
        method.setAccessible(true);

        Object empty = method.invoke(fakeClient, "");
        Object jsonNull = method.invoke(fakeClient, "null");

        assertEquals(0, ((com.google.gson.JsonArray) empty).size());
        assertEquals(0, ((com.google.gson.JsonArray) jsonNull).size());
    }

    private void setStaticField(Class<?> type, String fieldName, Object value) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static class FakeInteractshClient extends InteractshClient {
        private final boolean failOnRegister;
        private final boolean registered;
        private final String interactionUrl;

        FakeInteractshClient(boolean failOnRegister, boolean registered, String interactionUrl) {
            super("oast.test");
            this.failOnRegister = failOnRegister;
            this.registered = registered;
            this.interactionUrl = interactionUrl;
        }

        @Override
        public synchronized String register() throws Exception {
            if (failOnRegister) {
                throw new Exception("simulated register failure");
            }
            return interactionUrl;
        }

        @Override
        public boolean isRegistered() {
            return registered;
        }

        @Override
        public String getInteractionUrl() {
            return interactionUrl;
        }
    }
}

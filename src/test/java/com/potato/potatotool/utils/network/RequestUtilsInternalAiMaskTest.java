package com.potato.potatotool.utils.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RequestUtils 内置AI脱敏测试")
class RequestUtilsInternalAiMaskTest {

    @Test
    @DisplayName("internalAiRequest=true 时脱敏 URL 和主机")
    void maskInternalAiAddressWhenInternalRequest() throws Exception {
        RequestObj requestObj = new RequestObj()
                .setUrl("http://127.0.0.1:50003/stream")
                .setInternalAiRequest(true);

        Method method = RequestUtils.class.getDeclaredMethod("maskInternalAiAddress", String.class, boolean.class);
        method.setAccessible(true);

        String input = "[×] 请求失败: http://127.0.0.1:50003/stream, 错误: Failed to connect to /127.0.0.1:50003";
        String output = (String) method.invoke(null, input, requestObj.isInternalAiRequest());

        assertTrue(output.contains("内置AI服务地址"));
        assertTrue(output.contains("内置AI服务主机"));
        assertTrue(!output.contains("127.0.0.1:50003"));
    }

    @Test
    @DisplayName("internalAiRequest=false 时保持原文")
    void keepOriginalWhenNotInternalRequest() throws Exception {
        RequestObj requestObj = new RequestObj()
                .setUrl("https://api.example.com/v1/chat/completions")
                .setInternalAiRequest(false);

        Method method = RequestUtils.class.getDeclaredMethod("maskInternalAiAddress", String.class, boolean.class);
        method.setAccessible(true);

        String input = "[×] 请求失败: https://api.example.com/v1/chat/completions, 错误: Failed to connect to /api.example.com:443";
        String output = (String) method.invoke(null, input, requestObj.isInternalAiRequest());

        assertEquals(input, output);
    }
}

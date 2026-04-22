package com.potato.potatotool.controller.publicPane;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("PaneSetting AI Base URL 保存前校验测试")
class PaneSettingAiBaseUrlValidationTest {

    @Test
    @DisplayName("留空时允许保存以便继续使用内置配置")
    void shouldAllowBlankBaseUrl() {
        assertNull(PaneSettingSupport.validateAiBaseUrl(""));
        assertNull(PaneSettingSupport.validateAiBaseUrl("   "));
        assertNull(PaneSettingSupport.validateAiBaseUrl(null));
    }

    @Test
    @DisplayName("完整 HTTP 或 HTTPS 地址允许保存")
    void shouldAllowFullHttpUrl() {
        assertNull(PaneSettingSupport.validateAiBaseUrl("https://api.openai.com/v1"));
        assertNull(PaneSettingSupport.validateAiBaseUrl("http://127.0.0.1:3000/v1/chat/completions"));
    }

    @Test
    @DisplayName("相对地址在保存阶段直接拦截")
    void shouldRejectRelativeUrl() {
        assertEquals("setting.ai.api.base.invalid", PaneSettingSupport.validateAiBaseUrl("/v1"));
        assertEquals("setting.ai.api.base.invalid", PaneSettingSupport.validateAiBaseUrl("v1/chat/completions"));
    }

    @Test
    @DisplayName("非 HTTP 协议地址在保存阶段直接拦截")
    void shouldRejectNonHttpScheme() {
        assertEquals("setting.ai.api.base.invalid", PaneSettingSupport.validateAiBaseUrl("ftp://example.com/v1"));
    }
}

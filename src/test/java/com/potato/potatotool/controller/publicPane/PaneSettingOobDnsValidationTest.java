package com.potato.potatotool.controller.publicPane;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaneSetting OOB DNS测试前置校验")
class PaneSettingOobDnsValidationTest {

    @Test
    @DisplayName("CEYE平台缺少 identifier 或 token 时应阻断测试")
    void shouldBlockWhenCeyeMissingRequiredFields() {
        assertTrue(PaneSetting.shouldBlockDnsCeyeTest("CEYE_IO", "", "token"));
        assertTrue(PaneSetting.shouldBlockDnsCeyeTest("CEYE_IO", "identifier", ""));
        assertTrue(PaneSetting.shouldBlockDnsCeyeTest("CEYE_IO", " ", "token"));
        assertTrue(PaneSetting.shouldBlockDnsCeyeTest("CEYE_IO", "identifier", null));
    }

    @Test
    @DisplayName("validateDnsCeye 返回对应文案键")
    void validateDnsCeyeShouldReturnExpectedMessageKey() {
        assertTrue(PaneSetting.validateDnsCeye("CEYE_IO", "", "token", false)
                .equals("setting.oob.dns.ceye.identifier.required"));
        assertTrue(PaneSetting.validateDnsCeye("CEYE_IO", "identifier", "", false)
                .equals("setting.oob.dns.ceye.token.required"));
        assertTrue(PaneSetting.validateDnsCeye("CEYE_IO", "", "token", true)
                .equals("setting.oob.dns.test.ceye.missing"));
        assertTrue(PaneSetting.validateDnsCeye("DNSLOG_CN", "", "", true) == null);
    }
}

package com.potato.potatotool.controller.publicPane;

import com.potato.potatotool.utils.network.ProxyUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaneSetting 主代理保存前校验测试")
class PaneSettingProxyValidationTest {

    @Test
    @DisplayName("主代理关闭时不阻断保存")
    void shouldNotBlockSaveWhenProxyDisabled() {
        boolean blocked = PaneSetting.shouldBlockSaveForMainProxy(
                false,
                "http://127.0.0.1:8080",
                new ProxyUtils.ProxyReachabilityResult(false, "setting.proxy.unavailable.refused")
        );

        assertFalse(blocked);
    }

    @Test
    @DisplayName("主代理开启且代理不可用时阻断保存")
    void shouldBlockSaveWhenProxyEnabledButUnavailable() {
        boolean blocked = PaneSetting.shouldBlockSaveForMainProxy(
                true,
                "http://127.0.0.1:8080",
                new ProxyUtils.ProxyReachabilityResult(false, "setting.proxy.unavailable.refused")
        );

        assertTrue(blocked);
    }

    @Test
    @DisplayName("主代理开启且代理可用时允许保存")
    void shouldAllowSaveWhenProxyEnabledAndReachable() {
        boolean blocked = PaneSetting.shouldBlockSaveForMainProxy(
                true,
                "http://127.0.0.1:8080",
                new ProxyUtils.ProxyReachabilityResult(true, null)
        );

        assertFalse(blocked);
    }
}

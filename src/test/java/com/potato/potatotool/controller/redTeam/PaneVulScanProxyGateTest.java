package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.utils.network.ProxyUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaneVulScan 代理阻断测试")
class PaneVulScanProxyGateTest {

    @Test
    @DisplayName("主代理关闭时不阻断扫描")
    void shouldNotBlockScanWhenMainProxyDisabled() {
        boolean blocked = PaneVulScanSupport.shouldBlockScanForProxy(
                false,
                true,
                "http://127.0.0.1:8080",
                new ProxyUtils.ProxyReachabilityResult(false, "setting.proxy.unavailable.refused")
        );

        assertFalse(blocked);
    }

    @Test
    @DisplayName("漏扫代理关闭时不阻断扫描")
    void shouldNotBlockScanWhenVulnScanProxyDisabled() {
        boolean blocked = PaneVulScanSupport.shouldBlockScanForProxy(
                true,
                false,
                "http://127.0.0.1:8080",
                new ProxyUtils.ProxyReachabilityResult(false, "setting.proxy.unavailable.refused")
        );

        assertFalse(blocked);
    }

    @Test
    @DisplayName("代理地址缺失时阻断扫描")
    void shouldBlockScanWhenProxyAddressMissing() {
        boolean blocked = PaneVulScanSupport.shouldBlockScanForProxy(
                true,
                true,
                "   ",
                new ProxyUtils.ProxyReachabilityResult(true, null)
        );

        assertTrue(blocked);
    }

    @Test
    @DisplayName("代理地址协议不合法时阻断扫描")
    void shouldBlockScanWhenProxySchemaInvalid() {
        boolean blocked = PaneVulScanSupport.shouldBlockScanForProxy(
                true,
                true,
                "127.0.0.1:8080",
                new ProxyUtils.ProxyReachabilityResult(true, null)
        );

        assertTrue(blocked);
    }

    @Test
    @DisplayName("代理不可达时阻断扫描")
    void shouldBlockScanWhenProxyUnreachable() {
        boolean blocked = PaneVulScanSupport.shouldBlockScanForProxy(
                true,
                true,
                "http://127.0.0.1:8080",
                new ProxyUtils.ProxyReachabilityResult(false, "setting.proxy.unavailable.refused")
        );

        assertTrue(blocked);
    }

    @Test
    @DisplayName("代理可用时允许扫描")
    void shouldAllowScanWhenProxyReachable() {
        boolean blocked = PaneVulScanSupport.shouldBlockScanForProxy(
                true,
                true,
                "http://127.0.0.1:8080",
                new ProxyUtils.ProxyReachabilityResult(true, null)
        );

        assertFalse(blocked);
    }
}

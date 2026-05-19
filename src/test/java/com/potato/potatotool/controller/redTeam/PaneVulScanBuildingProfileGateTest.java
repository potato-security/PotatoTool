package com.potato.potatotool.controller.redTeam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaneVulScan 楼栋剖面门禁测试")
class PaneVulScanBuildingProfileGateTest {

    @Test
    @DisplayName("缺少全局 Header 配置时应阻断扫描")
    void shouldBlockWhenHeadersMissing() {
        assertTrue(PaneVulScanSupport.shouldBlockScanForBuildingProfile(null));
        assertTrue(PaneVulScanSupport.shouldBlockScanForBuildingProfile(Collections.<String, String>emptyMap()));
    }

    @Test
    @DisplayName("缺少楼栋标识时应阻断扫描")
    void shouldBlockWhenBuildingProfileMissing() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("X-Scan-Window", "light-baseline");

        assertTrue(PaneVulScanSupport.shouldBlockScanForBuildingProfile(headers));
    }

    @Test
    @DisplayName("缺少扫描窗口时应阻断扫描")
    void shouldBlockWhenScanWindowMissing() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("X-Building-Profile", "A-Building");

        assertTrue(PaneVulScanSupport.shouldBlockScanForBuildingProfile(headers));
    }

    @Test
    @DisplayName("楼栋标识和扫描窗口齐全时允许扫描")
    void shouldAllowWhenBuildingProfileAndWindowPresent() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("X-Building-Profile", "B-Building");
        headers.put("X-Scan-Window", "proxy-regression");

        assertFalse(PaneVulScanSupport.shouldBlockScanForBuildingProfile(headers));
    }
}

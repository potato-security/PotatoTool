package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("InputTypeDetector 目标输入覆盖测试")
class InputTypeDetectorCoverageTest {

    @Test
    @DisplayName("应识别 URL、IP:PORT、域名和本地路径")
    void shouldDetectCommonInputTypes() {
        assertEquals(PocObj.InputType.URL, InputTypeDetector.detect("https://example.com/app"));
        assertEquals(PocObj.InputType.IP_PORT, InputTypeDetector.detect("127.0.0.1:8443"));
        assertEquals(PocObj.InputType.DOMAIN, InputTypeDetector.detect("portal.internal.example.com"));
        assertEquals(PocObj.InputType.LOCAL_PATH, InputTypeDetector.detect("./target/test-classes"));
    }

    @Test
    @DisplayName("空输入、非法 URL、纯 IP、带端口域名和带路径主机应落入当前规则")
    void shouldNormalizeEdgeInputsByCurrentRules() {
        assertEquals(PocObj.InputType.URL, InputTypeDetector.detect(""));
        assertEquals(PocObj.InputType.URL, InputTypeDetector.detect("bad url"));
        assertEquals(PocObj.InputType.URL, InputTypeDetector.detect("192.168.1.10"));
        assertEquals(PocObj.InputType.IP_PORT, InputTypeDetector.detect("example.com:8080"));
        assertEquals(PocObj.InputType.URL, InputTypeDetector.detect("example.com/path"));
    }

    @Test
    @DisplayName("URL 与本地路径规范化应按类型补齐或展开")
    void shouldNormalizeTargets() {
        String normalizedUrl = InputTypeDetector.normalizeTarget("example.com", PocObj.InputType.URL);
        String normalizedHomePath = InputTypeDetector.normalizeTarget("~/demo", PocObj.InputType.LOCAL_PATH);

        assertEquals("http://example.com", normalizedUrl);
        assertEquals(System.getProperty("user.home") + "/demo", normalizedHomePath);
    }
}

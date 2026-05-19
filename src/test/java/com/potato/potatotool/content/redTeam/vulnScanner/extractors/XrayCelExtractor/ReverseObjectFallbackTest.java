package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReverseObject mock/fallback 行为测试")
class ReverseObjectFallbackTest {

    @AfterEach
    void tearDown() {
        DnsLogService.setPlatform(DnsLogService.Platform.DNSLOG_CN);
        DnsLogService.setMockMode(false);
        DnsLogService.clearCache();
    }

    @Test
    @DisplayName("mock 模式下 newReverse 应生成可用 mock 域名")
    void testMockModeAvailabilityAndBehavior() {
        DnsLogService.setMockMode(true);
        DnsLogService.clearCache();
        DnsLogService.setPlatform(DnsLogService.Platform.CEYE_IO);
        DnsLogService.configureCeye("", "");

        ReverseObject reverseObject = new ReverseObject();

        assertNotNull(reverseObject.getDomain());
        assertTrue(reverseObject.isAvailable());
        assertTrue(reverseObject.getDomain().endsWith(".dnslog.mock"));
        assertFalse(DnsLogService.isFallbackDomain(reverseObject.getDomain()));
        assertFalse(reverseObject.waitFor(1));
        assertFalse(reverseObject.hasCallback());
        assertNull(reverseObject.getRecords());
    }

    @Test
    @DisplayName("mock 模式记录回连后 ReverseObject 应检测到回调")
    void testMockModeRecordedResolutionShouldBeObservable() {
        DnsLogService.setMockMode(true);
        DnsLogService.clearCache();

        ReverseObject reverseObject = new ReverseObject();
        DnsLogService.recordMockResolution(reverseObject.getDomain());

        assertTrue(reverseObject.isAvailable());
        assertTrue(reverseObject.hasCallback());
        assertNotNull(reverseObject.getRecords());
        assertTrue(reverseObject.waitFor(1));
    }
}

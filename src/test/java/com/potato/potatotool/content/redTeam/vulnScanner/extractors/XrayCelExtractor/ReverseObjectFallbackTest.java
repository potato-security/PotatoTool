package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReverseObject fallback 行为测试")
class ReverseObjectFallbackTest {

    @Test
    @DisplayName("DNSLog fallback 场景下直接不可用")
    void testFallbackAvailabilityAndBehavior() {
        DnsLogService.setMockMode(true);
        DnsLogService.clearCache();
        DnsLogService.setPlatform(DnsLogService.Platform.CEYE_IO);
        DnsLogService.configureCeye("", "");

        ReverseObject reverseObject = new ReverseObject();

        assertFalse(reverseObject.isAvailable());
        assertTrue(DnsLogService.isFallbackDomain(reverseObject.getDomain()));
        assertFalse(reverseObject.waitFor(1));
        assertFalse(reverseObject.hasCallback());
        assertNull(reverseObject.getRecords());

        DnsLogService.setPlatform(DnsLogService.Platform.DNSLOG_CN);
        DnsLogService.setMockMode(false);
    }
}

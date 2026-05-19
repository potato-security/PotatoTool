package com.potato.potatotool.content.redTeam.vulnScanner.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DnsLogService 域名可用性判定测试")
class DnsLogServiceDomainAvailabilityTest {

    @Test
    @DisplayName("fallback 域名判定")
    void testFallbackDomainDetection() {
        assertTrue(DnsLogService.isFallbackDomain("abc123.dnslog.example.com"));
        assertTrue(DnsLogService.isFallbackDomain("ABC123.DNSLOG.EXAMPLE.COM"));

        assertFalse(DnsLogService.isFallbackDomain("abc123.dnslog.cn"));
        assertFalse(DnsLogService.isFallbackDomain("abc123.test.ceye.io"));
        assertFalse(DnsLogService.isFallbackDomain(""));
        assertFalse(DnsLogService.isFallbackDomain(null));
    }

    @Test
    @DisplayName("真实 DNSLog 域名判定")
    void testRealDnsLogDomainDetection() {
        assertTrue(DnsLogService.isRealDnsLogDomain("abc123.dnslog.cn"));
        assertTrue(DnsLogService.isRealDnsLogDomain("abc123.test.ceye.io"));

        assertFalse(DnsLogService.isRealDnsLogDomain("abc123.dnslog.example.com"));
        assertFalse(DnsLogService.isRealDnsLogDomain("   "));
        assertFalse(DnsLogService.isRealDnsLogDomain(null));
    }

    @Test
    @DisplayName("同一 session 下基础域名与子域名都应命中缓存映射")
    void testBaseDomainAndSubDomainShouldReuseSameSessionMapping() throws Exception {
        DnsLogService.clearCache();

        Class<?> infoClass = Class.forName(DnsLogService.class.getName() + "$DnsLogInfo");
        Constructor<?> constructor = infoClass.getDeclaredConstructor(String.class, String.class, DnsLogService.Platform.class);
        constructor.setAccessible(true);
        Object info = constructor.newInstance("base.dnslog.cn", "session-1", DnsLogService.Platform.DNSLOG_CN);

        Method cacheMethod = DnsLogService.class.getDeclaredMethod("cacheDnslogDomain", infoClass, String.class);
        cacheMethod.setAccessible(true);
        cacheMethod.invoke(null, info, "base.dnslog.cn");
        cacheMethod.invoke(null, info, "sub.base.dnslog.cn");

        Method findMethod = DnsLogService.class.getDeclaredMethod("findDnsLogInfo", String.class);
        findMethod.setAccessible(true);
        Object baseInfo = findMethod.invoke(null, "base.dnslog.cn");
        Object subInfo = findMethod.invoke(null, "sub.base.dnslog.cn");

        assertTrue(baseInfo == subInfo, "基础域名与子域名应映射到同一个 session 信息");

        Field sessionIdField = infoClass.getDeclaredField("sessionId");
        sessionIdField.setAccessible(true);
        assertTrue("session-1".equals(sessionIdField.get(baseInfo)));
    }

    @Test
    @DisplayName("未知 dnslog 域名不回退到当前 session")
    void testUnknownDnslogDomainDoesNotFallbackToCurrentSession() {
        DnsLogService.setMockMode(false);
        DnsLogService.clearCache();
        DnsLogService.setPlatform(DnsLogService.Platform.DNSLOG_CN);

        assertNull(DnsLogService.queryDnsLogRecords("unknown.dnslog.cn"));
    }

    @Test
    @DisplayName("mock 模式只返回已显式触发的解析记录")
    void testMockModeOnlyReturnsRecordedResolution() {
        DnsLogService.setMockMode(true);
        DnsLogService.clearCache();

        String domain = "abc123.dnslog.mock";
        assertNull(DnsLogService.queryDnsLogRecords(domain));
        assertFalse(DnsLogService.hasDnsResolution(domain));

        DnsLogService.recordMockResolution(domain);

        assertTrue(DnsLogService.hasDnsResolution(domain));
        assertTrue(DnsLogService.queryDnsLogRecords(domain).contains(domain));

        DnsLogService.setMockMode(false);
        DnsLogService.clearCache();
    }
}

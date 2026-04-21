package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.cdn.CdnChecker;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DomainInfoMergerRegressionTest {

    @Test
    public void mergeDomainInfos_shouldSupportDomainArray() {
        JsonObject row = new JsonObject();
        row.addProperty("ip", "1.1.1.1");
        row.addProperty("port", 443);

        JsonArray domains = new JsonArray();
        domains.add("a.example.com");
        domains.add("b.example.com");
        row.add("domain", domains);

        JsonArray input = new JsonArray();
        input.add(row);

        primeCdnCache("1.1.1.1", "a.example.com\nb.example.com");
        List<DomainInfo> merged = DomainInfoMerger.mergeDomainInfos("unit-test", input);

        assertEquals(1, merged.size());
        assertEquals("a.example.com\nb.example.com", merged.get(0).getDomain());
    }

    @Test
    public void mergeDomainInfoList_shouldKeepDifferentVirtualHostsOnSameIpPort() {
        List<DomainInfo> items = new ArrayList<>();
        items.add(buildDomainInfo("1.1.1.1", "443", "a.example.com", null, null, "fofa"));
        items.add(buildDomainInfo("1.1.1.1", "443", "b.example.com", null, null, "quake"));

        DomainInfoMerger.mergeDomainInfoList(items);

        assertEquals(2, items.size());
        assertEquals("a.example.com", items.get(0).getDomain());
        assertEquals("b.example.com", items.get(1).getDomain());
    }

    @Test
    public void mergeDomainInfoList_shouldMergeSameAssetAndCombineSources() {
        List<DomainInfo> items = new ArrayList<>();

        DomainInfo first = buildDomainInfo("2.2.2.2", "443", "portal.example.com", "portal.example.com", "https://portal.example.com", "fofa");
        first.setTitle("Portal");
        items.add(first);

        DomainInfo second = buildDomainInfo("2.2.2.2", "443", null, "portal.example.com", "https://portal.example.com/login", "hunter");
        second.setStatusCode("200");
        items.add(second);

        DomainInfoMerger.mergeDomainInfoList(items);

        assertEquals(1, items.size());
        DomainInfo merged = items.get(0);
        assertEquals("Portal", merged.getTitle());
        assertEquals("200", merged.getStatusCode());
        assertEquals("fofa\nhunter", merged.getDataSource());
    }

    @Test
    public void getUniqueDomainInfoInB_shouldKeepNewVirtualHostOnSharedIp() {
        List<DomainInfo> existing = new ArrayList<>();
        existing.add(buildDomainInfo("3.3.3.3", "443", "known.example.com", null, null, "fofa"));

        List<DomainInfo> candidates = new ArrayList<>();
        candidates.add(buildDomainInfo("3.3.3.3", "443", "new-vhost.example.com", null, null, "hunter"));

        List<DomainInfo> unique = DomainInfoMerger.getUniqueDomainInfoInB(existing, candidates);

        assertEquals(1, unique.size());
        assertEquals("new-vhost.example.com", unique.get(0).getDomain());
    }

    @Test
    public void getUniqueDomainInfoInB_shouldFilterExactSameAsset() {
        List<DomainInfo> existing = new ArrayList<>();
        existing.add(buildDomainInfo("4.4.4.4", "8443", "same.example.com", "same.example.com", "https://same.example.com", "fofa"));

        List<DomainInfo> candidates = new ArrayList<>();
        candidates.add(buildDomainInfo("4.4.4.4", "8443", null, "same.example.com", "https://same.example.com/admin", "hunter"));

        List<DomainInfo> unique = DomainInfoMerger.getUniqueDomainInfoInB(existing, candidates);

        assertNotNull(unique);
        assertEquals(0, unique.size());
    }

    @Test
    public void mergeDomainInfoList_shouldPreserveShadowMetadata() {
        List<DomainInfo> items = new ArrayList<>();

        DomainInfo first = buildDomainInfo("5.5.5.5", "443", "shadow.example.com", "shadow.example.com", "https://shadow.example.com", "fofa");
        items.add(first);

        DomainInfo second = buildDomainInfo("5.5.5.5", "443", null, "shadow.example.com", "https://shadow.example.com/login", "hunter");
        second.setShadowAsset(true);
        second.setShadowScore(88);
        second.setShadowMatchedAlias("中交建");
        second.setShadowReasons(new ArrayList<String>());
        second.getShadowReasons().add("标题命中别名");
        items.add(second);

        DomainInfoMerger.mergeDomainInfoList(items);

        assertEquals(1, items.size());
        assertTrue(items.get(0).isShadowAsset());
        assertEquals(Integer.valueOf(88), items.get(0).getShadowScore());
        assertEquals("中交建", items.get(0).getShadowMatchedAlias());
        assertTrue(items.get(0).getShadowReasons().contains("标题命中别名"));
    }

    private static DomainInfo buildDomainInfo(String ip, String port, String domain, String host, String url, String dataSource) {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.setIp(ip);
        domainInfo.setPort(port);
        domainInfo.setDomain(domain);
        domainInfo.setHost(host);
        if (url != null) {
            domainInfo.setUrl(url);
        }
        domainInfo.setDataSource(dataSource);
        return domainInfo;
    }

    @SuppressWarnings("unchecked")
    private static void primeCdnCache(String ip, String domain) {
        try {
            Field ipCacheField = CdnChecker.class.getDeclaredField("ipCdnCache");
            ipCacheField.setAccessible(true);
            ((Map<String, Boolean>) ipCacheField.get(null)).put(ip, false);

            Field domainCacheField = CdnChecker.class.getDeclaredField("domainCdnCache");
            domainCacheField.setAccessible(true);
            ((Map<String, Boolean>) domainCacheField.get(null)).put(domain, false);
        } catch (Exception e) {
            throw new AssertionError("failed to prime CDN cache for tests", e);
        }
    }
}

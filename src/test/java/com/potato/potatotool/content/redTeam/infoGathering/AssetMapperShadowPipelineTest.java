package com.potato.potatotool.content.redTeam.infoGathering;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.cdn.CdnChecker;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ShadowAssetCandidate;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ShadowAssetContext;
import com.potato.potatotool.content.redTeam.infoGathering.tools.FofaSearch;
import com.potato.potatotool.content.redTeam.infoGathering.tools.HunterSearch;
import com.potato.potatotool.content.redTeam.infoGathering.tools.QuakeSearch;
import com.potato.potatotool.content.redTeam.infoGathering.tools.ShodanSearch;
import com.potato.potatotool.content.redTeam.infoGathering.tools.ZoomeyeSearch;
import com.potato.potatotool.content.redTeam.infoGathering.utils.ShadowAssetEvaluator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssetMapperShadowPipelineTest {

    @Test
    public void shadowPipeline_shouldMergeRepeatedRecallAcrossAliasesAndPlatforms() throws Exception {
        primeCdnCache("1.1.1.1", "shadow.example.com");

        AssetMapper assetMapper = buildAssetMapper(
                singleResultMap("中国交通建设股份有限公司", searchResult("1.1.1.1", "443", "shadow.example.com", "中国交建官网")),
                singleResultMap("中交建", searchResult("1.1.1.1", "443", "shadow.example.com", "中交建统一门户"))
        );

        @SuppressWarnings("unchecked")
        List<ShadowAssetCandidate> candidates = (List<ShadowAssetCandidate>) invokePrivate(
                assetMapper,
                "recallShadowAssetCandidates",
                new Class[]{Set.class},
                new LinkedHashSet<>(Arrays.asList("中交建", "中国交通建设股份有限公司"))
        );

        assertEquals(1, candidates.size());
        ShadowAssetCandidate candidate = candidates.get(0);
        assertEquals("shadow.example.com", candidate.getDomainInfo().getDomain());
        assertEquals(new LinkedHashSet<>(Arrays.asList("中国交通建设股份有限公司", "中交建")), candidate.getRecalledAliases());
        assertTrue(candidate.getDomainInfo().getDataSource().contains("泛型检索：中国交通建设股份有限公司"));
        assertTrue(candidate.getDomainInfo().getDataSource().contains("泛型检索：中交建"));
    }

    @Test
    public void shadowPipeline_shouldFilterExistingExactAssetButKeepNewVirtualHost() throws Exception {
        AssetMapper assetMapper = buildAssetMapper(emptyResultMap(), emptyResultMap());

        List<DomainInfo> existingAssets = new ArrayList<>();
        existingAssets.add(buildDomainInfo("2.2.2.2", "443", "known.example.com", "Known Portal", "known-md5"));

        List<ShadowAssetCandidate> candidates = new ArrayList<>();
        candidates.add(buildCandidate(buildDomainInfo("2.2.2.2", "443", "known.example.com", "Known Portal", "known-md5"), "中交建"));
        candidates.add(buildCandidate(buildDomainInfo("2.2.2.2", "443", "new-vhost.example.com", "New VHost", "new-md5"), "中交建"));

        @SuppressWarnings("unchecked")
        List<ShadowAssetCandidate> filtered = (List<ShadowAssetCandidate>) invokePrivate(
                assetMapper,
                "filterNewShadowAssetCandidates",
                new Class[]{List.class, List.class},
                existingAssets,
                candidates
        );

        assertEquals(1, filtered.size());
        assertEquals("new-vhost.example.com", filtered.get(0).getDomainInfo().getDomain());
    }

    @Test
    public void shadowPipeline_shouldEvaluateAcceptedAndRejectedCandidatesAndPreserveReasons() throws Exception {
        AssetMapper assetMapper = buildAssetMapper(emptyResultMap(), emptyResultMap());

        DomainInfo referenceDomain = buildDomainInfo("3.3.3.3", "443", "www.ccccltd.cn", "中国交建官网", "icon-match");

        @SuppressWarnings("unchecked")
        ShadowAssetContext context = (ShadowAssetContext) invokePrivate(
                assetMapper,
                "buildShadowAssetContext",
                new Class[]{String.class, Set.class, Set.class, Set.class, List.class},
                "中国交通建设股份有限公司",
                new LinkedHashSet<>(Arrays.asList("中国交通建设股份有限公司", "中交建")),
                new LinkedHashSet<>(Collections.singletonList("ccccltd.cn")),
                new LinkedHashSet<>(Collections.singletonList("京ICP备030173号")),
                Collections.singletonList(referenceDomain)
        );

        List<ShadowAssetCandidate> candidates = Arrays.asList(
                buildCandidate(buildDomainInfo("4.4.4.4", "443", "smart.example.com", "中交建智慧工地平台", "icon-match"), "中交建"),
                buildCandidate(buildDomainInfo("5.5.5.5", "443", "plain.example.com", "欢迎访问", "icon-other"), "中交建")
        );

        ShadowAssetEvaluator evaluator = new ShadowAssetEvaluator(false, new ShadowAssetEvaluator.AiReviewer() {
            @Override
            public boolean isRelevant(Map<String, Object> candidateWebBaseInfoMap, String companyPrompt, Map<String, Object> targetWebBaseInfoMap) {
                return false;
            }
        });

        @SuppressWarnings("unchecked")
        List<DomainInfo> accepted = (List<DomainInfo>) invokePrivate(
                assetMapper,
                "evaluateShadowAssetCandidates",
                new Class[]{List.class, ShadowAssetContext.class, ShadowAssetEvaluator.class},
                candidates,
                context,
                evaluator
        );

        assertEquals(1, accepted.size());
        DomainInfo acceptedDomain = accepted.get(0);
        assertEquals("smart.example.com", acceptedDomain.getDomain());
        assertTrue(acceptedDomain.isShadowAsset());
        assertEquals("中交建", acceptedDomain.getShadowMatchedAlias());
        assertTrue(acceptedDomain.getShadowReasons().contains("标题命中别名"));
        assertTrue(acceptedDomain.getShadowReasons().contains("图标哈希命中"));
        assertFalse(candidates.get(1).getDomainInfo().isShadowAsset());
    }

    @Test
    public void shadowPipeline_shouldHandleEmptyAliasSetWithoutPlatformCalls() throws Exception {
        StubFofaSearch fofaSearch = new StubFofaSearch(singleResultMap("中交建", searchResult("6.6.6.6", "443", "cancelled.example.com", "Cancelled")));

        AssetMapper assetMapper = new AssetMapper();
        setField(assetMapper, "fofaSearch", fofaSearch);
        setField(assetMapper, "hunterSearch", new StubHunterSearch(emptyResultMap()));
        setField(assetMapper, "quakeSearch", new StubQuakeSearch(emptyResultMap()));
        setField(assetMapper, "shodanSearch", new StubShodanSearch(emptyResultMap()));
        setField(assetMapper, "zoomeyeSearch", new StubZoomeyeSearch(emptyResultMap()));

        @SuppressWarnings("unchecked")
        List<ShadowAssetCandidate> candidates = (List<ShadowAssetCandidate>) invokePrivate(
                assetMapper,
                "recallShadowAssetCandidates",
                new Class[]{Set.class},
                new LinkedHashSet<String>()
        );

        assertTrue(candidates.isEmpty());
        assertEquals(0, fofaSearch.callCount);
    }

    @Test
    public void shadowPipeline_shouldClampEvaluateLimitBetween60And200() throws Exception {
        AssetMapper assetMapper = buildAssetMapper(emptyResultMap(), emptyResultMap());

        int lowerBound = (Integer) invokePrivate(assetMapper, "getShadowAssetEvaluateLimit", new Class[]{int.class}, 1);
        int upperBound = (Integer) invokePrivate(assetMapper, "getShadowAssetEvaluateLimit", new Class[]{int.class}, 100);
        int middle = (Integer) invokePrivate(assetMapper, "getShadowAssetEvaluateLimit", new Class[]{int.class}, 30);

        assertEquals(60, lowerBound);
        assertEquals(200, upperBound);
        assertEquals(90, middle);
    }

    private static AssetMapper buildAssetMapper(Map<String, JsonArray> primaryResults,
                                                Map<String, JsonArray> secondaryResults) throws Exception {
        AssetMapper assetMapper = new AssetMapper();
        setField(assetMapper, "fofaSearch", new StubFofaSearch(primaryResults));
        setField(assetMapper, "hunterSearch", new StubHunterSearch(secondaryResults));
        setField(assetMapper, "quakeSearch", new StubQuakeSearch(emptyResultMap()));
        setField(assetMapper, "shodanSearch", new StubShodanSearch(emptyResultMap()));
        setField(assetMapper, "zoomeyeSearch", new StubZoomeyeSearch(emptyResultMap()));
        return assetMapper;
    }

    private static ShadowAssetCandidate buildCandidate(DomainInfo domainInfo, String recalledAlias) {
        ShadowAssetCandidate candidate = new ShadowAssetCandidate(domainInfo);
        candidate.addRecalledAlias(recalledAlias);
        return candidate;
    }

    private static DomainInfo buildDomainInfo(String ip, String port, String domain, String title, String iconMd5) {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.setIp(ip);
        domainInfo.setPort(port);
        domainInfo.setDomain(domain);
        domainInfo.setTitle(title);
        Map<String, Object> webInfoMap = new HashMap<>();
        webInfoMap.put("title", title);
        webInfoMap.put("body", title == null ? "" : title + " 首页");
        webInfoMap.put("iconMd5", iconMd5);
        webInfoMap.put("iconUrl", "https://static.example.com/" + iconMd5 + ".ico");
        domainInfo.setWebInfoMap(webInfoMap);
        return domainInfo;
    }

    private static JsonArray searchResult(String ip, String port, String domain, String title) {
        JsonArray result = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("ip", ip);
        item.addProperty("port", port);
        item.addProperty("domain", domain);
        item.addProperty("title", title);
        result.add(item);
        return result;
    }

    private static Map<String, JsonArray> singleResultMap(String alias, JsonArray jsonArray) {
        Map<String, JsonArray> result = new HashMap<>();
        result.put(alias, jsonArray);
        return result;
    }

    private static Map<String, JsonArray> emptyResultMap() {
        return new HashMap<>();
    }

    private static Object invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = AssetMapper.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = AssetMapper.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static void primeCdnCache(String ip, String domain) throws Exception {
        Field ipCacheField = CdnChecker.class.getDeclaredField("ipCdnCache");
        ipCacheField.setAccessible(true);
        ((Map<String, Boolean>) ipCacheField.get(null)).put(ip, false);

        Field domainCacheField = CdnChecker.class.getDeclaredField("domainCdnCache");
        domainCacheField.setAccessible(true);
        ((Map<String, Boolean>) domainCacheField.get(null)).put(domain, false);
    }

    private static class StubFofaSearch extends FofaSearch {
        private final Map<String, JsonArray> responses;
        private int callCount = 0;

        private StubFofaSearch(Map<String, JsonArray> responses) {
            super("", false);
            this.responses = responses;
        }

        @Override
        public JsonArray getInfoByBodyFilterIcp_Fofa(String companyNameStr) {
            callCount++;
            return responses.getOrDefault(companyNameStr, new JsonArray());
        }
    }

    private static class StubHunterSearch extends HunterSearch {
        private final Map<String, JsonArray> responses;

        private StubHunterSearch(Map<String, JsonArray> responses) {
            super(new HashSet<>(Collections.singletonList("test")), false);
            this.responses = responses;
        }

        @Override
        public JsonArray getInfoByBodyFilterIcp_Hunter(String companyNameStr) {
            return responses.getOrDefault(companyNameStr, new JsonArray());
        }
    }

    private static class StubQuakeSearch extends QuakeSearch {
        private final Map<String, JsonArray> responses;

        private StubQuakeSearch(Map<String, JsonArray> responses) {
            super(new HashSet<>(Collections.singletonList("test")), false);
            this.responses = responses;
        }

        @Override
        public JsonArray getInfoByBodyFilteIcp_Quake(String companyNameStr) {
            return responses.getOrDefault(companyNameStr, new JsonArray());
        }
    }

    private static class StubShodanSearch extends ShodanSearch {
        private final Map<String, JsonArray> responses;

        private StubShodanSearch(Map<String, JsonArray> responses) {
            super("", false);
            this.responses = responses;
        }

        @Override
        public JsonArray getInfoByBodyFilterIcp_shodan(String companyNameStr) {
            return responses.getOrDefault(companyNameStr, new JsonArray());
        }
    }

    private static class StubZoomeyeSearch extends ZoomeyeSearch {
        private final Map<String, JsonArray> responses;

        private StubZoomeyeSearch(Map<String, JsonArray> responses) {
            super("", false);
            this.responses = responses;
        }

        @Override
        public JsonArray getInfoByBodyFilterIcp_zoomeye(String companyNameStr) {
            return responses.getOrDefault(companyNameStr, new JsonArray());
        }
    }
}

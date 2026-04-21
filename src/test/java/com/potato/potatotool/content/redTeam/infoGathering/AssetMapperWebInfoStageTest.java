package com.potato.potatotool.content.redTeam.infoGathering;

import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetObj;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssetMapperWebInfoStageTest {

    @Test
    public void initialWebCollection_shouldCollectOnlyBasicInfoAndLeaveFullPendingWhenDeepRequested() throws Exception {
        StageTrackingAssetMapper assetMapper = new StageTrackingAssetMapper();
        List<DomainInfo> domainInfos = Arrays.asList(
                domain("portal.example.com"),
                domain("api.example.com")
        );

        invokePrivate(assetMapper, "performInitialWebInfoCollection",
                new Class[]{List.class, AssetObj.class, boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class, int.class, String.class},
                domainInfos, buildAssetObj(), true, false, true, false, 2, 3, 20, ExecutorServiceManager.ExecutorPoolNames.ASSET);

        assertEquals(2, assetMapper.initialCalls.size());
        assertTrue(assetMapper.fullCalls.isEmpty());
        assertTrue(domainInfos.get(0).isDoWebInfoMap());
        assertFalse(domainInfos.get(0).isDoFullWebInfoMap());
        assertEquals("initial", domainInfos.get(0).getWebInfoMap().get("phase"));
        assertFalse(domainInfos.get(1).isDoFullWebInfoMap());
    }

    @Test
    public void finalWebCollection_shouldUpgradeAssetsEvenWhenBasicInfoAlreadyExists() throws Exception {
        StageTrackingAssetMapper assetMapper = new StageTrackingAssetMapper();
        List<DomainInfo> domainInfos = Arrays.asList(
                domain("portal.example.com"),
                domain("api.example.com")
        );
        AssetObj assetObj = buildAssetObj();

        invokePrivate(assetMapper, "performInitialWebInfoCollection",
                new Class[]{List.class, AssetObj.class, boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class, int.class, String.class},
                domainInfos, assetObj, true, false, true, false, 2, 3, 20, ExecutorServiceManager.ExecutorPoolNames.ASSET);
        invokePrivate(assetMapper, "performFinalWebInfoCollection",
                new Class[]{List.class, AssetObj.class, boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class, int.class, String.class},
                domainInfos, assetObj, true, false, true, false, 2, 3, 20, ExecutorServiceManager.ExecutorPoolNames.ASSET);

        assertEquals(2, assetMapper.initialCalls.size());
        assertEquals(2, assetMapper.fullCalls.size());
        assertTrue(domainInfos.get(0).isDoWebInfoMap());
        assertTrue(domainInfos.get(0).isDoFullWebInfoMap());
        assertEquals("full", domainInfos.get(0).getWebInfoMap().get("phase"));
        assertTrue(domainInfos.get(1).isDoFullWebInfoMap());
    }

    @Test
    public void finalWebCollection_shouldSkipWhenBasicCollectionAlreadyEquivalentToFull() throws Exception {
        StageTrackingAssetMapper assetMapper = new StageTrackingAssetMapper();
        List<DomainInfo> domainInfos = Arrays.asList(
                domain("portal.example.com"),
                domain("api.example.com")
        );
        AssetObj assetObj = buildAssetObj();

        invokePrivate(assetMapper, "performInitialWebInfoCollection",
                new Class[]{List.class, AssetObj.class, boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class, int.class, String.class},
                domainInfos, assetObj, false, true, false, false, 2, 3, 1, ExecutorServiceManager.ExecutorPoolNames.ASSET);
        invokePrivate(assetMapper, "performFinalWebInfoCollection",
                new Class[]{List.class, AssetObj.class, boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class, int.class, String.class},
                domainInfos, assetObj, false, true, false, false, 2, 3, 1, ExecutorServiceManager.ExecutorPoolNames.ASSET);

        assertEquals(1, assetMapper.initialCalls.size());
        assertTrue(assetMapper.fullCalls.isEmpty());
        assertTrue(domainInfos.get(0).isDoFullWebInfoMap());
        assertFalse(domainInfos.get(1).isDoWebInfoMap());
    }

    @Test
    public void finalWebCollection_shouldRespectThresholdWhenIconSearchRequiresDeferredDeepCollection() throws Exception {
        StageTrackingAssetMapper assetMapper = new StageTrackingAssetMapper();
        List<DomainInfo> domainInfos = Arrays.asList(
                domain("portal.example.com"),
                domain("api.example.com"),
                domain("oa.example.com")
        );
        AssetObj assetObj = buildAssetObj();

        invokePrivate(assetMapper, "performInitialWebInfoCollection",
                new Class[]{List.class, AssetObj.class, boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class, int.class, String.class},
                domainInfos, assetObj, false, true, true, false, 2, 3, 2, ExecutorServiceManager.ExecutorPoolNames.ASSET);
        invokePrivate(assetMapper, "performFinalWebInfoCollection",
                new Class[]{List.class, AssetObj.class, boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class, int.class, String.class},
                domainInfos, assetObj, false, true, true, false, 2, 3, 2, ExecutorServiceManager.ExecutorPoolNames.ASSET);

        assertEquals(2, assetMapper.initialCalls.size());
        assertEquals(2, assetMapper.fullCalls.size());
        assertTrue(domainInfos.get(0).isDoFullWebInfoMap());
        assertTrue(domainInfos.get(1).isDoFullWebInfoMap());
        assertFalse(domainInfos.get(2).isDoFullWebInfoMap());
    }

    private static DomainInfo domain(String host) {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.setDomain(host);
        domainInfo.setPort("443");
        return domainInfo;
    }

    private static AssetObj buildAssetObj() {
        AssetObj assetObj = new AssetObj();
        assetObj.setCrawlProxy(false);
        assetObj.setMaxDepth(2);
        assetObj.setMaxSubPathCount(3);
        assetObj.setLocalFullDetectionThreshold(20);
        return assetObj;
    }

    private static Object invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = AssetMapper.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static class StageTrackingAssetMapper extends AssetMapper {
        private final List<String> initialCalls = Collections.synchronizedList(new ArrayList<String>());
        private final List<String> fullCalls = Collections.synchronizedList(new ArrayList<String>());

        @Override
        protected Map<String, Object> fetchInitialWebInfo(String baseUrl, int maxDepth, int maxSubPathCount, boolean crawlProxy) {
            initialCalls.add(baseUrl);
            return webInfo("initial", baseUrl);
        }

        @Override
        protected Map<String, Object> fetchFullWebInfo(String baseUrl, boolean hasCrawlLinks, boolean hasFindSensitiveInfo,
                                                       int maxDepth, int maxSubPathCount, boolean crawlProxy) {
            fullCalls.add(baseUrl);
            return webInfo("full", baseUrl);
        }

        private static Map<String, Object> webInfo(String phase, String baseUrl) {
            Map<String, Object> webInfoMap = new HashMap<>();
            webInfoMap.put("phase", phase);
            webInfoMap.put("url", "https://" + baseUrl);
            webInfoMap.put("title", phase + "-" + baseUrl);
            return webInfoMap;
        }
    }
}

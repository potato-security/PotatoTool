package com.potato.potatotool.content.redTeam.infoGathering;

import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetObj;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.CompanyCandidate;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.CompanyCandidateSelectionResult;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.NetAssets;
import com.potato.potatotool.utils.core.I18nTextUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssetMapperCompanyResolutionTest {

    @Test
    public void resolveRootCompanyCandidate_shouldAutoConfirmOnlyExactMatch() throws Exception {
        TestAssetMapper assetMapper = new TestAssetMapper();
        assetMapper.chinazResponses.put(
                "中国交通建设股份有限公司",
                Collections.singletonList(candidate("中国交通建设股份有限公司", "cz-1", null, CompanyCandidate.SOURCE_CHINAZ))
        );

        Object resolutionResult = invokePrivate(
                assetMapper,
                "resolveRootCompanyCandidate",
                new Class[]{String.class},
                "中国交通建设股份有限公司"
        );

        assertNotNull(resolutionResult);
        CompanyCandidate candidate = extractCandidate(resolutionResult);
        assertEquals("中国交通建设股份有限公司", candidate.getCompanyName());
        assertEquals("cz-1", candidate.getChinazCompanyId());
        assertFalse(extractManuallyConfirmed(resolutionResult));
        assertTrue(assetMapper.shownCandidateChoosers.isEmpty());
    }

    @Test
    public void resolveRootCompanyCandidate_shouldUseManualConfirmationAndRefreshExactCandidate() throws Exception {
        TestAssetMapper assetMapper = new TestAssetMapper();
        assetMapper.chinazResponses.put(
                "中交建",
                Collections.singletonList(candidate("中国交通建设股份有限公司", "cz-1", null, CompanyCandidate.SOURCE_CHINAZ))
        );
        assetMapper.chinazResponses.put(
                "中国交通建设股份有限公司",
                Collections.singletonList(candidate("中国交通建设股份有限公司", "cz-1", null, CompanyCandidate.SOURCE_CHINAZ))
        );
        assetMapper.aiqichaResponses.put(
                "中国交通建设股份有限公司",
                Collections.singletonList(candidate("中国交通建设股份有限公司", null, "aqc-1", CompanyCandidate.SOURCE_AIQICHA))
        );
        assetMapper.selectionQueue.add(new CompanyCandidateSelectionResult(
                CompanyCandidateSelectionResult.Action.CONFIRM,
                candidate("中国交通建设股份有限公司", "cz-1", null, CompanyCandidate.SOURCE_CHINAZ)
        ));

        Object resolutionResult = invokePrivate(
                assetMapper,
                "resolveRootCompanyCandidate",
                new Class[]{String.class},
                "中交建"
        );

        assertNotNull(resolutionResult);
        CompanyCandidate candidate = extractCandidate(resolutionResult);
        assertEquals("中国交通建设股份有限公司", candidate.getCompanyName());
        assertEquals("cz-1", candidate.getChinazCompanyId());
        assertEquals("aqc-1", candidate.getAiqichaPid());
        assertTrue(extractManuallyConfirmed(resolutionResult));
        assertEquals(1, assetMapper.shownCandidateChoosers.size());
        assertFalse(assetMapper.shownCandidateChoosers.get(0).aiGenerated);
    }

    @Test
    public void resolveRootCompanyCandidate_shouldRetryWithAiFullNameAfterNoneOfAbove() throws Exception {
        TestAssetMapper assetMapper = new TestAssetMapper();
        assetMapper.chinazResponses.put(
                "中交建",
                Collections.singletonList(candidate("中国交通建设集团有限公司", "cz-group", null, CompanyCandidate.SOURCE_CHINAZ))
        );
        assetMapper.chinazResponses.put(
                "中国交通建设股份有限公司",
                Collections.singletonList(candidate("中国交通建设股份有限公司", "cz-1", null, CompanyCandidate.SOURCE_CHINAZ))
        );
        assetMapper.aiFullNameResponses.put(
                "中交建",
                new LinkedHashSet<>(Collections.singletonList("中国交通建设股份有限公司"))
        );
        assetMapper.selectionQueue.add(new CompanyCandidateSelectionResult(
                CompanyCandidateSelectionResult.Action.NONE_OF_ABOVE,
                null
        ));
        assetMapper.selectionQueue.add(new CompanyCandidateSelectionResult(
                CompanyCandidateSelectionResult.Action.CONFIRM,
                CompanyCandidate.fromAiName("中国交通建设股份有限公司")
        ));

        Object resolutionResult = invokePrivate(
                assetMapper,
                "resolveRootCompanyCandidate",
                new Class[]{String.class},
                "中交建"
        );

        assertNotNull(resolutionResult);
        CompanyCandidate candidate = extractCandidate(resolutionResult);
        assertEquals("中国交通建设股份有限公司", candidate.getCompanyName());
        assertTrue(extractManuallyConfirmed(resolutionResult));
        assertEquals(2, assetMapper.shownCandidateChoosers.size());
        assertFalse(assetMapper.shownCandidateChoosers.get(0).aiGenerated);
        assertTrue(assetMapper.shownCandidateChoosers.get(1).aiGenerated);
    }

    @Test
    public void resolveRootCompanyCandidate_shouldReturnNullWhenUserCancels() throws Exception {
        TestAssetMapper assetMapper = new TestAssetMapper();
        assetMapper.chinazResponses.put(
                "中交建",
                Collections.singletonList(candidate("中国交通建设股份有限公司", "cz-1", null, CompanyCandidate.SOURCE_CHINAZ))
        );
        assetMapper.selectionQueue.add(new CompanyCandidateSelectionResult(
                CompanyCandidateSelectionResult.Action.CANCEL,
                null
        ));

        Object resolutionResult = invokePrivate(
                assetMapper,
                "resolveRootCompanyCandidate",
                new Class[]{String.class},
                "中交建"
        );

        assertNull(resolutionResult);
        assertEquals(1, assetMapper.shownCandidateChoosers.size());
    }

    @Test
    public void resolveRootCompanyCandidate_shouldReturnNullWhenCandidateChooserTimesOut() throws Exception {
        TestAssetMapper assetMapper = new TestAssetMapper();
        assetMapper.chinazResponses.put(
                "中交建",
                Collections.singletonList(candidate("中国交通建设股份有限公司", "cz-1", null, CompanyCandidate.SOURCE_CHINAZ))
        );
        assetMapper.selectionQueue.add(new CompanyCandidateSelectionResult(
                CompanyCandidateSelectionResult.Action.TIMEOUT,
                null
        ));

        Object resolutionResult = invokePrivate(
                assetMapper,
                "resolveRootCompanyCandidate",
                new Class[]{String.class},
                "中交建"
        );

        assertNull(resolutionResult);
        assertEquals(1, assetMapper.shownCandidateChoosers.size());
        assertFalse(assetMapper.shownCandidateChoosers.get(0).aiGenerated);
    }

    @Test
    public void resolveRootCompanyCandidate_shouldStopWhenAiReturnsRepeatedQuery() throws Exception {
        TestAssetMapper assetMapper = new TestAssetMapper();
        assetMapper.aiFullNameResponses.put(
                "中交建",
                new LinkedHashSet<>(Collections.singletonList("中交建"))
        );

        Object resolutionResult = invokePrivate(
                assetMapper,
                "resolveRootCompanyCandidate",
                new Class[]{String.class},
                "中交建"
        );

        assertNull(resolutionResult);
        assertTrue(assetMapper.shownCandidateChoosers.isEmpty());
    }

    @Test
    public void handleCompanyName_shouldFallbackToSeedCollectionWhenRootCompanyNotConfirmed() throws Exception {
        TestAssetMapper assetMapper = new TestAssetMapper();

        Set<String> icpNoSet = new LinkedHashSet<>(Collections.singletonList("京ICP备030173号"));
        Set<String> tmpDomainSet = new LinkedHashSet<>(Collections.singletonList("www.ccccltd.cn"));

        invokePrivate(
                assetMapper,
                "handleCompanyName",
                new Class[]{String.class, AssetObj.class, Set.class, Set.class},
                "中国交通建设股份有限公司",
                new AssetObj(),
                icpNoSet,
                tmpDomainSet
        );

        assertEquals("未确认根公司，跳过企业扩展流程", assetMapper.tipMessage);
        assertEquals(1, assetMapper.dispatchCount);
        assertEquals("中国交通建设股份有限公司", assetMapper.dispatchedNetAssets.getCompanyName());
        assertNull(assetMapper.dispatchedCompanyNames);
        assertEquals(tmpDomainSet, assetMapper.dispatchedDomains);
        assertEquals(icpNoSet, assetMapper.dispatchedIcpNos);
    }

    @Test
    public void handleCompanyName_shouldShowManualRetryTipWhenDirectCompanyInputCannotBeConfirmed() throws Exception {
        TestAssetMapper assetMapper = new TestAssetMapper();

        invokePrivate(
                assetMapper,
                "handleCompanyName",
                new Class[]{String.class, AssetObj.class, Set.class, Set.class},
                "中交建",
                new AssetObj(),
                null,
                null
        );

        assertEquals(I18nTextUtils.getString("infosearch.company.root.manual.retry"), assetMapper.tipMessage);
        assertEquals(0, assetMapper.dispatchCount);
    }

    private static CompanyCandidate candidate(String companyName, String chinazId, String aiqichaPid, String source) {
        CompanyCandidate candidate = new CompanyCandidate();
        candidate.setCompanyName(companyName);
        candidate.setChinazCompanyId(chinazId);
        candidate.setAiqichaPid(aiqichaPid);
        candidate.addSource(source);
        return candidate;
    }

    private static Object invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = AssetMapper.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static CompanyCandidate extractCandidate(Object resolutionResult) throws Exception {
        Field field = resolutionResult.getClass().getDeclaredField("candidate");
        field.setAccessible(true);
        return (CompanyCandidate) field.get(resolutionResult);
    }

    private static boolean extractManuallyConfirmed(Object resolutionResult) throws Exception {
        Field field = resolutionResult.getClass().getDeclaredField("manuallyConfirmed");
        field.setAccessible(true);
        return field.getBoolean(resolutionResult);
    }

    private static class TestAssetMapper extends AssetMapper {
        private final Map<String, List<CompanyCandidate>> chinazResponses = new HashMap<>();
        private final Map<String, List<CompanyCandidate>> aiqichaResponses = new HashMap<>();
        private final Map<String, LinkedHashSet<String>> aiFullNameResponses = new HashMap<>();
        private final Deque<CompanyCandidateSelectionResult> selectionQueue = new ArrayDeque<>();
        private final List<ShownCandidateChooser> shownCandidateChoosers = new ArrayList<>();
        private String tipMessage;
        private int dispatchCount;
        private NetAssets dispatchedNetAssets;
        private Set<String> dispatchedCompanyNames;
        private Set<String> dispatchedDomains;
        private Set<String> dispatchedIcpNos;

        @Override
        protected List<CompanyCandidate> fetchChinazCompanyCandidates(String companyName) {
            return copyCandidates(chinazResponses.get(companyName));
        }

        @Override
        protected List<CompanyCandidate> fetchAiqichaCompanyCandidates(String companyName) {
            return copyCandidates(aiqichaResponses.get(companyName));
        }

        @Override
        protected Set<String> fetchAiFullCompanyNames(String currentQuery) {
            LinkedHashSet<String> result = aiFullNameResponses.get(currentQuery);
            if (result == null) {
                return new LinkedHashSet<>();
            }
            return new LinkedHashSet<>(result);
        }

        @Override
        protected void showCompanyCandidateChoosePaneBox(List<CompanyCandidate> candidates, boolean isAiGenerated, String currentQuery) {
            shownCandidateChoosers.add(new ShownCandidateChooser(currentQuery, isAiGenerated, copyCandidates(candidates)));
        }

        @Override
        protected CompanyCandidateSelectionResult waitAndGetCompanyCandidateSelection() {
            return selectionQueue.isEmpty()
                    ? new CompanyCandidateSelectionResult(CompanyCandidateSelectionResult.Action.CANCEL, null)
                    : selectionQueue.removeFirst();
        }

        @Override
        protected void showTip(String message, boolean isSuccess) {
            tipMessage = message;
        }

        @Override
        protected void dispatchCollectedSeeds(AssetObj assetObj, NetAssets netAssets, Set<String> companyNameSet, Set<String> domainSet, Set<String> icpNoSet) {
            dispatchCount++;
            dispatchedNetAssets = netAssets;
            dispatchedCompanyNames = copySet(companyNameSet);
            dispatchedDomains = copySet(domainSet);
            dispatchedIcpNos = copySet(icpNoSet);
        }
    }

    private static class ShownCandidateChooser {
        private final String currentQuery;
        private final boolean aiGenerated;
        private final List<CompanyCandidate> candidates;

        private ShownCandidateChooser(String currentQuery, boolean aiGenerated, List<CompanyCandidate> candidates) {
            this.currentQuery = currentQuery;
            this.aiGenerated = aiGenerated;
            this.candidates = candidates;
        }
    }

    private static List<CompanyCandidate> copyCandidates(List<CompanyCandidate> source) {
        List<CompanyCandidate> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (CompanyCandidate candidate : source) {
            CompanyCandidate copy = new CompanyCandidate();
            copy.setCompanyName(candidate.getCompanyName());
            copy.setChinazCompanyId(candidate.getChinazCompanyId());
            copy.setAiqichaPid(candidate.getAiqichaPid());
            copy.setCompanyStatus(candidate.getCompanyStatus());
            copy.setLegalRepresentative(candidate.getLegalRepresentative());
            copy.setRegisteredCapital(candidate.getRegisteredCapital());
            copy.setRegisteredTime(candidate.getRegisteredTime());
            copy.addSources(candidate.getSources());
            result.add(copy);
        }
        return result;
    }

    private static Set<String> copySet(Set<String> source) {
        if (source == null) {
            return null;
        }
        return new LinkedHashSet<>(source);
    }
}

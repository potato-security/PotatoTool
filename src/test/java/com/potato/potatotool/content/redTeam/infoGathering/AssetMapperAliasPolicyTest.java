package com.potato.potatotool.content.redTeam.infoGathering;

import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetObj;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssetMapperAliasPolicyTest {

    @Test
    public void addOriginalInputAliasIfNeeded_shouldAddAliasForManualRootConfirmation() throws Exception {
        Set<String> aliases = new HashSet<>();

        invokeAddOriginalInputAliasIfNeeded(
                aliases,
                "中交建",
                "中国交通建设股份有限公司",
                true,
                true,
                "中国交通建设股份有限公司"
        );

        assertTrue(aliases.contains("中交建"));
    }

    @Test
    public void addOriginalInputAliasIfNeeded_shouldNotAddAliasForAutoConfirmedRootCompany() throws Exception {
        Set<String> aliases = new HashSet<>();

        invokeAddOriginalInputAliasIfNeeded(
                aliases,
                "中交建",
                "中国交通建设股份有限公司",
                true,
                false,
                "中国交通建设股份有限公司"
        );

        assertFalse(aliases.contains("中交建"));
    }

    @Test
    public void addOriginalInputAliasIfNeeded_shouldNotAddAliasForDomainOrIpEntry() throws Exception {
        Set<String> aliases = new HashSet<>();

        invokeAddOriginalInputAliasIfNeeded(
                aliases,
                "中交建",
                "中国交通建设股份有限公司",
                false,
                true,
                "中国交通建设股份有限公司"
        );

        assertFalse(aliases.contains("中交建"));
    }

    @Test
    public void addOriginalInputAliasIfNeeded_shouldNotAddAliasForSubCompanyOrSameName() throws Exception {
        Set<String> subCompanyAliases = new HashSet<>();
        invokeAddOriginalInputAliasIfNeeded(
                subCompanyAliases,
                "中交建",
                "中国交通建设股份有限公司",
                true,
                true,
                "中交一航局有限公司"
        );

        Set<String> sameNameAliases = new HashSet<>();
        invokeAddOriginalInputAliasIfNeeded(
                sameNameAliases,
                "中国交通建设股份有限公司",
                "中国交通建设股份有限公司",
                true,
                true,
                "中国交通建设股份有限公司"
        );

        assertFalse(subCompanyAliases.contains("中交建"));
        assertFalse(sameNameAliases.contains("中国交通建设股份有限公司"));
    }

    @Test
    public void resolveCompanyAliasesForCollection_shouldExpandAliasesWhenOnlyShadowAssetsEnabled() {
        AliasAwareAssetMapper assetMapper = new AliasAwareAssetMapper();
        AssetObj assetObj = new AssetObj();
        assetObj.setSearchShadowAssets(true);
        assetObj.setUseGithub(false);
        assetMapper.aiAliases = linkedSet("中国交建", "中交建");

        Set<String> aliases = assetMapper.resolveAliases(
                assetObj,
                "中国交通建设股份有限公司",
                "中交建",
                "中国交通建设股份有限公司",
                true,
                true
        );

        assertTrue(assetMapper.companyAliasChooserShown);
        assertEquals(linkedSet("中国交建", "中交建", "中国交通建设股份有限公司"), aliases);
    }

    @Test
    public void resolveCompanyAliasesForCollection_shouldExpandAliasesWhenOnlyGithubEnabled() {
        AliasAwareAssetMapper assetMapper = new AliasAwareAssetMapper();
        AssetObj assetObj = new AssetObj();
        assetObj.setSearchShadowAssets(false);
        assetObj.setUseGithub(true);
        assetMapper.aiAliases = linkedSet("中国交建");

        Set<String> aliases = assetMapper.resolveAliases(
                assetObj,
                "中国交通建设股份有限公司",
                "中国交通建设股份有限公司",
                "中国交通建设股份有限公司",
                true,
                false
        );

        assertTrue(assetMapper.companyAliasChooserShown);
        assertEquals(linkedSet("中国交建", "中国交通建设股份有限公司"), aliases);
    }

    @Test
    public void resolveCompanyAliasesForCollection_shouldFallbackToCurrentCompanyWhenAiReturnsEmpty() {
        AliasAwareAssetMapper assetMapper = new AliasAwareAssetMapper();
        AssetObj assetObj = new AssetObj();
        assetObj.setSearchShadowAssets(true);
        assetObj.setUseGithub(false);
        assetMapper.aiAliases = new LinkedHashSet<>();

        Set<String> aliases = assetMapper.resolveAliases(
                assetObj,
                "中国交通建设股份有限公司",
                "中交建",
                "中国交通建设股份有限公司",
                true,
                true
        );

        assertTrue(assetMapper.companyAliasChooserShown);
        assertEquals(linkedSet("中交建", "中国交通建设股份有限公司"), aliases);
        assertEquals(linkedSet("中交建", "中国交通建设股份有限公司"), assetMapper.lastShownAliases);
    }

    private static void invokeAddOriginalInputAliasIfNeeded(Set<String> aliases,
                                                            String originalInputCompanyName,
                                                            String confirmedCompanyName,
                                                            boolean directCompanyInput,
                                                            boolean manuallyConfirmed,
                                                            String currentCompanyName) throws Exception {
        Method method = AssetMapper.class.getDeclaredMethod(
                "addOriginalInputAliasIfNeeded",
                Set.class,
                String.class,
                String.class,
                boolean.class,
                boolean.class,
                String.class
        );
        method.setAccessible(true);
        method.invoke(
                new AssetMapper(),
                aliases,
                originalInputCompanyName,
                confirmedCompanyName,
                directCompanyInput,
                manuallyConfirmed,
                currentCompanyName
        );
    }

    private static LinkedHashSet<String> linkedSet(String... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    private static class AliasAwareAssetMapper extends AssetMapper {
        private Set<String> aiAliases = new LinkedHashSet<>();
        private final List<String> progressMessages = new ArrayList<>();
        private boolean companyAliasChooserShown;
        private LinkedHashSet<String> lastShownAliases = new LinkedHashSet<>();

        private Set<String> resolveAliases(AssetObj assetObj,
                                           String currentCompanyName,
                                           String originalInputCompanyName,
                                           String confirmedCompanyName,
                                           boolean directCompanyInput,
                                           boolean manuallyConfirmed) {
            return resolveCompanyAliasesForCollection(
                    assetObj,
                    currentCompanyName,
                    originalInputCompanyName,
                    confirmedCompanyName,
                    directCompanyInput,
                    manuallyConfirmed,
                    "ai",
                    "user",
                    "done"
            );
        }

        @Override
        protected Set<String> fetchAiCompanyAliases(String companyName) {
            return new LinkedHashSet<>(aiAliases);
        }

        @Override
        protected void showCompanyNameChoosePaneBox(Set<String> companyNamesSet) {
            companyAliasChooserShown = true;
            lastShownAliases = new LinkedHashSet<>(companyNamesSet);
        }

        @Override
        protected Set<String> waitAndGetCompanyNameSet() {
            return new LinkedHashSet<>(lastShownAliases);
        }

        @Override
        protected void dispatchCollectedSeeds(AssetObj assetObj, com.potato.potatotool.content.redTeam.infoGathering.classObj.NetAssets netAssets, Set<String> companyNameSet, Set<String> domainSet, Set<String> icpNoSet) {
        }
    }
}

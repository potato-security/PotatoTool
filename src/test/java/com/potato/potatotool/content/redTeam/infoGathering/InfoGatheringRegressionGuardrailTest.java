package com.potato.potatotool.content.redTeam.infoGathering;

import com.potato.potatotool.content.redTeam.infoGathering.cdn.CdnChecker;
import com.potato.potatotool.content.redTeam.infoGathering.subDomain.SubdomainBruteForcer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InfoGatheringRegressionGuardrailTest {

    @Test
    public void subdomainBruteForcer_shouldNotKeepStaticResultCache() {
        for (Field field : SubdomainBruteForcer.class.getDeclaredFields()) {
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            boolean isSet = Set.class.isAssignableFrom(field.getType());
            assertFalse(isStatic && isSet, "unexpected static Set field: " + field.getName());
        }
    }

    @Test
    public void subdomainBruteForcer_shouldKeepParallelismBounded() throws Exception {
        Field field = SubdomainBruteForcer.class.getDeclaredField("MAX_PARALLEL_QUERIES");
        field.setAccessible(true);

        assertEquals(24, field.getInt(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void cdnChecker_shouldCacheRepeatedIpLookup() throws Exception {
        Field cacheField = CdnChecker.class.getDeclaredField("ipCdnCache");
        cacheField.setAccessible(true);
        Map<String, Boolean> cache = (Map<String, Boolean>) cacheField.get(null);
        cache.clear();

        CdnChecker.isCdnIp("127.0.0.1");
        CdnChecker.isCdnIp("127.0.0.1");

        assertEquals(1, cache.size());
        assertTrue(cache.containsKey("127.0.0.1"));
    }

    @Test
    public void sourceGuardrails_shouldKeepCriticalFixesInPlace() throws Exception {
        String paneInfoSearch = readSource("src/main/java/com/potato/potatotool/controller/redTeam/PaneInfoSearch.java");
        String aiUtils = readSource("src/main/java/com/potato/potatotool/content/redTeam/infoGathering/utils/AiUtils.java");
        String hunterSearch = readSource("src/main/java/com/potato/potatotool/content/redTeam/infoGathering/tools/HunterSearch.java");
        String subdomainBruteForcer = readSource("src/main/java/com/potato/potatotool/content/redTeam/infoGathering/subDomain/SubdomainBruteForcer.java");
        String utils = readSource("src/main/java/com/potato/potatotool/content/redTeam/infoGathering/utils/Utils.java");

        assertFalse(paneInfoSearch.contains("currentThread.stop()"));
        assertTrue(paneInfoSearch.contains("currentTask.cancel(true);"));
        assertTrue(paneInfoSearch.contains("currentThread.interrupt();"));
        assertTrue(aiUtils.contains("boolean hasIconUrl = true;"));
        assertTrue(hunterSearch.contains("domainInfo.add(element);"));
        assertTrue(subdomainBruteForcer.contains("detectWildcardAnswers"));
        assertTrue(utils.contains(".setCallTimeout(6)"));
        assertTrue(utils.contains(".setRetries(0)"));
    }

    private static String readSource(String relativePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(relativePath)), StandardCharsets.UTF_8);
    }
}

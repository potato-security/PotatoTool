package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CdpHeadlessExecutorTest {

    @Test
    public void testNormalizeScriptExpressionWrapsLegacyReturnStyleBlocks() {
        String normalized = CdpHeadlessExecutor.normalizeScriptExpression("return document.title");

        assertTrue(normalized.startsWith("(function(){"));
        assertTrue(normalized.contains("return document.title"));
    }

    @Test
    public void testNormalizeScriptExpressionKeepsPlainExpression() {
        String normalized = CdpHeadlessExecutor.normalizeScriptExpression("document.title");

        assertEquals("document.title", normalized);
    }

    @Test
    public void testToJavaValueCoversPrimitivesCollectionsAndNull() {
        JsonElement payload = new JsonParser().parse(
                "{\"title\":\"Example\",\"count\":2,\"ok\":true,\"items\":[1,\"two\"],\"empty\":null}");

        Object converted = CdpHeadlessExecutor.toJavaValue(payload);

        assertTrue(converted instanceof Map);
        Map<?, ?> map = (Map<?, ?>) converted;
        assertEquals("Example", map.get("title"));
        assertEquals(2L, map.get("count"));
        assertEquals(Boolean.TRUE, map.get("ok"));
        assertTrue(map.get("items") instanceof List);
        List<?> items = (List<?>) map.get("items");
        assertEquals(1L, items.get(0));
        assertEquals("two", items.get(1));
        assertNull(map.get("empty"));
    }

    @Test
    public void testShouldSkipInitialNavigationWhenLeadingNavigateExists() {
        List<HeadlessHandler.BrowserStep> steps = Collections.singletonList(
                new HeadlessHandler.BrowserStep("navigate", Collections.singletonMap("url", "http://example.com")));

        assertTrue(CdpHeadlessExecutor.hasLeadingNavigateStep(steps));
        assertFalse(CdpHeadlessExecutor.shouldPerformInitialNavigation("http://example.com", steps));
    }

    @Test
    public void testShouldPerformInitialNavigationWhenNoLeadingNavigateExists() {
        List<HeadlessHandler.BrowserStep> steps = Collections.singletonList(
                new HeadlessHandler.BrowserStep("waitload", Collections.<String, String>emptyMap()));

        assertFalse(CdpHeadlessExecutor.hasLeadingNavigateStep(steps));
        assertTrue(CdpHeadlessExecutor.shouldPerformInitialNavigation("http://example.com", steps));
    }
}

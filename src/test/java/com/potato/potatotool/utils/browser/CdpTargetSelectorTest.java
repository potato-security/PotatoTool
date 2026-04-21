package com.potato.potatotool.utils.browser;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CdpTargetSelectorTest {

    @Test
    public void testPreferUrlContainsShouldSelectMatchingPageTarget() {
        CdpTargetInfo background = new CdpTargetInfo("1", "page", "Example", "https://www.example.com", "ws://127.0.0.1/devtools/page/1");
        CdpTargetInfo aiqicha = new CdpTargetInfo("2", "page", "Aiqicha", "https://aiqicha.baidu.com/s?q=test", "ws://127.0.0.1/devtools/page/2");

        CdpTargetInfo selected = CdpTargetSelector.preferUrlContains("aiqicha.baidu.com")
                .select(Arrays.asList(background, aiqicha));

        assertEquals("2", selected.getId());
    }

    @Test
    public void testPreferUrlContainsShouldFallbackToFirstAttachablePage() {
        CdpTargetInfo first = new CdpTargetInfo("1", "page", "Example", "https://www.example.com", "ws://127.0.0.1/devtools/page/1");
        CdpTargetInfo second = new CdpTargetInfo("2", "page", "Another", "https://www.another.com", "ws://127.0.0.1/devtools/page/2");

        CdpTargetInfo selected = CdpTargetSelector.preferUrlContains("missing-keyword")
                .select(Arrays.asList(first, second));

        assertEquals("1", selected.getId());
    }

    @Test
    public void testFirstPageShouldIgnoreNonPageTargetsAndTargetsWithoutWebsocket() {
        CdpTargetInfo serviceWorker = new CdpTargetInfo("sw", "service_worker", "worker", "https://example.com/sw.js", "ws://127.0.0.1/devtools/page/sw");
        CdpTargetInfo pageWithoutSocket = new CdpTargetInfo("page-1", "page", "page", "https://example.com", "");
        CdpTargetInfo attachablePage = new CdpTargetInfo("page-2", "page", "page2", "https://example.com/2", "ws://127.0.0.1/devtools/page/2");

        CdpTargetInfo selected = CdpTargetSelector.firstPage()
                .select(Arrays.asList(serviceWorker, pageWithoutSocket, attachablePage));

        assertEquals("page-2", selected.getId());
    }

    @Test
    public void testSelectorShouldReturnNullForEmptyTargets() {
        assertNull(CdpTargetSelector.firstPage().select(Collections.<CdpTargetInfo>emptyList()));
        assertNull(CdpTargetSelector.preferUrlContains("aiqicha").select(null));
    }
}

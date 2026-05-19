package com.potato.potatotool.content.redTeam.vulnScanner.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VulnScanServiceTargetNormalizationTest {

    @Test
    void setTargetsAndAddTargetShouldUseSameNormalizationRules() {
        VulnScanService service = VulnScanService.getInstance();
        service.clearTargets();

        service.setTargets(Arrays.asList(
                "example.com",
                "127.0.0.1:8443",
                "portal.internal.example.com",
                "./target/test-classes"
        ));
        List<String> normalizedFromSetTargets = service.getTargetsSnapshot();

        service.clearTargets();
        service.addTarget("example.com");
        service.addTarget("127.0.0.1:8443");
        service.addTarget("portal.internal.example.com");
        service.addTarget("./target/test-classes");
        List<String> normalizedFromAddTarget = service.getTargetsSnapshot();

        assertEquals(normalizedFromSetTargets, normalizedFromAddTarget);
        assertEquals("http://example.com", normalizedFromSetTargets.get(0));
        assertEquals("127.0.0.1:8443", normalizedFromSetTargets.get(1));
        assertEquals("portal.internal.example.com", normalizedFromSetTargets.get(2));
        assertEquals("./target/test-classes", normalizedFromSetTargets.get(3));
    }

    @Test
    void setTargetsShouldTrimAndSkipBlankValues() {
        VulnScanService service = VulnScanService.getInstance();
        service.clearTargets();

        service.setTargets(Arrays.asList("  example.com  ", "", "   ", "\t127.0.0.1:9443\t"));

        assertEquals(Arrays.asList("http://example.com", "127.0.0.1:9443"), service.getTargetsSnapshot());
    }

    @Test
    void setTargetsShouldPreserveCurrentRulesForIpInvalidUrlAndPathLikeTargets() {
        VulnScanService service = VulnScanService.getInstance();
        service.clearTargets();

        service.setTargets(Arrays.asList(
                "192.168.1.10",
                "bad url",
                "example.com/path",
                "https://already.example/app"
        ));

        assertEquals(
                Arrays.asList(
                        "http://192.168.1.10",
                        "http://bad url",
                        "http://example.com/path",
                        "https://already.example/app"
                ),
                service.getTargetsSnapshot()
        );
    }
}

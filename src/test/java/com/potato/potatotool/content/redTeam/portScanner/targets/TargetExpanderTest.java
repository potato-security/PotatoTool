package com.potato.potatotool.content.redTeam.portScanner.targets;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TargetExpanderTest {
    @Test
    public void expand_shouldHandleIpv4CidrRangeHostAndUrl() {
        List<String> input = Arrays.asList("127.0.0.0/30", "192.168.1.10-12", "https://example.com:8443/path", "localhost");
        List<String> expanded = collect(TargetExpander.expand(input));

        assertTrue(expanded.contains("127.0.0.0"));
        assertTrue(expanded.contains("127.0.0.3"));
        assertTrue(expanded.contains("192.168.1.10"));
        assertTrue(expanded.contains("192.168.1.12"));
        assertTrue(expanded.contains("example.com"));
        assertTrue(expanded.contains("localhost"));
    }

    @Test
    public void expand_shouldKeepWideIpv6CidrAsSingleTarget() {
        List<String> expanded = collect(TargetExpander.expand(Arrays.asList("2001:db8::/64")));

        assertEquals(1, expanded.size());
        assertEquals("2001:db8::", expanded.get(0));
    }

    @Test
    public void estimateHosts_shouldEstimateRangeAndSmallIpv6Cidr() {
        assertEquals(3, TargetExpander.estimateHosts(Arrays.asList("192.168.1.10-12")));
        assertEquals(2, TargetExpander.estimateHosts(Arrays.asList("2001:db8::/127")));
    }

    private List<String> collect(Iterator<String> iterator) {
        List<String> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }
}

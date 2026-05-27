package com.potato.potatotool.content.redTeam.portScanner.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HostInputParserTest {
    @Test
    public void parseLines_shouldSkipCommentsAndBlankLinesAndNormalizeUrls() {
        List<String> targets = HostInputParser.parseLines("# comment\n\nhttps://example.com:8443/a\n127.0.0.1 # local\n");

        assertEquals(2, targets.size());
        assertEquals("example.com", targets.get(0));
        assertEquals("127.0.0.1", targets.get(1));
    }

    @Test
    public void parseLines_shouldRejectUnknownSchemes() {
        assertThrows(IllegalArgumentException.class, () -> HostInputParser.parseLines("ftp://example.com"));
    }
}

package com.potato.potatotool.content.redTeam.portScanner.targets;

import com.potato.potatotool.content.redTeam.portScanner.model.ScanTask;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PortPlannerTest {
    @Test
    public void parsePorts_shouldExpandRangesAndDeduplicate() {
        int[] ports = PortPlanner.parsePorts("22,80,80,8000-8002,65535", null);

        assertArrayEquals(new int[]{22, 80, 8000, 8001, 8002, 65535}, ports);
    }

    @Test
    public void parsePorts_shouldRejectInvalidPorts() {
        assertThrows(IllegalArgumentException.class, () -> PortPlanner.parsePorts("0", null));
        assertThrows(IllegalArgumentException.class, () -> PortPlanner.parsePorts("65536", null));
        assertThrows(IllegalArgumentException.class, () -> PortPlanner.parsePorts("90-80", null));
    }

    @Test
    public void plan_shouldGenerateTasksLazilyByHostThenPort() {
        Iterator<ScanTask> iterator = PortPlanner.plan(Arrays.asList("a", "b").iterator(), new int[]{80, 443});

        assertEquals("a:80", iterator.next().getKey());
        assertEquals("a:443", iterator.next().getKey());
        assertEquals("b:80", iterator.next().getKey());
        assertEquals("b:443", iterator.next().getKey());
        assertFalse(iterator.hasNext());
    }
}

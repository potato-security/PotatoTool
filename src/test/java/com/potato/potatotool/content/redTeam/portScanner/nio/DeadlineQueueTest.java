package com.potato.potatotool.content.redTeam.portScanner.nio;

import com.potato.potatotool.content.redTeam.portScanner.model.ScanTask;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeadlineQueueTest {
    @Test
    public void expireUntil_shouldExpireInDeadlineOrderAndAllowRemove() {
        DeadlineQueue queue = new DeadlineQueue();
        TaskAttachment first = new TaskAttachment(new ScanTask("127.0.0.1", 80), 0, 100);
        TaskAttachment second = new TaskAttachment(new ScanTask("127.0.0.1", 81), 0, 200);
        TaskAttachment removed = new TaskAttachment(new ScanTask("127.0.0.1", 82), 0, 150);
        queue.add(second);
        queue.add(first);
        queue.add(removed);
        queue.remove(removed);

        List<Integer> expired = new ArrayList<>();
        queue.expireUntil(250, attachment -> expired.add(attachment.getTask().getPort()));

        assertEquals(2, expired.size());
        assertEquals(80, expired.get(0).intValue());
        assertEquals(81, expired.get(1).intValue());
    }

    @Test
    public void nextDelay_shouldReturnZeroForExpiredDeadline() {
        DeadlineQueue queue = new DeadlineQueue();
        queue.add(new TaskAttachment(new ScanTask("127.0.0.1", 80), 0, 100));

        assertEquals(0, queue.nextDelay(150));
        assertTrue(queue.nextDelay(50) <= 50);
    }
}

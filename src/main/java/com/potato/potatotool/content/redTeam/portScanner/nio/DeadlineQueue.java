package com.potato.potatotool.content.redTeam.portScanner.nio;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class DeadlineQueue {
    private final ConcurrentSkipListMap<Long, TaskAttachment> map = new ConcurrentSkipListMap<>();
    private final AtomicLong seq = new AtomicLong();

    public void add(TaskAttachment attachment) {
        long key = (attachment.getDeadlineMs() << 20) | (seq.incrementAndGet() & 0xFFFFFL);
        attachment.deadlineKey = key;
        map.put(key, attachment);
    }

    public void remove(TaskAttachment attachment) {
        if (attachment != null) {
            map.remove(attachment.deadlineKey);
        }
    }

    public long nextDelay(long now) {
        Map.Entry<Long, TaskAttachment> head = map.firstEntry();
        if (head == null) {
            return 50L;
        }
        long deadline = head.getKey() >> 20;
        return Math.max(0L, deadline - now);
    }

    public void expireUntil(long now, ExpireConsumer consumer) {
        Map.Entry<Long, TaskAttachment> entry;
        while ((entry = map.firstEntry()) != null && (entry.getKey() >> 20) <= now) {
            if (map.remove(entry.getKey()) != null) {
                consumer.accept(entry.getValue());
            }
        }
    }

    public void clear() {
        map.clear();
    }

    public interface ExpireConsumer {
        void accept(TaskAttachment attachment);
    }
}

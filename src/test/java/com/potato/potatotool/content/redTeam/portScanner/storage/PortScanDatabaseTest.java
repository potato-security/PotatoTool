package com.potato.potatotool.content.redTeam.portScanner.storage;

import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PortScanDatabaseTest {
    @TempDir
    Path tempDir;

    @Test
    public void saveScan_shouldPersistHistoryAndReloadFullScan() throws Exception {
        PortScanDatabase database = new PortScanDatabase(tempDir.resolve("portscan_history.db"));
        PortScanResult result = sampleResult();

        database.saveScan(result);
        List<PortScanDatabase.HistoryItem> history = database.loadHistory();
        PortScanResult loaded = database.loadScan("scan-db-test");

        assertEquals(1, history.size());
        assertEquals("scan-db-test", history.get(0).getScanId());
        assertEquals(1, history.get(0).getOpenCount());
        assertNotNull(loaded);
        assertEquals(2, loaded.getResults().size());
        assertEquals(1, loaded.getOpenResults().size());
        PortResult open = loaded.getOpenResults().get(0);
        assertEquals("127.0.0.1", open.getHost());
        assertEquals(443, open.getPort());
        assertEquals("https 中文", open.getService());
        assertEquals("hello,世界", open.getBanner());
        assertEquals(true, open.isTls());
        assertEquals(15L, open.getRttMs());
    }

    @Test
    public void deleteScan_shouldRemoveHistoryAndResults() throws Exception {
        PortScanDatabase database = new PortScanDatabase(tempDir.resolve("portscan_history_delete.db"));
        PortScanResult result = sampleResult();

        database.saveScan(result);
        database.deleteScan("scan-db-test");

        assertEquals(0, database.loadHistory().size());
        assertNull(database.loadScan("scan-db-test"));
    }

    @Test
    public void clearHistory_shouldRemoveAllHistoryAndResults() throws Exception {
        PortScanDatabase database = new PortScanDatabase(tempDir.resolve("portscan_history_clear.db"));
        PortScanResult first = sampleResult();
        PortScanResult second = sampleResult();
        second.setScanId("scan-db-test-2");

        database.saveScan(first);
        database.saveScan(second);
        database.clearHistory();

        assertEquals(0, database.loadHistory().size());
        assertNull(database.loadScan("scan-db-test"));
        assertNull(database.loadScan("scan-db-test-2"));
    }

    private PortScanResult sampleResult() {
        PortScanResult result = new PortScanResult();
        result.setScanId("scan-db-test");
        result.setStartTime(1000L);
        result.setEndTime(2000L);
        result.setTotalTasks(2L);
        result.setCompletedTasks(2L);
        PortResult open = new PortResult("127.0.0.1", 443, PortState.OPEN);
        open.setService("https 中文");
        open.setBanner("hello,世界");
        open.setTls(true);
        open.setRttMs(15L);
        open.setTimestamp(1234L);
        result.addOrUpdate(open);
        PortResult filtered = new PortResult("127.0.0.1", 444, PortState.FILTERED);
        filtered.setService("unknown");
        filtered.setBanner("");
        filtered.setRttMs(600L);
        filtered.setTimestamp(1235L);
        result.addOrUpdate(filtered);
        return result;
    }
}

package com.potato.potatotool.content.redTeam.portScanner.report;

import com.google.gson.Gson;
import com.potato.potatotool.content.redTeam.portScanner.model.PortResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortScanResult;
import com.potato.potatotool.content.redTeam.portScanner.model.PortState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PortScanReportWriterTest {
    @TempDir
    Path tempDir;

    @Test
    public void write_shouldExportJsonCsvTxtAndMarkdownAsUtf8() throws Exception {
        PortScanResult result = sampleResult();
        PortScanReportWriter writer = new PortScanReportWriter();

        File json = tempDir.resolve("scan.json").toFile();
        File csv = tempDir.resolve("scan.csv").toFile();
        File txt = tempDir.resolve("scan.txt").toFile();
        File md = tempDir.resolve("scan.md").toFile();
        writer.write(result, "json", json);
        writer.write(result, "csv", csv);
        writer.write(result, "txt", txt);
        writer.write(result, "md", md);

        PortScanResult parsed = new Gson().fromJson(read(json), PortScanResult.class);
        assertEquals("scan-report-test", parsed.getScanId());
        assertEquals(2, parsed.getResults().size());
        assertTrue(read(csv).contains("\"nginx 中文\""));
        assertTrue(read(csv).contains("\"hello,世界\""));
        assertTrue(read(txt).contains("127.0.0.1:80\tOPEN\tnginx 中文\thello,世界"));
        assertTrue(read(md).contains("| 127.0.0.1 | 80 | OPEN | nginx 中文 | 12 | hello,世界 |"));
    }

    private String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private PortScanResult sampleResult() {
        PortScanResult result = new PortScanResult();
        result.setScanId("scan-report-test");
        result.setStartTime(1000L);
        result.setEndTime(2000L);
        result.setTotalTasks(2L);
        result.setCompletedTasks(2L);
        PortResult open = new PortResult("127.0.0.1", 80, PortState.OPEN);
        open.setService("nginx 中文");
        open.setBanner("hello,世界");
        open.setRttMs(12L);
        result.addOrUpdate(open);
        PortResult closed = new PortResult("127.0.0.1", 81, PortState.CLOSED);
        closed.setService("unknown");
        closed.setBanner("");
        closed.setRttMs(3L);
        result.addOrUpdate(closed);
        return result;
    }
}

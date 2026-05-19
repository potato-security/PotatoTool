package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VulnScanDatabaseHistoryTest {

    private String originalUserHome;

    @AfterEach
    void tearDown() throws Exception {
        resetDatabaseSingleton();
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        Constants.cachedConfig = null;
    }

    @Test
    void shouldPersistScanHistoryAndVulnerabilityDetails(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        VulnScanDatabase database = freshDatabase();

        long recordId = database.saveScanRecord(
                Arrays.asList(buildResult("one", PocObj.Severity.HIGH), buildResult("two", PocObj.Severity.LOW)),
                configProfile("A楼-默认"),
                42L,
                "completed"
        );

        List<ScanHistory> histories = database.queryAllHistory();
        assertEquals(1, histories.size());
        ScanHistory history = histories.get(0);
        assertEquals(recordId, history.getId());
        assertEquals(2, history.getTargetCount());
        assertEquals(2, history.getPocCount());
        assertEquals(2, history.getVulnCount());
        assertEquals(1, history.getHighCount());
        assertEquals(1, history.getLowCount());
        assertEquals("completed", history.getStatus());
        assertEquals("A楼-默认", history.getConfig().get("profile"));

        List<VulnDetail> details = database.loadHistoryDetails(recordId);
        assertEquals(2, details.size());
        assertEquals("poc-one", details.get(0).getPocId());
        assertEquals("http", details.get(0).getProtocol());

        DatabaseStatistics statistics = database.getStatistics();
        assertEquals(1, statistics.getTotalScans());
        assertEquals(2, statistics.getTotalVulns());
        assertEquals(Integer.valueOf(1), statistics.getSeverityStats().get("HIGH"));
        assertEquals(Integer.valueOf(1), statistics.getSeverityStats().get("LOW"));
        assertTrue(Files.exists(tempHome.resolve("db").resolve("history.sqlite")));
    }

    @Test
    void shouldUpdateExistingHistoryRecordByScanIdAndReplaceDetails(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        VulnScanDatabase database = freshDatabase();

        long firstId = database.saveOrUpdateScanRecord(
                "scan-001",
                Collections.singletonList(buildResult("alpha", PocObj.Severity.MEDIUM)),
                configProfile("B楼-轻量"),
                10L,
                "paused"
        );

        long secondId = database.saveOrUpdateScanRecord(
                "scan-001",
                Arrays.asList(buildResult("beta", PocObj.Severity.CRITICAL), buildResult("gamma", PocObj.Severity.HIGH)),
                configProfile("B楼-回归"),
                88L,
                "completed"
        );

        assertEquals(firstId, secondId);

        List<ScanHistory> histories = database.queryAllHistory();
        assertEquals(1, histories.size());
        ScanHistory history = histories.get(0);
        assertEquals(2, history.getTargetCount());
        assertEquals(2, history.getPocCount());
        assertEquals(2, history.getVulnCount());
        assertEquals(1, history.getCriticalCount());
        assertEquals(1, history.getHighCount());
        assertEquals("completed", history.getStatus());
        assertEquals("B楼-回归", history.getConfig().get("profile"));

        List<VulnDetail> details = database.loadHistoryDetails(firstId);
        assertEquals(2, details.size());
        assertEquals("poc-beta", details.get(0).getPocId());
        assertEquals("poc-gamma", details.get(1).getPocId());
    }

    private void configureIsolatedHome(Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path dbPath = tempHome.resolve("db").resolve("history.sqlite");
        Files.createDirectories(dbPath.getParent());

        String json = "{\n" +
                "  \"VulnScan\": {\n" +
                "    \"dbPath\": \"" + escapeForJson(dbPath.toString()) + "\"\n" +
                "  }\n" +
                "}";
        Files.write(configDir.resolve("config.json"), json.getBytes(StandardCharsets.UTF_8));
        Constants.cachedConfig = null;
        resetDatabaseSingleton();
    }

    private VulnScanDatabase freshDatabase() throws Exception {
        resetDatabaseSingleton();
        return VulnScanDatabase.getInstance();
    }

    private void resetDatabaseSingleton() throws Exception {
        Field instanceField = VulnScanDatabase.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        Object existing = instanceField.get(null);
        if (existing instanceof VulnScanDatabase) {
            ((VulnScanDatabase) existing).close();
        }
        instanceField.set(null, null);
    }

    private Map<String, Object> configProfile(String profileName) {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("profile", profileName);
        config.put("threads", 4);
        config.put("timeout", 6);
        return config;
    }

    private ScanResult buildResult(String suffix, PocObj.Severity severity) {
        ScanResult result = new ScanResult();
        result.setTarget("http://127.0.0.1/" + suffix);
        result.setVulnerable(true);

        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("poc-" + suffix);
        poc.setName("漏洞-" + suffix);
        poc.setSeverity(severity);
        poc.setVulType("RCE");
        poc.setOriginalFormat("nuclei");
        poc.setProtocol("http");
        poc.setDescription("说明-" + suffix);
        result.setPoc(poc);
        return result;
    }

    private String escapeForJson(String value) {
        return value.replace("\\", "\\\\");
    }
}

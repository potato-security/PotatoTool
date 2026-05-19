package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PocDatabaseManagerExecutionStatsTest {

    private String originalUserHome;

    @AfterEach
    void tearDown() throws Exception {
        resetSingletons();
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        Constants.cachedConfig = null;
    }

    @Test
    void shouldPersistExecutionStatsForSuccessAndError(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        PocDatabaseManager manager = freshManager();

        PocObj.Poc poc = buildPoc();
        PocDatabaseManager.ImportResult importResult = manager.insertPoc(
                poc,
                "id: poc-status-test\nname: poc-status-test",
                "poc-status-test.yaml",
                "JUnit"
        );
        assertTrue(importResult.isSuccess());

        manager.updateExecutionStats(importResult.getPocId(), true);
        PocDatabaseManager.PocEntity afterSuccess = manager.getPocById(importResult.getPocId());
        assertEquals(1, afterSuccess.getExecutionCount());
        assertEquals(1, afterSuccess.getSuccessCount());
        assertEquals(0, afterSuccess.getErrorCount());
        assertNotNull(afterSuccess.getLastUsedAt());

        manager.updateExecutionStats(importResult.getPocId(), false);
        PocDatabaseManager.PocEntity afterError = manager.getPocById(importResult.getPocId());
        assertEquals(2, afterError.getExecutionCount());
        assertEquals(1, afterError.getSuccessCount());
        assertEquals(1, afterError.getErrorCount());
        assertNotNull(afterError.getLastUsedAt());
        assertTrue(Files.exists(tempHome.resolve("db").resolve("poc-status.sqlite")));
    }

    private PocObj.Poc buildPoc() {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId("poc-status-test");
        poc.setName("POC 状态统计测试");
        poc.setSeverity(PocObj.Severity.HIGH);
        poc.setProtocol("http");
        poc.setOriginalFormat("nuclei");
        poc.setDescription("用于验证 execution_count/success_count/error_count 落库");
        poc.setVulType("RCE");
        poc.setTags(Arrays.asList("status", "regression"));
        return poc;
    }

    private void configureIsolatedHome(Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path dbPath = tempHome.resolve("db").resolve("poc-status.sqlite");
        Files.createDirectories(dbPath.getParent());

        String json = "{\n" +
                "  \"VulnScan\": {\n" +
                "    \"dbPath\": \"" + escapeForJson(dbPath.toString()) + "\"\n" +
                "  }\n" +
                "}";
        Files.write(configDir.resolve("config.json"), json.getBytes(StandardCharsets.UTF_8));
        Constants.cachedConfig = null;
        resetSingletons();
    }

    private PocDatabaseManager freshManager() throws Exception {
        resetSingletons();
        return PocDatabaseManager.getInstance();
    }

    private void resetSingletons() throws Exception {
        resetSingleton(VulnScanDatabase.class, "instance", true);
        resetSingleton(PocDatabaseManager.class, "instance", false);
    }

    private void resetSingleton(Class<?> type, String fieldName, boolean closeDatabase) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object existing = field.get(null);
        if (closeDatabase && existing instanceof VulnScanDatabase) {
            ((VulnScanDatabase) existing).close();
        }
        field.set(null, null);
    }

    private String escapeForJson(String value) {
        return value.replace("\\", "\\\\");
    }
}

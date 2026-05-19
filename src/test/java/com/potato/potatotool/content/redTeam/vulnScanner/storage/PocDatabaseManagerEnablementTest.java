package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PocDatabaseManager 启停状态测试")
class PocDatabaseManagerEnablementTest {

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
    @DisplayName("禁用POC后不应出现在启用集合中，重新启用后应恢复")
    void shouldExcludeDisabledPocsFromEnabledLoad(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        PocDatabaseManager manager = freshManager();

        PocDatabaseManager.ImportResult first = manager.insertPoc(
                buildPoc("enabled-a", "nuclei", PocObj.Severity.HIGH),
                "id: enabled-a\nname: enabled-a",
                "enabled-a.yaml",
                "JUnit"
        );
        PocDatabaseManager.ImportResult second = manager.insertPoc(
                buildPoc("enabled-b", "goby", PocObj.Severity.MEDIUM),
                "id: enabled-b\nname: enabled-b",
                "enabled-b.json",
                "JUnit"
        );

        assertTrue(first.isSuccess());
        assertTrue(second.isSuccess());

        assertEquals(2, manager.getAllEnabledPocs().size());

        manager.setEnabled(second.getPocId(), false);
        List<PocObj.Poc> afterDisable = manager.getAllEnabledPocs();
        assertEquals(1, afterDisable.size());
        assertEquals(first.getPocId(), afterDisable.get(0).getId());

        manager.setEnabled(second.getPocId(), true);
        assertEquals(2, manager.getAllEnabledPocs().size());
    }

    @Test
    @DisplayName("批量禁用和批量启用应影响启用POC集合")
    void shouldApplyBatchEnablementStateToEnabledLoad(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        PocDatabaseManager manager = freshManager();

        PocDatabaseManager.ImportResult first = manager.insertPoc(
                buildPoc("batch-a", "nuclei", PocObj.Severity.HIGH),
                "id: batch-a\nname: batch-a",
                "batch-a.yaml",
                "JUnit"
        );
        PocDatabaseManager.ImportResult second = manager.insertPoc(
                buildPoc("batch-b", "xray", PocObj.Severity.LOW),
                "id: batch-b\nname: batch-b",
                "batch-b.yaml",
                "JUnit"
        );

        List<String> ids = Arrays.asList(first.getPocId(), second.getPocId());
        assertEquals(2, manager.batchSetEnabled(ids, false));
        assertTrue(manager.getAllEnabledPocs().isEmpty());

        assertEquals(2, manager.batchSetEnabled(ids, true));
        assertEquals(2, manager.getAllEnabledPocs().size());
    }

    private PocObj.Poc buildPoc(String id, String format, PocObj.Severity severity) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setSeverity(severity);
        poc.setProtocol("http");
        poc.setOriginalFormat(format);
        poc.setDescription("用于验证数据库中的启停状态是否影响默认加载集合");
        poc.setVulType("RCE");
        poc.setTags(Collections.singletonList("enablement"));
        return poc;
    }

    private void configureIsolatedHome(Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path dbPath = tempHome.resolve("db").resolve("poc-enablement.sqlite");
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
        resetSingleton(PocDatabaseInitializer.class, "instance", false);
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

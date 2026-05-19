package com.potato.potatotool.content.redTeam.vulnScanner.storage;

import com.potato.potatotool.content.redTeam.vulnScanner.model.TaskState;
import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VulnScanDatabase 异常链路测试")
class VulnScanDatabaseFailureTest {

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
    @DisplayName("数据库连接关闭后保存任务状态应抛出 SQLException")
    void shouldFailToSaveTaskStateAfterClose(@TempDir Path tempHome) throws Exception {
        configureIsolatedHome(tempHome);
        VulnScanDatabase database = VulnScanDatabase.getInstance();
        database.close();

        TaskState taskState = new TaskState();
        taskState.setTaskId("task-1");
        taskState.setScanId("scan-1");
        taskState.setTarget("http://127.0.0.1");
        taskState.setPocId("poc-1");
        taskState.setCompleted(true);
        taskState.setVulnerable(false);
        taskState.setStartTime(System.currentTimeMillis());
        taskState.setEndTime(System.currentTimeMillis());

        SQLException exception = assertThrows(SQLException.class, () -> database.saveTaskState(taskState));
        assertTrue(exception.getMessage() != null && !exception.getMessage().trim().isEmpty());
    }

    private void configureIsolatedHome(Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Path dbPath = tempHome.resolve("db").resolve("failure.sqlite");
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

    private void resetDatabaseSingleton() throws Exception {
        Field instanceField = VulnScanDatabase.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        Object existing = instanceField.get(null);
        if (existing instanceof VulnScanDatabase) {
            ((VulnScanDatabase) existing).close();
        }
        instanceField.set(null, null);
    }

    private String escapeForJson(String value) {
        return value.replace("\\", "\\\\");
    }
}

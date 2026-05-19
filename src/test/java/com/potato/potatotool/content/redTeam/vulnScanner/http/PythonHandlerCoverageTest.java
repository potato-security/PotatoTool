package com.potato.potatotool.content.redTeam.vulnScanner.http;

import com.potato.potatotool.utils.core.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PythonHandler 受控执行测试")
class PythonHandlerCoverageTest {

    private String originalUserHome;

    @AfterEach
    void tearDown() {
        PythonHandler.resetPythonPath();
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        Constants.cachedConfig = null;
    }

    @Test
    @DisplayName("应执行受控 Python 脚本并返回上下文结果")
    void shouldExecuteControlledPythonSnippet(@TempDir Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());
        Files.createDirectories(tempHome.resolve(".PotatoTool"));
        Constants.cachedConfig = null;
        PythonHandler.resetPythonPath();

        assertTrue(PythonHandler.isPythonAvailable());
        assertNotNull(PythonHandler.getPythonVersion());

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("name", "potato");
        context.put("count", 3);

        PythonHandler.PythonResponse response = PythonHandler.executePython(
                "set_result({'message': name.upper(), 'value': count + 1})",
                context,
                5
        );

        assertTrue(response.isSuccess());
        assertEquals(0, response.getExitCode());
        assertTrue(response.getResult() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertEquals("POTATO", result.get("message"));
        assertEquals(4.0, result.get("value"));
    }

    @Test
    @DisplayName("超时脚本应返回超时错误")
    void shouldReturnTimeoutWhenScriptExceedsLimit(@TempDir Path tempHome) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());
        Files.createDirectories(tempHome.resolve(".PotatoTool"));
        Constants.cachedConfig = null;
        PythonHandler.resetPythonPath();

        PythonHandler.PythonResponse response = PythonHandler.executePython(
                "import time\ntime.sleep(2)\nset_result('done')",
                new HashMap<String, Object>(),
                1
        );

        assertFalse(response.isSuccess());
        assertEquals(-1, response.getExitCode());
        assertTrue(response.getError().contains("执行超时"));
    }
}

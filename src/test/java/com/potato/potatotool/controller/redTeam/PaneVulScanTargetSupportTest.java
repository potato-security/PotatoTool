package com.potato.potatotool.controller.redTeam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PaneVulScan 目标解析与文件导入测试")
class PaneVulScanTargetSupportTest {

    @Test
    @DisplayName("应解析单目标、多目标并忽略空行和注释")
    void shouldParseTargetsFromTextareaContent() {
        assertEquals(
                Collections.singletonList("https://example.com/app"),
                PaneVulScanTargetSupport.parseTargets("https://example.com/app")
        );

        assertEquals(
                Arrays.asList("https://one.example", "127.0.0.1:8443", "./target/test-classes"),
                PaneVulScanTargetSupport.parseTargets(
                        "https://one.example\n" +
                        "\n" +
                        " 127.0.0.1:8443 \n" +
                        "# ignore-me\n" +
                        "./target/test-classes\n"
                )
        );
    }

    @Test
    @DisplayName("空输入或仅注释输入应得到空目标列表")
    void shouldReturnEmptyTargetsForBlankInput() {
        assertEquals(Collections.emptyList(), PaneVulScanTargetSupport.parseTargets(""));
        assertEquals(Collections.emptyList(), PaneVulScanTargetSupport.parseTargets("   \n\t"));
        assertEquals(Collections.emptyList(), PaneVulScanTargetSupport.parseTargets("# only-comment\n   \n# second"));
    }

    @Test
    @DisplayName("导入目标文件应保留原始行内容与行数")
    void shouldReadTargetFileContent(@TempDir Path tempDir) throws Exception {
        Path targetFile = tempDir.resolve("targets.txt");
        Files.write(
                targetFile,
                Arrays.asList("https://a.example", "", "# note", "127.0.0.1:8080"),
                StandardCharsets.UTF_8
        );

        assertEquals(
                Arrays.asList("https://a.example", "", "# note", "127.0.0.1:8080"),
                PaneVulScanTargetSupport.readTargetLines(targetFile)
        );
        assertEquals(
                "https://a.example\n\n# note\n127.0.0.1:8080",
                PaneVulScanTargetSupport.readTargetFileContent(targetFile)
        );
    }
}

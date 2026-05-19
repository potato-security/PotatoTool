package com.potato.potatotool.content.redTeam.vulnScanner.loader;

import com.potato.potatotool.content.redTeam.vulnScanner.exception.PocLoadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PocLoader 异常链路测试")
class PocLoaderFailureTest {

    @Test
    @DisplayName("非法 JSON POC 应抛出 PocLoadException")
    void shouldFailOnInvalidJsonPoc(@TempDir Path tempDir) throws Exception {
        Path invalidJson = tempDir.resolve("invalid.json");
        Files.write(invalidJson, "{\"name\":\"broken\",".getBytes(StandardCharsets.UTF_8));

        PocLoader loader = new PocLoader();
        PocLoadException exception = assertThrows(PocLoadException.class,
                () -> loader.loadFromFile(invalidJson.toString()));

        assertTrue(exception.getMessage().contains("解析POC文件失败")
                || exception.getMessage().contains("POC解析异常")
                || exception.getMessage().contains("加载失败"));
    }

    @Test
    @DisplayName("不存在的 POC 文件应抛出 PocLoadException")
    void shouldFailOnMissingPocFile(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing.yaml");
        PocLoader loader = new PocLoader();

        PocLoadException exception = assertThrows(PocLoadException.class,
                () -> loader.loadFromFile(missing.toString()));

        assertTrue(exception.getMessage().contains("文件不存在"));
    }
}

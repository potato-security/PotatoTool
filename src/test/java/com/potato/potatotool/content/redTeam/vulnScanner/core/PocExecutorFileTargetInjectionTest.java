package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("File 目标路径注入测试")
public class PocExecutorFileTargetInjectionTest {

    private static final String BUILTIN_POC =
            "src/main/resources/poc/nucleiPoc/file/android/debug-enabled.yaml";

    @Test
    @DisplayName("内置 file 模板未显式写 paths 时应默认扫描目标本地文件")
    public void testBuiltinFilePocShouldUseTargetPathWhenPathsMissing() throws Exception {
        PocLoader loader = new PocLoader();
        loader.setVerbose(false);
        PocObj.Poc poc = loader.loadFromFile(BUILTIN_POC);

        assertNotNull(poc, "内置 file POC 加载失败");
        assertNotNull(poc.getVerifySteps(), "verifySteps 不应为空");
        assertFalse(poc.getVerifySteps().isEmpty(), "verifySteps 不应为空");

        File positive = createTempFile("android-debug-positive", ".xml",
                "<manifest package=\"com.example\" xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                        "  <application android:debuggable=\"true\" />\n" +
                        "</manifest>\n");
        File negative = createTempFile("android-debug-negative", ".xml",
                "<manifest package=\"com.example\" xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                        "  <application android:allowBackup=\"false\" />\n" +
                        "</manifest>\n");

        ScanConfig config = new ScanConfig();
        config.setInputType(PocObj.InputType.LOCAL_FILE);
        config.setAutoDetectInputType(false);
        config.setLocalTargetPath(positive.getAbsolutePath());
        config.setEnableResponseCache(false);
        config.setEnableClustering(false);
        config.setThreads(1);
        config.setTimeout(5);
        config.setRetries(0);

        PocExecutor executor = new PocExecutor(config);

        ScanResult positiveResult = executor.execute(positive.getAbsolutePath(), poc);
        ScanResult negativeResult = executor.execute(negative.getAbsolutePath(), poc);

        assertTrue(positiveResult.isVulnerable(), "正样本应命中 android-debug-enabled");
        assertFalse(negativeResult.isVulnerable(), "反样本不应误报 android-debug-enabled");
    }

    private File createTempFile(String prefix, String suffix, String content) throws Exception {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }
}

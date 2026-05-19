package com.potato.potatotool.controller.redTeam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PaneVulScan 静态 UI 审计测试")
class PaneVulScanStaticUiAuditTest {

    private static final Pattern I18N_PATTERN =
            Pattern.compile("userData=\"i18n(?:-prompt|-combobox)?:([^\"]+)\"");
    private static final Pattern PROMPT_PATTERN =
            Pattern.compile("promptText=\"([^\"]+)\"");

    @Test
    @DisplayName("pane_vulScan.fxml 引用的 i18n key 应同时存在于中英文资源文件")
    void shouldResolveAllReferencedI18nKeys() throws Exception {
        String fxml = readClasspath("/fxml/redTeam/pane_vulScan.fxml");
        Properties zh = loadProperties("/i18n/messages_zh_CN.properties");
        Properties en = loadProperties("/i18n/messages_en_US.properties");

        Set<String> missing = new HashSet<String>();
        Matcher matcher = I18N_PATTERN.matcher(fxml);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key.startsWith("vulnscan.scanmode")
                    || key.startsWith("vulnscan.inputtype")
                    || key.startsWith("vulnscan.filter.formatopt")
                    || key.startsWith("vulnscan.filter.severityopt")
                    || key.startsWith("vulnscan.filter.statusopt")
                    || key.startsWith("vulnscan.filter.loglevel")) {
                if (!hasComboboxBundle(zh, key) || !hasComboboxBundle(en, key)) {
                    missing.add(key);
                }
            } else if (!zh.containsKey(key) || !en.containsKey(key)) {
                missing.add(key);
            }
        }

        assertTrue(missing.isEmpty(), "缺失 i18n key: " + missing);
    }

    @Test
    @DisplayName("pane_vulScan.fxml 不应保留未绑定 i18n 的中文 promptText")
    void shouldNotKeepHardcodedChinesePromptText() throws Exception {
        String fxml = readClasspath("/fxml/redTeam/pane_vulScan.fxml");
        String normalizedFxml = fxml.replaceAll("\\s+", " ");

        assertTrue(normalizedFxml.contains("promptText=\"请输入目标 URL/IP，每行一个...\" userData=\"i18n-prompt:vulnscan.target.placeholder\""));
        assertTrue(normalizedFxml.contains("promptText=\"搜索 ID / 名称 / 标签...\" prefWidth=\"250\" userData=\"i18n-prompt:vulnscan.search.placeholder\""));
        assertTrue(normalizedFxml.contains("promptText=\"搜索日志内容...\" prefWidth=\"250\" userData=\"i18n-prompt:vulnscan.ui.logsearch.placeholder\""));
        assertTrue(normalizedFxml.contains("promptText=\"格式\" userData=\"i18n-prompt:vulnscan.ui.exportformat.prompt\""));

        Set<String> unmatchedChinesePrompts = new HashSet<String>();
        Matcher matcher = PROMPT_PATTERN.matcher(fxml);
        while (matcher.find()) {
            String prompt = matcher.group(1);
            if (containsChinese(prompt)
                    && !prompt.equals("请输入目标 URL/IP，每行一个...")
                    && !prompt.equals("搜索 ID / 名称 / 标签...")
                    && !prompt.equals("搜索日志内容...")
                    && !prompt.equals("格式")) {
                unmatchedChinesePrompts.add(prompt);
            }
        }
        assertEquals(new HashSet<String>(), unmatchedChinesePrompts,
                "未绑定 i18n 的中文 promptText 清单发生变化");
    }

    private Properties loadProperties(String path) throws Exception {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            assertTrue(inputStream != null, "资源不存在: " + path);
            properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private String readClasspath(String path) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            assertTrue(inputStream != null, "资源不存在: " + path);
            byte[] buffer = new byte[4096];
            StringBuilder builder = new StringBuilder();
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            }
            return builder.toString();
        }
    }

    private boolean containsChinese(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= 0x4e00 && ch <= 0x9fff) {
                return true;
            }
        }
        return false;
    }

    private boolean hasComboboxBundle(Properties properties, String baseKey) {
        String countValue = properties.getProperty(baseKey + ".count");
        if (countValue == null) {
            return false;
        }
        int count;
        try {
            count = Integer.parseInt(countValue.trim());
        } catch (NumberFormatException ex) {
            return false;
        }
        for (int i = 1; i <= count; i++) {
            if (!properties.containsKey(baseKey + "." + i)) {
                return false;
            }
        }
        return true;
    }
}

package com.potato.potatotool.content.redTeam.vulnScanner.config;

import com.google.gson.JsonObject;
import com.potato.potatotool.MainApplication;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpLogService;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.network.HeaderManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("楼栋/资产组本机模拟剖面测试")
class VulnScanBuildingProfileSimulationTest {

    private String originalUserHome;

    @AfterEach
    void tearDown() throws Exception {
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        Constants.cachedConfig = null;
        resetSingleton(HeaderManager.class, "instance");
        resetSingleton(VulnScanConfig.class, "instance");
        HttpLogService.clearCache();
        HttpLogService.closeClient();
        HttpLogService.configureInteractsh("oast.pro", null);
        HttpLogService.setPlatform(HttpLogService.Platform.INTERACTSH);
        DnsLogService.clearCache();
        DnsLogService.setMockMode(false);
        DnsLogService.configureCeye(null, null);
        DnsLogService.setPlatform(DnsLogService.Platform.DNSLOG_CN);
    }

    @Test
    @DisplayName("A楼默认直连剖面应走直连、默认 OOB 和独立报告目录")
    void shouldApplyBuildingAProfile(@TempDir Path tempHome) throws Exception {
        configureProfile(tempHome, profileAJson(tempHome));

        VulnScanConfig config = VulnScanConfig.getInstance();
        assertFalse(config.isProxyEnabled());
        assertEquals("", config.getProxyAddress());
        assertEquals(tempHome.resolve("reports").resolve("building-a").toString(), config.getReportDirectory());

        invokeApplyOobSettings();

        assertEquals(HttpLogService.Platform.INTERACTSH, HttpLogService.getCurrentPlatform());
        assertEquals(DnsLogService.Platform.DNSLOG_CN, DnsLogService.getCurrentPlatform());

        Map<String, String> headers = HeaderManager.getInstance().getCustomHeaders();
        assertEquals("A-Building", headers.get("X-Building-Profile"));
        assertEquals("light-baseline", headers.get("X-Scan-Window"));
    }

    @Test
    @DisplayName("B楼代理剖面应启用漏扫代理并保留认证头与独立报告目录")
    void shouldApplyBuildingBProfile(@TempDir Path tempHome) throws Exception {
        configureProfile(tempHome, profileBJson(tempHome));

        VulnScanConfig config = VulnScanConfig.getInstance();
        assertTrue(config.isProxyEnabled());
        assertEquals("http://127.0.0.1:18081", config.getProxyAddress());
        assertEquals(tempHome.resolve("reports").resolve("building-b").toString(), config.getReportDirectory());

        Map<String, String> headers = HeaderManager.getInstance().getCustomHeaders();
        assertEquals("session=b-profile; tenant=ops", headers.get("Cookie"));
        assertEquals("Bearer building-b-token", headers.get("Authorization"));
        assertEquals("B-Building", headers.get("X-Building-Profile"));
    }

    @Test
    @DisplayName("C楼 OOB 剖面应切换到自定义 HTTP OOB 与 CEYE DNS")
    void shouldApplyBuildingCProfile(@TempDir Path tempHome) throws Exception {
        configureProfile(tempHome, profileCJson(tempHome));

        VulnScanConfig config = VulnScanConfig.getInstance();
        assertFalse(config.isProxyEnabled());
        assertEquals(tempHome.resolve("reports").resolve("building-c").toString(), config.getReportDirectory());

        invokeApplyOobSettings();

        assertEquals(HttpLogService.Platform.CUSTOM, HttpLogService.getCurrentPlatform());
        assertEquals(DnsLogService.Platform.CEYE_IO, DnsLogService.getCurrentPlatform());
    }

    private void configureProfile(Path tempHome, String json) throws Exception {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempHome.toString());

        Path configDir = tempHome.resolve(".PotatoTool");
        Files.createDirectories(configDir);
        Files.write(configDir.resolve("config.json"), json.getBytes(StandardCharsets.UTF_8));
        Constants.cachedConfig = null;
        resetSingleton(HeaderManager.class, "instance");
        resetSingleton(VulnScanConfig.class, "instance");
    }

    private void invokeApplyOobSettings() throws Exception {
        Method method = MainApplication.class.getDeclaredMethod("applyOobSettings");
        method.setAccessible(true);
        method.invoke(new MainApplication());
    }

    private void resetSingleton(Class<?> type, String fieldName) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, null);
    }

    private String profileAJson(Path tempHome) {
        return "{\n" +
                "  \"" + ConfigConstants.PROXY + "\": {\n" +
                "    \"" + ConfigConstants.PROXY_ENABLE + "\": false,\n" +
                "    \"" + ConfigConstants.PROXY_ADDRESS + "\": \"\",\n" +
                "    \"" + ConfigConstants.PROXY_SERVICES + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_SERVICE + "\": false\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.HTTP_HEADERS + "\": {\n" +
                "    \"" + ConfigConstants.HTTP_HEADERS_GLOBAL + "\": {\n" +
                "      \"X-Building-Profile\": \"A-Building\",\n" +
                "      \"X-Scan-Window\": \"light-baseline\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.VULNSCAN + "\": {\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT_DIR + "\": \"" + escape(tempHome.resolve("reports").resolve("building-a").toString()) + "\",\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR + "\": \"" + escape(tempHome.resolve("reports").resolve("building-a").toString()) + "\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.OOB + "\": {\n" +
                "    \"" + ConfigConstants.OOB_HTTP + "\": {\n" +
                "      \"" + ConfigConstants.OOB_HTTP_PLATFORM + "\": \"INTERACTSH\",\n" +
                "      \"" + ConfigConstants.OOB_HTTP_INTERACTSH_SERVER + "\": \"oast.pro\"\n" +
                "    },\n" +
                "    \"" + ConfigConstants.OOB_DNS + "\": {\n" +
                "      \"" + ConfigConstants.OOB_DNS_PLATFORM + "\": \"DNSLOG_CN\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private String profileBJson(Path tempHome) {
        return "{\n" +
                "  \"" + ConfigConstants.PROXY + "\": {\n" +
                "    \"" + ConfigConstants.PROXY_ENABLE + "\": true,\n" +
                "    \"" + ConfigConstants.PROXY_ADDRESS + "\": \"http://127.0.0.1:18081\",\n" +
                "    \"" + ConfigConstants.PROXY_SERVICES + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_SERVICE + "\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.HTTP_HEADERS + "\": {\n" +
                "    \"" + ConfigConstants.HTTP_HEADERS_GLOBAL + "\": {\n" +
                "      \"Authorization\": \"Bearer building-b-token\",\n" +
                "      \"Cookie\": \"session=b-profile; tenant=ops\",\n" +
                "      \"X-Building-Profile\": \"B-Building\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.VULNSCAN + "\": {\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT_DIR + "\": \"" + escape(tempHome.resolve("reports").resolve("building-b").toString()) + "\",\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR + "\": \"" + escape(tempHome.resolve("reports").resolve("building-b").toString()) + "\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private String profileCJson(Path tempHome) {
        return "{\n" +
                "  \"" + ConfigConstants.PROXY + "\": {\n" +
                "    \"" + ConfigConstants.PROXY_ENABLE + "\": false,\n" +
                "    \"" + ConfigConstants.PROXY_ADDRESS + "\": \"\",\n" +
                "    \"" + ConfigConstants.PROXY_SERVICES + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_SERVICE + "\": false\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.VULNSCAN + "\": {\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT_DIR + "\": \"" + escape(tempHome.resolve("reports").resolve("building-c").toString()) + "\",\n" +
                "    \"" + ConfigConstants.VULNSCAN_REPORT + "\": {\n" +
                "      \"" + ConfigConstants.VULNSCAN_REPORT_DEFAULT_DIR + "\": \"" + escape(tempHome.resolve("reports").resolve("building-c").toString()) + "\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"" + ConfigConstants.OOB + "\": {\n" +
                "    \"" + ConfigConstants.OOB_HTTP + "\": {\n" +
                "      \"" + ConfigConstants.OOB_HTTP_PLATFORM + "\": \"CUSTOM\",\n" +
                "      \"" + ConfigConstants.OOB_HTTP_CUSTOM_SERVER + "\": \"https://oob.local.test\",\n" +
                "      \"" + ConfigConstants.OOB_HTTP_CUSTOM_TOKEN + "\": \"local-oob-token\"\n" +
                "    },\n" +
                "    \"" + ConfigConstants.OOB_DNS + "\": {\n" +
                "      \"" + ConfigConstants.OOB_DNS_PLATFORM + "\": \"CEYE_IO\",\n" +
                "      \"" + ConfigConstants.OOB_DNS_CEYE_IDENTIFIER + "\": \"building-c\",\n" +
                "      \"" + ConfigConstants.OOB_DNS_CEYE_TOKEN + "\": \"ceye-token\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\");
    }
}

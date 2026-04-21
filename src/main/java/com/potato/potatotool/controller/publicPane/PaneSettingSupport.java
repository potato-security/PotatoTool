package com.potato.potatotool.controller.publicPane;

import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import com.potato.potatotool.utils.network.ProxyUtils;

import java.util.LinkedHashMap;
import java.util.Map;

final class PaneSettingSupport {

    private PaneSettingSupport() {
    }

    static void fillAiConfigMap(Map<String, Object> aiMap,
                                String providerValue,
                                String plainBaseUrl,
                                String plainApiKey,
                                String plainModelName,
                                int timeoutMs,
                                boolean thinkingEnabled,
                                int thinkingBudgetTokens) {
        aiMap.put(ConfigConstants.AI_PROVIDER, providerValue);
        aiMap.put(ConfigConstants.AI_BASE_URL, plainBaseUrl);
        aiMap.put(ConfigConstants.AI_API_KEY, plainApiKey);
        aiMap.put(ConfigConstants.AI_MODEL_NAME, plainModelName);
        aiMap.put(ConfigConstants.AI_TIMEOUT_MS, timeoutMs);

        Map<String, Object> thinkingMap = new LinkedHashMap<>();
        thinkingMap.put(ConfigConstants.AI_THINKING_ENABLED, thinkingEnabled);
        thinkingMap.put(ConfigConstants.AI_THINKING_BUDGET_TOKENS, thinkingBudgetTokens);
        aiMap.put(ConfigConstants.AI_THINKING, thinkingMap);
    }

    static boolean shouldBlockSaveForMainProxy(boolean proxyEnabled,
                                               String proxyAddress,
                                               ProxyUtils.ProxyReachabilityResult result) {
        if (!proxyEnabled) {
            return false;
        }
        return result == null || !result.isReachable();
    }

    static String validateDnsCeye(String selectedDnsPlatform,
                                  String ceyeIdentifierValue,
                                  String ceyeTokenValue,
                                  boolean forConnectivityTest) {
        if (!DnsLogService.Platform.CEYE_IO.name().equalsIgnoreCase(selectedDnsPlatform)) {
            return null;
        }
        if (ceyeIdentifierValue == null || ceyeIdentifierValue.trim().isEmpty()) {
            return forConnectivityTest
                    ? "setting.oob.dns.test.ceye.missing"
                    : "setting.oob.dns.ceye.identifier.required";
        }
        if (ceyeTokenValue == null || ceyeTokenValue.trim().isEmpty()) {
            return forConnectivityTest
                    ? "setting.oob.dns.test.ceye.missing"
                    : "setting.oob.dns.ceye.token.required";
        }
        return null;
    }

    static boolean shouldBlockDnsCeyeTest(String selectedDnsPlatform,
                                          String ceyeIdentifierValue,
                                          String ceyeTokenValue) {
        return validateDnsCeye(selectedDnsPlatform, ceyeIdentifierValue, ceyeTokenValue, true) != null;
    }
}

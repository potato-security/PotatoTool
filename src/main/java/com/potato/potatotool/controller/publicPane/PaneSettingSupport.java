package com.potato.potatotool.controller.publicPane;

import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import com.potato.potatotool.utils.network.ProxyUtils;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

final class PaneSettingSupport {

    private PaneSettingSupport() {
    }

    static void fillAiConfigMap(Map<String, Object> aiMap,
                                boolean useBuiltinGateway,
                                String providerValue,
                                String plainBaseUrl,
                                String plainApiKey,
                                String plainModelName,
                                int timeoutMs,
                                boolean thinkingEnabled,
                                int thinkingBudgetTokens) {
        aiMap.put(ConfigConstants.AI_USE_BUILTIN_GATEWAY, useBuiltinGateway);
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

    static String normalizeAiProviderDisplayValue(String providerValue) {
        String normalized = providerValue == null ? "" : providerValue.trim();
        if (normalized.isEmpty() || "OPENAI".equalsIgnoreCase(normalized)) {
            return "OPENAI";
        }
        return normalized.toUpperCase();
    }

    static String normalizeAiProviderConfigValue(String providerValue) {
        String normalized = providerValue == null ? "" : providerValue.trim();
        if (normalized.isEmpty()) {
            return "OPENAI";
        }
        if ("OPENAI".equalsIgnoreCase(normalized)) {
            return "OPENAI";
        }
        return normalized.toUpperCase();
    }

    static boolean supportsAiThinking(String providerValue, String modelName) {
        String provider = normalizeAiProviderConfigValue(providerValue);
        return "OPENAI".equals(provider)
                || "ANTHROPIC".equals(provider)
                || "GEMINI".equals(provider);
    }

    static boolean supportsAiThinkingBudget(String providerValue, String modelName) {
        String provider = normalizeAiProviderConfigValue(providerValue);
        return "ANTHROPIC".equals(provider);
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

    static String validateAiBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return null;
        }
        try {
            URI uri = URI.create(baseUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return "setting.ai.api.base.invalid";
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return "setting.ai.api.base.invalid";
            }
            return null;
        } catch (Exception ignored) {
            return "setting.ai.api.base.invalid";
        }
    }

    static boolean shouldBlockDnsCeyeTest(String selectedDnsPlatform,
                                          String ceyeIdentifierValue,
                                          String ceyeTokenValue) {
        return validateDnsCeye(selectedDnsPlatform, ceyeIdentifierValue, ceyeTokenValue, true) != null;
    }
}

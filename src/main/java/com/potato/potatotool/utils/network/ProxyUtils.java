package com.potato.potatotool.utils.network;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.I18nTextUtils;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;

public class ProxyUtils {

    public static class ProxyReachabilityResult {
        private final boolean reachable;
        private final String reasonKey;

        public ProxyReachabilityResult(boolean reachable, String reasonKey) {
            this.reachable = reachable;
            this.reasonKey = reasonKey;
        }

        public boolean isReachable() {
            return reachable;
        }

        public String getReasonKey() {
            return reasonKey;
        }
    }

    public static boolean isMainProxyEnabled() {
        try {
            JsonObject proxyConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.PROXY);
            if (proxyConfig != null && proxyConfig.has(ConfigConstants.PROXY_ENABLE)) {
                return proxyConfig.get(ConfigConstants.PROXY_ENABLE).getAsBoolean();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getMainProxyAddress() {
        try {
            JsonObject proxyConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.PROXY);
            if (proxyConfig != null && proxyConfig.has(ConfigConstants.PROXY_ADDRESS)) {
                return proxyConfig.get(ConfigConstants.PROXY_ADDRESS).getAsString();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isServiceProxyEnabled(String serviceName) {
        try {
            JsonObject proxyConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.PROXY);
            if (proxyConfig.has(ConfigConstants.PROXY_SERVICES) &&
                proxyConfig.get(ConfigConstants.PROXY_SERVICES).isJsonObject()) {
                JsonObject services = proxyConfig.getAsJsonObject(ConfigConstants.PROXY_SERVICES);
                if (services != null && services.has(serviceName)) {
                    return services.get(serviceName).getAsBoolean();
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isProxyActive(boolean serviceProxyEnabled) {
        if (!serviceProxyEnabled || !isMainProxyEnabled()) {
            return false;
        }
        String proxyAddress = getMainProxyAddress();
        return proxyAddress != null && !proxyAddress.trim().isEmpty();
    }

    public static boolean isServiceProxyActive(String serviceName) {
        return isProxyActive(isServiceProxyEnabled(serviceName));
    }

    public static void applyProxy(RequestObj requestObj, boolean serviceProxyEnabled) {
        if (requestObj == null) {
            return;
        }
        requestObj.setProxies(null);
        if (!isProxyActive(serviceProxyEnabled)) {
            return;
        }
        requestObj.setProxies(getMainProxyAddress().trim());
    }

    public static void applyServiceProxy(RequestObj requestObj, String serviceName) {
        applyProxy(requestObj, isServiceProxyEnabled(serviceName));
    }

    public static LinkedHashMap<String, Object> getServiceProxyStates() {
        LinkedHashMap<String, Object> servicesMap = new LinkedHashMap<>();
        try {
            JsonObject proxyConfig = (JsonObject) Constants.getOutsideConfig(ConfigConstants.PROXY);
            if (proxyConfig != null &&
                proxyConfig.has(ConfigConstants.PROXY_SERVICES) &&
                proxyConfig.get(ConfigConstants.PROXY_SERVICES).isJsonObject()) {
                JsonObject services = proxyConfig.getAsJsonObject(ConfigConstants.PROXY_SERVICES);
                for (String key : services.keySet()) {
                    servicesMap.put(key, services.get(key).getAsBoolean());
                }
            }
        } catch (Exception ignored) {
        }
        return servicesMap;
    }

    static LinkedHashMap<String, Object> mergeServiceProxyStates(LinkedHashMap<String, Object> currentStates,
                                                                 String serviceName,
                                                                 boolean enabled) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        if (currentStates != null) {
            merged.putAll(currentStates);
        }
        merged.put(serviceName, enabled);
        return merged;
    }

    public static boolean saveServiceProxyEnabled(String serviceName, boolean enabled) {
        LinkedHashMap<String, Object> servicesMap = mergeServiceProxyStates(getServiceProxyStates(), serviceName, enabled);

        LinkedHashMap<String, Object> proxyConfigMap = new LinkedHashMap<>();
        proxyConfigMap.put(ConfigConstants.PROXY_SERVICES, servicesMap);
        return Constants.saveConfig(proxyConfigMap, ConfigConstants.PROXY);
    }

    public static ProxyReachabilityResult checkProxyAddressReachability(String proxyAddress, int timeoutMs) {
        if (proxyAddress == null) {
            return new ProxyReachabilityResult(false, "setting.proxy.unavailable.invalid.address");
        }

        String trimmed = proxyAddress.trim();
        if (trimmed.isEmpty()) {
            return new ProxyReachabilityResult(false, "setting.proxy.unavailable.invalid.address");
        }

        String normalized = trimmed;
        int schemaIndex = normalized.indexOf("://");
        if (schemaIndex >= 0) {
            normalized = normalized.substring(schemaIndex + 3);
        }

        int separator = normalized.lastIndexOf(':');
        if (separator <= 0 || separator == normalized.length() - 1) {
            return new ProxyReachabilityResult(false, "setting.proxy.unavailable.invalid.address");
        }

        String host = normalized.substring(0, separator).trim();
        String portText = normalized.substring(separator + 1).trim();
        if (host.isEmpty()) {
            return new ProxyReachabilityResult(false, "setting.proxy.unavailable.invalid.address");
        }

        int port;
        try {
            port = Integer.parseInt(portText);
            if (port <= 0 || port > 65535) {
                return new ProxyReachabilityResult(false, "setting.proxy.unavailable.invalid.address");
            }
        } catch (NumberFormatException e) {
            return new ProxyReachabilityResult(false, "setting.proxy.unavailable.invalid.address");
        }

        try (Socket socket = new Socket()) {
            InetAddress resolvedAddress = InetAddress.getByName(host);
            socket.connect(new InetSocketAddress(resolvedAddress, port), timeoutMs);
            return new ProxyReachabilityResult(true, null);
        } catch (SocketTimeoutException e) {
            return new ProxyReachabilityResult(false, "setting.proxy.unavailable.timeout");
        } catch (UnknownHostException e) {
            return new ProxyReachabilityResult(false, "setting.proxy.unavailable.host");
        } catch (ConnectException e) {
            return new ProxyReachabilityResult(false, "setting.proxy.unavailable.refused");
        } catch (Exception e) {
            return new ProxyReachabilityResult(false, "setting.proxy.unavailable.generic");
        }
    }

    public static String getReasonKey(ProxyReachabilityResult result) {
        return result == null || result.getReasonKey() == null
                ? "setting.proxy.unavailable.generic"
                : result.getReasonKey();
    }

    public static String buildUnavailableMessage(ProxyReachabilityResult result) {
        return I18nTextUtils.getString(
                "setting.proxy.unavailable.disabled.reason",
                I18nTextUtils.getString(getReasonKey(result))
        );
    }

    public static ProxyReachabilityResult ensureMainProxyAvailability() {
        if (!isMainProxyEnabled()) {
            return new ProxyReachabilityResult(true, null);
        }

        String proxyAddress = getMainProxyAddress();
        ProxyReachabilityResult result = checkProxyAddressReachability(proxyAddress, 1500);
        if (result.isReachable()) {
            return result;
        }

        LinkedHashMap<String, Object> proxyMap = new LinkedHashMap<>();
        proxyMap.put(ConfigConstants.PROXY_ENABLE, false);
        Constants.saveConfig(proxyMap, ConfigConstants.PROXY);
        return result;
    }
}

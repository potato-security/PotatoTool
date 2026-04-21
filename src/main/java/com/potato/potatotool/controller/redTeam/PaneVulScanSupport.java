package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.utils.network.ProxyUtils;

final class PaneVulScanSupport {

    private PaneVulScanSupport() {
    }

    static boolean shouldBlockScanForProxy(boolean mainProxyEnabled,
                                           boolean vulnScanProxyEnabled,
                                           String proxyAddress,
                                           ProxyUtils.ProxyReachabilityResult result) {
        if (!mainProxyEnabled || !vulnScanProxyEnabled) {
            return false;
        }
        if (proxyAddress == null) {
            return true;
        }
        String trimmed = proxyAddress.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        boolean hasSupportedSchema = trimmed.startsWith("http://")
                || trimmed.startsWith("https://")
                || trimmed.startsWith("socks://");
        if (!hasSupportedSchema) {
            return true;
        }
        return result == null || !result.isReachable();
    }
}

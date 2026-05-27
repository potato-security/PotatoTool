package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.utils.network.ProxyUtils;

import java.util.concurrent.Future;

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

    static boolean cancelTaskBeforeEngineStart(boolean taskRunning,
                                               Future<?> task,
                                               Thread workerThread,
                                               boolean scanEngineRunning) {
        if (!taskRunning || task == null || scanEngineRunning) {
            return false;
        }
        cancelTask(task, workerThread);
        return true;
    }

    static boolean cancelRunningTask(boolean taskRunning, Future<?> task, Thread workerThread) {
        if (!taskRunning || task == null) {
            return false;
        }
        cancelTask(task, workerThread);
        return true;
    }

    private static void cancelTask(Future<?> task, Thread workerThread) {
        task.cancel(true);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }
}

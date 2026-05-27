package com.potato.potatotool.content.redTeam.vulnScanner.extractors.DslExtractor;

import com.potato.potatotool.content.redTeam.vulnScanner.util.ScanLogger;

/**
 * DSL 执行期日志上下文，避免并发扫描时用全局开关串扰。
 */
public final class DslLogContext {

    private static final ThreadLocal<Boolean> DEBUG_ENABLED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DslLogContext() {
    }

    public static void setDebugEnabled(boolean enabled) {
        DEBUG_ENABLED.set(enabled);
    }

    public static void clear() {
        DEBUG_ENABLED.remove();
    }

    public static void debug(String message) {
        if (Boolean.TRUE.equals(DEBUG_ENABLED.get())) {
            ScanLogger.getInstance().debug("DSL", message);
        }
    }

    public static void warn(String message) {
        ScanLogger.getInstance().warn("DSL", message);
    }

    public static void error(String message) {
        ScanLogger.getInstance().error("DSL", message);
    }
}

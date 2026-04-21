package com.potato.potatotool.utils.browser;

import java.util.List;
import java.util.Locale;

/**
 * Selects a target page from the DevTools target list.
 */
public interface CdpTargetSelector {
    CdpTargetInfo select(List<CdpTargetInfo> targets);

    static CdpTargetSelector firstPage() {
        return new CdpTargetSelector() {
            @Override
            public CdpTargetInfo select(List<CdpTargetInfo> targets) {
                if (targets == null) {
                    return null;
                }
                for (CdpTargetInfo target : targets) {
                    if (target != null && target.isPage() && !target.getWebSocketDebuggerUrl().isEmpty()) {
                        return target;
                    }
                }
                return null;
            }
        };
    }

    static CdpTargetSelector preferUrlContains(final String keyword) {
        return new CdpTargetSelector() {
            @Override
            public CdpTargetInfo select(List<CdpTargetInfo> targets) {
                if (targets == null) {
                    return null;
                }

                String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
                CdpTargetInfo fallback = null;
                for (CdpTargetInfo target : targets) {
                    if (target == null || !target.isPage() || target.getWebSocketDebuggerUrl().isEmpty()) {
                        continue;
                    }
                    if (fallback == null) {
                        fallback = target;
                    }
                    if (!normalizedKeyword.isEmpty()
                            && target.getUrl().toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                        return target;
                    }
                }
                return fallback;
            }
        };
    }
}

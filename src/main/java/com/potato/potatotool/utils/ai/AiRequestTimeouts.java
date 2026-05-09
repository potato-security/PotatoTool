package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.network.RequestObj;

/**
 * AI 请求超时策略。
 * 普通请求沿用统一超时；流式请求取消整体 callTimeout，并放宽 readTimeout。
 */
public final class AiRequestTimeouts {

    private static final int STREAM_CONNECT_TIMEOUT_MIN_SEC = 10;
    private static final int STREAM_CONNECT_TIMEOUT_MAX_SEC = 60;
    private static final int STREAM_WRITE_TIMEOUT_MIN_SEC = 60;
    private static final int STREAM_READ_TIMEOUT_MIN_SEC = 300;

    private AiRequestTimeouts() {
    }

    public static RequestObj apply(RequestObj requestObj, AiRuntimeConfig runtimeConfig, boolean stream) {
        int configuredTimeoutSec = toSeconds(runtimeConfig == null ? 60000 : runtimeConfig.getTimeoutMs());
        return stream ? applyStreaming(requestObj, configuredTimeoutSec) : applyStandard(requestObj, configuredTimeoutSec);
    }

    public static RequestObj applyStandard(RequestObj requestObj, AiRuntimeConfig runtimeConfig) {
        return applyStandard(requestObj, toSeconds(runtimeConfig == null ? 60000 : runtimeConfig.getTimeoutMs()));
    }

    private static RequestObj applyStandard(RequestObj requestObj, int timeoutSec) {
        int effectiveTimeoutSec = Math.max(1, timeoutSec);
        return requestObj
                .setTimeOut(effectiveTimeoutSec)
                .setReadTimeout(effectiveTimeoutSec)
                .setWriteTimeout(effectiveTimeoutSec)
                .setCallTimeout(effectiveTimeoutSec);
    }

    private static RequestObj applyStreaming(RequestObj requestObj, int timeoutSec) {
        int effectiveTimeoutSec = Math.max(1, timeoutSec);
        int connectTimeoutSec = Math.max(STREAM_CONNECT_TIMEOUT_MIN_SEC,
                Math.min(effectiveTimeoutSec, STREAM_CONNECT_TIMEOUT_MAX_SEC));
        int writeTimeoutSec = Math.max(effectiveTimeoutSec, STREAM_WRITE_TIMEOUT_MIN_SEC);
        int readTimeoutSec = Math.max(effectiveTimeoutSec, STREAM_READ_TIMEOUT_MIN_SEC);
        return requestObj
                .setTimeOut(connectTimeoutSec)
                .setReadTimeout(readTimeoutSec)
                .setWriteTimeout(writeTimeoutSec)
                .setCallTimeout(0);
    }

    private static int toSeconds(int timeoutMs) {
        return Math.max(1, (timeoutMs + 999) / 1000);
    }
}

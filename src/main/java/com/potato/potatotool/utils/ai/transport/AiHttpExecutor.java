package com.potato.potatotool.utils.ai.transport;

import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.network.RequestUtils;

public final class AiHttpExecutor {

    private AiHttpExecutor() {
    }

    public static CustomHttpResponse requests(RequestObj requestObj) throws Exception {
        if (AiGatewayClientFactory.shouldUseSecureClient(requestObj)) {
            return RequestUtils.requests(requestObj, AiGatewayClientFactory.createClient(requestObj));
        }
        return RequestUtils.requests(requestObj);
    }
}

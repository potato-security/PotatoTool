package com.potato.potatotool.utils.ai.provider;

import com.potato.potatotool.utils.ai.AiAttachmentSupportResolver;
import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiChatRequest;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.model.AiStreamEvent;
import com.potato.potatotool.utils.network.RequestObj;

import java.util.List;

public interface AiProviderAdapter {
    default AiAttachmentSupportResult resolveAttachmentSupport(AiRuntimeConfig runtimeConfig, List<AiAttachment> attachments) {
        return AiAttachmentSupportResolver.resolve(runtimeConfig, attachments);
    }

    default boolean supportsAttachments(AiRuntimeConfig runtimeConfig) {
        return resolveAttachmentSupport(runtimeConfig, null).isSupported();
    }

    default String unsupportedAttachmentsMessage(AiRuntimeConfig runtimeConfig) {
        return resolveAttachmentSupport(runtimeConfig, null).getMessage().trim().isEmpty()
                ? "当前 AI 接口暂不支持附件上传"
                : resolveAttachmentSupport(runtimeConfig, null).getMessage();
    }

    default PreparedRequest prepareRequest(AiRuntimeConfig runtimeConfig, AiChatRequest request) throws Exception {
        return PreparedRequest.of(buildRequest(runtimeConfig, request));
    }

    RequestObj buildRequest(AiRuntimeConfig runtimeConfig, AiChatRequest request);

    List<AiStreamEvent> parseSseLine(String line);

    default String parseResponse(String body) {
        return "";
    }

    final class PreparedRequest {
        private final RequestObj requestObj;
        private final CleanupAction cleanupAction;

        private PreparedRequest(RequestObj requestObj, CleanupAction cleanupAction) {
            this.requestObj = requestObj;
            this.cleanupAction = cleanupAction;
        }

        public static PreparedRequest of(RequestObj requestObj) {
            return new PreparedRequest(requestObj, null);
        }

        public static PreparedRequest of(RequestObj requestObj, CleanupAction cleanupAction) {
            return new PreparedRequest(requestObj, cleanupAction);
        }

        public RequestObj getRequestObj() {
            return requestObj;
        }

        public void cleanup() {
            if (cleanupAction == null) {
                return;
            }
            try {
                cleanupAction.run();
            } catch (Exception ignored) {
            }
        }
    }

    interface CleanupAction {
        void run() throws Exception;
    }
}

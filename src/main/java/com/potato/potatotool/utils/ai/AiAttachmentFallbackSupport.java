package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiChatRequest;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.core.I18nTextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AiAttachmentFallbackSupport {

    private AiAttachmentFallbackSupport() {
    }

    public static boolean shouldFallbackAfterUploadFailure(Exception exception) {
        if (exception == null) {
            return false;
        }
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("404")
                || normalized.contains("405")
                || normalized.contains("415")
                || normalized.contains("501")
                || normalized.contains("unsupported")
                || normalized.contains("not support")
                || normalized.contains("not implemented")
                || normalized.contains("upload failed")
                || normalized.contains("files api")
                || normalized.contains("/files");
    }

    public static FallbackPlan buildFallbackPlan(AiProviderType providerType,
                                                 AiChatRequest request,
                                                 List<AiAttachment> attachments) throws Exception {
        if (providerType == null || request == null || attachments == null || attachments.isEmpty()) {
            return null;
        }

        List<AiAttachment> inlineAttachments = new ArrayList<AiAttachment>();
        List<AiAttachmentUtils.TextContextResult> textContexts = new ArrayList<AiAttachmentUtils.TextContextResult>();
        long totalTextBytes = 0L;

        for (AiAttachment attachment : attachments) {
            if (attachment == null) {
                continue;
            }

            if (AiAttachmentUtils.supportsRelayInline(providerType, attachment)) {
                inlineAttachments.add(attachment);
                continue;
            }

            AiAttachmentUtils.TextContextResult textContext = AiAttachmentUtils.readTextContext(attachment);
            if (!textContext.isSupported()) {
                return null;
            }

            totalTextBytes += textContext.getOriginalSizeBytes();
            if (totalTextBytes > AiAttachmentUtils.MAX_TEXT_CONTEXT_TOTAL_SIZE_BYTES) {
                return null;
            }
            textContexts.add(textContext);
        }

        String mergedQuestion = mergeQuestionWithAttachmentContext(
                request.getQuestion(),
                buildTextContextSuffix(textContexts)
        );

        AiChatRequest fallbackRequest = new AiChatRequest(
                mergedQuestion,
                request.getHistoryQuestion(),
                request.getHistory(),
                inlineAttachments,
                request.getThinkingConfig(),
                request.isStream(),
                request.getSystemPrompt()
        );

        return new FallbackPlan(fallbackRequest, inlineAttachments);
    }

    public static String buildTextContextSuffix(List<AiAttachmentUtils.TextContextResult> textContexts) {
        if (textContexts == null || textContexts.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n\n")
                .append(I18nTextUtils.getString("ai.prompt.section.text.attachment.context"))
                .append("\n")
                .append(I18nTextUtils.getString("ai.prompt.text.attachment.context.notes"))
                .append("\n\n");

        for (int i = 0; i < textContexts.size(); i++) {
            AiAttachmentUtils.TextContextResult textContext = textContexts.get(i);
            if (textContext == null || !textContext.isSupported()) {
                continue;
            }
            builder.append("### ")
                    .append(I18nTextUtils.getString("ai.prompt.text.attachment.label"))
                    .append(i + 1)
                    .append("\n")
                    .append(I18nTextUtils.getString("ai.prompt.file.name"))
                    .append(": ")
                    .append(textContext.getFileName())
                    .append("\n")
                    .append(I18nTextUtils.getString("ai.prompt.file.charset"))
                    .append(": ")
                    .append(textContext.getCharsetName())
                    .append("\n")
                    .append("<file-content>\n")
                    .append(textContext.getText())
                    .append("\n</file-content>\n\n");
        }

        return builder.toString().trim();
    }

    private static String mergeQuestionWithAttachmentContext(String question, String questionSuffix) {
        String baseQuestion = question == null ? "" : question.trim();
        String suffix = questionSuffix == null ? "" : questionSuffix.trim();
        if (suffix.isEmpty()) {
            return baseQuestion;
        }
        if (baseQuestion.isEmpty()) {
            return suffix;
        }
        return baseQuestion + "\n\n" + suffix;
    }

    public static final class FallbackPlan {
        private final AiChatRequest request;
        private final List<AiAttachment> inlineAttachments;

        private FallbackPlan(AiChatRequest request, List<AiAttachment> inlineAttachments) {
            this.request = request;
            this.inlineAttachments = inlineAttachments == null
                    ? new ArrayList<AiAttachment>()
                    : new ArrayList<AiAttachment>(inlineAttachments);
        }

        public AiChatRequest getRequest() {
            return request;
        }

        public List<AiAttachment> getInlineAttachments() {
            return new ArrayList<AiAttachment>(inlineAttachments);
        }
    }
}

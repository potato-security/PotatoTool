package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.core.I18nTextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiPromptUtils {
    private AiPromptUtils() {
    }

    public static String generalAssistantSystemPrompt() {
        return I18nTextUtils.getString("ai.prompt.general.system");
    }

    public static String buildConversationPrompt(String userQuestion, List<AiAttachment> attachments) {
        String normalizedQuestion = trimToEmpty(userQuestion);
        List<AiAttachment> safeAttachments = attachments == null
                ? Collections.<AiAttachment>emptyList()
                : attachments;
        if (safeAttachments.isEmpty()) {
            return normalizedQuestion;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(I18nTextUtils.getString("ai.prompt.section.user.goal"))
                .append("\n")
                .append(normalizedQuestion.isEmpty() ? defaultAttachmentAnalysisRequest() : normalizedQuestion)
                .append("\n\n")
                .append(I18nTextUtils.getString("ai.prompt.section.attachment.notes"))
                .append("\n")
                .append(I18nTextUtils.getString("ai.prompt.conversation.attachment.notes"))
                .append("\n\n");

        for (int i = 0; i < safeAttachments.size(); i++) {
            AiAttachment attachment = safeAttachments.get(i);
            if (attachment == null) {
                continue;
            }
            builder.append("### ")
                    .append(I18nTextUtils.getString("ai.prompt.attachment.label"))
                    .append(i + 1)
                    .append("\n")
                    .append(I18nTextUtils.getString("ai.prompt.file.name"))
                    .append(": ")
                    .append(trimToEmpty(attachment.getFileName()))
                    .append("\n")
                    .append(I18nTextUtils.getString("ai.prompt.file.size"))
                    .append(": ")
                    .append(attachment.getFileSize())
                    .append(" bytes\n")
                    .append("MIME: ")
                    .append(trimToEmpty(attachment.getMimeType()))
                    .append("\n\n");
        }

        builder.append(I18nTextUtils.getString("ai.prompt.conversation.final.instruction"));
        return builder.toString().trim();
    }

    public static String buildConversationHistoryLabel(String userQuestion, List<AiAttachment> attachments) {
        String normalizedQuestion = trimToEmpty(userQuestion);
        if (attachments == null || attachments.isEmpty()) {
            return normalizedQuestion;
        }

        List<String> fileNames = new ArrayList<String>();
        for (AiAttachment attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            String fileName = trimToEmpty(attachment.getFileName());
            if (!fileName.isEmpty()) {
                fileNames.add(fileName);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append(normalizedQuestion.isEmpty() ? defaultAttachmentAnalysisRequest() : normalizedQuestion);
        if (!fileNames.isEmpty()) {
            builder.append("\n[")
                    .append(I18nTextUtils.getString("ai.prompt.attachment.summary.label"))
                    .append(": ");
            for (int i = 0; i < fileNames.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(fileNames.get(i));
            }
            builder.append("]");
        }
        return builder.toString();
    }

    public static String attachmentAnalysisRequest() {
        return defaultAttachmentAnalysisRequest();
    }

    public static String codeOptimizationSystemPrompt() {
        return I18nTextUtils.getString("ai.prompt.code.optimize.system");
    }

    public static String buildCodeOptimizationPrompt(String code) {
        return I18nTextUtils.getString("ai.prompt.code.optimize.user", trimToEmpty(code));
    }

    public static String securityAnalysisSystemPrompt() {
        return I18nTextUtils.getString("ai.prompt.security.system");
    }

    public static String buildSecurityAnalysisPrompt(String evilCode, String encodeModes) {
        return I18nTextUtils.getString(
                "ai.prompt.security.analysis.user",
                trimToEmpty(encodeModes),
                trimToEmpty(evilCode)
        );
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String defaultAttachmentAnalysisRequest() {
        return I18nTextUtils.getString("ai.prompt.attachment.default.request");
    }
}

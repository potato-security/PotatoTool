package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiAttachmentDispatchPlan;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.ai.provider.AiProviderAdapter;
import com.potato.potatotool.utils.core.I18nTextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiAttachmentDispatchPlanner {

    private AiAttachmentDispatchPlanner() {
    }

    public static AiAttachmentDispatchPlan plan(AiRuntimeConfig runtimeConfig,
                                                AiProviderAdapter providerAdapter,
                                                List<AiAttachment> attachments) {
        List<AiAttachment> safeAttachments = normalizeAttachments(attachments);
        if (safeAttachments.isEmpty()) {
            return AiAttachmentDispatchPlan.supported(Collections.<AiAttachment>emptyList(), "");
        }
        if (runtimeConfig == null || providerAdapter == null) {
            return AiAttachmentDispatchPlan.unsupported(I18nTextUtils.getString("ai.attach.error.config.missing"));
        }
        if (safeAttachments.size() > AiAttachmentUtils.MAX_ATTACHMENT_COUNT) {
            return AiAttachmentDispatchPlan.unsupported(
                    I18nTextUtils.getString("ai.attach.error.count.limit", AiAttachmentUtils.MAX_ATTACHMENT_COUNT)
            );
        }

        AiAttachmentSupportResult allSupport = providerAdapter.resolveAttachmentSupport(runtimeConfig, safeAttachments);
        if (allSupport.isSupported()) {
            return AiAttachmentDispatchPlan.supported(safeAttachments, "");
        }

        List<AiAttachment> requestAttachments = new ArrayList<AiAttachment>();
        List<AiAttachmentUtils.TextContextResult> textContexts = new ArrayList<AiAttachmentUtils.TextContextResult>();
        long totalTextBytes = 0L;

        for (AiAttachment attachment : safeAttachments) {
            AiAttachmentSupportResult singleSupport = providerAdapter.resolveAttachmentSupport(
                    runtimeConfig,
                    Collections.singletonList(attachment)
            );
            if (singleSupport.isSupported()) {
                requestAttachments.add(attachment);
                continue;
            }

            AiAttachmentUtils.TextContextResult textContext;
            try {
                textContext = AiAttachmentUtils.readTextContext(attachment);
            } catch (Exception e) {
                return AiAttachmentDispatchPlan.unsupported(
                        I18nTextUtils.getString(
                                "ai.attach.read.failed",
                                e.getMessage() == null ? safeName(attachment) : e.getMessage()
                        )
                );
            }

            if (!textContext.isSupported()) {
                if (textContext.isTooLarge()) {
                    return AiAttachmentDispatchPlan.unsupported(
                            I18nTextUtils.getString(
                                    "ai.attach.error.text.context.single.limit",
                                    formatSize(textContext.getLimitBytes()),
                                    safeName(attachment)
                            )
                    );
                }
                String singleMessage = singleSupport.getMessage();
                if (singleMessage == null || singleMessage.trim().isEmpty()) {
                    singleMessage = I18nTextUtils.getString("ai.attach.error.text.context.uncertain", safeName(attachment));
                }
                return AiAttachmentDispatchPlan.unsupported(singleMessage);
            }

            totalTextBytes += textContext.getOriginalSizeBytes();
            if (totalTextBytes > AiAttachmentUtils.MAX_TEXT_CONTEXT_TOTAL_SIZE_BYTES) {
                return AiAttachmentDispatchPlan.unsupported(
                        I18nTextUtils.getString(
                                "ai.attach.error.text.context.total.limit",
                                formatSize(AiAttachmentUtils.MAX_TEXT_CONTEXT_TOTAL_SIZE_BYTES)
                        )
                );
            }
            textContexts.add(textContext);
        }

        AiAttachmentSupportResult requestSupport = providerAdapter.resolveAttachmentSupport(runtimeConfig, requestAttachments);
        if (!requestSupport.isSupported()) {
            return AiAttachmentDispatchPlan.unsupported(requestSupport.getMessage());
        }

        return AiAttachmentDispatchPlan.supported(
                requestAttachments,
                AiAttachmentFallbackSupport.buildTextContextSuffix(textContexts)
        );
    }

    private static List<AiAttachment> normalizeAttachments(List<AiAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }
        List<AiAttachment> safeAttachments = new ArrayList<AiAttachment>();
        for (AiAttachment attachment : attachments) {
            if (attachment != null) {
                safeAttachments.add(attachment);
            }
        }
        return safeAttachments;
    }

    private static String safeName(AiAttachment attachment) {
        if (attachment == null || attachment.getFileName() == null || attachment.getFileName().trim().isEmpty()) {
            return "unknown";
        }
        return attachment.getFileName().trim();
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0L) {
            return "0 B";
        }
        if (bytes >= 1024L * 1024L) {
            return String.valueOf(Math.max(1L, bytes / (1024L * 1024L))) + " MB";
        }
        return String.valueOf(Math.max(1L, bytes / 1024L)) + " KB";
    }
}

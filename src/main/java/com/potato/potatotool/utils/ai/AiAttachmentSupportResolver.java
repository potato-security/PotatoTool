package com.potato.potatotool.utils.ai;

import com.potato.potatotool.utils.ai.model.AiAttachment;
import com.potato.potatotool.utils.ai.model.AiAttachmentMode;
import com.potato.potatotool.utils.ai.model.AiAttachmentSupportResult;
import com.potato.potatotool.utils.ai.model.AiProviderType;
import com.potato.potatotool.utils.ai.model.AiRuntimeConfig;
import com.potato.potatotool.utils.core.I18nTextUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiAttachmentSupportResolver {

    private AiAttachmentSupportResolver() {
    }

    public static AiAttachmentSupportResult resolve(AiRuntimeConfig runtimeConfig, List<AiAttachment> attachments) {
        List<AiAttachment> safeAttachments = normalizeAttachments(attachments);
        if (safeAttachments.isEmpty()) {
            return AiAttachmentSupportResult.supported(
                    AiAttachmentMode.NONE,
                    AiAttachmentUtils.MAX_ATTACHMENT_COUNT,
                    AiAttachmentUtils.MAX_ATTACHMENT_SIZE_BYTES,
                    AiAttachmentUtils.MAX_TOTAL_ATTACHMENT_SIZE_BYTES
            );
        }
        if (runtimeConfig == null) {
            return AiAttachmentSupportResult.unsupported(
                    I18nTextUtils.getString("ai.attach.error.config.missing"),
                    AiAttachmentMode.NONE,
                    AiAttachmentUtils.MAX_ATTACHMENT_COUNT,
                    AiAttachmentUtils.MAX_ATTACHMENT_SIZE_BYTES,
                    AiAttachmentUtils.MAX_TOTAL_ATTACHMENT_SIZE_BYTES
            );
        }

        AiProviderType providerType = runtimeConfig.getProviderType();
        if (providerType == AiProviderType.OPENAI) {
            return resolveOpenAiCompatible(runtimeConfig, safeAttachments);
        }
        if (isOfficialHost(runtimeConfig)) {
            return resolveOfficial(providerType, safeAttachments);
        }
        return resolveRelay(providerType, safeAttachments);
    }

    private static AiAttachmentSupportResult resolveOpenAiCompatible(AiRuntimeConfig runtimeConfig,
                                                                     List<AiAttachment> attachments) {
        String host = resolveHost(runtimeConfig == null ? "" : runtimeConfig.getBaseUrl());
        String modelName = normalizeModelName(runtimeConfig == null ? "" : runtimeConfig.getModelName());

        if ("api.openai.com".equalsIgnoreCase(host)) {
            return resolveOfficial(AiProviderType.OPENAI, attachments);
        }
        if (isDeepSeekOfficialHost(host)) {
            return unsupported(
                    I18nTextUtils.getString("ai.attach.error.deepseek.text.only", runtimeConfig == null ? "" : runtimeConfig.getModelName()),
                    AiAttachmentMode.NONE,
                    AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                    AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
            );
        }
        if (isGlmOfficialHost(host)) {
            if (supportsGlmVisionModel(modelName)) {
                return resolveOpenAiInlineImage(attachments);
            }
            return unsupported(
                    I18nTextUtils.getString("ai.attach.error.glm.visual.only", runtimeConfig == null ? "" : runtimeConfig.getModelName()),
                    AiAttachmentMode.NONE,
                    AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                    AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
            );
        }
        if (isQwenOfficialHost(host)) {
            if (supportsQwenVisionModel(modelName)) {
                return resolveOpenAiInlineImage(attachments);
            }
            return unsupported(
                    I18nTextUtils.getString("ai.attach.error.qwen.visual.only", runtimeConfig == null ? "" : runtimeConfig.getModelName()),
                    AiAttachmentMode.NONE,
                    AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                    AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
            );
        }
        if (isKnownDeepSeekTextModel(modelName)
                || isKnownGlmTextModel(modelName)
                || isKnownQwenTextOnlyModel(modelName)) {
            return unsupported(
                    I18nTextUtils.getString("ai.attach.error.model.text.only", runtimeConfig == null ? "" : runtimeConfig.getModelName()),
                    AiAttachmentMode.NONE,
                    AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                    AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
            );
        }
        return resolveOfficial(AiProviderType.OPENAI, attachments);
    }

    private static AiAttachmentSupportResult resolveOpenAiInlineImage(List<AiAttachment> attachments) {
        String error = validateCommonLimits(
                attachments,
                AiAttachmentUtils.MAX_ATTACHMENT_COUNT,
                AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
        );
        if (!error.isEmpty()) {
            return unsupported(
                    error,
                    AiAttachmentMode.INLINE_IMAGE,
                    AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                    AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
            );
        }

        for (AiAttachment attachment : attachments) {
            if (!AiAttachmentUtils.isImage(attachment)) {
                return unsupported(
                        I18nTextUtils.getString("ai.attach.error.relay.openai.image.only", safeName(attachment)),
                        AiAttachmentMode.INLINE_IMAGE,
                        AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                        AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
                );
            }
        }

        return AiAttachmentSupportResult.supported(
                AiAttachmentMode.INLINE_IMAGE,
                AiAttachmentUtils.MAX_ATTACHMENT_COUNT,
                AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
        );
    }

    private static AiAttachmentSupportResult resolveOfficial(AiProviderType providerType, List<AiAttachment> attachments) {
        String error = validateCommonLimits(
                attachments,
                AiAttachmentUtils.MAX_ATTACHMENT_COUNT,
                AiAttachmentUtils.MAX_ATTACHMENT_SIZE_BYTES,
                AiAttachmentUtils.MAX_TOTAL_ATTACHMENT_SIZE_BYTES
        );
        if (!error.isEmpty()) {
            return unsupported(error, AiAttachmentMode.REMOTE_UPLOAD_REFERENCE,
                    AiAttachmentUtils.MAX_ATTACHMENT_SIZE_BYTES,
                    AiAttachmentUtils.MAX_TOTAL_ATTACHMENT_SIZE_BYTES);
        }

        for (AiAttachment attachment : attachments) {
            if (!AiAttachmentUtils.isSupportedForUpload(attachment)) {
                return unsupported(
                        I18nTextUtils.getString("ai.attach.error.type.unsupported", safeName(attachment)),
                        AiAttachmentMode.REMOTE_UPLOAD_REFERENCE,
                        AiAttachmentUtils.MAX_ATTACHMENT_SIZE_BYTES,
                        AiAttachmentUtils.MAX_TOTAL_ATTACHMENT_SIZE_BYTES
                );
            }
            if (!AiAttachmentUtils.isSupportedByProvider(providerType, attachment)) {
                String name = safeName(attachment);
                if (providerType == AiProviderType.ANTHROPIC) {
                    return unsupported(
                            I18nTextUtils.getString("ai.attach.error.anthropic.unsupported", name),
                            AiAttachmentMode.REMOTE_UPLOAD_REFERENCE,
                            AiAttachmentUtils.MAX_ATTACHMENT_SIZE_BYTES,
                            AiAttachmentUtils.MAX_TOTAL_ATTACHMENT_SIZE_BYTES
                    );
                }
                return unsupported(
                        I18nTextUtils.getString("ai.attach.error.type.unsupported", name),
                        AiAttachmentMode.REMOTE_UPLOAD_REFERENCE,
                        AiAttachmentUtils.MAX_ATTACHMENT_SIZE_BYTES,
                        AiAttachmentUtils.MAX_TOTAL_ATTACHMENT_SIZE_BYTES
                );
            }
        }

        return AiAttachmentSupportResult.supported(
                AiAttachmentMode.REMOTE_UPLOAD_REFERENCE,
                AiAttachmentUtils.MAX_ATTACHMENT_COUNT,
                AiAttachmentUtils.MAX_ATTACHMENT_SIZE_BYTES,
                AiAttachmentUtils.MAX_TOTAL_ATTACHMENT_SIZE_BYTES
        );
    }

    private static AiAttachmentSupportResult resolveRelay(AiProviderType providerType, List<AiAttachment> attachments) {
        String error = validateCommonLimits(
                attachments,
                AiAttachmentUtils.MAX_ATTACHMENT_COUNT,
                AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
        );
        AiAttachmentMode mode = relayMode(providerType);
        if (!error.isEmpty()) {
            return unsupported(
                    error,
                    mode,
                    AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                    AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
            );
        }

        for (AiAttachment attachment : attachments) {
            if (!AiAttachmentUtils.supportsRelayInline(providerType, attachment)) {
                return unsupported(
                        relayUnsupportedMessage(providerType, attachment),
                        mode,
                        AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                        AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
                );
            }
        }

        return AiAttachmentSupportResult.supported(
                mode,
                AiAttachmentUtils.MAX_ATTACHMENT_COUNT,
                AiAttachmentUtils.MAX_INLINE_ATTACHMENT_SIZE_BYTES,
                AiAttachmentUtils.MAX_INLINE_TOTAL_ATTACHMENT_SIZE_BYTES
        );
    }

    private static String validateCommonLimits(List<AiAttachment> attachments,
                                               int maxAttachmentCount,
                                               long maxAttachmentSizeBytes,
                                               long maxTotalAttachmentSizeBytes) {
        if (attachments.size() > maxAttachmentCount) {
            return I18nTextUtils.getString("ai.attach.error.count.limit", maxAttachmentCount);
        }

        long totalSize = 0L;
        for (AiAttachment attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            long fileSize = Math.max(0L, attachment.getFileSize());
            if (fileSize > maxAttachmentSizeBytes) {
                return I18nTextUtils.getString(
                        "ai.attach.error.single.limit",
                        formatMb(maxAttachmentSizeBytes),
                        safeName(attachment)
                );
            }
            totalSize += fileSize;
        }

        if (totalSize > maxTotalAttachmentSizeBytes) {
            return I18nTextUtils.getString("ai.attach.error.total.limit", formatMb(maxTotalAttachmentSizeBytes));
        }
        return "";
    }

    private static AiAttachmentSupportResult unsupported(String message,
                                                         AiAttachmentMode mode,
                                                         long maxAttachmentSizeBytes,
                                                         long maxTotalAttachmentSizeBytes) {
        return AiAttachmentSupportResult.unsupported(
                message,
                mode,
                AiAttachmentUtils.MAX_ATTACHMENT_COUNT,
                maxAttachmentSizeBytes,
                maxTotalAttachmentSizeBytes
        );
    }

    private static AiAttachmentMode relayMode(AiProviderType providerType) {
        return providerType == AiProviderType.GEMINI
                ? AiAttachmentMode.INLINE_MEDIA
                : AiAttachmentMode.INLINE_IMAGE;
    }

    private static String relayUnsupportedMessage(AiProviderType providerType, AiAttachment attachment) {
        String fileName = safeName(attachment);
        if (providerType == AiProviderType.ANTHROPIC) {
            return I18nTextUtils.getString("ai.attach.error.relay.anthropic.image.only", fileName);
        }
        if (providerType == AiProviderType.GEMINI) {
            return I18nTextUtils.getString("ai.attach.error.relay.gemini.media.only", fileName);
        }
        return I18nTextUtils.getString("ai.attach.error.relay.openai.image.only", fileName);
    }

    private static boolean isOfficialHost(AiRuntimeConfig runtimeConfig) {
        if (runtimeConfig == null) {
            return false;
        }
        String host = resolveHost(runtimeConfig.getBaseUrl());
        if (host.isEmpty()) {
            return false;
        }
        if (runtimeConfig.getProviderType() == AiProviderType.ANTHROPIC) {
            return "api.anthropic.com".equalsIgnoreCase(host);
        }
        if (runtimeConfig.getProviderType() == AiProviderType.GEMINI) {
            return "generativelanguage.googleapis.com".equalsIgnoreCase(host);
        }
        return "api.openai.com".equalsIgnoreCase(host);
    }

    private static boolean isGlmOfficialHost(String host) {
        return "open.bigmodel.cn".equalsIgnoreCase(host);
    }

    private static boolean isQwenOfficialHost(String host) {
        return "dashscope.aliyuncs.com".equalsIgnoreCase(host)
                || "dashscope-intl.aliyuncs.com".equalsIgnoreCase(host)
                || "dashscope-us.aliyuncs.com".equalsIgnoreCase(host);
    }

    private static boolean isDeepSeekOfficialHost(String host) {
        return "api.deepseek.com".equalsIgnoreCase(host);
    }

    private static boolean supportsGlmVisionModel(String modelName) {
        if (modelName.isEmpty()) {
            return false;
        }
        return modelName.startsWith("glm-4v")
                || modelName.startsWith("glm-4.5v")
                || modelName.startsWith("glm-4.6v")
                || modelName.contains("-4v-")
                || modelName.contains("-4.5v-")
                || modelName.contains("-4.6v-");
    }

    private static boolean supportsQwenVisionModel(String modelName) {
        if (modelName.isEmpty()) {
            return false;
        }
        return modelName.startsWith("qwen3.6-")
                || modelName.startsWith("qwen3.5-")
                || modelName.startsWith("qwen3-vl")
                || modelName.startsWith("qwen2.5-vl")
                || modelName.startsWith("qwen-vl")
                || modelName.startsWith("qvq-")
                || modelName.contains("omni");
    }

    private static boolean isKnownDeepSeekTextModel(String modelName) {
        if (modelName.isEmpty()) {
            return false;
        }
        return "deepseek-chat".equals(modelName)
                || "deepseek-reasoner".equals(modelName)
                || modelName.startsWith("deepseek-v3")
                || modelName.startsWith("deepseek-r1");
    }

    private static boolean isKnownGlmTextModel(String modelName) {
        if (modelName.isEmpty()) {
            return false;
        }
        if (supportsGlmVisionModel(modelName)) {
            return false;
        }
        return modelName.startsWith("glm-5")
                || modelName.startsWith("glm-4.7")
                || modelName.startsWith("glm-4.6")
                || modelName.startsWith("glm-4-plus")
                || modelName.startsWith("glm-4-air")
                || modelName.startsWith("glm-4-airx")
                || modelName.startsWith("glm-4-flash");
    }

    private static boolean isKnownQwenTextOnlyModel(String modelName) {
        if (modelName.isEmpty()) {
            return false;
        }
        if (supportsQwenVisionModel(modelName)) {
            return false;
        }
        return modelName.startsWith("qwen3-max")
                || modelName.startsWith("qwen3-coder")
                || modelName.startsWith("qwen-long")
                || modelName.startsWith("qwen-doc")
                || modelName.startsWith("qwen-math");
    }

    private static String resolveHost(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "";
        }
        try {
            String host = URI.create(baseUrl.trim()).getHost();
            return host == null ? "" : host.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String normalizeModelName(String modelName) {
        return modelName == null ? "" : modelName.trim().toLowerCase();
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

    private static String formatMb(long bytes) {
        return String.valueOf(Math.max(1L, bytes / (1024L * 1024L))) + " MB";
    }
}

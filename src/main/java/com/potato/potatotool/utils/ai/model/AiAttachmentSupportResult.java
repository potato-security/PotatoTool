package com.potato.potatotool.utils.ai.model;

public class AiAttachmentSupportResult {
    private final boolean supported;
    private final AiAttachmentMode mode;
    private final String message;
    private final int maxAttachmentCount;
    private final long maxAttachmentSizeBytes;
    private final long maxTotalAttachmentSizeBytes;

    private AiAttachmentSupportResult(boolean supported,
                                      AiAttachmentMode mode,
                                      String message,
                                      int maxAttachmentCount,
                                      long maxAttachmentSizeBytes,
                                      long maxTotalAttachmentSizeBytes) {
        this.supported = supported;
        this.mode = mode == null ? AiAttachmentMode.NONE : mode;
        this.message = message == null ? "" : message;
        this.maxAttachmentCount = maxAttachmentCount;
        this.maxAttachmentSizeBytes = Math.max(0L, maxAttachmentSizeBytes);
        this.maxTotalAttachmentSizeBytes = Math.max(0L, maxTotalAttachmentSizeBytes);
    }

    public static AiAttachmentSupportResult supported(AiAttachmentMode mode,
                                                      int maxAttachmentCount,
                                                      long maxAttachmentSizeBytes,
                                                      long maxTotalAttachmentSizeBytes) {
        return new AiAttachmentSupportResult(
                true,
                mode,
                "",
                maxAttachmentCount,
                maxAttachmentSizeBytes,
                maxTotalAttachmentSizeBytes
        );
    }

    public static AiAttachmentSupportResult unsupported(String message,
                                                        AiAttachmentMode mode,
                                                        int maxAttachmentCount,
                                                        long maxAttachmentSizeBytes,
                                                        long maxTotalAttachmentSizeBytes) {
        return new AiAttachmentSupportResult(
                false,
                mode,
                message,
                maxAttachmentCount,
                maxAttachmentSizeBytes,
                maxTotalAttachmentSizeBytes
        );
    }

    public boolean isSupported() {
        return supported;
    }

    public AiAttachmentMode getMode() {
        return mode;
    }

    public String getMessage() {
        return message;
    }

    public int getMaxAttachmentCount() {
        return maxAttachmentCount;
    }

    public long getMaxAttachmentSizeBytes() {
        return maxAttachmentSizeBytes;
    }

    public long getMaxTotalAttachmentSizeBytes() {
        return maxTotalAttachmentSizeBytes;
    }
}

package com.potato.potatotool.utils.ai.model;

import java.util.ArrayList;
import java.util.List;

public class AiAttachmentDispatchPlan {
    private final boolean supported;
    private final String message;
    private final List<AiAttachment> requestAttachments;
    private final String questionSuffix;

    private AiAttachmentDispatchPlan(boolean supported,
                                     String message,
                                     List<AiAttachment> requestAttachments,
                                     String questionSuffix) {
        this.supported = supported;
        this.message = message == null ? "" : message;
        this.requestAttachments = requestAttachments == null
                ? new ArrayList<AiAttachment>()
                : new ArrayList<AiAttachment>(requestAttachments);
        this.questionSuffix = questionSuffix == null ? "" : questionSuffix;
    }

    public static AiAttachmentDispatchPlan supported(List<AiAttachment> requestAttachments, String questionSuffix) {
        return new AiAttachmentDispatchPlan(true, "", requestAttachments, questionSuffix);
    }

    public static AiAttachmentDispatchPlan unsupported(String message) {
        return new AiAttachmentDispatchPlan(false, message, null, "");
    }

    public boolean isSupported() {
        return supported;
    }

    public String getMessage() {
        return message;
    }

    public List<AiAttachment> getRequestAttachments() {
        return new ArrayList<AiAttachment>(requestAttachments);
    }

    public String getQuestionSuffix() {
        return questionSuffix;
    }
}

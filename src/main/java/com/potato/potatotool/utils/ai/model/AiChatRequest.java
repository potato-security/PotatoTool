package com.potato.potatotool.utils.ai.model;

import java.util.ArrayList;
import java.util.List;

public class AiChatRequest {
    private final String question;
    private final String historyQuestion;
    private final List<AiMessage> history;
    private final List<AiAttachment> attachments;
    private final AiThinkingConfig thinkingConfig;
    private final boolean stream;
    private final String systemPrompt;

    public AiChatRequest(String question, List<AiMessage> history, AiThinkingConfig thinkingConfig, boolean stream) {
        this(question, question, history, null, thinkingConfig, stream, null);
    }

    public AiChatRequest(String question,
                         String historyQuestion,
                         List<AiMessage> history,
                         AiThinkingConfig thinkingConfig,
                         boolean stream,
                         String systemPrompt) {
        this(question, historyQuestion, history, null, thinkingConfig, stream, systemPrompt);
    }

    public AiChatRequest(String question,
                         String historyQuestion,
                         List<AiMessage> history,
                         List<AiAttachment> attachments,
                         AiThinkingConfig thinkingConfig,
                         boolean stream,
                         String systemPrompt) {
        this.question = question;
        this.historyQuestion = historyQuestion == null ? question : historyQuestion;
        this.history = history == null ? new ArrayList<AiMessage>() : new ArrayList<AiMessage>(history);
        this.attachments = attachments == null ? new ArrayList<AiAttachment>() : new ArrayList<AiAttachment>(attachments);
        this.thinkingConfig = thinkingConfig;
        this.stream = stream;
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt;
    }

    public String getQuestion() {
        return question;
    }

    public String getHistoryQuestion() {
        return historyQuestion;
    }

    public List<AiMessage> getHistory() {
        return new ArrayList<AiMessage>(history);
    }

    public List<AiAttachment> getAttachments() {
        return new ArrayList<AiAttachment>(attachments);
    }

    public AiThinkingConfig getThinkingConfig() {
        return thinkingConfig;
    }

    public boolean isStream() {
        return stream;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }
}

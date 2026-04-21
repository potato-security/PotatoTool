package com.potato.potatotool.utils.ai.model;

import java.util.ArrayList;
import java.util.List;

public class AiChatRequest {
    private final String question;
    private final String historyQuestion;
    private final List<AiMessage> history;
    private final AiThinkingConfig thinkingConfig;
    private final boolean stream;
    private final String systemPrompt;

    public AiChatRequest(String question, List<AiMessage> history, AiThinkingConfig thinkingConfig, boolean stream) {
        this(question, question, history, thinkingConfig, stream, null);
    }

    public AiChatRequest(String question,
                         String historyQuestion,
                         List<AiMessage> history,
                         AiThinkingConfig thinkingConfig,
                         boolean stream,
                         String systemPrompt) {
        this.question = question;
        this.historyQuestion = historyQuestion == null ? question : historyQuestion;
        this.history = history == null ? new ArrayList<AiMessage>() : new ArrayList<AiMessage>(history);
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

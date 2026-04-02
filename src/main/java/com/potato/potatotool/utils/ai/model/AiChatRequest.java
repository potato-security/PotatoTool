package com.potato.potatotool.utils.ai.model;

import java.util.ArrayList;
import java.util.List;

public class AiChatRequest {
    private final String question;
    private final List<AiMessage> history;
    private final AiThinkingConfig thinkingConfig;
    private final boolean stream;

    public AiChatRequest(String question, List<AiMessage> history, AiThinkingConfig thinkingConfig, boolean stream) {
        this.question = question;
        this.history = history == null ? new ArrayList<AiMessage>() : new ArrayList<AiMessage>(history);
        this.thinkingConfig = thinkingConfig;
        this.stream = stream;
    }

    public String getQuestion() {
        return question;
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
}

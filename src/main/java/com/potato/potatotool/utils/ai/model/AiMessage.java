package com.potato.potatotool.utils.ai.model;

public class AiMessage {
    private final String role;
    private final String content;

    public AiMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}

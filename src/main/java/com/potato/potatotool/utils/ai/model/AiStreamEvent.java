package com.potato.potatotool.utils.ai.model;

public class AiStreamEvent {
    public enum Type {
        TOKEN,
        THINKING_TOKEN,
        DONE,
        ERROR
    }

    private final Type type;
    private final String content;

    private AiStreamEvent(Type type, String content) {
        this.type = type;
        this.content = content;
    }

    public static AiStreamEvent token(String content) {
        return new AiStreamEvent(Type.TOKEN, content);
    }

    public static AiStreamEvent thinkingToken(String content) {
        return new AiStreamEvent(Type.THINKING_TOKEN, content);
    }

    public static AiStreamEvent done() {
        return new AiStreamEvent(Type.DONE, "");
    }

    public static AiStreamEvent error(String message) {
        return new AiStreamEvent(Type.ERROR, message);
    }

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
}

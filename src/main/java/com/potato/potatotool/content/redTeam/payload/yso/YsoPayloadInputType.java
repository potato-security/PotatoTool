package com.potato.potatotool.content.redTeam.payload.yso;

public enum YsoPayloadInputType {
    URL_OR_DOMAIN("payload.yso.command.placeholder.urldns", "payload.yso.input.url_or_domain"),
    WOODPECKER_COMMAND("payload.yso.command.placeholder.generic", "payload.yso.input.woodpecker_command"),
    CLASS_FILE("payload.yso.command.placeholder.classfile", "payload.yso.input.class_file");

    private final String promptKey;
    private final String i18nKey;

    YsoPayloadInputType(String promptKey, String i18nKey) {
        this.promptKey = promptKey;
        this.i18nKey = i18nKey;
    }

    public String getPromptKey() {
        return promptKey;
    }

    public String getI18nKey() {
        return i18nKey;
    }
}

package com.potato.potatotool.content.redTeam.payload.yso;

public enum YsoPayloadCategory {
    ALL("payload.yso.category.all"),
    CLASS_LOADING("payload.yso.category.classloading"),
    FILE_OPERATION("payload.yso.category.file_operation"),
    DNS_DETECTION("payload.yso.category.dns"),
    JDK_INTERNAL("payload.yso.category.jdk"),
    LOCAL_HELPER("payload.yso.category.local");

    private final String i18nKey;

    YsoPayloadCategory(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    public String getI18nKey() {
        return i18nKey;
    }
}

package com.potato.potatotool.content.redTeam.infoGathering.classObj;

/**
 * @author Potato
 * @date 2026/4/3 19:30
 */
public enum ShadowAssetReason {
    ICP_MATCH("ICP备案命中"),
    KNOWN_DOMAIN_MATCH("主域或子域命中"),
    CERT_SUBJECT_MATCH("证书主体命中"),
    COMPANY_FIELD_MATCH("平台主体字段命中"),
    TITLE_MATCH("标题命中别名"),
    BODY_MATCH("正文命中别名"),
    ICON_HASH_MATCH("图标哈希命中"),
    ICON_SIMILAR("图标相似"),
    MULTI_SOURCE_MATCH("多平台交叉命中"),
    AI_REVIEW_MATCH("AI复核通过"),
    THIRD_PARTY_PORTAL("第三方门户特征"),
    BIDDING_PORTAL("招采平台特征"),
    RECRUITMENT_PORTAL("招聘平台特征");

    private final String label;

    ShadowAssetReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

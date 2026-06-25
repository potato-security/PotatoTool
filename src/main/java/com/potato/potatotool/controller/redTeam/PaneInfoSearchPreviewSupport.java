package com.potato.potatotool.controller.redTeam;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.ToStart;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DataTypeConstants;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.InfoGatheringStageMetric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PaneInfoSearchPreviewSupport {

    private PaneInfoSearchPreviewSupport() {
    }

    static PreviewState buildPreviewState(ToStart.StartupPage startupPage) {
        if (startupPage == null) {
            return null;
        }
        switch (startupPage) {
            case RED_INFO_SEARCH_PLATFORMS:
                return buildPlatformsState();
            case RED_INFO_SEARCH_ADVANCED:
                return buildAdvancedState();
            case RED_INFO_SEARCH_RESULT:
                return buildResultState();
            case RED_INFO_SEARCH_PROXY_WARNING:
                return buildProxyWarningState();
            default:
                return null;
        }
    }

    private static PreviewState buildPlatformsState() {
        PreviewState state = buildBaseState();
        state.searchModeIndex = 1;
        state.question = "app=\"Potato Portal\"";
        state.platformSelections.put("fofa", true);
        state.platformSelections.put("hunter", false);
        state.platformSelections.put("quake", false);
        state.platformSelections.put("zoomeye", false);
        state.platformSelections.put("shodan", false);
        state.platformSelections.put("google", false);
        state.platformSelections.put("github", false);
        return state;
    }

    private static PreviewState buildAdvancedState() {
        PreviewState state = buildBaseState();
        state.searchModeIndex = 0;
        state.question = "potato 科技";
        state.platformSelections.put("fofa", true);
        state.platformSelections.put("hunter", true);
        state.platformSelections.put("quake", true);
        state.platformSelections.put("zoomeye", true);
        state.platformSelections.put("shodan", true);
        state.platformSelections.put("google", true);
        state.platformSelections.put("github", true);
        state.advancedSelections.put("weight", true);
        state.advancedSelections.put("searchSslSubdomain", true);
        state.advancedSelections.put("bruteForceSubdomain", true);
        state.advancedSelections.put("searchShadowAssets", true);
        state.advancedSelections.put("hasLocalFullDetection", true);
        state.advancedSelections.put("hasIconSearch", true);
        state.advancedSelections.put("hasCrawlLinks", true);
        state.advancedSelections.put("hasFindSensitiveInfo", true);
        state.advancedSelections.put("hasCipAggregator", true);
        state.textValues.put("weightStr", "85,90");
        state.textValues.put("maxDepth", "3");
        state.textValues.put("maxSubPathCount", "60");
        state.textValues.put("maxGoogleSearchCount", "25");
        state.textValues.put("maxGithubSearchCount", "20");
        state.textValues.put("cipThreshold", "6");
        state.textValues.put("localFullDetectionThreshold", "18");
        state.textValues.put("shadowAssetsThreshold", "35");
        return state;
    }

    private static PreviewState buildResultState() {
        PreviewState state = buildAdvancedState();
        state.question = "potato attack surface";
        state.resultEntries.add(createEntry("智能检索阶段总览", createStageSummaryPayload()));
        state.resultEntries.add(createEntry("SEO 与备案聚合", createSeoPayload()));
        state.resultEntries.add(createEntry("影子资产候选 3 条", createShadowPayload()));
        state.resultEntries.add(createEntry("微信公众号与 App 资产", createWxAppPayload()));
        return state;
    }

    private static PreviewState buildProxyWarningState() {
        PreviewState state = buildBaseState();
        state.question = "10.20.30.0/24";
        state.searchModeIndex = 0;
        state.platformSelections.put("fofa", true);
        state.platformSelections.put("hunter", true);
        state.advancedSelections.put("searchShadowAssets", true);
        state.warningTip = "主代理未开启，但资产测绘子代理已启用，请先统一代理配置。";
        return state;
    }

    private static PreviewState buildBaseState() {
        PreviewState state = new PreviewState();
        state.platformSelections.put("fofa", true);
        state.platformSelections.put("hunter", true);
        state.platformSelections.put("quake", true);
        state.platformSelections.put("zoomeye", false);
        state.platformSelections.put("shodan", false);
        state.platformSelections.put("google", false);
        state.platformSelections.put("github", false);

        state.advancedSelections.put("weight", true);
        state.advancedSelections.put("searchSslSubdomain", false);
        state.advancedSelections.put("bruteForceSubdomain", false);
        state.advancedSelections.put("searchShadowAssets", false);
        state.advancedSelections.put("hasLocalFullDetection", false);
        state.advancedSelections.put("hasIconSearch", true);
        state.advancedSelections.put("hasCrawlLinks", false);
        state.advancedSelections.put("hasFindSensitiveInfo", false);
        state.advancedSelections.put("hasCipAggregator", false);

        state.textValues.put("weightStr", "100,100");
        state.textValues.put("maxDepth", "2");
        state.textValues.put("maxSubPathCount", "30");
        state.textValues.put("maxGoogleSearchCount", "10");
        state.textValues.put("maxGithubSearchCount", "10");
        state.textValues.put("cipThreshold", "4");
        state.textValues.put("localFullDetectionThreshold", "20");
        state.textValues.put("shadowAssetsThreshold", "50");
        return state;
    }

    private static ResultEntry createEntry(String message, ResultPayload payload) {
        return new ResultEntry(message, payload.type, payload.data);
    }

    private static ResultPayload createStageSummaryPayload() {
        List<InfoGatheringStageMetric> metrics = Arrays.asList(
                new InfoGatheringStageMetric("主体识别", 1320L, 2, "确认企业名称与备案主体"),
                new InfoGatheringStageMetric("资产聚合", 4860L, 14, "聚合公网入口、证书与备案站点"),
                new InfoGatheringStageMetric("影子资产", 2780L, 3, "筛出高相似度候选")
        );
        return new ResultPayload(DataTypeConstants.STAGE_SUMMARY, metrics);
    }

    private static ResultPayload createSeoPayload() {
        JsonObject root = new JsonObject();

        JsonObject domainInfo = new JsonObject();
        domainInfo.addProperty("目标域名", "portal.potato.example");
        domainInfo.addProperty("解析地址", "203.0.113.24");
        domainInfo.addProperty("证书主体", "Potato Security Lab");
        root.add("域名信息", domainInfo);

        JsonObject icpInfo = new JsonObject();
        icpInfo.addProperty("备案号", "沪ICP备2026001234号");
        icpInfo.addProperty("备案所属", "土豆安全实验室");
        icpInfo.addProperty("审核时间", "2026-06-21");
        root.add("备案信息", icpInfo);

        JsonObject websiteInfo = new JsonObject();
        websiteInfo.addProperty("网站标题", "Potato Portal");
        websiteInfo.addProperty("框架", "Spring Boot / Vue");
        websiteInfo.addProperty("响应摘要", "登陆页、SSO、工单入口");
        root.add("网站信息", websiteInfo);
        return new ResultPayload(DataTypeConstants.SEO, root);
    }

    private static ResultPayload createShadowPayload() {
        List<DomainInfo> domainInfos = new ArrayList<>();

        DomainInfo first = new DomainInfo();
        first.setDomain("vpn-potato.example");
        first.setIp("198.51.100.18");
        first.setPort("443");
        first.setTitle("Potato VPN");
        first.setShadowMatchedAlias("Potato VPN");
        first.setShadowScore(96);
        first.setShadowReasons(Arrays.asList("ICP备案主体一致", "favicon 命中", "证书 O 字段相同"));
        first.setShadowDecisionSource("preview-policy");
        domainInfos.add(first);

        DomainInfo second = new DomainInfo();
        second.setDomain("git-potato.example");
        second.setIp("198.51.100.26");
        second.setPort("8443");
        second.setTitle("Git Mirror");
        second.setShadowMatchedAlias("研发代码托管");
        second.setShadowScore(91);
        second.setShadowReasons(Arrays.asList("同 ASN", "页面关键词命中"));
        second.setShadowDecisionSource("preview-policy");
        domainInfos.add(second);

        DomainInfo third = new DomainInfo();
        third.setDomain("oa-shadow.potato.example");
        third.setIp("203.0.113.61");
        third.setPort("443");
        third.setTitle("OA 登录");
        third.setShadowMatchedAlias("办公协同");
        third.setShadowScore(88);
        third.setShadowReasons(Arrays.asList("备案标题近似", "域名语义近似"));
        third.setShadowDecisionSource("preview-policy");
        domainInfos.add(third);

        return new ResultPayload(DataTypeConstants.SHADOW, domainInfos);
    }

    private static ResultPayload createWxAppPayload() {
        JsonArray companies = new JsonArray();
        JsonObject company = new JsonObject();
        JsonObject companyData = new JsonObject();

        JsonArray apps = new JsonArray();
        JsonObject app = new JsonObject();
        app.addProperty("name", "Potato SOC");
        app.addProperty("classify", "安全运维");
        app.addProperty("logoWord", "SOC");
        app.addProperty("logoBrief", "安全运营工作台");
        apps.add(app);
        companyData.add("app", apps);

        JsonArray wxList = new JsonArray();
        JsonObject wx = new JsonObject();
        wx.addProperty("principalName", "土豆安全实验室");
        wx.addProperty("wechatId", "potato-sec");
        wx.addProperty("wechatName", "Potato 安全");
        wx.addProperty("wechatIntruduction", "漏洞情报与产品动态");
        wxList.add(wx);
        companyData.add("wx", wxList);

        JsonArray domainAndIcp = new JsonArray();
        JsonObject domain = new JsonObject();
        domain.addProperty("domain", "portal.potato.example");
        domain.addProperty("homeSite", "https://portal.potato.example");
        domain.addProperty("icpNo", "沪ICP备2026001234号");
        domain.addProperty("siteName", "Potato Portal");
        domainAndIcp.add(domain);
        companyData.add("domainAndIcp", domainAndIcp);

        JsonArray weightCompanies = new JsonArray();
        JsonObject weightCompany = new JsonObject();
        weightCompany.addProperty("entName", "上海土豆云科技有限公司");
        weightCompany.addProperty("regRate", "92%");
        weightCompany.addProperty("regCapital", "2000万");
        weightCompany.addProperty("investment", "Potato Security Lab");
        weightCompanies.add(weightCompany);
        companyData.add("weightCompany", weightCompanies);

        company.add("土豆安全实验室", companyData);
        companies.add(company);
        return new ResultPayload(DataTypeConstants.WXAPPSUBCOM, companies);
    }

    static final class PreviewState {
        private int searchModeIndex;
        private String question;
        private String warningTip;
        private final LinkedHashMap<String, Boolean> platformSelections = new LinkedHashMap<String, Boolean>();
        private final LinkedHashMap<String, Boolean> advancedSelections = new LinkedHashMap<String, Boolean>();
        private final LinkedHashMap<String, String> textValues = new LinkedHashMap<String, String>();
        private final List<ResultEntry> resultEntries = new ArrayList<ResultEntry>();

        int getSearchModeIndex() {
            return searchModeIndex;
        }

        String getQuestion() {
            return question;
        }

        String getWarningTip() {
            return warningTip;
        }

        Map<String, Boolean> getPlatformSelections() {
            return platformSelections;
        }

        Map<String, Boolean> getAdvancedSelections() {
            return advancedSelections;
        }

        Map<String, String> getTextValues() {
            return textValues;
        }

        List<ResultEntry> getResultEntries() {
            return resultEntries;
        }
    }

    static final class ResultEntry {
        private final String message;
        private final String type;
        private final Object data;

        ResultEntry(String message, String type, Object data) {
            this.message = message;
            this.type = type;
            this.data = data;
        }

        String getMessage() {
            return message;
        }

        String getType() {
            return type;
        }

        Object getData() {
            return data;
        }
    }

    private static final class ResultPayload {
        private final String type;
        private final Object data;

        private ResultPayload(String type, Object data) {
            this.type = type;
            this.data = data;
        }
    }
}

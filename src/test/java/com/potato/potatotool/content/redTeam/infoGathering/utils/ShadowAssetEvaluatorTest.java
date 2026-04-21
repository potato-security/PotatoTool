package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ShadowAssetCandidate;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ShadowAssetContext;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ShadowAssetDecision;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ShadowAssetReason;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShadowAssetEvaluatorTest {

    @Test
    public void evaluate_shouldAcceptWhenIcpMatches() {
        ShadowAssetEvaluator evaluator = new ShadowAssetEvaluator(false, alwaysFalseAiReviewer());
        ShadowAssetDecision decision = evaluator.evaluate(
                buildCandidate(buildDomainInfo("portal.example.com", "京ICP备030173号-1"), "中交建"),
                buildContext()
        );

        assertTrue(decision.isAccepted());
        assertTrue(decision.getReasons().contains(ShadowAssetReason.ICP_MATCH.getLabel()));
        assertTrue(decision.getScore() >= 100);
    }

    @Test
    public void evaluate_shouldRejectWeakAliasWithoutDeterministicEvidenceEvenIfAiSaysRelevant() {
        final AtomicInteger aiReviewCount = new AtomicInteger(0);
        ShadowAssetEvaluator evaluator = new ShadowAssetEvaluator(false, new ShadowAssetEvaluator.AiReviewer() {
            @Override
            public boolean isRelevant(Map<String, Object> candidateWebBaseInfoMap, String companyPrompt, Map<String, Object> targetWebBaseInfoMap) {
                aiReviewCount.incrementAndGet();
                return true;
            }
        });

        DomainInfo domainInfo = new DomainInfo();
        domainInfo.setDomain("example.org");
        domainInfo.setTitle("欢迎访问");
        domainInfo.setWebInfoMap(webInfo("欢迎访问", "普通展示页", "icon-2"));

        ShadowAssetDecision decision = evaluator.evaluate(buildCandidate(domainInfo, "中交建"), buildContext());

        assertFalse(decision.isAccepted());
        assertEquals(0, aiReviewCount.get());
    }

    @Test
    public void evaluate_shouldAcceptWhenTitleAndIconMatch() {
        ShadowAssetEvaluator evaluator = new ShadowAssetEvaluator(false, alwaysFalseAiReviewer());

        DomainInfo domainInfo = new DomainInfo();
        domainInfo.setDomain("smart.example.com");
        domainInfo.setTitle("中交建智慧工地平台");
        domainInfo.setWebInfoMap(webInfo("中交建智慧工地平台", "平台首页", "icon-1"));

        ShadowAssetDecision decision = evaluator.evaluate(buildCandidate(domainInfo, "中交建"), buildContext());

        assertTrue(decision.isAccepted());
        assertEquals("中交建", decision.getMatchedAlias());
        assertTrue(decision.getReasons().contains(ShadowAssetReason.TITLE_MATCH.getLabel()));
        assertTrue(decision.getReasons().contains(ShadowAssetReason.ICON_HASH_MATCH.getLabel()));
    }

    @Test
    public void evaluate_shouldRejectThirdPartyPortal() {
        ShadowAssetEvaluator evaluator = new ShadowAssetEvaluator(false, alwaysFalseAiReviewer());

        DomainInfo domainInfo = new DomainInfo();
        domainInfo.setDomain("www.qcc.com");
        domainInfo.setTitle("企查查 - 中国交通建设股份有限公司");
        domainInfo.setWebInfoMap(webInfo("企查查 - 中国交通建设股份有限公司", "企业工商信息查询", "icon-3"));

        ShadowAssetDecision decision = evaluator.evaluate(buildCandidate(domainInfo, "中国交通建设股份有限公司"), buildContext());

        assertFalse(decision.isAccepted());
        assertTrue(decision.getReasons().contains(ShadowAssetReason.THIRD_PARTY_PORTAL.getLabel()));
    }

    private static ShadowAssetEvaluator.AiReviewer alwaysFalseAiReviewer() {
        return new ShadowAssetEvaluator.AiReviewer() {
            @Override
            public boolean isRelevant(Map<String, Object> candidateWebBaseInfoMap, String companyPrompt, Map<String, Object> targetWebBaseInfoMap) {
                return false;
            }
        };
    }

    private static ShadowAssetCandidate buildCandidate(DomainInfo domainInfo, String recalledAlias) {
        ShadowAssetCandidate candidate = new ShadowAssetCandidate(domainInfo);
        candidate.addRecalledAlias(recalledAlias);
        return candidate;
    }

    private static ShadowAssetContext buildContext() {
        ShadowAssetContext context = new ShadowAssetContext();
        context.setRootCompanyName("中国交通建设股份有限公司");
        context.setCompanyNames(new LinkedHashSet<>(Arrays.asList("中国交通建设股份有限公司", "中交建")));
        context.setDomains(new LinkedHashSet<>(Collections.singletonList("ccccltd.cn")));
        context.setIcpNos(new LinkedHashSet<>(Collections.singletonList("京ICP备030173号")));
        context.setReferenceWebInfoMaps(Collections.singletonList(webInfo("中国交建官网", "官网首页", "icon-1")));
        return context;
    }

    private static DomainInfo buildDomainInfo(String domain, String icp) {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.setDomain(domain);
        domainInfo.setIcp(icp);
        domainInfo.setWebInfoMap(webInfo("", "", "icon-0"));
        return domainInfo;
    }

    private static Map<String, Object> webInfo(String title, String body, String iconMd5) {
        Map<String, Object> webInfo = new HashMap<>();
        webInfo.put("title", title);
        webInfo.put("body", body);
        webInfo.put("iconMd5", iconMd5);
        webInfo.put("iconUrl", "https://static.example.com/" + iconMd5 + ".ico");
        return webInfo;
    }
}

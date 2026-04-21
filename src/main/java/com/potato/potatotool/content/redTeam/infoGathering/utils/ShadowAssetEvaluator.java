package com.potato.potatotool.content.redTeam.infoGathering.utils;

import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ShadowAssetCandidate;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ShadowAssetContext;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ShadowAssetDecision;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ShadowAssetReason;
import com.potato.potatotool.content.redTeam.infoGathering.imgSimilarity.ImgSimilarity;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Potato
 * @date 2026/4/3 19:30
 */
public class ShadowAssetEvaluator {
    private static final int ACCEPT_THRESHOLD = 70;
    private static final int AI_REVIEW_MIN_SCORE = 40;
    private static final int AI_REVIEW_BONUS = 25;
    private static final int MULTI_SOURCE_BONUS = 10;

    private static final List<String> THIRD_PARTY_DOMAIN_PATTERNS = Arrays.asList(
            "aiqicha.baidu.com", "qcc.com", "qichacha.com", "tianyancha.com", "qixin.com",
            "zhaopin.com", "51job.com", "liepin.com", "zhipin.com"
    );
    private static final List<String> THIRD_PARTY_TEXT_PATTERNS = Arrays.asList(
            "爱企查", "企查查", "天眼查", "启信宝", "智联招聘", "前程无忧", "BOSS直聘", "猎聘"
    );
    private static final List<String> BIDDING_TEXT_PATTERNS = Arrays.asList(
            "招标", "投标", "采购", "招采", "比选", "中标", "公共资源交易"
    );
    private static final List<String> RECRUITMENT_TEXT_PATTERNS = Arrays.asList(
            "招聘", "求职", "社招", "校招", "岗位招聘", "职位招聘"
    );

    private final boolean crawlProxy;
    private final AiReviewer aiReviewer;
    private final Map<String, Map<String, Object>> webBaseInfoCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> iconSimilarityCache = new ConcurrentHashMap<>();

    public interface AiReviewer {
        boolean isRelevant(Map<String, Object> candidateWebBaseInfoMap, String companyPrompt, Map<String, Object> targetWebBaseInfoMap);
    }

    public ShadowAssetEvaluator(boolean crawlProxy) {
        this(crawlProxy, new AiReviewer() {
            @Override
            public boolean isRelevant(Map<String, Object> candidateWebBaseInfoMap, String companyPrompt, Map<String, Object> targetWebBaseInfoMap) {
                return AiUtils.getContentRelevance_Ai(candidateWebBaseInfoMap, companyPrompt, targetWebBaseInfoMap);
            }
        });
    }

    public ShadowAssetEvaluator(boolean crawlProxy, AiReviewer aiReviewer) {
        this.crawlProxy = crawlProxy;
        this.aiReviewer = aiReviewer;
    }

    public ShadowAssetDecision evaluate(ShadowAssetCandidate candidate, ShadowAssetContext context) {
        ShadowAssetDecision decision = new ShadowAssetDecision();
        if (candidate == null || candidate.getDomainInfo() == null || context == null) {
            return decision;
        }

        DomainInfo domainInfo = candidate.getDomainInfo();
        Map<String, Object> webBaseInfoMap = domainInfo.getWebInfoMap();

        String basicText = buildSearchableText(domainInfo, null);
        String matchedAlias = findMatchedAlias(context, candidate, basicText);
        decision.setMatchedAlias(matchedAlias);

        applyDeterministicRules(domainInfo, context, basicText, decision);
        if (!decision.isAccepted() && !hasHardBlock(decision)) {
            webBaseInfoMap = getCandidateWebBaseInfo(domainInfo);
            String fullText = buildSearchableText(domainInfo, webBaseInfoMap);

            if (decision.getMatchedAlias() == null || decision.getMatchedAlias().isEmpty()) {
                decision.setMatchedAlias(findMatchedAlias(context, candidate, fullText));
            }
            applyWebEvidenceRules(domainInfo, context, fullText, webBaseInfoMap, decision);
        }

        if (!hasHardBlock(decision) && decision.getScore() >= AI_REVIEW_MIN_SCORE && decision.getScore() < ACCEPT_THRESHOLD) {
            if (reviewByAi(context, webBaseInfoMap, decision.getMatchedAlias())) {
                decision.addScore(AI_REVIEW_BONUS);
                decision.addReason(ShadowAssetReason.AI_REVIEW_MATCH);
                decision.setDecisionSource("rule+ai");
            }
        }

        decision.setAccepted(!hasHardBlock(decision) && decision.getScore() >= ACCEPT_THRESHOLD);
        if (!"rule+ai".equals(decision.getDecisionSource())) {
            decision.setDecisionSource("rule");
        }
        return decision;
    }

    private void applyDeterministicRules(DomainInfo domainInfo, ShadowAssetContext context, String searchableText, ShadowAssetDecision decision) {
        String matchedName = findMatchedCompanyName(searchableText, orderedCompanyNames(context));

        if (matchesKnownIcp(domainInfo.getIcp(), context.getIcpNos())) {
            decision.addScore(100);
            decision.addReason(ShadowAssetReason.ICP_MATCH);
        }

        if (matchesKnownDomain(domainInfo, context.getDomains())) {
            decision.addScore(95);
            decision.addReason(ShadowAssetReason.KNOWN_DOMAIN_MATCH);
        }

        if (containsCompanyName(domainInfo.getCertsSubjectOrg(), orderedCompanyNames(context))) {
            decision.addScore(85);
            decision.addReason(ShadowAssetReason.CERT_SUBJECT_MATCH);
            if (decision.getMatchedAlias() == null || decision.getMatchedAlias().isEmpty()) {
                decision.setMatchedAlias(matchedName);
            }
        }

        if (containsCompanyName(domainInfo.getCompany(), orderedCompanyNames(context))) {
            decision.addScore(80);
            decision.addReason(ShadowAssetReason.COMPANY_FIELD_MATCH);
            if (decision.getMatchedAlias() == null || decision.getMatchedAlias().isEmpty()) {
                decision.setMatchedAlias(matchedName);
            }
        }

        if (countDataSources(domainInfo.getDataSource()) > 1) {
            decision.addScore(MULTI_SOURCE_BONUS);
            decision.addReason(ShadowAssetReason.MULTI_SOURCE_MATCH);
        }

        applyNegativeSignals(domainInfo, searchableText, decision);
        decision.setAccepted(!hasHardBlock(decision) && decision.getScore() >= ACCEPT_THRESHOLD);
    }

    private void applyWebEvidenceRules(DomainInfo domainInfo, ShadowAssetContext context, String searchableText,
                                       Map<String, Object> webBaseInfoMap, ShadowAssetDecision decision) {
        String matchedAlias = decision.getMatchedAlias();
        if (matchedAlias == null || matchedAlias.isEmpty()) {
            matchedAlias = findMatchedCompanyName(searchableText, orderedCompanyNames(context));
            decision.setMatchedAlias(matchedAlias);
        }

        if (matchedAlias != null && !matchedAlias.isEmpty()) {
            if (containsTitleMatch(domainInfo, webBaseInfoMap, matchedAlias, context.getRootCompanyName())) {
                decision.addScore(30);
                decision.addReason(ShadowAssetReason.TITLE_MATCH);
            }
        }

        if (containsBodyMatch(webBaseInfoMap, orderedCompanyNames(context))) {
            decision.addScore(25);
            decision.addReason(ShadowAssetReason.BODY_MATCH);
        }

        ShadowAssetReason iconReason = matchReferenceIcon(webBaseInfoMap, context.getReferenceWebInfoMaps());
        if (iconReason == ShadowAssetReason.ICON_HASH_MATCH) {
            decision.addScore(40);
            decision.addReason(iconReason);
        } else if (iconReason == ShadowAssetReason.ICON_SIMILAR) {
            decision.addScore(35);
            decision.addReason(iconReason);
        }

        applyNegativeSignals(domainInfo, searchableText, decision);
    }

    private boolean reviewByAi(ShadowAssetContext context, Map<String, Object> webBaseInfoMap, String matchedAlias) {
        if (webBaseInfoMap == null || webBaseInfoMap.isEmpty() || aiReviewer == null) {
            return false;
        }

        String companyPrompt = context.getRootCompanyName();
        if (matchedAlias != null && !matchedAlias.trim().isEmpty() && context.getRootCompanyName() != null
                && !matchedAlias.trim().equals(context.getRootCompanyName().trim())) {
            companyPrompt = context.getRootCompanyName() + "（别名：" + matchedAlias.trim() + "）";
        }

        int checked = 0;
        for (Map<String, Object> referenceWebInfoMap : context.getReferenceWebInfoMaps()) {
            if (referenceWebInfoMap == null || referenceWebInfoMap.isEmpty()) {
                continue;
            }
            checked++;
            if (aiReviewer.isRelevant(webBaseInfoMap, companyPrompt, referenceWebInfoMap)) {
                return true;
            }
            if (checked >= 3) {
                break;
            }
        }

        return context.getReferenceWebInfoMaps().isEmpty() && aiReviewer.isRelevant(webBaseInfoMap, companyPrompt, null);
    }

    private Map<String, Object> getCandidateWebBaseInfo(DomainInfo domainInfo) {
        if (domainInfo.getWebInfoMap() != null && !domainInfo.getWebInfoMap().isEmpty()) {
            return domainInfo.getWebInfoMap();
        }

        String baseUrl = buildBaseUrl(domainInfo);
        if (baseUrl == null || baseUrl.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> cached = webBaseInfoCache.get(baseUrl);
        if (cached != null) {
            return cached;
        }

        Map<String, Object> webBaseInfoMap = Utils.getWebBaseInfo(baseUrl, true, crawlProxy);
        if (webBaseInfoMap == null) {
            webBaseInfoMap = Collections.emptyMap();
        }
        webBaseInfoCache.put(baseUrl, webBaseInfoMap);
        return webBaseInfoMap;
    }

    private String buildBaseUrl(DomainInfo domainInfo) {
        if (domainInfo == null) {
            return null;
        }

        if (domainInfo.getUrl() != null && domainInfo.getUrl().startsWith("http")) {
            return domainInfo.getUrl();
        }

        String host = domainInfo.getDomain();
        if (host == null || host.trim().isEmpty()) {
            host = domainInfo.getIp();
        }
        if (host == null || host.trim().isEmpty()) {
            return null;
        }

        String port = domainInfo.getPort();
        if (port == null || port.trim().isEmpty()) {
            return host.trim();
        }
        return host.trim() + ":" + port.trim();
    }

    private void applyNegativeSignals(DomainInfo domainInfo, String searchableText, ShadowAssetDecision decision) {
        String normalizedText = normalizeText(searchableText);
        String normalizedHostText = normalizeText(domainInfo.getDomain()) + "\n" + normalizeText(domainInfo.getHost()) + "\n" + normalizeText(domainInfo.getUrl());

        if (!decision.getReasons().contains(ShadowAssetReason.THIRD_PARTY_PORTAL.getLabel())
                && (containsAny(normalizedHostText, THIRD_PARTY_DOMAIN_PATTERNS) || containsAny(normalizedText, THIRD_PARTY_TEXT_PATTERNS))) {
            decision.addScore(-120);
            decision.addReason(ShadowAssetReason.THIRD_PARTY_PORTAL);
        }
        if (!decision.getReasons().contains(ShadowAssetReason.BIDDING_PORTAL.getLabel())
                && containsAny(normalizedText, BIDDING_TEXT_PATTERNS)) {
            decision.addScore(-35);
            decision.addReason(ShadowAssetReason.BIDDING_PORTAL);
        }
        if (!decision.getReasons().contains(ShadowAssetReason.RECRUITMENT_PORTAL.getLabel())
                && containsAny(normalizedText, RECRUITMENT_TEXT_PATTERNS)) {
            decision.addScore(-35);
            decision.addReason(ShadowAssetReason.RECRUITMENT_PORTAL);
        }
    }

    private boolean hasHardBlock(ShadowAssetDecision decision) {
        return decision.getReasons().contains(ShadowAssetReason.THIRD_PARTY_PORTAL.getLabel()) && decision.getScore() < ACCEPT_THRESHOLD;
    }

    private boolean containsBodyMatch(Map<String, Object> webBaseInfoMap, List<String> companyNames) {
        if (webBaseInfoMap == null || webBaseInfoMap.isEmpty()) {
            return false;
        }
        String body = normalizeText(String.valueOf(webBaseInfoMap.get("body")));
        return containsCompanyName(body, companyNames);
    }

    private boolean containsTitleMatch(DomainInfo domainInfo, Map<String, Object> webBaseInfoMap, String matchedAlias, String rootCompanyName) {
        String title = domainInfo.getTitle();
        if ((title == null || title.trim().isEmpty()) && webBaseInfoMap != null) {
            title = stringValue(webBaseInfoMap.get("title"));
        }
        String normalizedTitle = normalizeText(title);
        if (normalizedTitle.isEmpty()) {
            return false;
        }

        String normalizedAlias = normalizeText(matchedAlias);
        String normalizedRoot = normalizeText(rootCompanyName);
        return (!normalizedAlias.isEmpty() && normalizedTitle.contains(normalizedAlias))
                || (!normalizedRoot.isEmpty() && normalizedTitle.contains(normalizedRoot));
    }

    private ShadowAssetReason matchReferenceIcon(Map<String, Object> webBaseInfoMap, List<Map<String, Object>> referenceWebInfoMaps) {
        if (webBaseInfoMap == null || webBaseInfoMap.isEmpty() || referenceWebInfoMaps == null || referenceWebInfoMaps.isEmpty()) {
            return null;
        }

        String iconMd5 = stringValue(webBaseInfoMap.get("iconMd5"));
        String iconUrl = stringValue(webBaseInfoMap.get("iconUrl"));
        int checked = 0;
        for (Map<String, Object> referenceWebInfoMap : referenceWebInfoMaps) {
            if (referenceWebInfoMap == null || referenceWebInfoMap.isEmpty()) {
                continue;
            }
            checked++;
            String refMd5 = stringValue(referenceWebInfoMap.get("iconMd5"));
            if (!iconMd5.isEmpty() && !refMd5.isEmpty() && iconMd5.equalsIgnoreCase(refMd5)) {
                return ShadowAssetReason.ICON_HASH_MATCH;
            }

            String refIconUrl = stringValue(referenceWebInfoMap.get("iconUrl"));
            if (!iconUrl.isEmpty() && !refIconUrl.isEmpty() && isIconSimilar(iconUrl, refIconUrl)) {
                return ShadowAssetReason.ICON_SIMILAR;
            }

            if (checked >= 3) {
                break;
            }
        }
        return null;
    }

    private boolean isIconSimilar(String leftIconUrl, String rightIconUrl) {
        String cacheKey = leftIconUrl + "||" + rightIconUrl;
        Boolean cached = iconSimilarityCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        boolean result = false;
        try {
            result = new ImgSimilarity().matchSimilar(new URL(leftIconUrl), new URL(rightIconUrl));
        } catch (Exception ignored) {
        }
        iconSimilarityCache.put(cacheKey, result);
        iconSimilarityCache.put(rightIconUrl + "||" + leftIconUrl, result);
        return result;
    }

    private boolean matchesKnownIcp(String icp, Set<String> knownIcpNos) {
        String normalizedIcp = normalizeIcp(icp);
        if (normalizedIcp.isEmpty() || knownIcpNos == null) {
            return false;
        }
        for (String knownIcpNo : knownIcpNos) {
            if (normalizedIcp.equals(normalizeIcp(knownIcpNo))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesKnownDomain(DomainInfo domainInfo, Set<String> knownDomains) {
        if (knownDomains == null || knownDomains.isEmpty()) {
            return false;
        }

        Set<String> identifiers = collectIdentifiers(domainInfo);
        for (String identifier : identifiers) {
            for (String knownDomain : knownDomains) {
                String normalizedKnownDomain = normalizeDomain(knownDomain);
                if (normalizedKnownDomain.isEmpty()) {
                    continue;
                }
                if (identifier.equals(normalizedKnownDomain) || identifier.endsWith("." + normalizedKnownDomain)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> collectIdentifiers(DomainInfo domainInfo) {
        LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        addIdentifiers(identifiers, domainInfo.getDomain());
        addIdentifiers(identifiers, domainInfo.getHost());
        addIdentifier(identifiers, extractHost(domainInfo.getUrl()));
        return identifiers;
    }

    private void addIdentifiers(Set<String> identifiers, String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return;
        }
        String[] parts = rawValue.split("[\\n,;]+");
        for (String part : parts) {
            addIdentifier(identifiers, part);
        }
    }

    private void addIdentifier(Set<String> identifiers, String rawValue) {
        String normalized = normalizeDomain(rawValue);
        if (!normalized.isEmpty()) {
            identifiers.add(normalized);
        }
    }

    private String findMatchedAlias(ShadowAssetContext context, ShadowAssetCandidate candidate, String searchableText) {
        List<String> names = new ArrayList<>();
        if (candidate != null && candidate.getRecalledAliases() != null) {
            names.addAll(candidate.getRecalledAliases());
        }
        names.addAll(orderedCompanyNames(context));
        return findMatchedCompanyName(searchableText, names);
    }

    private String findMatchedCompanyName(String text, Collection<String> companyNames) {
        String normalizedText = normalizeText(text);
        if (normalizedText.isEmpty() || companyNames == null || companyNames.isEmpty()) {
            return null;
        }

        List<String> orderedNames = new ArrayList<>();
        for (String companyName : companyNames) {
            if (companyName != null && !companyName.trim().isEmpty()) {
                orderedNames.add(companyName.trim());
            }
        }
        orderedNames.sort(Comparator.comparingInt(String::length).reversed());

        for (String companyName : orderedNames) {
            if (companyName.length() < 2) {
                continue;
            }
            if (normalizedText.contains(normalizeText(companyName))) {
                return companyName;
            }
        }
        return null;
    }

    private boolean containsCompanyName(String text, Collection<String> companyNames) {
        return findMatchedCompanyName(text, companyNames) != null;
    }

    private List<String> orderedCompanyNames(ShadowAssetContext context) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (context != null) {
            if (context.getRootCompanyName() != null && !context.getRootCompanyName().trim().isEmpty()) {
                ordered.add(context.getRootCompanyName().trim());
            }
            if (context.getCompanyNames() != null) {
                List<String> aliases = new ArrayList<>(context.getCompanyNames());
                aliases.sort(Comparator.comparingInt(String::length).reversed());
                for (String alias : aliases) {
                    if (alias != null && !alias.trim().isEmpty()) {
                        ordered.add(alias.trim());
                    }
                }
            }
        }
        return new ArrayList<>(ordered);
    }

    private int countDataSources(String dataSource) {
        if (dataSource == null || dataSource.trim().isEmpty()) {
            return 0;
        }
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        for (String item : dataSource.split("[\\n,;]+")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                sources.add(trimmed);
            }
        }
        return sources.size();
    }

    private String buildSearchableText(DomainInfo domainInfo, Map<String, Object> webBaseInfoMap) {
        List<String> parts = new ArrayList<>();
        appendIfNotBlank(parts, domainInfo.getDomain());
        appendIfNotBlank(parts, domainInfo.getHost());
        appendIfNotBlank(parts, domainInfo.getUrl());
        appendIfNotBlank(parts, domainInfo.getTitle());
        appendIfNotBlank(parts, domainInfo.getCompany());
        appendIfNotBlank(parts, domainInfo.getCertsSubjectOrg());
        if (webBaseInfoMap != null && !webBaseInfoMap.isEmpty()) {
            appendIfNotBlank(parts, stringValue(webBaseInfoMap.get("url")));
            appendIfNotBlank(parts, stringValue(webBaseInfoMap.get("title")));
            appendIfNotBlank(parts, stringValue(webBaseInfoMap.get("body")));
        }
        return String.join("\n", parts);
    }

    private void appendIfNotBlank(List<String> parts, String value) {
        if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value.trim())) {
            parts.add(value.trim());
        }
    }

    private boolean containsAny(String normalizedText, List<String> patterns) {
        if (normalizedText == null || normalizedText.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (normalizedText.contains(normalizeText(pattern))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeIcp(String icp) {
        if (icp == null) {
            return "";
        }
        return icp.replaceAll("\\s+", "").replaceAll("-\\d+$", "").trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDomain(String domain) {
        if (domain == null) {
            return "";
        }
        String normalized = domain.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("*.")) {
            normalized = normalized.substring(2);
        }
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7);
        } else if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8);
        }
        int slashIndex = normalized.indexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(0, slashIndex);
        }
        return normalized;
    }

    private String extractHost(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        try {
            return new URL(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return "null".equals(normalized) ? "" : normalized;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}

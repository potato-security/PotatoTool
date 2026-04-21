package com.potato.potatotool.content.redTeam.infoGathering.classObj;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Potato
 * @date 2026/4/3 19:30
 */
public class ShadowAssetDecision {
    private boolean accepted;
    private int score;
    private String matchedAlias;
    private String decisionSource = "rule";
    private final LinkedHashSet<String> reasons = new LinkedHashSet<>();

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void addScore(int delta) {
        this.score += delta;
    }

    public String getMatchedAlias() {
        return matchedAlias;
    }

    public void setMatchedAlias(String matchedAlias) {
        this.matchedAlias = matchedAlias;
    }

    public String getDecisionSource() {
        return decisionSource;
    }

    public void setDecisionSource(String decisionSource) {
        this.decisionSource = decisionSource;
    }

    public List<String> getReasons() {
        return new ArrayList<>(reasons);
    }

    public void addReason(ShadowAssetReason reason) {
        if (reason != null) {
            reasons.add(reason.getLabel());
        }
    }

    public void applyTo(DomainInfo domainInfo) {
        if (domainInfo == null) {
            return;
        }
        domainInfo.setShadowAsset(accepted);
        domainInfo.setShadowScore(score);
        domainInfo.setShadowMatchedAlias(matchedAlias);
        domainInfo.setShadowReasons(getReasons());
        domainInfo.setShadowDecisionSource(decisionSource);
    }
}

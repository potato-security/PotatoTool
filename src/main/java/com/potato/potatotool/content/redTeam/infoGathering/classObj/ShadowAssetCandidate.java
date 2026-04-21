package com.potato.potatotool.content.redTeam.infoGathering.classObj;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Potato
 * @date 2026/4/3 19:30
 */
public class ShadowAssetCandidate {
    private DomainInfo domainInfo;
    private Set<String> recalledAliases = new LinkedHashSet<>();

    public ShadowAssetCandidate() {
    }

    public ShadowAssetCandidate(DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
    }

    public DomainInfo getDomainInfo() {
        return domainInfo;
    }

    public void setDomainInfo(DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
    }

    public Set<String> getRecalledAliases() {
        return recalledAliases;
    }

    public void setRecalledAliases(Set<String> recalledAliases) {
        this.recalledAliases = recalledAliases == null
                ? new LinkedHashSet<String>()
                : new LinkedHashSet<>(recalledAliases);
    }

    public void addRecalledAlias(String recalledAlias) {
        if (recalledAlias != null && !recalledAlias.trim().isEmpty()) {
            this.recalledAliases.add(recalledAlias.trim());
        }
    }
}

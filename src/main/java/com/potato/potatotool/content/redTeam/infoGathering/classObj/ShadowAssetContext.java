package com.potato.potatotool.content.redTeam.infoGathering.classObj;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Potato
 * @date 2026/4/3 19:30
 */
public class ShadowAssetContext {
    private String rootCompanyName;
    private Set<String> companyNames = new LinkedHashSet<>();
    private Set<String> domains = new LinkedHashSet<>();
    private Set<String> icpNos = new LinkedHashSet<>();
    private List<Map<String, Object>> referenceWebInfoMaps = new ArrayList<>();

    public String getRootCompanyName() {
        return rootCompanyName;
    }

    public void setRootCompanyName(String rootCompanyName) {
        this.rootCompanyName = rootCompanyName;
    }

    public Set<String> getCompanyNames() {
        return companyNames;
    }

    public void setCompanyNames(Set<String> companyNames) {
        this.companyNames = companyNames == null ? new LinkedHashSet<String>() : new LinkedHashSet<>(companyNames);
    }

    public Set<String> getDomains() {
        return domains;
    }

    public void setDomains(Set<String> domains) {
        this.domains = domains == null ? new LinkedHashSet<String>() : new LinkedHashSet<>(domains);
    }

    public Set<String> getIcpNos() {
        return icpNos;
    }

    public void setIcpNos(Set<String> icpNos) {
        this.icpNos = icpNos == null ? new LinkedHashSet<String>() : new LinkedHashSet<>(icpNos);
    }

    public List<Map<String, Object>> getReferenceWebInfoMaps() {
        return referenceWebInfoMaps;
    }

    public void setReferenceWebInfoMaps(List<Map<String, Object>> referenceWebInfoMaps) {
        this.referenceWebInfoMaps = referenceWebInfoMaps == null
                ? new ArrayList<Map<String, Object>>()
                : new ArrayList<>(referenceWebInfoMaps);
    }
}

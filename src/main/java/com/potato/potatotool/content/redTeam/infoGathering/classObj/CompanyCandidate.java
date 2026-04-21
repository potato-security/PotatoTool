package com.potato.potatotool.content.redTeam.infoGathering.classObj;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 公司候选项，供根公司确认流程使用。
 */
public class CompanyCandidate {
    public static final String SOURCE_CHINAZ = "Chinaz";
    public static final String SOURCE_AIQICHA = "Aiqicha";
    public static final String SOURCE_AI = "AI";

    private String companyName;
    private String chinazCompanyId;
    private String aiqichaPid;
    private String companyStatus;
    private String legalRepresentative;
    private String registeredCapital;
    private String registeredTime;
    private final LinkedHashSet<String> sources = new LinkedHashSet<>();

    public static CompanyCandidate fromAiName(String companyName) {
        CompanyCandidate candidate = new CompanyCandidate();
        candidate.setCompanyName(companyName);
        candidate.addSource(SOURCE_AI);
        return candidate;
    }

    public void mergeFrom(CompanyCandidate other) {
        if (other == null) {
            return;
        }
        addSources(other.getSources());
        if (this.chinazCompanyId == null || this.chinazCompanyId.isEmpty()) {
            this.chinazCompanyId = other.getChinazCompanyId();
        }
        if (this.aiqichaPid == null || this.aiqichaPid.isEmpty()) {
            this.aiqichaPid = other.getAiqichaPid();
        }
        if (this.companyStatus == null || this.companyStatus.isEmpty()) {
            this.companyStatus = other.getCompanyStatus();
        }
        if (this.legalRepresentative == null || this.legalRepresentative.isEmpty()) {
            this.legalRepresentative = other.getLegalRepresentative();
        }
        if (this.registeredCapital == null || this.registeredCapital.isEmpty()) {
            this.registeredCapital = other.getRegisteredCapital();
        }
        if (this.registeredTime == null || this.registeredTime.isEmpty()) {
            this.registeredTime = other.getRegisteredTime();
        }
    }

    public void addSource(String source) {
        if (source != null && !source.trim().isEmpty()) {
            this.sources.add(source.trim());
        }
    }

    public void addSources(Set<String> sourceSet) {
        if (sourceSet == null) {
            return;
        }
        for (String source : sourceSet) {
            addSource(source);
        }
    }

    public String getSourceSummary() {
        return String.join(" / ", sources);
    }

    public Set<String> getSources() {
        return sources;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getChinazCompanyId() {
        return chinazCompanyId;
    }

    public void setChinazCompanyId(String chinazCompanyId) {
        this.chinazCompanyId = chinazCompanyId;
    }

    public String getAiqichaPid() {
        return aiqichaPid;
    }

    public void setAiqichaPid(String aiqichaPid) {
        this.aiqichaPid = aiqichaPid;
    }

    public String getCompanyStatus() {
        return companyStatus;
    }

    public void setCompanyStatus(String companyStatus) {
        this.companyStatus = companyStatus;
    }

    public String getLegalRepresentative() {
        return legalRepresentative;
    }

    public void setLegalRepresentative(String legalRepresentative) {
        this.legalRepresentative = legalRepresentative;
    }

    public String getRegisteredCapital() {
        return registeredCapital;
    }

    public void setRegisteredCapital(String registeredCapital) {
        this.registeredCapital = registeredCapital;
    }

    public String getRegisteredTime() {
        return registeredTime;
    }

    public void setRegisteredTime(String registeredTime) {
        this.registeredTime = registeredTime;
    }
}

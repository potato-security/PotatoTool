package com.potato.potatotool.content.redTeam.infoGathering.classObj;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * @author Potato
 * @date 2024/11/5 21:05
 */
public class NetAssets {
    private String companyName;
    private List<DomainInfo> domainInfoList;


    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public List<DomainInfo> getDomainInfoList() {
        return domainInfoList;
    }

    public void setDomainInfoList(List<DomainInfo> domainInfoList) {
        this.domainInfoList = domainInfoList;
    }
}

package com.potato.potatotool.content.redTeam.infoGathering.classObj;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;

import java.util.*;

/**
 * @author Potato
 * @date 2024/10/18 11:43
 */
public class AssetObj {
    private String Fofa_Key = "";
    private Set<String> Hunter_Key = new HashSet<>();
    private String Quake_Key = "";
    private String Shodan_Key = "";
    private String Zoomeye_Key = "";
    private boolean getSubdomain = false;
    private boolean bruteForceSubdomain = false;
    private boolean searchShadowAssets = false;
    private Set<String> domain = new HashSet<>();
    private Set<String> ipc = new HashSet<>();

    public AssetObj() {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Asset");
        this.setFofa_Key(tmpJsonObj.getAsJsonPrimitive("Fofa_Key").getAsString());
        this.setHunter_Key(tmpJsonObj.getAsJsonPrimitive("Hunter_Key").getAsJsonArray());
        this.setQuake_Key(tmpJsonObj.getAsJsonPrimitive("Quake_Key").getAsString());
        this.setShodan_Key(tmpJsonObj.getAsJsonPrimitive("Shodan_Key").getAsString());
        this.setZoomeye_Key(tmpJsonObj.getAsJsonPrimitive("Zoomeye_Key").getAsString());
    }

    public Set<String> getDomain() {
        return domain;
    }

    public AssetObj setDomain(Set<String> domain) {
        this.domain = domain;
        return this;
    }
    public AssetObj addDomain(String domain) {
        this.domain.add(domain.replace("*.", ""));
        return this;
    }
    public AssetObj addDomain(Set<String> domainSet) {
        this.domain.addAll(domainSet);
        return this;
    }

    public Set<String> getIcp() {
        return ipc;
    }
    public AssetObj setIcp(Set<String> ipc) {
        this.ipc = ipc;
        return this;
    }
    public AssetObj addIcp(String ipc) {
        this.ipc.add(ipc.replaceAll("-.*", ""));
        return this;
    }

    public boolean isGetSubdomain() {
        return getSubdomain;
    }

    public AssetObj setGetSubdomain(boolean getSubdomain) {
        this.getSubdomain = getSubdomain;
        return this;
    }

    public boolean isBruteForceSubdomain() {
        return bruteForceSubdomain;
    }

    public AssetObj setBruteForceSubdomain(boolean bruteForceSubdomain) {
        this.bruteForceSubdomain = bruteForceSubdomain;
        return this;
    }

    public String getFofa_Key() {
        return Fofa_Key;
    }

    public AssetObj setFofa_Key(String fofa_Key) {
        this.Fofa_Key = fofa_Key;
        return this;
    }

    public Set<String> getHunter_Key() {
        return Hunter_Key;
    }

    public AssetObj setHunter_Key(Set<String> hunter_Key) {
        this.Hunter_Key = hunter_Key;
        return this;
    }
    public AssetObj setHunter_Key(JsonArray hunter_Key_JsonArray) {
        Set<String> Hunter_Key_Set = new HashSet<>();
        for (JsonElement element : hunter_Key_JsonArray) {
            Hunter_Key_Set.add(element.getAsString());
        }
        this.setHunter_Key(Hunter_Key_Set);
        return this;
    }

    public String getQuake_Key() {
        return Quake_Key;
    }

    public AssetObj setQuake_Key(String quake_Key) {
        this.Quake_Key = quake_Key;
        return this;
    }

    public String getShodan_Key() {
        return Shodan_Key;
    }

    public AssetObj setShodan_Key(String shodan_Key) {
        this.Shodan_Key = shodan_Key;
        return this;
    }

    public String getZoomeye_Key() {
        return Zoomeye_Key;
    }

    public AssetObj setZoomeye_Key(String zoomeye_Key) {
        this.Zoomeye_Key = zoomeye_Key;
        return this;
    }

    public boolean isSearchShadowAssets() {
        return searchShadowAssets;
    }

    public AssetObj setSearchShadowAssets(boolean searchShadowAssets) {
        this.searchShadowAssets = searchShadowAssets;
        return this;
    }
}

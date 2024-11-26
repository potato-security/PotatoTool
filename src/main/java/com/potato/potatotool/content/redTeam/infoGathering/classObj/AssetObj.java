package com.potato.potatotool.content.redTeam.infoGathering.classObj;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;

import java.util.*;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2024/10/18 11:43
 */
public class AssetObj {
    private String Fofa_Key = "";
    private Set<String> Hunter_Key = new HashSet<>();
    private Set<String> Quake_Key = new HashSet<>();
    private String Shodan_Key = "";
    private String Zoomeye_Key = "";
    private boolean hasAssetKey = false;

    private Set<HashMap<String, String>> Google_API = new HashSet<>();
    private Set<String> GitHub_Token = new HashSet<>();

    private List<Integer> weightThresholdList = new ArrayList<>();
    private boolean useGoogle = false;
    private boolean useGithub = false;
    private boolean bruteForceSubdomain = false;
    private boolean searchShadowAssets = true;
    private boolean hasCrawlLinks = true;
    private boolean hasFindSensitiveInfo = true;
    private int maxDepth = 2;
    private int maxSubPathCount = 3;
    private Set<String> domain = new HashSet<>();
    private Set<String> icp = new HashSet<>();
//    private List<DomainInfo> domainInfoList = new ArrayList<>();
    private List<NetAssets> companyDomainInfoList = new ArrayList<>();

    public static String FOFA_KEY = "Fofa_Key";
    public static String HUNTER_KEY = "Hunter_Key";
    public static String QUAKE_KEY = "Quake_Key";
    public static String SHODAN_KEY = "Shodan_Key";
    public static String ZOOMEYE_KEY = "Zoomeye_Key";
    public static String GOOGLE_API = "Google_API";
    public static String GITHUB_TOKEN = "GitHub_Token";

    public AssetObj() {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Asset");
        this.setFofa_Key(tmpJsonObj.get(FOFA_KEY).getAsString());
        this.setHunter_Key(tmpJsonObj.getAsJsonArray(HUNTER_KEY));
        this.setQuake_Key(tmpJsonObj.getAsJsonArray(QUAKE_KEY));
        this.setShodan_Key(tmpJsonObj.get(SHODAN_KEY).getAsString());
        this.setZoomeye_Key(tmpJsonObj.get(ZOOMEYE_KEY).getAsString());
        this.setGoogle_API(tmpJsonObj.getAsJsonArray(GOOGLE_API));
        this.setGitHub_Token(tmpJsonObj.getAsJsonArray(GITHUB_TOKEN));
    }

    public static boolean hasSetKey(String key){
        try {
            JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Asset");
            if(key.equals(FOFA_KEY) || key.equals(QUAKE_KEY) || key.equals(SHODAN_KEY) || key.equals(ZOOMEYE_KEY)){
                String value = tmpJsonObj.get(key).getAsString();
                if(value!=null && !value.isEmpty()) return true;
            }else if(key.equals(HUNTER_KEY) || key.equals(GOOGLE_API) || key.equals(GITHUB_TOKEN)){
                JsonArray value = tmpJsonObj.getAsJsonArray(key);
                if(value!=null && !value.isEmpty()) return true;
            }
        }catch (Exception e){
            if(debugMode) e.printStackTrace();
        }

        return false;
    };

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
        return icp;
    }
    public AssetObj setIcp(Set<String> icp) {
        for(String item : icp){
            addIcp(item);
        }
        return this;
    }
    public AssetObj addIcp(String ipc) {
        this.icp.add(ipc.replaceAll("-.*", ""));
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

    public Set<String> getQuake_Key() {
        return Quake_Key;
    }

    public AssetObj setQuake_Key(Set<String> quake_Key) {
        this.Quake_Key = quake_Key;
        return this;
    }

    public AssetObj setQuake_Key(JsonArray quake_Key_JsonArray) {
        Set<String> Quake_Key_Set = new HashSet<>();
        for (JsonElement element : quake_Key_JsonArray) {
            Quake_Key_Set.add(element.getAsString());
        }
        this.setQuake_Key(Quake_Key_Set);
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

    public Set<HashMap<String, String>> getGoogle_API() {
        return Google_API;
    }

    public void setGoogle_API(Set<HashMap<String, String>> google_API) {
        this.Google_API = google_API;
    }
    public AssetObj setGoogle_API(JsonArray google_API_JsonArray) {
        Set<HashMap<String, String>> google_API = new HashSet<>();
        for (JsonElement element : google_API_JsonArray) {
            HashMap<String, String> map = new HashMap<>();
            JsonObject obj = element.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
            }
            google_API.add(map);
        }
        this.setGoogle_API(google_API);
        return this;
    }

    public Set<String> getGitHub_Token() {
        return GitHub_Token;
    }

    public void setGitHub_Token(Set<String> gitHub_Token) {
        this.GitHub_Token = gitHub_Token;
    }
    public AssetObj setGitHub_Token(JsonArray gitHub_Token_JsonArray) {
        Set<String> gitHub_Token = new HashSet<>();
        for (JsonElement element : gitHub_Token_JsonArray) {
            gitHub_Token.add(element.getAsString());
        }
        this.setGitHub_Token(gitHub_Token);
        return this;
    }

    public boolean isHasCrawlLinks() {
        return hasCrawlLinks;
    }

    public void setHasCrawlLinks(boolean hasCrawlLinks) {
        this.hasCrawlLinks = hasCrawlLinks;
    }

    public boolean isHasFindSensitiveInfo() {
        return hasFindSensitiveInfo;
    }

    public void setHasFindSensitiveInfo(boolean hasFindSensitiveInfo) {
        this.hasFindSensitiveInfo = hasFindSensitiveInfo;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMaxSubPathCount() {
        return maxSubPathCount;
    }

    public void setMaxSubPathCount(int maxSubPathCount) {
        this.maxSubPathCount = maxSubPathCount;
    }

    public List<Integer> getWeightThresholdList() {
        return weightThresholdList;
    }

    public void setWeightThresholdList(List<Integer> weightThresholdList) {
        this.weightThresholdList = weightThresholdList;
    }

    public boolean isHasAssetKey() {
        return (Fofa_Key!=null &&!Fofa_Key.isEmpty()) || (Quake_Key!=null&&!Quake_Key.isEmpty()) || (this.Shodan_Key!=null&&!this.Shodan_Key.isEmpty()) || (this.Zoomeye_Key!=null&&!this.Zoomeye_Key.isEmpty()) || (this.Hunter_Key!=null && !this.Hunter_Key.isEmpty());
    }

    public void setHasAssetKey(boolean hasAssetKey) {
        this.hasAssetKey = hasAssetKey;
    }

    public List<NetAssets> getCompanyDomainInfoList() {
        return companyDomainInfoList;
    }

    public void setCompanyDomainInfoList(List<NetAssets> companyDomainInfoList) {
        this.companyDomainInfoList = companyDomainInfoList;
    }

    public void addCompanyDomainInfoList(NetAssets netAssets) {
        this.companyDomainInfoList.add(netAssets);
    }

    public boolean isUseGoogle() {
        return useGoogle;
    }

    public void setUseGoogle(boolean useGoogle) {
        this.useGoogle = useGoogle;
    }

    public boolean isUseGithub() {
        return useGithub;
    }

    public void setUseGithub(boolean useGithub) {
        this.useGithub = useGithub;
    }
}

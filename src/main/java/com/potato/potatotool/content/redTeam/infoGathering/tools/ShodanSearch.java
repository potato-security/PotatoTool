package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetKeyConstants;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.data.JsonUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2023/10/12 11:54
 */
public class ShodanSearch {
    private static String SHODAN_KEY;
    private static boolean Proxy = false;
    private static boolean isEffectiveKey = true;
    public ShodanSearch(String SHODAN_KEY, boolean Proxy){
        this.SHODAN_KEY = SHODAN_KEY;
        this.Proxy = Proxy;
    }

    public JsonArray search_Shodan(String qInfo) {
        JsonArray domainInfo = new JsonArray();
        if( qInfo==null || qInfo.isEmpty() || !isEffectiveKey) return domainInfo;
        if( SHODAN_KEY==null || SHODAN_KEY.isEmpty() ) return domainInfo;

        try {
            RequestObj obj = new RequestObj()
                    .setUrl("https://api.shodan.io/shodan/host/search?key=" + SHODAN_KEY + "&query=" + StrUtils.urlEncode(qInfo))
                    .setMethod("GET")
                    .setNoUserAgent(true)
                    .setTimeOut(20)
                    .setRetries(3);
            if(!Proxy) obj.setProxies(null);

            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode!= 200) {
                con.disconnect();
                return domainInfo;
            }

            domainInfo = con.getJson().getAsJsonObject().get("matches").getAsJsonArray();
        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }
        return domainInfo;
    }

    public static String getError_Shodan() {
        isEffectiveKey = true;
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        boolean Proxy = JsonUtils.containsString(tmpJsonObj.getAsJsonArray(AssetKeyConstants.PROXY_KEY), AssetKeyConstants.SHODAN_KEY);
        ShodanSearch shodanSearch = new ShodanSearch(tmpJsonObj.getAsJsonPrimitive(AssetKeyConstants.SHODAN_KEY).getAsString(), Proxy);
        String qInfo = "ip:8.8.8.8";
        if (shodanSearch.search_Shodan(qInfo).isEmpty()){
            isEffectiveKey = false;
            return "无效的Shodan_Key";
        }
        return null;
    }

    public JsonArray getInfoByDomain_Shodan(String domain) {
        if(domain==null || domain.isEmpty()) return new JsonArray();
        String qInfo = "ssl.cert.subject.cn:" + domain + " hostname:" + domain;

        return search_Shodan(qInfo);
    }

    public JsonArray getInfoByBodyFilterIcp_shodan(String companyNameStr) {
        if(companyNameStr==null || companyNameStr.isEmpty()) return new JsonArray();
        String qInfo = "http.html:" + companyNameStr;

        return search_Shodan(qInfo);
    }

    public JsonArray getInfoByIp_Shodan(String ip) {
        if(ip==null || ip.isEmpty()) return new JsonArray();
        String qInfo = "ip:" + ip;
        if(ip.contains("/")) qInfo = "net:" + ip;

        return search_Shodan(qInfo);
    }

    public JsonArray getInfoByIcon_Shodan(String iconMmh3) {
        if(iconMmh3==null || iconMmh3.isEmpty()) return new JsonArray();
        String qInfo = "http.favicon.hash:" + iconMmh3;

        return search_Shodan(qInfo);
    }

    public static void main(String[] args) {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        boolean Proxy = JsonUtils.containsString(tmpJsonObj.getAsJsonArray(AssetKeyConstants.PROXY_KEY), AssetKeyConstants.SHODAN_KEY);
        ShodanSearch shodanSearch = new ShodanSearch(tmpJsonObj.getAsJsonPrimitive(AssetKeyConstants.SHODAN_KEY).getAsString(), Proxy);
        System.out.println(shodanSearch.getInfoByDomain_Shodan("potato.gold"));
    }

}

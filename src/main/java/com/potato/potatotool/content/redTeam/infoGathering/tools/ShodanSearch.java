package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/10/12 11:54
 */
public class ShodanSearch {
    private static String SHODAN_KEY;
    public ShodanSearch(String SHODAN_KEY){
        this.SHODAN_KEY = SHODAN_KEY;
    }

    public JsonArray search_Shodan(String qInfo) {
        JsonArray domainInfo = new JsonArray();
        if( qInfo==null || qInfo.isEmpty() ) return domainInfo;
        if( SHODAN_KEY==null || SHODAN_KEY.isEmpty() ){
            System.out.println("未设置SHODAN_KEY，无法调用Shodan接口");
            return domainInfo;
        }

        try {
            RequestObj obj = new RequestObj()
                    .setUrl("https://api.shodan.io/shodan/host/search?key=" + SHODAN_KEY + "&query=" + strUtils.urlEncode(qInfo))
                    .setMethod("GET")
                    .setNoUserAgent(true)
                    .setRetries(3);

            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode!= 200) {
                return domainInfo;
            }

            domainInfo = con.getJson().getAsJsonObject().get("matches").getAsJsonArray();
        } catch (Exception e) {
            if (debugMode) System.out.println(e);
        }
        return domainInfo;
    }

    public JsonArray getInfoByDomain_Shodan(String domain) {
        if(domain==null || domain.isEmpty()) return new JsonArray();
        String qInfo = "ssl.cert.subject.cn:" + domain + " hostname:" + domain;

        return search_Shodan(qInfo);
    }

    // TODO net:118.69.133.0/24 ip:123  http.favicon.hash:iconMmh3  http.html:body内容

    public static void main(String[] args) {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Asset");
        ShodanSearch shodanSearch = new ShodanSearch(tmpJsonObj.getAsJsonPrimitive("Shodan_Key").getAsString());
        System.out.println(shodanSearch.getInfoByDomain_Shodan("potato.gold"));
    }

    public JsonArray getInfoByBodyFilterIcp_shodan(String companyNameStr, String domain) {
        if(companyNameStr==null || companyNameStr.isEmpty()) return new JsonArray();
        String qInfo = "http.html:" + companyNameStr;

        return search_Shodan(qInfo);
    }
}

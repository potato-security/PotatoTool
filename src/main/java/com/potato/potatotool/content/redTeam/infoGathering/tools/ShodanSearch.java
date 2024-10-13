package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.AssetMapper.isDomainName;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/10/12 11:54
 */
public class ShodanSearch {
    private static String SHODAN_KEY = "***REMOVED***";

    public static JsonArray search_shodan(String qInfo) {
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
            System.out.println(obj.getUrl());

            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();
            System.out.println(statusCode);
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

    public static JsonArray getDomainByDomainCert_hunter(String domain) {
        String qInfo = null;

        if(isDomainName(domain)){
            qInfo = "ssl.cert.subject.cn:" + domain;
        }

        return search_shodan(qInfo);
    }

    // TODO net:118.69.133.0/24 ip:123  hostname:googld.com  http.favicon.hash:iconMmh3   ssl.cert.subject.cn:googld.com http.html:body内容

    public static void main(String[] args) {
        System.out.println(getDomainByDomainCert_hunter("potato.gold"));
    }
}

package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;

import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/14 09:41
 */
public class ZoomeyeSearch {
    private static String ZOOMEYE_KEY = "***REMOVED***";

    public static JsonArray search_zoomeye(String qInfo) {
        JsonArray domainInfo = new JsonArray();
        if( qInfo==null || qInfo.isEmpty() ) return domainInfo;
        if( ZOOMEYE_KEY==null || ZOOMEYE_KEY.isEmpty() ){
            System.out.println("未设置QUAKE_KEY，无法调用Zoomeye接口");
            return domainInfo;
        }


        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("API-KEY", ZOOMEYE_KEY);
            RequestObj obj = new RequestObj()
                    .setUrl("https://api.zoomeye.hk/host/search?query=" + strUtils.urlEncode(qInfo) + "&page=1&facets=app,os")
                    .setMethod("GET")
                    .setHeaders(headers)
                    .setRetries(3);

            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode!= 200) {
                if(statusCode == 401){
                    System.out.println("请求未经身份验证，API 令牌缺失、无效或已过期");
                }else if(statusCode == 402){
                    System.out.println("请求未获得授权，提供的凭证无法访问指定资源");
                }
                return domainInfo;
            }

            domainInfo = con.getJson().getAsJsonObject().get("matches").getAsJsonArray();
        } catch (Exception e) {
            if (debugMode) System.out.println(e);
        }
        return domainInfo;
    }

    // TODO  cidr:52.2.254.36/24 ip:"8.8.8.8"  site:google.com hostname:google.com iconhash:"37578595"  dig:"模糊搜索body"

    public static JsonArray getDomainByCompanyOrDomain_zoomeye(String companyOrDomain) {
        String qInfo = "ssl:\"" + companyOrDomain + "\" org:\"" + companyOrDomain + "\"";
        return search_zoomeye(qInfo);
    }

    public static void main(String[] args) {
        System.out.println(getDomainByCompanyOrDomain_zoomeye("360.net"));
    }
}

package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;

import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.Utils.isDomainName;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/14 09:41
 */
public class ZoomeyeSearch {
    // Zoomeye接口均需翻墙
    private static String ZOOMEYE_KEY;
    public ZoomeyeSearch(String ZOOMEYE_KEY){
        this.ZOOMEYE_KEY = ZOOMEYE_KEY;
    }

    public JsonArray search_Zoomeye(String qInfo) {
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

    // TODO  cidr:52.2.254.36/24 ip:"8.8.8.8"  iconhash:"37578595"  dig:"模糊搜索body"

    public JsonArray getInfoByCompanyOrDomain_Zoomeye(String companyOrDomain) {
        if(companyOrDomain==null || companyOrDomain.isEmpty()) return new JsonArray();
        String qInfo = "ssl:\"" + companyOrDomain + "\" org:\"" + companyOrDomain + "\"";
        if(isDomainName(companyOrDomain)){
            qInfo += " hostname:\"" + companyOrDomain + "\" site:\"" + companyOrDomain;
        }
        return search_Zoomeye(qInfo);
    }

    public static void main(String[] args) {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Asset");
        ZoomeyeSearch zoomeyeSearch = new ZoomeyeSearch(tmpJsonObj.getAsJsonPrimitive("Zoomeye_Key").getAsString());
        System.out.println(zoomeyeSearch.getInfoByCompanyOrDomain_Zoomeye("360.net"));
    }

    public JsonArray getInfoByBodyFilterIcp_zoomeye(String companyNameStr, String domain) {
        if(companyNameStr==null || companyNameStr.isEmpty()) return new JsonArray();
        String qInfo = "dig:\"" + companyNameStr + "\"-org:\"" + companyNameStr + "\"-hostname:\"" + domain + "\"-site:\"" + domain + "\"";
        return search_Zoomeye(qInfo);
    }
}

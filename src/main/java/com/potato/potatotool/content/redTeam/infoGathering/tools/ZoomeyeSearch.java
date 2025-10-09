package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.data.JsonUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;

import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.Utils.isDomainName;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/14 09:41
 */
public class ZoomeyeSearch {
    // Zoomeye接口均需翻墙
    private static String ZOOMEYE_KEY;
    private static boolean Proxy = false;
    private static boolean isEffectiveKey = true;
    public ZoomeyeSearch(String ZOOMEYE_KEY, boolean Proxy){
        this.ZOOMEYE_KEY = ZOOMEYE_KEY;
        this.Proxy = Proxy;
    }

    public JsonArray search_Zoomeye(String qInfo) {
        JsonArray domainInfo = new JsonArray();
        if( qInfo==null || qInfo.isEmpty()  || !isEffectiveKey) return domainInfo;
        if( ZOOMEYE_KEY==null || ZOOMEYE_KEY.isEmpty() ) return domainInfo;


        Map<String, String> headers = new HashMap<>();
        headers.put("API-KEY", ZOOMEYE_KEY);
        RequestObj obj = new RequestObj()
                .setUrl("https://api.zoomeye.hk/host/search?query=" + StrUtils.urlEncode(qInfo) + "&page=1&facets=app,os")
                .setMethod("GET")
                .setHeaders(headers)
                .setTimeOut(20)
                .setRetries(3);
        if(!Proxy) obj.setProxies(null);

        try (CustomHttpResponse con = requests(obj)){
            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode!= 200) {
                if(statusCode == 401){
                    System.out.println("请求未经身份验证，API 令牌缺失、无效或已过期");
                }else if(statusCode == 402){
                    System.out.println("提供的凭证无法访问指定资源/资源信用不足");
                }
                return domainInfo;
            }

            domainInfo = con.getJson().getAsJsonObject().get("matches").getAsJsonArray();
        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }
        return domainInfo;
    }

    public static String getError_Zoomeye() {
        isEffectiveKey = true;
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(ConfigConstants.ASSET);
        boolean Proxy = JsonUtils.isProxyEnabled(ConfigConstants.ZOOMEYE_KEY);
        ZoomeyeSearch zoomeyeSearch = new ZoomeyeSearch(tmpJsonObj.getAsJsonPrimitive(ConfigConstants.ZOOMEYE_KEY).getAsString(), Proxy);
        String qInfo = "ip:\"8.8.8.8\"";
        if (zoomeyeSearch.search_Zoomeye(qInfo).isEmpty()){
            isEffectiveKey = false;
            return "无效的Zoomeye_Key/限国外IP";
        }
        return null;
    }

    public JsonArray getInfoByCompanyOrDomain_Zoomeye(String companyOrDomain) {
        if(companyOrDomain==null || companyOrDomain.isEmpty()) return new JsonArray();
        String qInfo = "ssl:\"" + companyOrDomain + "\" org:\"" + companyOrDomain + "\"";
        if(isDomainName(companyOrDomain)){
            qInfo += " hostname:\"" + companyOrDomain + "\" site:\"" + companyOrDomain;
        }
        return search_Zoomeye(qInfo);
    }

    public JsonArray getInfoByBodyFilterIcp_zoomeye(String companyNameStr) {
        if(companyNameStr==null || companyNameStr.isEmpty()) return new JsonArray();
        String qInfo = "dig:\"" + companyNameStr + "\" -org:\"" + companyNameStr + "\"";
        return search_Zoomeye(qInfo);
    }

    public JsonArray getInfoByIp_Zoomeye(String ip) {
        if(ip==null || ip.isEmpty()) return new JsonArray();
        String qInfo = "ip:\"" + ip + "\"";
        if(ip.contains("/")) qInfo = "cidr:" + ip;
        return search_Zoomeye(qInfo);
    }

    public JsonArray getInfoByIcon_Zoomeye(String iconMmh3) {
        if(iconMmh3==null || iconMmh3.isEmpty()) return new JsonArray();
        String qInfo = "iconhash:\"" + iconMmh3 + "\"";
        return search_Zoomeye(qInfo);
    }

    public static void main(String[] args) {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(ConfigConstants.ASSET);
        boolean Proxy = JsonUtils.isProxyEnabled(ConfigConstants.ZOOMEYE_KEY);
        ZoomeyeSearch zoomeyeSearch = new ZoomeyeSearch(tmpJsonObj.getAsJsonPrimitive(ConfigConstants.ZOOMEYE_KEY).getAsString(), Proxy);
        System.out.println(zoomeyeSearch.getInfoByCompanyOrDomain_Zoomeye("360.net"));
    }
}

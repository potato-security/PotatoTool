package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetKeyConstants;
import com.potato.potatotool.utils.Constants;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.jsonUtils;

import java.util.*;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.Utils.isDomainName;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/14 09:42
 */
public class QuakeSearch {
    private static List<String> QUAKE_KEY_LIST;
    private static boolean Proxy = false;
    private static boolean isEffectiveKey = true;
    public QuakeSearch(Set<String> QUAKE_KEY_LIST, boolean Proxy){
        this.QUAKE_KEY_LIST =  new ArrayList<String>(QUAKE_KEY_LIST);
        this.Proxy = Proxy;
    }

    public JsonArray search_Quake(String qInfo) {
        JsonArray domainInfo = new JsonArray();
        if( qInfo==null || qInfo.isEmpty() || !isEffectiveKey) return domainInfo;
        if(QUAKE_KEY_LIST.size()==0|| QUAKE_KEY_LIST.get(0).isEmpty())  return domainInfo;

        for(String QUAKE_KEY : QUAKE_KEY_LIST) {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-QuakeToken", QUAKE_KEY);
                JsonObject jsonData = new JsonObject();
                jsonData.addProperty("query", qInfo);
                jsonData.addProperty("start", 0);
                jsonData.addProperty("size", 500);
                RequestObj obj = new RequestObj()
                        .setUrl("https://quake.360.net/api/v3/search/quake_service")
                        .setMethod("POST")
                        .setHeaders(headers)
                        .setPostData(jsonData)
                        .setTimeOut(20)
                        .setRetries(3);
                if(!Proxy) obj.setProxies(null);

                CustomHttpResponse con = requests(obj);
                int statusCode = con.getResponseCode();
                // 检查请求状态码
                if (statusCode != 200) {
                    con.disconnect();
                    return domainInfo;
                }
                JsonObject res = con.getJson().getAsJsonObject();
                if(res.has("code") && res.get("code").getAsString().equals("q2001")){
                    System.out.println("QUAKE积分已用完");
                }else {
                    JsonElement resData = res.get("data");
                    if (resData.isJsonArray()) {
                        domainInfo = resData.getAsJsonArray();
                        break;
                    }
                }
            } catch (Exception e) {
                if (debugMode) e.printStackTrace();
            }
        }
        return domainInfo;
    }

    public static String getError_Quake() {
        isEffectiveKey = true;
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        Set<String> Quake_Key_Set = new HashSet<>();
        for (JsonElement element : tmpJsonObj.getAsJsonArray(AssetKeyConstants.QUAKE_KEY)) {
            Quake_Key_Set.add(element.getAsString());
        }
        boolean Proxy = jsonUtils.containsString(tmpJsonObj.getAsJsonArray(AssetKeyConstants.PROXY_KEY), AssetKeyConstants.QUAKE_KEY);
        QuakeSearch quakeSearch = new QuakeSearch(Quake_Key_Set, Proxy);
        String qInfo = "ip:\"8.8.8.8\"";
        if (quakeSearch.search_Quake(qInfo).isEmpty()){
            isEffectiveKey = false;
            return "无效的Quake_Key/积分用完了";
        }
        return null;
    }

    public JsonArray getInfoByIcpNo_Quake(String icpNo) {
        if(icpNo==null || icpNo.isEmpty()) return new JsonArray();
        String qInfo = "icp:\"" + icpNo + "\"";
        return search_Quake(qInfo);
    }

    public JsonArray getInfoByBodyFilteIcp_Quake(String companyNameStr) {
        if(companyNameStr==null || companyNameStr.isEmpty()) return new JsonArray();
        String qInfo = "body:\"" + companyNameStr + "\" AND NOT cert:\"" + companyNameStr + "\"";
        return search_Quake(qInfo);
    }

    public JsonArray getInfoByCompanyOrDomain_Quake(String companyOrDomain) {
        if(companyOrDomain==null || companyOrDomain.isEmpty()) return new JsonArray();
        String qInfo = "cert:\"" + companyOrDomain + "\"";
        if(isDomainName(companyOrDomain)){
            qInfo += " OR domain:\"" + companyOrDomain + "\"";
        }
        return search_Quake(qInfo);
    }

    public JsonArray getInfoByIp_Quake(String ip) {
        if(ip==null || ip.isEmpty()) return new JsonArray();
        String qInfo = "ip:\"" + ip + "\"";
        return search_Quake(qInfo);
    }

    public JsonArray getInfoByIcon_Quake(String iconMd5) {
        if(iconMd5==null || iconMd5.isEmpty()) return new JsonArray();
        String qInfo = "favicon:\"" + iconMd5 + "\"";
        return search_Quake(qInfo);
    }

    public static void main(String[] args) {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        Set<String> Quake_Key_Set = new HashSet<>();
        for (JsonElement element : tmpJsonObj.getAsJsonArray(AssetKeyConstants.QUAKE_KEY)) {
            Quake_Key_Set.add(element.getAsString());
        }
        boolean Proxy = jsonUtils.containsString(tmpJsonObj.getAsJsonArray(AssetKeyConstants.PROXY_KEY), AssetKeyConstants.QUAKE_KEY);
        QuakeSearch quakeSearch = new QuakeSearch(Quake_Key_Set, Proxy);
        System.out.println(quakeSearch.getInfoByCompanyOrDomain_Quake("360.net"));
    }

}

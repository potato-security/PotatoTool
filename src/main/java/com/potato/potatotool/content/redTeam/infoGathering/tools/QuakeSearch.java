package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;

import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.Utils.isDomainName;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/14 09:42
 */
public class QuakeSearch {
    private static String QUAKE_KEY;
    public QuakeSearch(String QUAKE_KEY){
        this.QUAKE_KEY = QUAKE_KEY;
    }

    public JsonArray search_Quake(String qInfo) {
        JsonArray domainInfo = new JsonArray();
        if( qInfo==null || qInfo.isEmpty() ) return domainInfo;
        if( QUAKE_KEY==null || QUAKE_KEY.isEmpty() ){
            System.out.println("未设置QUAKE_KEY，无法调用Quake接口");
            return domainInfo;
        }

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
                    .setRetries(3);

            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode!= 200) {
                return domainInfo;
            }

            domainInfo = con.getJson().getAsJsonObject().get("data").getAsJsonArray();
        } catch (Exception e) {
            if (debugMode) System.out.println(e);
        }
        return domainInfo;
    }

    public JsonArray getInfoByIcpNo_Quake(String icpNo) {
        if(icpNo==null || icpNo.isEmpty()) return new JsonArray();
        String qInfo = "icp:\"" + icpNo + "\"";
        return search_Quake(qInfo);
    }

    // TODO  OR  模糊搜索公司名记得需要带双引号  ip: "1.1.1.1/16" body:"奇虎" favicon:"0488faca4c19046b94d07c3ee83cf9d6"

    public JsonArray getInfoByBodyFilteIcp_Quake(String companyNameStr, String domainStr) {
        if(companyNameStr==null || companyNameStr.isEmpty()) return new JsonArray();
        String qInfo = "body:\"" + companyNameStr + "\" AND NOT cert:\"" + companyNameStr + "\" AND NOT domain:\"" + domainStr + "\"";
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

    public static void main(String[] args) {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Asset");
        QuakeSearch quakeSearch = new QuakeSearch(tmpJsonObj.getAsJsonPrimitive("Quake_Key").getAsString());
        System.out.println(quakeSearch.getInfoByCompanyOrDomain_Quake("360.net"));
    }

}

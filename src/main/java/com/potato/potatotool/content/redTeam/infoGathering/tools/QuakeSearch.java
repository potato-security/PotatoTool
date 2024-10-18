package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;

import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.AssetMapper.isDomainName;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/14 09:42
 */
public class QuakeSearch {
    private static String QUAKE_KEY = "***REMOVED***";

    public static JsonArray search_quake(String qInfo) {
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

    // TODO  OR  模糊搜索公司名记得需要带双引号  ip: "1.1.1.1/16"  domain:"360.cn"  icp:"京ICP备08010314号" body:"奇虎" favicon:"0488faca4c19046b94d07c3ee83cf9d6"

    public static JsonArray getDomainByCompanyOrDomain_quake(String companyOrDomain) {
        String qInfo = "cert:\"" + companyOrDomain + "\"";
        return search_quake(qInfo);
    }

    public static void main(String[] args) {
        System.out.println(getDomainByCompanyOrDomain_quake("360.net"));
    }
}

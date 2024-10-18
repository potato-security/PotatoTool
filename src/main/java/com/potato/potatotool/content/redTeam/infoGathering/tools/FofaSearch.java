package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.AssetMapper.isDomainName;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/10/1 15:53
 */
public class FofaSearch {
    private static String FOFA_KEY = "***REMOVED***";

    /**
     *         单独一个不需要认证的FOFA接口
     * FOFAMap 提供快速查看某IP承载的端口和协议的方法
     * @param ipOrDomain
     * @return
     */
    public static JsonObject getBriefExtendedInfo_fofa(String ipOrDomain) {
        JsonObject briefExtendedInfo = new JsonObject();

        try {
            RequestObj obj = new RequestObj()
                    .setUrl("https://amap.fofa.info/host/" + ipOrDomain)
                    .setMethod("GET").setRetries(3);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode != 200) {
                return briefExtendedInfo;
            }

            briefExtendedInfo = con.getJson().getAsJsonObject();

        }catch (Exception e){
            if(debugMode) e.printStackTrace();
        }

        return briefExtendedInfo;
    }

    public static JsonArray search_fofa(String qInfo) {
        JsonArray domainInfo = new JsonArray();
        if( qInfo==null || qInfo.isEmpty() ) return domainInfo;
        if( FOFA_KEY==null || FOFA_KEY.isEmpty() ){
            System.out.println("未设置FOFA_KEY，无法调用FOFA接口");
            return domainInfo;
        }

        try {
            RequestObj obj = new RequestObj()
                    .setUrl("https://fofa.info/api/v1/search/all?&size=10000&fields=ip,domain,host,icp,port,protocol,title,certs_subject_org&key=" + FOFA_KEY + "&qbase64=" + strUtils.urlEncode(strUtils.base64Encode(qInfo)))
                    .setMethod("GET")
                    .setRetries(3);
            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode!= 200) {
                return domainInfo;
            }
            domainInfo = con.getJson().getAsJsonObject().get("results").getAsJsonArray();
        } catch (Exception e) {
            if (debugMode) System.out.println(e);
        }
        return domainInfo;
    }

    public static JsonArray getDomainByIcon_fofa(String hash_mmh3) {
        String qInfo = "icon_hash=\"" + hash_mmh3 + "\"";
        return search_fofa(qInfo);
    }

    public static JsonArray getDomainByCompanyOrDomain_fofa(String companyOrDomain) { // TODO 需要完全确定的公司名，不要模糊匹配
        String qInfo = "cert=\"" + companyOrDomain + "\"";
        return search_fofa(qInfo);
    }

    public static JsonArray getDomainByIp_fofa(String ip) {
        String qInfo = "ip=\"" + ip + "\"";
        return search_fofa(qInfo);
    }

    // TODO domain="potato.gold"	icp="京ICP证030173号"

    public static void main(String[] args) {

//        JsonArray domainInfo = getDomainByCompanyOrDomain_fofa("国家能源投资集团有限责任公司");
//        System.out.println(domainInfo);
//        System.out.println(domainInfo.size());

        JsonArray ipInfo = getDomainByIp_fofa("13.227.83.19");
        System.out.println(ipInfo);
//
//        JsonArray domainInfo1 = getDomainByIcon_fofa("37578595");
//        System.out.println(domainInfo1);
//        System.out.println(domainInfo1.size());
//
//
//        JsonObject briefExtendedInfo = getBriefExtendedInfo_fofa("potato.gold");
//        System.out.println(briefExtendedInfo);
    }
}

package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;
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
    private static String FOFA_KEY;
    public FofaSearch(String FOFA_KEY){
        this.FOFA_KEY = FOFA_KEY;
    }

    /**
     *         单独一个不需要认证的FOFA接口
     * FOFAMap 提供快速查看某IP承载的端口和协议的方法
     * @param ipOrDomain
     * @return
     */
    public JsonObject getBriefExtendedInfo_Fofa(String ipOrDomain) {
        JsonObject briefExtendedInfo = new JsonObject();
        if(ipOrDomain==null || ipOrDomain.isEmpty()) return briefExtendedInfo;

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

    public JsonArray search_Fofa(String qInfo) {
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

    public JsonArray getInfoByIcon_Fofa(String hash_mmh3) {
        if(hash_mmh3==null || hash_mmh3.isEmpty()) return new JsonArray();
        String qInfo = "icon_hash=\"" + hash_mmh3 + "\"";
        return search_Fofa(qInfo);
    }

    public JsonArray getInfoByCompanyOrDomain_Fofa(String companyOrDomain) {
        if(companyOrDomain==null || companyOrDomain.isEmpty()) return new JsonArray();
        String qInfo = "cert=\"" + companyOrDomain + "\"";
        if(isDomainName(companyOrDomain)){
            qInfo += "||domain=\"" + companyOrDomain + "\"";
        }
        return search_Fofa(qInfo);
    }

    public JsonArray getInfoByIp_Fofa(String ip) {
        if(ip==null || ip.isEmpty()) return new JsonArray();
        String qInfo = "ip=\"" + ip + "\"";
        return search_Fofa(qInfo);
    }

    public JsonArray getInfoByIcpNo_Fofa(String icpNo) {
        if(icpNo==null || icpNo.isEmpty()) return new JsonArray();
        String qInfo = "icp=\"" + icpNo + "\"";
        return search_Fofa(qInfo);
    }

    public JsonArray getInfoByBodyFilterIcp_Fofa(String companyNameStr, String domainStr) {
        if(companyNameStr==null || companyNameStr.isEmpty()) return new JsonArray();
        String qInfo = "body=\"" + companyNameStr + "\" && cert!=\"" + companyNameStr + "\" && domain!=\"" + domainStr + "\"";
        return search_Fofa(qInfo);
    }

    public static void main(String[] args) {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Asset");
        FofaSearch fofaSearch = new FofaSearch(tmpJsonObj.getAsJsonPrimitive("Fofa_Key").getAsString());

//        JsonArray domainInfo = fofaSearch.getDomainByCompanyOrDomain_Fofa("国家能源投资集团有限责任公司");
//        System.out.println(domainInfo);
//        System.out.println(domainInfo.size());

        JsonArray ipInfo = fofaSearch.getInfoByIp_Fofa("13.227.83.19");
        System.out.println(ipInfo);
//
//        JsonArray domainInfo1 = fofaSearch.getDomainByIcon_Fofa("37578595");
//        System.out.println(domainInfo1);
//        System.out.println(domainInfo1.size());
//
//
//        JsonObject briefExtendedInfo = fofaSearch.getBriefExtendedInfo_Fofa("potato.gold");
//        System.out.println(briefExtendedInfo);
    }
}

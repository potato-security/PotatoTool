package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetKeyConstants;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.data.JsonUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.Utils.*;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.Utils.getElementText;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2023/9/28 11:16
 */
public class GetCompany {

    private  static String Chinaz_Cookie = "";
    private static Boolean Proxy = false;
    static {
        JsonObject tmpJsonObj_Asset = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        Chinaz_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(AssetKeyConstants.CHINAZ_COOKIE).getAsString();
        Proxy = JsonUtils.containsString(tmpJsonObj_Asset.getAsJsonArray(AssetKeyConstants.PROXY_KEY), AssetKeyConstants.CHINAZ_COOKIE);
    }

    public static JsonArray getCompany_chinaz(String company) {
        JsonObject tmpJsonObj_Asset = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        Chinaz_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(AssetKeyConstants.CHINAZ_COOKIE).getAsString();
        Proxy = JsonUtils.containsString(tmpJsonObj_Asset.getAsJsonArray(AssetKeyConstants.PROXY_KEY), AssetKeyConstants.CHINAZ_COOKIE);

        JsonArray companyList = new JsonArray();
        if(company.isEmpty()) return companyList;

        try {
            int index = 0;
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", Chinaz_Cookie);

            while (true) {
                index += 1;

                RequestObj obj = new RequestObj().setUrl("https://data.chinaz.com/company/t0-p0-c0-i0-d0-s-" + StrUtils.urlEncode(company) + "/" + index)
                        .setMethod("GET").setRetries(3).setHeaders(headers);
                if(!Proxy) obj.setProxies(null);
                try (CustomHttpResponse con = requests(obj)) {
                    int statusCode = con.getResponseCode();
                    if (statusCode != 200) {
                        return companyList;
                    }

                    Document doc = con.getDocument();

                    Elements ulElements = getElements(doc, "body > div:nth-of-type(2) > div:nth-of-type(2) > div > ul ");

                    if (ulElements.size() > 1) {
                        for (int i = 1; i < ulElements.size(); i++) { // 从第二个ul开始(第一个ul为头部标签)
                            JsonObject companyInfo = new JsonObject();
                            Element ul = ulElements.get(i);
                            companyInfo.addProperty("企业名称", getElementText(ul, "li:nth-of-type(2) > a"));
                            companyInfo.addProperty("企业ID", getElementAttr(ul, "li:nth-of-type(2) > a", "href") .replace("/company/", ""));
                            companyInfo.addProperty("企业状态", getElementText(ul, "li:nth-of-type(3)"));
                            companyInfo.addProperty("法定代表人", getElementText(ul, "li:nth-of-type(5)"));
                            companyInfo.addProperty("注册资本", getElementText(ul, "li:nth-of-type(6)"));
                            companyInfo.addProperty("注册时间", getElementText(ul, "li:nth-of-type(7)"));
                            companyInfo.addProperty("deepGet", false);
                            companyList.add(companyInfo);
                        }
                        if (ulElements.size() < 21) break;
                    }else {
                        // 可能会出现：【服务器繁忙！ 请等待】  不必处理，没必要爬取太多数据
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }

        return companyList;
    }

    public static JsonObject getCompanyDetails_chinaz(String companyId, String companyName) {
        JsonObject tmpJsonObj_Asset = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        Chinaz_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(AssetKeyConstants.CHINAZ_COOKIE).getAsString();
        Proxy = JsonUtils.containsString(tmpJsonObj_Asset.getAsJsonArray(AssetKeyConstants.PROXY_KEY), AssetKeyConstants.CHINAZ_COOKIE);

        JsonObject companyDetailsMap = new JsonObject();

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", Chinaz_Cookie);
            RequestObj obj = new RequestObj().setUrl("https://data.chinaz.com/company/" + companyId)
                    .setMethod("GET").setRetries(3).setHeaders(headers);
            try (CustomHttpResponse con = requests(obj)) {
                int statusCode = con.getResponseCode();
                if (statusCode != 200) {
                    return companyDetailsMap;
                }

                Document doc = con.getDocument();

                // 提取信息
                JsonObject businessInfoMap = extractBusinessInfo_chinaz(doc);
                JsonArray wxInfoList = extractWxInfo_chinaz(doc);
                JsonArray softwareInfoList = extractSoftwareInfo_chinaz(doc);
                JsonArray icpInfoList = extractIcpInfo_chinaz(doc);

                companyDetailsMap.addProperty("企业名称", companyName);
                companyDetailsMap.add("工商信息", businessInfoMap);
                companyDetailsMap.add("微信公众号", wxInfoList);
                companyDetailsMap.add("软件著作", softwareInfoList);
                companyDetailsMap.add("网站备案", icpInfoList);
            }
        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
        }

        return companyDetailsMap;
    }

    private static JsonArray extractIcpInfo_chinaz(Document doc) {
        JsonArray IcpInfoList = new JsonArray();

        Elements trElements = getElements(doc, "#wzba-module + * > tbody > tr");

        for (Element tr : trElements){
            JsonObject icpInfo = new JsonObject();

            String domain = getElementText(tr, "td:nth-of-type(5)");
            icpInfo.addProperty("网站域名", domain);
            icpInfo.addProperty("备案号", getElementText(tr, "td:nth-of-type(6)"));
            icpInfo.addProperty("网站名称", getElementText(tr, "td:nth-of-type(3)"));
            IcpInfoList.add(icpInfo);
        }

        return IcpInfoList;
    }

    private static JsonArray extractSoftwareInfo_chinaz(Document doc) {
        JsonArray SoftwareInfoList = new JsonArray();

        Elements trElements = getElements(doc, "#rjzzq-module + * > tbody > tr");

        for (Element tr : trElements){
            JsonObject softwareInfo = new JsonObject();

            softwareInfo.addProperty("软件简称", getElementText(tr, "td:nth-of-type(4)"));
            softwareInfo.addProperty("版本号", getElementText(tr, "td:nth-of-type(7)"));
            SoftwareInfoList.add(softwareInfo);
        }

        return SoftwareInfoList;
    }

    private static JsonArray extractWxInfo_chinaz(Document doc) {
        JsonArray WxInfoList = new JsonArray();

        Elements trElements = getElements(doc, "#wxgzh-module + * > tbody > tr");

        for (Element tr : trElements){
            JsonObject wxInfo = new JsonObject();

            wxInfo.addProperty("公众号名称", getElementText(tr, "td:nth-of-type(2)"));
            wxInfo.addProperty("微信号", getElementText(tr, "td:nth-of-type(3)"));
            wxInfo.addProperty("公众号简介", getElementText(tr, "td:nth-of-type(4)"));
            WxInfoList.add(wxInfo);
        }

        return WxInfoList;
    }

    private static JsonObject extractBusinessInfo_chinaz(Document doc) {

        JsonObject businessInfoMap = new JsonObject();
        businessInfoMap.addProperty("工商注册号", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(2)"));
        businessInfoMap.addProperty("组织机构代码", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(4)"));
        businessInfoMap.addProperty("统一社会信用代码", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2)"));
        businessInfoMap.addProperty("纳税人识别号", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(5) > td:nth-of-type(2)"));
        businessInfoMap.addProperty("公司类型", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(4)"));
        businessInfoMap.addProperty("所属行业", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(5) > td:nth-of-type(4)"));
        businessInfoMap.addProperty("地址", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(2) > div > div:nth-of-type(2) > div > div:nth-of-type(2)"));

        return businessInfoMap;
    }

    public static void main(String[] args) throws Exception {
//        JsonArray companyList = getCompany_chinaz("清华大学");
        JsonObject companyDetailsMap = getCompanyDetails_chinaz("5d54cf13f1ae7a274b7cf610", "清华大学");//清华大学
        System.out.println(companyDetailsMap);
//        System.out.println(companyList);
//        List<Map<String, String>> companyList = getCompany_chinaz("清华大学");
//        List<Map<String, String>> companyList = getCompany_chinaz("神华集团有限责任公司北京服务分公司");

//        List<Map<String, String>> companyList = getCompany_chinaz("深圳市投资控股有限公司");
//        System.out.println(companyList);
//        System.out.println(companyList.size());
//        Map<String, Object> companyDetailsMap = getCompanyDetails_chinaz(companyList.get(0).get("企业ID"));
//        System.out.println(companyDetailsMap);
//        System.out.println(companyDetailsMap.size());

//        try (BufferedReader br = new BufferedReader(new FileReader("/Users/a/Library/Containers/com.tencent.WeWorkMac/Data/Documents/Profiles/72E3F51AB224938EF8C93E98DF173761/Caches/Files/2024-10/d45c19ed4b78299a023b215065d7995b/123.txt"));
//            CSVWriter writer = new CSVWriter(new FileWriter("/Users/a/Library/Containers/com.tencent.WeWorkMac/Data/Documents/Profiles/72E3F51AB224938EF8C93E98DF173761/Caches/Files/2024-10/d45c19ed4b78299a023b215065d7995b/website_info.csv"));
//        ) {
//            String[] header = {"公司名", "网站域名"};
//            writer.writeNext(header);
//            String line;
//            while ((line = br.readLine()) != null) {
//                String companyName = line.trim();
//                if(companyName.isEmpty()) continue;
//                System.out.println(companyName);
//
//                JsonArray domainInfo_fofa = FofaSearch.getDomainByCompanyOrDomain_fofa(companyName);
//                JsonArray domainInfo_hunter = HunterSearch.getDomainByCompanyOrDomain_hunter(companyName);
//
//                try {
//                    List<Map<String, String>> companyList = getCompany_chinaz(companyName);
//                    if(companyList.size()==0) continue;
//                    Map<String, Object> companyDetailsMap = getCompanyDetails_chinaz(companyList.get(0).get("企业ID"));
//
//                    // 获取网站备案信息并写入 CSV 文件
//                    List<Map<String, String>> websiteRegistrations = (List<Map<String, String>>) companyDetailsMap.get("网站备案");
//                    Set<String> domainList =new HashSet<>();
//                    for (Map<String, String> registration : websiteRegistrations) {
//                        domainList.add(registration.get("网站域名"));
//                    }
//                    for(JsonElement jsonElement : domainInfo_fofa ){
//                        JsonArray jsonElement_Array = jsonElement.getAsJsonArray();
//                        String tmpDomain = jsonElement_Array.get(1).getAsString();
//                        if (tmpDomain.isEmpty()) jsonElement_Array.get(2).getAsString();
//                        domainList.add(tmpDomain);
//                    }
//                    for(JsonElement jsonElement : domainInfo_hunter){
//                        JsonObject jsonElement_obj = jsonElement.getAsJsonObject();
//                        String tmpDomain = jsonElement_obj.get("domain").getAsString();
//                        if (tmpDomain.isEmpty()) jsonElement_obj.get("url").getAsString();
//                        domainList.add(tmpDomain);
//                    }
//                    Set<String> tmpDomainList = new HashSet<>();
//                    for(String str : domainList){
//                        tmpDomainList.addAll(GetSubDomain.getSubByDomainOrDomainCert(str));
//                    }
//                    domainList.addAll(tmpDomainList);
//                    for(String str : domainList){
//                        writer.writeNext(new String[]{
//                                companyName,
//                                str
//                        });
//                    }
//                }catch (Exception ee){
//                    System.out.println(ee);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


}

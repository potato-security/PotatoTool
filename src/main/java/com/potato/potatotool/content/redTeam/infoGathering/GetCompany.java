package com.potato.potatotool.content.redTeam.infoGathering;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.opencsv.CSVWriter;
import com.potato.potatotool.content.redTeam.infoGathering.subDomain.GetSubDomain;
import com.potato.potatotool.content.redTeam.infoGathering.tools.FofaSearch;
import com.potato.potatotool.content.redTeam.infoGathering.tools.HunterSearch;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.Utils.*;
import static com.potato.potatotool.content.redTeam.infoGathering.Utils.getElementText;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/9/28 11:16
 */
public class GetCompany {

    public static List<Map<String, String>> getCompany_chinaz(String company) {
        List<Map<String, String>> companyList = new ArrayList<>();

        try {
            int index = 0;

            while (true) {
                index += 1;

                RequestObj obj = new RequestObj().setUrl("https://data.chinaz.com/company/t0-p0-c0-i0-d0-s-" + strUtils.urlEncode(company) + "/" + index)
                        .setMethod("GET").setRetries(3);
                CustomHttpResponse con = requests(obj);

                int statusCode = con.getResponseCode();
                if (statusCode != 200) {
                    return companyList;
                }

                Document doc = con.getDocument();
                Elements ulElements = getElements(doc, "body > div:nth-of-type(2) > div:nth-of-type(2) > div > ul ");

                if (ulElements.size() > 1) {
                    for (int i = 1; i < ulElements.size(); i++) { // 从第二个ul开始(第一个ul为头部标签)
                        Map<String, String> companyInfo = new HashMap<>();
                        Element ul = ulElements.get(i);
                        companyInfo.put("企业名称", getElementText(ul, "li:nth-of-type(2) > a"));
                        companyInfo.put("企业ID", getElementAttr(ul, "li:nth-of-type(2) > a", "href") .replace("/company/", ""));
                        companyInfo.put("企业状态", getElementText(ul, "li:nth-of-type(3)"));
                        companyInfo.put("法定代表人", getElementText(ul, "li:nth-of-type(5)"));
                        companyInfo.put("注册资本", getElementText(ul, "li:nth-of-type(6)"));
                        companyInfo.put("注册时间", getElementText(ul, "li:nth-of-type(7)"));
                        companyList.add(companyInfo);
                    }
                    if (ulElements.size() < 21) break;
                }else {
                    // 可能会出现：【服务器繁忙！ 请等待】  不必处理，没必要爬取太多数据
                    break;
                }

            }
        } catch (Exception e) {
            if (debugMode) System.out.println(e);
        }

        return companyList;
    }

    public static Map<String, Object> getCompanyDetails_chinaz(String companyId) {
        Map<String, Object> companyDetailsMap = new HashMap<>();

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", "***REMOVED***");
            RequestObj obj = new RequestObj().setUrl("https://data.chinaz.com/company/" + companyId)
                    .setMethod("GET").setRetries(3).setHeaders(headers); // TODO 设置Cookie才能读取到备案网站

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                return companyDetailsMap;
            }

            Document doc = con.getDocument();

            // 提取信息
            Map<String, String> businessInfoMap = extractBusinessInfo_chinaz(doc);
            List<Map<String, String>> wxInfoList = extractWxInfo_chinaz(doc);
            List<Map<String, String>> softwareInfoList = extractSoftwareInfo_chinaz(doc);
            List<Map<String, String>> icpInfoList = extractIcpInfo_chinaz(doc);

            companyDetailsMap.put("工商信息", businessInfoMap);
            companyDetailsMap.put("微信公众号", wxInfoList);
            companyDetailsMap.put("软件著作", softwareInfoList);
            companyDetailsMap.put("网站备案", icpInfoList);

        } catch (Exception e) {
            if(debugMode) System.out.println(e);
        }

        return companyDetailsMap;
    }

    private static List<Map<String, String>> extractIcpInfo_chinaz(Document doc) {
        List<Map<String, String>> IcpInfoList = new ArrayList<>();

        Elements trElements = getElements(doc, "#wzba-module + * > tbody > tr");

        for (Element tr : trElements){
            Map<String, String> icpInfo = new HashMap<>();

            String domain = getElementText(tr, "td:nth-of-type(5)");
            icpInfo.put("网站域名", domain);
            icpInfo.put("备案号", getElementText(tr, "td:nth-of-type(6)"));
            icpInfo.put("网站名称", getElementText(tr, "td:nth-of-type(3)"));
            IcpInfoList.add(icpInfo);
        }

        return IcpInfoList;
    }

    private static List<Map<String, String>> extractSoftwareInfo_chinaz(Document doc) {
        List<Map<String, String>> SoftwareInfoList = new ArrayList<>();

        Elements trElements = getElements(doc, "#rjzzq-module + * > tbody > tr");

        for (Element tr : trElements){
            Map<String, String> softwareInfo = new HashMap<>();

            softwareInfo.put("软件简称", getElementText(tr, "td:nth-of-type(4)"));
            softwareInfo.put("版本号", getElementText(tr, "td:nth-of-type(7)"));
            SoftwareInfoList.add(softwareInfo);
        }

        return SoftwareInfoList;
    }

    private static List<Map<String, String>> extractWxInfo_chinaz(Document doc) {
        List<Map<String, String>> WxInfoList = new ArrayList<>();

        Elements trElements = getElements(doc, "#wxgzh-module + * > tbody > tr");

        for (Element tr : trElements){
            Map<String, String> wxInfo = new HashMap<>();

            wxInfo.put("公众号名称", getElementText(tr, "td:nth-of-type(2)"));
            wxInfo.put("微信号", getElementText(tr, "td:nth-of-type(3)"));
            wxInfo.put("公众号简介", getElementText(tr, "td:nth-of-type(4)"));
            WxInfoList.add(wxInfo);
        }

        return WxInfoList;
    }

    private static Map<String, String> extractBusinessInfo_chinaz(Document doc) {

        Map<String, String> businessInfoMap = new HashMap<>();
        businessInfoMap.put("工商注册号", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(2)"));
        businessInfoMap.put("组织机构代码", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(4)"));
        businessInfoMap.put("统一社会信用代码", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2)"));
        businessInfoMap.put("纳税人识别号", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(5) > td:nth-of-type(2)"));
        businessInfoMap.put("公司类型", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(4)"));
        businessInfoMap.put("所属行业", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(4) > div:nth-of-type(1) > table > tbody > tr:nth-of-type(5) > td:nth-of-type(4)"));
        businessInfoMap.put("地址", getElementText(doc, "body > div:nth-of-type(3) > div:nth-of-type(2) > div > div:nth-of-type(2) > div > div:nth-of-type(2)"));

        return businessInfoMap;
    }

    public static void main(String[] args) throws Exception {
//        List<Map<String, String>> companyList = getCompany_chinaz("清华大学");
//        List<Map<String, String>> companyList = getCompany_chinaz("神华集团有限责任公司北京服务分公司");

//        List<Map<String, String>> companyList = getCompany_chinaz("深圳市投资控股有限公司");
//        System.out.println(companyList);
//        System.out.println(companyList.size());
//        Map<String, Object> companyDetailsMap = getCompanyDetails_chinaz(companyList.get(0).get("企业ID"));
//        System.out.println(companyDetailsMap);
//        System.out.println(companyDetailsMap.size());

        try (BufferedReader br = new BufferedReader(new FileReader("/Users/a/Library/Containers/com.tencent.WeWorkMac/Data/Documents/Profiles/72E3F51AB224938EF8C93E98DF173761/Caches/Files/2024-10/d45c19ed4b78299a023b215065d7995b/123.txt"));
            CSVWriter writer = new CSVWriter(new FileWriter("/Users/a/Library/Containers/com.tencent.WeWorkMac/Data/Documents/Profiles/72E3F51AB224938EF8C93E98DF173761/Caches/Files/2024-10/d45c19ed4b78299a023b215065d7995b/website_info.csv"));
        ) {
            String[] header = {"公司名", "网站域名"};
            writer.writeNext(header);
            String line;
            while ((line = br.readLine()) != null) {
                String companyName = line.trim();
                if(companyName.isEmpty()) continue;
                System.out.println(companyName);

                JsonArray domainInfo_fofa = FofaSearch.getDomainByCompanyOrDomain_fofa(companyName);
                JsonArray domainInfo_hunter = HunterSearch.getDomainByCompanyOrDomain_hunter(companyName);

                try {
                    List<Map<String, String>> companyList = getCompany_chinaz(companyName);
                    if(companyList.size()==0) continue;
                    Map<String, Object> companyDetailsMap = getCompanyDetails_chinaz(companyList.get(0).get("企业ID"));

                    // 获取网站备案信息并写入 CSV 文件
                    List<Map<String, String>> websiteRegistrations = (List<Map<String, String>>) companyDetailsMap.get("网站备案");
                    Set<String> domainList =new HashSet<>();
                    for (Map<String, String> registration : websiteRegistrations) {
                        domainList.add(registration.get("网站域名"));
                    }
                    for(JsonElement jsonElement : domainInfo_fofa ){
                        JsonArray jsonElement_Array = jsonElement.getAsJsonArray();
                        String tmpDomain = jsonElement_Array.get(1).getAsString();
                        if (tmpDomain.isEmpty()) jsonElement_Array.get(2).getAsString();
                        domainList.add(tmpDomain);
                    }
                    for(JsonElement jsonElement : domainInfo_hunter){
                        JsonObject jsonElement_obj = jsonElement.getAsJsonObject();
                        String tmpDomain = jsonElement_obj.get("domain").getAsString();
                        if (tmpDomain.isEmpty()) jsonElement_obj.get("url").getAsString();
                        domainList.add(tmpDomain);
                    }
                    Set<String> tmpDomainList = new HashSet<>();
                    for(String str : domainList){
                        tmpDomainList.addAll(GetSubDomain.getSubByDomainOrDomainCert(str));
                    }
                    domainList.addAll(tmpDomainList);
                    for(String str : domainList){
                        writer.writeNext(new String[]{
                                companyName,
                                str
                        });
                    }
                }catch (Exception ee){
                    System.out.println(ee);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

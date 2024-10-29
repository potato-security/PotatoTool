package com.potato.potatotool.content.redTeam.infoGathering;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetObj;
import com.potato.potatotool.content.redTeam.infoGathering.subDomain.GetSubDomain;
import com.potato.potatotool.content.redTeam.infoGathering.subDomain.SubdomainBruteForcer;
import com.potato.potatotool.content.redTeam.infoGathering.tools.*;
import com.potato.potatotool.content.redTeam.infoGathering.utils.AiUtils;
import com.potato.potatotool.content.redTeam.infoGathering.tools.GetCompany;
import com.potato.potatotool.content.redTeam.infoGathering.utils.Utils;

import java.util.Set;
import java.util.regex.Pattern;

import static com.potato.potatotool.content.redTeam.infoGathering.tools.GetSeo.*;
import static com.potato.potatotool.content.redTeam.infoGathering.tools.GetDomain.*;

/**
 * @author Potato
 * @date 2023/4/23 10:05
 *
 * 空间测绘主类
 */
public class AssetMapper {

    private static String Fofa_Key;
    private static Set<String> Hunter_Key;
    private static String Quake_Key;
    private static String Shodan_Key;
    private static String Zoomeye_Key;

    private static FofaSearch fofaSearch;
    private static HunterSearch hunterSearch;
    private static QuakeSearch quakeSearch;
    private static ShodanSearch shodanSearch;
    private static ZoomeyeSearch zoomeyeSearch;

    public static void searchInfo(String input) {
        AssetObj assetObj = new AssetObj();

        Fofa_Key = assetObj.getFofa_Key();
        Hunter_Key = assetObj.getHunter_Key();
        Quake_Key = assetObj.getQuake_Key();
        Shodan_Key = assetObj.getShodan_Key();
        Zoomeye_Key = assetObj.getZoomeye_Key();

        fofaSearch = new FofaSearch(Fofa_Key);
        hunterSearch = new HunterSearch(Hunter_Key);
        quakeSearch = new QuakeSearch(Quake_Key);
        shodanSearch = new ShodanSearch(Shodan_Key);
        zoomeyeSearch = new ZoomeyeSearch(Zoomeye_Key);

        // TODO 网址换成域名/ip

        if (Utils.isIPAddress(input)) {
            handleIPAddress(input, assetObj);
        } else if (Utils.isDomainName(input)) {
            handleDomainName(input, assetObj);
        } else {
            handleCompanyName(input, assetObj);
        }
    }



    // 处理IP地址的逻辑
    private static void handleIPAddress(String ip, AssetObj assetObj) {
        System.out.println("输入了一个IP地址: " + ip);
        JsonArray domainList = getDomainByIp(ip);
        System.out.println("反查IP");
        System.out.println(domainList);
        JsonArray domainListChoose = domainList;
        // TODO 选择需要展示的、需要深度检索的、
        for (JsonElement jsonElement :domainListChoose){
            String domain = jsonElement.getAsJsonObject().get("domain").getAsString();
            String addtime = jsonElement.getAsJsonObject().get("addtime").getAsString();
            String uptime = jsonElement.getAsJsonObject().get("uptime").getAsString();
            boolean show = jsonElement.getAsJsonObject().get("show").getAsBoolean();
            boolean deepGet = true;//jsonElement.getAsJsonObject().get("deepGet").getAsBoolean();

            if(deepGet) {
                assetObj.addDomain(domain);
            }
        }

        for (String domain : assetObj.getDomain()){
            JsonObject seoMap = getSeo(domain);
            String ipcType = seoMap.getAsJsonObject("备案信息").get("备案性质").getAsString();

            if(!ipcType.equals("个人")){
                String companyName = seoMap.getAsJsonObject("备案信息").get("备案所属").getAsString();
                if(companyName.isEmpty()||companyName.equals("-")) companyName = seoMap.getAsJsonObject("域名信息").get("注册人/机构").getAsString();

                JsonArray companyList = GetCompany.getCompany_chinaz(companyName);
                if(companyList.size()>0){
                    JsonObject companyDetailsMap = GetCompany.getCompanyDetails_chinaz(companyList.get(0).getAsJsonObject().get("企业ID").getAsString());
                }

                JsonArray companyInfoMap = AiqichaSearch.getCompanyInfoIteration(companyName);
            }
        }




    }

    // 处理域名的逻辑
    private static void handleDomainName(String domain, AssetObj assetObj) {
        System.out.println("输入了一个域名: " + domain);
        JsonObject seoMap = getSeo(domain);
        System.out.println("基础域名信息");
        System.out.println(seoMap);

        String ipcType = seoMap.getAsJsonObject("备案信息").get("备案性质").getAsString();
        if(!ipcType.equals("个人")){
            String companyName = seoMap.getAsJsonObject("备案信息").get("备案所属").getAsString();
            if(companyName.isEmpty()||companyName.equals("-")) companyName = seoMap.getAsJsonObject("域名信息").get("注册人/机构").getAsString();

            JsonArray companyList = GetCompany.getCompany_chinaz(companyName);
            System.out.println("公司列表");
            System.out.println(companyList);
            if(companyList.size()>0){
                JsonObject companyDetailsMap = GetCompany.getCompanyDetails_chinaz(companyList.get(0).getAsJsonObject().get("企业ID").getAsString());
                System.out.println("公司信息详情_1");
                System.out.println(companyDetailsMap);

                JsonArray icpInfo = companyDetailsMap.getAsJsonArray("网站备案");

                for(JsonElement jsonElement : icpInfo) {
                    String icpNo = jsonElement.getAsJsonObject().get("备案号").getAsString();
                    String domainStr = jsonElement.getAsJsonObject().get("网站域名").getAsString();
                    assetObj.addIcp(icpNo);
                    assetObj.addDomain(domainStr);
                }
            }

            JsonArray companyInfoMap = AiqichaSearch.getCompanyInfoIteration(companyName);
            System.out.println("公司信息详情_2");
            System.out.println(companyInfoMap);
            for(JsonElement jsonElement : companyInfoMap) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String subCompanyName = jsonObject.entrySet().iterator().next().getKey();
                JsonArray domainAndIcp = jsonObject.getAsJsonObject(subCompanyName).getAsJsonArray("domainAndIcp");
                for(JsonElement subJsonElement :domainAndIcp){
                    String icpNo = subJsonElement.getAsJsonObject().get("icpNo").getAsString();
                    String domainStr = jsonElement.getAsJsonObject().get("domain").getAsString();
                    assetObj.addIcp(icpNo);
                    assetObj.addDomain(domainStr);
                }
            }


            for(String domainStr : assetObj.getDomain()) {
                if (assetObj.isGetSubdomain()) {
                    assetObj.setDomain(GetSubDomain.getSubByDomainOrDomainCert(domainStr));
                    if (assetObj.isBruteForceSubdomain()) {
                        assetObj.setDomain(SubdomainBruteForcer.getSubDomain(domainStr));
                    }
                }
            }

            // TODO 展示备案号、手动传入、修改备案号
            if(Fofa_Key.isEmpty() && Quake_Key.isEmpty() && Shodan_Key.isEmpty() && Zoomeye_Key.isEmpty() && Hunter_Key.size()==0) {
                for(String domainStr : assetObj.getDomain()) {
                    JsonObject briefExtendedInfo = fofaSearch.getBriefExtendedInfo_Fofa(domainStr);
                    // TODO 处理格式
                }
            }else {

                for (String domainStr : assetObj.getDomain()){
                    JsonArray domainInfo_Fofa = fofaSearch.getInfoByCompanyOrDomain_Fofa(domainStr);
                    JsonArray info_Hunter = hunterSearch.getInfoByCompanyOrDomain_Hunter(domainStr);
                    JsonArray info_Quake = quakeSearch.getInfoByCompanyOrDomain_Quake(domainStr);
                    JsonArray info_Shodan = shodanSearch.getInfoByDomain_shodan(domainStr);
                    JsonArray info_Zoomeye = zoomeyeSearch.getInfoByCompanyOrDomain_zoomeye(domainStr);
                    // TODO 处理格式   获取最新icp 排除遗漏的icp  并加入
                }


                for (String icpNo : assetObj.getIcp()) {
                    JsonArray icpInfo_Fofa = fofaSearch.getInfoByIcpNo_Fofa(icpNo);
                    JsonArray icpInfo_Hunter = hunterSearch.getInfoByIcpNo_Hunter(icpNo);
                    JsonArray icpInfo_Quake = quakeSearch.getInfoByIcpNo_Quake(icpNo);
                    // TODO 处理格式
                }
            }

            // 开始检索影子资产
            if(assetObj.isSearchShadowAssets()){
                Set<String> companyNameSet = AiUtils.getCompanyName_Ai(companyName);
                // TODO 用户修改公司名列表
                
                for(String companyNameStr : companyNameSet){
                    JsonArray info_Fofa = fofaSearch.getInfoByBodyFilterIcp_Fofa(companyNameStr, domain);
                    JsonArray info_Hunter = hunterSearch.getInfoByBodyFilterIcp_Hunter(companyNameStr, domain);
                    JsonArray info_Quake = quakeSearch.getInfoByBodyFilteIcp_Quake(companyNameStr, domain);
                    JsonArray info_Shodan = shodanSearch.getInfoByBodyFilterIcp_shodan(companyNameStr, domain);
                    JsonArray info_Zoomeye = zoomeyeSearch.getInfoByBodyFilterIcp_zoomeye(companyNameStr, domain);
                    // TODO 处理格式
                    // TODO title、body相关性过滤
                    // icon 图标相关性
                    // 手动选择icon、剔除不相关
                    // 合并去重
                    // C端口聚合
                }
            }



        }
    }

    // 处理公司名称的逻辑
    private static void handleCompanyName(String companyName, AssetObj assetObj) {
        System.out.println("输入了一个公司名称: " + companyName);
        JsonArray companyList = GetCompany.getCompany_chinaz(companyName);
        System.out.println("公司列表");
        System.out.println(companyList);
        // TODO 用户选择公司名
        JsonArray companyListChoose = companyList;
        for(JsonElement jsonElement : companyListChoose) {
            boolean deepGet = true;//jsonElement.getAsJsonObject().get("deepGet").getAsBoolean();
            if(deepGet) {
                String companyId= jsonElement.getAsJsonObject().get("企业ID").getAsString();
                String companyNameChoose = jsonElement.getAsJsonObject().get("企业名称").getAsString();
                JsonObject companyDetailsMap = GetCompany.getCompanyDetails_chinaz(companyId);
                System.out.println("公司信息详情_1");
                System.out.println(companyDetailsMap);


                JsonArray companyInfoMap = AiqichaSearch.getCompanyInfoIteration(companyNameChoose);
                System.out.println("公司信息详情_2");
                System.out.println(companyInfoMap);

            }
        }


    }






    public static void main(String[] args) {
        // 示例输入
//        searchInfo("124.232.185.44");
//        searchInfo("82.157.56.206");
//        searchInfo("183.3.221.110");
//        searchInfo("www.ceic.com");
//        searchInfo("ceic.com");
//        searchInfo("www.potato.gold");
        searchInfo("www.shenhuagroup.com.cn");
//        searchInfo("www.shenhuagroup.com.cn");
//        searchInfo("www.pinyin.cn");
//        searchInfo("北京搜狗信息服务有限公司");
//        searchInfo("国家能源投资集团有限责任公司");
//        System.out.println(InternetDomainName.from("yjglj.chuzhou.gov.cn").publicSuffix().toString());
//        System.out.println(InternetDomainName.from("yjglj.chuzhou.gov.cn").topPrivateDomain().toString());
//        System.out.println(getIpListBydomain("www.szciic.com"));
    }

}

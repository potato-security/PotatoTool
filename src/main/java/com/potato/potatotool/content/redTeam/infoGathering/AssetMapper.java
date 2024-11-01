package com.potato.potatotool.content.redTeam.infoGathering;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetObj;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.DomainInfo;
import com.potato.potatotool.content.redTeam.infoGathering.subDomain.GetSubDomain;
import com.potato.potatotool.content.redTeam.infoGathering.subDomain.SubdomainBruteForcer;
import com.potato.potatotool.content.redTeam.infoGathering.tools.*;
import com.potato.potatotool.content.redTeam.infoGathering.utils.AiUtils;
import com.potato.potatotool.content.redTeam.infoGathering.tools.GetCompany;
import com.potato.potatotool.content.redTeam.infoGathering.utils.DomainInfoMerger;
import com.potato.potatotool.content.redTeam.infoGathering.utils.Utils;

import java.util.List;
import java.util.Set;

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
                    // JsonObject briefExtendedInfo=
                    // {"ip":"82.157.56.206","domain":"","ports":[{"port":21,"base_protocol":"tcp","protocol":"ftp"}
                }
            }else {

                for (String domainStr : assetObj.getDomain()){
                    JsonArray info_Fofa = fofaSearch.getInfoByCompanyOrDomain_Fofa(domainStr);
                    JsonArray info_Hunter = hunterSearch.getInfoByCompanyOrDomain_Hunter(domainStr);
                    JsonArray info_Quake = quakeSearch.getInfoByCompanyOrDomain_Quake(domainStr);
                    JsonArray info_Shodan = shodanSearch.getInfoByDomain_Shodan(domainStr);
                    JsonArray info_Zoomeye = zoomeyeSearch.getInfoByCompanyOrDomain_Zoomeye(domainStr);
                    // TODO 处理格式   获取最新icp 排除遗漏的icp  并加入
                    // 不存在则都=[]
//                     JsonArray domainInfo_Fofa=
//                     [{"ip":"82.157.56.206","domain":"potato.gold","host":"www.potato.gold:3000","icp":"","port":"3000","protocol":"http","title":"聊天室","certs_subject_org":""},...]
//                     JsonArray info_Hunter=
//                     [{"is_risk":"","url":"http://www.potato.gold:16666","ip":"82.157.56.206","port":16666,"web_title":"","domain":"www.potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"Flask","version":"1.0.1"},{"name":"Python","version":"3.8.6"}],"os":"","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-29","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.0 200 OK\r\nContent-Type: application/json\r\nContent-Length: 32\r\nServer: Werkzeug/1.0.1 Python/3.8.6\r\nDate: Tue, 29 Oct 2024 03:22:50 GMT\r\n\r\n{\\\"code\\\":0,\\\"msg\\\":\\\"Unauthorized\\\"}\n","vul_list":"","header":"HTTP/1.0 200 OK\nContent-Type: application/json\nContent-Length: 32\nServer: Werkzeug/1.0.1 Python/3.8.6\nDate: Tue, 29 Oct 2024 03:26:49 GMT\n"},...]
//                     JsonArray info_Quake=
//                     [{"components":[{"product_level":"中间支撑层","product_type":["Web开发框架(Web framework)"],"product_vendor":"twitter","product_name_cn":"Bootstrap","product_name_en":"Bootstrap","id":"5e847718e3a6c618fd5e8974","product_catalog":["开发与运维"],"version":""},{"product_level":"应用业务层","product_type":["Web服务器"],"product_vendor":"Microsoft微软公司","product_name_cn":"Microsoft IIS Web服务器","product_name_en":"Microsoft IIS Web Service","id":"5e832b1e421aa99e98861685","product_catalog":["Web系统与应用"],"version":"8.5"},{"product_level":"中间支撑层","product_type":["编程语言","Web开发框架(Web framework)"],"product_vendor":"PHP.net","product_name_cn":"PHP","product_name_en":"PHP","id":"5eaa505a7d719d79733cb67f","product_catalog":["开发与运维"],"version":"5.4.45"},{"product_level":"中间支撑层","product_type":["Web开发框架(Web framework)"],"product_vendor":"The jQuery Foundation.","product_name_cn":"jQuery","product_name_en":"jQuery","id":"5e847718e3a6c618fd5e8973","product_catalog":["开发与运维"],"version":""},{"product_level":"中间支撑层","product_type":["编程语言"],"product_vendor":"Microsoft微软公司","product_name_cn":"ASP.NET","product_name_en":"ASP.NET","id":"60989f905c24f9e51bebaf4f","product_catalog":["开发与运维"],"version":""}],"images":[{"data":"","width":716,"height":537,"s3_url":""}],"org":"Tencent","ip":"82.*.*.*","is_ipv6":false,"transport":"tcp","hostname":"","port":443,"service":{"response":"HTTP/1.1 200 OK\r\nContent-Length: 11971\r\nAccess-Control-Allow-Origin: *\r\nCache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0\r\nContent-Encoding: gzip\r\nContent-Type: text/html; charset=utf-8\r\nDate: Mon, 28 Oct 2024 12:42:31 GMT\r\nExpires: Thu, 19 Nov 1981 08:52:00 GMT\r\nPragma: no-cache\r\nServer: Microsoft-IIS/8.5\r\nVary: Accept-Encoding\r\nX-Powered-By: PHP/5.4.45, ASP.NET\r\n\r\n","response_hash":"af4fc97276ca46f872a3eaf285d10113","dns":"暂无权限","name":"http/ssl","http":{"x_powered_by":"PHP/5.4.45, ASP.NET","dom_tree":"暂无权限","header_order_hash":"暂无权限","server":"Microsoft-IIS/8.5","status_code":"暂无权限","api_list":"暂无权限","http_load_url":"暂无权限","link":"暂无权限","http_load_count":"暂无权限","body":"暂无权限","meta_keywords":"暂无权限","title":"Potato's Blog","path":"/","icp":"暂无权限","host":"potato.gold","cookie_element":"暂无权限","favicon":"暂无权限","data_sources":"暂无权限","page_type_keyword":"暂无权限","html_hash":"暂无权限","response_headers":"暂无权限","information":"暂无权限"},"cert":"暂无权限","tls":"暂无权限","version":""},"domain":"potato.gold","location":{"province_cn":"北京市","isp":"腾讯","province_en":"Beijing","country_en":"China","district_cn":"","gps":[116.401159,39.902798],"street_cn":"","city_en":"Beijing City","district_en":"","country_cn":"中国","street_en":"","city_cn":"北京市","country_code":"CN","asname":"CNNIC-TENCENT-NET-AP","scene_cn":"数据中心","scene_en":"Hosting","radius":105.2321},"time":"2024-10-28T12:42:43.554Z","asn":45090,"id":"potato.gold_443_tcp"},...]
//                     JsonArray info_Shodan=
//                     [{"product":"nginx","hash":-1609083510,"ip":1616761883,"org":"Comcast Business","isp":"Comcast Business","transport":"tcp","cpe":["cpe:/a:igor_sysoev:nginx"],"data":"HTTP/1.1 400 Bad Request\r\nServer: nginx\r\nDate: Mon, 25 Jan 2021 21:33:48 GMT\r\nContent-Type: text/html\r\nContent-Length: 650\r\nConnection: close\r\n\r\n","asn":"AS7922","port":443,"hostnames":["three.webapplify.net"],"location":{"city":"Denver","region_code":"CO","area_code":null,"longitude":-104.9078,"country_code3":null,"latitude":39.7301,"postal_code":null,"dma_code":751,"country_code":"US","country_name":"United States"},"timestamp":"2021-01-25T21:33:49.154513","domains":["webapplify.net"],"http":{"robots_hash":null,"redirects":[],"securitytxt":null,"title":"400 The plain HTTP request was sent to HTTPS port","sitemap_hash":null,"robots":null,"server":"nginx","host":"96.93.212.27","html":"\r\n400 The plain HTTP request was sent to HTTPS port\r\n\r\n400 Bad Request\r\nThe plain HTTP request was sent to HTTPS port\r\nnginx\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n","location":"/","components":{},"securitytxt_hash":null,"sitemap":null,"html_hash":199333125},"os":null,"_shodan":{"crawler":"c9b639b99e5410a46f656e1508a68f1e6e5d6f99","ptr":true,"id":"534cc127-e734-44bc-be88-2e219a56a099","module":"auto","options":{}},"ip_str":"96.93.212.27"},{"product":"nginx","hostnames":["kolobok.us"],"hash":1940048442,"ip":3104568883,"org":"RuWeb","isp":"RuWeb","transport":"tcp","cpe":["cpe:/a:igor_sysoev:nginx:1.4.2"],"data":"HTTP/1.1 410 Gone\r\nServer: nginx/1.4.2\r\nDate: Mon, 25 Jan 2021 21:33:50 GMT\r\nContent-Type: text/html; charset=iso-8859-1\r\nContent-Length: 295\r\nConnection: keep-alive\r\n\r\n","asn":"AS49189","port":80,"version":"1.4.2","location":{"city":null,"region_code":null,"area_code":null,"longitude":37.6068,"country_code3":null,"latitude":55.7386,"postal_code":null,"dma_code":null,"country_code":"RU","country_name":"Russia"},"timestamp":"2021-01-25T21:33:51.172037","domains":["kolobok.us"],"http":{"robots_hash":null,"redirects":[],"securitytxt":null,"title":"410 Gone","sitemap_hash":null,"robots":null,"server":"nginx/1.4.2","host":"185.11.246.51","html":"\n\n410 Gone\n\nGone\nThe requested resource/\nis no longer available on this server and there is no forwarding address.\nPlease remove all references to this resource.\n\n","location":"/","components":{},"securitytxt_hash":null,"sitemap":null,"html_hash":922034037},"os":null,"_shodan":{"crawler":"c9b639b99e5410a46f656e1508a68f1e6e5d6f99","ptr":true,"id":"118b7360-01d0-4edb-8ee9-01e411c23e60","module":"auto","options":{}},"ip_str":"185.11.246.51"},...]
//                    JsonArray info_Zoomeye=
//                     [{"jarm":"","ico":{"mmh3":"37578595","md5":"03439ca7d20660b7af45c10fe90ed793"},"txtfile":{"robotsmd5":"","securitymd5":""},"ip":"82.157.56.206","portinfo":{"hostname":"","os":"Windows","port":443,"service":"https","title":["Potato's Blog"],"version":"8.5","device":"web application","extrainfo":"ASP.NET","rdns":"","app":"Microsoft IIS httpd","banner":"HTTP/1.1 200 OK\r\nCache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0\r\nPragma: no-cache\r\nContent-Type: text/html; charset=utf-8\r\nExpires: Thu, 19 Nov 1981 08:52:00 GMT\r\nServer: Microsoft-IIS/8.5\r\nX-Powered-By: PHP/5.4.45\r\nSet-Cookie: ZDEDebuggerPresent=php,phtml,php3; path=/\r\nSet-Cookie: PHPSESSID=dlpgcgnkhd39k2edjeatlbfs75; path=/\r\nX-Powered-By: ASP.NET\r\nAccess-Control-Allow-Origin: *\r\nDate: Wed, 19 Oct 2022 00:40:15 GMT\r\nConnection: close\r\nContent-Length: 42530\r\n\r\n<!DOCTYPE html>\r\n<html lang=\"zh-cn\">\r\n<head>\r\n    <meta charset=\"UTF-8\">\r\n    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\r\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\r\n    <title>Potato's Blog</title>\r\n    <meta name=\"keywords\" content=\"Potato\">\r\n    <meta name=\"description\" content=\"Potato's Blog\">\r\n    <link rel=\"icon\" href=\"https://potato.gold/data/uploads/20220303/00254385a778b1aca058a792e1aa9d8d.ico\">\r\n    <link rel=\"stylesheet\" href=\"https://potato.gold/public/common/css/bootstrap.min.css\">\r\n    <link rel=\"stylesheet\" href=\"https://potato.gold/public/cBlog-seceleI/css/cBlog-seceleI.css\">\r\n    <script src=\"https://potato.gold/public/common/js/jquery.min.js\"></script>\r\n    <script src=\"https://potato.gold/public/common/js/bootstrap.min.js\"></script>\r\n<!--    <script src=\"https://potato.gold/public/cBlog-seceleI/js/rightInfo.js\"></script>&lt;!&ndash;记得class=\"rightInfo\"&ndash;&gt;-->\r\n    <style>\r\n        /*body{--bgpic: url(https://82.157.56.206/data/uploads/20220209/592693b77337f8ae97bdb044efd39fa4.png);}*/\r\n        body{--bgpic: url(https://api.2heng.xin/cover/);}\r\n    </style>\r\n</head>\r\n<body>\r\n\r\n<script src=\"https://potato.gold/public/cBlog-seceleI/js/mouse.js\"></script><!--鼠标特效,背景漂浮需要放在body加载后-->\r\n<link rel=\"stylesheet\" type=\"text/css\" href=\"https://potato.gold/public/cBlog-seceleI/css/GalMenu.css\">\r\n<script type=\"text/javascript\" src=\"https://potato.gold/public/cBlog-seceleI/js/jquery.min.js\"></script>\r\n<script type=\"text/javascript\" src=\"https://potato.gold/public/cBlog-seceleI/js/GalMenu.js\"></script>\r\n<script type=\"text/javascript\">\r\n    $(document).ready(function() {\r\n        $(\"body\").galMenu({\r\n            menu:\"galRing\",\r\n            click_to_close:true,\r\n            stay_open:false\r\n        });\r\n    });\r\n</script>\r\n<div class=\"galMenu galRing\">\r\n    <div class=\"circle\" id=\"gal\">\r\n        <div class=\"ring\">\r\n            <a href=\"#\" title=\"Home\" class=\"menuItem\"></a>\r\n            <a href=\"#\" target=\"_blank\" title=\"Blog\" class=\"menuItem\"></a>\r\n            <a href=\"#\" title=\"About\" class=\"menuItem\"></a>\r\n            <a href=\"#\" title=\"Contact\" class=\"menuItem\"></a>\r\n            <a href=\"#\" title=\"Social\" class=\"menuItem\"></a>\r\n            <a href=\"#\" title=\"Other\" class=\"menuItem\"></a>\r\n        </div>\r\n        <audio id=\"galAudio\" src=\"https://potato.gold/public/cBlog-seceleI/audio/messUp_cough.m4a\"></audio>\r\n    </div>\r\n</div>\r\n<div id=\"overlay\" style=\"opacity: 0.4; cursor: pointer;z-index: 1\"></div>\r\n\r\n<div class=\"bg\"></div>\r\n<div class=\"bg-fixed\" style=\"background: url(https://potato.gold/public/cBlog-seceleI/images/grid.png) repeat;\"></div><!--网格处理-->\r\n<div class=\"body-overlay\"></div>\r\n<nav class=\"navbar navbar-default navbar-fixed-top\" role=\"navigation\">\r\n    <div class=\"container\">\r\n        <div class=\"navbar-header\">\r\n            <button type=\"button\" class=\"navbar-toggle collapsed\" data-toggle=\"collapse\" data-target=\"#bs-example-navbar-collapse-1\">\r\n                <span class=\"icon-bar\"></span>\r\n                <span class=\"icon-bar\"></span>\r\n                <span class=\"icon-bar\"></span>\r\n            </button>\r\n            <a class=\"navbar-brand\" href=\"/index.html\">\r\n                <img src=\"https://potato.gold/data/uploads/20220303/bfde3405774c177cc1d449d4c7fd0fe8.png\" height=\"20\">\r\n            </a>\r\n            <a class=\"navbar-brand\" href=\"/index.html\">Potato's Blog</a>\r\n        </div>\r\n        <div class=\"collapse navbar-collapse\" id=\"bs-example-navbar-collapse-1\">\r\n            <ul class=\"nav navbar-nav\" id=\"daohangcaidan\">\r\n                <li><a href=\"https://potato."},"ssl":"SSL Certificate\nVersion: TLS 1.2\nCipherSuit: TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA\nHandshake Message: x509: cannot validate certificate for 82.157.56.206 because it doesn't contain any IP SANs\nCertificate:\n    Data:\n        Version: 3 (0x2)\n        Serial Number: 2934091270199499416730016997173303562 (0x23515de539a1ceb1eaaab0ddfe3190a)\n    Signature Algorithm: SHA256-RSA\n        Issuer: C=CN,CN=TrustAsia TLS RSA CA,O=TrustAsia Technologies, Inc.,OU=Domain Validated SSL\n        Validity\n            Not Before: Dec 07 00:00:12 2021 UTC\n            Not After : Dec 06 23:59:11 2022 UTC\n        Subject: CN=potato.gold\n        Subject Public Key Info:\n            Public Key Algorithm: RSA\n                Public-Key: (2048 bit)\n                Modulus:   \n                    b9:d8:90:01:69:4d:85:a4:ab:e3:e0:7d:d7:7b:5b:\n                    40:48:d0:c4:3e:3a:90:71:21:b6:7a:6c:ad:26:4d:\n                    92:1f:6d:f9:97:d4:66:b8:84:26:ec:f0:5b:91:c8:\n                    aa:8d:b6:bf:20:b5:ea:70:17:0e:0a:4b:b4:29:b3:\n                    f6:6d:ce:df:04:fc:49:0e:6c:3f:8d:00:fa:3b:7b:\n                    a1:90:92:bd:3a:08:d3:e8:1f:fa:e4:e1:3c:aa:d4:\n                    df:2a:62:1f:77:c0:16:64:ef:ac:b4:c2:f6:e8:2c:\n                    00:3a:b8:77:0f:b6:02:dc:a9:e1:48:e5:eb:81:14:\n                    89:0d:fa:87:69:d8:a1:16:fd:53:12:fd:06:5d:d5:\n                    81:48:48:91:3a:5a:77:e6:38:83:52:c1:26:ed:e1:\n                    b1:f2:3a:1b:32:26:fb:5b:50:16:dc:55:04:a7:da:\n                    21:12:f3:12:51:96:5a:ce:d8:5d:ae:78:15:c7:59:\n                    c6:8d:22:9f:d3:b4:6a:bb:31:80:ff:0a:63:cd:f3:\n                    8d:a0:20:33:5f:25:30:1f:c7:e9:87:ee:a3:35:4f:\n                    5d:50:ba:e2:a1:ae:f1:ad:dc:83:13:d8:db:75:dc:\n                    4d:5e:c1:90:7b:ad:67:b3:a5:80:e8:ff:db:42:0f:\n                    d3:f4:26:64:d8:3b:24:7d:0b:47:0f:78:f1:68:5a:\n                    b7\n                Exponent: 65537 (0x10001)\n        X509v3 extensions:\n            X509v3 Subject Alternative Name:\n                DNS:potato.gold, DNS:www.potato.gold\n            Authority Information Access:\n                OCSP - URI:http://statuse.digitalcertvalidation.com\n                CA Issuers - URI:http://cacerts.digitalcertvalidation.com/TrustAsiaTLSRSACA.crt\n\n            X509v3 Authority Key Identifier:\n                keyid:7F:D3:99:F3:A0:47:0E:31:00:56:56:22:8E:B7:CC:9E:DD:CA:01:8A\n            X509v3 Basic Constraints:\n                CA:False\n            X509v3 Key Usage: critical\n                Digital Signature, Key Encipherment\n            Unknown extension 1.3.6.1.4.1.11129.2.4.2\n            X509v3 Extended Key Usage:\n                TLS Web Server Authentication, TLS Web Client Authentication\n            X509v3 Certificate Policies:\n                Policy: 2.23.140.1.2.1\n            X509v3 Subject Key Identifier:\n                64:D1:79:20:D3:A8:32:2E:89:40:21:EE:81:2D:6C:D4:2B:99:CA:DB\n\n    Signature Algorithm: SHA256-RSA\n         0e:f1:6e:b5:f4:69:a7:97:c7:09:e6:ad:45:b4:8b:f2:68:d0:\n         4e:4b:27:de:39:f1:ae:40:69:0b:be:eb:a0:13:dd:d8:22:ff:\n         08:aa:a7:a4:1c:57:ac:3c:13:21:26:5c:a1:1d:d5:19:bc:9e:\n         b3:2f:42:08:de:14:4f:2b:a6:a4:fa:bb:02:0f:d2:3e:69:da:\n         2c:b1:44:61:de:96:16:0c:25:fc:90:d6:0b:20:16:14:b3:69:\n         44:ae:ed:65:0d:96:af:8c:f9:30:19:56:ed:d6:dc:af:16:6b:\n         7f:0c:c9:ef:90:23:c6:32:f2:11:b3:58:7f:61:f7:47:9d:1c:\n         f1:56:68:4f:0a:1e:d5:89:bd:25:ad:e8:05:45:50:6d:eb:a3:\n         c9:3a:0e:3e:17:ec:55:a7:74:0b:f2:10:e7:a2:0a:0b:11:3a:\n         f4:6c:57:58:b1:6a:b3:8a:ba:42:99:ab:77:6e:20:03:7c:97:\n         43:86:16:d4:d7:01:f5:e8:3d:b9:72:31:2b:33:7d:b6:f2:cf:\n         6e:78:35:36:f7:31:31:ac:e3:bf:39:f9:fb:9a:1c:78:e7:5d:\n         5d:55:6f:aa:3c:b8:f0:91:54:5d:29:8c:14:1b:51:2f:16:2d:\n         90:51:ff:b8:89:89:9b:d3:79:d6:3d:4c:bc:75:0c:5b:a0:f4:\n         4d:eb:c7:5d","timestamp":"2022-10-19T08:40:45","geoinfo":{"continent":{"code":"AP","names":{"en":"Asia","zh-CN":"亚洲"},"geoname_id":null},"owner":"","country":{"code":"CN","names":{"en":"China","zh-CN":"中国"},"geoname_id":null},"base_station":"","city":{"names":{"en":"Beijing","zh-CN":"北京"},"geoname_id":null},"timezone":"UTC+8","idc":"IDC","scene":{"en":"Hosting","cn":"数据中心"},"zipcode":"100005","district":{"names":{"en":"","zh-CN":null},"geoname_id":null},"organization":"Shenzhen Tencent Computer Systems Company Limited","location":{"lon":"116.401159","lat":"39.902798"},"aso":"","asn":"45090","subdivisions":{"names":{"en":"Beijing","zh-CN":"北京"},"geoname_id":null},"PoweredBy":"埃文","organization_CN":null},"protocol":{"application":"HTTP","probe":"GetRequestHost","transport":"tcp"},"honeypot":0,"whois":{}},...]
                }


                for (String icpNo : assetObj.getIcp()) {
                    JsonArray icpInfo_Fofa = fofaSearch.getInfoByIcpNo_Fofa(icpNo);
                    JsonArray icpInfo_Hunter = hunterSearch.getInfoByIcpNo_Hunter(icpNo);
                    JsonArray icpInfo_Quake = quakeSearch.getInfoByIcpNo_Quake(icpNo);
                    // TODO 处理格式
                    // JsonArray icpInfo_Fofa=
                    // []
                    // JsonArray icpInfo_Hunter=
                    // [{"is_risk":"","url":"http://www.potato.gold:16666","ip":"82.157.56.206","port":16666,"web_title":"","domain":"www.potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"Flask","version":"1.0.1"},{"name":"Python","version":"3.8.6"}],"os":"","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-29","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.0 200 OK\r\nContent-Type: application/json\r\nContent-Length: 32\r\nServer: Werkzeug/1.0.1 Python/3.8.6\r\nDate: Tue, 29 Oct 2024 03:22:50 GMT\r\n\r\n{\\\"code\\\":0,\\\"msg\\\":\\\"Unauthorized\\\"}\n","vul_list":"","header":"HTTP/1.0 200 OK\nContent-Type: application/json\nContent-Length: 32\nServer: Werkzeug/1.0.1 Python/3.8.6\nDate: Tue, 29 Oct 2024 03:26:49 GMT\n"},{"is_risk":"","url":"http://potato.gold:16666","ip":"82.157.56.206","port":16666,"web_title":"","domain":"potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"Flask","version":"1.0.1"},{"name":"Python","version":"3.8.6"}],"os":"","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-29","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.0 200 OK\r\nContent-Type: application/json\r\nContent-Length: 32\r\nServer: Werkzeug/1.0.1 Python/3.8.6\r\nDate: Tue, 29 Oct 2024 03:22:50 GMT\r\n\r\n{\\\"code\\\":0,\\\"msg\\\":\\\"Unauthorized\\\"}\n","vul_list":"","header":"HTTP/1.0 200 OK\nContent-Type: application/json\nContent-Length: 32\nServer: Werkzeug/1.0.1 Python/3.8.6\nDate: Tue, 29 Oct 2024 03:22:57 GMT\n"},{"is_risk":"","url":"http://www.potato.gold:3000","ip":"82.157.56.206","port":3000,"web_title":"hack.chat","domain":"www.potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"Azure CDN","version":""}],"os":"","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-28","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n","vul_list":"","header":"HTTP/1.1 200 OK\nServer: ecstatic-3.3.2\nContent-Type: text/html; charset=UTF-8\nLast-Modified: Mon, 18 Apr 2022 10:03:41 GMT\nDate: Sun, 27 Oct 2024 19:08:50 GMT\nEtag: W/\"6755399441295497-2903-2022-04-18T10:03:41.300Z\"\nCache-Control: max-age=3600\nConnection: keep-alive\nKeep-Alive: timeout=5\nContent-Length: 2903\n"},{"is_risk":"","url":"http://potato.gold:3000","ip":"82.157.56.206","port":3000,"web_title":"hack.chat","domain":"potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"Azure CDN","version":""}],"os":"","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-28","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n","vul_list":"","header":"HTTP/1.1 200 OK\nDate: Sun, 27 Oct 2024 19:08:45 GMT\nEtag: W/\"6755399441295497-2903-2022-04-18T10:03:41.300Z\"\nKeep-Alive: timeout=5\nServer: ecstatic-3.3.2\nConnection: keep-alive\nContent-Type: text/html; charset=UTF-8\nContent-Length: 2903\nCache-Control: max-age=3600\nLast-Modified: Mon, 18 Apr 2022 10:03:41 GMT\n"},{"is_risk":"","url":"http://potato.gold:16667","ip":"82.157.56.206","port":16667,"web_title":"","domain":"potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"Flask","version":"1.0.1"},{"name":"Python","version":"3.8.6"}],"os":"","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-27","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.0 200 OK\r\nContent-Type: application/json\r\nContent-Length: 32\r\nServer: Werkzeug/1.0.1 Python/3.8.6\r\nDate: Sun, 27 Oct 2024 14:46:20 GMT\r\n\r\n{\\\"code\\\":0,\\\"msg\\\":\\\"Unauthorized\\\"}\n","vul_list":"","header":"HTTP/1.0 200 OK\nContent-Type: application/json\nContent-Length: 32\nServer: Werkzeug/1.0.1 Python/3.8.6\nDate: Sun, 27 Oct 2024 14:46:28 GMT\n"},{"is_risk":"","url":"http://www.potato.gold:16667","ip":"82.157.56.206","port":16667,"web_title":"","domain":"www.potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"Flask","version":"1.0.1"},{"name":"Python","version":"3.8.6"}],"os":"","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-27","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.0 200 OK\r\nContent-Type: application/json\r\nContent-Length: 32\r\nServer: Werkzeug/1.0.1 Python/3.8.6\r\nDate: Sun, 27 Oct 2024 14:46:20 GMT\r\n\r\n{\\\"code\\\":0,\\\"msg\\\":\\\"Unauthorized\\\"}\n","vul_list":"","header":"HTTP/1.0 200 OK\nContent-Type: application/json\nContent-Length: 32\nServer: Werkzeug/1.0.1 Python/3.8.6\nDate: Sun, 27 Oct 2024 14:46:27 GMT\n"},{"is_risk":"","url":"http://potato.gold","ip":"82.157.56.206","port":80,"web_title":"Potato's Blog","domain":"potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"Microsoft ASP.NET","version":""},{"name":"IIS","version":"8.5"},{"name":"PHP","version":"5.4.45"},{"name":"jQuery","version":""},{"name":"Bootstrap","version":""},{"name":"Windows Server","version":""}],"os":"Windows","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-27","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.1 301 Moved Permanently\r\nContent-Type: text/html; charset=UTF-8\r\nLocation: https://82.157.56.206:80/\r\nServer: Microsoft-IIS/8.5\r\nX-Powered-By: ASP.NET\r\nAccess-Control-Allow-Origin: *\r\nDate: Sun, 27 Oct 2024 09:17:58 GMT\r\nContent-Length: 148\r\n\r\n<head><title>文档已移动</title></head>\n<body><h1>对象已移动</h1>可在<a HREF=\\\"https://82.157.56.206:80/\\\">此处</a>找到该文档</body>","vul_list":"","header":"HTTP/1.1 200 OK\nExpires: Thu, 19 Nov 1981 08:52:00 GMT\nContent-Type: text/html; charset=utf-8\nDate: Sun, 27 Oct 2024 09:18:18 GMT\nVary: Accept-Encoding\nCookie: ZDEDebuggerPresent=php,phtml,php3; path=/,think_var=en; expires=Sun, 27-Oct-2024 10:18:15 GMT; path=/,PHPSESSID=6aurj3cms6o46lqth4bbq0e724; path=/\nCache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0\nPragma: no-cache\nAccess-Control-Allow-Origin: *\nServer: Microsoft-IIS/8.5\nX-Powered-By: PHP/5.4.45,ASP.NET\n"},{"is_risk":"","url":"http://www.potato.gold","ip":"82.157.56.206","port":80,"web_title":"Potato's Blog","domain":"www.potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"PHP","version":"5.4.45"},{"name":"IIS","version":"8.5"},{"name":"Microsoft ASP.NET","version":""},{"name":"jQuery","version":""},{"name":"Windows Server","version":""},{"name":"Bootstrap","version":""}],"os":"Windows","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-27","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.1 301 Moved Permanently\r\nContent-Type: text/html; charset=UTF-8\r\nLocation: https://82.157.56.206:80/\r\nServer: Microsoft-IIS/8.5\r\nX-Powered-By: ASP.NET\r\nAccess-Control-Allow-Origin: *\r\nDate: Sun, 27 Oct 2024 09:17:58 GMT\r\nContent-Length: 148\r\n\r\n<head><title>文档已移动</title></head>\n<body><h1>对象已移动</h1>可在<a HREF=\\\"https://82.157.56.206:80/\\\">此处</a>找到该文档</body>","vul_list":"","header":"HTTP/1.1 200 OK\nAccess-Control-Allow-Origin: *\nDate: Sun, 27 Oct 2024 09:18:13 GMT\nExpires: Thu, 19 Nov 1981 08:52:00 GMT\nServer: Microsoft-IIS/8.5\nX-Powered-By: PHP/5.4.45,ASP.NET\nContent-Type: text/html; charset=utf-8\nVary: Accept-Encoding\nCache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0\nPragma: no-cache\nCookie: ZDEDebuggerPresent=php,phtml,php3; path=/,think_var=en; expires=Sun, 27-Oct-2024 10:18:09 GMT; path=/,PHPSESSID=d66d353244vfi3p4u7uv151uo0; path=/\n"},{"is_risk":"","url":"http://potato.gold:5000","ip":"82.157.56.206","port":5000,"web_title":"","domain":"potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"Flask","version":"1.0.1"},{"name":"Python","version":"3.8.6"}],"os":"","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-27","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.0 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: 38\r\nServer: Werkzeug/1.0.1 Python/3.8.6\r\nDate: Sun, 27 Oct 2024 02:16:45 GMT\r\n\r\n[Error]: Authorization does not exist!","vul_list":"","header":"HTTP/1.0 200 OK\nContent-Type: text/html; charset=utf-8\nContent-Length: 38\nServer: Werkzeug/1.0.1 Python/3.8.6\nDate: Sun, 27 Oct 2024 05:29:48 GMT\n"},{"is_risk":"","url":"http://www.potato.gold:5000","ip":"82.157.56.206","port":5000,"web_title":"","domain":"www.potato.gold","is_risk_protocol":"","protocol":"http","base_protocol":"tcp","status_code":200,"component":[{"name":"Flask","version":"1.0.1"},{"name":"Python","version":"3.8.6"}],"os":"","company":"李君颐","number":"京ICP备2022004104号-1","country":"中国","province":"北京市","city":"北京市","updated_at":"2024-10-27","is_web":"是","as_org":"Shenzhen Tencent Computer Systems Company Limited","isp":"腾讯","banner":"HTTP/1.0 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: 38\r\nServer: Werkzeug/1.0.1 Python/3.8.6\r\nDate: Sun, 27 Oct 2024 02:16:45 GMT\r\n\r\n[Error]: Authorization does not exist!","vul_list":"","header":"HTTP/1.0 200 OK\nDate: Sun, 27 Oct 2024 05:29:41 GMT\nContent-Type: text/html; charset=utf-8\nContent-Length: 38\nServer: Werkzeug/1.0.1 Python/3.8.6\n"}]
                    // JsonArray icpInfo_Quake=
                    // []
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
//        searchInfo("www.shenhuagroup.com.cn");
//        searchInfo("www.shenhuagroup.com.cn");
//        searchInfo("www.pinyin.cn");
//        searchInfo("北京搜狗信息服务有限公司");
//        searchInfo("国家能源投资集团有限责任公司");
//        System.out.println(InternetDomainName.from("yjglj.chuzhou.gov.cn").publicSuffix().toString());
//        System.out.println(InternetDomainName.from("yjglj.chuzhou.gov.cn").topPrivateDomain().toString());
//        System.out.println(getIpListBydomain("www.szciic.com"));
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

        String icpNo = "京ICP备2022004104号";
//        String icpNo = "京ICP备2022004189号";
        String domainStr = "potato.gold";
        JsonArray info_Fofa = fofaSearch.getInfoByCompanyOrDomain_Fofa(domainStr);
        JsonArray info_Hunter = hunterSearch.getInfoByCompanyOrDomain_Hunter(domainStr);
        JsonArray info_Quake = quakeSearch.getInfoByCompanyOrDomain_Quake(domainStr);
        JsonArray info_Shodan = shodanSearch.getInfoByDomain_Shodan(domainStr);
        JsonArray info_Zoomeye = zoomeyeSearch.getInfoByCompanyOrDomain_Zoomeye(domainStr);
        System.out.println("JsonArray info_Fofa=");
        System.out.println(info_Fofa);
        System.out.println(info_Fofa.size());
        System.out.println("JsonArray info_Hunter=");
        System.out.println(info_Hunter);
        System.out.println(info_Hunter.size());
        System.out.println("JsonArray info_Quake=");
        System.out.println(info_Quake);
        System.out.println(info_Quake.size());
        System.out.println("JsonArray info_Shodan=");
        System.out.println(info_Shodan);
        System.out.println(info_Shodan.size());
        System.out.println("JsonArray info_Zoomeye=");
        System.out.println(info_Zoomeye);
        System.out.println(info_Zoomeye.size());


        DomainInfoMerger merger = new DomainInfoMerger();
        List<DomainInfo> mergedList = merger.mergeDomainInfos(info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
        System.out.println(mergedList.size());

        for (DomainInfo domainInfo : mergedList) {
            System.out.println(domainInfo);
        }
    }

}

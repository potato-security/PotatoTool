package com.potato.potatotool.content.redTeam.infoGathering;

import com.google.gson.JsonArray;

import java.util.Map;
import java.util.regex.Pattern;

import static com.potato.potatotool.content.redTeam.infoGathering.GetSeo.*;
import static com.potato.potatotool.content.redTeam.infoGathering.GetDomain.*;

/**
 * @author Potato
 * @date 2023/4/23 10:05
 *
 * 空间测绘主类
 */
public class AssetMapper {
    // 正则表达式匹配IP地址
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // 正则表达式匹配域名
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?!-)([A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,63}$"
    );

    public static void searchInfo(String input) {
        if (isIPAddress(input)) {
            handleIPAddress(input);
        } else if (isDomainName(input)) {
            handleDomainName(input);
        } else {
            handleCompanyName(input);
        }
    }

    // 判断是否为IP地址
    public static boolean isIPAddress(String input) {
        return IP_PATTERN.matcher(input).matches();
    }

    // 判断是否为域名
    public static boolean isDomainName(String input) {
        return DOMAIN_PATTERN.matcher(input).matches();
    }

    // 处理IP地址的逻辑
    private static void handleIPAddress(String ip) {
        System.out.println("这是一个IP地址: " + ip);
        JsonArray domainList = getDomainByIp(ip);
        System.out.println("size");
        System.out.println(domainList.size());
        // 添加处理IP地址的逻辑
    }

    // 处理域名的逻辑
    private static void handleDomainName(String domain) {
        System.out.println("这是一个域名: " + domain);
        Map<String, Map<String, String>> seoMap = getSeo(domain);
        System.out.println(seoMap);
        // 添加处理域名的逻辑
    }

    // 处理公司名称的逻辑
    private static void handleCompanyName(String companyName) {
        System.out.println("这是一个公司名称: " + companyName);
        // 添加处理公司名称的逻辑
    }






    public static void main(String[] args) {
        // 示例输入
        searchInfo("124.232.185.44");
        searchInfo("82.157.56.206");
        searchInfo("www.ceic.com");
        searchInfo("ceic.com");
        searchInfo("www.potato.gold");
        searchInfo("www.shenhuagroup.com.cn");
        searchInfo("国家能源投资集团有限责任公司");
//        System.out.println(InternetDomainName.from("yjglj.chuzhou.gov.cn").publicSuffix().toString());
//        System.out.println(InternetDomainName.from("yjglj.chuzhou.gov.cn").topPrivateDomain().toString());
//        System.out.println(getIpListBydomain("www.szciic.com"));
    }

}

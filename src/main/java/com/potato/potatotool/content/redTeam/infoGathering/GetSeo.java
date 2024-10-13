package com.potato.potatotool.content.redTeam.infoGathering;

import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.Utils.*;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/9/23 23:41
 *
 * 域名、ip解析相关类
 */
public class GetSeo {



    public static Map<String, Map<String, String>> getSeo(String domain) {
        CompletableFuture<Map<String, Map<String, String>>> futureChinaz = CompletableFuture.supplyAsync(() -> getSeo_chinaz(domain));
        CompletableFuture<Map<String, Map<String, String>>> futureAizhan = CompletableFuture.supplyAsync(() -> getSeo_aizhan(domain));

        CompletableFuture<Map<String, Map<String, String>>> combinedFuture = futureChinaz.thenCombine(futureAizhan, GetSeo::mergeMaps);

        try {
            return combinedFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // 使用chinaz对Domain进行SEO综合查询
    private static Map<String, Map<String, String>> getSeo_chinaz(String domain) {
        Map<String, Map<String, String>> seoMap = new HashMap<>();

        try {
            RequestObj obj = new RequestObj().setUrl("https://seo.chinaz.com/" + domain)
                    .setMethod("GET").setRetries(3);
            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();

            // 检查请求状态码
            if (statusCode != 200) {
                return seoMap;
            }

            Document doc = con.getDocument();

            // 提取信息
            Map<String, String> domainInfo = extractDomainInfo_chinaz(doc);
            Map<String, String> icpInfo = extractIcpInfo_chinaz(doc);
            Map<String, String> websiteInfo = extractWebsiteInfo(doc);

            seoMap.put("域名信息", domainInfo);
            seoMap.put("备案信息", icpInfo);
            seoMap.put("网站信息", websiteInfo);

        } catch (Exception e) {
            if (debugMode) System.out.println(e);
        }

        return seoMap;
    }

    // 使用aizhan对Domain进行SEO综合查询
    private static Map<String, Map<String, String>> getSeo_aizhan(String domain) {
        Map<String, Map<String, String>> seoMap = new HashMap<>();

        try {
            RequestObj obj = new RequestObj().setUrl("https://www.aizhan.com/cha/" + domain + "/")
                    .setMethod("GET").setRetries(3);
            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();

            // 检查请求状态码
            if (statusCode != 200) {
                return seoMap;
            }

            Document doc = con.getDocument();

            // 提取信息
            Map<String, String> domainInfo = extractDomainInfo_aizhan(doc);
            Map<String, String> icpInfo = extractIcpInfo_aizhan(doc);
            Map<String, String> rankInfo = extractRankInfo_aizhan(doc);

            seoMap.put("域名信息", domainInfo);
            seoMap.put("备案信息", icpInfo);
            seoMap.put("权重信息", rankInfo);

        } catch (Exception e) {
            if (debugMode) System.out.println(e);
        }

        return seoMap;
    }


    // 融合map1和map2
    // map1 中的值不被 map2 的值覆盖，只有在 map1 中对应的 key 不存在或者值为空的情况下，才从 map2 中赋值
    private static Map<String, Map<String, String>> mergeMaps(Map<String, Map<String, String>> map1, Map<String, Map<String, String>> map2) {
        for (Map.Entry<String, Map<String, String>> entry : map2.entrySet()) {
            // 使用 merge 方法将 map2 的条目合并到 map1
            map1.merge(entry.getKey(), entry.getValue(), (subMap1, subMap2) -> {
                // 遍历 subMap2 中的每个键值对
                for (Map.Entry<String, String> innerEntry : subMap2.entrySet()) {
                    String innerKey = innerEntry.getKey();
                    String innerValue = innerEntry.getValue();

                    // 只有当 subMap1 中没有该 key 或者其值为空时，才赋值
                    if (!subMap1.containsKey(innerKey) || subMap1.get(innerKey) == null || subMap1.get(innerKey).isEmpty()) {
                        subMap1.put(innerKey, innerValue);
                    }
                }
                return subMap1; // 返回合并后的 subMap1
            });
        }
        return map1; // 返回最终的合并结果
    }

    private static Map<String, String> extractDomainInfo_chinaz(Document doc) {
        Map<String, String> domainInfo = new HashMap<>();

        domainInfo.put("注册人/机构", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(2) > div:nth-of-type(1) > span:nth-of-type(1) > i"));
        domainInfo.put("域名年龄", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(2) > div:nth-of-type(2) > span > a > i"));

        return domainInfo;
    }

    private static Map<String, String> extractDomainInfo_aizhan(Document doc) {
        Map<String, String> domainInfo = new HashMap<>();

        Elements liElements = doc.select("body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(5) > td:nth-of-type(2) > ul > li");

        for (Element li : liElements) {
            String liText = li.text().trim();

            if (liText.contains("注册人/机构")) {
                String org = li.selectFirst("a") != null ? li.selectFirst("a").text().trim() : "";
                if (org.startsWith("//whois.ename.net") || org.contains("（更新）")) org = "";
                domainInfo.put("注册人/机构", org);
            }

            if (liText.contains("年龄：")) {
                domainInfo.put("域名年龄", liText.replace("年龄：", "").trim());
            }

            if (liText.contains("邮箱") || liText.matches(".*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) {
                String emailHref = li.selectFirst("a") != null ? li.selectFirst("a").attr("href").trim() : "";
                domainInfo.put("注册邮箱", getEmail(emailHref));
            }
        }

        return domainInfo;
    }

    private static Map<String, String> extractRankInfo_aizhan(Document doc) {
        Map<String, String> rankInfo = new HashMap<>();

        rankInfo.put("百度权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(1) > a > img", "alt").replace("n", "0"));
        rankInfo.put("移动权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(2) > a > img", "alt").replace("n", "0"));
        rankInfo.put("360权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(3) > a > img", "alt").replace("n", "0"));
        rankInfo.put("神马权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(4) > a > img", "alt").replace("n", "0"));
        rankInfo.put("搜狗权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(5) > a > img", "alt").replace("n", "0"));
        rankInfo.put("谷歌权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(6) > a > img", "alt").replace("n", "0"));

        return rankInfo;
    }

    private static String getEmail(String emailUrl) {
        String email = "";
        if (emailUrl.isEmpty()) return email;

        try {
            RequestObj obj = new RequestObj().setUrl(emailUrl)
                    .setMethod("GET").setRetries(3);
            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();

            // 检查请求状态码
            if (statusCode != 200) {
                return email;
            }

            Document doc = con.getDocument();
            email = getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(1) > div:nth-of-type(2) > form > input:nth-of-type(1)", "value");

        } catch (Exception e) {
            if (debugMode) System.out.println(e);
        }

        return email;
    }

    private static Map<String, String> extractIcpInfo_chinaz(Document doc) {
        Map<String, String> icpInfo = new HashMap<>();

        icpInfo.put("备案号", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > span:nth-of-type(1) > i > a"));
        icpInfo.put("备案所属", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > span:nth-of-type(2) > i"));
        icpInfo.put("备案性质", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > span:nth-of-type(3) > i"));

        return icpInfo;
    }

    private static Map<String, String> extractIcpInfo_aizhan(Document doc) {
        Map<String, String> icpInfo = new HashMap<>();
        icpInfo.put("备案号", getElementText(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > ul > li:nth-of-type(1) > a"));
        icpInfo.put("备案所属", getElementText(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > ul > li:nth-of-type(3) > span"));
        icpInfo.put("备案性质", getElementText(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > ul > li:nth-of-type(2) > span"));

        return icpInfo;
    }

    private static Map<String, String> extractWebsiteInfo(Document doc) {
        Map<String, String> websiteInfo = new HashMap<>();

        // IP及所属地
        String elementText = getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(5) > td:nth-of-type(2) > div:nth-of-type(1) > span:nth-of-type(1) > i > a");
        parseIpInfo(elementText, websiteInfo);

        // 网站描述
        websiteInfo.put("网站描述", getElementText(doc, "body > div:nth-of-type(10) > div:nth-of-type(2) > div:nth-of-type(1) > div"));

        return websiteInfo;
    }

    private static void parseIpInfo(String elementText, Map<String, String> websiteInfo) {
        Pattern pattern = Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+)\\[(.*?)\\]$");
        Matcher matcher = pattern.matcher(elementText);

        if (matcher.find()) {
            String ip = matcher.group(1);
            String location = matcher.group(2);
            websiteInfo.put("IP", ip);
            websiteInfo.put("IP所属", location);
        } else {
            websiteInfo.put("IP", elementText);
        }
    }

}

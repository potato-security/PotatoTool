package com.potato.potatotool.content.redTeam.infoGathering;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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



    public static JsonObject getSeo(String domain) {
        CompletableFuture<JsonObject> futureChinaz = CompletableFuture.supplyAsync(() -> getSeo_chinaz(domain));
        CompletableFuture<JsonObject> futureAizhan = CompletableFuture.supplyAsync(() -> getSeo_aizhan(domain));

        CompletableFuture<JsonObject> combinedFuture = futureChinaz.thenCombine(futureAizhan, GetSeo::mergeJsonObjects);

        try {
            return combinedFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new JsonObject();
        }
    }

    // 使用chinaz对Domain进行SEO综合查询
    private static JsonObject getSeo_chinaz(String domain) {
        JsonObject seoMap = new JsonObject();

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
            JsonObject domainInfo = extractDomainInfo_chinaz(doc);
            JsonObject icpInfo = extractIcpInfo_chinaz(doc);
            JsonObject websiteInfo = extractWebsiteInfo(doc);

            seoMap.add("域名信息", domainInfo);
            seoMap.add("备案信息", icpInfo);
            seoMap.add("网站信息", websiteInfo);

        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }

        return seoMap;
    }

    // 使用aizhan对Domain进行SEO综合查询
    private static JsonObject getSeo_aizhan(String domain) {
        JsonObject seoMap = new JsonObject();

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
            JsonObject domainInfo = extractDomainInfo_aizhan(doc);
            JsonObject icpInfo = extractIcpInfo_aizhan(doc);
            JsonObject rankInfo = extractRankInfo_aizhan(doc);

            seoMap.add("域名信息", domainInfo);
            seoMap.add("备案信息", icpInfo);
            seoMap.add("权重信息", rankInfo);

        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }

        return seoMap;
    }


    // 深度融合jsonObject1和jsonObject2
    public static JsonObject mergeJsonObjects(JsonObject obj1, JsonObject obj2) {
        JsonObject merged = new JsonObject();
        for (String key : obj1.keySet()) {
            JsonElement value1 = obj1.get(key);
            if (obj2.has(key) && obj2.get(key)!=null && !obj2.get(key).isJsonNull()) {
                JsonElement value2 = obj2.get(key);
                JsonElement mergedValue = mergeElements(value1, value2);
                merged.add(key, mergedValue);
            } else {
                merged.add(key, value1);
            }
        }
        for (String key : obj2.keySet()) {
            if (!merged.has(key)) {
                merged.add(key, obj2.get(key));
            }
        }
        return merged;
    }

    private static JsonElement mergeElements(JsonElement elem1, JsonElement elem2) {
        if (elem1.isJsonObject() && elem2.isJsonObject()) {
            return mergeJsonObjects(elem1.getAsJsonObject(), elem2.getAsJsonObject());
        } else if (elem1.isJsonArray() && elem2.isJsonArray()) {
            return mergeJsonArrays(elem1.getAsJsonArray(), elem2.getAsJsonArray());
        } else {
            return elem2; // 如果不是 JsonObject 或 JsonArray，则返回 elem2
        }
    }

    private static JsonArray mergeJsonArrays(JsonArray array1, JsonArray array2) {
        JsonArray mergedArray = new JsonArray();
        for (JsonElement elem : array1) {
            mergedArray.add(elem);
        }
        for (JsonElement elem : array2) {
            if(!mergedArray.contains(elem)) mergedArray.add(elem);
        }
        return mergedArray;
    }

    private static JsonObject extractDomainInfo_chinaz(Document doc) {
        JsonObject domainInfo = new JsonObject();

        domainInfo.addProperty("域名", getElementAttr(doc, "body > div:nth-of-type(2) > div:nth-of-type(2) > div:nth-of-type(3) > div > input", "value").trim());
        domainInfo.addProperty("注册人/机构", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(2) > div:nth-of-type(1) > span:nth-of-type(1) > i"));
        domainInfo.addProperty("域名年龄", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(3) > td:nth-of-type(2) > div:nth-of-type(2) > span > a > i"));
        System.out.println(domainInfo);

        return domainInfo;
    }

    private static JsonObject extractDomainInfo_aizhan(Document doc) {
        JsonObject domainInfo = new JsonObject();

        domainInfo.addProperty("域名", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(1) > div:nth-of-type(2) > form > input[1]", "value").trim());
//        domainInfo.addProperty("域名", getElementAttr(doc, "input#domain", "value").trim());
        System.out.println(domainInfo);

        Elements liElements = doc.select("body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(5) > td:nth-of-type(2) > ul > li");
        for (Element li : liElements) {
            String liText = li.text().trim();

            if (liText.contains("注册人/机构")) {
                String org = li.selectFirst("a") != null ? li.selectFirst("a").text().trim() : "";
                if (org.startsWith("//whois.ename.net") || org.contains("（更新）")) org = "";
                domainInfo.addProperty("注册人/机构", org);
            }

            if (liText.contains("年龄：")) {
                domainInfo.addProperty("域名年龄", liText.replace("年龄：", "").trim());
            }

            if (liText.contains("邮箱") || liText.matches(".*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) {
                String emailHref = li.selectFirst("a") != null ? li.selectFirst("a").attr("href").trim() : "";
                domainInfo.addProperty("注册邮箱", getEmail(emailHref));
            }
        }

        return domainInfo;
    }

    private static JsonObject extractRankInfo_aizhan(Document doc) {
        JsonObject rankInfo = new JsonObject();

        rankInfo.addProperty("百度权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(1) > a > img", "alt").replace("n", "0"));
        rankInfo.addProperty("移动权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(2) > a > img", "alt").replace("n", "0"));
        rankInfo.addProperty("360权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(3) > a > img", "alt").replace("n", "0"));
        rankInfo.addProperty("神马权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(4) > a > img", "alt").replace("n", "0"));
        rankInfo.addProperty("搜狗权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(5) > a > img", "alt").replace("n", "0"));
        rankInfo.addProperty("谷歌权重", getElementAttr(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(2) > td > ul > li:nth-of-type(6) > a > img", "alt").replace("n", "0"));

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

    private static JsonObject extractIcpInfo_chinaz(Document doc) {
        JsonObject icpInfo = new JsonObject();

        icpInfo.addProperty("备案号", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > span:nth-of-type(1) > i > a"));
        icpInfo.addProperty("备案所属", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > span:nth-of-type(2) > i"));
        icpInfo.addProperty("备案性质", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > span:nth-of-type(3) > i"));

        return icpInfo;
    }

    private static JsonObject extractIcpInfo_aizhan(Document doc) {
        JsonObject icpInfo = new JsonObject();
        icpInfo.addProperty("备案号", getElementText(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > ul > li:nth-of-type(1) > a"));
        icpInfo.addProperty("备案所属", getElementText(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > ul > li:nth-of-type(3) > span"));
        icpInfo.addProperty("备案性质", getElementText(doc, "body > div:nth-of-type(4) > div:nth-of-type(2) > div:nth-of-type(2) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > ul > li:nth-of-type(2) > span"));

        return icpInfo;
    }

    private static JsonObject extractWebsiteInfo(Document doc) {
        JsonObject websiteInfo = new JsonObject();

        // IP及所属地
        String elementText = getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(5) > td:nth-of-type(2) > div:nth-of-type(1) > span:nth-of-type(1) > i > a");
        parseIpInfo(elementText, websiteInfo);

        // 网站描述
        websiteInfo.addProperty("网站描述", getElementText(doc, "body > div:nth-of-type(10) > div:nth-of-type(2) > div:nth-of-type(1)"));

        return websiteInfo;
    }

    private static void parseIpInfo(String elementText, JsonObject websiteInfo) {
        Pattern pattern = Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+)\\[(.*?)\\]$");
        Matcher matcher = pattern.matcher(elementText);

        if (matcher.find()) {
            String ip = matcher.group(1);
            String location = matcher.group(2);
            websiteInfo.addProperty("IP", ip);
            websiteInfo.addProperty("IP所属", location);
        } else {
            websiteInfo.addProperty("IP", elementText);
        }
    }

}

package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetKeyConstants;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.data.JsonUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.Utils.*;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2023/9/23 23:41
 *
 * 域名、ip解析相关类
 */
public class GetSeo {

    public static JsonObject getSeo(String domain) {

        String poolName = ExecutorServiceManager.ExecutorPoolNames.GETSEO_ASSET;
        ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
        List<CompletableFuture<JsonObject>> futures = new ArrayList<>();

        CompletableFuture<JsonObject> futureChinaz = CompletableFuture.supplyAsync(() -> getSeo_chinaz(domain), executor);
        CompletableFuture<JsonObject> futureAizhan = CompletableFuture.supplyAsync(() -> getSeo_aizhan(domain), executor);
        futures.add(futureChinaz);
        futures.add(futureAizhan);

        JsonObject result1 = new JsonObject();
        JsonObject result2 = new JsonObject();
        for (Future<?> future : futures) {
            try {
                JsonObject tempResult = (JsonObject) future.get();
                if (future == futureChinaz) {
                    result1 = tempResult;
                } else if (future == futureAizhan) {
                    result2 = tempResult;
                }
            } catch (CancellationException ce) {} catch (Exception e) {
                if(debugMode) e.printStackTrace();
            }
        }

        // 停止所有线程
        ExecutorServiceManager.shutdownExecutor(poolName);

        return mergeJsonObjects(result1, result2);

    }

    private static final Pattern PERSONAL_SITE_PATTERN = Pattern.compile("是(.*?)个人网站，");
    private static final Pattern COMPANY_SITE_PATTERN = Pattern.compile("是(.*?)旗下网站，");

    // 使用chinaz对Domain进行SEO综合查询
    private static JsonObject getSeo_chinaz(String domain) {
        JsonObject tmpJsonObj_Asset = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        String Chinaz_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(AssetKeyConstants.CHINAZ_COOKIE).getAsString();
        boolean Proxy = JsonUtils.containsString(tmpJsonObj_Asset.getAsJsonArray(AssetKeyConstants.PROXY_KEY), AssetKeyConstants.CHINAZ_COOKIE);

        JsonObject seoMap = new JsonObject();
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", Chinaz_Cookie);

        try {
            RequestObj obj = new RequestObj().setUrl("https://seo.chinaz.com/" + domain)
                    .setMethod("GET").setRetries(3).setHeaders(headers);
            if(!Proxy) obj.setProxies(null);

            CustomHttpResponse con = requests(obj);
            int statusCode = con.getResponseCode();

            // 检查请求状态码
            if (statusCode != 200) {
                con.disconnect();
                return seoMap;
            }

            Document doc = con.getDocument();

            // 提取信息
            JsonObject domainInfo = extractDomainInfo_chinaz(doc);
            JsonObject icpInfo = extractIcpInfo_chinaz(doc);
            JsonObject websiteInfo = extractWebsiteInfo(doc);

            // 尝试从描述中提取信息
            icpInfo = extractFromDescription(websiteInfo, icpInfo);

            seoMap.add("域名信息", domainInfo);
            seoMap.add("备案信息", icpInfo);
            seoMap.add("网站信息", websiteInfo);

        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }

        return seoMap;
    }

    private static boolean isIcpInfoEmpty(JsonObject icpInfo) {
        return isEmptyOrNull(icpInfo.get("备案号")) &&
                isEmptyOrNull(icpInfo.get("备案所属")) &&
                isEmptyOrNull(icpInfo.get("备案性质"));
    }

    private static boolean isEmptyOrNull(JsonElement element) {
        return element == null ||
                element.isJsonNull() ||
                element.getAsString().trim().isEmpty();
    }

    private static JsonObject extractFromDescription(JsonObject websiteInfo, JsonObject icpInfo) {
        String description = websiteInfo.get("网站描述").toString();
        // 检查个人网站模式
        Matcher personalMatcher = PERSONAL_SITE_PATTERN.matcher(description);
        if (personalMatcher.find()) {
            String owner = personalMatcher.group(1);
            icpInfo.addProperty("备案所属", owner);
            icpInfo.addProperty("备案性质", "个人");
        }

        // 检查企业网站模式
        Matcher companyMatcher = COMPANY_SITE_PATTERN.matcher(description);
        if (companyMatcher.find()) {
            String owner = companyMatcher.group(1);
            icpInfo.addProperty("备案所属", owner);
            icpInfo.addProperty("备案性质", "企业");
        }

        return icpInfo;
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
                con.disconnect();
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
            if(elem1.equals("-") || elem1==null || elem1.isJsonNull() || elem1.getAsString().isEmpty()) return elem2;
            return elem1;
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

        domainInfo.addProperty("域名", getElementAttr(doc, "#host", "value").trim());
        domainInfo.addProperty("注册人/机构", getElementText(doc, "span:contains(注册人/机构：) > i").replace("redacted for privacy",""));
        domainInfo.addProperty("域名年龄", getElementText(doc, "span:contains(域名年龄：) > a > i"));

        return domainInfo;
    }

    private static JsonObject extractDomainInfo_aizhan(Document doc) {
        JsonObject domainInfo = new JsonObject();

        domainInfo.addProperty("域名", getElementAttr(doc, "#domain", "value").trim());

        Elements liElements = doc.select("#whois > li");
        for (Element li : liElements) {
            String liText = li.text().trim();

            if (liText.contains("注册人/机构")) {
                String org = li.selectFirst("a") != null ? li.selectFirst("a").text().trim() : "";
                if (org.startsWith("//whois.ename.net") || org.contains("（更新）") || org.contains("redacted for privacy")) org = "";
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

        rankInfo.addProperty("百度权重", getElementAttr(doc, "#baidurank_br > img", "alt").replace("n", "0"));
        rankInfo.addProperty("移动权重", getElementAttr(doc, "#baidurank_mbr > img", "alt").replace("n", "0"));
        rankInfo.addProperty("360权重", getElementAttr(doc, "#360_pr > img", "alt").replace("n", "0"));
        rankInfo.addProperty("神马权重", getElementAttr(doc, "#sm_pr > img", "alt").replace("n", "0"));
        rankInfo.addProperty("搜狗权重", getElementAttr(doc, "#sogou_pr > img", "alt").replace("n", "0"));
        rankInfo.addProperty("谷歌权重", getElementAttr(doc, "#google_pr > img", "alt").replace("n", "0"));

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
                con.disconnect();
                return email;
            }

            Document doc = con.getDocument();
            email = getElementAttr(doc, "#domain", "value");

        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }

        return email;
    }

    // TODO 不要用绝对路径  https://seo.chinaz.com/www.shenhuagroup.com.cn   备案信息暂时隐藏了
    private static JsonObject extractIcpInfo_chinaz(Document doc) {
        JsonObject icpInfo = new JsonObject();

        icpInfo.addProperty("备案号", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > span:nth-of-type(1) > i > a"));
        icpInfo.addProperty("备案所属", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > span:nth-of-type(2) > i"));
        icpInfo.addProperty("备案性质", getElementText(doc, "body > div:nth-of-type(4) > table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > span:nth-of-type(3) > i"));

        return icpInfo;
    }

    private static JsonObject extractIcpInfo_aizhan(Document doc) {
        JsonObject icpInfo = new JsonObject();
        icpInfo.addProperty("备案号", getElementText(doc, "#icp > li:nth-of-type(1) > a"));
        icpInfo.addProperty("备案所属", getElementText(doc, "#icp > li:nth-of-type(3) > span"));
        icpInfo.addProperty("备案性质", getElementText(doc, "#icp > li:nth-of-type(2) > span"));

        return icpInfo;
    }

    private static JsonObject extractWebsiteInfo(Document doc) {
        JsonObject websiteInfo = new JsonObject();

        // IP及所属地
        String elementText = getElementText(doc, "span:contains(IP：) > i > a");
        parseIpInfo(elementText, websiteInfo);

        // 网站描述
        websiteInfo.addProperty("网站描述", getElementText(doc, "#siteDetails"));

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

    public static void main(String[] args) {
        JsonObject jsonObject = getSeo("www.shenhuagroup.com.cn");
        System.out.println(jsonObject);


        JsonObject jsonObject1 = getSeo("potato.gold");
        System.out.println(jsonObject1);
    }

}

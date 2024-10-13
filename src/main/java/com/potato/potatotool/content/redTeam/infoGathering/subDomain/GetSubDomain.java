package com.potato.potatotool.content.redTeam.infoGathering.subDomain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.Utils.getElementText;
import static com.potato.potatotool.content.redTeam.infoGathering.Utils.getElements;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/9/29 14:56
 */
public class GetSubDomain {
    public static int timeOut = 5;

    public static Set<String> getSubByDomainOrDomainCert(String domain) {
        if(domain==null || domain.isEmpty() || domain.startsWith("http")) return new LinkedHashSet<>();

        // 使用CompletableFuture异步调用两个HTTP请求方法
        CompletableFuture<Set<String>> future_crt = CompletableFuture.supplyAsync(() -> getSubByDomainCert_crt(domain));
        CompletableFuture<Set<String>> future_certspotter = CompletableFuture.supplyAsync(() -> getSubByDomainCert_certspotter(domain));
        CompletableFuture<Set<String>> future_chaziyu = CompletableFuture.supplyAsync(() -> getSubByDomain_chaziyu(domain));
        CompletableFuture<Set<String>> future_rapiddns = CompletableFuture.supplyAsync(() -> getSubByDomain_rapiddns(domain));
        CompletableFuture<Set<String>> future_lienvault = CompletableFuture.supplyAsync(() -> getSubByDomain_alienvault(domain));
        CompletableFuture<Set<String>> future_ip138 = CompletableFuture.supplyAsync(() -> getSubByDomain_ip138(domain));


        try {
            return Stream.of(future_crt, future_certspotter, future_chaziyu, future_rapiddns, future_lienvault, future_ip138)
                    .map(CompletableFuture::join)
                    .flatMap(Set::stream)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (Exception e) {
            if (debugMode) System.out.println(e);
            return new LinkedHashSet<>();
        }
    }

    private static Set<String> getSubByDomainCert_crt(String domain) {
        Set<String> subDomain = new LinkedHashSet<>();

        try {
            RequestObj obj = new RequestObj().setUrl("https://crt.sh/?q=%." + domain + "&output=json")  // ?O=公司名&output=json 不兼容公司名为中文名
                    .setMethod("GET").setRandomUserAgent(false).setRetries(2).setTimeOut(timeOut);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode != 200) {
                return subDomain;
            }

            JsonArray jsonArray = con.getJson().getAsJsonArray();
            for (JsonElement element: jsonArray){
                String subDomainTmp = element.getAsJsonObject().get("name_value").getAsString().replace("*.", "");
                String[] subDomainListTmp = subDomainTmp.split("\n");
                subDomain.addAll(Arrays.asList(subDomainListTmp));
            }

        }catch (Exception e){
            if(debugMode) System.out.println(e);
        }

        return subDomain;
    }

    private static Set<String> getSubByDomainCert_certspotter(String domain) {
        Set<String> subDomain = new LinkedHashSet<>();

        try {
            RequestObj obj = new RequestObj().setUrl("https://api.certspotter.com/v1/issuances?domain=" + domain + "&include_subdomains=true&expand=dns_names")
                    .setMethod("GET").setRandomUserAgent(false).setRetries(2).setTimeOut(timeOut);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode != 200) {
                return subDomain;
            }

            JsonArray jsonArray = con.getJson().getAsJsonArray();
            for (JsonElement element: jsonArray){
                JsonArray subDomainTmpArray = element.getAsJsonObject().get("dns_names").getAsJsonArray();
                for(JsonElement subDomainTmp : subDomainTmpArray){
                    subDomain.add(subDomainTmp.getAsString().replace("*.", ""));
                }
            }

        }catch (Exception e){
            if(debugMode) System.out.println(e);
        }

        return subDomain;
    }

    // 有频次限制
    private static Set<String> getSubByDomain_chaziyu(String domain) {
        Set<String> subDomain = new LinkedHashSet<>();

        try {
            RequestObj obj = new RequestObj().setUrl("https://chaziyu.com/" + domain + "/")
                    .setMethod("GET").setRandomUserAgent(false).setRetries(2).setTimeOut(timeOut);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode != 200) {
                return subDomain;
            }

            Document doc = con.getDocument();
            Elements elements = getElements(doc, "body > div:nth-of-type(1) > div:nth-of-type(2) > div > div:nth-of-type(1) > div:nth-of-type(2) > div > div:nth-of-type(1) > div:nth-of-type(2) > table > tbody > tr");

            for(Element element : elements){
                subDomain.add(getElementText(element, "td:nth-of-type(2) > a"));
            }

            if(subDomain.size()==50){
                int index = 1;
                while (true) {
                    index += 1;
                    obj.setUrl("https://chaziyu.com/ipchaxun.do?domain=" + domain + "&page=" + index);

                    CustomHttpResponse con_next = requests(obj);

                    int statusCode_next = con.getResponseCode();
                    if (statusCode_next != 200) {
                        return subDomain;
                    }

                    try {
                        JsonArray jsonArray = con_next.getJson().getAsJsonObject().get("data").getAsJsonObject().get("result").getAsJsonArray();
                        for(JsonElement strElement : jsonArray){
                            subDomain.add(strElement.getAsString());
                        }
                        if(jsonArray.size()!=50) break;
                    }catch (Exception er){ // 返回元素不存在
                        break;
                    }
                }
            }

        }catch (Exception e){
            if(debugMode) System.out.println(e);
        }

        return subDomain;
    }


    private static Set<String> getSubByDomain_rapiddns(String domain) {
        Set<String> subDomain = new LinkedHashSet<>();

        try {
            int index = 0;
            while (true) {
                index += 1;
                RequestObj obj = new RequestObj().setUrl("https://rapiddns.io/s/" + domain + "?page=" + index)
                        .setMethod("GET").setRandomUserAgent(false).setRetries(2).setTimeOut(timeOut);

                CustomHttpResponse con = requests(obj);

                int statusCode = con.getResponseCode();
                // 检查请求状态码
                if (statusCode != 200) {
                    return subDomain;
                }

                Document doc = con.getDocument();
                Elements elements = getElements(doc, "body > section:nth-of-type(2) > div > div > div > div:nth-of-type(2) > table > tbody > tr");

                if(elements.size()==0) break;

                for (Element element : elements) {
                    subDomain.add(getElementText(element, "td:nth-of-type(1)"));
                }
            }

        }catch (Exception e){
            if(debugMode) System.out.println(e);
        }

        return subDomain;
    }


    private static Set<String> getSubByDomain_alienvault(String domain) {
        Set<String> subDomain = new LinkedHashSet<>();

        try {
            RequestObj obj = new RequestObj().setUrl("https://otx.alienvault.com/api/v1/indicators/domain/" + domain + "/passive_dns")
                    .setMethod("GET").setRandomUserAgent(false).setRetries(2).setTimeOut(timeOut);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            // 检查请求状态码
            if (statusCode != 200) {
                return subDomain;
            }

            JsonObject jsonObject = con.getJson().getAsJsonObject();
            if(jsonObject.has("passive_dns") && jsonObject.get("passive_dns").isJsonArray()){
                JsonArray jsonArray = jsonObject.get("passive_dns").getAsJsonArray();
                for(JsonElement element : jsonArray){
                    subDomain.add(element.getAsJsonObject().get("hostname").getAsString());
                }
            }

        }catch (Exception e){
            if(debugMode) System.out.println(e);
        }

        return subDomain;
    }

    // 正则表达式匹配ip138的token
    private static final Pattern TOKEN_PATTERN_ip138_Or_ipchaxun = Pattern.compile(
            "_TOKEN\\s*=\\s*'(.*?)';"
    );
    private static Set<String> getSubByDomain_ip138(String domain){
        Set<String> subDomain = new LinkedHashSet<>();

        try {
            RequestObj obj = new RequestObj().setUrl("https://site.ip138.com/" + domain + "/domain.htm")
                    .setMethod("GET").setRandomUserAgent(false).setRetries(2).setTimeOut(timeOut);

            CustomHttpResponse con = requests(obj);
            String content = con.getTextStr();
            int statusCode = con.getResponseCode();

            // 检查请求状态码
            if (statusCode != 200) {
                return subDomain;
            }

            Matcher matcher = TOKEN_PATTERN_ip138_Or_ipchaxun.matcher(content);
            if (matcher.find()) {

                String token = matcher.group(1);
                int index = 0;

                while (true) {
                    index += 1;
                    obj.setUrl("https://site.ip138.com/index/querychild/?domain=" + domain + "&page=" + index+ "&token=" + token);

                    CustomHttpResponse con_sub = requests(obj);

                    int statusCode_sub = con_sub.getResponseCode();
                    if (statusCode_sub != 200) {
                        break;
                    }

                    JsonObject jsonObject = con_sub.getJson().getAsJsonObject();
                    if (jsonObject.has("data") && jsonObject.get("data").isJsonArray() && jsonObject.get("data").getAsJsonArray().size()>0) {
                        for (JsonElement element : jsonObject.getAsJsonArray("data")) {
                            JsonArray jsonArray = jsonObject.get("data").getAsJsonArray();
                            for(JsonElement tmpData : jsonArray){
                                subDomain.add(tmpData.getAsString());
                            }
                        }
                    } else {
                        break;
                    }

                }
            }

        } catch (Exception e) {
            if(debugMode) System.out.println(e);
        }

        return subDomain;
    }

    public static void main(String[] args) {
//        Set<String> domain = getSubByDomainCert_crt("itsinghua.com");
//        System.out.println(domain);
//        Set<String> domain2 = getSubByDomainCert_certspotter("itsinghua.com");
//        System.out.println(domain2);

//        Set<String> domain3 = getSubByDomain_alienvault("itsinghua.com");
//        System.out.println(domain3);

//        Set<String> domain4 = getSubByDomain_chaziyu("gdut.edu.cn");
//        System.out.println(domain4);
//        System.out.println(domain4.size());
//
//        Set<String> domain5 = getSubByDomain_rapiddns("gdut.edu.cn");
//        System.out.println(domain5);
//        System.out.println(domain5.size());
//
//        Set<String> domain6 = getSubByDomain_ip138("gdut.edu.cn");
//        System.out.println(domain6);
//        System.out.println(domain6.size());

//        Set<String> domain7 = getSubByDomainOrDomainCert("gdut.edu.cn");
//        System.out.println(domain7);
//        System.out.println(domain7.size());
    }
}

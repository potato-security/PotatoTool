package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.network.RequestObj;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.tools.GetSeo.getSeo;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2023/9/28 09:17
 */
public class GetDomain {

    public static JsonArray getDomainByIp(String ip, int MaxSize) {

        String poolName = ExecutorServiceManager.ExecutorPoolNames.GETDOMAIN_ASSET;
        ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
        List<CompletableFuture<JsonArray>> futures = new ArrayList<>();

        CompletableFuture<JsonArray> futureIp138 = CompletableFuture.supplyAsync(() -> getDomainByIp_ip138(ip, MaxSize), executor);
        CompletableFuture<JsonArray> futureIpchaxun = CompletableFuture.supplyAsync(() -> getDomainByIp_ipchaxun(ip, MaxSize), executor);
        futures.add(futureIp138);
        futures.add(futureIpchaxun);

        JsonArray result1 = new JsonArray();
        JsonArray result2 = new JsonArray();
        for (Future<?> future : futures) {
            try {
                JsonArray tempResult = (JsonArray) future.get();
                if (future == futureIp138) {
                    result1 = tempResult;
                } else if (future == futureIpchaxun) {
                    result2 = tempResult;
                }
            } catch (CancellationException ce) {} catch (Exception e) {
                if(debugMode) e.printStackTrace();
            }
        }

        // 停止所有线程
        ExecutorServiceManager.shutdownExecutor(poolName);

        return domainDataDeduplication(result1, result2, MaxSize);

    }

    // 正则表达式匹配ip138的token
    private static final Pattern TOKEN_PATTERN_ip138_Or_ipchaxun = Pattern.compile(
            "_TOKEN\\s*=\\s*'(.*?)';"
    );
    // 使用ip138进行 域名反查
    private static JsonArray getDomainByIp_ip138(String ip, int MaxSize) {
        JsonArray domainDataArray = new JsonArray(); // 爬取反查域名信息

        try {
            RequestObj obj = new RequestObj().setUrl("https://site.ip138.com/" + ip + "/")
                    .setMethod("GET").setRandomUserAgent(false).setRetries(3);

            CustomHttpResponse con = requests(obj);
            String content = con.getTextStr();
            int statusCode = con.getResponseCode();

            // 检查请求状态码
            if (statusCode != 200) {
                con.disconnect();
                return domainDataArray;
            }

            Matcher matcher = TOKEN_PATTERN_ip138_Or_ipchaxun.matcher(content);
            if (matcher.find()) {

                String token = matcher.group(1);
                int index = 0;

                while (true) {
                    index += 1;
                    RequestObj obj_sub = new RequestObj().setUrl("https://site.ip138.com/index/querybyip/?ip=" + ip + "&page=" + index+ "&token=" + token)
                            .setMethod("GET").setRandomUserAgent(false).setRetries(4);

                    CustomHttpResponse con_sub = requests(obj_sub);

                    int statusCode_sub = con_sub.getResponseCode();
                    if (statusCode_sub != 200) {
                        con.disconnect();
                        break;
                    }

                    JsonObject jsonObject = con_sub.getJson().getAsJsonObject();
                    if (jsonObject.has("data") && jsonObject.get("data").isJsonArray()) {
                        for (JsonElement element : jsonObject.getAsJsonArray("data")) {
                            element.getAsJsonObject().addProperty("show", false);
                            element.getAsJsonObject().addProperty("deepGet", false);
                            domainDataArray.add(element);
                            if(domainDataArray.size()==MaxSize) return domainDataArray;
                        }
                    } else {
                        break;
                    }

                }
            }

        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
        }

        return domainDataArray;
    }

    private static JsonArray getDomainByIp_ipchaxun(String ip, int MaxSize){
        JsonArray domainDataArray = new JsonArray(); // 爬取反查域名信息

        try {
            RequestObj obj = new RequestObj().setUrl("https://ipchaxun.com/" + ip + "/")
                    .setMethod("GET").setRandomUserAgent(false).setRetries(3);

            CustomHttpResponse con = requests(obj);
            String content = con.getTextStr();
            int statusCode = con.getResponseCode();

            // 检查请求状态码
            if (statusCode != 200) {
                con.disconnect();
                return domainDataArray;
            }

            Matcher matcher = TOKEN_PATTERN_ip138_Or_ipchaxun.matcher(content);
            if (matcher.find()) {

                String token = matcher.group(1);
                int index = 0;

                while (true) {
                    index += 1;
                    RequestObj obj_sub = new RequestObj().setUrl("https://ipchaxun.com/index/index/querybyip/?ip=" + ip + "&page=" + index+ "&token=" + token)
                            .setMethod("GET").setRandomUserAgent(false).setRetries(3);

                    CustomHttpResponse con_sub = requests(obj_sub);

                    int statusCode_sub = con_sub.getResponseCode();
                    if (statusCode_sub != 200) {
                        con.disconnect();
                        break;
                    }

                    JsonObject jsonObject = con_sub.getJson().getAsJsonObject();
                    if (jsonObject.has("data") && jsonObject.get("data").isJsonArray() && jsonObject.get("data").getAsJsonArray().size()>0) {
                        for (JsonElement element : jsonObject.getAsJsonArray("data")) {
                            element.getAsJsonObject().remove("_id");
                            element.getAsJsonObject().addProperty("show", false);
                            element.getAsJsonObject().addProperty("deepGet", false);
                            domainDataArray.add(element);
                            if(domainDataArray.size()==MaxSize) return domainDataArray;
                        }
                    } else {
                        break;
                    }

                }
            }

        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
        }

        return domainDataArray;
    }

    // webscan的api均属于国外cdn，需翻墙且很卡。备选暂不使用
    private static JsonArray getDomainByIp_webscan(String ip, int MaxSize){
        JsonArray domainDataArray = new JsonArray();

        try {
            RequestObj obj = new RequestObj().setUrl("https://api.webscan.cc/?action=query&ip=" + ip)
                    .setMethod("GET").setRetries(3);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                con.disconnect();
                return domainDataArray;
            }

            JsonArray jsonArray = con.getJson().getAsJsonArray();
            for (JsonElement element : jsonArray) {
                JsonObject oldObject = element.getAsJsonObject();
                String domain = oldObject.get("domain").getAsString();

                if(domain.equals(ip)) break;

                JsonObject newObject = new JsonObject();
                newObject.addProperty("domain", domain);
                newObject.addProperty("addtime", "");
                newObject.addProperty("uptime", "");

                domainDataArray.add(newObject);
                if(domainDataArray.size()==MaxSize) break;
            }

        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
        }

        return domainDataArray;
    }

    // 根据domain字段去重，保留uptime较新的数据，并获取域名SEO信息
    private static JsonArray domainDataDeduplication(JsonArray jsonArray1, JsonArray jsonArray2, int MaxSize) {
        Map<String, JsonObject> domainMap = new HashMap<>();

        // 处理第一个JsonArray
        for (JsonElement element : jsonArray1) {
            JsonObject jsonObject = element.getAsJsonObject();
            String domain = jsonObject.get("domain").getAsString();
            domainMap.put(domain, jsonObject);
        }

        // 处理第二个JsonArray并根据uptime去重
        for (JsonElement element : jsonArray2) {
            JsonObject jsonObject = element.getAsJsonObject();
            String domain = jsonObject.get("domain").getAsString();
            String uptime = jsonObject.get("uptime").getAsString();

            if (domainMap.containsKey(domain)) {
                String existingUptime = domainMap.get(domain).get("uptime").getAsString();
                if (uptime.compareTo(existingUptime) > 0) {
                    domainMap.put(domain, jsonObject);
                }
            } else {
                domainMap.put(domain, jsonObject);
            }
        }

        // 将Map中的JsonObject转换为List并按照uptime降序排序
        List<JsonObject> sortedList = new ArrayList<>(domainMap.values());
        sortedList.sort((o1, o2) -> {
            String uptime1 = o1.get("uptime").getAsString();
            String uptime2 = o2.get("uptime").getAsString();
            return uptime2.compareTo(uptime1); // 降序排序
        });

        // 将排序后的List转换回JsonArray
        JsonArray resultArray = new JsonArray();
        for (JsonObject jsonObject : sortedList) {
            if(resultArray.size()==MaxSize) break;

            String domain = jsonObject.get("domain").getAsString();
            JsonObject seoMap = getSeo(domain);
            jsonObject.add("seoMap", seoMap);

            resultArray.add(jsonObject);
        }

        return resultArray;
    }

}

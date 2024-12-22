package com.potato.potatotool.content.redTeam.infoGathering.infoLeakage.searchEnginesLeakage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetKeyConstants;
import com.potato.potatotool.content.redTeam.infoGathering.infoLeakage.gitLeakage.GitHubLeakage;
import com.potato.potatotool.content.redTeam.infoGathering.utils.AiUtils;
import com.potato.potatotool.utils.*;

import java.util.*;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2023/10/1 15:53
 */
public class GoogleSearch {
    private static List<HashMap<String, String>> Google_API_List;
    private static boolean Proxy = false;
    private static boolean isEffectiveKey = true;
    private static int keyIndex = 0;

    public GoogleSearch(Set<HashMap<String, String>> Google_API_List, boolean Proxy){
        this.Google_API_List = new ArrayList<HashMap<String, String>>(Google_API_List);
        this.Proxy = Proxy;
    }

    public JsonArray searchLeakageByDomain(String domain, boolean isCrawlProxy, int maxGoogleSearchCount){
        String input = "site:" + domain + " cominurl:sql | 密码 | 内部 | password | inurl:apidocs | inurl:api-docs | inurl:swagger | inurl:api-explorer | intext:\"sql syntax near\" | intext:\"syntax error has occurred\" | intext:\"incorrect syntax near\" | intext:\"unexpected end of SQL command\" | intext:\"Warning: mysql_connect()\" | intext:\"Warning: mysql_query()\" | intext:\"Warning: pg_connect()\" | filetype:sqlext:sql | ext:dbf | ext:mdb";
        if(domain.isEmpty()){
            input = "";
        }
        return search(input, null, true, isCrawlProxy, maxGoogleSearchCount);
    }

    public JsonArray searchLeakageByIp(String ip, boolean isCrawlProxy, int maxGoogleSearchCount){
        String input = "\"" + ip + "\" cominurl:sql | 密码 | 内部 | password | inurl:apidocs | inurl:api-docs | inurl:swagger | inurl:api-explorer | intext:\"sql syntax near\" | intext:\"syntax error has occurred\" | intext:\"incorrect syntax near\" | intext:\"unexpected end of SQL command\" | intext:\"Warning: mysql_connect()\" | intext:\"Warning: mysql_query()\" | intext:\"Warning: pg_connect()\" | filetype:sqlext:sql | ext:dbf | ext:mdb";
        if(ip.isEmpty()){
            input = "";
        }
        return search(input, null, true, isCrawlProxy, maxGoogleSearchCount);
    }

    // 获取影子资产domain，已经过icon\ai过滤
    public JsonArray searchDomainByCompanyName(String companyName, Map<String, Object> targetWebBaseInfoMap, boolean isCrawlProxy, int maxGoogleSearchCount){
        String input = "\"" + companyName + "\"";
        if(companyName.isEmpty()){
            input = "";
        }
        return search(input, targetWebBaseInfoMap, false, isCrawlProxy, maxGoogleSearchCount);
    }


    public JsonArray search(String input, Map<String, Object> targetWebBaseInfoMap, boolean isSearchLeakage, boolean isCrawlProxy, int maxGoogleSearchCount){
        JsonArray repoArray = new JsonArray();
        if(input.isEmpty() ||Google_API_List.size()==0|| Google_API_List.get(0).isEmpty() || !isEffectiveKey){
            return repoArray;
        }

        int index = 0;

        while (true) {
            if(index * 10 >= maxGoogleSearchCount || index==10) break; // 一页10条，当前index=上次页数
            int currentIndex = index * 10 + 1;
            index += 1;

            String Google_Key = Google_API_List.get(keyIndex).get("Google_Key");
            String Google_Cx = Google_API_List.get(keyIndex).get("Google_Cx");

            try {
                RequestObj obj = new RequestObj()
                        .setUrl("https://customsearch.googleapis.com/customsearch/v1?key=" + Google_Key + "&q=" + strUtils.urlEncode(input) + "&cx=" + Google_Cx + "&start=" + currentIndex)
                        .setMethod("GET");
                if(!Proxy) obj.setProxies(null);

                CustomHttpResponse con = requests(obj);

                int statusCode = con.getResponseCode();
                // 检查请求状态码
                if (statusCode != 200) {
                    con.disconnect();
                    if(statusCode == 429){
                        keyIndex += 1;
                        index -= 1;
                        if(keyIndex+1 > Google_API_List.size()){
                            System.out.println("所有Google账号今日均无免费额度可使用。");
                            break;
                        }else {
                            continue;
                        }
                    }
                    break;
                }

                JsonObject repoArrayObj = con.getJson().getAsJsonObject();
                if(!input.contains("【Check】")) {
                    if (repoArrayObj.has("items")) {
                        JsonArray items = repoArrayObj.getAsJsonArray("items");
                        for (JsonElement item : items) {
                            JsonObject repoObj = new JsonObject();
                            String url = item.getAsJsonObject().get("link").getAsString();
                            String domain = item.getAsJsonObject().get("displayLink").getAsString();
                            String title = item.getAsJsonObject().get("title").getAsString();
                            String content = item.getAsJsonObject().get("snippet").getAsString();
                            String des = item.getAsJsonObject().get("snippet").getAsString();

                            if (isSearchLeakage) {
                                // ip/domain
                                Set<String> leakageList = AiUtils.getLeakage_Ai(content);
                                JsonArray leakageArray = new JsonArray();
                                if (leakageList.size() > 0) {
                                    for (String leakage : leakageList) {
                                        leakageArray.add(leakage);
                                    }
                                    repoObj.add("leakageArray", leakageArray);
                                } else {
                                    continue;
                                }
                            } else {
                                // 公司名
                                boolean isContentRelevance = AiUtils.getContentRelevance_Ai(domain, input.replace("\"", ""), targetWebBaseInfoMap, isCrawlProxy);
                                if (!isContentRelevance) {
                                    continue;
                                }
                            }

                            repoObj.addProperty("url", url);
                            repoObj.addProperty("domain", domain);
                            repoObj.addProperty("title", title);
                            repoObj.addProperty("content", content);
                            repoObj.addProperty("des", des);
                            repoArray.add(repoObj);
                        }
                    } else {
                        break;
                    }
                }else {
                    // 仅做检查使用
                    if(repoArrayObj.has("queries")){
                        JsonArray res = repoArrayObj.getAsJsonObject("queries").getAsJsonArray("request");
                        if(res.size()>0){
                            repoArray.add("OK");
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                if (debugMode) e.printStackTrace();
                break;
            }
        }

        return repoArray;
    }

    public static String getError_Google() {
        isEffectiveKey = true;
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        JsonArray jsonArray = tmpJsonObj.getAsJsonArray(AssetKeyConstants.GOOGLE_API);
        Set<HashMap<String, String>> google_API_List = new HashSet<>();
        for (JsonElement element : jsonArray) {
            HashMap<String, String> map = new HashMap<>();
            JsonObject obj = element.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
            }
            google_API_List.add(map);
        }
        boolean Proxy = jsonUtils.containsString(tmpJsonObj.getAsJsonArray(AssetKeyConstants.PROXY_KEY), AssetKeyConstants.GOOGLE_API);
        GoogleSearch googleSearch = new GoogleSearch(google_API_List, Proxy);
        String domain = "【Check】";
        if (googleSearch.searchLeakageByDomain(domain, true, 1).isEmpty()){
            isEffectiveKey = false;
            return "无效的Google_API/限国外IP";
        }
        return null;
    }


    public static void main(String[] args) {
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(AssetKeyConstants.ASSET);
        JsonArray jsonArray = tmpJsonObj.getAsJsonArray(AssetKeyConstants.GOOGLE_API);
        Set<HashMap<String, String>> google_API_List = new HashSet<>();
        for (JsonElement element : jsonArray) {
            HashMap<String, String> map = new HashMap<>();
            JsonObject obj = element.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
            }
            google_API_List.add(map);
        }
        boolean Proxy = jsonUtils.containsString(tmpJsonObj.getAsJsonArray(AssetKeyConstants.PROXY_KEY), AssetKeyConstants.GOOGLE_API);
        GoogleSearch googleSearch = new GoogleSearch(google_API_List, Proxy);


        System.out.println(googleSearch.searchLeakageByDomain("aabyss.cn", true, 10));

        System.out.println(googleSearch.searchLeakageByIp("127.0.0.1", true, 10));
    }
}

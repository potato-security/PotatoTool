package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.CustomHttpResponse;
import com.potato.potatotool.utils.RequestObj;
import com.potato.potatotool.utils.strUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.requestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/16 18:35
 */
public class AiqichaSearch {

    private static Map<String, String> headers = new HashMap<>();

    private static List<Integer> weightThresholdList = new ArrayList<>();
    static {
//        weightThresholdList.add(100);
//        weightThresholdList.add(100);
        //TODO 界面说明 view-source:https://aiqicha.baidu.com
        headers.put("Cookie", "***REMOVED***");
        headers.put("Referer", "https://aiqicha.baidu.com");
        headers.put("Connection", "close");
    }

    public static List<Integer> getWeightThresholdList() {
        return weightThresholdList;
    }
    public static void setWeightThresholdList(List<Integer> weightThresholdList) {
        AiqichaSearch.weightThresholdList = weightThresholdList;
    }

    public static JsonArray getCompanyInfoIteration(String companyName){
        JsonArray result = new JsonArray();
        if(companyName.isEmpty()) return result;

        JsonObject companyInfo = getCompanyInfo(companyName,null);
        result.add(companyInfo);

        JsonArray weightCompany = companyInfo.getAsJsonObject(companyName).getAsJsonArray("weightCompany");
        for(JsonElement jsonElement : weightCompany){
            String subCompanyName = jsonElement.getAsJsonObject().get("entName").getAsString();
            String subPid = jsonElement.getAsJsonObject().get("pid").getAsString();
            result.add(getCompanyInfo(subCompanyName, subPid));
        }

        return result;
    }

    public static JsonObject getCompanyInfo(String companyName, String subPid){
        JsonObject result = new JsonObject();
        if(companyName.isEmpty()) return result;

        String companyId;
        if(subPid == null){
            companyId = getCompanyId(companyName);
        }else {
            companyId = subPid;
        }

        CompletableFuture<JsonArray> appFuture = CompletableFuture.supplyAsync(() -> getApp(companyId));
        CompletableFuture<JsonArray> wxFuture = CompletableFuture.supplyAsync(() -> getWx(companyId));
        CompletableFuture<JsonArray> domainAndIcpFuture = CompletableFuture.supplyAsync(() -> getDomainAndIcp(companyId));
        CompletableFuture<Void> allOf;
        CompletableFuture<JsonArray> weightCompanyFuture = new CompletableFuture<>();
        if(subPid == null) {
            weightCompanyFuture = CompletableFuture.supplyAsync(() -> getWeightCompany(companyId));
            allOf = CompletableFuture.allOf(appFuture, wxFuture, domainAndIcpFuture, weightCompanyFuture);
        }else {
            allOf = CompletableFuture.allOf(appFuture, wxFuture, domainAndIcpFuture);
        }

        allOf.join();

        try {
            JsonObject tmpData = new JsonObject();
            tmpData.add("app", appFuture.get());
            tmpData.add("wx", wxFuture.get());
            tmpData.add("domainAndIcp", domainAndIcpFuture.get());
            if(subPid == null) tmpData.add("weightCompany", weightCompanyFuture.get());
            result.add(companyName, tmpData);
        } catch (Exception e) {
            e.printStackTrace();  // 处理可能的异常
        }

        return result;
    }

    private static JsonArray getApp(String companyId){
        return getDataByC("https://aiqicha.baidu.com/c/appinfoAjax?size=100&pid=" + companyId);
    }

    private static JsonArray getWx(String companyId){
        return getDataByC("https://aiqicha.baidu.com/c/wechatoaAjax?size=100&pid=" + companyId);
    }

    private static JsonArray getDomainAndIcp(String companyId){
        return getDataByC("https://aiqicha.baidu.com/cs/icpInfoAjax?size=1000&pid=" + companyId);
    }

    private static JsonArray getWeightCompany(String companyId){
        return getWeightCompany(companyId, 0);
    }

    private static JsonArray getWeightCompany(String companyId, int currentIndex){
        JsonArray finalJsonArray = new JsonArray();
        if(weightThresholdList.size() == 0) return finalJsonArray;

        JsonArray currentJsonArray = getDataByC("https://aiqicha.baidu.com/stockchart/stockchartAjax?drill=2&pid=" + companyId);

        JsonArray currentJsonArray_Filter = filterByRegRate(currentJsonArray, weightThresholdList.get(currentIndex));
        finalJsonArray.addAll(currentJsonArray_Filter);

        if(currentJsonArray_Filter.size() > 0 && weightThresholdList.size() > currentIndex + 1){
            for(JsonElement jsonElement : currentJsonArray_Filter) {
                String subCompanyId = jsonElement.getAsJsonObject().get("pid").getAsString();
                boolean investment = jsonElement.getAsJsonObject().get("investment").getAsBoolean();
                if(investment) finalJsonArray.addAll(getWeightCompany(subCompanyId, currentIndex + 1));
            }
        }

        return finalJsonArray;
    }

    public static JsonArray filterByRegRate(JsonArray inputArray, int weightThreshold) {
        if(inputArray==null) return new JsonArray();
        return StreamSupport.stream(inputArray.spliterator(), false)
                .map(JsonElement::getAsJsonObject)  // 将每个 JsonElement 转换为 JsonObject
                .filter(jsonObject -> {
                    if (jsonObject.has("regRate")) {  // 检查字段是否存在
                        try {
                            String regRateStr = jsonObject.get("regRate").getAsString().replace("%", "");
                            double regRate = Double.parseDouble(regRateStr);
                            return regRate >= weightThreshold;  // 使用 weightThreshold 作为过滤条件
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                    return false;
                })
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
    }


    private static JsonArray getDataByC(String url){
        JsonArray jsonArray = new JsonArray();

        try {
            RequestObj obj = new RequestObj()
                    .setUrl(url)
                    .setMethod("GET")
                    .setHeaders(headers)
                    .setRetries(3);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                return null;
            }

            JsonObject tmpJsonObject = con.getJson().getAsJsonObject();

            if (!tmpJsonObject.getAsJsonObject("data").isJsonNull()) {
                JsonObject dataObject = tmpJsonObject.getAsJsonObject("data");
                if (url.contains("stockchart/stockchartAjax")) {
                    JsonObject investRecordDataObject = dataObject.getAsJsonObject("investRecordData");
                    if (investRecordDataObject!= null &&!investRecordDataObject.isJsonNull()) {
                        jsonArray = investRecordDataObject.getAsJsonArray("list");
                    }
                } else {
                    if (dataObject.has("list") && dataObject.get("list").isJsonArray()) {
                        jsonArray = dataObject.getAsJsonArray("list");
                    }
                }
            }

        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
        }

        return jsonArray;
    }

    // TODO 如果code=300  弹出页面验证码验证一下
    private static String getCompanyId(String companyName){
        String pidValue = null;

        try {
            RequestObj obj = new RequestObj()
                    .setUrl("https://aiqicha.baidu.com/s?q=" + strUtils.urlEncode(companyName))
                    .setMethod("GET")
                    .setHeaders(headers)
//                    .setProxies("http://127.0.0.1:8080")
                    .setRetries(3);

            CustomHttpResponse con = requests(obj);

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                return null;
            }

            String contentStr = con.getTextStr();
            String regex = "\"pid\":\"(\\d+)\"";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(contentStr);

            if (matcher.find()) {
                pidValue = matcher.group(1);
            }
        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
            System.out.println(e);
        }

        return pidValue;
    }

    public static void main(String[] args) {
        System.out.println(getCompanyInfoIteration("北京搜狗信息服务有限公司"));

//        try (BufferedReader br = new BufferedReader(new FileReader("/Users/a/Library/Containers/com.tencent.WeWorkMac/Data/Documents/Profiles/72E3F51AB224938EF8C93E98DF173761/Caches/Files/2024-10/d45c19ed4b78299a023b215065d7995b/123.txt"));
//             CSVWriter writer = new CSVWriter(new FileWriter("/Users/a/Library/Containers/com.tencent.WeWorkMac/Data/Documents/Profiles/72E3F51AB224938EF8C93E98DF173761/Caches/Files/2024-10/d45c19ed4b78299a023b215065d7995b/1234.csv"));
//        ) {
//            String[] header = {"公司名", "域名", "备案号", "网站名称"};
//            writer.writeNext(header);
//            String line;
//            while ((line = br.readLine()) != null) {
//                String companyName = line.trim();
//                if(companyName.isEmpty()) continue;
//                System.out.println(companyName);
//
//                JsonArray infoArray = getCompanyInfoIteration(companyName);
//                System.out.println(infoArray);
//
//                for(JsonElement jsonElement : infoArray){
//                    JsonObject jsonObject = jsonElement.getAsJsonObject();
//                    String subCompanyName = jsonObject.entrySet().iterator().next().getKey();
//                    JsonArray domainAndIcp = jsonObject.getAsJsonObject(subCompanyName).getAsJsonArray("domainAndIcp");
//                    for(JsonElement subJsonElement :domainAndIcp){
//                        JsonArray domainList = subJsonElement.getAsJsonObject().get("domain").getAsJsonArray();
//                        String domain = StreamSupport.stream(domainList.spliterator(), false)
//                                .map(JsonElement::getAsString)
//                                .collect(Collectors.joining("\n"));
//                        String icpNo = subJsonElement.getAsJsonObject().get("icpNo").getAsString();
//                        String siteName = subJsonElement.getAsJsonObject().get("siteName").getAsString();
//                        writer.writeNext(new String[]{
//                                subCompanyName, domain, icpNo, siteName
//                        });
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}

package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.MainApplication;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetConstants;
import com.potato.potatotool.controller.redTeam.PaneInfoSearch;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.network.ProxyUtils;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import javafx.application.HostServices;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/16 18:35
 */
public class AiqichaSearch {
    private Map<String, String> headers = new HashMap<>();

    private List<Integer> weightThresholdList = new ArrayList<>();

    HostServices services = MainApplication.letGetHostServices();

    private static String Aiqicha_Cookie = "";
    private static Boolean Proxy = false;
    static {
        JsonObject tmpJsonObj_Asset = (JsonObject) Constants.getOutsideConfig(AssetConstants.ASSET);
        Aiqicha_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(AssetConstants.AIQICHA_COOKIE).getAsString();
        Proxy = ProxyUtils.isServiceProxyEnabled(AssetConstants.AIQICHA_COOKIE);
    }

    private PaneInfoSearch paneInfoSearch;

    public AiqichaSearch(List<Integer> weightThresholdList, PaneInfoSearch paneInfoSearch){
        JsonObject tmpJsonObj_Asset = (JsonObject) Constants.getOutsideConfig(AssetConstants.ASSET);
        Aiqicha_Cookie = tmpJsonObj_Asset.getAsJsonPrimitive(AssetConstants.AIQICHA_COOKIE).getAsString();
        Proxy = ProxyUtils.isServiceProxyEnabled(AssetConstants.AIQICHA_COOKIE);

        this.weightThresholdList = weightThresholdList;
        this.headers.put("Cookie", Aiqicha_Cookie);
        this.headers.put("Referer", "https://aiqicha.baidu.com");
        this.headers.put("Connection", "close");

        this.paneInfoSearch = paneInfoSearch;
    }

    public JsonArray getCompanyInfoIteration(String companyName){
        JsonArray result = new JsonArray();
        if(companyName.isEmpty()) return result;

        JsonObject companyInfo = getCompanyInfo(companyName,null);
        result.add(companyInfo);

        if(companyInfo!=null && companyInfo.size()>0) {
            JsonArray weightCompany = companyInfo.getAsJsonObject(companyName).getAsJsonArray("weightCompany");
            for (JsonElement jsonElement : weightCompany) {
                String subCompanyName = jsonElement.getAsJsonObject().get("entName").getAsString();
                String subPid = jsonElement.getAsJsonObject().get("pid").getAsString();
                result.add(getCompanyInfo(subCompanyName, subPid));
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
        }

        return result;
    }

    public JsonObject getCompanyInfo(String companyName, String subPid){
        JsonObject result = new JsonObject();
        if(companyName.isEmpty()) return result;

        String companyId;
        if(subPid == null){
            companyId = getCompanyId(companyName);
        }else {
            companyId = subPid;
        }

        if (companyId==null||companyId.isEmpty()) return result;


        String poolName = ExecutorServiceManager.ExecutorPoolNames.AIQICHA_ASSET;
        ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
        List<CompletableFuture<JsonArray>> futures = new ArrayList<>();

        CompletableFuture<JsonArray> appFuture = CompletableFuture.supplyAsync(() -> getApp(companyId), executor);
        CompletableFuture<JsonArray> wxFuture = CompletableFuture.supplyAsync(() -> getWx(companyId), executor);
        CompletableFuture<JsonArray> domainAndIcpFuture = CompletableFuture.supplyAsync(() -> getDomainAndIcp(companyId), executor);
        CompletableFuture<JsonArray> weightCompanyFuture = null;
        futures.add(appFuture);
        futures.add(wxFuture);
        futures.add(domainAndIcpFuture);
        if (subPid == null) {
            weightCompanyFuture = CompletableFuture.supplyAsync(() -> getWeightCompany(companyId), executor);
            futures.add(weightCompanyFuture);
        }

        JsonObject tmpData = new JsonObject();
        for (Future<?> future : futures) {
            try {
                JsonArray tmpJsonArray = (JsonArray) future.get();
                if (future == appFuture) {
                    tmpData.add("app", tmpJsonArray);
                } else if (future == wxFuture) {
                    tmpData.add("wx", tmpJsonArray);
                } else if (future == domainAndIcpFuture) {
                    tmpData.add("domainAndIcp", tmpJsonArray);
                } else if (future == weightCompanyFuture) {
                    tmpData.add("weightCompany", tmpJsonArray);
                }
                result.add(companyName, tmpData);
            } catch (CancellationException ce) {} catch (Exception e) {
                if (debugMode) e.printStackTrace();
            }
        }

        ExecutorServiceManager.shutdownExecutor(poolName);

        return result;
    }

    private JsonArray getApp(String companyId){
        return getDataByC("https://aiqicha.baidu.com/c/appinfoAjax?size=100&pid=" + companyId);
//        return null;
    }

    private JsonArray getWx(String companyId){
        return getDataByC("https://aiqicha.baidu.com/c/wechatoaAjax?size=100&pid=" + companyId);
//        return null;
    }

    private JsonArray getDomainAndIcp(String companyId){
        return getDataByC("https://aiqicha.baidu.com/cs/icpInfoAjax?size=1000&pid=" + companyId);
    }

    private JsonArray getWeightCompany(String companyId){
        return getWeightCompany(companyId, 0);
    }

    private JsonArray getWeightCompany(String companyId, int currentIndex){
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

    public JsonArray filterByRegRate(JsonArray inputArray, int weightThreshold) {
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


    private JsonArray getDataByC(String url){
        JsonArray jsonArray = new JsonArray();

        RequestObj obj = new RequestObj()
                .setUrl(url)
                .setMethod("GET")
                .setHeaders(headers)
                .setRetries(2);
        ProxyUtils.applyProxy(obj, Proxy);

        try (CustomHttpResponse con = requests(obj)) {

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                return null;
            }

            JsonObject tmpJsonObject = con.getJson().getAsJsonObject();

            if (tmpJsonObject.getAsJsonObject("data") != null && !tmpJsonObject.getAsJsonObject("data").isJsonNull()) {
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

    private boolean openedWeb = false;
    private String getCompanyId(String companyName){
        String pidValue = null;

        RequestObj obj = new RequestObj()
                .setUrl("https://aiqicha.baidu.com/s?q=" + StrUtils.urlEncode(companyName))
                .setMethod("GET")
                .setHeaders(headers)
                .setRetryWaitTime(5)
                .setRetries(2);
        ProxyUtils.applyProxy(obj, Proxy);

        try (CustomHttpResponse con = requests(obj)) {

            int statusCode = con.getResponseCode();
            if (statusCode != 200) {
                if(statusCode == 302) {
                    String location= con.getHeaderField("Location").get(0);
                    if(location.equals("https://aiqicha.baidu.com/acount/accessrestriction")) {
                        paneInfoSearch.showTip("启动软件时关闭代理，使用中国IP", false);
                        Thread.sleep(5000);
                        return getCompanyId(companyName);
                    }else if(location.startsWith("https://wappass.baidu.com")) {
                        if (paneInfoSearch != null) paneInfoSearch.showTip("请尽快验证个人账号(他人账户无法校验)，10秒后重试……", false);
                        Thread.sleep(2000);
                        if (!openedWeb)
                            services.showDocument(location);
                        openedWeb = true;
                        Thread.sleep(10000);
                        return getCompanyId(companyName);
                    }else if(location.startsWith("/login?u=") || location.startsWith("https://aiqicha.baidu.com/cbae/tr?headto=")){
                        paneInfoSearch.showTip("请确认[爱企查]Cookie是否有效，跳过部分流程……", false);
                    }
                }
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
        }

        return pidValue;
    }

    public static void main(String[] args) {
        List<Integer> weightThresholdList = new ArrayList<>();
        weightThresholdList.add(100);
        weightThresholdList.add(100);
        System.out.println(new AiqichaSearch(weightThresholdList, null).getCompanyInfoIteration("北京搜狗信息服务有限公司"));

//        weightThresholdList.add(100);
//        weightThresholdList.add(100);
//        System.out.println(new AiqichaSearch(weightThresholdList).getCompanyInfoIteration("北京搜狗信息服务有限公司"));

//        try (BufferedReader br = new BufferedReader(new FileReader("/Users/a/Desktop/项目开发/111/123.txt"));
//             CSVWriter writer = new CSVWriter(new FileWriter("/Users/a/Desktop/项目开发/111/123.csv"));
//        ) {
//            String[] header = {"公司名", "域名", "备案号", "网站名称"};
//            writer.writeNext(header);
//            String line;
//            while ((line = br.readLine()) != null) {
//                String companyName = line.trim();
//                if(companyName.isEmpty()) continue;
//                System.out.println(companyName);
//
//                JsonArray infoArray = new AiqichaSearch(weightThresholdList).getCompanyInfoIteration(companyName);
//                System.out.println(infoArray);
//
//                for(JsonElement jsonElement : infoArray){
//                    JsonObject jsonObject = jsonElement.getAsJsonObject();
//                    if(jsonObject.size()>0) {
//                        String subCompanyName = jsonObject.entrySet().iterator().next().getKey();
//                        JsonArray domainAndIcp = jsonObject.getAsJsonObject(subCompanyName).getAsJsonArray("domainAndIcp");
//                        for (JsonElement subJsonElement : domainAndIcp) {
//                            JsonArray domainList = subJsonElement.getAsJsonObject().get("domain").getAsJsonArray();
//                            String domain = StreamSupport.stream(domainList.spliterator(), false)
//                                    .map(JsonElement::getAsString)
//                                    .collect(Collectors.joining("\n"));
//                            String icpNo = subJsonElement.getAsJsonObject().get("icpNo").getAsString();
//                            String siteName = subJsonElement.getAsJsonObject().get("siteName").getAsString();
//                            System.out.println(111);
//                            writer.writeNext(new String[]{
//                                    subCompanyName, domain, icpNo, siteName
//                            });
//                        }
//                    }
//                }
////                Thread.sleep(2000);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

}

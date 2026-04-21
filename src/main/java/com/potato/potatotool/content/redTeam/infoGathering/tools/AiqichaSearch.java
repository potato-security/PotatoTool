package com.potato.potatotool.content.redTeam.infoGathering.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.AssetConstants;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.CompanyCandidate;
import com.potato.potatotool.controller.redTeam.PaneInfoSearch;
import com.potato.potatotool.utils.browser.CdpBrowserSession;
import com.potato.potatotool.utils.browser.CdpPageSnapshot;
import com.potato.potatotool.utils.browser.CdpTargetSelector;
import com.potato.potatotool.utils.browser.BrowserRuntimeConfig;
import com.potato.potatotool.utils.core.Constants;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.browser.ManagedChromeSession;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.ProxyUtils;
import com.potato.potatotool.utils.network.RequestObj;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.CompanyCandidateUtils.findExactCandidate;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.CompanyCandidateUtils.mergeCandidates;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2024/10/16 18:35
 */
public class AiqichaSearch {
    private static final String SEARCH_PAGE_ACCESS_RESTRICTION = "https://aiqicha.baidu.com/acount/accessrestriction";
    private static final String SEARCH_PAGE_WAPPASS_PREFIX = "https://wappass.baidu.com";
    private static final String SEARCH_PAGE_LOGIN_PREFIX = "/login?u=";
    private static final String SEARCH_PAGE_TR_PREFIX = "https://aiqicha.baidu.com/cbae/tr?headto=";
    private static final String SEARCH_PAGE_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36";
    private static final int MAX_SEARCH_PAGE_ATTEMPTS = 4;
    private static final int MAX_ACCESS_RESTRICTION_RETRIES = 1;
    private static final int MAX_ACCOUNT_VERIFY_RETRIES = 1;
    private static final int MAX_NON_REAL_PAGE_RETRIES = 1;
    private static final long ACCESS_RESTRICTION_WAIT_MS = 1500L;
    private static final long ACCOUNT_VERIFY_WAIT_MS = 3000L;
    private static final long NON_REAL_PAGE_WAIT_MS = 200L;
    private static final long SUB_COMPANY_INTERVAL_MS = 2000L;
    private static final long EXISTING_BROWSER_SESSION_WAIT_MS = 5000L;
    private static final long MANAGED_BROWSER_READY_WAIT_MS = 10000L;
    private static final long MANAGED_BROWSER_USER_WAIT_MS = 120000L;
    private static final int CDP_OPERATION_TIMEOUT_MILLIS = 10000;
    private static final String AIQICHA_TARGET_KEYWORD = "aiqicha.baidu.com";
    private static final Pattern PAGE_DATA_PATTERN = Pattern.compile("window\\.pageData\\s*=\\s*(\\{.*?\\});\\s*window\\.isSpider", Pattern.DOTALL);
    private static final Pattern HTML_TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern EM_TAG_PATTERN = Pattern.compile("</?em>", Pattern.CASE_INSENSITIVE);
    private static final Pattern AB_COOKIE_KEY_PATTERN = Pattern.compile("^ab\\d{9}$");

    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> activeSearchHeaders;

    private List<Integer> weightThresholdList = new ArrayList<>();

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
        this.headers.put("User-Agent", SEARCH_PAGE_USER_AGENT);

        this.paneInfoSearch = paneInfoSearch;
    }

    public JsonArray getCompanyInfoIteration(String companyName){
        return getCompanyInfoIteration(companyName, null);
    }

    public JsonArray getCompanyInfoIteration(String companyName, String companyPid){
        JsonArray result = new JsonArray();
        if(companyName.isEmpty()) return result;
        if (shouldStop()) return result;

        JsonObject companyInfo = getCompanyInfo(companyName, companyPid);
        if(companyInfo != null && companyInfo.size() > 0) {
            result.add(companyInfo);
        }

        if(companyInfo!=null && companyInfo.size()>0) {
            JsonArray weightCompany = companyInfo.getAsJsonObject(companyName).getAsJsonArray("weightCompany");
            for (JsonElement jsonElement : weightCompany) {
                if (shouldStop()) {
                    return result;
                }
                String subCompanyName = jsonElement.getAsJsonObject().get("entName").getAsString();
                String subPid = jsonElement.getAsJsonObject().get("pid").getAsString();
                JsonObject subCompanyInfo = getCompanyInfo(subCompanyName, subPid);
                if(subCompanyInfo != null && subCompanyInfo.size() > 0) {
                    result.add(subCompanyInfo);
                }
                if (!sleepInterruptibly(SUB_COMPANY_INTERVAL_MS)) {
                    return result;
                }
            }
        }

        return result;
    }

    public List<CompanyCandidate> searchCompanyCandidates(String companyName) {
        List<CompanyCandidate> result = new ArrayList<>();
        if (companyName == null || companyName.trim().isEmpty()) {
            return result;
        }

        JsonObject pageData = getSearchPageData(companyName);
        if (pageData == null || !pageData.has("result") || pageData.get("result").isJsonNull()) {
            return result;
        }

        JsonObject resultObject = pageData.getAsJsonObject("result");
        List<CompanyCandidate> resultListCandidates = extractCandidates(resultObject.getAsJsonArray("resultList"));
        return mergeCandidates(resultListCandidates);
    }

    public JsonObject getCompanyInfo(String companyName, String subPid){
        JsonObject result = new JsonObject();
        if(companyName.isEmpty()) return result;
        if (shouldStop()) return result;

        String companyId;
        if(subPid == null){
            companyId = getCompanyId(companyName);
        }else {
            companyId = subPid;
        }

        if (companyId==null||companyId.isEmpty()) {
            showTip("爱企查未获取到真实搜索结果页，已跳过：" + companyName);
            return result;
        }


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
                return jsonArray;
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
        List<CompanyCandidate> candidates = searchCompanyCandidates(companyName);
        CompanyCandidate exactCandidate = findExactCandidate(companyName, candidates);
        if (exactCandidate != null && exactCandidate.getAiqichaPid() != null && !exactCandidate.getAiqichaPid().isEmpty()) {
            return exactCandidate.getAiqichaPid();
        }
        return null;
    }

    private JsonObject getSearchPageData(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            return null;
        }
        for (Map<String, String> searchHeaders : buildSearchHeadersCandidates()) {
            JsonObject pageData = getSearchPageDataWithHeaders(companyName, searchHeaders);
            if (pageData != null) {
                return pageData;
            }
        }
        if (shouldReuseExistingBrowserSession()) {
            SearchPageFetchResult existingSessionFetchResult = fetchSearchPageResultFromExistingBrowserSession(companyName);
            if (existingSessionFetchResult != null) {
                SearchPageAnalysis existingSessionAnalysis = analyzeSearchPage(companyName, existingSessionFetchResult);
                if (existingSessionAnalysis.kind == SearchPageKind.REAL_PAGE) {
                    applyWorkingHeaders(buildSearchHeadersByCookie(buildExistingSessionSearchCookie()));
                    showTip("已复用本地浏览器会话获取爱企查真页面");
                    return existingSessionAnalysis.pageData;
                }
                if (existingSessionAnalysis.kind == SearchPageKind.CAPTCHA) {
                    showTip("本地浏览器会话当前处于百度安全验证页，请在直连Chrome中手动完成验证并停留在爱企查结果页后重试");
                } else if (existingSessionAnalysis.kind == SearchPageKind.ACCESS_RESTRICTION
                        || existingSessionAnalysis.kind == SearchPageKind.ACCESS_RESTRICTION_SHELL) {
                    showTip("本地浏览器会话当前仍被爱企查限制，请先在直连Chrome中完成一次真实搜索后重试");
                }
            }
        }

        SearchPageFetchResult managedBrowserFetchResult = fetchSearchPageResultFromManagedBrowser(companyName);
        if (managedBrowserFetchResult != null) {
            SearchPageAnalysis managedBrowserAnalysis = analyzeSearchPage(companyName, managedBrowserFetchResult);
            if (managedBrowserAnalysis.kind == SearchPageKind.REAL_PAGE) {
                showTip("已通过临时直连Chrome获取爱企查真页面");
                return managedBrowserAnalysis.pageData;
            }
        }
        return null;
    }

    private JsonObject getSearchPageDataWithHeaders(String companyName, Map<String, String> searchHeaders) {
        int attempts = 0;
        int accessRestrictionRetries = 0;
        int accountVerifyRetries = 0;
        int nonRealPageRetries = 0;
        while (attempts < MAX_SEARCH_PAGE_ATTEMPTS && !shouldStop()) {
            attempts++;
            this.activeSearchHeaders = searchHeaders;
            SearchPageFetchResult fetchResult;
            try {
                fetchResult = fetchSearchPageResult(companyName);
            } finally {
                this.activeSearchHeaders = null;
            }
            if (fetchResult == null) {
                return null;
            }
            if (fetchResult.statusCode == 200) {
                SearchPageAnalysis analysis = analyzeSearchPage(companyName, fetchResult);
                if (analysis.kind == SearchPageKind.REAL_PAGE) {
                    applyWorkingHeaders(searchHeaders);
                    return analysis.pageData;
                }
                if (analysis.shouldRetry() && nonRealPageRetries < MAX_NON_REAL_PAGE_RETRIES && sleepInterruptibly(NON_REAL_PAGE_WAIT_MS)) {
                    nonRealPageRetries++;
                    continue;
                }
                return null;
            }
            if (fetchResult.statusCode != 302) {
                return null;
            }

            String location = fetchResult.location == null ? "" : fetchResult.location;
            if (SEARCH_PAGE_ACCESS_RESTRICTION.equals(location)) {
                showTip("启动软件时关闭代理，使用中国IP");
                if (accessRestrictionRetries >= MAX_ACCESS_RESTRICTION_RETRIES || !sleepInterruptibly(ACCESS_RESTRICTION_WAIT_MS)) {
                    return null;
                }
                accessRestrictionRetries++;
                continue;
            }
            if (location.startsWith(SEARCH_PAGE_WAPPASS_PREFIX)) {
                showTip("请尽快验证个人账号(他人账户无法校验)，3秒后重试，失败则跳过……");
                if (!openedWeb) {
                    openDocument(location);
                }
                openedWeb = true;
                if (accountVerifyRetries >= MAX_ACCOUNT_VERIFY_RETRIES || !sleepInterruptibly(ACCOUNT_VERIFY_WAIT_MS)) {
                    return null;
                }
                accountVerifyRetries++;
                continue;
            }
            if (location.startsWith(SEARCH_PAGE_LOGIN_PREFIX) || location.startsWith(SEARCH_PAGE_TR_PREFIX)) {
                showTip("请确认[爱企查]Cookie是否有效，跳过部分流程……");
            }
            return null;
        }

        return null;
    }

    protected SearchPageFetchResult fetchSearchPageResult(String companyName) {
        return fetchSearchPageResult(companyName, activeSearchHeaders != null ? activeSearchHeaders : buildSearchHeaders());
    }

    protected SearchPageFetchResult fetchSearchPageResult(String companyName, Map<String, String> searchHeaders) {
        RequestObj obj = new RequestObj()
                .setUrl("https://aiqicha.baidu.com/s?q=" + StrUtils.urlEncode(companyName) + "&t=0")
                .setMethod("GET")
                .setHeaders(searchHeaders)
                .setRetryWaitTime(5)
                .setRetries(2);
        ProxyUtils.applyProxy(obj, Proxy);

        try (CustomHttpResponse con = requests(obj)) {
            URL finalUrl = con.getURL();
            return new SearchPageFetchResult(
                    con.getResponseCode(),
                    readLocation(con),
                    con.getTextStr(),
                    finalUrl == null ? "" : finalUrl.toString()
            );
        } catch (Exception e) {
            if(debugMode) e.printStackTrace();
            return null;
        }
    }

    protected SearchPageFetchResult fetchSearchPageResultFromExistingBrowserSession(String companyName) {
        String searchUrl = "https://aiqicha.baidu.com/s?q=" + StrUtils.urlEncode(companyName) + "&t=0";
        try {
            try (CdpBrowserSession session = attachAiqichaBrowserSession(getExistingBrowserSessionPort())) {
                if (session == null) {
                    return null;
                }
                CdpPageSnapshot currentSnapshot = session.getPage().snapshot(CDP_OPERATION_TIMEOUT_MILLIS);
                SearchPageFetchResult currentFetchResult = toBrowserFetchResult(currentSnapshot);
                if (currentFetchResult != null
                        && analyzeSearchPage(companyName, currentFetchResult).kind == SearchPageKind.REAL_PAGE) {
                    return currentFetchResult;
                }

                session.getPage().navigate(searchUrl, CDP_OPERATION_TIMEOUT_MILLIS);
                if (!sleepInterruptibly(EXISTING_BROWSER_SESSION_WAIT_MS)) {
                    return null;
                }
                CdpPageSnapshot snapshot = session.getPage().snapshot(CDP_OPERATION_TIMEOUT_MILLIS);
                if (snapshot == null || snapshot.getHtml() == null || snapshot.getHtml().trim().isEmpty()) {
                    return null;
                }
                return toBrowserFetchResult(snapshot);
            }
        } catch (Exception e) {
            if (debugMode) {
                e.printStackTrace();
            }
            return null;
        }
    }

    protected SearchPageFetchResult fetchSearchPageResultFromManagedBrowser(String companyName) {
        String searchUrl = "https://aiqicha.baidu.com/s?q=" + StrUtils.urlEncode(companyName) + "&t=0";
        String browserPath = BrowserRuntimeConfig.resolveBrowserPath();
        if (browserPath == null || browserPath.trim().isEmpty()) {
            showTip("未检测到Chrome/Chromium浏览器，已跳过爱企查浏览器辅助模式");
            return null;
        }

        ManagedChromeSession session = null;
        try {
            session = ManagedChromeSession.start(browserPath, searchUrl, true, MANAGED_BROWSER_READY_WAIT_MS);
            showTip("已打开临时直连Chrome，请在窗口中完成爱企查登录/安全验证并停留在结果页，完成后将自动继续");

            SearchPageKind lastKind = null;
            long deadline = System.currentTimeMillis() + MANAGED_BROWSER_USER_WAIT_MS;
            while (System.currentTimeMillis() < deadline && !shouldStop()) {
                try (CdpBrowserSession browserSession = attachAiqichaBrowserSession(session.getDevToolsPort())) {
                    if (browserSession != null) {
                        SearchPageFetchResult fetchResult = toBrowserFetchResult(
                                browserSession.getPage().snapshot(CDP_OPERATION_TIMEOUT_MILLIS));
                        if (fetchResult != null) {
                            SearchPageAnalysis analysis = analyzeSearchPage(companyName, fetchResult);
                            if (analysis.kind == SearchPageKind.REAL_PAGE) {
                                applyWorkingHeaders(buildSearchHeadersByCookie(buildDevToolsSearchCookie(session.getDevToolsPort())));
                                return fetchResult;
                            }
                            lastKind = analysis.kind;
                        }
                    }
                } catch (Exception ignored) {
                }
                if (!sleepInterruptibly(1000L)) {
                    return null;
                }
            }

            if (lastKind == SearchPageKind.CAPTCHA) {
                showTip("等待爱企查安全验证超时，已关闭临时Chrome");
            } else {
                showTip("等待爱企查真实结果页超时，已关闭临时Chrome");
            }
        } catch (Exception e) {
            if (debugMode) {
                e.printStackTrace();
            }
            showTip("启动临时直连Chrome失败，已跳过爱企查浏览器辅助模式：" + e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return null;
    }

    protected boolean sleepInterruptibly(long millis) {
        long remaining = millis;
        while (remaining > 0) {
            if (shouldStop()) {
                return false;
            }
            long waitMillis = Math.min(remaining, 200L);
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            remaining -= waitMillis;
        }
        return !shouldStop();
    }

    protected void showTip(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        if (paneInfoSearch != null) {
            paneInfoSearch.showTip(message, false);
        } else {
            System.out.println("[AiqichaSearch] " + message);
        }
    }

    protected void openDocument(String location) {
        if (location == null || location.trim().isEmpty()) {
            return;
        }
        try {
            Class<?> mainApplicationClass = Class.forName("com.potato.potatotool.MainApplication");
            Method hostServicesMethod = mainApplicationClass.getMethod("letGetHostServices");
            Object hostServices = hostServicesMethod.invoke(null);
            if (hostServices != null) {
                Method showDocumentMethod = hostServices.getClass().getMethod("showDocument", String.class);
                showDocumentMethod.invoke(hostServices, location);
            }
        } catch (Throwable ignored) {
        }
    }

    protected boolean shouldStop() {
        return Thread.currentThread().isInterrupted() || (paneInfoSearch != null && paneInfoSearch.isSearchCancelled());
    }

    private String readLocation(CustomHttpResponse con) {
        try {
            List<String> locationList = con.getHeaderField("Location");
            if (locationList != null && !locationList.isEmpty()) {
                return locationList.get(0);
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private JsonObject parseSearchPageData(String contentStr) {
        if (contentStr == null || contentStr.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = PAGE_DATA_PATTERN.matcher(contentStr);

        if (matcher.find()) {
            try {
                return new JsonParser().parse(matcher.group(1)).getAsJsonObject();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    protected SearchPageAnalysis analyzeSearchPage(String companyName, SearchPageFetchResult fetchResult) {
        if (fetchResult == null) {
            return SearchPageAnalysis.of(SearchPageKind.INVALID_RESPONSE, null);
        }
        String finalUrl = fetchResult.finalUrl == null ? "" : fetchResult.finalUrl;
        String contentStr = fetchResult.contentStr == null ? "" : fetchResult.contentStr;
        if (finalUrl.contains("wappass.baidu.com/static/captcha") || contentStr.contains("百度安全验证")) {
            return SearchPageAnalysis.of(SearchPageKind.CAPTCHA, null);
        }
        if (finalUrl.contains("/acount/accessrestriction")) {
            return SearchPageAnalysis.of(SearchPageKind.ACCESS_RESTRICTION, null);
        }

        JsonObject pageData = parseSearchPageData(contentStr);
        if (pageData == null) {
            return SearchPageAnalysis.of(SearchPageKind.INVALID_RESPONSE, null);
        }

        JsonObject resultObject = pageData.has("result") && pageData.get("result").isJsonObject()
                ? pageData.getAsJsonObject("result")
                : null;
        String queryWord = normalizeSearchValue(readString(pageData, "queryWord"));
        String queryStr = normalizeSearchValue(readString(resultObject, "queryStr"));
        String title = extractHtmlTitle(contentStr);
        JsonArray resultList = resultObject == null ? null : resultObject.getAsJsonArray("resultList");
        JsonArray absorbed = resultObject == null ? null : resultObject.getAsJsonArray("absorbed");
        int resultListSize = resultList == null ? 0 : resultList.size();
        int absorbedSize = absorbed == null ? 0 : absorbed.size();
        boolean metaPresent = hasAnyNonBlank(
                readString(resultObject, "title"),
                readString(resultObject, "keywords"),
                readString(resultObject, "description")
        );
        String normalizedInput = normalizeSearchValue(companyName);

        if (queryWord.isEmpty() && resultListSize == 0 && title.isEmpty()) {
            return SearchPageAnalysis.of(SearchPageKind.ACCESS_RESTRICTION_SHELL, pageData);
        }
        if (normalizedInput.equals(queryWord) && resultListSize == 0 && absorbedSize > 0 && title.isEmpty()) {
            return SearchPageAnalysis.of(SearchPageKind.WEAK_RESULT_PAGE, pageData);
        }
        if (normalizedInput.equals(queryWord) && !normalizedInput.equals(queryStr)) {
            return SearchPageAnalysis.of(SearchPageKind.FAKE_RESULT_PAGE, pageData);
        }
        if (normalizedInput.equals(queryWord) && title.isEmpty()) {
            return SearchPageAnalysis.of(SearchPageKind.FAKE_RESULT_PAGE, pageData);
        }
        if (normalizedInput.equals(queryWord) && normalizedInput.equals(queryStr) && metaPresent) {
            return SearchPageAnalysis.of(SearchPageKind.REAL_PAGE, pageData);
        }
        return SearchPageAnalysis.of(SearchPageKind.INVALID_RESPONSE, pageData);
    }

    protected Map<String, String> buildSearchHeaders() {
        List<Map<String, String>> candidates = buildSearchHeadersCandidates();
        return candidates.isEmpty() ? buildSearchHeadersByCookie("") : candidates.get(0);
    }

    protected List<Map<String, String>> buildSearchHeadersCandidates() {
        List<Map<String, String>> candidates = new ArrayList<Map<String, String>>();
        String existingSessionCookie = buildExistingSessionSearchCookie();
        if (!existingSessionCookie.isEmpty()) {
            candidates.add(buildSearchHeadersByCookie(existingSessionCookie));
        }
        String mergedCookie = buildSearchCookie();
        if (!mergedCookie.isEmpty() && !mergedCookie.equals(existingSessionCookie)) {
            candidates.add(buildSearchHeadersByCookie(mergedCookie));
        }
        String minimalCookie = buildMinimalSearchCookie();
        if (!minimalCookie.isEmpty() && !minimalCookie.equals(mergedCookie) && !minimalCookie.equals(existingSessionCookie)) {
            candidates.add(buildSearchHeadersByCookie(minimalCookie));
        }
        if (candidates.isEmpty()) {
            candidates.add(buildSearchHeadersByCookie(""));
        }
        return candidates;
    }

    protected String buildExistingSessionSearchCookie() {
        if (!shouldReuseExistingBrowserSession()) {
            return "";
        }
        return buildDevToolsSearchCookie(getExistingBrowserSessionPort());
    }

    protected String buildDevToolsSearchCookie(int devToolsPort) {
        Map<String, String> browserCookieMap;
        try {
            browserCookieMap = CdpBrowserSession.readAllCookies(
                    devToolsPort, CdpTargetSelector.preferUrlContains(AIQICHA_TARGET_KEYWORD));
        } catch (Exception e) {
            if (debugMode) {
                e.printStackTrace();
            }
            return "";
        }
        if (browserCookieMap == null || browserCookieMap.isEmpty()) {
            return "";
        }

        Map<String, String> cookieMap = parseCookies(Aiqicha_Cookie);
        String browserBaiduid = browserCookieMap.get("BAIDUID");
        if (browserBaiduid != null && !browserBaiduid.trim().isEmpty()) {
            cookieMap.put("BAIDUID", browserBaiduid.trim());
        }
        String browserBduss = browserCookieMap.get("BDUSS");
        if (browserBduss != null && !browserBduss.trim().isEmpty()) {
            cookieMap.put("BDUSS", browserBduss.trim());
        }
        if (!cookieMap.containsKey("BAIDUID") || cookieMap.get("BAIDUID").trim().isEmpty()) {
            return "";
        }

        long now = System.currentTimeMillis();
        String abKey = buildCurrentAbKey(now);
        String abValue = buildCurrentAbValue(now);
        if (abKey.isEmpty() || abValue.isEmpty()) {
            return toCookieString(cookieMap);
        }
        removeAbCookies(cookieMap);
        cookieMap.put(abKey, abValue);
        return toCookieString(cookieMap);
    }

    protected boolean shouldReuseExistingBrowserSession() {
        return BrowserRuntimeConfig.isReuseExistingSessionEnabled();
    }

    protected int getExistingBrowserSessionPort() {
        return BrowserRuntimeConfig.getExistingSessionPort();
    }

    private Map<String, String> buildSearchHeadersByCookie(String cookie) {
        Map<String, String> searchHeaders = new LinkedHashMap<>();
        searchHeaders.put("Referer", "https://aiqicha.baidu.com");
        searchHeaders.put("Connection", "keep-alive");
        searchHeaders.put("User-Agent", SEARCH_PAGE_USER_AGENT);
        searchHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        searchHeaders.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        searchHeaders.put("Cache-Control", "max-age=0");
        searchHeaders.put("Upgrade-Insecure-Requests", "1");
        if (!cookie.isEmpty()) {
            searchHeaders.put("Cookie", cookie);
        }
        return searchHeaders;
    }

    private void applyWorkingHeaders(Map<String, String> searchHeaders) {
        if (searchHeaders == null || searchHeaders.isEmpty()) {
            return;
        }
        this.headers = new HashMap<String, String>(searchHeaders);
    }

    protected String buildSearchCookie() {
        if (Aiqicha_Cookie == null || Aiqicha_Cookie.trim().isEmpty()) {
            return "";
        }
        Map<String, String> cookieMap = parseCookies(Aiqicha_Cookie);
        long now = System.currentTimeMillis();
        String abKey = buildCurrentAbKey(now);
        String abValue = buildCurrentAbValue(now);
        if (abKey.isEmpty() || abValue.isEmpty()) {
            return Aiqicha_Cookie.trim();
        }
        removeAbCookies(cookieMap);
        cookieMap.put(abKey, abValue);
        return toCookieString(cookieMap);
    }

    protected String buildMinimalSearchCookie() {
        if (Aiqicha_Cookie == null || Aiqicha_Cookie.trim().isEmpty()) {
            return "";
        }
        Map<String, String> cookieMap = parseCookies(Aiqicha_Cookie);
        String baiduid = cookieMap.get("BAIDUID");
        if (baiduid == null || baiduid.trim().isEmpty()) {
            return "";
        }
        long now = System.currentTimeMillis();
        String abKey = buildCurrentAbKey(now);
        String abValue = buildCurrentAbValue(now);
        if (abKey.isEmpty() || abValue.isEmpty()) {
            return "BAIDUID=" + baiduid;
        }
        return "BAIDUID=" + baiduid + "; " + abKey + "=" + abValue;
    }

    protected String buildCurrentAbKey(long nowMillis) {
        long hourStartMillis = (nowMillis / 3600000L) * 3600000L;
        String hourStart = String.valueOf(hourStartMillis);
        return "ab" + hourStart.substring(0, Math.min(9, hourStart.length()));
    }

    protected String buildCurrentAbValue(long nowMillis) {
        try {
            String raw = StrUtils.md5(SEARCH_PAGE_USER_AGENT + "A110A") + nowMillis;
            if (raw.length() < 4) {
                return raw;
            }
            return new StringBuilder(raw.length())
                    .append(raw.charAt(0))
                    .append(raw.charAt(raw.length() - 2))
                    .append(raw, 2, raw.length() - 2)
                    .append(raw.charAt(1))
                    .append(raw.charAt(raw.length() - 1))
                    .toString();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, String> parseCookies(String cookieStr) {
        Map<String, String> cookieMap = new LinkedHashMap<>();
        if (cookieStr == null || cookieStr.trim().isEmpty()) {
            return cookieMap;
        }
        for (String part : cookieStr.split(";")) {
            String trimmed = part == null ? "" : part.trim();
            if (trimmed.isEmpty() || !trimmed.contains("=")) {
                continue;
            }
            String[] keyValue = trimmed.split("=", 2);
            cookieMap.put(keyValue[0].trim(), keyValue.length > 1 ? keyValue[1].trim() : "");
        }
        return cookieMap;
    }

    private void removeAbCookies(Map<String, String> cookieMap) {
        if (cookieMap == null || cookieMap.isEmpty()) {
            return;
        }
        Iterator<String> iterator = cookieMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key != null && AB_COOKIE_KEY_PATTERN.matcher(key).matches()) {
                iterator.remove();
            }
        }
    }

    private String toCookieString(Map<String, String> cookieMap) {
        if (cookieMap == null || cookieMap.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(key.trim()).append("=").append(value == null ? "" : value.trim());
        }
        return builder.toString();
    }

    private String extractHtmlTitle(String contentStr) {
        if (contentStr == null || contentStr.isEmpty()) {
            return "";
        }
        Matcher matcher = HTML_TITLE_PATTERN.matcher(contentStr);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String normalizeSearchValue(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasAnyNonBlank(String... values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String stripHighlightTags(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return EM_TAG_PATTERN.matcher(value).replaceAll("").trim();
    }

    private CdpBrowserSession attachAiqichaBrowserSession(int devToolsPort) throws Exception {
        return CdpBrowserSession.attach(devToolsPort, CdpTargetSelector.preferUrlContains(AIQICHA_TARGET_KEYWORD));
    }

    private SearchPageFetchResult toBrowserFetchResult(CdpPageSnapshot snapshot) {
        if (snapshot == null || snapshot.getHtml() == null || snapshot.getHtml().trim().isEmpty()) {
            return null;
        }
        return new SearchPageFetchResult(200, "", snapshot.getHtml(), snapshot.getUrl());
    }

    protected static class SearchPageFetchResult {
        private final int statusCode;
        private final String location;
        private final String contentStr;
        private final String finalUrl;

        protected SearchPageFetchResult(int statusCode, String location, String contentStr, String finalUrl) {
            this.statusCode = statusCode;
            this.location = location;
            this.contentStr = contentStr;
            this.finalUrl = finalUrl;
        }

        protected SearchPageFetchResult(int statusCode, String location, String contentStr) {
            this(statusCode, location, contentStr, "");
        }
    }

    protected enum SearchPageKind {
        CAPTCHA,
        ACCESS_RESTRICTION,
        ACCESS_RESTRICTION_SHELL,
        WEAK_RESULT_PAGE,
        FAKE_RESULT_PAGE,
        REAL_PAGE,
        INVALID_RESPONSE
    }

    protected static class SearchPageAnalysis {
        private final SearchPageKind kind;
        private final JsonObject pageData;

        private SearchPageAnalysis(SearchPageKind kind, JsonObject pageData) {
            this.kind = kind;
            this.pageData = pageData;
        }

        protected static SearchPageAnalysis of(SearchPageKind kind, JsonObject pageData) {
            return new SearchPageAnalysis(kind, pageData);
        }

        protected boolean shouldRetry() {
            return kind == SearchPageKind.ACCESS_RESTRICTION_SHELL
                    || kind == SearchPageKind.WEAK_RESULT_PAGE
                    || kind == SearchPageKind.FAKE_RESULT_PAGE
                    || kind == SearchPageKind.INVALID_RESPONSE;
        }
    }

    private List<CompanyCandidate> extractCandidates(JsonArray jsonArray) {
        List<CompanyCandidate> candidates = new ArrayList<>();
        if (jsonArray == null) {
            return candidates;
        }

        for (JsonElement jsonElement : jsonArray) {
            if (!jsonElement.isJsonObject()) {
                continue;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String name = readString(jsonObject, "titleName");
            if (name.isEmpty()) {
                name = stripHighlightTags(readString(jsonObject, "entName"));
            }
            if (name.isEmpty()) {
                name = stripHighlightTags(readString(jsonObject, "name"));
            }
            if (name.isEmpty()) {
                continue;
            }

            CompanyCandidate candidate = new CompanyCandidate();
            candidate.setCompanyName(name);
            candidate.setAiqichaPid(readString(jsonObject, "pid"));
            candidate.setCompanyStatus(readString(jsonObject, "openStatus"));
            if (candidate.getCompanyStatus() == null || candidate.getCompanyStatus().isEmpty()) {
                candidate.setCompanyStatus(readString(jsonObject, "status"));
            }
            candidate.setLegalRepresentative(readString(jsonObject, "legalPerson"));
            candidate.setRegisteredCapital(readString(jsonObject, "regCapital"));
            if (candidate.getRegisteredCapital() == null || candidate.getRegisteredCapital().isEmpty()) {
                candidate.setRegisteredCapital(readString(jsonObject, "regCap"));
            }
            candidate.setRegisteredTime(readString(jsonObject, "startDate"));
            if (candidate.getRegisteredTime() == null || candidate.getRegisteredTime().isEmpty()) {
                candidate.setRegisteredTime(readString(jsonObject, "validityFrom"));
            }
            candidate.addSource(CompanyCandidate.SOURCE_AIQICHA);
            candidates.add(candidate);
        }

        return candidates;
    }

    private String readString(JsonObject jsonObject, String key) {
        if (jsonObject == null || key == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return "";
        }
        try {
            return jsonObject.get(key).getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    public static void main(String[] args) {
        List<Integer> weightThresholdList = new ArrayList<>();
        weightThresholdList.add(100);
        weightThresholdList.add(100);
        System.out.println(new AiqichaSearch(weightThresholdList, null).getCompanyInfoIteration("北京搜狗信息服务有限公司"));

        System.out.println("java.net.useSystemProxies: " + System.getProperty("java.net.useSystemProxies"));
        System.out.println("HTTP Proxy Host: " + System.getProperty("http.proxyHost"));
        System.out.println("HTTPS Proxy Host: " + System.getProperty("https.proxyHost"));
        System.out.println("socksProxyHost: " + System.getProperty("socksProxyHost"));
        System.out.println("proxySelectorForHttps: " + System.getProperty("proxySelectorForHttps"));
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

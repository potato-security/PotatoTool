package com.potato.potatotool.content.redTeam.infoGathering;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potato.potatotool.content.redTeam.infoGathering.classObj.*;
import com.potato.potatotool.content.redTeam.infoGathering.infoAggregation.CipAggregator;
import com.potato.potatotool.content.redTeam.infoGathering.infoLeakage.gitLeakage.GitHubLeakage;
import com.potato.potatotool.content.redTeam.infoGathering.infoLeakage.searchEnginesLeakage.GoogleSearch;
import com.potato.potatotool.content.redTeam.infoGathering.subDomain.GetSubDomain;
import com.potato.potatotool.content.redTeam.infoGathering.subDomain.SubdomainBruteForcer;
import com.potato.potatotool.content.redTeam.infoGathering.tools.*;
import com.potato.potatotool.content.redTeam.infoGathering.utils.*;
import com.potato.potatotool.content.redTeam.infoGathering.tools.GetCompany;
import com.potato.potatotool.controller.redTeam.PaneInfoSearch;
import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.core.I18nTextUtils;
import com.potato.potatotool.utils.data.StrUtils;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.content.redTeam.infoGathering.cdn.CdnChecker.isCdnIp;
import static com.potato.potatotool.content.redTeam.infoGathering.tools.GetSeo.*;
import static com.potato.potatotool.content.redTeam.infoGathering.tools.GetDomain.*;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.CompanyCandidateUtils.findExactCandidate;
import static com.potato.potatotool.content.redTeam.infoGathering.utils.CompanyCandidateUtils.mergeCandidates;

/**
 * @author Potato
 * @date 2023/4/23 10:05
 *
 * 空间测绘主类
 */
public class AssetMapper {
    private PaneInfoSearch paneInfoSearch;
    private long searchToken;

    private String Fofa_Key;
    private Set<String> Hunter_Key;
    private Set<String> Quake_Key;
    private String Shodan_Key;
    private String Zoomeye_Key;

    private FofaSearch fofaSearch;
    private HunterSearch hunterSearch;
    private QuakeSearch quakeSearch;
    private ShodanSearch shodanSearch;
    private ZoomeyeSearch zoomeyeSearch;
    private GoogleSearch googleSearch;
    private GitHubLeakage gitHubLeakage;

    private AiqichaSearch aiqichaSearch;

    public void setController(PaneInfoSearch paneInfoSearch) {
        this.paneInfoSearch = paneInfoSearch;
    }

    public void setSearchToken(long searchToken) {
        this.searchToken = searchToken;
    }

    private boolean shouldStop() {
        return paneInfoSearch != null && paneInfoSearch.isSearchCancelled(searchToken);
    }

    private void updateProgress(String message, boolean isComplete, Map<String, Object> dataMap) {
        if (paneInfoSearch != null) {
            paneInfoSearch.updateEchoVBox(message, isComplete, dataMap);
        }
    }

    protected List<CompanyCandidate> fetchChinazCompanyCandidates(String companyName) {
        return GetCompany.searchCompanyCandidates_chinaz(companyName);
    }

    protected List<CompanyCandidate> fetchAiqichaCompanyCandidates(String companyName) {
        if (aiqichaSearch == null) {
            return new ArrayList<>();
        }
        return aiqichaSearch.searchCompanyCandidates(companyName);
    }

    protected Set<String> fetchAiFullCompanyNames(String currentQuery) {
        return AiUtils.getCompanyFullName_Ai(currentQuery);
    }

    protected Set<String> fetchAiCompanyAliases(String companyName) {
        return AiUtils.getCompanyName_Ai(companyName);
    }

    protected void showCompanyCandidateChoosePaneBox(List<CompanyCandidate> candidates, boolean isAiGenerated, String currentQuery) {
        if (paneInfoSearch != null) {
            paneInfoSearch.showCompanyCandidateChoosePaneBox(candidates, isAiGenerated, currentQuery);
        }
    }

    protected CompanyCandidateSelectionResult waitAndGetCompanyCandidateSelection() {
        if (paneInfoSearch == null) {
            return new CompanyCandidateSelectionResult(CompanyCandidateSelectionResult.Action.CANCEL, null);
        }
        return paneInfoSearch.waitAndGetCompanyCandidateSelection();
    }

    protected void showCompanyNameChoosePaneBox(Set<String> companyNamesSet) {
        if (paneInfoSearch != null) {
            paneInfoSearch.showCompanyNameChoosePaneBox(companyNamesSet);
        }
    }

    protected Set<String> waitAndGetCompanyNameSet() {
        if (paneInfoSearch == null) {
            return new LinkedHashSet<>();
        }
        return paneInfoSearch.waitAndGetCompanyNameSet();
    }

    protected void showTip(String message, boolean isSuccess) {
        if (paneInfoSearch != null) {
            paneInfoSearch.showTip(message, isSuccess);
        }
    }

    protected void dispatchCollectedSeeds(AssetObj assetObj, NetAssets netAssets, Set<String> companyNameSet, Set<String> domainSet, Set<String> icpNoSet) {
        getData(assetObj, netAssets, companyNameSet, domainSet, icpNoSet);
    }

    protected Map<String, Object> fetchInitialWebInfo(String baseUrl, int maxDepth, int maxSubPathCount, boolean crawlProxy) {
        return Utils.getWebInfo(baseUrl, false, false, maxDepth, maxSubPathCount, crawlProxy);
    }

    protected Map<String, Object> fetchFullWebInfo(String baseUrl, boolean hasCrawlLinks, boolean hasFindSensitiveInfo,
                                                   int maxDepth, int maxSubPathCount, boolean crawlProxy) {
        return Utils.getWebInfo(baseUrl, hasCrawlLinks, hasFindSensitiveInfo, maxDepth, maxSubPathCount, crawlProxy);
    }

    protected Set<String> resolveCompanyAliasesForCollection(AssetObj assetObj,
                                                             String currentCompanyName,
                                                             String originalInputCompanyName,
                                                             String confirmedCompanyName,
                                                             boolean directCompanyInput,
                                                             boolean manuallyConfirmed,
                                                             String aiProgressText,
                                                             String userProgressText,
                                                             String doneProgressText) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        aliases.add(currentCompanyName);
        if (!shouldCollectCompanyAliases(assetObj)) {
            return aliases;
        }

        updateProgress(aiProgressText, false, null);
        aliases.addAll(fetchAiCompanyAliases(currentCompanyName));
        addOriginalInputAliasIfNeeded(aliases, originalInputCompanyName, confirmedCompanyName,
                directCompanyInput, manuallyConfirmed, currentCompanyName);
        aliases = normalizeCompanyAliases(aliases, currentCompanyName);

        updateProgress(userProgressText, false, null);
        showCompanyNameChoosePaneBox(aliases);
        if (shouldStop()) {
            return aliases;
        }

        Set<String> selectedAliases = waitAndGetCompanyNameSet();
        if (shouldStop()) {
            return selectedAliases;
        }
        if (selectedAliases != null && !selectedAliases.isEmpty()) {
            aliases = normalizeCompanyAliases(selectedAliases, currentCompanyName);
        }

        updateProgress(doneProgressText, true, null);
        return aliases;
    }

    public void searchInfo(String input, AssetObj assetObj) {
        input = input.trim();
        if(input.isEmpty()) return;
        if (shouldStop()) return;
        // 网址提取域名/ip,剔除端口,剔除掩码
        input = standardFormat(input);

        Fofa_Key = assetObj.getFofa_Key();
        Hunter_Key = assetObj.getHunter_Key();
        Quake_Key = assetObj.getQuake_Key();
        Shodan_Key = assetObj.getShodan_Key();
        Zoomeye_Key = assetObj.getZoomeye_Key();

        fofaSearch = new FofaSearch(Fofa_Key, assetObj.isFofaProxy());
        hunterSearch = new HunterSearch(Hunter_Key, assetObj.isHunterProxy());
        quakeSearch = new QuakeSearch(Quake_Key, assetObj.isQuakeProxy());
        shodanSearch = new ShodanSearch(Shodan_Key, assetObj.isShodanProxy());
        zoomeyeSearch = new ZoomeyeSearch(Zoomeye_Key, assetObj.isZoomeyeProxy());
        googleSearch = new GoogleSearch(assetObj.getGoogle_API(), assetObj.isGoogleProxy());
        gitHubLeakage = new GitHubLeakage(assetObj.getGitHub_Token(), assetObj.isGithubProxy());

        aiqichaSearch = new AiqichaSearch(assetObj.getWeightThresholdList(), paneInfoSearch);

        if (Utils.isIPAddress(input)) {
            handleIPAddress(input, assetObj);
        } else if (Utils.isDomainName(input)) {
            handleDomainName(input, assetObj, null);
        } else {
            handleCompanyName(input, assetObj, null, null);
        }

        if (shouldStop()) return;
        paneInfoSearch.updateEchoVBox("导出报告中……", false, null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStr = sdf.format(new Date(System.currentTimeMillis()));
        String outXlsxFile = StrUtils.getCurrentJarDir() + File.separator + "AssetResult" + File.separator + input.replaceAll("[/\\\\:*?\"<>|]", "_") + "_" + timeStr +".xlsx";
        String error = new AssetExcelExporter().exportToExcel(assetObj, outXlsxFile);
        paneInfoSearch.updateEchoVBox(error==null ? "报告导出至:" + outXlsxFile : "导出失败_[Error]：" + error, true, null);
    }


    public void searchInfo_standard(String input, AssetObj assetObj) {
        input = input.trim();
        if(input.isEmpty()) return;
        if (shouldStop()) return;

        Fofa_Key = assetObj.getFofa_Key();
        Hunter_Key = assetObj.getHunter_Key();
        Quake_Key = assetObj.getQuake_Key();
        Shodan_Key = assetObj.getShodan_Key();
        Zoomeye_Key = assetObj.getZoomeye_Key();

        fofaSearch = new FofaSearch(Fofa_Key, assetObj.isFofaProxy());
        hunterSearch = new HunterSearch(Hunter_Key, assetObj.isHunterProxy());
        quakeSearch = new QuakeSearch(Quake_Key, assetObj.isQuakeProxy());
        shodanSearch = new ShodanSearch(Shodan_Key, assetObj.isShodanProxy());
        zoomeyeSearch = new ZoomeyeSearch(Zoomeye_Key, assetObj.isZoomeyeProxy());

        List<DomainInfo> mergedList = new ArrayList<>();
        if(!assetObj.isHasAssetKey()) {
            paneInfoSearch.updateEchoVBox("搜索语法：" + input +"，初始化数据-（未选择平台，仅支持输入纯域名/IP）", true, null);
            paneInfoSearch.updateEchoVBox("Amap检索：" + input, false, null);
            JsonObject briefExtendedInfo = fofaSearch.getBriefExtendedInfo_Fofa(input);
            mergedList = DomainInfoMerger.mergeDomainInfos("Amap检索：" + input, briefExtendedInfo);
            paneInfoSearch.updateEchoVBox("Amap检索：" + input, true, null);
        }else {
            paneInfoSearch.updateEchoVBox("搜索语法：" + input +"，初始化数据-（需输入对应平台语法）", true, null);
            paneInfoSearch.updateEchoVBox("平台检索：" + input, false, null);
            JsonArray info_Fofa = fofaSearch.search_Fofa(input);
            JsonArray info_Hunter = hunterSearch.search_Hunter(input);
            JsonArray info_Quake = quakeSearch.search_Quake(input);
            JsonArray info_Shodan = shodanSearch.search_Shodan(input);
            JsonArray info_Zoomeye = zoomeyeSearch.search_Zoomeye(input);
            paneInfoSearch.updateEchoVBox("平台检索：" + input, true, null);

            // 合并信息
            paneInfoSearch.updateEchoVBox("数据格式化", true, null);
            mergedList = DomainInfoMerger.mergeDomainInfos("空间测绘平台：" + input, info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
        }
        NetAssets netAssets = new NetAssets();
        netAssets.setCompanyName("-");
        netAssets.setDomainInfoList(mergedList);
        assetObj.addCompanyDomainInfoList(netAssets);
        paneInfoSearch.updateEchoVBox("查询结束", true, null);

        if (shouldStop()) return;
        paneInfoSearch.updateEchoVBox("导出报告中……", false, null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStr = sdf.format(new Date(System.currentTimeMillis()));
        String outXlsxFile = StrUtils.getCurrentJarDir() + File.separator + "AssetResult" + File.separator + input.replaceAll("[/\\\\:*?\"<>|]", "_") + "_" + timeStr +".xlsx";
        String error = new AssetExcelExporter().exportToExcel(assetObj, outXlsxFile);
        paneInfoSearch.updateEchoVBox(error==null ? "报告导出至:" + outXlsxFile : "导出失败_[Error]：" + error, true, null);
    }

    public static String standardFormat(String input) {
        if(input.toLowerCase(Locale.ROOT).startsWith("http")) {
            try {
                URL parsedUrl = new URL(input);
                String host = parsedUrl.getHost();
                return host; // 返回域名，不包含端口
            } catch (Exception e) {}
        }
        if(input.contains("/")) input = input.split("/")[0];
        if(input.contains(":")) input = input.split(":")[0];
        return input;
    }


    // 处理IP地址的逻辑
    private void handleIPAddress(String ip, AssetObj assetObj) {
        if (shouldStop()) return;
        paneInfoSearch.updateEchoVBox("传入IP地址_" + ip + "，初始化数据", true, null);

        if(isCdnIp(ip)){
            paneInfoSearch.updateEchoVBox("为CDN_Ip，跳过增量检索", true, null);
        }else {
            paneInfoSearch.updateEchoVBox("反查域名，并获取域名SEO信息", false, null);
            JsonArray domainList = getDomainByIp(ip, 5);
            paneInfoSearch.updateEchoVBox("选择域名_By_User", false, null);
            paneInfoSearch.showDomainChoosePaneBox(domainList);
            if (shouldStop()) return;
            JsonArray domainListChoose = paneInfoSearch.waitAndGetDomainSet();
            paneInfoSearch.updateEchoVBox("反查域名_Top5", true, null);
            if (shouldStop()) return;

            // 相同备案公司的域名不处理
            List<String> companyNameList = new ArrayList<>();
            if (domainListChoose != null && domainListChoose.size() > 0) {
                for (JsonElement jsonElement : domainListChoose) {
                    if (shouldStop()) return;
                    JsonObject domainJsonObject = jsonElement.getAsJsonObject();
                    String domain = domainJsonObject.get("domain").getAsString();
                    JsonObject hasSeoMap = domainJsonObject.getAsJsonObject("seoMap");
                    String tmpCompanyName = hasSeoMap.getAsJsonObject("备案信息").get("备案所属").toString();
                    if (!companyNameList.contains(tmpCompanyName)) {
                        companyNameList.add(tmpCompanyName);
                        handleDomainName(domain, assetObj, hasSeoMap);
                    }
                }
            }
        }

        // 单独检索当前传入的IP
        paneInfoSearch.updateEchoVBox("平台检索原始IP：" + ip, false, null);
        if (shouldStop()) return;

        // 获取不同平台的 IP 信息
        JsonArray info_Fofa = fofaSearch.getInfoByIp_Fofa(ip);
        JsonArray info_Hunter = hunterSearch.getInfoByIp_Hunter(ip);
        JsonArray info_Quake = quakeSearch.getInfoByIp_Quake(ip);
        JsonArray info_Shodan = shodanSearch.getInfoByIp_Shodan(ip);
        JsonArray info_Zoomeye = zoomeyeSearch.getInfoByIp_Zoomeye(ip);

        // 合并信息
        paneInfoSearch.updateEchoVBox("数据整合：" + ip, false, null);
        List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos("检索原始IP：" + ip, info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
        NetAssets netAssets = new NetAssets();
        netAssets.setCompanyName("-");
        netAssets.setDomainInfoList(mergedList);
        assetObj.addCompanyDomainInfoList(netAssets);

        paneInfoSearch.updateEchoVBox("平台检索原始IP", true, null);

        paneInfoSearch.updateEchoVBox("查询结束", true, null);
    }

    // 处理域名的逻辑
    private void handleDomainName(String domain, AssetObj assetObj, JsonObject hasSeoMap) {
        if (shouldStop()) return;
        if(hasSeoMap==null) paneInfoSearch.updateEchoVBox("传入域名_" + domain + "，初始化数据", true, null);
        // 初始化变量
        Set<String> icpNoSet = new HashSet<>();
        Set<String> tmpDomainSet = new HashSet<>(); // 存储初步检索的域名
        tmpDomainSet.add(domain);

        // 获取SEO信息
        JsonObject seoMap = new JsonObject();
        if(hasSeoMap != null) {
            seoMap = hasSeoMap;
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("type", DataTypeConstants.SEO);
            dataMap.put("data", seoMap);
            paneInfoSearch.updateEchoVBox("检索域名基础信息", true, dataMap);
        } else {
            paneInfoSearch.updateEchoVBox("检索域名基础信息", false, null);
            seoMap = getSeo(domain);
            assetObj.setSeoMap(seoMap);
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("type", DataTypeConstants.SEO);
            dataMap.put("data", seoMap);
            paneInfoSearch.updateEchoVBox("检索域名基础信息", true, dataMap);
        }
        String icpType = seoMap.getAsJsonObject("备案信息").get("备案性质").getAsString();
        String icpNoStr = seoMap.getAsJsonObject("备案信息").get("备案号").getAsString();
        String companyName = seoMap.getAsJsonObject("备案信息").get("备案所属").getAsString();
        icpNoSet.add(icpNoStr);
        if (shouldStop()) return;
        if(!icpType.isEmpty() && !icpType.equals("-") && !icpType.equals("个人") && !companyName.isEmpty() && !companyName.equals("-")){
            handleCompanyName(companyName, assetObj, icpNoSet, tmpDomainSet);
        }else {
            NetAssets netAssets = new NetAssets();
            netAssets.setCompanyName(companyName);
            getData(assetObj, netAssets, null, tmpDomainSet, icpNoSet);
        }

        if (hasSeoMap==null) paneInfoSearch.updateEchoVBox("查询结束", true, null);
    }

    /**
     *
     * @param assetObj          AssetObj对象
     * @param companyNameSet    企业名、别名集合
     * @param domainSet            域名
     * @param icpNoSet             icp备案号
     */
    private void getData(AssetObj assetObj, NetAssets netAssets, Set<String> companyNameSet, Set<String> domainSet, Set<String> icpNoSet){
        if (shouldStop()) return;
        // 初始化变量
        Set<String> tmpDomainSet = new HashSet<>(domainSet);
        List<DomainInfo> domainInfoList = new ArrayList<>();
        Set<String> tmpIcpNoSet = new HashSet<>(icpNoSet);
        netAssets.setDomainInfoList(domainInfoList);
        assetObj.addCompanyDomainInfoList(netAssets);

        // 提取配置信息
        boolean hasCrawlLinks = assetObj.isHasCrawlLinks();
        boolean hasFindSensitiveInfo = assetObj.isHasFindSensitiveInfo();
        int maxDepth = assetObj.getMaxDepth();
        int maxSubPathCount = assetObj.getMaxSubPathCount();
        int maxGoogleSearchCount = assetObj.getMaxGoogleSearchCount();
        int maxGithubSearchCount = assetObj.getMaxGithubSearchCount();
        boolean isHasAssetKey = assetObj.isHasAssetKey();
        boolean isUseGithub = assetObj.isUseGithub();
        boolean isUseGoogle = assetObj.isUseGoogle();
        boolean isSearchShadowAssets = assetObj.isSearchShadowAssets();
        boolean isHasCipAggregator = assetObj.isHasCipAggregator();
        boolean isHasLocalFullDetection = assetObj.isHasLocalFullDetection();
        boolean isHasIconSearch = assetObj.isHasIconSearch();
        boolean isSslProxy = assetObj.isSslProxy();
        int localFullDetectionThreshold = assetObj.getLocalFullDetectionThreshold();
        int shadowAssetsThreshold = assetObj.getShadowAssetsThreshold();
        int cipThreshold = assetObj.getCipThreshold();

        long workflowStartedAt = System.nanoTime();
        InfoGatheringStageRecorder stageRecorder = new InfoGatheringStageRecorder();
        String poolName = ExecutorServiceManager.ExecutorPoolNames.ASSET;
        ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
        List<CompletableFuture<?>> futures = new ArrayList<>();

        // 根据主域名获取子域
        int currentIndex = 0;
        if(assetObj.isSearchSslSubdomainBox()){
            int beforeDomainCount = tmpDomainSet.size();
            InfoGatheringStageRecorder.StageToken stageToken = stageRecorder.start("SSL子域扩展");
            int totalNum = domainSet.size();
            for(String domainStr : domainSet) {
                if (shouldStop()) return;
                currentIndex++;
                double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                String showText = String.format("正在查询子域名By_SSL [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, domainStr, progress);
                paneInfoSearch.updateEchoVBox(showText, false, null);
                tmpDomainSet.addAll(GetSubDomain.getSubByDomainOrDomainCert(domainStr, isSslProxy));
            }
            recordStage(stageRecorder, stageToken, Math.max(0, tmpDomainSet.size() - beforeDomainCount),
                    "主域名数：" + totalNum);
        }
        currentIndex = 0;
        if (assetObj.isBruteForceSubdomain()) {
            int beforeDomainCount = tmpDomainSet.size();
            InfoGatheringStageRecorder.StageToken stageToken = stageRecorder.start("爆破子域扩展");
            int totalNum = domainSet.size();
            for(String domainStr : domainSet) {
                if (shouldStop()) return;
                currentIndex++;
                double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                String showText = String.format("正在爆破子域名 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, domainStr, progress);
                paneInfoSearch.updateEchoVBox(showText, false, null);
                tmpDomainSet.addAll((SubdomainBruteForcer.getSubDomain(domainStr)));
            }
            recordStage(stageRecorder, stageToken, Math.max(0, tmpDomainSet.size() - beforeDomainCount),
                    "主域名数：" + totalNum);
        }
        paneInfoSearch.updateEchoVBox("获取子域名", true, null);


        if(!isHasAssetKey) {
            // amap官方接口已关闭，暂无免费接口可用
            paneInfoSearch.updateEchoVBox("Amap检索域名信息(官方接口已关闭)", true, null);
            if(false) {
                // 无任何平台Key时，调用amap
                paneInfoSearch.updateEchoVBox("Amap检索域名信息", false, null);

                int totalTasks = tmpDomainSet.size();
                AtomicInteger completedTasks = new AtomicInteger(0); // 已完成任务计数器

                executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
                futures = new ArrayList<>();
                for (String domainStr : tmpDomainSet) {
                    // 将每个任务提交到线程池
                    CompletableFuture<List<DomainInfo>> future = CompletableFuture.supplyAsync(() -> {
                        JsonObject briefExtendedInfo = fofaSearch.getBriefExtendedInfo_Fofa(domainStr);

                        int progress = completedTasks.incrementAndGet();
                        double percentage = ((progress - 0.5) * 100.0) / totalTasks;
                        String showText = String.format("正在检索域名信息 [%d/%d]：%s | 进度：%.2f%%",
                                progress, totalTasks, domainStr, percentage);
                        paneInfoSearch.updateEchoVBox(showText, false, null);
                        return DomainInfoMerger.mergeDomainInfos("Amap检索：" + domainStr, briefExtendedInfo);
                    }, executor);
                    futures.add(future);
                }

                // 等待所有任务完成并收集结果
                for (Future<?> future : futures) {
                    try {
                        domainInfoList.addAll((List<DomainInfo>) future.get());
                    } catch (CancellationException ce) {
                        continue;
                    } catch (Exception e) {
                        if (debugMode) e.printStackTrace();
                    }
                }
                // 停止所有线程
                ExecutorServiceManager.shutdownExecutor(poolName);

                paneInfoSearch.updateEchoVBox("Amap检索域名信息", true, null);
            }
        }else {
            // 平台查询原始域名  【平台api存在频率限制，不建议多线程调用】
            paneInfoSearch.updateEchoVBox("平台检索原始域名", false, null);

            currentIndex = 0;
            int totalNum = domainSet.size();
            int rawDomainResultBefore = domainInfoList.size();
            InfoGatheringStageRecorder.StageToken rawDomainStage = stageRecorder.start("平台检索原始域名");
            for (String domainStr : domainSet) {
                if (shouldStop()) return;
                currentIndex++;
                double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                String showText = String.format("正在检索原始域名_by_平台 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, domainStr, progress);
                paneInfoSearch.updateEchoVBox(showText, false, null);

                // 获取不同平台的域名信息
                JsonArray info_Fofa = fofaSearch.getInfoByCompanyOrDomain_Fofa(domainStr);
                JsonArray info_Hunter = hunterSearch.getInfoByCompanyOrDomain_Hunter(domainStr);
                JsonArray info_Quake = quakeSearch.getInfoByCompanyOrDomain_Quake(domainStr);
                JsonArray info_Shodan = shodanSearch.getInfoByDomain_Shodan(domainStr);
                JsonArray info_Zoomeye = zoomeyeSearch.getInfoByCompanyOrDomain_Zoomeye(domainStr);

                // 合并信息
                String showText_merge = String.format("数据整合 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, domainStr, progress);
                paneInfoSearch.updateEchoVBox(showText_merge, false, null);
                List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos("检索原始域名：" + domainStr, info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                domainInfoList.addAll(mergedList);           // 添加到总的域名信息列表

                String showText_update = String.format("更新相关域名列表及ICP列表 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, domainStr, progress);
                paneInfoSearch.updateEchoVBox(showText_update, false, null);
                // 更新域名列表和 ICP 列表
                for (DomainInfo domainInfo : mergedList) {
                    String icp = domainInfo.getIcp();
                    String tmpDomainStr = domainInfo.getDomain();
                    if (icp != null && !icp.equals("-") && !icp.isEmpty() && icp.length() > 10) {
                        tmpIcpNoSet.add(icp.replaceAll("\\s*-\\s*\\d+$", ""));
                    }
                    if (tmpDomainStr != null && !tmpDomainStr.isEmpty()) {
                        tmpDomainSet.add(tmpDomainStr.replace("*.", ""));
                    }
                }
            }

            paneInfoSearch.updateEchoVBox("平台检索原始域名", true, null);
            recordStage(stageRecorder, rawDomainStage, totalNum,
                    "新增资产：" + Math.max(0, domainInfoList.size() - rawDomainResultBefore));


            // icp查询
            paneInfoSearch.updateEchoVBox("平台检索ICP", false, null);
            currentIndex = 0;
            totalNum = tmpIcpNoSet.size();
            int icpResultBefore = domainInfoList.size();
            InfoGatheringStageRecorder.StageToken icpStage = stageRecorder.start("平台检索ICP备案");
            for (String icpNoStr : tmpIcpNoSet) {
                if (shouldStop()) return;
                currentIndex++;
                double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                String showText = String.format("正在检索ICP_by_平台 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, icpNoStr, progress);
                paneInfoSearch.updateEchoVBox(showText, false, null);

                // 获取不同平台的 ICP 信息
                JsonArray icpInfo_Fofa = fofaSearch.getInfoByIcpNo_Fofa(icpNoStr);
                JsonArray icpInfo_Hunter = hunterSearch.getInfoByIcpNo_Hunter(icpNoStr);
                JsonArray icpInfo_Quake = quakeSearch.getInfoByIcpNo_Quake(icpNoStr);

                // 合并信息
                String showText_merge = String.format("数据整合 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, icpNoStr, progress);
                paneInfoSearch.updateEchoVBox(showText_merge, false, null);
                List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos("检索ICP：" + icpNoStr, icpInfo_Fofa, icpInfo_Hunter, icpInfo_Quake);
                domainInfoList.addAll(mergedList);  // 添加到总的域名信息列表

                // 更新域名列表
                String showText_update = String.format("更新相关域名列表 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, icpNoStr, progress);
                paneInfoSearch.updateEchoVBox(showText_update, false, null);
                for (DomainInfo domainInfo : mergedList) {
                    String tmpDomainStr = domainInfo.getDomain();
                    if (tmpDomainStr != null && !tmpDomainStr.isEmpty()) {
                        tmpDomainSet.add(tmpDomainStr.replace("*.", ""));
                    }
                }
            }
            paneInfoSearch.updateEchoVBox("平台检索ICP", true, null);
            recordStage(stageRecorder, icpStage, totalNum,
                    "新增资产：" + Math.max(0, domainInfoList.size() - icpResultBefore));

            // 剔除cdnIp，对IP进行检索
            paneInfoSearch.updateEchoVBox("剔除CDN_Ip，平台检索IP", false, null);
            Set<String> oldIpSet = domainInfoList.stream()
                    .filter(domainInfo -> domainInfo.getIp()!= null &&!domainInfo.isCND())
                    .map(DomainInfo::getIp)
                    .collect(Collectors.toSet());
            currentIndex = 0;
            totalNum = oldIpSet.size();
            int firstIpResultBefore = domainInfoList.size();
            InfoGatheringStageRecorder.StageToken firstIpStage = stageRecorder.start("平台检索首批IP");
            for (String ip : oldIpSet) {
                if (shouldStop()) return;
                currentIndex++;
                double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                String showText = String.format("正在检索IP_by_平台 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, ip, progress);
                paneInfoSearch.updateEchoVBox(showText, false, null);

                // 获取不同平台的 IP 信息
                JsonArray info_Fofa = fofaSearch.getInfoByIp_Fofa(ip);
                JsonArray info_Hunter = hunterSearch.getInfoByIp_Hunter(ip);
                JsonArray info_Quake = quakeSearch.getInfoByIp_Quake(ip);
                JsonArray info_Shodan = shodanSearch.getInfoByIp_Shodan(ip);
                JsonArray info_Zoomeye = zoomeyeSearch.getInfoByIp_Zoomeye(ip);

                // 合并信息
                String showText_merge = String.format("数据整合 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, ip, progress);
                paneInfoSearch.updateEchoVBox(showText_merge, false, null);
                List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos("检索IP：" + ip, info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                domainInfoList.addAll(mergedList);  // 添加到总的域名信息列表
            }
            paneInfoSearch.updateEchoVBox("剔除CDN_Ip，平台检索IP", true, null);
            recordStage(stageRecorder, firstIpStage, totalNum,
                    "新增资产：" + Math.max(0, domainInfoList.size() - firstIpResultBefore));


            // 去重处理
            int beforeFirstDedup = domainInfoList.size();
            InfoGatheringStageRecorder.StageToken firstDedupStage = stageRecorder.start("第一波数据去重");
            paneInfoSearch.updateEchoVBox("第一波数据去重", false, null);
            domainInfoList = DomainInfoMerger.mergeDomainInfoList(domainInfoList);
            paneInfoSearch.updateEchoVBox("第一波数据去重", true, null);
            recordStage(stageRecorder, firstDedupStage, domainInfoList.size(),
                    "去重前：" + beforeFirstDedup + "，去重后：" + domainInfoList.size());

            List<DomainInfo> initialWebInfoTargets = getInitialWebInfoTargets(domainInfoList, isHasLocalFullDetection, localFullDetectionThreshold);
            InfoGatheringStageRecorder.StageToken initialWebStage = initialWebInfoTargets.isEmpty()
                    ? null
                    : stageRecorder.start("基础网页信息探测");
            performInitialWebInfoCollection(domainInfoList, assetObj, isHasLocalFullDetection, isHasIconSearch,
                    hasCrawlLinks, hasFindSensitiveInfo, maxDepth, maxSubPathCount,
                    localFullDetectionThreshold, poolName);
            if (initialWebStage != null) {
                recordStage(stageRecorder, initialWebStage, initialWebInfoTargets.size(),
                        requiresDeferredDeepCollection(hasCrawlLinks, hasFindSensitiveInfo)
                                ? "模式：基础探测"
                                : "模式：基础探测即最终结果");
            }
            List<DomainInfo> chooseWebInfoMapList = new ArrayList<>();
            if(isHasIconSearch) {
                paneInfoSearch.showIconChoosePaneBox(domainInfoList);
                paneInfoSearch.updateEchoVBox("用户选择企业相关icon", false, null);
                if (shouldStop()) return;
                chooseWebInfoMapList = paneInfoSearch.waitAndGetIconDomainInfoList();
                paneInfoSearch.updateEchoVBox("获取企业相关icon", true, null);

                Set<String> md5Record = ConcurrentHashMap.newKeySet();
                paneInfoSearch.updateEchoVBox("平台检索icon", false, null);
                InfoGatheringStageRecorder.StageToken iconStage = chooseWebInfoMapList.isEmpty()
                        ? null
                        : stageRecorder.start("平台检索Icon");

                currentIndex = 0;
                totalNum = chooseWebInfoMapList.size();
                for (DomainInfo domainInfo : chooseWebInfoMapList) {
                    if (shouldStop()) return;
                    currentIndex++;
                    double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                    String domainStr = domainInfo.getDomain();
                    if (domainStr == null || domainStr.isEmpty()) domainStr = domainInfo.getIp();
                    String showText = String.format("正在检索icon_by_平台 [%d/%d]：%s | 进度：%.2f%%",
                            currentIndex, totalNum, domainStr, progress);
                    paneInfoSearch.updateEchoVBox(showText, false, null);

                    Map<String, Object> webInfoMap = domainInfo.getWebInfoMap();
                    if (webInfoMap == null || !webInfoMap.containsKey("iconMd5") || webInfoMap.get("iconMd5") == null || webInfoMap.get("iconMd5").toString().isEmpty()) {
                        continue;
                    }

                    String iconMd5 = webInfoMap.get("iconMd5").toString();
                    if (!md5Record.add(iconMd5)) {
                        continue;
                    }

                    String iconMmh3 = webInfoMap.get("iconMmh3").toString();
                    JsonArray info_Fofa = fofaSearch.getInfoByIcon_Fofa(iconMmh3);
                    JsonArray info_Hunter = hunterSearch.getInfoByIcon_Hunter(iconMd5);
                    JsonArray info_Quake = quakeSearch.getInfoByIcon_Quake(iconMd5);
                    JsonArray info_Shodan = shodanSearch.getInfoByIcon_Shodan(iconMmh3);
                    JsonArray info_Zoomeye = zoomeyeSearch.getInfoByIcon_Zoomeye(iconMd5);

                    List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos("检索选中图标：" + iconMd5, info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                    domainInfoList.addAll(mergedList);
                }
                paneInfoSearch.updateEchoVBox("平台检索icon", true, null);
                if (iconStage != null) {
                    recordStage(stageRecorder, iconStage, chooseWebInfoMapList.size(),
                            "唯一Icon查询数：" + md5Record.size());
                }
            }
            // 去重处理
            paneInfoSearch.updateEchoVBox("第二波数据去重", false, null);
            domainInfoList = DomainInfoMerger.mergeDomainInfoList(domainInfoList);
            paneInfoSearch.updateEchoVBox("第二波数据去重", true, null);

            // 开始检索影子资产
            if(isSearchShadowAssets){
                InfoGatheringStageRecorder.StageToken shadowStage = stageRecorder.start("影子资产检索");
                int recalledShadowCount = 0;
                int uniqueShadowCount = 0;
                int acceptedShadowCount = 0;
                paneInfoSearch.updateEchoVBox("检索影子资产", false, null);
                if(companyNameSet!=null && companyNameSet.size()>0) {
                    List<ShadowAssetCandidate> shadowCandidates = recallShadowAssetCandidates(companyNameSet);
                    recalledShadowCount = shadowCandidates.size();
                    paneInfoSearch.updateEchoVBox("影子资产召回候选：" + shadowCandidates.size(), true, null);

                    List<ShadowAssetCandidate> uniqueShadowCandidates = filterNewShadowAssetCandidates(domainInfoList, shadowCandidates);
                    uniqueShadowCount = uniqueShadowCandidates.size();
                    paneInfoSearch.updateEchoVBox("影子资产候选去重后：" + uniqueShadowCandidates.size(), true, null);

                    if (!uniqueShadowCandidates.isEmpty()) {
                        uniqueShadowCandidates.sort(new Comparator<ShadowAssetCandidate>() {
                            @Override
                            public int compare(ShadowAssetCandidate left, ShadowAssetCandidate right) {
                                int aliasCompare = Integer.compare(right.getRecalledAliases().size(), left.getRecalledAliases().size());
                                if (aliasCompare != 0) {
                                    return aliasCompare;
                                }
                                return Integer.compare(getDataSourceCount(right.getDomainInfo().getDataSource()),
                                        getDataSourceCount(left.getDomainInfo().getDataSource()));
                            }
                        });

                        int evaluateLimit = Math.min(getShadowAssetEvaluateLimit(shadowAssetsThreshold), uniqueShadowCandidates.size());
                        if (evaluateLimit < uniqueShadowCandidates.size()) {
                            uniqueShadowCandidates = new ArrayList<>(uniqueShadowCandidates.subList(0, evaluateLimit));
                        }
                        paneInfoSearch.updateEchoVBox("影子资产进入评估：" + uniqueShadowCandidates.size(), true, null);

                        ShadowAssetContext shadowAssetContext = buildShadowAssetContext(netAssets.getCompanyName(),
                                companyNameSet, tmpDomainSet, tmpIcpNoSet, chooseWebInfoMapList);
                        ShadowAssetEvaluator shadowAssetEvaluator = new ShadowAssetEvaluator(assetObj.isCrawlProxy());
                        List<DomainInfo> acceptedShadowDomains = evaluateShadowAssetCandidates(uniqueShadowCandidates, shadowAssetContext, shadowAssetEvaluator);
                        acceptedShadowDomains.sort(new Comparator<DomainInfo>() {
                            @Override
                            public int compare(DomainInfo left, DomainInfo right) {
                                Integer leftScore = left.getShadowScore() == null ? Integer.MIN_VALUE : left.getShadowScore();
                                Integer rightScore = right.getShadowScore() == null ? Integer.MIN_VALUE : right.getShadowScore();
                                return Integer.compare(rightScore, leftScore);
                            }
                        });

                        if (acceptedShadowDomains.size() > shadowAssetsThreshold) {
                            acceptedShadowDomains = new ArrayList<>(acceptedShadowDomains.subList(0, shadowAssetsThreshold));
                        }
                        acceptedShadowDomains = DomainInfoMerger.mergeDomainInfoList(acceptedShadowDomains);
                        acceptedShadowCount = acceptedShadowDomains.size();
                        Map<String, Object> shadowDataMap = null;
                        if (!acceptedShadowDomains.isEmpty()) {
                            shadowDataMap = new HashMap<>();
                            shadowDataMap.put("type", DataTypeConstants.SHADOW);
                            shadowDataMap.put("data", acceptedShadowDomains);
                        }
                        paneInfoSearch.updateEchoVBox("影子资产最终确认：" + acceptedShadowDomains.size(), true, shadowDataMap);
                        domainInfoList.addAll(acceptedShadowDomains);
                    }
                }
                paneInfoSearch.updateEchoVBox("检索影子资产", true, null);
                String shadowSummary = companyNameSet == null || companyNameSet.isEmpty()
                        ? "无企业名称集合，跳过"
                        : "召回：" + recalledShadowCount + "，去重后：" + uniqueShadowCount + "，确认：" + acceptedShadowCount;
                recordStage(stageRecorder, shadowStage, acceptedShadowCount, shadowSummary);
            }

            // 新IP集合
            paneInfoSearch.updateEchoVBox("梳理新增IP项", false, null);
            Set<String> newIpSet = domainInfoList.stream()
                    .filter(domainInfo -> domainInfo.getIp()!= null &&!domainInfo.isCND())
                    .map(DomainInfo::getIp)
                    .collect(Collectors.toSet());
            // 获取未进行IP检索的集合
            Set<String> finalOldIpSet = oldIpSet;
            Set<String> addedIps = newIpSet.stream()
                    .filter(ip ->!finalOldIpSet.contains(ip))
                    .collect(Collectors.toSet());
            if(addedIps.size()>0) paneInfoSearch.updateEchoVBox("第二波平台检索IP", false, null);

            currentIndex = 0;
            totalNum = addedIps.size();
            int secondIpResultBefore = domainInfoList.size();
            InfoGatheringStageRecorder.StageToken secondIpStage = addedIps.isEmpty()
                    ? null
                    : stageRecorder.start("平台检索新增IP");
            for (String ip : addedIps) {
                if (shouldStop()) return;
                currentIndex++;
                double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                String showText = String.format("正在检索IP_by_平台 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, ip, progress);
                paneInfoSearch.updateEchoVBox(showText, false, null);

                // 获取不同平台的 IP 信息
                JsonArray info_Fofa = fofaSearch.getInfoByIp_Fofa(ip);
                JsonArray info_Hunter = hunterSearch.getInfoByIp_Hunter(ip);
                JsonArray info_Quake = quakeSearch.getInfoByIp_Quake(ip);
                JsonArray info_Shodan = shodanSearch.getInfoByIp_Shodan(ip);
                JsonArray info_Zoomeye = zoomeyeSearch.getInfoByIp_Zoomeye(ip);

                // 合并信息
                String showText_merge = String.format("数据整合 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, ip, progress);
                paneInfoSearch.updateEchoVBox(showText_merge, false, null);
                List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos("检索IP：" + ip, info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                domainInfoList.addAll(mergedList);  // 添加到总的域名信息列表
            }
            if(addedIps.size()>0) {
                paneInfoSearch.updateEchoVBox("第二波平台检索IP", true, null);
                recordStage(stageRecorder, secondIpStage, addedIps.size(),
                        "新增资产：" + Math.max(0, domainInfoList.size() - secondIpResultBefore));
            }


            // 剔除cdnIp，进行C段聚合
            if(isHasCipAggregator) {
                InfoGatheringStageRecorder.StageToken cipAggregateStage = stageRecorder.start("C段聚合");
                paneInfoSearch.updateEchoVBox("剔除CDN_Ip，C段聚合", false, null);
                Map<String, Integer> getFrequentCSegments = CipAggregator.getFrequentCSegments(newIpSet, cipThreshold);
                paneInfoSearch.updateEchoVBox("剔除CDN_Ip，C段聚合", true, null);
                recordStage(stageRecorder, cipAggregateStage, getFrequentCSegments.size(),
                        "阈值：" + cipThreshold);

                currentIndex = 0;
                totalNum = getFrequentCSegments.size();
                if (getFrequentCSegments.size() > 0) {
                    int cipResultBefore = domainInfoList.size();
                    InfoGatheringStageRecorder.StageToken cipSearchStage = stageRecorder.start("平台检索C段");
                    paneInfoSearch.updateEchoVBox("平台检索C段", false, null);
                    for (String ip : getFrequentCSegments.keySet()) {
                        if (shouldStop()) return;
                        // 提交每个 IP 查询任务
                        currentIndex++;
                        double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                        String showText = String.format("正在检索C段_by_平台 [%d/%d]：%s | 进度：%.2f%%",
                                currentIndex, totalNum, ip, progress);
                        paneInfoSearch.updateEchoVBox(showText, false, null);

                        // 获取不同平台的 C段 信息
                        JsonArray info_Fofa = fofaSearch.getInfoByIp_Fofa(ip);
                        JsonArray info_Hunter = hunterSearch.getInfoByIp_Hunter(ip);
                        JsonArray info_Quake = quakeSearch.getInfoByIp_Quake(ip);
                        JsonArray info_Shodan = shodanSearch.getInfoByIp_Shodan(ip);
                        JsonArray info_Zoomeye = zoomeyeSearch.getInfoByIp_Zoomeye(ip);

                        // 合并信息
                        String showText_merge = String.format("数据整合 [%d/%d]：%s | 进度：%.2f%%",
                                currentIndex, totalNum, ip, progress);
                        paneInfoSearch.updateEchoVBox(showText_merge, false, null);
                        List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos("检索C段", info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                        domainInfoList.addAll(mergedList);  // 添加到总的域名信息列表
                    }
                    paneInfoSearch.updateEchoVBox("平台检索C段", true, null);
                    recordStage(stageRecorder, cipSearchStage, getFrequentCSegments.size(),
                            "新增资产：" + Math.max(0, domainInfoList.size() - cipResultBefore));
                }
            }
        }

        // 去重
        int beforeThirdDedup = domainInfoList.size();
        InfoGatheringStageRecorder.StageToken thirdDedupStage = stageRecorder.start("第三波数据去重");
        paneInfoSearch.updateEchoVBox("第三波数据去重", false, null);
        domainInfoList = DomainInfoMerger.mergeDomainInfoList(domainInfoList);
        paneInfoSearch.updateEchoVBox("第三波数据去重", true, null);
        recordStage(stageRecorder, thirdDedupStage, domainInfoList.size(),
                "去重前：" + beforeThirdDedup + "，去重后：" + domainInfoList.size());


        // 使用线程安全的集合来存储已处理过的域名信息
        if(isUseGoogle) {
            InfoGatheringStageRecorder.StageToken googleStage = stageRecorder.start("检索Google信息泄露");
            paneInfoSearch.updateEchoVBox("检索Google信息泄露", false, null);
            ConcurrentMap<String, DoDomainInfo> doGoogleDomainInfoMap = new ConcurrentHashMap<>();

            int totalTasks = domainInfoList.size();
            AtomicInteger completedTasks = new AtomicInteger(0);
            executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
            futures = new ArrayList<>();
            for (DomainInfo domainInfo : domainInfoList) {
                if (shouldStop()) break;
                CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                    if (shouldStop()) return null;
                    String domainStr = domainInfo.getDomain();
                    if (domainStr != null && !domainStr.isEmpty()) {
                        // 使用 computeIfAbsent 避免重复查询
                        DoDomainInfo doDomainInfo = doGoogleDomainInfoMap.computeIfAbsent(domainStr, domain -> {
                            DoDomainInfo newDoDomainInfo = new DoDomainInfo();
                            newDoDomainInfo.setDomain(domain);

                            JsonArray googleLeakage = googleSearch.searchLeakageByDomain(domain, assetObj.isCrawlProxy(), maxGoogleSearchCount);
                            newDoDomainInfo.setGoogldLeakage(googleLeakage);

                            return newDoDomainInfo;
                        });

                        domainInfo.setGoogldLeakage(doDomainInfo.getGoogldLeakage());
                    }

                    int progress = completedTasks.incrementAndGet();
                    double percentage = ((progress - 0.5) * 100.0) / totalTasks;
                    String showText = String.format("正在检索Google信息泄露 [%d/%d]：%s | 进度：%.2f%%",
                            progress, totalTasks , domainStr, percentage);
                    paneInfoSearch.updateEchoVBox(showText, false, null);
                    return null;
                }, executor);

                // 添加到 futures 列表，稍后等待所有任务完成
                futures.add(future);
            }
            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get(); // 阻塞直到任务完成
                } catch (CancellationException ce) {
                } catch (Exception e) {
                    if (debugMode) e.printStackTrace();
                }
            }
            // 停止所有线程
            ExecutorServiceManager.shutdownExecutor(poolName);

            paneInfoSearch.updateEchoVBox("检索Google信息泄露", true, null);
            recordStage(stageRecorder, googleStage, domainInfoList.size(),
                    "唯一域名：" + doGoogleDomainInfoMap.size());
        }

        if(isUseGithub) {
            InfoGatheringStageRecorder.StageToken githubStage = stageRecorder.start("检索Github信息泄露");
            paneInfoSearch.updateEchoVBox("检索Github信息泄露", false, null);
            // 使用线程安全的集合来存储已处理过的域名信息
            ConcurrentMap<String, DoDomainInfo> doGitDomainInfoMap = new ConcurrentHashMap<>();

            int totalTasks = domainInfoList.size();
            AtomicInteger completedTasks = new AtomicInteger(0);
            executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
            futures = new ArrayList<>();
            for (DomainInfo domainInfo : domainInfoList) {
                if (shouldStop()) break;
                CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
                    if (shouldStop()) return null;
                    String domainStr = domainInfo.getDomain();
                    if (domainStr != null && !domainStr.isEmpty()) {
                        // 使用 computeIfAbsent 避免重复查询
                        DoDomainInfo doDomainInfo = doGitDomainInfoMap.computeIfAbsent(domainStr, domain -> {
                            DoDomainInfo newDoDomainInfo = new DoDomainInfo();
                            newDoDomainInfo.setDomain(domain);

                            JsonArray gitRepoLeakage = gitHubLeakage.getRepo(companyNameSet, domain, maxGithubSearchCount);
                            newDoDomainInfo.setGitRepoLeakage(gitRepoLeakage);

                            return newDoDomainInfo;
                        });

                        domainInfo.setGitRepoLeakage(doDomainInfo.getGitRepoLeakage());
                    }

                    int progress = completedTasks.incrementAndGet();
                    double percentage = ((progress - 0.5) * 100.0) / totalTasks;
                    String showText = String.format("正在检索Github信息泄露 [%d/%d]：%s | 进度：%.2f%%",
                            progress, totalTasks , domainStr, percentage);
                    paneInfoSearch.updateEchoVBox(showText, false, null);
                    return null;
                }, executor);

                // 添加到 futures 列表，稍后等待所有任务完成
                futures.add(future);
            }
            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get(); // 阻塞直到任务完成
                } catch (CancellationException ce) {
                } catch (Exception e) {
                    if (debugMode) e.printStackTrace();
                }
            }
            // 停止所有线程
            ExecutorServiceManager.shutdownExecutor(poolName);

            paneInfoSearch.updateEchoVBox("检索Github信息泄露", true, null);
            recordStage(stageRecorder, githubStage, domainInfoList.size(),
                    "唯一域名：" + doGitDomainInfoMap.size());
        }

        List<DomainInfo> finalWebInfoTargets = getFinalWebInfoTargets(domainInfoList, isHasLocalFullDetection, localFullDetectionThreshold);
        InfoGatheringStageRecorder.StageToken finalWebStage = finalWebInfoTargets.isEmpty()
                ? null
                : stageRecorder.start("最终深度信息探测");
        performFinalWebInfoCollection(domainInfoList, assetObj, isHasLocalFullDetection, isHasIconSearch,
                hasCrawlLinks, hasFindSensitiveInfo, maxDepth, maxSubPathCount,
                localFullDetectionThreshold, poolName);
        if (finalWebStage != null) {
            recordStage(stageRecorder, finalWebStage, finalWebInfoTargets.size(),
                    "模式：深度探测补齐");
        }

        stageRecorder.addMetric("总流程", Math.max(0L, (System.nanoTime() - workflowStartedAt) / 1_000_000L),
                domainInfoList.size(), "最终资产数：" + domainInfoList.size());
        emitStageSummary(netAssets, stageRecorder);
    }

    public String getBaseUrl(DomainInfo domainInfo){
        String baseUrl = domainInfo.getDomain();
        String port = domainInfo.getPort();
        if (baseUrl == null || baseUrl.isEmpty()){
            baseUrl = domainInfo.getIp();
        }
        if (port != null && !port.isEmpty()) {
            baseUrl += ":" + port;
        }
        if(baseUrl.startsWith(":")) return null;
        return baseUrl;
    }

    private void performInitialWebInfoCollection(List<DomainInfo> domainInfoList, AssetObj assetObj,
                                                 boolean isHasLocalFullDetection, boolean isHasIconSearch,
                                                 boolean hasCrawlLinks, boolean hasFindSensitiveInfo,
                                                 int maxDepth, int maxSubPathCount,
                                                 int localFullDetectionThreshold, String poolName) {
        if ((!isHasLocalFullDetection && !isHasIconSearch) || domainInfoList == null || domainInfoList.isEmpty()) {
            return;
        }

        final boolean requiresDeferredDeepCollection = requiresDeferredDeepCollection(hasCrawlLinks, hasFindSensitiveInfo);
        final List<DomainInfo> collectionTargets = getInitialWebInfoTargets(domainInfoList, isHasLocalFullDetection, localFullDetectionThreshold);
        if (collectionTargets.isEmpty()) {
            return;
        }

        updateProgress("第一波基础网页信息探测", false, null);
        final int totalTasks = collectionTargets.size();
        final AtomicInteger completedTasks = new AtomicInteger(0);
        ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (final DomainInfo domainInfo : collectionTargets) {
            if (shouldStop()) break;
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                String domainStr = domainInfo.getDomain();
                if (domainStr == null || domainStr.isEmpty()) domainStr = domainInfo.getIp();

                String baseUrl = getBaseUrl(domainInfo);
                if (baseUrl != null && !baseUrl.isEmpty()) {
                    domainInfo.setWebInfoMap(fetchInitialWebInfo(baseUrl, maxDepth, maxSubPathCount, assetObj.isCrawlProxy()));
                }
                domainInfo.setDoWebInfoMap(true);
                domainInfo.setDoFullWebInfoMap(!requiresDeferredDeepCollection);

                int progress = completedTasks.incrementAndGet();
                double percentage = ((progress - 0.5) * 100.0) / totalTasks;
                String showText = String.format("正在基础网页信息探测 [%d/%d]：%s | 进度：%.2f%%",
                        progress, totalTasks, domainStr, percentage);
                updateProgress(showText, false, null);
                return null;
            }, executor);
            futures.add(future);
        }
        waitForFuturesAndShutdown(futures, poolName);
        updateProgress("第一波基础网页信息探测", true, null);
    }

    private void performFinalWebInfoCollection(List<DomainInfo> domainInfoList, AssetObj assetObj,
                                               boolean isHasLocalFullDetection, boolean isHasIconSearch,
                                               boolean hasCrawlLinks, boolean hasFindSensitiveInfo,
                                               int maxDepth, int maxSubPathCount,
                                               int localFullDetectionThreshold, String poolName) {
        if (!requiresDeferredDeepCollection(hasCrawlLinks, hasFindSensitiveInfo)
                || (!isHasLocalFullDetection && !isHasIconSearch)
                || domainInfoList == null || domainInfoList.isEmpty()) {
            return;
        }

        final List<DomainInfo> collectionTargets = getFinalWebInfoTargets(domainInfoList, isHasLocalFullDetection, localFullDetectionThreshold);
        if (collectionTargets.isEmpty()) {
            return;
        }

        updateProgress("第二波最终深度信息探测", false, null);
        final int totalTasks = collectionTargets.size();
        final AtomicInteger completedTasks = new AtomicInteger(0);
        ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (final DomainInfo domainInfo : collectionTargets) {
            if (shouldStop()) break;
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                if (shouldStop()) return null;
                String domainStr = domainInfo.getDomain();
                if (domainStr == null || domainStr.isEmpty()) domainStr = domainInfo.getIp();

                if (!domainInfo.isDoFullWebInfoMap()) {
                    String baseUrl = getBaseUrl(domainInfo);
                    if (baseUrl != null && !baseUrl.isEmpty()) {
                        domainInfo.setWebInfoMap(fetchFullWebInfo(baseUrl, hasCrawlLinks, hasFindSensitiveInfo,
                                maxDepth, maxSubPathCount, assetObj.isCrawlProxy()));
                    }
                    domainInfo.setDoWebInfoMap(true);
                    domainInfo.setDoFullWebInfoMap(true);
                }

                int progress = completedTasks.incrementAndGet();
                double percentage = ((progress - 0.5) * 100.0) / totalTasks;
                String showText = String.format("正在最终深度信息探测 [%d/%d]：%s | 进度：%.2f%%",
                        progress, totalTasks, domainStr, percentage);
                updateProgress(showText, false, null);
                return null;
            }, executor);
            futures.add(future);
        }
        waitForFuturesAndShutdown(futures, poolName);
        updateProgress("第二波最终深度信息探测", true, null);
    }

    private List<DomainInfo> getInitialWebInfoTargets(List<DomainInfo> domainInfoList, boolean isHasLocalFullDetection,
                                                      int localFullDetectionThreshold) {
        if (isHasLocalFullDetection) {
            return new ArrayList<>(domainInfoList);
        }
        return limitDomainInfoTargets(domainInfoList, localFullDetectionThreshold);
    }

    private List<DomainInfo> getFinalWebInfoTargets(List<DomainInfo> domainInfoList, boolean isHasLocalFullDetection,
                                                    int localFullDetectionThreshold) {
        if (isHasLocalFullDetection) {
            return new ArrayList<>(domainInfoList);
        }
        return limitDomainInfoTargets(domainInfoList, localFullDetectionThreshold);
    }

    private List<DomainInfo> limitDomainInfoTargets(List<DomainInfo> domainInfoList, int limit) {
        if (domainInfoList == null || domainInfoList.isEmpty() || limit <= 0) {
            return new ArrayList<>();
        }
        int maxSize = Math.min(domainInfoList.size(), limit);
        return new ArrayList<>(domainInfoList.subList(0, maxSize));
    }

    private boolean requiresDeferredDeepCollection(boolean hasCrawlLinks, boolean hasFindSensitiveInfo) {
        return hasCrawlLinks || hasFindSensitiveInfo;
    }

    private void waitForFuturesAndShutdown(List<CompletableFuture<?>> futures, String poolName) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (CancellationException ce) {
            } catch (Exception e) {
                if (debugMode) e.printStackTrace();
            }
        }
        ExecutorServiceManager.shutdownExecutor(poolName);
    }

    private void recordStage(InfoGatheringStageRecorder stageRecorder, InfoGatheringStageRecorder.StageToken stageToken,
                             int itemCount, String summary) {
        stageRecorder.finish(stageToken, itemCount, summary);
    }

    private void emitStageSummary(NetAssets netAssets, InfoGatheringStageRecorder stageRecorder) {
        if (stageRecorder == null || stageRecorder.isEmpty()) {
            return;
        }
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("type", DataTypeConstants.STAGE_SUMMARY);
        dataMap.put("data", stageRecorder.snapshot());
        updateProgress(buildStageSummaryMessage(netAssets), true, dataMap);
    }

    private String buildStageSummaryMessage(NetAssets netAssets) {
        String companyName = netAssets == null ? null : netAssets.getCompanyName();
        if (companyName == null || companyName.trim().isEmpty()) {
            companyName = "-";
        }
        return "信息收集阶段统计：" + companyName;
    }

    private List<ShadowAssetCandidate> recallShadowAssetCandidates(Set<String> companyNameSet) {
        Map<String, ShadowAssetCandidate> candidateMap = new LinkedHashMap<>();
        List<String> orderedCompanyNames = new ArrayList<>(companyNameSet);
        orderedCompanyNames.sort(new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                return Integer.compare(right.length(), left.length());
            }
        });

        int currentIndex = 0;
        int totalNum = orderedCompanyNames.size();
        for (String companyNameStr : orderedCompanyNames) {
            if (shouldStop()) {
                return new ArrayList<>();
            }
            currentIndex++;
            double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
            String showText = String.format("正在泛型检索 [%d/%d]：%s | 进度：%.2f%%",
                    currentIndex, totalNum, companyNameStr, progress);
            String showTextMerge = String.format("影子资产数据整合 [%d/%d]：%s | 进度：%.2f%%",
                    currentIndex, totalNum, companyNameStr, progress);
            updateProgress(showText, false, null);

            JsonArray info_Fofa = fofaSearch.getInfoByBodyFilterIcp_Fofa(companyNameStr);
            JsonArray info_Hunter = hunterSearch.getInfoByBodyFilterIcp_Hunter(companyNameStr);
            JsonArray info_Quake = quakeSearch.getInfoByBodyFilteIcp_Quake(companyNameStr);
            JsonArray info_Shodan = shodanSearch.getInfoByBodyFilterIcp_shodan(companyNameStr);
            JsonArray info_Zoomeye = zoomeyeSearch.getInfoByBodyFilterIcp_zoomeye(companyNameStr);

            updateProgress(showTextMerge, false, null);
            List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos("泛型检索：" + companyNameStr,
                    info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
            mergedList = DomainInfoMerger.mergeDomainInfoList(mergedList);
            for (DomainInfo domainInfo : mergedList) {
                addOrMergeShadowCandidate(candidateMap, domainInfo, companyNameStr);
            }
        }
        return new ArrayList<>(candidateMap.values());
    }

    private void addOrMergeShadowCandidate(Map<String, ShadowAssetCandidate> candidateMap, DomainInfo domainInfo, String recalledAlias) {
        if (domainInfo == null) {
            return;
        }

        Map.Entry<String, ShadowAssetCandidate> existingEntry = findExistingShadowCandidate(candidateMap, domainInfo);
        if (existingEntry != null) {
            List<DomainInfo> mergeList = new ArrayList<>();
            mergeList.add(existingEntry.getValue().getDomainInfo());
            mergeList.add(domainInfo);
            DomainInfoMerger.mergeDomainInfoList(mergeList);
            existingEntry.getValue().setDomainInfo(mergeList.get(0));
            existingEntry.getValue().addRecalledAlias(recalledAlias);
            return;
        }

        ShadowAssetCandidate candidate = new ShadowAssetCandidate(domainInfo);
        candidate.addRecalledAlias(recalledAlias);
        candidateMap.put(buildShadowCandidateKey(domainInfo), candidate);
    }

    private Map.Entry<String, ShadowAssetCandidate> findExistingShadowCandidate(Map<String, ShadowAssetCandidate> candidateMap, DomainInfo domainInfo) {
        String candidateKey = buildShadowCandidateKey(domainInfo);
        ShadowAssetCandidate directMatch = candidateMap.get(candidateKey);
        if (directMatch != null) {
            return new AbstractMap.SimpleEntry<>(candidateKey, directMatch);
        }

        for (Map.Entry<String, ShadowAssetCandidate> entry : candidateMap.entrySet()) {
            if (isSameShadowCandidate(entry.getValue().getDomainInfo(), domainInfo)) {
                return entry;
            }
        }
        return null;
    }

    private List<ShadowAssetCandidate> filterNewShadowAssetCandidates(List<DomainInfo> domainInfoList, List<ShadowAssetCandidate> shadowCandidates) {
        List<ShadowAssetCandidate> result = new ArrayList<>();
        for (ShadowAssetCandidate shadowCandidate : shadowCandidates) {
            if (shouldStop()) {
                return new ArrayList<>();
            }
            List<DomainInfo> singleton = new ArrayList<>();
            singleton.add(shadowCandidate.getDomainInfo());
            if (!DomainInfoMerger.getUniqueDomainInfoInB(domainInfoList, singleton).isEmpty()) {
                result.add(shadowCandidate);
            }
        }
        return result;
    }

    private List<DomainInfo> evaluateShadowAssetCandidates(List<ShadowAssetCandidate> shadowCandidates,
                                                           ShadowAssetContext shadowAssetContext,
                                                           ShadowAssetEvaluator shadowAssetEvaluator) {
        List<DomainInfo> acceptedDomains = new ArrayList<>();
        int currentIndex = 0;
        int totalNum = shadowCandidates.size();
        int rejectedCount = 0;

        for (ShadowAssetCandidate shadowCandidate : shadowCandidates) {
            if (shouldStop()) {
                return new ArrayList<>();
            }
            currentIndex++;
            double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
            DomainInfo domainInfo = shadowCandidate.getDomainInfo();
            String domainStr = domainInfo.getDomain();
            if (domainStr == null || domainStr.isEmpty()) {
                domainStr = domainInfo.getIp();
            }
            String showText = String.format("正在评估影子资产 [%d/%d]：%s | 进度：%.2f%%",
                    currentIndex, totalNum, domainStr, progress);
            updateProgress(showText, false, null);

            ShadowAssetDecision decision = shadowAssetEvaluator.evaluate(shadowCandidate, shadowAssetContext);
            decision.applyTo(domainInfo);
            if (decision.isAccepted()) {
                acceptedDomains.add(domainInfo);
            } else {
                rejectedCount++;
            }
        }
        updateProgress("影子资产评估完成：通过 " + acceptedDomains.size() + "，拒绝 " + rejectedCount, true, null);
        return acceptedDomains;
    }

    private ShadowAssetContext buildShadowAssetContext(String rootCompanyName, Set<String> companyNameSet,
                                                       Set<String> domainSet, Set<String> icpNoSet,
                                                       List<DomainInfo> chooseWebInfoMapList) {
        ShadowAssetContext context = new ShadowAssetContext();
        context.setRootCompanyName(rootCompanyName);
        context.setCompanyNames(companyNameSet);
        context.setDomains(domainSet);
        context.setIcpNos(icpNoSet);

        List<Map<String, Object>> referenceWebInfoMaps = new ArrayList<>();
        if (chooseWebInfoMapList != null) {
            for (DomainInfo selectedDomainInfo : chooseWebInfoMapList) {
                if (selectedDomainInfo != null && selectedDomainInfo.getWebInfoMap() != null && !selectedDomainInfo.getWebInfoMap().isEmpty()) {
                    referenceWebInfoMaps.add(selectedDomainInfo.getWebInfoMap());
                }
            }
        }
        context.setReferenceWebInfoMaps(referenceWebInfoMaps);
        return context;
    }

    private int getShadowAssetEvaluateLimit(int shadowAssetsThreshold) {
        int limit = Math.max(60, shadowAssetsThreshold * 3);
        return Math.min(limit, 200);
    }

    private int getDataSourceCount(String dataSource) {
        if (dataSource == null || dataSource.trim().isEmpty()) {
            return 0;
        }
        Set<String> sourceSet = new LinkedHashSet<>();
        for (String item : dataSource.split("[\\n,;]+")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                sourceSet.add(trimmed);
            }
        }
        return sourceSet.size();
    }

    private boolean isSameShadowCandidate(DomainInfo left, DomainInfo right) {
        if (!buildShadowBaseKey(left).equals(buildShadowBaseKey(right))) {
            return false;
        }

        Set<String> leftIdentifiers = collectShadowCandidateIdentifiers(left);
        Set<String> rightIdentifiers = collectShadowCandidateIdentifiers(right);
        if (leftIdentifiers.isEmpty() || rightIdentifiers.isEmpty()) {
            return true;
        }

        for (String identifier : leftIdentifiers) {
            if (rightIdentifiers.contains(identifier)) {
                return true;
            }
        }
        return false;
    }

    private String buildShadowCandidateKey(DomainInfo domainInfo) {
        String baseKey = buildShadowBaseKey(domainInfo);
        Set<String> identifiers = collectShadowCandidateIdentifiers(domainInfo);
        if (identifiers.isEmpty()) {
            return baseKey;
        }
        return baseKey + "|" + String.join("|", identifiers);
    }

    private String buildShadowBaseKey(DomainInfo domainInfo) {
        String ip = domainInfo.getIp() == null ? "" : domainInfo.getIp().trim();
        String port = domainInfo.getPort() == null ? "" : domainInfo.getPort().trim();
        return ip + ":" + port;
    }

    private Set<String> collectShadowCandidateIdentifiers(DomainInfo domainInfo) {
        Set<String> identifiers = new TreeSet<>();
        addShadowIdentifier(identifiers, domainInfo.getDomain());
        addShadowIdentifier(identifiers, domainInfo.getHost());
        addShadowIdentifier(identifiers, extractShadowHost(domainInfo.getUrl()));
        return identifiers;
    }

    private void addShadowIdentifier(Set<String> identifiers, String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return;
        }
        for (String item : rawValue.split("[\\n,;]+")) {
            String normalized = item.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("*.")) {
                normalized = normalized.substring(2);
            }
            if (normalized.endsWith(".")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            if (!normalized.isEmpty()) {
                identifiers.add(normalized);
            }
        }
    }

    private String extractShadowHost(String urlValue) {
        if (urlValue == null || urlValue.trim().isEmpty()) {
            return null;
        }
        try {
            return new URL(urlValue).getHost();
        } catch (Exception e) {
            return urlValue;
        }
    }

    private List<CompanyCandidate> searchCompanyCandidates(String companyName) {
        List<CompanyCandidate> chinazCandidates = fetchChinazCompanyCandidates(companyName);
        List<CompanyCandidate> aiqichaCandidates = fetchAiqichaCompanyCandidates(companyName);
        return mergeCandidates(chinazCandidates, aiqichaCandidates);
    }

    private static class CompanyResolutionResult {
        private final CompanyCandidate candidate;
        private final boolean manuallyConfirmed;

        private CompanyResolutionResult(CompanyCandidate candidate, boolean manuallyConfirmed) {
            this.candidate = candidate;
            this.manuallyConfirmed = manuallyConfirmed;
        }
    }

    private CompanyCandidate refreshCompanyCandidate(CompanyCandidate candidate) {
        if (candidate == null || candidate.getCompanyName() == null || candidate.getCompanyName().trim().isEmpty()) {
            return candidate;
        }

        List<CompanyCandidate> refreshedCandidates = searchCompanyCandidates(candidate.getCompanyName());
        CompanyCandidate exactCandidate = findExactCandidate(candidate.getCompanyName(), refreshedCandidates);
        if (exactCandidate == null) {
            return candidate;
        }
        exactCandidate.mergeFrom(candidate);
        return exactCandidate;
    }

    private CompanyCandidate chooseAiFullCompanyName(String currentQuery) {
        if (shouldStop()) {
            return null;
        }
        updateProgress("AI生成完整公司名候选：" + currentQuery, false, null);
        Set<String> aiCompanyNames = fetchAiFullCompanyNames(currentQuery);
        updateProgress("AI生成完整公司名候选：" + currentQuery, true, null);

        List<CompanyCandidate> aiCandidates = new ArrayList<>();
        for (String aiCompanyName : aiCompanyNames) {
            if (aiCompanyName != null && !aiCompanyName.trim().isEmpty()) {
                String trimmedAiCompanyName = aiCompanyName.trim();
                if (!trimmedAiCompanyName.equals(currentQuery)) {
                    aiCandidates.add(CompanyCandidate.fromAiName(trimmedAiCompanyName));
                }
            }
        }
        if (aiCandidates.isEmpty()) {
            return null;
        }

        showCompanyCandidateChoosePaneBox(aiCandidates, true, currentQuery);
        CompanyCandidateSelectionResult aiSelection = waitAndGetCompanyCandidateSelection();
        if (shouldStop()) {
            return null;
        }
        if (aiSelection.getAction() != CompanyCandidateSelectionResult.Action.CONFIRM) {
            return null;
        }
        return aiSelection.getSelectedCandidate();
    }

    private CompanyResolutionResult resolveRootCompanyCandidate(String companyName) {
        String currentQuery = companyName == null ? "" : companyName.trim();
        Set<String> attemptedQueries = new HashSet<>();
        boolean manuallyConfirmed = false;

        while (!currentQuery.isEmpty() && attemptedQueries.add(currentQuery)) {
            if (shouldStop()) {
                return null;
            }

            updateProgress("检索根公司候选：" + currentQuery, false, null);
            List<CompanyCandidate> candidates = searchCompanyCandidates(currentQuery);
            updateProgress("检索根公司候选：" + currentQuery, true, null);

            CompanyCandidate exactCandidate = findExactCandidate(currentQuery, candidates);
            if (exactCandidate != null) {
                return new CompanyResolutionResult(exactCandidate, manuallyConfirmed);
            }

            if (!candidates.isEmpty()) {
                showCompanyCandidateChoosePaneBox(candidates, false, currentQuery);
                CompanyCandidateSelectionResult selection = waitAndGetCompanyCandidateSelection();
                if (shouldStop()) {
                    return null;
                }
                if (selection.getAction() == CompanyCandidateSelectionResult.Action.CONFIRM) {
                    if (selection.getSelectedCandidate() == null) {
                        return null;
                    }
                    manuallyConfirmed = true;
                    return new CompanyResolutionResult(refreshCompanyCandidate(selection.getSelectedCandidate()), true);
                }
                if (selection.getAction() == CompanyCandidateSelectionResult.Action.TIMEOUT) {
                    return null;
                }
                if (selection.getAction() == CompanyCandidateSelectionResult.Action.CANCEL) {
                    return null;
                }
            }

            CompanyCandidate aiCompanyCandidate = chooseAiFullCompanyName(currentQuery);
            if (aiCompanyCandidate == null || aiCompanyCandidate.getCompanyName() == null || aiCompanyCandidate.getCompanyName().trim().isEmpty()) {
                return null;
            }
            manuallyConfirmed = true;
            currentQuery = aiCompanyCandidate.getCompanyName().trim();
        }

        return null;
    }

    private void addOriginalInputAliasIfNeeded(Set<String> companyNameSet, String originalInputCompanyName,
                                               String confirmedCompanyName, boolean directCompanyInput,
                                               boolean manuallyConfirmed, String currentCompanyName) {
        if (!directCompanyInput || !manuallyConfirmed) {
            return;
        }
        if (originalInputCompanyName == null || originalInputCompanyName.trim().isEmpty()) {
            return;
        }
        if (confirmedCompanyName == null || confirmedCompanyName.trim().isEmpty()) {
            return;
        }
        if (currentCompanyName == null || !currentCompanyName.equals(confirmedCompanyName)) {
            return;
        }

        String trimmedOriginalInput = originalInputCompanyName.trim();
        if (!trimmedOriginalInput.equals(confirmedCompanyName.trim())) {
            companyNameSet.add(trimmedOriginalInput);
        }
    }

    private boolean shouldCollectCompanyAliases(AssetObj assetObj) {
        return assetObj != null && (assetObj.isSearchShadowAssets() || assetObj.isUseGithub());
    }

    private LinkedHashSet<String> normalizeCompanyAliases(Set<String> aliases, String currentCompanyName) {
        LinkedHashSet<String> normalizedAliases = new LinkedHashSet<>();
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias == null) {
                    continue;
                }
                String trimmedAlias = alias.trim();
                if (!trimmedAlias.isEmpty()) {
                    normalizedAliases.add(trimmedAlias);
                }
            }
        }
        if (currentCompanyName != null) {
            String trimmedCompanyName = currentCompanyName.trim();
            if (!trimmedCompanyName.isEmpty()) {
                normalizedAliases.add(trimmedCompanyName);
            }
        }
        return normalizedAliases;
    }

    private JsonObject getConfirmedCompanyInfoMap(CompanyCandidate rootCandidate) {
        JsonObject companyInfoMap = new JsonObject();
        if (rootCandidate == null || rootCandidate.getCompanyName() == null || rootCandidate.getCompanyName().trim().isEmpty()) {
            return companyInfoMap;
        }

        if ((rootCandidate.getChinazCompanyId() == null || rootCandidate.getChinazCompanyId().isEmpty())) {
            List<CompanyCandidate> chinazCandidates = fetchChinazCompanyCandidates(rootCandidate.getCompanyName());
            CompanyCandidate exactChinazCandidate = findExactCandidate(rootCandidate.getCompanyName(), chinazCandidates);
            if (exactChinazCandidate != null) {
                rootCandidate.mergeFrom(exactChinazCandidate);
            }
        }

        if (rootCandidate.getChinazCompanyId() == null || rootCandidate.getChinazCompanyId().isEmpty()) {
            return companyInfoMap;
        }

        return GetCompany.getCompanyDetails_chinaz(rootCandidate.getChinazCompanyId(), rootCandidate.getCompanyName());
    }

    private JsonArray getConfirmedCompanyDetails(CompanyCandidate rootCandidate) {
        if (rootCandidate == null || rootCandidate.getCompanyName() == null || rootCandidate.getCompanyName().trim().isEmpty()) {
            return new JsonArray();
        }
        return aiqichaSearch.getCompanyInfoIteration(rootCandidate.getCompanyName(), rootCandidate.getAiqichaPid());
    }

    // 处理公司名称的逻辑
    private void handleCompanyName(String companyName, AssetObj assetObj, Set<String> hasIcpNoSet, Set<String> hasTmpDomainSet) {
        if (shouldStop()) return;
        boolean directCompanyInput = hasIcpNoSet==null&&hasTmpDomainSet==null;
        String originalInputCompanyName = companyName;
        if(directCompanyInput) updateProgress("传入公司名_" + companyName + "，初始化数据", true, null);
        Set<String> icpNoSet = new HashSet<>();
        Set<String> tmpDomainSet = new HashSet<>(); // 存储初步检索的域名
        if(hasIcpNoSet!=null&& hasTmpDomainSet!=null){
            icpNoSet = hasIcpNoSet;
            tmpDomainSet = hasTmpDomainSet;
        }

        CompanyResolutionResult resolutionResult = resolveRootCompanyCandidate(companyName);
        if (shouldStop()) {
            return;
        }
        CompanyCandidate rootCandidate = resolutionResult == null ? null : resolutionResult.candidate;
        if (rootCandidate == null) {
            String tipMessage = directCompanyInput
                    ? I18nTextUtils.getString("infosearch.company.root.manual.retry")
                    : I18nTextUtils.getString("infosearch.company.root.skip");
            showTip(tipMessage, false);
            updateProgress(tipMessage, true, null);
            if (!tmpDomainSet.isEmpty() || !icpNoSet.isEmpty()) {
                NetAssets netAssets = new NetAssets();
                netAssets.setCompanyName(companyName);
                dispatchCollectedSeeds(assetObj, netAssets, null, tmpDomainSet, icpNoSet);
            }
            if(hasIcpNoSet==null&& hasTmpDomainSet==null) updateProgress("查询结束", true, null);
            return;
        }

        companyName = rootCandidate.getCompanyName();
        boolean manuallyConfirmed = resolutionResult != null && resolutionResult.manuallyConfirmed;
        paneInfoSearch.updateEchoVBox("已确认根公司：" + companyName, true, null);

        paneInfoSearch.updateEchoVBox("检索相关公司信息", false, null);
        JsonObject companyInfoMap = getConfirmedCompanyInfoMap(rootCandidate);
        if(companyInfoMap.size()>0){
            assetObj.setCompanyInfoMap(companyInfoMap);
            JsonArray icpInfo = companyInfoMap.getAsJsonArray("网站备案");

            for(JsonElement jsonElement : icpInfo) {
                String icpNo = jsonElement.getAsJsonObject().get("备案号").getAsString();
                String domainStr = jsonElement.getAsJsonObject().get("网站域名").getAsString();
                assetObj.addIcp(icpNo);
                icpNoSet.add(icpNo);
                tmpDomainSet.add(domainStr.replace("*.", ""));
            }
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("type", DataTypeConstants.ICP);
            dataMap.put("data", companyInfoMap);
            paneInfoSearch.updateEchoVBox("检索公司相关的备案号及域名信息", true, dataMap);
        } else {
            paneInfoSearch.updateEchoVBox("检索公司相关的备案号及域名信息", true, null);
        }

        paneInfoSearch.updateEchoVBox("检索公司相关的微信公众号、APP、子公司信息", false, null);
        JsonArray companyDetailsInfoMap = getConfirmedCompanyDetails(rootCandidate);
        assetObj.setCompanyDetailsInfoMap(companyDetailsInfoMap);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("type", DataTypeConstants.WXAPPSUBCOM);
        dataMap.put("data", companyDetailsInfoMap);
        paneInfoSearch.updateEchoVBox("检索公司及符合权重的子公司相关的备案号、域名、微信公众号、APP等信息", true, dataMap);

        boolean hasSubCompanyName = false;
        int currentIndex = 0;
        int totalNum = companyDetailsInfoMap.size();
        for(JsonElement jsonElement : companyDetailsInfoMap) {
            if (shouldStop()) return;
            currentIndex++;
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if(jsonObject!=null && !jsonObject.isJsonNull() && jsonObject.size()>0) {
                String subCompanyName = jsonObject.entrySet().iterator().next().getKey();
                if (subCompanyName != null && !subCompanyName.isEmpty()) {
                    hasSubCompanyName = true;
                    JsonArray domainAndIcp = jsonObject.getAsJsonObject(subCompanyName).getAsJsonArray("domainAndIcp");
                    Set<String> subComDomainSet = new HashSet<>();
                    Set<String> subIcpNoSet = new HashSet<>();
                    for (JsonElement subJsonElement : domainAndIcp) {
                        String icpNo = subJsonElement.getAsJsonObject().get("icpNo").getAsString();
                        subIcpNoSet.add(icpNo);
                        JsonArray domainList = subJsonElement.getAsJsonObject().getAsJsonArray("domain");
                        if (!icpNo.isEmpty()) assetObj.addIcp(icpNo);
                        domainList.forEach(element -> {
                            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                                subComDomainSet.add(element.getAsString());
                            }
                        });
                    }

                    double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                    String showText_data = String.format("检索企业 [%d/%d]：%s | 进度：%.2f%%",
                            currentIndex, totalNum, subCompanyName, progress);
                    paneInfoSearch.updateEchoVBox(showText_data, true, null);

                    String showText_Ai = String.format("正在获取企业别名_By_Ai [%d/%d]：%s | 进度：%.2f%%",
                            currentIndex, totalNum, subCompanyName, progress);
                    String showText_User = String.format("正在获取企业别名_By_User [%d/%d]：%s | 进度：%.2f%%",
                            currentIndex, totalNum, subCompanyName, progress);
                    String showText = String.format("企业别名 [%d/%d]：%s | 进度：%.2f%%",
                            currentIndex, totalNum, subCompanyName, progress);
                    if (totalNum == 1) showText = "获取企业别名：" + subCompanyName;
                    Set<String> subCompanyNameSet = resolveCompanyAliasesForCollection(assetObj,
                            subCompanyName,
                            originalInputCompanyName,
                            companyName,
                            directCompanyInput,
                            manuallyConfirmed,
                            showText_Ai,
                            showText_User,
                            showText);
                    if (shouldStop()) return;

                    NetAssets netAssets = new NetAssets();
                    netAssets.setCompanyName(subCompanyName);
                    getData(assetObj, netAssets, subCompanyNameSet, subComDomainSet, subIcpNoSet);
                }
            }
        }

        if(!hasSubCompanyName){
            Set<String> companyNameSet = resolveCompanyAliasesForCollection(assetObj,
                    companyName,
                    originalInputCompanyName,
                    companyName,
                    directCompanyInput,
                    manuallyConfirmed,
                    "正在获取企业别名_By_Ai：" + companyName,
                    "正在获取企业别名_By_User：" + companyName,
                    "企业别名：" + companyName);
            if (shouldStop()) return;

            NetAssets netAssets = new NetAssets();
            netAssets.setCompanyName(companyName);
            getData(assetObj, netAssets, companyNameSet, tmpDomainSet, icpNoSet);
        }

        if(hasIcpNoSet==null&& hasTmpDomainSet==null) paneInfoSearch.updateEchoVBox("查询结束", true, null);
    }

    public static void main(String[] args) {
        // 示例输入
//        AssetMapper assetMapper = new AssetMapper();
//
//        AssetObj assetObj = new AssetObj();
//        assetMapper.searchInfo("www.pinyin.cn", assetObj);
//        System.out.println(1111);
//        assetMapper.searchInfo("potato.gold", assetObj);
        System.out.println(standardFormat("https://219.133.105.102:10040"));
    }

}

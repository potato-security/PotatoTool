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
import com.potato.potatotool.controller.PaneInfoSearch;
import com.potato.potatotool.utils.ExecutorServiceManager;
import com.potato.potatotool.utils.strUtils;

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

/**
 * @author Potato
 * @date 2023/4/23 10:05
 *
 * 空间测绘主类
 */
public class AssetMapper {
    private PaneInfoSearch paneInfoSearch;

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

    public void searchInfo(String input, AssetObj assetObj) {
        input = input.trim();
        if(input.isEmpty()) return;
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

        paneInfoSearch.updateEchoVBox("导出报告中……", false, null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStr = sdf.format(new Date(System.currentTimeMillis()));
        String outXlsxFile = strUtils.getCurrentJarDir() + File.separator + "AssetResult" + File.separator + input + "_" + timeStr +".xlsx";
        String error = new AssetExcelExporter().exportToExcel(assetObj, outXlsxFile);
        paneInfoSearch.updateEchoVBox(error==null ? "报告导出至:" + outXlsxFile : "导出失败_[Error]：" + error, true, null);
    }


    public void searchInfo_standard(String input, AssetObj assetObj) {
        input = input.trim();
        if(input.isEmpty()) return;

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

        paneInfoSearch.updateEchoVBox("导出报告中……", false, null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStr = sdf.format(new Date(System.currentTimeMillis()));
        String outXlsxFile = strUtils.getCurrentJarDir() + File.separator + "AssetResult" + File.separator + input + "_" + timeStr +".xlsx";
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
        paneInfoSearch.updateEchoVBox("传入IP地址_" + ip + "，初始化数据", true, null);

        if(isCdnIp(ip)){
            paneInfoSearch.updateEchoVBox("为CDN_Ip，跳过增量检索", true, null);
        }else {
            paneInfoSearch.updateEchoVBox("反查域名，并获取域名SEO信息", false, null);
            JsonArray domainList = getDomainByIp(ip, 5);
            paneInfoSearch.updateEchoVBox("选择域名_By_User", false, null);
            paneInfoSearch.showDomainChoosePaneBox(domainList);
            JsonArray domainListChoose = paneInfoSearch.waitAndGetDomainSet();
            paneInfoSearch.updateEchoVBox("反查域名_Top5", true, null);

            // 相同备案公司的域名不处理
            List<String> companyNameList = new ArrayList<>();
            if (domainListChoose != null && domainListChoose.size() > 0) {
                for (JsonElement jsonElement : domainListChoose) {
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

        String poolName = ExecutorServiceManager.ExecutorPoolNames.ASSET;
        ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
        List<CompletableFuture<?>> futures = new ArrayList<>();

        // 根据主域名获取子域
        int currentIndex = 0;
        if(assetObj.isSearchSslSubdomainBox()){
            int totalNum = domainSet.size();
            for(String domainStr : domainSet) {
                currentIndex++;
                double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                String showText = String.format("正在查询子域名By_SSL [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, domainStr, progress);
                paneInfoSearch.updateEchoVBox(showText, false, null);
                tmpDomainSet.addAll(GetSubDomain.getSubByDomainOrDomainCert(domainStr, isSslProxy));
            }
        }
        currentIndex = 0;
        if (assetObj.isBruteForceSubdomain()) {
            int totalNum = domainSet.size();
            for(String domainStr : domainSet) {
                currentIndex++;
                double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                String showText = String.format("正在爆破子域名 [%d/%d]：%s | 进度：%.2f%%",
                        currentIndex, totalNum, domainStr, progress);
                paneInfoSearch.updateEchoVBox(showText, false, null);
                tmpDomainSet.addAll((SubdomainBruteForcer.getSubDomain(domainStr)));
            }
        }
        paneInfoSearch.updateEchoVBox("获取子域名", true, null);


        if(!isHasAssetKey) {
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
                            progress, totalTasks , domainStr, percentage);
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
                    if(debugMode) e.printStackTrace();
                }
            }
            // 停止所有线程
            ExecutorServiceManager.shutdownExecutor(poolName);

            paneInfoSearch.updateEchoVBox("Amap检索域名信息", true, null);

        }else {
            // 平台查询原始域名  【平台api存在频率限制，不建议多线程调用】
            paneInfoSearch.updateEchoVBox("平台检索原始域名", false, null);

            currentIndex = 0;
            int totalNum = domainSet.size();
            for (String domainStr : domainSet) {
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
                    if (icp != null && !icp.equals("-") && !icp.isEmpty()) {
                        tmpIcpNoSet.add(icp.replaceAll("\\s*-\\s*\\d+$", ""));
                    }
                    if (tmpDomainStr != null && !tmpDomainStr.isEmpty()) {
                        tmpDomainSet.add(tmpDomainStr.replace("*.", ""));
                    }
                }
            }

            paneInfoSearch.updateEchoVBox("平台检索原始域名", true, null);


            // icp查询
            paneInfoSearch.updateEchoVBox("平台检索ICP", false, null);
            currentIndex = 0;
            totalNum = tmpIcpNoSet.size();
            for (String icpNoStr : tmpIcpNoSet) {
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

            // 剔除cdnIp，对IP进行检索
            paneInfoSearch.updateEchoVBox("剔除CDN_Ip，平台检索IP", false, null);
            Set<String> oldIpSet = domainInfoList.stream()
                    .filter(domainInfo -> domainInfo.getIp()!= null &&!domainInfo.isCND())
                    .map(DomainInfo::getIp)
                    .collect(Collectors.toSet());
            currentIndex = 0;
            totalNum = oldIpSet.size();
            for (String ip : oldIpSet) {
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


            // 去重处理
            paneInfoSearch.updateEchoVBox("第一波数据去重", false, null);
            domainInfoList = DomainInfoMerger.mergeDomainInfoList(domainInfoList);
            paneInfoSearch.updateEchoVBox("第一波数据去重", true, null);

            if(isHasLocalFullDetection || isHasIconSearch) {
                paneInfoSearch.updateEchoVBox("第一波全量深度信息探测", false, null);
                int totalTasks = domainInfoList.size();
                AtomicInteger completedTasks = new AtomicInteger(0); // 已完成任务计数器
                int tmpIndex = 0;
                executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
                futures = new ArrayList<>();
                for (DomainInfo domainInfo : domainInfoList) {
                    tmpIndex++;
                    if (!isHasLocalFullDetection && tmpIndex > localFullDetectionThreshold) break;
                    // 提交每个 DomainInfo 的 Web 信息获取任务
                    CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                        String domainStr = domainInfo.getDomain();
                        if (domainStr == null || domainStr.isEmpty()) domainStr = domainInfo.getIp();

                        String baseUrl = getBaseUrl(domainInfo);
                        if (baseUrl != null && !baseUrl.isEmpty()) {
                            domainInfo.setWebInfoMap(Utils.getWebInfo(baseUrl, hasCrawlLinks, hasFindSensitiveInfo, maxDepth, maxSubPathCount, assetObj.isCrawlProxy()));
                        }
                        domainInfo.setDoWebInfoMap(true);

                        int progress = completedTasks.incrementAndGet();
                        double percentage = ((progress - 0.5) * 100.0) / totalTasks;
                        String showText = String.format("正在全量深度信息探测 [%d/%d]：%s | 进度：%.2f%%",
                                progress, totalTasks, domainStr, percentage);
                        paneInfoSearch.updateEchoVBox(showText, false, null);
                        return null;
                    }, executor);
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

                paneInfoSearch.updateEchoVBox("第一波全量深度信息探测", true, null);
            }
            List<DomainInfo> chooseWebInfoMapList = new ArrayList<>();
            if(isHasIconSearch) {
                paneInfoSearch.showIconChoosePaneBox(domainInfoList);
                paneInfoSearch.updateEchoVBox("用户选择企业相关icon", false, null);
                chooseWebInfoMapList = paneInfoSearch.waitAndGetIconDomainInfoList();
                paneInfoSearch.updateEchoVBox("获取企业相关icon", true, null);

                Set<String> md5Record = ConcurrentHashMap.newKeySet();
                paneInfoSearch.updateEchoVBox("平台检索icon", false, null);

                currentIndex = 0;
                totalNum = chooseWebInfoMapList.size();
                for (DomainInfo domainInfo : chooseWebInfoMapList) {
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
            }
            // 去重处理
            paneInfoSearch.updateEchoVBox("第二波数据去重", false, null);
            domainInfoList = DomainInfoMerger.mergeDomainInfoList(domainInfoList);
            paneInfoSearch.updateEchoVBox("第二波数据去重", true, null);

            // 开始检索影子资产
            if(isSearchShadowAssets){
                paneInfoSearch.updateEchoVBox("检索影子资产", false, null);
                if(companyNameSet!=null && companyNameSet.size()>0) {
                    String companyNamesStr = String.join("、", companyNameSet);

                    currentIndex = 0;
                    totalNum = companyNameSet.size();
                    for (String companyNameStr : companyNameSet) {
                        currentIndex++;
                        double progress = ((currentIndex - 0.5) * 100.0) / totalNum;
                        String showText = String.format("正在泛型检索 [%d/%d]：%s | 进度：%.2f%%",
                                currentIndex, totalNum, companyNameStr, progress);
                        paneInfoSearch.updateEchoVBox(showText, false, null);

                        // 查询各平台数据
                        JsonArray info_Fofa = fofaSearch.getInfoByBodyFilterIcp_Fofa(companyNameStr);
                        JsonArray info_Hunter = hunterSearch.getInfoByBodyFilterIcp_Hunter(companyNameStr);
                        JsonArray info_Quake = quakeSearch.getInfoByBodyFilteIcp_Quake(companyNameStr);
                        JsonArray info_Shodan = shodanSearch.getInfoByBodyFilterIcp_shodan(companyNameStr);
                        JsonArray info_Zoomeye = zoomeyeSearch.getInfoByBodyFilterIcp_zoomeye(companyNameStr);

                        // 整合影子资产并过滤非已确定的资产
                        String showText_merge = String.format("数据整合 [%d/%d]：%s | 进度：%.2f%%",
                                currentIndex, totalNum, companyNameStr, progress);
                        String showText_filter = String.format("过滤非已确定的资产 [%d/%d]：%s | 进度：%.2f%%",
                                currentIndex, totalNum, companyNameStr, progress);
                        paneInfoSearch.updateEchoVBox(showText_merge, false, null);
                        List<DomainInfo> tmpMergedList = DomainInfoMerger.mergeDomainInfos("泛型检索：" + companyNameStr, info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                        paneInfoSearch.updateEchoVBox(showText_filter, false, null);
                        tmpMergedList = DomainInfoMerger.getUniqueDomainInfoInB(domainInfoList, tmpMergedList);
                        tmpMergedList = tmpMergedList.subList(0, Math.min(shadowAssetsThreshold, tmpMergedList.size()));

                        // 根据图标相似度和内容识别关联资产
                        String showText_ai = String.format("根据图标相似度、内容识别过滤资产 [%d/%d]：%s | 进度：%.2f%%",
                                currentIndex, totalNum, companyNameStr, progress);
                        paneInfoSearch.updateEchoVBox(showText_ai, false, null);
                        List<DomainInfo> relevantDomains = new ArrayList<>();
                        Map<String, Object> chooseWebBaseInfoMap = chooseWebInfoMapList.size()> 0 ? chooseWebInfoMapList.get(0).getWebInfoMap() : null;
                        for (DomainInfo domainInfo : tmpMergedList) {
                            String baseUrl = getBaseUrl(domainInfo);
                            if (baseUrl != null && !baseUrl.isEmpty() && AiUtils.getContentRelevance_Ai(baseUrl, companyNamesStr, chooseWebBaseInfoMap, assetObj.isCrawlProxy())) {
                                relevantDomains.add(domainInfo);
                            }
                        }

                        // 去重
                        String showText_rem = String.format("数据去重 [%d/%d]：%s | 进度：%.2f%%",
                                currentIndex, totalNum, companyNameStr, progress);
                        paneInfoSearch.updateEchoVBox(showText_rem + companyNameStr, false, null);
                        List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfoList(relevantDomains);
                        domainInfoList.addAll(mergedList);
                    }
                }
                paneInfoSearch.updateEchoVBox("检索影子资产", true, null);
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
            for (String ip : addedIps) {
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
            }


            // 剔除cdnIp，进行C段聚合
            if(isHasCipAggregator) {
                paneInfoSearch.updateEchoVBox("剔除CDN_Ip，C段聚合", false, null);
                Map<String, Integer> getFrequentCSegments = CipAggregator.getFrequentCSegments(newIpSet, cipThreshold);
                paneInfoSearch.updateEchoVBox("剔除CDN_Ip，C段聚合", true, null);

                currentIndex = 0;
                totalNum = getFrequentCSegments.size();
                if (getFrequentCSegments.size() > 0) {
                    paneInfoSearch.updateEchoVBox("平台检索C段", false, null);
                    for (String ip : getFrequentCSegments.keySet()) {
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
                }
            }
        }

        // 去重
        paneInfoSearch.updateEchoVBox("第三波数据去重", false, null);
        domainInfoList = DomainInfoMerger.mergeDomainInfoList(domainInfoList);
        paneInfoSearch.updateEchoVBox("第三波数据去重", true, null);


        // 使用线程安全的集合来存储已处理过的域名信息
        if(isUseGoogle) {
            paneInfoSearch.updateEchoVBox("检索Google信息泄露", false, null);
            ConcurrentMap<String, DoDomainInfo> doGoogleDomainInfoMap = new ConcurrentHashMap<>();

            int totalTasks = domainInfoList.size();
            AtomicInteger completedTasks = new AtomicInteger(0);
            executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
            futures = new ArrayList<>();
            for (DomainInfo domainInfo : domainInfoList) {
                CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
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
        }

        if(isUseGithub) {
            paneInfoSearch.updateEchoVBox("检索Github信息泄露", false, null);
            // 使用线程安全的集合来存储已处理过的域名信息
            ConcurrentMap<String, DoDomainInfo> doGitDomainInfoMap = new ConcurrentHashMap<>();

            int totalTasks = domainInfoList.size();
            AtomicInteger completedTasks = new AtomicInteger(0);
            executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
            futures = new ArrayList<>();
            for (DomainInfo domainInfo : domainInfoList) {
                CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
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
        }

        if(isHasLocalFullDetection) {
            paneInfoSearch.updateEchoVBox("第二波全量深度信息探测", false, null);
            int totalTasks = domainInfoList.size();
            AtomicInteger completedTasks = new AtomicInteger(0); // 已完成任务计数器
            executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
            futures = new ArrayList<>();
            for (DomainInfo domainInfo : domainInfoList) {
                CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
                    String domainStr = domainInfo.getDomain();
                    if (domainStr == null || domainStr.isEmpty()) domainStr = domainInfo.getIp();
                    int progress = completedTasks.incrementAndGet();
                    double percentage = ((progress - 0.5) * 100.0) / totalTasks;
                    String showText = String.format("正在全量深度信息探测 [%d/%d]：%s | 进度：%.2f%%",
                            progress, totalTasks, domainStr, percentage);

                    if (!domainInfo.isDoWebInfoMap()) {
                        // 获取网站信息
                        String baseUrl = getBaseUrl(domainInfo);
                        if (baseUrl != null && !baseUrl.isEmpty()) {
                            domainInfo.setWebInfoMap(Utils.getWebInfo(baseUrl, hasCrawlLinks, hasFindSensitiveInfo, maxDepth, maxSubPathCount, assetObj.isCrawlProxy()));
                        }
                        domainInfo.setDoWebInfoMap(true);
                    }

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

            paneInfoSearch.updateEchoVBox("第二波全量深度信息探测", true, null);
        }
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

    // 处理公司名称的逻辑
    private void handleCompanyName(String companyName, AssetObj assetObj, Set<String> hasIcpNoSet, Set<String> hasTmpDomainSet) {
        if(hasIcpNoSet==null&&hasTmpDomainSet==null) paneInfoSearch.updateEchoVBox("传入公司名_" + companyName + "，初始化数据", true, null);
        Set<String> icpNoSet = new HashSet<>();
        Set<String> tmpDomainSet = new HashSet<>(); // 存储初步检索的域名
        if(hasIcpNoSet!=null&& hasTmpDomainSet!=null){
            icpNoSet = hasIcpNoSet;
            tmpDomainSet = hasTmpDomainSet;
        }

        paneInfoSearch.updateEchoVBox("检索相关公司信息", false, null);
        JsonArray companyList = GetCompany.getCompany_chinaz(companyName);
        if(companyList.size()>0){
            JsonObject companyInfoMap = GetCompany.getCompanyDetails_chinaz(companyList.get(0).getAsJsonObject().get("企业ID").getAsString(), companyList.get(0).getAsJsonObject().get("企业名称").getAsString());
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
        }

        paneInfoSearch.updateEchoVBox("检索公司相关的微信公众号、APP、子公司信息", false, null);
        JsonArray companyDetailsInfoMap = aiqichaSearch.getCompanyInfoIteration(companyName);
        assetObj.setCompanyDetailsInfoMap(companyDetailsInfoMap);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("type", DataTypeConstants.WXAPPSUBCOM);
        dataMap.put("data", companyDetailsInfoMap);
        paneInfoSearch.updateEchoVBox("检索公司及符合权重的子公司相关的备案号、域名、微信公众号、APP等信息", true, dataMap);

        boolean hasSubCompanyName = false;
        int currentIndex = 0;
        int totalNum = companyDetailsInfoMap.size();
        for(JsonElement jsonElement : companyDetailsInfoMap) {
            currentIndex++;
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if(jsonObject!=null && !jsonObject.isJsonNull() && jsonObject.size()>0) {
                String subCompanyName = jsonObject.entrySet().iterator().next().getKey();
                if (subCompanyName != null && !subCompanyName.isEmpty()) {
                    hasSubCompanyName = true;
                    JsonArray domainAndIcp = jsonObject.getAsJsonObject(subCompanyName).getAsJsonArray("domainAndIcp");
                    // 每家公司名（subCompanyName）
                    // 该公司的domain列表(subComDomainSet)
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

                    Set<String> subCompanyNameSet = new HashSet<>();
                    if(assetObj.isSearchShadowAssets() && assetObj.isUseGithub()) {
                        String showText_Ai = String.format("正在获取企业别名_By_Ai [%d/%d]：%s | 进度：%.2f%%",
                                currentIndex, totalNum, subCompanyName, progress);
                        String showText_User = String.format("正在获取企业别名_By_User [%d/%d]：%s | 进度：%.2f%%",
                                currentIndex, totalNum, subCompanyName, progress);
                        String showText = String.format("企业别名 [%d/%d]：%s | 进度：%.2f%%",
                                currentIndex, totalNum, subCompanyName, progress);
                        if (totalNum == 1) showText = "获取企业别名：" + subCompanyName;
                        // 获取企业别名by_Ai
                        paneInfoSearch.updateEchoVBox(showText_Ai, false, null);
                        subCompanyNameSet = AiUtils.getCompanyName_Ai(subCompanyName);
                        paneInfoSearch.updateEchoVBox(showText_User, false, null);
                        paneInfoSearch.showCompanyNameChoosePaneBox(subCompanyNameSet);
                        subCompanyNameSet = paneInfoSearch.waitAndGetCompanyNameSet();
                        paneInfoSearch.updateEchoVBox(showText, true, null);
                    }else {
                        subCompanyNameSet.add(subCompanyName);
                    }

                    NetAssets netAssets = new NetAssets();
                    netAssets.setCompanyName(subCompanyName);
                    getData(assetObj, netAssets, subCompanyNameSet, subComDomainSet, subIcpNoSet);
                }
            }
        }

        if(!hasSubCompanyName){
            Set<String> companyNameSet = new HashSet<>();
            if(assetObj.isSearchShadowAssets() && assetObj.isUseGithub()) {
                paneInfoSearch.updateEchoVBox("正在获取企业别名_By_Ai：" + companyName, false, null);
                companyNameSet = AiUtils.getCompanyName_Ai(companyName);
                paneInfoSearch.updateEchoVBox("正在获取企业别名_By_User：" + companyName, false, null);
                paneInfoSearch.showCompanyNameChoosePaneBox(companyNameSet);
                companyNameSet = paneInfoSearch.waitAndGetCompanyNameSet();
                paneInfoSearch.updateEchoVBox("企业别名：" + companyName, true, null);
            }else {
                companyNameSet.add(companyName);
            }

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

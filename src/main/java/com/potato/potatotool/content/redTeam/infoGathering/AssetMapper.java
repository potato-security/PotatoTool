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

import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
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

        fofaSearch = new FofaSearch(Fofa_Key);
        hunterSearch = new HunterSearch(Hunter_Key);
        quakeSearch = new QuakeSearch(Quake_Key);
        shodanSearch = new ShodanSearch(Shodan_Key);
        zoomeyeSearch = new ZoomeyeSearch(Zoomeye_Key);
        googleSearch = new GoogleSearch(assetObj.getGoogle_API());
        gitHubLeakage = new GitHubLeakage(assetObj.getGitHub_Token());

        aiqichaSearch = new AiqichaSearch(assetObj.getWeightThresholdList(), paneInfoSearch);

        if (Utils.isIPAddress(input)) {
            handleIPAddress(input, assetObj);
        } else if (Utils.isDomainName(input)) {
            handleDomainName(input, assetObj, null);
        } else {
            handleCompanyName(input, assetObj, null, null);
        }
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
        List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos(info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
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
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("type", DataTypeConstants.SEO);
            dataMap.put("data", seoMap);
            paneInfoSearch.updateEchoVBox("检索域名基础信息", true, dataMap);
        }
        String ipcType = seoMap.getAsJsonObject("备案信息").get("备案性质").getAsString();
        String icpNoStr = seoMap.getAsJsonObject("备案信息").get("备案号").getAsString();
        String companyName = seoMap.getAsJsonObject("备案信息").get("备案所属").getAsString();
        icpNoSet.add(icpNoStr);
        if(!ipcType.isEmpty() && !ipcType.equals("-") && !ipcType.equals("个人") && !companyName.isEmpty() && !companyName.equals("-")){
            handleCompanyName(companyName, assetObj, icpNoSet, tmpDomainSet);
        }else {
            List<DomainInfo> tmpDomainInfoList = getData(assetObj, null, tmpDomainSet, icpNoSet);
            NetAssets netAssets = new NetAssets();
            netAssets.setCompanyName(companyName);
            netAssets.setDomainInfoList(tmpDomainInfoList);
            assetObj.addCompanyDomainInfoList(netAssets);
        }

        if (hasSeoMap==null) paneInfoSearch.updateEchoVBox("查询结束", true, null);

        String error = new AssetExcelExporter().exportToExcel(assetObj, "/Users/a/Desktop/项目开发/PotatoTool/123.xlsx");
        paneInfoSearch.updateEchoVBox(error==null ? "导出至" : "导出失败_[Error]：" + error, true, null);
    }

    private ExecutorService executor = ExecutorServiceManager.getInstance().getExecutor();
    private List<Future<?>> futures = ExecutorServiceManager.futures;
    /**
     *
     * @param assetObj          AssetObj对象
     * @param companyNameSet    企业名、别名集合
     * @param domainSet            域名
     * @param icpNoSet             icp备案号
     */
    private List<DomainInfo> getData(AssetObj assetObj, Set<String> companyNameSet, Set<String> domainSet, Set<String> icpNoSet){
        // 初始化变量
        Set<String> tmpDomainSet = new HashSet<>(domainSet);
        List<DomainInfo> domainInfoList = new ArrayList<>();
        Set<String> tmpIcpNoSet = new HashSet<>(icpNoSet);

        // 提取配置信息
        boolean hasCrawlLinks = assetObj.isHasCrawlLinks();
        boolean hasFindSensitiveInfo = assetObj.isHasFindSensitiveInfo();
        int maxDepth = assetObj.getMaxDepth();
        int maxSubPathCount = assetObj.getMaxSubPathCount();
        boolean isSearchShadowAssets = assetObj.isSearchShadowAssets();

        // 根据主域名获取子域
        for(String domainStr : domainSet) {
            paneInfoSearch.updateEchoVBox("获取子域名By_SSL：" +  domainStr, false, null);
            tmpDomainSet.addAll(GetSubDomain.getSubByDomainOrDomainCert(domainStr));
            if (assetObj.isBruteForceSubdomain()) {
                paneInfoSearch.updateEchoVBox("爆破子域名：" +  domainStr, false, null);
                tmpDomainSet.addAll((SubdomainBruteForcer.getSubDomain(domainStr)));
            }
        }
        paneInfoSearch.updateEchoVBox("获取子域名", true, null);


        if(!assetObj.isHasAssetKey()) {
            // 无任何平台Key时，调用amap
            paneInfoSearch.updateEchoVBox("Amap检索域名信息", false, null);
            for (String domainStr : tmpDomainSet) {
                // 将每个任务提交到线程池
                Future<List<DomainInfo>> future = executor.submit(() -> {
                    paneInfoSearch.updateEchoVBox("检索域名信息" + domainStr, false, null);
                    JsonObject briefExtendedInfo = fofaSearch.getBriefExtendedInfo_Fofa(domainStr);
                    return DomainInfoMerger.mergeDomainInfos(briefExtendedInfo);
                });
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
            ExecutorServiceManager.getInstance().forceShutdown();

            paneInfoSearch.updateEchoVBox("Amap检索域名信息", true, null);

        }else {
            // 平台查询原始域名  【平台api存在频率限制，不建议多线程调用】
            paneInfoSearch.updateEchoVBox("平台检索原始域名", false, null);

            for (String domainStr : domainSet) {
                paneInfoSearch.updateEchoVBox("平台检索原始域名：" + domainStr, false, null);

                // 获取不同平台的域名信息
                JsonArray info_Fofa = fofaSearch.getInfoByCompanyOrDomain_Fofa(domainStr);
                JsonArray info_Hunter = hunterSearch.getInfoByCompanyOrDomain_Hunter(domainStr);
                JsonArray info_Quake = quakeSearch.getInfoByCompanyOrDomain_Quake(domainStr);
                JsonArray info_Shodan = shodanSearch.getInfoByDomain_Shodan(domainStr);
                JsonArray info_Zoomeye = zoomeyeSearch.getInfoByCompanyOrDomain_Zoomeye(domainStr);

                // 合并信息

                paneInfoSearch.updateEchoVBox("数据整合：" + domainStr, false, null);
                List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos(info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                domainInfoList.addAll(mergedList);           // 添加到总的域名信息列表

                paneInfoSearch.updateEchoVBox("更新相关域名列表及ICP列表：" + domainStr, false, null);
                // 更新域名列表和 ICP 列表
                for (DomainInfo domainInfo : mergedList) {
                    String icp = domainInfo.getIcp();
                    String tmpDomainStr = domainInfo.getDomain();
                    if (icp != null && !icp.isEmpty()) {
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
            for (String icpNoStr : tmpIcpNoSet) {
                paneInfoSearch.updateEchoVBox("平台检索ICP：" + icpNoStr, false, null);

                // 获取不同平台的 ICP 信息
                JsonArray icpInfo_Fofa = fofaSearch.getInfoByIcpNo_Fofa(icpNoStr);
                JsonArray icpInfo_Hunter = hunterSearch.getInfoByIcpNo_Hunter(icpNoStr);
                JsonArray icpInfo_Quake = quakeSearch.getInfoByIcpNo_Quake(icpNoStr);

                // 合并信息
                paneInfoSearch.updateEchoVBox("数据整合：" + icpNoStr, false, null);
                List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos(icpInfo_Fofa, icpInfo_Hunter, icpInfo_Quake);
                domainInfoList.addAll(mergedList);  // 添加到总的域名信息列表

                // 更新域名列表
                paneInfoSearch.updateEchoVBox("更新相关域名列表：" + icpNoStr, false, null);
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
            for (String ip : oldIpSet) {
                paneInfoSearch.updateEchoVBox("平台检索IP：" + ip, false, null);

                // 获取不同平台的 IP 信息
                JsonArray info_Fofa = fofaSearch.getInfoByIp_Fofa(ip);
                JsonArray info_Hunter = hunterSearch.getInfoByIp_Hunter(ip);
                JsonArray info_Quake = quakeSearch.getInfoByIp_Quake(ip);
                JsonArray info_Shodan = shodanSearch.getInfoByIp_Shodan(ip);
                JsonArray info_Zoomeye = zoomeyeSearch.getInfoByIp_Zoomeye(ip);

                // 合并信息
                paneInfoSearch.updateEchoVBox("数据整合：" + ip, false, null);
                List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos(info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                domainInfoList.addAll(mergedList);  // 添加到总的域名信息列表
            }
            paneInfoSearch.updateEchoVBox("剔除CDN_Ip，平台检索IP", true, null);


            // 去重处理
            paneInfoSearch.updateEchoVBox("第一波数据去重", false, null);
            domainInfoList = DomainInfoMerger.mergeDomainInfoList(domainInfoList);
            paneInfoSearch.updateEchoVBox("第一波数据去重", true, null);
            paneInfoSearch.updateEchoVBox("第一波全量深度信息探测", false, null);
            for (DomainInfo domainInfo : domainInfoList) {
                // 提交每个 DomainInfo 的 Web 信息获取任务
                Future<Void> future = executor.submit(() -> {
                    String baseUrl = getBaseUrl(domainInfo);
                    if (baseUrl != null && !baseUrl.isEmpty()) {
                        paneInfoSearch.updateEchoVBox("全量深度信息探测：" + baseUrl, false, null);
                        domainInfo.setWebInfoMap(Utils.getWebInfo(baseUrl, hasCrawlLinks, hasFindSensitiveInfo, maxDepth, maxSubPathCount));
                    }
                    domainInfo.setDoWebInfoMap(true);
                    return null;
                });
                futures.add(future);
            }
            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get(); // 阻塞直到任务完成
                } catch (CancellationException ce) {} catch (Exception e) {
                    if(debugMode) e.printStackTrace();
                }
            }
            // 停止所有线程
            ExecutorServiceManager.getInstance().forceShutdown();

            paneInfoSearch.updateEchoVBox("第一波全量深度信息探测", true, null);

            paneInfoSearch.showIconChoosePaneBox(domainInfoList);
            paneInfoSearch.updateEchoVBox("用户选择企业相关icon", false, null);
            List<DomainInfo> chooseWebInfoMapList = paneInfoSearch.waitAndGetIconDomainInfoList();
            paneInfoSearch.updateEchoVBox("获取企业相关icon", true, null);

            Set<String> md5Record = ConcurrentHashMap.newKeySet();
            paneInfoSearch.updateEchoVBox("平台检索icon", false, null);
            for (DomainInfo domainInfo : chooseWebInfoMapList) {

                Map<String, Object> webInfoMap = domainInfo.getWebInfoMap();
                if (webInfoMap == null || !webInfoMap.containsKey("iconMd5") || webInfoMap.get("iconMd5") == null) {
                    return Collections.emptyList();
                }

                String iconMd5 = webInfoMap.get("iconMd5").toString();
                if (!md5Record.add(iconMd5)) {
                    return Collections.emptyList(); // 排除重复icon
                }

                String iconMmh3 = webInfoMap.get("iconMmh3").toString();
                JsonArray info_Fofa = fofaSearch.getInfoByIcon_Fofa(iconMmh3);
                JsonArray info_Hunter = hunterSearch.getInfoByIcon_Hunter(iconMd5);
                JsonArray info_Quake = quakeSearch.getInfoByIcon_Quake(iconMd5);
                JsonArray info_Shodan = shodanSearch.getInfoByIcon_Shodan(iconMmh3);
                JsonArray info_Zoomeye = zoomeyeSearch.getInfoByIcon_Zoomeye(iconMd5);

                List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos(info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                domainInfoList.addAll(mergedList);
                System.out.println(mergedList);
            }
            paneInfoSearch.updateEchoVBox("平台检索icon", true, null);

            // 去重处理
            paneInfoSearch.updateEchoVBox("第二波数据去重", false, null);
            domainInfoList = DomainInfoMerger.mergeDomainInfoList(domainInfoList);
            paneInfoSearch.updateEchoVBox("第二波数据去重", true, null);

            // 开始检索影子资产
            if(isSearchShadowAssets){
                paneInfoSearch.updateEchoVBox("检索影子资产", false, null);
                if(companyNameSet!=null && companyNameSet.size()>0) {
                    String companyNamesStr = String.join("、", companyNameSet);
                    for (String companyNameStr : companyNameSet) {
                        paneInfoSearch.updateEchoVBox("泛型搜索：" + companyNamesStr, false, null);

                        // 查询各平台数据
                        JsonArray info_Fofa = fofaSearch.getInfoByBodyFilterIcp_Fofa(companyNameStr);
                        JsonArray info_Hunter = hunterSearch.getInfoByBodyFilterIcp_Hunter(companyNameStr);
                        JsonArray info_Quake = quakeSearch.getInfoByBodyFilteIcp_Quake(companyNameStr);
                        JsonArray info_Shodan = shodanSearch.getInfoByBodyFilterIcp_shodan(companyNameStr);
                        JsonArray info_Zoomeye = zoomeyeSearch.getInfoByBodyFilterIcp_zoomeye(companyNameStr);

                        // 整合影子资产并过滤非已确定的资产
                        paneInfoSearch.updateEchoVBox("数据整合：" + companyNameStr, false, null);
                        List<DomainInfo> tmpMergedList = DomainInfoMerger.mergeDomainInfos(info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                        paneInfoSearch.updateEchoVBox("过滤非已确定的资产", false, null);
                        tmpMergedList = DomainInfoMerger.getUniqueDomainInfoInB(domainInfoList, tmpMergedList);


                        // 根据图标相似度和内容识别关联资产
                        paneInfoSearch.updateEchoVBox("根据图标相似度、内容识别过滤资产：" + companyNameStr, false, null);
                        List<DomainInfo> relevantDomains = new ArrayList<>();
                        for (DomainInfo domainInfo : tmpMergedList) {
                            String baseUrl = getBaseUrl(domainInfo);
                            if (baseUrl != null && !baseUrl.isEmpty() && AiUtils.getContentRelevance_Ai(baseUrl, companyNamesStr, "tmpDomain")) {
                                relevantDomains.add(domainInfo);
                            }
                        }

                        // 去重
                        paneInfoSearch.updateEchoVBox("数据去重：" + companyNameStr, false, null);
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
            for (String ip : addedIps) {
                paneInfoSearch.updateEchoVBox("平台检索IP：" + ip, false, null);

                // 获取不同平台的 IP 信息
                JsonArray info_Fofa = fofaSearch.getInfoByIp_Fofa(ip);
                JsonArray info_Hunter = hunterSearch.getInfoByIp_Hunter(ip);
                JsonArray info_Quake = quakeSearch.getInfoByIp_Quake(ip);
                JsonArray info_Shodan = shodanSearch.getInfoByIp_Shodan(ip);
                JsonArray info_Zoomeye = zoomeyeSearch.getInfoByIp_Zoomeye(ip);

                // 合并信息
                paneInfoSearch.updateEchoVBox("数据整合：" + ip, false, null);
                List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos(info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                domainInfoList.addAll(mergedList);  // 添加到总的域名信息列表
            }
            if(addedIps.size()>0) {
                paneInfoSearch.updateEchoVBox("第二波平台检索IP", true, null);
            }


            // 剔除cdnIp，进行C段聚合
            paneInfoSearch.updateEchoVBox("剔除CDN_Ip，C段聚合", false, null);
            Map<String, Integer> getFrequentCSegments = CipAggregator.getFrequentCSegments(newIpSet);
            paneInfoSearch.updateEchoVBox("剔除CDN_Ip，C段聚合", true, null);

            if(getFrequentCSegments.size()>0) {
                paneInfoSearch.updateEchoVBox("平台检索C段", false, null);
                for (String ip : getFrequentCSegments.keySet()) {
                    // 提交每个 IP 查询任务
                    paneInfoSearch.updateEchoVBox("平台检索C段：" + ip, false, null);

                    // 获取不同平台的 C段 信息
                    JsonArray info_Fofa = fofaSearch.getInfoByIp_Fofa(ip);
                    JsonArray info_Hunter = hunterSearch.getInfoByIp_Hunter(ip);
                    JsonArray info_Quake = quakeSearch.getInfoByIp_Quake(ip);
                    JsonArray info_Shodan = shodanSearch.getInfoByIp_Shodan(ip);
                    JsonArray info_Zoomeye = zoomeyeSearch.getInfoByIp_Zoomeye(ip);

                    // 合并信息
                    paneInfoSearch.updateEchoVBox("数据整合：" + ip, false, null);
                    List<DomainInfo> mergedList = DomainInfoMerger.mergeDomainInfos(info_Fofa, info_Hunter, info_Quake, info_Shodan, info_Zoomeye);
                    domainInfoList.addAll(mergedList);  // 添加到总的域名信息列表
                }
                paneInfoSearch.updateEchoVBox("平台检索C段", true, null);
            }
        }

        // 去重
        paneInfoSearch.updateEchoVBox("第三波数据去重", false, null);
        domainInfoList = DomainInfoMerger.mergeDomainInfoList(domainInfoList);
        paneInfoSearch.updateEchoVBox("第三波数据去重", true, null);


        boolean isUseGoogle = assetObj.isUseGoogle();
        // 使用线程安全的集合来存储已处理过的域名信息
        if(isUseGoogle) {
            paneInfoSearch.updateEchoVBox("检索Google信息泄露", false, null);
            ConcurrentMap<String, DoDomainInfo> doGoogleDomainInfoMap = new ConcurrentHashMap<>();
            for (DomainInfo domainInfo : domainInfoList) {
                Future<?> future = executor.submit(() -> {
                    String domainStr = domainInfo.getDomain();
                    if (domainStr != null && !domainStr.isEmpty()) {
                        // 使用 computeIfAbsent 避免重复查询
                        DoDomainInfo doDomainInfo = doGoogleDomainInfoMap.computeIfAbsent(domainStr, domain -> {
                            DoDomainInfo newDoDomainInfo = new DoDomainInfo();
                            newDoDomainInfo.setDomain(domain);

                            paneInfoSearch.updateEchoVBox("检索Google信息泄露：" + domainStr, false, null);
                            JsonArray googleLeakage = googleSearch.searchLeakageByDomain(domain);
                            newDoDomainInfo.setGoogldLeakage(googleLeakage);

                            return newDoDomainInfo;
                        });

                        domainInfo.setGoogldLeakage(doDomainInfo.getGoogldLeakage());
                    }
                });

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
            ExecutorServiceManager.getInstance().forceShutdown();

            paneInfoSearch.updateEchoVBox("检索Google信息泄露", true, null);
        }

        boolean isUseGithub = assetObj.isUseGithub();
        if(isUseGithub) {
            paneInfoSearch.updateEchoVBox("检索Github信息泄露", false, null);
            // 使用线程安全的集合来存储已处理过的域名信息
            ConcurrentMap<String, DoDomainInfo> doGitDomainInfoMap = new ConcurrentHashMap<>();
            for (DomainInfo domainInfo : domainInfoList) {
                Future<?> future = executor.submit(() -> {
                    String domainStr = domainInfo.getDomain();
                    if (domainStr != null && !domainStr.isEmpty()) {
                        // 使用 computeIfAbsent 避免重复查询
                        DoDomainInfo doDomainInfo = doGitDomainInfoMap.computeIfAbsent(domainStr, domain -> {
                            DoDomainInfo newDoDomainInfo = new DoDomainInfo();
                            newDoDomainInfo.setDomain(domain);

                            paneInfoSearch.updateEchoVBox("检索Github信息泄露：" + domainStr, false, null);;
                            JsonArray gitRepoLeakage = gitHubLeakage.getRepo(companyNameSet, domain);
                            newDoDomainInfo.setGitRepoLeakage(gitRepoLeakage);

                            return newDoDomainInfo;
                        });

                        domainInfo.setGitRepoLeakage(doDomainInfo.getGitRepoLeakage());
                    }
                });

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
            ExecutorServiceManager.getInstance().forceShutdown();

            paneInfoSearch.updateEchoVBox("检索Github信息泄露", true, null);
        }

        paneInfoSearch.updateEchoVBox("第二波全量深度信息探测", false, null);
        for (DomainInfo domainInfo : domainInfoList) {
            Future<?> future = executor.submit(() -> {
                String domainStr = domainInfo.getDomain();
                if (domainStr != null && !domainStr.isEmpty()) {
                    // 针对尚未获取网站信息的域名进行处理
                    if (domainInfo.isDoWebInfoMap()) {
                        // 获取网站信息
                        String baseUrl = getBaseUrl(domainInfo);
                        if (baseUrl != null && !baseUrl.isEmpty()) {
                            paneInfoSearch.updateEchoVBox("第二波全量深度信息探测: " + baseUrl, false, null);
                            domainInfo.setWebInfoMap(Utils.getWebInfo(baseUrl, hasCrawlLinks, hasFindSensitiveInfo, maxDepth, maxSubPathCount));
                        }
                        domainInfo.setDoWebInfoMap(true);
                    }
                }
            });

            // 添加到 futures 列表，稍后等待所有任务完成
            futures.add(future);
        }
        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get(); // 阻塞直到任务完成
            } catch (CancellationException ce) {} catch (Exception e) {
                if(debugMode) e.printStackTrace();
            }
        }
        // 停止所有线程
        ExecutorServiceManager.getInstance().forceShutdown();

        paneInfoSearch.updateEchoVBox("第二波全量深度信息探测", true, null);

        return domainInfoList;
    }

    public String getBaseUrl(DomainInfo domainInfo){
        String baseUrl = domainInfo.getUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = domainInfo.getDomain();
            String port = domainInfo.getPort();
            if (baseUrl == null || baseUrl.isEmpty()){
                baseUrl = domainInfo.getIp();
            }
            if (port != null && !port.isEmpty()) {
                baseUrl += ":" + port;
            }
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
            JsonObject companyDetailsMap = GetCompany.getCompanyDetails_chinaz(companyList.get(0).getAsJsonObject().get("企业ID").getAsString());

            JsonArray icpInfo = companyDetailsMap.getAsJsonArray("网站备案");

            for(JsonElement jsonElement : icpInfo) {
                String icpNo = jsonElement.getAsJsonObject().get("备案号").getAsString();
                String domainStr = jsonElement.getAsJsonObject().get("网站域名").getAsString();
                assetObj.addIcp(icpNo);
                icpNoSet.add(icpNo);
                tmpDomainSet.add(domainStr.replace("*.", ""));
            }
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("type", DataTypeConstants.ICP);
            dataMap.put("data", companyDetailsMap);
            paneInfoSearch.updateEchoVBox("检索公司相关的备案号及域名信息", true, dataMap);
        }

        paneInfoSearch.updateEchoVBox("检索公司相关的微信公众号、APP、子公司信息", false, null);
        JsonArray companyInfoMap = aiqichaSearch.getCompanyInfoIteration(companyName);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("type", DataTypeConstants.WXAPPSUBCOM);
        dataMap.put("data", companyInfoMap);
        paneInfoSearch.updateEchoVBox("检索公司及符合权重的子公司相关的备案号、域名、微信公众号、APP等信息", true, dataMap);

        boolean hasSubCompanyName = false;
        for(JsonElement jsonElement : companyInfoMap) {
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
                    // 获取企业别名by_Ai
                    paneInfoSearch.updateEchoVBox("获取[" + subCompanyName + " ]企业别名_By_Ai", false, null);
                    Set<String> subCompanyNameSet = AiUtils.getCompanyName_Ai(subCompanyName);
                    paneInfoSearch.updateEchoVBox("修改[" + subCompanyName + " ]企业别名_By_User", false, null);
                    paneInfoSearch.showCompanyNameChoosePaneBox(subCompanyNameSet);
                    subCompanyNameSet = paneInfoSearch.waitAndGetCompanyNameSet();
                    paneInfoSearch.updateEchoVBox("获取[" + subCompanyName + " ]企业别名", true, null);

                    List<DomainInfo> tmpDomainInfoList = getData(assetObj, subCompanyNameSet, subComDomainSet, subIcpNoSet);

                    NetAssets netAssets = new NetAssets();
                    netAssets.setCompanyName(subCompanyName);
                    netAssets.setDomainInfoList(tmpDomainInfoList);
                    assetObj.addCompanyDomainInfoList(netAssets);
                }
            }
        }

        if(!hasSubCompanyName){
            paneInfoSearch.updateEchoVBox("获取[" + companyName + " ]企业别名_By_Ai", false, null);
            Set<String> companyNameSet = AiUtils.getCompanyName_Ai(companyName);
            paneInfoSearch.updateEchoVBox("修改[" + companyName + " ]企业别名_By_User", false, null);
            paneInfoSearch.showCompanyNameChoosePaneBox(companyNameSet);
            companyNameSet = paneInfoSearch.waitAndGetCompanyNameSet();
            paneInfoSearch.updateEchoVBox("获取[" + companyName + " ]企业别名", true, null);

            List<DomainInfo> tmpDomainInfoList = getData(assetObj, companyNameSet, tmpDomainSet, icpNoSet);

            NetAssets netAssets = new NetAssets();
            netAssets.setCompanyName(companyName);
            netAssets.setDomainInfoList(tmpDomainInfoList);
            assetObj.addCompanyDomainInfoList(netAssets);
        }

        if(hasIcpNoSet==null&& hasTmpDomainSet==null) paneInfoSearch.updateEchoVBox("查询结束", true, null);
    }


    public static String getFirstFromSet(Set<String> domain) {
        for (String value : domain) {
            return value;
        }
        return null;
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

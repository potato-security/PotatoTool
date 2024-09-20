package com.potato.potatotool.content.redTeam;

import com.opencsv.CSVReader;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.opencsv.CSVWriter;
import com.potato.potatotool.utils.ExecutorServiceManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static com.potato.potatotool.utils.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2023/4/12 14:21
 */
public class kbCheck {

    // 版本号和版本之间的映射表，以正确识别
    // 系统信息输出中指定的Windows 10/11/Server 2016/2019/2022版本
    private static final Map<Integer, String> buildNumbers = new LinkedHashMap<>();
    static {
        buildNumbers.put(10240, "1507");
        buildNumbers.put(10586, "1511");
        buildNumbers.put(14393, "1607");
        buildNumbers.put(15063, "1703");
        buildNumbers.put(16299, "1709");
        buildNumbers.put(17134, "1803");
        buildNumbers.put(17763, "1809");
        buildNumbers.put(18362, "1903");
        buildNumbers.put(18363, "1909");
        buildNumbers.put(19041, "2004");
        buildNumbers.put(19042, "20H2");
        buildNumbers.put(19043, "21H1");
        buildNumbers.put(19044, "21H2"); // Windows 10
        buildNumbers.put(19045, "22H2");
        buildNumbers.put(20348, "21H2"); // Windows Server 2022
        buildNumbers.put(22000, "21H2"); // Windows 11
        buildNumbers.put(22621, "22H2");
        buildNumbers.put(22631, "23H2");
        buildNumbers.put(26100, "24H2");
    }

    public static List<Map<String, String>> cves = new ArrayList<>();
    static {
        cves = init();
    }

    private static final Map<String, String> DEFAULT_HEADERS = new HashMap<>();
    static {
        // 初始化默认请求头
        DEFAULT_HEADERS.put("authority", "www.catalog.update.microsoft.com");
        DEFAULT_HEADERS.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        DEFAULT_HEADERS.put("sec-fetch-mode", "navigate");
        DEFAULT_HEADERS.put("sec-fetch-user", "?1");
        DEFAULT_HEADERS.put("sec-fetch-site", "none");
    }

    public static String productfilter;
    public static String win;
    public static int mybuild;
    public static String version = null;
    public static String arch;
    public static List<String> hotfixes = new ArrayList<>();

    public static ExecutorService executor = ExecutorServiceManager.getInstance().getExecutor();
    public static List<Future<?>> futures = ExecutorServiceManager.futures;


    /**
     * 初始化命令集
     *
     * @return json数据
     */
    public static List<Map<String, String>> init() {

        List<Map<String, String>> csvData = new ArrayList<>();

        try (InputStream winKbInfoInputStream = getResourceStream("winKbInfo");
             CSVReader reader = new CSVReader(new InputStreamReader(winKbInfoInputStream, StandardCharsets.UTF_8))) {

            String[] headers = reader.readNext();
            if (headers == null) {
                throw new RuntimeException("CSV文件为空或无法读取列标题");
            }
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], nextLine[i]);
                }
                csvData.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return csvData;

    }


    // 解析systeminfo输出信息
    public static void determineProduct(String systeminfo) {

        // 修复7_sp1_x64_enterprise_fr_systeminfo_powershell.txt
        systeminfo = systeminfo.replace('\u00A0', '\u0020');

        // 操作系统版本
        Pattern regexVersion = Pattern.compile(".*?((\\d+\\.?){3}) ((Service Pack (\\d)|N\\/\\w|.+) )?[ -\\xa5]+ (\\d+).*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher versionMatcher = regexVersion.matcher(systeminfo);
        if (!versionMatcher.find()) {
            throw new IllegalArgumentException("Not able to detect OS version based on provided input file.");
        }

        mybuild = Integer.parseInt(versionMatcher.group(6));
        String servicePack = versionMatcher.group(5);

        // 操作系统名称
        Pattern winPattern = Pattern.compile(".*?Microsoft[\\(R\\)]{0,3} Windows[\\(R\\)?]{0,3} ?(Serverr? )?(\\d+\\.?\\d?( R2)?|XP|VistaT).*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher winMatcher = winPattern.matcher(systeminfo);
        if (!winMatcher.find()) {
            throw new IllegalArgumentException("Not able to detect OS name based on provided input file.");
        }
        win = winMatcher.group(2);

        // 系统类型
        Pattern archPattern = Pattern.compile(".*?([\\w\\d]+?)-based PC.*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher archMatcher = archPattern.matcher(systeminfo);
        arch = archMatcher.find() ? archMatcher.group(1) : "x64";

        // 修补程序
        hotfixes = getHotfixes(systeminfo);

        // 根据内部版本确定Windows 10/11版本
        for (Map.Entry<Integer, String> entry : buildNumbers.entrySet()) {
            if (mybuild == entry.getKey()) {
                version = entry.getValue();
                break;
            } else if(mybuild > entry.getKey()) {
                version = entry.getValue();
            } else {
                break;
            }
        }

        // 编译产品过滤器的名称
        buildProductFilter(servicePack);

    }

    // 从提供的文本中提取kb信息
    private static List<String> getHotfixes(String text) {
        Pattern hotfixPattern = Pattern.compile(".*KB(\\d+).*", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher hotfixMatcher = hotfixPattern.matcher(text);
        while (hotfixMatcher.find()) {
            hotfixes.add(hotfixMatcher.group(1));
        }
        return new ArrayList<>(new HashSet<>(hotfixes));
    }

    // 从提供的文本中提取产品构建信息
    private static void buildProductFilter(String servicepack) {
        if (!Arrays.asList("XP", "VistaT", "2003", "2003 R2").contains(win)) {
            arch = arch.equals("X86") ? "32-bit" : "x64-based";
        }

        switch (win) {
            case "XP":
                productfilter = "Microsoft Windows XP" + (arch.equals("X86") ? "" : " Professional " + arch + " Edition");
                break;
            case "VistaT":
                productfilter = "Windows Vista" + (arch.equals("x86") ? "" : " " + arch + " Edition");
                break;
            case "7":
            case "8":
            case "8.1":
                productfilter = "Windows " + win + " for " + arch + " Systems";
                break;
            case "10":
            case "11":
                productfilter = "Windows " + win + " Version " + version + " for " + arch + " Systems";
                break;
            case "2003":
            case "2008":
            case "2008 R2":
            case "2012":
            case "2012 R2":
            case "2016":
            case "2019":
            case "2022":
                productfilter = "Windows Server " + win + (version == null ? "" : " " + version) + (arch.equals("x64") ? " x64 Edition" : "");
                break;
            default:
                throw new RuntimeException("Failed assessing Windows version " + win);
        }

        if (servicepack != null && !servicepack.isEmpty()) {
            productfilter += " Service Pack " + servicepack;
        }

    }

    // 筛选缺少的kb
    public static Map<String, List<Map<String, String>>> determineMissingPatches() {

        List<String> hotfixesOrig = new ArrayList<>(hotfixes);
        List<Map<String, String>> filtered = new ArrayList<>();

        // 带有Service Pack的产品
        if (productfilter.contains("Service Pack")) {
            for (Map<String, String> cve : cves) {
                if (!cve.get("影响产品").contains(productfilter)) {
                    continue;
                }

                cve.put("相关的", "true");
                filtered.add(cve);

                String supersedes = cve.get("替代KB编号");
                if (supersedes != null && !supersedes.isEmpty()) {
                    hotfixesOrig.add(supersedes);
                }
            }
        } else {
            // 确保如果productfilter不包含Service Pack，不会在产品名称中列出包含Service Pack的操作系统版本
            String productfilterSP = productfilter + " Service Pack";
            for (Map<String, String> cve : cves) {
                if (!cve.get("影响产品").contains(productfilter) ||
                        cve.get("影响产品").contains(productfilterSP)) {
                    continue;
                }

                cve.put("相关的", "true");
                filtered.add(cve);

                String supersedes = cve.get("替代KB编号");
                if (supersedes != null && !supersedes.isEmpty()) {
                    hotfixesOrig.add(supersedes);
                }
            }
        }

        // 收集已被替换的补丁，并于系统上安装的补丁合并
        String superseededHotfixes = String.join(";", hotfixesOrig);

        Set<String> marked = new HashSet<>();
        markSuperseededHotfix(filtered, superseededHotfixes, marked);

        // 检查剩余的KB是否包含重叠，例如单独的安全修补程序，该修补程序也包含在每月汇总更新中
        Set<String> supersedes = filtered.stream()
                .filter(cve -> cve.get("相关的").equals("true"))
                .map(cve -> cve.get("替代KB编号"))
                .collect(Collectors.toSet());

        filtered.forEach(cve -> {
            if (supersedes.contains(cve.get("KB编号"))) {
                cve.put("相关的", "false");
            }
        });

        List<Map<String, String>> found = filtered.stream()
                .filter(cve -> cve.get("相关的").equals("true"))
                .collect(Collectors.toList());

        found.forEach(f -> f.remove("相关的"));

        Map<String, List<Map<String, String>>> result = new HashMap<>();
        result.put("filtered", filtered);
        result.put("found", found);

        return result;
    }

    // 当kb被替代，递归标记不相关
    private static void markSuperseededHotfix(List<Map<String, String>> filtered,
                                              String superseeded, Set<String> marked) {
        for (String ssitem : superseeded.split(";")) {
            for (Map<String, String> ss : filtered.stream()
                    .filter(cve -> cve.get("相关的").equals("true") && ssitem.equals(cve.get("KB编号")))
                    .collect(Collectors.toList())) {

                ss.put("相关的", "false");

                String supersedes = ss.get("替代KB编号");
                if (supersedes != null && !supersedes.isEmpty() && !marked.contains(supersedes)) {
                    marked.add(supersedes);
                    markSuperseededHotfix(filtered, supersedes, marked);
                }
            }
        }
    }

    // 获取最新安装的补丁信息
    public static Map<String, String> getMostRecentKb(List<Map<String, String>> results) {
        // 提取所有的日期，并转换为整数类型
        Optional<Integer> maxDate = results.stream()
                .map(r -> r.get("发布日期"))
                .filter(date -> date != null && !date.isEmpty())
                .map(Integer::parseInt)
                .max(Integer::compareTo);

        // 如果找到最大日期，返回相应的Map；否则返回null
        return maxDate.map(date ->
                results.stream()
                        .filter(kb -> String.valueOf(date).equals(kb.get("发布日期")))
                        .findFirst()
                        .orElse(null)
        ).orElse(null);
    }

    // 过滤Windows Server操作系统的重复CVE，这些操作系统通常具有完全相同的“Windows Server 2XXX”和“Windows Server 3XXX(Server Core installation)”CVE
    private static List<Map<String, String>> filterDuplicates(List<Map<String, String>> found) {
        if (!productfilter.contains("Windows Server")){
            return found;
        }

        Set<String> cveSet = found.stream()
                .map(cve -> cve.get("CVE编号"))
                .collect(Collectors.toSet());
        List<Map<String, String>> newfound = new ArrayList<>();

        for (String cve : cveSet) {
            List<Map<String, String>> coreResults = found.stream()
                    .filter(cr -> cr.get("CVE编号").equals(cve) &&
                            cr.get("影响产品").contains("Server Core"))
                    .collect(Collectors.toList());

            if (coreResults.isEmpty()) {
                List<Map<String, String>> normalResults = found.stream()
                        .filter(nr -> nr.get("CVE编号").equals(cve))
                        .collect(Collectors.toList());
                newfound.addAll(normalResults);
                continue;
            }

            for (Map<String, String> r : coreResults) {
                List<Map<String, String>> regularCounterparts = found.stream()
                        .filter(c -> !c.get("影响产品").contains("Server Core") &&
                                c.get("CVE编号").equals(r.get("CVE编号")) &&
                                c.get("KB编号").equals(r.get("KB编号")) &&
                                c.get("标题").equals(r.get("标题")) &&
                                c.get("影响组件").equals(r.get("影响组件")) &&
                                c.get("严重性").equals(r.get("严重性")) &&
                                c.get("漏洞影响").equals(r.get("漏洞影响")) &&
                                c.get("漏洞利用").equals(r.get("漏洞利用")))
                        .collect(Collectors.toList());

                if (!regularCounterparts.isEmpty()) {
                    newfound.addAll(regularCounterparts);
                } else {
                    newfound.add(r);
                }
            }
        }

        return newfound;
    }

    // 根据用户指定的筛选器隐藏结果
    private static List<Map<String, String>> applyDisplayFilters(List<Map<String, String>> found,
                                                                 List<String> hiddenVulns,
                                                                 boolean onlyExploits,
                                                                 List<String> impacts,
                                                                 List<String> severities) {
        if (found == null) found = Collections.emptyList();
        if (hiddenVulns == null) hiddenVulns = Collections.emptyList();
        if (impacts == null) impacts = Collections.emptyList();
        if (severities == null) severities = Collections.emptyList();
        List<String> hiddenVulnsLower = hiddenVulns.stream().map(String::toLowerCase).collect(Collectors.toList());
        List<String> impactsLower = impacts.stream().map(String::toLowerCase).collect(Collectors.toList());
        List<String> severitiesLower = severities.stream().map(String::toLowerCase).collect(Collectors.toList());


        List<Map<String, String>> filtered = new ArrayList<>();
        for (Map<String, String> cve : found) {
            boolean add = true;

            // 检查隐藏的漏洞
            for (String hidden : hiddenVulnsLower) {
                if (cve.get("影响组件").toLowerCase().contains(hidden) ||
                        cve.get("影响产品").toLowerCase().contains(hidden) ||
                        cve.get("标题").toLowerCase().contains(hidden)) {
                    add = false;
                    break;
                }
            }

            // 检查漏洞影响
            if (add) {
                for (String impact : impactsLower) {
                    if (cve.get("漏洞影响").toLowerCase().contains(impact)) {
                        add = true;
                        break;
                    } else {
                        add = false;
                    }
                }
            }

            // 检查严重性
            if (add) {
                for (String severity : severitiesLower) {
                    if (cve.get("严重性").toLowerCase().contains(severity)) {
                        add = true;
                        break;
                    } else {
                        add = false;
                    }
                }
            }

            // 如果CVE通过了所有检查，则将其添加到筛选列表中
            if (add) {
                filtered.add(cve);
            }
        }

        if (onlyExploits) {
            filtered = filtered.stream()
                    .filter(cve -> !cve.get("漏洞利用").isEmpty())
                    .collect(Collectors.toList());
        }

        return filtered;
    }

    // 在Microsoft Update目录中查找被取代的KB
    private static List<Map<String, String>> applyMucFilter(List<Map<String, String>> found, List<String> kbsInstalled) {
        if (found == null || found.isEmpty()) {
            return Collections.emptyList();
        }

        if (kbsInstalled == null) {
            kbsInstalled = Collections.emptyList();
        }

        Map<String, Set<String>> supersededBy = new HashMap<>();
        List<Future<?>> futures = found.stream()
                .map(cve -> executor.submit(() -> {
                    String kb = cve.get("KB编号");
                    if (!supersededBy.containsKey(kb)) {
                        supersededBy.put(kb, lookupSupersedence(kb));
                    }
                    return null;
                }))
                .collect(Collectors.toList());

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Set<String> finalKbsInstalled = new HashSet<>(kbsInstalled);
        return found.stream()
                .filter(cve -> !supersededBy.getOrDefault(cve.get("KB编号"), Collections.emptySet()).stream().anyMatch(finalKbsInstalled::contains))
                .collect(Collectors.toList());
    }


    private static Set<String> lookupSupersedence(String kb) {
        Set<String> kbids = new HashSet<>();
        try {
            Document doc = Jsoup.connect("https://www.catalog.update.microsoft.com/Search.aspx?q=" + kb).headers(DEFAULT_HEADERS).get();
            Element rows = doc.getElementById("ctl00_catalogBody_updateMatches");
            if (rows != null) {
                // 查找替代编号

                Elements updates = rows.select("a[onclick*=goToDetails]");
                List<Future<Set<String>>> futures = updates.stream()
                        .map(a -> executor.submit(() -> lookupSupersedenceByUid(a.attr("id").split("_")[0])))
                        .collect(Collectors.toList());

                for (Future<Set<String>> future : futures) {
                    kbids.addAll(future.get());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kbids;
    }

    private static Set<String> lookupSupersedenceByUid(String uid) {
        Set<String> kbids = new HashSet<>();
        try {
            Document doc = Jsoup.connect("https://www.catalog.update.microsoft.com/ScopedViewInline.aspx?updateid=" + uid).headers(DEFAULT_HEADERS)
                    .timeout(60000).get();
            Elements supers = doc.select("div#supersededbyInfo");
            if (supers.size() == 1) {
                String text = supers.first().text();
                Matcher matcher = Pattern.compile("KB[0-9]+").matcher(text);
                while (matcher.find()) {
                    kbids.add(matcher.group().replaceFirst("^KB", ""));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kbids;
    }

    // 拆分KB列表和可用的潜在服务包/累积更新
    public static Map<String, Object> getPatchesServicepacks(List<Map<String, String>> filtered) {

        Map<String, Object> resultMap = new HashMap<>();

        // 提取可用的 Service Pack（如果有）
        List<Map<String, String>> sp = filtered.stream()
                .filter(c -> c.get("CVE编号").startsWith("SP"))
                .collect(Collectors.toList());

        if (!sp.isEmpty()) {
            Map<String, String> spEntry = sp.get(0);  // 应该只有一个结果

            // 只关注 OS+架构，当前的服务包与此无关
            String updatedProductFilter = productfilter.replaceAll(" Service Pack \\d", "");

            // 确定操作系统可用的 Service Pack，并确定可用的最新版本
            List<Map<String, String>> servicepacks = cves.stream()
                    .filter(c -> c.get("CVE编号").startsWith("SP") && c.get("影响产品").contains(updatedProductFilter))
                    .collect(Collectors.toList());
            Map<String, String> lastPatch = getLastPatch(servicepacks, spEntry);

            // 从常规 KB 输出中删除 Service Pack
            List<Map<String, String>> kbs = filtered.stream()
                    .filter(c -> !c.get("CVE编号").startsWith("SP"))
                    .collect(Collectors.toList());

            resultMap.put("kbs", kbs);
            resultMap.put("lastPatch", lastPatch);
        } else {
            resultMap.put("kbs", filtered);
            resultMap.put("lastPatch", null);
        }

        return resultMap;
    }

    // 获取最新补丁，递归地追溯到取代所提供记录的记录
    private static Map<String, String> getLastPatch(List<Map<String, String>> servicepacks, Map<String, String> kb) {
        // 递归查找取代当前补丁的补丁
        List<Map<String, String>> results = servicepacks.stream()
                .filter(c -> c.get("替代KB编号").equals(kb.get("KB编号")))
                .collect(Collectors.toList());

        if (!results.isEmpty()) {
            return getLastPatch(servicepacks, results.get(0));
        } else {
            return kb;
        }
    }

    public static List<Map<String, String>> filterKB(String inputText,boolean isMucFilter){
        determineProduct(inputText);

        Map<String, List<Map<String, String>>> determineMissingPatches = determineMissingPatches();
        List<Map<String, String>> filtered = determineMissingPatches.get("filtered");
        List<Map<String, String>> found = determineMissingPatches.get("found");

        // 如果是windows serer类型，过滤重复漏洞
        filtered = filterDuplicates(found);

        // 在Microsoft Update目录中查找被取代的KB
        if(isMucFilter) {
            filtered = applyMucFilter(filtered, hotfixes);
        }

        // 拆分KB列表和可用的潜在服务包/累积更新
        Map<String, Object> patchesServicepacks = getPatchesServicepacks(filtered);
        List<Map<String,String>> kbs = (List<Map<String, String>>) patchesServicepacks.get("kbs");

        // 按发布日期从大到小排序
        kbs.sort((map1, map2) -> map2.get("发布日期").compareTo(map1.get("发布日期")));

        return kbs;
    }

    public static void main(String[] args) {
        String systeminfo = "主机名:           DESKTOP-EMGF5BE\n" +
                "OS 名称:          Microsoft Windows 10 专业版\n" +
                "OS 版本:          10.0.18363 暂缺 Build 18363\n" +
                "OS 制造商:        Microsoft Corporation\n" +
                "OS 配置:          独立工作站\n" +
                "OS 构建类型:      Multiprocessor Free\n" +
                "注册的所有人:     Windows 用户\n" +
                "注册的组织:\n" +
                "产品 ID:          00331-10000-00001-AA265\n" +
                "初始安装日期:     2021-9-17, 21:46:11\n" +
                "系统启动时间:     2024-4-24, 22:45:05\n" +
                "系统制造商:       VMware, Inc.\n" +
                "系统型号:         VMware7,1\n" +
                "系统类型:         x64-based PC\n" +
                "处理器:           安装了 2 个处理器。\n" +
                "                  [01]: Intel64 Family 6 Model 158 Stepping 10 GenuineIntel ~2592 Mhz\n" +
                "                  [02]: Intel64 Family 6 Model 158 Stepping 10 GenuineIntel ~2592 Mhz\n" +
                "BIOS 版本:        VMware, Inc. VMW71.00V.18452719.B64.2108091906, 2021-8-9\n" +
                "Windows 目录:     C:\\Windows\n" +
                "系统目录:         C:\\Windows\\system32\n" +
                "启动设备:         \\Device\\HarddiskVolume1\n" +
                "系统区域设置:     zh-cn;中文(中国)\n" +
                "输入法区域设置:   zh-cn;中文(中国)\n" +
                "时区:             (UTC+08:00) 北京，重庆，香港特别行政区，乌鲁木齐\n" +
                "物理内存总量:     5,119 MB\n" +
                "可用的物理内存:   2,885 MB\n" +
                "虚拟内存: 最大值: 5,439 MB\n" +
                "虚拟内存: 可用:   3,403 MB\n" +
                "虚拟内存: 使用中: 2,036 MB\n" +
                "页面文件位置:     C:\\pagefile.sys\n" +
                "域:               WORKGROUP\n" +
                "登录服务器:       \\\\DESKTOP-EMGF5BE\n" +
                "修补程序:         安装了 6 个修补程序。\n" +
                "                  [01]: KB4601556\n" +
                "                  [02]: KB4513661\n" +
                "                  [03]: KB4516115\n" +
                "                  [04]: KB4517245\n" +
                "                  [05]: KB4521863\n" +
                "                  [06]: KB4517389\n" +
                "网卡:             安装了 2 个 NIC。\n" +
                "                  [01]: Intel(R) 82574L Gigabit Network Connection\n" +
                "                      连接名:      Ethernet0\n" +
                "                      启用 DHCP:   是\n" +
                "                      DHCP 服务器: 172.16.246.254\n" +
                "                      IP 地址\n" +
                "                        [01]: 172.16.246.250\n" +
                "                        [02]: fe80::8403:21ce:5678:4ec6\n" +
                "                  [02]: Bluetooth Device (Personal Area Network)\n" +
                "                      连接名:      蓝牙网络连接\n" +
                "                      状态:        媒体连接已中断\n" +
                "Hyper-V 要求:     已检测到虚拟机监控程序。将不显示 Hyper-V 所需的功能。";

        determineProduct(systeminfo);

        System.out.println("当前电脑系统信息：");
        System.out.println("- Name：" + productfilter);
        System.out.println("- Generation：" + win);
        System.out.println("- Build：" + mybuild);
        System.out.println("- Version：" + version);
        System.out.println("- Architecture：" + arch);
        System.out.println("- Hotfixes：" + hotfixes);

        Map<String, List<Map<String, String>>> determineMissingPatches = determineMissingPatches();
        List<Map<String, String>> filtered = determineMissingPatches.get("filtered");
        List<Map<String, String>> found = determineMissingPatches.get("found");

//        // 选择性使用
//        // 获取最新安装的补丁信息
//        Map<String, String> recentKb = getMostRecentKb(found);
//        if (recentKb != null) {
//            int recentDate = Integer.parseInt(recentKb.get("发布日期"));
//
//            // 筛选发布日期大于等于当前最新安装的补丁发布日期的元素
//            found = found.stream()
//                    .filter(kb -> Integer.parseInt(kb.get("发布日期")) >= recentDate)
//                    .collect(Collectors.toList());
//        }

        // 如果是windows serer类型，过滤重复漏洞
        found = filterDuplicates(found);

        // 如果存在过滤
        List<String> hiddenVulns = null;
        boolean onlyExploits = false;
        List<String> impacts = null;
        List<String> severities = null;
        if((hiddenVulns != null && hiddenVulns.isEmpty()) || onlyExploits || (impacts != null && impacts.isEmpty()) || (severities != null && severities.isEmpty())){
            filtered = applyDisplayFilters(found, hiddenVulns, onlyExploits, impacts, severities);
        }else {
            filtered = found;
        }


        // 在Microsoft Update目录中查找被取代的KB
        filtered = applyMucFilter(filtered, hotfixes);

        // 拆分KB列表和可用的潜在服务包/累积更新
        Map<String, Object> patchesServicepacks = getPatchesServicepacks(filtered);
        List<Map<String,String>> kbs = (List<Map<String, String>>) patchesServicepacks.get("kbs");
        Map<String, String> lastPatch = (Map<String, String>) patchesServicepacks.get("lastPatch");

        // 按发布日期从大到小排序
        kbs.sort((map1, map2) -> map2.get("发布日期").compareTo(map1.get("发布日期")));

        // 统计补丁编号
        Set<String> missingPatches = kbs.stream()
                .map(r -> r.get("KB编号"))
                .collect(Collectors.toSet());
        System.out.println("[-] 缺失补丁数量: " + missingPatches.size());

        // 统计BulletinKB的出现次数
        Map<String, Long> grouped = kbs.stream()
                .filter(r -> r.get("发布日期") != null)
                .collect(Collectors.groupingBy(r -> r.get("KB编号"), Collectors.counting()));
        // 按次数排序并打印结果
        grouped.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    String kb = entry.getKey();
                    long number = entry.getValue();
                    System.out.println("    - KB" + kb + ": 修补了 " + number + " 个漏洞");
                });
        // 显示缺少的Service Pack
        if (lastPatch != null && !lastPatch.isEmpty()) {
            System.out.println("[-] 缺少的Service Pack");
            System.out.println("    - " + lastPatch.get("标题"));
        }

        // 显示额外缺少的KBs（当使用--missing参数时）
        if (missingPatches.size() > grouped.size()) {
            Set<String> foundKBs = grouped.keySet();
            Set<String> difference = new HashSet<>(missingPatches);
            difference.removeAll(foundKBs);

            for (String kb : difference) {
                System.out.println("    - KB" + kb + ": 修补了未知数量的漏洞");
            }
            System.out.println("[I] 在 https://support.microsoft.com/help/KBID 上查看未知补丁的详细信息，\n例如，KB890830 的链接为 https://support.microsoft.com/help/890830");
        }

        // 显示最近的KB发布日期（如果可用）
        if (grouped.isEmpty()) {
            return;
        }

        Map<String, String> foundKB = kbs.stream()
                .max(Comparator.comparing(kb -> kb.get("发布日期")))
                .orElse(Collections.emptyMap());
        String message = "[I] 最近发布日期的KB";
        System.out.println(String.format("%s\n    - ID: KB%s\n    - 发布日期: %s",
                message, foundKB.get("KB编号"), foundKB.get("发布日期")));

        System.out.println("缺失的kbs：");
        System.out.println(kbs);
        System.out.println("缺失服务包：");
        System.out.println(lastPatch);


//        try (CSVWriter writer = new CSVWriter(new FileWriter("csvFile.csv"))) {
//            // 写入CSV头部
//            String[] header = kbs.get(0).keySet().toArray(new String[0]);
//            writer.writeNext(header);
//
//            // 写入数据
//            for (Map<String, String> row : kbs) {
//                String[] values = row.values().toArray(new String[0]);
//                writer.writeNext(values);
//            }
//
//            System.out.println("CSV文件写入成功: " + "csvFile.csv");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


    }
}

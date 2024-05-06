package com.potato.potatotool.content;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.opencsv.CSVReader;

import static com.potato.potatotool.utils.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2023/4/12 14:21
 */
public class kbCheck {

    /**
     * 测试调用，接入时请模拟传参
     * @inputStr    用户传入进程列表 (String)
     */
//    public static void main(String []args) {
//
//        List<Map<String, String>> csvData = init();
//
//        List<String> strList = new ArrayList<>();
//        strList.add("4014329");
//        strList.add("3216916");
//        strList.add("4022721");
//        strList.add("4022168");
//
//        List<Map<String, String>> filteredKB = filterKB(csvData, strList);
//        System.out.println(filteredKB);
//        List<Map<String, String>> filteredExp = filterExp(filteredKB);
//        System.out.println(filteredExp);
//
//        Set<String> uniqueComponents = filteredKB.stream()
//                .map(row -> row.get("影响组件"))
//                .filter(data -> data != null && !data.isEmpty())
//                .collect(Collectors.toSet());
//
//        for (String data : uniqueComponents) {
//            System.out.println("影响组件: " + data);
//        }
//
//        Set<String> uniqueProducts = filteredKB.stream()
//                .map(row -> row.get("影响产品"))
//                .filter(data -> data != null && !data.isEmpty())
//                .collect(Collectors.toSet());
//
//        for (String data : uniqueProducts) {
//            System.out.println("影响产品: " + data);
//        }
//
//    }


    /**
     * 初始化命令集
     * @return  json数据
     */
    public static List<Map<String, String>> init(){

        List<Map<String, String>> csvData = new ArrayList<>();

        try{
            InputStream winKbInfoInputStream = getResourceStream("winKbInfo");
            CSVReader reader = new CSVReader(new InputStreamReader(winKbInfoInputStream));
            String[] headers = reader.readNext(); // 读取列标题
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


    public static List<Map<String, String>> filterKB(List<Map<String, String>> csvData, List<String> strList) {
        return csvData.stream()
                .filter(row -> !strList.contains(row.get("KB编号"))) // 过滤掉存在于strList中的KB编号
                .filter(row -> !strList.contains(row.get("替代KB编号"))) // 过滤掉可以替代的KB编号
                .collect(Collectors.toList());
    }


    public static List<Map<String, String>> filterExp(List<Map<String, String>> csvData) {
        return csvData.stream()
                .filter(row -> row.get("漏洞利用").contains(""))
                .collect(Collectors.toList());
    }


    // 正则提取kb编号  无kb标识
    public static List<Map<String, String>> filterKB(List<Map<String, String>> csvData, String inputText){

        List<String> kbList = new ArrayList<>();

        String regex = "\\bKB(\\d+)\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputText);

        while (matcher.find()) {
            kbList.add(matcher.group(1));
        }
        System.out.println(kbList);

        String osName = "";
        Matcher nameMatcher = Pattern.compile("\\b(OS Name|OS 名称):\\s*(.+)\\b").matcher(inputText);
        if (nameMatcher.find()) {
            osName = nameMatcher.group(2);
        }
        osName = osName.replace("Microsoft ","");
        System.out.println(osName);

        String osVersion = "";
        Matcher versionMatcher = Pattern.compile("\\b(OS Version|OS 版本):\\s*(.+)\\b").matcher(inputText);
        if (versionMatcher.find()) {
            osVersion = versionMatcher.group(2);
        }
        System.out.println(osVersion);

        String sysVersion = "";
        Matcher sysVersionMatcher = Pattern.compile("\\b(System Type|系统类型):\\s*(.+)\\b").matcher(inputText);
        if (sysVersionMatcher.find()) {
            sysVersion = sysVersionMatcher.group(2);
        }
        sysVersion = sysVersion.split(" ")[0];
        System.out.println(sysVersion);

        return filterKB(csvData, kbList);
    }

    public static void main(String []args) {
        String data = "主机名:           DESKTOP-EMGF5BE\n" +
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
        filterKB(init(),data);
//        System.out.println();
    }


}

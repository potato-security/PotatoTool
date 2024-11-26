package com.potato.potatotool.content.blueTeam;

import org.lionsoul.ip2region.SearchTest;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.utils.Constants.getResourceFileTmpPath;
import static com.potato.potatotool.utils.strUtils.joinList_r;


/**
 * @author Potato
 * @date 2023/4/18 10:28
 */
public class ipInfo {

    /**
     * 测试调用，接入时请模拟传参
     */
    public static void main(String []args) throws Exception {

        // String ip = "124.133.247.98"; // 要查询的IP地址
        // 使用纯真数据库 国外的准，暂时不用
        // IPSeeker.I.init(qqwryPath);
        // String ipAddress = IPSeeker.I.getAddress(ip);
        // System.out.println(ipAddress);

        // 使用ip2region
        Searcher searcher = init();

        // 模拟用户传参
        String inputStr="\n活动连接\n\n  协议  本地地址          外部地址        状态           PID\n  TCP    0.0.0.0:21             0.0.0.0:0              LISTENING       1176\n  TCP    0.0.0.0:80             0.0.0.0:0              LISTENING       4\n" +
                "  TCP    0.0.0.0:135            0.0.0.0:0              LISTENING       568\n" +
                "  TCP    0.0.0.0:443            0.0.0.0:0              LISTENING       4\n" +
                "  TCP    0.0.0.0:445            0.0.0.0:0              LISTENING       4\n" +
                "  TCP    0.0.0.0:888            0.0.0.0:0              LISTENING       4\n" +
                "  TCP    0.0.0.0:3000           0.0.0.0:0              LISTENING       3332\n" +
                "  TCP    0.0.0.0:3306           0.0.0.0:0              LISTENING       1256\n" +
                "  TCP    0.0.0.0:3389           0.0.0.0:0              LISTENING       2244\n" +
                "  TCP    0.0.0.0:5985           0.0.0.0:0              LISTENING       4\n" +
                "  TCP    0.0.0.0:6060           0.0.0.0:0              LISTENING       3308\n" +
                "  TCP    0.0.0.0:47001          0.0.0.0:0              LISTENING       4\n" +
                "  TCP    0.0.0.0:49152          0.0.0.0:0              LISTENING       380\n" +
                "  TCP    0.0.0.0:49153          0.0.0.0:0              LISTENING       676\n" +
                "  TCP    0.0.0.0:49154          0.0.0.0:0              LISTENING       712\n" +
                "  TCP    0.0.0.0:49155          0.0.0.0:0              LISTENING       1340\n" +
                "  TCP    0.0.0.0:49160          0.0.0.0:0              LISTENING       472\n" +
                "  TCP    0.0.0.0:49192          0.0.0.0:0              LISTENING       464\n" +
                "  TCP    10.0.24.15:139         0.0.0.0:0              LISTENING       4\n" +
                "  TCP    10.0.24.15:443         66.249.79.35:48026     ESTABLISHED     4\n" +
                "  TCP    10.0.24.15:3000        3.8.123.126:21345      ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        13.40.27.223:21345     ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        35.178.250.170:21345   ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        45.143.201.62:65256    ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        80.66.66.14:28714      ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        89.248.163.166:40969   ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        89.248.165.253:40136   ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        92.63.196.3:42677      ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        104.152.52.56:35942    ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        104.152.52.61:40172    ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        107.174.176.6:56195    ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3000        198.23.149.111:46782   ESTABLISHED     3332\n" +
                "  TCP    10.0.24.15:3389        114.247.188.136:3629   ESTABLISHED     2244\n" +
                "  TCP    10.0.24.15:6060        167.99.201.117:54802   ESTABLISHED     3308\n" +
                "  TCP    10.0.24.15:6060        167.248.133.45:54880   ESTABLISHED     3308\n" +
                "  TCP    10.0.24.15:56113       42.157.129.47:443      TIME_WAIT       0\n" +
                "  TCP    10.0.24.15:56121       169.254.0.203:80       TIME_WAIT       0\n" +
                "  TCP    10.0.24.15:56125       169.254.0.203:80       TIME_WAIT       0\n" +
                "  TCP    10.0.24.15:57048       169.254.0.55:5574      ESTABLISHED     84\n" +
                "  TCP    127.0.0.1:14147        0.0.0.0:0              LISTENING       1176\n" +
                "  TCP    [::]:21                [::]:0                 LISTENING       1176\n" +
                "  TCP    [::]:80                [::]:0                 LISTENING       4\n" +
                "  TCP    [::]:135               [::]:0                 LISTENING       568\n" +
                "  TCP    [::]:443               [::]:0                 LISTENING       4\n" +
                "  TCP    [::]:445               [::]:0                 LISTENING       4\n" +
                "  TCP    [::]:888               [::]:0                 LISTENING       4\n" +
                "  TCP    [::]:3389              [::]:0                 LISTENING       2244\n" +
                "  TCP    [::]:5985              [::]:0                 LISTENING       4\n" +
                "  TCP    [::]:6060              [::]:0                 LISTENING       3308\n" +
                "  TCP    [::]:47001             [::]:0                 LISTENING       4\n" +
                "  TCP    [::]:49152             [::]:0                 LISTENING       380\n" +
                "  TCP    [::]:49153             [::]:0                 LISTENING       676\n" +
                "  TCP    [::]:49154             [::]:0                 LISTENING       712\n" +
                "  TCP    [::]:49155             [::]:0                 LISTENING       1340\n" +
                "  TCP    [::]:49160             [::]:0                 LISTENING       472\n" +
                "  TCP    [::]:49192             [::]:0                 LISTENING       464\n" +
                "  TCP    [::1]:14147            [::]:0                 LISTENING       1176\n" +
                "  UDP    0.0.0.0:123            *:*                                    760\n" +
                "  UDP    0.0.0.0:500            *:*                                    712\n" +
                "  UDP    0.0.0.0:3389           *:*                                    2244\n" +
                "  UDP    0.0.0.0:4500           *:*                                    712\n" +
                "  UDP    0.0.0.0:5355           *:*                                    844\n" +
                "  UDP    10.0.24.15:137         *:*                                    4\n" +
                "  UDP    10.0.24.15:138         *:*                                    4\n" +
                "  UDP    [::]:123               *:*                                    760\n" +
                "  UDP    [::]:500               *:*                                    712\n" +
                "  UDP    [::]:3389              *:*                                    2244\n" +
                "  UDP    [::]:4500              *:*                                    712\n" +
                "  UDP    [::]:5355              *:*                                    844\n" +
                "Extracted IPv6 addresses: [2400:cb00:2048:1::c629:d7a2, 2001:db8:85a3:0:0:8a2e:370:7334 , fe80::1ff:fe23:4567:890a]";

        LinkedHashSet<String> ipList = getIpListFromReg(inputStr);
        LinkedHashMap<String, String> ipPosDict = getIpPosDict(searcher, ipList);
        String inputPosStr = getInputPosStr(searcher, inputStr, ipList);

        // IP抽取
        System.out.println("[√] IP抽取：");
        System.out.println(joinList_r(ipList));

//        // IP抽取+pos标记
//        System.out.println("[√] IP抽取+pos标记：\n");
//        System.out.println(joinList_r(ipPosList));
//
//        // 模拟筛选
//        List<String> filterList = new ArrayList<String>();
//        filterList.add("中国");
//        filterList.add("荷兰");
//        // IP抽取+ip筛选
//        String filterIpStr = joinList_r(filterIpStr(ipPosList, filterList));
//        System.out.println("[√] IP抽取+ip筛选：");
//        System.out.println(filterIpStr);
//        // IP抽取+ip筛选+pos标记
//        String filterIpPosStr = joinList_r(filterIpPos(ipPosList, filterList));
//        System.out.println("[√] IP抽取+ip筛选+pos标记：");
//        System.out.println(filterIpPosStr);

        // 原文本+pos标记
        System.out.println("[√] 原文本+pos标记：");
        System.out.println(inputPosStr);


    }


    public static Searcher init() {

        Searcher searcher = null;

        try {

            String ip2regionPath = Paths.get(System.getProperty("user.home"), ".PotatoTool","ip2region.xdb").toString();
            searcher = SearchTest.createSearcher(ip2regionPath, "vectorIndex");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return searcher;

    }


    /**
     * @param ipStr     原始检索结果（美国|0|伊利诺伊|芝加哥|0）
     * @return          格式化后结果（美国伊利诺伊芝加哥）
     * @throws Exception
     */
    public static String formatSearchPos(String ipStr) throws Exception {
        Set<String> infoList= new LinkedHashSet<String>(Arrays.asList(ipStr.split("\\|")));
        return String.join("", infoList).replace("0","");
    }



    /**
     * @param inputStr  用户输入的字符串
     * @return          正则匹配返回该字符串中存在的IP（数组）
     */
    public static LinkedHashSet<String> getIpListFromReg(String inputStr){

//        Pattern pattern = Pattern.compile("(?<!\\d)(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)(?!\\d)");
        String ipv4Pattern = "(?<!\\d)(25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})\\." +
                "(25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})\\." +
                "(25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})\\." +
                "(25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})(?!\\d)";

        // 定义 IPv6 的正则表达式
        String ipv6Pattern = "(([0-9a-fA-F]{1,4}:){1,7}(:[0-9a-fA-F]{1,4}){1,7}|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))" ;

        Pattern pattern = Pattern.compile(ipv4Pattern + "|" + ipv6Pattern);
        Matcher matcher = pattern.matcher(inputStr);

        LinkedHashSet<String> ipList = new LinkedHashSet<String>(); // 该类型插入内容不重复且按顺序

        while (matcher.find()) {
            ipList.add(matcher.group());
        }
        return ipList;
    }


    /**
     * @param searcher  检索对象
     * @param ipList    检索ip数组
     * @return          返回ip:pos字典
     * @throws Exception
     */
    public static LinkedHashMap<String, String> getIpPosDict(Searcher searcher, Set<String> ipList) {

        LinkedHashMap<String, String> ipPosDict = new LinkedHashMap<>();

        for (String ipData : ipList) {
            String posData = "暂未收录";
            try {
                formatSearchPos(searcher.search(ipData));
            }catch (Exception e){}
            ipPosDict.put(ipData, posData);
        }

        return ipPosDict;

    }


    /**
     * @param searcher  检索对象
     * @param ipList    检索ip数组
     * @return          标识ipPos信息
     * @throws Exception
     */
    public static String getInputPosStr(Searcher searcher, String inputStr, Set<String> ipList) throws Exception {

        String newInputStr = inputStr;

        for (String tmpIpData : ipList) {

            String newData = tmpIpData + "[" + formatSearchPos(searcher.search(tmpIpData)) + "]";
            newInputStr = newInputStr.replace(tmpIpData, newData);

        }

        return newInputStr;

    }

    public static LinkedHashMap<String, String> filterIpPos(LinkedHashMap<String, String> ipPosDict, String prefix){

        LinkedHashMap<String, String> newTmpipPosDict = new LinkedHashMap<>(ipPosDict);

        if(prefix == "国内IP"){
            newTmpipPosDict.entrySet().removeIf(entry -> !entry.getValue().startsWith("中国"));
        }else if(prefix == "国外IP"){
            newTmpipPosDict.entrySet().removeIf(entry -> entry.getValue().startsWith("中国") || entry.getValue().startsWith("内网"));//剔除国内IP
        }else if(prefix == "外网IP"){
            newTmpipPosDict.entrySet().removeIf(entry -> entry.getValue().startsWith("内网IP"));//剔除内网IP
        }else if(prefix != null){
            newTmpipPosDict.entrySet().removeIf(entry -> !entry.getKey().startsWith(prefix) && !entry.getValue().startsWith(prefix));
        }

        return newTmpipPosDict;

    }


    /**
     * @param posIpList     ip[pos信息]数据
     * @param filterList    筛选特征（数组）
     * @return              匹配ip[pos信息]筛选信息的数据
     */
    public static Set<String> filterIpPos(Set<String> posIpList, List<String> filterList) {

        Set<String> filterTmpList = new LinkedHashSet<String>();

        for (String posline : posIpList) {
            for (String filter : filterList){
                if (posline.contains(filter)) {
                    filterTmpList.add(posline);
                }
            }
        }

        return filterTmpList;

    }

    /**
     * @param posIpList     ip[pos信息]数据
     * @param filterList    过滤特征（数组）
     * @return              匹配ip[pos信息]过滤信息的数据
     */
    private static Set<String> unfilterIpPos(Set<String> posIpList, List<String> filterList) {

        Set<String> filterTmpList = new LinkedHashSet<String>();

        for (String posline : posIpList) {
            for (String filter : filterList){
                if (!posline.contains(filter)) {
                    filterTmpList.add(posline);
                }
            }
        }

        return filterTmpList;

    }

    /**
     * @param posIpList     ip[pos信息]数据
     * @param filterList    过滤特征（数组）
     * @return              剔除过滤信息的ip数据
     */
    public static Set<String> filterIpStr(Set<String> posIpList, List<String> filterList) {

        Set<String> filterTmpList = filterIpPos(posIpList, filterList);
        Set<String> resultIpStr = new LinkedHashSet<String>();

        for (String tmpline : filterTmpList){
            resultIpStr.add( tmpline.replaceAll("\\[.*?\\]","") );
        }

        return resultIpStr;

    }

    /**
     * @param posIpList     ip[pos信息]数据
     * @param filterList    过滤特征（数组）
     * @return              匹配ip过滤信息的数据
     */
    private static Set<String> unfilterIpStr(Set<String> posIpList, List<String> filterList) {

        Set<String> filterTmpList = unfilterIpPos(posIpList, filterList);
        Set<String> resultIpStr = new LinkedHashSet<String>();

        for (String tmpline : filterTmpList){
            resultIpStr.add( tmpline.replaceAll("\\[.*?\\]","") );
        }

        return resultIpStr;

    }


}

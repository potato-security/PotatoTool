package com.potato.potatotool.content.blueTeam;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/***
 * 测试IP V6 提取 文档
 */

public class IPMatcher {
    public static void main(String[] args) {
        List<String> strings = new ArrayList<>();
        strings.add("This is a test with an IPv6: 2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        strings.add("No IPv6 here!");
        strings.add("Another IPv6: fe80::1ff:fe23:4567:890a");
        strings.add("\n活动连接\n\n  协议  本地地址          外部地址        状态           PID\n  TCP    0.0.0.0:21             0.0.0.0:0              LISTENING       1176\n  TCP    0.0.0.0:80             0.0.0.0:0              LISTENING       4\n" +
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
                "Extracted IPv6 addresses: [2400:cb00:2048:1::c629:d7a2, 2001:db8:85a3:0:0:8a2e:370:7334 , fe80::1ff:fe23:4567:890a]");

        List<String> ipv6Addresses = extractIPv6(strings);
        System.out.println("Extracted IPv6 Addresses: " + ipv6Addresses);
    }

    public static List<String> extractIPv6(List<String> strings) {
        List<String> ipv6Addresses = new ArrayList<>();
        String ipv6Pattern = "(?<![:\\w])((?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,7}:|(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|::(?:[0-9a-fA-F]{1,4}:){0,5}[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,5}:(?:[0-9a-fA-F]{1,4}:){1,2}[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,4}:(?:[0-9a-fA-F]{1,4}:){1,3}[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,3}:(?:[0-9a-fA-F]{1,4}:){1,4}[0-9a-fA-F]{1,4}|(?:[0-9a-fA-F]{1,4}:){1,2}:(?:[0-9a-fA-F]{1,4}:){1,5}[0-9a-fA-F]{1,4}|[0-9a-fA-F]{1,4}:(?:[0-9a-fA-F]{1,4}:){1,6}[0-9a-fA-F]{1,4}|:(?::[0-9a-fA-F]{1,4}){1,7})(?![:\\w])";

        Pattern pattern = Pattern.compile(ipv6Pattern);
        for (String str : strings) {
            Matcher matcher = pattern.matcher(str);
            while (matcher.find()) {
                ipv6Addresses.add(matcher.group());
            }
        }
        return ipv6Addresses;
    }
}

package com.potato.potatotool.content.redTeam.infoGathering.cdn;

import com.potato.potatotool.utils.Constants;
import org.apache.commons.net.util.SubnetUtils;
import org.xbill.DNS.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2024/10/22 11:39
 */
public class CdnChecker {
    private static List<String> cdnCnameList;
    private static List<String> cdnIpList;

    static{
        cdnCnameList = Constants.getResourceList("cdnCname");
        cdnIpList = Constants.getResourceList("cdnIp");
    }

    public static boolean isCdnDomain(String domain) {
        try {
            // 1. 判断 CNAME
            Record[] cnameRecords = getRecord(domain, Type.CNAME);
            if (cnameRecords != null) {
                for (Record record : cnameRecords) {
                    CNAMERecord cnameRecord = (CNAMERecord) record;
                    String cname = cnameRecord.getTarget().toString();
                    // 检查CNAME是否在CDN CNAME列表中
                    for (String cdnCname : cdnCnameList) {
                        if (cname.contains(cdnCname)) {
                            return true;
                        }
                    }
                }
            }else {
                // 2. 判断 SOA
                Record[] soaRecords = getRecord(domain, Type.SOA);
                if (soaRecords != null) {
                    for (Record record : soaRecords) {
                        SOARecord soaRecord = (SOARecord) record;
                        String soaDomain = soaRecord.getHost().toString();
                        // 检查soa是否在CDN CNAME列表中
                        for (String cdnCname : cdnCnameList) {
                            if (soaDomain.contains(cdnCname)) {
                                return true;
                            }
                        }
                    }
                }
            }
            // 3. 判断 A 记录
            Record[] aRecords = getRecord(domain, Type.A);
            if (aRecords != null) {
                for (Record record : aRecords) {
                    ARecord aRecord = (ARecord) record;
                    String ipAddress = aRecord.getAddress().getHostAddress();
                    System.out.println(ipAddress);
                    if (isIpInCidrList(ipAddress, cdnIpList)) {
                        return true;
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Failed to resolve domain: " + domain);
        }
        return false; // 不是 CDN
    }

    public static boolean isCdnIp(String ip) {
        return isIpInCidrList(ip);
    }

    private static Record[] getRecord(String domain, int type) throws IOException {
        Lookup lookup = new Lookup(domain, type);
        Record[] records = lookup.run();
        return records;
    }


    public static boolean isIpInRange(String ip, String cidr) {
        SubnetUtils subnetUtils = new SubnetUtils(cidr);
        subnetUtils.setInclusiveHostCount(true); // 包含主机
        return subnetUtils.getInfo().isInRange(ip);
    }

    public static boolean isIpInCidrList(String ip) {
        for (String cidr : cdnIpList) {
            if (isIpInRange(ip, cidr)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        String domain = "rtp-idngalan-1.shop"; // 替换为你要检查的域名
        boolean isCdn = isCdnDomain(domain);
        System.out.println(domain + " 是 CDN: " + isCdn);
    }


}

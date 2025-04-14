package com.potato.potatotool.content.blueTeam;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;

import com.opencsv.CSVWriter;
import com.potato.potatotool.utils.ReadabilityChecker;
import com.potato.potatotool.utils.SecurityInitializer;
import com.potato.potatotool.utils.strUtils;
import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.DnsResourceRecordType;
import org.pcap4j.packet.namednumber.TcpPort;
import org.pcap4j.packet.namednumber.UdpPort;

/**
 * @author Potato
 * @date 2024/9/7 17:00
 */
public class ReadPacketFile {
    private final String pcapFilePath;
    private int packetCounter = 0; // 用于计数数据包序号
    private final String outputFilePath;
    private String inputKey = null;
    private String inputIv = null;
    private List<String> traverse = new ArrayList<>();
    private String customPath = null;

    public ReadPacketFile(String pcapFilePath, String inputKey, String inputIv, List<String> traverse, String customPath) {
        this.pcapFilePath = pcapFilePath;
        this.outputFilePath = generateOutputFilePath(pcapFilePath);
        this.inputKey = inputKey;
        this.inputIv = inputIv;
        this.traverse = traverse;
        this.customPath = customPath;
    }

    private String generateOutputFilePath(String pcapFilePath) {
        Path path = Paths.get(pcapFilePath);
        String baseName = path.getFileName().toString();
        String nameWithoutExt = baseName.substring(0, baseName.lastIndexOf('.'));
        String uuid = UUID.randomUUID().toString();
        String outputFilePath = strUtils.getCurrentJarDir() + File.separator + "PcapDecrypt" + File.separator + nameWithoutExt + "_" + uuid + ".csv";

        Path outputDirPath = Paths.get(outputFilePath).getParent();
        if (!Files.exists(outputDirPath)) {
            try {
                Files.createDirectories(outputDirPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return outputFilePath;
    }


    public String getPackets() {
        try (PcapHandle handle = Pcaps.openOffline(pcapFilePath);
             OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outputFilePath), StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {
            osw.write('\ufeff');

            String[] header = {"No.", "时间", "源IP", "目的IP", "源端口", "目的端口", "协议", "协议-1", "Payload", "Payload_HEX", "解密内容", "解密方式"};
            writer.writeNext(header);

            boolean continueProcessing = true;
            while (continueProcessing) {
                try {
                    Packet packet = handle.getNextPacketEx();
                    packetCounter++;
                    Map<String, String> packetInfo = new LinkedHashMap<>();

                    packetInfo.put("No.", String.valueOf(packetCounter));
                    packetInfo.put("时间", handle.getTimestamp().toString());

                    // 解析 IP 层
                    String srcIp = "";
                    String dstIp = "";
                    if (packet.contains(IpV4Packet.class)) {
                        IpV4Packet ipPacket = packet.get(IpV4Packet.class);
                        srcIp = ipPacket.getHeader().getSrcAddr().toString().replace("/","");
                        dstIp = ipPacket.getHeader().getDstAddr().toString().replace("/","");
                    } else if (packet.contains(IpV6Packet.class)) {
                        IpV6Packet ipPacket = packet.get(IpV6Packet.class);
                        srcIp = ipPacket.getHeader().getSrcAddr().toString().replace("/","");
                        dstIp = ipPacket.getHeader().getDstAddr().toString().replace("/","");
                    } else if (packet.contains(ArpPacket.class)) {
                        ArpPacket arpPacket = packet.get(ArpPacket.class);
                        srcIp = arpPacket.getHeader().getSrcProtocolAddr().getHostAddress();
                        dstIp = arpPacket.getHeader().getDstProtocolAddr().getHostAddress();
                    }
                    packetInfo.put("源IP", srcIp);
                    packetInfo.put("目的IP", dstIp);

                    // 处理TCP协议
                    if (packet.contains(TcpPacket.class)) {
                        handleTcpPacket(packet, packetInfo, packetCounter, srcIp, dstIp, handle.getTimestamp().toString());
                    }
                    // 处理DNS协议
                    else if (packet.contains(DnsPacket.class)) {
                        handleDnsPacket(packet, packetInfo);
                    }
                    // 处理UDP协议
                    else if (packet.contains(UdpPacket.class)) {
                        handleUdpPacket(packet, packetInfo);
                    }
                    // 处理ICMP协议
                    else if (packet.contains(IcmpV4CommonPacket.class)||packet.contains(IcmpV6CommonPacket.class)) {
                        handleIcmpPacket(packet, packetInfo);
                    }
                    // 处理未知协议
                    else {
                        handleUnknownProtocol(packet, packetInfo);
                    }

                    writePacketToCsv(packetInfo, writer);
                } catch (TimeoutException e) {
                    System.out.println("解析" + pcapFilePath + "超时");
                } catch (EOFException e) {
                    // 读取完毕：已到达文件末尾
                    continueProcessing = false;
                } catch (Exception e) {
                    System.err.println("解析" + pcapFilePath + "发生错误：" + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (PcapNativeException | IOException e) {
            e.printStackTrace();
        }
        return outputFilePath;
    }

    // 将解析后的数据包写入CSV文件
    private void writePacketToCsv(Map<String, String> packetInfo, CSVWriter writer) throws IOException {
        List<String> rowData = new ArrayList<>();
        // 根据表头的顺序依次添加值
        rowData.add(packetInfo.getOrDefault("No.", ""));
        rowData.add(packetInfo.getOrDefault("时间", ""));
        rowData.add(packetInfo.getOrDefault("源IP", ""));
        rowData.add(packetInfo.getOrDefault("目的IP", ""));
        rowData.add(packetInfo.getOrDefault("源端口", ""));
        rowData.add(packetInfo.getOrDefault("目的端口", ""));
        rowData.add(packetInfo.getOrDefault("协议", ""));
        rowData.add(packetInfo.getOrDefault("协议-1", ""));
        rowData.add(packetInfo.getOrDefault("Payload", ""));
        rowData.add(packetInfo.getOrDefault("Payload_HEX", ""));
        rowData.add(packetInfo.getOrDefault("解密内容", "不涉及"));
        rowData.add(packetInfo.getOrDefault("解密方式", "不涉及"));

        // 写入到CSV文件
        writer.writeNext(rowData.toArray(new String[0]));
    }

    private String hexData = "";
    private int lastHttpIndex = 0; // 当前处理的最新的http包序号
    private int sameHttpIndex = 0; // 针对分块传输的http包处理到的序号
    private String lastHttpSrcIp = "";
    private String lastHttpDstIp = "";
    private String lastHttpSrcPort = "";
    private String lastHttpDstPort = "";
    private String lastHttpTime = "";

    // 处理TCP协议
    private void handleTcpPacket(Packet packet, Map<String, String> packetInfo, int packetCounter, String srcIp, String dstIp, String time) {
        TcpPacket tcpPacket = packet.get(TcpPacket.class);
        String srcPort = tcpPacket.getHeader().getSrcPort().toString();
        String dstPort = tcpPacket.getHeader().getDstPort().toString();

        packetInfo.put("源端口", srcPort);
        packetInfo.put("目的端口", dstPort);
        packetInfo.put("协议", "TCP");

        if (tcpPacket.getPayload() != null) {
            byte[] payloadBytes = tcpPacket.getPayload().getRawData();
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);

            packetInfo.put("Payload", payload);
            packetInfo.put("Payload_HEX", strUtils.byteToHex(payloadBytes));

            String payloadData = parseTcpPayload(payloadBytes, packetInfo, tcpPacket.getHeader().getSrcPort());

            if(packetInfo.getOrDefault("类型", "").equals("POST")||packetInfo.getOrDefault("类型", "").equals("响应")){
                hexData = payloadData;
                lastHttpIndex = packetCounter;
                lastHttpSrcIp = srcIp;
                lastHttpDstIp = dstIp;
                lastHttpSrcPort = srcPort;
                lastHttpDstPort = dstPort;
                lastHttpTime = time;
                sameHttpIndex = packetCounter;
            }
            if(packetInfo.getOrDefault("协议-1", "").equals("")
                    && packetCounter==sameHttpIndex+1
                    && lastHttpSrcIp.equals(srcIp)
                    && lastHttpDstIp.equals(dstIp)
                    && lastHttpSrcPort.equals(srcPort)
                    && lastHttpDstPort.equals(dstPort)
//                    && lastHttpTime.equals(time)
            ){
                sameHttpIndex += 1;
                hexData += payloadData;

                decodeData(hexData, packetInfo, true);
            }else {
                decodeData(payloadData, packetInfo, false);
            }
        }
    }

    // 处理UDP协议
    private void handleUdpPacket(Packet packet, Map<String, String> packetInfo) {
        UdpPacket udpPacket = packet.get(UdpPacket.class);
        UdpPort srcPort = udpPacket.getHeader().getSrcPort();
        UdpPort dstPort = udpPacket.getHeader().getDstPort();
        packetInfo.put("源端口", srcPort.toString());
        packetInfo.put("目的端口", dstPort.toString());
        packetInfo.put("协议", "UDP");

        if (udpPacket.getPayload() != null) {
            byte[] payloadBytes = udpPacket.getPayload().getRawData();
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            packetInfo.put("Payload", payload);
            packetInfo.put("Payload_HEX", strUtils.byteToHex(payloadBytes));
            decodeData(payload, packetInfo, false);
        }
    }

    // 处理DNS协议
    private void handleDnsPacket(Packet packet, Map<String, String> packetInfo) {
        DnsPacket dnsPacket = packet.get(DnsPacket.class);
        packetInfo.put("协议", "DNS");
        List<DnsQuestion> questions = dnsPacket.getHeader().getQuestions();
        List<DnsResourceRecord> answers = dnsPacket.getHeader().getAnswers();
        String payloadStr = "";

        for (DnsQuestion question : questions) {
            payloadStr += "DNS domainName: " + question.getQName().getName() + "\n";
        }
        for (DnsResourceRecord answer : answers) {
            if (answer.getDataType() == DnsResourceRecordType.TXT) {
                String txtData = answer.getRData().toString();
                payloadStr += "DNS TXT记录: " + txtData + "\n";
            } else if (answer.getDataType() == DnsResourceRecordType.CNAME) {
                String data = answer.getRData().toString();
                payloadStr += "DNS CNAME记录: " + data + "\n";
            } else if(answer.getDataType() == DnsResourceRecordType.MX) {
                String data = answer.getRData().toString();
                payloadStr += "DNS MX记录: " + data + "\n";
            }
        }

        packetInfo.put("Payload", payloadStr);
        packetInfo.put("Payload_HEX", strUtils.byteToHex(dnsPacket.getPayload().getRawData()));
        // 不需要加解密，即使需要，单独进行

    }

    // 处理ICMP协议
    private void handleIcmpPacket(Packet packet, Map<String, String> packetInfo) {
        if (packet.contains(IcmpV4CommonPacket.class)) {
            IcmpV4CommonPacket icmpPacket = packet.get(IcmpV4CommonPacket.class);
            packetInfo.put("协议", "ICMP");
            if (icmpPacket.getPayload() != null) {
                byte[] payloadBytes = icmpPacket.getPayload().getRawData();
                String payload = new String(payloadBytes, StandardCharsets.UTF_8);
                packetInfo.put("Payload", payload);
                packetInfo.put("Payload_HEX", strUtils.byteToHex(payloadBytes));
                decodeData(payload, packetInfo, false);
            }
        } else if (packet.contains(IcmpV6CommonPacket.class)) {
            IcmpV6CommonPacket icmpv6Packet = packet.get(IcmpV6CommonPacket.class);
            packetInfo.put("协议", "ICMPv6");
            if (icmpv6Packet.getPayload() != null) {
                byte[] payloadBytes = icmpv6Packet.getPayload().getRawData();
                String payload = new String(payloadBytes, StandardCharsets.UTF_8);
                packetInfo.put("Payload", payload);
                packetInfo.put("Payload_HEX", strUtils.byteToHex(payloadBytes));
                decodeData(payload, packetInfo, false);
            }
        }
    }

    // 处理未知协议
    private void handleUnknownProtocol(Packet packet, Map<String, String> packetInfo) {
        packetInfo.put("协议", "");
        if (packet.getPayload() != null) {
            byte[] payloadBytes = packet.getPayload().getRawData();
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            packetInfo.put("Payload", payload);
            packetInfo.put("Payload_HEX", strUtils.byteToHex(payloadBytes));
            decodeData(payload, packetInfo, false);
        }
    }


    // 解密payload
    private void decodeData(String payloadData, Map<String, String> packetInfo, boolean isConcatenatedData){
        webShellDecrypt wsd = new webShellDecrypt();
        wsd.inputKey = inputKey;
        wsd.inputIv = inputIv;
        wsd.traverse = traverse;
        wsd.customPath = customPath;

        Map<String, Object> decodeDataMap = wsd.dealBody(payloadData);
        if(decodeDataMap!=null) {
            int error = (int) decodeDataMap.get("error");
            String data = (String) decodeDataMap.get("data");
            String encodeModeList = decodeDataMap.get("encodeModeList").toString();
            if(error == 1){
                data = "解密失败，可能不涉及解密，可自行粘贴16进制单独解密";
                if(isConcatenatedData){
                    data = "检测到可能为分块流量，将尝试拼接解密";
                }
                encodeModeList = "不涉及";
            }else {
                if(isConcatenatedData) {
                    data = "【分块流量，已进行拼接尝试】\n" + data;
                }
            }
            packetInfo.put("解密内容", data);
            packetInfo.put("解密方式", encodeModeList);
        }
    }

    // 根据TCP端口解析协议内容
    private String parseTcpPayload(byte[] payload, Map<String, String> packetInfo, TcpPort dstPort) {
        String data = "";
        String payloadStr = new String(payload, StandardCharsets.UTF_8);
        if (payloadStr.startsWith("GET")) {
            packetInfo.put("协议-1", "HTTP");
            packetInfo.put("类型", "GET");
            data = extractGetData(payloadStr);
        } else if (payloadStr.startsWith("POST")) {
            packetInfo.put("协议-1", "HTTP");
            packetInfo.put("类型", "POST");
            data = extractPostData(payload, packetInfo);
        } else if (payloadStr.startsWith("HTTP/")) {
            packetInfo.put("协议-1", "HTTP");
            packetInfo.put("类型", "响应");
            data = extractResponseData(payload, packetInfo);
        } else if (payloadStr.startsWith("HEAD /")) {
            packetInfo.put("协议-1", "HTTP");
            packetInfo.put("类型", "探测");
        } else {
            data = extractData(payload, packetInfo);
        }

        if (dstPort.valueAsInt() == 25) {
            packetInfo.put("协议-1", "SMTP");
        } else if (dstPort.valueAsInt() == 21) {
            packetInfo.put("协议-1", "FTP");
        } else if (dstPort.valueAsInt() == 23) {
            packetInfo.put("协议-1", "Telnet");
        }

        return data;
    }

    // 提取 GET 请求的数据
    private String extractGetData(String payload) {
        String queryParams = "";
        String[] lines = payload.split("\r\n");
        String requestLine = lines[0];
        String url = requestLine.split(" ")[1];
        if (url.contains("?")) {
            queryParams = url.split("\\?")[1];
        }
        return queryParams;
    }

    // 提取 POST 请求的数据
    private String extractPostData(byte[] payload, Map<String, String> packetInfo) {
        String postData = "";
        byte[] separator = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

        int index = indexOf(payload, separator);
        if (index != -1 && index + separator.length < payload.length) {
            byte[] postDataBytes = Arrays.copyOfRange(payload, index + separator.length, payload.length);
            if(ReadabilityChecker.assessReadability(postDataBytes)){
                postData = new String(postDataBytes, StandardCharsets.UTF_8);
            }else {
                postData = strUtils.byteToHex(postDataBytes);
                packetInfo.put("Payload_HEX", postData);
            }
        }

        return postData;
    }

    // 提取 HTTP 响应数据
    private String extractResponseData(byte[] payload, Map<String, String> packetInfo) {
        String responseBody = "";
        byte[] separator = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

        int index = indexOf(payload, separator);
        if (index != -1 && index + separator.length < payload.length) {
            byte[] responseBodyBytes = Arrays.copyOfRange(payload, index + separator.length, payload.length);
            if(ReadabilityChecker.assessReadability(responseBodyBytes)){
                responseBody = new String(responseBodyBytes, StandardCharsets.UTF_8);
            }else {
                responseBody = strUtils.byteToHex(responseBodyBytes);
                packetInfo.put("Payload_HEX", responseBody);
            }
        }

        return responseBody;
    }

    // 转换其他数据
    private String extractData(byte[] payload, Map<String, String> packetInfo) {
        String data =  "";
        if(ReadabilityChecker.assessReadability(payload)){
            data = new String(payload, StandardCharsets.UTF_8);
        }else {
            data = strUtils.byteToHex(payload);
            packetInfo.put("Payload_HEX", data);
        }
        return data;
    }

    // 查找指定字节数组的起始位置
    private int indexOf(byte[] array, byte[] target) {
        outer:
        for (int i = 0; i < array.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }


    // 非魔改cs流量特征
    public static long checksum8(String text) {
        if (text.length() < 4) {
            return 0L;
        }
        text = text.replace("/", "");
        long sum = 0L;
        for (int x = 0; x < text.length(); x++) {
            sum += text.charAt(x);
        }
        return sum % 256L;
    }

    public static void main(String[] args) {
        // 非项目启动调用，需要单独初始化安全证书套件
        SecurityInitializer.initializeSecurityProvider();
        ReadPacketFile reader = new ReadPacketFile("/Users/a/Desktop/项目开发/PotatoTool/冰蝎3.0测试包.pcap", null, null, new ArrayList<>(), null);
        reader.getPackets();
    }
}
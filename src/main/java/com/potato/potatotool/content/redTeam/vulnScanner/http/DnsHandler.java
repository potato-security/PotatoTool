package com.potato.potatotool.content.redTeam.vulnScanner.http;

import org.xbill.DNS.*;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DNS 查询处理器
 * 用于执行 DNS 查询并解析响应
 * 支持 Nuclei DNS 协议的所有功能
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class DnsHandler {
    
    /**
     * DNS 查询结果
     */
    public static class DnsResponse {
        private String domain;              // 查询的域名
        private String queryType;           // 查询类型
        private int statusCode;             // 响应状态码（RCODE）
        private long duration;              // 查询耗时（毫秒）
        private List<String> answers;       // 响应结果列表
        private String raw;                 // 原始响应（完整的 DNS 消息）
        private boolean success;            // 查询是否成功
        private String error;               // 错误信息
        
        public DnsResponse() {
            this.answers = new ArrayList<>();
        }
        
        // Getters and Setters
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        
        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public List<String> getAnswers() { return answers; }
        public void setAnswers(List<String> answers) { this.answers = answers; }
        
        public String getRaw() { return raw; }
        public void setRaw(String raw) { this.raw = raw; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        @Override
        public String toString() {
            return "DnsResponse{" +
                    "domain='" + domain + '\'' +
                    ", queryType='" + queryType + '\'' +
                    ", statusCode=" + statusCode +
                    ", duration=" + duration + "ms" +
                    ", answers=" + answers +
                    ", success=" + success +
                    '}';
        }
    }
    
    /**
     * 执行 DNS 查询
     * 
     * @param domain 域名
     * @param type DNS 记录类型（A, AAAA, CNAME, MX, TXT等）
     * @return DNS 查询响应
     */
    public static DnsResponse query(String domain, String type) {
        return query(domain, type, null, true, 2);
    }
    
    /**
     * 执行 DNS 查询（完整参数）
     * 
     * @param domain 域名
     * @param type DNS 记录类型
     * @param resolver 自定义 DNS 解析器（如 "8.8.8.8"）
     * @param recursion 是否递归查询
     * @param retries 重试次数
     * @return DNS 查询响应
     */
    public static DnsResponse query(String domain, String type, String resolver, boolean recursion, int retries) {
        DnsResponse response = new DnsResponse();
        response.setDomain(domain);
        response.setQueryType(type);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 解析 DNS 记录类型
            int recordType = parseRecordType(type);
            
            // 创建查询对象
            Name queryName = Name.fromString(domain, Name.root);
            Record queryRecord = Record.newRecord(queryName, recordType, DClass.IN);
            Message queryMessage = Message.newQuery(queryRecord);
            
            // Message.newQuery 默认会打开 RD；显式关闭以支持非递归查询。
            if (!recursion) {
                queryMessage.getHeader().unsetFlag(Flags.RD);
            }
            
            // 创建解析器
            Resolver dnsResolver;
            if (resolver != null && !resolver.isEmpty()) {
                // 使用自定义解析器
                dnsResolver = new SimpleResolver(resolver);
            } else {
                // 使用系统默认解析器
                dnsResolver = new SimpleResolver();
            }
            
            // 设置超时
            dnsResolver.setTimeout(Duration.ofSeconds(10));
            
            // 发送查询
            Message responseMessage = dnsResolver.send(queryMessage);
            
            // 计算耗时
            long duration = System.currentTimeMillis() - startTime;
            response.setDuration(duration);
            
            // 解析响应
            parseResponse(responseMessage, response);
            
            response.setSuccess(true);
            
        } catch (TextParseException e) {
            response.setSuccess(false);
            response.setError("域名解析错误: " + e.getMessage());
            System.err.println("DNS 查询失败 - 域名格式错误: " + domain + " - " + e.getMessage());
        } catch (UnknownHostException e) {
            response.setSuccess(false);
            response.setError("DNS 解析器地址无效: " + e.getMessage());
            System.err.println("DNS 查询失败 - 解析器地址无效: " + resolver + " - " + e.getMessage());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("DNS 查询异常: " + e.getMessage());
            System.err.println("DNS 查询失败: " + domain + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 如果没有设置耗时，计算当前耗时
            if (response.getDuration() == 0) {
                response.setDuration(System.currentTimeMillis() - startTime);
            }
        }
        
        return response;
    }
    
    /**
     * 解析 DNS 响应消息
     */
    private static void parseResponse(Message message, DnsResponse response) {
        if (message == null) {
            response.setStatusCode(-1);
            return;
        }
        
        // 设置响应状态码（RCODE）
        response.setStatusCode(message.getRcode());
        
        // 设置原始响应
        response.setRaw(message.toString());
        
        // 解析应答区段
        Record[] answerRecords = message.getSectionArray(Section.ANSWER);
        if (answerRecords != null && answerRecords.length > 0) {
            for (Record record : answerRecords) {
                String answer = formatRecord(record);
                if (answer != null && !answer.isEmpty()) {
                    response.getAnswers().add(answer);
                }
            }
        }
        
        // 如果应答区段为空，尝试解析授权区段（NS 记录）
        if (response.getAnswers().isEmpty()) {
            Record[] authorityRecords = message.getSectionArray(Section.AUTHORITY);
            if (authorityRecords != null && authorityRecords.length > 0) {
                for (Record record : authorityRecords) {
                    String answer = formatRecord(record);
                    if (answer != null && !answer.isEmpty()) {
                        response.getAnswers().add("(Authority) " + answer);
                    }
                }
            }
        }
    }
    
    /**
     * 格式化 DNS 记录为字符串
     */
    private static String formatRecord(Record record) {
        if (record == null) {
            return null;
        }
        
        // 根据记录类型格式化输出
        switch (record.getType()) {
            case Type.A:
                ARecord aRecord = (ARecord) record;
                return aRecord.getAddress().getHostAddress();
            
            case Type.AAAA:
                AAAARecord aaaaRecord = (AAAARecord) record;
                return aaaaRecord.getAddress().getHostAddress();
            
            case Type.CNAME:
                CNAMERecord cnameRecord = (CNAMERecord) record;
                return cnameRecord.getTarget().toString();
            
            case Type.MX:
                MXRecord mxRecord = (MXRecord) record;
                return mxRecord.getPriority() + " " + mxRecord.getTarget().toString();
            
            case Type.NS:
                NSRecord nsRecord = (NSRecord) record;
                return nsRecord.getTarget().toString();
            
            case Type.TXT:
                TXTRecord txtRecord = (TXTRecord) record;
                return String.join("", txtRecord.getStrings());
            
            case Type.PTR:
                PTRRecord ptrRecord = (PTRRecord) record;
                return ptrRecord.getTarget().toString();
            
            case Type.SOA:
                SOARecord soaRecord = (SOARecord) record;
                return soaRecord.getHost().toString() + " " + 
                       soaRecord.getAdmin().toString() + " " +
                       soaRecord.getSerial();
            
            case Type.SRV:
                SRVRecord srvRecord = (SRVRecord) record;
                return srvRecord.getPriority() + " " + 
                       srvRecord.getWeight() + " " + 
                       srvRecord.getPort() + " " + 
                       srvRecord.getTarget().toString();
            
            case Type.CAA:
                CAARecord caaRecord = (CAARecord) record;
                return caaRecord.getFlags() + " " + 
                       caaRecord.getTag() + " " + 
                       caaRecord.getValue();
            
            default:
                // 其他类型使用默认字符串表示
                return record.rdataToString();
        }
    }
    
    /**
     * 解析 DNS 记录类型字符串为 dnsjava 类型常量
     */
    private static int parseRecordType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return Type.A;  // 默认 A 记录
        }
        
        switch (typeStr.toUpperCase()) {
            case "A":
                return Type.A;
            case "AAAA":
                return Type.AAAA;
            case "CNAME":
                return Type.CNAME;
            case "MX":
                return Type.MX;
            case "NS":
                return Type.NS;
            case "TXT":
                return Type.TXT;
            case "PTR":
                return Type.PTR;
            case "SOA":
                return Type.SOA;
            case "SRV":
                return Type.SRV;
            case "CAA":
                return Type.CAA;
            case "ANY":
                return Type.ANY;
            default:
                System.err.println("未知的 DNS 记录类型: " + typeStr + "，使用默认 A 记录");
                return Type.A;
        }
    }
    
    /**
     * 批量查询（用于查询多个域名）
     */
    public static List<DnsResponse> batchQuery(List<String> domains, String type, String resolver, boolean recursion, int retries) {
        List<DnsResponse> responses = new ArrayList<>();
        for (String domain : domains) {
            DnsResponse response = query(domain, type, resolver, recursion, retries);
            responses.add(response);
        }
        return responses;
    }
    
    /**
     * 测试方法
     */
    public static void main(String[] args) {
        // 测试 A 记录查询
        System.out.println("=== 测试 A 记录查询 ===");
        DnsResponse responseA = query("www.baidu.com", "A");
        System.out.println(responseA);
        System.out.println("应答: " + responseA.getAnswers());
        
        // 测试 AAAA 记录查询
        System.out.println("\n=== 测试 AAAA 记录查询 ===");
        DnsResponse responseAAAA = query("www.google.com", "AAAA");
        System.out.println(responseAAAA);
        System.out.println("应答: " + responseAAAA.getAnswers());
        
        // 测试 MX 记录查询
        System.out.println("\n=== 测试 MX 记录查询 ===");
        DnsResponse responseMX = query("gmail.com", "MX");
        System.out.println(responseMX);
        System.out.println("应答: " + responseMX.getAnswers());
        
        // 测试 TXT 记录查询
        System.out.println("\n=== 测试 TXT 记录查询 ===");
        DnsResponse responseTXT = query("google.com", "TXT");
        System.out.println(responseTXT);
        System.out.println("应答: " + responseTXT.getAnswers());
        
        // 测试使用自定义 DNS 服务器
        System.out.println("\n=== 测试自定义 DNS 服务器（8.8.8.8）===");
        DnsResponse responseCustom = query("www.example.com", "A", "8.8.8.8", true, 2);
        System.out.println(responseCustom);
        System.out.println("应答: " + responseCustom.getAnswers());
    }
}

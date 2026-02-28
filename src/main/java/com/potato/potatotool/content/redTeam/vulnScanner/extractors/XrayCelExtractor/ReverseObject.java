package com.potato.potatotool.content.redTeam.vulnScanner.extractors.XrayCelExtractor;

import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;

/**
 * Reverse 反连对象
 * 对应 Xray 官方 newReverse() 函数
 * 
 * 基于 DnsLogService 实现，支持：
 * - dnslog.cn
 * - ceye.io
 * 
 * 使用场景：
 * ```yaml
 * set:
 *   reverse: newReverse()
 * rules:
 *   - method: GET
 *     path: /api?url={{reverse.url}}
 *   - method: GET
 *     path: /check
 *     expression: reverse.wait(10)
 * ```
 * 
 * @author Potato
 * @date 2025-10-29
 */
public class ReverseObject {
    
    /**
     * DNSLog 域名（唯一标识）
     */
    private final String domain;
    
    /**
     * HTTP URL（用于 HTTP 反连检测）
     */
    private final String url;
    
    /**
     * IP 地址（占位，DNSLog 场景下不需要）
     */
    private final String ip;
    
    /**
     * 创建时间
     */
    private final long createTime;
    
    /**
     * 构造函数
     * 自动生成 DNSLog 域名
     */
    public ReverseObject() {
        // 使用已有的 DnsLogService 生成域名
        this.domain = DnsLogService.generateDnsLogDomain();
        this.url = "http://" + this.domain;
        this.ip = "127.0.0.1"; // 占位
        this.createTime = System.currentTimeMillis();
        
        System.out.println("✓ 创建 Reverse 对象: " + this.domain);
    }
    
    /**
     * 等待反连
     * 轮询检查 DNSLog 是否收到解析请求
     * 
     * @param timeout 超时时间（秒）
     * @return 是否收到反连
     */
    public boolean waitFor(int timeout) {
        if (timeout <= 0) {
            timeout = 10; // 默认10秒
        }
        
        long endTime = System.currentTimeMillis() + (timeout * 1000L);
        int checkCount = 0;
        
        System.out.println("等待反连: " + this.domain + " (超时: " + timeout + "秒)");
        
        while (System.currentTimeMillis() < endTime) {
            checkCount++;
            
            try {
                // 使用已有的 DnsLogService 查询记录
                String records = DnsLogService.queryDnsLogRecords(this.domain);
                
                if (records != null && !records.isEmpty()) {
                    System.out.println("✓ 检测到反连！域名: " + this.domain);
                    System.out.println("  记录: " + records);
                    return true;
                }
                
                // 每0.5秒检查一次
                Thread.sleep(500);
                
                // 每5次检查打印一次进度
                if (checkCount % 10 == 0) {
                    long remaining = (endTime - System.currentTimeMillis()) / 1000;
                    System.out.println("  [" + checkCount + "] 仍在等待... (剩余 " + remaining + "秒)");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("等待被中断");
                break;
            } catch (Exception e) {
                System.err.println("查询 DNSLog 记录失败: " + e.getMessage());
            }
        }
        
        System.out.println("✗ 未检测到反连（超时）");
        return false;
    }
    
    /**
     * 等待反连（重载方法，默认10秒）
     * 
     * @return 是否收到反连
     */
    public boolean waitFor() {
        return waitFor(10);
    }
    
    /**
     * 检查是否已有反连（不等待）
     * 
     * @return 是否已收到反连
     */
    public boolean hasCallback() {
        try {
            String records = DnsLogService.queryDnsLogRecords(this.domain);
            return records != null && !records.isEmpty();
        } catch (Exception e) {
            System.err.println("检查反连失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取 DNSLog 域名
     * 
     * @return 域名
     */
    public String getDomain() {
        return domain;
    }
    
    /**
     * 获取 HTTP URL
     * 
     * @return URL
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * 获取 IP 地址
     * 
     * @return IP 地址
     */
    public String getIp() {
        return ip;
    }
    
    /**
     * 获取创建时间
     * 
     * @return 创建时间戳
     */
    public long getCreateTime() {
        return createTime;
    }
    
    /**
     * 获取 DNSLog 记录
     * 
     * @return 记录（JSON格式）
     */
    public String getRecords() {
        try {
            return DnsLogService.queryDnsLogRecords(this.domain);
        } catch (Exception e) {
            System.err.println("获取 DNSLog 记录失败: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public String toString() {
        return "ReverseObject{" +
                "domain='" + domain + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}


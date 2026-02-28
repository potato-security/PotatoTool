# 漏洞扫描测试 POC 集合

## 测试靶场

### 本地靶场
运行 `VulnTestLabServer.java` 启动本地测试靶场：
```bash
# HTTP 服务: http://127.0.0.1:18080
# TCP 服务: 127.0.0.1:18021
```

### 在线靶场推荐
| 靶场名称 | 地址 | 说明 |
|---------|------|------|
| DVWA | http://www.dvwa.co.uk/ | 经典 Web 漏洞靶场 |
| VulnHub | https://www.vulnhub.com/ | 虚拟机靶场 |
| HackTheBox | https://www.hackthebox.eu/ | 在线渗透测试平台 |
| PortSwigger Labs | https://portswigger.net/web-security | Web 安全学院靶场 |
| OWASP WebGoat | https://owasp.org/www-project-webgoat/ | OWASP 官方靶场 |
| Vulhub | https://vulhub.org/ | Docker 漏洞靶场 |

---

## POC 测试矩阵

### 按格式分类

| 格式 | 数量 | 复杂度范围 | 测试重点 |
|------|------|------------|----------|
| **Nuclei** | 6 | ★~★★★★ | DSL、多步骤、Payload |
| **Xray** | 2 | ★★~★★★ | CEL 表达式、多规则 |
| **Goby** | 1 | ★★ | 标准扫描结构 |
| **Pocsuite** | 1 | ★★ | verify/exploit 分离 |

### 按功能分类

| 功能点 | POC 文件 | 靶场接口 |
|--------|----------|----------|
| 状态码匹配 | `nuclei/01-simple-status.yaml` | `/health` |
| 关键词匹配 | `nuclei/02-word-header.yaml` | `/version` |
| 正则提取 | `nuclei/03-regex-extract.yaml` | `/admin/config` |
| DSL 表达式 | `nuclei/04-dsl-matcher.yaml` | `/sqli?id=1'` |
| 多步骤流程 | `nuclei/05-multi-step-flow.yaml` | `/api/login` → `/api/secret` |
| Payload Fuzzing | `nuclei/06-payloads-fuzz.yaml` | `/lfi?file=` |
| CEL 表达式 | `xray/01-simple-cel.yaml` | `/.git/config` |
| 多规则组合 | `xray/02-multi-rule.yaml` | `/cmdi?cmd=` |
| 标准扫描 | `goby/01-simple-scan.json` | `/phpinfo` |
| 验证利用 | `pocsuite/01-simple-verify.json` | `/backup.sql` |

---

## 详细 POC 说明

### Nuclei 格式 (6个)

#### 1. 01-simple-status.yaml
- **复杂度**: ★☆☆☆☆
- **测试点**: STATUS 匹配器
- **靶场接口**: `GET /health`
- **预期结果**: 状态码 200 匹配成功

#### 2. 02-word-header.yaml
- **复杂度**: ★★☆☆☆
- **测试点**: WORD 匹配器、header part、AND 条件
- **靶场接口**: `GET /version`
- **预期结果**: 响应头和响应体关键词匹配

#### 3. 03-regex-extract.yaml
- **复杂度**: ★★★☆☆
- **测试点**: REGEX 匹配器、提取器
- **靶场接口**: `GET /admin/config`
- **预期结果**: 提取密码等敏感信息

#### 4. 04-dsl-matcher.yaml
- **复杂度**: ★★★☆☆
- **测试点**: DSL 匹配器、contains 函数
- **靶场接口**: `GET /sqli?id=1'`
- **预期结果**: DSL 表达式匹配 SQL 错误信息

#### 5. 05-multi-step-flow.yaml
- **复杂度**: ★★★★☆
- **测试点**: 多步骤执行、变量传递
- **靶场接口**: `POST /api/login` → `GET /api/secret`
- **预期结果**: 第一步提取 Token，第二步使用 Token

#### 6. 06-payloads-fuzz.yaml
- **复杂度**: ★★★★☆
- **测试点**: payloads、batteringram 模式
- **靶场接口**: `GET /lfi?file={{payload}}`
- **预期结果**: 多 Payload 轮询测试 LFI

### Xray 格式 (2个)

#### 1. 01-simple-cel.yaml
- **复杂度**: ★★☆☆☆
- **测试点**: CEL 表达式、bcontains 函数
- **靶场接口**: `GET /.git/config`
- **预期结果**: CEL 表达式匹配成功

#### 2. 02-multi-rule.yaml
- **复杂度**: ★★★☆☆
- **测试点**: 多规则、r1() && r2()
- **靶场接口**: `GET /cmdi?cmd=`
- **预期结果**: 两个规则都匹配成功

### Goby 格式 (1个)

#### 1. 01-simple-scan.json
- **复杂度**: ★★☆☆☆
- **测试点**: ScanSteps、ResponseTest
- **靶场接口**: `GET /phpinfo`
- **预期结果**: 检测 PHPInfo 页面

### Pocsuite 格式 (1个)

#### 1. 01-simple-verify.json
- **复杂度**: ★★☆☆☆
- **测试点**: verify steps、matchers
- **靶场接口**: `GET /backup.sql`
- **预期结果**: 检测数据库备份文件

---

## 测试运行方法

### 1. 启动靶场
```java
// 运行 VulnTestLabServer.main()
// 或者使用 Maven:
// mvn exec:java -Dexec.mainClass="...VulnTestLabServer"
```

### 2. 运行扫描测试
```java
VulnScanService service = new VulnScanService();
// 加载测试 POC
service.getPocRepository().loadPocDirectory("src/test/resources/testpocs");
// 执行扫描
ScanResult result = service.scan("http://127.0.0.1:18080");
```

---

## 匹配器类型覆盖

| 类型 | 覆盖 POC | 说明 |
|------|----------|------|
| STATUS | ✅ 01-simple-status | 状态码匹配 |
| WORD | ✅ 02-word-header | 关键词匹配 |
| REGEX | ✅ 03-regex-extract | 正则表达式 |
| DSL | ✅ 04-dsl-matcher | Nuclei DSL |
| CEL | ✅ xray/01-simple-cel | Xray CEL |
| BINARY | ⏳ 待添加 | 二进制匹配 |
| HASH | ⏳ 待添加 | 哈希匹配 |
| JSON | ⏳ 待添加 | JSON 路径 |
| TIME | ⏳ 待添加 | 时间盲注 |

## 协议类型覆盖

| 协议 | 覆盖 | 说明 |
|------|------|------|
| HTTP | ✅ 全部 | 主要测试协议 |
| TCP | ⏳ 待添加 | 需要 TCP POC |
| DNS | ⏳ 待添加 | 需要 DNS POC |
| Code | ⏳ 待添加 | JavaScript/Python |

---

## 后续计划

1. **添加更多协议 POC**
   - TCP 协议（FTP Banner 检测）
   - DNS 协议（DNS 查询）
   - Code 协议（JavaScript/Python）

2. **添加更多匹配器测试**
   - BINARY 匹配器
   - HASH 匹配器
   - TIME 匹配器（时间盲注）

3. **添加复杂场景**
   - OOB 检测（配合 Interactsh）
   - Cookie 复用
   - 认证绕过链

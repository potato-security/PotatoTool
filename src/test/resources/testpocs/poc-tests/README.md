# POC 测试用例

## 测试服务器

- **PocTestServer** - 端口 18889
- 启动命令: `mvn exec:java -Dexec.mainClass="com.potato.potatotool.vulnScanner.testlab.PocTestServer" -Dexec.classpathScope=test`

## 测试用例列表

### Pocsuite POC 测试

| 测试名称 | POC 文件 | 正面测试URL | 负面测试URL | 状态 |
|---------|---------|------------|------------|------|
| PHPCMS SQL注入 | pocsuite-phpcms-sqli.json | /pocsuite/phpcms | /safe/pocsuite/phpcms | ✅ 通过 |
| 多步骤认证绕过 | pocsuite-multi-step.json | /pocsuite/multi | /safe/pocsuite/multi | ✅ 通过 |
| 时间盲注 | pocsuite-time-blind.json | /pocsuite/timebased | /safe/pocsuite/timebased | ⚠️ 待调试(匹配器时间检测) |

### Xray POC 测试

| 测试名称 | POC 文件 | 正面测试URL | 负面测试URL | 状态 |
|---------|---------|------------|------------|------|
| SpringBlade SQL注入 | xray-springblade-sqli.yml | /xray/springblade | /safe/xray/springblade | ⚠️ 格式问题(需用Xray格式而非Nuclei) |
| Jeecg-Boot RCE | xray-jeecg-rce.yml | /xray/jeecg | /safe/xray/jeecg | ⚠️ 格式问题(需用Xray格式而非Nuclei) |

### Goby POC 测试

| 测试名称 | POC 文件 | 正面测试URL | 负面测试URL | 状态 |
|---------|---------|------------|------------|------|
| 360天擎 SQL注入 | goby-360tianqing-sqli.json | /goby/360tianqing | /safe/goby/360tianqing | ✅ 通过 |
| Nacos默认密码 | goby-nacos-default-pwd.json | /goby/nacos | /safe/goby/nacos | ✅ 通过 |

## 已修复问题

1. **NOT_CONTAINS 操作符**：在 `ResponseMatcher.matchContentBased()` 中添加了对 `NOT_CONTAINS` 操作的支持

## 待完成事项

1. **时间盲注测试**：需要调整匹配器以支持时间延迟检测
2. **Xray POC 格式**：需要使用正确的 Xray YAML 格式（包含 `rules` 字段）而非 Nuclei 格式

## 运行测试

```bash
# 编译
mvn compile test-compile -DskipTests

# 运行集成测试
mvn exec:java -Dexec.mainClass="com.potato.potatotool.vulnScanner.testlab.PocIntegrationTest" -Dexec.classpathScope=test
```

## 测试结果 (2025-11-28 更新)

### 完整 POC 测试（使用原始 POC 目录）

- **POC 总数**: 402 个（不含 Nuclei DNS）
  - Pocsuite: 3 个
  - Xray: 31 个
  - Goby: 368 个
- **POC 加载成功率**: 99.8%
- **测试通过率**: 13.4%（54/402）

### 测试框架文件
- `DynamicPocTestServer.java`: 动态测试服务器，自动生成漏洞响应
- `FullPocIntegrationTest.java`: 完整集成测试，遍历所有 POC
- `PocAnalyzer.java`: POC 分析器

### 运行命令
```bash
# 运行完整 POC 测试
mvn exec:java -Dexec.mainClass="com.potato.potatotool.vulnScanner.testlab.FullPocIntegrationTest" -Dexec.classpathScope=test

# 运行 POC 分析器
mvn exec:java -Dexec.mainClass="com.potato.potatotool.vulnScanner.testlab.PocAnalyzer" -Dexec.classpathScope=test
```

### 待优化项
1. **Xray POC 表达式解析**: 复杂的 CEL 表达式需要完善
2. **多步骤 POC**: 需要状态管理和 token 传递
3. **时间盲注**: 需要实际服务器延迟
4. **DNSLog/Interactsh**: 需要 DNS 服务器模拟

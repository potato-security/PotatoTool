# 二进制序列化格式解密测试

## 概述

本测试套件用于验证WebShell解密服务对二进制序列化格式的支持，包括：
- BSON（MongoDB二进制JSON）
- MessagePack（高效二进制序列化）
- CBOR（RFC 8949标准）
- Smile（Jackson二进制JSON）
- Hessian（Caucho二进制协议）

## 测试文件说明

### 1. BinaryDeserializationTest.java
主测试类，包含14个测试用例，覆盖以下场景：

| 测试用例 | 格式 | 编码方式 | 说明 |
|---------|------|---------|------|
| testBsonBase64 | BSON | Base64 | 基础BSON解密 |
| testMessagePackBase64 | MessagePack | Base64 | 基础MessagePack解密 |
| testCborBase64 | CBOR | Base64 | 基础CBOR解密 |
| testSmileBase64 | Smile | Base64 | 带魔术头的Smile解密 |
| testCborHex | CBOR | Hex | Hex编码的CBOR数据 |
| testMessagePackByteArray | MessagePack | byte[] | Java数组格式的MessagePack |
| testFullHttpPacketCbor | CBOR | HTTP包 | 完整HTTP POST请求 |
| testBsonByteArray | BSON | byte[] | Java数组格式的BSON |
| testNestedBase64Cbor | CBOR | Base64嵌套 | 多层编码 |
| testFullHttpPacketMessagePack | MessagePack | HTTP包 | 完整HTTP包 |
| testSmileHex | Smile | Hex | Hex编码的Smile数据 |
| testHessianBase64 | Hessian | Base64 | Hessian2协议 |
| testShortByteSequence | N/A | byte[] | 负面测试（1字节） |
| testMultiLayerEncryption | CBOR | 多层嵌套 | URL+Base64+CBOR |

### 2. BinaryDataGenerator.java
数据生成器，用于生成真实的二进制序列化数据。

**功能**：
- 生成BSON/MessagePack/CBOR/Smile格式的测试数据
- 输出Base64、Hex、Java数组三种格式
- 提供HTTP包模板

## 使用方法

### 运行完整测试套件

```bash
# 方式1：直接运行主类
cd /Users/potato/Desktop/projectDevelopment/PotatoTool
java -cp target/classes:target/test-classes \
  com.potato.potatotool.content.blueTeam.webshellDecrypt.BinaryDeserializationTest

# 方式2：使用Maven
mvn test -Dtest=BinaryDeserializationTest
```

### 运行数据生成器

```bash
# 生成新的测试数据
java -cp target/classes:target/test-classes \
  com.potato.potatotool.content.blueTeam.webshellDecrypt.BinaryDataGenerator
```

### 运行单个测试用例

在IDE中打开 `BinaryDeserializationTest.java`，直接运行单个测试方法：

```java
// 例如：只测试CBOR格式
BinaryDeserializationTest.testCborBase64();
```

## 测试数据格式说明

### 1. Base64格式
```
data=pWNjbWRmd2hvYW1ppGFyZ3OBoC10YXJnZXSiZmlsZWkvc2JpbgovYmlu
```

### 2. Hex格式
```
data=A56673746174757367737563636573736464617461A2626964016464616D6564746573
```

### 3. byte[]数组格式
```java
data={-126, -93, 99, 109, 100, -94, 108, 115, -92, 112, 97, 116, 104, -91, 47, 116, 109, 112}
```

### 4. 完整HTTP包格式
```http
POST /shell.php HTTP/1.1
Host: victim.com
Content-Type: application/octet-stream
Content-Length: 48

data=pWNjbWRmd2hvYW1ppGFyZ3OBoC10YXJnZXSiZmlsZWkvc2JpbgovYmlu
```

## 测试数据生成步骤

如果需要自定义测试数据，可以使用 `BinaryDataGenerator`：

```java
// 1. 生成原始二进制数据
byte[] data = BinaryDataGenerator.generateCbor();

// 2. 转换为Base64
String base64 = BinaryDataGenerator.toBase64(data);

// 3. 转换为Hex
String hex = BinaryDataGenerator.toHex(data);

// 4. 转换为Java数组
String javaArray = BinaryDataGenerator.toJavaArray(data);

// 5. 打印所有格式
BinaryDataGenerator.printAllFormats("自定义数据", data);
```

## 预期结果示例

### 成功解密
```
解密模式: [[Base64, CBOR]]
错误信息: null
解密结果:
{
  "cmd": "whoami",
  "args": ["test"]
}
```

### 解密失败
```
解密模式: []
错误信息: 未识别的加密方式
解密结果: (原始数据)
```

## 测试覆盖范围

### 格式检测
- ✅ 精确魔术头检测（Smile: 3A 29 0A）
- ✅ 长度验证（BSON: 前4字节+尾部0x00）
- ✅ 首字节范围检测（CBOR: A0-BF, MessagePack: 80-9F）
- ✅ 最小长度限制（10-20字节）

### 编码方式
- ✅ Base64编码
- ✅ Hex编码
- ✅ Java byte[]数组格式
- ✅ 嵌套编码（Base64+二进制格式）

### HTTP传输
- ✅ Content-Type MIME类型检测
- ✅ POST参数解析
- ✅ 完整HTTP包处理

### 边界条件
- ✅ 短字节序列（< 10字节）应被拒绝
- ✅ 格式检测失败时返回null
- ✅ 错误信息不作为返回值

## 注意事项

1. **依赖要求**
   - 必须先运行 `SecurityInitializer.initializeSecurityProvider()`
   - 需要相关依赖库（BSON、Jackson、MessagePack等）

2. **性能考虑**
   - 测试数据较小（< 1KB），适合快速验证
   - 生产环境可能需要处理更大的数据

3. **错误处理**
   - 解密失败时返回原始数据
   - debugMode下会输出详细错误信息

4. **兼容性**
   - 适配Java 8+
   - 兼容哥斯拉、冰蝎等WebShell工具的加密方式

## 扩展测试

如需测试更多场景，可以：

1. **修改测试数据**
   ```java
   Map<String, Object> data = new HashMap<>();
   data.put("custom_key", "custom_value");
   byte[] cborData = new ObjectMapper(new CBORFactory()).writeValueAsBytes(data);
   ```

2. **添加新的格式**
   - 实现 `BinaryDeserializer` 接口
   - 在 `BinaryDeserializerFactory` 中注册
   - 添加对应的测试用例

3. **压力测试**
   ```java
   // 生成大量数据
   for (int i = 0; i < 10000; i++) {
       testCborBase64();
   }
   ```

## 常见问题

### Q1: 测试失败，提示"未识别的加密方式"
A: 检查测试数据是否正确，使用 `BinaryDataGenerator` 重新生成。

### Q2: 短字节序列被跳过
A: 这是正常行为，最小长度限制（10-20字节）用于防止误报。

### Q3: 错误信息出现在输出中
A: 检查 `AbstractBinaryDeserializer.formatError()` 是否返回null而非错误字符串。

## 相关文档

- [二进制序列化格式常量定义](../classObj/BinaryFormatConstants.java)
- [反序列化器接口](../decoder/utils/BinaryDeserializer.java)
- [WebShell解密服务](../WebShellDecryptService.java)

---

**作者**: Potato
**日期**: 2025/12/29
**版本**: 1.0

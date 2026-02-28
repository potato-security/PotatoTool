package com.potato.potatotool.content.redTeam.vulnScanner.goby;

/**
 * Goby POC 测试套件
 * 
 * 使用方法：
 * 1. 在 IDEA 中运行所有测试: 右键点击 goby 包 -> Run 'Tests in 'goby''
 * 2. 使用 Maven 运行所有测试: mvn test
 * 3. 运行单个测试类: mvn test -Dtest=GobyJsonObjTest
 * 4. 运行特定方法: mvn test -Dtest=GobyJsonObjTest#testBasicPocParsing
 * 
 * 测试包括：
 * 
 * 单元测试:
 * - GobyJsonObjTest: Goby JSON POC 数据模型测试
 * - RawHttpRequestParserTest: 原始 HTTP 报文解析测试
 * - VariableExtractorTest: 变量提取器测试
 * - ResponseCacheTest: 响应缓存测试
 * - GobyPocConverterTest: POC 转换器测试
 * 
 * 集成测试:
 * - GobyPocIntegrationTest: Goby POC 集成测试
 * 
 * @author Potato
 * @date 2025-11-01
 */
public class GobyPocTestRunner {
    // 测试套件说明类
    // 注意: JUnit 5 的测试套件需要额外依赖，这里提供测试说明
}


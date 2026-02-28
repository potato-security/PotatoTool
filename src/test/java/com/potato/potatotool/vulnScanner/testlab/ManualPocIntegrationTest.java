package com.potato.potatotool.vulnScanner.testlab;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;

/**
 * 手工编写的 POC 集成测试
 * 
 * 与 DynamicPocTestServer 不同，这个测试使用手工编写的测试服务器，
 * 每个 POC 的测试用例都是人工阅读 POC 原文后独立编写的，
 * 不依赖 POC 解析器来生成测试数据。
 * 
 * 这样可以真正验证 POC 解析器和匹配器的正确性。
 * 
 * @author Potato
 */
public class ManualPocIntegrationTest {
    
    private static final int PORT = 18890;
    private static final String BASE_URL = "http://127.0.0.1:" + PORT;
    
    private ManualPocTestServer testServer;
    private PocLoader pocLoader;
    
    // 测试统计
    private int totalTests = 0;
    private int passedTests = 0;
    private int failedTests = 0;
    
    public static void main(String[] args) {
        ManualPocIntegrationTest test = new ManualPocIntegrationTest();
        try {
            test.runAllTests();
        } catch (Exception e) {
            System.err.println("测试执行异常:");
            e.printStackTrace();
            System.err.flush();
        } catch (Throwable t) {
            System.err.println("测试执行严重错误:");
            t.printStackTrace();
            System.err.flush();
        } finally {
            System.out.flush();
            System.err.flush();
            // 强制退出，确保所有线程都停止
            System.exit(0);
        }
    }
    
    public void runAllTests() throws Exception {
        System.out.println("========================================");
        System.out.println("手工 POC 集成测试");
        System.out.println("========================================");
        System.out.println("说明：每个测试用例都是人工阅读 POC 原文后独立编写的");
        System.out.println("      用于验证 POC 解析器和匹配器的正确性");
        System.out.println("========================================\n");
        
        // 启动测试服务器
        testServer = new ManualPocTestServer(PORT);
        testServer.start();
        
        // 初始化 POC 加载器
        pocLoader = new PocLoader();
        
        try {
            // 等待服务器启动
            Thread.sleep(500);
            
            // 运行 Pocsuite POC 测试
            System.out.println("\n=== Pocsuite POC 测试 (3个) ===\n");
            testPocsuitePocs();
            
            // 运行 Xray POC 测试
            System.out.println("\n=== Xray POC 测试 (31个) ===\n");
            testXrayPocs();
            
            // 运行 Goby POC 测试
            System.out.println("\n=== Goby POC 测试 (368个) ===\n");
            testGobyPocs();
            
            // 打印结果
            printResults();
            
        } finally {
            testServer.stop();
        }
    }
    
    // ========================================
    // Pocsuite POC 测试
    // ========================================
    
    private void testPocsuitePocs() {
        // POC 1: PHPCMS SQL 注入
        testPocBothWays(
            "Pocsuite.json",
            "src/main/resources/poc/pocsuitePoc/Pocsuite.json",
            BASE_URL + "/vuln/pocsuite/phpcms",  // 漏洞版本
            BASE_URL + "/safe/pocsuite/phpcms",  // 安全版本
            "PHPCMS 2008 SQL 注入漏洞",
            "正向：响应包含 MD5(1)；负向：响应不包含敏感信息"
        );
        
        // POC 2: 多步骤认证绕过
        testPocBothWays(
            "multi_step_with_necessary.json",
            "src/main/resources/poc/pocsuitePoc/multi_step_with_necessary.json",
            BASE_URL + "/vuln/pocsuite/multistep",  // 漏洞版本
            BASE_URL + "/safe/pocsuite/multistep",  // 安全版本
            "多步骤认证绕过漏洞",
            "正向：token+admin面板；负向：需要认证"
        );
        
        // POC 3: 时间盲注
        testPocBothWays(
            "sql_time_blind_injection.json",
            "src/main/resources/poc/pocsuitePoc/sql_time_blind_injection.json",
            BASE_URL + "/vuln/pocsuite/timebased",  // 漏洞版本
            BASE_URL + "/safe/pocsuite/timebased",  // 安全版本
            "MySQL 时间盲注漏洞",
            "正向：延迟5秒；负向：立即返回"
        );
    }
    
    // ========================================
    // Xray POC 测试（30个可测试，1个反连跳过）
    // ========================================
    
    private void testXrayPocs() {
        String xrayBase = "src/main/resources/poc/xrayPoc/2024-7-xray/";
        
        // ==================== 文件读取类 ====================
        testPocBothWays(
            "3C环境监测系统 ReadLog",
            xrayBase + "3C环境自动监测监控系统 ReadLog 任意文件读取漏洞/fileread.yml",
            BASE_URL + "/vuln/xray/3c",
            BASE_URL + "/safe/xray/3c",
            "任意文件读取漏洞",
            "正向：返回web.config内容；负向：拒绝访问"
        );
        
        testPocBothWays(
            "华天动力OA downloadWpsFile.jsp",
            xrayBase + "华天动力OA downloadWpsFile.jsp 任意文件读取漏洞/fileread.yml",
            BASE_URL + "/vuln/xray/huatian",
            BASE_URL + "/safe/xray/huatian",
            "任意文件读取漏洞",
            "正向：返回web.xml内容；负向：拒绝访问"
        );
        
        // ==================== SQL注入类 ====================
        testPocBothWays(
            "华磊科技物流 SQL注入",
            xrayBase + "华磊科技物流 getOrderTrackingNumberSQL注入漏洞/sqli.yml",
            BASE_URL + "/vuln/xray/hualei",
            BASE_URL + "/safe/xray/hualei",
            "PostgreSQL SQL注入漏洞",
            "正向：返回postgres错误；负向：正常响应"
        );
        
        // ==================== RCE类 ====================
        testPocBothWays(
            "Jeecg-Boot loadTableData RCE",
            xrayBase + "Jeecg-Boot loadTableData 远程代码执行漏洞/rce.yml",
            BASE_URL + "/vuln/xray/jeecg",
            BASE_URL + "/safe/xray/jeecg",
            "FreeMarker模板注入RCE",
            "正向：返回/etc/passwd内容；负向：拒绝访问"
        );
        
        // ==================== 未授权访问类 ====================
        testPocBothWays(
            "满客宝智慧食堂系统 未授权",
            xrayBase + "满客宝智慧食堂系统 selectUserByOrgId 未授权访问漏洞/unauth.yml",
            BASE_URL + "/vuln/xray/mankb",
            BASE_URL + "/safe/xray/mankb",
            "未授权访问漏洞",
            "正向：返回用户密码；负向：需要认证"
        );
        
        // ==================== FastJson反序列化 ====================
        testPocBothWays(
            "海康威视 FastJson反序列化",
            xrayBase + "海康威视-综合安防管理平台 keepAlive 存在 FastJson 反序列化/deserialization.yml",
            BASE_URL + "/vuln/xray/hikvision",
            BASE_URL + "/safe/xray/hikvision",
            "FastJson反序列化RCE",
            "正向：执行计算返回结果；负向：拒绝访问"
        );
        
        // ==================== SSRF反连类 ====================
        // 注意：反连验证依赖 DNSLog 服务，测试中验证框架是否正确处理 newReverse()
        testPocBothWays(
            "泛微E-Mobile SSRF",
            xrayBase + "泛微E-Mobile installOperate.do SSRF漏洞/ssrf.yml",
            BASE_URL + "/vuln/xray/fanwei",
            BASE_URL + "/safe/xray/fanwei",
            "SSRF反连漏洞",
            "正向：返回200+反连验证；负向：拒绝访问"
        );
    }
    
    // ========================================
    // Goby POC 测试（待实现）
    // ========================================
    
    private void testGobyPocs() {
        String gobyBase = "src/main/resources/poc/gobyPoc/";
        
        // Goby POC 1: Alibaba Nacos 未授权访问漏洞
        testPocBothWays(
            "Alibaba Nacos 未授权访问",
            gobyBase + "Alibaba Nacos 未授权访问漏洞.json",
            BASE_URL + "/vuln/goby/nacos",
            BASE_URL + "/safe/goby/nacos",
            "Nacos API 未授权访问导致用户信息泄露",
            "正向：返回500(接口可访问)；负向：返回401未授权"
        );
        
        // Goby POC 2: Apache 2.4.49 路径遍历 CVE-2021-41773
        // 注：只测试正向，因为 Java HttpServer 会归一化路径，导致无法区分漏洞/安全版本
        testPocPositiveOnly(
            "Apache 2.4.49 路径遍历 CVE-2021-41773",
            gobyBase + "Apache_2.4.49_Path_Traversal_CVE_2021_41773.json",
            BASE_URL + "/vuln/goby/apache-traversal",
            "Apache 2.4.49 路径遍历导致任意文件读取",
            "正向：返回200+/etc/passwd内容（负向测试跳过：HttpServer归一化路径）"
        );
        
        // Goby POC 3: 360天擎数据库信息泄露
        testPocBothWays(
            "360天擎数据库信息泄露",
            gobyBase + "360_Tianqing_database_information_disclosure.json",
            BASE_URL + "/vuln/goby/360tianqing",
            BASE_URL + "/safe/goby/360tianqing",
            "360天擎数据库配置信息泄露",
            "正向：返回数据库配置；负向：返回401未授权"
        );
        
        // Goby POC 4: Nacos 默认密码
        testPocBothWays(
            "Nacos 默认密码",
            gobyBase + "Alibaba_Nacos_Default_password.json",
            BASE_URL + "/vuln/goby/nacos-default-pwd",
            BASE_URL + "/safe/goby/nacos-default-pwd",
            "Nacos 控制台存在默认密码 nacos/nacos",
            "正向：使用默认密码登录成功返回200；负向：返回401"
        );
        
        // Goby POC 5: Apache APISIX 默认 Token
        testPocBothWays(
            "Apache APISIX 默认 Token CVE-2020-13945",
            gobyBase + "Apache_APISIX_Admin_API_Default_Token_CVE_2020_13945.json",
            BASE_URL + "/vuln/goby/apisix",
            BASE_URL + "/safe/goby/apisix",
            "Apache APISIX Admin API 使用默认 Token",
            "正向：使用默认Token创建路由返回201；负向：返回401"
        );
        
        // Goby POC 6: Apache Kylin 未授权配置泄露
        testPocBothWays(
            "Apache Kylin 未授权配置泄露 CVE-2020-13937",
            gobyBase + "Apache Kylin 未授权配置泄露 CVE-2020-13937.json",
            BASE_URL + "/vuln/goby/kylin",
            BASE_URL + "/safe/goby/kylin",
            "Apache Kylin 配置信息未授权访问",
            "正向：返回200+config；负向：返回401"
        );
        
        // Goby POC 7: Apache ActiveMQ 弱口令
        testPocBothWays(
            "Apache ActiveMQ 弱口令",
            gobyBase + "Apache ActiveMQ Console控制台弱口令.json",
            BASE_URL + "/vuln/goby/activemq",
            BASE_URL + "/safe/goby/activemq",
            "Apache ActiveMQ Console 默认密码 admin:admin",
            "正向：使用默认密码登录返回200+Version；负向：返回401"
        );
        
        // Goby POC 8: Adobe ColdFusion LFI
        testPocBothWays(
            "Adobe ColdFusion LFI CVE-2010-2861",
            gobyBase + "Adobe_ColdFusion_LFI_CVE-2010-2861.json",
            BASE_URL + "/vuln/goby/coldfusion",
            BASE_URL + "/safe/goby/coldfusion",
            "Adobe ColdFusion 本地文件包含漏洞",
            "正向：返回200+rdspassword；负向：返回403"
        );
        
        // Goby POC 9: Ametys CMS 信息泄露
        testPocBothWays(
            "Ametys CMS 信息泄露 CVE-2022-26159",
            gobyBase + "Ametys_CMS_infoleak_CVE_2022_26159.json",
            BASE_URL + "/vuln/goby/ametys",
            BASE_URL + "/safe/goby/ametys",
            "Ametys CMS 自动补全信息泄露",
            "正向：返回200+item；负向：返回401"
        );
        
        // Goby POC 10: 360天擎 SQL 注入
        testPocBothWays(
            "360天擎 SQL 注入",
            gobyBase + "360_TianQing_ccid_SQL_injectable.json",
            BASE_URL + "/vuln/goby/360tianqing-sqli",
            BASE_URL + "/safe/goby/360tianqing-sqli",
            "360天擎 ccid 参数 SQL 注入",
            "正向：返回200+result+success；负向：返回401"
        );
        
        // Goby POC 11: Apache Solr 文件读取
        testPocBothWays(
            "Apache Solr 任意文件读取",
            gobyBase + "Apache Solr任意文件读取漏洞.json",
            BASE_URL + "/vuln/goby/solr",
            BASE_URL + "/safe/goby/solr",
            "Apache Solr 未授权访问导致文件读取",
            "正向：返回200+responseHeader；负向：返回401"
        );
        
        // Goby POC 12: Spring Boot Actuator 未授权
        testPocBothWays(
            "Spring Boot Actuator 未授权",
            gobyBase + "Spring_boot_actuator_unauthorized_access.json",
            BASE_URL + "/vuln/goby/springboot-actuator",
            BASE_URL + "/safe/goby/springboot-actuator",
            "Spring Boot Actuator 端点未授权访问",
            "正向：返回200+actuator；负向：返回401"
        );
        
        // Goby POC 13: Adslr 信息泄露
        testPocBothWays(
            "Adslr 信息泄露",
            gobyBase + "Adslr_Enterprise_online_behavior_management_system_Information_leak.json",
            BASE_URL + "/vuln/goby/adslr",
            BASE_URL + "/safe/goby/adslr",
            "Adslr 企业上网行为管理系统信息泄露",
            "正向：返回200+WPA-PSK；负向：返回401"
        );
        
        // Goby POC 14: AVCON6 文件下载
        testPocPositiveOnly(
            "AVCON6 任意文件下载",
            gobyBase + "AVCON6_org_execl_download.action_file_down.json",
            BASE_URL + "/vuln/goby/avcon6",
            "AVCON6 任意文件下载漏洞",
            "正向：返回200+root（路径遍历类只测正向）"
        );
        
        // Goby POC 15: ADSelfService Plus RCE
        testPocBothWays(
            "ADSelfService Plus RCE CVE-2021-40539",
            gobyBase + "ADSelfService_Plus_RCE_CVE_2021_40539.json",
            BASE_URL + "/vuln/goby/adselfservice",
            BASE_URL + "/safe/goby/adselfservice",
            "ManageEngine ADSelfService Plus 远程代码执行",
            "正向：返回200+tabLogo；负向：返回403"
        );
        
        // Goby POC 16: RuoYi Druid 未授权
        testPocBothWays(
            "RuoYi Druid 未授权访问",
            gobyBase + "RuoYi_Druid_Unauthorized_access.json",
            BASE_URL + "/vuln/goby/ruoyi-druid",
            BASE_URL + "/safe/goby/ruoyi-druid",
            "RuoYi 框架 Druid 监控页面未授权",
            "正向：返回200+DruidStatService；负向：返回401"
        );
        
        // Goby POC 17: XXL-JOB 默认密码
        testPocBothWays(
            "XXL-JOB 默认弱口令",
            gobyBase + "XXL_JOB_Default_password.json",
            BASE_URL + "/vuln/goby/xxljob",
            BASE_URL + "/safe/goby/xxljob",
            "XXL-JOB 任务调度中心默认密码",
            "正向：返回200+登录成功；负向：返回401"
        );
        
        // Goby POC 18: Alibaba Canal 默认密码
        testPocBothWays(
            "Alibaba Canal 默认密码",
            gobyBase + "alibaba_canal_default_password.json",
            BASE_URL + "/vuln/goby/canal",
            BASE_URL + "/safe/goby/canal",
            "Alibaba Canal Admin 默认密码",
            "正向：返回200+success；负向：返回401"
        );
        
        // Goby POC 19: SonarQube 未授权
        testPocBothWays(
            "SonarQube 未授权访问 CVE-2020-27986",
            gobyBase + "SonarQube_unauth_CVE_2020_27986.json",
            BASE_URL + "/vuln/goby/sonarqube",
            BASE_URL + "/safe/goby/sonarqube",
            "SonarQube API 未授权访问",
            "正向：返回200+sonar.core.id；负向：返回401"
        );
        
        // Goby POC 20: Apache Druid 文件读取
        testPocBothWays(
            "Apache Druid 文件读取 CVE-2021-36749",
            gobyBase + "Apache_Druid_Abritrary_File_Read_CVE_2021_36749.json",
            BASE_URL + "/vuln/goby/druid-fileread",
            BASE_URL + "/safe/goby/druid-fileread",
            "Apache Druid 任意文件读取漏洞",
            "正向：返回200+root:x:；负向：返回401"
        );
        
        // Goby POC 21: Apache Flink 文件读取
        testPocPositiveOnly(
            "Apache Flink 文件读取 CVE-2020-17519",
            gobyBase + "Apache_Flink_CVE_2020_17519.json",
            BASE_URL + "/vuln/goby/flink",
            "Apache Flink 路径遍历导致文件读取",
            "正向：返回200+/bin/bash（路径遍历只测正向）"
        );
        
        // Goby POC 22: Apache Airflow 未授权
        testPocBothWays(
            "Apache Airflow 未授权访问",
            gobyBase + "Apache_Airflow_Unauthorized.json",
            BASE_URL + "/vuln/goby/airflow",
            BASE_URL + "/safe/goby/airflow",
            "Apache Airflow 管理界面未授权",
            "正向：返回200+Airflow+DAGs；负向：返回401"
        );
        
        // Goby POC 23: Apache CouchDB 未授权
        testPocBothWays(
            "Apache CouchDB 未授权访问",
            gobyBase + "Apache_CouchDB_Unauth.json",
            BASE_URL + "/vuln/goby/couchdb",
            BASE_URL + "/safe/goby/couchdb",
            "Apache CouchDB 未授权访问",
            "正向：返回200+couchdb；负向：返回401"
        );
        
        // Goby POC 24: Apache Dubbo Admin 默认密码
        testPocBothWays(
            "Apache Dubbo Admin 默认密码",
            gobyBase + "Apache_Dubbo_Admin_Default_Password.json",
            BASE_URL + "/vuln/goby/dubbo",
            BASE_URL + "/safe/goby/dubbo",
            "Apache Dubbo Admin 默认密码",
            "正向：返回200+Dubbo；负向：返回401"
        );
        
        // Goby POC 25: Metabase 文件读取
        testPocBothWays(
            "Metabase 文件读取 CVE-2021-41277",
            gobyBase + "Metabase_Geojson_Arbitrary_File_Read_CVE_2021_41277.json",
            BASE_URL + "/vuln/goby/metabase",
            BASE_URL + "/safe/goby/metabase",
            "Metabase GeoJSON 任意文件读取",
            "正向：返回200+root:x:；负向：返回401"
        );
        
        // Goby POC 26: Weblogic SSRF
        testPocBothWays(
            "Weblogic SSRF CVE-2014-4210",
            gobyBase + "Weblogic_SSRF.json",
            BASE_URL + "/vuln/goby/weblogic-ssrf",
            BASE_URL + "/safe/goby/weblogic-ssrf",
            "Oracle WebLogic Server SSRF 漏洞",
            "正向：返回200+XML_SoapException；负向：返回401"
        );
        
        // Goby POC 27: Jetty WEB-INF 文件读取
        testPocPositiveOnly(
            "Jetty WEB-INF 文件读取 CVE-2021-34429",
            gobyBase + "Jetty_WEB_INF_FileRead_CVE_2021_34429.json",
            BASE_URL + "/vuln/goby/jetty",
            "Jetty 路径遍历导致敏感文件读取",
            "正向：返回200+web.xml（路径遍历只测正向）"
        );
        
        // Goby POC 28: Laravel .env 泄露
        testPocBothWays(
            "Laravel .env 配置泄露 CVE-2017-16894",
            gobyBase + "Laravel_.env_configuration_file_leaks_CVE_2017_16894.json",
            BASE_URL + "/vuln/goby/laravel",
            BASE_URL + "/safe/goby/laravel",
            "Laravel .env 配置文件泄露",
            "正向：返回200+APP_KEY；负向：返回404"
        );
        
        // Goby POC 29: VMware vCenter 文件读取
        testPocBothWays(
            "VMware vCenter 文件读取",
            gobyBase + "VMware_vCenter_v7.0.2_Arbitrary_File_Read.json",
            BASE_URL + "/vuln/goby/vcenter",
            BASE_URL + "/safe/goby/vcenter",
            "VMware vCenter 任意文件读取",
            "正向：返回200+root；负向：返回401"
        );
        
        // Goby POC 30: Apache ShenYu 未授权
        testPocBothWays(
            "Apache ShenYu 未授权访问 CVE-2022-23944",
            gobyBase + "Apache_ShenYu_Admin_Unauth_Access_CVE_2022_23944.json",
            BASE_URL + "/vuln/goby/shenyu",
            BASE_URL + "/safe/goby/shenyu",
            "Apache ShenYu Admin 未授权访问",
            "正向：返回200+query success；负向：返回401"
        );
        
        // Goby POC 31: Spring Cloud Function SpEL RCE
        testPocBothWays(
            "Spring Cloud Function SpEL RCE CVE-2022-22963",
            gobyBase + "Spring_Cloud_Function_SpEL_RCE_CVE_2022_22963.json",
            BASE_URL + "/vuln/goby/spring-cloud-function",
            BASE_URL + "/safe/goby/spring-cloud-function",
            "Spring Cloud Function SpEL 注入",
            "正向：返回500+Runtime；负向：返回404"
        );
        
        // Goby POC 32: Spring Cloud Gateway RCE
        testPocBothWays(
            "Spring Cloud Gateway SpEL RCE CVE-2022-22947",
            gobyBase + "Spring_Cloud_Gateway_Actuator_API_SpEL_Code_Injection_CVE_2022_22947.json",
            BASE_URL + "/vuln/goby/spring-cloud-gateway",
            BASE_URL + "/safe/goby/spring-cloud-gateway",
            "Spring Cloud Gateway 代码注入",
            "正向：返回200+routes；负向：返回401"
        );
        
        // Goby POC 33: Atlassian Confluence OGNL RCE
        testPocBothWays(
            "Atlassian Confluence OGNL RCE CVE-2022-26134",
            gobyBase + "Atlassian_Confluence_OGNL_Injection_RCE_CVE_2022_26134.json",
            BASE_URL + "/vuln/goby/confluence",
            BASE_URL + "/safe/goby/confluence",
            "Atlassian Confluence OGNL 注入",
            "正向：返回200+uid=0；负向：返回401"
        );
        
        // Goby POC 34: VMware Workspace ONE RCE
        testPocBothWays(
            "VMware Workspace ONE RCE CVE-2022-22954",
            gobyBase + "VMware_Workspace_ONE_Access_RCE_CVE_2022_22954.json",
            BASE_URL + "/vuln/goby/vmware-workspace",
            BASE_URL + "/safe/goby/vmware-workspace",
            "VMware Workspace ONE SSTI RCE",
            "正向：返回200+Execute；负向：返回401"
        );
        
        // Goby POC 35: F5 BIG-IP iControl RCE
        testPocBothWays(
            "F5 BIG-IP iControl RCE CVE-2022-1388",
            gobyBase + "F5_BIG_IP_iControl_REST_API_auth_bypass_CVE_2022_1388.json",
            BASE_URL + "/vuln/goby/f5-bigip",
            BASE_URL + "/safe/goby/f5-bigip",
            "F5 BIG-IP iControl REST 命令执行",
            "正向：返回200+commandResult；负向：返回401"
        );
        
        // Goby POC 36: 致远OA 信息泄露
        testPocBothWays(
            "致远OA 用户信息泄露",
            gobyBase + "Seeyon_OA_A6_initDataAssess.jsp_User_information_leakage.json",
            BASE_URL + "/vuln/goby/seeyon",
            BASE_URL + "/safe/goby/seeyon",
            "致远OA A6 用户信息泄露",
            "正向：返回200+users；负向：返回401"
        );
        
        // Goby POC 37: 用友NC BshServlet RCE
        testPocBothWays(
            "用友NC BshServlet RCE",
            gobyBase + "yongyou_NC_bsh.servlet.BshServlet_RCE.json",
            BASE_URL + "/vuln/goby/yongyou-nc",
            BASE_URL + "/safe/goby/yongyou-nc",
            "用友NC BeanShell 远程代码执行",
            "正向：返回200+BeanShell；负向：返回404"
        );
        
        // Goby POC 38: 泛微OA 文件读取
        testPocBothWays(
            "泛微OA 文件读取",
            gobyBase + "Landray_OA_custom.jsp_Fileread.json",
            BASE_URL + "/vuln/goby/weaver-oa",
            BASE_URL + "/safe/goby/weaver-oa",
            "泛微OA 任意文件读取",
            "正向：返回200+database；负向：返回401"
        );
        
        // Goby POC 39: ShopXO 文件读取
        testPocBothWays(
            "ShopXO 任意文件读取 CNVD-2021-15822",
            gobyBase + "ShopXO_Fileread_CNVD_2021_15822.json",
            BASE_URL + "/vuln/goby/shopxo",
            BASE_URL + "/safe/goby/shopxo",
            "ShopXO 任意文件读取",
            "正向：返回200+root；负向：返回401"
        );
        
        // Goby POC 40: YAPI RCE
        testPocBothWays(
            "YAPI 远程代码执行",
            gobyBase + "YAPI_RCE.json",
            BASE_URL + "/vuln/goby/yapi",
            BASE_URL + "/safe/goby/yapi",
            "YAPI Mock 功能远程代码执行",
            "正向：返回200+errcode:0；负向：返回401"
        );
        
        // Goby POC 41: Ruijie Smartweb 弱口令
        testPocBothWays(
            "锐捷 Smartweb 弱口令",
            gobyBase + "Ruijie_Smartweb_Default_Password_CNVD_2020_56167.json",
            BASE_URL + "/vuln/goby/ruijie",
            BASE_URL + "/safe/goby/ruijie",
            "锐捷 Smartweb 默认密码",
            "正向：返回200+login_ok；负向：返回401"
        );
        
        // Goby POC 42: 大华 DSS 文件下载
        testPocBothWays(
            "大华 DSS 任意文件下载",
            gobyBase + "Zhejiang_Dahua_DSS_System_Filedownload_CNVD_2020_61986.json",
            BASE_URL + "/vuln/goby/dahua",
            BASE_URL + "/safe/goby/dahua",
            "大华 DSS 任意文件下载漏洞",
            "正向：返回200+root；负向：返回401"
        );
        
        // Goby POC 43: Zabbix SAML 认证绕过
        testPocBothWays(
            "Zabbix SAML 认证绕过 CVE-2022-23131",
            gobyBase + "zabbix_saml_cve_2022_23131.json",
            BASE_URL + "/vuln/goby/zabbix",
            BASE_URL + "/safe/goby/zabbix",
            "Zabbix SAML SSO 认证绕过",
            "正向：返回302重定向；负向：返回401"
        );
        
        // Goby POC 44: D-Link 路由器 RCE
        testPocBothWays(
            "D-Link 路由器 RCE CVE-2019-16920",
            gobyBase + "Dlink_RCE_CVE_2019_16920.json",
            BASE_URL + "/vuln/goby/dlink",
            BASE_URL + "/safe/goby/dlink",
            "D-Link 路由器命令执行漏洞",
            "正向：返回200+admin；负向：返回401"
        );
        
        // Goby POC 45: Samsung WLAN AP RCE
        testPocBothWays(
            "Samsung WLAN AP RCE CVE-2020-10587",
            gobyBase + "Samsung_WLAN_AP_RCE.json",
            BASE_URL + "/vuln/goby/samsung",
            BASE_URL + "/safe/goby/samsung",
            "Samsung WLAN AP 命令执行漏洞",
            "正向：返回200+uid=0；负向：返回401"
        );
        
        // Goby POC 46: MinIO SSRF
        testPocBothWays(
            "MinIO SSRF CVE-2021-21287",
            gobyBase + "MinIO_Browser_API_SSRF_CVE_2021_21287.json",
            BASE_URL + "/vuln/goby/minio",
            BASE_URL + "/safe/goby/minio",
            "MinIO SSRF 漏洞",
            "正向：返回200+version；负向：返回401"
        );
        
        // Goby POC 47: Node.js 路径遍历
        testPocPositiveOnly(
            "Node.js Express 路径遍历 CVE-2017-14849",
            gobyBase + "Node.js_Path_Traversal_CVE_2017_14849.json",
            BASE_URL + "/vuln/goby/nodejs",
            "Node.js Express 路径遍历漏洞",
            "正向：返回200+root（路径遍历只测正向）"
        );
        
        // Goby POC 48: PHP 8.1 后门
        testPocBothWays(
            "PHP 8.1 后门 CVE-2024-4577",
            gobyBase + "PHP_8.1.0-dev_Zerodium_Backdoor_RCE.json",
            BASE_URL + "/vuln/goby/php8",
            BASE_URL + "/safe/goby/php8",
            "PHP 8.1.0-dev 后门代码执行",
            "正向：返回200+uid=0；负向：返回200 without uid"
        );
        
        // Goby POC 49: Portainer RCE
        testPocBothWays(
            "Portainer 未授权 RCE",
            gobyBase + "Portainer_Init_Deploy_CVE_2018_19367.json",
            BASE_URL + "/vuln/goby/portainer",
            BASE_URL + "/safe/goby/portainer",
            "Portainer 未授权 API 访问",
            "正向：返回200+Username；负向：返回409"
        );
        
        // Goby POC 50: GitLab RCE CVE-2021-22205
        testPocBothWays(
            "GitLab RCE CVE-2021-22205",
            gobyBase + "Gitlab_RCE_CVE_2021_22205.json",
            BASE_URL + "/vuln/goby/gitlab",
            BASE_URL + "/safe/goby/gitlab",
            "GitLab ExifTool 远程代码执行",
            "正向：返回200+Created；负向：返回401"
        );
        
        // Goby POC 51: Grafana 任意文件读取
        testPocPositiveOnly(
            "Grafana 任意文件读取 CVE-2021-43798",
            gobyBase + "Grafana_v8.x_Arbitrary_File_Read_CVE_2021_43798.json",
            BASE_URL + "/vuln/goby/grafana",
            "Grafana 8.x 任意文件读取",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 52: Consul Rexec RCE
        testPocBothWays(
            "Consul Rexec RCE",
            gobyBase + "Consul_Rexec_RCE.json",
            BASE_URL + "/vuln/goby/consul",
            BASE_URL + "/safe/goby/consul",
            "Consul 远程执行漏洞",
            "正向：返回200+DisableRemoteExec:false，负向：返回403"
        );
        
        // Goby POC 53: Coremail 配置泄露
        testPocBothWays(
            "Coremail Config Disclosure",
            gobyBase + "Coremail_Config_Disclosure.json",
            BASE_URL + "/vuln/goby/coremail",
            BASE_URL + "/safe/goby/coremail",
            "Coremail 配置信息泄露",
            "正向：返回200+configHome，负向：返回404"
        );
        
        // Goby POC 54: Docker Registry API 未授权
        testPocBothWays(
            "Docker Registry API Unauth",
            gobyBase + "Docker_Registry_API_Unauth.json",
            BASE_URL + "/vuln/goby/docker-registry",
            BASE_URL + "/safe/goby/docker-registry",
            "Docker Registry API 未授权访问",
            "正向：返回200+repositories，负向：返回401"
        );
        
        // Goby POC 55: D-Link DCS 密码泄露
        testPocBothWays(
            "D-Link DCS 密码泄露 CVE-2020-25078",
            gobyBase + "D-Link_DCS_2530L_Administrator_password_disclosure_CVE_2020_25078.json",
            BASE_URL + "/vuln/goby/dlink-dcs",
            BASE_URL + "/safe/goby/dlink-dcs",
            "D-Link DCS 监控密码泄露",
            "正向：返回200+name+pass，负向：返回401"
        );
        
        // Goby POC 56: Discuz RCE
        testPocBothWays(
            "Discuz RCE WOOYUN-2010-080723",
            gobyBase + "Discuz_RCE_WOOYUN_2010_080723.json",
            BASE_URL + "/vuln/goby/discuz",
            BASE_URL + "/safe/goby/discuz",
            "Discuz 远程代码执行",
            "正向：返回200+PHP Version，负向：返回普通页面"
        );
        
        // Goby POC 57: DedeCMS 信息泄露
        testPocBothWays(
            "DedeCMS InfoLeak CVE-2018-6910",
            gobyBase + "DedeCMS_InfoLeak_CVE_2018_6910.json",
            BASE_URL + "/vuln/goby/dedecms",
            BASE_URL + "/safe/goby/dedecms",
            "DedeCMS 路径泄露",
            "正向：返回200+Fatal error+downmix.inc.php，负向：返回404"
        );
        
        // Goby POC 58: FineReport 目录遍历
        testPocPositiveOnly(
            "FineReport 目录遍历",
            gobyBase + "FineReport_Directory_traversal.json",
            BASE_URL + "/vuln/goby/finereport",
            "FineReport 目录遍历漏洞",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 59: GoCD 文件读取
        testPocPositiveOnly(
            "GoCD 文件读取 CVE-2021-43287",
            gobyBase + "GoCD_Arbitrary_file_reading_CVE_2021_43287.json",
            BASE_URL + "/vuln/goby/gocd",
            "GoCD 任意文件读取",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 60: Confluence OGNL RCE CVE-2021-26084
        testPocBothWays(
            "Confluence OGNL RCE CVE-2021-26084",
            gobyBase + "Confluence_RCE_CVE_2021_26084.json",
            BASE_URL + "/vuln/goby/confluence-ognl",
            BASE_URL + "/safe/goby/confluence-ognl",
            "Confluence OGNL 注入 RCE",
            "正向：返回200+uid=0，负向：返回302"
        );
        
        // Goby POC 61: Jira 路径遍历
        testPocPositiveOnly(
            "Jira 路径遍历 CVE-2021-26086",
            gobyBase + "Atlassian_Jira_Path_Traversal_CVE_2021_26086.json",
            BASE_URL + "/vuln/goby/jira-path",
            "Jira 路径遍历漏洞",
            "正向：返回200+WEB-INF（路径遍历只测正向）"
        );
        
        // Goby POC 62: Jira 信息泄露
        testPocBothWays(
            "Jira 信息泄露 CVE-2020-14181",
            gobyBase + "Atlassian_Jira_user_information_disclosure_CVE_2020_14181.json",
            BASE_URL + "/vuln/goby/jira-infoleak",
            BASE_URL + "/safe/goby/jira-infoleak",
            "Jira 用户信息泄露",
            "正向：返回200+users，负向：返回401"
        );
        
        // Goby POC 63: CraftCMS Seomatic RCE
        testPocBothWays(
            "CraftCMS Seomatic RCE CVE-2020-9597",
            gobyBase + "CraftCMS_Seomatic_RCE_CVE_2020_9597.json",
            BASE_URL + "/vuln/goby/craftcms",
            BASE_URL + "/safe/goby/craftcms",
            "CraftCMS Seomatic 远程代码执行",
            "正向：返回200+uid=0，负向：返回404"
        );
        
        // Goby POC 64: Cacti Weathermap 文件写入
        testPocBothWays(
            "Cacti Weathermap 文件写入",
            gobyBase + "Cacti_Weathermap_File_Write.json",
            BASE_URL + "/vuln/goby/cacti",
            BASE_URL + "/safe/goby/cacti",
            "Cacti Weathermap 文件写入",
            "正向：返回200+Weathermap，负向：返回403"
        );
        
        // Goby POC 65: ClickHouse SQL注入
        testPocBothWays(
            "ClickHouse SQLI",
            gobyBase + "ClickHouse_SQLI.json",
            BASE_URL + "/vuln/goby/clickhouse",
            BASE_URL + "/safe/goby/clickhouse",
            "ClickHouse SQL注入漏洞",
            "正向：返回200+1，负向：返回401"
        );
        
        // Goby POC 66: Citrix 未授权 LFI
        testPocPositiveOnly(
            "Citrix 未授权 LFI CVE-2020-8193",
            gobyBase + "Citrix_Unauthorized_CVE_2020_8193.json",
            BASE_URL + "/vuln/goby/citrix",
            "Citrix 本地文件包含",
            "正向：返回200+workgroup（路径遍历只测正向）"
        );
        
        // Goby POC 67: CouchCMS 信息泄露
        testPocBothWays(
            "CouchCMS 信息泄露 CVE-2018-7662",
            gobyBase + "Couch_CMS_Infoleak_CVE_2018_7662.json",
            BASE_URL + "/vuln/goby/couchcms",
            BASE_URL + "/safe/goby/couchcms",
            "CouchCMS 配置信息泄露",
            "正向：返回200+admin_email，负向：返回404"
        );
        
        // Goby POC 68: D-Link DIR-850L 信息泄露
        testPocBothWays(
            "D-Link DIR-850L Info Leak",
            gobyBase + "Dlink_850L_Info_Leak.json",
            BASE_URL + "/vuln/goby/dlink-dir850l",
            BASE_URL + "/safe/goby/dlink-dir850l",
            "D-Link DIR-850L 信息泄露",
            "正向：返回200+password，负向：返回401"
        );
        
        // Goby POC 69: D-Link ShareCenter DNS-320 RCE
        testPocBothWays(
            "D-Link ShareCenter DNS-320 RCE",
            gobyBase + "D_Link_ShareCenter_DNS_320_RCE.json",
            BASE_URL + "/vuln/goby/dlink-sharecenter",
            BASE_URL + "/safe/goby/dlink-sharecenter",
            "D-Link ShareCenter DNS-320 远程代码执行",
            "正向：返回200+uid=0，负向：返回404"
        );
        
        // Goby POC 70: Datang AC 默认密码
        testPocBothWays(
            "Datang AC Default Password",
            gobyBase + "Datang_AC_Default_Password.json",
            BASE_URL + "/vuln/goby/datang-ac",
            BASE_URL + "/safe/goby/datang-ac",
            "大唐 AC 默认密码",
            "正向：返回200+success+token，负向：返回401"
        );
        
        // Goby POC 71: DocCMS SQL注入
        testPocBothWays(
            "DocCMS SQL注入",
            gobyBase + "DocCMS_keyword_SQL_injection_Vulnerability.json",
            BASE_URL + "/vuln/goby/doccms",
            BASE_URL + "/safe/goby/doccms",
            "DocCMS keyword SQL注入",
            "正向：返回200+data+username，负向：返回空data"
        );
        
        // Goby POC 72: DotCMS 文件上传
        testPocBothWays(
            "DotCMS 任意文件上传 CVE-2022-26352",
            gobyBase + "DotCMS_Arbitrary_File_Upload_CVE_2022_26352.json",
            BASE_URL + "/vuln/goby/dotcms",
            BASE_URL + "/safe/goby/dotcms",
            "DotCMS 任意文件上传漏洞",
            "正向：返回200+File uploaded，负向：返回403"
        );
        
        // Goby POC 73: F5 BIG-IP RCE CVE-2021-22986
        testPocBothWays(
            "F5 BIG-IP RCE CVE-2021-22986",
            gobyBase + "F5_BIG_IP_iControl_REST_Unauthenticated_RCE_CVE_2021_22986.json",
            BASE_URL + "/vuln/goby/f5-rce",
            BASE_URL + "/safe/goby/f5-rce",
            "F5 BIG-IP iControl REST 未授权 RCE",
            "正向：返回200+commandResult+uid=0，负向：返回401"
        );
        
        // Goby POC 74: Fastmeeting 文件读取
        testPocPositiveOnly(
            "Fastmeeting 任意文件读取",
            gobyBase + "Fastmeeting_Arbitrary_File_Read.json",
            BASE_URL + "/vuln/goby/fastmeeting",
            "Fastmeeting 任意文件读取",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 75: GitLab SSRF
        testPocBothWays(
            "GitLab SSRF CVE-2021-22214",
            gobyBase + "GitLab_SSRF_CVE_2021_22214.json",
            BASE_URL + "/vuln/goby/gitlab-ssrf",
            BASE_URL + "/safe/goby/gitlab-ssrf",
            "GitLab SSRF 漏洞",
            "正向：返回200+projects，负向：返回401"
        );
        
        // Goby POC 76: GitLab GraphQL 邮箱泄露
        testPocBothWays(
            "GitLab GraphQL 邮箱泄露 CVE-2020-26413",
            gobyBase + "GitLab_Graphql_Email_information_disclosure_CVE_2020_26413.json",
            BASE_URL + "/vuln/goby/gitlab-graphql",
            BASE_URL + "/safe/goby/gitlab-graphql",
            "GitLab GraphQL 邮箱信息泄露",
            "正向：返回200+email，负向：返回401"
        );
        
        // Goby POC 77: Apache Struts2 S2-053 RCE
        testPocBothWays(
            "Apache Struts2 S2-053 RCE CVE-2017-12611",
            gobyBase + "Apache_Struts2_S2_053_RCE_CVE_2017_12611.json",
            BASE_URL + "/vuln/goby/struts2-s2053",
            BASE_URL + "/safe/goby/struts2-s2053",
            "Apache Struts2 S2-053 远程代码执行",
            "正向：返回200+uid=0，负向：返回OK"
        );
        
        // Goby POC 78: Apache Struts2 S2-059 RCE
        testPocBothWays(
            "Apache Struts2 S2-059 RCE CVE-2019-0230",
            gobyBase + "Apache_Struts2_S2_059_RCE_CVE_2019_0230.json",
            BASE_URL + "/vuln/goby/struts2-s2059",
            BASE_URL + "/safe/goby/struts2-s2059",
            "Apache Struts2 S2-059 远程代码执行",
            "正向：返回200+uid=0，负向：返回OK"
        );
        
        // Goby POC 79: Apache Struts2 S2-062 RCE
        testPocBothWays(
            "Apache Struts2 S2-062 RCE CVE-2021-31805",
            gobyBase + "Apache_Struts2_S2_062_RCE_CVE_2021_31805.json",
            BASE_URL + "/vuln/goby/struts2-s2062",
            BASE_URL + "/safe/goby/struts2-s2062",
            "Apache Struts2 S2-062 远程代码执行",
            "正向：返回200+uid=0，负向：返回OK"
        );
        
        // Goby POC 80: AspCMS SQL注入
        testPocBothWays(
            "AspCMS SQL注入",
            gobyBase + "AspCMS_commentList.asp_SQLinjection_vulnerability.json",
            BASE_URL + "/vuln/goby/aspcms",
            BASE_URL + "/safe/goby/aspcms",
            "AspCMS commentList SQL注入",
            "正向：返回200+Microsoft SQL Server，负向：返回OK"
        );
        
        // Goby POC 81: Eyou 邮件系统 RCE
        testPocBothWays(
            "Eyou Mail RCE CNVD-2021-26422",
            gobyBase + "Eyou_Mail_System_RCE_CNVD_2021_26422.json",
            BASE_URL + "/vuln/goby/eyou",
            BASE_URL + "/safe/goby/eyou",
            "亿邮邮件系统远程代码执行",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 82: H3C IMC RCE
        testPocBothWays(
            "H3C IMC RCE",
            gobyBase + "H3C_IMC_RCE.json",
            BASE_URL + "/vuln/goby/h3c-imc",
            BASE_URL + "/safe/goby/h3c-imc",
            "H3C IMC 远程代码执行",
            "正向：返回200+uid=0，负向：返回401"
        );
        
        // Goby POC 83: H5S Video Platform 信息泄露
        testPocBothWays(
            "H5S Video Platform Info Leak",
            gobyBase + "H5S_CONSOLE_Video_Platform_GetSrc_Information_Leak_CNVD_2021_25919.json",
            BASE_URL + "/vuln/goby/h5s-video",
            BASE_URL + "/safe/goby/h5s-video",
            "H5S 视频平台信息泄露",
            "正向：返回200+strUser+strPassword，负向：返回401"
        );
        
        // Goby POC 84: HIKVISION 文件下载
        testPocPositiveOnly(
            "HIKVISION 任意文件下载",
            gobyBase + "HIKVISION_Video_coding_equipment_Download_any_file.json",
            BASE_URL + "/vuln/goby/hikvision",
            "海康威视设备任意文件下载",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 85: IFW8 Router 密码泄露
        testPocBothWays(
            "IFW8 Router Password Leak CVE-2019-16313",
            gobyBase + "IFW8_Router_ROM_v4.31_Credential_Discovery_CVE_2019_16313.json",
            BASE_URL + "/vuln/goby/ifw8",
            BASE_URL + "/safe/goby/ifw8",
            "IFW8 路由器密码泄露",
            "正向：返回200+admin:password，负向：返回404"
        );
        
        // Goby POC 86: IceWarp WebClient RCE
        testPocBothWays(
            "IceWarp WebClient RCE",
            gobyBase + "IceWarp_WebClient_basic_RCE.json",
            BASE_URL + "/vuln/goby/icewarp",
            BASE_URL + "/safe/goby/icewarp",
            "IceWarp WebClient 远程代码执行",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 87: Jellyfin 任意文件读取
        testPocPositiveOnly(
            "Jellyfin 任意文件读取 CVE-2021-21402",
            gobyBase + "Jellyfin_10.7.0_Unauthenticated_Abritrary_File_Read_CVE_2021_21402.json",
            BASE_URL + "/vuln/goby/jellyfin",
            "Jellyfin 任意文件读取",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 88: Jetty WEB-INF 文件读取
        testPocPositiveOnly(
            "Jetty WEB-INF FileRead CVE-2021-28169",
            gobyBase + "Jetty_WEB_INF_FileRead_CVE_2021_28169.json",
            BASE_URL + "/vuln/goby/jetty",
            "Jetty WEB-INF 敏感文件读取",
            "正向：返回200+web-app（路径遍历只测正向）"
        );
        
        // Goby POC 89: 金和 OA C6 默认密码
        testPocBothWays(
            "JinHe OA C6 Default Password",
            gobyBase + "JinHe_OA_C6_Default_password.json",
            BASE_URL + "/vuln/goby/jinhe-oa",
            BASE_URL + "/safe/goby/jinhe-oa",
            "金和 OA C6 默认密码",
            "正向：返回200+success+token，负向：返回401"
        );
        
        // Goby POC 90: 极通 EWEBS 文件读取
        testPocPositiveOnly(
            "Jitong EWEBS Fileread",
            gobyBase + "Jitong_EWEBS_Fileread.json",
            BASE_URL + "/vuln/goby/jitong-ewebs",
            "极通 EWEBS 任意文件读取",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 91: KEDACOM MTS 文件读取
        testPocPositiveOnly(
            "KEDACOM MTS 任意文件下载 CNVD-2020-48650",
            gobyBase + "KEDACOM_MTS_transcoding_server_Fileread_CNVD_2020_48650.json",
            BASE_URL + "/vuln/goby/kedacom-mts",
            "科达 MTS 转码服务器文件下载",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 92: 金山 V8 任意文件读取
        testPocPositiveOnly(
            "Kingsoft V8 Arbitrary File Read",
            gobyBase + "Kingsoft_V8_Arbitrary_file_read.json",
            BASE_URL + "/vuln/goby/kingsoft-v8",
            "金山 V8 任意文件读取",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 93: Konga 默认 JWT Key
        testPocBothWays(
            "Konga Default JWT Key",
            gobyBase + "Konga_Default_JWT_KEY.json",
            BASE_URL + "/vuln/goby/konga",
            BASE_URL + "/safe/goby/konga",
            "Konga 默认 JWT 密钥",
            "正向：返回200+token，负向：返回401"
        );
        
        // Goby POC 94: Kyan 账号密码泄露
        testPocBothWays(
            "Kyan Account Password Leak",
            gobyBase + "Kyan_Account_password_leak.json",
            BASE_URL + "/vuln/goby/kyan",
            BASE_URL + "/safe/goby/kyan",
            "Kyan 网络监控账号密码泄露",
            "正向：返回200+admin+root，负向：返回401"
        );
        
        // Goby POC 95: Lanproxy 目录遍历
        testPocPositiveOnly(
            "Lanproxy 目录遍历 CVE-2021-3019",
            gobyBase + "Lanproxy_Directory_traversal_CVE_2021_3019.json",
            BASE_URL + "/vuln/goby/lanproxy",
            "Lanproxy 目录遍历漏洞",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 96: Laravel .env 配置泄露
        testPocBothWays(
            "Laravel .env Config Leak CVE-2017-16894",
            gobyBase + "Laravel_.env_configuration_file_leaks_CVE_2017_16894.json",
            BASE_URL + "/vuln/goby/laravel-env",
            BASE_URL + "/safe/goby/laravel-env",
            "Laravel .env 配置文件泄露",
            "正向：返回200+APP_NAME+APP_KEY，负向：返回404"
        );
        
        // Goby POC 97: Leadsec ACM 信息泄露
        testPocBothWays(
            "Leadsec ACM Info Leak CNVD-2016-08574",
            gobyBase + "Leadsec_ACM_information_leakage_CNVD_2016_08574.json",
            BASE_URL + "/vuln/goby/leadsec-acm",
            BASE_URL + "/safe/goby/leadsec-acm",
            "网御 ACM 信息泄露",
            "正向：返回200+username+password，负向：返回401"
        );
        
        // Goby POC 98: MPSec ISG1000 文件下载
        testPocPositiveOnly(
            "MPSec ISG1000 Filedownload CNVD-2021-43984",
            gobyBase + "MPSec_ISG1000_Gateway_Filedownload_CNVD_2021_43984.json",
            BASE_URL + "/vuln/goby/mpsec-isg",
            "迈普安全 ISG1000 文件下载",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 99: Metabase 任意文件读取
        testPocPositiveOnly(
            "Metabase Geojson File Read CVE-2021-41277",
            gobyBase + "Metabase_Geojson_Arbitrary_File_Read_CVE_2021_41277.json",
            BASE_URL + "/vuln/goby/metabase",
            "Metabase Geojson 任意文件读取",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 100: Microsoft Exchange SSRF
        testPocBothWays(
            "Microsoft Exchange SSRF CVE-2021-26885",
            gobyBase + "Microsoft_Exchange_Server_SSRF_CVE_2021_26885.json",
            BASE_URL + "/vuln/goby/exchange-ssrf",
            BASE_URL + "/safe/goby/exchange-ssrf",
            "Microsoft Exchange Server SSRF 漏洞",
            "正向：返回200+X-FEServer，负向：返回401"
        );
        
        // Goby POC 101: MinIO Browser API SSRF
        testPocBothWays(
            "MinIO Browser API SSRF CVE-2021-21287",
            gobyBase + "MinIO_Browser_API_SSRF_CVE_2021_21287.json",
            BASE_URL + "/vuln/goby/minio-ssrf",
            BASE_URL + "/safe/goby/minio-ssrf",
            "MinIO Browser API SSRF 漏洞",
            "正向：返回200+result，负向：返回401"
        );
        
        // Goby POC 102: Node-RED 任意文件读取
        testPocPositiveOnly(
            "Node-RED ui_base File Read CVE-2021-3223",
            gobyBase + "Node_RED_ui_base_Arbitrary_File_Read.json",
            BASE_URL + "/vuln/goby/nodered",
            "Node-RED ui_base 任意文件读取",
            "正向：返回200+root:x:（路径遍历只测正向）"
        );
        
        // Goby POC 103: OpenSNS RCE
        testPocBothWays(
            "OpenSNS RCE",
            gobyBase + "OpenSNS_RCE.json",
            BASE_URL + "/vuln/goby/opensns",
            BASE_URL + "/safe/goby/opensns",
            "OpenSNS 远程代码执行",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 104: Oracle Weblogic LDAP RCE
        testPocBothWays(
            "Oracle Weblogic LDAP RCE CVE-2021-2109",
            gobyBase + "Oracle_Weblogic_LDAP_RCE_CVE_2021_2109.json",
            BASE_URL + "/vuln/goby/weblogic-ldap",
            BASE_URL + "/safe/goby/weblogic-ldap",
            "Oracle Weblogic LDAP RCE 漏洞",
            "正向：返回200+uid=0，负向：返回401"
        );
        
        // Goby POC 105: 向日葵 RCE
        testPocBothWays(
            "Oray Sunlogin RCE CNVD-2022-03672",
            gobyBase + "Oray_Sunlogin_RCE_CNVD_2022_03672_CNVD_2022_10270.json",
            BASE_URL + "/vuln/goby/sunlogin",
            BASE_URL + "/safe/goby/sunlogin",
            "向日葵远程代码执行漏洞",
            "正向：返回200+uid=0，负向：返回404"
        );
        
        // Goby POC 106: Portainer Init Deploy
        testPocBothWays(
            "Portainer Init Deploy CVE-2018-19367",
            gobyBase + "Portainer_Init_Deploy_CVE_2018_19367.json",
            BASE_URL + "/vuln/goby/portainer",
            BASE_URL + "/safe/goby/portainer",
            "Portainer 初始化部署漏洞",
            "正向：返回200+initialized，负向：返回409"
        );
        
        // Goby POC 107: 锐捷 EWEB RCE
        testPocBothWays(
            "Ruijie Networks EWEB RCE CNVD-2021-09650",
            gobyBase + "Ruijie_Networks_EWEB_Network_Management_System_RCE_CNVD_2021_09650.json",
            BASE_URL + "/vuln/goby/ruijie-eweb",
            BASE_URL + "/safe/goby/ruijie-eweb",
            "锐捷 EWEB 网管系统远程代码执行",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 108: 锐捷 RG-UAC 密码泄露
        testPocBothWays(
            "Ruijie RG-UAC Password Leak CNVD-2021-14536",
            gobyBase + "Ruijie_RG_UAC_Password_leakage_CNVD_2021_14536.json",
            BASE_URL + "/vuln/goby/ruijie-uac",
            BASE_URL + "/safe/goby/ruijie-uac",
            "锐捷 RG-UAC 密码泄露",
            "正向：返回200+password，负向：返回401"
        );
        
        // Goby POC 109: 锐捷 Smartweb 弱密码
        testPocBothWays(
            "Ruijie Smartweb Default Password CNVD-2020-56167",
            gobyBase + "Ruijie_Smartweb_Default_Password_CNVD_2020_56167.json",
            BASE_URL + "/vuln/goby/ruijie-smartweb",
            BASE_URL + "/safe/goby/ruijie-smartweb",
            "锐捷 Smartweb 默认密码",
            "正向：返回200+success，负向：返回401"
        );
        
        // Goby POC 110: RuoYi Druid 未授权
        testPocBothWays(
            "RuoYi Druid Unauthorized Access",
            gobyBase + "RuoYi_Druid_Unauthorized_access.json",
            BASE_URL + "/vuln/goby/ruoyi-druid",
            BASE_URL + "/safe/goby/ruoyi-druid",
            "RuoYi Druid 未授权访问",
            "正向：返回200+Druid Monitor，负向：返回401"
        );
        
        // Goby POC 111: Samsung WLAN AP RCE
        testPocBothWays(
            "Samsung WLAN AP RCE",
            gobyBase + "Samsung_WLAN_AP_RCE.json",
            BASE_URL + "/vuln/goby/samsung-wlan",
            BASE_URL + "/safe/goby/samsung-wlan",
            "三星 WLAN AP 远程代码执行",
            "正向：返回200+uid=0，负向：返回401"
        );
        
        // Goby POC 112: SDWAN Smart Gateway 默认密码
        testPocBothWays(
            "SDWAN Smart Gateway Default Password",
            gobyBase + "SDWAN_Smart_Gateway_Default_Password.json",
            BASE_URL + "/vuln/goby/sdwan-gateway",
            BASE_URL + "/safe/goby/sdwan-gateway",
            "SDWAN 智能网关默认密码",
            "正向：返回200+true+userid，负向：返回false"
        );
        
        // Goby POC 113: Seeyon OA A6 DownExcelBeanServlet 信息泄露
        testPocBothWays(
            "Seeyon OA A6 DownExcelBeanServlet Info Leak",
            gobyBase + "Seeyon_OA_A6_DownExcelBeanServlet_User_information_leakage.json",
            BASE_URL + "/vuln/goby/seeyon-downexcel",
            BASE_URL + "/safe/goby/seeyon-downexcel",
            "致远OA A6 用户信息泄露",
            "正向：返回200+包含@邮箱，负向：返回404"
        );
        
        // Goby POC 114: SonarQube 未授权 CVE-2020-27986
        testPocBothWays(
            "SonarQube Unauth CVE-2020-27986",
            gobyBase + "SonarQube_unauth_CVE_2020_27986.json",
            BASE_URL + "/vuln/goby/sonarqube",
            BASE_URL + "/safe/goby/sonarqube",
            "SonarQube 未授权访问",
            "正向：返回200+sonaranalyzer，负向：返回401"
        );
        
        // Goby POC 115: SpiderFlow RCE
        testPocBothWays(
            "SpiderFlow RCE",
            gobyBase + "SpiderFlow_save__remote_code.json",
            BASE_URL + "/vuln/goby/spiderflow",
            BASE_URL + "/safe/goby/spiderflow",
            "SpiderFlow 远程代码执行",
            "正向：返回200+exec，负向：返回403"
        );
        
        // Goby POC 116: Spring Boot Actuator Logview 路径遍历
        testPocPositiveOnly(
            "Spring Boot Actuator Logview Path Traversal",
            gobyBase + "Spring_Boot_Actuator_Logview_Path_Traversal_CVE_2021_21234.json",
            BASE_URL + "/vuln/goby/springboot-logview",
            "Spring Boot Logview 路径遍历",
            "正向：返回200+root:"
        );
        
        // Goby POC 117: TamronOS IPTV RCE
        testPocBothWays(
            "TamronOS IPTV RCE",
            gobyBase + "TamronOS_IPTV_RCE.json",
            BASE_URL + "/vuln/goby/tamronos-iptv",
            BASE_URL + "/safe/goby/tamronos-iptv",
            "TamronOS IPTV 远程代码执行",
            "正向：返回200+uid=，负向：返回403"
        );
        
        // Goby POC 118: 泛微 EOffice 任意文件上传
        testPocBothWays(
            "Weaver EOffice File Upload",
            gobyBase + "Weaver_EOffice_Arbitrary_File_Upload_CNVD_2021_49104.json",
            BASE_URL + "/vuln/goby/weaver-eoffice",
            BASE_URL + "/safe/goby/weaver-eoffice",
            "泛微 EOffice 任意文件上传",
            "正向：返回200+logo-eoffice.php，负向：返回403"
        );
        
        // Goby POC 119: WSO2 文件上传 CVE-2022-29464
        testPocBothWays(
            "WSO2 File Upload CVE-2022-29464",
            gobyBase + "WSO2_fileupload_CVE_2022_29464.json",
            BASE_URL + "/vuln/goby/wso2-upload",
            BASE_URL + "/safe/goby/wso2-upload",
            "WSO2 任意文件上传",
            "正向：返回200+数字，负向：返回403"
        );
        
        // Goby POC 120: Xieda OA 文件下载
        testPocBothWays(
            "Xieda OA File Download",
            gobyBase + "Xieda_OA_Filedownload_CNVD_2021_29066.json",
            BASE_URL + "/vuln/goby/xieda-oa",
            BASE_URL + "/safe/goby/xieda-oa",
            "协达 OA 任意文件下载",
            "正向：返回200+password，负向：返回404"
        );
        
        // Goby POC 121: YAPI RCE
        testPocBothWays(
            "YAPI RCE",
            gobyBase + "YAPI_RCE.json",
            BASE_URL + "/vuln/goby/yapi",
            BASE_URL + "/safe/goby/yapi",
            "YAPI 远程代码执行",
            "正向：返回200+邮箱不能为空，负向：返回禁止注册"
        );
        
        // Goby POC 122: 浙江大华 DSS 文件下载
        testPocBothWays(
            "Zhejiang Dahua DSS File Download",
            gobyBase + "Zhejiang_Dahua_DSS_System_Filedownload_CNVD_2020_61986.json",
            BASE_URL + "/vuln/goby/dahua-dss",
            BASE_URL + "/safe/goby/dahua-dss",
            "浙江大华 DSS 文件下载",
            "正向：返回200+root，负向：返回404"
        );
        
        // Goby POC 123: Docker Registry API 未授权
        testPocBothWays(
            "Docker Registry API Unauth",
            gobyBase + "Docker_Registry_API_Unauth.json",
            BASE_URL + "/vuln/goby/docker-registry",
            BASE_URL + "/safe/goby/docker-registry",
            "Docker Registry API 未授权",
            "正向：返回200+repositories，负向：返回401"
        );
        
        // Goby POC 124: Dubbo Admin 默认密码
        testPocBothWays(
            "Dubbo Admin Default Password",
            gobyBase + "Dubbo_Admin_Default_Password.json",
            BASE_URL + "/vuln/goby/dubbo-admin",
            BASE_URL + "/safe/goby/dubbo-admin",
            "Dubbo Admin 默认密码",
            "正向：返回200+Dubbo Admin，负向：返回401"
        );
        
        // Goby POC 125: Discuz RCE
        testPocBothWays(
            "Discuz RCE",
            gobyBase + "Discuz_RCE_WOOYUN_2010_080723.json",
            BASE_URL + "/vuln/goby/discuz-rce",
            BASE_URL + "/safe/goby/discuz-rce",
            "Discuz 远程代码执行",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 126: DedeCMS 信息泄露
        testPocBothWays(
            "DedeCMS Info Leak CVE-2018-6910",
            gobyBase + "DedeCMS_InfoLeak_CVE_2018_6910.json",
            BASE_URL + "/vuln/goby/dedecms-info",
            BASE_URL + "/safe/goby/dedecms-info",
            "DedeCMS 信息泄露",
            "正向：返回200+版本号，负向：返回404"
        );
        
        // Goby POC 127: D-Link DNS-320 RCE
        testPocBothWays(
            "D-Link DNS-320 RCE",
            gobyBase + "D_Link_ShareCenter_DNS_320_RCE.json",
            BASE_URL + "/vuln/goby/dlink-dns320",
            BASE_URL + "/safe/goby/dlink-dns320",
            "D-Link ShareCenter DNS-320 RCE",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 128: Datang AC 默认密码
        testPocBothWays(
            "Datang AC Default Password",
            gobyBase + "Datang_AC_Default_Password.json",
            BASE_URL + "/vuln/goby/datang-ac",
            BASE_URL + "/safe/goby/datang-ac",
            "大唐 AC 默认密码",
            "正向：返回200+success，负向：返回401"
        );
        
        // Goby POC 129: FineReport 目录遍历
        testPocPositiveOnly(
            "FineReport Directory Traversal",
            gobyBase + "FineReport_Directory_traversal.json",
            BASE_URL + "/vuln/goby/finereport-traversal",
            "FineReport 目录遍历",
            "正向：返回200+root:"
        );
        
        // Goby POC 130: GitLab RCE CVE-2021-22205
        testPocBothWays(
            "GitLab RCE CVE-2021-22205",
            gobyBase + "GitLab_RCE_CVE-2021-22205.json",
            BASE_URL + "/vuln/goby/gitlab-rce",
            BASE_URL + "/safe/goby/gitlab-rce",
            "GitLab 远程代码执行",
            "正向：返回200+uid=0，负向：返回401"
        );
        
        // Goby POC 131: GoCD 任意文件读取
        testPocBothWays(
            "GoCD File Read CVE-2021-43287",
            gobyBase + "GoCD_Arbitrary_file_reading_CVE_2021_43287.json",
            BASE_URL + "/vuln/goby/gocd-fileread",
            BASE_URL + "/safe/goby/gocd-fileread",
            "GoCD 任意文件读取",
            "正向：返回200+root:，负向：返回404"
        );
        
        // Goby POC 132: U8 OA
        testPocBothWays(
            "U8 OA Vulnerability",
            gobyBase + "U8_OA.json",
            BASE_URL + "/vuln/goby/u8-oa",
            BASE_URL + "/safe/goby/u8-oa",
            "用友 U8 OA 漏洞",
            "正向：返回200+uid=0，负向：返回401"
        );
        
        // Goby POC 133: VMware vCenter 文件读取
        testPocBothWays(
            "VMware vCenter File Read",
            gobyBase + "VMware_vCenter_v7.0.2_Arbitrary_File_Read.json",
            BASE_URL + "/vuln/goby/vcenter-fileread",
            BASE_URL + "/safe/goby/vcenter-fileread",
            "VMware vCenter 文件读取",
            "正向：返回200+root:，负向：返回404"
        );
        
        // Goby POC 134: Weblogic SSRF
        testPocBothWays(
            "Weblogic SSRF",
            gobyBase + "Weblogic_SSRF.json",
            BASE_URL + "/vuln/goby/weblogic-ssrf",
            BASE_URL + "/safe/goby/weblogic-ssrf",
            "Weblogic SSRF",
            "正向：返回200，负向：返回404"
        );
        
        // Goby POC 135: Weaver OA 8 SQL注入
        testPocBothWays(
            "Weaver OA 8 SQL Injection",
            gobyBase + "Weaver_OA_8_SQL_injection.json",
            BASE_URL + "/vuln/goby/weaver-oa8",
            BASE_URL + "/safe/goby/weaver-oa8",
            "泛微 OA 8 SQL注入",
            "正向：返回200+MD5，负向：返回error"
        );
        
        // Goby POC 136: XXL-JOB 默认密码
        testPocBothWays(
            "XXL-JOB Default Password",
            gobyBase + "XXL_JOB_Default_password.json",
            BASE_URL + "/vuln/goby/xxljob-default",
            BASE_URL + "/safe/goby/xxljob-default",
            "XXL-JOB 默认密码",
            "正向：返回200+LOGIN_IDENTITY，负向：返回账号或密码错误"
        );
        
        // Goby POC 137: Spring4Shell RCE
        testPocBothWays(
            "Spring4Shell RCE CVE-2022-22965",
            gobyBase + "Spring_Framework_Data_Binding_Rules_Spring4Shell_RCE_CVE_2022_22965.json",
            BASE_URL + "/vuln/goby/spring4shell",
            BASE_URL + "/safe/goby/spring4shell",
            "Spring Framework RCE",
            "正向：返回200+uid=0，负向：返回404"
        );
        
        // Goby POC 138: Wayos AC 默认密码
        testPocBothWays(
            "Wayos AC Default Password",
            gobyBase + "Wayos_AC_Centralized_management_system_Default_Password_CNVD_2021_00876.json",
            BASE_URL + "/vuln/goby/wayos-ac",
            BASE_URL + "/safe/goby/wayos-ac",
            "Wayos AC 默认密码",
            "正向：返回200+success，负向：返回401"
        );
        
        // Goby POC 139: Security Devices 硬编码密码
        testPocBothWays(
            "Security Devices Hardcoded Password",
            gobyBase + "Security_Devices_Hardcoded_Password.json",
            BASE_URL + "/vuln/goby/security-devices",
            BASE_URL + "/safe/goby/security-devices",
            "安全设备硬编码密码",
            "正向：返回200+Admin Console，负向：返回401"
        );
        
        // Goby POC 140: 中新金盾 默认密码
        testPocBothWays(
            "ZhongXinJingDun Default Password",
            gobyBase + "ZhongXinJingDun_Default_administrator_password.json",
            BASE_URL + "/vuln/goby/zhongxinjingdun",
            BASE_URL + "/safe/goby/zhongxinjingdun",
            "中新金盾默认密码",
            "正向：返回200+success，负向：返回401"
        );
        
        // Goby POC 141: ZZZCMS RCE
        testPocBothWays(
            "ZZZCMS RCE",
            gobyBase + "ZZZCMS_parserSearch_RCE.go.json",
            BASE_URL + "/vuln/goby/zzzcms",
            BASE_URL + "/safe/goby/zzzcms",
            "ZZZCMS 远程代码执行",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 142: 亿邮邮件系统 RCE
        testPocBothWays(
            "Eyou Mail System RCE",
            gobyBase + "Eyou_Mail_System_RCE_CNVD_2021_26422.json",
            BASE_URL + "/vuln/goby/eyou-mail",
            BASE_URL + "/safe/goby/eyou-mail",
            "亿邮邮件系统 RCE",
            "正向：返回200+root，负向：返回403"
        );
        
        // Goby POC 143: Apache Airflow 未授权
        testPocBothWays(
            "Apache Airflow Unauthorized",
            gobyBase + "Apache_Airflow_Unauthorized.json",
            BASE_URL + "/vuln/goby/airflow",
            BASE_URL + "/safe/goby/airflow",
            "Apache Airflow 未授权访问",
            "正向：返回200+Airflow DAGs，负向：返回401"
        );
        
        // Goby POC 144: Apache CouchDB 未授权
        testPocBothWays(
            "Apache CouchDB Unauth",
            gobyBase + "Apache_CouchDB_Unauth.json",
            BASE_URL + "/vuln/goby/couchdb",
            BASE_URL + "/safe/goby/couchdb",
            "Apache CouchDB 未授权访问",
            "正向：返回200+httpd_design_handlers，负向：返回401"
        );
        
        // Goby POC 145: Nacos 默认密码
        testPocBothWays(
            "Alibaba Nacos Default Password",
            gobyBase + "Alibaba_Nacos_Default_password.json",
            BASE_URL + "/vuln/goby/nacos-default",
            BASE_URL + "/safe/goby/nacos-default",
            "Nacos 默认密码",
            "正向：返回200+accessToken，负向：返回403"
        );
        
        // Goby POC 146: 蓝凌 OA 任意文件读取
        testPocBothWays(
            "Landray OA File Read",
            gobyBase + "landray_OA_Arbitrary_file_read.json",
            BASE_URL + "/vuln/goby/landray-oa",
            BASE_URL + "/safe/goby/landray-oa",
            "蓝凌 OA 任意文件读取",
            "正向：返回200+root，负向：返回403"
        );
        
        // Goby POC 147: 通达 OA 未授权
        testPocBothWays(
            "TongDa OA Unauth",
            gobyBase + "tongdaoa_unauth.json",
            BASE_URL + "/vuln/goby/tongda-oa",
            BASE_URL + "/safe/goby/tongda-oa",
            "通达 OA 未授权访问",
            "正向：返回200不含relogin，负向：返回relogin"
        );
        
        // Goby POC 148: 用友 NC RCE
        testPocBothWays(
            "Yonyou NC RCE",
            gobyBase + "Yonyou_UFIDA_NC_bsh.servlet.BshServlet_rce.json",
            BASE_URL + "/vuln/goby/yonyou-nc",
            BASE_URL + "/safe/goby/yonyou-nc",
            "用友 NC BeanShell RCE",
            "正向：返回200+BeanShell，负向：返回404"
        );
        
        // Goby POC 149: Apache Druid 文件读取
        testPocBothWays(
            "Apache Druid File Read",
            gobyBase + "Apache_Druid_Arbitrary_File_Read_CVE_2021_36749.json",
            BASE_URL + "/vuln/goby/druid-fileread",
            BASE_URL + "/safe/goby/druid-fileread",
            "Apache Druid 任意文件读取",
            "正向：返回200+root，负向：返回404"
        );
        
        // Goby POC 150: Apache Flink 文件读取
        testPocBothWays(
            "Apache Flink File Read",
            gobyBase + "Apache_Flink_CVE_2020_17519.json",
            BASE_URL + "/vuln/goby/flink-fileread",
            BASE_URL + "/safe/goby/flink-fileread",
            "Apache Flink 任意文件读取",
            "正向：返回200+root，负向：返回404"
        );
        
        // Goby POC 151: ActiveMQ 默认密码
        testPocBothWays(
            "Apache ActiveMQ Default Password",
            gobyBase + "Apache_ActiveMQ_default_admin_account.json",
            BASE_URL + "/vuln/goby/activemq-default",
            BASE_URL + "/safe/goby/activemq-default",
            "ActiveMQ 默认密码",
            "正向：返回200+ActiveMQ，负向：返回401"
        );
        
        // Goby POC 152: H3C IMC RCE
        testPocBothWays(
            "H3C IMC RCE",
            gobyBase + "H3C_IMC_RCE.json",
            BASE_URL + "/vuln/goby/h3c-imc",
            BASE_URL + "/safe/goby/h3c-imc",
            "H3C IMC 远程代码执行",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 153: 海康威视 RCE
        testPocBothWays(
            "Hikvision RCE",
            gobyBase + "Hikvision_RCE_CVE_2021_36260.json",
            BASE_URL + "/vuln/goby/hikvision-rce",
            BASE_URL + "/safe/goby/hikvision-rce",
            "海康威视 RCE",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 154: Jellyfin 文件读取
        testPocBothWays(
            "Jellyfin File Read",
            gobyBase + "Jellyfin_prior_to_10.7.0_Unauthenticated_Arbitrary_File_Read_CVE_2021_21402.json",
            BASE_URL + "/vuln/goby/jellyfin-fileread",
            BASE_URL + "/safe/goby/jellyfin-fileread",
            "Jellyfin 任意文件读取",
            "正向：返回200+root，负向：返回404"
        );
        
        // Goby POC 155: Jetty WEB-INF 文件读取
        testPocBothWays(
            "Jetty WEB-INF File Read",
            gobyBase + "Jetty_WEB_INF_FileRead_CVE_2021_34429.json",
            BASE_URL + "/vuln/goby/jetty-fileread",
            BASE_URL + "/safe/goby/jetty-fileread",
            "Jetty WEB-INF 文件读取",
            "正向：返回200+xml，负向：返回404"
        );
        
        // Goby POC 156: 金和 OA 默认密码
        testPocBothWays(
            "Jinhe OA Default Password",
            gobyBase + "JinHe_OA_C6_Default_password.json",
            BASE_URL + "/vuln/goby/jinhe-oa-c6",
            BASE_URL + "/safe/goby/jinhe-oa-c6",
            "金和 OA 默认密码",
            "正向：返回200+success，负向：返回401"
        );
        
        // Goby POC 157: 金山 V8 默认密码
        testPocBothWays(
            "Kingsoft V8 Default Password",
            gobyBase + "Kingsoft_V8_Default_weak_password.json",
            BASE_URL + "/vuln/goby/kingsoft-v8",
            BASE_URL + "/safe/goby/kingsoft-v8",
            "金山 V8 默认密码",
            "正向：返回200+Kingsoft V8，负向：返回401"
        );
        
        // Goby POC 158: Kyan 密码泄露
        testPocBothWays(
            "Kyan Password Leak",
            gobyBase + "Kyan_Account_password_leak.json",
            BASE_URL + "/vuln/goby/kyan-leak",
            BASE_URL + "/safe/goby/kyan-leak",
            "Kyan 密码泄露",
            "正向：返回200+password，负向：返回403"
        );
        
        // Goby POC 159: VMware Workspace ONE RCE
        testPocBothWays(
            "VMware Workspace ONE RCE",
            gobyBase + "VMware_Workspace_ONE_Access_RCE_CVE_2022_22954.json",
            BASE_URL + "/vuln/goby/vmware-workspace",
            BASE_URL + "/safe/goby/vmware-workspace",
            "VMware Workspace ONE RCE",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 160: WebSVN RCE
        testPocBothWays(
            "WebSVN RCE",
            gobyBase + "WebSVN_before_2.6.1_Injection_RCE_CVE_2021_32305.json",
            BASE_URL + "/vuln/goby/websvn-rce",
            BASE_URL + "/safe/goby/websvn-rce",
            "WebSVN RCE",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 161: Zabbix SAML CVE-2022-23131
        testPocBothWays(
            "Zabbix SAML CVE-2022-23131",
            gobyBase + "zabbix_saml_cve_2022_23131.json",
            BASE_URL + "/vuln/goby/zabbix-saml",
            BASE_URL + "/safe/goby/zabbix-saml",
            "Zabbix SAML 认证绕过",
            "正向：返回302，负向：返回403"
        );
        
        // Goby POC 162: Alibaba Canal 默认密码
        testPocBothWays(
            "Alibaba Canal Default Password",
            gobyBase + "alibaba_canal_default_password.json",
            BASE_URL + "/vuln/goby/alibaba-canal",
            BASE_URL + "/safe/goby/alibaba-canal",
            "Alibaba Canal 默认密码",
            "正向：返回200+success，负向：返回401"
        );
        
        // Goby POC 163: 深信服行为感知 RCE
        testPocBothWays(
            "Sangfor Behavior RCE",
            gobyBase + "sangfor_Behavior_perception_system_c.php_RCE.json",
            BASE_URL + "/vuln/goby/sangfor-rce",
            BASE_URL + "/safe/goby/sangfor-rce",
            "深信服行为感知系统 RCE",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 164: 畅捷 CRM SQL注入
        testPocBothWays(
            "Chanjet CRM SQL Injection",
            gobyBase + "chanjet_CRM_get_usedspace.php_sql_injection.json",
            BASE_URL + "/vuln/goby/chanjet-crm",
            BASE_URL + "/safe/goby/chanjet-crm",
            "畅捷 CRM SQL注入",
            "正向：返回200+MD5，负向：返回error"
        );
        
        // Goby POC 165: Fastmeeting 文件读取
        testPocBothWays(
            "Fastmeeting File Read",
            gobyBase + "Fastmeeting_Arbitrary_File_Read.json",
            BASE_URL + "/vuln/goby/fastmeeting",
            BASE_URL + "/safe/goby/fastmeeting",
            "快会议任意文件读取",
            "正向：返回200+root，负向：返回404"
        );
        
        // Goby POC 166: H5S 视频平台信息泄露
        testPocBothWays(
            "H5S Video Platform Info Leak",
            gobyBase + "H5S_video_platform_GetUserInfo_Account_password_leakage.json",
            BASE_URL + "/vuln/goby/h5s-video",
            BASE_URL + "/safe/goby/h5s-video",
            "H5S 视频平台信息泄露",
            "正向：返回200+strUser，负向：返回403"
        );
        
        // Goby POC 167: IceWarp RCE
        testPocBothWays(
            "IceWarp RCE",
            gobyBase + "IceWarp_WebClient_basic_RCE.json",
            BASE_URL + "/vuln/goby/icewarp-rce",
            BASE_URL + "/safe/goby/icewarp-rce",
            "IceWarp WebClient RCE",
            "正向：返回200+uid=0，负向：返回403"
        );
        
        // Goby POC 168: Konga 默认 JWT KEY
        testPocBothWays(
            "Konga Default JWT KEY",
            gobyBase + "Konga_Default_JWT_KEY.json",
            BASE_URL + "/vuln/goby/konga-jwt",
            BASE_URL + "/safe/goby/konga-jwt",
            "Konga 默认 JWT KEY",
            "正向：返回200+token，负向：返回401"
        );
        
        // Goby POC 169: 启来 OA SQL注入
        testPocBothWays(
            "Qilai OA SQL Injection",
            gobyBase + "qilaiOA_messageurl.aspx_SQLinjection.json",
            BASE_URL + "/vuln/goby/qilai-oa",
            BASE_URL + "/safe/goby/qilai-oa",
            "启来 OA SQL注入",
            "正向：返回200+MD5，负向：返回error"
        );
        
        // Goby POC 170: 华天动力 OA SQL注入
        testPocBothWays(
            "Huatian OA SQL Injection",
            gobyBase + "huatiandongliOA_8000workFlowService_SQLinjection.json",
            BASE_URL + "/vuln/goby/huatian-oa",
            BASE_URL + "/safe/goby/huatian-oa",
            "华天动力 OA SQL注入",
            "正向：返回200+MD5，负向：返回error"
        );
        
        // Goby POC 171: 360 天擎 SQL注入
        testPocBothWays(
            "360 TianQing SQL Injection",
            gobyBase + "360_TianQing_ccid_SQL_injectable.json",
            BASE_URL + "/vuln/goby/360tianqing-ccid",
            BASE_URL + "/safe/goby/360tianqing-ccid",
            "360 天擎 SQL注入",
            "正向：返回200+MD5，负向：返回error"
        );
        
        // Goby POC 184: ADSelfService Plus RCE
        testPocBothWays(
            "ADSelfService Plus RCE",
            gobyBase + "ADSelfService_Plus_RCE_CVE-2021-40539.json",
            BASE_URL + "/vuln/goby/adselfservice",
            BASE_URL + "/safe/goby/adselfservice",
            "ADSelfService Plus RCE",
            "正向：返回200+var d = new Date，负向：返回404"
        );

        // Goby POC 185: AVCON6 文件下载
        testPocBothWays(
            "AVCON6 File Download",
            gobyBase + "AVCON6_org_execl_download.action_file_down.json",
            BASE_URL + "/vuln/goby/avcon6",
            BASE_URL + "/safe/goby/avcon6",
            "AVCON6 任意文件下载",
            "正向：返回200+root，负向：返回404"
        );

        // Goby POC 186: Active UC RCE
        testPocBothWays(
            "Active UC RCE",
            gobyBase + "Active_UC_index.action_RCE.json",
            BASE_URL + "/vuln/goby/active-uc",
            BASE_URL + "/safe/goby/active-uc",
            "Active UC RCE",
            "正向：返回200+Windows IP，负向：返回404"
        );

        // Goby POC 187: Adslr 信息泄露
        testPocBothWays(
            "Adslr Info Leak",
            gobyBase + "Adslr_Enterprise_online_behavior_management_system_Information_leakage.json",
            BASE_URL + "/vuln/goby/adslr",
            BASE_URL + "/safe/goby/adslr",
            "Adslr 信息泄露",
            "正向：返回200+WPA-PSK，负向：返回404"
        );

        // Goby POC 188: Nacos 未授权添加用户
        testPocBothWays(
            "Nacos Unauth Add User",
            gobyBase + "Alibaba_Nacos_Add_user_not_authorized.json",
            BASE_URL + "/vuln/goby/nacos-unauth-user",
            BASE_URL + "/safe/goby/nacos-unauth-user",
            "Nacos 未授权添加用户",
            "正向：返回500/200，负向：返回403"
        );

        // Goby POC 189: APISIX Dashboard 未授权
        testPocBothWays(
            "APISIX Dashboard Unauth",
            gobyBase + "Apache_APISIX_Dashboard_API_Unauthorized_Access_CVE-2021-45232.json",
            BASE_URL + "/vuln/goby/apisix-dashboard",
            BASE_URL + "/safe/goby/apisix-dashboard",
            "APISIX Dashboard 未授权访问",
            "正向：返回200+Consumers，负向：返回401"
        );

        // Goby POC 190: CouchDB 权限提升
        testPocBothWays(
            "CouchDB PrivEsc",
            gobyBase + "Apache_CouchDB_Remote_Privilege_Escalation_CVE-2017-12635.json",
            BASE_URL + "/vuln/goby/couchdb-privesc",
            BASE_URL + "/safe/goby/couchdb-privesc",
            "CouchDB 权限提升",
            "正向：返回201+org.couchdb.user，负向：返回401"
        );

        // Goby POC 191: Apache HTTP SSRF
        testPocBothWays(
            "Apache HTTP SSRF",
            gobyBase + "Apache_HTTP_Server_2.4.48_mod_proxy_SSRF_CVE_2021_40438.json",
            BASE_URL + "/vuln/goby/apache-ssrf",
            BASE_URL + "/safe/goby/apache-ssrf",
            "Apache HTTP Server SSRF",
            "正向：返回200+Example Domain，负向：返回404"
        );

        // Goby POC 192: Apache HTTP Path Traversal
        testPocBothWays(
            "Apache HTTP Path Traversal",
            gobyBase + "Apache_HTTP_Server_2.4.49_2.4.50_Path_Traversal_CVE_2021_42013.json",
            BASE_URL + "/vuln/goby/apache-traversal-new",
            BASE_URL + "/safe/goby/apache-traversal-new",
            "Apache HTTP Server 目录遍历",
            "正向：返回200+root，负向：返回403"
        );

        // Goby POC 193: Kylin Default Password
        testPocBothWays(
            "Kylin Default Password",
            gobyBase + "Apache_Kylin_Console_Default_password.json",
            BASE_URL + "/vuln/goby/kylin-default",
            BASE_URL + "/safe/goby/kylin-default",
            "Apache Kylin 默认密码",
            "正向：返回200，负向：返回401"
        );

        // Goby POC 194: Kylin Unauth Config
        testPocBothWays(
            "Kylin Unauth Config",
            gobyBase + "Apache_Kylin_Unauthorized_configuration_disclosure.json",
            BASE_URL + "/vuln/goby/kylin-unauth",
            BASE_URL + "/safe/goby/kylin-unauth",
            "Apache Kylin 未授权配置泄露",
            "正向：返回200+config，负向：返回401"
        );

        // Goby POC 195: ShenYu Unauth Access
        testPocBothWays(
            "ShenYu Unauth Access",
            gobyBase + "Apache_ShenYu_Admin_Unauth_Access_CVE_2022_23944.json",
            BASE_URL + "/vuln/goby/shenyu-unauth-new",
            BASE_URL + "/safe/goby/shenyu-unauth-new",
            "Apache ShenYu 未授权访问",
            "正向：返回200+query success，负向：返回401"
        );

        // Goby POC 196: Struts2 S2-053
        testPocBothWays(
            "Struts2 S2-053",
            gobyBase + "Apache_Struts2_S2_053_RCE_CVE_2017_12611.json",
            BASE_URL + "/vuln/goby/struts2-s2-053",
            BASE_URL + "/safe/goby/struts2-s2-053",
            "Struts2 S2-053 RCE",
            "正向：返回200+root，负向：返回404"
        );

        // Goby POC 197: AspCMS SQL Injection
        testPocBothWays(
            "AspCMS SQL Injection",
            gobyBase + "AspCMS_commentList.asp_SQLinjection_vulnerability.json",
            BASE_URL + "/vuln/goby/aspcms-sqli",
            BASE_URL + "/safe/goby/aspcms-sqli",
            "AspCMS SQL注入",
            "正向：返回200+admin，负向：返回200(Normal)"
        );

        // Goby POC 198: AspCMS Backend Leak
        testPocBothWays(
            "AspCMS Backend Leak",
            gobyBase + "Aspcms_Backend_Leak.json",
            BASE_URL + "/vuln/goby/aspcms-leak",
            BASE_URL + "/safe/goby/aspcms-leak",
            "AspCMS 后台地址泄露",
            "正向：返回200+alert，负向：返回404"
        );

        // Goby POC 199: BSPHP Unauth
        testPocBothWays(
            "BSPHP Unauth",
            gobyBase + "BSPHP_index.php_unauthorized_access_information.json",
            BASE_URL + "/vuln/goby/bsphp-unauth",
            BASE_URL + "/safe/goby/bsphp-unauth",
            "BSPHP 未授权访问",
            "正向：返回200+user，负向：返回401"
        );

        // Goby POC 200: BigAnt Path Traversal
        testPocBothWays(
            "BigAnt Path Traversal",
            gobyBase + "BigAnt_Server_v5.6.06_Path_Traversal_CVE_2022_23347.json",
            BASE_URL + "/vuln/goby/bigant-traversal",
            BASE_URL + "/safe/goby/bigant-traversal",
            "BigAnt 目录遍历",
            "正向：返回200+fonts，负向：返回404"
        );

        // Goby POC 201: Portainer Unauth
        testPocBothWays(
            "Portainer Unauth",
            gobyBase + "CVE_2018_19367_.json",
            BASE_URL + "/vuln/goby/portainer-unauth",
            BASE_URL + "/safe/goby/portainer-unauth",
            "Portainer 未授权访问",
            "正向：返回404，负向：返回204"
        );

        // Goby POC 202: Cacti Weathermap File Write
        testPocBothWays(
            "Cacti Weathermap File Write",
            gobyBase + "Cacti_Weathermap_File_Write.json",
            BASE_URL + "/vuln/goby/cacti-filewrite",
            BASE_URL + "/safe/goby/cacti-filewrite",
            "Cacti Weathermap 文件写入",
            "正向：返回200+OK，负向：返回404"
        );

        // Goby POC 203: Casdoor SQL Injection
        testPocBothWays(
            "Casdoor SQL Injection",
            gobyBase + "Casdoor_1.13.0_SQL_InjectionCVE_2022_24124.json",
            BASE_URL + "/vuln/goby/casdoor-sqli",
            BASE_URL + "/safe/goby/casdoor-sqli",
            "Casdoor SQL注入",
            "正向：返回200+XPATH syntax，负向：返回200 OK"
        );

        // Goby POC 204: Cerebro SQL Injection
        testPocBothWays(
            "Cerebro SQL Injection",
            gobyBase + "Cerebro_request_SSRF.json",
            BASE_URL + "/vuln/goby/cerebro-sqli",
            BASE_URL + "/safe/goby/cerebro-sqli",
            "Cerebro SQL注入",
            "正向：返回500+SELECT，负向：返回200 OK"
        );

        // Goby POC 205: Chanjet CRM SQL Injection
        testPocBothWays(
            "Chanjet CRM SQL Injection",
            gobyBase + "Chanjet_CRM_get_usedspace.php_sql_injection_CNVD_2021_12845.json",
            BASE_URL + "/vuln/goby/chanjet-sqli",
            BASE_URL + "/safe/goby/chanjet-sqli",
            "畅捷 CRM SQL注入",
            "正向：返回200+!^^，负向：返回200 OK"
        );

        // Goby POC 206: China Mobile Yu Routing Leak
        testPocBothWays(
            "China Mobile Yu Routing Leak",
            gobyBase + "China_Mobile_Yu_Routing_ExportSettings.sh_Info_Leak_CNVD_2020_67110.json",
            BASE_URL + "/vuln/goby/chinamobile-yu",
            BASE_URL + "/safe/goby/chinamobile-yu",
            "移动和路由信息泄露",
            "正向：返回200+Password，负向：返回403"
        );

        // Goby POC 207: China Mobile Yu Routing Login Bypass
        testPocBothWays(
            "China Mobile Yu Routing Login Bypass",
            gobyBase + "China_Mobile_Yu_Routing_Login_Bypass.json",
            BASE_URL + "/vuln/goby/chinamobile-login-bypass",
            BASE_URL + "/safe/goby/chinamobile-login-bypass",
            "移动和路由登陆绕过",
            "正向：返回200+admin/index.asp，负向：返回200(Login Failed)"
        );

        // Goby POC 208: Citrix Unauthorized CVE-2020-8193
        testPocBothWays(
            "Citrix Unauthorized",
            gobyBase + "Citrix_Unauthorized_CVE_2020_8193.json",
            BASE_URL + "/vuln/goby/citrix-unauth",
            BASE_URL + "/safe/goby/citrix-unauth",
            "Citrix 未授权访问",
            "正向：返回406+SESSID，负向：返回200 OK"
        );

        // Goby POC 209: ClickHouse SQLi
        testPocBothWays(
            "ClickHouse SQLi",
            gobyBase + "ClickHouse_SQLI.json",
            BASE_URL + "/vuln/goby/clickhouse-sqli",
            BASE_URL + "/safe/goby/clickhouse-sqli",
            "ClickHouse SQL注入",
            "正向：返回200+default/system，负向：返回200 OK"
        );

        // Goby POC 210: ClusterEngine RCE
        testPocBothWays(
            "ClusterEngine RCE",
            gobyBase + "ClusterEngineV4.0_RCE_.json",
            BASE_URL + "/vuln/goby/clusterengine-rce",
            BASE_URL + "/safe/goby/clusterengine-rce",
            "ClusterEngine RCE",
            "正向：返回200+root:x:，负向：返回200(Login Failed)"
        );

        // Goby POC 211: CmsEasy SQLi
        testPocBothWays(
            "CmsEasy SQLi",
            gobyBase + "CmsEasy_crossall_act.php_SQL_injection_vulnerability.json",
            BASE_URL + "/vuln/goby/cmseasy-sqli",
            BASE_URL + "/safe/goby/cmseasy-sqli",
            "CmsEasy SQL注入",
            "正向：返回200+123，负向：返回200 OK"
        );

        // Goby POC 212: Coldfusion LFI
        testPocBothWays(
            "Coldfusion LFI",
            gobyBase + "Coldfusion_LFI_CVE_2010_2861.json",
            BASE_URL + "/vuln/goby/coldfusion-lfi",
            BASE_URL + "/safe/goby/coldfusion-lfi",
            "Coldfusion LFI",
            "正向：返回200+rdspassword，负向：返回404"
        );

        // Goby POC 213: Confluence RCE
        testPocBothWays(
            "Confluence RCE",
            gobyBase + "Confluence_RCE_CVE_2021_26084.json",
            BASE_URL + "/vuln/goby/confluence-rce",
            BASE_URL + "/safe/goby/confluence-rce",
            "Confluence RCE",
            "正向：返回200+queryString，负向：返回200 OK"
        );

        // Goby POC 214: Consul Rexec RCE
        testPocBothWays(
            "Consul Rexec RCE",
            gobyBase + "Consul_Rexec_RCE.json",
            BASE_URL + "/vuln/goby/consul-rce",
            BASE_URL + "/safe/goby/consul-rce",
            "Consul Rexec RCE",
            "正向：返回200，负向：返回403"
        );

        // Goby POC 215: Coremail Config Disclosure
        testPocBothWays(
            "Coremail Config Disclosure",
            gobyBase + "Coremail_Config_Disclosure.json",
            BASE_URL + "/vuln/goby/coremail-config",
            BASE_URL + "/safe/goby/coremail-config",
            "Coremail 配置泄露",
            "正向：返回200+config，负向：返回401"
        );

        // Goby POC 216: CouchCMS Info Leak
        testPocBothWays(
            "CouchCMS Info Leak",
            gobyBase + "CouchCMS_Infoleak_CVE-2018-7662.json",
            BASE_URL + "/vuln/goby/couchcms-infoleak",
            BASE_URL + "/safe/goby/couchcms-infoleak",
            "CouchCMS 信息泄露",
            "正向：返回200+Fatal error，负向：返回404"
        );

        // Goby POC 217: CouchDB Unauth
        testPocBothWays(
            "CouchDB Unauth",
            gobyBase + "Couchdb_Unauth.json",
            BASE_URL + "/vuln/goby/couchdb-unauth",
            BASE_URL + "/safe/goby/couchdb-unauth",
            "CouchDB 未授权访问",
            "正向：返回200+httpd_design_handlers，负向：返回401"
        );

        // Goby POC 218: CraftCMS SEOmatic RCE
        testPocBothWays(
            "CraftCMS SEOmatic RCE",
            gobyBase + "CraftCMS_SEOmatic_Server-Side_Template_Injection_CVE-2020-9597.json",
            BASE_URL + "/vuln/goby/craftcms-seomatic",
            BASE_URL + "/safe/goby/craftcms-seomatic",
            "CraftCMS SEOmatic RCE",
            "正向：返回200+25，负向：返回200 OK"
        );

        // Goby POC 219: D-Link AC Default Password
        testPocBothWays(
            "D-Link AC Default Password",
            gobyBase + "D-Link_AC_management_system_Default_Password.json",
            BASE_URL + "/vuln/goby/dlink-ac-default",
            BASE_URL + "/safe/goby/dlink-ac-default",
            "D-Link AC 默认密码",
            "正向：返回200+D-Link，负向：返回200(Login)"
        );

        // Goby POC 220: D-Link DCS Info Leak
        testPocBothWays(
            "D-Link DCS Info Leak",
            gobyBase + "D-Link_DCS_2530L_Administrator_password_disclosure_CVE_2020_25078.json",
            BASE_URL + "/vuln/goby/dlink-dcs-infoleak",
            BASE_URL + "/safe/goby/dlink-dcs-infoleak",
            "D-Link DCS 信息泄露",
            "正向：返回200+name=admin，负向：返回403"
        );

        // Goby POC 221: D-Link DIR-850L Info Leak
        testPocBothWays(
            "D-Link DIR-850L Info Leak",
            gobyBase + "D-Link_DIR-850L_Info_Leak.json",
            BASE_URL + "/vuln/goby/dlink-dir850l-infoleak",
            BASE_URL + "/safe/goby/dlink-dir850l-infoleak",
            "D-Link DIR-850L 信息泄露",
            "正向：返回200+uid，负向：返回200(Login)"
        );

        // Goby POC 222: D-Link CVE-2019-17506 Info Leak
        testPocBothWays(
            "D-Link CVE-2019-17506 Info Leak",
            gobyBase + "D-Link_Info_Leak_CVE-2019-17506.json",
            BASE_URL + "/vuln/goby/dlink-cve-2019-17506",
            BASE_URL + "/safe/goby/dlink-cve-2019-17506",
            "D-Link CVE-2019-17506 信息泄露",
            "正向：返回200+admin，负向：返回200(Login)"
        );

        // Goby POC 223: D-Link ShareCenter RCE
        testPocBothWays(
            "D-Link ShareCenter RCE",
            gobyBase + "D-Link_ShareCenter_DNS_320_RCE.json",
            BASE_URL + "/vuln/goby/dlink-sharecenter-rce",
            BASE_URL + "/safe/goby/dlink-sharecenter-rce",
            "D-Link ShareCenter RCE",
            "正向：返回200+var/www，负向：返回404"
        );

        // Goby POC 224: D-Link AC Weak Password
        testPocBothWays(
            "D-Link AC Weak Password",
            gobyBase + "D_Link_AC_Centralized_management_system__Default_weak_password.json",
            BASE_URL + "/vuln/goby/dlink-ac-weakpwd",
            BASE_URL + "/safe/goby/dlink-ac-weakpwd",
            "D-Link AC 弱口令",
            "正向：返回200+Success，负向：返回200+flag=0"
        );

        // Goby POC 225: D-Link DC Info Leak
        testPocBothWays(
            "D-Link DC Info Leak",
            gobyBase + "D_Link_DC_Disclosure_of_account_password_information.json",
            BASE_URL + "/vuln/goby/dlink-dc-infoleak",
            BASE_URL + "/safe/goby/dlink-dc-infoleak",
            "D-Link DC 信息泄露",
            "正向：返回200+name=admin，负向：返回404"
        );

        // Goby POC 226: D-Link DIR-868L Account Leak
        testPocBothWays(
            "D-Link DIR-868L Account Leak",
            gobyBase + "D_Link_DIR_868L_getcfg.php_Account_password_leakage.json",
            BASE_URL + "/vuln/goby/dlink-dir868l-infoleak",
            BASE_URL + "/safe/goby/dlink-dir868l-infoleak",
            "D-Link DIR-868L 账号泄露",
            "正向：返回200+password，负向：返回200(Login)"
        );

        // Goby POC 228: Datang AC Default Password
        testPocBothWays(
            "Datang AC Default Password",
            gobyBase + "Datang_AC_Default_Password.json",
            BASE_URL + "/vuln/goby/datang-ac-default",
            BASE_URL + "/safe/goby/datang-ac-default",
            "大唐电信AC 默认密码",
            "正向：返回200+window.open，负向：返回200(Login)"
        );

        // Goby POC 229: DedeCMS Carbuyaction File Include
        testPocBothWays(
            "DedeCMS Carbuyaction File Include",
            gobyBase + "DedeCMS_Carbuyaction_FileInclude.json",
            BASE_URL + "/vuln/goby/dedecms-carbuyaction",
            BASE_URL + "/safe/goby/dedecms-carbuyaction",
            "DedeCMS Carbuyaction 文件包含",
            "正向：返回200+Cod::respond，负向：返回200(Normal)"
        );

        // Goby POC 230: DedeCMS Info Leak CVE-2018-6910
        testPocBothWays(
            "DedeCMS Info Leak CVE-2018-6910",
            gobyBase + "DedeCMS_InfoLeak_CVE-2018-6910.json",
            BASE_URL + "/vuln/goby/dedecms-infoleak-2018",
            BASE_URL + "/safe/goby/dedecms-infoleak-2018",
            "DedeCMS 信息泄露 CVE-2018-6910",
            "正向：返回200+Fatal error，负向：返回200(Normal)"
        );

        // Goby POC 232: Discuz!ML 3.x RCE
        testPocBothWays(
            "Discuz!ML 3.x RCE",
            gobyBase + "Discuz!ML_3.x_RCE_CNVD-2019-22239.json",
            BASE_URL + "/vuln/goby/discuz-ml-rce",
            BASE_URL + "/safe/goby/discuz-ml-rce",
            "Discuz!ML RCE",
            "正向：返回200+PHP Version，负向：返回200(Normal)"
        );

        // Goby POC 234: Discuz RCE WOOYUN-2010-080723
        testPocBothWays(
            "Discuz RCE WOOYUN-2010-080723",
            gobyBase + "Discuz_RCE_WOOYUN_2010_080723.json",
            BASE_URL + "/vuln/goby/discuz-rce-wooyun",
            BASE_URL + "/safe/goby/discuz-rce-wooyun",
            "Discuz RCE WooYun",
            "正向：返回200+PHP Version，负向：返回200(Normal)"
        );

        // Goby POC 235: Discuz Wechat Plugins Unauth
        testPocBothWays(
            "Discuz Wechat Plugins Unauth",
            gobyBase + "Discuz_Wechat_Plugins_Unauth.json",
            BASE_URL + "/vuln/goby/discuz-wechat-unauth",
            BASE_URL + "/safe/goby/discuz-wechat-unauth",
            "Discuz 微信插件未授权",
            "正向：返回302+Set-Cookie，负向：返回404"
        );

        // Goby POC 236: Discuz v72 SQLi
        testPocBothWays(
            "Discuz v72 SQLi",
            gobyBase + "Discuz_v72_SQLI.json",
            BASE_URL + "/vuln/goby/discuz-v72-sqli",
            BASE_URL + "/safe/goby/discuz-v72-sqli",
            "Discuz v72 SQL注入",
            "正向：返回200+MySQL Query Error，负向：返回200(Normal)"
        );

        // Goby POC 239: D-Link RCE CVE-2019-16920
        testPocBothWays(
            "D-Link RCE CVE-2019-16920",
            gobyBase + "Dlink_RCE_CVE_2019_16920.json",
            BASE_URL + "/vuln/goby/dlink-rce-cve2019-16920",
            BASE_URL + "/safe/goby/dlink-rce-cve2019-16920",
            "D-Link RCE (DNSLog)",
            "正向：返回200，负向：返回404 (注意：DNSLog验证可能失败)"
        );

        // Goby POC 240: DocCMS SQLi
        testPocBothWays(
            "DocCMS SQLi",
            gobyBase + "DocCMS_keyword_SQL_injection_Vulnerability.json",
            BASE_URL + "/vuln/goby/doccms-sqli",
            BASE_URL + "/safe/goby/doccms-sqli",
            "DocCMS SQL注入",
            "正向：返回200+XPATH syntax，负向：返回200(Normal)"
        );

        // Goby POC 241: Docker Registry API Unauth
        testPocBothWays(
            "Docker Registry API Unauth",
            gobyBase + "Docker_Registry_API_Unauth.json",
            BASE_URL + "/vuln/goby/docker-registry-unauth",
            BASE_URL + "/safe/goby/docker-registry-unauth",
            "Docker Registry 未授权",
            "正向：返回200+header，负向：返回401"
        );

        // Goby POC 243: Dubbo Admin Default Password
        testPocBothWays(
            "Dubbo Admin Default Password",
            gobyBase + "Dubbo_Admin_Default_Password.json",
            BASE_URL + "/vuln/goby/dubbo-admin-default",
            BASE_URL + "/safe/goby/dubbo-admin-default",
            "Dubbo Admin 默认密码",
            "正向：返回200+Dubbo Admin，负向：返回401"
        );

        // Goby POC 244: Eyou Mail RCE
        testPocBothWays(
            "Eyou Mail RCE",
            gobyBase + "Eyou_Mail_System_RCE_CNVD_2021_26422.json",
            BASE_URL + "/vuln/goby/eyou-mail-rce",
            BASE_URL + "/safe/goby/eyou-mail-rce",
            "Eyou Mail RCE",
            "正向：返回200+root，负向：返回200(Normal)"
        );

        // Goby POC 246: F5 BIG-IP RCE
        testPocBothWays(
            "F5 BIG-IP RCE",
            gobyBase + "F5_BIG_IP_RCE_CVE_2021_22986_exp.json",
            BASE_URL + "/vuln/goby/f5-bigip-rce",
            BASE_URL + "/safe/goby/f5-bigip-rce",
            "F5 BIG-IP RCE",
            "正向：返回200+dHN4dHMK，负向：返回401"
        );

        // Goby POC 247: F5 BIG-IP Auth Bypass
        testPocBothWays(
            "F5 BIG-IP Auth Bypass",
            gobyBase + "F5_BIG_IP_iControl_REST_API_auth_bypass_CVE_2022_1388.json",
            BASE_URL + "/vuln/goby/f5-bigip-auth-bypass",
            BASE_URL + "/safe/goby/f5-bigip-auth-bypass",
            "F5 BIG-IP 认证绕过",
            "正向：返回200+Authorization failed，负向：返回401"
        );

        // Goby POC 249: Fastmeeting Arbitrary File Read
        testPocBothWays(
            "Fastmeeting Arbitrary File Read",
            gobyBase + "Fastmeeting_Arbitrary_File_Read.json",
            BASE_URL + "/vuln/goby/fastmeeting-fileread",
            BASE_URL + "/safe/goby/fastmeeting-fileread",
            "Fastmeeting 任意文件读取",
            "正向：返回200+[fonts]，负向：返回200(Normal)"
        );

        // Goby POC 250: FineReport Directory Traversal
        testPocBothWays(
            "FineReport Directory Traversal",
            gobyBase + "FineReport_Directory_traversal.json",
            BASE_URL + "/vuln/goby/finereport-dirtraversal",
            BASE_URL + "/safe/goby/finereport-dirtraversal",
            "FineReport 目录遍历",
            "正向：返回200+etc/passwd，负向：返回200(Normal)"
        );

        // Goby POC 251: FineReport Arbitrary File Read
        testPocBothWays(
            "FineReport Arbitrary File Read",
            gobyBase + "FineReport_v8.0_Arbitrary_file_read_.json",
            BASE_URL + "/vuln/goby/finereport-fileread",
            BASE_URL + "/safe/goby/finereport-fileread",
            "FineReport 任意文件读取",
            "正向：返回200+CDATA，负向：返回200(Normal)"
        );

        // Goby POC 254: FineReport v9 File Overwrite
        testPocBothWays(
            "FineReport v9 File Overwrite",
            gobyBase + "FineReport_v9_Arbitrary_File_Overwrite.json",
            BASE_URL + "/vuln/goby/finereport-v9-overwrite",
            BASE_URL + "/safe/goby/finereport-v9-overwrite",
            "FineReport v9 文件覆盖",
            "正向：返回200+随机字符串(可能失败)，负向：返回404/200"
        );

        // Goby POC 255: Finetree 5MP Auth
        testPocBothWays(
            "Finetree 5MP Auth",
            gobyBase + "Finetree_5MP_Network_Camera_Default_Login_unauthorized_user_add.json",
            BASE_URL + "/vuln/goby/finetree-5mp-auth",
            BASE_URL + "/safe/goby/finetree-5mp-auth",
            "Finetree 5MP 默认口令/未授权",
            "正向：返回302或200+Add User，负向：返回200(Login)或403"
        );
        
        // Goby POC 257: GitLab Graphql Email Leak
        testPocBothWays(
            "GitLab Graphql Email Leak",
            gobyBase + "GitLab Graphql邮箱信息泄露漏洞 CVE-2020-26413.json",
            BASE_URL + "/vuln/goby/gitlab-graphql-email-leak",
            BASE_URL + "/safe/goby/gitlab-graphql-email-leak",
            "GitLab 邮箱泄露",
            "正向：返回200+email，负向：返回200(Empty)"
        );

        // Goby POC 260: GitLab RCE CVE-2021-22205
        testPocBothWays(
            "GitLab RCE CVE-2021-22205",
            gobyBase + "GitLab_RCE_CVE-2021-22205.json",
            BASE_URL + "/vuln/goby/gitlab-rce-cve-2021-22205",
            BASE_URL + "/safe/goby/gitlab-rce-cve-2021-22205",
            "GitLab RCE CVE-2021-22205",
            "正向：返回422+Failed to process image，负向：返回200+ok"
        );

        // Goby POC 261: GitLab SSRF CVE-2021-22214
        testPocBothWays(
            "GitLab SSRF CVE-2021-22214",
            gobyBase + "GitLab_SSRF_CVE_2021_22214.json",
            BASE_URL + "/vuln/goby/gitlab-ssrf-cve-2021-22214",
            BASE_URL + "/safe/goby/gitlab-ssrf-cve-2021-22214",
            "GitLab SSRF CVE-2021-22214",
            "正向：返回200+does not have valid YAML syntax，负向：返回200+Valid"
        );

        // Goby POC 263: Grafana Angularjs XSS
        testPocBothWays(
            "Grafana Angularjs XSS",
            gobyBase + "Grafana_Angularjs_Rendering_XSS_CVE_2021_41174.json",
            BASE_URL + "/vuln/goby/grafana-xss",
            BASE_URL + "/safe/goby/grafana-xss",
            "Grafana Angularjs XSS",
            "正向：返回200+frontend_boot_js_done_time_seconds，负向：返回404"
        );

        // Goby POC 267: H3C IMC RCE
        testPocBothWays(
            "H3C IMC RCE",
            gobyBase + "H3C_IMC_RCE.json",
            BASE_URL + "/vuln/goby/h3c-imc-rce",
            BASE_URL + "/safe/goby/h3c-imc-rce",
            "H3C IMC RCE",
            "正向：返回200+Administrator，负向：返回200+Normal"
        );

        // Goby POC 268: H5S GetSrc Info Leak
        testPocBothWays(
            "H5S GetSrc Info Leak",
            gobyBase + "H5S_CONSOLE_Video_Platform_GetSrc_Information_Leak_CNVD_2021_25919.json",
            BASE_URL + "/vuln/goby/h5s-getsrc-leak",
            BASE_URL + "/safe/goby/h5s-getsrc-leak",
            "H5S GetSrc 信息泄露",
            "正向：返回200+H5_STREAM，负向：返回401"
        );

        // Goby POC 269: H5S GetUserInfo Info Leak
        testPocBothWays(
            "H5S GetUserInfo Info Leak",
            gobyBase + "H5S_Video_Platform_GetUserInfo_Info_Leak_CNVD_2021_35567.json",
            BASE_URL + "/vuln/goby/h5s-userinfo-leak",
            BASE_URL + "/safe/goby/h5s-userinfo-leak",
            "H5S GetUserInfo 信息泄露",
            "正向：返回200+strPasswd，负向：返回401"
        );

        // Goby POC 270: Hikvision File Download
        testPocBothWays(
            "Hikvision File Download",
            gobyBase + "HIKVISION 视频编码设备接入网关 任意文件下载.json",
            BASE_URL + "/vuln/goby/hikvision-file-download",
            BASE_URL + "/safe/goby/hikvision-file-download",
            "海康威视文件下载",
            "正向：返回200+user，负向：返回403"
        );

        // Goby POC 272: Hikvision RCE
        testPocBothWays(
            "Hikvision RCE",
            gobyBase + "Hikvision_RCE_CVE_2021_36260.json",
            BASE_URL + "/vuln/goby/hikvision-rce",
            BASE_URL + "/safe/goby/hikvision-rce",
            "海康威视 RCE",
            "正向：返回200+result，负向：返回404"
        );

        // Goby POC 273: Hikvision Unauthenticated RCE
        testPocBothWays(
            "Hikvision Unauthenticated RCE",
            gobyBase + "Hikvision_Unauthenticated_RCE_CVE-2021-36260.json",
            BASE_URL + "/vuln/goby/hikvision-rce-unauth",
            BASE_URL + "/safe/goby/hikvision-rce-unauth",
            "海康威视未授权 RCE",
            "正向：返回200+result，负向：返回404"
        );

        // Goby POC 274: Hikvision Any File Download
        testPocBothWays(
            "Hikvision Any File Download",
            gobyBase + "Hikvision_Video_Encoding_Device_Access_Gateway_Any_File_Download.json",
            BASE_URL + "/vuln/goby/hikvision-any-file-download",
            BASE_URL + "/safe/goby/hikvision-any-file-download",
            "海康威视任意文件下载",
            "正向：返回200+$file_name，负向：返回200 Normal"
        );

        // Goby POC 275: HotelDruid XSS
        testPocBothWays(
            "HotelDruid XSS",
            gobyBase + "HotelDruid_Hotel_Management_Software_v3.0.3_XSS_CVE_2022_26564.json",
            BASE_URL + "/vuln/goby/hoteldruid-xss",
            BASE_URL + "/safe/goby/hoteldruid-xss",
            "HotelDruid XSS",
            "正向：返回200+XSS，负向：返回200 Normal"
        );

        // Goby POC 276: Hsmedia Hgateway Default Account
        testPocBothWays(
            "Hsmedia Hgateway Default Account",
            gobyBase + "Hsmedia_Hgateway_Default_account.json",
            BASE_URL + "/vuln/goby/hsmedia-default-account",
            BASE_URL + "/safe/goby/hsmedia-default-account",
            "Hsmedia Hgateway 默认账户",
            "正向：返回200+success，负向：返回200+flag=0"
        );

        // Goby POC 277: IFW8 Router Password Leakage
        testPocBothWays(
            "IFW8 Router Password Leakage",
            gobyBase + "IFW8_Enterprise_router_Password_leakage_.json",
            BASE_URL + "/vuln/goby/ifw8-password-leakage",
            BASE_URL + "/safe/goby/ifw8-password-leakage",
            "IFW8 路由器密码泄露",
            "正向：返回200+pwd，负向：返回200 Login Page"
        );

        // Goby POC 278: IFW8 Router Credential Discovery
        testPocBothWays(
            "IFW8 Router Credential Discovery",
            gobyBase + "IFW8_Router_ROM_v4.31_Credential_Discovery_CVE_2019_16313.json",
            BASE_URL + "/vuln/goby/ifw8-credential-discovery",
            BASE_URL + "/safe/goby/ifw8-credential-discovery",
            "IFW8 路由器凭证发现",
            "正向：返回200+pwd，负向：返回200 Login Page"
        );

        // Goby POC 279: IRDM4000 Smart station Unauthorized access
        testPocBothWays(
            "IRDM4000 Unauthorized Access",
            gobyBase + "IRDM4000_Smart_station_Unauthorized_access.json",
            BASE_URL + "/vuln/goby/irdm4000-unauthorized",
            BASE_URL + "/safe/goby/irdm4000-unauthorized",
            "IRDM4000 未授权访问",
            "正向：返回200+视频监管，负向：返回200 Login"
        );

        // Goby POC 280: IceWarp WebClient basic RCE
        testPocBothWays(
            "IceWarp WebClient Basic RCE",
            gobyBase + "IceWarp_WebClient_basic_RCE.json",
            BASE_URL + "/vuln/goby/icewarp-basic-rce",
            BASE_URL + "/safe/goby/icewarp-basic-rce",
            "IceWarp RCE",
            "正向：返回200+Windows IP，负向：返回200 Normal"
        );

        // Goby POC 281: JQuery 1.7.2 File Download
        testPocBothWays(
            "JQuery 1.7.2 File Download",
            gobyBase + "JQuery_1.7.2Version_site_foreground_arbitrary_file_download.json",
            BASE_URL + "/vuln/goby/jquery-file-download",
            BASE_URL + "/safe/goby/jquery-file-download",
            "JQuery 任意文件下载",
            "正向：返回200+root，负向：返回200 Normal"
        );

        // Goby POC 282: JQuery 1.7.2 Filedownload
        testPocBothWays(
            "JQuery 1.7.2 Filedownload",
            gobyBase + "JQuery_1.7.2_Filedownload.json",
            BASE_URL + "/vuln/goby/jquery-1.7.2-file-download",
            BASE_URL + "/safe/goby/jquery-1.7.2-file-download",
            "JQuery 文件下载",
            "正向：返回200+root，负向：返回200 Normal"
        );

        // Goby POC 283: Jellyfin 10.7.0 Unauthenticated Arbitrary File Read
        testPocBothWays(
            "Jellyfin 10.7.0 File Read",
            gobyBase + "Jellyfin_10.7.0_Unauthenticated_Abritrary_File_Read_CVE_2021_21402.json",
            BASE_URL + "/vuln/goby/jellyfin-file-read-21402",
            BASE_URL + "/safe/goby/jellyfin-file-read-21402",
            "Jellyfin 10.7.0 任意文件读取",
            "正向：返回200+font+file+extension，负向：返回404"
        );

        // Goby POC 284: Jellyfin 10.7.2 SSRF
        testPocBothWays(
            "Jellyfin 10.7.2 SSRF",
            gobyBase + "Jellyfin_10.7.2_SSRF_CVE-2021-29490.json",
            BASE_URL + "/vuln/goby/jellyfin-ssrf-29490",
            BASE_URL + "/safe/goby/jellyfin-ssrf-29490",
            "Jellyfin 10.7.2 SSRF",
            "正向：返回200+百度，负向：返回200 Normal"
        );

        // Goby POC 285: Jellyfin SSRF (Reuse POC 284 Handler)
        testPocBothWays(
            "Jellyfin SSRF",
            gobyBase + "Jellyfin_SSRF_CVE_2021_29490.json",
            BASE_URL + "/vuln/goby/jellyfin-ssrf-reuse",
            BASE_URL + "/safe/goby/jellyfin-ssrf-reuse",
            "Jellyfin SSRF",
            "正向：返回200+百度，负向：返回200 Normal"
        );

        // Goby POC 286: Jellyfin prior to 10.7.0 File Read
        testPocBothWays(
            "Jellyfin Prior to 10.7.0 File Read",
            gobyBase + "Jellyfin_prior_to_10.7.0_Unauthenticated_Arbitrary_File_Read_CVE_2021_21402.json",
            BASE_URL + "/vuln/goby/jellyfin-file-read-prior",
            BASE_URL + "/safe/goby/jellyfin-file-read-prior",
            "Jellyfin < 10.7.0 任意文件读取",
            "正向：返回200+font+file+extension，负向：返回404"
        );

        // Goby POC 287: Jetty WEB-INF FileRead CVE-2021-28169
        testPocBothWays(
            "Jetty WEB-INF FileRead CVE-2021-28169",
            gobyBase + "Jetty_WEB_INF_FileRead_CVE_2021_28169.json",
            BASE_URL + "/vuln/goby/jetty-fileread-28169",
            BASE_URL + "/safe/goby/jetty-fileread-28169",
            "Jetty WEB-INF 文件读取 (CVE-2021-28169)",
            "正向：返回200+jetty，负向：返回404"
        );

        // Goby POC 288: Jetty WEB-INF FileRead CVE-2021-34429
        testPocBothWays(
            "Jetty WEB-INF FileRead CVE-2021-34429",
            gobyBase + "Jetty_WEB_INF_FileRead_CVE_2021_34429.json",
            BASE_URL + "/vuln/goby/jetty-fileread-34429",
            BASE_URL + "/safe/goby/jetty-fileread-34429",
            "Jetty WEB-INF 文件读取 (CVE-2021-34429)",
            "正向：返回200+web-app，负向：返回404"
        );

        // Goby POC 289: JinHe OA C6 Default password
        testPocBothWays(
            "JinHe OA C6 Default password",
            gobyBase + "JinHe_OA_C6_Default_password.json",
            BASE_URL + "/vuln/goby/jinhe-oa-default-pwd",
            BASE_URL + "/safe/goby/jinhe-oa-default-pwd",
            "金和 OA C6 默认密码",
            "正向：返回200+OK+系统管理员，负向：返回200 Login Failed"
        );

        // Goby POC 290: JinHe OA C6 download.jsp Arbitrary fileread
        testPocBothWays(
            "JinHe OA C6 File Read",
            gobyBase + "JinHe_OA_C6_download.jsp_Arbitrary_fileread.json",
            BASE_URL + "/vuln/goby/jinhe-oa-fileread",
            BASE_URL + "/safe/goby/jinhe-oa-fileread",
            "金和 OA C6 任意文件读取",
            "正向：返回200+xml，负向：返回404"
        );

        // Goby POC 291: JingHe OA C6 Default password
        testPocBothWays(
            "JingHe OA C6 Default password",
            gobyBase + "JingHe_OA_C6_Default_password.json",
            BASE_URL + "/vuln/goby/jinghe-oa-default-pwd",
            BASE_URL + "/safe/goby/jinghe-oa-default-pwd",
            "金和 OA C6 默认密码 (JingHe Typo)",
            "正向：返回200+OK+系统管理员，负向：返回200 Login Failed"
        );

        // Goby POC 292: Jinher OA C6 download.jsp Arbitrary file read
        testPocBothWays(
            "Jinher OA C6 File Read",
            gobyBase + "Jinher_OA_C6_download.jsp_Arbitrary_file_read.json",
            BASE_URL + "/vuln/goby/jinher-oa-fileread",
            BASE_URL + "/safe/goby/jinher-oa-fileread",
            "金和 OA C6 任意文件读取 (Jinher Typo)",
            "正向：返回200+xml，负向：返回404"
        );

        // Goby POC 293: Jinshan V8 Arbitrary File Read
        testPocBothWays(
            "Jinshan V8 File Read",
            gobyBase + "Jinshan_V8.json",
            BASE_URL + "/vuln/goby/kingsoft-v8-fileread",
            BASE_URL + "/safe/goby/kingsoft-v8-fileread",
            "金山 V8 任意文件读取",
            "正向：返回200+filename，负向：返回200 Normal"
        );

        // Goby POC 294: Jitong EWEBS Arbitrary File Read
        testPocBothWays(
            "Jitong EWEBS File Read",
            gobyBase + "Jitong_EWEBS_Fileread.json",
            BASE_URL + "/vuln/goby/jitong-ewebs-fileread",
            BASE_URL + "/safe/goby/jitong-ewebs-fileread",
            "极通 EWEBS 任意文件读取",
            "正向：返回200+MAPI，负向：返回200 Normal"
        );

        // Goby POC 295: Jitong EWEBS Arbitrary File Read (Reuse)
        testPocBothWays(
            "Jitong EWEBS File Read (Dup)",
            gobyBase + "Jitong_EWEBS_arbitrary_file_read.json",
            BASE_URL + "/vuln/goby/jitong-ewebs-fileread",
            BASE_URL + "/safe/goby/jitong-ewebs-fileread",
            "极通 EWEBS 任意文件读取 (Duplicate)",
            "正向：返回200+MAPI，负向：返回200 Normal"
        );

        // Goby POC 296: Jitong EWEBS Phpinfo Leak
        testPocBothWays(
            "Jitong EWEBS Phpinfo",
            gobyBase + "Jitong_EWEBS_phpinfo_leak.json",
            BASE_URL + "/vuln/goby/jitong-ewebs-phpinfo",
            BASE_URL + "/safe/goby/jitong-ewebs-phpinfo",
            "极通 EWEBS phpinfo 泄露",
            "正向：返回200+PHP Version，负向：返回200 Normal"
        );

        // Goby POC 297: KEDACOM MTS File Download
        testPocBothWays(
            "KEDACOM MTS File Download",
            gobyBase + "KEDACOM_MTS_transcoding_server_Arbitrary_file_download_CNVD_2020_48650.json",
            BASE_URL + "/vuln/goby/kedacom-mts-download",
            BASE_URL + "/safe/goby/kedacom-mts-download",
            "科达 MTS 转码服务器文件下载",
            "正向：返回200+root:x:，负向：返回404"
        );

        // Goby POC 298: KEDACOM MTS File Read (Dup)
        testPocBothWays(
            "KEDACOM MTS File Read",
            gobyBase + "KEDACOM_MTS_transcoding_server_Fileread_CNVD_2020_48650.json",
            BASE_URL + "/vuln/goby/kedacom-mts-download",
            BASE_URL + "/safe/goby/kedacom-mts-download",
            "科达 MTS 转码服务器文件读取",
            "正向：返回200+root:x:，负向：返回404"
        );

        // Goby POC 299: Kingsoft V8 Arbitrary File Read (Dup)
        testPocBothWays(
            "Kingsoft V8 File Read",
            gobyBase + "Kingsoft_V8_Arbitrary_file_read.json",
            BASE_URL + "/vuln/goby/kingsoft-v8-fileread",
            BASE_URL + "/safe/goby/kingsoft-v8-fileread",
            "金山 V8 任意文件读取 (Duplicate)",
            "正向：返回200+filename，负向：返回200 Normal"
        );

        // Goby POC 300: Kingsoft V8 Default Weak Password
        testPocBothWays(
            "Kingsoft V8 Weak Password",
            gobyBase + "Kingsoft_V8_Default_weak_password.json",
            BASE_URL + "/vuln/goby/kingsoft-v8-weak-pwd",
            BASE_URL + "/safe/goby/kingsoft-v8-weak-pwd",
            "金山 V8 默认弱口令",
            "正向：返回200+userSession，负向：返回200 Login Failed"
        );

        // Goby POC 301: Kingsoft V8 Default Login (Dup)
        testPocBothWays(
            "Kingsoft V8 Default Login",
            gobyBase + "Kingsoft_V8_Terminal_Security_System_Default_Login_CNVD_2021_32425.json",
            BASE_URL + "/vuln/goby/kingsoft-v8-weak-pwd",
            BASE_URL + "/safe/goby/kingsoft-v8-weak-pwd",
            "金山 V8 默认登录 (Duplicate)",
            "正向：返回200+userSession，负向：返回200 Login Failed"
        );

        // Goby POC 302: Kingsoft V8 Fileread (Dup)
        testPocBothWays(
            "Kingsoft V8 Fileread",
            gobyBase + "Kingsoft_V8_Terminal_Security_System_Fileread.json",
            BASE_URL + "/vuln/goby/kingsoft-v8-fileread",
            BASE_URL + "/safe/goby/kingsoft-v8-fileread",
            "金山 V8 文件读取 (Duplicate)",
            "正向：返回200+filename，负向：返回200 Normal"
        );

        // Goby POC 303: Konga Default JWT Key
        testPocBothWays(
            "Konga Default JWT Key",
            gobyBase + "Konga_Default_JWT_KEY.json",
            BASE_URL + "/vuln/goby/konga-jwt",
            BASE_URL + "/safe/goby/konga-jwt",
            "Konga 默认 JWT 密钥",
            "正向：返回200+username，负向：返回401"
        );

        // Goby POC 304: Kyan Account Password Leak
        testPocBothWays(
            "Kyan Account Leak",
            gobyBase + "Kyan.json",
            BASE_URL + "/vuln/goby/kyan-account-leak",
            BASE_URL + "/safe/goby/kyan-account-leak",
            "Kyan 账号密码泄露",
            "正向：返回200+Password，负向：返回200 Normal"
        );

        // Goby POC 305: Kyan Account Leak (Dup)
        testPocBothWays(
            "Kyan Account Leak 2",
            gobyBase + "Kyan_Account_password_leak.json",
            BASE_URL + "/vuln/goby/kyan-account-leak",
            BASE_URL + "/safe/goby/kyan-account-leak",
            "Kyan 账号密码泄露 2",
            "正向：返回200+Password，负向：返回200 Normal"
        );

        // Goby POC 306: Kyan Design Account Leak (Dup)
        testPocBothWays(
            "Kyan Design Account Leak",
            gobyBase + "Kyan_design_account_password_disclosure.json",
            BASE_URL + "/vuln/goby/kyan-account-leak",
            BASE_URL + "/safe/goby/kyan-account-leak",
            "Kyan Design 账号密码泄露",
            "正向：返回200+Password，负向：返回200 Normal"
        );

        // Goby POC 307: Kyan Network Monitoring Leak (Dup)
        testPocBothWays(
            "Kyan Network Monitoring Leak",
            gobyBase + "Kyan_network_monitoring_device_account_password_leak.json",
            BASE_URL + "/vuln/goby/kyan-account-leak",
            BASE_URL + "/safe/goby/kyan-account-leak",
            "Kyan 网络监控设备账号泄露",
            "正向：返回200+Password，负向：返回200 Normal"
        );

        // Goby POC 308: Kyan RCE
        testPocBothWays(
            "Kyan RCE",
            gobyBase + "Kyan_network_monitoring_device_run.php_RCE.json",
            BASE_URL + "/vuln/goby/kyan-rce",
            BASE_URL + "/safe/goby/kyan-rce",
            "Kyan 远程代码执行",
            "正向：返回200+PHP Version，负向：返回200 Normal"
        );

        // Goby POC 309: Kyan RCE (Dup)
        testPocBothWays(
            "Kyan RCE 2",
            gobyBase + "Kyan_run.php_RCE.json",
            BASE_URL + "/vuln/goby/kyan-rce",
            BASE_URL + "/safe/goby/kyan-rce",
            "Kyan 远程代码执行 2",
            "正向：返回200+PHP Version，负向：返回200 Normal"
        );

        // Goby POC 310: Landray OA Custom JSP File Read
        testPocBothWays(
            "Landray OA File Read",
            gobyBase + "Landray_OA_custom.jsp_Fileread.json",
            BASE_URL + "/vuln/goby/landray-oa-custom-fileread",
            BASE_URL + "/safe/goby/landray-oa-custom-fileread",
            "蓝凌 OA custom.jsp 任意文件读取",
            "正向：返回200+root:，负向：返回200 Normal"
        );

        // Goby POC 311: Lanproxy Directory Traversal
        testPocBothWays(
            "Lanproxy Directory Traversal",
            gobyBase + "Lanproxy 目录遍历漏洞 CVE-2021-3019.json",
            BASE_URL + "/vuln/goby/lanproxy-traversal",
            BASE_URL + "/safe/goby/lanproxy-traversal",
            "Lanproxy 目录遍历",
            "正向：返回200+server.ssl，负向：返回404"
        );

        // Goby POC 312: Lanproxy File Read (Dup)
        testPocBothWays(
            "Lanproxy File Read",
            gobyBase + "Lanproxy_Arbitrary_File_Read_CVE_2021_3019.json",
            BASE_URL + "/vuln/goby/lanproxy-traversal",
            BASE_URL + "/safe/goby/lanproxy-traversal",
            "Lanproxy 任意文件读取",
            "正向：返回200+server.ssl，负向：返回404"
        );

        // Goby POC 313: Lanproxy Directory Traversal (Dup)
        testPocBothWays(
            "Lanproxy Directory Traversal 2",
            gobyBase + "Lanproxy_Directory_traversal_CVE_2021_3019.json",
            BASE_URL + "/vuln/goby/lanproxy-traversal",
            BASE_URL + "/safe/goby/lanproxy-traversal",
            "Lanproxy 目录遍历 2",
            "正向：返回200+server.ssl，负向：返回404"
        );

        // Goby POC 314: Laravel .env Leak
        testPocBothWays(
            "Laravel .env Leak",
            gobyBase + "Laravel .env 配置文件泄露 CVE-2017-16894.json",
            BASE_URL + "/vuln/goby/laravel-env-leak",
            BASE_URL + "/safe/goby/laravel-env-leak",
            "Laravel .env 泄露",
            "正向：返回200+APP_KEY，负向：返回404"
        );

        // Goby POC 315: Laravel .env Leak (Dup)
        testPocBothWays(
            "Laravel .env Leak 2",
            gobyBase + "Laravel_.env_configuration_file_leaks_(CVE-2017-16894).json",
            BASE_URL + "/vuln/goby/laravel-env-leak",
            BASE_URL + "/safe/goby/laravel-env-leak",
            "Laravel .env 泄露 2",
            "正向：返回200+APP_KEY，负向：返回404"
        );

        // Goby POC 316: Laravel .env Leak (Dup)
        testPocBothWays(
            "Laravel .env Leak 3",
            gobyBase + "Laravel_.env_configuration_file_leaks_CVE_2017_16894.json",
            BASE_URL + "/vuln/goby/laravel-env-leak",
            BASE_URL + "/safe/goby/laravel-env-leak",
            "Laravel .env 泄露 3",
            "正向：返回200+APP_KEY，负向：返回404"
        );

        // Goby POC 317: Leadsec ACM Info Leak
        testPocBothWays(
            "Leadsec ACM Info Leak",
            gobyBase + "Leadsec_ACM_infoleak_CNVD-2016-08574.json",
            BASE_URL + "/vuln/goby/leadsec-acm-leak",
            BASE_URL + "/safe/goby/leadsec-acm-leak",
            "网御星云 ACM 信息泄露",
            "正向：返回200+admin:password，负向：返回404"
        );

        // Goby POC 318: Leadsec ACM Info Leak (Dup)
        testPocBothWays(
            "Leadsec ACM Info Leak 2",
            gobyBase + "Leadsec_ACM_information_leakage_CNVD_2016_08574.json",
            BASE_URL + "/vuln/goby/leadsec-acm-leak",
            BASE_URL + "/safe/goby/leadsec-acm-leak",
            "网御星云 ACM 信息泄露 2",
            "正向：返回200+admin:password，负向：返回404"
        );

        // Goby POC 319: MPSec ISG1000 Gateway File Download
        testPocBothWays(
            "MPSec ISG1000 File Download",
            gobyBase + "MPSec_ISG1000_Gateway_Filedownload_CNVD_2021_43984.json",
            BASE_URL + "/vuln/goby/mpsec-isg-download",
            BASE_URL + "/safe/goby/mpsec-isg-download",
            "迈普 ISG1000 网关文件下载",
            "正向：返回200+root:x:，负向：返回200 Normal"
        );

        // Goby POC 320: MPSec ISG1000 File Download (Dup)
        testPocBothWays(
            "MPSec ISG1000 File Download 2",
            gobyBase + "MPSec_ISG1000_Security_Gateway_Arbitrary_File_Download_Vulnerability.json",
            BASE_URL + "/vuln/goby/mpsec-isg-download",
            BASE_URL + "/safe/goby/mpsec-isg-download",
            "迈普 ISG1000 网关文件下载 2",
            "正向：返回200+root:x:，负向：返回200 Normal"
        );

        // Goby POC 321: Mallgard Firewall Default Login
        testPocBothWays(
            "Mallgard Firewall Default Login",
            gobyBase + "Mallgard_Firewall_Default_Login_CNVD_2020_73282.json",
            BASE_URL + "/vuln/goby/mallgard-firewall-login",
            BASE_URL + "/safe/goby/mallgard-firewall-login",
            "网神防火墙默认登录",
            "正向：返回200+message success，负向：返回200 Login Failed"
        );

        // Goby POC 322: MessageSolution EEA Info Leak
        testPocBothWays(
            "MessageSolution EEA Info Leak",
            gobyBase + "MessageSolution  邮件归档系统EEA 信息泄露漏洞 CNVD-2021-10543.json",
            BASE_URL + "/vuln/goby/messagesolution-eea-leak",
            BASE_URL + "/safe/goby/messagesolution-eea-leak",
            "MessageSolution EEA 信息泄露",
            "正向：返回200+administrator，负向：返回200 Normal"
        );

        // Goby POC 323: MessageSolution EEA Info Leak (Dup)
        testPocBothWays(
            "MessageSolution EEA Info Leak 2",
            gobyBase + "MessageSolution_EEA_information_disclosure.json",
            BASE_URL + "/vuln/goby/messagesolution-eea-leak-dup",
            BASE_URL + "/safe/goby/messagesolution-eea-leak-dup",
            "MessageSolution EEA 信息泄露 2",
            "正向：返回200+administrator，负向：返回200 Normal"
        );

        // Goby POC 324: MessageSolution EEA Info Leak (Dup)
        testPocBothWays(
            "MessageSolution EEA Info Leak 3",
            gobyBase + "MessageSolution_EEA_information_disclosure_CNVD_2021_10543.json",
            BASE_URL + "/vuln/goby/messagesolution-eea-leak-dup",
            BASE_URL + "/safe/goby/messagesolution-eea-leak-dup",
            "MessageSolution EEA 信息泄露 3",
            "正向：返回200+administrator，负向：返回200 Normal"
        );

        // Goby POC 325: Metabase Geojson Arbitrary File Read
        testPocBothWays(
            "Metabase Geojson File Read",
            gobyBase + "Metabase_Geojson_Arbitrary_File_Read_CVE-2021-41277.json",
            BASE_URL + "/vuln/goby/metabase-geojson-fileread",
            BASE_URL + "/safe/goby/metabase-geojson-fileread",
            "Metabase Geojson 任意文件读取",
            "正向：返回200+/root:/bin/ash，负向：返回200 Normal"
        );

        // Goby POC 326: Metabase Geojson Arbitrary File Read (Dup)
        testPocBothWays(
            "Metabase Geojson File Read 2",
            gobyBase + "Metabase_Geojson_Arbitrary_File_Read_CVE_2021_41277.json",
            BASE_URL + "/vuln/goby/metabase-geojson-fileread",
            BASE_URL + "/safe/goby/metabase-geojson-fileread",
            "Metabase Geojson 任意文件读取 2",
            "正向：返回200+/root:/bin/ash，负向：返回200 Normal"
        );

        // Goby POC 327: Metabase Geojson Arbitrary File Read (Dup)
        testPocBothWays(
            "Metabase Geojson File Read 3",
            gobyBase + "Metabase_geojson_Arbitrary_file_reading_CVE_2021_41277.json",
            BASE_URL + "/vuln/goby/metabase-geojson-fileread",
            BASE_URL + "/safe/goby/metabase-geojson-fileread",
            "Metabase Geojson 任意文件读取 3",
            "正向：返回200+/root:/bin/ash，负向：返回200 Normal"
        );

        // Goby POC 328: Micro module monitoring system User_list.php information leakage
        testPocBothWays(
            "Micro module monitoring Info Leak",
            gobyBase + "Micro_module_monitoring_system_User_list.php_information_leakage.json",
            BASE_URL + "/vuln/goby/micro-module-leak",
            BASE_URL + "/safe/goby/micro-module-leak",
            "微模块监控系统信息泄露",
            "正向：返回200+password1，负向：返回200 Normal"
        );

        // Goby POC 329: Microsoft Exchange SSRF
        testPocBothWays(
            "Microsoft Exchange SSRF",
            gobyBase + "Microsoft Exchange SSRF漏洞 CVE-2021-26885.json",
            BASE_URL + "/vuln/goby/exchange-ssrf",
            BASE_URL + "/safe/goby/exchange-ssrf",
            "Exchange SSRF (CVE-2021-26885)",
            "正向：返回500+NegotiateSecurityContext，负向：返回200 Normal"
        );

        // Goby POC 330: Microsoft Exchange SSRF (Dup)
        testPocBothWays(
            "Microsoft Exchange SSRF 2",
            gobyBase + "Microsoft_Exchange_Server_SSRF_CVE_2021_26885.json",
            BASE_URL + "/vuln/goby/exchange-ssrf",
            BASE_URL + "/safe/goby/exchange-ssrf",
            "Exchange SSRF (Duplicate)",
            "正向：返回500+NegotiateSecurityContext，负向：返回200 Normal"
        );

        // Goby POC 331: MinIO Browser API SSRF
        testPocBothWays(
            "MinIO Browser API SSRF",
            gobyBase + "MinIO_Browser_API_SSRF_CVE_2021_21287.json",
            BASE_URL + "/vuln/goby/minio-browser-ssrf",
            BASE_URL + "/safe/goby/minio-browser-ssrf",
            "MinIO Browser API SSRF",
            "正向：返回200+message，负向：返回200 Normal"
        );

        // Goby POC 334: Node-RED ui_base Arbitrary File Read
        testPocBothWays(
            "Node-RED File Read",
            gobyBase + "Node-RED_ui_base_Arbitrary_File_Read_CVE-2021-3223.json",
            BASE_URL + "/vuln/goby/node-red-fileread",
            BASE_URL + "/safe/goby/node-red-fileread",
            "Node-RED 任意文件读取",
            "正向：返回200+root:x:，负向：返回404"
        );

        // Goby POC 335: Node.js Path Traversal
        testPocBothWays(
            "Node.js Path Traversal",
            gobyBase + "Node.js_Path_Traversal_CVE_2017_14849.json",
            BASE_URL + "/vuln/goby/nodejs-path-traversal",
            BASE_URL + "/safe/goby/nodejs-path-traversal",
            "Node.js 路径遍历",
            "正向：返回200+root，负向：返回404"
        );

        // Goby POC 336: Node-RED File Read (Dup)
        testPocBothWays(
            "Node-RED File Read 2",
            gobyBase + "Node_RED_ui_base_Arbitrary_File_Read.json",
            BASE_URL + "/vuln/goby/node-red-fileread",
            BASE_URL + "/safe/goby/node-red-fileread",
            "Node-RED 任意文件读取 2",
            "正向：返回200+root:x:，负向：返回404"
        );

        // Goby POC 337: OpenSNS RCE
        testPocBothWays(
            "OpenSNS RCE",
            gobyBase + "OpenSNS_Application_ShareController.class.php__remote_command_execution_vulnerability.json",
            BASE_URL + "/vuln/goby/opensns-rce",
            BASE_URL + "/safe/goby/opensns-rce",
            "OpenSNS RCE",
            "正向：返回200+PHP Version，负向：返回200 Normal"
        );

        // Goby POC 338: OpenSNS RCE (Dup)
        testPocBothWays(
            "OpenSNS RCE 2",
            gobyBase + "OpenSNS_RCE.json",
            BASE_URL + "/vuln/goby/opensns-rce",
            BASE_URL + "/safe/goby/opensns-rce",
            "OpenSNS RCE 2",
            "正向：返回200+PHP Version，负向：返回200 Normal"
        );

        // Goby POC 339: Oracle WebLogic Server Path Traversal
        testPocBothWays(
            "WebLogic Path Traversal",
            gobyBase + "Oracle_WebLogic_Server_Path_Traversal_CVE_2022_21371.json",
            BASE_URL + "/vuln/goby/weblogic-path-traversal",
            BASE_URL + "/safe/goby/weblogic-path-traversal",
            "WebLogic 路径遍历",
            "正向：返回200+root，负向：返回404"
        );

        // Goby POC 340: Oracle Weblogic LDAP RCE
        testPocBothWays(
            "WebLogic LDAP RCE",
            gobyBase + "Oracle_Weblogic_LDAP_RCE_CVE_2021_2109.json",
            BASE_URL + "/vuln/goby/weblogic-ldap-rce",
            BASE_URL + "/safe/goby/weblogic-ldap-rce",
            "WebLogic LDAP RCE",
            "正向：返回200+root，负向：返回404"
        );

        // Goby POC 341: Oracle Weblogic SSRF
        testPocBothWays(
            "WebLogic SSRF",
            gobyBase + "Oracle_Weblogic_SearchPublicRegistries.jsp_SSRF_CVE_2014_4210.json",
            BASE_URL + "/vuln/goby/weblogic-ssrf",
            BASE_URL + "/safe/goby/weblogic-ssrf",
            "WebLogic SSRF",
            "正向：返回200+XML_SoapException，负向：返回200 Normal"
        );

        // Goby POC 342: Oray Sunlogin RCE
        testPocBothWays(
            "Oray Sunlogin RCE",
            gobyBase + "Oray_Sunlogin_RCE_CNVD_2022_03672_CNVD_2022_10270.json",
            BASE_URL + "/vuln/goby/oray-sunlogin-rce",
            BASE_URL + "/safe/goby/oray-sunlogin-rce",
            "向日葵 RCE",
            "正向：返回200+verify_string，负向：返回200 Normal"
        );

        // Goby POC 343: PHP Zerodium Backdoor RCE
        testPocBothWays(
            "PHP Zerodium Backdoor",
            gobyBase + "PHP_8.1.0-dev_Zerodium_Backdoor_RCE.json",
            BASE_URL + "/vuln/goby/php-zerodium-rce",
            BASE_URL + "/safe/goby/php-zerodium-rce",
            "PHP 8.1.0-dev 后门",
            "正向：返回200+int(54289)，负向：返回200 Normal"
        );

        // Goby POC 344: Portainer Init Deploy
        testPocBothWays(
            "Portainer Init Deploy",
            gobyBase + "Portainer_Init_Deploy_CVE_2018_19367.json",
            BASE_URL + "/vuln/goby/portainer-init",
            BASE_URL + "/safe/goby/portainer-init",
            "Portainer 初始部署漏洞",
            "正向：返回200+Success，负向：返回404"
        );

        // Goby POC 345: RG UAC
        testPocBothWays(
            "RG UAC",
            gobyBase + "RG_UAC.json",
            BASE_URL + "/vuln/goby/rg-uac",
            BASE_URL + "/safe/goby/rg-uac",
            "锐捷 UAC",
            "正向：返回200+Vulnerable，负向：返回200 Normal"
        );

        // Goby POC 346: Riskscanner SQL Injection
        testPocBothWays(
            "Riskscanner SQL Injection",
            gobyBase + "Riskscanner_list_SQL_injection.json",
            BASE_URL + "/vuln/goby/riskscanner-sqli",
            BASE_URL + "/safe/goby/riskscanner-sqli",
            "Riskscanner SQL 注入",
            "正向：返回500+SQL syntax，负向：返回200 Normal"
        );

        // Goby POC 347: Ruijie EWEB RCE
        testPocBothWays(
            "Ruijie EWEB RCE",
            gobyBase + "Ruijie_Networks_EWEB_Network_Management_System_RCE_CNVD_2021_09650.json",
            BASE_URL + "/vuln/goby/ruijie-eweb-rce",
            BASE_URL + "/safe/goby/ruijie-eweb-rce",
            "锐捷 EWEB RCE",
            "正向：返回200+123，负向：返回200 Normal"
        );

        // Goby POC 348: Ruijie RG-UAC Password Leak
        testPocBothWays(
            "Ruijie RG-UAC Password Leak",
            gobyBase + "Ruijie_RG_UAC_Password_leakage_CNVD_2021_14536.json",
            BASE_URL + "/vuln/goby/ruijie-rg-uac-leak",
            BASE_URL + "/safe/goby/ruijie-rg-uac-leak",
            "锐捷 RG-UAC 密码泄露",
            "正向：返回200+password，负向：返回200 Normal"
        );

        // Goby POC 349: Ruijie Smartweb Password Leak
        testPocBothWays(
            "Ruijie Smartweb Password Leak",
            gobyBase + "Ruijie_Smartweb_Default_Password_CNVD_2020_56167.json",
            BASE_URL + "/vuln/goby/ruijie-smartweb-leak",
            BASE_URL + "/safe/goby/ruijie-smartweb-leak",
            "锐捷 Smartweb 密码泄露",
            "正向：返回200+admin，负向：返回200 Normal"
        );

        // Goby POC 350: Ruijie Smartweb Password Leak (Dup)
        testPocBothWays(
            "Ruijie Smartweb Password Leak 2",
            gobyBase + "Ruijie_Smartweb_Management_System_Password_Information_Disclosure_CNVD_2021_17369.json",
            BASE_URL + "/vuln/goby/ruijie-smartweb-leak",
            BASE_URL + "/safe/goby/ruijie-smartweb-leak",
            "锐捷 Smartweb 密码泄露 2",
            "正向：返回200+admin，负向：返回200 Normal"
        );

        // Goby POC 351: Ruijie Smartweb Password Leak (Dup)
        testPocBothWays(
            "Ruijie Smartweb Password Leak 3",
            gobyBase + "Ruijie_smartweb_password_information_disclosure.json",
            BASE_URL + "/vuln/goby/ruijie-smartweb-leak",
            BASE_URL + "/safe/goby/ruijie-smartweb-leak",
            "锐捷 Smartweb 密码泄露 3",
            "正向：返回200+admin，负向：返回200 Normal"
        );

        // Goby POC 352: Ruijie Smartweb Weak Password
        testPocBothWays(
            "Ruijie Smartweb Weak Password",
            gobyBase + "Ruijie_smartweb_weak_password.json",
            BASE_URL + "/vuln/goby/ruijie-smartweb-weak-pwd",
            BASE_URL + "/safe/goby/ruijie-smartweb-weak-pwd",
            "锐捷 Smartweb 弱口令",
            "正向：返回200+LEVEL15，负向：返回401"
        );

        // Goby POC 353: RuoYi Druid Unauthorized Access
        testPocBothWays(
            "RuoYi Druid Unauth",
            gobyBase + "RuoYi_Druid_Unauthorized_access.json",
            BASE_URL + "/vuln/goby/ruoyi-druid-unauth",
            BASE_URL + "/safe/goby/ruoyi-druid-unauth",
            "若依 Druid 未授权访问",
            "正向：返回200+Druid Stat Index，负向：返回404"
        );

        // Goby POC 354: SDWAN Smart Gateway Default Password
        testPocBothWays(
            "SDWAN Smart Gateway Default Pwd",
            gobyBase + "SDWAN_Smart_Gateway_Default_Password.json",
            BASE_URL + "/vuln/goby/sdwan-smart-gateway",
            BASE_URL + "/safe/goby/sdwan-smart-gateway",
            "SDWAN 智能网关默认密码",
            "正向：返回200+success，负向：返回401"
        );

        // Goby POC 355: SDWAN Smart Gateway Weak Password (Dup)
        testPocBothWays(
            "SDWAN Smart Gateway Weak Pwd",
            gobyBase + "SDWAN_smart_gateway_weak_password.json",
            BASE_URL + "/vuln/goby/sdwan-smart-gateway",
            BASE_URL + "/safe/goby/sdwan-smart-gateway",
            "SDWAN 智能网关弱口令",
            "正向：返回200+success，负向：返回401"
        );

        // Goby POC 356: Samsung WLAN AP RCE
        testPocBothWays(
            "Samsung WLAN AP RCE",
            gobyBase + "Samsung_WLAN_AP_RCE.json",
            BASE_URL + "/vuln/goby/samsung-wlan-rce",
            BASE_URL + "/safe/goby/samsung-wlan-rce",
            "三星 WLAN AP RCE",
            "正向：返回200+uid=0(root)，负向：返回200 Normal"
        );

        // Goby POC 357: Samsung WLAN AP RCE 2 (Dup)
        testPocBothWays(
            "Samsung WLAN AP RCE 2",
            gobyBase + "Samsung_WLAN_AP_WEA453e_RCE.json",
            BASE_URL + "/vuln/goby/samsung-wlan-rce",
            BASE_URL + "/safe/goby/samsung-wlan-rce",
            "三星 WLAN AP RCE 2",
            "正向：返回200+uid=0(root)，负向：返回200 Normal"
        );

        // Goby POC 358: Samsung WLAN AP RCE 3 (Dup)
        testPocBothWays(
            "Samsung WLAN AP RCE 3",
            gobyBase + "Samsung_WLAN_AP_wea453e_router_RCE.json",
            BASE_URL + "/vuln/goby/samsung-wlan-rce",
            BASE_URL + "/safe/goby/samsung-wlan-rce",
            "三星 WLAN AP RCE 3",
            "正向：返回200+uid=0(root)，负向：返回200 Normal"
        );

        // Goby POC 359: Security Devices Hardcoded Password
        testPocBothWays(
            "Security Devices Hardcoded Pwd",
            gobyBase + "Security_Devices_Hardcoded_Password.json",
            BASE_URL + "/vuln/goby/security-devices-password",
            BASE_URL + "/safe/goby/security-devices-password",
            "安全设备硬编码密码",
            "正向：返回200+admin，负向：返回401"
        );

        // Goby POC 360: Seeyon OA A6 DownExcelBeanServlet
        testPocBothWays(
            "Seeyon OA A6 DownExcel",
            gobyBase + "Seeyon_OA_A6_DownExcelBeanServlet_User_information_leakage.json",
            BASE_URL + "/vuln/goby/seeyon-downexcel",
            BASE_URL + "/safe/goby/seeyon-downexcel",
            "致远 OA A6 用户信息泄露",
            "正向：返回200+@，负向：返回200 Normal"
        );

        // Goby POC 361: Seeyon OA A6 createMysql.jsp
        testPocBothWays(
            "Seeyon OA A6 createMysql",
            gobyBase + "Seeyon_OA_A6__Disclosure_of_database_sensitive_information.json",
            BASE_URL + "/vuln/goby/seeyon-createmysql",
            BASE_URL + "/safe/goby/seeyon-createmysql",
            "致远 OA A6 数据库信息泄露",
            "正向：返回200+root，负向：返回404"
        );

        // Goby POC 362: Seeyon OA A6 initDataAssess.jsp
        testPocBothWays(
            "Seeyon OA A6 initDataAssess",
            gobyBase + "Seeyon_OA_A6_initDataAssess.jsp_User_information_leakage.json",
            BASE_URL + "/vuln/goby/seeyon-initdata",
            BASE_URL + "/safe/goby/seeyon-initdata",
            "致远 OA A6 initDataAssess 信息泄露",
            "正向：返回200+personList，负向：返回200 Normal"
        );

        // Goby POC 363: Seeyon OA A6 setextno.jsp
        testPocBothWays(
            "Seeyon OA A6 setextno",
            gobyBase + "Seeyon_OA_A6_setextno.jsp_SQL_injection.json",
            BASE_URL + "/vuln/goby/seeyon-setextno",
            BASE_URL + "/safe/goby/seeyon-setextno",
            "致远 OA A6 setextno SQL 注入",
            "正向：返回200+c4ca4238a0b923820dcc509a6f75849b，负向：返回200 Normal"
        );

        // Goby POC 364: Seeyon OA A6 test.jsp
        testPocBothWays(
            "Seeyon OA A6 test.jsp",
            gobyBase + "Seeyon_OA_A6_test.jsp_SQL_injection.json",
            BASE_URL + "/vuln/goby/seeyon-testjsp",
            BASE_URL + "/safe/goby/seeyon-testjsp",
            "致远 OA A6 test.jsp SQL 注入",
            "正向：返回200+c4ca4238a0b923820dcc509a6f75849b，负向：返回200 Normal"
        );

        // Goby POC 365: Seeyon OA A8-m Info Leak
        testPocBothWays(
            "Seeyon OA A8-m Info Leak",
            gobyBase + "Seeyon_OA_A8_m_Information_leakage.json",
            BASE_URL + "/vuln/goby/seeyon-a8-leak",
            BASE_URL + "/safe/goby/seeyon-a8-leak",
            "致远 OA A8-m 信息泄露",
            "正向：返回200+Password，负向：返回200 Normal"
        );

        // Goby POC 366: Shiziyu CMS SQL Injection
        testPocBothWays(
            "Shiziyu CMS SQL Injection",
            gobyBase + "ShiziyuCms_ApiController.class.php_SQL_injection.go.json",
            BASE_URL + "/vuln/goby/shiziyu-cms-sqli",
            BASE_URL + "/safe/goby/shiziyu-cms-sqli",
            "狮子鱼 CMS SQL 注入",
            "正向：返回200+md5(1)，负向：返回200 Normal"
        );

        // Goby POC 367: Shiziyu CMS SQL Injection 2 (Dup)
        testPocBothWays(
            "Shiziyu CMS SQL Injection 2",
            gobyBase + "ShiziyuCms_ApigoodsController.class.php_SQL_injection.go.json",
            BASE_URL + "/vuln/goby/shiziyu-cms-sqli",
            BASE_URL + "/safe/goby/shiziyu-cms-sqli",
            "狮子鱼 CMS SQL 注入 2",
            "正向：返回200+md5(1)，负向：返回200 Normal"
        );

        // Goby POC 368: ShopXO File Read
        testPocBothWays(
            "ShopXO File Read",
            gobyBase + "ShopXO_Fileread_CNVD_2021_15822.json",
            BASE_URL + "/vuln/goby/shopxo-fileread",
            BASE_URL + "/safe/goby/shopxo-fileread",
            "ShopXO 任意文件读取",
            "正向：返回200+root:x:，负向：返回200 Normal"
        );

        // Goby POC 369: ShopXO File Read 2 (Dup)
        testPocBothWays(
            "ShopXO File Read 2",
            gobyBase + "ShopXO_download_Arbitrary_file_read_CNVD_2021_15822.json",
            BASE_URL + "/vuln/goby/shopxo-fileread",
            BASE_URL + "/safe/goby/shopxo-fileread",
            "ShopXO 任意文件读取 2",
            "正向：返回200+root:x:，负向：返回200 Normal"
        );

        // Goby POC 370: Shterm QiZhi Fortress
        testPocBothWays(
            "Shterm QiZhi Fortress",
            gobyBase + "Shterm_QiZhi_Fortress_Unauthorized_Access_CNVD_2019_27717.json",
            BASE_URL + "/vuln/goby/shterm-qizhi",
            BASE_URL + "/safe/goby/shterm-qizhi",
            "齐治堡垒机未授权访问",
            "正向：返回200+事件审计，负向：返回200 Normal"
        );

        // Goby POC 371: SonarQube Search Projects
        testPocBothWays(
            "SonarQube Search Projects",
            gobyBase + "SonarQube_search_projects_information.json",
            BASE_URL + "/vuln/goby/sonarqube-search",
            BASE_URL + "/safe/goby/sonarqube-search",
            "SonarQube 项目搜索信息泄露",
            "正向：返回200+paging，负向：返回401"
        );

        // Goby POC 372: SonarQube Unauth
        testPocBothWays(
            "SonarQube Unauth",
            gobyBase + "SonarQube_unauth_CVE-2020-27986.json",
            BASE_URL + "/vuln/goby/sonarqube-unauth",
            BASE_URL + "/safe/goby/sonarqube-unauth",
            "SonarQube 未授权访问 (CVE-2020-27986)",
            "正向：返回200+sonaranalyzer-cs，负向：返回401"
        );

        // Goby POC 373: SonarQube Unauth (Dup)
        testPocBothWays(
            "SonarQube Unauth 2",
            gobyBase + "SonarQube_unauth_CVE_2020_27986.json",
            BASE_URL + "/vuln/goby/sonarqube-unauth",
            BASE_URL + "/safe/goby/sonarqube-unauth",
            "SonarQube 未授权访问 2",
            "正向：返回200+sonaranalyzer-cs，负向：返回401"
        );

        // Goby POC 374: SonicWall SSL-VPN RCE
        testPocBothWays(
            "SonicWall SSL-VPN RCE",
            gobyBase + "SonicWall SSL-VPN 远程命令执行漏洞.json",
            BASE_URL + "/vuln/goby/sonicwall-rce",
            BASE_URL + "/safe/goby/sonicwall-rce",
            "SonicWall SSL-VPN RCE",
            "正向：返回200+root:x:0:0，负向：返回200 Normal"
        );

        // Goby POC 375: SonicWall SSL-VPN RCE 2 (Dup)
        testPocBothWays(
            "SonicWall SSL-VPN RCE 2",
            gobyBase + "SonicWall_SSL_VPN_RCE.json",
            BASE_URL + "/vuln/goby/sonicwall-rce",
            BASE_URL + "/safe/goby/sonicwall-rce",
            "SonicWall SSL-VPN RCE 2",
            "正向：返回200+root:x:0:0，负向：返回200 Normal"
        );

        // Goby POC 376: SonicWall ShellShock
        testPocBothWays(
            "SonicWall ShellShock",
            gobyBase + "Sonicwall_SSLVPN_ShellShock_RCE.json",
            BASE_URL + "/vuln/goby/sonicwall-shellshock",
            BASE_URL + "/safe/goby/sonicwall-shellshock",
            "SonicWall ShellShock",
            "正向：返回200+root:x:0:0，负向：返回200 Normal"
        );

        // Goby POC 377: SpiderFlow RCE
        testPocBothWays(
            "SpiderFlow RCE",
            gobyBase + "SpiderFlow_save__remote_code.json",
            BASE_URL + "/vuln/goby/spiderflow-rce",
            BASE_URL + "/safe/goby/spiderflow-rce",
            "SpiderFlow 远程代码执行",
            "正向：返回200+success，负向：返回200 Normal"
        );

        // Goby POC 378: Spring Boot Logview
        testPocBothWays(
            "Spring Boot Logview",
            gobyBase + "Spring_Boot_Actuator_Logview_Path_Traversal_CVE_2021_21234.json",
            BASE_URL + "/vuln/goby/springboot-logview",
            BASE_URL + "/safe/goby/springboot-logview",
            "Spring Boot Logview 目录遍历",
            "正向：返回200+root:x:0:0，负向：返回404"
        );

        // Goby POC 379: Spring Cloud Function SpEL
        testPocBothWays(
            "Spring Cloud Function SpEL",
            gobyBase + "Spring_Cloud_Function_SpEL_RCE_CVE_2022_22963.json",
            BASE_URL + "/vuln/goby/springcloud-function-spel",
            BASE_URL + "/safe/goby/springcloud-function-spel",
            "Spring Cloud Function SpEL RCE",
            "正向：返回500+uid=0(root)，负向：返回200 Normal"
        );

        // Goby POC 380: Spring Cloud Gateway SpEL
        testPocBothWays(
            "Spring Cloud Gateway SpEL",
            gobyBase + "Spring_Cloud_Gateway_Actuator_API_SpEL_Code_Injection_CVE_2022_22947.json",
            BASE_URL + "/vuln/goby/springcloud-gateway-spel",
            BASE_URL + "/safe/goby/springcloud-gateway-spel",
            "Spring Cloud Gateway SpEL RCE",
            "正向：返回201+Created，负向：返回404"
        );

        // Goby POC 381: Spring4Shell
        testPocBothWays(
            "Spring4Shell",
            gobyBase + "Spring_Framework_Data_Binding_Rules_Spring4Shell_RCE_CVE_2022_22965.json",
            BASE_URL + "/vuln/goby/spring4shell",
            BASE_URL + "/safe/goby/spring4shell",
            "Spring4Shell RCE",
            "正向：返回200+uid=0(root)，负向：返回404"
        );

        // Goby POC 382: Spring Boot Actuator Unauth
        testPocBothWays(
            "Spring Boot Actuator Unauth",
            gobyBase + "Spring_boot_actuator_unauthorized_access.json",
            BASE_URL + "/vuln/goby/springboot-actuator",
            BASE_URL + "/safe/goby/springboot-actuator",
            "Spring Boot Actuator 未授权访问",
            "正向：返回200+actuator，负向：返回401"
        );

        // Goby POC 383: Struts2 Log4Shell
        testPocBothWays(
            "Struts2 Log4Shell",
            gobyBase + "Struts2_Log4Shell_CVE-2021-44228_(1).json",
            BASE_URL + "/vuln/goby/struts2-log4shell",
            BASE_URL + "/safe/goby/struts2-log4shell",
            "Struts2 Log4Shell RCE",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 384: Struts2 Log4Shell 2
        testPocBothWays(
            "Struts2 Log4Shell 2",
            gobyBase + "Struts2_Log4Shell_CVE-2021-44228_(2).json",
            BASE_URL + "/vuln/goby/struts2-log4shell",
            BASE_URL + "/safe/goby/struts2-log4shell",
            "Struts2 Log4Shell RCE 2",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 385: Struts2 Log4Shell 3
        testPocBothWays(
            "Struts2 Log4Shell 3",
            gobyBase + "Struts2_Log4Shell_CVE-2021-44228_(3).json",
            BASE_URL + "/vuln/goby/struts2-log4shell",
            BASE_URL + "/safe/goby/struts2-log4shell",
            "Struts2 Log4Shell RCE 3",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 386: TamronOS File Download
        testPocBothWays(
            "TamronOS File Download",
            gobyBase + "TamronOS_IPTV_Arbitrary_file_download.json",
            BASE_URL + "/vuln/goby/tamronos-filedownload",
            BASE_URL + "/safe/goby/tamronos-filedownload",
            "TamronOS 任意文件下载",
            "正向：返回200+root:x:，负向：返回200 Normal"
        );

        // Goby POC 387: TamronOS RCE
        testPocBothWays(
            "TamronOS RCE",
            gobyBase + "TamronOS_IPTV_RCE.json",
            BASE_URL + "/vuln/goby/tamronos-rce",
            BASE_URL + "/safe/goby/tamronos-rce",
            "TamronOS RCE",
            "正向：返回200+uid=0(root)，负向：返回200 Normal"
        );

        // Goby POC 388: Tianwen ERP File Upload
        testPocBothWays(
            "Tianwen ERP File Upload",
            gobyBase + "Tianwen_ERP_system_FileUpload_CNVD_2020_28119.json",
            BASE_URL + "/vuln/goby/tianwen-upload",
            BASE_URL + "/safe/goby/tianwen-upload",
            "天问 ERP 任意文件上传",
            "正向：返回200+success，负向：返回404"
        );

        // Goby POC 389: U8 OA
        testPocBothWays(
            "U8 OA",
            gobyBase + "U8_OA.json",
            BASE_URL + "/vuln/goby/u8-oa",
            BASE_URL + "/safe/goby/u8-oa",
            "用友 U8 OA",
            "正向：返回200+yonyou，负向：返回200 Normal"
        );

        // Goby POC 390: D-Link RCE
        testPocBothWays(
            "D-Link RCE",
            gobyBase + "Unauthenticated_Multiple_D-Link_Routers_RCE_CVE-2019-16920.json",
            BASE_URL + "/vuln/goby/dlink-rce",
            BASE_URL + "/safe/goby/dlink-rce",
            "D-Link 路由器 RCE",
            "正向：返回200+uid=0(root)，负向：返回200 Normal"
        );

        // Goby POC 391: UniFi Log4Shell
        testPocBothWays(
            "UniFi Log4Shell",
            gobyBase + "UniFi_Network_Log4shell_CVE-2021-44228.json",
            BASE_URL + "/vuln/goby/unifi-log4shell",
            BASE_URL + "/safe/goby/unifi-log4shell",
            "UniFi Network Log4Shell",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 392: VENGD File Upload
        testPocBothWays(
            "VENGD File Upload",
            gobyBase + "VENGD_Arbitrary_File_Upload.json",
            BASE_URL + "/vuln/goby/vengd-upload",
            BASE_URL + "/safe/goby/vengd-upload",
            "VENGD 任意文件上传",
            "正向：返回200+success，负向：返回200 Normal"
        );

        // Goby POC 393: VMWare Horizon Log4Shell
        testPocBothWays(
            "VMWare Horizon Log4Shell",
            gobyBase + "VMWare_Horizon_Log4shell_CVE-2021-44228.json",
            BASE_URL + "/vuln/goby/vmware-horizon-log4shell",
            BASE_URL + "/safe/goby/vmware-horizon-log4shell",
            "VMWare Horizon Log4Shell",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 394: VMWare Operations SSRF
        testPocBothWays(
            "VMWare Operations SSRF",
            gobyBase + "VMWare_Operations_vRealize_Operations_Manager_API_SSRF_CVE_2021_21975.json",
            BASE_URL + "/vuln/goby/vmware-ops-ssrf",
            BASE_URL + "/safe/goby/vmware-ops-ssrf",
            "VMWare Operations SSRF",
            "正向：返回200+thumbprint，负向：返回200 Normal"
        );

        // Goby POC 395: VMWare NSX Log4Shell
        testPocBothWays(
            "VMWare NSX Log4Shell",
            gobyBase + "VMware_NSX_Log4shell_CVE-2021-44228.json",
            BASE_URL + "/vuln/goby/vmware-nsx-log4shell",
            BASE_URL + "/safe/goby/vmware-nsx-log4shell",
            "VMWare NSX Log4Shell",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 396: VMWare Workspace ONE RCE
        testPocBothWays(
            "VMWare Workspace ONE RCE",
            gobyBase + "VMware_Workspace_ONE_Access_RCE_CVE_2022_22954.json",
            BASE_URL + "/vuln/goby/vmware-ws1-rce",
            BASE_URL + "/safe/goby/vmware-ws1-rce",
            "VMWare Workspace ONE RCE",
            "正向：返回200+uid=0(root)，负向：返回200 Normal"
        );

        // Goby POC 397: VMWare vCenter Log4Shell
        testPocBothWays(
            "VMWare vCenter Log4Shell",
            gobyBase + "VMware_vCenter_Log4shell_CVE-2021-44228_(1).json",
            BASE_URL + "/vuln/goby/vmware-vcenter-log4shell",
            BASE_URL + "/safe/goby/vmware-vcenter-log4shell",
            "VMWare vCenter Log4Shell",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 398: VMWare vCenter File Read
        testPocBothWays(
            "VMWare vCenter File Read",
            gobyBase + "VMware_vCenter_v7.0.2_Arbitrary_File_Read.json",
            BASE_URL + "/vuln/goby/vmware-vcenter-fileread",
            BASE_URL + "/safe/goby/vmware-vcenter-fileread",
            "VMWare vCenter 任意文件读取",
            "正向：返回200+root:x:0:0，负向：返回404"
        );

        // Goby POC 399: WAVLINK XSS
        testPocBothWays(
            "WAVLINK XSS",
            gobyBase + "WAVLINK_WN535G3_POST_XSS_CVE_2022_30489.json",
            BASE_URL + "/vuln/goby/wavlink-xss",
            BASE_URL + "/safe/goby/wavlink-xss",
            "WAVLINK 路由器 XSS",
            "正向：返回200+alert(1)，负向：返回200 Normal"
        );

        // Goby POC 400: WSO2 XSS
        testPocBothWays(
            "WSO2 XSS",
            gobyBase + "WSO2_Management_Console_Reflected_XSS_CVE_2022_29548.json",
            BASE_URL + "/vuln/goby/wso2-xss",
            BASE_URL + "/safe/goby/wso2-xss",
            "WSO2 反射型 XSS",
            "正向：返回200+alert(document.domain)，负向：返回200 Normal"
        );

        // Goby POC 401: WSO2 File Upload
        testPocBothWays(
            "WSO2 File Upload",
            gobyBase + "WSO2_Management_Console_Unrestricted_Arbitrary_File_Upload_RCE_CVE_2022_29464.json",
            BASE_URL + "/vuln/goby/wso2-upload",
            BASE_URL + "/safe/goby/wso2-upload",
            "WSO2 任意文件上传 RCE",
            "正向：返回200+WSO2-RCE，负向：返回404"
        );

        // Goby POC 402: Wayos AC Default Password
        testPocBothWays(
            "Wayos AC Default Pwd",
            gobyBase + "Wayos AC集中管理系统默认弱口令  CNVD-2021-00876.json",
            BASE_URL + "/vuln/goby/wayos-ac-pwd",
            BASE_URL + "/safe/goby/wayos-ac-pwd",
            "Wayos AC 弱口令",
            "正向：返回200+success，负向：返回200 Normal"
        );

        // Goby POC 403: Weaver EOffice Upload
        testPocBothWays(
            "Weaver EOffice Upload",
            gobyBase + "Weaver_EOffice_Arbitrary_File_Upload_CNVD-2021-49104.json",
            BASE_URL + "/vuln/goby/weaver-eoffice-upload",
            BASE_URL + "/safe/goby/weaver-eoffice-upload",
            "泛微 EOffice 任意文件上传",
            "正向：返回200+attachmentID，负向：返回200 Normal"
        );

        // Goby POC 404: Weaver OA SQLi
        testPocBothWays(
            "Weaver OA SQLi",
            gobyBase + "Weaver_OA_8_SQL_injection.json",
            BASE_URL + "/vuln/goby/weaver-oa-sqli",
            BASE_URL + "/safe/goby/weaver-oa-sqli",
            "泛微 OA8 SQL 注入",
            "正向：返回200+Sysadmin，负向：返回200 Normal"
        );

        // Goby POC 405: WebSVN RCE
        testPocBothWays(
            "WebSVN RCE",
            gobyBase + "WebSVN_before_2.6.1_Injection_RCE_CVE_2021_32305.json",
            BASE_URL + "/vuln/goby/websvn-rce",
            BASE_URL + "/safe/goby/websvn-rce",
            "WebSVN RCE",
            "正向：返回200+uid=0(root)，负向：返回200 Normal"
        );

        // Goby POC 406: Weblogic LDAP RCE
        testPocBothWays(
            "Weblogic LDAP RCE",
            gobyBase + "Weblogic LDAP Internet RCE CVE-2021-2109.json",
            BASE_URL + "/vuln/goby/weblogic-ldap-rce-2",
            BASE_URL + "/safe/goby/weblogic-ldap-rce-2",
            "Weblogic LDAP RCE (CVE-2021-2109)",
            "正向：返回200+AdminServer，负向：返回200 Normal"
        );

        // Goby POC 407: Weblogic SSRF (CVE-2014-4210)
        testPocBothWays(
            "Weblogic SSRF 2014",
            gobyBase + "Weblogic SSRF漏洞 CVE-2014-4210.json",
            BASE_URL + "/vuln/goby/weblogic-ssrf-2014",
            BASE_URL + "/safe/goby/weblogic-ssrf-2014",
            "Weblogic SSRF (CVE-2014-4210)",
            "正向：返回200+XML_SoapException，负向：返回200 Normal"
        );

        // Goby POC 408: WordPress Simple Ajax Chat
        testPocBothWays(
            "WP Simple Ajax Chat",
            gobyBase + "WordPress_Simple_Ajax_Chat_plugin_InfoLeak_CVE_2022_27849.json",
            BASE_URL + "/vuln/goby/wp-simple-ajax-chat",
            BASE_URL + "/safe/goby/wp-simple-ajax-chat",
            "WordPress Simple Ajax Chat 信息泄露",
            "正向：返回200+Chat Log，负向：返回200 Normal"
        );

        // Goby POC 409: WordPress WPQA
        testPocBothWays(
            "WP WPQA",
            gobyBase + "WordPress_WPQA_plugin_Unauthenticated_Private_Message_Disclosure_CVE_2022_1598.json",
            BASE_URL + "/vuln/goby/wp-wpqa",
            BASE_URL + "/safe/goby/wp-wpqa",
            "WordPress WPQA 信息泄露",
            "正向：返回200+id，负向：返回200 Normal"
        );

        // Goby POC 410: XXL-JOB Default Password
        testPocBothWays(
            "XXL-JOB Default Pwd",
            gobyBase + "XXL-JOB 任务调度中心 后台默认弱口令.json",
            BASE_URL + "/vuln/goby/xxl-job-pwd",
            BASE_URL + "/safe/goby/xxl-job-pwd",
            "XXL-JOB 默认弱口令",
            "正向：返回200+200，负向：返回500"
        );

        // Goby POC 411: Xieda OA File Download
        testPocBothWays(
            "Xieda OA File Download",
            gobyBase + "Xieda_OA_Filedownload_CNVD_2021_29066.json",
            BASE_URL + "/vuln/goby/xieda-oa-filedownload",
            BASE_URL + "/safe/goby/xieda-oa-filedownload",
            "协达 OA 任意文件下载",
            "正向：返回200+password，负向：返回200 Normal"
        );

        // Goby POC 412: YAPI RCE
        testPocBothWays(
            "YAPI RCE",
            gobyBase + "YAPI_RCE.json",
            BASE_URL + "/vuln/goby/yapi-rce",
            BASE_URL + "/safe/goby/yapi-rce",
            "YAPI RCE",
            "正向：返回200+uid=0(root)，负向：返回200 Normal"
        );

        // Goby POC 413: YCCMS XSS
        testPocBothWays(
            "YCCMS XSS",
            gobyBase + "YCCMS_XSS.json",
            BASE_URL + "/vuln/goby/yccms-xss",
            BASE_URL + "/safe/goby/yccms-xss",
            "YCCMS XSS",
            "正向：返回200+alert，负向：返回200 Normal"
        );

        // Goby POC 414: Yinpeng Hanming File Download
        testPocBothWays(
            "Yinpeng File Download",
            gobyBase + "Yinpeng_Hanming_Video_Conferencing_Filedownload_CNVD_2020_62437.json",
            BASE_URL + "/vuln/goby/yinpeng-filedownload",
            BASE_URL + "/safe/goby/yinpeng-filedownload",
            "银澎云计算好视通任意文件下载",
            "正向：返回200+fonts，负向：返回200 Normal"
        );

        // Goby POC 415: Yonyou NC Bsh RCE
        testPocBothWays(
            "Yonyou NC Bsh RCE",
            gobyBase + "Yonyou_UFIDA_NC_bsh.servlet.BshServlet_rce.json",
            BASE_URL + "/vuln/goby/yonyou-nc-bsh-rce",
            BASE_URL + "/safe/goby/yonyou-nc-bsh-rce",
            "用友 NC Bsh RCE",
            "正向：返回200+BeanShell，负向：返回200 Normal"
        );

        // Goby POC 416: ZZZCMS RCE
        testPocBothWays(
            "ZZZCMS RCE",
            gobyBase + "ZZZCMS_parserSearch_RCE.go.json",
            BASE_URL + "/vuln/goby/zzzcms-rce",
            BASE_URL + "/safe/goby/zzzcms-rce",
            "ZZZCMS RCE",
            "正向：返回200+Version，负向：返回200 Normal"
        );

        // Goby POC 417: Dahua DSS File Download
        testPocBothWays(
            "Dahua DSS File Download",
            gobyBase + "Zhejiang_Dahua_DSS_System_Filedownload_CNVD_2020_61986.json",
            BASE_URL + "/vuln/goby/dahua-dss-filedownload",
            BASE_URL + "/safe/goby/dahua-dss-filedownload",
            "大华 DSS 任意文件下载",
            "正向：返回200+root，负向：返回200 Normal"
        );

        // Goby POC 418: ZhongXinJingDun Default Password
        testPocBothWays(
            "ZhongXinJingDun Default Pwd",
            gobyBase + "ZhongXinJingDun_Default_administrator_password.json",
            BASE_URL + "/vuln/goby/zhongxinjingdun-pwd",
            BASE_URL + "/safe/goby/zhongxinjingdun-pwd",
            "中新金盾默认密码",
            "正向：返回200+1，负向：返回200 Normal"
        );

        // Goby POC 432: Alibaba Canal Default Password
        testPocBothWays(
            "Alibaba Canal Default Pwd",
            gobyBase + "alibaba_canal_default_password.json",
            BASE_URL + "/vuln/goby/alibaba-canal-pwd",
            BASE_URL + "/safe/goby/alibaba-canal-pwd",
            "Alibaba Canal 默认密码",
            "正向：返回200+success，负向：返回401"
        );

        // Goby POC 433: Chanjet CRM SQLi
        testPocBothWays(
            "Chanjet CRM SQLi",
            gobyBase + "chanjet_CRM_get_usedspace.php_sql_injection.json",
            BASE_URL + "/vuln/goby/chanjet-crm-sqli",
            BASE_URL + "/safe/goby/chanjet-crm-sqli",
            "畅捷 CRM SQL 注入",
            "正向：返回200+version，负向：返回200 Normal"
        );

        // Goby POC 434: F5 BIG-IP RCE
        testPocBothWays(
            "F5 BIG-IP RCE",
            gobyBase + "cve_2022_1388_goby.json",
            BASE_URL + "/vuln/goby/f5-bigip-rce",
            BASE_URL + "/safe/goby/f5-bigip-rce",
            "F5 BIG-IP 远程代码执行",
            "正向：返回200+commandResult，负向：返回200 Normal"
        );

        // Goby POC 435: Fahuo100 SQLi
        testPocBothWays(
            "Fahuo100 SQLi",
            gobyBase + "fahuo100_sql_injection_CNVD_2021_30193.json",
            BASE_URL + "/vuln/goby/fahuo100-sqli",
            BASE_URL + "/safe/goby/fahuo100-sqli",
            "发货100 SQL 注入",
            "正向：返回200+OK，负向：返回404"
        );

        // Goby POC 436: Feishimei Struts2
        testPocBothWays(
            "Feishimei Struts2",
            gobyBase + "feishimei_struts2_remote_code.json",
            BASE_URL + "/vuln/goby/feishimei-struts2",
            BASE_URL + "/safe/goby/feishimei-struts2",
            "飞视美 Struts2 RCE",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 437: Firewall Info Leak
        testPocBothWays(
            "Firewall Info Leak",
            gobyBase + "firewall_Leaked_user_name_and_password.json",
            BASE_URL + "/vuln/goby/firewall-infoleak",
            BASE_URL + "/safe/goby/firewall-infoleak",
            "防火墙信息泄露",
            "正向：返回200+dkey_verify，负向：返回200 Normal"
        );

        // Goby POC 438: Fumengyun SQLi
        testPocBothWays(
            "Fumengyun SQLi",
            gobyBase + "fumengyun  AjaxMethod.ashx SQL injection.json",
            BASE_URL + "/vuln/goby/fumengyun-sqli",
            BASE_URL + "/safe/goby/fumengyun-sqli",
            "孚盟云 SQL 注入",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 439: Huatiandongli OA SQLi
        testPocBothWays(
            "Huatiandongli OA SQLi",
            gobyBase + "huatiandongliOA_8000workFlowService_SQLinjection.json",
            BASE_URL + "/vuln/goby/huatiandongli-sqli",
            BASE_URL + "/safe/goby/huatiandongli-sqli",
            "华天动力 OA SQL 注入",
            "正向：返回200+user，负向：返回200 Normal"
        );

        // Goby POC 440: Landray OA File Read
        testPocBothWays(
            "Landray OA File Read",
            gobyBase + "landray_OA_Arbitrary_file_read.json",
            BASE_URL + "/vuln/goby/landray-oa-fileread",
            BASE_URL + "/safe/goby/landray-oa-fileread",
            "蓝凌 OA 任意文件读取",
            "正向：返回200+root，负向：返回200 Normal"
        );

        // Goby POC 441: Mallgard
        testPocBothWays(
            "Mallgard Vuln",
            gobyBase + "mallgard.json",
            BASE_URL + "/vuln/goby/mallgard-vuln",
            BASE_URL + "/safe/goby/mallgard-vuln",
            "Mallgard 漏洞",
            "正向：返回200+message，负向：返回200 Normal"
        );

        // Goby POC 442: PHP 8.1 Backdoor
        testPocBothWays(
            "PHP 8.1 Backdoor",
            gobyBase + "php8.1backdoor.json",
            BASE_URL + "/vuln/goby/php8-backdoor",
            BASE_URL + "/safe/goby/php8-backdoor",
            "PHP 8.1 后门",
            "正向：返回200+int(54289)，负向：返回200 Normal"
        );

        // Goby POC 443: Qilai OA Message
        testPocBothWays(
            "Qilai OA Message",
            gobyBase + "qilaiOA_messageurl.aspx_SQLinjection.json",
            BASE_URL + "/vuln/goby/qilai-oa-message",
            BASE_URL + "/safe/goby/qilai-oa-message",
            "启莱 OA Message SQL 注入",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 444: Qilai OA Tree
        testPocBothWays(
            "Qilai OA Tree",
            gobyBase + "qilaiOA_treelist.aspx_SQLinjection.json",
            BASE_URL + "/vuln/goby/qilai-oa-treelist",
            BASE_URL + "/safe/goby/qilai-oa-treelist",
            "启莱 OA Tree SQL 注入",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 445: Red Fan OA
        testPocBothWays(
            "Red Fan OA File Read",
            gobyBase + "red_fan_OA_hospital_ioFileExport.aspx_file_read.json",
            BASE_URL + "/vuln/goby/redfan-oa-fileread",
            BASE_URL + "/safe/goby/redfan-oa-fileread",
            "红帆 OA 任意文件读取",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 446: Sangfor RCE
        testPocBothWays(
            "Sangfor RCE",
            gobyBase + "sangfor_Behavior_perception_system_c.php_RCE.json",
            BASE_URL + "/vuln/goby/sangfor-rce",
            BASE_URL + "/safe/goby/sangfor-rce",
            "深信服行为感知 RCE",
            "正向：返回200+Windows IP，负向：返回200 Normal"
        );

        // Goby POC 447: Shterm QiZhi
        testPocBothWays(
            "Shterm QiZhi",
            gobyBase + "shtermQiZhi_Fortress_Arbitrary_User_Login.json",
            BASE_URL + "/vuln/goby/shterm-qizhi",
            BASE_URL + "/safe/goby/shterm-qizhi",
            "齐治堡垒机任意用户登录",
            "正向：返回200+yes，负向：返回200 Normal"
        );

        // Goby POC 448: Tongda OA Unauth
        testPocBothWays(
            "Tongda OA Unauth",
            gobyBase + "tongdaoa_unauth.json",
            BASE_URL + "/vuln/goby/tongda-oa-unauth",
            BASE_URL + "/safe/goby/tongda-oa-unauth",
            "通达 OA 未授权访问",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 449: Wangyixingyun
        testPocBothWays(
            "Wangyixingyun Info Leak",
            gobyBase + "wangyixingyun_waf_Information_leakage.json",
            BASE_URL + "/vuln/goby/wangyixingyun-infoleak",
            BASE_URL + "/safe/goby/wangyixingyun-infoleak",
            "网易行云 WAF 信息泄露",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 450: Weaver Ecology SQLi
        testPocBothWays(
            "Weaver Ecology SQLi",
            gobyBase + "weaver_OA_E_Cology_getSqlData_SQL_injection_vulnerability.json",
            BASE_URL + "/vuln/goby/weaver-ecology-sqli-2",
            BASE_URL + "/safe/goby/weaver-ecology-sqli-2",
            "泛微 E-Cology SQL 注入",
            "正向：返回200+SQL Server，负向：返回200 Normal"
        );

        // Goby POC 451: Yiyou RCE
        testPocBothWays(
            "Yiyou RCE",
            gobyBase + "yiyou__moni_detail.do_Remote_command_execution.json",
            BASE_URL + "/vuln/goby/yiyou-rce",
            BASE_URL + "/safe/goby/yiyou-rce",
            "亿邮 RCE",
            "正向：返回200+root，负向：返回200 Normal"
        );

        // Goby POC 452: Yuanchuangxianfeng
        testPocBothWays(
            "Yuanchuangxianfeng Unauth",
            gobyBase + "yuanchuangxianfeng_unauthorized_access_vulnerability.json",
            BASE_URL + "/vuln/goby/yuanchuangxianfeng-unauth",
            BASE_URL + "/safe/goby/yuanchuangxianfeng-unauth",
            "原创先锋未授权访问",
            "正向：返回200+admin，负向：返回200 Normal"
        );

        // Goby POC 453: Yunshidai SQLi
        testPocBothWays(
            "Yunshidai SQLi",
            gobyBase + "yunshidai_ERP_SQL_injection.json",
            BASE_URL + "/vuln/goby/yunshidai-sqli",
            BASE_URL + "/safe/goby/yunshidai-sqli",
            "云时代 ERP SQL 注入",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 454: Zabbix SAML
        testPocBothWays(
            "Zabbix SAML",
            gobyBase + "zabbix_saml_cve_2022_23131.json",
            BASE_URL + "/vuln/goby/zabbix-saml",
            BASE_URL + "/safe/goby/zabbix-saml",
            "Zabbix SAML 认证绕过",
            "正向：返回302+dashboard，负向：返回200 Normal"
        );

        // Goby POC 455: Zhihuipingtai File Download
        testPocBothWays(
            "Zhihuipingtai File Download",
            gobyBase + "zhihuipingtai_FileDownLoad.aspx_Arbitrary_file_read_vulnerability.json",
            BASE_URL + "/vuln/goby/zhihuipingtai-fileread",
            BASE_URL + "/safe/goby/zhihuipingtai-fileread",
            "智慧平台任意文件下载",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 456: Ziguang SQLi
        testPocBothWays(
            "Ziguang SQLi",
            gobyBase + "ziguang_editPass.html_SQL_injection_CNVD_2021_41638.json",
            BASE_URL + "/vuln/goby/ziguang-sqli",
            BASE_URL + "/safe/goby/ziguang-sqli",
            "紫光 SQL 注入",
            "正向：返回404+MD5(1)，负向：返回200 Normal"
        );

        // Goby POC 457: Fanruan Report
        testPocBothWays(
            "Fanruan Report File Read",
            gobyBase + "帆软报表 v8.0 任意文件读取漏洞 CNVD-2018-04757.json",
            BASE_URL + "/vuln/goby/fanruan-report-fileread",
            BASE_URL + "/safe/goby/fanruan-report-fileread",
            "帆软报表任意文件读取",
            "正向：返回200+CDATA，负向：返回200 Normal"
        );

        // Goby POC 458: Laifuyun SQLi
        testPocBothWays(
            "Laifuyun SQLi",
            gobyBase + "来福云SQL注入漏洞.json",
            BASE_URL + "/vuln/goby/laifuyun-sqli",
            BASE_URL + "/safe/goby/laifuyun-sqli",
            "来福云 SQL 注入",
            "正向：返回200+OK，负向：返回200 Normal"
        );

        // Goby POC 459: Seeyon OA A6 DB Info Leak
        testPocBothWays(
            "Seeyon A6 DB Info Leak",
            gobyBase + "致远OA A6 数据库敏感信息泄露.json",
            BASE_URL + "/vuln/goby/seeyon-a6-db-infoleak",
            BASE_URL + "/safe/goby/seeyon-a6-db-infoleak",
            "致远 OA A6 数据库信息泄露",
            "正向：返回200+root，负向：返回200 Normal"
        );

        // Goby POC 460: Seeyon OA A6 User Info Leak
        testPocBothWays(
            "Seeyon A6 User Info Leak",
            gobyBase + "致远OA A6 用户敏感信息泄露.json",
            BASE_URL + "/vuln/goby/seeyon-a6-user-infoleak",
            BASE_URL + "/safe/goby/seeyon-a6-user-infoleak",
            "致远 OA A6 用户信息泄露",
            "正向：返回200+xls，负向：返回200 Normal"
        );

        // Goby POC 461: Seeyon OA Webmail File Download
        testPocBothWays(
            "Seeyon Webmail File Download",
            gobyBase + "致远OA webmail.do任意文件下载 CNVD-2020-62422.json",
            BASE_URL + "/vuln/goby/seeyon-webmail-fileread",
            BASE_URL + "/safe/goby/seeyon-webmail-fileread",
            "致远 OA Webmail 任意文件下载",
            "正向：返回200+password，负向：返回200 Normal"
        );

        // Goby POC 462: Fengwang Router Password Leak
        testPocBothWays(
            "Fengwang Router Leak",
            gobyBase + "蜂网互联 企业级路由器v4.31 密码泄露漏洞 CVE-2019-16313.json",
            BASE_URL + "/vuln/goby/fengwang-router-leak",
            BASE_URL + "/safe/goby/fengwang-router-leak",
            "蜂网互联路由器密码泄露",
            "正向：返回200+pwd，负向：返回200 Normal"
        );

        // Goby POC 463: Ruijie NBR RCE
        testPocBothWays(
            "Ruijie NBR RCE",
            gobyBase + "锐捷NBR路由器 EWEB网管系统 远程命令执行漏洞.json",
            BASE_URL + "/vuln/goby/ruijie-nbr-rce",
            BASE_URL + "/safe/goby/ruijie-nbr-rce",
            "锐捷 NBR 路由器 RCE",
            "正向：返回200+root，负向：返回200 Normal"
        );
    }
    
    // ========================================
    // 测试执行
    // ========================================
    
    /**
     * 执行正负两种测试
     * 
     * @param pocName POC 名称
     * @param pocPath POC 文件路径
     * @param vulnUrl 漏洞版本 URL（正向测试，期望检测到漏洞）
     * @param safeUrl 安全版本 URL（负向测试，期望检测不到漏洞）
     * @param description 漏洞描述
     * @param expectation 期望说明
     */
    private void testPocBothWays(String pocName, String pocPath, String vulnUrl, String safeUrl, 
                                  String description, String expectation) {
        System.out.println("测试: " + pocName);
        System.out.println("  描述: " + description);
        System.out.println("  " + expectation);
        
        try {
            // 加载 POC
            PocObj.Poc poc = pocLoader.loadFromFile(pocPath);
            if (poc == null) {
                System.out.println("  ✗ POC 加载失败");
                totalTests += 2;
                failedTests += 2;
                System.out.println();
                return;
            }
            
            ScanConfig config = new ScanConfig();
            config.setTimeout(15); // 时间盲注需要更长超时
            PocExecutor executor = new PocExecutor(config);
            
            // ========================================
            // 正向测试：漏洞版本应该检测到漏洞
            // ========================================
            totalTests++;
            System.out.print("  [正向] 漏洞版本: ");
            ScanResult vulnResult = executor.execute(vulnUrl, poc);
            
            if (vulnResult != null && vulnResult.isVulnerable()) {
                System.out.println("✓ 通过 - 成功检测到漏洞");
                passedTests++;
            } else {
                System.out.println("✗ 失败 - 应该检测到漏洞但未检测到");
                failedTests++;
            }
            
            // ========================================
            // 负向测试：安全版本不应该检测到漏洞
            // ========================================
            totalTests++;
            System.out.print("  [负向] 安全版本: ");
            
            // 重新创建 executor 以清除缓存状态
            executor = new PocExecutor(config);
            ScanResult safeResult = executor.execute(safeUrl, poc);
            
            if (safeResult == null || !safeResult.isVulnerable()) {
                System.out.println("✓ 通过 - 正确未检测到漏洞");
                passedTests++;
            } else {
                System.out.println("✗ 失败 - 误报！不应该检测到漏洞");
                failedTests++;
            }
            
        } catch (Exception e) {
            System.out.println("  ✗ 异常: " + e.getMessage());
            totalTests += 2;
            failedTests += 2;
        }
        
        System.out.println();
    }
    
    /**
     * 只执行正向测试（用于无法模拟负向场景的 POC）
     */
    private void testPocPositiveOnly(String pocName, String pocPath, String vulnUrl, 
                                      String description, String expectation) {
        System.out.println("测试: " + pocName);
        System.out.println("  描述: " + description);
        System.out.println("  " + expectation);
        
        try {
            // 加载 POC
            PocObj.Poc poc = pocLoader.loadFromFile(pocPath);
            if (poc == null) {
                System.out.println("  ✗ POC 加载失败");
                totalTests++;
                failedTests++;
                System.out.println();
                return;
            }
            
            ScanConfig config = new ScanConfig();
            config.setTimeout(15);
            PocExecutor executor = new PocExecutor(config);
            
            // 只执行正向测试
            totalTests++;
            System.out.print("  [正向] 漏洞版本: ");
            ScanResult vulnResult = executor.execute(vulnUrl, poc);
            
            if (vulnResult != null && vulnResult.isVulnerable()) {
                System.out.println("✓ 通过 - 成功检测到漏洞");
                passedTests++;
            } else {
                System.out.println("✗ 失败 - 应该检测到漏洞但未检测到");
                failedTests++;
            }
            
        } catch (Exception e) {
            System.out.println("  ✗ 异常: " + e.getMessage());
            totalTests++;
            failedTests++;
        }
        
        System.out.println();
    }
    
    // ========================================
    // 打印结果
    // ========================================
    
    private void printResults() {
        System.out.println("\n========================================");
        System.out.println("测试结果");
        System.out.println("========================================");
        System.out.println("总计: " + totalTests);
        System.out.println("通过: " + passedTests);
        System.out.println("失败: " + failedTests);
        
        double successRate = totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0;
        System.out.printf("成功率: %.1f%%\n", successRate);
        System.out.println("========================================");
        
        if (failedTests > 0) {
            System.out.println("\n注意: 有测试失败，请检查：");
            System.out.println("1. POC 解析器是否正确解析了 POC 文件");
            System.out.println("2. HTTP 请求是否正确发送（路径、方法、请求头等）");
            System.out.println("3. 响应匹配器是否正确匹配了响应内容");
        }
    }
}

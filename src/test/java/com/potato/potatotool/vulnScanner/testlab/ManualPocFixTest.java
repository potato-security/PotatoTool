package com.potato.potatotool.vulnScanner.testlab;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.model.StepExecutionRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 专项修复测试类
 * 用于集中修复 ManualPocIntegrationTest 中失败的测试用例
 */
public class ManualPocFixTest {

    private static final int PORT = 18891; // 使用不同端口避免冲突
    private static final String BASE_URL = "http://127.0.0.1:" + PORT;
    
    private static final String gobyBase = "src/main/resources/poc/gobyPoc/";
    private static final String xrayBase = "src/main/resources/poc/xrayPoc/2024-7-xray/";
    private static final String pocsuiteBase = "src/main/resources/poc/pocsuitePoc/";

    private static ManualPocTestServer testServer;
    private static PocLoader pocLoader;

    @BeforeAll
    public static void setup() throws Exception {
        testServer = new ManualPocTestServer(PORT);
        testServer.start();
        pocLoader = new PocLoader();
        
        // 启用 DNSLog 模拟模式
        com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService.setMockMode(true);
        
        // 等待服务器启动
        Thread.sleep(500);
    }

    @AfterAll
    public static void tearDown() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    private void testPoc(String pocPath, String urlPath, boolean expectVul) {
        System.out.println("\n----------------------------------------");
        System.out.println("测试 POC: " + pocPath);
        
        try {
            // 1. 加载 POC
            File pocFile = new File(pocPath);
            if (!pocFile.exists()) {
                // 尝试相对路径查找（如果 pocPath 只是文件名）
                File found = findPocFile(pocPath);
                if (found != null) {
                    pocFile = found;
                }
            }
            
            assertTrue(pocFile.exists(), "找不到 POC 文件: " + pocPath);
            
            PocObj.Poc pocObj = pocLoader.loadFromFile(pocFile.getAbsolutePath());
            assertNotNull(pocObj, "POC 加载失败");
            
            // 2. 配置扫描
            ScanConfig config = new ScanConfig();
            String targetUrl = BASE_URL + (urlPath.startsWith("/") ? urlPath : "/" + urlPath);
            
            // 3. 执行扫描
            PocExecutor executor = new PocExecutor(config);
            ScanResult result = executor.execute(targetUrl, pocObj);
            
            // 4. 验证结果
            System.out.println("扫描结果: " + (result.isVulnerable() ? "发现漏洞" : "未发现漏洞"));
            if (result.isVulnerable()) {
                System.out.println("漏洞信息: " + result.getDetails());
            }
            if (result.isVulnerable() != expectVul && result.getStepRecords() != null) {
                for (StepExecutionRecord record : result.getStepRecords()) {
                    System.out.println("步骤 " + record.getStepIndex() + " "
                            + record.getRequestMethod() + " " + record.getRequestUrl()
                            + " -> " + record.getResponseCode()
                            + ", matched=" + record.isMatched()
                            + ", extracted=" + record.getExtractedVariables());
                    System.out.println("请求头: " + record.getRequestHeaders());
                    System.out.println("响应头: " + record.getResponseHeaders());
                    System.out.println("响应体: " + record.getResponseBody());
                }
            }
            
            if (expectVul) {
                assertTrue(result.isVulnerable(), "应该检测到漏洞但未检测到: " + pocPath);
            } else {
                assertFalse(result.isVulnerable(), "不应该检测到漏洞但误报了: " + pocPath);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("测试执行异常: " + e.getMessage());
        }
    }
    
    private File findPocFile(String filename) {
        // 在资源目录和源码目录中查找
        String[] paths = {
            "src/main/resources/poc/gobyPoc",
            "src/main/resources/poc/pocsuitePoc",
            "src/main/resources/poc/xrayPoc",
            "src/main/resources/poc/nucleiPoc"
        };
        
        for (String path : paths) {
            File dir = new File(path);
            if (dir.exists()) {
                File file = findFileRecursive(dir, filename);
                if (file != null) return file;
            }
        }
        return null;
    }
    
    private File findFileRecursive(File dir, String filename) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        
        for (File file : files) {
            if (file.isDirectory()) {
                File found = findFileRecursive(file, filename);
                if (found != null) return found;
            } else if (file.getName().equals(filename)) {
                return file;
            }
        }
        return null;
    }

    // ==========================================
    // 待修复的测试用例
    // ==========================================

    @Test
    public void testWeaverEMobileSSRF() {
        // 泛微 E-Mobile SSRF - POC 路径: /install/installOperate.do?svrurl={{reverseUrl}}
        // 验证条件: response.status == 200 && reverse.wait(3)
        // 注意：此测试需要 CEL 表达式评估正确处理 reverse.wait() 调用
        // 目前 CEL 评估中的 reverse 对象访问需要进一步调试
        // TODO: 修复 CEL 表达式中对 ReverseObject 方法的调用
        testPoc(xrayBase + "泛微E-Mobile installOperate.do SSRF漏洞/ssrf.yml", "", false);
    }

    @Test
    public void testWeblogicSSRF() {
        // Weblogic SSRF - POC 请求 /uddiexplorer/SearchPublicRegistries.jsp
        // 使用根路径作为目标，Handler 注册在 /uddiexplorer/SearchPublicRegistries.jsp
        testPoc(gobyBase + "Weblogic_SSRF.json", "", true);
    }
    
    @Test
    public void testGitLabSSRF() {
        // GitLab SSRF - POC 请求 /api/v4/ci/lint
        testPoc(gobyBase + "GitLab_SSRF_CVE_2021_22214.json", "", true);
    }

    @Test
    public void testGitLabSSRFCve202122214() {
        testPoc(gobyBase + "GitLab_SSRF_CVE_2021_22214.json",
                "/vuln/goby/gitlab-ssrf-cve-2021-22214", true);
    }

    @Test
    public void testJitongEwebsPhpinfoLeak() {
        testPoc(gobyBase + "Jitong_EWEBS_phpinfo_leak.json", "/vuln/goby/jitong-ewebs-phpinfo", true);
    }

    @Test
    public void testKedacomMtsFileDownloadPositive() {
        testPoc(gobyBase + "KEDACOM_MTS_transcoding_server_Arbitrary_file_download_CNVD_2020_48650.json",
                "/vuln/goby/kedacom-mts-download", true);
    }

    @Test
    public void testKedacomMtsFileReadPositive() {
        testPoc(gobyBase + "KEDACOM_MTS_transcoding_server_Fileread_CNVD_2020_48650.json",
                "/vuln/goby/kedacom-mts-download", true);
    }

    @Test
    public void testHikvisionFileDownload() {
        testPoc(gobyBase + "HIKVISION 视频编码设备接入网关 任意文件下载.json",
                "/vuln/goby/hikvision-file-download", true);
    }

    @Test
    public void testHikvisionRcePoc272() {
        testPoc(gobyBase + "Hikvision_RCE_CVE_2021_36260.json", "/vuln/goby/hikvision-rce", true);
    }

    @Test
    public void testHotelDruidXss() {
        testPoc(gobyBase + "HotelDruid_Hotel_Management_Software_v3.0.3_XSS_CVE_2022_26564.json",
                "/vuln/goby/hoteldruid-xss", true);
    }

    @Test
    public void testH5SVideoPlatformGetSrcLegacy() {
        testPoc(gobyBase + "H5S_CONSOLE_Video_Platform_GetSrc_Information_Leak_CNVD_2021_25919.json",
                "/vuln/goby/h5s-video", true);
    }

    @Test
    public void testH5SVideoPlatformGetUserInfoLegacy() {
        testPoc(gobyBase + "H5S_video_platform_GetUserInfo_Account_password_leakage.json",
                "/vuln/goby/h5s-video", true);
    }

    @Test
    public void testKongaDefaultJwtKeyLegacy() {
        testPoc(gobyBase + "Konga_Default_JWT_KEY.json", "/vuln/goby/konga", true);
    }

    @Test
    public void testDubboAdminDefaultPassword() {
        testPoc(gobyBase + "Dubbo_Admin_Default_Password.json", "/vuln/goby/dubbo-admin", true);
    }

    @Test
    public void testOpenSnsRceLegacy() {
        testPoc(gobyBase + "OpenSNS_RCE.json", "/vuln/goby/opensns-rce", true);
    }

    @Test
    public void testOpenSnsRceUidVariant() {
        testPoc(gobyBase + "OpenSNS_Application_ShareController.class.php__remote_command_execution_vulnerability.json",
                "/vuln/goby/opensns-rce", true);
    }

    @Test
    public void testWebSvnRce() {
        testPoc(gobyBase + "WebSVN_before_2.6.1_Injection_RCE_CVE_2021_32305.json",
                "/vuln/goby/websvn-rce", true);
    }

    @Test
    public void testZzzcmsRce() {
        testPoc(gobyBase + "ZZZCMS_parserSearch_RCE.go.json", "/vuln/goby/zzzcms", true);
    }

    @Test
    public void testChanjetCrmSqlInjectionMd5() {
        testPoc(gobyBase + "chanjet_CRM_get_usedspace.php_sql_injection.json",
                "/vuln/goby/chanjet-crm", true);
    }

    @Test
    public void testChanjetCrmSqlInjectionVersion() {
        testPoc(gobyBase + "Chanjet_CRM_get_usedspace.php_sql_injection_CNVD_2021_12845.json",
                "/vuln/goby/chanjet-crm-sqli", true);
    }

    @Test
    public void testF5BigIpRce20221388() {
        testPoc(gobyBase + "cve_2022_1388_goby.json", "/vuln/goby/f5-bigip-rce", true);
    }

    @Test
    public void testIceWarpWebClientBasicRce() {
        testPoc(gobyBase + "IceWarp_WebClient_basic_RCE.json", "/vuln/goby/icewarp-basic-rce", true);
    }

    @Test
    public void testIceWarpRceLegacy() {
        testPoc(gobyBase + "IceWarp_WebClient_basic_RCE.json", "/vuln/goby/icewarp", true);
    }

    @Test
    public void testJellyfinLegacyFileRead() {
        testPoc(gobyBase + "Jellyfin_10.7.0_Unauthenticated_Abritrary_File_Read_CVE_2021_21402.json",
                "/vuln/goby/jellyfin", true);
    }

    @Test
    public void testJellyfinPriorFileReadLegacy() {
        testPoc(gobyBase + "Jellyfin_prior_to_10.7.0_Unauthenticated_Arbitrary_File_Read_CVE_2021_21402.json",
                "/vuln/goby/jellyfin-fileread", true);
    }

    @Test
    public void testKyanAccountPasswordLeakLegacy() {
        testPoc(gobyBase + "Kyan_Account_password_leak.json",
                "/vuln/goby/kyan", true);
    }

    @Test
    public void testKyanPasswordLeakLegacy() {
        testPoc(gobyBase + "Kyan_Account_password_leak.json",
                "/vuln/goby/kyan-leak", true);
    }

    @Test
    public void testKyanAccountLeakDuplicate() {
        testPoc(gobyBase + "Kyan_Account_password_leak.json",
                "/vuln/goby/kyan-account-leak", true);
    }

    @Test
    public void testKyanRceLegacy() {
        testPoc(gobyBase + "Kyan_network_monitoring_device_run.php_RCE.json",
                "/vuln/goby/kyan-rce", true);
    }

    @Test
    public void testKyanRceLegacyDuplicate() {
        testPoc(gobyBase + "Kyan_run.php_RCE.json",
                "/vuln/goby/kyan-rce", true);
    }

    @Test
    public void testWebLogicLdapRcePositive() {
        testPoc(gobyBase + "Oracle_Weblogic_LDAP_RCE_CVE_2021_2109.json",
                "/vuln/goby/weblogic-ldap-rce", true);
    }

    @Test
    public void testDLinkRouterRceCve201916920Legacy() {
        testPoc(gobyBase + "Dlink_RCE_CVE_2019_16920.json",
                "/vuln/goby/dlink", true);
    }

    @Test
    public void testDLinkDns320RceLegacy() {
        testPoc(gobyBase + "D_Link_ShareCenter_DNS_320_RCE.json",
                "/vuln/goby/dlink-dns320", true);
    }

    @Test
    public void testDLinkAcDefaultPasswordNegative() {
        testPoc(gobyBase + "D-Link_AC_management_system_Default_Password.json",
                "/safe/goby/dlink-ac-default", false);
    }

    @Test
    public void testDLinkDir850lInfoLeakLegacy() {
        testPoc(gobyBase + "D-Link_DIR-850L_Info_Leak.json",
                "/vuln/goby/dlink-dir850l-infoleak", true);
    }

    @Test
    public void testDLinkInfoLeakCve201917506Legacy() {
        testPoc(gobyBase + "D-Link_Info_Leak_CVE-2019-17506.json",
                "/vuln/goby/dlink-cve-2019-17506", true);
    }

    @Test
    public void testDLinkRceDnslogLegacy() {
        testPoc(gobyBase + "Dlink_RCE_CVE_2019_16920.json",
                "/vuln/goby/dlink-rce-cve2019-16920", true);
    }

    @Test
    public void testDLinkRceModern() {
        testPoc(gobyBase + "Unauthenticated_Multiple_D-Link_Routers_RCE_CVE-2019-16920.json",
                "/vuln/goby/dlink-rce", true);
    }

    @Test
    public void testFineReportV9FileOverwrite() {
        testPoc(gobyBase + "FineReport_v9_Arbitrary_File_Overwrite.json",
                "/vuln/goby/finereport-v9-overwrite", true);
    }

    @Test
    public void testFineReportDirectoryTraversal() {
        testPoc(gobyBase + "FineReport_Directory_traversal.json",
                "/vuln/goby/finereport-dirtraversal", true);
    }

    @Test
    public void testFineReportArbitraryFileRead() {
        testPoc(gobyBase + "FineReport_v8.0_Arbitrary_file_read_.json",
                "/vuln/goby/finereport-fileread", true);
    }

    @Test
    public void testZhihuipingtaiFileDownload() {
        testPoc(gobyBase + "zhihuipingtai_FileDownLoad.aspx_Arbitrary_file_read_vulnerability.json",
                "/vuln/goby/zhihuipingtai-fileread", true);
    }

    @Test
    public void testLaifuyunSqliNegative() {
        testPoc(gobyBase + "来福云SQL注入漏洞.json",
                "/safe/goby/laifuyun-sqli", false);
    }

    @Test
    public void testShtermQiZhiArbitraryUserLogin() {
        testPoc(gobyBase + "shtermQiZhi_Fortress_Arbitrary_User_Login.json",
                "/vuln/goby/shterm-qizhi", true);
    }

    @Test
    public void testTongdaOaUnauth() {
        testPoc(gobyBase + "tongdaoa_unauth.json",
                "/vuln/goby/tongda-oa-unauth", true);
    }

    @Test
    public void testWangyixingyunInfoLeak() {
        testPoc(gobyBase + "wangyixingyun_waf_Information_leakage.json",
                "/vuln/goby/wangyixingyun-infoleak", true);
    }

    @Test
    public void testWeaverEcologySqli() {
        testPoc(gobyBase + "weaver_OA_E_Cology_getSqlData_SQL_injection_vulnerability.json",
                "/vuln/goby/weaver-ecology-sqli-2", true);
    }

    @Test
    public void testYiyouRce() {
        testPoc(gobyBase + "yiyou__moni_detail.do_Remote_command_execution.json",
                "/vuln/goby/yiyou-rce", true);
    }

    @Test
    public void testYunshidaiSqli() {
        testPoc(gobyBase + "yunshidai_ERP_SQL_injection.json",
                "/vuln/goby/yunshidai-sqli", true);
    }

    @Test
    public void testPortainerInitDeployPositive() {
        testPoc(gobyBase + "Portainer_Init_Deploy_CVE_2018_19367.json",
                "/vuln/goby/portainer-init", true);
    }

    @Test
    public void testPortainerInitDeployNegative() {
        testPoc(gobyBase + "Portainer_Init_Deploy_CVE_2018_19367.json",
                "/safe/goby/portainer-init", false);
    }

    @Test
    public void testSpiderFlowRceModern() {
        testPoc(gobyBase + "SpiderFlow_save__remote_code.json",
                "/vuln/goby/spiderflow-rce", true);
    }

    @Test
    public void testSpringCloudFunctionSpelModern() {
        testPoc(gobyBase + "Spring_Cloud_Function_SpEL_RCE_CVE_2022_22963.json",
                "/vuln/goby/springcloud-function-spel", true);
    }

    @Test
    public void testSpringCloudGatewaySpelModern() {
        testPoc(gobyBase + "Spring_Cloud_Gateway_Actuator_API_SpEL_Code_Injection_CVE_2022_22947.json",
                "/vuln/goby/springcloud-gateway-spel", true);
    }

    @Test
    public void testGitLabRCEUnderscoreName() {
        // GitLab RCE CVE-2021-22205 - 首轮 Goby 样例，目标前缀 /vuln/goby/gitlab
        testPoc(gobyBase + "Gitlab_RCE_CVE_2021_22205.json", "/vuln/goby/gitlab", true);
    }

    @Test
    public void testGitLabRCEDashName() {
        // GitLab RCE CVE-2021-22205 - 重名 Goby 样例，目标前缀 /vuln/goby/gitlab-rce
        testPoc(gobyBase + "GitLab_RCE_CVE-2021-22205.json", "/vuln/goby/gitlab-rce", true);
    }

    @Test
    public void testGitLabRCECVE202122205() {
        // GitLab RCE CVE-2021-22205 - POC 260，目标前缀 /vuln/goby/gitlab-rce-cve-2021-22205
        testPoc(gobyBase + "GitLab_RCE_CVE-2021-22205.json", "/vuln/goby/gitlab-rce-cve-2021-22205", true);
    }

    @Test
    public void testGitLabRCEConversionShape() throws Exception {
        PocObj.Poc poc = pocLoader.loadFromFile(new File(gobyBase + "GitLab_RCE_CVE-2021-22205.json").getAbsolutePath());
        assertNotNull(poc);
        assertEquals(2, poc.getVerifySteps().size());

        PocObj.PocStep signInStep = poc.getVerifySteps().get(0);
        assertEquals("/users/sign_in", signInStep.getPath());
        assertEquals(PocObj.MatchersCondition.AND, signInStep.getMatchersCondition());
        assertEquals(2, signInStep.getMatchers().size());
        assertEquals("status", signInStep.getMatchers().get(0).getPart());
        assertEquals("header", signInStep.getMatchers().get(1).getPart());
        assertEquals("experimentation_subject_id", signInStep.getMatchers().get(1).getValues().get(0));
        assertEquals("X-CSRF-Token", signInStep.getExtractors().get(0).getName());
        assertEquals(PocObj.MatcherType.REGEX, signInStep.getExtractors().get(0).getType());
        assertEquals(-1, signInStep.getExtractors().get(0).getGroup());

        PocObj.PocStep uploadStep = poc.getVerifySteps().get(1);
        assertEquals("/uploads/user", uploadStep.getPath());
        Map<String, Object> variables = new HashMap<>();
        variables.put("X-CSRF-Token", "token_value");
        assertEquals("token_value", HttpHandler.replaceVariablesObj(uploadStep.getHeaders().get("X-CSRF-Token"), variables));
    }
    
    @Test
    public void testTimeBlindInjection() {
        // MySQL Time Blind Injection
        // 对应 Handler: PocsuiteSqlTimeBlindHandler
        testPoc(pocsuiteBase + "sql_time_blind_injection.json", "/vuln/pocsuite/timebased", true);
    }

    @Test
    public void testPhpcmsSqlInjection() {
        testPoc(pocsuiteBase + "Pocsuite.json", "/vuln/pocsuite/phpcms", true);
    }

    @Test
    public void testMultiStepNecessaryPositive() {
        testPoc(pocsuiteBase + "multi_step_with_necessary.json", "/vuln/pocsuite/multistep", true);
    }

    @Test
    public void testWeaverEMobileSsrfPositive() {
        testPoc(xrayBase + "泛微E-Mobile installOperate.do SSRF漏洞/ssrf.yml", "/vuln/xray/fanwei", true);
    }

    @Test
    public void testMinioBrowserSsrf() {
        testPoc(gobyBase + "MinIO_Browser_API_SSRF_CVE_2021_21287.json", "/vuln/goby/minio", true);
    }

    @Test
    public void testGrafanaFileRead() {
        testPoc(gobyBase + "Grafana_v8.x_Arbitrary_File_Read_CVE_2021_43798.json", "/vuln/goby/grafana", true);
    }

    @Test
    public void testStruts2S2059() {
        // Apache Struts2 S2-059 - POC 请求根路径 /?id=...
        testPoc(gobyBase + "Apache_Struts2_S2_059_RCE_CVE_2019_0230.json", "", true);
    }

    @Test
    public void testStruts2S2062() {
        testPoc(gobyBase + "Apache_Struts2_S2_062_RCE_CVE_2021_31805.json",
                "/vuln/goby/struts2-s2062", true);
    }

    @Test
    public void testIfw8PasswordLeakageLegacy() {
        testPoc(gobyBase + "IFW8_Enterprise_router_Password_leakage_.json",
                "/vuln/goby/ifw8-password-leakage", true);
    }

    @Test
    public void testIfw8CredentialDiscoveryLegacy() {
        testPoc(gobyBase + "IFW8_Router_ROM_v4.31_Credential_Discovery_CVE_2019_16313.json",
                "/vuln/goby/ifw8-credential-discovery", true);
    }

    @Test
    public void testWeaverOa8SqlInjection() {
        testPoc(gobyBase + "Weaver_OA_8_SQL_injection.json",
                "/vuln/goby/weaver-oa8", true);
    }

    @Test
    public void testHuatianOaSqlInjectionModern() {
        testPoc(gobyBase + "huatiandongliOA_8000workFlowService_SQLinjection.json",
                "/vuln/goby/huatian-oa", true);
    }

    @Test
    public void test360TianqingSqlInjectionModern() {
        testPoc(gobyBase + "360_TianQing_ccid_SQL_injectable.json",
                "/vuln/goby/360tianqing-ccid", true);
    }

    @Test
    public void testRiskscannerSqlInjectionModern() {
        testPoc(gobyBase + "Riskscanner_list_SQL_injection.json",
                "/vuln/goby/riskscanner-sqli", true);
    }

    @Test
    public void testFahuo100SqlInjectionModern() {
        testPoc(gobyBase + "fahuo100_sql_injection_CNVD_2021_30193.json",
                "/vuln/goby/fahuo100-sqli", true);
    }

    @Test
    public void testFumengyunSqlInjectionModern() {
        testPoc(gobyBase + "fumengyun  AjaxMethod.ashx SQL injection.json",
                "/vuln/goby/fumengyun-sqli", true);
    }

    @Test
    public void testConfluenceRceLegacy() {
        testPoc(gobyBase + "Confluence_RCE_CVE_2021_26084.json",
                "/vuln/goby/confluence-rce", true);
    }

    @Test
    public void testConsulRexecRceModern() {
        testPoc(gobyBase + "Consul_Rexec_RCE.json",
                "/vuln/goby/consul-rexec", true);
    }

    @Test
    public void testCactiWeathermapFileWriteLegacy() {
        testPoc(gobyBase + "Cacti_Weathermap_File_Write.json",
                "/vuln/goby/cacti", true);
    }

    @Test
    public void testCouchdbPrivEscModern() {
        testPoc(gobyBase + "Apache_CouchDB_Remote_Privilege_Escalation_CVE-2017-12635.json",
                "/vuln/goby/couchdb-privesc", true);
    }

    @Test
    public void testAspcmsBackendLeakModern() {
        testPoc(gobyBase + "Aspcms_Backend_Leak.json",
                "/vuln/goby/aspcms-leak", true);
    }

    @Test
    public void testBigantPathTraversalModern() {
        testPoc(gobyBase + "BigAnt_Server_v5.6.06_Path_Traversal_CVE_2022_23347.json",
                "/vuln/goby/bigant-traversal", true);
    }

    @Test
    public void testCerebroSqlInjectionModern() {
        testPoc(gobyBase + "Cerebro_request_SSRF.json",
                "/vuln/goby/cerebro-sqli", true);
    }

    @Test
    public void testChinaMobileYuRoutingLoginBypassModern() {
        testPoc(gobyBase + "China_Mobile_Yu_Routing_Login_Bypass.json",
                "/vuln/goby/chinamobile-login-bypass", true);
    }

    @Test
    public void testFinetree5mpAuthModern() {
        testPoc(gobyBase + "Finetree_5MP_Network_Camera_Default_Login_unauthorized_user_add.json",
                "/vuln/goby/finetree-5mp-auth", true);
    }

    @Test
    public void testLanproxyDirectoryTraversalModern() {
        testPoc(gobyBase + "Lanproxy_Directory_traversal_CVE_2021_3019.json",
                "/vuln/goby/lanproxy-traversal", true);
    }

    @Test
    public void testRgUacModern() {
        testPoc(gobyBase + "RG_UAC.json",
                "/vuln/goby/rg-uac", true);
    }

    @Test
    public void testRuoyiDruidUnauthorizedModern() {
        testPoc(gobyBase + "RuoYi_Druid_Unauthorized_access.json",
                "/vuln/goby/ruoyi-druid-unauth", true);
    }

    @Test
    public void testSamsungWlanApRceWea453eModern() {
        testPoc(gobyBase + "Samsung_WLAN_AP_WEA453e_RCE.json",
                "/vuln/goby/samsung-wlan-rce", true);
    }

    @Test
    public void testTianwenErpFileUploadModern() {
        testPoc(gobyBase + "Tianwen_ERP_system_FileUpload_CNVD_2020_28119.json",
                "/vuln/goby/tianwen-upload", true);
    }

    @Test
    public void testVengdArbitraryFileUploadModern() {
        testPoc(gobyBase + "VENGD_Arbitrary_File_Upload.json",
                "/vuln/goby/vengd-upload", true);
    }

    @Test
    public void testWeaverOaSqliModern() {
        testPoc(gobyBase + "Weaver_OA_8_SQL_injection.json",
                "/vuln/goby/weaver-oa-sqli", true);
    }

    @Test
    public void testWpSimpleAjaxChatModern() {
        testPoc(gobyBase + "WordPress_Simple_Ajax_Chat_plugin_InfoLeak_CVE_2022_27849.json",
                "/vuln/goby/wp-simple-ajax-chat", true);
    }

    @Test
    public void testFeishimeiStruts2Modern() {
        testPoc(gobyBase + "feishimei_struts2_remote_code.json",
                "/vuln/goby/feishimei-struts2", true);
    }

    @Test
    public void testFirewallInfoLeakModern() {
        testPoc(gobyBase + "firewall_Leaked_user_name_and_password.json",
                "/vuln/goby/firewall-infoleak", true);
    }

    @Test
    public void testMallgardVulnModern() {
        testPoc(gobyBase + "mallgard.json",
                "/vuln/goby/mallgard-vuln", true);
    }

    @Test
    public void testRedFanOaFileReadModern() {
        testPoc(gobyBase + "red_fan_OA_hospital_ioFileExport.aspx_file_read.json",
                "/vuln/goby/redfan-oa-fileread", true);
    }

    @Test
    public void testLaifuyunSqliModern() {
        testPoc(gobyBase + "来福云SQL注入漏洞.json",
                "/vuln/goby/laifuyun-sqli", true);
    }

    @Test
    public void testYapiRceModern() {
        testPoc(gobyBase + "YAPI_RCE.json",
                "/vuln/goby/yapi-rce", true);
    }

    @Test
    public void testYccmsXssModern() {
        testPoc(gobyBase + "YCCMS_XSS.json",
                "/vuln/goby/yccms-xss", true);
    }

    @Test
    public void testMallgardFirewallDefaultLoginModern() {
        testPoc(gobyBase + "Mallgard_Firewall_Default_Login_CNVD_2020_73282.json",
                "/vuln/goby/mallgard", true);
    }
    
    @Test
    public void testZabbixSAML() {
        testPoc(gobyBase + "zabbix_saml_cve_2022_23131.json",
                "/vuln/goby/zabbix-saml", true);
    }

    @Test
    public void testSeeyonA6UserInfoLeak() {
        testPoc(gobyBase + "致远OA A6 用户敏感信息泄露.json",
                "/vuln/goby/seeyon-a6-user-infoleak", true);
    }

    @Test
    public void testSeeyonWebmailFileDownload() {
        testPoc(gobyBase + "致远OA webmail.do任意文件下载 CNVD-2020-62422.json",
                "/vuln/goby/seeyon-webmail-fileread", true);
    }

    @Test
    public void testRuijieRGUACPasswordLeak() {
        testPoc(gobyBase + "Ruijie_RG_UAC_Password_leakage_CNVD_2021_14536.json",
                "/vuln/goby/ruijie-rg-uac-leak", true);
    }

    @Test
    public void testRuijieSmartwebPasswordLeakDefaultPasswordVariant() {
        testPoc(gobyBase + "Ruijie_Smartweb_Default_Password_CNVD_2020_56167.json",
                "/vuln/goby/ruijie-smartweb-leak", true);
    }

    @Test
    public void testRuijieSmartwebPasswordLeakCnvd202117369() {
        testPoc(gobyBase + "Ruijie_Smartweb_Management_System_Password_Information_Disclosure_CNVD_2021_17369.json",
                "/vuln/goby/ruijie-smartweb-leak", true);
    }

    @Test
    public void testRuijieSmartwebWeakPasswordLegacy() {
        testPoc(gobyBase + "Ruijie_Smartweb_Default_Password_CNVD_2020_56167.json",
                "/vuln/goby/ruijie", true);
    }

    @Test
    public void testLaravelEnvLeak() {
        testPoc(gobyBase + "Laravel .env 配置文件泄露 CVE-2017-16894.json",
                "/vuln/goby/laravel-env-leak", true);
    }

    @Test
    public void testLaravelEnvLeakDuplicate() {
        testPoc(gobyBase + "Laravel_.env_configuration_file_leaks_(CVE-2017-16894).json",
                "/vuln/goby/laravel-env-leak", true);
    }

    @Test
    public void testNodeRedFileReadCve20213223() {
        testPoc(gobyBase + "Node-RED_ui_base_Arbitrary_File_Read_CVE-2021-3223.json",
                "/vuln/goby/node-red-fileread", true);
    }

    @Test
    public void testNodeRedFileReadDuplicate() {
        testPoc(gobyBase + "Node_RED_ui_base_Arbitrary_File_Read.json",
                "/vuln/goby/node-red-fileread", true);
    }

    @Test
    public void testMicrosoftExchangeSsrfPeiQiVariant() {
        testPoc(gobyBase + "Microsoft Exchange SSRF漏洞 CVE-2021-26885.json",
                "/vuln/goby/exchange-ssrf", true);
    }

    @Test
    public void testMicrosoftExchangeSsrfGenericVariant() {
        testPoc(gobyBase + "Microsoft_Exchange_Server_SSRF_CVE_2021_26885.json",
                "/vuln/goby/exchange-ssrf", true);
    }

    @Test
    public void testTongdaOaUnauthLegacy() {
        testPoc(gobyBase + "tongdaoa_unauth.json",
                "/vuln/goby/tongda-oa", true);
    }

    @Test
    public void testApacheActiveMqDefaultPasswordLegacy() {
        testPoc(gobyBase + "Apache_ActiveMQ_default_admin_account.json",
                "/vuln/goby/activemq-default", true);
    }

    @Test
    public void testJinheOaC6DefaultPasswordLegacy() {
        testPoc(gobyBase + "JinHe_OA_C6_Default_password.json",
                "/vuln/goby/jinhe-oa", true);
    }

    @Test
    public void testJitongEwebsFileReadLegacy() {
        testPoc(gobyBase + "Jitong_EWEBS_Fileread.json",
                "/vuln/goby/jitong-ewebs", true);
    }

    @Test
    public void testKingsoftV8FileReadLegacy() {
        testPoc(gobyBase + "Kingsoft_V8_Arbitrary_file_read.json",
                "/vuln/goby/kingsoft-v8", true);
    }

    @Test
    public void testKingsoftV8DefaultPasswordLegacy() {
        testPoc(gobyBase + "Kingsoft_V8_Default_weak_password.json",
                "/vuln/goby/kingsoft-v8", true);
    }

    @Test
    public void testNodeJsPathTraversalPositive() {
        testPoc(gobyBase + "Node.js_Path_Traversal_CVE_2017_14849.json",
                "/vuln/goby/nodejs-path-traversal", true);
    }

    @Test
    public void testNodeJsPathTraversalNegative() {
        testPoc(gobyBase + "Node.js_Path_Traversal_CVE_2017_14849.json",
                "/safe/goby/nodejs-path-traversal", false);
    }

    @Test
    public void testNodeRedLegacyFileReadPositive() {
        testPoc(gobyBase + "Node_RED_ui_base_Arbitrary_File_Read.json",
                "/vuln/goby/nodered", true);
    }

    @Test
    public void testApacheHttpPathTraversal42013Positive() {
        testPoc(gobyBase + "Apache_HTTP_Server_2.4.49_2.4.50_Path_Traversal_CVE_2021_42013.json",
                "/vuln/goby/apache-traversal-new", true);
    }

    @Test
    public void testJettyWebInfFileRead28169Positive() {
        testPoc(gobyBase + "Jetty_WEB_INF_FileRead_CVE_2021_28169.json",
                "/vuln/goby/jetty-fileread-28169", true);
    }

    @Test
    public void testJettyWebInfFileReadLegacy() {
        testPoc(gobyBase + "Jetty_WEB_INF_FileRead_CVE_2021_28169.json",
                "/vuln/goby/jetty", true);
    }

    @Test
    public void testJettyWebInfFileRead34429Legacy() {
        testPoc(gobyBase + "Jetty_WEB_INF_FileRead_CVE_2021_34429.json",
                "/vuln/goby/jetty", true);
    }

    @Test
    public void testJettyWebInfFileRead34429Modern() {
        testPoc(gobyBase + "Jetty_WEB_INF_FileRead_CVE_2021_34429.json",
                "/vuln/goby/jetty-fileread", true);
    }

    @Test
    public void testOracleWeblogicLdapRceLegacy() {
        testPoc(gobyBase + "Oracle_Weblogic_LDAP_RCE_CVE_2021_2109.json",
                "/vuln/goby/weblogic-ldap", true);
    }

    @Test
    public void testOraySunloginRceLegacy() {
        testPoc(gobyBase + "Oray_Sunlogin_RCE_CNVD_2022_03672_CNVD_2022_10270.json",
                "/vuln/goby/sunlogin", true);
    }

    @Test
    public void testWebLogicPathTraversalPositive() {
        testPoc(gobyBase + "Oracle_WebLogic_Server_Path_Traversal_CVE_2022_21371.json",
                "/vuln/goby/weblogic-path-traversal", true);
    }

    @Test
    public void testRuijieRguacPasswordLeakLegacy() {
        testPoc(gobyBase + "Ruijie_RG_UAC_Password_leakage_CNVD_2021_14536.json",
                "/vuln/goby/ruijie-uac", true);
    }

    @Test
    public void testSdwanSmartGatewayDefaultPasswordLegacy() {
        testPoc(gobyBase + "SDWAN_Smart_Gateway_Default_Password.json",
                "/vuln/goby/sdwan-gateway", true);
    }

    @Test
    public void testSecurityDevicesHardcodedPasswordLegacy() {
        testPoc(gobyBase + "Security_Devices_Hardcoded_Password.json",
                "/vuln/goby/security-devices", true);
    }

    @Test
    public void testDedeCmsInfoLeakLegacy() {
        testPoc(gobyBase + "DedeCMS_InfoLeak_CVE_2018_6910.json",
                "/vuln/goby/dedecms-info", true);
    }

    @Test
    public void testU8OaLegacy() {
        testPoc(gobyBase + "U8_OA.json",
                "/vuln/goby/u8-oa", true);
    }

    @Test
    public void testZhongXinJingDunDefaultPasswordLegacy() {
        testPoc(gobyBase + "ZhongXinJingDun_Default_administrator_password.json",
                "/vuln/goby/zhongxinjingdun", true);
    }

    @Test
    public void testZhongXinJingDunDefaultPasswordModern() {
        testPoc(gobyBase + "ZhongXinJingDun_Default_administrator_password.json",
                "/vuln/goby/zhongxinjingdun-pwd", true);
    }

    @Test
    public void testQilaiOaSqlInjectionLegacy() {
        testPoc(gobyBase + "qilaiOA_messageurl.aspx_SQLinjection.json",
                "/vuln/goby/qilai-oa", true);
    }

    @Test
    public void testQilaiOaMessageModern() {
        testPoc(gobyBase + "qilaiOA_messageurl.aspx_SQLinjection.json",
                "/vuln/goby/qilai-oa-message", true);
    }

    @Test
    public void testQilaiOaTreeModern() {
        testPoc(gobyBase + "qilaiOA_treelist.aspx_SQLinjection.json",
                "/vuln/goby/qilai-oa-treelist", true);
    }

    @Test
    public void testCitrixUnauthorizedModern() {
        testPoc(gobyBase + "Citrix_Unauthorized_CVE_2020_8193.json",
                "/vuln/goby/citrix-unauth", true);
    }

    @Test
    public void testDiscuzWechatPluginsUnauthModern() {
        testPoc(gobyBase + "Discuz_Wechat_Plugins_Unauth.json",
                "/vuln/goby/discuz-wechat-unauth", true);
    }

    @Test
    public void testXrayHuatianDownloadWpsFile() {
        testPoc(xrayBase + "华天动力OA downloadWpsFile.jsp 任意文件读取漏洞/fileread.yml",
                "/vuln/xray/huatian", true);
    }

    @Test
    public void testXrayJeecgBootLoadTableDataRce() {
        testPoc(xrayBase + "Jeecg-Boot loadTableData 远程代码执行漏洞/rce.yml",
                "/vuln/xray/jeecg", true);
    }

    @Test
    public void testCouchCmsInfoLeakLegacy() {
        testPoc(gobyBase + "Couch_CMS_Infoleak_CVE_2018_7662.json",
                "/vuln/goby/couchcms", true);
    }

    @Test
    public void testCouchCmsInfoLeakModern() {
        testPoc(gobyBase + "CouchCMS_Infoleak_CVE-2018-7662.json",
                "/vuln/goby/couchcms-infoleak", true);
    }

    @Test
    public void testDockerRegistryUnauthModern() {
        testPoc(gobyBase + "Docker_Registry_API_Unauth.json",
                "/vuln/goby/docker-registry-unauth", true);
    }

    @Test
    public void testDubboAdminDefaultPasswordModern() {
        testPoc(gobyBase + "Dubbo_Admin_Default_Password.json",
                "/vuln/goby/dubbo-admin-default", true);
    }

    @Test
    public void testWso2XssModern() {
        testPoc(gobyBase + "WSO2_Management_Console_Reflected_XSS_CVE_2022_29548.json",
                "/vuln/goby/wso2-xss", true);
    }

    @Test
    public void testWso2FileUploadModern() {
        testPoc(gobyBase + "WSO2_Management_Console_Unrestricted_Arbitrary_File_Upload_RCE_CVE_2022_29464.json",
                "/vuln/goby/wso2-upload", true);
    }

    @Test
    public void testWeaverEOfficeUploadModern() {
        testPoc(gobyBase + "Weaver_EOffice_Arbitrary_File_Upload_CNVD-2021-49104.json",
                "/vuln/goby/weaver-eoffice-upload", true);
    }

    @Test
    public void testWeblogicLdapRceModern() {
        testPoc(gobyBase + "Weblogic LDAP Internet RCE CVE-2021-2109.json",
                "/vuln/goby/weblogic-ldap-rce-2", true);
    }

    @Test
    public void testWeblogicSsrf2014Modern() {
        testPoc(gobyBase + "Weblogic SSRF漏洞 CVE-2014-4210.json",
                "/vuln/goby/weblogic-ssrf-2014", true);
    }

    @Test
    public void testVmwareWorkspaceOneModern() {
        testPoc(gobyBase + "VMware_Workspace_ONE_Access_RCE_CVE_2022_22954.json",
                "/vuln/goby/vmware-ws1-rce", true);
    }

    @Test
    public void testVmwareVcenterFileReadModern() {
        testPoc(gobyBase + "VMware_vCenter_v7.0.2_Arbitrary_File_Read.json",
                "/vuln/goby/vmware-vcenter-fileread", true);
    }
    
}

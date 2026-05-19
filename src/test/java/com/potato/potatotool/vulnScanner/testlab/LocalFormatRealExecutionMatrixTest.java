package com.potato.potatotool.vulnScanner.testlab;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.core.PocExecutor;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsLogService;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocLoader;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("四种格式本机真实执行矩阵测试")
public class LocalFormatRealExecutionMatrixTest {

    private static final int MANUAL_PORT = 18896;
    private static final String MANUAL_BASE_URL = "http://127.0.0.1:" + MANUAL_PORT;
    private static final int NEW_POC_PORT = 18897;
    private static final String NEW_POC_BASE_URL = "http://127.0.0.1:" + NEW_POC_PORT;

    private static ManualPocTestServer manualServer;
    private static NewPocTestServer newPocServer;
    private static PocLoader pocLoader;

    @BeforeAll
    static void setup() throws Exception {
        manualServer = new ManualPocTestServer(MANUAL_PORT);
        manualServer.start();
        newPocServer = new NewPocTestServer(NEW_POC_PORT);
        newPocServer.start();
        pocLoader = new PocLoader();
        DnsLogService.setMockMode(true);
        Thread.sleep(500L);
    }

    @AfterAll
    static void tearDown() {
        if (manualServer != null) {
            manualServer.stop();
        }
        if (newPocServer != null) {
            newPocServer.stop();
        }
        DnsLogService.setMockMode(false);
    }

    @Test
    @DisplayName("Nuclei/Xray/Goby/Pocsuite 各 3 个样例应可在本机真实执行并输出汇总")
    void shouldExecuteThreeLocalCasesPerFormatAndWriteSummary() throws Exception {
        List<ExecutionCase> cases = new ArrayList<ExecutionCase>();
        cases.add(new ExecutionCase("nuclei", "cve-2024-8859",
                "src/test/resources/newPoc/nucleiPoc/01_cve_lfi_basic.yaml", NEW_POC_BASE_URL + "/vuln/nuclei/01"));
        cases.add(new ExecutionCase("nuclei", "payloads-detect",
                "src/test/resources/newPoc/nucleiPoc/03_payloads_detect.yaml", NEW_POC_BASE_URL + "/vuln/nuclei/03"));
        cases.add(new ExecutionCase("nuclei", "default-login",
                "src/test/resources/newPoc/nucleiPoc/10_default_login.yaml", NEW_POC_BASE_URL + "/vuln/nuclei/10"));

        cases.add(new ExecutionCase("xray", "fanwei-ssrf",
                "src/main/resources/poc/xrayPoc/2024-7-xray/泛微E-Mobile installOperate.do SSRF漏洞/ssrf.yml",
                MANUAL_BASE_URL + "/vuln/xray/fanwei"));
        cases.add(new ExecutionCase("xray", "huatian-fileread",
                "src/main/resources/poc/xrayPoc/2024-7-xray/华天动力OA downloadWpsFile.jsp 任意文件读取漏洞/fileread.yml",
                MANUAL_BASE_URL + "/vuln/xray/huatian"));
        cases.add(new ExecutionCase("xray", "jeecg-rce",
                "src/main/resources/poc/xrayPoc/2024-7-xray/Jeecg-Boot loadTableData 远程代码执行漏洞/rce.yml",
                MANUAL_BASE_URL + "/vuln/xray/jeecg"));

        cases.add(new ExecutionCase("goby", "minio-ssrf",
                "src/main/resources/poc/gobyPoc/MinIO_Browser_API_SSRF_CVE_2021_21287.json",
                MANUAL_BASE_URL + "/vuln/goby/minio"));
        cases.add(new ExecutionCase("goby", "grafana-fileread",
                "src/main/resources/poc/gobyPoc/Grafana_v8.x_Arbitrary_File_Read_CVE_2021_43798.json",
                MANUAL_BASE_URL + "/vuln/goby/grafana"));
        cases.add(new ExecutionCase("goby", "struts2-s2062",
                "src/main/resources/poc/gobyPoc/Apache_Struts2_S2_062_RCE_CVE_2021_31805.json",
                MANUAL_BASE_URL + "/vuln/goby/struts2-s2062"));

        cases.add(new ExecutionCase("pocsuite", "time-blind",
                "src/main/resources/poc/pocsuitePoc/sql_time_blind_injection.json",
                MANUAL_BASE_URL + "/vuln/pocsuite/timebased"));
        cases.add(new ExecutionCase("pocsuite", "phpcms-sqli",
                "src/main/resources/poc/pocsuitePoc/Pocsuite.json",
                MANUAL_BASE_URL + "/vuln/pocsuite/phpcms"));
        cases.add(new ExecutionCase("pocsuite", "multi-step-necessary",
                "src/main/resources/poc/pocsuitePoc/multi_step_with_necessary.json",
                MANUAL_BASE_URL + "/vuln/pocsuite/multistep"));

        List<String> summaryLines = new ArrayList<String>();
        summaryLines.add("format\tcase_name\tpoc_path\ttarget\tvulnerable\tstep_record_count\tmatched_path");

        int nucleiCount = 0;
        int xrayCount = 0;
        int gobyCount = 0;
        int pocsuiteCount = 0;

        for (ExecutionCase executionCase : cases) {
            PocObj.Poc poc = loadPoc(executionCase);
            assertNotNull(poc, "POC 加载失败: " + executionCase.pocPath);

            ScanConfig config = new ScanConfig();
            if ("nuclei".equals(executionCase.format)) {
                NewPocTestServer.setVulnerableMode(true);
            }
            ScanResult result = new PocExecutor(config).execute(executionCase.targetUrl, poc);

            assertTrue(result.isVulnerable(), "本机真实执行应命中: " + executionCase.caseName);
            summaryLines.add(buildSummaryLine(executionCase, result));

            if ("nuclei".equals(executionCase.format)) {
                nucleiCount++;
            } else if ("xray".equals(executionCase.format)) {
                xrayCount++;
            } else if ("goby".equals(executionCase.format)) {
                gobyCount++;
            } else if ("pocsuite".equals(executionCase.format)) {
                pocsuiteCount++;
            }
        }

        assertEquals(3, nucleiCount);
        assertEquals(3, xrayCount);
        assertEquals(3, gobyCount);
        assertEquals(3, pocsuiteCount);

        File outputDir = new File("target/format-real-execution");
        assertTrue(outputDir.exists() || outputDir.mkdirs());
        Files.write(new File(outputDir, "real-execution-summary.tsv").toPath(),
                joinLines(summaryLines).getBytes(StandardCharsets.UTF_8));
    }

    private PocObj.Poc loadPoc(ExecutionCase executionCase) throws Exception {
        return pocLoader.loadFromFile(executionCase.pocPath);
    }

    private String buildSummaryLine(ExecutionCase executionCase, ScanResult result) {
        int stepRecordCount = result.getStepRecords() == null ? 0 : result.getStepRecords().size();
        String matchedPath = result.getMatchedPath() == null ? "" : result.getMatchedPath().replace('\t', ' ');
        return executionCase.format + "\t" + executionCase.caseName + "\t" + executionCase.pocPath + "\t" +
                executionCase.targetUrl + "\t" + result.isVulnerable() + "\t" + stepRecordCount + "\t" + matchedPath;
    }

    private String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append('\n');
        }
        return builder.toString();
    }

    private static final class ExecutionCase {
        private final String format;
        private final String caseName;
        private final String pocPath;
        private final String targetUrl;

        private ExecutionCase(String format, String caseName, String pocPath, String targetUrl) {
            this.format = format;
            this.caseName = caseName;
            this.pocPath = pocPath;
            this.targetUrl = targetUrl;
        }
    }
}

package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.GobyJsonObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocsuiteJsonObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.XrayYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.NucleiPocConverter;
import com.potato.potatotool.content.redTeam.vulnScanner.util.converter.XrayPocConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("官方公开 POC 资源兼容性测试")
public class ExternalOfficialPocCompatibilityTest {

    private static final Gson GSON = new Gson();
    private static final File OUTPUT_ROOT = new File("target/external-official-pocs");
    private static final File CACHE_ROOT = new File(OUTPUT_ROOT, "cache");
    private static final File APPROVED_ROOT = new File(OUTPUT_ROOT, "approved");
    private static final File REVIEW_ONLY_ROOT = new File(OUTPUT_ROOT, "review-only");

    private static final List<String> DANGEROUS_TOKENS = Arrays.asList(
            "curl ",
            "wget ",
            "bash -c",
            "cmd.exe",
            "powershell",
            "processbuilder",
            "runtime.getruntime()",
            "os.system(",
            "subprocess.",
            "interactsh",
            "dnslog",
            "ceye"
    );

    private static final RepoSpec NUCLEI_REPO = new RepoSpec(
            "nuclei",
            "projectdiscovery/nuclei-templates",
            "main",
            "MIT",
            "official yaml"
    );

    private static final RepoSpec XRAY_REPO = new RepoSpec(
            "xray",
            "chaitin/xray",
            "master",
            "Other/NOASSERTION",
            "official yaml with extra license constraints"
    );

    private static final RepoSpec POCSUITE_REPO = new RepoSpec(
            "pocsuite3",
            "knownsec/pocsuite3",
            "master",
            "Other/NOASSERTION",
            "official public samples are python, not current json input"
    );

    private static final RepoSpec GOBYVULS_REPO = new RepoSpec(
            "gobyvuls",
            "gobysec/GobyVuls",
            "master",
            "unspecified",
            "official public samples are markdown/docs, not current json input"
    );

    private static final List<ExternalSample> NUCLEI_SAMPLES = Arrays.asList(
            nuclei("http/technologies/wordpress-detect.yaml", "http"),
            nuclei("http/technologies/springboot-actuator.yaml", "http"),
            nuclei("http/technologies/default-lighttpd-page.yaml", "http"),
            nuclei("dns/detect-dangling-cname.yaml", "dns"),
            nuclei("dns/dmarc-detect.yaml", "dns"),
            nuclei("ssl/tls-version.yaml", "ssl"),
            nuclei("ssl/expired-ssl.yaml", "ssl"),
            nuclei("file/nodejs/generic-path-traversal.yaml", "file"),
            nuclei("network/exposures/exposed-redis.yaml", "network"),
            nuclei("headless/extract-urls.yaml", "headless")
    );

    private static final List<ExternalSample> XRAY_SAMPLES = Arrays.asList(
            xray("pocs/activemq-default-password.yml", "http"),
            xray("pocs/airflow-unauth.yml", "http"),
            xray("pocs/alibaba-canal-default-password.yml", "http"),
            xray("pocs/alibaba-canal-info-leak.yml", "http"),
            xray("pocs/alibaba-nacos-v1-auth-bypass.yml", "http"),
            xray("pocs/apache-ambari-default-password.yml", "http"),
            xray("pocs/apache-druid-cve-2021-36749.yml", "http"),
            xray("pocs/apache-httpd-cve-2021-41773-path-traversal.yml", "http"),
            xray("pocs/apache-kylin-unauth-cve-2020-13937.yml", "http"),
            xray("pocs/druid-monitor-unauth.yml", "http")
    );

    private static final List<ExternalSample> POCSUITE_MISMATCH_SAMPLES = Arrays.asList(
            pocsuite("pocsuite3/pocs/20190404_WEB_Confluence_path_traversal.py"),
            pocsuite("pocsuite3/pocs/20210923_WEB_Vmware_vCenter_Server_FIleUpload_CVE-2021-22005.py"),
            pocsuite("pocsuite3/pocs/20211008_web_apache-httpd_dir-traversal-rce_cve-2021-41773_cve-2021-42013.py"),
            pocsuite("pocsuite3/pocs/demo_poc.py"),
            pocsuite("pocsuite3/pocs/drupalgeddon2.py"),
            pocsuite("pocsuite3/pocs/ecshop_rce.py"),
            pocsuite("pocsuite3/pocs/libssh_auth_bypass.py"),
            pocsuite("pocsuite3/pocs/node_red_unauthorized_rce.py"),
            pocsuite("pocsuite3/pocs/redis_unauthorized_access.py"),
            pocsuite("pocsuite3/pocs/thinkphp_rce.py")
    );

    private static final List<ExternalSample> GOBY_MISMATCH_SAMPLES = Arrays.asList(
            gobyvuls("91skzy_Enterprise_process_control_system_formservice_File_Upload_vulnerability.md"),
            gobyvuls("91skzy_Enterprise_process_control_system_wc.db_Information_Disclosure_vulnerability.md"),
            gobyvuls("ActiveMQ/CVE-2016-3088/README.md"),
            gobyvuls("Adobe_ColdFusion_CFIDE_adminapi_servermanager_servermanager.cfc_File_Read_Vulnerability_(CVE-2024-20767).md"),
            gobyvuls("Adobe_ColdFusion_WDDX_JGroups_remote_code_execution_vulnerability.md"),
            gobyvuls("F5/CVE-2021-22986/README.md"),
            gobyvuls("F5/CVE-2022-1388/README.md"),
            gobyvuls("Jetty/README.md"),
            gobyvuls("Apache_Kafka_Connect_remote_code_execution_vulnerability_(CVE-2023-25194).md"),
            gobyvuls("Apache_Superset_Cookie_Permission_Bypass_Vulnerability_(CVE-2023-30776).md")
    );

    @Test
    @DisplayName("官方样例应先完成静态审查并写出清单")
    void shouldReviewOfficialSamplesAndWriteManifest() throws Exception {
        prepareOutputDirectories();

        List<RepoMetadata> repoMetadata = Arrays.asList(
                fetchRepoMetadata(NUCLEI_REPO),
                fetchRepoMetadata(XRAY_REPO),
                fetchRepoMetadata(POCSUITE_REPO),
                fetchRepoMetadata(GOBYVULS_REPO)
        );

        List<ReviewRecord> reviewRecords = new ArrayList<ReviewRecord>();
        reviewRecords.addAll(reviewSamples(NUCLEI_SAMPLES, true));
        reviewRecords.addAll(reviewSamples(XRAY_SAMPLES, true));
        reviewRecords.addAll(reviewSamples(POCSUITE_MISMATCH_SAMPLES, false));
        reviewRecords.addAll(reviewSamples(GOBY_MISMATCH_SAMPLES, false));

        writeRepoMetadata(repoMetadata);
        writeReviewRecords(reviewRecords);

        assertEquals(40, reviewRecords.size(), "应记录 40 个外部样例审查结果");
        assertEquals(20, countStatus(reviewRecords, "approved"), "Nuclei/Xray 20 个样例应通过静态审查");
        assertEquals(20, countStatus(reviewRecords, "shape-mismatch"), "Pocsuite/Goby 20 个样例应被记录为格式边界");
        assertTrue(new File(OUTPUT_ROOT, "repo-metadata.tsv").isFile());
        assertTrue(new File(OUTPUT_ROOT, "sample-review.tsv").isFile());
    }

    @Test
    @DisplayName("Nuclei 官方样例应完成加载与禁用执行转换检查")
    void shouldLoadAndConvertSelectedOfficialNucleiTemplates() throws Exception {
        prepareOutputDirectories();
        List<String> rows = new ArrayList<String>();
        rows.add("source\tpath\tprotocol\tparsed\tconverted\tstepCount");

        NucleiPocConverter converter = new NucleiPocConverter();
        for (ExternalSample sample : NUCLEI_SAMPLES) {
            String content = fetchSampleContent(sample);
            ReviewOutcome outcome = reviewContent(sample, content);
            assertTrue(outcome.approved, "Nuclei 样例未通过审查: " + sample.path);

            NucleiYamlObj.Poc yaml = PocConverter.loadNucleiYamlFromContent(content);
            assertNotNull(yaml, "Nuclei YAML 解析失败: " + sample.path);

            PocObj.Poc poc = converter.convert(yaml);
            assertNotNull(poc, "Nuclei 转换失败: " + sample.path);
            assertNotNull(poc.getProtocol(), "协议缺失: " + sample.path);
            assertFalse(poc.getProtocol().trim().isEmpty(), "协议为空: " + sample.path);

            int stepCount = countSteps(poc);
            assertTrue(stepCount > 0, "应至少保留一个步骤: " + sample.path);
            rows.add(sample.source + "\t" + sample.path + "\t" + poc.getProtocol() + "\ttrue\ttrue\t" + stepCount);
        }

        writeLines(new File(OUTPUT_ROOT, "nuclei-compatibility.tsv"), rows);
    }

    @Test
    @DisplayName("Xray 官方样例应完成加载与禁用执行转换检查")
    void shouldLoadAndConvertSelectedOfficialXrayPocs() throws Exception {
        prepareOutputDirectories();
        List<String> rows = new ArrayList<String>();
        rows.add("source\tpath\tprotocol\tparsed\tconverted\tstepCount");

        XrayPocConverter converter = new XrayPocConverter();
        for (ExternalSample sample : XRAY_SAMPLES) {
            String content = fetchSampleContent(sample);
            ReviewOutcome outcome = reviewContent(sample, content);
            assertTrue(outcome.approved, "Xray 样例未通过审查: " + sample.path);

            XrayYamlObj.Poc yaml = PocConverter.loadXrayYamlFromContent(content);
            assertNotNull(yaml, "Xray YAML 解析失败: " + sample.path);

            PocObj.Poc poc = converter.convert(yaml);
            assertNotNull(poc, "Xray 转换失败: " + sample.path);
            assertEquals("xray", poc.getOriginalFormat(), "格式标记错误: " + sample.path);

            int stepCount = countSteps(poc);
            assertTrue(stepCount > 0, "应至少保留一个步骤: " + sample.path);
            rows.add(sample.source + "\t" + sample.path + "\t" + poc.getProtocol() + "\ttrue\ttrue\t" + stepCount);
        }

        writeLines(new File(OUTPUT_ROOT, "xray-compatibility.tsv"), rows);
    }

    @Test
    @DisplayName("Pocsuite3 与 Goby 官方公开资源应被记录为格式边界")
    void shouldRecordOfficialFormatGapForPocsuiteAndGoby() throws Exception {
        prepareOutputDirectories();
        List<String> rows = new ArrayList<String>();
        rows.add("source\tpath\treviewStatus\tparsedAsCurrentFormat\tconvertedAsCurrentFormat\tnote");

        for (ExternalSample sample : POCSUITE_MISMATCH_SAMPLES) {
            String content = fetchSampleContent(sample);
            ReviewOutcome outcome = reviewContent(sample, content);
            assertEquals("shape-mismatch", outcome.status, "Pocsuite 官方 python 样例应记录为格式边界");

            PocsuiteJsonObj.PocJson json = PocConverter.loadPocsuiteJsonFromContent(content);
            assertNull(json, "官方 python 不应被误解析为当前 JSON 输入: " + sample.path);

            rows.add(sample.source + "\t" + sample.path + "\t" + outcome.status + "\tfalse\tfalse\tpython sample");
        }

        for (ExternalSample sample : GOBY_MISMATCH_SAMPLES) {
            String content = fetchSampleContent(sample);
            ReviewOutcome outcome = reviewContent(sample, content);
            assertEquals("shape-mismatch", outcome.status, "GobyVuls markdown 样例应记录为格式边界");

            GobyJsonObj.PocJson json = PocConverter.loadGobyJsonFromContent(content);
            assertNull(json, "官方 markdown 不应被误解析为当前 Goby JSON 输入: " + sample.path);

            rows.add(sample.source + "\t" + sample.path + "\t" + outcome.status + "\tfalse\tfalse\tmarkdown sample");
        }

        writeLines(new File(OUTPUT_ROOT, "official-format-gaps.tsv"), rows);
    }

    private List<ReviewRecord> reviewSamples(List<ExternalSample> samples, boolean approvedShouldBeCached) throws Exception {
        List<ReviewRecord> records = new ArrayList<ReviewRecord>();
        for (ExternalSample sample : samples) {
            String content = fetchSampleContent(sample);
            ReviewOutcome outcome = reviewContent(sample, content);
            File savedFile;
            if (approvedShouldBeCached && outcome.approved) {
                savedFile = writeReviewedContent(APPROVED_ROOT, sample, content);
            } else {
                savedFile = writeReviewedContent(REVIEW_ONLY_ROOT, sample, content);
            }
            records.add(new ReviewRecord(
                    sample.source,
                    sample.repo.repo,
                    sample.repo.branch,
                    sample.path,
                    sample.rawUrl(),
                    sample.repo.licenseOrRestriction,
                    "no",
                    "no",
                    outcome.status,
                    join(outcome.findings),
                    relativize(savedFile)
            ));
        }
        return records;
    }

    private ReviewOutcome reviewContent(ExternalSample sample, String content) {
        String lower = content.toLowerCase(Locale.ROOT);
        List<String> findings = new ArrayList<String>();
        for (String token : DANGEROUS_TOKENS) {
            if (lower.contains(token)) {
                findings.add(token.trim());
            }
        }

        if (sample.shapeMismatch) {
            findings.add(sample.mismatchNote);
            return new ReviewOutcome("shape-mismatch", false, findings);
        }

        return new ReviewOutcome(findings.isEmpty() ? "approved" : "blocked", findings.isEmpty(), findings);
    }

    private String fetchSampleContent(ExternalSample sample) throws Exception {
        File cacheFile = toCacheFile(sample);
        if (cacheFile.isFile()) {
            return readFile(cacheFile);
        }

        String content = httpGet(sample.rawUrl());
        writeFile(cacheFile, content);
        return content;
    }

    private RepoMetadata fetchRepoMetadata(RepoSpec repo) throws Exception {
        String branchUrl = "https://api.github.com/repos/" + repo.repo + "/branches/" + repo.branch;
        @SuppressWarnings("unchecked")
        LinkedTreeMap<String, Object> branchData =
                GSON.fromJson(httpGet(branchUrl), LinkedTreeMap.class);
        @SuppressWarnings("unchecked")
        LinkedTreeMap<String, Object> commitData =
                (LinkedTreeMap<String, Object>) branchData.get("commit");
        String sha = String.valueOf(commitData.get("sha"));
        return new RepoMetadata(repo.source, repo.repo, repo.branch, sha, repo.licenseOrRestriction, repo.note);
    }

    private void prepareOutputDirectories() {
        assertTrue(OUTPUT_ROOT.exists() || OUTPUT_ROOT.mkdirs());
        assertTrue(CACHE_ROOT.exists() || CACHE_ROOT.mkdirs());
        assertTrue(APPROVED_ROOT.exists() || APPROVED_ROOT.mkdirs());
        assertTrue(REVIEW_ONLY_ROOT.exists() || REVIEW_ONLY_ROOT.mkdirs());
    }

    private File writeReviewedContent(File baseDir, ExternalSample sample, String content) throws IOException {
        File file = new File(baseDir, sample.source + "/" + sample.path);
        writeFile(file, content);
        return file;
    }

    private void writeRepoMetadata(List<RepoMetadata> metadataList) throws IOException {
        List<String> rows = new ArrayList<String>();
        rows.add("source\trepo\tbranch\theadSha\tlicenseOrRestriction\tnote");
        for (RepoMetadata metadata : metadataList) {
            rows.add(metadata.source + "\t" + metadata.repo + "\t" + metadata.branch + "\t"
                    + metadata.headSha + "\t" + metadata.licenseOrRestriction + "\t" + metadata.note);
        }
        writeLines(new File(OUTPUT_ROOT, "repo-metadata.tsv"), rows);
    }

    private void writeReviewRecords(List<ReviewRecord> records) throws IOException {
        List<String> rows = new ArrayList<String>();
        rows.add("source\trepo\tbranch\tpath\turl\tlicenseOrRestriction\tmodified\texecuted\treviewStatus\tfindings\tstoredAt");
        for (ReviewRecord record : records) {
            rows.add(record.source + "\t" + record.repo + "\t" + record.branch + "\t" + record.path + "\t"
                    + record.url + "\t" + record.licenseOrRestriction + "\t" + record.modified + "\t"
                    + record.executed + "\t" + record.reviewStatus + "\t" + record.findings + "\t" + record.storedAt);
        }
        writeLines(new File(OUTPUT_ROOT, "sample-review.tsv"), rows);
    }

    private File toCacheFile(ExternalSample sample) {
        return new File(CACHE_ROOT, sample.source + "/" + sample.path);
    }

    private int countSteps(PocObj.Poc poc) {
        int count = 0;
        if (poc.getVerifySteps() != null) {
            count += poc.getVerifySteps().size();
        }
        if (poc.getExploitSteps() != null) {
            count += poc.getExploitSteps().size();
        }
        return count;
    }

    private int countStatus(List<ReviewRecord> records, String status) {
        int count = 0;
        for (ReviewRecord record : records) {
            if (status.equals(record.reviewStatus)) {
                count++;
            }
        }
        return count;
    }

    private String relativize(File file) {
        return file.getPath().replace('\\', '/');
    }

    private void writeLines(File file, List<String> rows) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String row : rows) {
            builder.append(row).append('\n');
        }
        writeFile(file, builder.toString());
    }

    private void writeFile(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            assertTrue(parent.mkdirs() || parent.exists());
        }
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readFile(File file) throws IOException {
        try (InputStream inputStream = new BufferedInputStream(new java.io.FileInputStream(file));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String httpGet(String urlValue) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlValue).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "PotatoTool-ExternalOfficialPocCompatibilityTest");

        int code = connection.getResponseCode();
        InputStream inputStream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String body = readAll(inputStream);
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " for " + urlValue + ": " + body);
        }
        return body;
    }

    private String readAll(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        try (InputStream in = inputStream;
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private static ExternalSample nuclei(String path, String category) {
        return new ExternalSample("nuclei", NUCLEI_REPO, path, category, false, "");
    }

    private static ExternalSample xray(String path, String category) {
        return new ExternalSample("xray", XRAY_REPO, path, category, false, "");
    }

    private static ExternalSample pocsuite(String path) {
        return new ExternalSample("pocsuite3", POCSUITE_REPO, path, "python", true, "official python sample does not match current json converter input");
    }

    private static ExternalSample gobyvuls(String path) {
        return new ExternalSample("gobyvuls", GOBYVULS_REPO, path, "markdown", true, "official public sample is markdown, not goby json");
    }

    private static final class RepoSpec {
        private final String source;
        private final String repo;
        private final String branch;
        private final String licenseOrRestriction;
        private final String note;

        private RepoSpec(String source, String repo, String branch, String licenseOrRestriction, String note) {
            this.source = source;
            this.repo = repo;
            this.branch = branch;
            this.licenseOrRestriction = licenseOrRestriction;
            this.note = note;
        }
    }

    private static final class ExternalSample {
        private final String source;
        private final RepoSpec repo;
        private final String path;
        private final String category;
        private final boolean shapeMismatch;
        private final String mismatchNote;

        private ExternalSample(String source, RepoSpec repo, String path, String category, boolean shapeMismatch, String mismatchNote) {
            this.source = source;
            this.repo = repo;
            this.path = path;
            this.category = category;
            this.shapeMismatch = shapeMismatch;
            this.mismatchNote = mismatchNote;
        }

        private String rawUrl() {
            return "https://raw.githubusercontent.com/" + repo.repo + "/" + repo.branch + "/" + path;
        }
    }

    private static final class RepoMetadata {
        private final String source;
        private final String repo;
        private final String branch;
        private final String headSha;
        private final String licenseOrRestriction;
        private final String note;

        private RepoMetadata(String source, String repo, String branch, String headSha, String licenseOrRestriction, String note) {
            this.source = source;
            this.repo = repo;
            this.branch = branch;
            this.headSha = headSha;
            this.licenseOrRestriction = licenseOrRestriction;
            this.note = note;
        }
    }

    private static final class ReviewOutcome {
        private final String status;
        private final boolean approved;
        private final List<String> findings;

        private ReviewOutcome(String status, boolean approved, List<String> findings) {
            this.status = status;
            this.approved = approved;
            this.findings = findings == null ? Collections.<String>emptyList() : findings;
        }
    }

    private static final class ReviewRecord {
        private final String source;
        private final String repo;
        private final String branch;
        private final String path;
        private final String url;
        private final String licenseOrRestriction;
        private final String modified;
        private final String executed;
        private final String reviewStatus;
        private final String findings;
        private final String storedAt;

        private ReviewRecord(String source, String repo, String branch, String path, String url,
                             String licenseOrRestriction, String modified, String executed,
                             String reviewStatus, String findings, String storedAt) {
            this.source = source;
            this.repo = repo;
            this.branch = branch;
            this.path = path;
            this.url = url;
            this.licenseOrRestriction = licenseOrRestriction;
            this.modified = modified;
            this.executed = executed;
            this.reviewStatus = reviewStatus;
            this.findings = findings;
            this.storedAt = storedAt;
        }
    }
}

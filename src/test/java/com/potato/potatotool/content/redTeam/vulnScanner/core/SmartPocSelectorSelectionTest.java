package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventDispatcher;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocRepository;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SmartPocSelector POC选择测试")
class SmartPocSelectorSelectionTest {

    @Test
    @DisplayName("应按格式与严重级别筛选POC")
    void shouldFilterByFormatAndMinSeverity() {
        PocRepository repository = new PocRepository();
        repository.addPoc(buildHttpPoc("nuclei-critical", "nuclei", PocObj.Severity.CRITICAL, PocObj.PocCategory.CVES, "spring"));
        repository.addPoc(buildHttpPoc("nuclei-low", "nuclei", PocObj.Severity.LOW, PocObj.PocCategory.CVES, "spring"));
        repository.addPoc(buildHttpPoc("goby-critical", "goby", PocObj.Severity.CRITICAL, PocObj.PocCategory.CVES, "spring"));

        SmartPocSelector selector = new SmartPocSelector(repository, new PocExecutor(new ScanConfig()), new ScanEventDispatcher());

        ScanConfig config = ScanConfig.createDefaultUrlConfig();
        config.setAutoDetectInputType(false);
        config.setEnabledPocFormats(new HashSet<String>(Collections.singletonList("nuclei")));
        config.setMinSeverity(PocObj.Severity.HIGH);
        config.setSkipFingerprint(true);

        List<PocObj.Poc> selected = selector.selectAndFilterPocs("http://127.0.0.1", config).getPocs();

        assertEquals(1, selected.size());
        assertEquals("nuclei-critical", selected.get(0).getId());
    }

    @Test
    @DisplayName("禁用去重时应保留重复POC，启用去重时应折叠重复项")
    void shouldRespectDeduplicationSwitch() {
        PocRepository repository = new PocRepository();
        repository.addPoc(buildDuplicateNamedPoc("goby-duplicate", "goby", ""));
        repository.addPoc(buildDuplicateNamedPoc("nuclei-duplicate", "nuclei", "CVE-2024-0001"));

        SmartPocSelector selector = new SmartPocSelector(repository, new PocExecutor(new ScanConfig()), new ScanEventDispatcher());

        ScanConfig noDedupConfig = ScanConfig.createDefaultUrlConfig();
        noDedupConfig.setAutoDetectInputType(false);
        noDedupConfig.setSkipFingerprint(true);
        noDedupConfig.setEnableDeduplication(false);
        List<PocObj.Poc> withoutDedup = selector.selectAndFilterPocs("http://127.0.0.1", noDedupConfig).getPocs();
        assertEquals(2, withoutDedup.size());

        ScanConfig dedupConfig = ScanConfig.createDefaultUrlConfig();
        dedupConfig.setAutoDetectInputType(false);
        dedupConfig.setSkipFingerprint(true);
        dedupConfig.setEnableDeduplication(true);
        List<PocObj.Poc> withDedup = selector.selectAndFilterPocs("http://127.0.0.1", dedupConfig).getPocs();

        assertEquals(1, withDedup.size());
        assertEquals("nuclei-duplicate", withDedup.get(0).getId());
    }

    @Test
    @DisplayName("skipFingerprint 应关闭智能指纹筛选并保留特殊输入类POC")
    void shouldBypassFingerprintFilteringWhenSkipFingerprintEnabled() {
        PocRepository repository = new PocRepository();
        repository.addPoc(buildHttpPoc("token-spray", "nuclei", PocObj.Severity.HIGH, PocObj.PocCategory.TOKEN_SPRAY, "auth"));

        SmartPocSelector selector = new SmartPocSelector(repository, new PocExecutor(new ScanConfig()), new ScanEventDispatcher());

        ScanConfig smartConfig = ScanConfig.createDefaultUrlConfig();
        smartConfig.setAutoDetectInputType(false);
        smartConfig.setScanMode(ScanConfig.ScanMode.STANDARD);
        smartConfig.setSkipFingerprint(false);
        List<PocObj.Poc> smartModeSelected = selector.selectAndFilterPocs("http://127.0.0.1", smartConfig).getPocs();
        assertTrue(smartModeSelected.isEmpty());

        ScanConfig skipFingerprintConfig = ScanConfig.createDefaultUrlConfig();
        skipFingerprintConfig.setAutoDetectInputType(false);
        skipFingerprintConfig.setScanMode(ScanConfig.ScanMode.STANDARD);
        skipFingerprintConfig.setSkipFingerprint(true);
        skipFingerprintConfig.getExcludedCategories().remove(PocObj.PocCategory.TOKEN_SPRAY);

        List<PocObj.Poc> skipFingerprintSelected = selector.selectAndFilterPocs("http://127.0.0.1", skipFingerprintConfig).getPocs();

        assertEquals(1, skipFingerprintSelected.size());
        assertEquals("token-spray", skipFingerprintSelected.get(0).getId());
    }

    @Test
    @DisplayName("空POC仓库应返回空结果")
    void shouldReturnEmptyWhenRepositoryHasNoPocs() {
        SmartPocSelector selector = new SmartPocSelector(new PocRepository(), new PocExecutor(new ScanConfig()), new ScanEventDispatcher());

        ScanConfig config = ScanConfig.createDefaultUrlConfig();
        config.setAutoDetectInputType(false);
        config.setSkipFingerprint(true);

        List<PocObj.Poc> selected = selector.selectAndFilterPocs("http://127.0.0.1", config).getPocs();

        assertTrue(selected.isEmpty());
    }

    private PocObj.Poc buildHttpPoc(String id,
                                    String format,
                                    PocObj.Severity severity,
                                    PocObj.PocCategory category,
                                    String tag) {
        PocObj.Poc poc = new PocObj.Poc();
        poc.setId(id);
        poc.setName(id);
        poc.setOriginalFormat(format);
        poc.setSeverity(severity);
        poc.setCategory(category);
        poc.setProtocol("http");
        poc.setInputType(PocObj.InputType.URL);
        poc.setTags(Collections.singletonList(tag));
        poc.setVerifySteps(Collections.singletonList(new PocObj.PocStep()));
        return poc;
    }

    private PocObj.Poc buildDuplicateNamedPoc(String id, String format, String cveId) {
        PocObj.Poc poc = buildHttpPoc(id, format, PocObj.Severity.HIGH, PocObj.PocCategory.CVES, "dup");
        poc.setName("Shared Duplicate Name");
        poc.setVerifySteps(Arrays.asList(step("GET", "/admin"), step("POST", "/login")));
        poc.setCveId(cveId);
        return poc;
    }

    private PocObj.PocStep step(String method, String path) {
        PocObj.PocStep step = new PocObj.PocStep();
        step.setMethod(method);
        step.setPath(path);
        return step;
    }
}

package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.InputType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.PocCategory;
import com.potato.potatotool.content.redTeam.vulnScanner.core.FingerprintService.FingerprintResult;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanEventDispatcher;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanInfoEvent;
import com.potato.potatotool.content.redTeam.vulnScanner.event.ScanInfoEvent.InfoType;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocRepository;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.util.InputTypeDetector;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocDeduplicator;
import com.potato.potatotool.content.redTeam.vulnScanner.util.PocDeduplicator.DeduplicationResult;
import com.potato.potatotool.content.redTeam.vulnScanner.util.TagNormalizer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能 POC 筛选器
 * 从 UnifiedVulnScanService 提取的筛选逻辑：
 * 输入类型检测 → 指纹识别 → 7层筛选 → 去重
 *
 * @author Potato
 */
public class SmartPocSelector {

    private final PocRepository pocRepository;
    private final FingerprintService fingerprintService;
    private final PocDeduplicator deduplicator;
    private final ScanEventDispatcher eventDispatcher;
    private String scanId;

    public SmartPocSelector(PocRepository pocRepository, PocExecutor pocExecutor,
                            ScanEventDispatcher eventDispatcher) {
        this.pocRepository = pocRepository;
        this.fingerprintService = new FingerprintService(pocRepository, pocExecutor);
        this.deduplicator = new PocDeduplicator();
        this.eventDispatcher = eventDispatcher;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    /**
     * 完整筛选流程：输入类型检测 → 指纹识别 → selectPocs → 去重
     */
    public PocSelectionResult selectAndFilterPocs(String target, ScanConfig config) {
        // 1. 检测输入类型
        InputType inputType;
        if (config.isAutoDetectInputType()) {
            inputType = InputTypeDetector.detect(target);
            fireInfo(InfoType.INFO, "检测到输入类型: " + inputType);
        } else {
            inputType = config.getInputType();
        }

        // 2. 指纹识别（智能模式）
        FingerprintResult fingerprint = null;
        Set<String> fingerprintTags = Collections.emptySet();
        if (config.isSmartMode()) {
            fireInfo(InfoType.FINGERPRINT_STARTED, "开始指纹识别...");
            fingerprintService.setFingerprintTimeout(config.getFingerprintTimeout());
            fingerprint = fingerprintService.identify(target, inputType);
            if (fingerprint.hasFingerprint()) {
                fingerprintTags = fingerprint.getNormalizedTags();
                fireInfo(InfoType.FINGERPRINT_COMPLETED,
                    String.format("指纹识别完成: %s (识别出 %d 个技术栈)",
                        fingerprint.getFingerprintInfo().getProductName(), fingerprintTags.size()));
            } else {
                fireInfo(InfoType.FINGERPRINT_COMPLETED,
                    "指纹识别完成: 未识别出技术栈，将使用通用审计类 POC");
            }
        }

        // 3. 选择 POC
        List<PocObj.Poc> selectedPocs = selectPocs(config, inputType, fingerprintTags);

        // 4. 去重
        if (config.isEnableDeduplication() && selectedPocs.size() > 1) {
            DeduplicationResult dedupeResult = deduplicator.deduplicate(selectedPocs);
            if (dedupeResult.getSkippedCount() > 0) {
                fireInfo(InfoType.DEDUPLICATION,
                    String.format("去重完成: 保留 %d 个，跳过 %d 个重复 POC",
                        dedupeResult.getUniqueCount(), dedupeResult.getSkippedCount()));
            }
            selectedPocs = dedupeResult.getUniquePocs();
        }

        return new PocSelectionResult(selectedPocs, fingerprint);
    }

    /**
     * 根据配置选择 POC（7层筛选）
     */
    List<PocObj.Poc> selectPocs(ScanConfig config, InputType inputType, Set<String> fingerprintTags) {
        List<PocObj.Poc> result = new ArrayList<>();
        List<PocObj.Poc> allPocs = pocRepository.getAllPocs();

        int totalPocs = allPocs.size();
        int fingerprintPocs = 0;
        int vulnerabilityPocs = 0;
        int filteredByFormat = 0;
        int filteredByInputType = 0;
        int filteredByCategory = 0;
        int filteredByFlags = 0;
        int filteredByFingerprint = 0;
        int filteredByProtocol = 0;
        int filteredBySeverity = 0;
        int filteredByMode = 0;

        ScanConfig.ScanMode scanMode = config.getScanMode();

        for (PocObj.Poc poc : allPocs) {
            PocCategory category = poc.getCategory();
            PocObj.PocProtocolLayer protocolLayer = category.getProtocolLayer();
            PocObj.PocFunctionType functionType = category.getFunctionType();

            if (category == PocCategory.TECHNOLOGIES || category == PocCategory.NETWORK_DETECTION) {
                fingerprintPocs++;
            } else {
                vulnerabilityPocs++;
            }

            // 2. 按 POC 来源筛选
            if (!config.getEnabledPocFormats().contains(poc.getOriginalFormat())) {
                filteredByFormat++;
                continue;
            }

            // 3. 按协议层级筛选
            if (!protocolLayer.isApplicableFor(inputType)) {
                filteredByInputType++;
                continue;
            }

            // 3.5 按协议筛选（智能模式下有效）
            if (config.isSmartMode() && poc.getProtocol() != null) {
                String pocProtocol = poc.getProtocol().toLowerCase();
                String targetProtocol = config.getTargetProtocol();
                if (!pocProtocol.equals("http") && !pocProtocol.equals("https") && !pocProtocol.equals("network")) {
                    if (targetProtocol != null && !targetProtocol.isEmpty()) {
                        if (!pocProtocol.equalsIgnoreCase(targetProtocol)) {
                            filteredByProtocol++;
                            continue;
                        }
                    }
                }
            }

            // 4. 按扫描模式筛选
            if (!isCategoryAllowedForMode(category, functionType, scanMode, config)) {
                filteredByMode++;
                continue;
            }

            if (config.getExcludedCategories().contains(category)) {
                filteredByCategory++;
                continue;
            }

            if (!config.getEnabledCategories().isEmpty() &&
                !config.getEnabledCategories().contains(category)) {
                filteredByCategory++;
                continue;
            }

            // 4. 按标签筛选
            if (config.getTags() != null && !config.getTags().trim().isEmpty()) {
                if (poc.getTags() == null || poc.getTags().isEmpty()) {
                    filteredByCategory++;
                    continue;
                }
                String[] expectedTags = config.getTags().toLowerCase().split("\\s*,\\s*");
                boolean matchedTag = false;
                for (String expectedTag : expectedTags) {
                    if (expectedTag == null || expectedTag.trim().isEmpty()) {
                        continue;
                    }
                    for (String pocTag : poc.getTags()) {
                        if (pocTag != null && pocTag.equalsIgnoreCase(expectedTag.trim())) {
                            matchedTag = true;
                            break;
                        }
                    }
                    if (matchedTag) {
                        break;
                    }
                }
                if (!matchedTag) {
                    filteredByCategory++;
                    continue;
                }
            }

            // 5. 按严重程度筛选
            if (config.getMinSeverity() != null && poc.getSeverity() != null) {
                if (!isAboveSeverityThreshold(poc.getSeverity(), config.getMinSeverity())) {
                    filteredBySeverity++;
                    continue;
                }
            }

            // 6. 检查特殊 Flag
            if (poc.isRequiresHeadless() && !config.isEnableHeadless()) {
                filteredByFlags++;
                continue;
            }
            if (poc.isRequiresCode() && !config.isEnableCode()) {
                filteredByFlags++;
                continue;
            }
            if (poc.isRequiresFuzz() && !config.isEnableFuzz()) {
                filteredByFlags++;
                continue;
            }

            // 7. 智能模式：根据 Nuclei 官方分类流程筛选
            if (config.isSmartMode() && scanMode != ScanConfig.ScanMode.DISCOVERY) {
                if (category.isFingerprint()) {
                    filteredByFingerprint++;
                    continue;
                }

                if (category.requiresSpecialInput() && scanMode != ScanConfig.ScanMode.DEEP
                        && !isUserExplicitlyIncluded(category, config)) {
                    filteredByFingerprint++;
                    continue;
                }

                if (!fingerprintTags.isEmpty()) {
                    if (category.requiresFingerprint()) {
                        if (!TagNormalizer.matchesFingerprint(poc, fingerprintTags)) {
                            filteredByFingerprint++;
                            continue;
                        }
                    }
                } else {
                    if (category.requiresFingerprint()) {
                        filteredByFingerprint++;
                        continue;
                    }
                }
            }

            result.add(poc);
        }

        // 统计各阶段 POC 数量
        int genericAuditCount = (int) result.stream().filter(p -> p.getCategory().isGenericAudit()).count();
        int targetedVulnCount = (int) result.stream().filter(p -> p.getCategory().requiresFingerprint()).count();

        // 输出详细统计信息
        fireInfo(InfoType.INFO,
            String.format("POC 仓库统计: 总数 %d (指纹识别类 %d, 漏洞检测类 %d)",
                totalPocs, fingerprintPocs, vulnerabilityPocs));

        fireInfo(InfoType.INFO,
            String.format("扫描模式: %s (%s)", scanMode.name(), scanMode.getDescription()));

        if (config.isSmartMode() && !fingerprintTags.isEmpty()) {
            fireInfo(InfoType.INFO,
                String.format("智能筛选: 按指纹匹配过滤 %d 个 POC (匹配标签: %s)",
                    filteredByFingerprint, fingerprintTags.size() > 5 ?
                        fingerprintTags.stream().limit(5).collect(Collectors.joining(", ")) + "..." :
                        String.join(", ", fingerprintTags)));
            fireInfo(InfoType.INFO,
                String.format("最终选择: 针对性漏洞 %d 个 + 通用审计 %d 个 = %d 个 POC",
                    targetedVulnCount, genericAuditCount, result.size()));
        } else if (config.isSmartMode()) {
            fireInfo(InfoType.INFO,
                String.format("智能模式: 未识别出指纹，仅执行通用审计类 POC (%d 个)", result.size()));
            fireInfo(InfoType.INFO,
                "通用审计包括: 配置错误(misconfiguration)、信息泄露(exposures)、杂项检测(miscellaneous)");
        }

        fireInfo(InfoType.POC_SELECTION,
            String.format("筛选统计: 格式 %d, 输入类型 %d, 协议 %d, 模式 %d, 分类 %d, 严重度 %d, Flag %d, 指纹 %d => 最终 %d 个 POC",
                filteredByFormat, filteredByInputType, filteredByProtocol, filteredByMode,
                filteredByCategory, filteredBySeverity, filteredByFlags, filteredByFingerprint, result.size()));

        return result;
    }

    private boolean isCategoryAllowedForMode(PocCategory category, PocObj.PocFunctionType functionType,
                                             ScanConfig.ScanMode mode, ScanConfig config) {
        switch (mode) {
            case DISCOVERY:
                return functionType == PocObj.PocFunctionType.FINGERPRINT;
            case QUICK:
                return functionType == PocObj.PocFunctionType.VULNERABILITY
                    || functionType == PocObj.PocFunctionType.FINGERPRINT
                    || category == PocCategory.DEFAULT_LOGINS
                    || category == PocCategory.NETWORK_DEFAULT_LOGIN;
            case STANDARD:
                return functionType != PocObj.PocFunctionType.INFO_GATHERING
                    && functionType != PocObj.PocFunctionType.SPECIAL;
            case DEEP:
                return category != PocCategory.TOKEN_SPRAY
                    && category != PocCategory.CREDENTIAL_STUFFING;
            case OSINT:
                return functionType == PocObj.PocFunctionType.INFO_GATHERING
                    || functionType == PocObj.PocFunctionType.FINGERPRINT
                    || category == PocCategory.EXPOSURES;
            case COMPLIANCE:
                return functionType == PocObj.PocFunctionType.CONFIG_AUDIT;
            case CUSTOM:
            default:
                return true;
        }
    }

    private boolean isAboveSeverityThreshold(PocObj.Severity pocSeverity, PocObj.Severity threshold) {
        return getSeverityLevel(pocSeverity) >= getSeverityLevel(threshold);
    }

    private int getSeverityLevel(PocObj.Severity severity) {
        if (severity == null) return 0;
        switch (severity) {
            case INFO: return 1;
            case LOW: return 2;
            case MEDIUM: return 3;
            case HIGH: return 4;
            case CRITICAL: return 5;
            default: return 0;
        }
    }

    private boolean isUserExplicitlyIncluded(PocCategory category, ScanConfig config) {
        switch (category) {
            case OSINT:
            case TOKEN_SPRAY:
            case CREDENTIAL_STUFFING:
                return !config.getExcludedCategories().contains(category);
            default:
                return false;
        }
    }

    private void fireInfo(InfoType type, String message) {
        eventDispatcher.dispatchScanInfo(new ScanInfoEvent(this, scanId, type, message));
    }

    /**
     * 多目标筛选：逐目标指纹识别，POC 取并集去重
     */
    public MultiTargetSelectionResult selectAndFilterPocsMultiTarget(List<String> targets, ScanConfig config) {
        Map<String, FingerprintResult> fpMap = new LinkedHashMap<>();
        Map<String, PocObj.Poc> mergedPocs = new LinkedHashMap<>();

        for (String target : targets) {
            PocSelectionResult result = selectAndFilterPocs(target, config);
            if (result.getFingerprint() != null) {
                fpMap.put(target, result.getFingerprint());
            }
            for (PocObj.Poc poc : result.getPocs()) {
                mergedPocs.putIfAbsent(poc.getId(), poc);
            }
        }

        return new MultiTargetSelectionResult(new ArrayList<>(mergedPocs.values()), fpMap);
    }

    /**
     * POC 筛选结果（包含 POC 列表 + 指纹结果）
     */
    public static class PocSelectionResult {
        private final List<PocObj.Poc> pocs;
        private final FingerprintResult fingerprint;

        public PocSelectionResult(List<PocObj.Poc> pocs, FingerprintResult fingerprint) {
            this.pocs = pocs;
            this.fingerprint = fingerprint;
        }

        public List<PocObj.Poc> getPocs() {
            return pocs;
        }

        public FingerprintResult getFingerprint() {
            return fingerprint;
        }
    }

    /**
     * 多目标筛选结果（包含合并后的 POC 列表 + 每个目标的指纹映射）
     */
    public static class MultiTargetSelectionResult {
        private final List<PocObj.Poc> pocs;
        private final Map<String, FingerprintResult> fingerprintMap;

        public MultiTargetSelectionResult(List<PocObj.Poc> pocs, Map<String, FingerprintResult> fingerprintMap) {
            this.pocs = pocs;
            this.fingerprintMap = fingerprintMap;
        }

        public List<PocObj.Poc> getPocs() {
            return pocs;
        }

        public Map<String, FingerprintResult> getFingerprintMap() {
            return fingerprintMap;
        }
    }
}

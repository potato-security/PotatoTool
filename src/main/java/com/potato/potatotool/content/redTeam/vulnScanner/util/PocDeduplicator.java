package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

import java.util.*;

/**
 * POC 去重服务
 * 用于对多来源（Nuclei/Goby/Xray/Pocsuite）的 POC 进行去重
 * 
 * 去重策略：
 * 1. CVE ID 去重：同一 CVE 只保留一个 POC
 * 2. 名称去重：规范化名称后相同的去重
 * 
 * 注意：请求签名相同的 POC 不会被去重，而是在运行时复用同一个网络请求
 * 
 * @author Potato
 * @date 2025/12/11
 */
public class PocDeduplicator {
    
    /**
     * 去重优先级配置
     * 当发生重复时，优先保留哪种格式的 POC
     */
    private static final List<String> FORMAT_PRIORITY = Arrays.asList(
        "nuclei",   // 最高优先级
        "xray",
        "goby",
        "pocsuite"  // 最低优先级
    );
    
    /**
     * 对 POC 列表进行去重
     * 
     * @param pocs 原始 POC 列表
     * @return 去重结果（包含去重后的列表和跳过信息）
     */
    public DeduplicationResult deduplicate(List<PocObj.Poc> pocs) {
        if (pocs == null || pocs.isEmpty()) {
            return new DeduplicationResult();
        }
        
        DeduplicationResult result = new DeduplicationResult();
        
        // 按格式优先级排序，高优先级的在前面
        List<PocObj.Poc> sortedPocs = new ArrayList<>(pocs);
        sortedPocs.sort((p1, p2) -> {
            int priority1 = getFormatPriority(p1.getOriginalFormat());
            int priority2 = getFormatPriority(p2.getOriginalFormat());
            return Integer.compare(priority1, priority2);
        });
        
        // 去重索引
        Map<String, PocObj.Poc> uniqueByCve = new HashMap<>();     // CVE ID 去重
        Map<String, PocObj.Poc> uniqueByName = new HashMap<>();    // 规范化名称去重
        Map<String, PocObj.Poc> uniqueBySignature = new HashMap<>(); // 请求签名去重
        
        for (PocObj.Poc poc : sortedPocs) {
            String cveId = poc.getCveId();
            String normalizedName = normalizePocName(poc.getName());
            String signature = generatePocSignature(poc);
            
            // 1. CVE ID 去重（最严格）
            if (cveId != null && !cveId.isEmpty() && !cveId.equalsIgnoreCase("unknown")) {
                String normalizedCve = cveId.toUpperCase().trim();
                if (uniqueByCve.containsKey(normalizedCve)) {
                    PocObj.Poc existing = uniqueByCve.get(normalizedCve);
                    result.addSkipped(poc, SkipReason.CVE_DUPLICATE, 
                        "CVE重复: " + normalizedCve + " (保留: " + existing.getOriginalFormat() + "/" + existing.getId() + ")");
                    continue;
                }
                uniqueByCve.put(normalizedCve, poc);
            }
            
            // 2. 名称去重（中等严格）
            if (normalizedName != null && !normalizedName.isEmpty()) {
                if (uniqueByName.containsKey(normalizedName)) {
                    PocObj.Poc existing = uniqueByName.get(normalizedName);
                    // 如果已存在的 POC 没有 CVE 而当前有，替换
                    if (shouldReplace(existing, poc)) {
                        // 移除旧的，添加新的
                        result.removeUnique(existing);
                        result.addSkipped(existing, SkipReason.NAME_DUPLICATE, 
                            "名称重复但信息更少 (被替换为: " + poc.getOriginalFormat() + "/" + poc.getId() + ")");
                        uniqueByName.put(normalizedName, poc);
                    } else {
                        result.addSkipped(poc, SkipReason.NAME_DUPLICATE, 
                            "名称重复: " + normalizedName + " (保留: " + existing.getOriginalFormat() + "/" + existing.getId() + ")");
                        continue;
                    }
                } else {
                    uniqueByName.put(normalizedName, poc);
                }
            }
            
            // 3. 请求签名记录（不去重，只记录用于运行时复用请求）
            if (signature != null && !signature.isEmpty()) {
                if (uniqueBySignature.containsKey(signature)) {
                    // 请求签名相同的 POC 不去重，只记录以便运行时复用网络请求
                    result.addSignatureGroup(signature, poc);
                } else {
                    uniqueBySignature.put(signature, poc);
                    result.addSignatureGroup(signature, poc);
                }
            }
            
            // 通过 CVE/名称 去重检查
            result.addUnique(poc);
        }
        
        return result;
    }
    
    /**
     * 获取格式优先级
     */
    private int getFormatPriority(String format) {
        if (format == null) {
            return FORMAT_PRIORITY.size();
        }
        int index = FORMAT_PRIORITY.indexOf(format.toLowerCase());
        return index >= 0 ? index : FORMAT_PRIORITY.size();
    }
    
    /**
     * 规范化 POC 名称
     */
    private String normalizePocName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        
        return name.toLowerCase()
                   .trim()
                   // 移除常见前缀后缀
                   .replaceAll("^poc[-_]", "")
                   .replaceAll("[-_]poc$", "")
                   // 移除版本号
                   .replaceAll("[-_]v?\\d+(\\.\\d+)*$", "")
                   // 规范化分隔符
                   .replaceAll("[\\s_]+", "-")
                   // 移除特殊字符
                   .replaceAll("[^a-z0-9-]", "");
    }
    
    /**
     * 生成 POC 的请求签名
     * 用于检测发送相同请求的 POC
     */
    private String generatePocSignature(PocObj.Poc poc) {
        if (poc.getVerifySteps() == null || poc.getVerifySteps().isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (PocObj.PocStep step : poc.getVerifySteps()) {
            String method = step.getMethod() != null ? step.getMethod().toUpperCase() : "GET";
            String path = step.getPath() != null ? normalizePath(step.getPath()) : "/";
            
            sb.append(method).append(":").append(path);
            
            // 如果有 body，加入 body 的 hash
            if (step.getBody() != null && !step.getBody().isEmpty()) {
                sb.append(":").append(step.getBody().hashCode());
            }
            
            sb.append("|");
        }
        
        return sb.toString();
    }
    
    /**
     * 规范化路径（移除变量占位符）
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "/";
        }
        
        return path
            // 移除 Nuclei 变量 {{xxx}}
            .replaceAll("\\{\\{[^}]+\\}\\}", "*")
            // 移除 Xray 变量 {{xxx}}
            .replaceAll("\\{\\{[^}]+\\}\\}", "*")
            // 移除 Goby 变量 {xxx}
            .replaceAll("\\{[^}]+\\}", "*")
            // 规范化
            .toLowerCase();
    }
    
    /**
     * 判断是否应该用新 POC 替换旧 POC
     */
    private boolean shouldReplace(PocObj.Poc existing, PocObj.Poc newPoc) {
        // 如果新 POC 有 CVE 而旧的没有，替换
        if ((existing.getCveId() == null || existing.getCveId().isEmpty()) 
            && (newPoc.getCveId() != null && !newPoc.getCveId().isEmpty())) {
            return true;
        }
        
        // 如果新 POC 的格式优先级更高，替换
        int existingPriority = getFormatPriority(existing.getOriginalFormat());
        int newPriority = getFormatPriority(newPoc.getOriginalFormat());
        if (newPriority < existingPriority) {
            return true;
        }
        
        return false;
    }
    
    // ========== 内部类 ==========
    
    /**
     * 跳过原因枚举
     */
    public enum SkipReason {
        CVE_DUPLICATE,      // CVE ID 重复
        NAME_DUPLICATE,     // 名称重复
        SIGNATURE_DUPLICATE // 请求签名重复
    }
    
    /**
     * 去重结果
     */
    public static class DeduplicationResult {
        private List<PocObj.Poc> uniquePocs = new ArrayList<>();
        private List<SkippedPoc> skippedPocs = new ArrayList<>();
        private List<PocWarning> warnings = new ArrayList<>();
        private Map<String, List<PocObj.Poc>> signatureGroups = new HashMap<>();
        
        public void addUnique(PocObj.Poc poc) {
            uniquePocs.add(poc);
        }
        
        public void removeUnique(PocObj.Poc poc) {
            uniquePocs.remove(poc);
        }
        
        public void addSkipped(PocObj.Poc poc, SkipReason reason, String message) {
            skippedPocs.add(new SkippedPoc(poc, reason, message));
        }
        
        public void addWarning(PocObj.Poc poc, String message) {
            warnings.add(new PocWarning(poc, message));
        }
        
        public void addSignatureGroup(String signature, PocObj.Poc poc) {
            signatureGroups.computeIfAbsent(signature, k -> new ArrayList<>()).add(poc);
        }
        
        public Map<String, List<PocObj.Poc>> getSignatureGroups() {
            return signatureGroups;
        }
        
        public List<PocObj.Poc> getUniquePocs() {
            return uniquePocs;
        }
        
        public List<SkippedPoc> getSkippedPocs() {
            return skippedPocs;
        }
        
        public List<PocWarning> getWarnings() {
            return warnings;
        }
        
        public int getTotalCount() {
            return uniquePocs.size() + skippedPocs.size();
        }
        
        public int getUniqueCount() {
            return uniquePocs.size();
        }
        
        public int getSkippedCount() {
            return skippedPocs.size();
        }
        
        /**
         * 按跳过原因分组统计
         */
        public Map<SkipReason, Integer> getSkippedCountByReason() {
            Map<SkipReason, Integer> counts = new EnumMap<>(SkipReason.class);
            for (SkippedPoc skipped : skippedPocs) {
                counts.merge(skipped.getReason(), 1, Integer::sum);
            }
            return counts;
        }
        
        /**
         * 生成去重报告
         */
        public String generateReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("========== POC去重报告 ==========\n");
            sb.append(String.format("总计: %d 个 POC\n", getTotalCount()));
            sb.append(String.format("保留: %d 个 (%.1f%%)\n", getUniqueCount(), 
                getTotalCount() > 0 ? (getUniqueCount() * 100.0 / getTotalCount()) : 0));
            sb.append(String.format("跳过: %d 个 (%.1f%%)\n", getSkippedCount(),
                getTotalCount() > 0 ? (getSkippedCount() * 100.0 / getTotalCount()) : 0));
            
            if (!skippedPocs.isEmpty()) {
                sb.append("\n跳过原因统计:\n");
                Map<SkipReason, Integer> counts = getSkippedCountByReason();
                for (Map.Entry<SkipReason, Integer> entry : counts.entrySet()) {
                    sb.append(String.format("  - %s: %d 个\n", entry.getKey(), entry.getValue()));
                }
                
                // 显示部分跳过的 POC（最多10个）
                sb.append("\n跳过的POC示例（最多10个）:\n");
                skippedPocs.stream()
                    .limit(10)
                    .forEach(s -> sb.append(String.format("  - [%s] %s: %s\n", 
                        s.getPoc().getOriginalFormat(), s.getPoc().getId(), s.getMessage())));
                
                if (skippedPocs.size() > 10) {
                    sb.append(String.format("  ... 及其他 %d 个跳过的POC\n", skippedPocs.size() - 10));
                }
            }
            
            if (!warnings.isEmpty()) {
                sb.append(String.format("\n警告: %d 个\n", warnings.size()));
            }
            
            sb.append("==================================\n");
            return sb.toString();
        }
    }
    
    /**
     * 跳过的 POC 信息
     */
    public static class SkippedPoc {
        private final PocObj.Poc poc;
        private final SkipReason reason;
        private final String message;
        
        public SkippedPoc(PocObj.Poc poc, SkipReason reason, String message) {
            this.poc = poc;
            this.reason = reason;
            this.message = message;
        }
        
        public PocObj.Poc getPoc() { return poc; }
        public SkipReason getReason() { return reason; }
        public String getMessage() { return message; }
    }
    
    /**
     * POC 警告信息
     */
    public static class PocWarning {
        private final PocObj.Poc poc;
        private final String message;
        
        public PocWarning(PocObj.Poc poc, String message) {
            this.poc = poc;
            this.message = message;
        }
        
        public PocObj.Poc getPoc() { return poc; }
        public String getMessage() { return message; }
    }
}

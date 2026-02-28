package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.FingerprintInfo;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.InputType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.PocCategory;
import com.potato.potatotool.content.redTeam.vulnScanner.loader.PocRepository;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;
import com.potato.potatotool.content.redTeam.vulnScanner.util.TagNormalizer;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 指纹识别服务
 * 独立的指纹识别模块，支持所有输入类型
 * 用于智能扫描模式的前置指纹识别
 * 
 * @author Potato
 * @date 2025/12/11
 */
public class FingerprintService {
    
    private final PocRepository pocRepository;
    private final PocExecutor pocExecutor;
    
    // 指纹识别的默认超时时间（秒）
    private int fingerprintTimeout = 10;
    
    // 指纹识别的默认并发数
    private int fingerprintThreads = 10;
    
    // 指纹缓存（避免对同一目标重复识别）
    private final Map<String, FingerprintResult> fingerprintCache = new ConcurrentHashMap<>();
    
    // 缓存有效期（毫秒）
    private long cacheTtlMs = 300000; // 5 分钟
    
    public FingerprintService(PocRepository pocRepository, PocExecutor pocExecutor) {
        this.pocRepository = pocRepository;
        this.pocExecutor = pocExecutor;
    }
    
    /**
     * 执行指纹识别
     * 
     * @param target 目标（URL/IP:端口/域名）
     * @param inputType 输入类型
     * @return 指纹识别结果
     */
    public FingerprintResult identify(String target, InputType inputType) {
        if (target == null || target.isEmpty()) {
            return new FingerprintResult();
        }
        
        // 检查缓存
        String cacheKey = target + "|" + inputType;
        FingerprintResult cached = fingerprintCache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMs)) {
            return cached;
        }
        
        FingerprintResult result = new FingerprintResult();
        result.setTarget(target);
        result.setInputType(inputType);
        result.setStartTime(System.currentTimeMillis());
        
        try {
            // 根据输入类型选择指纹 POC
            List<PocObj.Poc> fingerprintPocs = selectFingerprintPocs(inputType);
            
            if (fingerprintPocs.isEmpty()) {
                result.setMessage("没有找到适用于 " + inputType + " 的指纹 POC");
                return result;
            }
            
            // 并行执行指纹识别
            ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(fingerprintThreads, fingerprintPocs.size()));
            
            List<Future<FingerprintMatch>> futures = new ArrayList<>();
            
            for (PocObj.Poc poc : fingerprintPocs) {
                futures.add(executor.submit(() -> executeFingerprintPoc(target, poc)));
            }
            
            // 收集结果
            for (Future<FingerprintMatch> future : futures) {
                try {
                    FingerprintMatch match = future.get(fingerprintTimeout, TimeUnit.SECONDS);
                    if (match != null && match.isMatched()) {
                        result.addMatch(match);
                    }
                } catch (TimeoutException e) {
                    // 超时，跳过此 POC
                } catch (Exception e) {
                    // 执行失败，跳过此 POC
                }
            }
            
            executor.shutdown();
            
            // 汇总结果
            result.summarize();
            
        } catch (Exception e) {
            result.setMessage("指纹识别失败: " + e.getMessage());
        }
        
        result.setEndTime(System.currentTimeMillis());
        
        // 存入缓存
        fingerprintCache.put(cacheKey, result);
        
        return result;
    }
    
    /**
     * 根据输入类型选择指纹 POC
     */
    private List<PocObj.Poc> selectFingerprintPocs(InputType inputType) {
        List<PocObj.Poc> result = new ArrayList<>();
        
        switch (inputType) {
            case URL:
                // HTTP 指纹：technologies + exposed-panels + honeypot
                result.addAll(pocRepository.findByCategory(PocCategory.TECHNOLOGIES));
                result.addAll(pocRepository.findByCategory(PocCategory.EXPOSED_PANELS));
                result.addAll(pocRepository.findByCategory(PocCategory.HONEYPOT));
                break;
                
            case IP_PORT:
                // 网络服务指纹：network/detection
                result.addAll(pocRepository.findByCategory(PocCategory.NETWORK_DETECTION));
                break;
                
            case DOMAIN:
                // DNS 配置检测
                result.addAll(pocRepository.findByCategory(PocCategory.DNS_CONFIG));
                break;
                
            default:
                // LOCAL_PATH/LOCAL_FILE/ANY 类型无需指纹识别，由SmartPocSelector使用通用审计POC
                break;
        }
        
        // 过滤掉需要特殊 flag 的 POC（如 headless、code）
        result = result.stream()
            .filter(p -> !p.isRequiresHeadless() && !p.isRequiresCode())
            .collect(Collectors.toList());
        
        return result;
    }
    
    /**
     * 执行单个指纹 POC
     */
    private FingerprintMatch executeFingerprintPoc(String target, PocObj.Poc poc) {
        FingerprintMatch match = new FingerprintMatch();
        match.setPocId(poc.getId());
        match.setPocName(poc.getName());
        
        try {
            ScanResult scanResult = pocExecutor.execute(target, poc);
            
            if (scanResult != null && scanResult.isVulnerable()) {
                match.setMatched(true);
                
                // 提取标签
                if (poc.getTags() != null) {
                    match.setTags(new ArrayList<>(poc.getTags()));
                }
                
                // 提取产品信息
                if (poc.getProduct() != null && !poc.getProduct().isEmpty()) {
                    match.setProduct(poc.getProduct());
                }
                
                // 提取版本信息（如果 POC 有提取器输出）
                if (scanResult.getDetails() != null) {
                    Object version = scanResult.getDetails().get("version");
                    if (version != null) {
                        match.setVersion(version.toString());
                    }
                    
                    Object server = scanResult.getDetails().get("server");
                    if (server != null) {
                        match.setServer(server.toString());
                    }
                    
                    Object technology = scanResult.getDetails().get("technology");
                    if (technology != null) {
                        match.setTechnology(technology.toString());
                    }
                }
            }
        } catch (Exception e) {
            // 执行失败
            match.setMatched(false);
            match.setError(e.getMessage());
        }
        
        return match;
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanupCache() {
        fingerprintCache.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(cacheTtlMs));
    }
    
    /**
     * 清空所有缓存
     */
    public void clearCache() {
        fingerprintCache.clear();
    }
    
    // ========== Getter/Setter ==========
    
    public int getFingerprintTimeout() {
        return fingerprintTimeout;
    }
    
    public void setFingerprintTimeout(int fingerprintTimeout) {
        this.fingerprintTimeout = fingerprintTimeout;
    }
    
    public int getFingerprintThreads() {
        return fingerprintThreads;
    }
    
    public void setFingerprintThreads(int fingerprintThreads) {
        this.fingerprintThreads = fingerprintThreads;
    }
    
    public long getCacheTtlMs() {
        return cacheTtlMs;
    }
    
    public void setCacheTtlMs(long cacheTtlMs) {
        this.cacheTtlMs = cacheTtlMs;
    }
    
    // ========== 内部类 ==========
    
    /**
     * 指纹识别结果
     */
    public static class FingerprintResult {
        private String target;
        private InputType inputType;
        private List<FingerprintMatch> matches = new ArrayList<>();
        private FingerprintInfo fingerprintInfo;
        private Set<String> normalizedTags = new HashSet<>();
        private long startTime;
        private long endTime;
        private String message;
        
        /**
         * 添加匹配结果
         */
        public void addMatch(FingerprintMatch match) {
            if (match != null) {
                matches.add(match);
            }
        }
        
        /**
         * 汇总识别结果
         */
        public void summarize() {
            fingerprintInfo = new FingerprintInfo();
            fingerprintInfo.setDetectedAt(System.currentTimeMillis());
            fingerprintInfo.setSource("nuclei-fingerprint");
            
            Set<String> allTags = new HashSet<>();
            
            for (FingerprintMatch match : matches) {
                // 收集标签
                if (match.getTags() != null) {
                    allTags.addAll(match.getTags());
                    fingerprintInfo.addDetectedTags(match.getTags());
                }
                
                // 设置产品信息（取第一个非空值）
                if (match.getProduct() != null && fingerprintInfo.getProductName() == null) {
                    fingerprintInfo.setProductName(match.getProduct());
                }
                
                // 设置版本信息
                if (match.getVersion() != null && fingerprintInfo.getVersion() == null) {
                    fingerprintInfo.setVersion(match.getVersion());
                }
                
                // 设置服务器信息
                if (match.getServer() != null && fingerprintInfo.getServer() == null) {
                    fingerprintInfo.setServer(match.getServer());
                }
                
                // 设置技术栈信息
                if (match.getTechnology() != null && fingerprintInfo.getTechnology() == null) {
                    fingerprintInfo.setTechnology(match.getTechnology());
                }
            }
            
            // 规范化标签
            normalizedTags = TagNormalizer.normalize(fingerprintInfo.getProductName(), 
                new ArrayList<>(allTags));
            fingerprintInfo.setNormalizedTags(normalizedTags);
            
            // 计算置信度
            if (!matches.isEmpty()) {
                fingerprintInfo.setConfidence(Math.min(1.0, matches.size() * 0.2));
            }
        }
        
        /**
         * 检查是否过期
         */
        public boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - endTime > ttlMs;
        }
        
        /**
         * 是否有有效的指纹信息
         */
        public boolean hasFingerprint() {
            return !matches.isEmpty() && !normalizedTags.isEmpty();
        }
        
        /**
         * 转换为 FingerprintInfo 对象
         */
        public FingerprintInfo toFingerprintInfo() {
            return fingerprintInfo;
        }
        
        // Getter/Setter
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        
        public InputType getInputType() { return inputType; }
        public void setInputType(InputType inputType) { this.inputType = inputType; }
        
        public List<FingerprintMatch> getMatches() { return matches; }
        public void setMatches(List<FingerprintMatch> matches) { this.matches = matches; }
        
        public FingerprintInfo getFingerprintInfo() { return fingerprintInfo; }
        public void setFingerprintInfo(FingerprintInfo fingerprintInfo) { this.fingerprintInfo = fingerprintInfo; }
        
        public Set<String> getNormalizedTags() { return normalizedTags; }
        public void setNormalizedTags(Set<String> normalizedTags) { this.normalizedTags = normalizedTags; }
        
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public long getDuration() { return endTime - startTime; }
    }
    
    /**
     * 单个指纹匹配结果
     */
    public static class FingerprintMatch {
        private String pocId;
        private String pocName;
        private boolean matched;
        private String product;
        private String version;
        private String server;
        private String technology;
        private List<String> tags;
        private String error;
        
        // Getter/Setter
        public String getPocId() { return pocId; }
        public void setPocId(String pocId) { this.pocId = pocId; }
        
        public String getPocName() { return pocName; }
        public void setPocName(String pocName) { this.pocName = pocName; }
        
        public boolean isMatched() { return matched; }
        public void setMatched(boolean matched) { this.matched = matched; }
        
        public String getProduct() { return product; }
        public void setProduct(String product) { this.product = product; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getServer() { return server; }
        public void setServer(String server) { this.server = server; }
        
        public String getTechnology() { return technology; }
        public void setTechnology(String technology) { this.technology = technology; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}

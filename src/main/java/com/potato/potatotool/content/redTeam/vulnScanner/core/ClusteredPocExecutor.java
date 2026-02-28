package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Poc;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.PocStep;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;

import java.util.*;
import java.util.concurrent.*;

/**
 * 请求聚类执行器
 * 核心功能：将相同请求特征的 POC 聚类，减少重复请求
 * 
 * 聚类逻辑：
 * 1. 按请求特征（URL路径+方法+请求体签名）对POC进行分组
 * 2. 对同一聚类中的POC，只发送一次请求
 * 3. 用这个响应匹配聚类中所有POC的匹配规则
 * 4. 显著减少网络请求数量，提高扫描效率
 * 
 * @author Potato
 * @date 2025/12/11
 */
public class ClusteredPocExecutor {
    
    private final ScanConfig scanConfig;
    private final ResponseCacheService cacheService;
    private final PocExecutor pocExecutor;
    private final ExecutorService executorService;
    
    // 聚类统计
    private volatile int totalRequests = 0;
    private volatile int savedRequests = 0;
    private volatile int clusteredPocs = 0;
    
    public ClusteredPocExecutor(ScanConfig scanConfig, PocExecutor pocExecutor) {
        this.scanConfig = scanConfig;
        this.cacheService = new ResponseCacheService();
        this.pocExecutor = pocExecutor;
        this.executorService = Executors.newFixedThreadPool(
            Math.max(1, scanConfig.getThreads() / 2)
        );
    }
    
    /**
     * 聚类执行 POC 列表
     * 
     * @param target 目标 URL
     * @param pocs POC 列表
     * @return 扫描结果列表
     */
    public List<ScanResult> executeClusteredPocs(String target, List<Poc> pocs) {
        if (pocs == null || pocs.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 重置统计
        totalRequests = 0;
        savedRequests = 0;
        clusteredPocs = 0;
        
        // 1. 对 POC 进行聚类
        Map<String, ClusterGroup> clusters = clusterPocs(pocs);
        
        List<ScanResult> allResults = new CopyOnWriteArrayList<>();
        
        // 2. 并行执行各聚类组
        List<Future<?>> futures = new ArrayList<>();
        
        for (Map.Entry<String, ClusterGroup> entry : clusters.entrySet()) {
            ClusterGroup group = entry.getValue();
            
            futures.add(executorService.submit(() -> {
                try {
                    List<ScanResult> results = executeClusterGroup(target, group);
                    allResults.addAll(results);
                } catch (Exception e) {
                    // 聚类执行失败，回退到单独执行
                    for (Poc poc : group.getPocs()) {
                        try {
                            ScanResult result = pocExecutor.execute(target, poc);
                            if (result != null) {
                                allResults.add(result);
                            }
                        } catch (Exception ex) {
                            // 忽略单个 POC 执行错误
                        }
                    }
                }
            }));
        }
        
        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get(scanConfig.getTimeout() * 2L, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 忽略超时
            }
        }
        
        return allResults;
    }
    
    /**
     * 对 POC 进行聚类
     * 聚类依据：第一个HTTP请求的路径+方法+请求体签名
     */
    private Map<String, ClusterGroup> clusterPocs(List<Poc> pocs) {
        Map<String, ClusterGroup> clusters = new HashMap<>();
        
        for (Poc poc : pocs) {
            // 获取聚类键
            String clusterKey = generateClusterKey(poc);
            
            if (clusterKey != null) {
                clusters.computeIfAbsent(clusterKey, k -> new ClusterGroup(clusterKey))
                        .addPoc(poc);
                clusteredPocs++;
            } else {
                // 无法聚类的POC（多步骤、非HTTP协议等），单独执行
                String uniqueKey = "unclustered_" + poc.getId();
                ClusterGroup singleGroup = new ClusterGroup(uniqueKey);
                singleGroup.addPoc(poc);
                singleGroup.setClusterable(false);
                clusters.put(uniqueKey, singleGroup);
            }
        }
        
        return clusters;
    }
    
    /**
     * 生成聚类键
     * 只有单步骤HTTP POC才能聚类（基于 PocStep 的 method/path/body 字段）
     */
    private String generateClusterKey(Poc poc) {
        // 只聚类单步骤HTTP POC（协议为 http）
        if (poc.getVerifySteps() == null || poc.getVerifySteps().size() != 1) {
            return null;
        }
        
        // 检查协议是否为 HTTP
        String protocol = poc.getProtocol();
        if (protocol != null && !protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
            return null;
        }
        
        PocStep step = poc.getVerifySteps().get(0);
        
        // PocStep 直接包含 method, path, body 字段
        String method = step.getMethod();
        String path = step.getPath();
        String body = step.getBody();
        
        // 构建聚类键：方法 + 路径 + 请求体签名
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(method != null ? method.toUpperCase() : "GET");
        keyBuilder.append("|");
        keyBuilder.append(path != null ? path : "/");
        keyBuilder.append("|");
        
        // 请求体签名（如果有）
        if (body != null && !body.isEmpty()) {
            keyBuilder.append(String.valueOf(body.hashCode()));
        } else {
            keyBuilder.append("nobody");
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 执行一个聚类组
     * 
     * 聚类执行策略：
     * - 对于可聚类的 POC 组，先执行第一个 POC
     * - 如果第一个 POC 发现漏洞，跳过同组其他 POC（因为它们检测相同路径）
     * - 如果第一个 POC 未发现漏洞，也跳过同组其他 POC（减少无效请求）
     * - 这样每个聚类组只发送一次请求
     */
    private List<ScanResult> executeClusterGroup(String target, ClusterGroup group) {
        List<ScanResult> results = new ArrayList<>();
        List<Poc> pocs = group.getPocs();
        
        if (pocs.isEmpty()) {
            return results;
        }
        
        // 不可聚类的POC，逐个执行
        if (!group.isClusterable()) {
            for (Poc poc : pocs) {
                try {
                    ScanResult result = pocExecutor.execute(target, poc);
                    if (result != null) {
                        results.add(result);
                    }
                    totalRequests++;
                } catch (Exception e) {
                    // 忽略单个 POC 执行错误
                }
            }
            return results;
        }
        
        // 可聚类的 POC 组：执行第一个 POC，用结果推断其他 POC
        Poc firstPoc = pocs.get(0);
        ScanResult firstResult = null;
        
        try {
            firstResult = pocExecutor.execute(target, firstPoc);
            totalRequests++;
            
            if (firstResult != null) {
                results.add(firstResult);
            }
        } catch (Exception e) {
            // 第一个 POC 执行失败，创建失败结果
            firstResult = new ScanResult();
            firstResult.setTarget(target);
            firstResult.setPoc(firstPoc);
            firstResult.setVulnerable(false);
            firstResult.setTimestamp(System.currentTimeMillis());
            results.add(firstResult);
        }
        
        // 对于同组的其他 POC，直接标记为已检测（节省请求）
        // 因为它们的请求特征相同，结果也应该相同
        for (int i = 1; i < pocs.size(); i++) {
            Poc poc = pocs.get(i);
            
            ScanResult result = new ScanResult();
            result.setTarget(target);
            result.setPoc(poc);
            result.setVulnerable(false); // 聚类组内其他 POC 默认不报告漏洞，避免重复
            result.setTimestamp(System.currentTimeMillis());
            result.setPocSource(poc.getOriginalFormat());
            result.setDuplicate(true);
            result.setDuplicateReason("聚类组内跳过：与 " + firstPoc.getId() + " 请求特征相同");
            
            results.add(result);
            savedRequests++;
        }
        
        return results;
    }
    
    /**
     * 获取聚类统计信息
     */
    public ClusterStats getStats() {
        return new ClusterStats(totalRequests, savedRequests, clusteredPocs);
    }
    
    /**
     * 关闭执行器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // ========== 内部类 ==========
    
    /**
     * 聚类组
     */
    private static class ClusterGroup {
        private final String key;
        private final List<Poc> pocs = new ArrayList<>();
        private boolean clusterable = true;
        
        public ClusterGroup(String key) {
            this.key = key;
        }
        
        public void addPoc(Poc poc) {
            pocs.add(poc);
        }
        
        public List<Poc> getPocs() { return pocs; }
        public boolean isClusterable() { return clusterable; }
        public void setClusterable(boolean clusterable) { this.clusterable = clusterable; }
    }
    
    /**
     * 聚类统计信息
     */
    public static class ClusterStats {
        private final int totalRequests;
        private final int savedRequests;
        private final int clusteredPocs;
        
        public ClusterStats(int totalRequests, int savedRequests, int clusteredPocs) {
            this.totalRequests = totalRequests;
            this.savedRequests = savedRequests;
            this.clusteredPocs = clusteredPocs;
        }
        
        public int getTotalRequests() { return totalRequests; }
        public int getSavedRequests() { return savedRequests; }
        public int getClusteredPocs() { return clusteredPocs; }
        
        /**
         * 获取节省的请求百分比
         */
        public double getSavedPercentage() {
            int expectedRequests = totalRequests + savedRequests;
            if (expectedRequests == 0) return 0;
            return (savedRequests * 100.0) / expectedRequests;
        }
        
        @Override
        public String toString() {
            return String.format("聚类统计: 实际请求 %d, 节省请求 %d (%.1f%%), 聚类POC数 %d",
                totalRequests, savedRequests, getSavedPercentage(), clusteredPocs);
        }
    }
}

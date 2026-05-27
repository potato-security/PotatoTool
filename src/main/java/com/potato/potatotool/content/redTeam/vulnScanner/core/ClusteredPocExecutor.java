package com.potato.potatotool.content.redTeam.vulnScanner.core;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.Poc;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanConfig;
import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 请求聚类执行器
 * 核心功能：将相同请求特征的 POC 聚类，减少重复请求
 * 
 * 聚类逻辑：
 * 1. 按完整请求签名对POC进行分组
 * 2. 同组 POC 仍各自执行 matcher/extractor
 * 3. 真正的重复网络请求由 PocExecutor 响应缓存复用
 * 4. 避免“相同请求、不同匹配规则”被错误跳过
 * 
 * @author Potato
 * @date 2025/12/11
 */
public class ClusteredPocExecutor {
    
    private final ScanConfig scanConfig;
    private final PocExecutor pocExecutor;
    private final ExecutorService executorService;
    private final RequestSignatureService requestSignatureService;
    private volatile boolean shutdownRequested = false;
    
    // 聚类统计
    private volatile int totalRequests = 0;
    private volatile int savedRequests = 0;
    private volatile int clusteredPocs = 0;
    
    public ClusteredPocExecutor(ScanConfig scanConfig, PocExecutor pocExecutor) {
        this.scanConfig = scanConfig;
        this.pocExecutor = pocExecutor;
        this.requestSignatureService = new RequestSignatureService();
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
        return executeClusteredPocs(target, pocs, null);
    }

    /**
     * 聚类执行 POC 列表（带实时回调）
     * 每个 ScanResult 产生时立即触发 onResult.accept(result)，用于进度上报与逐项处理。
     * 回调和批量返回都会拿到全部结果，调用方可二选一，也可同时使用。
     *
     * @param target 目标 URL
     * @param pocs POC 列表
     * @param onResult 单个结果回调（可为 null）
     * @return 扫描结果列表
     */
    public List<ScanResult> executeClusteredPocs(String target, List<Poc> pocs, Consumer<ScanResult> onResult) {
        if (pocs == null || pocs.isEmpty()) {
            return Collections.emptyList();
        }
        if (shutdownRequested || Thread.currentThread().isInterrupted()) {
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

            try {
                futures.add(executorService.submit(() -> {
                    if (shutdownRequested || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    try {
                        List<ScanResult> results = executeClusterGroup(target, group);
                        if (shutdownRequested || Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        notifyResults(results, onResult);
                        allResults.addAll(results);
                    } catch (Exception e) {
                        if (shutdownRequested || Thread.currentThread().isInterrupted()) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        // 聚类执行失败，回退到单独执行
                        for (Poc poc : group.getPocs()) {
                            if (shutdownRequested || Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            try {
                                ScanResult result = pocExecutor.execute(target, poc);
                                if (result != null && !shutdownRequested && !Thread.currentThread().isInterrupted()) {
                                    notifyResult(result, onResult);
                                    allResults.add(result);
                                }
                            } catch (Exception ex) {
                                if (Thread.currentThread().isInterrupted()) {
                                    return;
                                }
                                // 忽略单个 POC 执行错误
                            }
                        }
                    }
                }));
            } catch (RejectedExecutionException e) {
                if (!shutdownRequested) {
                    throw e;
                }
            }
        }

        // 等待所有任务真正完成。不能用固定 future 超时后直接返回，否则 UI 会提前显示完成，
        // 而内部线程仍在继续发请求和输出重试日志。
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                cancelFutures(futures);
                Thread.currentThread().interrupt();
                throw new CancellationException("聚类扫描被中断");
            } catch (CancellationException e) {
                cancelFutures(futures);
                throw e;
            } catch (ExecutionException e) {
                if (shutdownRequested) {
                    cancelFutures(futures);
                    break;
                }
            }
        }

        return allResults;
    }

    private void cancelFutures(List<Future<?>> futures) {
        if (futures == null) {
            return;
        }
        for (Future<?> future : futures) {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private void notifyResults(List<ScanResult> results, Consumer<ScanResult> onResult) {
        if (onResult == null || results == null || results.isEmpty()) {
            return;
        }
        for (ScanResult r : results) {
            notifyResult(r, onResult);
        }
    }

    private void notifyResult(ScanResult result, Consumer<ScanResult> onResult) {
        if (onResult == null || result == null) {
            return;
        }
        try {
            onResult.accept(result);
        } catch (Exception ignore) {
            // 回调异常不影响主流程
        }
    }
    
    /**
     * 对 POC 进行聚类
     * 聚类依据：第一个HTTP请求的完整请求签名。
     * 注意：聚类只决定调度分组，真实网络复用由 PocExecutor 的响应缓存完成，避免不同 matcher 被误跳过。
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
     * 生成聚类键。
     * 只有单步骤、无运行时依赖的 HTTP POC 才能聚类。签名必须包含方法、路径、body、headers、
     * cookie、raw 请求等会影响响应的输入，避免请求不完全一致时误合并。
     */
    private String generateClusterKey(Poc poc) {
        RequestSignatureService.RequestSignature signature = requestSignatureService.buildForPoc(poc);
        return signature.isCoalescible() ? signature.getSignatureKey() : null;
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
                if (shutdownRequested || Thread.currentThread().isInterrupted()) {
                    break;
                }
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
        
        for (Poc poc : pocs) {
            if (shutdownRequested || Thread.currentThread().isInterrupted()) {
                break;
            }
            try {
                ScanResult result = pocExecutor.execute(target, poc);
                if (result != null) {
                    results.add(result);
                }
                totalRequests++;
            } catch (Exception e) {
                ScanResult result = new ScanResult();
                result.setTarget(target);
                result.setPoc(poc);
                result.setVulnerable(false);
                result.setTimestamp(System.currentTimeMillis());
                results.add(result);
            }
        }
        savedRequests += Math.max(0, pocs.size() - 1);
        
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
        shutdownRequested = true;
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

    /**
     * 立即取消等待中和排队中的聚类任务。
     */
    public void shutdownNow() {
        shutdownRequested = true;
        executorService.shutdownNow();
    }

    public boolean isShutdown() {
        return executorService.isShutdown() || executorService.isTerminated();
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

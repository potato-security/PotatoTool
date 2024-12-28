package com.potato.potatotool.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池管理器，支持全局线程池管理
 * @author Potato
 * @date 2024/7/4
 */
public class ExecutorServiceManager {
    // 线程池名常量定义
    public static final class ExecutorPoolNames {
        public static final String AES_DECRYPT = "AesDecrypt";
        public static final String DES_DECRYPT = "DesDecrypt";
        public static final String BLOWFISH_DECRYPT = "BlowfishDecrypt";
        public static final String XOR_DECRYPT = "XorDecrypt";
        public static final String SHIRO_DECRYPT = "ShiroDecrypt";
        public static final List<String> DECRYPT_ARRAY = Arrays.asList(
                AES_DECRYPT,
                DES_DECRYPT,
                BLOWFISH_DECRYPT,
                XOR_DECRYPT,
                SHIRO_DECRYPT
        );

        public static final String ASSET = "Asset";
        public static final String GETSUBDOMAIN_ASSET = "GetSubDomainAsset";
        public static final String AIQICHA_ASSET = "AiqichaAsset";
        public static final String GETSEO_ASSET = "GetSeoAsset";
        public static final String GETDOMAIN_ASSET = "GetDomainAsset";
        public static final String SUBDOMAINBURTEFORCER_ASSET = "SubDomainBurteForcerAsset";
        public static final List<String> ASSET_ARRAY = Arrays.asList(
                ASSET,
                GETSUBDOMAIN_ASSET,
                AIQICHA_ASSET,
                GETSEO_ASSET,
                GETDOMAIN_ASSET,
                SUBDOMAINBURTEFORCER_ASSET
        );

        public static final String KB_CHECK = "KbCheck";
        public static final String KB_UID_CHECK = "KbUidCheck";
        public static final List<String> KB_ARRAY = Arrays.asList(
                KB_CHECK,
                KB_UID_CHECK
        );

        // 防止实例化
        private ExecutorPoolNames() {}
    }

    // 线程池缓存
    private static final Map<String, ExecutorService> EXECUTOR_CACHE = new ConcurrentHashMap<>();

    // 单例模式
    private static final ExecutorServiceManager INSTANCE = new ExecutorServiceManager();

    // 私有构造函数
    private ExecutorServiceManager() {}

    // 获取单例实例
    public static ExecutorServiceManager getInstance() {
        return INSTANCE;
    }

    public ExecutorService getExecutor() {
        return getExecutor("default");
    }

    /**
     * 自定义线程工厂
     */
    private static class EnhancedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean isDaemon;
        private final int priority;

        public EnhancedThreadFactory(String poolName, boolean isDaemon, int priority) {
            this.namePrefix = "pool-" + poolName + "-thread-";
            this.isDaemon = isDaemon;
            this.priority = priority;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(isDaemon);
            t.setPriority(priority);
            return t;
        }
    }


    /**
     * 创建自定义线程池
     * @param poolName 线程池名称
     * @param config 线程池配置
     * @return ExecutorService
     */
    public static ExecutorService getOrCreateExecutor(String poolName, ExecutorConfig config) {
        // 检查是否已存在
        return EXECUTOR_CACHE.computeIfAbsent(poolName, k -> {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    config.getCorePoolSize(),
                    config.getMaxPoolSize(),
                    config.getKeepAliveTime(),
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(config.getQueueCapacity()),
                    new EnhancedThreadFactory(
                            poolName,
                            config.isDaemon(),
                            config.getPriority()
                    )
            );

            // 设置拒绝策略
            executor.setRejectedExecutionHandler(config.getRejectedHandler());

            return executor;
        });
    }
    public static ExecutorService getOrCreateExecutor(String poolName) {
        return getOrCreateExecutor(poolName, ExecutorConfig.defaultConfig());
    }

    /**
     * 获取默认线程池
     * @return ExecutorService
     */
    public ExecutorService getDefaultExecutor() {
        return getOrCreateExecutor("default", ExecutorConfig.defaultConfig());
    }

    /**
     * 获取已存在的线程池
     * @param poolName 线程池名称
     * @return ExecutorService
     */
    public ExecutorService getExecutor(String poolName) {
        return EXECUTOR_CACHE.get(poolName);
    }

    /**
     * 关闭指定线程池
     * @param poolName 线程池名称
     */
    public static void shutdownExecutor(String poolName) {
        ExecutorService executor = EXECUTOR_CACHE.get(poolName);
        if (executor != null) {
            if (executor instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                // 尝试取消队列中的所有任务
                threadPoolExecutor.getQueue().forEach(task -> {
                    if (task instanceof Future<?>) {
                        ((Future<?>) task).cancel(true);
                    }
                });
            }
            executor.shutdownNow();
            EXECUTOR_CACHE.remove(poolName);
        }
    }
    public static void shutdownExecutor(List<String> poolNameArray) {
        for(String poolName : poolNameArray) {
            ExecutorService executor = EXECUTOR_CACHE.get(poolName);
            if (executor != null) {
                if (executor instanceof ThreadPoolExecutor) {
                    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                    // 尝试取消队列中的所有任务
                    threadPoolExecutor.getQueue().forEach(task -> {
                        if (task instanceof Future<?>) {
                            ((Future<?>) task).cancel(true);
                        }
                    });
                }
                executor.shutdownNow();
                EXECUTOR_CACHE.remove(poolName);
            }
        }
    }


    /**
     * 关闭所有线程池
     */
    public static void shutdownAll() {
        EXECUTOR_CACHE.values().forEach(executor -> {
            if (executor instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                // 尝试取消队列中的所有任务
                threadPoolExecutor.getQueue().forEach(task -> {
                    if (task instanceof Future<?>) {
                        ((Future<?>) task).cancel(true);
                    }
                });
            }
            executor.shutdownNow();
        });
        EXECUTOR_CACHE.clear();
    }


    /**
     * 提交任务
     * @param poolName 线程池名称
     * @param task 任务
     * @return Future
     */
    public Future<?> submitTask(String poolName, Runnable task) {
        ExecutorService executor = EXECUTOR_CACHE.getOrDefault(
                poolName,
                getDefaultExecutor()
        );
        return executor.submit(task);
    }


    /**
     * 线程池配置类
     */
    public static class ExecutorConfig {
        private int corePoolSize = calculateCorePoolSize();
        private int maxPoolSize = calculateMaxPoolSize();
        private long keepAliveTime = 10L;
        private int queueCapacity = 1024;
        private boolean daemon = true; // 守护进程-任务完成后线程自动销毁，不会阻止JVM关闭
        private int priority = Thread.NORM_PRIORITY;
        private RejectedExecutionHandler rejectedHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        // Getter and Setter methods
        public static ExecutorConfig defaultConfig() {
            return new ExecutorConfig();
        }

        // 计算核心线程数
        private static int calculateCorePoolSize() {
            return Math.max(Runtime.getRuntime().availableProcessors(), 2);
        }

        // 计算最大线程数
        private static int calculateMaxPoolSize() {
            return Runtime.getRuntime().availableProcessors() * 2 + 1;
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public long getKeepAliveTime() {
            return keepAliveTime;
        }

        public void setKeepAliveTime(long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public boolean isDaemon() {
            return daemon;
        }

        public void setDaemon(boolean daemon) {
            this.daemon = daemon;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public RejectedExecutionHandler getRejectedHandler() {
            return rejectedHandler;
        }

        public void setRejectedHandler(RejectedExecutionHandler rejectedHandler) {
            this.rejectedHandler = rejectedHandler;
        }
    }
}
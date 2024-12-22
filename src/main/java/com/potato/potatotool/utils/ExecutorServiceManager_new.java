package com.potato.potatotool.utils;

import com.google.common.util.concurrent.*;
import lombok.Builder;
import lombok.Data;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Potato
 * @date 2024/7/4 10:45
 */

public class ExecutorServiceManager_new {

    /**
     * 线程池配置
     */
    @Data
    @Builder
    public static class ThreadPoolConfig {
        private String poolName;
        private int corePoolSize;
        private int maximumPoolSize;
        private long keepAliveTime;
        private int queueCapacity;
        private RejectedExecutionHandler rejectedExecutionHandler;

        public static ThreadPoolConfig defaultConfig(String poolName) {
            return ThreadPoolConfig.builder()
                    .poolName(poolName)
                    .corePoolSize(Runtime.getRuntime().availableProcessors())
                    .maximumPoolSize(Runtime.getRuntime().availableProcessors() * 2)
                    .keepAliveTime(60L)
                    .queueCapacity(1000)
                    .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                    .build();
        }

        public void validate() {
            if (corePoolSize <= 0 || maximumPoolSize <= 0) {
                throw new IllegalArgumentException("核心和最大池大小必须大于0");
            }
            if (corePoolSize > maximumPoolSize) {
                throw new IllegalArgumentException("核心池大小不能大于最大池大小");
            }
        }
    }

    /**
     * 自定义线程工厂
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" + name + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    /**
     * 可监控的线程池执行器
     */
    public static class MonitorableThreadPoolExecutor extends ThreadPoolExecutor {
        private final String poolName;
        private final ThreadPoolMetrics metrics;

        public MonitorableThreadPoolExecutor(ThreadPoolConfig config) {
            super(config.getCorePoolSize(),
                    config.getMaximumPoolSize(),
                    config.getKeepAliveTime(),
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(config.getQueueCapacity()),
                    new NamedThreadFactory(config.getPoolName()),
                    config.getRejectedExecutionHandler());

            this.poolName = config.getPoolName();
            this.metrics = new ThreadPoolMetrics();
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            metrics.taskStarted();
            super.beforeExecute(t, r);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            metrics.taskCompleted();
            if (t != null) {
                metrics.taskFailed();
            }
            super.afterExecute(r, t);
        }

        public ThreadPoolMetrics getMetrics() {
            return metrics;
        }
    }

    /**
     * 线程池指标统计
     */
    @Data
    public static class ThreadPoolMetrics {
        private final AtomicInteger activeThreads = new AtomicInteger(0);
        private final AtomicInteger completedTasks = new AtomicInteger(0);
        private final AtomicInteger failedTasks = new AtomicInteger(0);
        private final AtomicInteger queuedTasks = new AtomicInteger(0);

        public void taskStarted() {
            activeThreads.incrementAndGet();
            queuedTasks.decrementAndGet();
        }

        public void taskCompleted() {
            activeThreads.decrementAndGet();
            completedTasks.incrementAndGet();
        }

        public void taskFailed() {
            failedTasks.incrementAndGet();
        }

        public void taskQueued() {
            queuedTasks.incrementAndGet();
        }

        public void logMetrics() {
            System.out.println(String.format("活动: %d, 已完成: %d, 失败: %d, 排队: %d",
                    activeThreads.get(), completedTasks.get(), failedTasks.get(), queuedTasks.get()));
        }
    }

    private final ConcurrentHashMap<String, ListeningExecutorService> threadPools = new ConcurrentHashMap<>();

    /**
     * 创建或获取线程池
     */
    public ListeningExecutorService getOrCreateThreadPool(String poolName) {
        return threadPools.computeIfAbsent(poolName, name -> {
            ThreadPoolConfig config = ThreadPoolConfig.defaultConfig(name);
            config.validate();
            return createThreadPool(config);
        });
    }

    /**
     * 使用自定义配置创建线程池
     */
    public ListeningExecutorService createThreadPool(ThreadPoolConfig config) {
        MonitorableThreadPoolExecutor executor = new MonitorableThreadPoolExecutor(config);
        return MoreExecutors.listeningDecorator(executor);
    }

    /**
     * 提交任务到指定线程池
     */
    public <T> ListenableFuture<T> submit(String poolName, Callable<T> task) {
        ListeningExecutorService executor = getOrCreateThreadPool(poolName);
        return executor.submit(task);
    }

    /**
     * 关闭所有线程池
     */
    public void shutdownAll() {
        threadPools.forEach((name, executor) -> {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                threadPools.remove(name);
            }
        });
    }
}

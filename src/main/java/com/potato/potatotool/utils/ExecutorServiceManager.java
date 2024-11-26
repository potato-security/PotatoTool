package com.potato.potatotool.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Potato
 * @date 2024/7/4 10:45
 */

public class ExecutorServiceManager {

    private final ExecutorService executor;
    public static List<Future<?>> futures;

    // 私有构造函数，外部不能实例化
    public ExecutorServiceManager() {
        // 通过读取配置或设置默认值来初始化线程池
//        int poolSize = 10; // 这里可以通过配置文件或环境变量来设置
//        this.executor = Executors.newFixedThreadPool(poolSize);
        this.executor = ForkJoinPool.commonPool();
        this.futures = new ArrayList<>();
    }

    // 内部静态类，实现懒加载单例模式
    private static class Holder {
        private static final ExecutorServiceManager INSTANCE = new ExecutorServiceManager();
    }

    // 获取单例实例的方法
    public static ExecutorServiceManager getInstance() {
        return Holder.INSTANCE;
    }

    // 获取 ExecutorService
    public ExecutorService getExecutor() {
        return this.executor;
    }

    // 强制关闭，立即中断任务
    public void forceShutdown() {
//        System.out.println("强制关闭，立即中断任务");
        if (executor != null && !executor.isShutdown()) {
            for (Future<?> future : futures) {
                try {
                    future.cancel(true); // 尝试取消每个任务
                }catch (Exception e){}
            }
            futures.clear(); // 清空列表

            executor.shutdownNow();
        }
    }

    // 计算可用最大线程数
    public static int getOptimalThreadPoolSize() {
        int cores = Runtime.getRuntime().availableProcessors(); // 获取可用处理器核心数
        long freeMemory = Runtime.getRuntime().freeMemory(); // 获取可用内存

        // 根据内存和核心数计算线程池大小
        int optimalSize = (int) Math.min(cores * 2, freeMemory / (1024 * 1024 * 5)); // 假设每个线程占用大约5MB的内存
        return Math.max(optimalSize, 1); // 确保至少有一个线程
    }

    // 查询线程池状态
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    public boolean isTerminated() {
        return executor.isTerminated();
    }
}

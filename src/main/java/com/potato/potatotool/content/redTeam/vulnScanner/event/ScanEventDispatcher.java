package com.potato.potatotool.content.redTeam.vulnScanner.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 扫描事件分发器
 * 负责管理事件监听器和分发事件
 * 
 * @author Potato
 * @date 2024-10-28
 */
public class ScanEventDispatcher {
    private final List<ScanEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService;
    private final boolean asyncDispatch;
    
    /**
     * 创建同步事件分发器
     */
    public ScanEventDispatcher() {
        this(false);
    }
    
    /**
     * 创建事件分发器
     * @param asyncDispatch 是否异步分发事件
     */
    public ScanEventDispatcher(boolean asyncDispatch) {
        this.asyncDispatch = asyncDispatch;
        if (asyncDispatch) {
            this.executorService = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                r -> {
                    Thread t = new Thread(r, "event-dispatcher");
                    t.setDaemon(true);
                    return t;
                }
            );
        } else {
            this.executorService = null;
        }
    }
    
    /**
     * 添加事件监听器
     */
    public void addEventListener(ScanEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除事件监听器
     */
    public void removeEventListener(ScanEventListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 清空所有监听器
     */
    public void clearListeners() {
        listeners.clear();
    }
    
    /**
     * 获取监听器数量
     */
    public int getListenerCount() {
        return listeners.size();
    }
    
    /**
     * 分发扫描开始事件
     */
    public void dispatchScanStarted(ScanStartedEvent event) {
        dispatch(() -> {
            for (ScanEventListener listener : listeners) {
                try {
                    listener.onScanStarted(event);
                } catch (Exception e) {
                    handleListenerException(listener, event, e);
                }
            }
        });
    }
    
    /**
     * 分发扫描进度事件
     */
    public void dispatchScanProgress(ScanProgressEvent event) {
        dispatch(() -> {
            for (ScanEventListener listener : listeners) {
                try {
                    listener.onScanProgress(event);
                } catch (Exception e) {
                    handleListenerException(listener, event, e);
                }
            }
        });
    }
    
    /**
     * 分发漏洞发现事件
     */
    public void dispatchVulnerabilityFound(VulnerabilityFoundEvent event) {
        dispatch(() -> {
            for (ScanEventListener listener : listeners) {
                try {
                    listener.onVulnerabilityFound(event);
                } catch (Exception e) {
                    handleListenerException(listener, event, e);
                }
            }
        });
    }
    
    /**
     * 分发扫描完成事件
     */
    public void dispatchScanCompleted(ScanCompletedEvent event) {
        dispatch(() -> {
            for (ScanEventListener listener : listeners) {
                try {
                    listener.onScanCompleted(event);
                } catch (Exception e) {
                    handleListenerException(listener, event, e);
                }
            }
        });
    }
    
    /**
     * 分发扫描错误事件
     */
    public void dispatchScanError(ScanErrorEvent event) {
        dispatch(() -> {
            for (ScanEventListener listener : listeners) {
                try {
                    listener.onScanError(event);
                } catch (Exception e) {
                    handleListenerException(listener, event, e);
                }
            }
        });
    }
    
    /**
     * 分发扫描信息事件
     */
    public void dispatchScanInfo(ScanInfoEvent event) {
        dispatch(() -> {
            for (ScanEventListener listener : listeners) {
                try {
                    listener.onScanInfo(event);
                } catch (Exception e) {
                    handleListenerException(listener, event, e);
                }
            }
        });
    }

    /**
     * 分发事件（支持同步和异步）
     */
    private void dispatch(Runnable task) {
        if (asyncDispatch && executorService != null) {
            executorService.submit(task);
        } else {
            task.run();
        }
    }
    
    /**
     * 处理监听器异常
     */
    private void handleListenerException(ScanEventListener listener, ScanEvent event, Exception e) {
        System.err.println("事件监听器执行异常: " + listener.getClass().getSimpleName() + 
                          ", 事件类型: " + event.getEventType() + ", 错误: " + e.getMessage());
        e.printStackTrace();
    }
    
    /**
     * 关闭事件分发器
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}


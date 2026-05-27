package com.potato.potatotool.content.redTeam.vulnScanner.model;

import java.util.List;
import java.util.Map;

/**
 * 扫描状态（支持暂停和恢复）
 * 
 * @author Potato
 * @date 2025/11/03
 */
public class ScanState {
    
    public enum Status {
        RUNNING,    // 运行中
        PAUSED,     // 已暂停
        STOPPED,    // 已停止
        COMPLETED,  // 已完成
        FAILED      // 执行失败
    }
    
    private String scanId;
    private Status status;
    private long startTime;
    private long pauseTime;
    private long resumeTime;
    private long endTime;           // 新增：结束时间

    // 扫描配置
    private int threads;
    private int timeout;
    private String proxy;
    private List<String> selectedFormats;

    // 目标和POC
    private List<String> targets;
    private List<String> pocIds; // 存储POC ID而不是完整对象

    // 进度信息
    private int totalTasks;
    private int completedTasks;
    private int vulnerabilitiesFound;

    // 严重度统计（新增）
    private int criticalCount;
    private int highCount;
    private int mediumCount;
    private int lowCount;
    private int infoCount;

    // 已完成的任务（用于恢复时跳过）
    private List<String> completedTaskIds;

    // 额外配置
    private Map<String, Object> additionalConfig;
    
    // Getters and Setters
    
    public String getScanId() {
        return scanId;
    }
    
    public void setScanId(String scanId) {
        this.scanId = scanId;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getPauseTime() {
        return pauseTime;
    }
    
    public void setPauseTime(long pauseTime) {
        this.pauseTime = pauseTime;
    }
    
    public long getResumeTime() {
        return resumeTime;
    }
    
    public void setResumeTime(long resumeTime) {
        this.resumeTime = resumeTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getThreads() {
        return threads;
    }
    
    public void setThreads(int threads) {
        this.threads = threads;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public String getProxy() {
        return proxy;
    }
    
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }
    
    public List<String> getSelectedFormats() {
        return selectedFormats;
    }
    
    public void setSelectedFormats(List<String> selectedFormats) {
        this.selectedFormats = selectedFormats;
    }
    
    public List<String> getTargets() {
        return targets;
    }
    
    public void setTargets(List<String> targets) {
        this.targets = targets;
    }
    
    public List<String> getPocIds() {
        return pocIds;
    }
    
    public void setPocIds(List<String> pocIds) {
        this.pocIds = pocIds;
    }
    
    public int getTotalTasks() {
        return totalTasks;
    }
    
    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }
    
    public int getCompletedTasks() {
        return completedTasks;
    }
    
    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }
    
    public int getVulnerabilitiesFound() {
        return vulnerabilitiesFound;
    }
    
    public void setVulnerabilitiesFound(int vulnerabilitiesFound) {
        this.vulnerabilitiesFound = vulnerabilitiesFound;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(int criticalCount) {
        this.criticalCount = criticalCount;
    }

    public int getHighCount() {
        return highCount;
    }

    public void setHighCount(int highCount) {
        this.highCount = highCount;
    }

    public int getMediumCount() {
        return mediumCount;
    }

    public void setMediumCount(int mediumCount) {
        this.mediumCount = mediumCount;
    }

    public int getLowCount() {
        return lowCount;
    }

    public void setLowCount(int lowCount) {
        this.lowCount = lowCount;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(int infoCount) {
        this.infoCount = infoCount;
    }

    public List<String> getCompletedTaskIds() {
        return completedTaskIds;
    }
    
    public void setCompletedTaskIds(List<String> completedTaskIds) {
        this.completedTaskIds = completedTaskIds;
    }
    
    public Map<String, Object> getAdditionalConfig() {
        return additionalConfig;
    }
    
    public void setAdditionalConfig(Map<String, Object> additionalConfig) {
        this.additionalConfig = additionalConfig;
    }
    
    /**
     * 计算扫描进度百分比
     */
    public double getProgress() {
        if (totalTasks == 0) {
            return 0.0;
        }
        return (double) completedTasks / totalTasks;
    }
    
    /**
     * 计算已暂停时长（秒）
     */
    public long getPausedDuration() {
        if (status != Status.PAUSED || pauseTime == 0) {
            return 0;
        }
        return (System.currentTimeMillis() - pauseTime) / 1000;
    }
    
    /**
     * 计算总耗时（秒）
     */
    public long getTotalDuration() {
        if (startTime == 0) {
            return 0;
        }
        long end = (status == Status.COMPLETED || status == Status.STOPPED || status == Status.FAILED) && endTime > 0
            ? endTime
            : System.currentTimeMillis();
        return (end - startTime) / 1000;
    }
}

package com.potato.potatotool.content.redTeam.vulnScanner.model;

/**
 * 单个扫描任务状态
 * 
 * @author Potato
 * @date 2025/11/03
 */
public class TaskState {
    
    private String taskId;
    private String scanId;
    private String target;
    private String pocId;
    private boolean completed;
    private boolean vulnerable;
    private long startTime;
    private long endTime;
    
    /**
     * 生成任务ID
     */
    public static String generateTaskId(String target, String pocId) {
        return target + "||" + pocId;
    }
    
    // Getters and Setters
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getScanId() {
        return scanId;
    }
    
    public void setScanId(String scanId) {
        this.scanId = scanId;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public String getPocId() {
        return pocId;
    }
    
    public void setPocId(String pocId) {
        this.pocId = pocId;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public boolean isVulnerable() {
        return vulnerable;
    }
    
    public void setVulnerable(boolean vulnerable) {
        this.vulnerable = vulnerable;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}


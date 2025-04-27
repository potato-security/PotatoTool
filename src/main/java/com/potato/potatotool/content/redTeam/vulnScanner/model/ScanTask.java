package com.potato.potatotool.content.redTeam.vulnScanner.model;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

/**
 * 扫描任务类
 */
public class ScanTask {
    private final String target;
    private final PocObj.Poc poc;

    /**
     * 创建扫描任务
     * @param target 目标URL
     * @param poc POC对象
     */
    public ScanTask(String target, PocObj.Poc poc) {
        this.target = target;
        this.poc = poc;
    }

    public String getTarget() {
        return target;
    }

    public PocObj.Poc getPoc() {
        return poc;
    }

    @Override
    public String toString() {
        return "ScanTask{" +
                "target='" + target + '\'' +
                ", poc=" + (poc != null ? poc.getName() : "null") +
                '}';
    }
} 
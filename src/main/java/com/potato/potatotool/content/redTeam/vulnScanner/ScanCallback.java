package com.potato.potatotool.content.redTeam.vulnScanner;

import com.potato.potatotool.content.redTeam.vulnScanner.model.ScanResult;

import java.util.List;

/**
 * 扫描回调接口，用于接收扫描结果和完成事件
 */
public interface ScanCallback {
    /**
     * 扫描结果回调
     * @param result 扫描结果
     */
    void onResult(ScanResult result);

    /**
     * 扫描完成回调
     * @param results 所有扫描结果
     */
    void onComplete(List<ScanResult> results);
} 
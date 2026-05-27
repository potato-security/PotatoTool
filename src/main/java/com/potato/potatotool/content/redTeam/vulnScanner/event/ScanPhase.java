package com.potato.potatotool.content.redTeam.vulnScanner.event;

/**
 * 扫描阶段枚举。
 * 用于统一指纹识别、POC筛选、漏洞扫描和收尾阶段的进度模型。
 */
public enum ScanPhase {
    PREPARING,
    FINGERPRINTING,
    SELECTING_POCS,
    SCANNING,
    FINALIZING,
    COMPLETED,
    STOPPED,
    FAILED
}

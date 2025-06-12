package com.potato.potatotool.content.blueTeam.webshell.model;

import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.crypto.DESUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解密步骤记录器
 * 用于记录和管理解密过程中的各个步骤及其结果
 * 
 * @author PotatoTool
 * @version 1.0
 */
public class DecryptStepRecorder {
    private final Map<String, String> steps = new LinkedHashMap<>();
    private String previousStep;
    
    /**
     * 添加解密步骤
     * 
     * @param stepName 步骤名称
     * @param stepResult 步骤结果
     */
    public void addStep(String stepName, String stepResult) {
        if (stepName == null || stepResult == null) {
            return;
        }
        steps.put(stepName, stepResult);
        previousStep = stepResult;
    }
    
    /**
     * 记录使用的解密方法
     * 
     * @param encodeMode 编码模式列表
     * @param aes AES工具实例
     * @param des DES工具实例
     */
    public void recordUsedMethods(List<String> encodeMode, AESUtils aes, DESUtils des) {
        if (encodeMode == null) {
            return;
        }
        
        // 直接记录所有步骤，因为SingleDecryptStrategy已经确保只有真正改变的步骤才会被记录
        for (Map.Entry<String, String> entry : steps.entrySet()) {
            String stepName = entry.getKey();
            String stepResult = entry.getValue();

            System.out.println(stepName);
            System.out.println(stepResult);
            
            addEncodeMode(encodeMode, stepName, stepResult, aes, des);
        }
    }
    
    /**
     * 根据步骤名称添加对应的编码模式
     * 
     * @param encodeMode 编码模式列表
     * @param stepName 步骤名称
     * @param stepResult 步骤结果
     * @param aes AES工具实例
     * @param des DES工具实例
     */
    private void addEncodeMode(List<String> encodeMode, String stepName, String stepResult,
                               AESUtils aes, DESUtils des) {
        switch (stepName) {
            // 需要单独处理的步骤
            case "HexDecode":
                encodeMode.add(stepResult.length() > 5 ? "HexDecode" : "MayBeHex");
                break;
            case "AESDecrypt":
                if (aes != null) {
                    encodeMode.add(String.format("AES\\%s\\%s<key:iv>%s:%s", 
                        aes.mode_AES.get(), aes.padding_AES.get(), 
                        aes.key_AES.get(), aes.iv_AES.get()));
                }
                break;
            case "DESDecrypt":
                if (des != null) {
                    encodeMode.add(String.format("DES\\%s\\%s<key:iv>%s:%s", 
                        des.mode_DES.get(), des.padding_DES.get(), 
                        des.key_DES.get(), des.iv_DES.get()));
                }
                break;
            default:
                // 默认解密步骤，直接添加步骤名称
                encodeMode.add(stepName);
                break;
        }
    }
    
    /**
     * 清空记录
     */
    public void clear() {
        steps.clear();
        previousStep = null;
    }
    
    /**
     * 获取步骤数量
     * 
     * @return 步骤数量
     */
    public int getStepCount() {
        return steps.size();
    }
    
    /**
     * 获取最后一步的结果
     * 
     * @return 最后一步的结果
     */
    public String getLastResult() {
        return previousStep;
    }
    
    /**
     * 获取所有步骤
     * 
     * @return 步骤映射
     */
    public Map<String, String> getSteps() {
        return new LinkedHashMap<>(steps);
    }
    
    /**
     * 检查是否包含指定步骤
     * 
     * @param stepName 步骤名称
     * @return 是否包含该步骤
     */
    public boolean containsStep(String stepName) {
        return steps.containsKey(stepName);
    }
    
    /**
     * 获取指定步骤的结果
     * 
     * @param stepName 步骤名称
     * @return 步骤结果，如果不存在则返回null
     */
    public String getStepResult(String stepName) {
        return steps.get(stepName);
    }
    
    /**
     * 检查是否为空
     * 
     * @return 是否为空
     */
    public boolean isEmpty() {
        return steps.isEmpty();
    }
    
    @Override
    public String toString() {
        return "DecryptStepRecorder{" +
                "stepCount=" + steps.size() +
                ", lastResult='" + previousStep + '\'' +
                '}';
    }
}
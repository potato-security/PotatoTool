package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.strategy;

import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptConfig;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptStepRecorder;
import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.crypto.DESUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.misc.ReadabilityChecker;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 单解密策略
 * 处理各种单一编码解密（URLDecode、Base64、Hex等）
 * 
 * @author Potato
 * @version 2.2
 */
public class SingleDecryptStrategy implements DecryptStrategy {
    
    private final StrUtils str;
    private final AESUtils aes;
    private final DESUtils des;
    
    /**
     * 构造函数 - 接收共享的工具类实例
     */
    public SingleDecryptStrategy(StrUtils sharedStrUtils, AESUtils sharedAesUtils, DESUtils sharedDesUtils) {
        this.str = sharedStrUtils;
        this.aes = sharedAesUtils;
        this.des = sharedDesUtils;
    }
    
    
    @Override
    public String decrypt(String content, DecryptConfig config, List<String> encodeMode) {
        if (!isApplicable(content)) {
            return null;
        }
        
        // 执行多轮单解密尝试
        for (int round = 0; round < 5; round++) {
            String result = performSingleDecryptRound(content, config, encodeMode, round);
            if (!result.equals(content)) {
                content = result;
            }
        }
        
        addAdditionalEncodeInfo(encodeMode);
        // encodeMode已经通过参数传递
        return content;
    }
    
    /**
     * 添加额外的编码信息
     */
    private void addAdditionalEncodeInfo(List<String> encodeMode) {
        if (aes.gzipCode || des.gzipCode || str.gzipCode) {
            encodeMode.add("Gzip");
        }
        if (aes.classCode || des.classCode || str.classCode) {
            encodeMode.add("Class编译");
        }
        if (aes.javaSerializeCode || des.serializeCode || str.javaSerializeCode) {
            encodeMode.add("反序列化");
        }

        // 检查二进制序列化格式
        if (str.bsonCode) encodeMode.add("BSON");
        if (str.messagePackCode) encodeMode.add("MessagePack");
        if (str.cborCode) encodeMode.add("CBOR");
        if (str.smileCode) encodeMode.add("Smile");
        if (str.hessianCode) encodeMode.add("Hessian");
        if (str.ubjsonCode) encodeMode.add("UBJSON");
        if (str.kryoCode) encodeMode.add("Kryo");
        if (str.fstCode) encodeMode.add("FST");
        if (str.avroCode) encodeMode.add("Avro");
    }
    
    /**
     * 执行一轮单解密
     */
    private String performSingleDecryptRound(String content, DecryptConfig config, 
                                            List<String> encodeMode, int round) {
        DecryptStepRecorder recorder = new DecryptStepRecorder();
        String currentStep = content;
        
        try {
            // 执行基础解密步骤
            currentStep = performBasicDecryptSteps(recorder, currentStep);
            
            // 执行高级解密步骤（AES/DES）
            currentStep = performAdvancedDecryptSteps(recorder, currentStep, config, encodeMode, round);
            
            // 记录使用的解密方法
            recorder.recordUsedMethods(encodeMode, aes, des);
            
        } catch (Exception e) {
            // 记录异常但不中断流程
            System.err.println("解密过程中发生异常: " + e.getMessage());
        }
        
        return currentStep;
    }
    
    /**
     * 执行基础解密步骤
     */
    private String performBasicDecryptSteps(DecryptStepRecorder recorder, String content) {
        String currentStep = content;
        
        // 基础解密步骤
        currentStep = executeAndRecord(recorder, "URLdecode", currentStep,
            input -> str.urlDecode(input));
        currentStep = executeAndRecord(recorder, "ChrDecode", currentStep,
            input -> str.chrFuncDecode(input));
        currentStep = executeAndRecord(recorder, "Base64", currentStep,
            input -> str.base64FuncDecode(input));
        currentStep = executeAndRecord(recorder, "Rot13", currentStep,
            input -> str.ROT13FuncDecode(input));
        currentStep = executeAndRecord(recorder, "strRev", currentStep,
            input -> str.strFuncRev(input));
        currentStep = executeAndRecord(recorder, "Unicode", currentStep,
            input -> str.decodeUnicode(input));
        
        // Hex解码特殊处理
        currentStep = performHexDecoding(recorder, currentStep);


        // 第二次Base64解码
        currentStep = performSecondBase64Decoding(recorder, currentStep);


        // Seeyon Base64解码
        currentStep = executeAndRecord(recorder, "seeyonBase64", currentStep,
                input -> str.seeyonBase64Decode(input));

        return currentStep;
    }
    
    /**
     * 执行Hex解码
     */
    private String performHexDecoding(DecryptStepRecorder recorder, String content) {
        boolean hexBeforeSerialize = str.javaSerializeCode;
        boolean hexBeforeClass = str.classCode;
        
        String hexResult = str.hexDecode(content);
        
        boolean hexAfterSerialize = str.javaSerializeCode;
        boolean hexAfterClass = str.classCode;
        
        // 判断解密后字符串可读性
        String result = ReadabilityChecker.assessReadability(hexResult) || 
                hexBeforeSerialize != hexAfterSerialize || 
                hexBeforeClass != hexAfterClass ? hexResult : content;
        
        // 只有当结果真正改变时才记录步骤
        if (!result.equals(content)) {
            recorder.addStep("HexDecode", result);
        }
        return result;
    }
    
    /**
     * 执行第二次Base64解码
     */
    private String performSecondBase64Decoding(DecryptStepRecorder recorder, String content) {
        String result = str.base64DecodeWebShell(content);

        if (result != null && result.length() > 100) {
            // 兼容js的btoa(toBinary(payload))
            result = ReadabilityChecker.assessReadability(result, 0.5, 0)
                ? result : content;
        } else if (result != null) {
            result = ReadabilityChecker.assessReadability(result) ? result : content;
        } else {
            result = content;
        }
        
        // 只有当结果真正改变时才记录步骤
        if (!result.equals(content)) {
            recorder.addStep("Base64", result);
        }
        return result;
    }
    
    /**
     * 执行高级解密步骤（AES/DES）
     */
    private String performAdvancedDecryptSteps(DecryptStepRecorder recorder, String content, 
                                             DecryptConfig config, List<String> encodeMode, int round) {
        String currentStep = content;
        
        // AES解密
        currentStep = performAESDecryption(recorder, currentStep, config, encodeMode, round);
        
        // DES解密
        currentStep = performDESDecryption(recorder, currentStep, config, encodeMode, round);
        
        return currentStep;
    }
    
    /**
     * 执行AES解密
     */
    private String performAESDecryption(DecryptStepRecorder recorder, String content, 
                                      DecryptConfig config, List<String> encodeMode, int round) {
        String result;
        //  已存在AES / 遍历字典并且非第4次循环(单解密3次+AES解密一次+单解密2次)
        if (str.listContantsStr((ArrayList<String>) encodeMode, "AES") || (!config.getTraverseList().isEmpty() && round!=3)) {
            result = content;
        } else {
            byte[] decrypted = aes.aesWebShellDecode(content, config.getInputKey(), 
                config.getInputIv(), config.getTraverseList(), config.getCustomPath());
            result = new String(decrypted, StandardCharsets.UTF_8);
        }
        
        // 只有当结果真正改变时才记录步骤
        if (!result.equals(content)) {
            recorder.addStep("AESDecrypt", result);
        }
        return result;
    }
    
    /**
     * 执行DES解密
     */
    private String performDESDecryption(DecryptStepRecorder recorder, String content, 
                                      DecryptConfig config, List<String> encodeMode, int round) {
        String result;
        
        if (str.listContantsStr((ArrayList<String>) encodeMode, "DES") || str.listContantsStr((ArrayList<String>) encodeMode, "AES") || (!config.getTraverseList().isEmpty() && round!=3) ) {
            result = content;
        } else {
            byte[] decrypted = des.desWebShellDecode(content, config.getInputKey(), 
                config.getInputIv(), config.getTraverseList(), config.getCustomPath());
            result = new String(decrypted, StandardCharsets.UTF_8);
        }
        
        // 只有当结果真正改变时才记录步骤
        if (!result.equals(content)) {
            recorder.addStep("DESDecrypt", result);
        }
        return result;
    }

    /**
     * 执行解密步骤并记录结果
     * 
     * @param recorder 步骤记录器
     * @param stepName 步骤名称
     * @param input 输入内容
     * @param decryptFunction 解密函数
     * @return 解密结果
     */
    private String executeAndRecord(DecryptStepRecorder recorder, String stepName, String input, 
                                   Function<String, String> decryptFunction) {
        try {
            String result = decryptFunction.apply(input);
            String finalResult = result != null ? result : input;
            // 只有当结果与输入不同时才记录步骤
            if (!finalResult.equals(input)) {
                recorder.addStep(stepName, finalResult);
            }
            return finalResult;
        } catch (Exception e) {
            System.err.println("执行解密步骤 " + stepName + " 时发生异常: " + e.getMessage());
            // 异常情况下不记录步骤
            return input;
        }
    }
    
    /**
     * 添加新的解密步骤（扩展接口）
     * 允许动态添加新的解密方法而无需修改现有代码
     * 
     * @param recorder 步骤记录器
     * @param stepName 步骤名称
     * @param input 输入内容
     * @param decryptFunction 自定义解密函数
     * @return 解密结果
     */
    public String addCustomDecryptStep(DecryptStepRecorder recorder, String stepName, String input,
                                      Function<String, String> decryptFunction) {
        if (recorder == null || stepName == null || input == null || decryptFunction == null) {
            return input;
        }
        
        try {
            String result = decryptFunction.apply(input);
            String finalResult = result != null ? result : input;
            // 只有当结果与输入不同时才记录步骤
            if (!finalResult.equals(input)) {
                recorder.addStep(stepName, finalResult);
            }
            return finalResult;
        } catch (Exception e) {
            System.err.println("执行自定义解密步骤 " + stepName + " 时发生异常: " + e.getMessage());
            // 异常情况下不记录步骤
            return input;
        }
    }

    @Override
    public boolean isApplicable(String content) {
        return content != null && !content.trim().isEmpty();
    }

}
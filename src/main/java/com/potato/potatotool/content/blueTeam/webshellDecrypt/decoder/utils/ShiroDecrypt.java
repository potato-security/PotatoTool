package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.data.StrUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2023/5/1 08:58
 */
public class ShiroDecrypt {

    public Set<String> keyArray_Shiro = new LinkedHashSet<>();

    /**
     * shiro流量反编译
     * @param code      原始payload
     * @param inputKey  主动传入的shiroKey，为null则遍历内部key
     * @param aes       外部传入初始化aesUtils用于记录解密状态
     * @param str       外部传入初始化strUtils用于记录解密状态
     * @return
     */
    public byte[] decrypt(String code, String inputKey, AESUtils aes, StrUtils str){

        byte[] res = null;
        int rememberIndex = code.indexOf("rememberMe=") + 11;
        code = rememberIndex==10 ? code : code.substring(rememberIndex);    //code.replace("rememberMe=","");
        int cookieEndIndex = code.indexOf(';');
        code = cookieEndIndex == -1 ? code : code.substring(0, cookieEndIndex);
        code = code.trim();

        // 排除非AES加密格式字符串传入（仅校验 rememberMe 的值部分）
        String aesPattern = "^[A-Za-z0-9+/]+={0,2}$";
        Pattern pattern = Pattern.compile(aesPattern);
        Matcher matcher = pattern.matcher(code.replace("\n","").replace("\r","").replace("\t",""));

        if (!matcher.matches()) {
            return null;
        }
        Set<String> keyArray = new LinkedHashSet<>();


        if(inputKey != null){
            keyArray.add(inputKey);
        } else {
            if(keyArray_Shiro.isEmpty()){ // 优先读取缓存数据
                try (InputStream shiroKeyInputStream = getResourceStream("shiroKey");
                     BufferedReader reader = new BufferedReader(new InputStreamReader(shiroKeyInputStream, StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        keyArray.add(line);
                    }
                    keyArray_Shiro = keyArray;

                } catch (Exception e) {
                    if (debugMode) e.printStackTrace();
                }
            }else {
                keyArray = keyArray_Shiro;
            }
        }

        byte[] rememberMeBytes;
        rememberMeBytes = StrUtils.base64Decode(code.getBytes(StandardCharsets.UTF_8));

        if (rememberMeBytes == null || rememberMeBytes.length <= 16) {
            return null;
        }

        byte[] rememberMeIv = Arrays.copyOfRange(rememberMeBytes, 0, 16);
        byte[] rememberMeCipherCbc = Arrays.copyOfRange(rememberMeBytes, 16, rememberMeBytes.length);

        String[] modeArray = {"CBC", "GCM"};

        String poolName = ExecutorServiceManager.ExecutorPoolNames.SHIRO_DECRYPT;
        ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
        List<CompletableFuture<byte[]>> futures = new ArrayList<>();

        for (String keyStr : keyArray) {
            for (String mode : modeArray) {
                CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        byte[] key = keyStr.getBytes(StandardCharsets.UTF_8);
                        byte[] iv = rememberMeIv;
                        String padding = mode.equals("GCM") ? "NoPadding" : "PKCS7Padding";

                        key = str.base64Decode(key);
                        byte[] cipherText = mode.equals("GCM")? rememberMeBytes : rememberMeCipherCbc;
                        byte[] decryptedTextBytes = aes.decrypt(cipherText , key, iv, mode, padding);
                        if(decryptedTextBytes != null && !decryptedTextBytes.equals("")) {
                            aes.mode_AES.set(mode);
                            aes.padding_AES.set(padding);
                            aes.key_AES.set(keyStr);
                            aes.iv_AES.set(mode.equals("GCM") ? "Null" : Base64.getEncoder().encodeToString(rememberMeIv) );

                            // 停止所有线程
                            ExecutorServiceManager.shutdownExecutor(poolName);
                        }
                        return decryptedTextBytes;
                    } catch (Exception e) {
                        if(debugMode)e.printStackTrace();
                        return null;
                    }
                }, executor);

                futures.add(future);
            }
        }

        for (Future<?> future : futures) {
            try {
                byte[] result = (byte[]) future.get();
                if (result != null && result.length > 0) {
                    res = result;
                    break;
                }
            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }
        }

        // 停止所有线程
        ExecutorServiceManager.shutdownExecutor(poolName);

        return res;
    }

}

package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.controller.PaneWebshellDecode;
import com.potato.potatotool.utils.ExecutorServiceManager;
import com.potato.potatotool.utils.aesUtils;
import com.potato.potatotool.utils.strUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2023/5/1 08:58
 */
public class shiroDecrypt {

    public Set<String> keyArray_Shiro = new HashSet<>();

    /**
     * shiro流量反编译
     * @param code      原始payload
     * @param inputKey  主动传入的shiroKey，为null则遍历内部key
     * @param aes       外部传入初始化aesUtils用于记录解密状态
     * @param str       外部传入初始化strUtils用于记录解密状态
     * @return
     */
    public byte[] decrypt(String code, String inputKey, aesUtils aes, strUtils str){

        // 排除非AES加密格式字符串传入
        String aesPattern = "^[A-Za-z0-9+/]+={0,2}$";
        Pattern pattern = Pattern.compile(aesPattern);
        Matcher matcher = pattern.matcher(code.replace("\n","").replace("\r","").replace("\t",""));

        if (!matcher.matches()) {
            return null;
        }

        byte[] res = null;
        int rememberIndex = code.indexOf("rememberMe=") + 11;
        code = rememberIndex==10 ? code : code.substring(rememberIndex);    //code.replace("rememberMe=","");
        Set<String> keyArray = new LinkedHashSet<>();


        if(inputKey != null){
            keyArray.add(inputKey);
        } else {
            if(keyArray_Shiro.isEmpty()){ // 优先读取缓存数据
                try{
                    InputStream shiroKeyInputStream = getResourceStream("shiroKey");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(shiroKeyInputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        keyArray.add(line);
                    }
                    keyArray_Shiro = keyArray;
                } catch (Exception e) {
                    if(debugMode)e.printStackTrace();
                }
            }else {
                keyArray = keyArray_Shiro;
            }
        }

        String[] modeArray = {"CBC", "GCM"};

        ExecutorService executor = ExecutorServiceManager.getInstance().getExecutor();
        List<Future<?>> futures = ExecutorServiceManager.futures;
        for (String keyStr : keyArray) {
            for (String mode : modeArray) {
                String finalCode = code;
                Callable<byte[]> task = () -> {
                    try {
                        byte[] key = keyStr.getBytes(StandardCharsets.UTF_8);
                        byte[] iv = aes.generateRandomBytes(16);
                        String padding = mode.equals("GCM") ? "NoPadding" : "PKCS7Padding";

                        key = str.base64Decode(key);
                        byte[] cipherText = str.base64Decode(finalCode.getBytes(StandardCharsets.UTF_8));
                        byte[] decryptedTextBytes = aes.decrypt(cipherText, key, iv, mode, padding);
                        aes.mode_AES.set(mode);
                        aes.padding_AES.set(padding);
                        aes.key_AES.set(keyStr);
                        aes.iv_AES.set(mode.equals("GCM") ? "Null" : "Random");
                        return decryptedTextBytes;
                    } catch (Exception e) {
                        if(debugMode)e.printStackTrace();
                        return null;
                    }
                };
                futures.add(executor.submit(task));
            }
        }

        for (Future<?> future : futures) {
            try {
                byte[] result = (byte[]) future.get();
                if (result != null && !result.equals("")) {

                    res = result;
                    break;
                }

            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }
        }

        for (Future<?> future : futures) {
            future.cancel(true);
        }
        futures.clear();

        // 停止所有线程
        ExecutorServiceManager.getInstance().forceShutdown();

        return res;
    }

}

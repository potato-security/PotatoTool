package com.potato.potatotool.content.blueTeam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2024/4/18 22:12
 */
public class md5Decrypt {

    public String mode = null;
    /**
     * 遍历解密方式 支持md5-16、md5-21、sha1
     * @param encryptedString 需要解密的字符串
     * @return
     */
    public String decrypt(String encryptedString){
        encryptedString = encryptedString.toLowerCase();
        String res = null;
        ExecutorService executor = ForkJoinPool.commonPool();
        List<Future<String>> futures = new ArrayList<>();


        boolean isMD5 = isMD5(encryptedString);
        boolean isMD516 = isMD516(encryptedString);
        boolean isSHA1 = isSHA1(encryptedString);

        if(isMD5||isMD516||isSHA1){
            try{
                InputStream inputStream = getResourceStream("md5");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {

                    String finalEncryptedString = encryptedString;
                    String finalLine = line;

                    Callable<String> task = () -> {
                        if (isMD5) {
                            mode = "MD5";
                            return hashString(finalLine, "MD5").equals(finalEncryptedString) ? finalLine : null;
                        } else if (isMD516) {
                            mode = "MD5-16";
                            return hashString(finalLine, "MD5").substring(8, 24).equals(finalEncryptedString) ? finalLine : null;
                        } else if (isSHA1) {
                            mode = "SHA1";
                            return hashString(finalLine, "SHA-1").equals(finalEncryptedString) ? finalLine : null;
                        }
                        return null;
                    };
                    futures.add(executor.submit(task));

                }

            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }
        }

        for (Future<String> future : futures) {
            try {
                String result = future.get();
                if (result != null && !result.equals("")) {
                    res = result;
                    break;
                }
            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }
        }

        // 停止所有线程
        executor.shutdownNow();

        return res;

    }

    private static String hashString(String input, String algorithm) {
        try {
            // 创建 MessageDigest 实例
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            // 使用指定的算法更新摘要
            byte[] hashBytes = digest.digest(input.getBytes());

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
            return null;
        }
    }

    // 判断字符串是否是 SHA-1 加密结果
    private static boolean isSHA1(String input) {
        Pattern pattern = Pattern.compile("[0-9a-f]{40}");
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    // 判断字符串是否是 MD5-32 加密结果
    private static boolean isMD5(String input) {
        Pattern pattern = Pattern.compile("[0-9a-f]{32}");
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    // 判断字符串是否是 MD5-16 加密结果
    private static boolean isMD516(String input) {
        Pattern pattern = Pattern.compile("[0-9a-f]{16}");
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }


    public static void main(String []args) {
        md5Decrypt md5 = new md5Decrypt();
        String str = md5.decrypt("8ee2027983915ec78acc45027d874316");
        System.out.println(md5.mode+"-8ee2027983915ec78acc45027d874316-"+str);
        String str1 = md5.decrypt("83915ec78acc4502");
        System.out.println(md5.mode+"-83915ec78acc4502-"+str1);
        String str2 = md5.decrypt("3e2e95f5ad970eadfa7e17eaf73da97024aa5359");
        System.out.println(md5.mode+"-3e2e95f5ad970eadfa7e17eaf73da97024aa5359-"+str2);
    }


}

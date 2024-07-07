package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.utils.SQLiteDBManager;

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

        boolean isMD5 = isMD5(encryptedString);
        boolean isMD516 = isMD516(encryptedString);
        boolean isSHA1 = isSHA1(encryptedString);

        if(isMD5||isMD516||isSHA1){
            try{

                Map<String, String> result= SQLiteDBManager.hashGetPlaintext(encryptedString);
                if(result!=null){
                    String plaintext = result.get("plaintext");
                    String md5 = result.get("MD5");
                    String md5_16 = result.get("MD5-16");
                    String sha1 = result.get("SHA1");

                    if (encryptedString.equals(md5)) {
                        mode = "MD5";
                    } else if (encryptedString.equals(md5_16)) {
                        mode = "MD5-16";
                    } else if (encryptedString.equals(sha1)) {
                        mode = "SHA1";
                    } else {
                        mode = "UNKNOWN";
                    }

                    res = plaintext;
                }

            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }

        }

        return res;

    }

    public static String hashString(String input, String algorithm) {
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
        String str = md5.decrypt("8EE2027983915ec78acc45027d874316");
        System.out.println(md5.mode+"-8ee2027983915ec78acc45027d874316-"+str);
        String str1 = md5.decrypt("83915ec78acc4502");
        System.out.println(md5.mode+"-83915ec78acc4502-"+str1);
        String str2 = md5.decrypt("3e2e95f5ad970eadfa7e17eaf73da97024aa5359");
        System.out.println(md5.mode+"-3e2e95f5ad970eadfa7e17eaf73da97024aa5359-"+str2);
    }


}

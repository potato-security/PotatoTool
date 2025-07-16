package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.google.gson.JsonObject;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;
import com.potato.potatotool.utils.misc.SQLiteDBManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.getConfigInfo;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2024/4/18 22:12
 */
public class MD5Decrypt {

    public String mode = null;
    public static String md5Url = getConfigInfo("md5Url");
    public static HashMap<String, String> headers = new HashMap();
    static {
        headers.put("AuthToken", "MHg2ZCwweDcwLDB4NzMsMHg3OSwweDdhLDB4NDQsMHgzMywweDc0LDB4NmQsMHg0NiwweDc1LDB4MzIsMHg3NywweDQ4LDB4NzEsMHg2YywweDZmLDB4NzUsMHgzOCwweDc3LDB4NmMsMHg0NiwweDY4LDB4MmYsMHg0OSwweDQ4LDB4NTEsMHgzNywweDQ3LDB4MzksMHg0NywweDRiLDB4NDcsMHg0YiwweDcxLDB4MzYsMHg2MSwweDM1LDB4NDMsMHg3MiwweDY1LDB4NmUsMHg2MywweDU0LDB4NDQsMHgzMiwweDRiLDB4NTAsMHg2NywweDc4LDB4NWEsMHg0ZCwweDM5LDB4NjEsMHg0ZCwweDRjLDB4MzksMHg1YSwweDJiLDB4NGEsMHg0NCwweDM2LDB4NGIsMHg2ZCwweDVhLDB4NTIsMHg0YywweDcxLDB4NGQsMHgzNiwweDczLDB4NDIsMHg2NywweDM5LDB4NzMsMHg3NCwweDRkLDB4NjUsMHg0MiwweDY3LDB4NmQsMHgzOCwweDUyLDB4NzIsMHg0NSwweDUyLDB4NTcsMHg1OCwweDU4LDB4NzYsMHgzNywweDcwLDB4NGMsMHg3MiwweDc3LDB4NGYsMHg0NiwweDM3LDB4NzMsMHg0OSwweDMyLDB4NTcsMHgzNCwweDZiLDB4NzUsMHg1OSwweDRkLDB4M2Q=");
    }
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

            // 若不存在本地md5库，则直接调用在线md5接口
            String TMP_FOLDER = ".PotatoTool";
            Path md5Path = Paths.get(System.getProperty("user.home"), TMP_FOLDER).resolve("md5_database.db");
            long fileSize = (long) (1.7 * 1024 * 1024 * 1024);
            try {
                fileSize = Files.size(md5Path);
            } catch (IOException e) {}

            if(!Files.exists(md5Path) || fileSize < (long) (1.66 * 1024 * 1024 * 1024)){
                res = decryptByNet(encryptedString);
                return res;
            }

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

    public String decryptByNet(String encryptedString){

        RequestObj obj = new RequestObj().setMethod("GET").setHeaders(headers).setUrl(md5Url + "/md5Decrypt?encryptedStr=" + encryptedString);

        try {
            CustomHttpResponse con = requests(obj);
            JsonObject res = con.getJson().getAsJsonObject();
            if( res!=null && res.has("plaintext")){
                System.out.println(res);
                mode = res.get("mode").getAsString();
                return res.get("plaintext").getAsString();
            }else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        MD5Decrypt md5 = new MD5Decrypt();
        String str = md5.decrypt("8EE2027983915ec78acc45027d874316");
        System.out.println(md5.mode+"-8ee2027983915ec78acc45027d874316-"+str);
        String str1 = md5.decrypt("83915ec78acc4502");
        System.out.println(md5.mode+"-83915ec78acc4502-"+str1);
        String str2 = md5.decrypt("3e2e95f5ad970eadfa7e17eaf73da97024aa5359");
        System.out.println(md5.mode+"-3e2e95f5ad970eadfa7e17eaf73da97024aa5359-"+str2);

    }


}

package com.potato.potatotool.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2024/3/28 11:36
 */
public class navicat12Des {

    public static String decryptString(String str){
        String key = "libcckeylibcckey";
        String iv = "libcciv libcciv ";
        String mode = "CBC";
        String padding = "PKCS5Padding";

        aesUtils aes = new aesUtils();
        try {
            byte[] res_tmp = aes.decrypt(strUtils.hexToByteArray(str), key.getBytes(StandardCharsets.UTF_8), iv.getBytes(StandardCharsets.UTF_8), mode, padding);
            String res = new String(res_tmp, StandardCharsets.UTF_8);
            res = ReadabilityChecker.assessReadability(res) ? res : null;

            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
            return null;
        }

    }
    public static void main(String []args) {
        String res = navicat12Des.decryptString("503AA930968F877F04770B47DD731DC0");
        System.out.println(res);

    }

}

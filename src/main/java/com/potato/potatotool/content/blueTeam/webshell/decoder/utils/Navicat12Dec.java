package com.potato.potatotool.content.blueTeam.webshell.decoder.utils;

import com.potato.potatotool.utils.misc.ReadabilityChecker;
import com.potato.potatotool.utils.crypto.AESUtils;
import com.potato.potatotool.utils.data.StrUtils;

import java.nio.charset.StandardCharsets;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2024/3/28 11:36
 */
public class Navicat12Dec {

    public static String decryptString(String str){
        String key = "libcckeylibcckey";
        String iv = "libcciv libcciv ";
        String mode = "CBC";
        String padding = "PKCS5Padding";

        AESUtils aes = new AESUtils();
        try {
            byte[] res_tmp = aes.decrypt(StrUtils.hexToByteArray(str), key.getBytes(StandardCharsets.UTF_8), iv.getBytes(StandardCharsets.UTF_8), mode, padding);
            String res = new String(res_tmp, StandardCharsets.UTF_8);
            res = ReadabilityChecker.assessReadability(res) ? res : null;

            return res;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
            return null;
        }

    }
    public static void main(String []args) {
        String res = Navicat12Dec.decryptString("503AA930968F877F04770B47DD731DC0");
        System.out.println(res);

    }

}

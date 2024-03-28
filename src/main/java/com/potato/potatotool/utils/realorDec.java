package com.potato.potatotool.utils;

import java.nio.charset.StandardCharsets;

/**
 * @author Potato
 * @date 2024/3/28 14:25
 */
public class realorDec {

    public static String decode(String str){
        try {

            String res = strUtils.base64Decode(str.replace("#", "="));
            if(ReadabilityChecker.assessReadability(res)){
                if(str.contains("#")){
                    return res;
                }
            }else {
                res = null;
            }

            if (res == null){
                String tmp_res = new String(nt_crypt(strUtils.base64Decode(str.replace("#", "=").getBytes(StandardCharsets.UTF_8)),false));
                if (ReadabilityChecker.assessReadability(tmp_res)){
                    return tmp_res;
                }
            }

            return null;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] nt_crypt(byte[] data, boolean bEncrypt) {
        int key = 3562;
        byte[] result = new byte[data.length];
        int a = 0;
        for (int i = 0; i < data.length; ++i) {
            a = data[i] & 0xFF ^ key >> 8 & 0xFFFFFFF;
            result[i] = (byte)(a &= 0xFF);
            key = bEncrypt ? (a + key) * 5891 + 5920 & 0xFFFFFFF : ((data[i] & 0xFF) + key) * 5891 + 5920 & 0xFFFFFFF;
        }
        return result;
    }

    public static void main(String []args) {
        String res = realorDec.decode("SprIrBOGcHdvCK63VoT0NHmSAow=");
        System.out.println(res);

    }

}

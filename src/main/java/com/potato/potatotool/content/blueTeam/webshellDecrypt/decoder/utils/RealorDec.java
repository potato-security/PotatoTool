package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.misc.ReadabilityChecker;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2024/3/28 14:25
 */
public class RealorDec {

    public static String decode(String str){
        try {

            String res = new StrUtils().base64Decode(str.replace("#", "="));
            if(ReadabilityChecker.assessReadability(res)){
                if(str.contains("#")){
                    return res;
                }
            }else {
                res = null;
            }

            if (res == null){
                String tmp_res = new String(nt_crypt(StrUtils.base64Decode(str.replace("#", "=").getBytes(StandardCharsets.UTF_8)),false));
                if (ReadabilityChecker.assessReadability(tmp_res)){
                    List<String> list = Arrays.asList("(", ")", "'", "\"", "[", "]", "\\", "{", "}", "：", "《", "》", "【", "】");

                    // 排除低概率意外情况，比如admin123
                    if( str.length() < 10 && StrUtils.containsAnyWithSet(tmp_res, list) ){
                        tmp_res = null;
                    }

                    return tmp_res;
                }
            }

            return null;

        }catch (Exception e){
            if(debugMode)e.printStackTrace();
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
        String res = RealorDec.decode("SprIrBOGcHdvCK63VoT0NHmSAow=");
        System.out.println(res);
    }

}

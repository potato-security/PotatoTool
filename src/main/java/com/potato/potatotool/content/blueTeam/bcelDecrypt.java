package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.utils.DeserializerUtils;
import com.potato.potatotool.utils.Utility;

/**
 * @author Potato
 * @date 2023/5/18 14:08
 */
public class bcelDecrypt {

    public static boolean classCode = false;
    public static boolean serializeCode = false;

    public static byte[] decrypt(String input){
        byte[] resByte = null;
        if(!input.toLowerCase().startsWith("$$bcel$$")) return null;

        // 剔除头部$$BCEL$$
        input = input.substring(8);

        try {
            resByte = Utility.decode (input,true);

            // 检查是否存在class/反序列化
            byte[] tmpDecryptedTextBytes = null;
            tmpDecryptedTextBytes = DeserializerUtils.classDataCheck(resByte);
            if(tmpDecryptedTextBytes==null) {
                tmpDecryptedTextBytes = DeserializerUtils.serializeCheck(resByte);
            }else {
                classCode = true;
            }
            if(tmpDecryptedTextBytes!=null) {
                resByte = tmpDecryptedTextBytes;
                serializeCode = true;
            }

            return resByte;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}

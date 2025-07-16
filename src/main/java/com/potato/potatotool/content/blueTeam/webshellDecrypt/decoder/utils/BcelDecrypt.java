package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.potato.potatotool.utils.data.DeserializerUtils;
import com.potato.potatotool.utils.misc.Utility;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2023/5/18 14:08
 */
public class BcelDecrypt {

    public boolean classCode = false;
    public boolean serializeCode = false;

    public byte[] decrypt(String input){
        byte[] resByte = null;
        if(!input.toLowerCase().startsWith("$$bcel$$")) return null;

        // 剔除头部$$BCEL$$
        input = input.substring(8);

        try {
            resByte = Utility.decode (input,true);

            // 检查是否存在class/反序列化
            byte[] tmpDecryptedTextBytes = DeserializerUtils.classDataCheck(resByte);
            byte[] tmpSerDecryptedTextBytes = null;
            if (tmpDecryptedTextBytes == null) {
                tmpSerDecryptedTextBytes = DeserializerUtils.serializeCheck(resByte);
            } else {
                resByte = tmpDecryptedTextBytes;
                classCode = true;
            }

            if (tmpSerDecryptedTextBytes != null) {
                resByte = tmpSerDecryptedTextBytes;
                serializeCode = true;
            }

            return resByte;

        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }

        return null;
    }

}

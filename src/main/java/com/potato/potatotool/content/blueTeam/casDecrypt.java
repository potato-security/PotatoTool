package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.utils.aesUtils;
import com.potato.potatotool.utils.strUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Potato
 * @date 2023/6/16 11:34
 */
public class casDecrypt {

    public static byte[] decrypt(String input,aesUtils aes){
        byte[] res = null;
        String code = "";
        input = strUtils.urlDecode(input);

        Pattern pattern = Pattern.compile("([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})_(.*)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            code = matcher.group(2);
        }
        if( code == null || code == "") {
            return res;
        }

        try {

            byte[] encryptData = strUtils.base64Decode(code.getBytes(StandardCharsets.UTF_8));

            byte[] key = {78, -47, -80, -25, 76, 55, -57, -111, -81, -3, -54, 62, 118, 15, 113, 0};
            byte[] iv = new byte[16];
            System.arraycopy(encryptData, 8, iv, 0, 16);
            String mode = "CBC";
            String padding = "PKCS7Padding";

            // 剔除header头部34个标志性字节
            byte[] tmpEncryptData = new byte[encryptData.length - 34];
            System.arraycopy(encryptData, 34, tmpEncryptData, 0, encryptData.length - 34);
            encryptData = tmpEncryptData;

            byte[] result = aes.decrypt(
                    encryptData,
                    key,
                    iv,
                    mode,
                    padding
            );

            aes.mode_AES.set(mode);
            aes.padding_AES.set(padding);
            aes.key_AES.set(Arrays.toString(key));
            aes.iv_AES.set(Arrays.toString(iv));

            res = result;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }
}

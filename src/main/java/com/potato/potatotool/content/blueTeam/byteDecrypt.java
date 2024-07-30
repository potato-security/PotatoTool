package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.utils.DeserializerUtils;
import com.potato.potatotool.utils.GzipUtils;
import com.potato.potatotool.utils.strUtils;

import java.nio.charset.StandardCharsets;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.strUtils.byteToHex;

/**
 * @author Potato
 * @date 2023/5/18 14:49
 */
public class byteDecrypt {

    public static boolean classCode = false;
    public static boolean serializeCode = false;
    public static boolean gzipCode = false;

    public static byte[] convertToByteArray(String input) {
        String[] parts = input.split(",");
        byte[] byteArray = new byte[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                byteArray[i] = Byte.parseByte(parts[i].trim());
            }
            return byteArray;
        } catch (NumberFormatException e) {
            // 如果解析失败，说明字符串不是byte[]格式
            return null;
        }
    }

    public static byte[] decrypt(String input){

        if(!input.matches("[-0-9,\\(\\)\\{\\}\\s\\n]+")) return null;

        input = input.replaceAll("[\\{\\}\\[\\]\\s\\n]", "");

        byte[] byteArray = convertToByteArray(input);
        if (byteArray == null) return null;

        try {
            //是否存在Gzip压缩特征
            if(strUtils.byteStartsWith(byteArray, 0, new byte[]{(byte) 0x1F, (byte) 0x8B})) {
                byteArray = GzipUtils.GzipDecompress(byteArray);

                if(strUtils.byteArrayContains(byteArray, new byte[]{0, 0, 0}) != -1 && !strUtils.byteStartsWith(byteArray, 0, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}) && !strUtils.byteStartsWith(byteArray, 0, new byte[]{(byte) 0xAC, (byte) 0xED, 0x00, 0x05}) ){  // 针对于哥斯拉key和value空字符需要转换为等号
                    byteArray = strUtils.byteReplaceZeroToD3(byteArray);
                }

                gzipCode = true;
            }

            // 检查是否存在class/反序列化
            byte[] tmpDecryptedTextBytes = DeserializerUtils.classDataCheck(byteArray);
            byte[] tmpSerDecryptedTextBytes = null;
            if (tmpDecryptedTextBytes == null) {
                tmpSerDecryptedTextBytes = DeserializerUtils.serializeCheck(byteArray);
            } else {
                byteArray = tmpDecryptedTextBytes;
                classCode = true;
            }

            if (tmpSerDecryptedTextBytes != null) {
                byteArray = tmpSerDecryptedTextBytes;
                serializeCode = true;
            }

            return byteArray;

        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }

        return null;
    }

}

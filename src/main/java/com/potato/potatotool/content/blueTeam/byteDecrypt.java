package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.utils.DeserializerUtils;
import com.potato.potatotool.utils.strUtils;

import java.nio.charset.StandardCharsets;

import static com.potato.potatotool.utils.strUtils.byteToHex;
import static com.potato.potatotool.utils.strUtils.hexDecode;

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
            if(byteToHex(byteArray).toLowerCase().startsWith("1f8b")){
                byteArray = strUtils.gzipDecompress(byteArray);
                String tmpHexData = byteToHex(byteArray);

                if(tmpHexData.contains("000000") && !tmpHexData.toLowerCase().startsWith("cafebabe") && !tmpHexData.toLowerCase().startsWith("aced0005") ){  // 针对于哥斯拉key和value空字符需要转换为等号
                    tmpHexData = strUtils.strRev( strUtils.strRev(tmpHexData).replaceAll("(00.{8})", "D3") );
                    byteArray = hexDecode(tmpHexData).getBytes(StandardCharsets.UTF_8);
                }

                gzipCode = true;
            }

            // 检查是否存在class/反序列化
            byte[] tmpDecryptedTextBytes = null;
            tmpDecryptedTextBytes = DeserializerUtils.classDataCheck(byteArray);
            if(tmpDecryptedTextBytes==null) {
                tmpDecryptedTextBytes = DeserializerUtils.serializeCheck(byteArray);
            }else {
                classCode = true;
            }
            if(tmpDecryptedTextBytes!=null) {
                byteArray = tmpDecryptedTextBytes;
                serializeCode = true;
            }

            return byteArray;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}

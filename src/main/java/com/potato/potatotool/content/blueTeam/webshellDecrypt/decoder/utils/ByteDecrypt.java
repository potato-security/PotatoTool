package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.potato.potatotool.utils.data.DeserializerUtils;
import com.potato.potatotool.utils.data.GzipUtils;
import com.potato.potatotool.utils.data.StrUtils;

import java.nio.charset.StandardCharsets;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2023/5/18 14:49
 */
public class ByteDecrypt {

    public boolean classCode = false;
    public boolean javaSerializeCode = false;
    public boolean gzipCode = false;

    // 二进制序列化格式标志
    public boolean bsonCode = false;
    public boolean messagePackCode = false;
    public boolean cborCode = false;
    public boolean smileCode = false;
    public boolean hessianCode = false;
    public boolean ubjsonCode = false;
    public boolean kryoCode = false;
    public boolean fstCode = false;
    public boolean avroCode = false;

    // 记录检测到的格式类型
    public String detectedBinaryFormat = null;

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

    public byte[] decrypt(String input){

        if(!input.matches("[-0-9,\\(\\)\\{\\}\\s\\n]+")) return null;

        input = input.replaceAll("[\\{\\}\\[\\]\\s\\n]", "");

        byte[] byteArray = convertToByteArray(input);
        if (byteArray == null) return null;

        try {
            //是否存在Gzip压缩特征
            if(StrUtils.byteStartsWith(byteArray, 0, new byte[]{(byte) 0x1F, (byte) 0x8B})) {
                byteArray = GzipUtils.GzipDecompress(byteArray);

                if(StrUtils.byteArrayContains(byteArray, new byte[]{0, 0, 0}) != -1 && !StrUtils.byteStartsWith(byteArray, 0, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}) && !StrUtils.byteStartsWith(byteArray, 0, new byte[]{(byte) 0xAC, (byte) 0xED, 0x00, 0x05}) ){  // 针对于哥斯拉key和value空字符需要转换为等号
                    byteArray = StrUtils.byteReplaceZeroToD3(byteArray);
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
                javaSerializeCode = true;
            }

            // 二进制序列化格式检测
            if (!classCode && !javaSerializeCode) {  // 避免重复检测
                byte[] binaryDeserialized = detectAndDeserializeBinary(byteArray);
                if (binaryDeserialized != null) {
                    byteArray = binaryDeserialized;
                }
            }

            return byteArray;

        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }

        return null;
    }

    /**
     * 检测并反序列化二进制格式
     * <p>
     * 分两步处理：
     * 1. 先检测格式类型
     * 2. 再执行反序列化获取结果
     * </p>
     *
     * @param data 待检测数据
     * @return 反序列化后的字节数组，如果无法识别或解析失败则返回null
     */
    private byte[] detectAndDeserializeBinary(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            // 步骤1：检测格式（不执行反序列化）
            String format = BinaryDeserializerFactory.detectFormat(data);
            if (format == null) {
                return null;  // 未识别的格式
            }

            // 步骤2：执行反序列化
            String result = BinaryDeserializerFactory.autoDeserialize(data);
            if (result == null) {
                return null;  // 反序列化失败
            }

            // 步骤3：根据检测到的格式设置标志
            detectedBinaryFormat = format;
            switch (format.toUpperCase()) {
                case "BSON":
                    bsonCode = true;
                    break;
                case "MESSAGEPACK":
                    messagePackCode = true;
                    break;
                case "CBOR":
                    cborCode = true;
                    break;
                case "SMILE":
                    smileCode = true;
                    break;
                case "HESSIAN":
                    hessianCode = true;
                    break;
                case "UBJSON":
                    ubjsonCode = true;
                    break;
                case "KRYO":
                    kryoCode = true;
                    break;
                case "FST":
                    fstCode = true;
                    break;
                case "AVRO":
                    avroCode = true;
                    break;
            }

            return result.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        } catch (Exception e) {
            if (debugMode) {
                e.printStackTrace();
            }
        }

        return null;
    }

}

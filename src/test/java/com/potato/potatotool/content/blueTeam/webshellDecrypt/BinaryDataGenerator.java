package com.potato.potatotool.content.blueTeam.webshellDecrypt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.io.BasicOutputBuffer;
import org.bson.json.JsonWriterSettings;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 二进制序列化数据生成器
 * <p>
 * 用于生成各种二进制格式的测试数据
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class BinaryDataGenerator {

    /**
     * 生成BSON格式数据
     */
    public static byte[] generateBson() {
        try {
            // 创建BSON文档
            BsonDocument doc = new BsonDocument();
            doc.put("name", new BsonString("test"));
            doc.put("age", new BsonInt32(25));

            // 序列化为byte[]
            BasicOutputBuffer buffer = new BasicOutputBuffer();
            BsonWriter writer = new org.bson.BsonBinaryWriter(buffer);
            new org.bson.codecs.BsonDocumentCodec().encode(
                writer,
                doc,
                org.bson.codecs.EncoderContext.builder().build()
            );

            return buffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成MessagePack格式数据
     */
    public static byte[] generateMessagePack() {
        try {
            ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());
            Map<String, Object> data = new HashMap<>();
            data.put("cmd", "whoami");
            data.put("args", new String[]{"test"});

            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成CBOR格式数据
     */
    public static byte[] generateCbor() {
        try {
            ObjectMapper mapper = new ObjectMapper(new CBORFactory());
            Map<String, Object> data = new HashMap<>();
            data.put("status", "success");

            Map<String, Object> innerData = new HashMap<>();
            innerData.put("id", 1);
            innerData.put("name", "test");
            data.put("data", innerData);

            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成Smile格式数据
     */
    public static byte[] generateSmile() {
        try {
            ObjectMapper mapper = new ObjectMapper(new SmileFactory());
            Map<String, String> data = new HashMap<>();
            data.put("user", "admin");
            data.put("role", "admin");

            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 工具方法：byte[]转Base64
     */
    public static String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * 工具方法：byte[]转Hex
     */
    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 工具方法：byte[]转Java数组格式
     */
    public static String toJavaArray(byte[] data) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < data.length; i++) {
            sb.append(data[i]);
            if (i < data.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 工具方法：打印所有格式
     */
    public static void printAllFormats(String name, byte[] data) {
        System.out.println("\n========== " + name + " ==========");
        System.out.println("原始长度: " + data.length + " 字节");
        System.out.println("\n1. Base64格式:");
        System.out.println(toBase64(data));
        System.out.println("\n2. Hex格式:");
        System.out.println(toHex(data));
        System.out.println("\n3. Java byte[]数组格式:");
        System.out.println(toJavaArray(data));
        System.out.println("\n4. 完整HTTP包示例:");
        System.out.println("POST /api/test HTTP/1.1");
        System.out.println("Host: example.com");
        System.out.println("Content-Type: application/octet-stream");
        System.out.println("Content-Length: " + toBase64(data).length());
        System.out.println();
        System.out.println("data=" + toBase64(data));
    }

    /**
     * 主方法：生成所有测试数据
     */
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           二进制序列化数据生成器                            ║");
        System.out.println("║  生成BSON、MessagePack、CBOR、Smile格式的测试数据           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        try {
            // 生成BSON数据
            byte[] bsonData = generateBson();
            if (bsonData != null) {
                printAllFormats("BSON格式", bsonData);
            }

            // 生成MessagePack数据
            byte[] msgpackData = generateMessagePack();
            if (msgpackData != null) {
                printAllFormats("MessagePack格式", msgpackData);
            }

            // 生成CBOR数据
            byte[] cborData = generateCbor();
            if (cborData != null) {
                printAllFormats("CBOR格式", cborData);
            }

            // 生成Smile数据
            byte[] smileData = generateSmile();
            if (smileData != null) {
                printAllFormats("Smile格式", smileData);
            }

            System.out.println("\n╔════════════════════════════════════════════════════════════╗");
            System.out.println("║              所有测试数据生成完成                           ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("数据生成出错：");
            e.printStackTrace();
        }
    }
}

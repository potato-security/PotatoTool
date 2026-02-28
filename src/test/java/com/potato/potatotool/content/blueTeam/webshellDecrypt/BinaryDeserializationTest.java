package com.potato.potatotool.content.blueTeam.webshellDecrypt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils.*;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptConfig;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.model.DecryptResult;
import com.potato.potatotool.utils.crypto.SecurityInitializer;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.io.BasicOutputBuffer;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.BsonBinaryWriter;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * 二进制序列化格式解密测试
 * <p>
 * 全面测试所有支持的二进制格式反序列化功能
 * </p>
 * <p>
 * 支持格式：
 * - 精确魔术头：Smile(:)\n)、Hessian(H\x01/02)、Avro(Obj\x01)
 * - 格式验证：BSON(长度+0x00)、MessagePack(0x80-0x9F)、CBOR(0xA0-0xBF)、UBJSON({+类型标记)
 * - 兜底格式：FST(0xFE/FF)、Kryo(尝试解析)
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class BinaryDeserializationTest {

    // ==================== 测试数据生成工具 ====================

    /**
     * 生成 BSON 格式测试数据
     * BSON 特征：前4字节小端序长度 + 尾部0x00
     */
    public static byte[] generateBsonData(Map<String, Object> data) {
        try {
            BsonDocument doc = new BsonDocument();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    doc.append(entry.getKey(), new BsonString((String) value));
                } else if (value instanceof Integer) {
                    doc.append(entry.getKey(), new BsonInt32((Integer) value));
                }
            }

            BasicOutputBuffer buffer = new BasicOutputBuffer();
            BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
            new BsonDocumentCodec().encode(writer, doc, EncoderContext.builder().build());
            writer.close();

            return buffer.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成BSON数据失败", e);
        }
    }

    /**
     * 生成 MessagePack 格式测试数据
     * MessagePack 特征：首字节 0x80-0x8F(fixmap) 或 0x90-0x9F(fixarray)
     */
    public static byte[] generateMessagePackData(Map<String, Object> data) {
        try {
            ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());
            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("生成MessagePack数据失败", e);
        }
    }

    /**
     * 生成 CBOR 格式测试数据
     * CBOR 特征：首字节 0xA0-0xBF(map) 或 0x80-0x9F(array)
     */
    public static byte[] generateCborData(Map<String, Object> data) {
        try {
            ObjectMapper mapper = new ObjectMapper(new CBORFactory());
            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("生成CBOR数据失败", e);
        }
    }

    /**
     * 生成 Smile 格式测试数据
     * Smile 特征：魔术头 0x3A 0x29 0x0A (:)\n)
     */
    public static byte[] generateSmileData(Map<String, Object> data) {
        try {
            ObjectMapper mapper = new ObjectMapper(new SmileFactory());
            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("生成Smile数据失败", e);
        }
    }

    /**
     * 生成 UBJSON 格式测试数据
     * UBJSON 特征：首字节 0x7B({) 或 0x5B([) + 类型标记
     */
    public static byte[] generateUbjsonData(Map<String, Object> data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0x7B); // { - 对象开始

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // 写入 key 长度类型和长度
                baos.write(0x69); // i - int8 长度类型
                baos.write(key.length());
                baos.write(key.getBytes("UTF-8"));

                // 写入 value
                if (value instanceof String) {
                    String strValue = (String) value;
                    baos.write(0x53); // S - string 类型
                    baos.write(0x69); // i - int8 长度类型
                    baos.write(strValue.length());
                    baos.write(strValue.getBytes("UTF-8"));
                } else if (value instanceof Integer) {
                    int intValue = (Integer) value;
                    if (intValue >= 0 && intValue <= 255) {
                        baos.write(0x55); // U - uint8
                        baos.write(intValue);
                    } else {
                        baos.write(0x6C); // l - int32
                        ByteBuffer buf = ByteBuffer.allocate(4);
                        buf.putInt(intValue);
                        baos.write(buf.array());
                    }
                } else if (value instanceof Boolean) {
                    baos.write((Boolean) value ? 0x54 : 0x46); // T/F
                }
            }

            baos.write(0x7D); // } - 对象结束
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成UBJSON数据失败", e);
        }
    }

    /**
     * 生成 Hessian2 格式测试数据
     * Hessian2 特征：魔术头 0x48 0x02 (H + version 2)
     */
    public static byte[] generateHessian2Data(Map<String, Object> data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.caucho.hessian.io.Hessian2Output output = new com.caucho.hessian.io.Hessian2Output(baos);
            output.writeObject(data);
            output.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成Hessian2数据失败", e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 字节数组转十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 字节数组转 Base64 字符串
     */
    public static String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 打印字节数组详情
     */
    public static void printByteDetails(String name, byte[] data) {
        System.out.println("\n--- " + name + " 数据详情 ---");
        System.out.println("长度: " + data.length + " 字节");
        System.out.println("首字节: 0x" + String.format("%02X", data[0] & 0xFF));
        System.out.println("Hex: " + bytesToHex(data));
        System.out.println("Base64: " + bytesToBase64(data));
    }

    // ==================== 格式检测测试 ====================

    /**
     * 测试1：BSON 格式检测和反序列化
     */
    public static void testBsonFormat() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试1：BSON 格式");
        System.out.println("特征：前4字节小端序长度 + 尾部0x00");
        System.out.println(repeat("=", 60));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", "admin");
        data.put("password", "123456");
        data.put("age", 25);

        byte[] bsonData = generateBsonData(data);
        printByteDetails("BSON", bsonData);

        // 验证特征
        int declaredLength = (bsonData[0] & 0xFF) |
                            ((bsonData[1] & 0xFF) << 8) |
                            ((bsonData[2] & 0xFF) << 16) |
                            ((bsonData[3] & 0xFF) << 24);
        System.out.println("声明长度: " + declaredLength + ", 实际长度: " + bsonData.length);
        System.out.println("尾部字节: 0x" + String.format("%02X", bsonData[bsonData.length - 1]));

        // 测试反序列化
        BsonDeserializer deserializer = new BsonDeserializer();
        System.out.println("canDeserialize: " + deserializer.canDeserialize(bsonData));
        String result = deserializer.deserialize(bsonData);
        System.out.println("反序列化结果:\n" + result);
    }

    /**
     * 测试2：MessagePack 格式检测和反序列化
     */
    public static void testMessagePackFormat() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试2：MessagePack 格式");
        System.out.println("特征：首字节 0x80-0x8F(fixmap) 或 0x90-0x9F(fixarray)");
        System.out.println(repeat("=", 60));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cmd", "whoami");
        data.put("path", "/tmp");

        byte[] msgpackData = generateMessagePackData(data);
        printByteDetails("MessagePack", msgpackData);

        // 测试反序列化
        MessagePackDeserializer deserializer = new MessagePackDeserializer();
        System.out.println("canDeserialize: " + deserializer.canDeserialize(msgpackData));
        String result = deserializer.deserialize(msgpackData);
        System.out.println("反序列化结果:\n" + result);
    }

    /**
     * 测试3：CBOR 格式检测和反序列化
     */
    public static void testCborFormat() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试3：CBOR 格式");
        System.out.println("特征：首字节 0xA0-0xBF(map) 或 0x80-0x9F(array)");
        System.out.println(repeat("=", 60));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "success");
        data.put("code", 200);
        data.put("message", "OK");

        byte[] cborData = generateCborData(data);
        printByteDetails("CBOR", cborData);

        // 测试反序列化
        CborDeserializer deserializer = new CborDeserializer();
        System.out.println("canDeserialize: " + deserializer.canDeserialize(cborData));
        String result = deserializer.deserialize(cborData);
        System.out.println("反序列化结果:\n" + result);
    }

    /**
     * 测试4：Smile 格式检测和反序列化
     */
    public static void testSmileFormat() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试4：Smile 格式");
        System.out.println("特征：魔术头 0x3A 0x29 0x0A (:)\\n)");
        System.out.println(repeat("=", 60));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", "admin");
        data.put("role", "superuser");

        byte[] smileData = generateSmileData(data);
        printByteDetails("Smile", smileData);

        // 验证魔术头
        System.out.println("魔术头验证: " +
            (smileData[0] == 0x3A && smileData[1] == 0x29 && smileData[2] == 0x0A ? "✓ 正确" : "✗ 错误"));

        // 测试反序列化
        SmileDeserializer deserializer = new SmileDeserializer();
        System.out.println("canDeserialize: " + deserializer.canDeserialize(smileData));
        String result = deserializer.deserialize(smileData);
        System.out.println("反序列化结果:\n" + result);
    }

    /**
     * 测试5：UBJSON 格式检测和反序列化
     */
    public static void testUbjsonFormat() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试5：UBJSON 格式");
        System.out.println("特征：首字节 0x7B({) 或 0x5B([) + 类型标记");
        System.out.println(repeat("=", 60));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "test");
        data.put("value", 100);
        data.put("active", true);

        byte[] ubjsonData = generateUbjsonData(data);
        printByteDetails("UBJSON", ubjsonData);

        // 测试反序列化
        UbjsonDeserializer deserializer = new UbjsonDeserializer();
        System.out.println("canDeserialize: " + deserializer.canDeserialize(ubjsonData));
        String result = deserializer.deserialize(ubjsonData);
        System.out.println("反序列化结果:\n" + result);
    }

    /**
     * 测试6：Hessian 格式检测和反序列化
     */
    public static void testHessianFormat() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试6：Hessian 格式");
        System.out.println("特征：魔术头 0x48 0x02 (H + version 2)");
        System.out.println(repeat("=", 60));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", "admin");
        data.put("password", "secret123");

        byte[] hessianData = generateHessian2Data(data);
        printByteDetails("Hessian", hessianData);

        // 测试反序列化
        HessianDeserializer deserializer = new HessianDeserializer();
        System.out.println("canDeserialize: " + deserializer.canDeserialize(hessianData));
        String result = deserializer.deserialize(hessianData);
        System.out.println("反序列化结果:\n" + result);
    }

    /**
     * 测试7：FST 格式（兜底反序列化器）
     */
    public static void testFstFormat() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试7：FST 格式（兜底）");
        System.out.println("特征：首字节 0xFE 或 0xFF（版本号）");
        System.out.println(repeat("=", 60));

        try {
            org.nustaq.serialization.FSTConfiguration fst =
                org.nustaq.serialization.FSTConfiguration.createDefaultConfiguration();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("cmd", "ls");
            data.put("path", "/tmp");

            byte[] fstData = fst.asByteArray(data);
            printByteDetails("FST", fstData);

            // 测试反序列化
            FstDeserializer deserializer = new FstDeserializer();
            System.out.println("canDeserialize: " + deserializer.canDeserialize(fstData));
            String result = deserializer.deserialize(fstData);
            System.out.println("反序列化结果:\n" + result);
        } catch (Exception e) {
            System.out.println("FST 测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试8：Kryo 格式（兜底反序列化器）
     */
    public static void testKryoFormat() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试8：Kryo 格式（兜底）");
        System.out.println("特征：无固定魔术头，通过尝试解析判断");
        System.out.println(repeat("=", 60));

        try {
            // 使用与 KryoDeserializer 相同的配置生成数据
            com.esotericsoftware.kryo.Kryo kryo = new com.esotericsoftware.kryo.Kryo();
            kryo.setRegistrationRequired(false);
            // 注意：这里使用默认的 references 设置（true）
            // 确保与 deserializer 的配置一致

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", "admin");
            data.put("action", "login");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.esotericsoftware.kryo.io.Output output = new com.esotericsoftware.kryo.io.Output(baos);
            kryo.writeClassAndObject(output, data);
            output.close();

            byte[] kryoData = baos.toByteArray();
            printByteDetails("Kryo", kryoData);

            // 测试反序列化
            KryoDeserializer deserializer = new KryoDeserializer();
            System.out.println("canDeserialize: " + deserializer.canDeserialize(kryoData));
            String result = deserializer.deserialize(kryoData);
            System.out.println("反序列化结果:\n" + result);
        } catch (Exception e) {
            System.out.println("Kryo 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试9：Avro 格式（容器格式）
     */
    public static void testAvroFormat() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试9：Avro 格式");
        System.out.println("特征：魔术头 0x4F 0x62 0x6A 0x01 (Obj\\x01)");
        System.out.println(repeat("=", 60));

        try {
            // 定义 Avro Schema
            String schemaJson = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[" +
                "{\"name\":\"username\",\"type\":\"string\"}," +
                "{\"name\":\"password\",\"type\":\"string\"}," +
                "{\"name\":\"age\",\"type\":\"int\"}" +
                "]}";
            Schema schema = new Schema.Parser().parse(schemaJson);

            // 创建记录
            GenericRecord user = new GenericData.Record(schema);
            user.put("username", "admin");
            user.put("password", "secret123");
            user.put("age", 25);

            // 序列化为 Avro 容器格式
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
            DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);
            dataFileWriter.create(schema, baos);
            dataFileWriter.append(user);
            dataFileWriter.close();

            byte[] avroData = baos.toByteArray();
            printByteDetails("Avro", avroData);

            // 验证魔术头 Obj\x01
            System.out.println("魔术头验证: " +
                (avroData[0] == 0x4F && avroData[1] == 0x62 && avroData[2] == 0x6A && avroData[3] == 0x01
                    ? "✓ 正确 (Obj\\x01)" : "✗ 错误"));

            // 测试反序列化
            AvroDeserializer deserializer = new AvroDeserializer();
            System.out.println("canDeserialize: " + deserializer.canDeserialize(avroData));
            String result = deserializer.deserialize(avroData);
            System.out.println("反序列化结果:\n" + result);
        } catch (Exception e) {
            System.out.println("Avro 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 自动检测测试 ====================

    /**
     * 测试10：BinaryDeserializerFactory 自动检测
     */
    public static void testAutoDetection() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试10：自动格式检测");
        System.out.println(repeat("=", 60));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("test", "data");
        data.put("count", 123);

        // 测试各种格式的自动检测
        System.out.println("\n--- BSON 自动检测 ---");
        byte[] bsonData = generateBsonData(data);
        System.out.println("检测格式: " + BinaryDeserializerFactory.detectFormat(bsonData));
        System.out.println("反序列化: " + BinaryDeserializerFactory.autoDeserialize(bsonData));

        System.out.println("\n--- MessagePack 自动检测 ---");
        byte[] msgpackData = generateMessagePackData(data);
        System.out.println("检测格式: " + BinaryDeserializerFactory.detectFormat(msgpackData));
        System.out.println("反序列化: " + BinaryDeserializerFactory.autoDeserialize(msgpackData));

        System.out.println("\n--- CBOR 自动检测 ---");
        byte[] cborData = generateCborData(data);
        System.out.println("检测格式: " + BinaryDeserializerFactory.detectFormat(cborData));
        System.out.println("反序列化: " + BinaryDeserializerFactory.autoDeserialize(cborData));

        System.out.println("\n--- Smile 自动检测 ---");
        byte[] smileData = generateSmileData(data);
        System.out.println("检测格式: " + BinaryDeserializerFactory.detectFormat(smileData));
        System.out.println("反序列化: " + BinaryDeserializerFactory.autoDeserialize(smileData));

        System.out.println("\n--- UBJSON 自动检测 ---");
        byte[] ubjsonData = generateUbjsonData(data);
        System.out.println("检测格式: " + BinaryDeserializerFactory.detectFormat(ubjsonData));
        System.out.println("反序列化: " + BinaryDeserializerFactory.autoDeserialize(ubjsonData));
    }

    // ==================== WebShell 解密服务测试 ====================

    /**
     * 测试11：完整 WebShell 解密流程（Base64 编码）
     */
    public static void testWebShellDecryptBase64() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试11：WebShell 解密流程（Base64编码）");
        System.out.println(repeat("=", 60));

        WebShellDecryptService service = new WebShellDecryptService();
        DecryptConfig config = DecryptConfig.builder().build();

        // 测试 CBOR + Base64
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cmd", "whoami");
        data.put("path", "/etc");

        byte[] cborData = generateCborData(data);
        String base64Data = bytesToBase64(cborData);

        String content = "POST /shell.php HTTP/1.1\r\n" +
                "Host: victim.com\r\n" +
                "Content-Type: application/x-www-form-urlencoded\r\n" +
                "\r\n" +
                "data=" + base64Data;

        System.out.println("请求内容:\n" + content);
        System.out.println("\n原始 CBOR 数据 (Base64): " + base64Data);

        DecryptResult result = service.decryptContent(content, config);
        System.out.println("\n解密模式: " + result.getEncodeModes());
        System.out.println("解密结果:\n" + result.getData());
    }

    /**
     * 测试12：完整 WebShell 解密流程（Hex 编码）
     */
    public static void testWebShellDecryptHex() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试12：WebShell 解密流程（Hex编码）");
        System.out.println(repeat("=", 60));

        WebShellDecryptService service = new WebShellDecryptService();
        DecryptConfig config = DecryptConfig.builder().build();

        // 测试 MessagePack + Hex
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "execute");
        data.put("target", "/bin/sh");

        byte[] msgpackData = generateMessagePackData(data);
        String hexData = bytesToHex(msgpackData);

        String content = "payload=" + hexData;

        System.out.println("请求内容: " + content);
        System.out.println("原始 MessagePack 数据 (Hex): " + hexData);

        DecryptResult result = service.decryptContent(content, config);
        System.out.println("\n解密模式: " + result.getEncodeModes());
        System.out.println("解密结果:\n" + result.getData());
    }

    /**
     * 测试13：完整 WebShell 解密流程（byte[] 数组格式）
     */
    public static void testWebShellDecryptByteArray() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试13：WebShell 解密流程（byte[]数组格式）");
        System.out.println(repeat("=", 60));

        WebShellDecryptService service = new WebShellDecryptService();
        DecryptConfig config = DecryptConfig.builder().build();

        // 测试 Smile + byte[] 数组
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", "root");
        data.put("level", 9);

        byte[] smileData = generateSmileData(data);

        // 转换为 Java byte[] 格式字符串
        StringBuilder byteArrayStr = new StringBuilder("{");
        for (int i = 0; i < smileData.length; i++) {
            byteArrayStr.append(smileData[i]);
            if (i < smileData.length - 1) {
                byteArrayStr.append(", ");
            }
        }
        byteArrayStr.append("}");

        String content = "session=" + byteArrayStr;

        System.out.println("请求内容: " + content);

        DecryptResult result = service.decryptContent(content, config);
        System.out.println("\n解密模式: " + result.getEncodeModes());
        System.out.println("解密结果:\n" + result.getData());
    }

    // ==================== 边界测试 ====================

    /**
     * 测试14：负面测试 - 普通 JSON 不应被识别为 UBJSON
     */
    public static void testNegativeJsonNotUbjson() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试14：负面测试 - 普通JSON不应被识别为UBJSON");
        System.out.println(repeat("=", 60));

        // 普通 JSON 以 {" 开头
        String json = "{\"name\":\"test\",\"value\":100}";
        byte[] jsonBytes = json.getBytes();

        System.out.println("测试数据: " + json);
        System.out.println("首字节: 0x" + String.format("%02X", jsonBytes[0]));
        System.out.println("第二字节: 0x" + String.format("%02X", jsonBytes[1]) + " (应该是 \" = 0x22)");

        UbjsonDeserializer ubjsonDeserializer = new UbjsonDeserializer();
        System.out.println("UBJSON canDeserialize: " + ubjsonDeserializer.canDeserialize(jsonBytes) + " (应该是 false)");

        String detected = BinaryDeserializerFactory.detectFormat(jsonBytes);
        System.out.println("自动检测格式: " + (detected == null ? "null (正确)" : detected + " (错误)"));
    }

    /**
     * 测试15：负面测试 - 短字节序列不应被误判
     */
    public static void testNegativeShortBytes() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试15：负面测试 - 短字节序列不应被误判");
        System.out.println(repeat("=", 60));

        // 只有 5 个字节，小于最小长度限制
        byte[] shortData = new byte[]{(byte) 0xA2, 0x01, 0x02, 0x03, 0x04};

        System.out.println("测试数据长度: " + shortData.length + " 字节");
        System.out.println("首字节: 0x" + String.format("%02X", shortData[0]) + " (CBOR fixmap 范围)");

        CborDeserializer cborDeserializer = new CborDeserializer();
        System.out.println("CBOR canDeserialize: " + cborDeserializer.canDeserialize(shortData) +
            " (应该是 false，因为长度 < 10)");

        MessagePackDeserializer msgpackDeserializer = new MessagePackDeserializer();
        System.out.println("MessagePack canDeserialize: " + msgpackDeserializer.canDeserialize(shortData) +
            " (应该是 false，因为长度 < 10)");
    }

    /**
     * 测试16：兜底反序列化器测试
     */
    public static void testFallbackDeserializers() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试16：兜底反序列化器测试");
        System.out.println(repeat("=", 60));

        try {
            // 创建 Kryo 数据（使用与 deserializer 一致的配置）
            com.esotericsoftware.kryo.Kryo kryo = new com.esotericsoftware.kryo.Kryo();
            kryo.setRegistrationRequired(false);
            // 使用默认的 references=true 设置

            Map<String, String> data = new HashMap<>();
            data.put("secret", "password123");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.esotericsoftware.kryo.io.Output output = new com.esotericsoftware.kryo.io.Output(baos);
            kryo.writeClassAndObject(output, data);
            output.close();

            byte[] kryoData = baos.toByteArray();

            System.out.println("Kryo 数据长度: " + kryoData.length);
            System.out.println("Kryo 数据首字节: 0x" + String.format("%02X", kryoData[0] & 0xFF));
            System.out.println("Kryo 数据 (Hex): " + bytesToHex(kryoData));

            // 主检测 - Kryo 现在应该能被检测到（如果首字节是 0x01 且含有 java 类名）
            String primaryFormat = BinaryDeserializerFactory.detectFormat(kryoData);
            System.out.println("主检测格式: " + (primaryFormat != null ? primaryFormat : "null"));

            // 兜底检测应该能识别
            String fallbackFormat = BinaryDeserializerFactory.detectFormatWithFallback(kryoData);
            System.out.println("兜底检测格式: " + fallbackFormat);

            // 自动反序列化应该成功
            String result = BinaryDeserializerFactory.autoDeserialize(kryoData);
            System.out.println("反序列化结果: " + result);

        } catch (Exception e) {
            System.out.println("兜底测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 支持格式列表 ====================

    /**
     * 测试17：列出所有支持的格式
     */
    public static void testListSupportedFormats() {
        System.out.println("\n" + repeat("=", 60));
        System.out.println("测试17：支持的格式列表");
        System.out.println(repeat("=", 60));

        List<String> formats = BinaryDeserializerFactory.getSupportedFormats();
        System.out.println("支持的格式数量: " + formats.size());
        for (int i = 0; i < formats.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + formats.get(i));
        }
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        // 初始化安全提供者
        SecurityInitializer.initializeSecurityProvider();

        System.out.println("╔" + repeat("═", 68) + "╗");
        System.out.println("║" + centerText("二进制序列化格式解密功能测试", 68) + "║");
        System.out.println("║" + centerText("支持格式: BSON, MessagePack, CBOR, Smile, UBJSON,", 68) + "║");
        System.out.println("║" + centerText("Hessian, Kryo, FST, Avro", 68) + "║");
        System.out.println("╚" + repeat("═", 68) + "╝");

        try {
            // 格式检测测试（1-9）
            System.out.println("\n\n【第一部分：格式检测测试】");
            testBsonFormat();           // 1
            testMessagePackFormat();    // 2
            testCborFormat();           // 3
            testSmileFormat();          // 4
            testUbjsonFormat();         // 5
            testHessianFormat();        // 6
            testFstFormat();            // 7
            testKryoFormat();           // 8
            testAvroFormat();           // 9

            // 自动检测测试（10）
            System.out.println("\n\n【第二部分：自动检测测试】");
            testAutoDetection();        // 10

            // WebShell 解密测试（11-13）
            System.out.println("\n\n【第三部分：WebShell解密测试】");
            testWebShellDecryptBase64();    // 11
            testWebShellDecryptHex();       // 12
            testWebShellDecryptByteArray(); // 13

            // 边界测试（14-16）
            System.out.println("\n\n【第四部分：边界测试】");
            testNegativeJsonNotUbjson();    // 14
            testNegativeShortBytes();       // 15
            testFallbackDeserializers();    // 16

            // 格式列表（17）
            System.out.println("\n\n【第五部分：格式列表】");
            testListSupportedFormats();     // 17

            // 总结
            System.out.println("\n\n╔" + repeat("═", 68) + "╗");
            System.out.println("║" + centerText("所有测试用例执行完成（共17个）", 68) + "║");
            System.out.println("║" + centerText("格式检测: 9 | 自动检测: 1 | 解密流程: 3 | 边界测试: 3 | 其他: 1", 68) + "║");
            System.out.println("╚" + repeat("═", 68) + "╝");

        } catch (Exception e) {
            System.err.println("\n测试执行出错：");
            e.printStackTrace();
        }
    }

    /**
     * 居中文本
     */
    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return repeat(" ", Math.max(0, padding)) + text + repeat(" ", Math.max(0, width - padding - text.length()));
    }

    /**
     * Java 8 兼容的字符串重复方法
     */
    private static String repeat(String str, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
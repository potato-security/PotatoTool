package com.potato.potatotool.content.blueTeam.webshellDecrypt;

import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils.BinaryDeserializerFactory;
import com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils.SnappyMsgPackDeserializer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Snappy + MessagePack 解密器测试
 *
 * @author Potato
 * @date 2025/01/04
 */
class SnappyMsgPackDeserializerTest {

    /**
     * 测试样本数据（来自用户提供的样例）
     */
    private static final String SAMPLE_DATA = "/wYAAHNOYVBwWQCJDADQOweCmDzwfTsHAACCpG1ldGGEp3Nlc3Npb27ZIDE0MjExNmU3ZDk4NDRmZGViMTFlNDg5ODFlZWE4YjU4pHR5cGUApWZsdXNoAaRwYXJ0AqRkYXRhkYOjc2VxAKdwYXlsb2FkxgAABjffAAAAMahhY2xfbGlzdJCycmVxX3BvbGljeV9pZAkUSLdyZXFfc2t5bmV0X3J1bGVfaWQFGRSSzgABATkFBQUjUGRhdGFfY29sbGVjdGlvbl9pZHOQugE8AHAJcEBfZXhwbGFpbl9yZXN1bHSQtAEcQGRldGVjdG9yX2F0dGFja3OAEecggaVTTFNJRKCoASZ8Ym9kecQZPD9waHAgZWNobyBtZDUoMTIzNDU2KTs/PqsBJA1mCMQAswUOCcRsZ3JvdXBfaWShM6h1cmxfcGF0aLQvcGhwLWNnaREIEC5leGWvATQUZGVjb2RlBSXwSaCkaG9zdLRuZnlxc3lzMS5uZm1lZGlhLmNvbadyZWZlcmVyoKxxdWVyeV9zdHJpbmfZN61kIGFsbG93X3VybF9pbmNsdWRlPTEgARegdXRvX3ByZXBlbmRfZmlsZT1waHA6Ly9pbnB1dKZtZXRob2SkUE9TVKsBhi1jCNknbQUL8MkvMjcyZGM2NDI4YzJmMTFlZmFkNzkwMDE2M2UzNWIwM2WscmVxX2xvY2F0aW9up3VybHBhdGiqdXNlcl9hZ2VudNlxTW96aWxsYS81LjAgKFgxMTsgQ3JPUyBpNjg2IDM5MTIuMTAxLjApIEFwcGxlV2ViS2l0LzUzNy4zNiAoS0hUTUwsIGxpa2UgR2Vja28pIENocm9tZS8yNy4wLjE0NTMuMTE2IFNhZmFyaS81MzcuMzaqcmVxX2hlYWRlctoBn1BPU1QgL3BoQnUBGD8lQURkK2E+NQEQJTNEMSsJGz45AQglM0Q9O0AgSFRUUC8xLjENCkhvc3Q6IE6vARwNClVzZXItQSELCDogTf4LAb4LAXwNCkFjY2VwdC1FbmNvZGluZzogZ3ppcCwgZGVmbGF0ZREgKDogKi8qDQpDb25uaT8sOiBrZWVwLWFsaXZlBRhAdGVudC1UeXBlOiBhcHBsaWMl8TwveC13d3ctZm9ybS11cmxlAWZUZWQNClJFRElSRUNULVNUQVRVUzogMRlFOExlbmd0aDog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";

    @Test
    void testCanDeserialize() {
        SnappyMsgPackDeserializer deserializer = new SnappyMsgPackDeserializer();

        // 测试 Base64 编码的样本数据
        byte[] sampleBytes = SAMPLE_DATA.getBytes(StandardCharsets.US_ASCII);
        assertTrue(deserializer.canDeserialize(sampleBytes), "应该能识别 Base64 编码的 Snappy+MsgPack 数据");

        // 测试解码后的二进制数据
        byte[] decodedData = Base64.getDecoder().decode(SAMPLE_DATA);
        assertTrue(deserializer.canDeserialize(decodedData), "应该能识别二进制 Snappy+MsgPack 数据");
    }

    @Test
    void testDeserialize() {
        SnappyMsgPackDeserializer deserializer = new SnappyMsgPackDeserializer();

        // 测试 Base64 编码的样本数据
        byte[] sampleBytes = SAMPLE_DATA.getBytes(StandardCharsets.US_ASCII);
        String result = deserializer.deserialize(sampleBytes);

        System.out.println("解密结果:");
        System.out.println(result);

        assertNotNull(result, "解密结果不应为空");
        // 检查结果不为空且不是错误信息
        assertFalse(result.startsWith("unable to decode"), "解密不应失败");
        // 检查结果应该是有效的JSON对象
        assertTrue(result.startsWith("{") || result.startsWith("["), "解密结果应该是JSON对象或数组");
        // 验证包含关键字段
        assertTrue(result.contains("meta"), "结果应包含 'meta' 字段");
        assertTrue(result.contains("session"), "结果应包含 'session' 字段");
    }

    @Test
    void testAutoDeserialize() {
        // 测试工厂自动检测
        byte[] sampleBytes = SAMPLE_DATA.getBytes(StandardCharsets.US_ASCII);
        String result = BinaryDeserializerFactory.autoDeserialize(sampleBytes);

        assertNotNull(result, "自动解密结果不应为空");

        System.out.println("自动检测解密结果:");
        System.out.println(result);

        // 验证包含关键字段
        assertTrue(result.contains("meta"), "结果应包含 'meta' 字段");
    }

    @Test
    void testFormatName() {
        SnappyMsgPackDeserializer deserializer = new SnappyMsgPackDeserializer();
        assertEquals("SNAPPY_MSGPACK", deserializer.getFormatName());
    }

    @Test
    void testInvalidData() {
        SnappyMsgPackDeserializer deserializer = new SnappyMsgPackDeserializer();

        // 测试无效数据
        byte[] invalidData = "this is not snappy msgpack data".getBytes(StandardCharsets.UTF_8);
        assertFalse(deserializer.canDeserialize(invalidData), "不应识别无效数据");

        // 测试空数据
        assertFalse(deserializer.canDeserialize(new byte[0]), "不应识别空数据");
        assertFalse(deserializer.canDeserialize(null), "不应识别null数据");
    }

    @Test
    void testDetectFormat() {
        byte[] sampleBytes = SAMPLE_DATA.getBytes(StandardCharsets.US_ASCII);
        String format = BinaryDeserializerFactory.detectFormat(sampleBytes);

        assertEquals("SNAPPY_MSGPACK", format, "应该检测为 SNAPPY_MSGPACK 格式");
    }
}
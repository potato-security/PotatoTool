package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potato.potatotool.utils.data.StrUtils;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.potato.potatotool.content.classObj.BinaryFormatConstants.FORMAT_SNAPPY_MSGPACK;

/**
 * Snappy + MessagePack 组合格式反序列化器
 * <p>
 * 支持解密 Base64 + Snappy Framing Format + MessagePack 格式的日志数据
 * </p>
 * <p>
 * 数据格式流程：
 * Base64 → [4字节头] + Snappy(sNaPpY) → [4字节长度头] + MessagePack
 * </p>
 * <p>
 * 典型应用场景：
 * - 安全设备日志解密
 * - WAF/IDS 日志分析
 * - 网络流量日志解码
 * </p>
 *
 * @author Potato
 * @date 2025/01/04
 */
public class SnappyMsgPackDeserializer extends AbstractBinaryDeserializer {

    /**
     * Snappy Framing Format 魔术头: "sNaPpY"
     */
    private static final byte[] SNAPPY_MAGIC = "sNaPpY".getBytes(StandardCharsets.US_ASCII);

    /**
     * MessagePack ObjectMapper（线程安全，单例复用）
     */
    private static final ObjectMapper MSGPACK_MAPPER = new ObjectMapper(new MessagePackFactory());

    @Override
    public String deserialize(byte[] data) {
        try {
            byte[] processedData = data;
            if (data == null || data.length == 0) {
                return null;
            }

            // 1. 尝试 Base64 解码（如果输入是文本格式）
            processedData = tryBase64Decode(processedData);

            // 2. 查找 Snappy 魔术头位置
            int snappyPos = findSnappyMagic(processedData);
            if (snappyPos < 0) {
                return formatError("SnappyMsgPack", new IllegalArgumentException("未找到 Snappy 标识 'sNaPpY'"), data);
            }

            // 3. 从 Snappy 魔术头开始，尝试多种解压策略
            byte[] snappyData = Arrays.copyOfRange(processedData, snappyPos, processedData.length);
            byte[] msgpackData = extractMessagePackData(snappyData);

            if (msgpackData == null || msgpackData.length == 0) {
                return formatError("SnappyMsgPack", new IllegalArgumentException("无法提取 MessagePack 数据"), data);
            }

            // 4. 反序列化 MessagePack
            Object result = MSGPACK_MAPPER.readValue(msgpackData, Object.class);

            // 5. 递归处理嵌套的 MessagePack 和 bytes
            result = processNestedData(result);

            return toJsonString(result);

        } catch (Exception e) {
            return formatError("SnappyMsgPack", e, data);
        }
    }

    /**
     * 提取 MessagePack 数据
     * <p>
     * 尝试多种策略提取 MessagePack 数据：
     * 1. 先尝试 Snappy 解压
     * 2. 如果解压失败，直接在数据中查找 MessagePack 起始位置
     * </p>
     */
    private byte[] extractMessagePackData(byte[] snappyData) {
        // 策略 1: 尝试 Snappy Framing Format 解压
        try {
            byte[] decompressed = decompressSnappyFramed(snappyData);
            if (decompressed != null && decompressed.length > 4) {
                // 检查解压后的数据是否包含有效的 MessagePack
                int msgpackStart = findMessagePackStart(decompressed);
                if (msgpackStart >= 0) {
                    byte[] msgpackData = Arrays.copyOfRange(decompressed, msgpackStart, decompressed.length);
                    if (isValidMessagePack(msgpackData)) {
                        return msgpackData;
                    }
                }
            }
        } catch (Exception e) {
            // 解压失败，尝试其他策略
        }

        // 策略 2: 直接在原始数据中查找 MessagePack 起始位置
        // 跳过 "sNaPpY"(6) + chunk_header(4) + CRC(4) = 14 字节
        int searchStart = 6; // 从 "sNaPpY" 之后开始搜索
        int msgpackStart = findMessagePackStart(snappyData, searchStart);
        if (msgpackStart >= 0) {
            byte[] msgpackData = Arrays.copyOfRange(snappyData, msgpackStart, snappyData.length);
            if (isValidMessagePack(msgpackData)) {
                return msgpackData;
            }
        }

        return null;
    }

    /**
     * 在数据中查找 MessagePack 起始位置
     */
    private int findMessagePackStart(byte[] data) {
        return findMessagePackStart(data, 0);
    }

    /**
     * 在数据中从指定位置开始查找 MessagePack 起始位置
     * <p>
     * MessagePack Map 起始字节：
     * - 0x80-0x8F: fixmap (0-15 elements)
     * - 0xDE: map16
     * - 0xDF: map32
     * </p>
     */
    private int findMessagePackStart(byte[] data, int startPos) {
        if (data == null || data.length < startPos + 2) {
            return -1;
        }

        // 在前 100 字节内搜索
        int searchLen = Math.min(100, data.length - 1);
        for (int i = startPos; i < searchLen; i++) {
            int b = data[i] & 0xFF;
            // 检查是否是 MessagePack Map 起始
            if ((b >= 0x80 && b <= 0x8F) || b == 0xDE || b == 0xDF) {
                // 额外检查：下一个字节应该是 fixstr (0xA0-0xBF) 或 str8/str16/str32
                if (i + 1 < data.length) {
                    int nextByte = data[i + 1] & 0xFF;
                    if ((nextByte >= 0xA0 && nextByte <= 0xBF) ||
                        nextByte == 0xD9 || nextByte == 0xDA || nextByte == 0xDB) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * 验证数据是否为有效的 MessagePack
     */
    private boolean isValidMessagePack(byte[] data) {
        if (data == null || data.length < 10) {
            return false;
        }
        try {
            Object result = MSGPACK_MAPPER.readValue(data, Object.class);
            // 检查结果是否为 Map 或 List（日志数据通常是这种结构）
            return result instanceof Map || result instanceof List;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean canDeserialize(byte[] data) {
        if (data == null || data.length < 10) {
            return false;
        }

        // 尝试 Base64 解码后检测
        byte[] processedData = tryBase64Decode(data);

        // 查找 Snappy 魔术头
        return findSnappyMagic(processedData) >= 0;
    }

    @Override
    public String getFormatName() {
        return FORMAT_SNAPPY_MSGPACK;
    }

    /**
     * 尝试 Base64 解码
     * <p>
     * 如果数据看起来像 Base64 编码（纯文本、高比例 Base64 字符），则尝试解码
     * </p>
     *
     * @param data 原始数据
     * @return 解码后的数据，如果不是 Base64 则返回原数据
     */
    private byte[] tryBase64Decode(byte[] data) {
        // 检查是否看起来像 Base64 文本
        if (!looksLikeBase64(data)) {
            return data;
        }

        try {
            // 移除可能的空白字符
            String text = new String(data, StandardCharsets.US_ASCII).trim();
            return Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException e) {
            // 不是有效的 Base64，返回原数据
            return data;
        }
    }

    /**
     * 检测数据是否看起来像 Base64 编码
     */
    private boolean looksLikeBase64(byte[] data) {
        if (data.length < 10) {
            return false;
        }

        // 如果已经包含 Snappy 魔术头，则不是 Base64
        if (findSnappyMagic(data) >= 0) {
            return false;
        }

        // 检查前 100 字节是否主要是 Base64 字符
        int checkLen = Math.min(100, data.length);
        int base64Chars = 0;

        for (int i = 0; i < checkLen; i++) {
            int c = data[i] & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
                (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=') {
                base64Chars++;
            } else if (c != '\r' && c != '\n' && c != ' ' && c != '\t') {
                // 非 Base64 字符且非空白，可能不是 Base64
                if (base64Chars < checkLen * 0.7) {
                    return false;
                }
            }
        }

        // 80% 以上是 Base64 字符则认为是 Base64
        return base64Chars >= checkLen * 0.8;
    }

    /**
     * 查找 Snappy 魔术头位置
     *
     * @param data 数据
     * @return 魔术头位置，未找到返回 -1
     */
    private int findSnappyMagic(byte[] data) {
        if (data == null || data.length < SNAPPY_MAGIC.length) {
            return -1;
        }

        // 在前 100 字节内查找（通常魔术头在数据开头附近）
        int searchLen = Math.min(100, data.length - SNAPPY_MAGIC.length + 1);

        for (int i = 0; i < searchLen; i++) {
            if (matchBytes(data, i, SNAPPY_MAGIC)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 匹配字节序列
     */
    private boolean matchBytes(byte[] data, int offset, byte[] pattern) {
        if (offset + pattern.length > data.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解压 Snappy Framing Format 数据
     * <p>
     * Snappy Framing Format 结构：
     * - 魔术头: "sNaPpY" (6 bytes)
     * - 多个 chunk，每个 chunk:
     *   - chunk_type (1 byte): 0x00=compressed, 0x01=uncompressed, 0xff=stream identifier
     *   - chunk_len (3 bytes, little-endian)
     *   - chunk_data (chunk_len bytes, 前 4 字节是 CRC32 校验)
     * </p>
     *
     * @param data Snappy framing format 数据（以 "sNaPpY" 开头）
     * @return 解压后的数据
     */
    private byte[] decompressSnappyFramed(byte[] data) throws Exception {
        if (!matchBytes(data, 0, SNAPPY_MAGIC)) {
            throw new IllegalArgumentException("数据不是 Snappy Framing Format");
        }

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int pos = 6; // 跳过 "sNaPpY"

        while (pos + 4 <= data.length) {
            int chunkType = data[pos] & 0xFF;
            int chunkLen = (data[pos + 1] & 0xFF) |
                          ((data[pos + 2] & 0xFF) << 8) |
                          ((data[pos + 3] & 0xFF) << 16);
            pos += 4;

            if (pos + chunkLen > data.length) {
                // 数据不完整，使用剩余数据
                chunkLen = data.length - pos;
            }

            byte[] chunkData = Arrays.copyOfRange(data, pos, pos + chunkLen);

            switch (chunkType) {
                case 0x00: // Compressed data (前 4 字节是 CRC32)
                    if (chunkData.length > 4) {
                        byte[] compressedPayload = Arrays.copyOfRange(chunkData, 4, chunkData.length);
                        byte[] decompressed = decompressRawSnappy(compressedPayload);
                        result.write(decompressed);
                    }
                    break;

                case 0x01: // Uncompressed data (前 4 字节是 CRC32)
                    if (chunkData.length > 4) {
                        result.write(chunkData, 4, chunkData.length - 4);
                    }
                    break;

                case 0xFF: // Stream identifier，跳过
                    break;

                default:
                    // 未知 chunk 类型，跳过
                    break;
            }

            pos += chunkLen;
        }

        return result.toByteArray();
    }

    /**
     * 手动实现原始 Snappy 解压算法
     * <p>
     * 基于 Python 实现移植，处理以下标签类型：
     * - Literal (tag_type == 0): 直接复制字面量数据
     * - Copy 1/2/3 (tag_type == 1/2/3): 从已解压数据中复制
     * </p>
     *
     * @param data 压缩数据
     * @return 解压后的数据
     */
    private byte[] decompressRawSnappy(byte[] data) {
        int pos = 0;

        // 读取 varint 格式的未压缩长度
        int uncompressedLen = 0;
        int shift = 0;
        while (pos < data.length) {
            int b = data[pos] & 0xFF;
            pos++;
            uncompressedLen |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
        }

        ByteArrayOutputStream result = new ByteArrayOutputStream(uncompressedLen);

        while (pos < data.length && result.size() < uncompressedLen) {
            int tag = data[pos] & 0xFF;
            pos++;
            int tagType = tag & 0x03;

            if (tagType == 0) {
                // Literal: 直接复制字面量数据
                int litLen = (tag >> 2) + 1;
                if (litLen > 60) {
                    // 扩展长度编码
                    int extra = litLen - 60;
                    litLen = 1;
                    for (int i = 0; i < extra && pos + i < data.length; i++) {
                        litLen += (data[pos + i] & 0xFF) << (8 * i);
                    }
                    pos += extra;
                }
                // 复制字面量数据
                int copyLen = Math.min(litLen, data.length - pos);
                result.write(data, pos, copyLen);
                pos += litLen;
            } else {
                // Copy: 从已解压数据中复制
                int length;
                int offset;

                if (tagType == 1) {
                    // Copy with 1-byte offset
                    length = ((tag >> 2) & 0x07) + 4;
                    offset = ((tag >> 5) << 8) | ((pos < data.length) ? (data[pos] & 0xFF) : 0);
                    pos += 1;
                } else if (tagType == 2) {
                    // Copy with 2-byte offset
                    length = (tag >> 2) + 1;
                    offset = (pos + 2 <= data.length) ?
                            ((data[pos] & 0xFF) | ((data[pos + 1] & 0xFF) << 8)) : 0;
                    pos += 2;
                } else {
                    // tagType == 3: Copy with 4-byte offset
                    length = (tag >> 2) + 1;
                    offset = (pos + 4 <= data.length) ?
                            ((data[pos] & 0xFF) |
                             ((data[pos + 1] & 0xFF) << 8) |
                             ((data[pos + 2] & 0xFF) << 16) |
                             ((data[pos + 3] & 0xFF) << 24)) : 0;
                    pos += 4;
                }

                // 从已解压的数据中复制
                if (offset > 0 && offset <= result.size()) {
                    byte[] currentResult = result.toByteArray();
                    int start = currentResult.length - offset;
                    for (int i = 0; i < length; i++) {
                        result.write(currentResult[start + (i % offset)]);
                    }
                }
            }
        }

        return result.toByteArray();
    }

    /**
     * 递归处理嵌套的 MessagePack 数据和 bytes
     * <p>
     * - 检测嵌套的 MessagePack 并解析
     * - 将 byte[] 转换为 UTF-8 字符串或 hex
     * </p>
     */
    private Object processNestedData(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof byte[]) {
            byte[] bytes = (byte[]) obj;
            // 尝试检测嵌套的 MessagePack
            if (isMessagePackData(bytes)) {
                try {
                    Object nested = MSGPACK_MAPPER.readValue(bytes, Object.class);
                    return processNestedData(nested);
                } catch (Exception e) {
                    // 不是有效的 MessagePack
                }
            }
            // 尝试 UTF-8 解码
            return tryDecodeUtf8(bytes);
        }

        if (obj instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) obj;
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                Object key = processNestedData(entry.getKey());
                Object value = processNestedData(entry.getValue());
                result.put(key, value);
            }
            return result;
        }

        if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                result.add(processNestedData(item));
            }
            return result;
        }

        return obj;
    }

    /**
     * 检测数据是否可能是 MessagePack
     */
    private boolean isMessagePackData(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }
        int firstByte = data[0] & 0xFF;
        // MessagePack Map: 0x80-0x8F, 0xDE, 0xDF
        // MessagePack Array: 0x90-0x9F, 0xDC, 0xDD
        return (firstByte >= 0x80 && firstByte <= 0x9F) ||
               firstByte == 0xDE || firstByte == 0xDF ||
               firstByte == 0xDC || firstByte == 0xDD;
    }

    /**
     * 尝试将 bytes 解码为 UTF-8 字符串
     */
    private Object tryDecodeUtf8(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try {
            String str = new String(bytes, StandardCharsets.UTF_8);
            // 检查是否是有效的 UTF-8 字符串（无乱码）
            if (isValidUtf8String(str)) {
                return str;
            }
        } catch (Exception e) {
            // 忽略
        }
        // 返回 hex 编码
        return StrUtils.byteToHex(bytes);
    }

    /**
     * 检查字符串是否是有效的可打印 UTF-8
     */
    private boolean isValidUtf8String(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        int printable = 0;
        for (char c : str.toCharArray()) {
            if (c >= 32 && c < 127 || Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                printable++;
            }
        }
        // 70% 以上可打印则认为是有效字符串
        return printable >= str.length() * 0.7;
    }
}
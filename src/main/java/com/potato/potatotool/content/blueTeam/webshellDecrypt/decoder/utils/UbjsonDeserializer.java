package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import static com.potato.potatotool.content.classObj.BinaryFormatConstants.*;

/**
 * UBJSON格式反序列化器
 *
 * Universal Binary JSON格式的启发式解析
 * - UBJSON 首字节：0x7B (Object {) 或 0x5B (Array [)
 * - MessagePack 首字节：0x80-0x9F (fixmap/fixarray)
 *
 * @author Potato
 * @date 2025/12/29
 */
public class UbjsonDeserializer extends AbstractBinaryDeserializer {

    // UBJSON 类型标记
    private static final byte TYPE_OBJECT = 0x7B; // {
    private static final byte TYPE_ARRAY = 0x5B;  // [
    private static final byte TYPE_OBJECT_END = 0x7D; // }
    private static final byte TYPE_ARRAY_END = 0x5D;  // ]
    private static final byte TYPE_STRING = 0x53; // S
    private static final byte TYPE_INT8 = 0x69;   // i
    private static final byte TYPE_UINT8 = 0x55;  // U
    private static final byte TYPE_INT16 = 0x49;  // I
    private static final byte TYPE_INT32 = 0x6C;  // l
    private static final byte TYPE_INT64 = 0x4C;  // L
    private static final byte TYPE_FLOAT32 = 0x64; // d
    private static final byte TYPE_FLOAT64 = 0x44; // D
    private static final byte TYPE_TRUE = 0x54;   // T
    private static final byte TYPE_FALSE = 0x46;  // F
    private static final byte TYPE_NULL = 0x5A;   // Z
    private static final byte TYPE_CHAR = 0x43;   // C
    private static final byte TYPE_NOOP = 0x4E;   // N

    private int position;

    @Override
    public String deserialize(byte[] data) {
        try {
            position = 0;
            Object result = parseValue(data);
            return toJsonString(result);
        } catch (Exception e) {
            return formatError("UBJSON", e, data);
        }
    }

    private Object parseValue(byte[] data) throws Exception {
        if (position >= data.length) {
            throw new Exception("Unexpected end of data");
        }

        byte type = data[position++];

        switch (type) {
            case TYPE_NULL:
                return null;
            case TYPE_TRUE:
                return true;
            case TYPE_FALSE:
                return false;
            case TYPE_INT8:
                return (int) data[position++];
            case TYPE_UINT8:
                return data[position++] & 0xFF;
            case TYPE_INT16:
                return readInt16(data);
            case TYPE_INT32:
                return readInt32(data);
            case TYPE_INT64:
                return readInt64(data);
            case TYPE_FLOAT32:
                return Float.intBitsToFloat(readInt32(data));
            case TYPE_FLOAT64:
                return Double.longBitsToDouble(readInt64(data));
            case TYPE_CHAR:
                return String.valueOf((char) data[position++]);
            case TYPE_STRING:
                return readString(data);
            case TYPE_OBJECT:
                return parseObject(data);
            case TYPE_ARRAY:
                return parseArray(data);
            case TYPE_NOOP:
                return parseValue(data); // Skip NOOP
            default:
                throw new Exception("Unknown UBJSON type: 0x" + Integer.toHexString(type & 0xFF));
        }
    }

    private java.util.Map<String, Object> parseObject(byte[] data) throws Exception {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();

        while (position < data.length && data[position] != TYPE_OBJECT_END) {
            // 读取 key（先读取长度类型，再读取字符串）
            int keyLength = readLength(data);
            String key = new String(data, position, keyLength, "UTF-8");
            position += keyLength;

            // 读取 value
            Object value = parseValue(data);
            map.put(key, value);
        }

        if (position < data.length && data[position] == TYPE_OBJECT_END) {
            position++;
        }

        return map;
    }

    private java.util.List<Object> parseArray(byte[] data) throws Exception {
        java.util.List<Object> list = new java.util.ArrayList<>();

        while (position < data.length && data[position] != TYPE_ARRAY_END) {
            list.add(parseValue(data));
        }

        if (position < data.length && data[position] == TYPE_ARRAY_END) {
            position++;
        }

        return list;
    }

    private int readLength(byte[] data) throws Exception {
        byte type = data[position++];
        switch (type) {
            case TYPE_INT8:
                return data[position++];
            case TYPE_UINT8:
                return data[position++] & 0xFF;
            case TYPE_INT16:
                return readInt16(data);
            case TYPE_INT32:
                return readInt32(data);
            default:
                throw new Exception("Invalid length type: 0x" + Integer.toHexString(type & 0xFF));
        }
    }

    private String readString(byte[] data) throws Exception {
        int length = readLength(data);
        String str = new String(data, position, length, "UTF-8");
        position += length;
        return str;
    }

    private int readInt16(byte[] data) {
        int value = ((data[position] & 0xFF) << 8) | (data[position + 1] & 0xFF);
        position += 2;
        return (short) value;
    }

    private int readInt32(byte[] data) {
        int value = ((data[position] & 0xFF) << 24) |
                   ((data[position + 1] & 0xFF) << 16) |
                   ((data[position + 2] & 0xFF) << 8) |
                   (data[position + 3] & 0xFF);
        position += 4;
        return value;
    }

    private long readInt64(byte[] data) {
        long value = ((long) (data[position] & 0xFF) << 56) |
                    ((long) (data[position + 1] & 0xFF) << 48) |
                    ((long) (data[position + 2] & 0xFF) << 40) |
                    ((long) (data[position + 3] & 0xFF) << 32) |
                    ((long) (data[position + 4] & 0xFF) << 24) |
                    ((long) (data[position + 5] & 0xFF) << 16) |
                    ((long) (data[position + 6] & 0xFF) << 8) |
                    ((long) (data[position + 7] & 0xFF));
        position += 8;
        return value;
    }

    @Override
    public boolean canDeserialize(byte[] data) {
        return isUbjsonFormat(data);
    }

    @Override
    public String getFormatName() {
        return FORMAT_UBJSON;
    }
}

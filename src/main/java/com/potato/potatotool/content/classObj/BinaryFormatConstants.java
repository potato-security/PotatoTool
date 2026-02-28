package com.potato.potatotool.content.classObj;

import java.util.*;

/**
 * 二进制序列化格式常量定义
 * <p>
 * 定义常见二进制序列化格式的魔术头、MIME类型映射等常量
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class BinaryFormatConstants {

    // ========== 魔术头定义 ==========
    /**
     * 二进制格式魔术头映射（按优先级排序）
     * LinkedHashMap保证检测顺序：精确魔术头 → 启发式检测
     */
    public static final Map<String, byte[]> MAGIC_HEADERS;

    static {
        MAGIC_HEADERS = new LinkedHashMap<>();
        // 精确魔术头（高优先级）
        MAGIC_HEADERS.put("SMILE", new byte[]{0x3A, 0x29, 0x0A}); // :)\n
        MAGIC_HEADERS.put("HESSIAN2", new byte[]{0x48, 0x02}); // H + version 2
        MAGIC_HEADERS.put("HESSIAN1", new byte[]{0x48, 0x01}); // H + version 1
        MAGIC_HEADERS.put("EXI", new byte[]{0x24, 0x45, 0x58, 0x49}); // $EXI
        MAGIC_HEADERS.put("UBJSON", new byte[]{0x7B}); // {
        MAGIC_HEADERS.put("AVRO_CONTAINER", new byte[]{0x4F, 0x62, 0x6A, 0x01}); // Obj\x01
    }

    // ========== MIME类型映射 ==========
    /**
     * Content-Type MIME类型到格式名称的映射
     */
    public static final Map<String, String> MIME_TYPE_MAP;

    // ========== 格式名称常量 ==========
    /** BSON格式（MongoDB） */
    public static final String FORMAT_BSON = "BSON";

    /** MessagePack格式 */
    public static final String FORMAT_MESSAGEPACK = "MESSAGEPACK";

    /** CBOR格式（RFC 8949） */
    public static final String FORMAT_CBOR = "CBOR";

    /** Smile格式（Jackson二进制JSON） */
    public static final String FORMAT_SMILE = "SMILE";

    /** Hessian格式（Caucho） */
    public static final String FORMAT_HESSIAN = "HESSIAN";

    /** UBJSON格式（Universal Binary JSON） */
    public static final String FORMAT_UBJSON = "UBJSON";

    /** Kryo格式（Java专用） */
    public static final String FORMAT_KRYO = "KRYO";

    /** FST格式（Fast Serialization） */
    public static final String FORMAT_FST = "FST";

    /** Protobuf格式（Google） */
    public static final String FORMAT_PROTOBUF = "PROTOBUF";

    /** Avro格式（Apache） */
    public static final String FORMAT_AVRO = "AVRO";

    /** Thrift格式（Apache） */
    public static final String FORMAT_THRIFT = "THRIFT";

    /** Snappy + MessagePack 组合格式（安全日志） */
    public static final String FORMAT_SNAPPY_MSGPACK = "SNAPPY_MSGPACK";


    static {
        MIME_TYPE_MAP = new HashMap<>();
        MIME_TYPE_MAP.put("application/bson", FORMAT_BSON);
        MIME_TYPE_MAP.put("application/msgpack", FORMAT_MESSAGEPACK);
        MIME_TYPE_MAP.put("application/x-msgpack", FORMAT_MESSAGEPACK);
        MIME_TYPE_MAP.put("application/cbor", FORMAT_CBOR);
        MIME_TYPE_MAP.put("application/x-hessian", FORMAT_HESSIAN);
        MIME_TYPE_MAP.put("application/x-jackson-smile", FORMAT_SMILE);
        MIME_TYPE_MAP.put("application/ubjson", FORMAT_UBJSON);
        MIME_TYPE_MAP.put("application/x-kryo", FORMAT_KRYO);
        MIME_TYPE_MAP.put("application/x-protobuf", FORMAT_PROTOBUF);
        MIME_TYPE_MAP.put("application/protobuf", FORMAT_PROTOBUF);
        MIME_TYPE_MAP.put("application/avro", FORMAT_AVRO);
        MIME_TYPE_MAP.put("application/x-thrift", FORMAT_THRIFT);
    }

    // ========== 检测辅助方法 ==========

    /**
     * 检测UBJSON格式特征
     * <p>
     * UBJSON特征：
     * - 首字节是 0x7B (Object {) 或 0x5B (Array [)
     * - 但必须排除普通 JSON（第二字节是 " 0x22 表示纯文本 JSON）
     * - UBJSON 对象内的 key 以类型标记开头（如 i 表示 int8 长度）
     * </p>
     *
     * @param data 待检测数据
     * @return true表示符合UBJSON特征
     */
    public static boolean isUbjsonFormat(byte[] data) {
        if (data == null || data.length < 3) {
            return false;
        }

        byte first = data[0];
        byte second = data[1];

        // 首字节必须是 { 或 [
        if (first != 0x7B && first != 0x5B) {
            return false;
        }

        // 排除普通 JSON：如果是 {" 或 [" 开头，则是纯文本 JSON
        if (second == 0x22) { // "
            return false;
        }

        // 排除普通 JSON：如果是 {{ 或 {[ 开头也不是 UBJSON
        if (second == 0x7B || second == 0x5B) {
            return false;
        }

        // 排除纯空格/换行等 JSON 格式化字符
        if (second == 0x20 || second == 0x0A || second == 0x0D || second == 0x09) {
            return false;
        }

        // UBJSON 对象的第二字节通常是类型标记：i, U, I, l, L, S 等
        // 或者是 } ] 表示空对象/数组
        byte[] validSecondBytes = {
            0x69, // i (int8)
            0x55, // U (uint8)
            0x49, // I (int16)
            0x6C, // l (int32)
            0x4C, // L (int64)
            0x53, // S (string)
            0x7D, // } (empty object)
            0x5D, // ] (empty array)
            0x54, // T (true)
            0x46, // F (false)
            0x5A, // Z (null)
            0x43, // C (char)
            0x64, // d (float32)
            0x44, // D (float64)
            0x4E, // N (noop)
            0x7B, // { (nested object) - 但上面已排除连续的
            0x5B  // [ (nested array)
        };

        for (byte valid : validSecondBytes) {
            if (second == valid) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检测BSON格式特征
     * <p>
     * BSON特征：前4字节是小端序文档长度 + 尾部0x00
     * </p>
     *
     * @param data 待检测数据
     * @return true表示符合BSON特征
     */
    public static boolean isBsonFormat(byte[] data) {
        if (data == null || data.length < 5) {
            return false;
        }

        try {
            // 读取前4字节作为小端序int
            int declaredLength = (data[0] & 0xFF) |
                                ((data[1] & 0xFF) << 8) |
                                ((data[2] & 0xFF) << 16) |
                                ((data[3] & 0xFF) << 24);

            // 验证：声明长度等于实际长度 && 尾部是0x00
            return declaredLength == data.length &&
                   data[data.length - 1] == 0x00;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检测CBOR格式特征
     * <p>
     * CBOR首字节范围：
     * - Map: 0xA0-0xBF (fixmap, indefinite)
     * - Array: 0x80-0x9F (fixarray, indefinite) - 与 MessagePack 重叠！
     * </p>
     * <p>
     * ⚠️ 为避免与 MessagePack 冲突，CBOR 检测仅使用 0xA0-0xBF 范围
     * 0x80-0x9F 范围优先识别为 MessagePack
     * </p>
     *
     * @param data 待检测数据
     * @return true表示符合CBOR特征
     */
    public static boolean isCborFormat(byte[] data) {
        if (data == null || data.length < 10) {
            return false;
        }

        int firstByte = data[0] & 0xFF;

        // CBOR Map: 0xA0-0xBF（优先检测，不与 MessagePack 冲突）
        if (firstByte >= 0xA0 && firstByte <= 0xBF) {
            return true;
        }

        // 0x80-0x9F 范围与 MessagePack 重叠
        // 需要更深入的启发式检测来区分
        if (firstByte >= 0x80 && firstByte <= 0x9F) {
            // 检查后续字节是否符合 CBOR 类型标记
            // CBOR 的类型标记更规范：高3位是 major type
            return isCborArrayHeuristic(data);
        }

        return false;
    }

    /**
     * CBOR Array 启发式检测
     * <p>
     * 区分 CBOR Array 和 MessagePack Array
     * </p>
     */
    private static boolean isCborArrayHeuristic(byte[] data) {
        if (data.length < 3) {
            return false;
        }

        int firstByte = data[0] & 0xFF;
        int secondByte = data[1] & 0xFF;

        // CBOR array 元素类型应该是有效的 CBOR major type
        // Major type 在高3位: 0-7
        int majorType = (secondByte >> 5) & 0x07;

        // CBOR 常见的 major type:
        // 0: unsigned integer
        // 1: negative integer
        // 2: byte string
        // 3: text string
        // 4: array
        // 5: map
        // 6: tag
        // 7: simple/float

        // 如果第二字节看起来像有效的 CBOR 类型，则认为是 CBOR
        // MessagePack 的第二字节通常是数据值而非类型标记
        return majorType >= 0 && majorType <= 7;
    }

    /**
     * 检测MessagePack格式特征
     * <p>
     * MessagePack首字节范围：
     * - Map: 0x80-0x8F (fixmap), 0xDE (map16), 0xDF (map32)
     * - Array: 0x90-0x9F (fixarray), 0xDC (array16), 0xDD (array32)
     * </p>
     * <p>
     * ⚠️ 最小长度要求：至少10字节，避免误判加密中间结果
     * </p>
     *
     * @param data 待检测数据
     * @return true表示符合MessagePack特征
     */
    public static boolean isMessagePackFormat(byte[] data) {
        if (data == null || data.length < 10) {
            return false;
        }

        int firstByte = data[0] & 0xFF;

        // Map: 0x80-0x8F, 0xDE, 0xDF
        // Array: 0x90-0x9F, 0xDC, 0xDD
        return (firstByte >= 0x80 && firstByte <= 0x9F) ||
               firstByte == 0xDE || firstByte == 0xDF ||
               firstByte == 0xDC || firstByte == 0xDD;
    }

    /**
     * 检测Protobuf格式特征（启发式）
     * <p>
     * Protobuf使用Varint编码，检测首字节是否为有效的field tag
     * Field tag = (field_number << 3) | wire_type
     * Wire type: 0=varint, 1=64bit, 2=length-delimited, 5=32bit
     * </p>
     * <p>
     * ⚠️ 最小长度要求：至少20字节，启发式检测需要更严格的验证
     * </p>
     *
     * @param data 待检测数据
     * @return true表示可能是Protobuf
     */
    public static boolean isProtobufFormat(byte[] data) {
        if (data == null || data.length < 20) {
            return false;
        }

        int firstByte = data[0] & 0xFF;

        // 检测首字节是否为有效的field tag
        // Wire type (低3位): 0, 1, 2, 5
        int wireType = firstByte & 0x07;
        return wireType == 0 || wireType == 1 || wireType == 2 || wireType == 5;
    }

    /**
     * 检测Kryo格式特征（实验性/已禁用自动检测）
     * <p>
     * Kryo 是 Java 专用序列化框架，无固定魔术头
     * 由于其字节特征与其他格式高度重叠，自动检测误判率极高
     * </p>
     * <p>
     * ⚠️ 当前策略：禁用自动检测，仅通过 MIME 类型显式识别
     * 如需检测 Kryo 格式，请确保 Content-Type 为 application/x-kryo
     * </p>
     *
     * @param data 待检测数据
     * @return false（禁用自动检测）
     */
    public static boolean isKryoFormat(byte[] data) {
        // Kryo 无固定魔术头，自动检测误判率极高
        // 禁用自动检测，仅通过 MIME 类型显式识别
        return false;
    }

    /**
     * 检测FST格式特征
     * <p>
     * FST首字节是版本号
     * </p>
     *
     * @param data 待检测数据
     * @return true表示可能是FST
     */
    public static boolean isFstFormat(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }

        int firstByte = data[0] & 0xFF;

        // FST版本号通常是254或255
        return firstByte == 254 || firstByte == 255;
    }

    // ========== 私有构造函数（工具类） ==========
    private BinaryFormatConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}

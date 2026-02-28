package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 二进制反序列化器工厂类
 * <p>
 * 统一管理所有反序列化器，提供自动检测和格式指定功能
 * </p>
 * <p>
 * 检测策略（按优先级）：
 * 1. 精确魔术头：Smile(:)\n)、Avro(Obj\x01)
 * 2. 类型标记检测：Hessian(M/H/O)、FST(java类名特征)、Kryo(java类名特征)
 * 3. 格式验证：BSON(长度+0x00)、MessagePack(0x80-0x9F)、CBOR(0xA0-0xBF)、UBJSON({+类型)
 * 4. 兜底尝试解析：无法通过特征识别时，直接尝试反序列化
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class BinaryDeserializerFactory {

    /**
     * 主反序列化器列表（有特征检测）
     * 按检测准确度排序：精确魔术头 → 类型标记 → 格式验证
     */
    private static final List<BinaryDeserializer> PRIMARY_DESERIALIZERS = Arrays.asList(
        // 最高优先级：Snappy + MessagePack 组合格式（"sNaPpY" 魔术头）
        new SnappyMsgPackDeserializer(),

        // 高优先级：精确魔术头（3字节以上）
        new SmileDeserializer(),       // 3A 29 0A (:)\n)
        new  AvroDeserializer(),        // 4F 62 6A 01 (Obj\x01)

        // 中高优先级：类型标记 + 类名特征
        new HessianDeserializer(),     // M(Map)/H(untyped)/O(Object) + 类名
        new KryoDeserializer(),        // 0x01 + java类名特征（放在 FST 之前）
        new FstDeserializer(),         // 0x00 + java类名特征（FST 2.x 不再检测 0x01）

        // 中等优先级：格式验证
        new BsonDeserializer(),        // 前4字节长度 + 尾部0x00
        new MessagePackDeserializer(), // 首字节 0x80-0x9F
        new CborDeserializer(),        // 首字节 0xA0-0xBF
        new UbjsonDeserializer()       // 首字节 0x7B + 类型标记
    );

    /**
     * 兜底反序列化器列表（无特征检测，直接尝试解析）
     * 当主检测器都无法识别时，按顺序尝试解析
     */
    private static final List<BinaryDeserializer> FALLBACK_DESERIALIZERS = Arrays.asList(
        new FstDeserializer(),         // FST 兜底（Java序列化框架）
        new KryoDeserializer()        // Kryo 兜底（Java序列化框架）
    );

    /**
     * 所有反序列化器（用于格式列表查询，去重）
     */
    private static final List<BinaryDeserializer> ALL_DESERIALIZERS;

    static {
        ALL_DESERIALIZERS = new ArrayList<>();
        ALL_DESERIALIZERS.addAll(PRIMARY_DESERIALIZERS);
        // 不添加 FALLBACK 中的重复项
    }

    /**
     * 自动检测并反序列化二进制数据
     * <p>
     * 检测策略：
     * 1. 遍历主反序列化器，使用第一个能够识别该数据的反序列化器
     * 2. 如果主反序列化器都无法识别，尝试兜底反序列化器（直接解析）
     * 3. 验证反序列化结果有效性（非空、非错误信息）
     * </p>
     *
     * @param data 待反序列化的二进制数据
     * @return 反序列化后的JSON字符串，如果无法识别则返回null
     */
    public static String autoDeserialize(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        // 第一阶段：尝试有特征检测的反序列化器
        for (BinaryDeserializer deserializer : PRIMARY_DESERIALIZERS) {
            try {
                if (deserializer.canDeserialize(data)) {
                    String result = deserializer.deserialize(data);
                    if (isValidResult(result)) {
                        if(debugMode){
                            System.out.println("反序列化format："+deserializer.getFormatName());
                            System.out.println("序列化结果"+result);
                        }
                        return result;
                    }
                }
            } catch (Exception e) {
                // 检测失败，继续尝试下一个
            }
        }

        // 第二阶段：尝试兜底反序列化器（跳过 canDeserialize，直接解析）
        for (BinaryDeserializer deserializer : FALLBACK_DESERIALIZERS) {
            try {
                String result = deserializer.deserialize(data);
                if (isValidResult(result)) {
                    if(debugMode){
                        System.out.println("反序列化format："+deserializer.getFormatName());
                        System.out.println("序列化结果"+result);
                    }
                    return result;
                }
            } catch (Exception e) {
                // 解析失败，继续尝试下一个
            }
        }

        return null; // 未识别的格式
    }

    /**
     * 验证反序列化结果是否有效
     * 排除空结果和错误信息
     */
    private static boolean isValidResult(String result) {
        if (result == null || result.isEmpty()) {
            return false;
        }
        // 排除错误信息（以 "unable to decode" 开头的是错误提示）
        if (result.startsWith("unable to decode")) {
            return false;
        }
        // 排除 "null" 字符串（反序列化得到 null 对象）
        if ("null".equals(result.trim())) {
            return false;
        }
        return true;
    }

    /**
     * 根据格式名称获取反序列化器
     *
     * @param format 格式名称（如"BSON"、"MessagePack"等，不区分大小写）
     * @return 对应的反序列化器，如果不支持该格式则返回null
     */
    public static BinaryDeserializer getDeserializer(String format) {
        if (format == null || format.trim().isEmpty()) {
            return null;
        }

        String normalizedFormat = format.trim().toUpperCase();

        for (BinaryDeserializer deserializer : ALL_DESERIALIZERS) {
            if (deserializer.getFormatName().equalsIgnoreCase(normalizedFormat)) {
                return deserializer;
            }
        }

        return null;
    }

    /**
     * 获取所有支持的格式列表
     *
     * @return 格式名称列表
     */
    public static List<String> getSupportedFormats() {
        List<String> formats = new ArrayList<>();
        for (BinaryDeserializer deserializer : ALL_DESERIALIZERS) {
            formats.add(deserializer.getFormatName());
        }
        return formats;
    }

    /**
     * 检测数据格式（不执行反序列化）
     * <p>
     * 注意：此方法仅通过特征检测，不尝试兜底解析
     * </p>
     *
     * @param data 待检测数据
     * @return 检测到的格式名称，如果无法识别则返回null
     */
    public static String detectFormat(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        for (BinaryDeserializer deserializer : PRIMARY_DESERIALIZERS) {
            try {
                if (deserializer.canDeserialize(data)) {
                    return deserializer.getFormatName();
                }
            } catch (Exception e) {
                // 检测失败，继续尝试下一个
            }
        }

        return null;
    }

    /**
     * 检测数据格式（包含兜底尝试）
     * <p>
     * 如果无法通过特征识别，会尝试用兜底反序列化器解析来判断格式
     * </p>
     *
     * @param data 待检测数据
     * @return 检测到的格式名称，如果无法识别则返回null
     */
    public static String detectFormatWithFallback(byte[] data) {
        // 先尝试精确检测
        String format = detectFormat(data);
        if (format != null) {
            return format;
        }

        // 尝试兜底解析来判断格式
        for (BinaryDeserializer deserializer : FALLBACK_DESERIALIZERS) {
            try {
                String result = deserializer.deserialize(data);
                if (isValidResult(result)) {
                    return deserializer.getFormatName();
                }
            } catch (Exception e) {
                // 解析失败，继续尝试下一个
            }
        }

        return null;
    }

    // 私有构造函数（工具类）
    private BinaryDeserializerFactory() {
        throw new UnsupportedOperationException("Utility class");
    }
}

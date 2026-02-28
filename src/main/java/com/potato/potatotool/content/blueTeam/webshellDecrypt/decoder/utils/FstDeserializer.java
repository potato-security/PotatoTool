package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import org.nustaq.serialization.FSTConfiguration;

import static com.potato.potatotool.content.classObj.BinaryFormatConstants.*;

/**
 * FST格式反序列化器
 * <p>
 * FST (Fast Serialization) 是Java专用快速序列化框架
 * </p>
 * <p>
 * FST 格式特征：
 * - FST 1.x: 首字节 0xFE 或 0xFF（版本标记）
 * - FST 2.x: 首字节 0x00 或 0x01（配置标记），第二字节为版本/配置
 * - 后续通常是类名长度 + "java." 开头的类名
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class FstDeserializer extends AbstractBinaryDeserializer {

    private static final FSTConfiguration FST = FSTConfiguration.createDefaultConfiguration();

    @Override
    public String deserialize(byte[] data) {
        try {
            Object obj = FST.asObject(data);

            if (obj == null) {
                return "null";
            }

            return toJsonString(obj);

        } catch (Exception e) {
            return formatError("FST", e, data);
        }
    }

    @Override
    public boolean canDeserialize(byte[] data) {
        if (data == null || data.length < 10) {
            return false;
        }

        int first = data[0] & 0xFF;
        int second = data[1] & 0xFF;
        int third = data.length > 2 ? (data[2] & 0xFF) : 0;

        // 1. FST 1.x 格式：首字节 0xFE 或 0xFF（版本标记）
        if (first == 0xFE || first == 0xFF) {
            return true;
        }

        // 2. FST 2.x 格式：首字节 0x00（默认配置）
        // 注意：0x01 被 Kryo 使用，不在此处检测
        // FST 2.x 默认配置通常是：0x00 + 类型标记 + 类信息
        if (first == 0x00) {
            // 第二字节检查：FST 类型标记通常在 0x00-0x7F 范围
            // 第三字节通常是类名长度或类ID
            if (second <= 0x7F && data.length > 5) {
                // 检查后续是否有 Java 类名特征
                // FST 类名通常出现在第4-6字节之后
                return containsJavaClassName(data, 3, Math.min(40, data.length));
            }
        }

        // 3. FST 紧凑格式：特定类型标记
        // FST 使用 0x02-0x1F 作为内置类型标记
        // 但要避免与 Kryo 的 0x01 冲突
        if (first >= 0x02 && first <= 0x1F && data.length > 10) {
            // 更严格的检查：类名应该出现在特定位置
            return containsJavaClassName(data, 2, Math.min(30, data.length));
        }

        return false;
    }

    /**
     * 检查数据中是否包含 Java 类名特征
     */
    private boolean containsJavaClassName(byte[] data, int start, int end) {
        // 查找 "java" (0x6A 0x61 0x76 0x61) 或 "org" (0x6F 0x72 0x67) 特征
        for (int i = start; i < end - 3; i++) {
            // "java"
            if (data[i] == 0x6A && data[i + 1] == 0x61 &&
                data[i + 2] == 0x76 && data[i + 3] == 0x61) {
                return true;
            }
            // "org."
            if (data[i] == 0x6F && data[i + 1] == 0x72 && data[i + 2] == 0x67) {
                return true;
            }
            // "com."
            if (data[i] == 0x63 && data[i + 1] == 0x6F && data[i + 2] == 0x6D) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getFormatName() {
        return FORMAT_FST;
    }
}

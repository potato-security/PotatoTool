package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

/**
 * 二进制反序列化器基础抽象类
 * <p>
 * 提供通用的错误处理、Hex转储等功能
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public abstract class AbstractBinaryDeserializer implements BinaryDeserializer {

    protected static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    /**
     * 记录错误信息并返回null
     * <p>
     * 错误信息仅在debugMode下输出到控制台，不作为返回值
     * </p>
     *
     * @param format 格式名称
     * @param e 异常
     * @param data 原始数据
     * @return null（表示解析失败）
     */
    protected String formatError(String format, Exception e, byte[] data) {
        // 仅在debugMode下输出错误信息
        if (Boolean.getBoolean("debugMode") || System.getProperty("debugMode") != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("⚠️ ").append(format).append("解析失败: ");
            sb.append(e.getMessage()).append("\n");

            // Hex转储（前256字节）
            sb.append("--- Hex Dump (前256字节) ---\n");
            sb.append(hexDump(data, 256));

            // 数据统计
            sb.append("\n--- 数据统计 ---\n");
            sb.append("总长度: ").append(data.length).append(" 字节\n");
            sb.append("可打印字符比例: ").append(calculatePrintableRatio(data)).append("%\n");

            System.err.println(sb.toString());
        }

        // 返回null表示解析失败
        return null;
    }

    /**
     * Hex转储
     *
     * @param data 数据
     * @param limit 最大字节数
     * @return Hex字符串
     */
    protected String hexDump(byte[] data, int limit) {
        StringBuilder sb = new StringBuilder();
        int len = Math.min(data.length, limit);

        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", data[i]));
            if ((i + 1) % 16 == 0) {
                sb.append("\n");
            }
        }

        if (data.length > limit) {
            sb.append("\n... (剩余 ").append(data.length - limit).append(" 字节)");
        }

        return sb.toString();
    }

    /**
     * 计算可打印字符比例
     *
     * @param data 数据
     * @return 百分比
     */
    protected int calculatePrintableRatio(byte[] data) {
        if (data == null || data.length == 0) {
            return 0;
        }

        int printable = 0;
        for (byte b : data) {
            int c = b & 0xFF;
            if (c >= 32 && c < 127) {
                printable++;
            }
        }

        return (int) ((printable * 100.0) / data.length);
    }

    /**
     * 检测魔术头
     *
     * @param data 数据
     * @param offset 偏移量
     * @param magic 魔术头
     * @return true表示匹配
     */
    protected boolean hasMagicHeader(byte[] data, int offset, byte[] magic) {
        if (data == null || magic == null) {
            return false;
        }

        if (data.length < offset + magic.length) {
            return false;
        }

        for (int i = 0; i < magic.length; i++) {
            if (data[offset + i] != magic[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * 将对象转换为格式化JSON字符串
     *
     * @param obj 对象
     * @return JSON字符串
     */
    protected String toJsonString(Object obj) {
        if (obj == null || obj instanceof Map && ((Map<?, ?>) obj).isEmpty()) {
            return null;
        }

        try {
            return GSON.toJson(obj);
        } catch (Exception e) {
            return null;
        }
    }
}

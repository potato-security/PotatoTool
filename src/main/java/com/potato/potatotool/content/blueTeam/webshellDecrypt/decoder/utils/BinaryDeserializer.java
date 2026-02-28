package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

/**
 * 二进制反序列化器接口
 * <p>
 * 定义统一的二进制格式反序列化接口，支持多种序列化格式的自动检测和解析
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public interface BinaryDeserializer {

    /**
     * 反序列化二进制数据为可读的JSON字符串
     *
     * @param data 待反序列化的二进制数据
     * @return JSON格式的字符串，如果反序列化失败则返回错误信息（带⚠️前缀）
     */
    String deserialize(byte[] data);

    /**
     * 检测数据是否可以被该反序列化器处理
     * <p>
     * 通过魔术头、格式特征等方式判断数据格式
     * </p>
     *
     * @param data 待检测的二进制数据
     * @return true表示可以反序列化，false表示不支持该格式
     */
    boolean canDeserialize(byte[] data);

    /**
     * 获取反序列化器支持的格式名称
     *
     * @return 格式名称（如"BSON"、"MessagePack"等）
     */
    String getFormatName();
}

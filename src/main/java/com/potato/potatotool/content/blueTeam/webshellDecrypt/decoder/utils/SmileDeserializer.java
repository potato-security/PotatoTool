package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import static com.potato.potatotool.content.classObj.BinaryFormatConstants.*;

/**
 * Smile格式反序列化器
 * <p>
 * 支持Jackson Smile二进制JSON格式的反序列化
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class SmileDeserializer extends AbstractBinaryDeserializer {

    private static final ObjectMapper SMILE_MAPPER = new ObjectMapper(new SmileFactory());
    private static final byte[] SMILE_MAGIC = new byte[]{0x3A, 0x29, 0x0A}; // :)\n

    @Override
    public String deserialize(byte[] data) {
        try {
            // 反序列化为Java对象
            Object obj = SMILE_MAPPER.readValue(data, Object.class);

            // 转换为格式化JSON
            return toJsonString(obj);

        } catch (Exception e) {
            return formatError("Smile", e, data);
        }
    }

    @Override
    public boolean canDeserialize(byte[] data) {
        return hasMagicHeader(data, 0, SMILE_MAGIC);
    }

    @Override
    public String getFormatName() {
        return FORMAT_SMILE;
    }
}

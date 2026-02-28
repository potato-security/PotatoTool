package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import static com.potato.potatotool.content.classObj.BinaryFormatConstants.*;

/**
 * CBOR格式反序列化器
 * <p>
 * 支持CBOR（RFC 8949）二进制格式的反序列化
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class CborDeserializer extends AbstractBinaryDeserializer {

    private static final ObjectMapper CBOR_MAPPER = new ObjectMapper(new CBORFactory());

    @Override
    public String deserialize(byte[] data) {
        try {
            // 反序列化为Java对象
            Object obj = CBOR_MAPPER.readValue(data, Object.class);

            // 转换为格式化JSON
            return toJsonString(obj);

        } catch (Exception e) {
            return formatError("CBOR", e, data);
        }
    }

    @Override
    public boolean canDeserialize(byte[] data) {
        return isCborFormat(data);
    }

    @Override
    public String getFormatName() {
        return FORMAT_CBOR;
    }
}

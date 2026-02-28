package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import static com.potato.potatotool.content.classObj.BinaryFormatConstants.*;

/**
 * MessagePack格式反序列化器
 * <p>
 * 支持MessagePack二进制格式的反序列化
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class MessagePackDeserializer extends AbstractBinaryDeserializer {

    private static final ObjectMapper MSGPACK_MAPPER = new ObjectMapper(new MessagePackFactory());

    @Override
    public String deserialize(byte[] data) {
        try {
            // 反序列化为Java对象
            Object obj = MSGPACK_MAPPER.readValue(data, Object.class);
            // 转换为格式化JSON
            return toJsonString(obj);

        } catch (Exception e) {
            return formatError("MessagePack", e, data);
        }
    }

    @Override
    public boolean canDeserialize(byte[] data) {
        return isMessagePackFormat(data);
    }

    @Override
    public String getFormatName() {
        return FORMAT_MESSAGEPACK;
    }
}

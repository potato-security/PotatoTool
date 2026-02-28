package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.io.BasicOutputBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.potato.potatotool.content.classObj.BinaryFormatConstants.*;

/**
 * BSON格式反序列化器
 * <p>
 * 支持MongoDB BSON格式的反序列化
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class BsonDeserializer extends AbstractBinaryDeserializer {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Override
    public String deserialize(byte[] data) {
        try {
            // 创建BSON二进制读取器
            ByteBuffer buffer = ByteBuffer.wrap(data);
            BsonBinaryReader reader = new BsonBinaryReader(buffer);

            // 解码为BSON文档
            BsonDocument doc = new BsonDocumentCodec().decode(
                reader,
                DecoderContext.builder().build()
            );

            // 转换为JSON字符串
            String jsonString = doc.toJson();

            // 格式化输出
            Object obj = JSON_MAPPER.readValue(jsonString, Object.class);
            return JSON_MAPPER.writerWithDefaultPrettyPrinter()
                             .writeValueAsString(obj);

        } catch (Exception e) {
            return formatError("BSON", e, data);
        }
    }

    @Override
    public boolean canDeserialize(byte[] data) {
        return isBsonFormat(data);
    }

    @Override
    public String getFormatName() {
        return FORMAT_BSON;
    }
}

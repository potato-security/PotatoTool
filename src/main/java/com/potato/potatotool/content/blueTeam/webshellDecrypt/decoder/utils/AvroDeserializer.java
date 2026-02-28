package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;

import static com.potato.potatotool.content.classObj.BinaryFormatConstants.*;

/**
 * Avro格式反序列化器（模糊解析）
 * <p>
 * Apache Avro格式的模糊解析
 * 支持Avro容器格式（带Schema），裸数据格式无法解析
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class AvroDeserializer extends AbstractBinaryDeserializer {

    private static final byte[] AVRO_MAGIC = new byte[]{0x4F, 0x62, 0x6A, 0x01}; // Obj\x01

    @Override
    public String deserialize(byte[] data) {
        try {
            // Avro容器格式解析
            SeekableByteArrayInput input = new SeekableByteArrayInput(data);
            GenericDatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
            DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(input, datumReader);

            StringBuilder result = new StringBuilder();
            result.append("[\n");

            int count = 0;
            while (dataFileReader.hasNext()) {
                GenericRecord record = dataFileReader.next();
                if (count > 0) {
                    result.append(",\n");
                }
                result.append("  ").append(record.toString());
                count++;

                // 限制输出数量
                if (count >= 100) {
                    result.append(",\n  ... (剩余 ").append(dataFileReader.getBlockCount() - 100).append(" 条记录)");
                    break;
                }
            }

            result.append("\n]");
            dataFileReader.close();

            return result.toString();

        } catch (Exception e) {
            // 解析失败统一返回null，不返回错误信息字符串
            return formatError("Avro", e, data);
        }
    }

    @Override
    public boolean canDeserialize(byte[] data) {
        // 仅支持Avro容器格式（带魔术头）
        return hasMagicHeader(data, 0, AVRO_MAGIC);
    }

    @Override
    public String getFormatName() {
        return FORMAT_AVRO;
    }
}

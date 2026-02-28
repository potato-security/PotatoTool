package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import static com.potato.potatotool.content.classObj.BinaryFormatConstants.*;

/**
 * Kryo格式反序列化器
 * <p>
 * Kryo是Java专用高性能序列化框架，无固定魔术头
 * </p>
 * <p>
 * Kryo 格式特征：
 * - 首字节通常是类ID（0x00-0x09 为内置类型，0x01 常见于自定义类）
 * - 后续是变长编码的类名（如果使用 writeClassAndObject）
 * - 类名通常以 "java." "org." "com." 开头
 * </p>
 * <p>
 * 注意：Kryo 实例不是线程安全的，每次反序列化创建新实例
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class KryoDeserializer extends AbstractBinaryDeserializer {

    /**
     * 创建配置好的 Kryo 实例
     * 每次调用创建新实例以保证线程安全
     */
    private Kryo createKryo() {
        Kryo kryo = new Kryo();
        // 允许未注册的类
        kryo.setRegistrationRequired(false);
        // 启用引用追踪
        kryo.setReferences(true);
        // 使用 Objenesis 策略以支持没有无参构造函数的类
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        return kryo;
    }

    @Override
    public String deserialize(byte[] data) {
        // 尝试多种 Kryo 配置
        // 配置1：默认配置（references=true）
        try {
            Kryo kryo = createKryo();
            Input input = new Input(data);
            Object obj = kryo.readClassAndObject(input);
            input.close();

            if (obj != null) {
                return toJsonString(obj);
            }
        } catch (Exception ignored) {
            // 配置1失败，尝试配置2
        }

        // 配置2：禁用引用追踪（references=false）
        try {
            Kryo kryo = createKryoNoReferences();
            Input input = new Input(data);
            Object obj = kryo.readClassAndObject(input);
            input.close();

            if (obj != null) {
                return toJsonString(obj);
            }
        } catch (Exception ignored) {
            // 配置2也失败
        }

        // 配置3：简单配置
        try {
            Kryo kryo = createSimpleKryo();
            Input input = new Input(data);
            Object obj = kryo.readClassAndObject(input);
            input.close();

            if (obj != null) {
                return toJsonString(obj);
            }
        } catch (Exception e) {
            return formatError("Kryo", e, data);
        }

        return formatError("Kryo", new Exception("All Kryo configurations failed"), data);
    }

    /**
     * 创建禁用引用追踪的 Kryo 实例
     */
    private Kryo createKryoNoReferences() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setReferences(false);  // 禁用引用追踪
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        return kryo;
    }

    /**
     * 创建简单 Kryo 实例（最小配置）
     */
    private Kryo createSimpleKryo() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        // 不设置 references 和 InstantiatorStrategy，使用默认值
        return kryo;
    }

    @Override
    public boolean canDeserialize(byte[] data) {
        if (data == null || data.length < 10) {
            return false;
        }

        int first = data[0] & 0xFF;

        // Kryo 格式特征检测：
        // 1. 首字节通常是 0x01（表示后续是完整类名）
        // 2. 首字节 0x00 表示 null
        // 3. 首字节 0x02-0x09 是内置类型引用

        // 如果首字节是 0x01，检查后续是否有 Java 类名
        if (first == 0x01 && data.length > 5) {
            // 第二字节开始是类名（变长编码）
            // 检查是否包含 "java" 等类名特征
            return containsJavaClassName(data, 1, Math.min(40, data.length));
        }

        // 如果首字节在 0x02-0x09 范围，可能是引用已注册的类
        // 这种情况难以准确检测，返回 false 让兜底机制处理
        return false;
    }

    /**
     * 检查数据中是否包含 Java 类名特征
     * Kryo 使用 UTF-8 编码存储类名
     */
    private boolean containsJavaClassName(byte[] data, int start, int end) {
        // 查找常见的 Java 包名前缀
        for (int i = start; i < end - 3; i++) {
            // "java" (0x6A 0x61 0x76 0x61)
            if (data[i] == 0x6A && data[i + 1] == 0x61 &&
                data[i + 2] == 0x76 && data[i + 3] == 0x61) {
                return true;
            }
            // "org" (0x6F 0x72 0x67)
            if (i < end - 2 && data[i] == 0x6F && data[i + 1] == 0x72 && data[i + 2] == 0x67) {
                return true;
            }
            // "com" (0x63 0x6F 0x6D)
            if (i < end - 2 && data[i] == 0x63 && data[i + 1] == 0x6F && data[i + 2] == 0x6D) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getFormatName() {
        return FORMAT_KRYO;
    }
}

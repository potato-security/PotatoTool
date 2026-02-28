package com.potato.potatotool.content.blueTeam.webshellDecrypt.decoder.utils;

import com.caucho.hessian.io.Hessian2Input;

import java.io.ByteArrayInputStream;

import static com.potato.potatotool.content.classObj.BinaryFormatConstants.*;

/**
 * Hessian格式反序列化器
 * <p>
 * 支持Caucho Hessian 1/2二进制协议的反序列化
 * </p>
 * <p>
 * Hessian 格式特征：
 * - RPC调用帧：以 H\x01 或 H\x02 开头（不常见于WebShell流量）
 * - 直接对象序列化：以类型标记开头，如 M(0x4D)=Map, L(0x4C)=List, S(0x53)=String 等
 * </p>
 *
 * @author Potato
 * @date 2025/12/29
 */
public class HessianDeserializer extends AbstractBinaryDeserializer {

    // Hessian RPC 魔术头（不常用）
    private static final byte[] HESSIAN1_MAGIC = new byte[]{0x48, 0x01}; // H + v1
    private static final byte[] HESSIAN2_MAGIC = new byte[]{0x48, 0x02}; // H + v2

    // Hessian 类型标记（直接对象序列化常用）
    private static final byte TYPE_MAP = 0x4D;          // M - Map/Object
    private static final byte TYPE_MAP_UNTYPED = 0x48;  // H - Untyped Map (Hessian2)
    private static final byte TYPE_LIST = 0x56;         // V - List (variable length)
    private static final byte TYPE_STRING = 0x53;       // S - String (length > 31)
    private static final byte TYPE_BINARY = 0x42;       // B - Binary
    private static final byte TYPE_OBJECT = 0x4F;       // O - Object definition
    private static final byte TYPE_CLASS_DEF = 0x43;    // C - Class definition

    @Override
    public String deserialize(byte[] data) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            Hessian2Input input = new Hessian2Input(bis);

            // 读取反序列化对象
            Object obj = input.readObject();
            input.close();

            // 验证结果有效性（防止假阳性）
            if (!isValidResult(obj, data.length)) {
                return null;
            }

            // 转换为JSON
            return toJsonString(obj);

        } catch (Exception e) {
            return formatError("Hessian", e, data);
        }
    }

    /**
     * 验证反序列化结果是否有效
     * 防止假阳性：Hessian 可能将任意字节误解为简单类型
     * <p>
     * Hessian2 单字节整数范围 0x80-0xBF 会被解释为 -16 到 47
     * 例如：0xA9 会被解释为整数 25，这是典型的假阳性
     * </p>
     *
     * @param obj 反序列化对象
     * @param totalBytes 总字节数
     * @return true 表示结果有效
     */
    private boolean isValidResult(Object obj, int totalBytes) {
        if (obj == null) {
            return false;
        }

        // 规则1：简单数字类型几乎肯定是假阳性
        // WebShell 流量不会只传输一个简单数字
        if (obj instanceof Number) {
            return false;
        }

        // 规则2：布尔类型也是假阳性
        if (obj instanceof Boolean) {
            return false;
        }

        // 规则3：如果是字符串，检查是否合理
        if (obj instanceof String) {
            String str = (String) obj;
            // 空字符串或太短的字符串（相对于原始数据）可能是假阳性
            if (str.isEmpty()) {
                return false;
            }
            // 如果字符串长度远小于原始数据长度，可能是假阳性
            // 真正的 Hessian 字符串会消费大部分数据
            if (str.length() < 3 && totalBytes > 10) {
                return false;
            }
        }

        // 规则4：只接受 Map、List、数组等复杂类型，或足够长的字符串
        return true;
    }

    @Override
    public boolean canDeserialize(byte[] data) {
        if (data == null || data.length < 5) {
            return false;
        }

        byte first = data[0];

        // 1. 检查 RPC 魔术头
        if (hasMagicHeader(data, 0, HESSIAN2_MAGIC) ||
            hasMagicHeader(data, 0, HESSIAN1_MAGIC)) {
            return true;
        }

        // 2. 检查直接对象序列化的类型标记
        // Map类型：M + 类型长度 + 类名
        if (first == TYPE_MAP && data.length > 3) {
            // 检查是否跟随有效的类型长度（0x00-0x1F 表示短字符串，或后续字节可读）
            int typeLen = data[1] & 0xFF;
            if (typeLen < 128 && data.length > 2 + typeLen) {
                // 检查类名是否以 "java." 或小写字母开头
                if (typeLen > 0) {
                    char firstChar = (char) (data[2] & 0xFF);
                    return Character.isLetter(firstChar);
                }
                return true; // 匿名Map
            }
        }

        // 3. Hessian2 紧凑格式：首字节范围检测
        // 0x00-0x1F: 短字符串
        // 0x20-0x2F: 二进制数据
        // 0x30-0x33: 长整型
        // 0x48: untyped map
        // 0x4D: typed map
        // 0x4F: object
        // 0x55-0x58: list
        if (first == TYPE_MAP_UNTYPED || first == TYPE_OBJECT ||
            first == TYPE_LIST || first == TYPE_CLASS_DEF) {
            return true;
        }

        // 4. Hessian2 紧凑Map：0x48 后跟字段
        // 紧凑格式的 untyped map
        if ((first & 0xFF) >= 0x55 && (first & 0xFF) <= 0x5F) {
            return true; // 紧凑 list
        }

        return false;
    }

    @Override
    public String getFormatName() {
        return FORMAT_HESSIAN;
    }
}

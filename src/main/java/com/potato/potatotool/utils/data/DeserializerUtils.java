package com.potato.potatotool.utils.data;

import com.google.gson.JsonObject;
import com.potato.potatotool.content.classObj.ConfigConstants;
import com.potato.potatotool.utils.core.Constants;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.getResourceString;
import static com.potato.potatotool.utils.decompile.DecompileUtils.Decompile;
import static com.potato.potatotool.utils.data.StrUtils.*;

/**
 * @author Potato
 * @date 2023/4/25 09:06
 */

public class DeserializerUtils{
    public static byte[] serializeParsing(byte[] decryptedTextBytes){
        // 创建 GraalVM 上下文
        try (Context context = Context.create()) {

            // 读取第一个 JS 文件的内容
            String jsCode = getResourceString("JavaStream");

            // 执行 JavaScript 代码
            context.eval("js", jsCode);

            // 获取 JavaScript 中的构造函数和方法
            Value javaStreamClass = context.getBindings("js").getMember("JavaStream");

            // 实例化 JavaStream 类并传入参数
            Value uint8Array = context.getBindings("js").getMember("Uint8Array");
            Value arrayBuffer = context.getBindings("js").getMember("ArrayBuffer").newInstance(decryptedTextBytes.length);
            Value uint8ArrayObj = uint8Array.newInstance(arrayBuffer);

            // 将 Java 的 byte[] 数组内容设置到 Uint8Array 对象中
            for (int i = 0; i < decryptedTextBytes.length; i++) {
                uint8ArrayObj.setArrayElement(i, decryptedTextBytes[i]);
            }
            Value javaStream = javaStreamClass.newInstance(uint8ArrayObj);

            // 调用 run 方法
            javaStream.invokeMember("run");

            // 调用 JavaStream 的 contents 数组中第一个元素的 getValue 方法
            System.out.println(javaStream.getMember("contents"));
            Value valueData = javaStream.getMember("contents").getArrayElement(0).invokeMember("getValue");
            JavaStreamArrayTraverser javaStreamObj= new JavaStreamArrayTraverser();
            javaStreamObj.traverseValue(valueData);

            return javaStreamObj.getByteArray();

        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    public static byte[] serializeCheck(byte[] decryptedTextBytes) throws Exception{
        byte[] resultData = null;

        // 反序列化数据标志，32-40字节hax=="aced0005" /对应byte[]={-84, -19, 0, 5}/new byte[]{(byte) 0xAC, (byte) 0xED, (byte) 0x00, (byte) 0x05}
        if (byteStartsWith(decryptedTextBytes, 16, new byte[]{(byte) 0xAC, (byte) 0xED, (byte) 0x00, (byte) 0x05})){
            decryptedTextBytes = byteSubArray(decryptedTextBytes, 16, decryptedTextBytes.length - 16);
        }
        if (byteStartsWith(decryptedTextBytes, 0, new byte[]{(byte) 0xAC, (byte) 0xED, (byte) 0x00, (byte) 0x05})){
            byte[] resultByte = DeserializerUtils.serializeParsing(decryptedTextBytes);

            // 反序列化恶意内容存储
            // cc6攻击链解析导出class及反编译java、其他的存储为ser
            String uuid = UUID.randomUUID().toString();
            String serTempFilePath = StrUtils.getCurrentJarDir() + File.separator + "Decompile" + File.separator + "serialize_" + uuid +".ser";
            Path outputDirPath = Paths.get(serTempFilePath).getParent();
            if (!Files.exists(outputDirPath)) {
                try {
                    Files.createDirectories(outputDirPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            StrUtils.createFile(decryptedTextBytes, serTempFilePath);
            if(resultByte!=null){
                StrUtils.createFile(resultByte, StrUtils.getCurrentJarDir() + File.separator + "Decompile" + File.separator + "serialize_" + uuid +".class");
                // 导出java
                String tips = "\n";
                //  初始化默认反编译模式配置
                JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(ConfigConstants.DECOMPILE);
                String decompileMode = tmpJsonObj.getAsJsonPrimitive(ConfigConstants.DECOMPILE_MODE).getAsString();

                String outputCode = tips + Decompile(resultByte, decompileMode);
                StrUtils.createFile(outputCode, StrUtils.getCurrentJarDir() + File.separator + "Decompile" + File.separator + "serialize_" + uuid +".java");
                resultData = outputCode.getBytes(StandardCharsets.UTF_8);

            }else{
                byte[] prefix = "【ser文件未能完全反编译，可能存在分段传输导致抽取class内容不完整，请自行在上下请求包中拼接16进制的ser文件内容进行单独解密。】\n".getBytes(StandardCharsets.UTF_8);
                resultData = ByteBuffer.allocate(prefix.length + decryptedTextBytes.length)
                        .put(prefix)
                        .put(decryptedTextBytes)
                        .array();
            }
        }
        return resultData;
    }


    /**
     * 检查是否为class数据流
     * @param byteData  需要检查是否存在class的byte[]数据
     */
    public static byte[] classDataCheck(byte[] byteData){
        byte[] resultData = null;

        // class数据流数据标志，开头="cafebabe"  / byte[] {(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE})
        if(byteStartsWith(byteData, 0, new byte[] {(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE})){
            String uuid = UUID.randomUUID().toString();
            System.out.println("可能存在class字节码数据，尝试导出class及java文件");
            String serTempFilePath = StrUtils.getCurrentJarDir() + File.separator + "Decompile" + File.separator + "tmpDataOut_" + uuid +".class";
            Path outputDirPath = Paths.get(serTempFilePath).getParent();
            if (!Files.exists(outputDirPath)) {
                try {
                    Files.createDirectories(outputDirPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String path = serTempFilePath;
            StrUtils.createFile(byteData, path);
            try {
                path = path + ".java";
                //  初始化默认反编译模式配置
                JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig(ConfigConstants.DECOMPILE);
                String decompileMode = tmpJsonObj.getAsJsonPrimitive(ConfigConstants.DECOMPILE_MODE).getAsString();
                String code = Decompile(byteData, decompileMode, path);
                if(code!=null){
                    resultData = code.getBytes(StandardCharsets.UTF_8);
                }else {
                    System.out.println("未能完全反编译，可能存在分段传输导致class内容不完整，请在上下请求包中拼接16进制的class文件内容进行单独解密");
                }
            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }

        }

        return resultData;
    }


    public static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }

    // 从字节数组反序列化对象
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        }
    }

}

class JavaStreamArrayTraverser {
    private byte[] byteArray;

    public byte[] getByteArray(){
        return byteArray;
    }

    public void traverseValue(Value value) {
        if (value.hasArrayElements()) {
            // 处理数组类型的值
            traverseArray(value);
        } else if (value.hasMembers()) {
            // 处理字典（对象）类型的值
            traverseObject(value);
        }
    }

    public void traverseArray(Value array) {
        int length = (int) array.getArraySize();

        // 遍历当前层级的数组元素
        for (int i = 0; i < length; i++) {
            Value element = array.getArrayElement(i);
            if (element.hasArrayElements() && element.getArraySize()>8 && startsWith(element,new int[]{-54, -2, -70, -66})) {
                int elementLength = (int)element.getArraySize();
                byteArray = new byte[elementLength];

                for (int j = 0; j < elementLength; j++) {
                    byteArray[j] = (byte) element.getArrayElement(j).asInt();
                }
            } else {
                // 递归遍历下一层级的值
                traverseValue(element);
            }
        }
    }

    public void traverseObject(Value object) {
        // 获取当前层级的属性列表，遍历当前层级的属性
        for (String key : object.getMemberKeys()) {
            traverseValue(object.getMember(key));
        }
    }

}

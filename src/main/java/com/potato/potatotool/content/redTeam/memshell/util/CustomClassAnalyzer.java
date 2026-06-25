package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import javax.servlet.Filter;
import javax.servlet.ServletRequestListener;
import java.io.File;
import java.io.DataInputStream;
import java.io.FileInputStream;

public class CustomClassAnalyzer {
    private static final String JAVAX_FILTER = Filter.class.getName();
    private static final String JAVAX_LISTENER = ServletRequestListener.class.getName();
    private static final String JAKARTA_FILTER = "jakarta.servlet.Filter";
    private static final String JAKARTA_LISTENER = "jakarta.servlet.ServletRequestListener";
    private static final String SPRING_ASYNC_INTERCEPTOR = "org.springframework.web.servlet.AsyncHandlerInterceptor";
    private static final String SPRING_INTERCEPTOR = "org.springframework.web.servlet.HandlerInterceptor";

    public static CustomClassMetadata analyze(String classFilePath) throws Exception {
        if (classFilePath == null || classFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Class file path is empty");
        }
        File classFile = new File(classFilePath.trim());
        if (!classFile.isFile()) {
            throw new IllegalArgumentException("Class file does not exist: " + classFilePath);
        }

        ClassPool classPool = new ClassPool(true);
        classPool.insertClassPath(new ClassClassPath(Filter.class));
        classPool.insertClassPath(new ClassClassPath(ServletRequestListener.class));
        ensureInterface(classPool, JAKARTA_FILTER);
        ensureInterface(classPool, JAKARTA_LISTENER);
        ensureInterface(classPool, SPRING_ASYNC_INTERCEPTOR);
        ensureInterface(classPool, SPRING_INTERCEPTOR);

        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(classFile))) {
            CtClass ctClass = classPool.makeClass(inputStream);
            try {
                String shellType = detectShellType(ctClass, classPool);
                return new CustomClassMetadata(shellType,
                        ClassNameUtil.requireValidJavaClassName(ctClass.getName(), "custom shell class name"));
            } finally {
                ctClass.detach();
            }
        }
    }

    private static String detectShellType(CtClass ctClass, ClassPool classPool) throws Exception {
        if (isSubtypeOf(ctClass, classPool, JAKARTA_FILTER)) {
            return MemoryShellConstants.SHELLTYPE_JAKARTA_FILTER;
        }
        if (isSubtypeOf(ctClass, classPool, JAKARTA_LISTENER)) {
            return MemoryShellConstants.SHELLTYPE_JAKARTA_LISTENER;
        }
        if (isSubtypeOf(ctClass, classPool, JAVAX_FILTER)) {
            return MemoryShellConstants.SHELLTYPE_FILTER;
        }
        if (isSubtypeOf(ctClass, classPool, JAVAX_LISTENER)) {
            return MemoryShellConstants.SHELLTYPE_LISTENER;
        }
        if (isSubtypeOf(ctClass, classPool, SPRING_ASYNC_INTERCEPTOR)
                || isSubtypeOf(ctClass, classPool, SPRING_INTERCEPTOR)
                || containsInterceptorInterface(ctClass)) {
            return MemoryShellConstants.SHELLTYPE_INTERCEPTOR;
        }
        throw new IllegalArgumentException("Custom class is neither a Filter, Listener, nor Interceptor: " + ctClass.getName());
    }

    private static boolean isSubtypeOf(CtClass ctClass, ClassPool classPool, String typeName) {
        try {
            CtClass targetType = classPool.get(typeName);
            return ctClass.subtypeOf(targetType);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean containsInterceptorInterface(CtClass ctClass) throws NotFoundException {
        try {
            CtClass[] interfaces = ctClass.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                CtClass intf = interfaces[i];
                if (intf.getName().contains("Interceptor") || containsInterceptorInterface(intf)) {
                    return true;
                }
            }
            CtClass superClass = ctClass.getSuperclass();
            return superClass != null
                    && !"java.lang.Object".equals(superClass.getName())
                    && containsInterceptorInterface(superClass);
        } catch (NotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void ensureInterface(ClassPool classPool, String interfaceName) {
        try {
            classPool.get(interfaceName);
        } catch (NotFoundException ignored) {
            classPool.makeInterface(interfaceName);
        }
    }
}

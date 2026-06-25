package com.potato.potatotool.content.redTeam.payload;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.util.ClassNameUtil;
import com.potato.potatotool.content.redTeam.memshell.util.FormatUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class PayloadOutputService {

    public static PayloadOutputResult format(PayloadOutputRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Payload output request is null");
        }
        byte[] inputBytes = request.getBytes();
        if (inputBytes == null || inputBytes.length == 0) {
            throw new IllegalArgumentException("Payload bytes are empty");
        }
        PayloadFormat format = request.getFormat();
        if (format == null) {
            throw new IllegalArgumentException("Payload format is null");
        }

        byte[] outputBytes;
        switch (format) {
            case RAW:
            case CLASS:
                outputBytes = inputBytes;
                break;
            case BASE64:
                outputBytes = FormatUtil.Base64Format(inputBytes);
                break;
            case BIGINTEGER:
                outputBytes = new BigInteger(inputBytes).toString(36).getBytes(StandardCharsets.UTF_8);
                break;
            case HEX:
                outputBytes = toHex(inputBytes).getBytes(StandardCharsets.UTF_8);
                break;
            case JAR:
                outputBytes = jarFormat(inputBytes, request.getClassName());
                break;
            case JSP:
                outputBytes = FormatUtil.JspFormat(inputBytes, toMemoryObj(request));
                break;
            case JS:
                outputBytes = FormatUtil.JsFormat(inputBytes, toMemoryObj(request));
                break;
            case BCEL:
                outputBytes = FormatUtil.BcelFormat(toMemoryObj(request));
                break;
            default:
                throw new IllegalArgumentException("Unsupported payload output format: " + format);
        }
        if (outputBytes == null || outputBytes.length == 0) {
            throw new IllegalStateException("Payload output is empty: " + format);
        }

        String filePath = null;
        if (isFileFormat(format) && request.getOutputPath() != null && !request.getOutputPath().trim().isEmpty()) {
            filePath = getOutputFilePath(format, request.getClassName(), request.getOutputPath());
            Files.write(new File(filePath).toPath(), outputBytes);
        }
        return new PayloadOutputResult(format, outputBytes, filePath);
    }

    public static PayloadFormat parseFormat(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Payload format is empty");
        }
        return PayloadFormat.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public static String getOutputFilePath(PayloadFormat format, String className, String outputPath) throws Exception {
        return PayloadOutputPathResolver.getOutputFilePath(format, className, outputPath);
    }

    public static boolean isFileFormat(PayloadFormat format) {
        return format == PayloadFormat.CLASS || format == PayloadFormat.JSP || format == PayloadFormat.JAR;
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        char[] hex = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            chars[i * 2] = hex[value >>> 4];
            chars[i * 2 + 1] = hex[value & 0x0f];
        }
        return new String(chars);
    }

    private static byte[] jarFormat(byte[] bytes, String className) throws Exception {
        String normalizedClassName = ClassNameUtil.requireValidJavaClassName(className, "payload class name");
        String entryName = normalizedClassName.replace('.', '/') + ".class";
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JarOutputStream jar = new JarOutputStream(out, manifest);
        try {
            jar.putNextEntry(new JarEntry(entryName));
            jar.write(bytes);
            jar.closeEntry();
        } finally {
            jar.close();
        }
        return out.toByteArray();
    }

    private static MemoryObj toMemoryObj(PayloadOutputRequest request) {
        MemoryObj memoryObj = new MemoryObj();
        memoryObj.setInjectorBytes(request.getBytes());
        memoryObj.setInjectorClassName(requireClassName(request.getClassName()));
        memoryObj.setInjectorSimpleClassName(simpleClassName(request.getClassName()));
        memoryObj.setLoaderClassName(request.getLoaderClassName());
        if (memoryObj.getLoaderClassName() == null || memoryObj.getLoaderClassName().trim().isEmpty()) {
            memoryObj.setLoaderClassName("org.apache.commons.collections.functors.PayloadLoader");
        }
        return memoryObj;
    }

    private static String requireClassName(String className) {
        return ClassNameUtil.requireValidJavaClassName(className, "payload class name");
    }

    private static String simpleClassName(String className) {
        String normalized = requireClassName(className);
        int dot = normalized.lastIndexOf('.');
        return dot >= 0 ? normalized.substring(dot + 1) : normalized;
    }

}

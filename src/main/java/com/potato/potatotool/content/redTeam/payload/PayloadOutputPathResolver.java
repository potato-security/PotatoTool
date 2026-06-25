package com.potato.potatotool.content.redTeam.payload;

import com.potato.potatotool.content.redTeam.memshell.util.ClassNameUtil;

import java.io.File;
import java.io.IOException;

public class PayloadOutputPathResolver {
    public static String getOutputFilePath(PayloadFormat format, String className, String outputPath) throws IOException {
        if (!PayloadOutputService.isFileFormat(format)) {
            throw new IllegalArgumentException("Unsupported file output format: " + format);
        }
        String normalizedOutputPath = outputPath == null || outputPath.trim().isEmpty() ? "." : outputPath.trim();
        File path = new File(normalizedOutputPath);
        String extension = extension(format);

        if (normalizedOutputPath.endsWith(".class") || normalizedOutputPath.endsWith(".jar") || normalizedOutputPath.endsWith(".jsp")) {
            if (!normalizedOutputPath.endsWith(extension)) {
                throw new IllegalArgumentException("Output file extension does not match format: " + format);
            }
            File parentDir = path.getParentFile();
            if (parentDir != null) {
                ensureDirectory(parentDir);
            }
            return path.getAbsolutePath();
        }

        ensureDirectory(path);
        String simpleClassName = simpleClassName(className);
        return new File(path, simpleClassName + extension).getAbsolutePath();
    }

    private static String simpleClassName(String className) {
        String normalized = ClassNameUtil.requireValidJavaClassName(className, "output class name");
        int index = normalized.lastIndexOf('.');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private static String extension(PayloadFormat format) {
        switch (format) {
            case CLASS:
                return ".class";
            case JAR:
                return ".jar";
            case JSP:
                return ".jsp";
            default:
                throw new IllegalArgumentException("Unsupported file output format: " + format);
        }
    }

    private static void ensureDirectory(File directory) throws IOException {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IOException("Output path is not a directory: " + directory.getAbsolutePath());
            }
            return;
        }
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Failed to create output directory: " + directory.getAbsolutePath());
        }
    }
}

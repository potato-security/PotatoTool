package com.potato.potatotool.utils.ai.model;

import java.io.File;

public class AiAttachment {
    private final String fileName;
    private final String absolutePath;
    private final long fileSize;
    private final String mimeType;

    public AiAttachment(String fileName, String absolutePath, long fileSize, String mimeType) {
        this.fileName = fileName == null ? "" : fileName;
        this.absolutePath = absolutePath == null ? "" : absolutePath;
        this.fileSize = Math.max(0L, fileSize);
        this.mimeType = mimeType == null ? "application/octet-stream" : mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public File toFile() {
        return absolutePath == null || absolutePath.trim().isEmpty() ? null : new File(absolutePath);
    }
}

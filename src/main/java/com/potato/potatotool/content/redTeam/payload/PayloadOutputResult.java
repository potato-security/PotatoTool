package com.potato.potatotool.content.redTeam.payload;

public class PayloadOutputResult {
    private final PayloadFormat format;
    private final byte[] bytes;
    private final String filePath;

    public PayloadOutputResult(PayloadFormat format, byte[] bytes, String filePath) {
        this.format = format;
        this.bytes = bytes;
        this.filePath = filePath;
    }

    public PayloadFormat getFormat() {
        return format;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isFileOutput() {
        return filePath != null && !filePath.trim().isEmpty();
    }
}

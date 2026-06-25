package com.potato.potatotool.content.redTeam.payload.yso;

import java.io.Serializable;
import java.util.Arrays;

public class ClassFilePayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final byte[] classBytes;

    public ClassFilePayload(String fileName, byte[] classBytes) {
        this.fileName = fileName;
        this.classBytes = classBytes == null ? new byte[0] : Arrays.copyOf(classBytes, classBytes.length);
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getClassBytes() {
        return Arrays.copyOf(classBytes, classBytes.length);
    }
}

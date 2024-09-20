package com.potato.potatotool.content.redTeam.secureprotect.exceptions;

public class ProtectionException extends Exception {
    private int errorCode;

    public ProtectionException(String message) {
        super(message);
        System.out.print("测试版，代码已剔除");
    }

    public int getErrorCode() {
        System.out.print("测试版，代码已剔除");
        return errorCode;
    }
}

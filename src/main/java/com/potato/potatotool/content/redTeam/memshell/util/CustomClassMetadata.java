package com.potato.potatotool.content.redTeam.memshell.util;

public class CustomClassMetadata {
    private final String shellType;
    private final String className;

    public CustomClassMetadata(String shellType, String className) {
        this.shellType = shellType;
        this.className = className;
    }

    public String getShellType() {
        return shellType;
    }

    public String getClassName() {
        return className;
    }
}

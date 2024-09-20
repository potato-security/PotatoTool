package com.potato.potatotool.content.redTeam.secureprotect.core;

public class ProtectionOptions {
    private boolean enablePacking;
    private boolean enableAntiReverseEngineering;
    private boolean enableAntiTampering;
    private boolean enableRuntimeProtection;
    private boolean enableScriptEncryption;
    private boolean enableCodeObfuscation;
    private boolean enableCodeEncryption;
    private boolean enableVirtualization;

    // Getter and Setter methods
    public boolean isEnablePacking() {
        return enablePacking;
    }

    public void setEnablePacking(boolean enablePacking) {
        this.enablePacking = enablePacking;
    }

    public boolean isEnableAntiReverseEngineering() {
        return enableAntiReverseEngineering;
    }

    public void setEnableAntiReverseEngineering(boolean enableAntiReverseEngineering) {
        this.enableAntiReverseEngineering = enableAntiReverseEngineering;
    }

    public boolean isEnableAntiTampering() {
        return enableAntiTampering;
    }

    public void setEnableAntiTampering(boolean enableAntiTampering) {
        this.enableAntiTampering = enableAntiTampering;
    }

    public boolean isEnableRuntimeProtection() {
        return enableRuntimeProtection;
    }

    public void setEnableRuntimeProtection(boolean enableRuntimeProtection) {
        this.enableRuntimeProtection = enableRuntimeProtection;
    }

    public boolean isEnableScriptEncryption() {
        return enableScriptEncryption;
    }

    public void setEnableScriptEncryption(boolean enableScriptEncryption) {
        this.enableScriptEncryption = enableScriptEncryption;
    }

    public boolean isEnableCodeObfuscation() {
        return enableCodeObfuscation;
    }

    public void setEnableCodeObfuscation(boolean enableCodeObfuscation) {
        this.enableCodeObfuscation = enableCodeObfuscation;
    }

    public boolean isEnableCodeEncryption() {
        return enableCodeEncryption;
    }

    public void setEnableCodeEncryption(boolean enableCodeEncryption) {
        this.enableCodeEncryption = enableCodeEncryption;
    }

    public boolean isEnableVirtualization() {
        return enableVirtualization;
    }

    public void setEnableVirtualization(boolean enableVirtualization) {
        this.enableVirtualization = enableVirtualization;
    }
}
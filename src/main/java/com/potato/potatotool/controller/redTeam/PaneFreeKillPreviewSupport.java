package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.ToStart;

import java.util.ArrayList;
import java.util.List;

final class PaneFreeKillPreviewSupport {

    private PaneFreeKillPreviewSupport() {
    }

    static PreviewState buildPreviewState(ToStart.StartupPage startupPage) {
        if (startupPage == null) {
            return null;
        }
        switch (startupPage) {
            case RED_FREE_KILL_SIGNATURE:
                return signatureState();
            case RED_FREE_KILL_QRCODE:
                return qrState();
            case RED_FREE_KILL_PROTECT:
                return protectState();
            case RED_FREE_KILL_RUNNING:
                return runningState();
            case RED_FREE_KILL_DONE:
                return doneState();
            case RED_FREE_KILL_FAILURE:
                return failureState();
            default:
                return null;
        }
    }

    private static PreviewState signatureState() {
        PreviewState state = new PreviewState();
        state.mode = PreviewMode.SIGNATURE;
        state.sigFilePath = "/Users/Potato/Desktop/projectDevelopment/PotatoTool/samples/unsigned/potato-loader.exe";
        state.exePath = "/Users/Potato/Desktop/projectDevelopment/PotatoTool/samples/signed/notepad-signed.exe";
        return state;
    }

    private static PreviewState qrState() {
        PreviewState state = new PreviewState();
        state.mode = PreviewMode.QR;
        state.contentQR = "https://preview.potato.example/download?id=qr-demo";
        state.widthQR = "360";
        state.logoPath = "/Users/Potato/Desktop/projectDevelopment/PotatoTool/screenshots/99_reference/logo-preview.png";
        state.widthLogo = "72";
        return state;
    }

    private static PreviewState protectState() {
        PreviewState state = new PreviewState();
        state.mode = PreviewMode.PROTECT;
        state.protectFilePath = "/Users/Potato/Desktop/projectDevelopment/PotatoTool/samples/loader/potato-agent.exe";
        state.filePos = ".text / .rdata / .pdata / .rsrc";
        state.fileCreateTime = "2026-06-24 01:42:18";
        state.fileChangeTime = "2026-06-24 01:49:32";
        state.fileRunTime = "2026-06-24 02:03:11";
        state.fileType = "PE32+ executable (GUI) x86-64";
        state.funcSize = "128";
        state.confusionSize = "48";
        state.virtualizationSize = "12";
        state.encodeSize = "19";
        state.importProtect = true;
        state.memoryProtect = true;
        state.compressProtect = true;
        state.resourceProtect = false;
        state.debuggerDetection = true;
        state.virtualDetection = true;
        state.protectMode = "代码虚拟化";
        state.assemblyLines.add("0x401000  push rbp");
        state.assemblyLines.add("0x401001  mov rbp, rsp");
        state.assemblyLines.add("0x401004  call decrypt_config");
        state.assemblyLines.add("0x40102A  jnz 0x401090 ; vm dispatch");
        return state;
    }

    private static PreviewState runningState() {
        PreviewState state = qrState();
        state.tipTitleQR = "正在生成二维码，请稍候...";
        return state;
    }

    private static PreviewState doneState() {
        PreviewState state = qrState();
        state.tipTitleQR = "二维码已生成：/Users/Potato/Desktop/projectDevelopment/PotatoTool/output/Qr/二维码_preview_success.png";
        return state;
    }

    private static PreviewState failureState() {
        PreviewState state = qrState();
        state.tipTitleQR = "二维码生成失败：java.io.IOException: logo file not found";
        return state;
    }

    enum PreviewMode {
        SIGNATURE,
        QR,
        PROTECT
    }

    static final class PreviewState {
        private PreviewMode mode;
        private String sigFilePath;
        private String exePath;
        private String contentQR;
        private String widthQR;
        private String logoPath;
        private String widthLogo;
        private String tipTitleQR;
        private String protectFilePath;
        private String filePos;
        private String fileCreateTime;
        private String fileChangeTime;
        private String fileRunTime;
        private String fileType;
        private String funcSize;
        private String confusionSize;
        private String virtualizationSize;
        private String encodeSize;
        private boolean importProtect;
        private boolean memoryProtect;
        private boolean compressProtect;
        private boolean resourceProtect;
        private boolean debuggerDetection;
        private boolean virtualDetection;
        private String protectMode;
        private final List<String> assemblyLines = new ArrayList<String>();

        PreviewMode getMode() {
            return mode;
        }

        String getSigFilePath() {
            return sigFilePath;
        }

        String getExePath() {
            return exePath;
        }

        String getContentQR() {
            return contentQR;
        }

        String getWidthQR() {
            return widthQR;
        }

        String getLogoPath() {
            return logoPath;
        }

        String getWidthLogo() {
            return widthLogo;
        }

        String getTipTitleQR() {
            return tipTitleQR;
        }

        String getProtectFilePath() {
            return protectFilePath;
        }

        String getFilePos() {
            return filePos;
        }

        String getFileCreateTime() {
            return fileCreateTime;
        }

        String getFileChangeTime() {
            return fileChangeTime;
        }

        String getFileRunTime() {
            return fileRunTime;
        }

        String getFileType() {
            return fileType;
        }

        String getFuncSize() {
            return funcSize;
        }

        String getConfusionSize() {
            return confusionSize;
        }

        String getVirtualizationSize() {
            return virtualizationSize;
        }

        String getEncodeSize() {
            return encodeSize;
        }

        boolean isImportProtect() {
            return importProtect;
        }

        boolean isMemoryProtect() {
            return memoryProtect;
        }

        boolean isCompressProtect() {
            return compressProtect;
        }

        boolean isResourceProtect() {
            return resourceProtect;
        }

        boolean isDebuggerDetection() {
            return debuggerDetection;
        }

        boolean isVirtualDetection() {
            return virtualDetection;
        }

        String getProtectMode() {
            return protectMode;
        }

        List<String> getAssemblyLines() {
            return assemblyLines;
        }
    }
}

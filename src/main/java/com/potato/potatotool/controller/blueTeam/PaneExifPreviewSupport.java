package com.potato.potatotool.controller.blueTeam;

import com.potato.potatotool.ToStart;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class PaneExifPreviewSupport {

    private static final String PREVIEW_QR_TEXT = "https://potatotool.example/exif-preview";

    private PaneExifPreviewSupport() {
    }

    static PreviewState buildPreviewState(ToStart.StartupPage startupPage) {
        if (startupPage == ToStart.StartupPage.BLUE_EXIF_RESULT) {
            LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
            metadata.put("文件名", "incident-camera-01.jpg");
            metadata.put("文件类型", "JPEG");
            metadata.put("拍摄时间", "2026:06:23 19:42:18");
            metadata.put("设备型号", "PotatoCam X1");
            metadata.put("镜头型号", "24-70mm F2.8");
            metadata.put("GPS经度", "121.4737");
            metadata.put("GPS纬度", "31.2304");
            metadata.put("软件", "PotatoTool Preview");
            return new PreviewState(PREVIEW_QR_TEXT, metadata, false);
        }
        if (startupPage == ToStart.StartupPage.BLUE_EXIF_FAILURE) {
            return new PreviewState("读取错误", new LinkedHashMap<String, String>(), true);
        }
        return null;
    }

    static final class PreviewState {
        private final String qrText;
        private final LinkedHashMap<String, String> metadataMap;
        private final boolean unsupported;

        PreviewState(String qrText, LinkedHashMap<String, String> metadataMap, boolean unsupported) {
            this.qrText = qrText;
            this.metadataMap = metadataMap == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<>(metadataMap);
            this.unsupported = unsupported;
        }

        String getQrText() {
            return qrText;
        }

        Map<String, String> getMetadataMap() {
            return Collections.unmodifiableMap(metadataMap);
        }

        boolean isUnsupported() {
            return unsupported;
        }
    }
}

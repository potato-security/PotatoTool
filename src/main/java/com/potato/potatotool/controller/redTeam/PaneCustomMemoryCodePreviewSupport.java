package com.potato.potatotool.controller.redTeam;

import com.potato.potatotool.ToStart;

import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.EXPRENCODER_VELOCITY;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.GADGET_FASTJSON_GROOVY;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.OUTPUTFORMAT_BASE64;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.OUTPUTFORMAT_JAR_AGENT;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.SERVER_SPRING_MVC;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.SERVER_SPRING_WEBFLUX;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.SERVER_TOMCAT;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.SHELLTYPE_INTERCEPTOR;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.SHELLTYPE_JAKARTA_FILTER;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.SHELLTYPE_WFHANDLERMETHOD;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.TOOL_BEHINDER;
import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.TOOL_GODZILLA;

final class PaneCustomMemoryCodePreviewSupport {

    private PaneCustomMemoryCodePreviewSupport() {
    }

    static PreviewState buildPreviewState(ToStart.StartupPage startupPage) {
        if (startupPage == null) {
            return null;
        }
        switch (startupPage) {
            case RED_CUSTOM_MEMORY_CODE_TYPE_OPTIONS:
                return typeOptionsState();
            case RED_CUSTOM_MEMORY_CODE_CUSTOM_OPTIONS:
                return customOptionsState();
            case RED_CUSTOM_MEMORY_CODE_GADGET:
                return gadgetState();
            case RED_CUSTOM_MEMORY_CODE_RESULT:
                return resultState();
            default:
                return null;
        }
    }

    private static PreviewState typeOptionsState() {
        PreviewState state = new PreviewState();
        state.toolType = TOOL_GODZILLA;
        state.serverType = SERVER_SPRING_WEBFLUX;
        state.shellType = SHELLTYPE_WFHANDLERMETHOD;
        state.outputFormat = OUTPUTFORMAT_BASE64;
        state.urlPattern = "/gateway/status";
        state.scrollValue = 0.0;
        return state;
    }

    private static PreviewState customOptionsState() {
        PreviewState state = new PreviewState();
        state.toolType = TOOL_BEHINDER;
        state.serverType = SERVER_TOMCAT;
        state.shellType = SHELLTYPE_JAKARTA_FILTER;
        state.outputFormat = OUTPUTFORMAT_JAR_AGENT;
        state.pass = "PreviewPass2026";
        state.key = "PreviewKey2026";
        state.headerName = "X-Potato-Token";
        state.headerValue = "preview-custom-flag";
        state.shellClassName = "com.potato.preview.memshell.PreviewJakartaFilter";
        state.injectorClassName = "com.potato.preview.memshell.PreviewAgentInjector";
        state.urlPattern = "/assets/preview/*";
        state.outputPath = "/Users/Potato/Desktop/projectDevelopment/PotatoTool/output/memshell/custom-preview";
        state.enableBypassJdkModule = true;
        state.scrollValue = 0.28;
        return state;
    }

    private static PreviewState gadgetState() {
        PreviewState state = new PreviewState();
        state.toolType = TOOL_GODZILLA;
        state.serverType = SERVER_SPRING_MVC;
        state.shellType = SHELLTYPE_INTERCEPTOR;
        state.outputFormat = OUTPUTFORMAT_BASE64;
        state.pass = "GadgetPass2026";
        state.key = "GadgetKey2026";
        state.headerName = "X-Audit-Trace";
        state.headerValue = "preview-gadget-hit";
        state.shellClassName = "com.potato.preview.memshell.SpringMvcInterceptorPreview";
        state.injectorClassName = "com.potato.preview.memshell.SpringMvcInjectorPreview";
        state.urlPattern = "/api/internal/report";
        state.outputPath = "/Users/Potato/Desktop/projectDevelopment/PotatoTool/output/memshell/gadget-preview";
        state.gadgetType = GADGET_FASTJSON_GROOVY;
        state.exprEncoder = EXPRENCODER_VELOCITY;
        state.scrollValue = 0.63;
        return state;
    }

    private static PreviewState resultState() {
        PreviewState state = gadgetState();
        state.outputText =
                "结果文件: /Users/Potato/Desktop/projectDevelopment/PotatoTool/output/memshell/preview/GodzillaSpringMvcInterceptorPreview.base64\n" +
                "内存马类型: Godzilla / SpringMVC / Interceptor\n" +
                "输出格式: BASE64\n" +
                "访问路径: /api/internal/report\n" +
                "请求头: X-Audit-Trace: preview-gadget-hit\n" +
                "内存马类名: com.potato.preview.memshell.SpringMvcInterceptorPreview\n" +
                "注入器类名: com.potato.preview.memshell.SpringMvcInjectorPreview\n" +
                "表达式编码: Velocity\n" +
                "专项利用: FastjsonGroovy\n" +
                "Payload 预览: rO0ABXNyAB9jb20ucG90YXRvLnByZXZpZXcubWVtc2hlbGwuUHJldmlld1BheWxvYWQAAAAAAAAAAQIAAUwABmJvZHl0ABJMamF2YS9sYW5nL1N0cmluZzt4cHQAOUJBU0U2NF9QUkVWSUVXX1BBWUxPQURfRk9SX1NDUkVFTlNIT1RfQVVESVQ=\n" +
                "状态: 预置生成结果，仅用于截图展示。";
        state.scrollValue = 1.0;
        return state;
    }

    static final class PreviewState {
        private String toolType;
        private String serverType;
        private String shellType;
        private String outputFormat;
        private String pass;
        private String key;
        private String headerName;
        private String headerValue;
        private String shellClassName;
        private String injectorClassName;
        private String urlPattern;
        private String outputPath;
        private boolean enableBypassJdkModule;
        private String gadgetType;
        private String exprEncoder;
        private String outputText;
        private double scrollValue;

        String getToolType() {
            return toolType;
        }

        String getServerType() {
            return serverType;
        }

        String getShellType() {
            return shellType;
        }

        String getOutputFormat() {
            return outputFormat;
        }

        String getPass() {
            return pass;
        }

        String getKey() {
            return key;
        }

        String getHeaderName() {
            return headerName;
        }

        String getHeaderValue() {
            return headerValue;
        }

        String getShellClassName() {
            return shellClassName;
        }

        String getInjectorClassName() {
            return injectorClassName;
        }

        String getUrlPattern() {
            return urlPattern;
        }

        String getOutputPath() {
            return outputPath;
        }

        boolean isEnableBypassJdkModule() {
            return enableBypassJdkModule;
        }

        String getGadgetType() {
            return gadgetType;
        }

        String getExprEncoder() {
            return exprEncoder;
        }

        String getOutputText() {
            return outputText;
        }

        double getScrollValue() {
            return scrollValue;
        }
    }
}

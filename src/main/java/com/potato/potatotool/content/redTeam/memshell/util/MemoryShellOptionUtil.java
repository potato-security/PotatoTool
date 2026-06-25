package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;

import java.util.Map;

public class MemoryShellOptionUtil {

    public static void normalizeAndRequireSupportedOptions(MemoryObj memoryObj) {
        if (memoryObj == null) {
            throw new IllegalArgumentException("Memory object is null");
        }
        String toolType = requireSupportedTool(memoryObj.getToolType());
        String serverType = requireSupportedServer(toolType, memoryObj.getServerType());
        String shellType = requireSupportedShell(toolType, serverType, memoryObj.getShellType());
        String outputFormat = requireSupportedOutputFormat(serverType, memoryObj.getOutputFormat());

        memoryObj.setToolType(toolType);
        memoryObj.setServerType(serverType);
        memoryObj.setShellType(shellType);
        memoryObj.setOutputFormat(outputFormat);

        if (MemoryShellConstants.isNoneOption(memoryObj.getGadgetType())) {
            memoryObj.setGadgetType(null);
        } else {
            memoryObj.setGadgetType(requireSupportedGadget(memoryObj.getGadgetType()));
        }
        if (MemoryShellConstants.isNoneOption(memoryObj.getExprEncoder())) {
            memoryObj.setExprEncoder(null);
        } else {
            memoryObj.setExprEncoder(requireSupportedExprEncoder(memoryObj.getExprEncoder()));
        }
    }

    public static void normalizeAndPrepareForGeneration(MemoryObj memoryObj) {
        normalizeAndRequireSupportedOptions(memoryObj);

        if (isBlank(memoryObj.getPass())) {
            memoryObj.setPass(MemoryShellRandomUtil.randomAlpha(6, 10));
        }
        if (isBlank(memoryObj.getKey())) {
            memoryObj.setKey(MemoryShellRandomUtil.randomAlpha(6, 10));
        }
        if (MemoryShellConstants.TOOL_NEOREGEORG.equals(memoryObj.getToolType())) {
            memoryObj.setKey("key");
        }

        if (!MemoryShellConstants.TOOL_CUSTOM.equals(memoryObj.getToolType()) && isBlank(memoryObj.getShellClassName())) {
            memoryObj.setShellClassName(ClassNameUtil.getRandomShellClassName(memoryObj.getShellType()));
        }
        if (!isBlank(memoryObj.getShellClassName())) {
            memoryObj.setShellClassName(ClassNameUtil.requireValidJavaClassName(memoryObj.getShellClassName(), "shell class name"));
        }
        if (isBlank(memoryObj.getInjectorClassName())) {
            memoryObj.setInjectorClassName(ClassNameUtil.getRandomInjectorClassName());
        }
        memoryObj.setInjectorClassName(ClassNameUtil.requireValidJavaClassName(memoryObj.getInjectorClassName(), "injector class name"));

        Map.Entry<String, String> header = RandomHeaderUtil.generateRandomHeader();
        if (isBlank(memoryObj.getHeaderName())) {
            memoryObj.setHeaderName(header.getKey());
        }
        memoryObj.setHeaderName(RandomHeaderUtil.requireValidHeaderName(memoryObj.getHeaderName()));
        if (isBlank(memoryObj.getHeaderValue())) {
            memoryObj.setHeaderValue(header.getValue());
        }
        memoryObj.setHeaderValue(RandomHeaderUtil.requireValidHeaderValue(memoryObj.getHeaderValue()));

        if (isBlank(memoryObj.getUrlPattern()) || "/*".equals(memoryObj.getUrlPattern()) || "/".equals(memoryObj.getUrlPattern())) {
            if (MemoryShellConstants.SHELLTYPE_WFHANDLERMETHOD.equals(memoryObj.getShellType())) {
                memoryObj.setUrlPattern("/" + MemoryShellRandomUtil.randomLowerAlpha(6));
            } else {
                memoryObj.setUrlPattern("/*");
            }
        }
        memoryObj.setUrlPattern(UrlPatternUtil.requireValidUrlPattern(memoryObj.getUrlPattern()));

        if (MemoryShellConstants.OUTPUTFORMAT_BCEL.equals(memoryObj.getOutputFormat())) {
            if (isBlank(memoryObj.getLoaderClassName())) {
                memoryObj.setLoaderClassName(ClassNameUtil.getRandomLoaderClassName());
            }
            memoryObj.setLoaderClassName(ClassNameUtil.requireValidJavaClassName(memoryObj.getLoaderClassName(), "loader class name"));
        }
        memoryObj.setInjectorSimpleClassName(getSimpleName(memoryObj.getInjectorClassName()));
    }

    public static String requireSupportedTool(String toolType) {
        return requireOneOf(toolType, "tool type", MemoryShellConstants.TOOLS);
    }

    public static String requireSupportedServer(String toolType, String serverType) {
        String normalizedTool = requireSupportedTool(toolType);
        return requireOneOf(serverType, "server type for " + normalizedTool, getServersForTool(normalizedTool));
    }

    public static String requireSupportedShell(String serverType, String shellType) {
        return requireSupportedShell(null, serverType, shellType);
    }

    public static String requireSupportedShell(String toolType, String serverType, String shellType) {
        String normalizedTool = normalize(toolType);
        if (normalizedTool != null) {
            normalizedTool = requireSupportedTool(normalizedTool);
        }
        String normalizedServer = requireKnownServer(serverType);
        return requireOneOf(shellType, "shell type for " + normalizedServer, getShellTypesForServer(normalizedServer, normalizedTool));
    }

    public static String requireSupportedOutputFormat(String serverType, String outputFormat) {
        String normalizedServer = requireKnownServer(serverType);
        return requireOneOf(outputFormat, "output format for " + normalizedServer, getOutputFormatsForServer(normalizedServer));
    }

    public static String requireSupportedGadget(String gadgetType) {
        return requireOneOf(gadgetType, "gadget type", MemoryShellConstants.GADGETS);
    }

    public static String requireSupportedExprEncoder(String exprEncoder) {
        return requireOneOf(exprEncoder, "expr encoder", MemoryShellConstants.EXPRENCODERS);
    }

    public static String[] getServersForTool(String toolType) {
        String normalizedTool = requireSupportedTool(toolType);
        if (MemoryShellConstants.TOOL_ANTSWORD.equals(normalizedTool)) {
            return MemoryShellConstants.SERVERS_TOOL_ANTSWORD;
        } else if (MemoryShellConstants.TOOL_BEHINDER.equals(normalizedTool)) {
            return MemoryShellConstants.SERVERS_TOOL_BEHINDER;
        } else if (MemoryShellConstants.TOOL_GODZILLA.equals(normalizedTool)) {
            return MemoryShellConstants.SERVERS_TOOL_GODZILLA;
        } else if (MemoryShellConstants.TOOL_CUSTOM.equals(normalizedTool)) {
            return MemoryShellConstants.SERVERS_TOOL_CUSTOM;
        } else if (MemoryShellConstants.TOOL_NEOREGEORG.equals(normalizedTool)) {
            return MemoryShellConstants.SERVERS_TOOL_NEOREGEORG;
        } else if (MemoryShellConstants.TOOL_SUO5.equals(normalizedTool)) {
            return MemoryShellConstants.SERVERS_TOOL_SUO5;
        }
        throw new IllegalArgumentException("Unsupported tool type: " + toolType);
    }

    public static String[] getShellTypesForServer(String serverType) {
        return getShellTypesForServer(serverType, null);
    }

    public static String[] getShellTypesForServer(String serverType, String toolType) {
        String normalizedServer = requireKnownServer(serverType);
        if (MemoryShellConstants.SERVER_SPRING_MVC.equals(normalizedServer)) {
            return MemoryShellConstants.SHELLTYPES_SERVER_SPRING_MVC;
        } else if (MemoryShellConstants.SERVER_SPRING_WEBFLUX.equals(normalizedServer)) {
            return MemoryShellConstants.SHELLTYPES_SERVER_SPRING_WEBFLUX;
        } else if (MemoryShellConstants.SERVER_TONGWEB.equals(normalizedServer)) {
            return MemoryShellConstants.SHELLTYPES_SERVER_TONGWEB;
        } else if (MemoryShellConstants.SERVER_TOMCAT.equals(normalizedServer)) {
            if (supportsValveShellTypes(toolType)) {
                return MemoryShellConstants.SHELLTYPES_SERVER_TOMCAT;
            }
            if (supportsJakartaShellTypes(toolType)) {
                return MemoryShellConstants.SHELLTYPES_SERVER_TOMCAT_JAKARTA;
            }
        }
        return MemoryShellConstants.SHELLTYPES;
    }

    private static boolean supportsValveShellTypes(String toolType) {
        if (toolType == null) {
            return true;
        }
        return MemoryShellConstants.TOOL_ANTSWORD.equals(toolType)
                || MemoryShellConstants.TOOL_BEHINDER.equals(toolType)
                || MemoryShellConstants.TOOL_GODZILLA.equals(toolType);
    }

    private static boolean supportsJakartaShellTypes(String toolType) {
        if (toolType == null) {
            return true;
        }
        return true;
    }

    public static String[] getOutputFormatsForServer(String serverType) {
        String normalizedServer = requireKnownServer(serverType);
        if (MemoryShellConstants.SERVER_TOMCAT.equals(normalizedServer)) {
            return MemoryShellConstants.OUTPUTFORMATS_SERVER_TOMCAT;
        } else if (MemoryShellConstants.SERVER_SPRING_MVC.equals(normalizedServer)) {
            return MemoryShellConstants.OUTPUTFORMATS_SERVER_SPRING_MVC;
        }
        return MemoryShellConstants.OUTPUTFORMATS;
    }

    private static String requireKnownServer(String serverType) {
        String normalized = normalize(serverType);
        if (contains(MemoryShellConstants.SERVERS_TOOL_CUSTOM, normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported server type: " + serverType);
    }

    private static String requireOneOf(String value, String label, String[] supportedValues) {
        String normalized = normalize(value);
        if (contains(supportedValues, normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported " + label + ": " + value);
    }

    private static boolean contains(String[] values, String value) {
        if (value == null) {
            return false;
        }
        for (String supportedValue : values) {
            if (supportedValue.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String getSimpleName(String className) {
        int lastDotIndex = className.lastIndexOf(".");
        if (lastDotIndex != -1 && lastDotIndex < className.length() - 1) {
            return className.substring(lastDotIndex + 1);
        }
        return className;
    }
}

package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.content.redTeam.memshell.memoryShell.antsword.*;
import com.potato.potatotool.content.redTeam.memshell.memoryShell.behinder.*;
import com.potato.potatotool.content.redTeam.memshell.memoryShell.godzilla.*;
import com.potato.potatotool.content.redTeam.memshell.memoryShell.neoregeorg.*;
import com.potato.potatotool.content.redTeam.memshell.memoryShell.suo5.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Potato
 * @date 2024/7/29 8:25
 */
public class MemoryShellUtil {

    private static final Map<String, Map<String, String>> TOOL_CLASSNAME_MAP = new HashMap();

    // TOOL_CLASSNAME_MAP={"Behinder":{"Filter":"...BehinderFilter"},}
    static {
        Map<String, String> Behinder_CLASSNAME_MAP = new HashMap();
        Behinder_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, BehinderFilter.class.getName());
        Behinder_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, BehinderListener.class.getName());
        Behinder_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_INTERCEPTOR, BehinderInterceptor.class.getName());

        TOOL_CLASSNAME_MAP.put(MemoryShellConstants.TOOL_BEHINDER, Behinder_CLASSNAME_MAP);


        Map<String, String> AntSword_CLASSNAME_MAP = new HashMap();
        AntSword_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, AntSwordFilter.class.getName());
        AntSword_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, AntSwordListener.class.getName());

        TOOL_CLASSNAME_MAP.put(MemoryShellConstants.TOOL_ANTSWORD, AntSword_CLASSNAME_MAP);


        Map<String, String> Godzilla_CLASSNAME_MAP = new HashMap();
        Godzilla_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, GodzillaFilter.class.getName());
        Godzilla_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, GodzillaListener.class.getName());
        Godzilla_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_INTERCEPTOR, GodzillaInterceptor.class.getName());
        Godzilla_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_WFHANDLERMETHOD, GodzillaWebFluxHandlerMethod.class.getName());

        TOOL_CLASSNAME_MAP.put(MemoryShellConstants.TOOL_GODZILLA, Godzilla_CLASSNAME_MAP);


        Map<String, String> Suo5_CLASSNAME_MAP = new HashMap();
        Suo5_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, Suo5Filter.class.getName());
        Suo5_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, Suo5Listener.class.getName());
        Suo5_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_INTERCEPTOR, Suo5Interceptor.class.getName());

        TOOL_CLASSNAME_MAP.put(MemoryShellConstants.TOOL_SUO5, Suo5_CLASSNAME_MAP);


        Map<String, String> NeoreGeorgCLASSNAME_MAP = new HashMap();
        NeoreGeorgCLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, NeoreGeorgFilter.class.getName());
        NeoreGeorgCLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, NeoreGeorgListener.class.getName());
        NeoreGeorgCLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_INTERCEPTOR, NeoreGeorgInterceptor.class.getName());

        TOOL_CLASSNAME_MAP.put(MemoryShellConstants.TOOL_NEOREGEORG, NeoreGeorgCLASSNAME_MAP);
    }

    // 根据 工具名称 及 shelll类型 获取对应 shell类名
    public static String getShellClassName(String toolType, String shellType) {
        return TOOL_CLASSNAME_MAP.get(toolType).get(shellType);
    }

}

package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.content.redTeam.memshell.injector.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Potato
 * @date 2024/7/29 11:25
 */
public class InjectorUtil {
    private static final Map<String, Map<String, String>> INJECTOR_CLASSNAME_MAP = new HashMap();

    // 初始化 INJECTOR_CLASSNAME_MAP 和 classMap，将不同服务器和 shell 类型对应的注入器类名进行映射。
    // INJECTOR_CLASSNAME_MAP={"Tomcat":{"Listener":"...TomcatListenerInjector ",},}
    static {
        Map<String, String> Glassfish_CLASSNAME_MAP = new HashMap();
        Glassfish_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, GlassFishListenerInjector.class.getName());
        Glassfish_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, GlassFishFilterInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_GLASSFISH, Glassfish_CLASSNAME_MAP);

        Map<String, String> Apusic_CLASSNAME_MAP = new HashMap();
        Apusic_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, ApusicListenerInjector.class.getName());
        Apusic_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, ApusicFilterInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_APUSIC, Apusic_CLASSNAME_MAP);

        Map<String, String> BES_CLASSNAME_MAP = new HashMap();
        BES_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, BESListenerInjector.class.getName());
        BES_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, BESFilterInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_BES, BES_CLASSNAME_MAP);

        Map<String, String> InforSuite_CLASSNAME_MAP = new HashMap();
        InforSuite_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, InforSuiteListenerInjector.class.getName());
        InforSuite_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, InforSuiteFilterInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_INFORSUITE, InforSuite_CLASSNAME_MAP);

        Map<String, String> TongWeb_CLASSNAME_MAP = new HashMap();
        TongWeb_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, TongWebListenerInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_TONGWEB, TongWeb_CLASSNAME_MAP);

        Map<String, String> Jetty_CLASSNAME_MAP = new HashMap();
        Jetty_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, JettyListenerInjector.class.getName());
        Jetty_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, JettyFilterInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_JETTY, Jetty_CLASSNAME_MAP);

        Map<String, String> Resin_CLASSNAME_MAP = new HashMap();
        Resin_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, ResinListenerInjector.class.getName());
        Resin_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, ResinFilterInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_RESIN, Resin_CLASSNAME_MAP);

        Map<String, String> Tomcat_CLASSNAME_MAP = new HashMap();
        Tomcat_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, TomcatListenerInjector.class.getName());
        Tomcat_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, TomcatFilterInjector.class.getName());
        Tomcat_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_JAKARTA_LISTENER, TomcatListenerInjector.class.getName());
        Tomcat_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_JAKARTA_FILTER, TomcatFilterInjector.class.getName());
        Tomcat_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_VALVE, TomcatValveInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_TOMCAT, Tomcat_CLASSNAME_MAP);

        Map<String, String> Undertow_CLASSNAME_MAP = new HashMap();
        Undertow_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, UndertowListenerInjector.class.getName());
        Undertow_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, UndertowFilterInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_UNDERTOW, Undertow_CLASSNAME_MAP);

        Map<String, String> WebLogic_CLASSNAME_MAP = new HashMap();
        WebLogic_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, WebLogicListenerInjector.class.getName());
        WebLogic_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, WebLogicFilterInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_WEBLOGIC, WebLogic_CLASSNAME_MAP);

        Map<String, String> WebSphere_CLASSNAME_MAP = new HashMap();
        WebSphere_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, WebSphereListenerInjector.class.getName());
        WebSphere_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, WebSphereFilterInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_WEBSPHERE, WebSphere_CLASSNAME_MAP);

        Map<String, String> JBoss_CLASSNAME_MAP = new HashMap();
        JBoss_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_LISTENER, TomcatListenerInjector.class.getName());
        JBoss_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_FILTER, TomcatFilterInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_JBOSS, JBoss_CLASSNAME_MAP);

        Map<String, String> SpringMVC_CLASSNAME_MAP = new HashMap();
        SpringMVC_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_INTERCEPTOR, SpringMVCInterceptorInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_SPRING_MVC, SpringMVC_CLASSNAME_MAP);

        Map<String, String> SpringWebFlux_CLASSNAME_MAP = new HashMap();
        SpringWebFlux_CLASSNAME_MAP.put(MemoryShellConstants.SHELLTYPE_WFHANDLERMETHOD, SpringWebFluxHandlerMethodInjector.class.getName());
        INJECTOR_CLASSNAME_MAP.put(MemoryShellConstants.SERVER_SPRING_WEBFLUX, SpringWebFlux_CLASSNAME_MAP);

    }

    // 根据 中间件/框架名称 及 shell类型 获取对应 注入器类名
    // serverType="Tomcat"  shellType="Listener"
    public static String getInjectorClassName(String serverType, String shellType) throws Exception {
        Map<String, String> injectorMap = INJECTOR_CLASSNAME_MAP.get(serverType);
        if (injectorMap == null) {
            throw new IllegalArgumentException("Unsupported server type: " + serverType);
        }
        String injectorClassName = injectorMap.get(shellType);
        if (injectorClassName == null) {
            throw new IllegalArgumentException("Unsupported shell type: " + serverType + "/" + shellType);
        }
        return injectorClassName;
    }

}

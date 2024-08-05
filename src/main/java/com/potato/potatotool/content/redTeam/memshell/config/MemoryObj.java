package com.potato.potatotool.content.redTeam.memshell.config;

import com.potato.potatotool.content.redTeam.memshell.factory.InjectorFactory;
import com.potato.potatotool.content.redTeam.memshell.factory.MemoryShellFactory;
import com.potato.potatotool.content.redTeam.memshell.util.ShowResultsUtil;

import java.util.HashMap;
import java.util.Map;

public class MemoryObj {

    private String injectorClassName;
    private String injectorSimpleClassName;
    private byte[] injectorBytes;
    private int injectorBytesLength;
    private boolean implementsASTTransformationType = false;
    private boolean implementsScriptEngineFactory = false;
    private String shellClassName;
    private String shellSimpleClassName;
    private byte[] shellBytes;
    private int shellBytesLength;
    private String shellGzipBase64String;
    private String urlPattern;
    private String outputFormat;
    private String savePath;
    private String pass;
    private String key;
    private String serverType;
    private String shellType;
    private String toolType;
    private String headerName;
    private String headerValue;
    private String methodBody;
    private String gadgetType;
    private Map result = new HashMap();
    private String exprEncoder;
    private String extenderSimpleClassName;
    private String loaderClassName;
    private String classFilePath;
    private Map<String, String> showResultMap;


    public String getInjectorClassName() {
        return injectorClassName;
    }

    public void setInjectorClassName(String injectorClassName) {
        this.injectorClassName = injectorClassName;
    }

    public String getInjectorSimpleClassName() {
        return injectorSimpleClassName;
    }

    public void setInjectorSimpleClassName(String injectorSimpleClassName) {
        this.injectorSimpleClassName = injectorSimpleClassName;
    }

    public byte[] getInjectorBytes() {
        return injectorBytes;
    }

    public void setInjectorBytes(byte[] injectorBytes) {
        this.injectorBytes = injectorBytes;
    }

    // TODO 非调试不需要
    public int getInjectorBytesLength() {
        return injectorBytesLength;
    }

    public void setInjectorBytesLength(int injectorBytesLength) {
        this.injectorBytesLength = injectorBytesLength;
    }

    public boolean isImplementsASTTransformationType() {
        return implementsASTTransformationType;
    }

    public void setImplementsASTTransformationType(boolean implementsASTTransformationType) {
        this.implementsASTTransformationType = implementsASTTransformationType;
    }

    public boolean isImplementsScriptEngineFactory() {
        return implementsScriptEngineFactory;
    }

    public void setImplementsScriptEngineFactory(boolean implementsScriptEngineFactory) {
        this.implementsScriptEngineFactory = implementsScriptEngineFactory;
    }

    public String getShellClassName() {
        return shellClassName;
    }

    public void setShellClassName(String shellClassName) {
        this.shellClassName = shellClassName;
    }

    public String getShellSimpleClassName() {
        return shellSimpleClassName;
    }

    public void setShellSimpleClassName(String shellSimpleClassName) {
        this.shellSimpleClassName = shellSimpleClassName;
    }

    public byte[] getShellBytes() {
        return shellBytes;
    }

    public void setShellBytes(byte[] shellBytes) {
        this.shellBytes = shellBytes;
    }

    // TODO 非调试不需要
    public int getShellBytesLength() {
        return shellBytesLength;
    }

    public void setShellBytesLength(int shellBytesLength) {
        this.shellBytesLength = shellBytesLength;
    }

    public String getShellGzipBase64String() {
        return shellGzipBase64String;
    }

    public void setShellGzipBase64String(String shellGzipBase64String) {
        this.shellGzipBase64String = shellGzipBase64String;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getShellType() {
        return shellType;
    }

    public void setShellType(String shellType) {
        this.shellType = shellType;
    }

    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderValue() {
        return headerValue;
    }

    public void setHeaderValue(String headerValue) {
        this.headerValue = headerValue;
    }

    public String getMethodBody() {
        return methodBody;
    }

    public void setMethodBody(String methodBody) {
        this.methodBody = methodBody;
    }

    public String getGadgetType() {
        return gadgetType;
    }

    public void setGadgetType(String gadgetType) {
        this.gadgetType = gadgetType;
    }

    public Map getResult() {
        return result;
    }

    public void setResult(Map result) {
        this.result = result;
    }

    public String getExprEncoder() {
        return exprEncoder;
    }

    public void setExprEncoder(String exprEncoder) {
        this.exprEncoder = exprEncoder;
    }

    public String getExtenderSimpleClassName() {
        return extenderSimpleClassName;
    }

    public void setExtenderSimpleClassName(String extenderSimpleClassName) {
        this.extenderSimpleClassName = extenderSimpleClassName;
    }

    public String getLoaderClassName() {
        return loaderClassName;
    }

    public void setLoaderClassName(String loaderClassName) {
        this.loaderClassName = loaderClassName;
    }

    public String getClassFilePath() {
        return classFilePath;
    }

    public void setClassFilePath(String classFilePath) {
        this.classFilePath = classFilePath;
    }


    public Map<String, String> getShowResultMap() {
        return showResultMap;
    }

    public void setShowResultMap(Map<String, String> resultShowMap) {
        this.showResultMap = resultShowMap;
    }


    public void buildMemoryShellAndInjector() throws Exception {
        new MemoryShellFactory().generateShell(this);
        new InjectorFactory().generateInjector(this);
        this.setShowResultMap( ShowResultsUtil.generateShowResultMap(this) );
    }

}

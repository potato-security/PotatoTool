package com.potato.potatotool.content.redTeam.vulnScanner.classObj;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Potato
 * @date 2025/3/12 15:44
 */
public class XrayYamlObj {

    @Data
    public static class Poc {
        private String name;
        private boolean manual;
        private String query;
        private String transport;
        private LinkedHashMap<String, Object> set = new LinkedHashMap<>();
        private Payloads payloads;
        private LinkedHashMap<String, Object> rules = new LinkedHashMap<>();
        private String expression;
        private Detail detail;
    }

    @Data
    public static class Rule {
        private RuleRequest request;
        private String expression;
        private LinkedHashMap<String, String> output = new LinkedHashMap<>();
    }

    @Data
    public static class RuleMapItem {
        private String key;
        private Rule value;
    }

    @Data
    public static class VariableMapItem {
        private String key;
        private String value;
    }

    @Data
    public static class RuleRequest {
        private boolean cache;
        private String method;
        private String path;
        private LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        private String body;
        private boolean follow_redirects;
        private String content;
        private String readTimeout;
        private String connectionID;
    }

    @Data
    public static class Payloads {
        private boolean continue_;
        private LinkedHashMap<String, Object> payloads = new LinkedHashMap<>();
    }

    @Data
    public static class Detail {
        private String author;
        private List<String> links;
        private FingerPrint fingerPrint;
        private Vulnerability vulnerability;
        private String description;
        private String version;
        private String tags;
    }

    @Data
    public static class FingerPrint {
        private List<Infos> infos;
        private HostInfo hostInfo;
    }

    @Data
    public static class Infos {
        private String id;
        private String name;
        private String version;
        private String type;
        private int confidence;
    }

    @Data
    public static class HostInfo {
        private String hostname;
    }

    @Data
    public static class Vulnerability {
        private String id;
        private String match;
    }
}

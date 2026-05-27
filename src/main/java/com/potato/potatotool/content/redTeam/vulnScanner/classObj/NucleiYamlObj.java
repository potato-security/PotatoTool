package com.potato.potatotool.content.redTeam.vulnScanner.classObj;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

import java.util.*;

/**
 * @author Potato
 * @date 2025/3/12 15:44
 */
public class NucleiYamlObj {

    @Data
    public static class Poc {
        private String id;
        private Info info;
        private Map<String, String> variables;
        private String flow;    // http(1) && http(2)
        private boolean self_contained;
        private List<Http> http;
        private List<Request> requests;
        private List<Tcp> tcp;
        private List<Tcp> network;  // network 协议（与 tcp 协议结构兼容）
        private List<Dns> dns;
        private List<WebSocket> websocket;
        private List<Ssl> ssl;
        private List<FileProtocol> file;
        private List<Headless> headless;
        private List<Code> code;
        private List<Code> javascript;  // JavaScript协议（与code协议相同结构）
    }

    @Data
    public static class Info {
        private String name;
        private String author;
        private Severity severity = Severity.unknown;;
        private String description;
        private Object reference;   // List<String> Or String
        private Classification classification;
        private Metadata metadata;
        private String tags;

        public enum Severity {
            info, low, medium, high, critical, unknown;
        }

    }

    @Data
    public static class Metadata {
        private int max_request;
        private Object shodan_query;   // List<String> Or String
        private Object fofa_query;
        private Object google_query;
        private Object publicwww_query;
        private Object zoomeye_query;
        private String vendor;
        private String product;
        
        // 兼容 YAML 连字符格式字段名
        public void setMaxRequest(int maxRequest) { this.max_request = maxRequest; }
        public void setShodanQuery(Object shodanQuery) { this.shodan_query = shodanQuery; }
        public void setFofaQuery(Object fofaQuery) { this.fofa_query = fofaQuery; }
        public void setGoogleQuery(Object googleQuery) { this.google_query = googleQuery; }
        public void setPublicwwwQuery(Object publicwwwQuery) { this.publicwww_query = publicwwwQuery; }
        public void setZoomeyeQuery(Object zoomeyeQuery) { this.zoomeye_query = zoomeyeQuery; }
    }

    @Data
    public static class Classification {
        private String cvss_metrics;
        private String cvss_score;
        private String cve_id;
        private String cwe_id;
        
        // 兼容 YAML 连字符格式字段名
        public void setCvssMetrics(String cvssMetrics) { this.cvss_metrics = cvssMetrics; }
        public void setCvssScore(String cvssScore) { this.cvss_score = cvssScore; }
        public void setCveId(String cveId) { this.cve_id = cveId; }
        public void setCweId(String cweId) { this.cwe_id = cweId; }
    }

    public enum Part {
        header, body, all, raw, data, interactsh_protocol, interactsh_request, ehlo
    }

    public enum Condition {
        and, or
    }

    public enum MatchersCondition {
        and, or
    }

    @Data
    public static class Http {
        private String id;
        private boolean global_matchers;
        private Map<String, Object> payloads;   // Map<String, List<String>> Or Map<String, String>
        private PocObj.VariablesType attack;
        private boolean cookie_reuse;
        private boolean disable_cookie;
        private boolean disable_path_automerge;
        private String digest_username;
        private String digest_password;
        private int threads;
        private String method;
        private boolean redirects;
        private List<String> path;
        private Map<String, String> headers;
        private boolean skip_variables_check;
        private boolean stop_at_first_match;
        private boolean unsafe;
        private boolean read_all;
        private boolean iterate_all;
        private List<String> raw;
        private MatchersCondition matchers_condition;
        private List<TemplateMatcher> matchers;
        private List<TemplateMatcher> extractors;
    }

    @Data
    public static class Request {
        private String id;
        private boolean global_matchers;
        private Map<String, Object> payloads;   // Map<String, List<String>> Or Map<String, String>
        private PocObj.VariablesType attack;
        private boolean cookie_reuse;
        private boolean disable_cookie;
        private boolean disable_path_automerge;
        private String digest_username;
        private String digest_password;
        private int threads;
        private String method;
        private boolean redirects;
        private List<String> path;
        private Map<String, String> headers;
        private boolean skip_variables_check;
        private boolean stop_at_first_match;
        private boolean unsafe;
        private boolean read_all;
        private boolean iterate_all;
        private List<String> raw;
        private MatchersCondition matchers_condition;
        private List<TemplateMatcher> matchers;
        private List<TemplateMatcher> extractors;
    }


    @Data
    public static class Tcp {
        @Setter(AccessLevel.NONE)
        private List<String> host;
        private List<Input> inputs;
        private String port; // 6379,6380
        private Map<String, Object> payloads;   // Map<String, List<String>> Or Map<String, String>
        private PocObj.VariablesType attack;
        private boolean stop_at_first_match;
        private int read_size;
        private MatchersCondition matchers_condition;
        private List<TemplateMatcher> matchers;
        private List<TemplateMatcher> extractors;

        // 支持 YAML 中 host 为字符串的情况
        public void setHost(String hostStr) {
            this.host = Collections.singletonList(hostStr);
        }

        // 支持 YAML 中 host 为列表的情况
        public void setHost(List<String> hostList) {
            this.host = hostList;
        }
    }

    public enum InputType {
        hex
    }

    @Data
    public static class Input {
        private String data;
        private InputType type;
        private String name;
        private int read;
    }

    public interface TemplateMatcher {
        String getType();
//        Part getPart();
//        void setPart(Part part);
        Condition getCondition();
        void setCondition(Condition condition);
    }

    @Data
    public static class Status implements TemplateMatcher {
        private String type = "status";
        private List<Integer> status;
        private Condition condition;
        private String name;
    }

    @Data
    public static class Size implements TemplateMatcher {
        private String type = "size";
        private List<Integer> size;
        private Condition condition;
        private String name;
    }

    @Data
    public static class Word implements TemplateMatcher {
        private String type = "word";
        private String encoding;
        private boolean negative;
//        private Part part = Part.all;
        private String part = "all";
        private List<String> words;
        private Condition condition;
        private boolean case_insensitive;
        private String name;
    }

    @Data
    public static class Binary implements TemplateMatcher {
        private String type = "binary";
//        private Part part = Part.body;
        private String part = "body";
        private List<String> binary;
        private Condition condition;
        private String name;
    }

    @Data
    public static class Dsl implements TemplateMatcher {
        private String type = "dsl";
        private List<String> dsl;
        private Condition condition;
        private boolean internal;
        private String name;
    }

    @Data
    public static class Time implements TemplateMatcher {
        private String type = "time";
        private List<String> time;
        private Condition condition;
        private String name;
    }

    @Data
    public static class Regex implements TemplateMatcher {
        private String type = "regex";
//        private Part part = Part.body;
        private String part = "body";
        private List<String> regex;
        private Condition condition;

        private int group = 1;
        private boolean internal = true;
        private String name;
    }

    @Data
    public static class Json implements TemplateMatcher {
        private String type = "json";
        private String part = "body";
        private List<String> json;
        private Condition condition;
        private boolean negative;
        private int group = 1;
        private boolean internal;
        private String name;
    }

    @Data
    public static class Kval implements TemplateMatcher {
        private String type = "kval";
        private List<String> kval;
        private String part = "all";
        private Condition condition;
        private boolean negative;
        private boolean internal;
        private String name;
    }

    @Data
    public static class Xpath implements TemplateMatcher {
        private String type = "xpath";
        private String part = "body";
        private List<String> xpath;
        private String attribute;
        private Condition condition;
        private boolean negative;
        private boolean internal;
        private String name;
    }

    /**
     * DNS 协议配置
     * 根据Nuclei官方文档，name字段只接受单个字符串，不是列表
     * 如果需要查询多个域名，应该创建多个DNS请求块
     */
    @Data
    public static class Dns {
        private String name;  // 单个域名字符串，如 "{{FQDN}}" 或 "example.com"
        private String type = "A";
        private String dns_class = "INET";
        private boolean recursion = true;
        private int retries = 2;
        private String resolvers;
        private MatchersCondition matchers_condition;
        private List<TemplateMatcher> matchers = new ArrayList<>();
        private List<TemplateMatcher> extractors = new ArrayList<>();
    }

    /**
     * WebSocket 协议配置
     */
    @Data
    public static class WebSocket {
        private String address;
        private Map<String, String> headers;
        private List<WebSocketInput> inputs;
        private int attack;
        private Map<String, Object> payloads;
        private MatchersCondition matchers_condition;
        private List<TemplateMatcher> matchers;
        private List<TemplateMatcher> extractors;
    }

    @Data
    public static class WebSocketInput {
        private String data;
        private String name;
    }

    /**
     * SSL/TLS 协议配置
     */
    @Data
    public static class Ssl {
        private String address;
        private MatchersCondition matchers_condition;
        private List<TemplateMatcher> matchers;
        private List<TemplateMatcher> extractors;
    }

    /**
     * File 协议配置
     */
    @Data
    public static class FileProtocol {
        private List<String> extensions;
        private List<String> paths;
        private boolean recursive = false;
        private int max_size = 104857600;
        private MatchersCondition matchers_condition;
        private List<TemplateMatcher> matchers;
        private List<TemplateMatcher> extractors;
    }

    /**
     * Headless 协议配置
     */
    @Data
    public static class Headless {
        private List<HeadlessStep> steps;
        private MatchersCondition matchers_condition;
        private List<TemplateMatcher> matchers;
        private List<TemplateMatcher> extractors;
    }

    /**
     * Headless 操作类型枚举
     */
    public enum HeadlessAction {
        navigate, waitload, script, click, input, screenshot, sleep, waitvisible,
        waitdialog, setheader, text, waitdom
    }

    @Data
    public static class HeadlessStep {
        private String action;
        private String name;
        private Map<String, String> args;
    }

    /**
     * Code 协议配置
     * 注意：Nuclei YAML 使用 'code:' 字段，需要兼容映射到 source
     */
    @Data
    public static class Code {
        private List<String> engine;
        private Object args;
        private Object pattern;
        private String source;
        private MatchersCondition matchers_condition;
        private List<TemplateMatcher> matchers;
        private List<TemplateMatcher> extractors;
        
        /**
         * 兼容 Nuclei YAML 中的 'code' 字段
         * 将 code 字段值映射到 source
         */
        public void setCode(String code) {
            this.source = code;
        }
        
        /**
         * 获取代码（优先返回 source）
         */
        public String getCode() {
            return this.source;
        }

        public List<String> getPatternList() {
            List<String> result = new ArrayList<>();
            if (this.pattern == null) {
                return result;
            }
            if (this.pattern instanceof Collection) {
                for (Object item : (Collection<?>) this.pattern) {
                    if (item != null) {
                        result.add(String.valueOf(item));
                    }
                }
            } else {
                result.add(String.valueOf(this.pattern));
            }
            return result;
        }
    }

    /**
     * Code 引擎类型枚举
     */
    public enum CodeEngine {
        javascript, python
    }
}

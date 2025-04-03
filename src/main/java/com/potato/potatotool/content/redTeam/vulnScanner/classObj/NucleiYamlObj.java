package com.potato.potatotool.content.redTeam.vulnScanner.classObj;

import lombok.Data;

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

//            public static Severity of(String value) {
//                return Arrays.stream(values())
//                        .filter(severity -> severity.toString().equals(value.toLowerCase()))
//                        .findAny()
//                        .orElse(unknown);
//            }
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
    }

    @Data
    public static class Classification {
        private String cvss_metrics;
        private String cvss_score;
        private String cve_id;
        private String cwe_id;
    }

    public enum Part {
        header, body, all, raw, data, interactsh_protocol, interactsh_request, ehlo
    }
    // 可能来自于poc中其他name字段

    public enum Condition {
        and, or
    }

    public enum MatchersCondition {
        and, or
    }

    public enum AttackType {
        sniper, batteringram, pitchfork, clusterbomb
    }

    @Data
    public static class Http {
        private boolean global_matchers;
        private Map<String, Object> payloads;   // Map<String, List<String>> Or Map<String, String>
        private AttackType attack;
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
        private boolean global_matchers;
        private Map<String, Object> payloads;   // Map<String, List<String>> Or Map<String, String>
        private AttackType attack;
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
        private List<String> host;
        private List<Input> inputs;
        private String port; // 6379,6380
        private Map<String, Object> payloads;   // Map<String, List<String>> Or Map<String, String>
        private AttackType attack;
        private int read_size;
        private MatchersCondition matchers_condition;
        private List<TemplateMatcher> matchers;
        private List<TemplateMatcher> extractors;
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
    }

    @Data
    public static class Binary implements TemplateMatcher {
        private String type = "binary";
//        private Part part = Part.body;
        private String part = "body";
        private List<String> binary;
        private Condition condition;
    }

    @Data
    public static class Dsl implements TemplateMatcher {
        private String type = "dsl";
        private String name;
        private List<String> dsl;
        private Condition condition;
    }

    @Data
    public static class Regex implements TemplateMatcher {
        private String type = "regex";
//        private Part part = Part.body;
        private String part = "body";
        private List<String> regex;
        private Condition condition;

        private int group = 1;
        private String name;
        private boolean internal = true;
    }

    @Data
    public static class Json implements TemplateMatcher {
        private String type = "json";
        private List<String> json;
        private Condition condition;
    }

    @Data
    public static class Kval implements TemplateMatcher {
        private String type = "kval";
        private List<String> kval;
        private Condition condition;
    }

    @Data
    public static class Xpath implements TemplateMatcher {
        private String type = "xpath";
        private List<String> xpath;
        private String attribute;
        private Condition condition;
    }
}

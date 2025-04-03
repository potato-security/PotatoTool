package com.potato.potatotool.content.redTeam.vulnScanner.classObj;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Potato
 * @date 2025/3/12 17:17
 */
public class GobyJsonObj {
    @Data
    public static class PocJson {
        private String Name;
        private String Level;
        private List<String> Tags;
        private String GobyQuery;
        private String Description;
        private String Product;
        private String Homepage;
        private String Author;
        private String Impact;
        private String Recommendation;
        // 网上很多Poc错写成法语单词Recommandation
        private String Recommandation;
        private List<String> References;
        private boolean HasExp;
        private List<ExpParam> ExpParams;
        private List<Object> ScanSteps;
        private List<Object> ExploitSteps;
        private String PostTime;
        private String GobyVersion;
    }

    @Data
    public static class ExpParam {
        private String Name;
        private String Type;
        private String Value;
        private String Show;
    }

    @Data
    public static class Check {
        private String type;    // 类型：item 或 group
        private String variable;
        private String operation;   // 操作类型：Contains, Not Contains, Regex, Start With, End With, ==, !=, >, <
        private String value;
        private String bz;
        private List<Check> checks; // 当type为group时的子检查项
    }

    @Data
    public static class Request {
        private String method;
        private String uri;
        private boolean follow_redirect;
        private LinkedHashMap<String, String> header = new LinkedHashMap<>();
        private String data_type;
        private String data;
        private List<String> set_variable;
    }

    @Data
    public static class ResponseTest {
        private String type;
        private String operation;
        private List<Check> checks;
    }

    @Data
    public static class ScanStep {
        private Request Request;
        private ResponseTest ResponseTest;
        private List<String> SetVariable;
    }
}

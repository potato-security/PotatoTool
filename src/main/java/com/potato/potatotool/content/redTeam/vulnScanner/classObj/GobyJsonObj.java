package com.potato.potatotool.content.redTeam.vulnScanner.classObj;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Potato
 * @date 2025/2/12 17:17
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
        
        // 全局变量 - 在所有步骤间共享（Phase 2新增）
        private Map<String, String> GlobalVariables;
        
        // 认证配置 - 用于需要认证的POC（Phase 3新增）
        private Authentication Authentication;
    }
    
    /**
     * 认证配置
     * 支持多种认证方式：Basic、Bearer、Digest等
     */
    @Data
    public static class Authentication {
        private String type;          // 认证类型: basic, bearer, digest, cookie
        private String username;      // 用户名(用于basic/digest)
        private String password;      // 密码(用于basic/digest)
        private String token;         // Token(用于bearer)
        private Map<String, String> cookies;  // Cookie(用于cookie认证)
    }

    @Data
    public static class ExpParam {
        private String Name;
        private String Type;
        private String Value;
        private String Show;
        
        // 小写字段，用于兼容不同格式的JSON
        private String name;
        private String type;
        private String value;
        private String show;
        
        // 重写getter方法以处理大小写混用情况
        public String getName() {
            return name != null ? name : Name;
        }
        
        public String getType() {
            return type != null ? type : Type;
        }
        
        public String getValue() {
            return value != null ? value : Value;
        }
        
        public String getShow() {
            return show != null ? show : Show;
        }
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
        private String uri;                    // 单个路径
        private List<String> uris;              // 多路径支持（Goby官方支持）
        private String raw;                     // 原始HTTP报文（Phase 2新增）
        private boolean follow_redirect;
        private LinkedHashMap<String, String> header = new LinkedHashMap<>();
        private String data_type;
        private String data;
        private List<String> set_variable;
        private Map<String, String> cookies;    // Cookie配置（Phase 3新增）
        
        /**
         * 获取所有URI路径（兼容单路径和多路径）
         * @return URI列表
         */
        public List<String> getAllUris() {
            List<String> allUris = new ArrayList<>();
            // 优先使用uris数组
            if (uris != null && !uris.isEmpty()) {
                allUris.addAll(uris);
            } 
            // 否则使用单个uri
            else if (uri != null && !uri.isEmpty()) {
                allUris.add(uri);
            }
            return allUris;
        }
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

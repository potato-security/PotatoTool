package com.potato.potatotool.content.redTeam.vulnScanner.classObj;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Potato
 * @date 2025/3/19 15:44
 */
public class PocsuiteJsonObj {

    @Data
    public static class PocJson {
        private PocInfo pocInfo;
        private PocExecute pocExecute;
    }

    @Data
    public static class PocInfo {
        private String vulID;
        private String version;
        private String vulDate;
        private String createDate;
        private String updateDate;
        private String name;
        private String protocol;
        private String vulType;
        private String author;
        private List<String> references;
        private String appName;
        private String appVersion;
        private String appPowerLink;
        private String desc;
        private List<String> samples;
    }

    @Data
    public static class PocExecute {
        private List<Step> verify;
        private List<Step> attack;
    }

    @Data
    public static class Step {
        private String step;
        private String method;
        private String vulPath;
        private String params;
        private String necessary;
        private LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        private String status;
        private Match match;
        private Map<String, Object> result;
    }

    @Data
    public static class Match {
        private List<String> regex;
        private String time;
    }

}
package com.potato.potatotool.content.blueTeam.webshell.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 解密配置类
 * 封装解密过程中需要的各种配置参数
 * 
 * @author Potato
 * @version 2.0
 */
public class DecryptConfig {
    
    private String inputKey;
    private String inputIv;
    private List<String> traverseList;
    private String customPath;
    
    private DecryptConfig() {
        this.traverseList = new ArrayList<>();
    }
    
    public static DecryptConfig defaultConfig() {
        return new DecryptConfig();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getInputKey() {
        return inputKey;
    }
    
    public String getInputIv() {
        return inputIv;
    }
    
    public List<String> getTraverseList() {
        return traverseList;
    }
    
    public String getCustomPath() {
        return customPath;
    }
    
    // Builder pattern
    public static class Builder {
        private DecryptConfig config;
        
        public Builder() {
            this.config = new DecryptConfig();
        }
        
        public Builder inputKey(String inputKey) {
            config.inputKey = inputKey;
            return this;
        }
        
        public Builder inputIv(String inputIv) {
            config.inputIv = inputIv;
            return this;
        }
        
        public Builder traverseList(List<String> traverseList) {
            config.traverseList = traverseList != null ? new ArrayList<>(traverseList) : new ArrayList<>();
            return this;
        }
        
        public Builder customPath(String customPath) {
            config.customPath = customPath;
            return this;
        }
        
        public DecryptConfig build() {
            return config;
        }
    }
}
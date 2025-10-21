package com.potato.potatotool.update.manifest;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.potato.potatotool.utils.core.Constants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 清单获取器 - 从多个源获取更新清单
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class ManifestFetcher {
    
    private static final int CONNECT_TIMEOUT = 3000;  // 3秒连接超时（优化启动速度）
    private static final int READ_TIMEOUT = 5000;     // 5秒读取超时（优化启动速度）
    
    private final Gson gson = new Gson();
    private final List<String> manifestSources;
    
    public ManifestFetcher() {
        this.manifestSources = new ArrayList<>();
        
        // 从配置文件读取清单源
        try {
            String githubUrl = Constants.getConfigInfo("manifestUrlGithub");
            String mirrorUrl = Constants.getConfigInfo("manifestUrlMirror");
            
            if (githubUrl != null && !githubUrl.isEmpty()) {
                manifestSources.add(githubUrl);
            }
            if (mirrorUrl != null && !mirrorUrl.isEmpty()) {
                manifestSources.add(mirrorUrl);
            }
            
            if (manifestSources.isEmpty()) {
                throw new RuntimeException("未配置清单源URL");
            }
            
        } catch (Exception e) {
            System.err.println("初始化清单源失败: " + e.getMessage());
            // 使用硬编码的默认值
            manifestSources.add("https://raw.githubusercontent.com/HotBoy-java/Resource/main/manifest.json");
            manifestSources.add("https://potato.gold/data/uploads/PotatoTool/manifest.json");
        }
    }
    
    /**
     * 获取更新清单
     * 多源策略：依次尝试所有源，直到成功
     * 
     * @return Manifest对象
     * @throws Exception 如果所有源都失败
     */
    public Manifest fetchManifest() throws Exception {
        List<Exception> errors = new ArrayList<>();
        
        for (int i = 0; i < manifestSources.size(); i++) {
            String source = manifestSources.get(i);
            try {
                if (debugMode) System.out.println("尝试从源 " + (i + 1) + "/" + manifestSources.size() + " 获取清单...");
                
                Manifest manifest = fetchFromSource(source);
                
                if (debugMode) System.out.println("成功获取更新清单");
                return manifest;
                
            } catch (Exception e) {
                System.err.println("获取失败，尝试下一个源...");
                errors.add(e);
            }
        }
        
        // 所有源都失败
        Exception lastError = errors.isEmpty() ? 
                new Exception("未配置清单源") : 
                errors.get(errors.size() - 1);
        throw new Exception("无法从任何源获取更新清单", lastError);
    }
    
    /**
     * 从指定源获取清单
     */
    private Manifest fetchFromSource(String urlString) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            
            // 设置请求参数
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("User-Agent", "PotatoTool/2.5");
            conn.setRequestProperty("Accept", "application/json");
            
            // 检查响应码
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("HTTP错误: " + responseCode);
            }
            
            // 读取响应
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }
            
            String jsonContent = response.toString();
            
            // 解析JSON
            try {
                Manifest manifest = gson.fromJson(jsonContent, Manifest.class);
                
                // 验证清单有效性
                if (!validateManifest(manifest)) {
                    throw new Exception("清单格式无效");
                }
                
                return manifest;
                
            } catch (JsonSyntaxException e) {
                throw new Exception("JSON解析失败: " + e.getMessage(), e);
            }
            
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * 验证清单的有效性
     */
    private boolean validateManifest(Manifest manifest) {
        if (manifest == null) {
            System.err.println("清单为null");
            return false;
        }
        
        if (manifest.getVersion() == null || manifest.getVersion().isEmpty()) {
            System.err.println("清单版本为空");
            return false;
        }
        
        if (manifest.getApp() == null) {
            System.err.println("清单缺少app节点");
            return false;
        }
        
        if (manifest.getApp().getVersion() == null || manifest.getApp().getVersion().isEmpty()) {
            System.err.println("app版本为空");
            return false;
        }
        
        if (manifest.getResources() == null) {
            System.err.println("清单缺少resources节点");
            return false;
        }
        
        return true;
    }
    
    /**
     * 添加自定义清单源
     */
    public void addManifestSource(String url) {
        if (url != null && !url.isEmpty() && !manifestSources.contains(url)) {
            manifestSources.add(url);
        }
    }
    
    /**
     * 获取所有配置的清单源
     */
    public List<String> getManifestSources() {
        return new ArrayList<>(manifestSources);
    }
    
    /**
     * 测试指定URL的可达性
     * @return 响应时间（毫秒），-1表示不可达
     */
    public long testSourceReachability(String urlString) {
        HttpURLConnection conn = null;
        try {
            long startTime = System.currentTimeMillis();
            
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            int responseCode = conn.getResponseCode();
            
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (responseCode == 200 || responseCode == 301 || responseCode == 302) {
                return elapsed;
            } else {
                return -1;
            }
            
        } catch (Exception e) {
            return -1;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}


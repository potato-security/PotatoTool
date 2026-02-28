package com.potato.potatotool.vulnScanner.testlab;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * newPoc 测试服务器
 * 为 newPoc 目录下的各类 POC 提供正向（漏洞）和反向（安全）的测试端点
 * 
 * @author Potato
 */
public class NewPocTestServer {
    
    private RawHttpServer rawServer;
    private int port;
    
    // 测试模式：用于区分 vuln 和 safe 测试（解决 OkHttp URL 规范化问题）
    private static volatile boolean vulnerableMode = true;
    
    public NewPocTestServer(int port) {
        this.port = port;
    }
    
    /**
     * 设置测试模式
     * @param vulnerable true=漏洞模式（返回漏洞响应），false=安全模式（返回安全响应）
     */
    public static void setVulnerableMode(boolean vulnerable) {
        vulnerableMode = vulnerable;
    }
    
    /**
     * 获取当前测试模式
     */
    public static boolean isVulnerableMode() {
        return vulnerableMode;
    }
    
    public void start() throws IOException {
        rawServer = new RawHttpServer(port);
        
        // 注册所有测试处理器
        registerGobyPocHandlers();
        registerNucleiPocHandlers();
        registerXrayPocHandlers();
        registerPocsuitePocHandlers();
        
        // 默认处理器 - 添加调试输出
        rawServer.setDefaultHandler((req, resp) -> {
            String path = req.getRawPath();
            String method = req.getMethod();
            String query = req.getQuery();
            System.out.println("[DEBUG] 未匹配请求: " + method + " " + path + (query != null ? "?" + query : "") + " (mode=" + (isVulnerableMode() ? "vuln" : "safe") + ")");
            resp.setStatus(404);
            resp.setContentType("text/plain");
            resp.setBody("Not Found: " + path);
        });
        
        rawServer.start();
        System.out.println("[NewPocTestServer] 启动在端口: " + port);
    }
    
    public void stop() {
        if (rawServer != null) {
            rawServer.stop();
        }
    }
    
    // ====================================
    // Goby POC Handlers
    // ====================================
    private void registerGobyPocHandlers() {
        // 01_basic_get_and_contains: 360 天擎 SQL 注入
        // 正向: GET /api/dp/rptsvcsyncpoint?ccid=1 返回 200 + result + success（不含 10001）
        rawServer.createContext("/vuln/goby/01/api/dp/rptsvcsyncpoint", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"result\": \"query_result\", \"success\": true, \"data\": []}");
        });
        // 反向: 返回包含 10001 错误码
        rawServer.createContext("/safe/goby/01/api/dp/rptsvcsyncpoint", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"result\": \"error\", \"code\": 10001, \"success\": false}");
        });
        
        // 02_post_custom_header: POST 请求 + 自定义 Header
        rawServer.createContext("/vuln/goby/02/test", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("<html><body>Custom Header Test Success</body></html>");
        });
        rawServer.createContext("/safe/goby/02/test", (req, resp) -> {
            resp.setStatus(403);
            resp.setContentType("text/plain");
            resp.setBody("Forbidden");
        });
        
        // 03_or_condition_multi_step: Apache RCE CVE-2021-41773
        // POC 路径: /cgi-bin/.%2e/.%2e/.%2e/.%2e/.%2e/bin/sh 或 /cgi-bin/.%%32%65 ...
        // OkHttp 规范化路径，使用测试模式状态
        RawHttpServer.RawHttpHandler goby03ModeHandler = (req, resp) -> {
            System.out.println("[DEBUG] Goby03 处理器被调用: " + req.getMethod() + " " + req.getRawPath() + " mode=" + (isVulnerableMode() ? "vuln" : "safe"));
            if (isVulnerableMode()) {
                // 获取请求体中的 echo 参数
                byte[] bodyBytes = req.getBody();
                String body = bodyBytes != null ? new String(bodyBytes) : "";
                System.out.println("[DEBUG] Goby03 请求体: " + body);
                String output = "12345678"; // 默认随机数
                // POC 格式: echo;echo {xxx} 或 echo;echo xxx
                // 需要提取花括号内的内容或最后一个 echo 后的内容
                if (body != null && body.contains("echo")) {
                    // 查找 {xxx} 格式
                    int braceStart = body.indexOf("{");
                    int braceEnd = body.indexOf("}");
                    if (braceStart >= 0 && braceEnd > braceStart) {
                        output = body.substring(braceStart + 1, braceEnd);
                    } else {
                        // 提取最后一个 echo 后面的内容
                        int lastEcho = body.lastIndexOf("echo");
                        if (lastEcho >= 0) {
                            output = body.substring(lastEcho + 4).trim();
                        }
                    }
                }
                System.out.println("[DEBUG] Goby03 返回: " + output);
                resp.setStatus(200);
                resp.setContentType("text/plain");
                resp.setBody(output);
            } else {
                resp.setStatus(403);
                resp.setContentType("text/plain");
                resp.setBody("Forbidden");
            }
        };
        rawServer.createContext("/bin/sh", goby03ModeHandler); // OkHttp 规范化后的路径
        rawServer.createContext("/vuln/goby/03", goby03ModeHandler);
        rawServer.createContext("/safe/goby/03", goby03ModeHandler);
        
        // 04_regex_operation: Apache 路径遍历 (regex 操作)
        // POC 路径: /cgi-bin/.%2e/%2e%2e/%2e%2e/%2e%2e/etc/passwd
        // OkHttp 会规范化路径，导致无法通过路径区分 vuln/safe，使用测试模式状态
        RawHttpServer.RawHttpHandler goby04ModeHandler = (req, resp) -> {
            if (isVulnerableMode()) {
                resp.setStatus(200);
                resp.setContentType("text/plain");
                resp.setBody("root:x:0:0:root:/root:/bin/bash\ndaemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin");
            } else {
                resp.setStatus(404);
                resp.setContentType("text/plain");
                resp.setBody("Not Found");
            }
        };
        rawServer.createContext("/etc/passwd", goby04ModeHandler); // OkHttp 规范化后的路径
        rawServer.createContext("/vuln/goby/04", goby04ModeHandler);
        rawServer.createContext("/safe/goby/04", goby04ModeHandler);
        
        // 05_no_redirect_md5: 不跟随重定向 + MD5 验证
        rawServer.createContext("/vuln/goby/05/test", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("d41d8cd98f00b204e9800998ecf8427e"); // MD5 of empty string
        });
        rawServer.createContext("/safe/goby/05/test", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("invalid_hash");
        });
        
        // 06_status_302_redirect: Apache SSRF CVE-2021-40438
        // POC 检查: status == 302 && header 包含 "http://www.baidu.com/search/error.html"
        // 路径很长，使用前缀匹配
        rawServer.createContext("/vuln/goby/06", (req, resp) -> {
            resp.setStatus(302);
            resp.setContentType("text/html");
            resp.setHeader("Location", "http://www.baidu.com/search/error.html");
            resp.setBody("Redirecting...");
        });
        rawServer.createContext("/safe/goby/06", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("Login Page");
        });
        
        // 07_status_500_error: 500 错误检测
        rawServer.createContext("/vuln/goby/07/error", (req, resp) -> {
            resp.setStatus(500);
            resp.setContentType("text/plain");
            resp.setBody("Internal Server Error: SQL syntax error");
        });
        rawServer.createContext("/safe/goby/07/error", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("OK");
        });
        
        // 08_basic_auth_not_equal: Basic Auth + not equal 操作
        rawServer.createContext("/vuln/goby/08/admin", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("<html><body>Admin Panel</body></html>");
        });
        rawServer.createContext("/safe/goby/08/admin", (req, resp) -> {
            resp.setStatus(401);
            resp.setContentType("text/html");
            resp.setHeader("WWW-Authenticate", "Basic realm=\"Admin\"");
            resp.setBody("Unauthorized");
        });
        
        // 09_regex_extract_empty_uri: 空 URI + 正则提取
        rawServer.createContext("/vuln/goby/09/", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("<html><title>Version: 1.2.3</title></html>");
        });
        rawServer.createContext("/safe/goby/09/", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("<html><title>Safe Page</title></html>");
        });
        
        // 10_redirect_design_issue: 重定向设计问题
        rawServer.createContext("/vuln/goby/10/sso", (req, resp) -> {
            resp.setStatus(302);
            resp.setContentType("text/html");
            resp.setHeader("Location", "/dashboard?user=admin");
            resp.setBody("");
        });
        rawServer.createContext("/safe/goby/10/sso", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("Please login");
        });
    }
    
    // ====================================
    // Nuclei POC Handlers
    // ====================================
    private void registerNucleiPocHandlers() {
        // 01_cve_lfi_basic: WordPress LFI
        // POC 路径: /wp-content/plugins/advanced-dewplayer/admin-panel/download-file.php
        // OkHttp 忽略 base URL 路径，直接请求绝对路径，使用测试模式状态
        RawHttpServer.RawHttpHandler nuclei01ModeHandler = (req, resp) -> {
            if (isVulnerableMode()) {
                resp.setStatus(200);
                resp.setContentType("text/plain");
                // 必须包含所有 4 个关键字: DB_NAME, DB_PASSWORD, DB_HOST, The base configurations of the WordPress
                resp.setBody("<?php\n" +
                    "define('DB_NAME', 'wordpress');\n" +
                    "define('DB_USER', 'root');\n" +
                    "define('DB_PASSWORD', 'secret');\n" +
                    "define('DB_HOST', 'localhost');\n" +
                    "/** The base configurations of the WordPress */\n");
            } else {
                resp.setStatus(404);
                resp.setContentType("text/plain");
                resp.setBody("File not found");
            }
        };
        rawServer.createContext("/wp-content/plugins/advanced-dewplayer", nuclei01ModeHandler);
        rawServer.createContext("/vuln/nuclei/01", nuclei01ModeHandler);
        rawServer.createContext("/safe/nuclei/01", nuclei01ModeHandler);
        
        // 02_extractors_login: GitHub 登录检查 (self-contained POC)
        // 这个 POC 是 self-contained，请求 github.com，测试服务器模拟 github 响应
        // 正向: 返回 302 + set-cookie: logged_in=yes + set-cookie: user_session=xxx
        rawServer.createContext("/vuln/nuclei/02/login", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            // 模拟 GitHub 登录页面，包含 authenticity_token
            resp.setBody("<html><body><div><div><div><div><form><input value='test_token'/></form></div></div></div></div></body></html>");
        });
        rawServer.createContext("/vuln/nuclei/02/session", (req, resp) -> {
            resp.setStatus(302);
            resp.setContentType("text/html");
            resp.setHeader("Location", "https://github.com");
            resp.setHeader("Set-Cookie", "logged_in=yes; user_session=abc123");
            resp.setBody("");
        });
        rawServer.createContext("/safe/nuclei/02", (req, resp) -> {
            resp.setStatus(401);
            resp.setContentType("text/html");
            resp.setBody("Invalid credentials");
        });
        
        // 03_payloads_detect: WordPress 检测
        // POC 检查多个路径: {{BaseURL}}, /wp-admin/install.php, /feed/, /?feed=rss2
        // POC 直接请求根路径，使用测试模式状态
        RawHttpServer.RawHttpHandler nuclei03ModeHandler = (req, resp) -> {
            if (isVulnerableMode()) {
                resp.setStatus(200);
                resp.setContentType("text/html");
                // 包含 WordPress 特征: generator 和 wp-content/uploads
                resp.setBody("<html><head><generator>https://wordpress.org/?v=5.8.1</generator>" +
                    "<meta name=\"generator\" content=\"wordpress 5.8.1\"></head>" +
                    "<body><a href=\"/wp-login.php\">Login</a>" +
                    "<link rel='stylesheet' href='/wp-content/uploads/style.css'></body></html>");
            } else {
                resp.setStatus(200);
                resp.setContentType("text/html");
                resp.setBody("<html><head><title>Simple Page</title></head></html>");
            }
        };
        rawServer.createContext("/", nuclei03ModeHandler); // 根路径
        rawServer.createContext("/wp-admin", nuclei03ModeHandler);
        rawServer.createContext("/feed", nuclei03ModeHandler);
        rawServer.createContext("/vuln/nuclei/03", nuclei03ModeHandler);
        rawServer.createContext("/safe/nuclei/03", nuclei03ModeHandler);
        
        // 04_dns_takeover: DNS Takeover (需要特殊处理)
        // 这类 POC 通常需要 DNS 查询，测试中模拟
        rawServer.createContext("/vuln/nuclei/04/dns-check", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("CNAME record found: pointing to non-existent domain");
        });
        rawServer.createContext("/safe/nuclei/04/dns-check", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("DNS record valid");
        });
        
        // 05_network_tcp: Redis 未授权访问 (TCP 协议，这里模拟 HTTP)
        rawServer.createContext("/vuln/nuclei/05/redis", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            // 模拟 Redis INFO 命令返回
            resp.setBody("# Server\nredis_version:6.0.9\nredis_mode:standalone\nos:Linux");
        });
        rawServer.createContext("/safe/nuclei/05/redis", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            // Redis Sentinel 模式不被视为漏洞
            resp.setBody("redis_mode:sentinel");
        });
        
        // 06_ssl_protocol: SSL/TLS 检测 (模拟)
        rawServer.createContext("/vuln/nuclei/06/ssl-info", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"subject_cn\": \"Kubernetes Ingress Controller Fake Certificate\", \"issuer_cn\": \"Self-Signed\"}");
        });
        rawServer.createContext("/safe/nuclei/06/ssl-info", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"subject_cn\": \"Valid Certificate\", \"issuer_cn\": \"DigiCert\"}");
        });
        
        // 07_file_protocol: 文件协议检测 (模拟)
        rawServer.createContext("/vuln/nuclei/07/file-check", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE\nAWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG");
        });
        rawServer.createContext("/safe/nuclei/07/file-check", (req, resp) -> {
            resp.setStatus(404);
            resp.setContentType("text/plain");
            resp.setBody("File not found");
        });
        
        // 08_multi_request: 多请求检测
        rawServer.createContext("/vuln/nuclei/08/step1", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"csrf_token\": \"tok3n123\"}");
        });
        rawServer.createContext("/vuln/nuclei/08/step2", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"status\": \"success\", \"admin\": true}");
        });
        rawServer.createContext("/safe/nuclei/08/step1", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"csrf_token\": \"tok3n123\"}");
        });
        rawServer.createContext("/safe/nuclei/08/step2", (req, resp) -> {
            resp.setStatus(403);
            resp.setContentType("application/json");
            resp.setBody("{\"status\": \"forbidden\"}");
        });
        
        // 10_default_login: Netflow Analyzer 默认登录检测
        // POC 路径: /netflow/jspui/j_security_check
        // POC 直接请求绝对路径，使用测试模式状态
        RawHttpServer.RawHttpHandler nuclei10ModeHandler = (req, resp) -> {
            System.out.println("[DEBUG] Nuclei10 处理器被调用: " + req.getMethod() + " " + req.getRawPath() + " mode=" + (isVulnerableMode() ? "vuln" : "safe"));
            if (isVulnerableMode()) {
                resp.setStatus(302);
                resp.setContentType("text/html");
                resp.setHeader("Set-Cookie", "NFA_Jsession=abc123; JSESSIONID=xyz789");
                resp.setHeader("Location", "/netflow;jsessionid=abc123");
                resp.setBody("");
                System.out.println("[DEBUG] Nuclei10 响应: status=302, Set-Cookie=NFA_Jsession=abc123; JSESSIONID=xyz789, Location=/netflow;jsessionid=abc123");
            } else {
                resp.setStatus(401);
                resp.setContentType("text/html");
                resp.setBody("<html><body>Invalid username or password</body></html>");
            }
        };
        rawServer.createContext("/netflow", nuclei10ModeHandler);
        rawServer.createContext("/vuln/nuclei/10", nuclei10ModeHandler);
        rawServer.createContext("/safe/nuclei/10", nuclei10ModeHandler);
        
        // 11_dsl_expressions: DSL 表达式测试
        // POC 路径: /api/data
        // POC 直接请求绝对路径，使用测试模式状态
        RawHttpServer.RawHttpHandler nuclei11ModeHandler = (req, resp) -> {
            System.out.println("[DEBUG] Nuclei11 处理器被调用: " + req.getMethod() + " " + req.getRawPath() + " mode=" + (isVulnerableMode() ? "vuln" : "safe"));
            if (isVulnerableMode()) {
                resp.setStatus(200);
                resp.setContentType("application/json");
                // 包含 success, 4位数字, 长度 > 100
                StringBuilder sb = new StringBuilder();
                sb.append("{\"status\": \"success\", \"id\": 1234, \"data\": \"");
                for (int i = 0; i < 30; i++) {
                    sb.append("test");
                }
                sb.append("\"}");
                String bodyStr = sb.toString();
                System.out.println("[DEBUG] Nuclei11 响应体长度: " + bodyStr.length() + ", 内容: " + bodyStr.substring(0, Math.min(50, bodyStr.length())) + "...");
                resp.setBody(bodyStr);
            } else {
                resp.setStatus(500);
                resp.setContentType("application/json");
                resp.setBody("{\"error\": \"failed\"}");
            }
        };
        rawServer.createContext("/api", nuclei11ModeHandler);
        rawServer.createContext("/vuln/nuclei/11", nuclei11ModeHandler);
        rawServer.createContext("/safe/nuclei/11", nuclei11ModeHandler);
        
        // 12_raw_request: Raw 请求测试
        rawServer.createContext("/vuln/nuclei/12/raw", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("Raw request processed successfully");
        });
        rawServer.createContext("/safe/nuclei/12/raw", (req, resp) -> {
            resp.setStatus(400);
            resp.setContentType("text/plain");
            resp.setBody("Bad Request");
        });
        
        // 13_variables_flow: 变量和流控制
        rawServer.createContext("/vuln/nuclei/13/init", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"session\": \"sess_abc123\"}");
        });
        rawServer.createContext("/vuln/nuclei/13/verify", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"valid\": true, \"user\": \"admin\"}");
        });
        rawServer.createContext("/safe/nuclei/13/init", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"session\": \"invalid\"}");
        });
        rawServer.createContext("/safe/nuclei/13/verify", (req, resp) -> {
            resp.setStatus(401);
            resp.setContentType("application/json");
            resp.setBody("{\"valid\": false}");
        });
        
        // 14_boundary_cases: 边界情况测试
        rawServer.createContext("/vuln/nuclei/14/api/empty", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("");
        });
        rawServer.createContext("/vuln/nuclei/14/api/large", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1100000; i++) {
                sb.append("a");
            }
            resp.setBody(sb.toString());
        });
        rawServer.createContext("/vuln/nuclei/14/api/special", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("<script>alert('xss')</script>");
        });
        rawServer.createContext("/vuln/nuclei/14/api/unicode", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain; charset=utf-8");
            resp.setBody("中文测试");
        });
        
        // 15_complex_nested: 复杂嵌套
        rawServer.createContext("/vuln/nuclei/15/complex", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"level1\": {\"level2\": {\"level3\": {\"value\": \"secret\"}}}}");
        });
        rawServer.createContext("/safe/nuclei/15/complex", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"level1\": {}}");
        });
    }
    
    // ====================================
    // Xray POC Handlers
    // ====================================
    private void registerXrayPocHandlers() {
        // 01_multi_rule_or_bmatches: Jeecg-Boot RCE (OR + bmatches)
        // POC 路径: /jeecg-boot/jmreport/loadTableData
        // POC 直接请求绝对路径，使用测试模式状态
        RawHttpServer.RawHttpHandler xray01ModeHandler = (req, resp) -> {
            System.out.println("[DEBUG] Xray01 处理器被调用: " + req.getMethod() + " " + req.getRawPath() + " mode=" + (isVulnerableMode() ? "vuln" : "safe"));
            if (isVulnerableMode()) {
                resp.setStatus(200);
                resp.setContentType("text/plain");
                // 模拟 Linux /etc/passwd 输出
                String bodyStr = "root:x:0:0:root:/root:/bin/bash\ndaemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin";
                System.out.println("[DEBUG] Xray01 响应体: " + bodyStr);
                resp.setBody(bodyStr);
            } else {
                resp.setStatus(200);
                resp.setContentType("application/json");
                resp.setBody("{\"error\": \"SQL syntax error\"}");
            }
        };
        rawServer.createContext("/jeecg-boot", xray01ModeHandler);
        rawServer.createContext("/vuln/xray/01", xray01ModeHandler);
        rawServer.createContext("/safe/xray/01", xray01ModeHandler);
        
        // 02_ssrf_reverse: SSRF (反连检测)
        rawServer.createContext("/vuln/xray/02/ssrf", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("Request sent to external server");
        });
        rawServer.createContext("/safe/xray/02/ssrf", (req, resp) -> {
            resp.setStatus(403);
            resp.setContentType("text/plain");
            resp.setBody("SSRF blocked");
        });
        
        // 03_time_blind_output_and: 时间盲注 + output + AND
        rawServer.createContext("/vuln/xray/03/kp/PreviewKPQT.jsp", (req, resp) -> {
            String query = req.getQuery();
            // 检查是否包含 WAITFOR DELAY
            if (query != null && query.contains("WAITFOR")) {
                // 提取延迟时间
                if (query.contains("0%3A0%3A0")) {
                    // 基准请求，立即返回
                    resp.setStatus(200);
                    resp.setContentType("text/html");
                    resp.setBody("<HTML><title>Result</title></HTML>");
                } else {
                    // 延迟请求，模拟延迟
                    try {
                        // 从 query 中提取延迟秒数
                        int delaySeconds = 6; // 默认延迟
                        if (query.contains("0%3A0%3A6")) delaySeconds = 6;
                        else if (query.contains("0%3A0%3A7")) delaySeconds = 7;
                        else if (query.contains("0%3A0%3A8")) delaySeconds = 8;
                        else if (query.contains("0%3A0%3A3")) delaySeconds = 3;
                        else if (query.contains("0%3A0%3A4")) delaySeconds = 4;
                        else if (query.contains("0%3A0%3A5")) delaySeconds = 5;
                        
                        TimeUnit.SECONDS.sleep(delaySeconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    resp.setStatus(200);
                    resp.setContentType("text/html");
                    resp.setBody("<HTML><title>Result</title></HTML>");
                }
            } else {
                resp.setStatus(200);
                resp.setContentType("text/html");
                resp.setBody("<HTML><title>Result</title></HTML>");
            }
        });
        rawServer.createContext("/safe/xray/03/kp/PreviewKPQT.jsp", (req, resp) -> {
            // 安全版本：不延迟
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("<HTML><title>Result</title></HTML>");
        });
        
        // 04_payloads_weak_password: ActiveMQ 弱口令检测
        // POC 路径: /index.php (check) + /action.php (auth)
        // POC 直接请求绝对路径，使用测试模式状态
        RawHttpServer.RawHttpHandler xray04ModeHandler = (req, resp) -> {
            System.out.println("[DEBUG] Xray04 处理器被调用: " + req.getMethod() + " " + req.getRawPath() + " mode=" + (isVulnerableMode() ? "vuln" : "safe"));
            if (isVulnerableMode()) {
                String path = req.getRawPath();
                resp.setStatus(200);
                resp.setContentType("text/html");
                String bodyStr;
                if (path.contains("action")) {
                    bodyStr = "<script>window.location='main.html'</script>";
                } else {
                    bodyStr = "<html><body>Reporter Dashboard</body></html>";
                }
                System.out.println("[DEBUG] Xray04 响应体: " + bodyStr);
                resp.setBody(bodyStr);
            } else {
                resp.setStatus(401);
                resp.setContentType("text/html");
                resp.setBody("Login required");
            }
        };
        rawServer.createContext("/index", xray04ModeHandler);
        rawServer.createContext("/action", xray04ModeHandler);
        rawServer.createContext("/vuln/xray/04", xray04ModeHandler);
        rawServer.createContext("/safe/xray/04", xray04ModeHandler);
        
        // 05_simple_file_read: 3CX SMC 文件读取
        // POC 路径: /Electron/download/windows/...
        // POC 直接请求绝对路径，使用测试模式状态
        RawHttpServer.RawHttpHandler xray05ModeHandler = (req, resp) -> {
            System.out.println("[DEBUG] Xray05 处理器被调用: " + req.getMethod() + " " + req.getRawPath() + " mode=" + (isVulnerableMode() ? "vuln" : "safe"));
            if (isVulnerableMode()) {
                resp.setStatus(200);
                resp.setContentType("text/plain");
                String bodyStr = "{\"CfgServerPassword\": \"admin123\"}";
                System.out.println("[DEBUG] Xray05 响应体: " + bodyStr);
                resp.setBody(bodyStr);
            } else {
                resp.setStatus(404);
                resp.setContentType("text/plain");
                resp.setBody("File not found");
            }
        };
        // OkHttp 会规范化路径遍历，如 /Electron/download/windows/../../../Http/... -> /Http/...
        rawServer.createContext("/Electron", xray05ModeHandler);
        rawServer.createContext("/Http", xray05ModeHandler); // 规范化后的路径
        rawServer.createContext("/vuln/xray/05", xray05ModeHandler);
        rawServer.createContext("/safe/xray/05", xray05ModeHandler);
        
        // 06_post_sqli: PostgreSQL SQL 注入
        // POC 路径: /getOrderTrackingNumber.htm  匹配 "postgres"
        RawHttpServer.RawHttpHandler xray06VulnHandler = (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("ERROR: invalid input syntax for type integer: postgres");
        };
        rawServer.createContext("/vuln/xray/06/getOrderTrackingNumber", xray06VulnHandler); // 前缀匹配
        rawServer.createContext("/safe/xray/06", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("Order not found");
        });
        
        // 07_unauth_access: 未授权访问
        // POC 路径: /yuding/selectUserByOrgId.action  匹配 status==200 + "password" + "success":true
        RawHttpServer.RawHttpHandler xray07VulnHandler = (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"success\":true, \"data\": [{\"username\": \"admin\", \"password\": \"123456\"}]}");
        };
        rawServer.createContext("/vuln/xray/07/yuding", xray07VulnHandler); // 前缀匹配
        rawServer.createContext("/safe/xray/07", (req, resp) -> {
            resp.setStatus(401);
            resp.setContentType("application/json");
            resp.setBody("{\"success\":false, \"message\": \"Unauthorized\"}");
        });
        
        // 08_fastjson_deserialization: FastJson 反序列化
        rawServer.createContext("/vuln/xray/08/api", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"status\": \"processed\", \"java_version\": \"1.8.0_291\"}");
        });
        rawServer.createContext("/safe/xray/08/api", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"status\": \"error\", \"message\": \"Invalid JSON\"}");
        });
        
        // 09_file_upload: 文件上传
        rawServer.createContext("/vuln/xray/09/feed/UploadFile.do", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"url\": \"/feed/ShowImage.do?file=testfile.jsp\", \"status\": \"success\"}");
        });
        rawServer.createContext("/vuln/xray/09/testfile.jsp", (req, resp) -> {
            // 模拟上传的 JSP 文件被执行
            resp.setStatus(200);
            resp.setContentType("text/plain");
            // 1792000000 是 40000 * 44800 的近似值
            resp.setBody("1792000000");
        });
        rawServer.createContext("/safe/xray/09/feed/UploadFile.do", (req, resp) -> {
            resp.setStatus(403);
            resp.setContentType("application/json");
            resp.setBody("{\"error\": \"File upload not allowed\"}");
        });
        
        // 10_rce_command_exec: RCE 命令执行
        rawServer.createContext("/vuln/xray/10/exec", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("uid=0(root) gid=0(root) groups=0(root)");
        });
        rawServer.createContext("/safe/xray/10/exec", (req, resp) -> {
            resp.setStatus(403);
            resp.setContentType("text/plain");
            resp.setBody("Command execution blocked");
        });
    }
    
    // ====================================
    // Pocsuite POC Handlers
    // ====================================
    private void registerPocsuitePocHandlers() {
        // 01_sqli_header_referer: PHPCMS SQL 注入 (Header Referer)
        rawServer.createContext("/vuln/pocsuite/01/data/js.php", (req, resp) -> {
            String referer = req.getHeaders().get("Referer");
            if (referer != null && referer.contains("SELECT")) {
                resp.setStatus(200);
                resp.setContentType("text/plain");
                // MD5(1) = c4ca4238a0b923820dcc509a6f75849b
                resp.setBody("Error: Duplicate entry 'c4ca4238a0b923820dcc509a6f75849b' for key");
            } else {
                resp.setStatus(200);
                resp.setContentType("text/plain");
                resp.setBody("Normal response");
            }
        });
        rawServer.createContext("/safe/pocsuite/01/data/js.php", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("Parameter filtered");
        });
        
        // 02_multi_step_necessary: 多步骤认证绕过
        rawServer.createContext("/vuln/pocsuite/02/api/auth/token", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9\"}");
        });
        rawServer.createContext("/vuln/pocsuite/02/admin/panel", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/html");
            resp.setBody("<html><body><h1>admin dashboard</h1></body></html>");
        });
        rawServer.createContext("/safe/pocsuite/02/api/auth/token", (req, resp) -> {
            resp.setStatus(401);
            resp.setContentType("application/json");
            resp.setBody("{\"error\": \"Invalid credentials\"}");
        });
        rawServer.createContext("/safe/pocsuite/02/admin/panel", (req, resp) -> {
            resp.setStatus(403);
            resp.setContentType("text/html");
            resp.setBody("<html><body>Access Denied</body></html>");
        });
        
        // 03_time_blind_injection: MySQL 时间盲注
        rawServer.createContext("/vuln/pocsuite/03/api/search", (req, resp) -> {
            String query = req.getQuery();
            if (query != null && query.contains("SLEEP")) {
                try {
                    // 延迟 5 秒
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"results\": []}");
        });
        rawServer.createContext("/safe/pocsuite/03/api/search", (req, resp) -> {
            // 安全版本：不延迟
            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.setBody("{\"results\": []}");
        });
    }
}

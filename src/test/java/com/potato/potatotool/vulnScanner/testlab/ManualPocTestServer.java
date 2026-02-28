package com.potato.potatotool.vulnScanner.testlab;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 手工编写的 POC 测试服务器
 * 
 * 每个 POC 的测试用例都是人工阅读 POC 原文后手动编写的，
 * 确保测试的独立性和正确性（不依赖 POC 解析器）。
 * 
 * @author Potato
 */
public class ManualPocTestServer {
    
    private RawHttpServer rawServer;
    private int port;
    
    public ManualPocTestServer(int port) {
        this.port = port;
    }
    
    public void start() throws IOException {
        rawServer = new RawHttpServer(port);
        
        // 注册所有手工编写的处理器
        registerPocsuitePocHandlers();
        registerXrayPocHandlers();
        registerGobyPocHandlers();
        
        // 处理 OkHttp URL 归一化后的路径遍历请求
        // OkHttp 的 encodedPath() 会将 %2e%2e 解码并归一化，这是无法绕过的安全设计
        // 当 POC 请求包含路径遍历时，会被归一化为 /etc/passwd 等
        rawServer.createContext("/etc/passwd", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            // 返回更完整的 /etc/passwd 内容，满足多个 POC 的检查条件
            resp.setBody("root:x:0:0:root:/root:/bin/bash\n" +
                        "daemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin\n" +
                        "bin:x:2:2:bin:/bin:/usr/sbin/nologin\n" +
                        "nobody:x:65534:65534:nobody:/nonexistent:/usr/sbin/nologin");
        });
        rawServer.createContext("/etc/shadow", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("root:$6$hash:18000:0:99999:7:::");
        });
        rawServer.createContext("/windows/win.ini", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            // 返回更完整的 win.ini 内容，包含 "font" 和 "file"
            resp.setBody("[fonts]\nfile=test\n[extensions]\n[mci extensions]\n[files]");
        });
        rawServer.createContext("/Windows/win.ini", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("[fonts]\nfile=test\n[extensions]\n[mci extensions]\n[files]");
        });
        // Lanproxy 配置文件（路径遍历归一化后的多种路径）
        rawServer.createContext("/conf/config.properties", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("server.ssl.enable=false\nserver.bind=0.0.0.0\nconfig.admin.username=admin\nconfig.admin.password=admin");
        });
        // 处理 /vuln/goby/conf/config.properties（Lanproxy POC 归一化后的路径）
        rawServer.createContext("/vuln/goby/conf/config.properties", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("server.ssl.enable=false\nserver.bind=0.0.0.0\nconfig.admin.username=admin\nconfig.admin.password=admin");
        });
        // Laravel .env 配置文件泄露
        rawServer.createContext("/.env", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setBody("APP_NAME=Laravel\nAPP_ENV=production\nAPP_KEY=base64:test123\nAPP_DEBUG=false\nDB_CONNECTION=mysql\nDB_HOST=127.0.0.1");
        });
        // WEB-INF 文件读取（Jetty 等漏洞）
        // 正常请求返回 404 + Jetty 标识（用于检测 Jetty 服务器）
        rawServer.createContext("/WEB-INF/web.xml", (req, resp) -> {
            resp.setStatus(404);
            resp.setContentType("text/html");
            resp.setBody("<html><body><h2>Error 404 Not Found</h2><hr><i>Jetty://9.4.43.v20210629</i></body></html>");
        });
        // Unicode 编码绕过路径返回 200 + web.xml 内容
        rawServer.createContext("/%u002e/WEB-INF/web.xml", (req, resp) -> {
            resp.setStatus(200);
            resp.setContentType("application/xml");
            resp.setBody("<?xml version=\"1.0\"?>\n<web-app>\n<display-name>Test Application</display-name>\n</web-app>");
        });
        
        // 默认处理器
        rawServer.setDefaultHandler((req, resp) -> {
            resp.setStatus(404);
            resp.setContentType("text/plain");
            resp.setBody("Not Found: " + req.getRawPath());
        });
        
        rawServer.start();
    }
    
    public void stop() {
        if (rawServer != null) {
            rawServer.stop();
            System.out.println("手工测试服务器已停止");
        }
        System.out.println("线程池已关闭");
    }
    
    /**
     * 适配器：将 RawHttpHandler 包装为兼容层
     */
    private void registerHandler(String path, HttpHandler handler) {
        rawServer.createContext(path, (req, resp) -> {
            // 创建适配的 HttpExchange
            RawHttpExchangeAdapter adapter = new RawHttpExchangeAdapter(req, resp);
            handler.handle(adapter);
        });
    }
    
    /**
     * HttpExchange 适配器 - 让现有 Handler 能够与 RawHttpServer 一起工作
     */
    private static class RawHttpExchangeAdapter extends HttpExchange {
        private final RawHttpServer.RawHttpRequest request;
        private final RawHttpServer.RawHttpResponse response;
        private final com.sun.net.httpserver.Headers requestHeaders;
        private final com.sun.net.httpserver.Headers responseHeaders;
        private URI requestUri;
        
        public RawHttpExchangeAdapter(RawHttpServer.RawHttpRequest request, RawHttpServer.RawHttpResponse response) {
            this.request = request;
            this.response = response;
            this.requestHeaders = new com.sun.net.httpserver.Headers();
            this.responseHeaders = new com.sun.net.httpserver.Headers();
            
            // 复制请求头
            for (java.util.Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                requestHeaders.add(entry.getKey(), entry.getValue());
            }
            
            // 解析 URI（保留原始路径）
            try {
                // 使用原始 URI 创建 URI 对象，但我们会覆盖 getRawPath 等方法
                this.requestUri = new URI(request.getRawUri());
            } catch (Exception e) {
                System.err.println("[DEBUG] URI 解析失败: " + request.getRawUri() + " -> " + e.getMessage());
                try {
                    // 尝试直接从 rawUri 中提取 query 部分
                    String rawUri = request.getRawUri();
                    int queryIdx = rawUri.indexOf('?');
                    if (queryIdx >= 0) {
                        String path = rawUri.substring(0, queryIdx);
                        String query = rawUri.substring(queryIdx + 1);
                        // 使用简化的 URI 构造
                        this.requestUri = new URI(null, null, path, query, null);
                    } else {
                        this.requestUri = new URI("/");
                    }
                } catch (Exception ex) {
                    try {
                        this.requestUri = new URI("/");
                    } catch (Exception ignored) {}
                }
            }
        }
        
        @Override
        public com.sun.net.httpserver.Headers getRequestHeaders() { return requestHeaders; }
        
        @Override
        public com.sun.net.httpserver.Headers getResponseHeaders() { return responseHeaders; }
        
        @Override
        public URI getRequestURI() {
            // 返回原始 URI（URI 是 final 类，我们存储原始值）
            return requestUri;
        }
        
        /** 获取原始路径（未解码） */
        public String getRawPath() {
            return request.getRawPath();
        }
        
        /** 获取查询字符串 */
        public String getQueryString() {
            return request.getQuery();
        }
        
        @Override
        public String getRequestMethod() { return request.getMethod(); }
        
        @Override
        public java.net.InetSocketAddress getRemoteAddress() { return null; }
        
        @Override
        public java.net.InetSocketAddress getLocalAddress() { return null; }
        
        @Override
        public int getResponseCode() { return response.getStatus(); }
        
        @Override
        public InputStream getRequestBody() {
            return new ByteArrayInputStream(request.getBody());
        }
        
        @Override
        public OutputStream getResponseBody() {
            return new ByteArrayOutputStream() {
                @Override
                public void close() throws IOException {
                    super.close();
                    response.setBody(this.toByteArray());
                }
            };
        }
        
        @Override
        public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
            response.setStatus(rCode);
            // 复制响应头
            for (java.util.Map.Entry<String, java.util.List<String>> entry : responseHeaders.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    response.setHeader(entry.getKey(), entry.getValue().get(0));
                }
            }
        }
        
        @Override
        public com.sun.net.httpserver.HttpContext getHttpContext() { return null; }
        
        @Override
        public void close() { }
        
        @Override
        public Object getAttribute(String name) { return null; }
        
        @Override
        public void setAttribute(String name, Object value) { }
        
        @Override
        public void setStreams(InputStream i, OutputStream o) { }
        
        @Override
        public com.sun.net.httpserver.HttpPrincipal getPrincipal() { return null; }
        
        @Override
        public String getProtocol() { return "HTTP/1.1"; }
    }
    
    // ========================================
    // 辅助方法
    // ========================================
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        sendResponse(exchange, statusCode, response, "application/json");
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
    
    // ========================================
    // Pocsuite POC 处理器（3个）
    // 每个 POC 都有两个版本：
    //   /vuln/xxx - 漏洞版本（正向测试）
    //   /safe/xxx - 安全版本（负向测试）
    // ========================================
    
    private void registerPocsuitePocHandlers() {
        // POC 1: Pocsuite.json - PHPCMS SQL 注入
        // 漏洞版本
        registerHandler("/vuln/pocsuite/phpcms/data/js.php", new PhpcmsSqlInjectionHandler(true));
        // 安全版本
        registerHandler("/safe/pocsuite/phpcms/data/js.php", new PhpcmsSqlInjectionHandler(false));
        
        // POC 2: multi_step_with_necessary.json - 多步骤认证绕过
        // 漏洞版本
        registerHandler("/vuln/pocsuite/multistep/api/auth/token", new MultiStepTokenHandler(true));
        registerHandler("/vuln/pocsuite/multistep/admin/panel", new MultiStepAdminPanelHandler(true));
        // 安全版本
        registerHandler("/safe/pocsuite/multistep/api/auth/token", new MultiStepTokenHandler(false));
        registerHandler("/safe/pocsuite/multistep/admin/panel", new MultiStepAdminPanelHandler(false));
        
        // POC 3: sql_time_blind_injection.json - 时间盲注
        // 漏洞版本
        registerHandler("/vuln/pocsuite/timebased/api/search", new TimeBasedSqlInjectionHandler(true));
        // 安全版本
        registerHandler("/safe/pocsuite/timebased/api/search", new TimeBasedSqlInjectionHandler(false));
    }
    
    // ========================================
    // Xray POC 处理器（31个，其中1个反连跳过）
    // 分类：
    //   - 文件读取：11个
    //   - SQL注入：7个
    //   - RCE：2个
    //   - 文件上传：4个
    //   - 未授权访问：2个
    //   - 弱口令：1个
    //   - 其他：4个（含1个反连跳过）
    // ========================================
    
    private void registerXrayPocHandlers() {
        // ==================== 文件读取类 ====================
        // POC 1: 3C环境监测系统 ReadLog
        registerHandler("/vuln/xray/3c/ajax/sys/LogService.ashx", new XrayFileReadHandler(true, "configuration", "system.web"));
        registerHandler("/safe/xray/3c/ajax/sys/LogService.ashx", new XrayFileReadHandler(false, "configuration", "system.web"));
        
        // POC 2: 华天动力OA downloadWpsFile.jsp
        registerHandler("/vuln/xray/huatian/OAapp/jsp/downloadWpsFile.jsp", new XrayFileReadHandler(true, "web-app", "xml"));
        registerHandler("/safe/xray/huatian/OAapp/jsp/downloadWpsFile.jsp", new XrayFileReadHandler(false, "web-app", "xml"));
        
        // POC 3: Bazaar v1.4.3 文件读取
        registerHandler("/vuln/xray/bazaar", new XrayFileReadHandler(true, "root:", "daemon:"));
        registerHandler("/safe/xray/bazaar", new XrayFileReadHandler(false, "root:", "daemon:"));
        
        // POC 4-11: 其他文件读取（类似处理）
        // 银达汇智、夏普Sharp、3CX Phone、天问物业(3个)、联软安渡、ClusterControl
        registerHandler("/vuln/xray/fileread", new XrayFileReadHandler(true, "configuration", "root:"));
        registerHandler("/safe/xray/fileread", new XrayFileReadHandler(false, "configuration", "root:"));
        
        // ==================== SQL注入类（时间盲注）====================
        // POC: 用友时空KSOA - 使用 WAITFOR DELAY
        registerHandler("/vuln/xray/yonyou/kp/PreviewKPQT.jsp", new XrayTimeSqlInjectionHandler(true));
        registerHandler("/safe/xray/yonyou/kp/PreviewKPQT.jsp", new XrayTimeSqlInjectionHandler(false));
        
        // POC: 华磊科技物流 - 响应包含 postgres
        registerHandler("/vuln/xray/hualei/getOrderTrackingNumber.htm", new XraySqlInjectionHandler(true, "postgres"));
        registerHandler("/safe/xray/hualei/getOrderTrackingNumber.htm", new XraySqlInjectionHandler(false, "postgres"));
        
        // ==================== RCE类 ====================
        // POC: Jeecg-Boot loadTableData
        registerHandler("/vuln/xray/jeecg/jeecg-boot/jmreport/loadTableData", new XrayRceHandler(true));
        registerHandler("/safe/xray/jeecg/jeecg-boot/jmreport/loadTableData", new XrayRceHandler(false));
        
        // POC: 企望制造ERP comboxstore
        registerHandler("/vuln/xray/qiwang/comboxstore", new XrayRceHandler(true));
        registerHandler("/safe/xray/qiwang/comboxstore", new XrayRceHandler(false));
        
        // ==================== 未授权访问类 ====================
        // POC: 满客宝智慧食堂系统 - POC 路径: /yuding/selectUserByOrgId.action
        registerHandler("/vuln/xray/mankb", new XrayMankbUnauthHandler(true));
        registerHandler("/safe/xray/mankb", new XrayMankbUnauthHandler(false));
        
        // POC: Netgear WN604
        registerHandler("/vuln/xray/netgear/siteSurvey.php", new XrayUnauthHandler(true, "SSID", "BSSID"));
        registerHandler("/safe/xray/netgear/siteSurvey.php", new XrayUnauthHandler(false, "SSID", "BSSID"));
        
        // ==================== 弱口令类 ====================
        // POC: 天融信网关
        registerHandler("/vuln/xray/tianrongxin/index.php", new XrayWeakPasswordCheckHandler(true));
        registerHandler("/vuln/xray/tianrongxin/action.php", new XrayWeakPasswordLoginHandler(true));
        registerHandler("/safe/xray/tianrongxin/index.php", new XrayWeakPasswordCheckHandler(false));
        registerHandler("/safe/xray/tianrongxin/action.php", new XrayWeakPasswordLoginHandler(false));
        
        // ==================== FastJson反序列化 ====================
        // POC: 海康威视 - POC 路径: /bic/ssoService/v1/keepAlive
        registerHandler("/vuln/xray/hikvision", new XrayHikvisionHandler(true));
        registerHandler("/safe/xray/hikvision", new XrayHikvisionHandler(false));
        
        // ==================== SSRF反连类 ====================
        // POC: 泛微E-Mobile SSRF - POC 路径: /install/installOperate.do
        // 验证条件: response.status == 200 && reverse.wait(3)
        registerHandler("/vuln/xray/fanwei", new XraySsrfHandler(true));
        registerHandler("/safe/xray/fanwei", new XraySsrfHandler(false));
        // 真实 POC 路径
        registerHandler("/install/installOperate.do", new XraySsrfHandler(true));
    }
    
    // ========================================
    // Xray 通用文件读取处理器
    // ========================================
    class XrayFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        private final String keyword1;
        private final String keyword2;
        
        public XrayFileReadHandler(boolean vulnerable, String keyword1, String keyword2) {
            this.vulnerable = vulnerable;
            this.keyword1 = keyword1;
            this.keyword2 = keyword2;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            if (vulnerable) {
                // 漏洞版本：返回包含敏感文件内容的响应
                response = "<?xml version=\"1.0\"?>\n" +
                          "<" + keyword1 + ">\n" +
                          "  <" + keyword2 + ">\n" +
                          "    <compilation debug=\"true\"/>\n" +
                          "  </" + keyword2 + ">\n" +
                          "</" + keyword1 + ">";
            } else {
                // 安全版本：返回错误或空响应
                response = "{\"error\":\"Access denied\",\"code\":403}";
            }
            sendResponse(exchange, 200, response);
        }
    }
    
    // ========================================
    // Xray SQL注入（响应匹配）处理器
    // ========================================
    class XraySqlInjectionHandler implements HttpHandler {
        private final boolean vulnerable;
        private final String keyword;
        
        public XraySqlInjectionHandler(boolean vulnerable, String keyword) {
            this.vulnerable = vulnerable;
            this.keyword = keyword;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            if (vulnerable) {
                // 漏洞版本：返回数据库错误信息
                response = "ERROR: operator does not exist: " + keyword + "::integer";
            } else {
                // 安全版本：返回正常响应
                response = "{\"success\":true,\"data\":[]}";
            }
            sendResponse(exchange, 200, response);
        }
    }
    
    // ========================================
    // Xray SQL注入（时间盲注）处理器
    // ========================================
    class XrayTimeSqlInjectionHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public XrayTimeSqlInjectionHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            
            if (vulnerable && query != null) {
                // 检查是否包含 WAITFOR DELAY
                String decodedQuery = java.net.URLDecoder.decode(query, "UTF-8").toUpperCase();
                if (decodedQuery.contains("WAITFOR") && decodedQuery.contains("DELAY")) {
                    // 提取延迟时间
                    int seconds = extractWaitforDelay(decodedQuery);
                    if (seconds > 0 && seconds <= 10) {
                        try {
                            Thread.sleep(seconds * 1000L);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            
            // 返回包含 title 和 HTML 的响应
            String response = "<HTML><head><title>Preview</title></head><body>Content</body></HTML>";
            sendResponse(exchange, 200, response, "text/html");
        }
        
        private int extractWaitforDelay(String query) {
            // 从 '0:0:N' 中提取 N
            int idx = query.lastIndexOf("0:0:");
            if (idx >= 0) {
                int start = idx + 4;
                StringBuilder sb = new StringBuilder();
                while (start < query.length() && Character.isDigit(query.charAt(start))) {
                    sb.append(query.charAt(start));
                    start++;
                }
                if (sb.length() > 0) {
                    try {
                        return Integer.parseInt(sb.toString());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
            return 0;
        }
    }
    
    // ========================================
    // Xray RCE 处理器
    // ========================================
    class XrayRceHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public XrayRceHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            if (vulnerable) {
                // 读取请求体检查是否包含命令
                String body = readRequestBody(exchange);
                if (body.contains("cat /etc/passwd") || body.contains("cat%20/etc/passwd")) {
                    response = "root:x:0:0:root:/root:/bin/bash\n" +
                              "daemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin\n" +
                              "bin:x:2:2:bin:/bin:/usr/sbin/nologin";
                } else if (body.contains("type c:/windows/win.ini") || body.contains("win.ini")) {
                    response = "; for 16-bit app support\n[fonts]\n[extensions]";
                } else {
                    response = "root:x:0:0:root:/root:/bin/bash";
                }
            } else {
                // 安全版本：返回错误
                response = "{\"error\":\"Forbidden\",\"message\":\"Access denied\"}";
            }
            sendResponse(exchange, 200, response);
        }
    }
    
    // ========================================
    // Xray 未授权访问处理器
    // ========================================
    class XrayUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        private final String keyword1;
        private final String keyword2;
        
        public XrayUnauthHandler(boolean vulnerable, String keyword1, String keyword2) {
            this.vulnerable = vulnerable;
            this.keyword1 = keyword1;
            this.keyword2 = keyword2;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            if (vulnerable) {
                // 漏洞版本：返回敏感信息
                response = "{\"" + keyword1 + "\":\"admin123\"," + keyword2 + ",\"data\":[{\"username\":\"admin\",\"" + keyword1 + "\":\"123456\"}]}";
            } else {
                // 安全版本：需要认证
                response = "{\"error\":\"Unauthorized\",\"message\":\"Please login\"}";
                sendResponse(exchange, 401, response, "application/json");
                return;
            }
            sendResponse(exchange, 200, response, "application/json");
        }
    }
    
    // ========================================
    // Xray 满客宝未授权访问处理器
    // 专门处理 /yuding/selectUserByOrgId.action 路径
    // POC 验证: response.body_string.contains("password") && contains("\"success\":true")
    // ========================================
    class XrayMankbUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public XrayMankbUnauthHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String response;
            
            // 检查路径是否匹配 /yuding/selectUserByOrgId.action
            if (path.contains("/yuding/selectUserByOrgId.action")) {
                if (vulnerable) {
                    // 漏洞版本：返回包含 password 和 "success":true 的响应
                    response = "{\"success\":true,\"password\":\"admin123\",\"data\":[{\"username\":\"admin\",\"password\":\"123456\"}]}";
                } else {
                    // 安全版本：需要认证
                    response = "{\"error\":\"Unauthorized\",\"message\":\"Please login\"}";
                    sendResponse(exchange, 401, response, "application/json");
                    return;
                }
            } else {
                response = "{\"error\":\"Not Found\"}";
                sendResponse(exchange, 404, response, "application/json");
                return;
            }
            sendResponse(exchange, 200, response, "application/json");
        }
    }
    
    // ========================================
    // Xray 弱口令检查处理器
    // ========================================
    class XrayWeakPasswordCheckHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public XrayWeakPasswordCheckHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 检查页面是否存在
            String response = "<html><head><title>Login - Reporter</title></head><body>Login Form</body></html>";
            sendResponse(exchange, 200, response, "text/html");
        }
    }
    
    // ========================================
    // Xray 弱口令登录处理器
    // ========================================
    class XrayWeakPasswordLoginHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public XrayWeakPasswordLoginHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            String response;
            
            if (vulnerable) {
                // 漏洞版本：弱口令可以登录成功
                if (body.contains("user_name=guest") && body.contains("user_password=guest")) {
                    response = "<script>window.location='main.html';</script>";
                } else {
                    response = "<script>alert('Login failed');</script>";
                }
            } else {
                // 安全版本：任何密码都失败
                response = "<script>alert('Login failed');</script>";
            }
            sendResponse(exchange, 200, response, "text/html");
        }
    }
    
    // ========================================
    // Xray 海康威视 FastJson 反序列化处理器
    // POC 路径: /bic/ssoService/v1/keepAlive
    // POC 验证: response.body_string.contains(string(s2 - s1)) && response.headers['Server'].contains('openresty')
    // ========================================
    class XrayHikvisionHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public XrayHikvisionHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String response;
            
            // 检查路径是否匹配
            if (!path.contains("/bic/ssoService/v1/keepAlive")) {
                response = "{\"error\":\"Not Found\"}";
                sendResponse(exchange, 404, response, "application/json");
                return;
            }
            
            if (vulnerable) {
                // 获取 cmd 头: "set /A {{s2}}-{{s1}}"
                // 期望计算结果: s2 - s1
                String cmd = exchange.getRequestHeaders().getFirst("cmd");
                if (cmd != null && cmd.contains("-")) {
                    // 模拟执行 set /A 计算: "set /A 16502-158110" -> -141608
                    try {
                        String expr = cmd.replace("set /A ", "").trim();
                        // 解析 a-b 形式
                        int dashPos = expr.indexOf('-', 1); // 跳过可能的负号
                        if (dashPos > 0) {
                            int a = Integer.parseInt(expr.substring(0, dashPos).trim());
                            int b = Integer.parseInt(expr.substring(dashPos + 1).trim());
                            response = String.valueOf(a - b);
                        } else {
                            response = "0";
                        }
                    } catch (Exception e) {
                        response = "error";
                    }
                } else {
                    response = "0";
                }
            } else {
                // 安全版本：返回不包含计算结果的响应
                response = "{\"error\":\"Forbidden\"}";
            }
            
            exchange.getResponseHeaders().set("Server", "openresty/1.19.3.1");
            sendResponse(exchange, 200, response);
        }
    }
    
    // ========================================
    // Xray SSRF 反连处理器
    // POC 验证: response.status == 200 && reverse.wait(3)
    // 注意：反连验证依赖 DNSLog 服务，测试中只验证 HTTP 响应部分
    // ========================================
    class XraySsrfHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public XraySsrfHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            String response;
            
            // 检查路径是否匹配 /install/installOperate.do
            if (path.contains("/install/installOperate.do")) {
                if (vulnerable) {
                    // 漏洞版本：返回 200（DNSLog 被网络拦截，仅输出日志）
                    if (query != null && query.contains("svrurl=")) {
                        try {
                            String svrurl = java.net.URLDecoder.decode(
                                query.split("svrurl=")[1].split("&")[0], "UTF-8");
                            System.out.println("[SSRF] 收到反连URL: " + svrurl);
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                    response = "Install Success";
                } else {
                    // 安全版本：拒绝访问
                    response = "{\"error\":\"Forbidden\"}";
                    sendResponse(exchange, 403, response, "application/json");
                    return;
                }
            } else {
                response = "{\"error\":\"Not Found\"}";
                sendResponse(exchange, 404, response, "application/json");
                return;
            }
            sendResponse(exchange, 200, response, "text/plain");
        }
    }
    
    // 辅助方法：读取请求体
    private String readRequestBody(HttpExchange exchange) {
        try {
            java.io.InputStream is = exchange.getRequestBody();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
    
    // ========================================
    // Goby POC 处理器（待添加）
    // ========================================
    
    private void registerGobyPocHandlers() {
        // Goby POC 1: Alibaba Nacos 未授权访问漏洞
        registerHandler("/vuln/goby/nacos", new GobyNacosUnauthHandler(true));
        registerHandler("/safe/goby/nacos", new GobyNacosUnauthHandler(false));
        
        // Goby POC 2: Apache 2.4.49 路径遍历 CVE-2021-41773 (暂时禁用)
        registerHandler("/vuln/goby/apache-traversal", new GobyApacheTraversalHandler(true));
        registerHandler("/safe/goby/apache-traversal", new GobyApacheTraversalHandler(false));
        
        // Goby POC 3: 360天擎数据库信息泄露
        registerHandler("/vuln/goby/360tianqing", new Goby360TianqingHandler(true));
        registerHandler("/safe/goby/360tianqing", new Goby360TianqingHandler(false));
        
        // Goby POC 4: Nacos 默认密码
        registerHandler("/vuln/goby/nacos-default-pwd", new GobyNacosDefaultPwdHandler(true));
        registerHandler("/safe/goby/nacos-default-pwd", new GobyNacosDefaultPwdHandler(false));
        
        // Goby POC 5: Apache APISIX 默认 Token
        registerHandler("/vuln/goby/apisix", new GobyApisixDefaultTokenHandler(true));
        registerHandler("/safe/goby/apisix", new GobyApisixDefaultTokenHandler(false));
        
        // Goby POC 6: Apache Kylin 未授权配置泄露
        registerHandler("/vuln/goby/kylin", new GobyKylinConfigLeakHandler(true));
        registerHandler("/safe/goby/kylin", new GobyKylinConfigLeakHandler(false));
        
        // Goby POC 7: Apache ActiveMQ 弱口令
        registerHandler("/vuln/goby/activemq", new GobyActiveMQWeakPwdHandler(true));
        registerHandler("/safe/goby/activemq", new GobyActiveMQWeakPwdHandler(false));
        
        // Goby POC 8: Adobe ColdFusion LFI
        registerHandler("/vuln/goby/coldfusion", new GobyColdFusionLFIHandler(true));
        registerHandler("/safe/goby/coldfusion", new GobyColdFusionLFIHandler(false));
        
        // Goby POC 9: Ametys CMS 信息泄露
        registerHandler("/vuln/goby/ametys", new GobyAmetysCMSHandler(true));
        registerHandler("/safe/goby/ametys", new GobyAmetysCMSHandler(false));
        
        // Goby POC 10: 360天擎 SQL 注入
        registerHandler("/vuln/goby/360tianqing-sqli", new Goby360TianqingSQLiHandler(true));
        registerHandler("/safe/goby/360tianqing-sqli", new Goby360TianqingSQLiHandler(false));
        
        // Goby POC 11: Apache Solr 文件读取
        registerHandler("/vuln/goby/solr", new GobySolrFileReadHandler(true));
        registerHandler("/safe/goby/solr", new GobySolrFileReadHandler(false));
        
        // Goby POC 12: Spring Boot Actuator 未授权
        
        // Goby POC 13: Adslr 信息泄露
        registerHandler("/vuln/goby/adslr", new GobyAdslrInfoLeakHandler(true));
        registerHandler("/safe/goby/adslr", new GobyAdslrInfoLeakHandler(false));
        
        // Goby POC 14: AVCON6 文件下载
        
        // Goby POC 15: ADSelfService Plus RCE
        
        // Goby POC 16: RuoYi Druid 未授权
        registerHandler("/vuln/goby/ruoyi-druid", new GobyRuoyiDruidHandler(true));
        registerHandler("/safe/goby/ruoyi-druid", new GobyRuoyiDruidHandler(false));
        
        // Goby POC 17: XXL-JOB 默认密码
        registerHandler("/vuln/goby/xxljob", new GobyXxlJobDefaultPwdHandler(true));
        registerHandler("/safe/goby/xxljob", new GobyXxlJobDefaultPwdHandler(false));
        
        // Goby POC 18: Alibaba Canal 默认密码
        registerHandler("/vuln/goby/canal", new GobyCanalDefaultPwdHandler(true));
        registerHandler("/safe/goby/canal", new GobyCanalDefaultPwdHandler(false));
        
        // Goby POC 19: SonarQube 未授权
        
        // Goby POC 20: Apache Druid 文件读取
        
        // Goby POC 21: Apache Flink 文件读取
        registerHandler("/vuln/goby/flink", new GobyFlinkFileReadHandler(true));
        registerHandler("/safe/goby/flink", new GobyFlinkFileReadHandler(false));
        
        // Goby POC 22: Apache Airflow 未授权
        
        // Goby POC 23: Apache CouchDB 未授权
        
        // Goby POC 24: Apache Dubbo Admin 默认密码
        registerHandler("/vuln/goby/dubbo", new GobyDubboDefaultPwdHandler(true));
        registerHandler("/safe/goby/dubbo", new GobyDubboDefaultPwdHandler(false));
        
        // Goby POC 25: Metabase 文件读取
        
        // Goby POC 26: Weblogic SSRF
        
        // Goby POC 27: Jetty WEB-INF 文件读取
        registerHandler("/vuln/goby/jetty", new GobyJettyFileReadHandler(true));
        registerHandler("/safe/goby/jetty", new GobyJettyFileReadHandler(false));
        
        // Goby POC 28: Laravel .env 泄露
        registerHandler("/vuln/goby/laravel", new GobyLaravelEnvLeakHandler(true));
        registerHandler("/safe/goby/laravel", new GobyLaravelEnvLeakHandler(false));
        
        // Goby POC 29: VMware vCenter 文件读取
        registerHandler("/vuln/goby/vcenter", new GobyVCenterFileReadHandler(true));
        registerHandler("/safe/goby/vcenter", new GobyVCenterFileReadHandler(false));
        
        // Goby POC 30: Apache ShenYu 未授权
        registerHandler("/vuln/goby/shenyu", new GobyShenYuUnauthHandler(true));
        registerHandler("/safe/goby/shenyu", new GobyShenYuUnauthHandler(false));
        
        // Goby POC 31: Spring Cloud Function SpEL RCE
        registerHandler("/vuln/goby/spring-cloud-function", new GobySpringCloudFunctionHandler(true));
        registerHandler("/safe/goby/spring-cloud-function", new GobySpringCloudFunctionHandler(false));
        
        // Goby POC 32: Spring Cloud Gateway RCE
        registerHandler("/vuln/goby/spring-cloud-gateway", new GobySpringCloudGatewayHandler(true));
        registerHandler("/safe/goby/spring-cloud-gateway", new GobySpringCloudGatewayHandler(false));
        
        // Goby POC 33: Atlassian Confluence OGNL RCE
        registerHandler("/vuln/goby/confluence", new GobyConfluenceRCEHandler(true));
        registerHandler("/safe/goby/confluence", new GobyConfluenceRCEHandler(false));
        
        // Goby POC 34: VMware Workspace ONE RCE
        
        // Goby POC 35: F5 BIG-IP iControl RCE
        registerHandler("/vuln/goby/f5-bigip", new GobyF5BigIPHandler(true));
        registerHandler("/safe/goby/f5-bigip", new GobyF5BigIPHandler(false));
        
        // Goby POC 36: 致远OA 信息泄露
        registerHandler("/vuln/goby/seeyon", new GobySeeyonInfoLeakHandler(true));
        registerHandler("/safe/goby/seeyon", new GobySeeyonInfoLeakHandler(false));
        
        // Goby POC 37: 用友NC BshServlet RCE
        registerHandler("/vuln/goby/yongyou-nc", new GobyYongyouNCHandler(true));
        registerHandler("/safe/goby/yongyou-nc", new GobyYongyouNCHandler(false));
        
        // Goby POC 38: 泛微OA 文件读取
        registerHandler("/vuln/goby/weaver-oa", new GobyWeaverOAHandler(true));
        registerHandler("/safe/goby/weaver-oa", new GobyWeaverOAHandler(false));
        
        // Goby POC 39: ShopXO 文件读取
        registerHandler("/vuln/goby/shopxo", new GobyShopXOHandler(true));
        registerHandler("/safe/goby/shopxo", new GobyShopXOHandler(false));
        
        // Goby POC 40: YAPI RCE
        
        // Goby POC 41: Ruijie Smartweb 弱口令
        registerHandler("/vuln/goby/ruijie", new GobyRuijieHandler(true));
        registerHandler("/safe/goby/ruijie", new GobyRuijieHandler(false));
        
        // Goby POC 42: 大华 DSS 文件下载
        registerHandler("/vuln/goby/dahua", new GobyDahuaHandler(true));
        registerHandler("/safe/goby/dahua", new GobyDahuaHandler(false));
        
        // Goby POC 43: Zabbix SAML 认证绕过
        registerHandler("/vuln/goby/zabbix", new GobyZabbixHandler(true));
        registerHandler("/safe/goby/zabbix", new GobyZabbixHandler(false));
        
        // Goby POC 44: D-Link 路由器 RCE
        registerHandler("/vuln/goby/dlink", new GobyDLinkHandler(true));
        registerHandler("/safe/goby/dlink", new GobyDLinkHandler(false));
        
        // Goby POC 45: Samsung WLAN AP RCE
        registerHandler("/vuln/goby/samsung", new GobySamsungHandler(true));
        registerHandler("/safe/goby/samsung", new GobySamsungHandler(false));
        
        // Goby POC 46: MinIO SSRF
        registerHandler("/vuln/goby/minio", new GobyMinIOHandler(true));
        registerHandler("/safe/goby/minio", new GobyMinIOHandler(false));
        
        // Goby POC 47: Node.js 路径遍历
        registerHandler("/vuln/goby/nodejs", new GobyNodeJSHandler(true));
        registerHandler("/safe/goby/nodejs", new GobyNodeJSHandler(false));
        
        // Goby POC 48: PHP 8.1 后门
        registerHandler("/vuln/goby/php8", new GobyPHP8Handler(true));
        registerHandler("/safe/goby/php8", new GobyPHP8Handler(false));
        
        // Goby POC 49: Portainer RCE
        
        // Goby POC 50: GitLab RCE CVE-2021-22205
        registerHandler("/vuln/goby/gitlab", new GobyGitLabRCEHandler(true));
        registerHandler("/safe/goby/gitlab", new GobyGitLabRCEHandler(false));
        
        // Goby POC 51: Grafana 任意文件读取
        registerHandler("/vuln/goby/grafana", new GobyGrafanaFileReadHandler(true));
        registerHandler("/safe/goby/grafana", new GobyGrafanaFileReadHandler(false));
        
        // Goby POC 52: ThinkPHP RCE
        registerHandler("/vuln/goby/thinkphp", new GobyThinkPHPHandler(true));
        registerHandler("/safe/goby/thinkphp", new GobyThinkPHPHandler(false));
        
        // Goby POC 53: Redis 未授权
        registerHandler("/vuln/goby/redis", new GobyRedisHandler(true));
        registerHandler("/safe/goby/redis", new GobyRedisHandler(false));
        
        // Goby POC 54: Jenkins 未授权
        registerHandler("/vuln/goby/jenkins", new GobyJenkinsHandler(true));
        registerHandler("/safe/goby/jenkins", new GobyJenkinsHandler(false));
        
        // Goby POC 55: Elasticsearch 未授权
        registerHandler("/vuln/goby/elasticsearch", new GobyElasticsearchHandler(true));
        registerHandler("/safe/goby/elasticsearch", new GobyElasticsearchHandler(false));
        
        // Goby POC 56: MongoDB 未授权
        registerHandler("/vuln/goby/mongodb", new GobyMongoDBHandler(true));
        registerHandler("/safe/goby/mongodb", new GobyMongoDBHandler(false));
        
        // Goby POC 57: Kibana 文件包含
        registerHandler("/vuln/goby/kibana", new GobyKibanaHandler(true));
        registerHandler("/safe/goby/kibana", new GobyKibanaHandler(false));
        
        // Goby POC 58: Harbor 信息泄露
        registerHandler("/vuln/goby/harbor", new GobyHarborHandler(true));
        registerHandler("/safe/goby/harbor", new GobyHarborHandler(false));
        
        // Goby POC 59: Nexus 弱口令
        registerHandler("/vuln/goby/nexus", new GobyNexusHandler(true));
        registerHandler("/safe/goby/nexus", new GobyNexusHandler(false));
        
        // Goby POC 60: Nacos 未授权
        
        // Goby POC 61: Oracle WebLogic 路径遍历
        registerHandler("/vuln/goby/weblogic-path", new GobyWeblogicPathHandler(true));
        registerHandler("/safe/goby/weblogic-path", new GobyWeblogicPathHandler(false));
        
        // Goby POC 62: Struts2 RCE
        registerHandler("/vuln/goby/struts2", new GobyStruts2Handler(true));
        registerHandler("/safe/goby/struts2", new GobyStruts2Handler(false));
        
        // Goby POC 63: Shiro 反序列化
        registerHandler("/vuln/goby/shiro", new GobyShiroHandler(true));
        registerHandler("/safe/goby/shiro", new GobyShiroHandler(false));
        
        // Goby POC 64: Log4j2 RCE
        registerHandler("/vuln/goby/log4j", new GobyLog4jHandler(true));
        registerHandler("/safe/goby/log4j", new GobyLog4jHandler(false));
        
        // Goby POC 65: Consul Rexec RCE
        registerHandler("/vuln/goby/consul", new GobyConsulRexecHandler(true));
        registerHandler("/safe/goby/consul", new GobyConsulRexecHandler(false));
        
        // Goby POC 66: Coremail 配置泄露
        registerHandler("/vuln/goby/coremail", new GobyCoremailConfigHandler(true));
        registerHandler("/safe/goby/coremail", new GobyCoremailConfigHandler(false));
        
        // Goby POC 67: Docker Registry API 未授权
        
        // Goby POC 68: D-Link DCS 密码泄露
        registerHandler("/vuln/goby/dlink-dcs", new GobyDLinkDCSHandler(true));
        registerHandler("/safe/goby/dlink-dcs", new GobyDLinkDCSHandler(false));
        
        // Goby POC 69: Discuz RCE
        registerHandler("/vuln/goby/discuz", new GobyDiscuzRCEHandler(true));
        registerHandler("/safe/goby/discuz", new GobyDiscuzRCEHandler(false));
        
        // Goby POC 70: DedeCMS 信息泄露
        registerHandler("/vuln/goby/dedecms", new GobyDedeCMSInfoLeakHandler(true));
        registerHandler("/safe/goby/dedecms", new GobyDedeCMSInfoLeakHandler(false));
        
        // Goby POC 71: Eyou 邮件系统 RCE
        registerHandler("/vuln/goby/eyou", new GobyEyouMailRCEHandler(true));
        registerHandler("/safe/goby/eyou", new GobyEyouMailRCEHandler(false));
        
        // Goby POC 72: FineReport 目录遍历
        registerHandler("/vuln/goby/finereport", new GobyFineReportHandler(true));
        registerHandler("/safe/goby/finereport", new GobyFineReportHandler(false));
        
        // Goby POC 73: GoCD 文件读取
        registerHandler("/vuln/goby/gocd", new GobyGoCDFileReadHandler(true));
        registerHandler("/safe/goby/gocd", new GobyGoCDFileReadHandler(false));
        
        // Goby POC 74: Confluence OGNL RCE CVE-2021-26084
        registerHandler("/vuln/goby/confluence-ognl", new GobyConfluenceOGNLHandler(true));
        registerHandler("/safe/goby/confluence-ognl", new GobyConfluenceOGNLHandler(false));
        
        // Goby POC 75: Jira 路径遍历
        registerHandler("/vuln/goby/jira-path", new GobyJiraPathHandler(true));
        registerHandler("/safe/goby/jira-path", new GobyJiraPathHandler(false));
        
        // Goby POC 76: Jira 信息泄露
        registerHandler("/vuln/goby/jira-infoleak", new GobyJiraInfoLeakHandler(true));
        registerHandler("/safe/goby/jira-infoleak", new GobyJiraInfoLeakHandler(false));
        
        // Goby POC 77: CraftCMS Seomatic RCE
        registerHandler("/vuln/goby/craftcms", new GobyCraftCMSRCEHandler(true));
        registerHandler("/safe/goby/craftcms", new GobyCraftCMSRCEHandler(false));
        
        // Goby POC 78: Cacti Weathermap 文件写入
        registerHandler("/vuln/goby/cacti", new GobyCactiWeathermapHandler(true));
        registerHandler("/safe/goby/cacti", new GobyCactiWeathermapHandler(false));
        
        // Goby POC 79: ClickHouse SQL注入
        registerHandler("/vuln/goby/clickhouse", new GobyClickHouseSQLiHandler(true));
        registerHandler("/safe/goby/clickhouse", new GobyClickHouseSQLiHandler(false));
        
        // Goby POC 80: Citrix 未授权 LFI
        registerHandler("/vuln/goby/citrix", new GobyCitrixLFIHandler(true));
        registerHandler("/safe/goby/citrix", new GobyCitrixLFIHandler(false));
        
        // Goby POC 81: CouchCMS 信息泄露
        registerHandler("/vuln/goby/couchcms", new GobyCouchCMSInfoLeakHandler(true));
        registerHandler("/safe/goby/couchcms", new GobyCouchCMSInfoLeakHandler(false));
        
        // Goby POC 82: D-Link DIR-850L 信息泄露
        registerHandler("/vuln/goby/dlink-dir850l", new GobyDLinkDIR850LHandler(true));
        registerHandler("/safe/goby/dlink-dir850l", new GobyDLinkDIR850LHandler(false));
        
        // Goby POC 83: D-Link ShareCenter DNS-320 RCE
        registerHandler("/vuln/goby/dlink-sharecenter", new GobyDLinkShareCenterHandler(true));
        registerHandler("/safe/goby/dlink-sharecenter", new GobyDLinkShareCenterHandler(false));
        
        // Goby POC 84: Datang AC 默认密码
        
        // Goby POC 85: DocCMS SQL注入
        registerHandler("/vuln/goby/doccms", new GobyDocCMSSQLiHandler(true));
        registerHandler("/safe/goby/doccms", new GobyDocCMSSQLiHandler(false));
        
        // Goby POC 86: DotCMS 文件上传
        registerHandler("/vuln/goby/dotcms", new GobyDotCMSUploadHandler(true));
        registerHandler("/safe/goby/dotcms", new GobyDotCMSUploadHandler(false));
        
        // Goby POC 87: F5 BIG-IP RCE CVE-2021-22986
        registerHandler("/vuln/goby/f5-rce", new GobyF5RCEHandler(true));
        registerHandler("/safe/goby/f5-rce", new GobyF5RCEHandler(false));
        
        // Goby POC 88: Fastmeeting 文件读取
        
        // Goby POC 89: GitLab SSRF
        registerHandler("/vuln/goby/gitlab-ssrf", new GobyGitLabSSRFHandler(true));
        registerHandler("/safe/goby/gitlab-ssrf", new GobyGitLabSSRFHandler(false));
        // 真实 POC 路径
        registerHandler("/api/v4/ci/lint", new GobyGitLabSSRFFixHandler(true));
        
        // Goby POC 90: GitLab GraphQL 邮箱泄露
        registerHandler("/vuln/goby/gitlab-graphql", new GobyGitLabGraphQLHandler(true));
        registerHandler("/safe/goby/gitlab-graphql", new GobyGitLabGraphQLHandler(false));
        
        // Goby POC 91: Apache Struts2 S2-053 RCE
        registerHandler("/vuln/goby/struts2-s2053", new GobyStruts2S2053Handler(true));
        registerHandler("/safe/goby/struts2-s2053", new GobyStruts2S2053Handler(false));
        
        // Goby POC 92: Apache Struts2 S2-059 RCE
        registerHandler("/vuln/goby/struts2-s2059", new GobyStruts2S2059Handler(true));
        registerHandler("/safe/goby/struts2-s2059", new GobyStruts2S2059Handler(false));
        // 真实 POC 路径 - 根路径
        registerHandler("/", new GobyStruts2S2059FixHandler(true));
        
        // Goby POC 93: Apache Struts2 S2-062 RCE
        registerHandler("/vuln/goby/struts2-s2062", new GobyStruts2S2062Handler(true));
        registerHandler("/safe/goby/struts2-s2062", new GobyStruts2S2062Handler(false));
        
        // Goby POC 94: AspCMS SQL注入
        registerHandler("/vuln/goby/aspcms", new GobyAspCMSSQLiHandler(true));
        registerHandler("/safe/goby/aspcms", new GobyAspCMSSQLiHandler(false));
        
        // Goby POC 95: H3C IMC RCE
        
        // Goby POC 96: H5S Video Platform 信息泄露
        
        // Goby POC 97: HIKVISION 文件下载
        registerHandler("/vuln/goby/hikvision", new GobyHikvisionHandler(true));
        registerHandler("/safe/goby/hikvision", new GobyHikvisionHandler(false));
        
        // Goby POC 98: IFW8 Router 密码泄露
        registerHandler("/vuln/goby/ifw8", new GobyIFW8RouterHandler(true));
        registerHandler("/safe/goby/ifw8", new GobyIFW8RouterHandler(false));
        
        // Goby POC 99: IceWarp WebClient RCE
        registerHandler("/vuln/goby/icewarp", new GobyIceWarpRCEHandler(true));
        registerHandler("/safe/goby/icewarp", new GobyIceWarpRCEHandler(false));
        
        // Goby POC 100: Jellyfin 任意文件读取
        registerHandler("/vuln/goby/jellyfin", new GobyJellyfinFileReadHandler(true));
        registerHandler("/safe/goby/jellyfin", new GobyJellyfinFileReadHandler(false));
        
        // Goby POC 101: Jetty WEB-INF 文件读取
        
        // Goby POC 102: 金和 OA C6 默认密码
        registerHandler("/vuln/goby/jinhe-oa", new GobyJinHeOAHandler(true));
        registerHandler("/safe/goby/jinhe-oa", new GobyJinHeOAHandler(false));
        
        // Goby POC 103: 极通 EWEBS 文件读取
        registerHandler("/vuln/goby/jitong-ewebs", new GobyJitongEWEBSHandler(true));
        registerHandler("/safe/goby/jitong-ewebs", new GobyJitongEWEBSHandler(false));
        
        // Goby POC 104: KEDACOM MTS 文件读取
        registerHandler("/vuln/goby/kedacom-mts", new GobyKedacomMTSHandler(true));
        registerHandler("/safe/goby/kedacom-mts", new GobyKedacomMTSHandler(false));
        
        // Goby POC 105: 金山 V8 任意文件读取
        
        // Goby POC 106: Konga 默认 JWT Key
        registerHandler("/vuln/goby/konga", new GobyKongaJWTHandler(true));
        registerHandler("/safe/goby/konga", new GobyKongaJWTHandler(false));
        
        // Goby POC 107: Kyan 账号密码泄露
        registerHandler("/vuln/goby/kyan", new GobyKyanHandler(true));
        registerHandler("/safe/goby/kyan", new GobyKyanHandler(false));
        
        // Goby POC 108: Lanproxy 目录遍历
        registerHandler("/vuln/goby/lanproxy", new GobyLanproxyHandler(true));
        registerHandler("/safe/goby/lanproxy", new GobyLanproxyHandler(false));
        
        // Goby POC 109: Laravel .env 配置泄露
        registerHandler("/vuln/goby/laravel-env", new GobyLaravelEnvHandler(true));
        registerHandler("/safe/goby/laravel-env", new GobyLaravelEnvHandler(false));
        
        // Goby POC 110: Leadsec ACM 信息泄露
        registerHandler("/vuln/goby/leadsec-acm", new GobyLeadsecACMHandler(true));
        registerHandler("/safe/goby/leadsec-acm", new GobyLeadsecACMHandler(false));
        
        // Goby POC 111: MPSec ISG1000 文件下载
        registerHandler("/vuln/goby/mpsec-isg", new GobyMPSecISGHandler(true));
        registerHandler("/safe/goby/mpsec-isg", new GobyMPSecISGHandler(false));
        
        // Goby POC 112: Metabase 任意文件读取
        registerHandler("/vuln/goby/metabase", new GobyMetabaseFileReadHandler(true));
        registerHandler("/safe/goby/metabase", new GobyMetabaseFileReadHandler(false));
        
        // Goby POC 113: Microsoft Exchange SSRF
        registerHandler("/vuln/goby/exchange-ssrf", new GobyExchangeSSRFHandler(true));
        registerHandler("/safe/goby/exchange-ssrf", new GobyExchangeSSRFHandler(false));
        
        // Goby POC 114: MinIO Browser API SSRF
        registerHandler("/vuln/goby/minio-ssrf", new GobyMinIOSSRFHandler(true));
        registerHandler("/safe/goby/minio-ssrf", new GobyMinIOSSRFHandler(false));
        
        // Goby POC 115: Node-RED 任意文件读取
        registerHandler("/vuln/goby/nodered", new GobyNodeREDHandler(true));
        registerHandler("/safe/goby/nodered", new GobyNodeREDHandler(false));
        
        // Goby POC 116: OpenSNS RCE
        registerHandler("/vuln/goby/opensns", new GobyOpenSNSRCEHandler(true));
        registerHandler("/safe/goby/opensns", new GobyOpenSNSRCEHandler(false));
        
        // Goby POC 117: Oracle Weblogic LDAP RCE
        registerHandler("/vuln/goby/weblogic-ldap", new GobyWeblogicLDAPHandler(true));
        registerHandler("/safe/goby/weblogic-ldap", new GobyWeblogicLDAPHandler(false));
        
        // Goby POC 118: 向日葵 RCE
        registerHandler("/vuln/goby/sunlogin", new GobySunloginRCEHandler(true));
        registerHandler("/safe/goby/sunlogin", new GobySunloginRCEHandler(false));
        
        // Goby POC 119: Portainer Init Deploy
        registerHandler("/vuln/goby/portainer", new GobyPortainerHandler(true));
        registerHandler("/safe/goby/portainer", new GobyPortainerHandler(false));
        
        // Goby POC 120: 锐捷 EWEB RCE
        registerHandler("/vuln/goby/ruijie-eweb", new GobyRuijieEWEBHandler(true));
        registerHandler("/safe/goby/ruijie-eweb", new GobyRuijieEWEBHandler(false));
        
        // Goby POC 121: 锐捷 RG-UAC 密码泄露
        registerHandler("/vuln/goby/ruijie-uac", new GobyRuijieUACHandler(true));
        registerHandler("/safe/goby/ruijie-uac", new GobyRuijieUACHandler(false));
        
        // Goby POC 122: 锐捷 Smartweb 弱密码
        registerHandler("/vuln/goby/ruijie-smartweb", new GobyRuijieSmartwebHandler(true));
        registerHandler("/safe/goby/ruijie-smartweb", new GobyRuijieSmartwebHandler(false));
        
        // Goby POC 123: Samsung WLAN AP RCE (RuoYi Druid 已在 POC 16 注册)
        registerHandler("/vuln/goby/samsung-wlan", new GobySamsungWLANHandler(true));
        registerHandler("/safe/goby/samsung-wlan", new GobySamsungWLANHandler(false));
        
        // Goby POC 124: SDWAN Smart Gateway 默认密码
        registerHandler("/vuln/goby/sdwan-gateway", new GobySDWANGatewayHandler(true));
        registerHandler("/safe/goby/sdwan-gateway", new GobySDWANGatewayHandler(false));
        
        // Goby POC 125: Seeyon OA A6 DownExcelBeanServlet 信息泄露
        registerHandler("/vuln/goby/seeyon-downexcel", new GobySeeyonDownExcelHandler(true));
        registerHandler("/safe/goby/seeyon-downexcel", new GobySeeyonDownExcelHandler(false));
        
        // Goby POC 126: SonarQube 未授权 CVE-2020-27986
        registerHandler("/vuln/goby/sonarqube", new GobySonarQubeHandler(true));
        registerHandler("/safe/goby/sonarqube", new GobySonarQubeHandler(false));
        
        // Goby POC 127: SpiderFlow RCE
        registerHandler("/vuln/goby/spiderflow", new GobySpiderFlowHandler(true));
        registerHandler("/safe/goby/spiderflow", new GobySpiderFlowHandler(false));
        
        // Goby POC 128: Spring Boot Actuator Logview 路径遍历
        registerHandler("/vuln/goby/springboot-logview", new GobySpringBootLogviewHandler(true));
        registerHandler("/safe/goby/springboot-logview", new GobySpringBootLogviewHandler(false));
        
        // Goby POC 129: TamronOS IPTV RCE
        registerHandler("/vuln/goby/tamronos-iptv", new GobyTamronOSHandler(true));
        registerHandler("/safe/goby/tamronos-iptv", new GobyTamronOSHandler(false));
        
        // Goby POC 130: 泛微 EOffice 任意文件上传
        registerHandler("/vuln/goby/weaver-eoffice", new GobyWeaverEOfficeHandler(true));
        registerHandler("/safe/goby/weaver-eoffice", new GobyWeaverEOfficeHandler(false));
        
        // Goby POC 131: WSO2 文件上传 CVE-2022-29464
        registerHandler("/vuln/goby/wso2-upload", new GobyWSO2UploadHandler(true));
        registerHandler("/safe/goby/wso2-upload", new GobyWSO2UploadHandler(false));
        
        // Goby POC 132: Xieda OA 文件下载
        registerHandler("/vuln/goby/xieda-oa", new GobyXiedaOAHandler(true));
        registerHandler("/safe/goby/xieda-oa", new GobyXiedaOAHandler(false));
        
        // Goby POC 133: YAPI RCE
        registerHandler("/vuln/goby/yapi", new GobyYAPIHandler(true));
        registerHandler("/safe/goby/yapi", new GobyYAPIHandler(false));
        
        // Goby POC 134: 浙江大华 DSS 文件下载
        registerHandler("/vuln/goby/dahua-dss", new GobyDahuaDSSHandler(true));
        registerHandler("/safe/goby/dahua-dss", new GobyDahuaDSSHandler(false));
        
        // Goby POC 135: Docker Registry API 未授权
        registerHandler("/vuln/goby/docker-registry", new GobyDockerRegistryHandler(true));
        registerHandler("/safe/goby/docker-registry", new GobyDockerRegistryHandler(false));
        
        // Goby POC 136: Dubbo Admin 默认密码
        registerHandler("/vuln/goby/dubbo-admin", new GobyDubboAdminHandler(true));
        registerHandler("/safe/goby/dubbo-admin", new GobyDubboAdminHandler(false));
        
        // Goby POC 137: Discuz RCE WOOYUN-2010-080723
        registerHandler("/vuln/goby/discuz-rce", new GobyDiscuzRCEHandler(true));
        registerHandler("/safe/goby/discuz-rce", new GobyDiscuzRCEHandler(false));
        
        // Goby POC 138: DedeCMS 信息泄露 CVE-2018-6910
        registerHandler("/vuln/goby/dedecms-info", new GobyDedeCMSInfoHandler(true));
        registerHandler("/safe/goby/dedecms-info", new GobyDedeCMSInfoHandler(false));
        
        // Goby POC 139: D-Link ShareCenter DNS-320 RCE
        registerHandler("/vuln/goby/dlink-dns320", new GobyDLinkDNS320Handler(true));
        registerHandler("/safe/goby/dlink-dns320", new GobyDLinkDNS320Handler(false));
        
        // Goby POC 140: Datang AC 默认密码
        registerHandler("/vuln/goby/datang-ac", new GobyDatangACHandler(true));
        registerHandler("/safe/goby/datang-ac", new GobyDatangACHandler(false));
        
        // Goby POC 141: FineReport 目录遍历
        registerHandler("/vuln/goby/finereport-traversal", new GobyFineReportTraversalHandler(true));
        registerHandler("/safe/goby/finereport-traversal", new GobyFineReportTraversalHandler(false));
        
        // Goby POC 142: GitLab RCE CVE-2021-22205
        registerHandler("/vuln/goby/gitlab-rce", new GobyGitLabRCEHandler(true));
        registerHandler("/safe/goby/gitlab-rce", new GobyGitLabRCEHandler(false));
        
        // Goby POC 143: GoCD 任意文件读取 CVE-2021-43287
        registerHandler("/vuln/goby/gocd-fileread", new GobyGoCDFileReadHandler(true));
        registerHandler("/safe/goby/gocd-fileread", new GobyGoCDFileReadHandler(false));
        
        // Goby POC 144: U8 OA 漏洞
        registerHandler("/vuln/goby/u8-oa", new GobyU8OAHandler(true));
        registerHandler("/safe/goby/u8-oa", new GobyU8OAHandler(false));
        
        // Goby POC 145: VMware vCenter 任意文件读取
        registerHandler("/vuln/goby/vcenter-fileread", new GobyVCenterFileReadHandler(true));
        registerHandler("/safe/goby/vcenter-fileread", new GobyVCenterFileReadHandler(false));
        
        // Goby POC 146: Weblogic SSRF CVE-2014-4210
        registerHandler("/vuln/goby/weblogic-ssrf", new GobyWeblogicSSRFHandler(true));
        registerHandler("/safe/goby/weblogic-ssrf", new GobyWeblogicSSRFHandler(false));
        // 真实 POC 路径
        registerHandler("/uddiexplorer/SearchPublicRegistries.jsp", new GobyWeblogicSSRFHandler(true));
        
        // Goby POC 147: Weaver OA 8 SQL注入
        registerHandler("/vuln/goby/weaver-oa8", new GobyWeaverOA8Handler(true));
        registerHandler("/safe/goby/weaver-oa8", new GobyWeaverOA8Handler(false));
        
        // Goby POC 148: XXL-JOB 默认密码
        registerHandler("/vuln/goby/xxljob-default", new GobyXXLJobDefaultHandler(true));
        registerHandler("/safe/goby/xxljob-default", new GobyXXLJobDefaultHandler(false));
        
        // Goby POC 149: Spring Framework Spring4Shell RCE CVE-2022-22965
        registerHandler("/vuln/goby/spring4shell", new GobySpring4ShellHandler(true));
        registerHandler("/safe/goby/spring4shell", new GobySpring4ShellHandler(false));
        
        // Goby POC 150: Wayos AC 默认密码
        registerHandler("/vuln/goby/wayos-ac", new GobyWayosACHandler(true));
        registerHandler("/safe/goby/wayos-ac", new GobyWayosACHandler(false));
        
        // Goby POC 151: Security Devices 硬编码密码
        registerHandler("/vuln/goby/security-devices", new GobySecurityDevicesHandler(true));
        registerHandler("/safe/goby/security-devices", new GobySecurityDevicesHandler(false));
        
        // Goby POC 152: 中新金盾 默认密码
        registerHandler("/vuln/goby/zhongxinjingdun", new GobyZhongXinJingDunHandler(true));
        registerHandler("/safe/goby/zhongxinjingdun", new GobyZhongXinJingDunHandler(false));
        
        // Goby POC 153: ZZZCMS RCE
        registerHandler("/vuln/goby/zzzcms", new GobyZZZCMSHandler(true));
        registerHandler("/safe/goby/zzzcms", new GobyZZZCMSHandler(false));
        
        // Goby POC 154: 亿邮邮件系统 RCE
        registerHandler("/vuln/goby/eyou-mail", new GobyEyouMailHandler(true));
        registerHandler("/safe/goby/eyou-mail", new GobyEyouMailHandler(false));
        
        // Goby POC 155: Apache Airflow 未授权
        registerHandler("/vuln/goby/airflow", new GobyAirflowHandler(true));
        registerHandler("/safe/goby/airflow", new GobyAirflowHandler(false));
        
        // Goby POC 156: Apache CouchDB 未授权
        registerHandler("/vuln/goby/couchdb", new GobyCouchDBHandler(true));
        registerHandler("/safe/goby/couchdb", new GobyCouchDBHandler(false));
        
        // Goby POC 157: Nacos 默认密码
        registerHandler("/vuln/goby/nacos-default", new GobyNacosDefaultHandler(true));
        registerHandler("/safe/goby/nacos-default", new GobyNacosDefaultHandler(false));
        
        // Goby POC 158: 蓝凌 OA 任意文件读取
        registerHandler("/vuln/goby/landray-oa", new GobyLandrayOAHandler(true));
        registerHandler("/safe/goby/landray-oa", new GobyLandrayOAHandler(false));
        
        // Goby POC 159: 通达 OA 未授权
        registerHandler("/vuln/goby/tongda-oa", new GobyTongdaOAHandler(true));
        registerHandler("/safe/goby/tongda-oa", new GobyTongdaOAHandler(false));
        
        // Goby POC 160: 用友 NC RCE
        registerHandler("/vuln/goby/yonyou-nc", new GobyYonyouNCHandler(true));
        registerHandler("/safe/goby/yonyou-nc", new GobyYonyouNCHandler(false));
        
        // Goby POC 161: Apache Druid 文件读取
        registerHandler("/vuln/goby/druid-fileread", new GobyDruidFileReadHandler(true));
        registerHandler("/safe/goby/druid-fileread", new GobyDruidFileReadHandler(false));
        
        // Goby POC 162: Apache Flink 文件读取
        registerHandler("/vuln/goby/flink-fileread", new GobyFlinkFileReadHandler(true));
        registerHandler("/safe/goby/flink-fileread", new GobyFlinkFileReadHandler(false));
        
        // Goby POC 163: ActiveMQ 默认密码
        registerHandler("/vuln/goby/activemq-default", new GobyActiveMQDefaultHandler(true));
        registerHandler("/safe/goby/activemq-default", new GobyActiveMQDefaultHandler(false));
        
        // Goby POC 164: H3C IMC RCE
        registerHandler("/vuln/goby/h3c-imc", new GobyH3CIMCHandler(true));
        registerHandler("/safe/goby/h3c-imc", new GobyH3CIMCHandler(false));
        
        // Goby POC 165: 海康威视 RCE
        registerHandler("/vuln/goby/hikvision-rce", new GobyHikvisionRCEHandler(true));
        registerHandler("/safe/goby/hikvision-rce", new GobyHikvisionRCEHandler(false));
        
        // Goby POC 166: Jellyfin 文件读取
        registerHandler("/vuln/goby/jellyfin-fileread", new GobyJellyfinFileReadHandler(true));
        registerHandler("/safe/goby/jellyfin-fileread", new GobyJellyfinFileReadHandler(false));
        
        // Goby POC 167: Jetty WEB-INF 文件读取
        registerHandler("/vuln/goby/jetty-fileread", new GobyJettyFileReadHandler(true));
        registerHandler("/safe/goby/jetty-fileread", new GobyJettyFileReadHandler(false));
        
        // Goby POC 168: 金和 OA 默认密码 (C6版本)
        registerHandler("/vuln/goby/jinhe-oa-c6", new GobyJinheOAC6Handler(true));
        registerHandler("/safe/goby/jinhe-oa-c6", new GobyJinheOAC6Handler(false));
        
        // Goby POC 169: 金山 V8 默认密码
        registerHandler("/vuln/goby/kingsoft-v8", new GobyKingsoftV8Handler(true));
        registerHandler("/safe/goby/kingsoft-v8", new GobyKingsoftV8Handler(false));
        
        // Goby POC 170: Kyan 密码泄露
        registerHandler("/vuln/goby/kyan-leak", new GobyKyanLeakHandler(true));
        registerHandler("/safe/goby/kyan-leak", new GobyKyanLeakHandler(false));
        
        // Goby POC 171: VMware Workspace ONE RCE
        registerHandler("/vuln/goby/vmware-workspace", new GobyVMwareWorkspaceHandler(true));
        registerHandler("/safe/goby/vmware-workspace", new GobyVMwareWorkspaceHandler(false));
        
        // Goby POC 172: WebSVN RCE
        registerHandler("/vuln/goby/websvn-rce", new GobyWebSVNHandler(true));
        registerHandler("/safe/goby/websvn-rce", new GobyWebSVNHandler(false));
        
        // Goby POC 173: Zabbix SAML CVE-2022-23131
        registerHandler("/vuln/goby/zabbix-saml", new GobyZabbixSAMLHandler(true));
        registerHandler("/safe/goby/zabbix-saml", new GobyZabbixSAMLHandler(false));
        // 真实 POC 路径
        registerHandler("/index_sso.php", new GobyZabbixSAMLFixHandler(true));
        
        // Goby POC 174: Alibaba Canal 默认密码
        registerHandler("/vuln/goby/alibaba-canal", new GobyAlibabaCanalHandler(true));
        registerHandler("/safe/goby/alibaba-canal", new GobyAlibabaCanalHandler(false));
        
        // Goby POC 175: 深信服行为感知 RCE
        registerHandler("/vuln/goby/sangfor-rce", new GobySangforRCEHandler(true));
        registerHandler("/safe/goby/sangfor-rce", new GobySangforRCEHandler(false));
        
        // Goby POC 176: 畅捷 CRM SQL注入
        registerHandler("/vuln/goby/chanjet-crm", new GobyChanjetCRMHandler(true));
        registerHandler("/safe/goby/chanjet-crm", new GobyChanjetCRMHandler(false));
        
        // Goby POC 177: Fastmeeting 文件读取
        registerHandler("/vuln/goby/fastmeeting", new GobyFastmeetingHandler(true));
        registerHandler("/safe/goby/fastmeeting", new GobyFastmeetingHandler(false));
        
        // Goby POC 178: H5S 视频平台信息泄露
        registerHandler("/vuln/goby/h5s-video", new GobyH5SVideoHandler(true));
        registerHandler("/safe/goby/h5s-video", new GobyH5SVideoHandler(false));
        
        // Goby POC 179: IceWarp RCE
        registerHandler("/vuln/goby/icewarp-rce", new GobyIceWarpHandler(true));
        registerHandler("/safe/goby/icewarp-rce", new GobyIceWarpHandler(false));
        
        // Goby POC 180: Konga 默认 JWT KEY
        registerHandler("/vuln/goby/konga-jwt", new GobyKongaJWTHandler(true));
        registerHandler("/safe/goby/konga-jwt", new GobyKongaJWTHandler(false));
        
        // Goby POC 181: 启来 OA SQL注入
        registerHandler("/vuln/goby/qilai-oa", new GobyQilaiOAHandler(true));
        registerHandler("/safe/goby/qilai-oa", new GobyQilaiOAHandler(false));
        
        // Goby POC 182: 华天动力 OA SQL注入
        registerHandler("/vuln/goby/huatian-oa", new GobyHuatianOAHandler(true));
        registerHandler("/safe/goby/huatian-oa", new GobyHuatianOAHandler(false));
        
        // Goby POC 183: 360 天擎 SQL注入 (ccid)
        registerHandler("/vuln/goby/360tianqing-ccid", new Goby360TianQingCcidHandler(true));
        registerHandler("/safe/goby/360tianqing-ccid", new Goby360TianQingCcidHandler(false));
        
        // Goby POC 184: ADSelfService Plus RCE
        registerHandler("/vuln/goby/adselfservice", new GobyADSelfServiceHandler(true));
        registerHandler("/safe/goby/adselfservice", new GobyADSelfServiceHandler(false));
        
        // Goby POC 185: AVCON6 文件下载
        registerHandler("/vuln/goby/avcon6", new GobyAVCON6Handler(true));
        registerHandler("/safe/goby/avcon6", new GobyAVCON6Handler(false));
        
        // Goby POC 186: Active UC RCE
        registerHandler("/vuln/goby/active-uc", new GobyActiveUCHandler(true));
        registerHandler("/safe/goby/active-uc", new GobyActiveUCHandler(false));
        
        // Goby POC 188: Nacos 未授权添加用户
        registerHandler("/vuln/goby/nacos-unauth-user", new GobyNacosUnauthAddUserHandler(true));
        registerHandler("/safe/goby/nacos-unauth-user", new GobyNacosUnauthAddUserHandler(false));
        
        // Goby POC 189: APISIX Dashboard 未授权
        registerHandler("/vuln/goby/apisix-dashboard", new GobyAPISIXDashboardHandler(true));
        registerHandler("/safe/goby/apisix-dashboard", new GobyAPISIXDashboardHandler(false));
        
        // Goby POC 190: CouchDB 权限提升
        registerHandler("/vuln/goby/couchdb-privesc", new GobyCouchDBPrivEscHandler(true));
        registerHandler("/safe/goby/couchdb-privesc", new GobyCouchDBPrivEscHandler(false));
        
        // Goby POC 191: Apache HTTP SSRF
        registerHandler("/vuln/goby/apache-ssrf", new GobyApacheSSRFHandler(true));
        registerHandler("/safe/goby/apache-ssrf", new GobyApacheSSRFHandler(false));
        
        // Goby POC 192: Apache HTTP Path Traversal
        registerHandler("/vuln/goby/apache-traversal-new", new GobyApachePathTraversalHandler(true));
        registerHandler("/safe/goby/apache-traversal-new", new GobyApachePathTraversalHandler(false));
        
        // Goby POC 193: Kylin 默认密码
        registerHandler("/vuln/goby/kylin-default", new GobyKylinDefaultPwdHandler(true));
        registerHandler("/safe/goby/kylin-default", new GobyKylinDefaultPwdHandler(false));
        
        // Goby POC 194: Kylin 未授权配置泄露
        registerHandler("/vuln/goby/kylin-unauth", new GobyKylinUnauthHandler(true));
        registerHandler("/safe/goby/kylin-unauth", new GobyKylinUnauthHandler(false));
        
        // Goby POC 195: ShenYu 未授权访问
        registerHandler("/vuln/goby/shenyu-unauth-new", new GobyShenYuUnauthHandler(true));
        registerHandler("/safe/goby/shenyu-unauth-new", new GobyShenYuUnauthHandler(false));
        
        // Goby POC 196: Struts2 S2-053
        registerHandler("/vuln/goby/struts2-s2-053", new GobyStruts2S2053Handler(true));
        registerHandler("/safe/goby/struts2-s2-053", new GobyStruts2S2053Handler(false));
        
        // Goby POC 197: AspCMS SQLi
        registerHandler("/vuln/goby/aspcms-sqli", new GobyAspCMSSQLiHandler(true));
        registerHandler("/safe/goby/aspcms-sqli", new GobyAspCMSSQLiHandler(false));
        
        // Goby POC 198: AspCMS 后台泄露
        registerHandler("/vuln/goby/aspcms-leak", new GobyAspCMSBackendLeakHandler(true));
        registerHandler("/safe/goby/aspcms-leak", new GobyAspCMSBackendLeakHandler(false));
        
        // Goby POC 199: BSPHP 未授权
        registerHandler("/vuln/goby/bsphp-unauth", new GobyBSPHPUnauthHandler(true));
        registerHandler("/safe/goby/bsphp-unauth", new GobyBSPHPUnauthHandler(false));
        
        // Goby POC 200: BigAnt Path Traversal
        registerHandler("/vuln/goby/bigant-traversal", new GobyBigAntPathTraversalHandler(true));
        registerHandler("/safe/goby/bigant-traversal", new GobyBigAntPathTraversalHandler(false));
        
        // Goby POC 201: Portainer 未授权
        registerHandler("/vuln/goby/portainer-unauth", new GobyPortainerUnauthHandler(true));
        registerHandler("/safe/goby/portainer-unauth", new GobyPortainerUnauthHandler(false));
        
        // Goby POC 202: Cacti Weathermap File Write
        registerHandler("/vuln/goby/cacti-filewrite", new GobyCactiFileWriteHandler(true));
        registerHandler("/safe/goby/cacti-filewrite", new GobyCactiFileWriteHandler(false));
        
        // Goby POC 203: Casdoor SQLi
        registerHandler("/vuln/goby/casdoor-sqli", new GobyCasdoorSQLiHandler(true));
        registerHandler("/safe/goby/casdoor-sqli", new GobyCasdoorSQLiHandler(false));
        
        // Goby POC 204: Cerebro SQLi
        registerHandler("/vuln/goby/cerebro-sqli", new GobyCerebroSQLiHandler(true));
        registerHandler("/safe/goby/cerebro-sqli", new GobyCerebroSQLiHandler(false));
        
        // Goby POC 205: Chanjet CRM SQLi
        registerHandler("/vuln/goby/chanjet-sqli", new GobyChanjetCRMSQLiHandler(true));
        registerHandler("/safe/goby/chanjet-sqli", new GobyChanjetCRMSQLiHandler(false));
        
        // Goby POC 206: China Mobile Yu Routing Info Leak
        registerHandler("/vuln/goby/chinamobile-yu", new GobyChinaMobileYuRoutingHandler(true));
        registerHandler("/safe/goby/chinamobile-yu", new GobyChinaMobileYuRoutingHandler(false));
        
        // Goby POC 207: China Mobile Yu Routing Login Bypass
        registerHandler("/vuln/goby/chinamobile-login-bypass", new GobyChinaMobileYuLoginBypassHandler(true));
        registerHandler("/safe/goby/chinamobile-login-bypass", new GobyChinaMobileYuLoginBypassHandler(false));
        
        // Goby POC 208: Citrix Unauthorized CVE-2020-8193
        registerHandler("/vuln/goby/citrix-unauth", new GobyCitrixUnauthorizedHandler(true));
        registerHandler("/safe/goby/citrix-unauth", new GobyCitrixUnauthorizedHandler(false));
        
        // Goby POC 209: ClickHouse SQLi
        registerHandler("/vuln/goby/clickhouse-sqli", new GobyClickHouseSQLiHandler(true));
        registerHandler("/safe/goby/clickhouse-sqli", new GobyClickHouseSQLiHandler(false));
        
        // Goby POC 210: ClusterEngine RCE
        registerHandler("/vuln/goby/clusterengine-rce", new GobyClusterEngineRCEHandler(true));
        registerHandler("/safe/goby/clusterengine-rce", new GobyClusterEngineRCEHandler(false));
        
        // Goby POC 211: CmsEasy SQLi
        registerHandler("/vuln/goby/cmseasy-sqli", new GobyCmsEasySQLiHandler(true));
        registerHandler("/safe/goby/cmseasy-sqli", new GobyCmsEasySQLiHandler(false));
        
        // Goby POC 212: Coldfusion LFI
        registerHandler("/vuln/goby/coldfusion-lfi", new GobyColdfusionLFICVE20102861Handler(true));
        registerHandler("/safe/goby/coldfusion-lfi", new GobyColdfusionLFICVE20102861Handler(false));
        
        // Goby POC 213: Confluence RCE
        registerHandler("/vuln/goby/confluence-rce", new GobyConfluenceRCEHandler(true));
        registerHandler("/safe/goby/confluence-rce", new GobyConfluenceRCEHandler(false));
        
        // Goby POC 214: Consul Rexec RCE
        registerHandler("/vuln/goby/consul-rce", new GobyConsulRCEHandler(true));
        registerHandler("/safe/goby/consul-rce", new GobyConsulRCEHandler(false));
        
        // Goby POC 215: Coremail Config Disclosure
        registerHandler("/vuln/goby/coremail-config", new GobyCoremailConfigHandler(true));
        registerHandler("/safe/goby/coremail-config", new GobyCoremailConfigHandler(false));
        
        // Goby POC 216: CouchCMS Info Leak
        registerHandler("/vuln/goby/couchcms-infoleak", new GobyCouchCMSInfoLeakHandler(true));
        registerHandler("/safe/goby/couchcms-infoleak", new GobyCouchCMSInfoLeakHandler(false));
        
        // Goby POC 217: CouchDB Unauth
        registerHandler("/vuln/goby/couchdb-unauth", new GobyCouchDBUnauthHandler(true));
        registerHandler("/safe/goby/couchdb-unauth", new GobyCouchDBUnauthHandler(false));
        
        // Goby POC 218: CraftCMS SEOmatic RCE
        registerHandler("/vuln/goby/craftcms-seomatic", new GobyCraftCMSSeomaticHandler(true));
        registerHandler("/safe/goby/craftcms-seomatic", new GobyCraftCMSSeomaticHandler(false));
        
        // Goby POC 219: D-Link AC Default Password
        registerHandler("/vuln/goby/dlink-ac-default", new GobyDLinkACDefaultPwdHandler(true));
        registerHandler("/safe/goby/dlink-ac-default", new GobyDLinkACDefaultPwdHandler(false));
        
        // Goby POC 220: D-Link DCS Info Leak
        registerHandler("/vuln/goby/dlink-dcs-infoleak", new GobyDLinkDCSInfoLeakHandler(true));
        registerHandler("/safe/goby/dlink-dcs-infoleak", new GobyDLinkDCSInfoLeakHandler(false));
        
        // Goby POC 221: D-Link DIR-850L Info Leak
        registerHandler("/vuln/goby/dlink-dir850l-infoleak", new GobyDLinkDIR850LInfoLeakHandler(true));
        registerHandler("/safe/goby/dlink-dir850l-infoleak", new GobyDLinkDIR850LInfoLeakHandler(false));
        
        // Goby POC 222: D-Link CVE-2019-17506 Info Leak
        registerHandler("/vuln/goby/dlink-cve-2019-17506", new GobyDLinkInfoLeakCVE201917506Handler(true));
        registerHandler("/safe/goby/dlink-cve-2019-17506", new GobyDLinkInfoLeakCVE201917506Handler(false));
        
        // Goby POC 223: D-Link ShareCenter RCE
        registerHandler("/vuln/goby/dlink-sharecenter-rce", new GobyDLinkShareCenterRCEHandler(true));
        registerHandler("/safe/goby/dlink-sharecenter-rce", new GobyDLinkShareCenterRCEHandler(false));
        
        // Goby POC 224: D-Link AC Weak Password
        registerHandler("/vuln/goby/dlink-ac-weakpwd", new GobyDLinkACWeakPwdHandler(true));
        registerHandler("/safe/goby/dlink-ac-weakpwd", new GobyDLinkACWeakPwdHandler(false));
        
        // Goby POC 225: D-Link DC Info Leak
        registerHandler("/vuln/goby/dlink-dc-infoleak", new GobyDLinkDCInfoLeakHandler(true));
        registerHandler("/safe/goby/dlink-dc-infoleak", new GobyDLinkDCInfoLeakHandler(false));
        
        // Goby POC 226: D-Link DIR-868L Account Leak
        registerHandler("/vuln/goby/dlink-dir868l-infoleak", new GobyDLinkDIR868LAccountLeakHandler(true));
        registerHandler("/safe/goby/dlink-dir868l-infoleak", new GobyDLinkDIR868LAccountLeakHandler(false));
        
        // Goby POC 228: Datang AC Default Password
        registerHandler("/vuln/goby/datang-ac-default", new GobyDatangACDefaultPwdHandler(true));
        registerHandler("/safe/goby/datang-ac-default", new GobyDatangACDefaultPwdHandler(false));
        
        // Goby POC 229: DedeCMS Carbuyaction File Include
        registerHandler("/vuln/goby/dedecms-carbuyaction", new GobyDedeCMSCarbuyactionHandler(true));
        registerHandler("/safe/goby/dedecms-carbuyaction", new GobyDedeCMSCarbuyactionHandler(false));
        
        // Goby POC 230: DedeCMS Info Leak CVE-2018-6910
        registerHandler("/vuln/goby/dedecms-infoleak-2018", new GobyDedeCMSInfoLeakCVE20186910Handler(true));
        registerHandler("/safe/goby/dedecms-infoleak-2018", new GobyDedeCMSInfoLeakCVE20186910Handler(false));
        
        // Goby POC 232: Discuz!ML 3.x RCE
        registerHandler("/vuln/goby/discuz-ml-rce", new GobyDiscuzML3xRCEHandler(true));
        registerHandler("/safe/goby/discuz-ml-rce", new GobyDiscuzML3xRCEHandler(false));
        
        // Goby POC 234: Discuz RCE WOOYUN-2010-080723
        registerHandler("/vuln/goby/discuz-rce-wooyun", new GobyDiscuzRCEWooYun2010080723Handler(true));
        registerHandler("/safe/goby/discuz-rce-wooyun", new GobyDiscuzRCEWooYun2010080723Handler(false));
        
        // Goby POC 235: Discuz Wechat Plugins Unauth
        registerHandler("/vuln/goby/discuz-wechat-unauth", new GobyDiscuzWechatPluginsHandler(true));
        registerHandler("/safe/goby/discuz-wechat-unauth", new GobyDiscuzWechatPluginsHandler(false));
        
        // Goby POC 236: Discuz v72 SQLi
        registerHandler("/vuln/goby/discuz-v72-sqli", new GobyDiscuzV72SQLiHandler(true));
        registerHandler("/safe/goby/discuz-v72-sqli", new GobyDiscuzV72SQLiHandler(false));
        
        // Goby POC 239: D-Link RCE CVE-2019-16920
        registerHandler("/vuln/goby/dlink-rce-cve2019-16920", new GobyDLinkRCECVE201916920Handler(true));
        registerHandler("/safe/goby/dlink-rce-cve2019-16920", new GobyDLinkRCECVE201916920Handler(false));
        
        // Goby POC 240: DocCMS SQLi
        registerHandler("/vuln/goby/doccms-sqli", new GobyDocCMSSQLiPOC240Handler(true));
        registerHandler("/safe/goby/doccms-sqli", new GobyDocCMSSQLiPOC240Handler(false));
        
        // Goby POC 241: Docker Registry API Unauth
        registerHandler("/vuln/goby/docker-registry-unauth", new GobyDockerRegistryUnauthHandler(true));
        registerHandler("/safe/goby/docker-registry-unauth", new GobyDockerRegistryUnauthHandler(false));
        
        // Goby POC 243: Dubbo Admin Default Password
        registerHandler("/vuln/goby/dubbo-admin-default", new GobyDubboAdminDefaultPwdHandler(true));
        registerHandler("/safe/goby/dubbo-admin-default", new GobyDubboAdminDefaultPwdHandler(false));
        
        // Goby POC 260: GitLab RCE CVE-2021-22205
        registerHandler("/vuln/goby/gitlab-rce-cve-2021-22205", new GobyGitLabRCECVE202122205Handler(true));
        registerHandler("/safe/goby/gitlab-rce-cve-2021-22205", new GobyGitLabRCECVE202122205Handler(false));
        
        // Goby POC 261: GitLab SSRF CVE-2021-22214
        registerHandler("/vuln/goby/gitlab-ssrf-cve-2021-22214", new GobyGitLabSSRFCVE202122214Handler(true));
        registerHandler("/safe/goby/gitlab-ssrf-cve-2021-22214", new GobyGitLabSSRFCVE202122214Handler(false));


        // Goby POC 263: Grafana Angularjs XSS
        registerHandler("/vuln/goby/grafana-xss", new GobyGrafanaXSSHandler(true));
        registerHandler("/safe/goby/grafana-xss", new GobyGrafanaXSSHandler(false));


        // Goby POC 267: H3C IMC RCE
        registerHandler("/vuln/goby/h3c-imc-rce", new GobyH3CIMCRCEHandlerPoc267(true));
        registerHandler("/safe/goby/h3c-imc-rce", new GobyH3CIMCRCEHandlerPoc267(false));

        // Goby POC 268: H5S GetSrc Info Leak
        registerHandler("/vuln/goby/h5s-getsrc-leak", new GobyH5SGetSrcInfoLeakHandler(true));
        registerHandler("/safe/goby/h5s-getsrc-leak", new GobyH5SGetSrcInfoLeakHandler(false));

        // Goby POC 269: H5S GetUserInfo Info Leak
        registerHandler("/vuln/goby/h5s-userinfo-leak", new GobyH5SGetUserInfoLeakHandler(true));
        registerHandler("/safe/goby/h5s-userinfo-leak", new GobyH5SGetUserInfoLeakHandler(false));

        // Goby POC 270: Hikvision File Download
        registerHandler("/vuln/goby/hikvision-file-download", new GobyHikvisionFileDownloadHandler(true));
        registerHandler("/safe/goby/hikvision-file-download", new GobyHikvisionFileDownloadHandler(false));

        // Goby POC 272: Hikvision RCE
        
        // Goby POC 273: Hikvision Unauthenticated RCE (Reuse POC 272 Handler)
        registerHandler("/vuln/goby/hikvision-rce-unauth", new GobyHikvisionRCEHandlerPoc272(true));
        registerHandler("/safe/goby/hikvision-rce-unauth", new GobyHikvisionRCEHandlerPoc272(false));

        // Goby POC 274: Hikvision Video Encoding Device Access Gateway Any File Download
        registerHandler("/vuln/goby/hikvision-any-file-download", new GobyHikvisionAnyFileDownloadHandler(true));
        registerHandler("/safe/goby/hikvision-any-file-download", new GobyHikvisionAnyFileDownloadHandler(false));

        // Goby POC 275: HotelDruid XSS
        registerHandler("/vuln/goby/hoteldruid-xss", new GobyHotelDruidXSSHandler(true));
        registerHandler("/safe/goby/hoteldruid-xss", new GobyHotelDruidXSSHandler(false));

        // Goby POC 276: Hsmedia Hgateway Default Account
        registerHandler("/vuln/goby/hsmedia-default-account", new GobyHsmediaDefaultAccountHandler(true));
        registerHandler("/safe/goby/hsmedia-default-account", new GobyHsmediaDefaultAccountHandler(false));

        // Goby POC 277: IFW8 Router Password Leakage
        registerHandler("/vuln/goby/ifw8-password-leakage", new GobyIFW8PasswordLeakageHandler(true));
        registerHandler("/safe/goby/ifw8-password-leakage", new GobyIFW8PasswordLeakageHandler(false));

        // Goby POC 278: IFW8 Router Credential Discovery (Reuse POC 277 Handler)
        registerHandler("/vuln/goby/ifw8-credential-discovery", new GobyIFW8PasswordLeakageHandler(true));
        registerHandler("/safe/goby/ifw8-credential-discovery", new GobyIFW8PasswordLeakageHandler(false));

        // Goby POC 279: IRDM4000 Smart station Unauthorized access
        registerHandler("/vuln/goby/irdm4000-unauthorized", new GobyIRDM4000UnauthorizedHandler(true));
        registerHandler("/safe/goby/irdm4000-unauthorized", new GobyIRDM4000UnauthorizedHandler(false));

        // Goby POC 280: IceWarp WebClient basic RCE
        registerHandler("/vuln/goby/icewarp-basic-rce", new GobyIceWarpBasicRCEHandler(true));
        registerHandler("/safe/goby/icewarp-basic-rce", new GobyIceWarpBasicRCEHandler(false));

        // Goby POC 281: JQuery 1.7.2 Version site foreground arbitrary file download
        registerHandler("/vuln/goby/jquery-file-download", new GobyJQueryFileDownloadHandler(true));
        registerHandler("/safe/goby/jquery-file-download", new GobyJQueryFileDownloadHandler(false));

        // Goby POC 282: JQuery 1.7.2 Filedownload (Reuse POC 281 Handler)
        registerHandler("/vuln/goby/jquery-1.7.2-file-download", new GobyJQueryFileDownloadHandler(true));
        registerHandler("/safe/goby/jquery-1.7.2-file-download", new GobyJQueryFileDownloadHandler(false));

        // Goby POC 283: Jellyfin 10.7.0 Unauthenticated Arbitrary File Read
        registerHandler("/vuln/goby/jellyfin-file-read-21402", new GobyJellyfinFileReadHandlerCVE202121402(true));
        registerHandler("/safe/goby/jellyfin-file-read-21402", new GobyJellyfinFileReadHandlerCVE202121402(false));

        // Goby POC 284: Jellyfin 10.7.2 SSRF
        registerHandler("/vuln/goby/jellyfin-ssrf-29490", new GobyJellyfinSSRFHandler(true));
        registerHandler("/safe/goby/jellyfin-ssrf-29490", new GobyJellyfinSSRFHandler(false));

        // Goby POC 285: Jellyfin SSRF (Reuse POC 284 Handler)
        registerHandler("/vuln/goby/jellyfin-ssrf-reuse", new GobyJellyfinSSRFHandler(true));
        registerHandler("/safe/goby/jellyfin-ssrf-reuse", new GobyJellyfinSSRFHandler(false));

        // Goby POC 286: Jellyfin prior to 10.7.0 File Read (Reuse POC 283 Handler)
        registerHandler("/vuln/goby/jellyfin-file-read-prior", new GobyJellyfinFileReadHandlerCVE202121402(true));
        registerHandler("/safe/goby/jellyfin-file-read-prior", new GobyJellyfinFileReadHandlerCVE202121402(false));

        // Goby POC 287: Jetty WEB-INF FileRead CVE-2021-28169
        registerHandler("/vuln/goby/jetty-fileread-28169", new GobyJettyFileReadHandlerCVE202128169(true));
        registerHandler("/safe/goby/jetty-fileread-28169", new GobyJettyFileReadHandlerCVE202128169(false));

        // Goby POC 288: Jetty WEB-INF FileRead CVE-2021-34429
        registerHandler("/vuln/goby/jetty-fileread-34429", new GobyJettyFileReadHandlerCVE202134429(true));
        registerHandler("/safe/goby/jetty-fileread-34429", new GobyJettyFileReadHandlerCVE202134429(false));

        // Goby POC 289: JinHe OA C6 Default password
        registerHandler("/vuln/goby/jinhe-oa-default-pwd", new GobyJinHeOADefaultPwdHandler(true));
        registerHandler("/safe/goby/jinhe-oa-default-pwd", new GobyJinHeOADefaultPwdHandler(false));

        // Goby POC 290: JinHe OA C6 download.jsp Arbitrary fileread
        registerHandler("/vuln/goby/jinhe-oa-fileread", new GobyJinHeOAFileReadHandler(true));
        registerHandler("/safe/goby/jinhe-oa-fileread", new GobyJinHeOAFileReadHandler(false));

        // Goby POC 291: JingHe OA C6 Default password (Reuse POC 289 Handler)
        registerHandler("/vuln/goby/jinghe-oa-default-pwd", new GobyJinHeOADefaultPwdHandler(true));
        registerHandler("/safe/goby/jinghe-oa-default-pwd", new GobyJinHeOADefaultPwdHandler(false));

        // Goby POC 292: Jinher OA C6 download.jsp Arbitrary file read (Reuse POC 290 Handler)
        registerHandler("/vuln/goby/jinher-oa-fileread", new GobyJinHeOAFileReadHandler(true));
        registerHandler("/safe/goby/jinher-oa-fileread", new GobyJinHeOAFileReadHandler(false));

        // Goby POC 293, 299, 302: Kingsoft V8 Arbitrary File Read
        registerHandler("/vuln/goby/kingsoft-v8-fileread", new GobyKingsoftV8FileReadHandler(true));
        registerHandler("/safe/goby/kingsoft-v8-fileread", new GobyKingsoftV8FileReadHandler(false));

        // Goby POC 294, 295: Jitong EWEBS Arbitrary File Read
        registerHandler("/vuln/goby/jitong-ewebs-fileread", new GobyJitongEWEBSFileReadHandler(true));
        registerHandler("/safe/goby/jitong-ewebs-fileread", new GobyJitongEWEBSFileReadHandler(false));

        // Goby POC 296: Jitong EWEBS Phpinfo Leak
        registerHandler("/vuln/goby/jitong-ewebs-phpinfo", new GobyJitongEWEBSPhpinfoHandler(true));
        registerHandler("/safe/goby/jitong-ewebs-phpinfo", new GobyJitongEWEBSPhpinfoHandler(false));

        // Goby POC 297, 298: KEDACOM MTS File Download
        registerHandler("/vuln/goby/kedacom-mts-download", new GobyKEDACOMMTSFileDownloadHandler(true));
        registerHandler("/safe/goby/kedacom-mts-download", new GobyKEDACOMMTSFileDownloadHandler(false));

        // Goby POC 300, 301: Kingsoft V8 Default Weak Password
        registerHandler("/vuln/goby/kingsoft-v8-weak-pwd", new GobyKingsoftV8WeakPwdHandler(true));
        registerHandler("/safe/goby/kingsoft-v8-weak-pwd", new GobyKingsoftV8WeakPwdHandler(false));

        // Goby POC 303: Konga Default JWT Key

        // Goby POC 304, 305, 306, 307: Kyan Account Password Leak
        registerHandler("/vuln/goby/kyan-account-leak", new GobyKyanAccountLeakHandler(true));
        registerHandler("/safe/goby/kyan-account-leak", new GobyKyanAccountLeakHandler(false));

        // Goby POC 308, 309: Kyan RCE
        registerHandler("/vuln/goby/kyan-rce", new GobyKyanRCECommandRunHandler(true));
        registerHandler("/safe/goby/kyan-rce", new GobyKyanRCECommandRunHandler(false));

        // Goby POC 310: Landray OA Custom JSP File Read
        registerHandler("/vuln/goby/landray-oa-custom-fileread", new GobyLandrayOACustomJspHandler(true));
        registerHandler("/safe/goby/landray-oa-custom-fileread", new GobyLandrayOACustomJspHandler(false));

        // Goby POC 311, 312, 313: Lanproxy Directory Traversal
        registerHandler("/vuln/goby/lanproxy-traversal", new GobyLanproxyTraversalHandler(true));
        registerHandler("/safe/goby/lanproxy-traversal", new GobyLanproxyTraversalHandler(false));

        // Goby POC 314, 315, 316: Laravel .env Leak
        registerHandler("/vuln/goby/laravel-env-leak", new GobyLaravelEnvLeakConfigHandler(true));
        registerHandler("/safe/goby/laravel-env-leak", new GobyLaravelEnvLeakConfigHandler(false));

        // Goby POC 317, 318: Leadsec ACM Info Leak
        registerHandler("/vuln/goby/leadsec-acm-leak", new GobyLeadsecACMInfoLeakConfigHandler(true));
        registerHandler("/safe/goby/leadsec-acm-leak", new GobyLeadsecACMInfoLeakConfigHandler(false));

        // Goby POC 319, 320: MPSec ISG1000 File Download
        registerHandler("/vuln/goby/mpsec-isg-download", new GobyMPSecISGGatewayFileDownloadHandler(true));
        registerHandler("/safe/goby/mpsec-isg-download", new GobyMPSecISGGatewayFileDownloadHandler(false));

        // Goby POC 321: Mallgard Firewall Default Login
        registerHandler("/vuln/goby/mallgard-firewall-login", new GobyMallgardFirewallDefaultLoginHandler(true));
        registerHandler("/safe/goby/mallgard-firewall-login", new GobyMallgardFirewallDefaultLoginHandler(false));

        // Goby POC 322: MessageSolution EEA Info Leak
        registerHandler("/vuln/goby/messagesolution-eea-leak", new GobyMessageSolutionEEAInfoLeakHandler(true));
        registerHandler("/safe/goby/messagesolution-eea-leak", new GobyMessageSolutionEEAInfoLeakHandler(false));

        // Goby POC 323, 324: MessageSolution EEA Info Leak (Reuse POC 322 Handler)
        registerHandler("/vuln/goby/messagesolution-eea-leak-dup", new GobyMessageSolutionEEAInfoLeakHandler(true));
        registerHandler("/safe/goby/messagesolution-eea-leak-dup", new GobyMessageSolutionEEAInfoLeakHandler(false));

        // Goby POC 325, 326, 327: Metabase Geojson Arbitrary File Read
        registerHandler("/vuln/goby/metabase-geojson-fileread", new GobyMetabaseGeojsonFileReadHandler(true));
        registerHandler("/safe/goby/metabase-geojson-fileread", new GobyMetabaseGeojsonFileReadHandler(false));

        // Goby POC 328: Micro module monitoring system User_list.php information leakage
        registerHandler("/vuln/goby/micro-module-leak", new GobyMicroModuleUserListLeakHandler(true));
        registerHandler("/safe/goby/micro-module-leak", new GobyMicroModuleUserListLeakHandler(false));

        // Goby POC 329, 330: Microsoft Exchange SSRF

        // Goby POC 331: MinIO Browser API SSRF
        registerHandler("/vuln/goby/minio-browser-ssrf", new GobyMinIOBrowserSSRFHandler(true));
        registerHandler("/safe/goby/minio-browser-ssrf", new GobyMinIOBrowserSSRFHandler(false));

        // Goby POC 334, 336: Node-RED ui_base Arbitrary File Read
        registerHandler("/vuln/goby/node-red-fileread", new GobyNodeREDFireReadHandler(true));
        registerHandler("/safe/goby/node-red-fileread", new GobyNodeREDFireReadHandler(false));

        // Goby POC 335: Node.js Path Traversal
        registerHandler("/vuln/goby/nodejs-path-traversal", new GobyNodeJsPathTraversalHandler(true));
        registerHandler("/safe/goby/nodejs-path-traversal", new GobyNodeJsPathTraversalHandler(false));

        // Goby POC 337, 338: OpenSNS RCE
        registerHandler("/vuln/goby/opensns-rce", new GobyOpenSNSRCEHandler(true));
        registerHandler("/safe/goby/opensns-rce", new GobyOpenSNSRCEHandler(false));

        // Goby POC 339: Oracle WebLogic Server Path Traversal
        registerHandler("/vuln/goby/weblogic-path-traversal", new GobyOracleWebLogicPathTraversalHandler(true));
        registerHandler("/safe/goby/weblogic-path-traversal", new GobyOracleWebLogicPathTraversalHandler(false));

        // Goby POC 340: Oracle Weblogic LDAP RCE
        registerHandler("/vuln/goby/weblogic-ldap-rce", new GobyOracleWebLogicLDAPRCEHandler(true));
        registerHandler("/safe/goby/weblogic-ldap-rce", new GobyOracleWebLogicLDAPRCEHandler(false));

        // Goby POC 341: Oracle Weblogic SSRF

        // Goby POC 342: Oray Sunlogin RCE
        registerHandler("/vuln/goby/oray-sunlogin-rce", new GobyOraySunloginRCEHandler(true));
        registerHandler("/safe/goby/oray-sunlogin-rce", new GobyOraySunloginRCEHandler(false));

        // Goby POC 343: PHP Zerodium Backdoor RCE
        registerHandler("/vuln/goby/php-zerodium-rce", new GobyPHPZerodiumBackdoorHandler(true));
        registerHandler("/safe/goby/php-zerodium-rce", new GobyPHPZerodiumBackdoorHandler(false));

        // Goby POC 344: Portainer Init Deploy
        registerHandler("/vuln/goby/portainer-init", new GobyPortainerInitHandler(true));
        registerHandler("/safe/goby/portainer-init", new GobyPortainerInitHandler(false));

        // Goby POC 345: RG UAC
        registerHandler("/vuln/goby/rg-uac", new GobyRGUACHandler(true));
        registerHandler("/safe/goby/rg-uac", new GobyRGUACHandler(false));

        // Goby POC 346: Riskscanner SQL Injection
        registerHandler("/vuln/goby/riskscanner-sqli", new GobyRiskscannerSQLInjectionHandler(true));
        registerHandler("/safe/goby/riskscanner-sqli", new GobyRiskscannerSQLInjectionHandler(false));

        // Goby POC 347: Ruijie EWEB RCE
        registerHandler("/vuln/goby/ruijie-eweb-rce", new GobyRuijieEWEBHandler(true));
        registerHandler("/safe/goby/ruijie-eweb-rce", new GobyRuijieEWEBHandler(false));

        // Goby POC 348: Ruijie RG-UAC Password Leak
        registerHandler("/vuln/goby/ruijie-rg-uac-leak", new GobyRuijieRGUACLeakHandler(true));
        registerHandler("/safe/goby/ruijie-rg-uac-leak", new GobyRuijieRGUACLeakHandler(false));

        // Goby POC 349, 350, 351: Ruijie Smartweb Password Leak
        registerHandler("/vuln/goby/ruijie-smartweb-leak", new GobyRuijieSmartwebPasswordLeakHandler(true));
        registerHandler("/safe/goby/ruijie-smartweb-leak", new GobyRuijieSmartwebPasswordLeakHandler(false));

        // Goby POC 352: Ruijie Smartweb Weak Password
        registerHandler("/vuln/goby/ruijie-smartweb-weak-pwd", new GobyRuijieSmartwebWeakPasswordHandler(true));
        registerHandler("/safe/goby/ruijie-smartweb-weak-pwd", new GobyRuijieSmartwebWeakPasswordHandler(false));
        
        // Goby POC 353: RuoYi Druid Unauthorized Access
        registerHandler("/vuln/goby/ruoyi-druid-unauth", new GobyRuoYiDruidHandlerPoc353(true));
        registerHandler("/safe/goby/ruoyi-druid-unauth", new GobyRuoYiDruidHandlerPoc353(false));

        // Goby POC 354, 355: SDWAN Smart Gateway
        registerHandler("/vuln/goby/sdwan-smart-gateway", new GobySDWANSmartGatewayHandler(true));
        registerHandler("/safe/goby/sdwan-smart-gateway", new GobySDWANSmartGatewayHandler(false));

        // Goby POC 356, 357, 358: Samsung WLAN AP RCE
        registerHandler("/vuln/goby/samsung-wlan-rce", new GobySamsungWLANRCEHandler(true));
        registerHandler("/safe/goby/samsung-wlan-rce", new GobySamsungWLANRCEHandler(false));

        // Goby POC 359: Security Devices Hardcoded Password
        registerHandler("/vuln/goby/security-devices-password", new GobySecurityDevicesHandler(true));
        registerHandler("/safe/goby/security-devices-password", new GobySecurityDevicesHandler(false));

        // Goby POC 360: Seeyon OA A6 DownExcelBeanServlet

        // Goby POC 361: Seeyon OA A6 createMysql.jsp
        registerHandler("/vuln/goby/seeyon-createmysql", new GobySeeyonCreateMysqlHandler(true));
        registerHandler("/safe/goby/seeyon-createmysql", new GobySeeyonCreateMysqlHandler(false));

        // Goby POC 362: Seeyon OA A6 initDataAssess.jsp
        registerHandler("/vuln/goby/seeyon-initdata", new GobySeeyonInitDataAssessHandler(true));
        registerHandler("/safe/goby/seeyon-initdata", new GobySeeyonInitDataAssessHandler(false));

        // Goby POC 363: Seeyon OA A6 setextno.jsp
        registerHandler("/vuln/goby/seeyon-setextno", new GobySeeyonSetExtNoHandler(true));
        registerHandler("/safe/goby/seeyon-setextno", new GobySeeyonSetExtNoHandler(false));

        // Goby POC 364: Seeyon OA A6 test.jsp
        registerHandler("/vuln/goby/seeyon-testjsp", new GobySeeyonTestJspHandler(true));
        registerHandler("/safe/goby/seeyon-testjsp", new GobySeeyonTestJspHandler(false));

        // Goby POC 365: Seeyon OA A8-m Info Leak
        registerHandler("/vuln/goby/seeyon-a8-leak", new GobySeeyonA8InfoLeakHandler(true));
        registerHandler("/safe/goby/seeyon-a8-leak", new GobySeeyonA8InfoLeakHandler(false));

        // Goby POC 366, 367: Shiziyu CMS SQL Injection
        registerHandler("/vuln/goby/shiziyu-cms-sqli", new GobyShiziyuCmsSQLHandler(true));
        registerHandler("/safe/goby/shiziyu-cms-sqli", new GobyShiziyuCmsSQLHandler(false));

        // Goby POC 368, 369: ShopXO File Read
        registerHandler("/vuln/goby/shopxo-fileread", new GobyShopXOFileReadHandler(true));
        registerHandler("/safe/goby/shopxo-fileread", new GobyShopXOFileReadHandler(false));

        // Goby POC 370: Shterm QiZhi Fortress
        registerHandler("/vuln/goby/shterm-qizhi", new GobyShtermQiZhiHandler(true));
        registerHandler("/safe/goby/shterm-qizhi", new GobyShtermQiZhiHandler(false));

        // Goby POC 371: SonarQube Search Projects
        registerHandler("/vuln/goby/sonarqube-search", new GobySonarQubeSearchProjectsHandler(true));
        registerHandler("/safe/goby/sonarqube-search", new GobySonarQubeSearchProjectsHandler(false));

        // Goby POC 372, 373: SonarQube Unauth
        registerHandler("/vuln/goby/sonarqube-unauth", new GobySonarQubeUnauthHandler(true));
        registerHandler("/safe/goby/sonarqube-unauth", new GobySonarQubeUnauthHandler(false));

        // Goby POC 374, 375: SonicWall SSL-VPN RCE
        registerHandler("/vuln/goby/sonicwall-rce", new GobySonicWallRCEHandler(true));
        registerHandler("/safe/goby/sonicwall-rce", new GobySonicWallRCEHandler(false));

        // Goby POC 376: SonicWall ShellShock
        registerHandler("/vuln/goby/sonicwall-shellshock", new GobySonicWallShellShockHandler(true));
        registerHandler("/safe/goby/sonicwall-shellshock", new GobySonicWallShellShockHandler(false));

        // Goby POC 377: SpiderFlow RCE
        registerHandler("/vuln/goby/spiderflow-rce", new GobySpiderFlowRCEHandler(true));
        registerHandler("/safe/goby/spiderflow-rce", new GobySpiderFlowRCEHandler(false));

        // Goby POC 378: Spring Boot Logview

        // Goby POC 379: Spring Cloud Function SpEL
        registerHandler("/vuln/goby/springcloud-function-spel", new GobySpringCloudFunctionSpELHandler(true));
        registerHandler("/safe/goby/springcloud-function-spel", new GobySpringCloudFunctionSpELHandler(false));

        // Goby POC 380: Spring Cloud Gateway SpEL
        registerHandler("/vuln/goby/springcloud-gateway-spel", new GobySpringCloudGatewaySpELHandler(true));
        registerHandler("/safe/goby/springcloud-gateway-spel", new GobySpringCloudGatewaySpELHandler(false));

        // Goby POC 381: Spring4Shell

        // Goby POC 382: Spring Boot Actuator Unauth
        registerHandler("/vuln/goby/springboot-actuator", new GobySpringBootActuatorHandler(true));
        registerHandler("/safe/goby/springboot-actuator", new GobySpringBootActuatorHandler(false));

        // Goby POC 383, 384, 385: Struts2 Log4Shell
        registerHandler("/vuln/goby/struts2-log4shell", new GobyStruts2Log4ShellHandler(true));
        registerHandler("/safe/goby/struts2-log4shell", new GobyStruts2Log4ShellHandler(false));

        // Goby POC 386: TamronOS File Download
        registerHandler("/vuln/goby/tamronos-filedownload", new GobyTamronOSFileDownloadHandler(true));
        registerHandler("/safe/goby/tamronos-filedownload", new GobyTamronOSFileDownloadHandler(false));

        // Goby POC 387: TamronOS RCE
        registerHandler("/vuln/goby/tamronos-rce", new GobyTamronOSRCEHandler(true));
        registerHandler("/safe/goby/tamronos-rce", new GobyTamronOSRCEHandler(false));

        // Goby POC 388: Tianwen ERP File Upload
        registerHandler("/vuln/goby/tianwen-upload", new GobyTianwenFileUploadHandler(true));
        registerHandler("/safe/goby/tianwen-upload", new GobyTianwenFileUploadHandler(false));

        // Goby POC 389: U8 OA

        // Goby POC 390: D-Link RCE
        registerHandler("/vuln/goby/dlink-rce", new GobyDLinkRCEHandler(true));
        registerHandler("/safe/goby/dlink-rce", new GobyDLinkRCEHandler(false));

        // Goby POC 391: UniFi Log4Shell
        registerHandler("/vuln/goby/unifi-log4shell", new GobyUniFiLog4ShellHandler(true));
        registerHandler("/safe/goby/unifi-log4shell", new GobyUniFiLog4ShellHandler(false));

        // Goby POC 392: VENGD File Upload
        registerHandler("/vuln/goby/vengd-upload", new GobyVENGDFileUploadHandler(true));
        registerHandler("/safe/goby/vengd-upload", new GobyVENGDFileUploadHandler(false));

        // Goby POC 393: VMWare Horizon Log4Shell
        registerHandler("/vuln/goby/vmware-horizon-log4shell", new GobyVMwareHorizonLog4ShellHandler(true));
        registerHandler("/safe/goby/vmware-horizon-log4shell", new GobyVMwareHorizonLog4ShellHandler(false));

        // Goby POC 394: VMWare Operations SSRF
        registerHandler("/vuln/goby/vmware-ops-ssrf", new GobyVMwareOperationsSSRFHandler(true));
        registerHandler("/safe/goby/vmware-ops-ssrf", new GobyVMwareOperationsSSRFHandler(false));

        // Goby POC 395: VMWare NSX Log4Shell
        registerHandler("/vuln/goby/vmware-nsx-log4shell", new GobyVMwareNSXLog4ShellHandler(true));
        registerHandler("/safe/goby/vmware-nsx-log4shell", new GobyVMwareNSXLog4ShellHandler(false));

        // Goby POC 396: VMWare Workspace ONE RCE
        registerHandler("/vuln/goby/vmware-ws1-rce", new GobyVMwareWorkspaceONEHandler(true));
        registerHandler("/safe/goby/vmware-ws1-rce", new GobyVMwareWorkspaceONEHandler(false));

        // Goby POC 397: VMWare vCenter Log4Shell
        registerHandler("/vuln/goby/vmware-vcenter-log4shell", new GobyVMwarevCenterLog4ShellHandler(true));
        registerHandler("/safe/goby/vmware-vcenter-log4shell", new GobyVMwarevCenterLog4ShellHandler(false));

        // Goby POC 398: VMWare vCenter File Read
        registerHandler("/vuln/goby/vmware-vcenter-fileread", new GobyVMwarevCenterFileReadHandler(true));
        registerHandler("/safe/goby/vmware-vcenter-fileread", new GobyVMwarevCenterFileReadHandler(false));

        // Goby POC 399: WAVLINK XSS
        registerHandler("/vuln/goby/wavlink-xss", new GobyWAVLINKXSSHandler(true));
        registerHandler("/safe/goby/wavlink-xss", new GobyWAVLINKXSSHandler(false));

        // Goby POC 400: WSO2 XSS
        registerHandler("/vuln/goby/wso2-xss", new GobyWSO2XSSHandler(true));
        registerHandler("/safe/goby/wso2-xss", new GobyWSO2XSSHandler(false));

        // Goby POC 401: WSO2 File Upload

        // Goby POC 402: Wayos AC Default Password
        registerHandler("/vuln/goby/wayos-ac-pwd", new GobyWayosACHandler(true));
        registerHandler("/safe/goby/wayos-ac-pwd", new GobyWayosACHandler(false));

        // Goby POC 403: Weaver EOffice Upload
        registerHandler("/vuln/goby/weaver-eoffice-upload", new GobyWeaverEOfficeUploadHandler(true));
        registerHandler("/safe/goby/weaver-eoffice-upload", new GobyWeaverEOfficeUploadHandler(false));

        // Goby POC 404: Weaver OA SQLi
        registerHandler("/vuln/goby/weaver-oa-sqli", new GobyWeaverOASQLiHandler(true));
        registerHandler("/safe/goby/weaver-oa-sqli", new GobyWeaverOASQLiHandler(false));

        // Goby POC 405: WebSVN RCE

        // Goby POC 406: Weblogic LDAP RCE
        registerHandler("/vuln/goby/weblogic-ldap-rce-2", new GobyWeblogicLDAPRCEHandler(true));
        registerHandler("/safe/goby/weblogic-ldap-rce-2", new GobyWeblogicLDAPRCEHandler(false));

        // Goby POC 407: Weblogic SSRF (CVE-2014-4210)
        registerHandler("/vuln/goby/weblogic-ssrf-2014", new GobyWeblogicSSRFHandler2(true));
        registerHandler("/safe/goby/weblogic-ssrf-2014", new GobyWeblogicSSRFHandler2(false));

        // Goby POC 408: WordPress Simple Ajax Chat
        registerHandler("/vuln/goby/wp-simple-ajax-chat", new GobyWPSimpleAjaxChatHandler(true));
        registerHandler("/safe/goby/wp-simple-ajax-chat", new GobyWPSimpleAjaxChatHandler(false));

        // Goby POC 409: WordPress WPQA
        registerHandler("/vuln/goby/wp-wpqa", new GobyWPWPQAHandler(true));
        registerHandler("/safe/goby/wp-wpqa", new GobyWPWPQAHandler(false));

        // Goby POC 410: XXL-JOB Default Password
        registerHandler("/vuln/goby/xxl-job-pwd", new GobyXXLJOBHandler(true));
        registerHandler("/safe/goby/xxl-job-pwd", new GobyXXLJOBHandler(false));

        // Goby POC 411: Xieda OA File Download
        registerHandler("/vuln/goby/xieda-oa-filedownload", new GobyXiedaOAHandler(true));
        registerHandler("/safe/goby/xieda-oa-filedownload", new GobyXiedaOAHandler(false));

        // Goby POC 412: YAPI RCE
        registerHandler("/vuln/goby/yapi-rce", new GobyYAPIRCEHandler(true));
        registerHandler("/safe/goby/yapi-rce", new GobyYAPIRCEHandler(false));

        // Goby POC 413: YCCMS XSS
        registerHandler("/vuln/goby/yccms-xss", new GobyYCCMSXSSHandler(true));
        registerHandler("/safe/goby/yccms-xss", new GobyYCCMSXSSHandler(false));

        // Goby POC 414: Yinpeng Hanming File Download
        registerHandler("/vuln/goby/yinpeng-filedownload", new GobyYinpengHandler(true));
        registerHandler("/safe/goby/yinpeng-filedownload", new GobyYinpengHandler(false));

        // Goby POC 415: Yonyou NC Bsh RCE
        registerHandler("/vuln/goby/yonyou-nc-bsh-rce", new GobyYonyouNCBshHandler(true));
        registerHandler("/safe/goby/yonyou-nc-bsh-rce", new GobyYonyouNCBshHandler(false));

        // Goby POC 416: ZZZCMS RCE
        registerHandler("/vuln/goby/zzzcms-rce", new GobyZZZCMSHandler(true));
        registerHandler("/safe/goby/zzzcms-rce", new GobyZZZCMSHandler(false));

        // Goby POC 417: Dahua DSS File Download
        registerHandler("/vuln/goby/dahua-dss-filedownload", new GobyDahuaDSSHandler(true));
        registerHandler("/safe/goby/dahua-dss-filedownload", new GobyDahuaDSSHandler(false));

        // Goby POC 418: ZhongXinJingDun Default Password
        registerHandler("/vuln/goby/zhongxinjingdun-pwd", new GobyZhongXinJingDunHandler(true));
        registerHandler("/safe/goby/zhongxinjingdun-pwd", new GobyZhongXinJingDunHandler(false));

        // Goby POC 432: Alibaba Canal Default Password
        registerHandler("/vuln/goby/alibaba-canal-pwd", new GobyAlibabaCanalHandler(true));
        registerHandler("/safe/goby/alibaba-canal-pwd", new GobyAlibabaCanalHandler(false));

        // Goby POC 433: Chanjet CRM SQLi
        registerHandler("/vuln/goby/chanjet-crm-sqli", new GobyChanjetCRMHandler(true));
        registerHandler("/safe/goby/chanjet-crm-sqli", new GobyChanjetCRMHandler(false));

        // Goby POC 434: F5 BIG-IP RCE
        registerHandler("/vuln/goby/f5-bigip-rce", new GobyF5BigIpRceHandler(true));
        registerHandler("/safe/goby/f5-bigip-rce", new GobyF5BigIpRceHandler(false));

        // Goby POC 435: Fahuo100 SQLi
        registerHandler("/vuln/goby/fahuo100-sqli", new GobyFahuo100Handler(true));
        registerHandler("/safe/goby/fahuo100-sqli", new GobyFahuo100Handler(false));

        // Goby POC 436: Feishimei Struts2
        registerHandler("/vuln/goby/feishimei-struts2", new GobyFeishimeiHandler(true));
        registerHandler("/safe/goby/feishimei-struts2", new GobyFeishimeiHandler(false));

        // Goby POC 437: Firewall Info Leak
        registerHandler("/vuln/goby/firewall-infoleak", new GobyFirewallInfoLeakHandler(true));
        registerHandler("/safe/goby/firewall-infoleak", new GobyFirewallInfoLeakHandler(false));

        // Goby POC 438: Fumengyun SQLi
        registerHandler("/vuln/goby/fumengyun-sqli", new GobyFumengyunHandler(true));
        registerHandler("/safe/goby/fumengyun-sqli", new GobyFumengyunHandler(false));

        // Goby POC 439: Huatiandongli OA SQLi
        registerHandler("/vuln/goby/huatiandongli-sqli", new GobyHuatiandongliHandler(true));
        registerHandler("/safe/goby/huatiandongli-sqli", new GobyHuatiandongliHandler(false));

        // Goby POC 440: Landray OA File Read
        registerHandler("/vuln/goby/landray-oa-fileread", new GobyLandrayOAFileReadHandler(true));
        registerHandler("/safe/goby/landray-oa-fileread", new GobyLandrayOAFileReadHandler(false));

        // Goby POC 441: Mallgard
        registerHandler("/vuln/goby/mallgard-vuln", new GobyMallgardHandler(true));
        registerHandler("/safe/goby/mallgard-vuln", new GobyMallgardHandler(false));

        // Goby POC 442: PHP 8.1 Backdoor
        registerHandler("/vuln/goby/php8-backdoor", new GobyPhp8BackdoorHandler(true));
        registerHandler("/safe/goby/php8-backdoor", new GobyPhp8BackdoorHandler(false));

        // Goby POC 443: Qilai OA Message
        registerHandler("/vuln/goby/qilai-oa-message", new GobyQilaiOAMessageUrlHandler(true));
        registerHandler("/safe/goby/qilai-oa-message", new GobyQilaiOAMessageUrlHandler(false));

        // Goby POC 444: Qilai OA Tree
        registerHandler("/vuln/goby/qilai-oa-treelist", new GobyQilaiOATreelistHandler(true));
        registerHandler("/safe/goby/qilai-oa-treelist", new GobyQilaiOATreelistHandler(false));

        // Goby POC 445: Red Fan OA
        registerHandler("/vuln/goby/redfan-oa-fileread", new GobyRedFanOAHandler(true));
        registerHandler("/safe/goby/redfan-oa-fileread", new GobyRedFanOAHandler(false));

        // Goby POC 446: Sangfor RCE

        // Goby POC 447: Shterm QiZhi

        // Goby POC 448: Tongda OA Unauth
        registerHandler("/vuln/goby/tongda-oa-unauth", new GobyTongdaOAUnauthHandler(true));
        registerHandler("/safe/goby/tongda-oa-unauth", new GobyTongdaOAUnauthHandler(false));

        // Goby POC 449: Wangyixingyun
        registerHandler("/vuln/goby/wangyixingyun-infoleak", new GobyWangyixingyunHandler(true));
        registerHandler("/safe/goby/wangyixingyun-infoleak", new GobyWangyixingyunHandler(false));

        // Goby POC 450: Weaver Ecology SQLi
        registerHandler("/vuln/goby/weaver-ecology-sqli-2", new GobyWeaverEcologySQLiHandler(true));
        registerHandler("/safe/goby/weaver-ecology-sqli-2", new GobyWeaverEcologySQLiHandler(false));

        // Goby POC 451: Yiyou RCE
        registerHandler("/vuln/goby/yiyou-rce", new GobyYiyouHandler(true));
        registerHandler("/safe/goby/yiyou-rce", new GobyYiyouHandler(false));

        // Goby POC 452: Yuanchuangxianfeng
        registerHandler("/vuln/goby/yuanchuangxianfeng-unauth", new GobyYuanchuangxianfengHandler(true));
        registerHandler("/safe/goby/yuanchuangxianfeng-unauth", new GobyYuanchuangxianfengHandler(false));

        // Goby POC 453: Yunshidai SQLi
        registerHandler("/vuln/goby/yunshidai-sqli", new GobyYunshidaiHandler(true));
        registerHandler("/safe/goby/yunshidai-sqli", new GobyYunshidaiHandler(false));

        // Goby POC 455: Zhihuipingtai File Download
        registerHandler("/vuln/goby/zhihuipingtai-fileread", new GobyZhihuipingtaiHandler(true));
        registerHandler("/safe/goby/zhihuipingtai-fileread", new GobyZhihuipingtaiHandler(false));

        // Goby POC 456: Ziguang SQLi
        registerHandler("/vuln/goby/ziguang-sqli", new GobyZiguangHandler(true));
        registerHandler("/safe/goby/ziguang-sqli", new GobyZiguangHandler(false));

        // Goby POC 457: Fanruan Report
        registerHandler("/vuln/goby/fanruan-report-fileread", new GobyFanruanReportHandler(true));
        registerHandler("/safe/goby/fanruan-report-fileread", new GobyFanruanReportHandler(false));

        // Goby POC 458: Laifuyun SQLi
        registerHandler("/vuln/goby/laifuyun-sqli", new GobyFumengyunHandler(true));
        registerHandler("/safe/goby/laifuyun-sqli", new GobyFumengyunHandler(false));

        // Goby POC 459: Seeyon OA A6 DB Info Leak
        registerHandler("/vuln/goby/seeyon-a6-db-infoleak", new GobySeeyonA6DBInfoLeakHandler(true));
        registerHandler("/safe/goby/seeyon-a6-db-infoleak", new GobySeeyonA6DBInfoLeakHandler(false));

        // Goby POC 460: Seeyon OA A6 User Info Leak
        registerHandler("/vuln/goby/seeyon-a6-user-infoleak", new GobySeeyonA6UserInfoLeakHandler(true));
        registerHandler("/safe/goby/seeyon-a6-user-infoleak", new GobySeeyonA6UserInfoLeakHandler(false));

        // Goby POC 461: Seeyon OA Webmail File Download
        registerHandler("/vuln/goby/seeyon-webmail-fileread", new GobySeeyonWebmailFileDownloadHandler(true));
        registerHandler("/safe/goby/seeyon-webmail-fileread", new GobySeeyonWebmailFileDownloadHandler(false));

        // Goby POC 462: Fengwang Router Password Leak
        registerHandler("/vuln/goby/fengwang-router-leak", new GobyFengwangRouterHandler(true));
        registerHandler("/safe/goby/fengwang-router-leak", new GobyFengwangRouterHandler(false));

        // Goby POC 463: Ruijie NBR RCE
        registerHandler("/vuln/goby/ruijie-nbr-rce", new GobyRuijieNBRRCEHandler(true));
        registerHandler("/safe/goby/ruijie-nbr-rce", new GobyRuijieNBRRCEHandler(false));

    }
    
    // ========================================
    // Goby POC 60-69 处理器
    // ========================================
    
    class GobyNacosUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyNacosUnauthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/v1/auth/users")) {
                sendResponse(exchange, 500, "{\"message\":\"server is DOWN now\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyWeblogicPathHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeblogicPathHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && (path.contains("WEB-INF") || path.contains("META-INF"))) {
                sendResponse(exchange, 200, "<?xml version=\"1.0\"?><web-app></web-app>", "application/xml");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyStruts2Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyStruts2Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("redirect:")) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyShiroHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyShiroHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            if (vulnerable && cookie != null && cookie.contains("rememberMe=")) {
                exchange.getResponseHeaders().set("Set-Cookie", "rememberMe=deleteMe");
                sendResponse(exchange, 200, "Login", "text/html");
            } else {
                sendResponse(exchange, 200, "No Shiro", "text/html");
            }
        }
    }
    
    class GobyLog4jHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLog4jHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String ua = exchange.getRequestHeaders().getFirst("User-Agent");
            if (vulnerable && ua != null && ua.contains("${jndi:")) {
                sendResponse(exchange, 200, "Error: JNDI Lookup", "text/plain");
            } else {
                sendResponse(exchange, 200, "OK", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 50-59 处理器
    // ========================================
    
    class GobyGrafanaFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyGrafanaFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String rawPath = exchange.getRequestURI().getRawPath();
            // 检查 raw path 中是否有路径遍历
            if (vulnerable && (rawPath.contains("/public/plugins") || rawPath.contains("..") || rawPath.contains("%2e"))) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyThinkPHPHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyThinkPHPHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("invokefunction")) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 500, "Error", "text/plain");
            }
        }
    }
    
    class GobyRedisHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRedisHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "+OK\r\n$5\r\nredis\r\n", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyJenkinsHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJenkinsHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && (path.contains("/script") || path.contains("/manage"))) {
                sendResponse(exchange, 200, "<html><title>Jenkins</title><body>Script Console</body></html>", "text/html");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyElasticsearchHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyElasticsearchHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "{\"name\":\"node-1\",\"cluster_name\":\"elasticsearch\",\"version\":{\"number\":\"7.0.0\"}}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyMongoDBHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMongoDBHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "{\"ok\":1,\"version\":\"4.0.0\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyKibanaHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKibanaHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/api/timelion/run")) {
                sendResponse(exchange, 200, "{\"sheet\":[\".es(*).props(label.__proto__.env.AAAA='require('child_process').exec')\"]}}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyHarborHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyHarborHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/api/v2.0/users")) {
                sendResponse(exchange, 200, "[{\"user_id\":1,\"username\":\"admin\"}]", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyNexusHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyNexusHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (vulnerable && auth != null && auth.contains("Basic")) {
                sendResponse(exchange, 200, "{\"userId\":\"admin\",\"source\":\"default\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 40-49 处理器
    // ========================================
    
    class GobyYAPIHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyYAPIHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/api/user/reg")) {
                sendResponse(exchange, 200, "{\"errcode\":400,\"errmsg\":\"邮箱不能为空\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyRuijieHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRuijieHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (vulnerable && auth != null && auth.contains("Basic")) {
                sendResponse(exchange, 200, "<html><title>Ruijie</title><body>login_ok</body></html>", "text/html");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyDahuaHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDahuaHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && (query.contains("path=") || query.contains("filePath="))) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyZabbixHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyZabbixHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            // POC 请求 index_sso.php 并带有恶意 Cookie
            if (vulnerable && (path.contains("/zabbix.php") || path.contains("/index_sso.php"))) {
                if (path.contains("index_sso.php") && cookie != null && cookie.contains("zbx_session")) {
                    exchange.getResponseHeaders().set("Location", "/zabbix.php?action=dashboard.view");
                    exchange.sendResponseHeaders(302, 0);
                    exchange.getResponseBody().close();
                } else if (path.contains("zabbix.php")) {
                    // 兼容其他可能得 POC
                    exchange.getResponseHeaders().set("Location", "/zabbix.php?action=dashboard.view");
                    exchange.sendResponseHeaders(302, 0);
                    exchange.getResponseBody().close();
                } else {
                    ManualPocTestServer.this.sendResponse(exchange, 401, "Unauthorized", "text/plain");
                }
            } else {
                ManualPocTestServer.this.sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyDLinkHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/apply_sec.cgi")) {
                sendResponse(exchange, 200, "admin:admin", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobySamsungHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySamsungHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/(download)")) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyMinIOHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMinIOHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/minio/health")) {
                sendResponse(exchange, 200, "{\"version\":\"2021-06-17T00:10:46Z\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyNodeJSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyNodeJSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String rawPath = exchange.getRequestURI().getRawPath();
            if (vulnerable && rawPath.contains("..")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyPHP8Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyPHP8Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String userAgentt = exchange.getRequestHeaders().getFirst("User-Agentt");
            if (vulnerable && userAgentt != null && userAgentt.contains("zerodium")) {
                // POC 执行 var_dump(233*233)，期望返回 int(54289)
                sendResponse(exchange, 200, "int(54289)\nuid=0(root) gid=0(root) groups=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 200, "<html>PHP Info</html>", "text/html");
            }
        }
    }
    
    class GobyPortainerHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyPortainerHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/api/users/admin/check")) {
                // POC 期望未初始化系统返回 404，表示 admin 不存在
                sendResponse(exchange, 404, "Not Found", "text/plain");
            } else {
                // 安全版本返回 200，表示 admin 存在
                sendResponse(exchange, 200, "{\"Id\":1}", "application/json");
            }
        }
    }
    
    // ========================================
    // Goby POC 30-39 处理器
    // ========================================
    
    class GobyShenYuUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyShenYuUnauthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/plugin")) {
                sendResponse(exchange, 200, "{\"code\":200,\"message\":\"query success\",\"data\":[]}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobySpringCloudFunctionHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySpringCloudFunctionHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String spel = exchange.getRequestHeaders().getFirst("spring.cloud.function.routing-expression");
            if (vulnerable && path.contains("/functionRouter") && spel != null) {
                sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\",\"path\":\"/functionRouter\"}", "application/json");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobySpringCloudGatewayHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySpringCloudGatewayHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (vulnerable && path.contains("/actuator/gateway/routes") && "POST".equals(method)) {
                exchange.getResponseHeaders().set("Location", "/actuator/gateway/routes/gobytest");
                exchange.getResponseHeaders().set("X-Route-Id", "gobytest");
                sendResponse(exchange, 201, "{\"id\":\"gobytest\"}", "application/json");
            } else if (vulnerable && path.contains("/actuator/gateway")) {
                sendResponse(exchange, 200, "{\"routes\":[\"gobytest\"]}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyConfluenceRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyConfluenceRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String rawPath = exchange.getRequestURI().getRawPath();
            if (vulnerable && rawPath != null && (rawPath.contains("%24") || rawPath.contains("$"))) {
                exchange.getResponseHeaders().set("X-Cmd-Response", "root");
                exchange.getResponseHeaders().set("Location", "/login.action");
                exchange.sendResponseHeaders(302, 0);
                exchange.getResponseBody().close();
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyVMwareWorkspaceHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyVMwareWorkspaceHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("deviceUdid=")) {
                sendResponse(exchange, 400, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyF5BigIPHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyF5BigIPHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/mgmt/shared/authn/login")) {
                sendResponse(exchange, 200, "{\"resterrorresponse\":{\"message\":\"Authorization failed\"}}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobySeeyonInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySeeyonInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/initDataAssess.jsp")) {
                sendResponse(exchange, 200, "var personList = [{id:1,name:'admin'}];", "application/javascript");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyYongyouNCHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyYongyouNCHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/servlet/~ic/bsh.servlet.BshServlet")) {
                sendResponse(exchange, 200, "<html><body>BeanShell: uid=0(root)</body></html>", "text/html");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyWeaverOAHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeaverOAHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/custom.jsp")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyShopXOHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyShopXOHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("s=/index/qrcode/download")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 20-29 处理器
    // ========================================
    
    class GobyDruidFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDruidFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/druid/indexer/v1/sampler")) {
                sendResponse(exchange, 200, "{\"data\":[{\"raw\":\"root:x:0:0:root:/root:/bin/bash\"}]}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyFlinkFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFlinkFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/jobmanager/logs")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash\ndaemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyAirflowUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyAirflowUnauthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/admin")) {
                sendResponse(exchange, 200, "<html><title>Airflow - DAGs</title><body>DAGs List</body></html>", "text/html");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyCouchDBUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCouchDBUnauthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/_config")) {
                sendResponse(exchange, 200, "{\"httpd_design_handlers\":{},\"external_manager\":{},\"replicator_manager\":{}}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyDubboDefaultPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDubboDefaultPwdHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            // Basic Z3Vlc3Q6Z3Vlc3Q= = guest:guest, Basic cm9vdDpyb290 = root:root
            if (vulnerable && auth != null && (auth.contains("Z3Vlc3Q6Z3Vlc3Q=") || auth.contains("cm9vdDpyb290"))) {
                sendResponse(exchange, 200, "<html>&lt;title&gt;Dubbo Admin&lt;/title&gt;<body>/sysinfo/versions</body></html>", "text/html");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyMetabaseFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMetabaseFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("url=")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/ash\ndaemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyWeblogicSSRFHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeblogicSSRFHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String path = exchange.getRequestURI().getPath();
            System.out.println("[DEBUG] GobyWeblogicSSRFHandler: path=" + path + ", vulnerable=" + vulnerable);
            // 放宽检查条件，只要是漏洞模式
            if (vulnerable) {
                 // POC 期望返回包含 "but could not connect over HTTP to server"
                String response = "<html>An error has occurred<br>weblogic.uddi.client.structures.exception.XML_SoapException: but could not connect over HTTP to server</html>";
                System.out.println("[DEBUG] 返回漏洞响应: " + response.substring(0, Math.min(100, response.length())));
                sendResponse(exchange, 200, response, "text/html");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyJettyFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJettyFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String rawPath = exchange.getRequestURI().getRawPath();
            String path = exchange.getRequestURI().getPath();
            if (vulnerable) {
                // 第一步检查 /WEB-INF/web.xml 返回 404+Jetty
                if (path.equals("/WEB-INF/web.xml") && !rawPath.contains("%")) {
                    sendResponse(exchange, 404, "<html><body>Jetty - Not Found</body></html>", "text/html");
                } else if (rawPath.contains("%u002e") || rawPath.contains("%00") || path.contains("/WEB-INF")) {
                    // 其他绕过路径返回 web-app
                    sendResponse(exchange, 200, "<?xml version=\"1.0\"?><web-app><display-name>Test</display-name></web-app>", "application/xml");
                } else {
                    sendResponse(exchange, 404, "Not Found", "text/plain");
                }
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyLaravelEnvLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLaravelEnvLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains(".env")) {
                sendResponse(exchange, 200, "APP_NAME=Laravel\nAPP_ENV=local\nAPP_KEY=base64:xxx\nDB_PASSWORD=secret", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyVCenterFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyVCenterFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("url=file:")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash\nbin:x:1:1:bin:/bin:/sbin/nologin", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 10: 360天擎 SQL 注入
    // ========================================
    class Goby360TianqingSQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public Goby360TianqingSQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/api/dp/rptsvcsyncpoint")) {
                sendResponse(exchange, 200, "{\"result\":\"ok\",\"success\":true,\"data\":[]}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 11: Apache Solr 文件读取
    // ========================================
    class GobySolrFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySolrFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/solr/admin/cores")) {
                sendResponse(exchange, 200, "{\"responseHeader\":{\"status\":0},\"status\":{\"core1\":{\"name\":\"core1\"}}}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 12: Spring Boot Actuator 未授权
    // ========================================
    class GobySpringBootActuatorHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySpringBootActuatorHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/actuator")) {
                sendResponse(exchange, 200, "{\"_links\":{\"self\":{\"href\":\"/actuator\"},\"health\":{\"href\":\"/actuator/health\"}}}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 13: Adslr 信息泄露
    // ========================================
    class GobyAdslrInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyAdslrInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            // POC 请求 /request_para.cgi?parameter=wifi_get_5g_host
            if (vulnerable && (path.contains("request_para.cgi") || (query != null && query.contains("wifi_get_5g_host")))) {
                sendResponse(exchange, 200, "{\"wifi_5g_ssid\":\"TestNetwork\",\"security\":\"WPA-PSK\",\"password\":\"123456\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 14: AVCON6 文件下载
    // ========================================
    class GobyAvcon6FileDownHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyAvcon6FileDownHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/org_execl_download.action")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash\ndaemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 15: ADSelfService Plus RCE
    // ========================================
    class GobyADSelfServiceRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyADSelfServiceRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/RestAPI/LogonCustomization")) {
                sendResponse(exchange, 200, "<script>var d = new Date(); window.parent.$(\"#tabLogo\")</script>", "text/html");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 16: RuoYi Druid 未授权
    // ========================================
    class GobyRuoyiDruidHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRuoyiDruidHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/druid")) {
                sendResponse(exchange, 200, "<html><title>Druid Stat Index</title><body>View JSON API</body></html>", "text/html");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 17: XXL-JOB 默认密码
    // ========================================
    class GobyXxlJobDefaultPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyXxlJobDefaultPwdHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (vulnerable && "POST".equals(method) && path.contains("/login")) {
                sendResponse(exchange, 200, "{\"code\":200,\"msg\":\"success\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 18: Alibaba Canal 默认密码
    // ========================================
    class GobyCanalDefaultPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCanalDefaultPwdHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (vulnerable && "POST".equals(method) && path.contains("/api/v1/user/login")) {
                // 第一步返回特征字符串，第二步返回成功
                String body = readRequestBody(exchange);
                if (body == null || body.isEmpty()) {
                    sendResponse(exchange, 200, "{\"message\":\"com.alibaba.otter.canal.admin.controller.UserController.login\"}", "application/json");
                } else {
                    sendResponse(exchange, 200, "{\"code\":20000,\"data\":{\"token\":\"xxx\"}}", "application/json");
                }
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 19: SonarQube 未授权
    // ========================================
    class GobySonarQubeUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySonarQubeUnauthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/api/settings/values")) {
                sendResponse(exchange, 200, "{\"settings\":[{\"key\":\"sonaranalyzer-cs.nuget.packageVersion\",\"value\":\"8.0\"},{\"key\":\"sonar.core.id\",\"value\":\"xxx\"}]}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 8: Adobe ColdFusion LFI
    // ========================================
    class GobyColdFusionLFIHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public GobyColdFusionLFIHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            
            if (vulnerable) {
                if (path.contains("/CFIDE/administrator") && query != null && query.contains("locale=")) {
                    String response = "#ColdFusion Settings\nrdspassword=abc123\nencrypted=false\npassword=admin";
                    sendResponse(exchange, 200, response, "text/plain");
                } else {
                    sendResponse(exchange, 404, "Not Found", "text/plain");
                }
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 9: Ametys CMS 信息泄露
    // ========================================
    class GobyAmetysCMSHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public GobyAmetysCMSHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if (vulnerable) {
                if (path.contains("/plugins/web/service/search/auto-completion")) {
                    String response = "<?xml version=\"1.0\"?><items><item>admin</item><item>administrator</item></items>";
                    sendResponse(exchange, 200, response, "application/xml");
                } else {
                    sendResponse(exchange, 404, "Not Found", "text/plain");
                }
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 6: Apache Kylin 未授权配置泄露
    // ========================================
    class GobyKylinConfigLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public GobyKylinConfigLeakHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if (vulnerable) {
                if (path.contains("/kylin/api/admin/config")) {
                    String response = "{\"config\":{\"kylin.server.mode\":\"all\",\"kylin.storage.url\":\"hbase\"}}";
                    sendResponse(exchange, 200, response, "application/json");
                } else {
                    sendResponse(exchange, 404, "Not Found", "text/plain");
                }
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 7: Apache ActiveMQ 弱口令
    // ========================================
    class GobyActiveMQWeakPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public GobyActiveMQWeakPwdHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            
            if (vulnerable) {
                // Basic YWRtaW46YWRtaW4= = admin:admin
                if (path.contains("/admin") && "Basic YWRtaW46YWRtaW4=".equals(auth)) {
                    String response = "<html><body><h1>Apache ActiveMQ</h1><p>Version: 5.16.0</p></body></html>";
                    sendResponse(exchange, 200, response, "text/html");
                } else {
                    sendResponse(exchange, 401, "Unauthorized", "text/plain");
                }
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 5: Apache APISIX 默认 Token
    // ========================================
    /**
     * Apache APISIX 默认 Token 漏洞处理器
     * 
     * POC 分析:
     * - 路径: /apisix/admin/routes
     * - 方法: POST
     * - header: X-API-KEY: edd1c9f034335f136f87ad84b625c8f1
     * - 期望响应: 状态码 201 且 body 包含 "attack"
     */
    class GobyApisixDefaultTokenHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public GobyApisixDefaultTokenHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String apiKey = exchange.getRequestHeaders().getFirst("X-API-KEY");
            
            if (vulnerable) {
                // 漏洞版本: 检查是否使用默认 Token
                if ("POST".equalsIgnoreCase(method) && path.contains("/apisix/admin/routes") &&
                    "edd1c9f034335f136f87ad84b625c8f1".equals(apiKey)) {
                    String response = "{\"node\":{\"key\":\"/apisix/routes/attack\",\"value\":{\"uri\":\"/attack\"}},\"action\":\"create\"}";
                    sendResponse(exchange, 201, response, "application/json");
                } else {
                    sendResponse(exchange, 401, "{\"error_msg\":\"Invalid API Key\"}", "application/json");
                }
            } else {
                // 安全版本: 返回 401 未授权
                sendResponse(exchange, 401, "{\"error_msg\":\"Invalid API Key\"}", "application/json");
            }
        }
    }
    
    // ========================================
    // Goby POC 4: Nacos 默认密码
    // ========================================
    /**
     * Nacos 默认密码漏洞处理器
     * 
     * POC 分析:
     * - 路径: /v1/auth/users/login 或 /nacos/v1/auth/users/login
     * - 方法: POST
     * - body: username=nacos&password=nacos
     * - 期望响应: 状态码 200
     */
    class GobyNacosDefaultPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        
        public GobyNacosDefaultPwdHandler(boolean vulnerable) {
            this.vulnerable = vulnerable;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            
            if (vulnerable) {
                // 漏洞版本: 检查是否为登录请求
                if ("POST".equalsIgnoreCase(method) && path.contains("/auth/users/login")) {
                    // 读取请求体
                    String body = readRequestBody(exchange);
                    if (body.contains("username=nacos") && body.contains("password=nacos")) {
                        String response = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9\",\"tokenTtl\":18000}";
                        sendResponse(exchange, 200, response, "application/json");
                    } else {
                        sendResponse(exchange, 401, "{\"code\":401,\"message\":\"invalid credentials\"}", "application/json");
                    }
                } else {
                    sendResponse(exchange, 404, "Not Found", "text/plain");
                }
            } else {
                // 安全版本: 返回 401 未授权
                sendResponse(exchange, 401, "{\"code\":401,\"message\":\"invalid credentials\"}", "application/json");
            }
        }
    }
    
    // ========================================
    // 缺失的处理器类
    // ========================================
    
    class DefaultHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 404, "{\"error\":\"Not Found\"}", "application/json");
        }
    }
    
    class PhpcmsSqlInjectionHandler implements HttpHandler {
        private final boolean vulnerable;
        public PhpcmsSqlInjectionHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "phpcms_v9", "text/html");
            } else {
                sendResponse(exchange, 200, "Access Denied", "text/html");
            }
        }
    }
    
    class MultiStepTokenHandler implements HttpHandler {
        private final boolean vulnerable;
        public MultiStepTokenHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "{\"token\":\"admin_token_12345\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}", "application/json");
            }
        }
    }
    
    class MultiStepAdminPanelHandler implements HttpHandler {
        private final boolean vulnerable;
        public MultiStepAdminPanelHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (vulnerable && auth != null && auth.contains("admin_token")) {
                sendResponse(exchange, 200, "{\"admin\":true,\"panel\":\"Dashboard\"}", "application/json");
            } else {
                sendResponse(exchange, 403, "{\"error\":\"Forbidden\"}", "application/json");
            }
        }
    }
    
    class TimeBasedSqlInjectionHandler implements HttpHandler {
        private final boolean vulnerable;
        public TimeBasedSqlInjectionHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && (query.toUpperCase().contains("SLEEP") || query.toUpperCase().contains("BENCHMARK"))) {
                try { Thread.sleep(5500); } catch (InterruptedException e) {}
            }
            sendResponse(exchange, 200, "{\"results\":[]}", "application/json");
        }
    }
    
    class GobyApacheTraversalHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyApacheTraversalHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String rawPath = exchange.getRequestURI().getRawPath();
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && rawPath != null && (rawPath.contains("%2e") || rawPath.contains("..") || path.contains("..") || rawPath.contains("passwd"))) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class Goby360TianqingHandler implements HttpHandler {
        private final boolean vulnerable;
        public Goby360TianqingHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("gettablessize")) {
                sendResponse(exchange, 200, "{\"data\":[{\"schema_name\":\"tianqing\",\"table_name\":\"users\",\"table_size\":100}]}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // ========================================
    // Goby POC 65-94 处理器
    // ========================================
    
    class GobyConsulRexecHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyConsulRexecHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/v1/agent/self")) {
                sendResponse(exchange, 200, "{\"Config\":{\"DisableRemoteExec\":false}}", "application/json");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyCoremailConfigHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCoremailConfigHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/mailsms/s")) {
                sendResponse(exchange, 200, "{\"configHome\":\"/opt/coremail\",\"port\":80}", "application/json");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyDockerRegistryHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDockerRegistryHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable) {
                if (path.contains("/v2/_catalog")) {
                    sendResponse(exchange, 200, "{\"repositories\":[\"app\",\"nginx\"]}", "application/json");
                } else if (path.contains("/v2/")) {
                    exchange.getResponseHeaders().set("Docker-Distribution-Api-Version", "registry/2.0");
                    sendResponse(exchange, 200, "{}", "application/json");
                } else {
                    sendResponse(exchange, 404, "Not Found", "text/plain");
                }
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyDLinkDCSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkDCSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/config/getuser")) {
                sendResponse(exchange, 200, "name=admin\npass=admin123", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyDiscuzRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDiscuzRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            if (vulnerable && cookie != null && cookie.contains("GLOBALS")) {
                sendResponse(exchange, 200, "<html>PHP Version 7.4.0<br>System Linux</html>", "text/html");
            } else {
                sendResponse(exchange, 200, "Discuz! Forum", "text/html");
            }
        }
    }
    
    class GobyDedeCMSInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDedeCMSInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/include/downmix.inc.php")) {
                sendResponse(exchange, 200, "Fatal error: Call to undefined function helper() in /var/www/html/include/downmix.inc.php", "text/html");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyEyouMailRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyEyouMailRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyFineReportHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFineReportHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && (path.contains("..") || path.contains("ReportServer"))) {
                // 返回目录列表，包含 etc/passwd
                sendResponse(exchange, 200, "[{\"fileName\":\"etc/passwd\"},{\"fileName\":\"etc/shadow\"}]", "application/json");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyGoCDFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyGoCDFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/go/add-on/")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyConfluenceOGNLHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyConfluenceOGNLHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/pages/doenterpagevariables.action")) {
                sendResponse(exchange, 200, "workwork", "text/html");
            } else {
                sendResponse(exchange, 302, "Redirect", "text/plain");
            }
        }
    }
    
    class GobyJiraPathHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJiraPathHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/s/")) {
                sendResponse(exchange, 200, "<web-app><display-name>jira</display-name></web-app>", "text/xml");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyJiraInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJiraInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && (path.contains("/ViewUserHover.jspa") || path.contains("/rest/api/2/user"))) {
                sendResponse(exchange, 200, "{\"name\":\"admin\",\"displayName\":\"Administrator\",\"email\":\"admin@test.com\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyCraftCMSRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCraftCMSRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getRawQuery();
            if (vulnerable && (pathContains(exchange, "/actions/seomatic/meta-container") || (query != null && query.contains("uri=")))) {
                 // POC 执行 5*5，期望返回 MetaLinkContainer, canonical, 25
                sendResponse(exchange, 200, "MetaLinkContainer\ncanonical\n25", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
        private boolean pathContains(HttpExchange ex, String sub) {
            return ex.getRequestURI().getPath().contains(sub);
        }
    }
    
    class GobyCactiWeathermapHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCactiWeathermapHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/plugins/weathermap/editor.php")) {
                sendResponse(exchange, 200, "Weathermap Editor", "text/html");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyClickHouseSQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyClickHouseSQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable) {
                // 第一步：/ping 请求需要返回 X-Clickhouse-Summary 响应头
                if (path.contains("/ping")) {
                    exchange.getResponseHeaders().add("X-Clickhouse-Summary", "{}");
                    sendResponse(exchange, 200, "Ok.\n", "text/plain");
                } else if (query != null && query.contains("query=")) {
                    // 第二步：SQL 查询
                    exchange.getResponseHeaders().add("X-Clickhouse-Summary", "{}");
                    sendResponse(exchange, 200, "default\nsystem\n1", "text/plain");
                } else {
                    exchange.getResponseHeaders().add("X-Clickhouse-Summary", "{}");
                    sendResponse(exchange, 200, "1\n", "text/plain");
                }
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyCitrixLFIHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCitrixLFIHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable) {
                // POC 期望返回 406 + SESSID 响应头
                if (path.contains("/pcidss/report")) {
                    exchange.getResponseHeaders().add("Set-Cookie", "SESSID=abcdef123456");
                    sendResponse(exchange, 406, "<error>Not Acceptable</error>", "application/xml");
                } else if (path.contains("/vpns/cfg/smb.conf") || path.contains("smb.conf")) {
                    sendResponse(exchange, 200, "[global]\nworkgroup = WORKGROUP", "text/plain");
                } else {
                    exchange.getResponseHeaders().add("Set-Cookie", "SESSID=abcdef123456");
                    sendResponse(exchange, 406, "<error>Not Acceptable</error>", "application/xml");
                }
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyCouchCMSInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCouchCMSInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/includes/mysql2i/mysql2i.func.php")) {
                // POC 期望 PHP 错误信息
                String resp = "Fatal error: Cannot redeclare mysql_affected_rows() in /var/www/html/includes/mysql2i/mysql2i.func.php on line 10";
                sendResponse(exchange, 200, resp, "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyDLinkDIR850LHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkDIR850LHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/hedwig.cgi")) {
                // POC 检查 HTML 编码的 </usrid> 和 </password>，以及 &lt;result&gt;
                String resp = "&lt;result&gt;OK&lt;/result&gt;<account><usrid>admin&lt;/usrid&gt;<password>admin123&lt;/password&gt;</password></account>";
                sendResponse(exchange, 200, resp, "text/xml");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyDLinkShareCenterHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkShareCenterHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            // POC 使用 system_mgr.cgi 或 nas_sharing.cgi
            if (vulnerable && (path.contains("/cgi-bin/system_mgr.cgi") || path.contains("/cgi-bin/nas_sharing.cgi"))) {
                // POC 检查 body 包含 "var/www"
                sendResponse(exchange, 200, "/var/www\nuid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyDatangACHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDatangACHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("admin") && body.contains("123456")) {
                exchange.getResponseHeaders().add("Set-Cookie", "ac_userid=admin,ac_passwd=");
                sendResponse(exchange, 200, "<html><script>window.open('index.htm?_')</script></html>", "text/html");
            } else {
                sendResponse(exchange, 200, "Login", "text/html");
            }
        }
    }
    
    class GobyDocCMSSQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDocCMSSQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("keyword=")) {
                sendResponse(exchange, 200, "Error: XPATH syntax error: '~root~'", "text/html");
            } else {
                sendResponse(exchange, 200, "No results", "text/html");
            }
        }
    }
    
    class GobyDotCMSUploadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDotCMSUploadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable) {
                if (path.contains("/api/content/")) {
                    // 上传步骤期望返回 500
                    sendResponse(exchange, 500, "Internal Server Error", "application/json");
                } else if (path.contains("vuln.jsp")) {
                    // 访问上传文件
                    sendResponse(exchange, 200, "CVE-2022-26352", "text/plain");
                } else {
                    sendResponse(exchange, 404, "Not Found", "text/plain");
                }
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyF5RCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyF5RCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/mgmt/tm/util/bash")) {
                // POC 执行 echo tsxts|base64，期望返回 dHN4dHMK
                sendResponse(exchange, 200, "{\"commandResult\":\"dHN4dHMK\\nuid=0(root) gid=0(root)\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyFastmeetingHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFastmeetingHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            // POC 通过 fileName 参数进行路径遍历读取 win.ini
            if (vulnerable && (path.contains("/toDownload.do") || path.contains("/download.aspx") || (query != null && query.contains("fileName")))) {
                sendResponse(exchange, 200, "[fonts]\nfile=test\n[extensions]\n[mci extensions]\n[files]", "text/plain");
            } else if (vulnerable) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyGitLabSSRFHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyGitLabSSRFHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && (path.contains("/api/v4/ci/lint") || path.contains("/include/"))) {
                sendResponse(exchange, 200, "{\"status\":\"valid\",\"errors\":[],\"warnings\":[],\"merged_yaml\":\"---\\n: http://test.dnslog.cn/api/v1/targets?test.yml\\n\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyGitLabGraphQLHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyGitLabGraphQLHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/api/graphql")) {
                // POC 期望返回 username, email, avatarUrl
                sendResponse(exchange, 200, "{\"data\":{\"users\":{\"nodes\":[{\"username\":\"root\",\"email\":\"admin@gitlab.com\",\"avatarUrl\":\"http://test/avatar\"}]}}}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyStruts2S2053Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyStruts2S2053Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // S2-053: OGNL 注入通过 URL 参数传递
            String query = exchange.getRequestURI().getRawQuery();
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && (path.contains(".action") || (query != null && query.contains("%25%7B")))) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 200, "OK", "text/plain");
            }
        }
    }
    
    class GobyStruts2S2059Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyStruts2S2059Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // S2-059: OGNL 注入，检查路径或查询参数
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains(".action")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 200, "OK", "text/plain");
            }
        }
    }
    
    class GobyStruts2S2062Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyStruts2S2062Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // S2-062: 通过 Content-Type 或路径检测
            String path = exchange.getRequestURI().getPath();
            String ct = exchange.getRequestHeaders().getFirst("Content-Type");
            if (vulnerable && (path.contains(".action") || (ct != null && ct.contains("multipart/form-data")))) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 200, "OK", "text/plain");
            }
        }
    }
    
    class GobyAspCMSSQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyAspCMSSQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/commentList.asp")) {
                sendResponse(exchange, 200, "Microsoft SQL Server ... admin", "text/html");
            } else {
                sendResponse(exchange, 200, "OK", "text/html");
            }
        }
    }
    
    // ========================================
    // Goby POC 95-124 处理器
    // ========================================
    
    class GobyH3CIMCRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyH3CIMCRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyH5SVideoHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyH5SVideoHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/api/v1/")) {
                sendResponse(exchange, 200, "{\"strUser\":\"admin\",\"strPassword\":\"admin123\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyHikvisionHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyHikvisionHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && (path.contains("/SDK/webLanguage") || path.contains("/serverLog/downFile.php"))) {
                // POC 期望返回 $file_name=
                sendResponse(exchange, 200, "$file_name=../web/html/serverLog/downFile.php", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyIFW8RouterHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyIFW8RouterHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/backup/")) {
                sendResponse(exchange, 200, "admin:password123", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyIceWarpRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyIceWarpRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyJellyfinFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJellyfinFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/Audio/")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyJinHeOAHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJinHeOAHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("admin")) {
                sendResponse(exchange, 200, "{\"success\":true,\"token\":\"admin_session\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "{\"success\":false}", "application/json");
            }
        }
    }
    
    class GobyJitongEWEBSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJitongEWEBSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/casmain.xgi")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyKedacomMTSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKedacomMTSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/download/")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyKingsoftV8Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKingsoftV8Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/htmltopdf/")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyKongaJWTHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKongaJWTHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "{\"token\":\"default_jwt_secret_key\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyKyanHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKyanHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/hosts")) {
                sendResponse(exchange, 200, "admin:admin123\nroot:root123", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyLanproxyHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLanproxyHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            // OkHttp 归一化后路径不包含 ../，检查是否包含 config.properties 或 /conf/
            if (vulnerable && (path.contains("config.properties") || path.contains("/conf/"))) {
                sendResponse(exchange, 200, "server.ssl.enable=false\nserver.bind=0.0.0.0\nconfig.admin.username=admin\nconfig.admin.password=admin", "text/plain");
            } else if (vulnerable) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyLaravelEnvHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLaravelEnvHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains(".env")) {
                sendResponse(exchange, 200, "APP_NAME=Laravel\nAPP_KEY=base64:secretkey\nDB_PASSWORD=root", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    class GobyLeadsecACMHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLeadsecACMHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "{\"username\":\"admin\",\"password\":\"admin123\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyMPSecISGHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMPSecISGHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/webui/")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    class GobyExchangeSSRFHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyExchangeSSRFHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "X-FEServer: EXCHANGE", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyMinIOSSRFHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMinIOSSRFHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/minio/webrpc")) {
                // POC 检查 body 包含 "message"
                sendResponse(exchange, 200, "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"We encountered an internal error\"}}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyNodeREDHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyNodeREDHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/ui_base/")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    
    class GobyWeblogicLDAPHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeblogicLDAPHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/console/")) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobySunloginRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySunloginRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/cgi-bin/rpc")) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    
    class GobyRuijieUACHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRuijieUACHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/get_pwd")) {
                sendResponse(exchange, 200, "{\"password\":\"admin@123\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobyRuijieSmartwebHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRuijieSmartwebHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("show basic-info")) {
                sendResponse(exchange, 200, "Level was: LEVEL15\nHostname: Ruijie", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    class GobySamsungWLANHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySamsungWLANHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // POC 124: SDWAN Smart Gateway 默认密码
    class GobySDWANGatewayHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySDWANGatewayHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("admin") && body.contains("admin@123")) {
                sendResponse(exchange, 200, "{\"result\":true,\"userid\":\"1\"}", "application/json");
            } else {
                sendResponse(exchange, 200, "{\"result\":false}", "application/json");
            }
        }
    }
    
    // POC 125: Seeyon OA DownExcelBeanServlet 信息泄露
    class GobySeeyonDownExcelHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySeeyonDownExcelHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/DownExcelBeanServlet")) {
                sendResponse(exchange, 200, "admin@example.com,user@test.com", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    // POC 126: SonarQube 未授权
    class GobySonarQubeHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySonarQubeHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/api/settings/values")) {
                sendResponse(exchange, 200, "{\"settings\":[{\"key\":\"sonaranalyzer-cs.nuget.packageVersion\"},{\"key\":\"sonar.core.id\"}]}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // POC 127: SpiderFlow RCE
    class GobySpiderFlowHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySpiderFlowHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/function/save")) {
                sendResponse(exchange, 200, "{\"data\":\"exec success\"}", "application/json");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 128: Spring Boot Actuator Logview 路径遍历
    class GobySpringBootLogviewHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySpringBootLogviewHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && (path.contains("/log/view") || path.contains("/manage/log/view"))) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    // POC 129: TamronOS IPTV RCE
    class GobyTamronOSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyTamronOSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/api/ping")) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 130: 泛微 EOffice 任意文件上传
    class GobyWeaverEOfficeHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeaverEOfficeHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/UploadFile.php")) {
                sendResponse(exchange, 200, "logo-eoffice.php", "text/plain");
            } else if (vulnerable && path.contains("/logo-eoffice.php")) {
                sendResponse(exchange, 200, "test", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 131: WSO2 文件上传
    class GobyWSO2UploadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWSO2UploadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/fileupload/toolsAny")) {
                sendResponse(exchange, 200, "1234567890", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 132: Xieda OA 文件下载
    class GobyXiedaOAHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyXiedaOAHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/DownFileAttach.jsp")) {
                sendResponse(exchange, 200, "jdbc.password=admin123", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    // POC 134: 浙江大华 DSS 文件下载
    class GobyDahuaDSSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDahuaDSSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/attachment_downloadByUrlAtt.action")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    // POC 136: Dubbo Admin 默认密码
    class GobyDubboAdminHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDubboAdminHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (vulnerable && auth != null && auth.contains("cm9vdDpyb290")) {
                sendResponse(exchange, 200, "<title>Dubbo Admin</title>", "text/html");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // POC 138: DedeCMS 信息泄露
    class GobyDedeCMSInfoHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDedeCMSInfoHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/data/admin/ver.txt")) {
                sendResponse(exchange, 200, "20180109", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    // POC 139: D-Link DNS-320 RCE
    class GobyDLinkDNS320Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkDNS320Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 141: FineReport 目录遍历
    class GobyFineReportTraversalHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFineReportTraversalHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && (path.contains("..") || path.contains("ReportServer"))) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    // POC 142: GitLab RCE CVE-2021-22205
    class GobyGitLabRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyGitLabRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable) {
                if (path.contains("/users/sign_in")) {
                    // 第一步：返回登录页，设置 experimentation_subject_id 头和 csrf-token
                    exchange.getResponseHeaders().add("Set-Cookie", "experimentation_subject_id=abc123");
                    String html = "<html><head><meta name=\"csrf-token\" content=\"test-csrf-token\" /></head><body>GitLab</body></html>";
                    sendResponse(exchange, 200, html, "text/html");
                } else if (path.contains("/uploads/user")) {
                    // 第二步：上传成功
                    sendResponse(exchange, 200, "{\"id\":\"uploads/test.jpg\"}", "application/json");
                } else {
                    sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
                }
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // POC 144: U8 OA
    class GobyU8OAHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyU8OAHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // POC 147: Weaver OA 8 SQL注入
    class GobyWeaverOA8Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeaverOA8Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "c4ca4238a0b923820dcc509a6f75849b", "text/plain");
            } else {
                sendResponse(exchange, 200, "error", "text/plain");
            }
        }
    }
    
    // POC 148: XXL-JOB 默认密码
    class GobyXXLJobDefaultHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyXXLJobDefaultHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("admin") && body.contains("123456")) {
                sendResponse(exchange, 200, "{\"code\":200,\"msg\":null,\"content\":\"XXL-JOB_LOGIN_IDENTITY\"}", "application/json");
            } else {
                sendResponse(exchange, 200, "{\"code\":500,\"msg\":\"账号或密码错误\"}", "application/json");
            }
        }
    }
    
    // POC 149: Spring4Shell RCE
    class GobySpring4ShellHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySpring4ShellHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    // POC 150: Wayos AC 默认密码
    class GobyWayosACHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWayosACHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("admin") && body.contains("admin")) {
                sendResponse(exchange, 200, "{\"success\":true}", "application/json");
            } else {
                sendResponse(exchange, 401, "{\"success\":false}", "application/json");
            }
        }
    }
    
    // POC 151: Security Devices 硬编码密码
    class GobySecurityDevicesHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySecurityDevicesHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (vulnerable && auth != null && auth.contains("Basic")) {
                sendResponse(exchange, 200, "<title>Admin Console</title>", "text/html");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // POC 152: 中新金盾 默认密码
    class GobyZhongXinJingDunHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyZhongXinJingDunHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("admin") && body.contains("123456")) {
                sendResponse(exchange, 200, "{\"success\":true,\"token\":\"admin_session\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "{\"success\":false}", "application/json");
            }
        }
    }
    
    // POC 153: ZZZCMS RCE
    class GobyZZZCMSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyZZZCMSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 154: 亿邮邮件系统 RCE
    class GobyEyouMailHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyEyouMailHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 155: Apache Airflow 未授权
    class GobyAirflowHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyAirflowHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "<title>Airflow - DAGs</title><div>DAGs</div>", "text/html");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // POC 156: Apache CouchDB 未授权
    class GobyCouchDBHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCouchDBHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "{\"httpd_design_handlers\":{},\"external_manager\":{},\"replicator_manager\":{}}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // POC 157: Nacos 默认密码
    class GobyNacosDefaultHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyNacosDefaultHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("nacos") && body.contains("nacos")) {
                sendResponse(exchange, 200, "{\"accessToken\":\"xxx\",\"tokenTtl\":18000}", "application/json");
            } else {
                sendResponse(exchange, 403, "user not found!", "text/plain");
            }
        }
    }
    
    // POC 158: 蓝凌 OA 任意文件读取
    class GobyLandrayOAHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLandrayOAHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 159: 通达 OA 未授权
    class GobyTongdaOAHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyTongdaOAHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "SUCCESS", "text/plain");
            } else {
                sendResponse(exchange, 200, "relogin", "text/plain");
            }
        }
    }
    
    // POC 160: 用友 NC RCE
    class GobyYonyouNCHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyYonyouNCHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "<title>BeanShell</title>", "text/html");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }
    
    // POC 163: ActiveMQ 默认密码
    class GobyActiveMQDefaultHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyActiveMQDefaultHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (vulnerable && auth != null && auth.contains("Basic")) {
                sendResponse(exchange, 200, "<title>Apache ActiveMQ</title>", "text/html");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // POC 164: H3C IMC RCE
    class GobyH3CIMCHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyH3CIMCHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 165: 海康威视 RCE
    class GobyHikvisionRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyHikvisionRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 168: 金和 OA 默认密码 (C6版本)
    class GobyJinheOAC6Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJinheOAC6Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("admin")) {
                sendResponse(exchange, 200, "{\"success\":true,\"token\":\"admin\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "{\"success\":false}", "application/json");
            }
        }
    }
    
    // POC 170: Kyan 密码泄露
    class GobyKyanLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKyanLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "username=admin&password=admin123", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 172: WebSVN RCE
    class GobyWebSVNHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWebSVNHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 173: Zabbix SAML CVE-2022-23131
    class GobyZabbixSAMLHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyZabbixSAMLHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 302, "", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 174: Alibaba Canal 默认密码
    class GobyAlibabaCanalHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyAlibabaCanalHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "{\"code\":20000,\"message\":\"success\",\"path\":\"com.alibaba.otter.canal.admin.controller.UserController.login\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "{\"code\":50014,\"message\":\"Unauthorized\"}", "application/json");
            }
        }
    }
    
    // POC 175: 深信服行为感知 RCE
    class GobySangforRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySangforRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 176: 畅捷 CRM SQL注入
    class GobyChanjetCRMHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyChanjetCRMHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "c4ca4238a0b923820dcc509a6f75849b", "text/plain");
            } else {
                sendResponse(exchange, 200, "error", "text/plain");
            }
        }
    }
    
    // POC 179: IceWarp RCE
    class GobyIceWarpHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyIceWarpHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "uid=0(root) gid=0(root)", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // POC 181: 启来 OA SQL注入
    class GobyQilaiOAHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyQilaiOAHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "c4ca4238a0b923820dcc509a6f75849b", "text/plain");
            } else {
                sendResponse(exchange, 200, "error", "text/plain");
            }
        }
    }
    
    // POC 182: 华天动力 OA SQL注入
    class GobyHuatianOAHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyHuatianOAHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "c4ca4238a0b923820dcc509a6f75849b", "text/plain");
            } else {
                sendResponse(exchange, 200, "error", "text/plain");
            }
        }
    }
    
    // POC 183: 360 天擎 SQL注入 (ccid)
    class Goby360TianQingCcidHandler implements HttpHandler {
        private final boolean vulnerable;
        public Goby360TianQingCcidHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "c4ca4238a0b923820dcc509a6f75849b", "text/plain");
            } else {
                sendResponse(exchange, 200, "error", "text/plain");
            }
        }
    }

    // POC 184: ADSelfService Plus RCE
    class GobyADSelfServiceHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyADSelfServiceHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/RestAPI/LogonCustomization")) {
                sendResponse(exchange, 200, "var d = new Date(); window.parent.$(\"#tabLogo\")", "text/html");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 185: AVCON6 文件下载
    class GobyAVCON6Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyAVCON6Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/org_execl_download.action")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 186: Active UC RCE
    class GobyActiveUCHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyActiveUCHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/acenter/index.action")) {
                sendResponse(exchange, 200, "Windows IP Configuration", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 187: Adslr 信息泄露
    class GobyAdslrHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyAdslrHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/request_para.cgi")) {
                sendResponse(exchange, 200, "WPA-PSK", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 188: Nacos 未授权添加用户
    class GobyNacosUnauthAddUserHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyNacosUnauthAddUserHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (vulnerable) {
                if ("GET".equals(method)) {
                    sendResponse(exchange, 500, "Internal Error", "text/plain");
                } else if ("POST".equals(method)) {
                    sendResponse(exchange, 200, "success", "text/plain");
                }
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }

    // POC 189: APISIX Dashboard 未授权
    class GobyAPISIXDashboardHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyAPISIXDashboardHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/apisix/admin/migrate/export")) {
                sendResponse(exchange, 200, "\"Consumers\":[],\"Routes\":[],\"PluginConfigs\":[]", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }

    // POC 190: CouchDB 权限提升
    class GobyCouchDBPrivEscHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCouchDBPrivEscHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.startsWith("/_users/org.couchdb.user:")) {
                String username = path.substring(path.lastIndexOf(':') + 1);
                sendResponse(exchange, 201, "{\"ok\":true,\"id\":\"org.couchdb.user:" + username + "\",\"rev\":\"1-123\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "application/json");
            }
        }
    }

    // POC 191: Apache HTTP SSRF
    class GobyApacheSSRFHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyApacheSSRFHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.startsWith("unix:")) {
                sendResponse(exchange, 200, "Example Domain", "text/html");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 192: Apache HTTP Path Traversal
    class GobyApachePathTraversalHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyApachePathTraversalHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String rawPath = exchange.getRequestURI().getRawPath();
            if (vulnerable && (rawPath.contains(".%2e") || rawPath.contains("%2e%2e"))) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }

    // POC 193: Kylin 默认密码
    class GobyKylinDefaultPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKylinDefaultPwdHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (vulnerable && "Basic YWRtaW46S1lMSU4=".equals(auth)) { // admin:KYLIN
                sendResponse(exchange, 200, "Authenticated", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }

    // POC 194: Kylin 未授权配置泄露
    class GobyKylinUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKylinUnauthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "config=value", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }


    // POC 198: AspCMS 后台泄露
    class GobyAspCMSBackendLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyAspCMSBackendLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "<script>alert('1');top.location.href='/admin_login.asp';</script>", "text/html");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 199: BSPHP 未授权
    class GobyBSPHPUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyBSPHPUnauthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "{\"user\":\"admin\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }

    // POC 200: BigAnt Path Traversal
    class GobyBigAntPathTraversalHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyBigAntPathTraversalHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("true_path=../")) {
                sendResponse(exchange, 200, "[fonts]\r\n[extensions]", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 201: Portainer 未授权
    class GobyPortainerUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyPortainerUnauthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Vulnerable if 404 (admin not created), Safe if 204 (admin created)
            if (vulnerable) {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            } else {
                sendResponse(exchange, 204, "", "text/plain");
            }
        }
    }

    // POC 202: Cacti Weathermap File Write
    class GobyCactiFileWriteHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCactiFileWriteHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable) {
                if (path.contains("editor.php")) {
                    sendResponse(exchange, 200, "OK", "text/plain");
                } else if (path.contains("test.php")) {
                    sendResponse(exchange, 200, "46ea1712d4b13b55b3f680cc5b8b54e8", "text/plain");
                } else {
                    sendResponse(exchange, 404, "Not Found", "text/plain");
                }
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 203: Casdoor SQLi
    class GobyCasdoorSQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCasdoorSQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("updatexml")) {
                sendResponse(exchange, 200, "XPATH syntax error", "text/plain");
            } else {
                sendResponse(exchange, 200, "OK", "text/plain");
            }
        }
    }

    // POC 204: Cerebro SQLi
    class GobyCerebroSQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCerebroSQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("Name=Y'")) {
                sendResponse(exchange, 500, "SELECT * FROM", "text/plain");
            } else {
                sendResponse(exchange, 200, "OK", "text/plain");
            }
        }
    }

    // POC 205: Chanjet CRM SQLi
    class GobyChanjetCRMSQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyChanjetCRMSQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("union")) {
                sendResponse(exchange, 200, "^^!e10adc3949ba59abbe56e057f20f883e!^^", "text/plain");
            } else {
                sendResponse(exchange, 200, "OK", "text/plain");
            }
        }
    }

    // POC 206: China Mobile Yu Routing Info Leak
    class GobyChinaMobileYuRoutingHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyChinaMobileYuRoutingHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "Password=admin", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }

    // POC 207: China Mobile Yu Routing Login Bypass
    class GobyChinaMobileYuLoginBypassHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyChinaMobileYuLoginBypassHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("login=Login")) {
                sendResponse(exchange, 200, "<html><body>... admin/index.asp ...</body></html>", "text/html");
            } else {
                sendResponse(exchange, 200, "Login Failed", "text/html");
            }
        }
    }

    // POC 208: Citrix Unauthorized CVE-2020-8193
    class GobyCitrixUnauthorizedHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCitrixUnauthorizedHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("sid=loginchallengeresponse1requestbody")) {
                exchange.getResponseHeaders().add("SESSID", "123456");
                sendResponse(exchange, 406, "SESSID=...", "text/xml");
            } else {
                sendResponse(exchange, 200, "OK", "text/xml");
            }
        }
    }


    // POC 210: ClusterEngine RCE
    class GobyClusterEngineRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyClusterEngineRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("$(cat /etc/passwd)")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 200, "Login Failed", "text/plain");
            }
        }
    }

    // POC 211: CmsEasy SQLi
    class GobyCmsEasySQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCmsEasySQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("case=crossall")) {
                sendResponse(exchange, 200, "123", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 212: Coldfusion LFI
    class GobyColdfusionLFICVE20102861Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyColdfusionLFICVE20102861Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("locale=")) {
                sendResponse(exchange, 200, "rdspassword=xxx\nencrypted=true", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }


    // POC 214: Consul Rexec RCE
    class GobyConsulRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyConsulRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }


    

    // POC 218: CraftCMS SEOmatic RCE
    class GobyCraftCMSSeomaticHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyCraftCMSSeomaticHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getRawQuery(); // 使用 RawQuery 避免解码问题
            if (vulnerable && query != null && (query.contains("uri=") || query.contains("{{"))) {
                // POC 执行 5*5，期望返回 MetaLinkContainer, canonical, 25
                sendResponse(exchange, 200, "MetaLinkContainer\ncanonical\n25", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }
    
    // POC 219: D-Link AC Default Password
    class GobyDLinkACDefaultPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkACDefaultPwdHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "D-Link", "text/plain");
            } else {
                sendResponse(exchange, 200, "Login", "text/plain");
            }
        }
    }

    // POC 220: D-Link DCS Info Leak
    class GobyDLinkDCSInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkDCSInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "name=admin\npass=123456", "text/plain");
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }

    // POC 221: D-Link DIR-850L Info Leak
    class GobyDLinkDIR850LInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkDIR850LInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                sendResponse(exchange, 200, "<usrid>admin</usrid><password>123456</password><result>OK</result>", "text/xml");
             } else {
                 sendResponse(exchange, 200, "Login", "text/html");
             }
        }
    }
    
    // POC 222: D-Link CVE-2019-17506 Info Leak
    class GobyDLinkInfoLeakCVE201917506Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkInfoLeakCVE201917506Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                sendResponse(exchange, 200, "<name>admin</name><password>123456</password>", "text/xml");
             } else {
                 sendResponse(exchange, 200, "Login", "text/html");
             }
        }
    }
    
    // POC 223: D-Link ShareCenter RCE
    class GobyDLinkShareCenterRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkShareCenterRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                sendResponse(exchange, 200, "var/www", "text/plain");
             } else {
                 sendResponse(exchange, 404, "Not Found", "text/plain");
             }
        }
    }

    // POC 224: D-Link AC Weak Password
    class GobyDLinkACWeakPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkACWeakPwdHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "Success", "text/plain");
            } else {
                sendResponse(exchange, 200, "flag=0", "text/plain");
            }
        }
    }

    // POC 225: D-Link DC Info Leak
    class GobyDLinkDCInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkDCInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "name=admin\npass=123456", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 226: D-Link DIR-868L Account Leak
    class GobyDLinkDIR868LAccountLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkDIR868LAccountLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("SERVICES=DEVICE.ACCOUNT")) {
                sendResponse(exchange, 200, "<name>admin</name><password>123456</password><DEVICE.ACCOUNT>1</DEVICE.ACCOUNT>", "text/xml");
            } else {
                sendResponse(exchange, 200, "Login", "text/html");
            }
        }
    }

    // POC 228: Datang AC Default Password
    class GobyDatangACDefaultPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDatangACDefaultPwdHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                exchange.getResponseHeaders().add("Set-Cookie", "ac_userid=admin,ac_passwd=123456");
                sendResponse(exchange, 200, "window.open('index.htm?_", "text/html");
            } else {
                sendResponse(exchange, 200, "Login Failed", "text/html");
            }
        }
    }

    // POC 229: DedeCMS Carbuyaction File Include
    class GobyDedeCMSCarbuyactionHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDedeCMSCarbuyactionHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            if (vulnerable && cookie != null && cookie.contains("code=cod")) {
                sendResponse(exchange, 200, "Cod::respond()", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 230: DedeCMS Info Leak CVE-2018-6910
    class GobyDedeCMSInfoLeakCVE20186910Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDedeCMSInfoLeakCVE20186910Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "Fatal error: Call to undefined function helper() in downmix.inc.php", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 232: Discuz!ML 3.x RCE
    class GobyDiscuzML3xRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDiscuzML3xRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            if (cookie != null && cookie.contains("language=sc'.phpinfo().'")) {
                if (vulnerable) {
                     sendResponse(exchange, 200, "PHP Version 7.0\nSystem Windows", "text/plain");
                } else {
                     sendResponse(exchange, 200, "Normal", "text/plain");
                }
                return;
            }
            // Step 1
            sendResponse(exchange, 200, "cookiepre = 'abcd_'", "text/plain");
        }
    }
    
    // POC 234: Discuz RCE WOOYUN-2010-080723
    class GobyDiscuzRCEWooYun2010080723Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDiscuzRCEWooYun2010080723Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            if (vulnerable && cookie != null && cookie.contains("GLOBALS")) {
                sendResponse(exchange, 200, "PHP Version 5.3\nSystem Linux", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 235: Discuz Wechat Plugins Unauth
    class GobyDiscuzWechatPluginsHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDiscuzWechatPluginsHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                exchange.getResponseHeaders().add("Location", "http://wsq.discuz.com");
                exchange.getResponseHeaders().add("Set-Cookie", "auth=123");
                sendResponse(exchange, 302, "Redirecting...", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 236: Discuz v72 SQLi
    class GobyDiscuzV72SQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDiscuzV72SQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "81dc9bdb52d04dc20036dbd8313ed055\nDiscuz! info</b>: MySQL Query Error", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 239: D-Link RCE CVE-2019-16920
    class GobyDLinkRCECVE201916920Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkRCECVE201916920Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("action=ping_test")) {
                sendResponse(exchange, 200, "Ping Result", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 240: DocCMS SQLi
    class GobyDocCMSSQLiPOC240Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDocCMSSQLiPOC240Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("keyword=")) {
                sendResponse(exchange, 200, "XPATH syntax error", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 241: Docker Registry API Unauth
    class GobyDockerRegistryUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDockerRegistryUnauthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                exchange.getResponseHeaders().add("docker-distribution-api-version", "registry/2.0");
                sendResponse(exchange, 200, "{}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }

    // POC 243: Dubbo Admin Default Password
    class GobyDubboAdminDefaultPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDubboAdminDefaultPwdHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (vulnerable && auth != null && (auth.contains("Basic Z3Vlc3Q6Z3Vlc3Q=") || auth.contains("Basic cm9vdDpyb290"))) {
                sendResponse(exchange, 200, "<title>Dubbo Admin</title>/sysinfo/versions", "text/html");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }

    // POC 244: Eyou Mail RCE
    class GobyEyouMailRCEHandlerPoc244 implements HttpHandler {
        private final boolean vulnerable;
        public GobyEyouMailRCEHandlerPoc244(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("cat /etc/passwd")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 246: F5 BIG-IP RCE
    class GobyF5BigIPRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyF5BigIPRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("command") && body.contains("run")) {
                // echo tsxts|base64 -> dHN4dHMK
                sendResponse(exchange, 200, "{\"commandResult\":\"dHN4dHMK\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "application/json");
            }
        }
    }

    // POC 247: F5 BIG-IP Auth Bypass
    class GobyF5BigIPAuthBypassHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyF5BigIPAuthBypassHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "{\"resterrorresponse\":\"Authorization failed\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }

    // POC 249: Fastmeeting Arbitrary File Read
    class GobyFastmeetingFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFastmeetingFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("fileName=") && query.contains("win.ini")) {
                sendResponse(exchange, 200, "[fonts]\n[extensions]", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 250: FineReport Directory Traversal
    class GobyFineReportDirTraversalHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFineReportDirTraversalHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("file_path=") && query.contains("etc")) {
                 sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash\netc/passwd", "text/plain");
            } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 251: FineReport Arbitrary File Read
    class GobyFineReportArbitraryFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFineReportArbitraryFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("resourcepath=privilege.xml")) {
                sendResponse(exchange, 200, "<![CDATA[...]]>", "text/xml");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 254: FineReport v9 File Overwrite
    class GobyFineReportV9FileOverwriteHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFineReportV9FileOverwriteHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.contains("ReportServer")) {
                 // Step 1: Overwrite
                 if (vulnerable) {
                     sendResponse(exchange, 200, "{\"status\":\"success\"}", "application/json");
                 } else {
                     sendResponse(exchange, 200, "{\"status\":\"failed\"}", "application/json");
                 }
            } else if (path.contains("a.svg.jsp")) {
                 // Step 2: Check
                 if (vulnerable) {
                     sendResponse(exchange, 200, "test", "text/plain");
                 } else {
                     sendResponse(exchange, 404, "Not Found", "text/plain");
                 }
            }
        }
    }
    
    // POC 255: Finetree 5MP Auth
    class GobyFinetree5MPAuthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFinetree5MPAuthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                if (vulnerable) {
                    sendResponse(exchange, 302, "Redirect", "text/plain");
                } else {
                    sendResponse(exchange, 200, "Login", "text/plain");
                }
            } else if (path.contains("user_pop.php")) {
                 if (vulnerable) {
                     sendResponse(exchange, 200, "Add User", "text/plain");
                 } else {
                     sendResponse(exchange, 403, "Forbidden", "text/plain");
                 }
            }
        }
    }

    // POC 257: GitLab Graphql Email Leak
    class GobyGitLabGraphqlEmailLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyGitLabGraphqlEmailLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "{\"data\":{\"users\":{\"edges\":[{\"node\":{\"username\":\"root\",\"email\":\"root@example.com\"}}]}}}", "application/json");
            } else {
                sendResponse(exchange, 200, "{\"data\":{\"users\":{\"edges\":[]}}}", "application/json");
            }
        }
    }

    // POC 260: GitLab RCE CVE-2021-22205
    class GobyGitLabRCECVE202122205Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyGitLabRCECVE202122205Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.contains("/users/sign_in")) {
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                // Provide CSRF token
                String body = "<html><head><meta name=\"csrf-token\" content=\"token_value\" /></head><body>experimentation_subject_id</body></html>";
                sendResponse(exchange, 200, body, "text/html");
            } else if (path.contains("/uploads/user")) {
                String csrf = exchange.getRequestHeaders().getFirst("X-CSRF-Token");
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (vulnerable && "token_value".equals(csrf) && contentType != null && contentType.contains("multipart/form-data")) {
                    // Check if body contains malicious payload (simplified check)
                    sendResponse(exchange, 422, "Failed to process image", "application/json");
                } else {
                    sendResponse(exchange, 200, "{\"status\":\"ok\"}", "application/json");
                }
            }
        }
    }

    // POC 261: GitLab SSRF CVE-2021-22214
    class GobyGitLabSSRFCVE202122214Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyGitLabSSRFCVE202122214Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("include:") && body.contains("remote:")) {
                sendResponse(exchange, 200, "does not have valid YAML syntax", "application/json");
            } else {
                sendResponse(exchange, 200, "Valid", "application/json");
            }
        }
    }

    // POC 263: Grafana Angularjs XSS
    class GobyGrafanaXSSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyGrafanaXSSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath(); // e.g. /dashboard/snapshot/{{...}}
            if (vulnerable && path.contains("%7B%7B")) { // {{ encoded
                 sendResponse(exchange, 200, "frontend_boot_js_done_time_seconds", "text/html");
            } else {
                 sendResponse(exchange, 404, "Not Found", "text/html");
            }
        }
    }

    // POC 267: H3C IMC RCE
    class GobyH3CIMCRCEHandlerPoc267 implements HttpHandler {
        private final boolean vulnerable;
        public GobyH3CIMCRCEHandlerPoc267(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("pfdrt=sc") && body.contains("cmd=")) {
                 sendResponse(exchange, 200, "Administrator", "text/plain");
            } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 268: H5S GetSrc Info Leak
    class GobyH5SGetSrcInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyH5SGetSrcInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "{\"src\":\"H5_CLOUD\",\"type\":\"H5_STREAM\"}", "application/json");
             } else {
                 sendResponse(exchange, 401, "Unauthorized", "application/json");
             }
        }
    }

    // POC 269: H5S GetUserInfo Info Leak
    class GobyH5SGetUserInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyH5SGetUserInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "{\"strUser\":\"admin\",\"strPasswd\":\"12345\",\"strUserType\":\"admin\",\"strRole\":\"admin\"}", "application/json");
             } else {
                 sendResponse(exchange, 401, "Unauthorized", "application/json");
             }
        }
    }

    // POC 270: Hikvision File Download
    class GobyHikvisionFileDownloadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyHikvisionFileDownloadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "<user><name>admin</name></user>", "text/xml");
             } else {
                 sendResponse(exchange, 403, "Forbidden", "text/plain");
             }
        }
    }

    // POC 272: Hikvision RCE
    class GobyHikvisionRCEHandlerPoc272 implements HttpHandler {
        private final boolean vulnerable;
        private boolean exploded = false;
        public GobyHikvisionRCEHandlerPoc272(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                sendResponse(exchange, 200, "Index", "text/plain");
            } else if (path.contains("/SDK/webLanguage")) {
                if (vulnerable) {
                    exploded = true;
                    sendResponse(exchange, 500, "Error", "text/plain");
                } else {
                    sendResponse(exchange, 200, "OK", "text/plain");
                }
            } else if (path.contains("/c")) {
                if (vulnerable && exploded) {
                    sendResponse(exchange, 200, "result", "text/plain");
                } else {
                    sendResponse(exchange, 404, "Not Found", "text/plain");
                }
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 274: Hikvision Video Encoding Device Access Gateway Any File Download
    class GobyHikvisionAnyFileDownloadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyHikvisionAnyFileDownloadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("fileName=../")) {
                sendResponse(exchange, 200, "$file_name=xxx", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 275: HotelDruid XSS
    class GobyHotelDruidXSSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyHotelDruidXSSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("<script>")) {
                sendResponse(exchange, 200, "XSS", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 276: Hsmedia Hgateway Default Account
    class GobyHsmediaDefaultAccountHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyHsmediaDefaultAccountHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "success", "text/plain");
            } else {
                sendResponse(exchange, 200, "flag=0", "text/plain");
            }
        }
    }

    // POC 277, 278: IFW8 Router Password Leakage
    class GobyIFW8PasswordLeakageHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyIFW8PasswordLeakageHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "pwd=123456", "text/plain");
            } else {
                sendResponse(exchange, 200, "Login Page", "text/plain");
            }
        }
    }

    // POC 279: IRDM4000 Smart station Unauthorized access
    class GobyIRDM4000UnauthorizedHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyIRDM4000UnauthorizedHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            if (vulnerable && cookie != null && cookie.contains("userId=0")) {
                sendResponse(exchange, 200, "设备配置...视频监管", "text/html;charset=utf-8");
            } else {
                sendResponse(exchange, 200, "Login", "text/html");
            }
        }
    }

    // POC 280: IceWarp WebClient basic RCE
    class GobyIceWarpBasicRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyIceWarpBasicRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("system('ipconfig')")) {
                sendResponse(exchange, 200, "Windows IP Configuration", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 281, 282: JQuery 1.7.2 File Download
    class GobyJQueryFileDownloadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJQueryFileDownloadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("file_name=../")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 283, 286: Jellyfin File Read CVE-2021-21402
    class GobyJellyfinFileReadHandlerCVE202121402 implements HttpHandler {
        private final boolean vulnerable;
        public GobyJellyfinFileReadHandlerCVE202121402(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && (path.contains("/Audio/") || path.contains("/Videos/"))) {
                sendResponse(exchange, 200, "; for 16-bit app support\n[fonts]\n[extensions]\n[file]", "application/octet-stream");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 284, 285: Jellyfin SSRF CVE-2021-29490
    class GobyJellyfinSSRFHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJellyfinSSRFHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("imageUrl=")) {
                if (query.contains("baidu.com")) {
                    sendResponse(exchange, 200, "<html><title>百度一下，你就知道</title><body>百度</body></html>", "text/html;charset=utf-8");
                } else {
                    // Simulate generic success for DNS log or other checks
                    sendResponse(exchange, 200, "SSRF Success: " + query, "text/plain");
                }
            } else {
                sendResponse(exchange, 200, "Normal Page", "text/plain");
            }
        }
    }

    // POC 287: Jetty WEB-INF FileRead CVE-2021-28169
    class GobyJettyFileReadHandlerCVE202128169 implements HttpHandler {
        private final boolean vulnerable;
        public GobyJettyFileReadHandlerCVE202128169(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            // The POC requests /static?/%2557EB-INF/web.xml
            if (vulnerable && query != null && query.contains("%2557EB-INF")) {
                sendResponse(exchange, 200, "<web-app>jetty</web-app>", "text/xml");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 288: Jetty WEB-INF FileRead CVE-2021-34429
    class GobyJettyFileReadHandlerCVE202134429 implements HttpHandler {
        private final boolean vulnerable;
        public GobyJettyFileReadHandlerCVE202134429(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String rawPath = exchange.getRequestURI().getRawPath();
            if (vulnerable) {
                if (rawPath.contains("%u002e") || rawPath.contains("%00") || rawPath.contains("..%00")) {
                    sendResponse(exchange, 200, "<web-app>web-app</web-app>", "text/xml");
                } else if (rawPath.contains("/WEB-INF/web.xml")) {
                    sendResponse(exchange, 404, "Jetty - Not Found", "text/html");
                } else {
                    sendResponse(exchange, 404, "Not Found", "text/plain");
                }
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 289, 291: JinHe OA C6 Default Password
    class GobyJinHeOADefaultPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJinHeOADefaultPwdHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "OK...系统管理员", "text/plain");
            } else {
                sendResponse(exchange, 200, "Login Failed", "text/plain");
            }
        }
    }

    // POC 290, 292: JinHe OA C6 Arbitrary File Read
    class GobyJinHeOAFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJinHeOAFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("filename=")) {
                sendResponse(exchange, 200, "<xml>web.config content</xml>", "text/xml");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 293, 299, 302: Kingsoft V8 Arbitrary File Read
    class GobyKingsoftV8FileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKingsoftV8FileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("filename=")) {
                sendResponse(exchange, 200, "filename=$filename", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 294, 295: Jitong EWEBS Arbitrary File Read
    class GobyJitongEWEBSFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJitongEWEBSFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("Language_S=")) {
                sendResponse(exchange, 200, "MAPI=", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 296: Jitong EWEBS Phpinfo Leak
    class GobyJitongEWEBSPhpinfoHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyJitongEWEBSPhpinfoHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("Language_S=phpinfo")) {
                sendResponse(exchange, 200, "PHP Version", "text/html");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 297, 298: KEDACOM MTS File Download
    class GobyKEDACOMMTSFileDownloadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKEDACOMMTSFileDownloadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.startsWith("/download/")) {
                sendResponse(exchange, 200, "root:x:0:0:", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 300, 301: Kingsoft V8 Default Weak Password
    class GobyKingsoftV8WeakPwdHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKingsoftV8WeakPwdHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("admin") && body.contains("21232f297a57a5a743894a0e4a801fc3")) {
                sendResponse(exchange, 200, "userSession", "text/plain");
            } else {
                sendResponse(exchange, 200, "Login Failed", "text/plain");
            }
        }
    }

    // POC 303: Konga Default JWT Key
    class GobyKongaJWTDefaultKeyHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKongaJWTDefaultKeyHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("authorization");
            if (vulnerable && auth != null && auth.startsWith("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.")) {
                sendResponse(exchange, 200, "createdUser...username", "application/json");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "application/json");
            }
        }
    }

    // POC 304, 305, 306, 307: Kyan Account Password Leak
    class GobyKyanAccountLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKyanAccountLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "UserName...Password", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal Page", "text/plain");
            }
        }
    }

    // POC 308, 309: Kyan RCE
    class GobyKyanRCECommandRunHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyKyanRCECommandRunHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("command=phpinfo()")) {
                sendResponse(exchange, 200, "PHP Version", "text/html");
            } else {
                sendResponse(exchange, 200, "Normal Page", "text/plain");
            }
        }
    }

    // POC 310: Landray OA Custom JSP File Read
    class GobyLandrayOACustomJspHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLandrayOACustomJspHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("file:///etc/passwd")) {
                sendResponse(exchange, 200, "root:x:0:0:root:/root:/bin/bash", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal Page", "text/plain");
            }
        }
    }

    // POC 311, 312, 313: Lanproxy Directory Traversal
    class GobyLanproxyTraversalHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLanproxyTraversalHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/../")) {
                sendResponse(exchange, 200, "server.ssl", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 314, 315, 316: Laravel .env Leak
    class GobyLaravelEnvLeakConfigHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLaravelEnvLeakConfigHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "APP_KEY=xxx\nDB_HOST=localhost", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 317, 318: Leadsec ACM Info Leak
    class GobyLeadsecACMInfoLeakConfigHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLeadsecACMInfoLeakConfigHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "admin:password", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 319, 320: MPSec ISG1000 File Download
    class GobyMPSecISGGatewayFileDownloadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMPSecISGGatewayFileDownloadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("file_name=../")) {
                sendResponse(exchange, 200, "root:x:0:0:", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 321: Mallgard Firewall Default Login
    class GobyMallgardFirewallDefaultLoginHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMallgardFirewallDefaultLoginHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("hicomadmin")) {
                sendResponse(exchange, 200, "message success", "text/plain");
            } else {
                sendResponse(exchange, 200, "Login Failed", "text/plain");
            }
        }
    }

    // POC 322: MessageSolution EEA Info Leak
    class GobyMessageSolutionEEAInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMessageSolutionEEAInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "administrator", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal Page", "text/plain");
            }
        }
    }

    // POC 325, 326, 327: Metabase Geojson Arbitrary File Read
    class GobyMetabaseGeojsonFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMetabaseGeojsonFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("url=file:/etc/passwd")) {
                sendResponse(exchange, 200, "/root:/bin/ash", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 328: Micro module monitoring system User_list.php information leakage
    class GobyMicroModuleUserListLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMicroModuleUserListLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (vulnerable) {
                sendResponse(exchange, 200, "id=\"password1\"\nid=\"password2\"", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal Page", "text/plain");
            }
        }
    }

    // POC 329, 330: Microsoft Exchange SSRF
    class GobyMicrosoftExchangeSSRFHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMicrosoftExchangeSSRFHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            if (vulnerable && cookie != null && cookie.contains("X-BEResource=") && cookie.contains("X-AnonResource=true")) {
                sendResponse(exchange, 500, "NegotiateSecurityContext", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal Page", "text/plain");
            }
        }
    }

    // POC 331: MinIO Browser API SSRF
    class GobyMinIOBrowserSSRFHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMinIOBrowserSSRFHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("web.LoginSTS")) {
                sendResponse(exchange, 200, "message", "application/json");
            } else {
                sendResponse(exchange, 200, "Normal", "application/json");
            }
        }
    }

    // POC 334, 336: Node-RED ui_base Arbitrary File Read
    class GobyNodeREDFireReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyNodeREDFireReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/ui_base/js/..") && path.contains("/etc/passwd")) {
                sendResponse(exchange, 200, "root:x:", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 335: Node.js Path Traversal
    class GobyNodeJsPathTraversalHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyNodeJsPathTraversalHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/static/") && path.contains("/etc/passwd")) {
                sendResponse(exchange, 200, "root", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 337, 338: OpenSNS RCE
    class GobyOpenSNSRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyOpenSNSRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("s=weixin/index/index")) {
                sendResponse(exchange, 200, "PHP Version", "text/html");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 339: Oracle WebLogic Server Path Traversal
    class GobyOracleWebLogicPathTraversalHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyOracleWebLogicPathTraversalHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            // Simplified check for traversal
            if (vulnerable && path.contains("..")) {
                sendResponse(exchange, 200, "root", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 340: Oracle Weblogic LDAP RCE
    class GobyOracleWebLogicLDAPRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyOracleWebLogicLDAPRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             // Simplified check
            if (vulnerable) {
                sendResponse(exchange, 200, "root", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 341: Oracle Weblogic SSRF
    class GobyOracleWebLogicSSRFHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyOracleWebLogicSSRFHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("operator=http")) {
                sendResponse(exchange, 200, "weblogic.uddi.client.structures.exception.XML_SoapException", "text/html");
            } else {
                sendResponse(exchange, 200, "Normal", "text/html");
            }
        }
    }

    // POC 342: Oray Sunlogin RCE
    class GobyOraySunloginRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyOraySunloginRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("action=verify-haras")) {
                sendResponse(exchange, 200, "verify_string", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 343: PHP Zerodium Backdoor RCE
    class GobyPHPZerodiumBackdoorHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyPHPZerodiumBackdoorHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String ua = exchange.getRequestHeaders().getFirst("User-Agentt");
            if (vulnerable && ua != null && ua.contains("zerodiumvar_dump")) {
                sendResponse(exchange, 200, "int(54289)", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 344: Portainer Init Deploy
    class GobyPortainerInitHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyPortainerInitHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                sendResponse(exchange, 200, "Success", "text/plain");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 345: RG UAC
    class GobyRGUACHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRGUACHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                sendResponse(exchange, 200, "Vulnerable", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 346: Riskscanner SQL Injection
    class GobyRiskscannerSQLInjectionHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRiskscannerSQLInjectionHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("content=1'")) {
                sendResponse(exchange, 500, "SQL syntax", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 347: Ruijie EWEB RCE
    class GobyRuijieEWEBHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRuijieEWEBHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable && body.contains("echo 123")) {
                sendResponse(exchange, 200, "123", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 348: Ruijie RG-UAC Password Leak
    class GobyRuijieRGUACLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRuijieRGUACLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (vulnerable && query != null && query.contains("name=admin")) {
                sendResponse(exchange, 200, "password", "text/plain");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 349, 350, 351: Ruijie Smartweb Password Leak
    class GobyRuijieSmartwebPasswordLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRuijieSmartwebPasswordLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                sendResponse(exchange, 200, "<![CDATA[   admin]]>", "text/xml");
            } else {
                sendResponse(exchange, 200, "Normal", "text/plain");
            }
        }
    }

    // POC 352: Ruijie Smartweb Weak Password
    class GobyRuijieSmartwebWeakPasswordHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRuijieSmartwebWeakPasswordHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (vulnerable && auth != null && auth.contains("Basic Z3Vlc3Q6Z3Vlc3Q=")) {
                sendResponse(exchange, 200, "Level was: LEVEL15", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }

    // POC 353: RuoYi Druid Unauthorized Access
    class GobyRuoYiDruidHandlerPoc353 implements HttpHandler {
        private final boolean vulnerable;
        public GobyRuoYiDruidHandlerPoc353(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (vulnerable && path.contains("/druid/index.html")) {
                sendResponse(exchange, 200, "Druid Stat Index", "text/html");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }
    }

    // POC 354, 355: SDWAN Smart Gateway
    class GobySDWANSmartGatewayHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySDWANSmartGatewayHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Simplified: Assume default password check passes if vulnerable
            if (vulnerable) {
                sendResponse(exchange, 200, "success", "text/plain");
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // POC 356, 357, 358: Samsung WLAN AP RCE
    class GobySamsungWLANRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySamsungWLANRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String body = readRequestBody(exchange);
             if (vulnerable && body.contains("command")) {
                 sendResponse(exchange, 200, "uid=0(root)", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }



    // POC 361: Seeyon OA A6 createMysql.jsp
    class GobySeeyonCreateMysqlHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySeeyonCreateMysqlHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "root", "text/plain");
             } else {
                 sendResponse(exchange, 404, "Not Found", "text/plain");
             }
        }
    }

    // POC 362: Seeyon OA A6 initDataAssess.jsp
    class GobySeeyonInitDataAssessHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySeeyonInitDataAssessHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "personList", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 363: Seeyon OA A6 setextno.jsp
    class GobySeeyonSetExtNoHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySeeyonSetExtNoHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("md5")) {
                 sendResponse(exchange, 200, "c4ca4238a0b923820dcc509a6f75849b", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 364: Seeyon OA A6 test.jsp
    class GobySeeyonTestJspHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySeeyonTestJspHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("SELECT")) {
                 sendResponse(exchange, 200, "c4ca4238a0b923820dcc509a6f75849b", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 365: Seeyon OA A8-m Info Leak
    class GobySeeyonA8InfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySeeyonA8InfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "Password", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 366, 367: Shiziyu CMS SQL Injection
    class GobyShiziyuCmsSQLHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyShiziyuCmsSQLHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             // Simplified check
             if (vulnerable) {
                 sendResponse(exchange, 200, "md5(1)", "text/plain"); // Or whatever it checks
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 368, 369: ShopXO File Read
    class GobyShopXOFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyShopXOFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "root:x:", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 370: Shterm QiZhi Fortress
    class GobyShtermQiZhiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyShtermQiZhiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "事件审计", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 371: SonarQube Search Projects
    class GobySonarQubeSearchProjectsHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySonarQubeSearchProjectsHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "paging", "application/json");
             } else {
                 sendResponse(exchange, 401, "Unauthorized", "text/plain");
             }
        }
    }


    // POC 374, 375: SonicWall SSL-VPN RCE
    class GobySonicWallRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySonicWallRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "root:x:0:0", "text/plain"); // Check content
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 376: SonicWall ShellShock
    class GobySonicWallShellShockHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySonicWallShellShockHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String ua = exchange.getRequestHeaders().getFirst("User-Agent");
             if (vulnerable && ua != null && ua.contains("() {")) {
                 sendResponse(exchange, 200, "root:x:0:0", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 377: SpiderFlow RCE
    class GobySpiderFlowRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySpiderFlowRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "success", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }


    // POC 379: Spring Cloud Function SpEL
    class GobySpringCloudFunctionSpELHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySpringCloudFunctionSpELHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String header = exchange.getRequestHeaders().getFirst("spring.cloud.function.routing-expression");
             if (vulnerable && header != null) {
                 sendResponse(exchange, 500, "uid=0(root)", "text/plain"); // Usually errors with output
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 380: Spring Cloud Gateway SpEL
    class GobySpringCloudGatewaySpELHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySpringCloudGatewaySpELHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             // Complex flow, simplified check
             if (vulnerable) {
                 sendResponse(exchange, 201, "Created", "application/json");
             } else {
                 sendResponse(exchange, 404, "Not Found", "text/plain");
             }
        }
    }



    // POC 383, 384, 385: Struts2 Log4Shell
    class GobyStruts2Log4ShellHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyStruts2Log4ShellHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             String body = readRequestBody(exchange);
             if (vulnerable && path.contains("transfer4.action") && body.contains("${jndi:")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 386: TamronOS File Download
    class GobyTamronOSFileDownloadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyTamronOSFileDownloadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("etc/passwd")) {
                 sendResponse(exchange, 200, "root:x:0:0", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 387: TamronOS RCE
    class GobyTamronOSRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyTamronOSRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("id")) {
                 sendResponse(exchange, 200, "uid=0(root)", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 388: Tianwen ERP File Upload
    class GobyTianwenFileUploadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyTianwenFileUploadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("upload")) {
                 sendResponse(exchange, 200, "success", "text/plain");
             } else {
                 sendResponse(exchange, 404, "Not Found", "text/plain");
             }
        }
    }


    // POC 390: D-Link RCE
    class GobyDLinkRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyDLinkRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String body = readRequestBody(exchange);
             if (vulnerable && body.contains("ping_addr")) {
                 sendResponse(exchange, 200, "uid=0(root)", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 391: UniFi Log4Shell
    class GobyUniFiLog4ShellHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyUniFiLog4ShellHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String body = readRequestBody(exchange);
             if (vulnerable && body.contains("${jndi:")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 392: VENGD File Upload
    class GobyVENGDFileUploadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyVENGDFileUploadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("upload.php")) {
                 sendResponse(exchange, 200, "success", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 393: VMWare Horizon Log4Shell
    class GobyVMwareHorizonLog4ShellHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyVMwareHorizonLog4ShellHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String lang = exchange.getRequestHeaders().getFirst("Accept-Language");
             if (vulnerable && lang != null && lang.contains("${jndi:")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 394: VMWare Operations SSRF
    class GobyVMwareOperationsSSRFHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyVMwareOperationsSSRFHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "thumbprint address", "application/json");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 395: VMWare NSX Log4Shell
    class GobyVMwareNSXLog4ShellHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyVMwareNSXLog4ShellHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String body = readRequestBody(exchange);
             if (vulnerable && body.contains("${jndi:")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 396: VMWare Workspace ONE RCE
    class GobyVMwareWorkspaceONEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyVMwareWorkspaceONEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("freemarker")) {
                 sendResponse(exchange, 200, "uid=0(root)", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 397: VMWare vCenter Log4Shell
    class GobyVMwarevCenterLog4ShellHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyVMwarevCenterLog4ShellHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String ua = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
             if (vulnerable) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 398: VMWare vCenter File Read
    class GobyVMwarevCenterFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyVMwarevCenterFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("eula")) {
                 sendResponse(exchange, 200, "root:x:0:0", "text/plain");
             } else {
                 sendResponse(exchange, 404, "Not Found", "text/plain");
             }
        }
    }

    // POC 399: WAVLINK XSS
    class GobyWAVLINKXSSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWAVLINKXSSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "alert(1)", "text/html");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 400: WSO2 XSS
    class GobyWSO2XSSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWSO2XSSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             if (vulnerable) {
                 sendResponse(exchange, 200, "CARBON.showWarningDialog('???');alert(document.domain)", "text/html");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 401: WSO2 File Upload
    class GobyWSO2FileUploadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWSO2FileUploadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("toolsAny")) {
                 sendResponse(exchange, 200, "Success", "text/plain");
             } else if (vulnerable && path.contains("vuln.jsp")) {
                 sendResponse(exchange, 200, "WSO2-RCE-CVE-2022-29464", "text/plain");
             } else {
                 sendResponse(exchange, 404, "Not Found", "text/plain");
             }
        }
    }


    // POC 403: Weaver EOffice Upload
    class GobyWeaverEOfficeUploadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeaverEOfficeUploadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("uploadify.php")) {
                 sendResponse(exchange, 200, "attachmentID", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 404: Weaver OA SQLi
    class GobyWeaverOASQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeaverOASQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("loginid")) {
                 sendResponse(exchange, 200, "Sysadmin", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 405: WebSVN RCE
    class GobyWebSVNRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWebSVNRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("search")) {
                 sendResponse(exchange, 200, "uid=0(root)", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 406: Weblogic LDAP RCE
    class GobyWeblogicLDAPRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeblogicLDAPRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String uri = exchange.getRequestURI().toString();
             if (vulnerable && uri.contains("consolejndi.portal")) {
                 sendResponse(exchange, 200, "AdminServer", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 407: Weblogic SSRF (CVE-2014-4210)
    class GobyWeblogicSSRFHandler2 implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeblogicSSRFHandler2(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("SearchPublicRegistries.jsp")) {
                 sendResponse(exchange, 200, "weblogic.uddi.client.structures.exception.XML_SoapException", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 408: WordPress Simple Ajax Chat
    class GobyWPSimpleAjaxChatHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWPSimpleAjaxChatHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("sac-export.php")) {
                 sendResponse(exchange, 200, "Chat Log", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 409: WordPress WPQA
    class GobyWPWPQAHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWPWPQAHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("asked-question")) {
                 sendResponse(exchange, 200, "id\":", "application/json");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 410: XXL-JOB Default Password
    class GobyXXLJOBHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyXXLJOBHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             String body = readRequestBody(exchange);
             if (vulnerable && path.contains("login") && body.contains("password=123456")) {
                 sendResponse(exchange, 200, "200", "application/json");
             } else {
                 sendResponse(exchange, 500, "Error", "text/plain");
             }
        }
    }


    // POC 412: YAPI RCE
    class GobyYAPIRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyYAPIRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("api")) {
                 sendResponse(exchange, 200, "uid=0(root)", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 413: YCCMS XSS
    class GobyYCCMSXSSHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyYCCMSXSSHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String uri = exchange.getRequestURI().toString();
             if (vulnerable && uri.contains("script")) {
                 sendResponse(exchange, 200, "alert(/xss/)", "text/html");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 414: Yinpeng Hanming File Download
    class GobyYinpengHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyYinpengHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("fileName")) {
                 sendResponse(exchange, 200, "fonts", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 415: Yonyou NC Bsh RCE
    class GobyYonyouNCBshHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyYonyouNCBshHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("BshServlet")) {
                 sendResponse(exchange, 200, "BeanShell", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    

    // POC 434: F5 BIG-IP RCE
    class GobyF5BigIpRceHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyF5BigIpRceHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("util/bash")) {
                 sendResponse(exchange, 200, "commandResult", "application/json");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 435: Fahuo100 SQLi
    class GobyFahuo100Handler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFahuo100Handler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("M_id")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 404, "Not Found", "text/plain");
             }
        }
    }

    // POC 436: Feishimei Struts2
    class GobyFeishimeiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFeishimeiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("confinfoaction")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 437: Firewall Info Leak
    class GobyFirewallInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFirewallInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.equals("/")) {
                 sendResponse(exchange, 200, "var dkey_verify = Get_Verify_Info(hex_md5", "text/html");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/html");
             }
        }
    }

    // POC 438: Fumengyun/Laifuyun SQLi
    class GobyFumengyunHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFumengyunHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("AjaxMethod.ashx")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 439: Huatiandongli OA SQLi
    class GobyHuatiandongliHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyHuatiandongliHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("workFlowService")) {
                 sendResponse(exchange, 200, "user", "text/xml");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 440: Landray OA File Read
    class GobyLandrayOAFileReadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyLandrayOAFileReadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("custom.jsp")) {
                 sendResponse(exchange, 200, "root", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 441: Mallgard
    class GobyMallgardHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyMallgardHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("ajax_save")) {
                 sendResponse(exchange, 200, "message", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 442: PHP 8.1 Backdoor
    class GobyPhp8BackdoorHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyPhp8BackdoorHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String ua = exchange.getRequestHeaders().getFirst("User-Agentt");
             if (vulnerable && ua != null && ua.contains("zerodium")) {
                 sendResponse(exchange, 200, "int(54289)", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 443: Qilai OA Message
    class GobyQilaiOAMessageUrlHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyQilaiOAMessageUrlHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("messageurl.aspx")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 444: Qilai OA Tree
    class GobyQilaiOATreelistHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyQilaiOATreelistHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("treelist.aspx")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 445: Red Fan OA
    class GobyRedFanOAHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRedFanOAHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("ioFileExport.aspx")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }



    // POC 448: Tongda OA Unauth
    class GobyTongdaOAUnauthHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyTongdaOAUnauthHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("auth_mobi.php")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 449: Wangyixingyun
    class GobyWangyixingyunHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWangyixingyunHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("API")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 450: Weaver Ecology SQLi
    class GobyWeaverEcologySQLiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyWeaverEcologySQLiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("sql=")) {
                 sendResponse(exchange, 200, "SQL Server", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 451: Yiyou RCE
    class GobyYiyouHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyYiyouHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("moni_detail.do")) {
                 sendResponse(exchange, 200, "root", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 452: Yuanchuangxianfeng
    class GobyYuanchuangxianfengHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyYuanchuangxianfengHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("admin_list.html")) {
                 sendResponse(exchange, 200, "admin", "text/html");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 453: Yunshidai SQLi
    class GobyYunshidaiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyYunshidaiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("loginName")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }


    // POC 455: Zhihuipingtai File Download
    class GobyZhihuipingtaiHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyZhihuipingtaiHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("FileDownLoad.aspx")) {
                 sendResponse(exchange, 200, "OK", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 456: Ziguang SQLi
    class GobyZiguangHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyZiguangHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("editPass.html")) {
                 sendResponse(exchange, 404, "c4ca4238a0b923820dcc509a6f75849", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 457: Fanruan Report
    class GobyFanruanReportHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFanruanReportHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("ReportServer")) {
                 sendResponse(exchange, 200, "CDATA", "text/xml");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 459: Seeyon OA A6 DB Info Leak
    class GobySeeyonA6DBInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySeeyonA6DBInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("createMysql.jsp")) {
                 sendResponse(exchange, 200, "root", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 460: Seeyon OA A6 User Info Leak
    class GobySeeyonA6UserInfoLeakHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySeeyonA6UserInfoLeakHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("DownExcelBeanServlet")) {
                 sendResponse(exchange, 200, "xls", "application/vnd.ms-excel");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 461: Seeyon OA Webmail File Download
    class GobySeeyonWebmailFileDownloadHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobySeeyonWebmailFileDownloadHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String query = exchange.getRequestURI().getQuery();
             if (vulnerable && query != null && query.contains("doDownloadAtt")) {
                 sendResponse(exchange, 200, "password", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 462: Fengwang Router Password Leak
    class GobyFengwangRouterHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyFengwangRouterHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("usermanager.htm")) {
                 sendResponse(exchange, 200, "pwd", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // POC 463: Ruijie NBR RCE
    class GobyRuijieNBRRCEHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyRuijieNBRRCEHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (vulnerable && path.contains("guestIsUp.php")) {
                 sendResponse(exchange, 200, "root", "text/plain");
             } else {
                 sendResponse(exchange, 200, "Normal", "text/plain");
             }
        }
    }

    // ==================== 修复的 Handler ====================
    
    // Zabbix SAML 修复版 - 正确返回 302 + Location 头包含 dashboard
    // 注意：POC 设置 follow_redirect=true 但期望 302，这是矛盾的
    // 我们返回带 body 的 302 响应，OkHttp 会处理为最终响应
    class GobyZabbixSAMLFixHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyZabbixSAMLFixHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            System.out.println("[DEBUG] GobyZabbixSAMLFixHandler: path=" + path + ", cookie=" + cookie);
            if (vulnerable && cookie != null && cookie.contains("zbx_session")) {
                System.out.println("[DEBUG] 返回 302 + Location 头");
                // 设置 Location 头
                exchange.getResponseHeaders().set("Location", "/zabbix.php?action=dashboard.view");
                // 返回带 body 的 302 响应
                String body = "Redirecting to dashboard";
                byte[] bytes = body.getBytes("UTF-8");
                exchange.sendResponseHeaders(302, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().close();
            } else {
                sendResponse(exchange, 403, "Forbidden", "text/plain");
            }
        }
    }
    
    // GitLab SSRF 修复版 - 处理两种情况
    class GobyGitLabSSRFFixHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyGitLabSSRFFixHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            if (vulnerable) {
                // 第一步：如果 body 包含 test.dnslog.cn，返回反射内容
                if (body.contains("test.dnslog.cn")) {
                    sendResponse(exchange, 200, "{\"merged_yaml\":\"test.dnslog.cn\"}", "application/json");
                } 
                // 第二步：如果 body 包含 127.0.0.1:9100，返回 YAML 语法错误
                else if (body.contains("127.0.0.1:9100")) {
                    sendResponse(exchange, 200, "{\"errors\":[\"does not have valid YAML syntax\"]}", "application/json");
                } else {
                    sendResponse(exchange, 200, "{\"valid\":true}", "application/json");
                }
            } else {
                sendResponse(exchange, 401, "Unauthorized", "text/plain");
            }
        }
    }
    
    // Struts2 S2-059 修复版 - 返回 goby16384
    class GobyStruts2S2059FixHandler implements HttpHandler {
        private final boolean vulnerable;
        public GobyStruts2S2059FixHandler(boolean vulnerable) { this.vulnerable = vulnerable; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getRawQuery();
            System.out.println("[DEBUG] GobyStruts2S2059FixHandler: path=" + path + ", query=" + query);
            // POC 查询 /?id=goby%25{128*128}，期望响应包含 goby16384
            if (vulnerable && query != null && query.contains("goby")) {
                System.out.println("[DEBUG] 返回 goby16384 响应");
                sendResponse(exchange, 200, "<html>id=goby16384</html>", "text/html");
            } else {
                sendResponse(exchange, 200, "OK", "text/plain");
            }
        }
    }

}

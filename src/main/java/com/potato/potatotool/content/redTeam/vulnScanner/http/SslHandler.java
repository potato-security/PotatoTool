package com.potato.potatotool.content.redTeam.vulnScanner.http;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SSL/TLS 处理器
 * 用于获取 SSL/TLS 证书信息和配置
 * 支持 Nuclei SSL 协议的所有功能
 * 
 * @author Potato
 * @date 2025-11-02
 */
public class SslHandler {
    
    /**
     * SSL/TLS 检测结果
     */
    public static class SslResponse {
        private String address;                 // 目标地址
        private boolean connected;              // 是否连接成功
        private long duration;                  // 检测耗时（毫秒）
        
        // 证书信息
        private String subjectDN;               // 证书主题
        private String issuerDN;                // 证书颁发者
        private String notBefore;               // 证书生效日期
        private String notAfter;                // 证书过期日期
        private List<String> subjectAltNames;   // 证书备用名称（SAN）
        private String serialNumber;            // 证书序列号
        private String signatureAlgorithm;      // 签名算法
        private String publicKeyAlgorithm;      // 公钥算法
        private int keySize;                    // 密钥长度
        
        // SSL/TLS 配置
        private String protocol;                // 协议版本（TLS 1.2, TLS 1.3等）
        private String cipherSuite;             // 加密套件
        private List<String> supportedProtocols; // 支持的协议列表
        private List<String> supportedCipherSuites; // 支持的加密套件列表
        
        // 证书链
        private int chainLength;                // 证书链长度
        private List<String> chainInfo;         // 证书链信息
        
        // 证书验证
        private boolean certificateValid;       // 证书是否有效
        private boolean expired;                // 证书是否过期
        private boolean selfSigned;             // 是否自签名证书
        
        private String raw;                     // 原始响应（证书完整信息）
        private boolean success;                // 操作是否成功
        private String error;                   // 错误信息
        
        public SslResponse() {
            this.subjectAltNames = new ArrayList<>();
            this.supportedProtocols = new ArrayList<>();
            this.supportedCipherSuites = new ArrayList<>();
            this.chainInfo = new ArrayList<>();
        }
        
        // Getters and Setters
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public String getSubjectDN() { return subjectDN; }
        public void setSubjectDN(String subjectDN) { this.subjectDN = subjectDN; }
        
        public String getIssuerDN() { return issuerDN; }
        public void setIssuerDN(String issuerDN) { this.issuerDN = issuerDN; }
        
        public String getNotBefore() { return notBefore; }
        public void setNotBefore(String notBefore) { this.notBefore = notBefore; }
        
        public String getNotAfter() { return notAfter; }
        public void setNotAfter(String notAfter) { this.notAfter = notAfter; }
        
        public List<String> getSubjectAltNames() { return subjectAltNames; }
        public void setSubjectAltNames(List<String> subjectAltNames) { this.subjectAltNames = subjectAltNames; }
        
        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
        
        public String getSignatureAlgorithm() { return signatureAlgorithm; }
        public void setSignatureAlgorithm(String signatureAlgorithm) { this.signatureAlgorithm = signatureAlgorithm; }
        
        public String getPublicKeyAlgorithm() { return publicKeyAlgorithm; }
        public void setPublicKeyAlgorithm(String publicKeyAlgorithm) { this.publicKeyAlgorithm = publicKeyAlgorithm; }
        
        public int getKeySize() { return keySize; }
        public void setKeySize(int keySize) { this.keySize = keySize; }
        
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        
        public String getCipherSuite() { return cipherSuite; }
        public void setCipherSuite(String cipherSuite) { this.cipherSuite = cipherSuite; }
        
        public List<String> getSupportedProtocols() { return supportedProtocols; }
        public void setSupportedProtocols(List<String> supportedProtocols) { this.supportedProtocols = supportedProtocols; }
        
        public List<String> getSupportedCipherSuites() { return supportedCipherSuites; }
        public void setSupportedCipherSuites(List<String> supportedCipherSuites) { this.supportedCipherSuites = supportedCipherSuites; }
        
        public int getChainLength() { return chainLength; }
        public void setChainLength(int chainLength) { this.chainLength = chainLength; }
        
        public List<String> getChainInfo() { return chainInfo; }
        public void setChainInfo(List<String> chainInfo) { this.chainInfo = chainInfo; }
        
        public boolean isCertificateValid() { return certificateValid; }
        public void setCertificateValid(boolean certificateValid) { this.certificateValid = certificateValid; }
        
        public boolean isExpired() { return expired; }
        public void setExpired(boolean expired) { this.expired = expired; }
        
        public boolean isSelfSigned() { return selfSigned; }
        public void setSelfSigned(boolean selfSigned) { this.selfSigned = selfSigned; }
        
        public String getRaw() { return raw; }
        public void setRaw(String raw) { this.raw = raw; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        @Override
        public String toString() {
            return "SslResponse{" +
                    "address='" + address + '\'' +
                    ", connected=" + connected +
                    ", protocol='" + protocol + '\'' +
                    ", cipherSuite='" + cipherSuite + '\'' +
                    ", subjectDN='" + subjectDN + '\'' +
                    ", issuerDN='" + issuerDN + '\'' +
                    ", expired=" + expired +
                    ", selfSigned=" + selfSigned +
                    ", success=" + success +
                    '}';
        }
    }
    
    /**
     * 执行 SSL/TLS 检测
     * 
     * @param address 目标地址（如 "example.com:443"）
     * @return SSL 检测响应
     */
    public static SslResponse check(String address) {
        return check(address, 10000);
    }
    
    /**
     * 执行 SSL/TLS 检测（完整参数）
     * 
     * @param address 目标地址
     * @param timeout 超时时间（毫秒）
     * @return SSL 检测响应
     */
    public static SslResponse check(String address, int timeout) {
        SslResponse response = new SslResponse();
        response.setAddress(address);
        
        long startTime = System.currentTimeMillis();
        SSLSocket socket = null;
        
        try {
            // 解析主机和端口
            String[] parts = address.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 443;
            
            // 创建信任所有证书的 TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            
            // 创建 SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            // 创建 SSLSocketFactory
            SSLSocketFactory factory = sslContext.getSocketFactory();
            
            // 连接到服务器
            System.out.println("→ 正在连接 SSL/TLS 服务器: " + address);
            socket = (SSLSocket) factory.createSocket(host, port);
            socket.setSoTimeout(timeout);
            
            // 开始握手
            socket.startHandshake();
            response.setConnected(true);
            
            // 获取 SSL 会话信息
            SSLSession session = socket.getSession();
            response.setProtocol(session.getProtocol());
            response.setCipherSuite(session.getCipherSuite());
            
            // 获取证书链
            Certificate[] certificates = session.getPeerCertificates();
            response.setChainLength(certificates.length);
            
            if (certificates.length > 0 && certificates[0] instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) certificates[0];
                
                // 解析证书信息
                parseCertificateInfo(cert, response);
                
                // 解析证书链
                parseCertificateChain(certificates, response);
                
                // 检查证书有效性
                checkCertificateValidity(cert, response);
            }
            
            // 获取支持的协议和加密套件
            response.setSupportedProtocols(Arrays.asList(socket.getSupportedProtocols()));
            response.setSupportedCipherSuites(Arrays.asList(socket.getSupportedCipherSuites()));
            
            // 生成原始响应
            StringBuilder raw = new StringBuilder();
            raw.append("Protocol: ").append(response.getProtocol()).append("\n");
            raw.append("CipherSuite: ").append(response.getCipherSuite()).append("\n");
            raw.append("Subject: ").append(response.getSubjectDN()).append("\n");
            raw.append("Issuer: ").append(response.getIssuerDN()).append("\n");
            raw.append("NotBefore: ").append(response.getNotBefore()).append("\n");
            raw.append("NotAfter: ").append(response.getNotAfter()).append("\n");
            raw.append("SerialNumber: ").append(response.getSerialNumber()).append("\n");
            response.setRaw(raw.toString());
            
            response.setSuccess(true);
            System.out.println("✓ SSL/TLS 检测成功");
            
        } catch (IOException e) {
            response.setSuccess(false);
            response.setError("连接失败: " + e.getMessage());
            System.err.println("SSL/TLS 检测失败 - 连接错误: " + address + " - " + e.getMessage());
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("SSL/TLS 检测异常: " + e.getMessage());
            System.err.println("SSL/TLS 检测失败: " + address + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            
            response.setDuration(System.currentTimeMillis() - startTime);
        }
        
        return response;
    }
    
    /**
     * 解析证书信息
     */
    private static void parseCertificateInfo(X509Certificate cert, SslResponse response) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            // 基本信息
            response.setSubjectDN(cert.getSubjectX500Principal().getName());
            response.setIssuerDN(cert.getIssuerX500Principal().getName());
            response.setNotBefore(dateFormat.format(cert.getNotBefore()));
            response.setNotAfter(dateFormat.format(cert.getNotAfter()));
            response.setSerialNumber(cert.getSerialNumber().toString(16).toUpperCase());
            response.setSignatureAlgorithm(cert.getSigAlgName());
            response.setPublicKeyAlgorithm(cert.getPublicKey().getAlgorithm());
            
            // 密钥长度
            if (cert.getPublicKey().getAlgorithm().equals("RSA")) {
                response.setKeySize(cert.getPublicKey().getEncoded().length * 8);
            }
            
            // Subject Alternative Names (SAN)
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans != null) {
                List<String> sanList = new ArrayList<>();
                for (List<?> san : sans) {
                    if (san.size() >= 2) {
                        sanList.add(san.get(1).toString());
                    }
                }
                response.setSubjectAltNames(sanList);
            }
            
        } catch (Exception e) {
            System.err.println("解析证书信息时出错: " + e.getMessage());
        }
    }
    
    /**
     * 解析证书链
     */
    private static void parseCertificateChain(Certificate[] certificates, SslResponse response) {
        try {
            List<String> chainInfo = new ArrayList<>();
            for (Certificate cert : certificates) {
                if (cert instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) cert;
                    String info = String.format("Subject: %s | Issuer: %s",
                        x509.getSubjectX500Principal().getName(),
                        x509.getIssuerX500Principal().getName());
                    chainInfo.add(info);
                }
            }
            response.setChainInfo(chainInfo);
        } catch (Exception e) {
            System.err.println("解析证书链时出错: " + e.getMessage());
        }
    }
    
    /**
     * 检查证书有效性
     */
    private static void checkCertificateValidity(X509Certificate cert, SslResponse response) {
        try {
            // 检查证书是否过期
            Date now = new Date();
            boolean expired = now.after(cert.getNotAfter()) || now.before(cert.getNotBefore());
            response.setExpired(expired);
            
            // 检查证书有效期
            try {
                cert.checkValidity();
                response.setCertificateValid(true);
            } catch (Exception e) {
                response.setCertificateValid(false);
            }
            
            // 检查是否自签名
            boolean selfSigned = cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal());
            response.setSelfSigned(selfSigned);
            
        } catch (Exception e) {
            System.err.println("检查证书有效性时出错: " + e.getMessage());
        }
    }
    
    /**
     * 测试方法
     */
    public static void main(String[] args) {
        // 测试常见网站的 SSL/TLS 配置
        String[] testSites = {
            "www.baidu.com:443",
            "www.google.com:443",
            "www.github.com:443"
        };
        
        for (String site : testSites) {
            System.out.println("\n=== 测试 " + site + " ===");
            SslResponse response = check(site);
            
            System.out.println(response);
            if (response.isSuccess()) {
                System.out.println("协议: " + response.getProtocol());
                System.out.println("加密套件: " + response.getCipherSuite());
                System.out.println("证书主题: " + response.getSubjectDN());
                System.out.println("证书颁发者: " + response.getIssuerDN());
                System.out.println("生效日期: " + response.getNotBefore());
                System.out.println("过期日期: " + response.getNotAfter());
                System.out.println("证书是否有效: " + response.isCertificateValid());
                System.out.println("是否过期: " + response.isExpired());
                System.out.println("是否自签名: " + response.isSelfSigned());
                System.out.println("证书链长度: " + response.getChainLength());
            } else {
                System.out.println("检测失败: " + response.getError());
            }
        }
    }
}




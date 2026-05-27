package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.InputType;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.PocCategory;
import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj.FileTargetType;

import java.util.List;

/**
 * POC 分类器
 * 根据 POC 文件路径和协议推断 InputType 和 Category
 * 
 * @author Potato
 * @date 2025/12/11
 */
public class PocClassifier {
    
    /**
     * 根据文件路径推断 POC 分类
     * 主要用于 Nuclei 模板，因为其目录结构有明确的分类
     * 
     * @param filePath POC 文件路径
     * @return 推断的分类
     */
    public static PocCategory inferCategoryFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return PocCategory.UNKNOWN;
        }
        
        // 统一路径分隔符
        String normalizedPath = filePath.replace("\\", "/").toLowerCase();
        
        // ========== HTTP 分类 ==========
        if (normalizedPath.contains("/http/")) {
            if (normalizedPath.contains("/cves/")) {
                return PocCategory.CVES;
            }
            if (normalizedPath.contains("/cnvd/")) {
                return PocCategory.CNVD;
            }
            if (normalizedPath.contains("/vulnerabilities/")) {
                return PocCategory.VULNERABILITIES;
            }
            if (normalizedPath.contains("/default-logins/")) {
                return PocCategory.DEFAULT_LOGINS;
            }
            if (normalizedPath.contains("/iot/")) {
                return PocCategory.IOT;
            }
            if (normalizedPath.contains("/misconfiguration/")) {
                return PocCategory.MISCONFIGURATION;
            }
            if (normalizedPath.contains("/exposures/")) {
                return PocCategory.EXPOSURES;
            }
            if (normalizedPath.contains("/exposed-panels/")) {
                return PocCategory.EXPOSED_PANELS;
            }
            if (normalizedPath.contains("/technologies/")) {
                return PocCategory.TECHNOLOGIES;
            }
            if (normalizedPath.contains("/honeypot/")) {
                return PocCategory.HONEYPOT;
            }
            if (normalizedPath.contains("/osint/")) {
                return PocCategory.OSINT;
            }
            if (normalizedPath.contains("/token-spray/")) {
                return PocCategory.TOKEN_SPRAY;
            }
            if (normalizedPath.contains("/fuzzing/")) {
                return PocCategory.FUZZING;
            }
            if (normalizedPath.contains("/credential-stuffing/")) {
                return PocCategory.CREDENTIAL_STUFFING;
            }
            if (normalizedPath.contains("/takeovers/")) {
                return PocCategory.TAKEOVERS;
            }
            if (normalizedPath.contains("/miscellaneous/")) {
                return PocCategory.MISCELLANEOUS;
            }
            // 默认 HTTP 归类为 VULNERABILITIES
            return PocCategory.VULNERABILITIES;
        }
        
        // ========== Network 分类 ==========
        if (normalizedPath.contains("/network/")) {
            if (normalizedPath.contains("/cves/")) {
                return PocCategory.NETWORK_CVE;
            }
            if (normalizedPath.contains("/misconfig/") || normalizedPath.contains("/misconfiguration/")) {
                return PocCategory.NETWORK_MISCONFIG;
            }
            if (normalizedPath.contains("/default-login/") || normalizedPath.contains("/default-logins/")) {
                return PocCategory.NETWORK_DEFAULT_LOGIN;
            }
            if (normalizedPath.contains("/detection/") || normalizedPath.contains("/enumeration/")) {
                return PocCategory.NETWORK_DETECTION;
            }
            // 默认 Network 归类为 NETWORK_DETECTION
            return PocCategory.NETWORK_DETECTION;
        }
        
        // ========== File 分类 ==========
        if (normalizedPath.contains("/file/")) {
            if (normalizedPath.contains("/keys/")) {
                return PocCategory.FILE_KEYS;
            }
            if (normalizedPath.contains("/malware/")) {
                return PocCategory.FILE_MALWARE;
            }
            if (normalizedPath.contains("/webshell/")) {
                return PocCategory.FILE_WEBSHELL;
            }
            if (normalizedPath.contains("/audit/")) {
                return PocCategory.FILE_AUDIT;
            }
            // 默认 File 归类为 FILE_AUDIT
            return PocCategory.FILE_AUDIT;
        }
        
        // ========== DNS 分类 ==========
        if (normalizedPath.contains("/dns/")) {
            if (normalizedPath.contains("takeover")) {
                return PocCategory.DNS_TAKEOVER;
            }
            return PocCategory.DNS_CONFIG;
        }
        
        // ========== 其他协议 ==========
        if (normalizedPath.contains("/ssl/")) {
            return PocCategory.SSL;
        }
        if (normalizedPath.contains("/headless/")) {
            return PocCategory.HEADLESS;
        }
        if (normalizedPath.contains("/code/")) {
            return PocCategory.CODE;
        }
        if (normalizedPath.contains("/javascript/")) {
            return PocCategory.JAVASCRIPT;
        }
        
        return PocCategory.UNKNOWN;
    }

    /**
     * 根据 POC 的 tags 推断分类
     * 当路径推断失败时使用
     *
     * @param poc POC 对象
     * @return 推断的分类
     */
    public static PocCategory inferCategoryFromTags(PocObj.Poc poc) {
        if (poc == null || poc.getTags() == null || poc.getTags().isEmpty()) {
            return PocCategory.UNKNOWN;
        }

        for (String tag : poc.getTags()) {
            if (tag == null) continue;
            String lowerTag = tag.toLowerCase().trim();

            // OSINT 分类
            if (lowerTag.equals("osint")) {
                return PocCategory.OSINT;
            }

            // Token Spray 分类
            if (lowerTag.equals("token-spray") || lowerTag.equals("tokenspray")) {
                return PocCategory.TOKEN_SPRAY;
            }

            // Credential Stuffing 分类
            if (lowerTag.equals("credential-stuffing") || lowerTag.equals("creds-stuffing")
                    || lowerTag.equals("credentialstuffing")) {
                return PocCategory.CREDENTIAL_STUFFING;
            }

            // Fuzzing 分类
            if (lowerTag.equals("fuzz") || lowerTag.equals("fuzzing")) {
                return PocCategory.FUZZING;
            }

            // 蜜罐检测
            if (lowerTag.equals("honeypot")) {
                return PocCategory.HONEYPOT;
            }

            // CVE 分类（检查 tag 是否以 cve- 开头）
            if (lowerTag.startsWith("cve-")) {
                return PocCategory.CVES;
            }

            // CNVD 分类
            if (lowerTag.startsWith("cnvd-")) {
                return PocCategory.CNVD;
            }

            // IOT 分类
            if (lowerTag.equals("iot")) {
                return PocCategory.IOT;
            }

            // 默认登录/弱口令
            if (lowerTag.equals("default-login") || lowerTag.equals("default-password")
                    || lowerTag.equals("weak-password")) {
                return PocCategory.DEFAULT_LOGINS;
            }

            // 配置错误
            if (lowerTag.equals("misconfig") || lowerTag.equals("misconfiguration")) {
                return PocCategory.MISCONFIGURATION;
            }

            // 接管类
            if (lowerTag.equals("takeover") || lowerTag.equals("subdomain-takeover")) {
                return PocCategory.TAKEOVERS;
            }
        }

        return PocCategory.UNKNOWN;
    }

    /**
     * 根据协议推断输入类型
     * 
     * @param protocol 协议类型（http, tcp, dns, file 等）
     * @param filePath 文件路径（辅助判断）
     * @return 推断的输入类型
     */
    public static InputType inferInputTypeFromProtocol(String protocol, String filePath) {
        if (protocol == null || protocol.isEmpty()) {
            // 尝试从文件路径推断
            return inferInputTypeFromPath(filePath);
        }
        
        String lowerProtocol = protocol.toLowerCase();
        
        switch (lowerProtocol) {
            case "http":
            case "https":
                return InputType.URL;
                
            case "tcp":
            case "udp":
            case "network":
                return InputType.IP_PORT;
                
            case "dns":
                return InputType.DOMAIN;
                
            case "file":
                // 需要根据模板内容进一步判断是目录还是特定文件
                return InputType.LOCAL_PATH;
                
            case "ssl":
            case "tls":
                // SSL 可以用于 HTTPS URL 或 IP:443
                return InputType.URL;
                
            case "headless":
            case "javascript":
                return InputType.URL;
                
            case "code":
                return InputType.LOCAL_PATH;
                
            default:
                return InputType.ANY;
        }
    }
    
    /**
     * 仅根据文件路径推断输入类型
     */
    private static InputType inferInputTypeFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return InputType.URL;
        }
        
        String normalizedPath = filePath.replace("\\", "/").toLowerCase();
        
        if (normalizedPath.contains("/http/") || normalizedPath.contains("/headless/")) {
            return InputType.URL;
        }
        if (normalizedPath.contains("/network/") || normalizedPath.contains("/javascript/")) {
            return InputType.IP_PORT;
        }
        if (normalizedPath.contains("/dns/")) {
            return InputType.DOMAIN;
        }
        if (normalizedPath.contains("/file/") || normalizedPath.contains("/code/")) {
            return InputType.LOCAL_PATH;
        }
        if (normalizedPath.contains("/ssl/")) {
            return InputType.URL;
        }
        
        // 默认为 URL
        return InputType.URL;
    }
    
    /**
     * 推断特殊 Flag 需求
     * 
     * @param filePath 文件路径
     * @param poc POC 对象（会被修改）
     */
    public static void inferSpecialFlags(String filePath, PocObj.Poc poc) {
        if (poc == null) {
            return;
        }

        String normalizedPath = filePath == null ? "" : filePath.replace("\\", "/").toLowerCase();
        boolean headlessByProtocol = "headless".equalsIgnoreCase(poc.getProtocol());
        boolean headlessBySteps = containsHeadlessStep(poc.getVerifySteps()) || containsHeadlessStep(poc.getExploitSteps());
        boolean codeByProtocol = "code".equalsIgnoreCase(poc.getProtocol())
                || "javascript".equalsIgnoreCase(poc.getProtocol());
        boolean codeBySteps = containsCodeStep(poc.getVerifySteps()) || containsCodeStep(poc.getExploitSteps());
        boolean sslByProtocol = "ssl".equalsIgnoreCase(poc.getProtocol());
        boolean sslBySteps = containsSslStep(poc.getVerifySteps()) || containsSslStep(poc.getExploitSteps());

        // Headless flag
        if (normalizedPath.contains("/headless/") || headlessByProtocol || headlessBySteps) {
            poc.setRequiresHeadless(true);
            poc.setInputType(InputType.URL);
            poc.setCategory(PocCategory.HEADLESS);
        }
        
        // Code flag
        if (normalizedPath.contains("/code/") || normalizedPath.contains("/javascript/")
                || codeByProtocol || codeBySteps) {
            poc.setRequiresCode(true);
            if (normalizedPath.contains("/code/") || "code".equalsIgnoreCase(poc.getProtocol())) {
                poc.setInputType(InputType.LOCAL_PATH);
                poc.setCategory(PocCategory.CODE);
            }
        }
        
        // Fuzz flag
        if (normalizedPath.contains("/fuzzing/") || normalizedPath.contains("/fuzz/")) {
            poc.setRequiresFuzz(true);
            poc.setCategory(PocCategory.FUZZING);
        }

        if (normalizedPath.contains("/ssl/") || sslByProtocol || sslBySteps) {
            if (normalizedPath.contains("/ssl/") || poc.getCategory() == PocCategory.UNKNOWN) {
                poc.setCategory(PocCategory.SSL);
            }
        }
    }

    private static boolean containsHeadlessStep(List<PocObj.PocStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return false;
        }

        for (PocObj.PocStep step : steps) {
            if (step instanceof PocObj.HeadlessStep) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCodeStep(List<PocObj.PocStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return false;
        }

        for (PocObj.PocStep step : steps) {
            if (step instanceof PocObj.CodeStep) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSslStep(List<PocObj.PocStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return false;
        }

        for (PocObj.PocStep step : steps) {
            if (step instanceof PocObj.SslStep) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 推断 File 协议的目标类型
     * 
     * @param poc POC 对象
     * @param filePath POC 文件路径
     */
    public static void inferFileTargetType(PocObj.Poc poc, String filePath) {
        if (poc == null || poc.getInputType() != InputType.LOCAL_PATH) {
            return;
        }
        
        // 检查 POC 的步骤中是否有特定文件路径
        if (poc.getVerifySteps() != null) {
            for (PocObj.PocStep step : poc.getVerifySteps()) {
                // 检查是否为 FileStep 类型
                if (step instanceof PocObj.FileStep) {
                    PocObj.FileStep fileStep = (PocObj.FileStep) step;
                    
                    // 如果有 extensions 过滤，通常是目录扫描
                    if (fileStep.getExtensions() != null && !fileStep.getExtensions().isEmpty()) {
                        poc.setFileTargetType(FileTargetType.DIRECTORY);
                        poc.setFileExtensions(fileStep.getExtensions());
                        return;
                    }
                    
                    // 如果 paths 只有一个且看起来像特定文件
                    if (fileStep.getPaths() != null && fileStep.getPaths().size() == 1) {
                        String path = fileStep.getPaths().get(0);
                        if (isSpecificFilePath(path)) {
                            poc.setFileTargetType(FileTargetType.SPECIFIC_FILE);
                            poc.setSpecificFilePath(path);
                            poc.setInputType(InputType.LOCAL_FILE);
                            return;
                        }
                    }
                }
            }
        }
        
        // 默认为目录扫描
        poc.setFileTargetType(FileTargetType.DIRECTORY);
    }
    
    /**
     * 判断路径是否为特定文件（而非目录模式）
     */
    private static boolean isSpecificFilePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // 如果路径包含通配符，则是目录扫描
        if (path.contains("*") || path.contains("?")) {
            return false;
        }
        
        // 常见的特定文件路径
        String[] specificFiles = {
            "/etc/passwd", "/etc/shadow", "/etc/hosts",
            "/.ssh/id_rsa", "/.ssh/id_dsa", "/.ssh/authorized_keys",
            "/proc/self/environ", "/proc/self/cmdline",
            "web.config", "wp-config.php", ".htaccess", ".htpasswd",
            "config.php", "database.yml", "settings.py"
        };
        
        String lowerPath = path.toLowerCase();
        for (String specific : specificFiles) {
            if (lowerPath.endsWith(specific) || lowerPath.equals(specific)) {
                return true;
            }
        }
        
        // 如果路径以文件扩展名结尾且不含目录通配符，可能是特定文件
        if (path.matches(".*\\.[a-zA-Z0-9]{1,5}$") && !path.contains("**")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 完整分类 POC（一站式方法）
     *
     * @param poc POC 对象
     * @param filePath POC 文件路径
     */
    public static void classifyPoc(PocObj.Poc poc, String filePath) {
        if (poc == null) {
            return;
        }

        // 1. 推断分类（优先从路径推断）
        PocCategory category = inferCategoryFromPath(filePath);
        if (category != PocCategory.UNKNOWN) {
            poc.setCategory(category);
        } else {
            // 路径推断失败时，尝试从 tags 推断
            category = inferCategoryFromTags(poc);
            if (category != PocCategory.UNKNOWN) {
                poc.setCategory(category);
            }
        }

        // 2. 推断输入类型
        InputType inputType = inferInputTypeFromProtocol(poc.getProtocol(), filePath);
        poc.setInputType(inputType);
        
        // 3. 推断特殊 Flag
        inferSpecialFlags(filePath, poc);
        
        // 4. 推断 File 目标类型
        if (inputType == InputType.LOCAL_PATH || inputType == InputType.LOCAL_FILE) {
            inferFileTargetType(poc, filePath);
        }
        
        // 5. 填充规范化标签
        TagNormalizer.fillNormalizedTags(poc);
    }
    
    /**
     * 判断 POC 是否为指纹识别类
     */
    public static boolean isFingerprintPoc(PocObj.Poc poc) {
        if (poc == null) {
            return false;
        }
        
        PocCategory category = poc.getCategory();
        return category == PocCategory.TECHNOLOGIES 
            || category == PocCategory.NETWORK_DETECTION
            || category == PocCategory.HONEYPOT;
    }
    
    /**
     * 判断 POC 是否应在默认 URL 扫描中排除
     */
    public static boolean shouldExcludeFromUrlScan(PocObj.Poc poc) {
        if (poc == null) {
            return false;
        }
        
        PocCategory category = poc.getCategory();
        return category == PocCategory.OSINT
            || category == PocCategory.TOKEN_SPRAY
            || category == PocCategory.FUZZING
            || category == PocCategory.CREDENTIAL_STUFFING;
    }
    
    /**
     * 判断 POC 是否为漏洞检测类（而非信息收集类）
     */
    public static boolean isVulnerabilityPoc(PocObj.Poc poc) {
        if (poc == null) {
            return false;
        }
        
        PocCategory category = poc.getCategory();
        return category == PocCategory.CVES
            || category == PocCategory.CNVD
            || category == PocCategory.VULNERABILITIES
            || category == PocCategory.DEFAULT_LOGINS
            || category == PocCategory.IOT
            || category == PocCategory.MISCONFIGURATION
            || category == PocCategory.EXPOSURES
            || category == PocCategory.NETWORK_CVE
            || category == PocCategory.NETWORK_MISCONFIG
            || category == PocCategory.NETWORK_DEFAULT_LOGIN
            || category == PocCategory.DNS_TAKEOVER;
    }
}

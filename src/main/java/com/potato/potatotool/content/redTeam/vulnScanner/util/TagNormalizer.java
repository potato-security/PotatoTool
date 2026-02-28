package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 标签规范化服务
 * 将不同 POC 格式（Nuclei/Goby/Xray）的 product/tags 映射到统一的规范化标签
 * 用于跨格式 POC 的指纹匹配
 * 
 * @author Potato
 * @date 2025/12/11
 */
public class TagNormalizer {
    
    /**
     * 标签别名映射表
     * Key: 规范化标签（小写）
     * Value: 该标签的所有别名集合
     */
    private static final Map<String, Set<String>> TAG_ALIASES = new ConcurrentHashMap<>();
    
    /**
     * 反向索引：别名 -> 规范化标签
     */
    private static final Map<String, String> ALIAS_TO_NORMALIZED = new ConcurrentHashMap<>();
    
    static {
        // ========== CMS 类 ==========
        registerAliases("wordpress", "wordpress", "wp", "WordPress", "Word Press", "wordpress-plugin", "wordpress-theme", "wp-plugin");
        registerAliases("drupal", "drupal", "Drupal");
        registerAliases("joomla", "joomla", "Joomla", "Joomla!");
        registerAliases("typecho", "typecho", "Typecho");
        registerAliases("dedecms", "dedecms", "DedeCMS", "织梦", "dede");
        registerAliases("discuz", "discuz", "Discuz!", "Discuz", "discuz-x");
        registerAliases("phpcms", "phpcms", "PHPCMS", "phpcms-v9");
        registerAliases("ecshop", "ecshop", "ECShop", "ecshop-v2", "ecshop-v3");
        registerAliases("shopify", "shopify", "Shopify");
        registerAliases("magento", "magento", "Magento", "magento2");
        registerAliases("prestashop", "prestashop", "PrestaShop");
        
        // ========== Java 框架类 ==========
        registerAliases("spring", "spring", "spring-boot", "springboot", "Spring Boot", "Spring Framework", "spring-framework", "spring-mvc", "springmvc");
        registerAliases("struts", "struts", "struts2", "Apache Struts", "Struts2", "struts-2");
        registerAliases("shiro", "shiro", "Apache Shiro", "apache-shiro");
        registerAliases("fastjson", "fastjson", "Fastjson", "alibaba-fastjson");
        registerAliases("jackson", "jackson", "Jackson", "fasterxml-jackson");
        registerAliases("log4j", "log4j", "Log4j", "log4j2", "Log4j2", "apache-log4j");
        registerAliases("weblogic", "weblogic", "WebLogic", "Oracle WebLogic", "oracle-weblogic");
        registerAliases("jboss", "jboss", "JBoss", "jboss-eap", "JBoss EAP", "wildfly", "WildFly");
        registerAliases("tomcat", "tomcat", "apache-tomcat", "Apache Tomcat", "Tomcat");
        registerAliases("jetty", "jetty", "Jetty", "eclipse-jetty");
        registerAliases("glassfish", "glassfish", "GlassFish", "oracle-glassfish");
        registerAliases("resin", "resin", "Resin", "caucho-resin");
        
        // ========== PHP 框架类 ==========
        registerAliases("thinkphp", "thinkphp", "ThinkPHP", "think-php", "thinkphp5", "thinkphp6");
        registerAliases("laravel", "laravel", "Laravel");
        registerAliases("yii", "yii", "Yii", "yii2", "Yii2");
        registerAliases("codeigniter", "codeigniter", "CodeIgniter", "ci");
        registerAliases("symfony", "symfony", "Symfony");
        
        // ========== Python 框架类 ==========
        registerAliases("django", "django", "Django");
        registerAliases("flask", "flask", "Flask");
        registerAliases("tornado", "tornado", "Tornado");
        registerAliases("fastapi", "fastapi", "FastAPI");
        
        // ========== Node.js 框架类 ==========
        registerAliases("express", "express", "Express", "expressjs", "express.js");
        registerAliases("nodejs", "nodejs", "node.js", "Node.js", "node");
        registerAliases("nextjs", "nextjs", "Next.js", "next.js");
        registerAliases("nuxtjs", "nuxtjs", "Nuxt.js", "nuxt.js");
        
        // ========== 服务器类 ==========
        registerAliases("nginx", "nginx", "Nginx", "NGINX");
        registerAliases("apache", "apache", "Apache", "apache-httpd", "httpd", "Apache HTTP Server");
        registerAliases("iis", "iis", "IIS", "Microsoft IIS", "microsoft-iis");
        registerAliases("lighttpd", "lighttpd", "Lighttpd");
        registerAliases("caddy", "caddy", "Caddy");
        
        // ========== 数据库类 ==========
        registerAliases("mysql", "mysql", "MySQL", "mariadb", "MariaDB");
        registerAliases("postgresql", "postgresql", "PostgreSQL", "postgres", "pgsql");
        registerAliases("redis", "redis", "Redis");
        registerAliases("mongodb", "mongodb", "MongoDB", "mongo");
        registerAliases("elasticsearch", "elasticsearch", "Elasticsearch", "elastic", "es");
        registerAliases("memcached", "memcached", "Memcached", "memcache");
        registerAliases("oracle", "oracle", "Oracle", "oracle-db", "Oracle Database");
        registerAliases("mssql", "mssql", "MSSQL", "sqlserver", "SQL Server", "Microsoft SQL Server");
        registerAliases("sqlite", "sqlite", "SQLite");
        
        // ========== 中间件类 ==========
        registerAliases("rabbitmq", "rabbitmq", "RabbitMQ");
        registerAliases("kafka", "kafka", "Kafka", "apache-kafka");
        registerAliases("activemq", "activemq", "ActiveMQ", "apache-activemq");
        registerAliases("zookeeper", "zookeeper", "ZooKeeper", "apache-zookeeper");
        registerAliases("nacos", "nacos", "Nacos", "alibaba-nacos");
        registerAliases("consul", "consul", "Consul", "hashicorp-consul");
        registerAliases("etcd", "etcd", "Etcd", "coreos-etcd");
        
        // ========== 网络设备类 ==========
        registerAliases("cisco", "cisco", "Cisco");
        registerAliases("huawei", "huawei", "Huawei", "华为");
        registerAliases("hikvision", "hikvision", "Hikvision", "海康威视", "海康");
        registerAliases("dahua", "dahua", "Dahua", "大华", "大华技术");
        registerAliases("tp-link", "tp-link", "TP-Link", "tplink", "TP-LINK");
        registerAliases("d-link", "d-link", "D-Link", "dlink", "D-LINK");
        registerAliases("netgear", "netgear", "Netgear", "NETGEAR");
        registerAliases("fortinet", "fortinet", "Fortinet", "fortigate", "FortiGate");
        registerAliases("paloalto", "paloalto", "Palo Alto", "palo-alto");
        registerAliases("juniper", "juniper", "Juniper");
        registerAliases("zyxel", "zyxel", "ZyXEL", "Zyxel");
        registerAliases("mikrotik", "mikrotik", "MikroTik", "routeros");
        registerAliases("ruijie", "ruijie", "Ruijie", "锐捷", "锐捷网络");
        
        // ========== 虚拟化/容器类 ==========
        registerAliases("docker", "docker", "Docker");
        registerAliases("kubernetes", "kubernetes", "Kubernetes", "k8s", "K8s");
        registerAliases("vmware", "vmware", "VMware", "vsphere", "vSphere", "vcenter", "vCenter");
        registerAliases("citrix", "citrix", "Citrix");
        registerAliases("openstack", "openstack", "OpenStack");
        
        // ========== 办公/协作类 ==========
        registerAliases("gitlab", "gitlab", "GitLab");
        registerAliases("jenkins", "jenkins", "Jenkins");
        registerAliases("confluence", "confluence", "Confluence", "atlassian-confluence");
        registerAliases("jira", "jira", "Jira", "JIRA", "atlassian-jira");
        registerAliases("nexus", "nexus", "Nexus", "sonatype-nexus");
        registerAliases("sonarqube", "sonarqube", "SonarQube", "sonar");
        registerAliases("harbor", "harbor", "Harbor");
        registerAliases("grafana", "grafana", "Grafana");
        registerAliases("prometheus", "prometheus", "Prometheus");
        registerAliases("kibana", "kibana", "Kibana");
        registerAliases("zabbix", "zabbix", "Zabbix");
        registerAliases("nagios", "nagios", "Nagios");
        
        // ========== 云服务类 ==========
        registerAliases("aws", "aws", "AWS", "Amazon Web Services", "amazon-aws");
        registerAliases("azure", "azure", "Azure", "Microsoft Azure", "microsoft-azure");
        registerAliases("gcp", "gcp", "GCP", "Google Cloud", "google-cloud");
        registerAliases("aliyun", "aliyun", "Aliyun", "阿里云", "alibaba-cloud");
        registerAliases("tencent-cloud", "tencent-cloud", "腾讯云", "tencent", "qcloud");
        
        // ========== 安全产品类 ==========
        registerAliases("weblogic", "weblogic", "WebLogic", "oracle-weblogic");
        registerAliases("f5", "f5", "F5", "f5-big-ip", "big-ip", "BIG-IP");
        registerAliases("checkpoint", "checkpoint", "Check Point", "checkpoint-firewall");
        registerAliases("sangfor", "sangfor", "Sangfor", "深信服");
        registerAliases("nsfocus", "nsfocus", "NSFocus", "绿盟");
        registerAliases("venustech", "venustech", "Venustech", "启明星辰");
        registerAliases("dbappsecurity", "dbappsecurity", "安恒信息", "安恒");
        
        // ========== OA/ERP 类 ==========
        registerAliases("seeyon", "seeyon", "Seeyon", "致远OA", "致远", "致远互联");
        registerAliases("tongda", "tongda", "Tongda", "通达OA", "通达");
        registerAliases("weaver", "weaver", "Weaver", "泛微OA", "泛微", "E-cology", "ecology", "e-cology");
        registerAliases("landray", "landray", "Landray", "蓝凌OA", "蓝凌");
        registerAliases("yonyou", "yonyou", "Yonyou", "用友", "用友NC", "nc", "U8");
        registerAliases("kingdee", "kingdee", "Kingdee", "金蝶", "金蝶云");
        registerAliases("sap", "sap", "SAP");
        registerAliases("oracle-ebs", "oracle-ebs", "Oracle EBS", "oracle-e-business");
        
        // ========== 编程语言 ==========
        registerAliases("php", "php", "PHP");
        registerAliases("java", "java", "Java", "JAVA");
        registerAliases("python", "python", "Python");
        registerAliases("ruby", "ruby", "Ruby");
        registerAliases("go", "go", "Go", "golang", "Golang");
        registerAliases("dotnet", "dotnet", ".NET", "asp.net", "ASP.NET", "aspnet");
        registerAliases("perl", "perl", "Perl");
        
        // ========== 漏洞类型标签 ==========
        registerAliases("rce", "rce", "RCE", "remote-code-execution", "Remote Code Execution");
        registerAliases("sqli", "sqli", "SQLi", "sql-injection", "SQL Injection");
        registerAliases("xss", "xss", "XSS", "cross-site-scripting");
        registerAliases("ssrf", "ssrf", "SSRF", "server-side-request-forgery");
        registerAliases("lfi", "lfi", "LFI", "local-file-inclusion");
        registerAliases("rfi", "rfi", "RFI", "remote-file-inclusion");
        registerAliases("xxe", "xxe", "XXE", "xml-external-entity");
        registerAliases("deserialization", "deserialization", "反序列化", "unsafe-deserialization");
        registerAliases("authentication-bypass", "authentication-bypass", "auth-bypass", "认证绕过");
        registerAliases("arbitrary-file-upload", "arbitrary-file-upload", "file-upload", "任意文件上传");
        registerAliases("arbitrary-file-read", "arbitrary-file-read", "file-read", "任意文件读取");
        registerAliases("default-login", "default-login", "default-password", "默认口令", "弱口令");
    }
    
    /**
     * 注册标签别名
     * @param normalizedTag 规范化标签
     * @param aliases 别名数组
     */
    private static void registerAliases(String normalizedTag, String... aliases) {
        Set<String> aliasSet = new HashSet<>(Arrays.asList(aliases));
        TAG_ALIASES.put(normalizedTag, aliasSet);
        
        // 建立反向索引
        for (String alias : aliases) {
            ALIAS_TO_NORMALIZED.put(alias.toLowerCase(), normalizedTag);
        }
    }
    
    /**
     * 规范化单个标签
     * @param tag 原始标签
     * @return 规范化后的标签
     */
    public static String normalizeTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return "";
        }
        
        // 1. 基础规范化：去空格，转小写
        String normalized = tag.trim().toLowerCase();
        
        // 2. 检查别名映射
        String mapped = ALIAS_TO_NORMALIZED.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        
        // 3. 尝试去除常见前缀后匹配
        String[] prefixes = {"apache-", "oracle-", "microsoft-", "alibaba-", "hashicorp-"};
        for (String prefix : prefixes) {
            if (normalized.startsWith(prefix)) {
                String withoutPrefix = normalized.substring(prefix.length());
                mapped = ALIAS_TO_NORMALIZED.get(withoutPrefix);
                if (mapped != null) {
                    return mapped;
                }
            }
        }
        
        // 4. 替换空格为横线
        normalized = normalized.replaceAll("\\s+", "-");
        
        return normalized;
    }
    
    /**
     * 规范化 POC 的 product 和 tags
     * @param product 产品名称（可为 null）
     * @param tags 标签列表（可为 null）
     * @return 规范化后的标签集合
     */
    public static Set<String> normalize(String product, List<String> tags) {
        Set<String> normalized = new HashSet<>();
        
        // 处理 product
        if (product != null && !product.isEmpty()) {
            String normalizedProduct = normalizeTag(product);
            if (!normalizedProduct.isEmpty()) {
                normalized.add(normalizedProduct);
            }
        }
        
        // 处理 tags
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && !tag.isEmpty()) {
                    String normalizedTag = normalizeTag(tag);
                    if (!normalizedTag.isEmpty()) {
                        normalized.add(normalizedTag);
                    }
                }
            }
        }
        
        return normalized;
    }
    
    /**
     * 检查指纹识别结果是否匹配 POC
     * @param poc POC 对象
     * @param fingerprintTags 指纹识别出的标签集合（已规范化）
     * @return 是否匹配
     */
    public static boolean matchesFingerprint(PocObj.Poc poc, Set<String> fingerprintTags) {
        if (fingerprintTags == null || fingerprintTags.isEmpty()) {
            return false;
        }
        
        // 获取 POC 的规范化标签
        Set<String> pocTags = poc.getNormalizedTags();
        if (pocTags == null || pocTags.isEmpty()) {
            return false;
        }
        
        // 检查是否有交集
        for (String pocTag : pocTags) {
            if (fingerprintTags.contains(pocTag)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取两个标签集合的交集（匹配的标签）
     * @param pocTags POC 的标签集合
     * @param fingerprintTags 指纹标签集合
     * @return 匹配的标签集合
     */
    public static Set<String> getMatchedTags(Set<String> pocTags, Set<String> fingerprintTags) {
        if (pocTags == null || fingerprintTags == null) {
            return Collections.emptySet();
        }
        
        return pocTags.stream()
                .filter(fingerprintTags::contains)
                .collect(Collectors.toSet());
    }
    
    /**
     * 获取所有已注册的规范化标签
     * @return 规范化标签集合
     */
    public static Set<String> getAllNormalizedTags() {
        return new HashSet<>(TAG_ALIASES.keySet());
    }
    
    /**
     * 获取某个规范化标签的所有别名
     * @param normalizedTag 规范化标签
     * @return 别名集合，如果不存在则返回空集合
     */
    public static Set<String> getAliases(String normalizedTag) {
        Set<String> aliases = TAG_ALIASES.get(normalizedTag);
        return aliases != null ? new HashSet<>(aliases) : Collections.emptySet();
    }
    
    /**
     * 动态添加标签别名（运行时扩展）
     * @param normalizedTag 规范化标签
     * @param aliases 新的别名
     */
    public static void addAliases(String normalizedTag, String... aliases) {
        Set<String> existingAliases = TAG_ALIASES.computeIfAbsent(normalizedTag, k -> new HashSet<>());
        for (String alias : aliases) {
            existingAliases.add(alias);
            ALIAS_TO_NORMALIZED.put(alias.toLowerCase(), normalizedTag);
        }
    }
    
    /**
     * 填充 POC 的 normalizedTags 字段
     * @param poc POC 对象
     */
    public static void fillNormalizedTags(PocObj.Poc poc) {
        if (poc == null) {
            return;
        }
        
        Set<String> normalized = normalize(poc.getProduct(), poc.getTags());
        poc.setNormalizedTags(normalized);
    }
}

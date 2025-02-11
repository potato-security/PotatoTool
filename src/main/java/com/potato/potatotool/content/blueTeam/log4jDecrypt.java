package com.potato.potatotool.content.blueTeam;

import com.potato.potatotool.utils.strUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Potato
 * @date 2023/4/10 08:10
 */
public class log4jDecrypt {

    /**
     * log4j解混淆  失败返回null
     * @param code
     * @return
     */
    public static String decrypt(String code){
        String res = null;
        strUtils str = new strUtils();

        String oldCode = code.toLowerCase();
        String tmpCode = str.urlDecode(code).toLowerCase();

        //  排除非log4j加密方式
        if(!str.containsAllChars(tmpCode, "${jndi:/") || !tmpCode.startsWith("${")) {
            return null;
        }

        //  针对获取环境变量-java\env\sys
        Map<String, String> envArgMap = new HashMap<>();
        // 1、java
        envArgMap.put("${java:version}", "{获取java版本}");
        envArgMap.put("${java:runtime}","{获取JVM信息}");
        envArgMap.put("${java:vm}","{获取VM信息}");
        envArgMap.put("${java:os}","{获取系统版本}");
        envArgMap.put("${java:hw}","{获取硬件信息}");
        envArgMap.put("${java:locale}","{获取当前地区}");
        // 2、LinuxWindows兼容的env
        envArgMap.put("${env:os}","{获取系统版本}");
        envArgMap.put("${env:username}","{获取用户名}");
        envArgMap.put("${env:classpath}","{获取类路径}");
        envArgMap.put("${env:java_home}","{获取java路径}");
        envArgMap.put("${env:path}","{获取环境配置路径}");
        // 3、sys
        envArgMap.put("${sys:awt.toolkit}", "{获取AWT工具包}");
        envArgMap.put("${sys:file.encoding}", "{获取文件编码}");
        envArgMap.put("${sys:file.encoding.pkg}", "{获取文件编码包}");
        envArgMap.put("${sys:file.separator}", "{获取文件分隔符}");
        envArgMap.put("${sys:java.awt.graphicsenv}", "{获取Java AWT图形环境}");
        envArgMap.put("${sys:java.awt.printerjob}", "{获取Java打印机作业}");
        envArgMap.put("${sys:java.class.path}", "{获取Java类路径}");
        envArgMap.put("${sys:java.class.version}", "{获取Java类版本}");
        envArgMap.put("${sys:java.endorsed.dirs}", "{获取Java支持的目录}");
        envArgMap.put("${sys:java.ext.dirs}", "{获取Java扩展目录}");
        envArgMap.put("${sys:java.home}", "{获取Java主目录}");
        envArgMap.put("${sys:java.io.tmpdir}", "{获取Java临时目录}");
        envArgMap.put("${sys:java.library.path}", "{获取Java库路径}");
        envArgMap.put("${sys:java.runtime.name}", "{获取Java运行时名称}");
        envArgMap.put("${sys:java.runtime.version}", "{获取Java运行时版本}");
        envArgMap.put("${sys:java.specification.name}", "{获取Java规范名称}");
        envArgMap.put("${sys:java.specification.vendor}", "{获取Java规范供应商}");
        envArgMap.put("${sys:java.specification.version}", "{获取Java规范版本}");
        envArgMap.put("${sys:java.vendor}", "{获取Java供应商}");
        envArgMap.put("${sys:java.vendor.url}", "{获取Java供应商URL}");
        envArgMap.put("${sys:java.vendor.url.bug}", "{获取Java供应商Bug URL}");
        envArgMap.put("${sys:java.version}", "{获取Java版本}");
        envArgMap.put("${sys:java.vm.info}", "{获取Java虚拟机信息}");
        envArgMap.put("${sys:java.vm.name}", "{获取Java虚拟机名称}");
        envArgMap.put("${sys:java.vm.specification.name}", "{获取Java虚拟机规范名称}");
        envArgMap.put("${sys:java.vm.specification.vendor}", "{获取Java虚拟机规范供应商}");
        envArgMap.put("${sys:java.vm.specification.version}", "{获取Java虚拟机规范版本}");
        envArgMap.put("${sys:java.vm.vendor}", "{获取Java虚拟机供应商}");
        envArgMap.put("${sys:java.vm.version}", "{获取Java虚拟机版本}");
        envArgMap.put("${sys:line.separator}", "{获取换行符}");
        envArgMap.put("${sys:os.arch}", "{获取操作系统架构}");
        envArgMap.put("${sys:os.name}", "{获取操作系统名称}");
        envArgMap.put("${sys:os.version}", "{获取系统版本}");
        envArgMap.put("${sys:path.separator}", "{获取路径分隔符}");
        envArgMap.put("${sys:sun.arch.data.model}", "{获取SUN架构数据模型}");
        envArgMap.put("${sys:sun.boot.class.path}", "{获取SUN引导类路径}");
        envArgMap.put("${sys:sun.boot.library.path}", "{获取SUN引导库路径}");
        envArgMap.put("${sys:sun.cpu.endian}", "{获取CPU大小端}");
        envArgMap.put("${sys:sun.cpu.isalist}", "{获取CPU指令集}");
        envArgMap.put("${sys:sun.desktop}", "{获取桌面环境}");
        envArgMap.put("${sys:sun.io.unicode.encoding}", "{获取SUN Unicode编码}");
        envArgMap.put("${sys:sun.java.command}", "{获取Java命令}");
        envArgMap.put("${sys:sun.java.launcher}", "{获取Java启动器}");
        envArgMap.put("${sys:sun.jnu.encoding}", "{获取SUN JNU编码}");
        envArgMap.put("${sys:sun.management.compiler}", "{获取SUN管理编译器}");
        envArgMap.put("${sys:sun.os.patch.level}", "{获取SUN操作系统补丁级别}");
        envArgMap.put("${sys:sun.stderr.encoding}", "{获取SUN标准错误编码}");
        envArgMap.put("${sys:user.country}", "{获取用户国家}");
        envArgMap.put("${sys:user.dir}", "{获取用户工作目录}");
        envArgMap.put("${sys:user.home}", "{获取用户主目录}");
        envArgMap.put("${sys:user.language}", "{获取用户语言}");
        envArgMap.put("${sys:user.name}", "{获取用户名}");
        envArgMap.put("${sys:user.script}", "{获取用户脚本}");
        envArgMap.put("${sys:user.timezone}", "{获取时区}");
        envArgMap.put("${sys:user.variant}", "{获取用户变体}");



        //  针对于lower、upper ${lower:J}
        //  针对于用户变量/环境变量设值 变量名:-值  返回值   ${ddd:-j}
        //  针对于使用date ${data:'值'}
        //  针对获取环境变量-java\env\sys
        Pattern pattern = Pattern.compile("\\$\\{(lower:|upper:|(?:(?!\\$\\{).)*?:-)((?:(?!\\$\\{).)*?)\\}|\\$\\{date:('|\")((?:(?!\\$\\{).)*?)('|\")\\}");
        //  "\\$\\{(lower:|upper:|(?:(?!\\$\\{).)*?:-)((?:(?!\\$\\{).)*?)\\}"

        for(int i=0; i<6 ; i++) {
            Matcher matcher = pattern.matcher(tmpCode);
            // 使用 Matcher 进行匹配和替换
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {

                // 获取第二部分的值
                String value = (matcher.group(2) !=null) ? matcher.group(2) : matcher.group(4);
                // 替换整个匹配的字符串为中间的值
                matcher.appendReplacement(result, value);

            }

            // 将最后一部分也追加到结果中
            matcher.appendTail(result);
            tmpCode = result.toString();

            //  针对获取环境变量-java\env\sys
            tmpCode = str.replaceMapKeys(tmpCode, envArgMap);
        }

        if(!tmpCode.equals(oldCode)) res = tmpCode;

        return res;
    }

}

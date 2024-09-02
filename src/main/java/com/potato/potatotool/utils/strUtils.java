package com.potato.potatotool.utils;

import com.potato.potatotool.controller.PaneWebshellDecode;
import org.apache.commons.lang.StringEscapeUtils;
import org.graalvm.polyglot.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.Constants.getResourceStream;


/**
 * @author Potato
 * @date 2023/4/4 20:21
 */
public class strUtils {

    /**
     * @param input 传入明文
     * @return      传出MD5值
     * @throws NoSuchAlgorithmException
     */
    public static String md5(String input) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        BigInteger no = new BigInteger(1, messageDigest);
        String hashtext = no.toString(16);
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }

        return hashtext;

    }


    /**
     * @param list  传入数组（Set对象特性->元素必不重复）
     * @return      数组换行拼接为字符串
     */
    public static String joinList_r(Set<String> list) {
        return String.join("\n", list);
    }
    public static String joinList(Set<String> list) {
        return String.join("", list);
    }


    /**
     * @return 随机字符串、区分大小写
     */
    public static String generateRandomString(int minLength, int maxLength) {
        if (minLength < 0 || maxLength < minLength) {
            throw new IllegalArgumentException("Invalid length parameters");
        }

        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        int length = random.nextInt(maxLength - minLength + 1) + minLength;// 生成1到10之间的随机长度

        for (int j = 0; j < length; j++) {

            char c;
            if (random.nextBoolean()) {
                c = (char) (random.nextInt(26) + 'a'); // 生成随机小写字母
            } else {
                c = (char) (random.nextInt(26) + 'A'); // 生成随机大写字母
            }

            sb.append(c);

        }

        return sb.toString();

    }

    /**
     * @param input     原字符串
     * @param randowNum u的个数是否随机，但转换统一个数，随机个数默认1-10个
     * @return          进行unicode编码后字符串
     */
    public static String toUnicodeUnify(String input, Boolean... randowNum) {

        String unicodeEncoded = "";

        String uString = "\\";
        int numU = 1;
        if(randowNum.length==1){
            if(randowNum[0]){
                numU = (int) (Math.random() * 10) + 1; // 1-10的随机数
            }
        }
        for (int i = 0; i < numU; i++) {
            uString += "u";// 拼接出\\u，randowNum==True随机出现u个数1-10 False 一个u
        }

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            unicodeEncoded += uString + Integer.toHexString(c | 0x10000).substring(1);
        }
        return unicodeEncoded;

    }

    /**
     * @param input     原字符串
     * @param randowNum 每个u的个数是否都随机，转换后的个数不统一，随机个数默认1-10个
     * @return          进行unicode编码后字符串
     */
    public static String toUnicode(String input, Boolean... randowNum) {

        String unicodeEncoded = "";

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            int numU = 1;
            if(randowNum.length==1){
                if(randowNum[0]){
                    numU = (int) (Math.random() * 10) + 1; // 1-10的随机数
                }
            }

            StringBuilder uString = new StringBuilder("\\");
            for (int j = 0; j < numU; j++) {
                uString.append("u");
            }

            unicodeEncoded += uString + Integer.toHexString(c | 0x10000).substring(1);
        }
        return unicodeEncoded;

    }


    /**
     * @param webShellCode  原始字符串，包含标记位{{index}}
     * @param randomString  替换字符串数据
     * @return              返回原始字符串根据标位对应数据替换后的结果
     */
    public static String replaceFlags(String webShellCode, String[] randomString){

        Pattern pattern = Pattern.compile("\\{\\{\\d+\\}\\}");
        Matcher matcher = pattern.matcher(webShellCode);

        while (matcher.find()) {

            String match = matcher.group();
            int num = Integer.parseInt(match.substring(2, match.length() - 2));
            String replacement = randomString[num];
            webShellCode = webShellCode.replace(match, replacement);

        }

        return webShellCode;

    }


    /**
     * @param size 随机字符串的个数
     * @return     返回size个随机字符串数组   如：size=3 result={"sd","qwed","dfxcsd"}
     */
    public static String[] createRandomStringList(int size){

        String[] randomString = new String[size];

        for (int i = 0; i < randomString.length; i++) {// 随机生成随机长度的字符串存入randomString

            String newString = strUtils.generateRandomString(1,10);
            if (!Arrays.asList(randomString).contains(newString)) {
                randomString[i] = newString;
            } else {
                i--;
            }

        }

        return  randomString;

    }


    /**
     * @param bytes 传入的byte[]
     * @return      返回字符串类型，默认utf-8编码
     */
    public static String byteToStr(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }


    /**
     *  两参数都使用byte[]进行System.arraycopy，会导致内存的不必要浪费，降低性能
     * @param outputStream ByteArrayOutputStream类型 可以避免频繁的数组复制操作
     * @param data         添加的byte[]
     */
    public static void addBytes(ByteArrayOutputStream outputStream, byte[] data) {

        try {
            outputStream.write(data);
        } catch (IOException e) {
            // 处理写入异常
            if(debugMode)e.printStackTrace();
        }

    }

    /**
     * 获取当前JAR文件所在目录
     * @return JAR文件所在目录的绝对路径
     */
    public static String getCurrentJarDir() {
        try {
            // 获取当前JAR文件的位置
            File jarFile = new File(strUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // 获取JAR文件所在目录的绝对路径
            return jarFile.getParentFile().getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 相对路径转绝对路径
     * @return 【对于JAR文件的】相对路径转绝对路径
     */
    public static String filePathtoAbsolute(String filePath) {

        Path path = Paths.get(filePath);
        if (!path.isAbsolute()){
            String jarDir = getCurrentJarDir();
            if (jarDir != null) {
                Path absolutePath = Paths.get(jarDir, filePath).normalize();
                filePath = absolutePath.toAbsolutePath().toString();
            } else {
                throw new RuntimeException("Failed to determine JAR directory.");
            }
        }

        return filePath;
    }


    /**
     * @param code      写入文件的内容 String
     * @param fileName  写入文件名称
     */
    public static void createFile(String code, String fileName){

        fileName = filePathtoAbsolute(fileName);

        File file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try {

            FileWriter myWriter = new FileWriter(fileName);
            myWriter.write(code);
            myWriter.close();

            System.out.println("[√] 文件保存成功 - " + fileName);

        } catch (IOException e) {

            System.out.println("[×] 文件保存失败 - " + fileName);
            if(debugMode)e.printStackTrace();

        }

    }


    /**
     * @param code      写入文件的内容 byte[]
     * @param fileName  写入文件名称
     */
    public static void createFile(byte[] code, String fileName){

        if(code==null) System.out.println("[×] 文件保存失败 - code为空");

        fileName = filePathtoAbsolute(fileName);

        File file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try {

            FileOutputStream outputStream = new FileOutputStream(fileName);
            outputStream.write(code);
            outputStream.close();

            System.out.println("[√] 文件保存成功 - " + fileName);

        } catch (IOException e) {

            System.out.println("[×] 文件保存失败 - " + fileName);
            if(debugMode)e.printStackTrace();

        }

    }


    /**
     * @param originalFilePath  原始文路径
     * @param newFilePath       新文件路径
     */
    public static void moveFile(String originalFilePath, String newFilePath){

        // 创建原始文件对象
        originalFilePath = filePathtoAbsolute(originalFilePath);
        File originalFile = new File(originalFilePath);

        newFilePath = filePathtoAbsolute(newFilePath);
        // 创建新文件对象
        File newFile = new File(newFilePath);

        if (!newFile.getParentFile().exists()) {
            newFile.getParentFile().mkdirs();
        }

        try {
            // 使用Files类的move方法来替换文件路径
            Files.move(originalFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[√] 文件保存成功 - " + newFilePath);

        } catch (IOException e) {
            System.out.println("[×] 文件保存失败 - " + newFilePath);
            if(debugMode)e.printStackTrace();
        }

    }


    /**
     *  读取文件内容
     * @param filePath  文件路径
     * @return          返回文件内容byte[]
     */
    public static byte[] readFile(String filePath){
        try {

            // 使用Files类的readAllBytes方法读取文件内容并存储为byte[]数组
            byte[] fileBytes = Files.readAllBytes(new File(filePath).toPath());
            return fileBytes;

        } catch (IOException e) {
            if(debugMode)e.printStackTrace();
        }

        return null;
    }




        /**
         * @param bytes byte[]数据
         * @return      byte[]数据转16进制
         */
    public static String byteToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }



    /**
     * @param inHex 待转换的Hex字符串
     * @return  转换后的byte
     */
    public static byte hexToByte(String inHex) {

        return (byte) Integer.parseInt(inHex, 16);

    }


    /**
     * @param inHex 待转换的Hex字符串
     * @return  转换后的byte数组结果
     */
    public static byte[] hexToByteArray(String inHex){
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1){
            //奇数
            hexlen++;
            result = new byte[(hexlen/2)];
            inHex="0"+inHex;
        }else {
            //偶数
            result = new byte[(hexlen/2)];
        }
        int j=0;
        for (int i = 0; i < hexlen; i+=2){
            result[j]=hexToByte(inHex.substring(i,i+2));
            j++;
        }
        return result;
    }


    /**
     *  判断byte[]A是否包含byte[]B
     * @param array     byte[]A
     * @param subArray  byte[]B
     * @return          boolean
     */
    public static boolean byteContains(byte[] array, byte[] subArray) {
        for (int i = 0; i <= array.length - subArray.length; i++) {
            if (Arrays.equals(Arrays.copyOfRange(array, i, i + subArray.length), subArray)) {
                return true;
            }
        }
        return false;
    }


    /**
     *  剔除byte[]数据中的某个byte数据
     * @param inputBytes    原始byte[]数组
     * @param byteData      需要剔除的byte
     * @return
     */
    public static byte[] bytesRemoveByte(byte[] inputBytes,byte byteData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (byte b : inputBytes) {
            if (b != byteData) {
                outputStream.write(b);
            }
        }

        return outputStream.toByteArray();
    }
    public static byte[] bytesRemoveByte(byte[] inputBytes,String strData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bytesData = strData.getBytes(StandardCharsets.UTF_8);
        byte byteData = bytesData[0];
        if(bytesData.length!=1){ throw new IllegalArgumentException("strData为多个byte");}

        for (byte b : inputBytes) {
            if (b != byteData) {
                outputStream.write(b);
            }
        }

        return outputStream.toByteArray();
    }
    public static byte[] bytesRemoveByte(byte[] inputBytes,String[] strDataList) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (byte b : inputBytes) {
            boolean exit = false;
            for (String strData : strDataList){

                byte[] bytesData = strData.getBytes(StandardCharsets.UTF_8);
                byte byteData = bytesData[0];
                if(bytesData.length!=1){ throw new IllegalArgumentException("strData为多个byte");}

                if (b == byteData) {
                    exit = true;
                }
            }
            if(!exit) outputStream.write(b);
        }

        return outputStream.toByteArray();
    }


    /**
     * @param str   原始字符串
     * @param spaceLen   空格填充后结果长度规定
     * @param type  字符串居中/居左/居右 chose[center/left/right]
     * @return      返回空格填充过后的字符串
     */
    public static String formatStr(String str, int spaceLen, String type){

        String result = str;

        if (!type.equals("left") && !type.equals("right") && !type.equals("center")) {
            throw new IllegalArgumentException("Invalid formatStr argument: " + type);
        }

        if( str.length() < spaceLen ){

            String space = "";

            for (int i = 0 ; i < (spaceLen - str.length()) ; i++){
                space += " ";
            }

            if (type.equals("left")){

                result = str + space;

            }
            else if (type.equals("right")){

                result = space + str;

            }
            else if (type.equals("center")){

                result =  space.substring(space.length() / 2, space.length()) + str + space.substring(0, space.length() / 2);

            }

        }

        return result;
    }


    /**
     * @return  返回随机UA头
     */
    public static String RandomUserAgent() {

        String[] userAgents = {
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:38.0) Gecko/20100101 Firefox/38.0",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0",
                "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/601.3.9 (KHTML, like Gecko) Version/9.0.2 Safari/601.3.9",
                "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.155 Safari/537.36",
                "Mozilla/5.0 (Windows NT 5.1; rv:40.0) Gecko/20100101 Firefox/40.0",
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)",
                "Mozilla/5.0 (compatible; MSIE 6.0; Windows NT 5.1)",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0",
                "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36",
                "Opera/9.80 (Windows NT 6.2; Win64; x64) Presto/2.12.388 Version/12.17",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:54.0) Gecko/20100101 Firefox/54.0",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 SE 2.X MetaSr 1.0",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 UBrowser/6.1.2107.204 Safari/537.36",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.96 Safari/537.36",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.96 Safari/537.36",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 OPR/45.0.2552.635",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 YaBrowser/17.6.0.1633 Yowser/2.5 Safari/537.36",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 SE 2.X MetaSr 1.0",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 QIHU 360EE",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 BIDUBrowser/9.7 Safari/537.36",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 2345Explorer/9.3.2.17331 Safari/537.36",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 LBBROWSER",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 QIHU 360SE",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 Core/1.53.2372.400 QQBrowser/9.7.13059.400"
        };
        Random random = new Random();

        return userAgents[random.nextInt(userAgents.length)];

    }


    /**
     *      A数组是否头部包含B数组
     * @param array     第一个数组A
     * @param prefix    第二个数组B
     * @return
     */
    public static boolean startsWith(int[] array, int[] prefix) {
        if (prefix.length > array.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }
    public static boolean startsWith(Value element, int[] expected) {
        if (element.hasArrayElements()) {
            int length = (int)element.getArraySize();
            if (expected.length > length) {
                return false;
            }

            for (int i = 0; i < expected.length; i++) {
                int value = element.getArrayElement(i).asInt();
                if (value != expected[i]) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }

    }


    /**
     *       针对input字符串 寻找key替换为value
     * @param input         传入原始字符串
     * @param dictionary    需要替换的key及value
     * @return
     */
    public static String replaceMapKeys(String input, Map<String, String> dictionary) {
        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            input = input.replace(key, value);
        }
        return input;
    }



    /**
     *      A数组的元素字符串，是否包含B字符串 A[x]="1234" B="123"
     * @param array     数组A
     * @param str       B字符串
     * @return
     */
    public boolean listContantsStr(ArrayList<String> array, String str) {
        for (String element : array) {
            if (element.contains(str)) {
                return true;
            }
        }

        return false;
    }


    /**
     *      A字符串中，是否包含B数组中的某一个元素
     * @param str     字符串A
     * @param list   B数组
     * @return
     */
    public static boolean containsAnyWithSet(String str, List<String> list) {
        Set<String> set = new HashSet<>(list);
        for (String item : set) {
            if (str.contains(item)) {
                return true;
            }
        }
        return false;
    }


    /**
     *      A字符串 是否包含B中所有字符
     * @param input         A字符串
     * @param characters    B字符集
     * @return
     */
    public static boolean containsAllChars(String input, String characters) {
        for (char c : characters.toCharArray()) {
            if (input.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }


    /**
     *      A字符串 是否包含B数组中所有元素
     * @param str         A字符串
     * @param elements      B数组
     * @return
     */
    public static boolean containsAllElements(String str, String[] elements) {
        for (String element : elements) {
            if (!str.contains(element)) {
                return false;
            }
        }
        return true;
    }


    /**
     *      Abyte集合 是否只有Bbyte
     * @param a    Abyte集合
     * @param b    Bbyte
     * @return
     */
    public static boolean allElementsAreByte(ArrayList<Byte> a, byte b) {
        for (Byte c : a) {
            if (!c.equals(b)) {
                return false;
            }
        }
        return true;
    }


    /**
     * @param conText   原始字符串
     * @return      URL编码后的字符串
     */
    public static String urlEncode(String conText) {
        try {
            return URLEncoder.encode(conText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // 处理编码异常
            if(debugMode)e.printStackTrace();
            return "";
        }
    }

    /**
     * @param urlText   原始字符串
     * @return      URL解码后的字符串   非url编码返回原内容
     */
    public static String urlDecode(String urlText){
        try {
            return URLDecoder.decode(urlText.replace("+", "%2B"), "UTF-8").replace("%2B", "+");
        } catch (Exception e) {
            return urlText;
        }
    }

    /**
     * @param conText   原始字符串
     * @return      HTML编码后的字符串
     */
    public static String htmlEncode(String conText) {
        String encodePayload = "";
        for (char c : conText.toCharArray()){
            encodePayload += "&#x" + Integer.toHexString(c) + ";";
        }
        return encodePayload;
    }

    /**
     * @param htmlText   原始字符串
     * @return      HTML解码后的字符串
     */
    public static String htmlDecode(String htmlText) {
        return StringEscapeUtils.unescapeHtml(htmlText);
    }

    /**
     * @param conText   原始字符串
     * @return      Base64编码后的字符串
     */
    public static String base64Encode(String conText) {
        return Base64.getEncoder().encodeToString(conText.getBytes(StandardCharsets.UTF_8));
    }
    public static String base64Encode(byte[] conText) {
        return Base64.getEncoder().encodeToString(conText);
    }


    /**
     * 支持变种base64变种解密
     */
    public static String seeyonBase64Decode(String baseText){
        String str1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        String str2 = "gx74KW1roM9qwzPFVOBLSlYaeyncdNbI=JfUCQRHtj2+Z05vshXi3GAEuT/m8Dpk6";  // 新映射字典

        baseText = baseText.trim();
        if(baseText.length() == 2) return null;

        Map<Character, Character> map = new HashMap<>();
        for(int i = 0; i < str2.length(); i++) {
            map.put(str2.charAt(i), str1.charAt(i));
        }

        StringBuilder output = new StringBuilder();
        for(char c : baseText.toCharArray()) {
            Character mapped = map.get(c);
            if(mapped == null) return null;
            output.append(mapped);
        }

        String res = new strUtils().base64Decode(output.toString());

        if(res==null){
            res = seeyonOldBase64Decode(output.toString());
        }

        return res;
    }
    public static String seeyonOldBase64Decode(String baseText){
        String str1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        String str2 = "FxcYg3UZvtEz50Na8G476=mLDI/jVfC9dsoMAiBhJSu2qPKe+QRbXry1TnkWHlOpw";  // 旧映射字典

        Map<Character, Character> map = new HashMap<>();
        for(int i = 0; i < str2.length(); i++) {
            map.put(str2.charAt(i), str1.charAt(i));
        }

        StringBuilder output = new StringBuilder();
        for(char c : baseText.toCharArray()) {
            output.append(map.get(c));
        }

        String res = new strUtils().base64Decode(output.toString().trim());

        return res;
    }

    /**
     * seeyon数据库密码加密
     * 兼容V1.0和V2.4
     */
    public static String seeyonDbDecode(String baseText){
        if(baseText.startsWith("/1.0/")) {

            baseText = baseText.substring("/1.0/".length());
            baseText = new strUtils().base64Decode(baseText);
            char[] encodeStringCharArray = baseText.toCharArray();
            for (int i = 0; i < encodeStringCharArray.length; ++i) {
                encodeStringCharArray[i] = (char) (encodeStringCharArray[i] - '\u0001');
            }
            return new String(encodeStringCharArray);

        }else if(baseText.startsWith("/2.4/")){

            baseText = baseText.substring("/2.4/".length());
            String SM4_KEY = "E6C63180C2806DD1F47B859DE501C15F";
            return SM4Decrypt(baseText, SM4_KEY);

        }
        return null;
    }

    /**
     * 帆软数据库密码加密
     */
    public static String fineReportDecode(String baseText){
        int[] PassWordArray = new int[]{19, 78, 10, 15, 100, 213, 43, 23};
        if (baseText != null && baseText.startsWith("___")) {
            baseText = baseText.substring(3);
            StringBuilder stringBuilder = new StringBuilder();
            int Step = 0;
            for (int i = 0; i <= baseText.length() - 4; i = (int)((byte)(i + 4))) {
                if (Step == PassWordArray.length) {
                    Step = 0;
                }
                String str = baseText.substring(i, i + 4);
                int num = Integer.parseInt(str, 16) ^ PassWordArray[Step];
                stringBuilder.append((char)num);
                Step = (byte)(Step + 1);
            }
            baseText = stringBuilder.toString();
        }
        return baseText;
    }

    public static String SM4Decrypt(String cipher, String key) {
        byte[] in;
        in = Base64.getDecoder().decode(cipher);

        byte[] keyBytes = hexToByteArray(key);
        return SM4Decrypt(in, keyBytes);
    }

    public static String SM4Decrypt(byte[] in, byte[] keyBytes) {

        try {

            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS7Padding", "BC");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "SM4");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decrypted = cipher.doFinal(in);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e){
            if(debugMode)e.printStackTrace();
            return null;
        }
    }


    /**
     * 兼容数据在进行Gzip解压
     * @param baseText   原始字符串
     * @return      Base64解码后的字符串
     * @throws Exception
     */
    public boolean classCode = false;
    public boolean serializeCode = false;
    public boolean gzipCode = false;
    public String base64Decode(String baseText) {
        try {
            baseText = baseText.replace("\n","").replace("\r","").replace("\t","");

            byte[] decodeBytes = Base64.getDecoder().decode(baseText);



            if(strUtils.byteStartsWith(decodeBytes, 0, new byte[]{(byte) 0x1F, (byte) 0x8B})) {

                byte[] tmpGzipRes = GzipUtils.GzipDecompress(decodeBytes);
                if(tmpGzipRes!=null){
                    if(ReadabilityChecker.assessReadability( new String(tmpGzipRes, StandardCharsets.UTF_8), new double[]{1,0} )){

                        if(strUtils.byteArrayContains(tmpGzipRes, new byte[]{0, 0, 0}) != -1 && !strUtils.byteStartsWith(tmpGzipRes, 0, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}) && !strUtils.byteStartsWith(tmpGzipRes, 0, new byte[]{(byte) 0xAC, (byte) 0xED, 0x00, 0x05}) ){  // 针对于哥斯拉key和value空字符需要转换为等号
                            tmpGzipRes = strUtils.byteReplaceZeroToD3(tmpGzipRes);
                        }
                        decodeBytes = tmpGzipRes;
                        gzipCode = true;
                    }
                }
            }

            // 检查是否存在class/反序列化
            byte[] tmpDecryptedTextBytes = DeserializerUtils.classDataCheck(decodeBytes);
            byte[] tmpSerDecryptedTextBytes = null;
            if (tmpDecryptedTextBytes == null) {
                tmpSerDecryptedTextBytes = DeserializerUtils.serializeCheck(decodeBytes);
            } else {
                decodeBytes = tmpDecryptedTextBytes;
                classCode = true;
            }

            if (tmpSerDecryptedTextBytes != null) {
                decodeBytes = tmpSerDecryptedTextBytes;
                serializeCode = true;
            }

            return new String(decodeBytes, "UTF-8");
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
            return null;
        }
    }
    public static byte[] base64Decode(byte[] baseText) {
        try {
            baseText = bytesRemoveByte(baseText, new String[]{"\n", "\r", "\t"});
            byte[] decodeBytes = Base64.getDecoder().decode(baseText);
            return decodeBytes;
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
            return null;
        }
    }


    /**
     * 递归计算多维数组总元素size
     * @param list
     * @return
     */
    public static int totalListSize(List list) {
        int sum = 0;
        for (Object item : list) {
            if (item instanceof List) {
                sum += totalListSize((List) item);
            } else {
                sum++;
            }
        }
        return sum;
    }



    /**
     * 解析base64函数，解密拼接返回最新字符串
     * @param baseText   原始字符串
     * @return      Base64解码后的字符串
     * @throws Exception
     */
    public static String base64FuncDecode(String baseText) {
        try {
            String regex_f = "\\s*[.+]\\s*(?=(base64_decode|Base64\\.getDecoder\\(\\)\\.decode|Base64Decode)\\s*\\(\\s*['\"](.*?)['\"]\\s*\\))";
            Pattern pattern_f = Pattern.compile(regex_f, Pattern.CASE_INSENSITIVE);
            Matcher matcher_f = pattern_f.matcher(baseText);
            baseText = matcher_f.replaceAll("");

            String regex = "\\b(base64_decode|Base64\\.getDecoder\\(\\)\\.decode|Base64Decode)\\s*\\(\\s*['\"](.*?)['\"]\\s*\\)";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(baseText);

            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String encodedString = matcher.group(2);
                String decodedString = new String(Base64.getDecoder().decode(encodedString));
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(decodedString));
            }
            matcher.appendTail(buffer);

            return buffer.toString();
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
            return baseText;
        }
    }

    /**
     * @param conText   原始字符串
     * @return      Hex编码后的字符串
     */
    public static String hexEncode(String conText) {
        String encodePayload = "";
        for (char c : conText.toCharArray()){
            encodePayload += "0x" + Integer.toHexString(c) + ",";
        }
        return encodePayload.substring(0, encodePayload.length()-1);
    }

    /**
     * @param hexText   原始字符串
     * @return      Hex解码后的字符串
     */
    public String hexDecode(String hexText) {

        String oldData = hexText;

        try{
            hexText = hexText.replace(",", "").replace("0x", "");

            byte[] tmpRes = hexToByteArray(hexText);
            // 检查是否存在class/反序列化
            byte[] tmpDecryptedTextBytes = DeserializerUtils.classDataCheck(tmpRes);
            byte[] tmpSerDecryptedTextBytes = null;
            if (tmpDecryptedTextBytes == null) {
                tmpSerDecryptedTextBytes = DeserializerUtils.serializeCheck(tmpRes);
            } else {
                tmpRes = tmpDecryptedTextBytes;
                classCode = true;
            }

            if (tmpSerDecryptedTextBytes != null) {
                tmpRes = tmpSerDecryptedTextBytes;
                serializeCode = true;
            }

            return (new String(tmpRes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
            return oldData;
        }
    }

    /**
     * 返回字节数组中存在的指定的连续子数组下标
     *
     * @param array 原始字节数组
     * @param subArray 要查找的子数组
     * @return 如果存在子数组则返回 下标，否则返回 -1
     */
    public static int byteArrayContains(byte[] array, byte[] subArray){
        int limit = array.length - subArray.length;
        for (int i = 0; i <= limit; i++) {
            boolean found = true;
            for (int j = 0; j < subArray.length; j++) {
                if (array[i + j] != subArray[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }

    /**
     * byte[]后往前匹配(byte[]{0,0,0}) ，然后继续往前再次匹配任意值的2个byte，将匹配到的5个byte元素替换成一个元素(byte)0xD3
     * 该函数会循环判断匹配
     *
     * @param byteArray 原始字节数组
     * @return
     */
    public static byte[] byteReplaceZeroToD3(byte[] byteArray){
        int index;
        while ((index = byteArrayContains(byteArray, new byte[]{0, 0, 0})) > 2) {

            int endIndex = index + 2; // 末尾下标
            int realyStartIndex = index - 2; // 真正需要开始截取掉的下标
            if (realyStartIndex < 0) realyStartIndex = 0; // 防止数组越界
            byteArray[endIndex] = (byte) 0x3D;
            byteArray = byteSubRejectArray(byteArray, realyStartIndex, 4);

        }
        return byteArray;
    }


    /**
     * byte[]array从下标offset开始，是否头部包含prefix
     *
     */
    public static boolean byteStartsWith(byte[] array, int offset, byte[] prefix) {
        if (array.length - offset < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (array[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * byte[]array从下标start开始，获取多少长度
     *
     */
    public static byte[] byteSubArray(byte[] array, int start, int length) {
        byte[] result = new byte[length];
        System.arraycopy(array, start, result, 0, length);
        return result;
    }

    /**
     * byte[]array从下标start开始，剔除多少长度
     *
     */
    public static byte[] byteSubRejectArray(byte[] array, int start, int length) {
        int newLength = array.length - length;
        byte[] result = new byte[newLength];
        System.arraycopy(array, 0, result, 0, start);
        if (start + length < array.length) {
            System.arraycopy(array, start + length, result, start, newLength - start);
        }
        return result;
    }

    /**
     * @param input   原始字符串
     * @return      Rot13加密后的字符串
     */
    public static String ROT13Encode(String input)
    {
        StringBuilder encrypted = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c >= 'a' && c <= 'z') {
                c = (char) (((c - 'a' + 13) % 26) + 'a');
            } else if (c >= 'A' && c <= 'Z') {
                c = (char) (((c - 'A' + 13) % 26) + 'A');
            }

            encrypted.append(c);
        }
        return encrypted.toString();
    }

    /**
     * @param input   原始字符串
     * @return      Rot13解密后的字符串
     */
    public static String ROT13Decode(String input)
    {
        StringBuilder decrypted = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c >= 'a' && c <= 'z') {
                c = (char) (((c - 'a' + 13) % 26) + 'a');
            } else if (c >= 'A' && c <= 'Z') {
                c = (char) (((c - 'A' + 13) % 26) + 'A');
            }

            decrypted.append(c);
        }

        return decrypted.toString();
    }


    /**
     * 检查rot13函数并进行rot13解码  支持多个rot13加号点号等拼接方式，支持大小写
     * @param conText   原始字符串
     * @return      Rot13解码后的字符串
     */
    public static String ROT13FuncDecode(String conText)
    {
        String regex_f = "\\s*[.+]\\s*(?=(rot13_decode|rot13\\.decode|str_rot13|rot13)\\s*\\(\\s*['\"](.*?)['\"]\\s*\\))";
        Pattern pattern_f = Pattern.compile(regex_f, Pattern.CASE_INSENSITIVE);
        Matcher matcher_f = pattern_f.matcher(conText);
        conText = matcher_f.replaceAll("");

        String regex = "\\b(rot13_decode|rot13\\.decode|str_rot13|rot13)\\s*\\(\\s*['\"](.*?)['\"]\\s*\\)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(conText);

        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String encodedString = matcher.group(2);
            String decodedString = new String(Base64.getDecoder().decode(encodedString));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(decodedString));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }


    /**
     * unicode 解码
     * @param  unicodeStr
     * @return String
     */
    public static String decodeUnicode(String unicodeStr) {
        Charset set = Charset.forName("UTF-16");
        Pattern p = Pattern.compile("\\\\u+([0-9a-fA-F]{4})");
        Matcher m = p.matcher( unicodeStr );
        int start = 0 ;
        int start2 = 0 ;
        StringBuffer sb = new StringBuffer();
        while( m.find( start ) ) {
            start2 = m.start() ;
            if( start2 > start ){
                String seg = unicodeStr.substring(start, start2) ;
                sb.append( seg );
            }
            String code = m.group( 1 );
            int i = Integer.valueOf( code , 16 );
            if (i == 0x000a) {
                sb.append('\r');
            }else if (i == 0x0020) {
                sb.append(' ');
            }else {
                byte[] bb = new byte[4];
                bb[0] = (byte) ((i >> 8) & 0xFF);
                bb[1] = (byte) (i & 0xFF);
                ByteBuffer b = ByteBuffer.wrap(bb);
                sb.append(String.valueOf(set.decode(b)).trim());
            }
            start = m.end() ;
        }
        start2 = unicodeStr.length() ;
        if( start2 > start ){
            String seg = unicodeStr.substring(start, start2) ;
            sb.append( seg );
        }
        return sb.toString() ;
    }

    /**
     * str反转 解码
     * @param  conText
     * @return String
     */
    public static String strRev(String conText){
        StringBuilder sb = new StringBuilder(conText);
        return sb.reverse().toString();
    }


    /**
     * 检查strrev函数并进行strrev调用  支持多个strrev加号点号等拼接方式，支持大小写
     * @param  conText
     * @return String
     */
    public static String strFuncRev(String conText){
        String regex_f = "\\s*[.+]\\s*(?=strrev\\s*\\(\\s*['\"](.*?)['\"]\\s*\\))";
        Pattern pattern_f = Pattern.compile(regex_f, Pattern.CASE_INSENSITIVE);
        Matcher matcher_f = pattern_f.matcher(conText);
        conText = matcher_f.replaceAll("");

        String regex = "\\bstrrev\\s*\\(\\s*['\"](.*?)['\"]\\s*\\)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(conText);

        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String encodedString = matcher.group(1);
            String decodedString = strRev(encodedString);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(decodedString));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }


    /**
     * CHR 编码
     * @param  conText
     * @return String
     */
    public static String chrEncode(String conText){
        String encodePayload = "";
        int tmpRes;
        int tmpOrd;
        String tmpStr;
        for(char c : conText.toCharArray()) {
            tmpStr = Character.toString(c);
            tmpOrd = tmpStr.length() > 0 ? (tmpStr.getBytes(StandardCharsets.UTF_8)[0] & 0xff) : 0;
            tmpRes = c < 0x80 ? c : tmpOrd;
            encodePayload += "CHR(" + tmpRes + ").";
        }
        return encodePayload.substring(0, encodePayload.length()-1);
    }

    /**
     * 检查CHR函数并进行CHR解码  支持多个chr加号点号等拼接方式，支持大小写
     * @param  chrText
     * @return String
     */
    public static String chrFuncDecode(String chrText){

        String regex_f = "\\s*[.+]\\s*(?=chr\\s*\\(\\s*(\\d+)\\s*\\))";
        Pattern pattern_f = Pattern.compile(regex_f, Pattern.CASE_INSENSITIVE);
        Matcher matcher_f = pattern_f.matcher(chrText);
        chrText = matcher_f.replaceAll("");

        String regex = "\\bchr\\s*\\(\\s*(\\d+)\\s*\\)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(chrText);

        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String numberStr = matcher.group(1);
            int number = Integer.parseInt(numberStr);
            char decodedChar = (char) number;
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(decodedChar)));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }


    /**
     *  检查是否使用了该加密方式
     * @param oldStr    源字符串
     * @param newStr    解密后新字符串
     * @return
     */
    public static String decodeCheck(String oldStr, String newStr) {

        if( !oldStr.equals(newStr) && newStr!="" && newStr!=null ){
            return newStr;
        }else{
            return "未采用该加密方式";
        }

    }


    /**
     *  异或加/解密
     * @param data   源byte数组
     * @param key    异或的byte数组
     * @return       异或后的byte数组
     */
    public byte[] xorEncode(byte[] data, byte[] key) {
        byte[] encryptedData = new byte[data.length];
        int keyLength = key.length;

        for (int i = 0; i < data.length; i++) {
            byte c = key[(i + 1) % keyLength];
            encryptedData[i] = (byte) (data[i] ^ c);
        }

        return encryptedData;
    }
    // 兼容String传参，单独异或可使用这个
    public String xorEncode(String data, String key) {
        return new String( xorEncode(data.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8 );
    }

    public byte[] xorEncode(byte[] data, String key) {
        byte[] encryptedData = new byte[data.length];

        byte[] keyByte = key.getBytes(StandardCharsets.UTF_8);
        int keyLength = keyByte.length;

        for (int i = 0; i < data.length; i++) {
            byte c = keyByte[(i + 1) % keyLength];
            encryptedData[i] = (byte) (data[i] ^ c);
        }

        String res = new String(encryptedData, StandardCharsets.UTF_8);

        if (ReadabilityChecker.assessReadability(res, new double[]{1, 0})) {
            xorKey = key;
            return encryptedData;
        } else if (res.startsWith("methodName") && res.length() > 15) {
            xorKey = key;

            if(strUtils.byteArrayContains(encryptedData, new byte[]{0, 0, 0}) != -1 && !strUtils.byteStartsWith(encryptedData, 0, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}) && !strUtils.byteStartsWith(encryptedData, 0, new byte[]{(byte) 0xAC, (byte) 0xED, 0x00, 0x05}) ){  // 针对于哥斯拉key和value空字符需要转换为等号
                encryptedData = strUtils.byteReplaceZeroToD3(encryptedData);
            }
            return encryptedData;
        } else if(strUtils.byteStartsWith(encryptedData, 0, new byte[]{(byte) 0x1F, (byte) 0x8B})) {
            byte[] tmpGzipRes = GzipUtils.GzipDecompress(encryptedData);
            if (tmpGzipRes != null) {
                if (ReadabilityChecker.assessReadability(new String(tmpGzipRes, StandardCharsets.UTF_8), new double[]{1, 0})) {

                    if(strUtils.byteArrayContains(tmpGzipRes, new byte[]{0, 0, 0}) != -1 && !strUtils.byteStartsWith(tmpGzipRes, 0, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}) && !strUtils.byteStartsWith(tmpGzipRes, 0, new byte[]{(byte) 0xAC, (byte) 0xED, 0x00, 0x05}) ){  // 针对于哥斯拉key和value空字符需要转换为等号
                        tmpGzipRes = strUtils.byteReplaceZeroToD3(tmpGzipRes);
                    }

                    xorKey = key + "+Gzip";

                    return tmpGzipRes;
                }
            }
        }

        return null;
    }



    /**
     * ！！！！！【兼容联合Gzip解密】！！！！！
     *  异或加/解密           webshell Key作为异或的第二byte[]
     * @param data          源byte数组
     * @param inputKeyStr   可以使用null，注意使用：(String) null
     * @param traverse      [可不传]调用50w字典进行爆破，将忽略inputKeyStr传入值
     * @param customPath    自定义字典路径
     * @return              异或后的byte数组
     */
    public Set<String> keyArray_AES = new HashSet<>();
    public String xorKey = "";
    public String xorEncode(byte[] data, String inputKeyStr, List traverse, String customPath) {
        if(data == null) return null;

        byte[] res = null;
        Set<String> keyArray = new LinkedHashSet<>();

        if(traverse.contains("XOR")){
            if(customPath != null && !customPath.equals("")){
                try{
                    BufferedReader reader = new BufferedReader(new FileReader(customPath));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        keyArray.add(line);
                    }
                    keyArray_AES = keyArray;
                } catch (Exception e) {
                    if(debugMode)e.printStackTrace();
                }
            }else{
                if(keyArray_AES.isEmpty()){ // 优先读取缓存数据
                    try(InputStream desKeyInputStream = getResourceStream("aesKey");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(desKeyInputStream))){

                        String line;
                        while ((line = reader.readLine()) != null) {
                            keyArray.add(line);
                        }
                        keyArray_AES = keyArray;
                    } catch (Exception e) {
                        if(debugMode)e.printStackTrace();
                    }
                }else {
                    keyArray = keyArray_AES;
                }
            }
        } else if( inputKeyStr == null ){
            //  webShell常见默认秘钥
            keyArray.add("e45e329feb5d925b");
            keyArray.add("3c6e0b8a9c15224a");
            keyArray.add("1a1dc91c907325c6");
            keyArray.add("ab645dd196197df7");
            keyArray.add("5f4dcc3b5aa765d6");
            keyArray.add("changeit");
        } else if( inputKeyStr != null ){
            keyArray.add(inputKeyStr);
        }

        ExecutorService executor = ExecutorServiceManager.getInstance().getExecutor();
        List<Future<?>> futures = ExecutorServiceManager.futures;

        for(String key : keyArray){
            Callable<byte[]> task = () -> {
                return xorEncode(data, key);
            };
            futures.add(executor.submit(task));
        }

        for (Future<?> future : futures) {
            try {
                byte[] result = (byte[]) future.get();
                if (result != null && !result.equals("")) {
                    res = result;
                    break;
                }
            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }
        }

        // 停止所有线程
        ExecutorServiceManager.getInstance().forceShutdown();

        return new String(res, StandardCharsets.UTF_8);
    }

    // 获取jar所在目录绝对路径
    public static String getSelfPath() throws Exception {
        String currentPath = strUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);
        currentPath = (new File(currentPath)).getCanonicalPath();
        return currentPath;
    }

    // 获取jar绝对路径
    public static String getSelfJarPath() throws Exception {
        String currentPath = strUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath().toString();
        return currentPath;
    }

    // copy剪贴板
    public static void setClipboardString(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable trans = new StringSelection(text);
        clipboard.setContents(trans, (ClipboardOwner)null);
    }

}

package com.potato.potatotool.utils;


import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2023/4/19 18:23
 */

public class aesUtils {
    private static final int IV_SIZE = 16;
    private static final String CIPHER_ALGORITHM = "AES/%s/%s";
    private static final String AES_MODE_CBC = "CBC";
    private static final String AES_MODE_ECB = "ECB";
    private static final String AES_MODE_GCM = "GCM";
    private static final String AES_MODE_CFB = "CFB";
    private static final String AES_MODE_OFB = "OFB";
    private static final String AES_MODE_CTR = "CTR";
    private static final String PADDING_NO_PADDING = "NoPadding";
    private static final String PADDING_PKCS5_PADDING = "PKCS5Padding";
    private static final String PADDING_PKCS7_PADDING = "PKCS7Padding";
    private static final String PADDING_ZERO_PADDING = "ZeroPadding";
    /**
     * 对输入的明文进行AES加密
     *
     * @param plainText 密文byte数组
     * @param keyBytes  密钥byte数组
     * @param iv        iv向量byte数组
     * @param mode      加密模式（如CBC、ECB、GCM等）
     * @param padding   填充方式（如NoPadding、PKCS7Padding、ZeroPadding等）
     * @return 加密后的密文字符串
     * @throws Exception 加密过程中的异常
     */
    public static byte[] encrypt(byte[] plainText, byte[] keyBytes,byte[] iv, String mode, String padding) throws Exception {
        validateMode(mode);
        validatePadding(padding);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(String.format(CIPHER_ALGORITHM, mode, padding), "BC");
        if (mode.equals(AES_MODE_ECB) || mode.equals(AES_MODE_CTR)) {

            // ECB及CTR不需要传入iv
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        } else if (mode.equals(AES_MODE_GCM)) {

            // GCM解密可自动提取iv，故不需要固定，自动生成
            byte[] ivTmp = generateRandomBytes(IV_SIZE);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(IV_SIZE * 8, ivTmp);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] encrypted = cipher.doFinal(plainText);
            byte[] cipherText = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(ivTmp, 0, cipherText, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, cipherText, IV_SIZE, encrypted.length);
            return cipherText;

        } else if (mode.equals(AES_MODE_CBC) || mode.equals(AES_MODE_CFB) || mode.equals(AES_MODE_OFB)) {

            //  该模式必须传入iv值，且解密iv同步
            if(iv==null||iv.length==0){
                throw new IllegalArgumentException("该模式必须传入iv值");
            }
            IvParameterSpec parameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        }

        return cipher.doFinal(plainText);
    }

    /**
     * 对输入的密文进行AES解密
     *
     * @param cipherText 密文byte数组
     * @param key        密钥byte数组
     * @param iv         iv向量byte数组
     * @param mode       加密模式（如CBC、ECB、GCM等）
     * @param padding    填充方式（如NoPadding、PKCS7Padding、ZeroPadding等）
     * @return 解密后的明文byte数组
     * @throws Exception 解密过程中的异常
     */
    public boolean classCode = false;
    public boolean serializeCode = false;
    public boolean gzipCode = false;
    public byte[] decrypt(byte[] cipherText, byte[] key, byte[] iv, String mode, String padding) throws Exception {
        try {
            validateMode(mode);
            validatePadding(padding);

            if(key==null){
//                System.out.println("无key输入");
                return null;
            }

            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance(String.format(CIPHER_ALGORITHM, mode, padding), "BC");
            if (mode.equals(AES_MODE_ECB) || mode.equals(AES_MODE_CTR)) {

                //  ECB和CTR不需要传输iv
                cipher.init(Cipher.DECRYPT_MODE, secretKey);

            } else if (mode.equals(AES_MODE_GCM)) {

                //  GCM不需要传输iv，可以提取出iv
                byte[] ivBytes = new byte[IV_SIZE];
                System.arraycopy(cipherText, 0, ivBytes, 0, IV_SIZE);
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(IV_SIZE * 8, ivBytes);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
                byte[] encrypted = new byte[cipherText.length - IV_SIZE];
                System.arraycopy(cipherText, IV_SIZE, encrypted, 0, encrypted.length);
                cipherText = encrypted;
            } else if (mode.equals(AES_MODE_CBC) || mode.equals(AES_MODE_CFB) || mode.equals(AES_MODE_OFB)) {

                //  必须手动传入iv
                if (iv == null || iv.length == 0) {
                    throw new IllegalArgumentException("该模式必须传入iv值");
                }
                IvParameterSpec parameterSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            }

            byte[] decryptedTextBytes = cipher.doFinal(cipherText);

            //是否存在Gzip压缩特征
            if (strUtils.byteToHex(decryptedTextBytes).toLowerCase().startsWith("1f8b")) {
                decryptedTextBytes = strUtils.gzipDecompress(decryptedTextBytes);
                String tmpHexData = strUtils.byteToHex(decryptedTextBytes);

                if (tmpHexData.contains("000000") && !tmpHexData.toLowerCase().startsWith("cafebabe") && !tmpHexData.toLowerCase().startsWith("aced0005")) {  // 针对于哥斯拉key和value空字符需要转换为等号
                    tmpHexData = strUtils.strRev(strUtils.strRev(tmpHexData).replaceAll("(00.{8})", "D3"));
                    decryptedTextBytes = strUtils.hexDecode(tmpHexData).getBytes(StandardCharsets.UTF_8);
                }

                gzipCode = true;
            }

            // 检查是否存在class/反序列化
            byte[] tmpDecryptedTextBytes = null;
            tmpDecryptedTextBytes = DeserializerUtils.classDataCheck(decryptedTextBytes);
            if (tmpDecryptedTextBytes == null) {
                tmpDecryptedTextBytes = DeserializerUtils.serializeCheck(decryptedTextBytes);
            } else {
                classCode = true;
            }
            if (tmpDecryptedTextBytes != null) {
                decryptedTextBytes = tmpDecryptedTextBytes;
                serializeCode = true;
            }
            boolean readability = ReadabilityChecker.assessReadability(decryptedTextBytes);
            return (!classCode && !serializeCode && !readability) ? null : decryptedTextBytes;

        }catch (Exception e){
            if(debugMode)e.printStackTrace();
            return null;
        }
    }

    // 验证加密模式是否合法
    private static void validateMode(String mode) {
        if (!mode.equals(AES_MODE_CBC) && !mode.equals(AES_MODE_ECB) && !mode.equals(AES_MODE_GCM)
                && !mode.equals(AES_MODE_CFB) && !mode.equals(AES_MODE_OFB)) {
            throw new IllegalArgumentException("无效的 AES 模式");
        }
    }

    // 验证填充方式是否合法
    private static void validatePadding(String padding) {
        if (!padding.equals(PADDING_NO_PADDING) && !padding.equals(PADDING_PKCS5_PADDING) && !padding.equals(PADDING_PKCS7_PADDING) && !padding.equals(PADDING_ZERO_PADDING)) {
            throw new IllegalArgumentException("无效的 padding 填充方式");
        }
    }


    // 生成随机字节
    public static byte[] generateRandomBytes(int size) {
        byte[] randomBytes = new byte[size];
        new SecureRandom().nextBytes(randomBytes);
        return randomBytes;
    }


    //  AtomicReference<>原子性更新，多线程环境下安全的类型
    public AtomicReference<String> mode_AES = new AtomicReference<>("");
    public AtomicReference<String> padding_AES = new AtomicReference<>("");
    public AtomicReference<String> key_AES = new AtomicReference<>("");
    public AtomicReference<String> iv_AES = new AtomicReference<>("Null");
    public Set<String> keyArray_AES = new HashSet<>();
    //  50w字典爆破调用方法：
    //  aesUtils aes=new aesUtils();
    //  String res = aes.aesWebShellDecode(encodeStr, null,true);
    //  运行时间特别长，导致Java UI（例如使用Swing）,需要将更新ui防止后台线程运行，防止堵塞
    //  若测试，误解率大，可使用ReadabilityChecker.assessReadability()方法判断可读性，抛除
    /**
     *  AES解密尝试【兼容+Gzip】     放了3个常见key，两种常见iv,两个常见mode CBC/ECB，填充方式PKCS5Padding
     * @param conText       原始字符串
     * @param inputKeyStr   指定key的值，NULL时尝试webShell最常见的3个key
     * @param traverse      [可不传]调用50w字典进行爆破，将忽略inputKeyStr传入值
     * @param customPath    自定义字典路径
     * @return              解密后结果--最好返回byte[]数据，而非string，防止后续传输存在问题
     * @throws Exception
     */
    public byte[] aesWebShellDecode(String conText, String inputKeyStr, String inputIv, boolean traverse, String customPath) {

        // 排除非AES加密格式字符串传入
        String aesPattern = "^[A-Za-z0-9+/]+={0,2}$";
        Pattern pattern = Pattern.compile(aesPattern);
        Matcher matcher = pattern.matcher(conText.replace("\n","").replace("\r","").replace("\t",""));

        if (!matcher.matches()) {
            return conText.getBytes(StandardCharsets.UTF_8);
        }

        byte[] res = null;
        Set<String> keyArray = new LinkedHashSet<>();


        if(traverse){
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
            }else {
                if(keyArray_AES.isEmpty()){ // 优先读取缓存数据
                    try{
                        InputStream aesKeyInputStream = getResourceStream("aesKey");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(aesKeyInputStream));
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
            keyArray.add("whir2014");
        } else if( inputKeyStr != null ){
            keyArray.add(inputKeyStr);
        }

        String[] modeArray = {"CBC", "ECB"};    //  webShell常见两种模式

        ExecutorService executor = ForkJoinPool.commonPool();
        List<Future<byte[]>> futures = new ArrayList<>();

        for (String keyStr : keyArray) {
            for (String mode : modeArray) {
                for (int i = 0; i < 2; i++) {
                    if (i == 1 && mode.equals("ECB")) continue;

                    int finalI = i;
                    Callable<byte[]> task = () -> {
                        try {
                            byte[] encryptData = strUtils.base64Decode(conText.getBytes(StandardCharsets.UTF_8));
                            byte[] key = keyStr.getBytes(StandardCharsets.UTF_8);
                            byte[] iv = inputIv!=null ? inputIv.getBytes(StandardCharsets.UTF_8) : finalI == 0 ? new byte[16] : key;

                            aesUtils aes = new aesUtils();
                            byte[] result = aes.decrypt(
                                    encryptData,
                                    key,
                                    iv,
                                    mode,
                                    "PKCS5Padding"
                            );
                            mode_AES.set(mode);
                            padding_AES.set("PKCS5Padding");
                            key_AES.set(keyStr);
                            iv_AES.set(!iv_AES.get().equals("Null") ? ( mode.equals("ECB") ? "Null": new String(iv, StandardCharsets.UTF_8) ) : "Null");
                            classCode = aes.classCode;
                            serializeCode = aes.serializeCode;
//                            System.out.println("~~~~~~");
//                            System.out.println(mode_AES.get());
//                            System.out.println(keyStr);
//                            System.out.println(iv_AES.get());
//                            System.out.println("———————");

                            return result;
                        } catch (Exception e) {
                            if(debugMode)e.printStackTrace();
                            return null;
                        }
                    };

                    futures.add(executor.submit(task));
                }
            }
        }

        for (Future<byte[]> future : futures) {
            try {
                byte[] result = future.get();
                if (result != null && !result.equals("")) {
                    res = result;
                    break;
                }
            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }
        }

        // 停止所有线程
        executor.shutdownNow();

        return res==null ? conText.getBytes(StandardCharsets.UTF_8) : res;
    }



}

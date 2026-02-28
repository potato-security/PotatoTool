package com.potato.potatotool.utils.crypto;


import com.potato.potatotool.utils.core.ExecutorServiceManager;
import com.potato.potatotool.utils.data.DeserializerUtils;
import com.potato.potatotool.utils.data.GzipUtils;
import com.potato.potatotool.utils.data.StrUtils;
import com.potato.potatotool.utils.misc.ReadabilityChecker;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2023/4/19 18:23
 */

public class DESUtils {
    private static final int IV_SIZE = 8;
    private static final String CIPHER_ALGORITHM = "DES/%s/%s";
    public static final String DES_MODE_CBC = "CBC";
    public static final String DES_MODE_ECB = "ECB";
    public static final String DES_MODE_GCM = "GCM";
    public static final String DES_MODE_CFB = "CFB";
    public static final String DES_MODE_OFB = "OFB";
    public static final String DES_MODE_CTR = "CTR";
    public static final String PADDING_NO_PADDING = "NoPadding";
    public static final String PADDING_PKCS5_PADDING = "PKCS5Padding";
    public static final String PADDING_PKCS7_PADDING = "PKCS7Padding";
    public static final String PADDING_ZERO_PADDING = "ZeroBytePadding";
    /**
     * 对输入的明文进行DES加密
     *
     * @param plainText 密文byte数组
     * @param keyBytes  密钥byte数组
     * @param iv        iv向量byte数组
     * @param mode      加密模式（如CBC、ECB、GCM等）
     * @param padding   填充方式（如NoPadding、PKCS7Padding、ZeroBytePadding等）
     * @return 加密后的密文字符串
     * @throws Exception 加密过程中的异常
     */
    public static byte[] encrypt(byte[] plainText, byte[] keyBytes,byte[] iv, String mode, String padding) throws Exception {
        validateMode(mode);
        validatePadding(padding);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");

        Cipher cipher = Cipher.getInstance(String.format(CIPHER_ALGORITHM, mode, padding), "BC");
        if (mode.equals(DES_MODE_ECB) || mode.equals(DES_MODE_CTR)) {

            // ECB及CTR不需要传入iv
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        } else if (mode.equals(DES_MODE_GCM)) {

            // GCM解密可自动提取iv，故不需要固定，自动生成
            byte[] ivTmp = generateRandomBytes(IV_SIZE);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(IV_SIZE * 8, ivTmp);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] encrypted = cipher.doFinal(plainText);
            byte[] cipherText = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(ivTmp, 0, cipherText, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, cipherText, IV_SIZE, encrypted.length);
            return cipherText;

        } else if (mode.equals(DES_MODE_CBC) || mode.equals(DES_MODE_CFB) || mode.equals(DES_MODE_OFB)) {

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
     * 对输入的密文进行DES解密
     *
     * @param cipherText 密文byte数组
     * @param key        密钥byte数组
     * @param iv         iv向量byte数组
     * @param mode       加密模式（如CBC、ECB、GCM等）
     * @param padding    填充方式（如NoPadding、PKCS7Padding、ZeroBytePadding等）
     * @return 解密后的明文byte数组
     * @throws Exception 解密过程中的异常
     */
    public boolean classCode = false;
    public boolean serializeCode = false;
    public boolean gzipCode = false;
    public byte[] decrypt(byte[] cipherText, byte[] tmpKey, byte[] iv, String mode, String padding) throws Exception {
        validateMode(mode);
        validatePadding(padding);

        try {

            byte[] key = new byte[8];
            System.arraycopy(tmpKey, 0, key, 0, 8);


            if(key==null){
//                System.out.println("无key输入");
                return null;
            }

            SecretKeySpec secretKey = new SecretKeySpec(key, "DES");

            Cipher cipher = Cipher.getInstance(String.format(CIPHER_ALGORITHM, mode, padding), "BC");
            if (mode.equals(DES_MODE_ECB) || mode.equals(DES_MODE_CTR)) {

                //  ECB和CTR不需要传输iv
                cipher.init(Cipher.DECRYPT_MODE, secretKey);

            } else if (mode.equals(DES_MODE_GCM)){

                //  GCM不需要传输iv，可以提取出iv
                byte[] ivBytes = new byte[IV_SIZE];
                System.arraycopy(cipherText, 0, ivBytes, 0, IV_SIZE);
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(IV_SIZE * 8, ivBytes);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
                byte[] encrypted = new byte[cipherText.length - IV_SIZE];
                System.arraycopy(cipherText, IV_SIZE, encrypted, 0, encrypted.length);
                cipherText = encrypted;
            } else if (mode.equals(DES_MODE_CBC) || mode.equals(DES_MODE_CFB) || mode.equals(DES_MODE_OFB)) {

                //  必须手动传入iv
                if(iv==null||iv.length==0){
                    throw new IllegalArgumentException("该模式必须传入iv值");
                }
                IvParameterSpec parameterSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            }

            byte[] decryptedTextBytes = cipher.doFinal(cipherText);

            //是否存在Gzip压缩特征
            if(StrUtils.byteStartsWith(decryptedTextBytes, 0, new byte[]{(byte) 0x1F, (byte) 0x8B})) {
                decryptedTextBytes = GzipUtils.GzipDecompress(decryptedTextBytes);

                if(StrUtils.byteArrayContains(decryptedTextBytes, new byte[]{0, 0, 0}) != -1 && !StrUtils.byteStartsWith(decryptedTextBytes, 0, new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}) && !StrUtils.byteStartsWith(decryptedTextBytes, 0, new byte[]{(byte) 0xAC, (byte) 0xED, 0x00, 0x05}) ){  // 针对于哥斯拉key和value空字符需要转换为等号
                    decryptedTextBytes = StrUtils.byteReplaceZeroToD3(decryptedTextBytes);
                }

                gzipCode = true;
            }

            // 检查是否存在class/反序列化
            byte[] tmpDecryptedTextBytes = DeserializerUtils.classDataCheck(decryptedTextBytes);
            byte[] tmpSerDecryptedTextBytes = null;
            if (tmpDecryptedTextBytes == null) {
                tmpSerDecryptedTextBytes = DeserializerUtils.serializeCheck(decryptedTextBytes);
            } else {
                decryptedTextBytes = tmpDecryptedTextBytes;
                classCode = true;
            }

            if (tmpSerDecryptedTextBytes != null) {
                decryptedTextBytes = tmpSerDecryptedTextBytes;
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
        if (!mode.equals(DES_MODE_CBC) && !mode.equals(DES_MODE_ECB) && !mode.equals(DES_MODE_GCM)
                && !mode.equals(DES_MODE_CFB) && !mode.equals(DES_MODE_OFB)) {
            throw new IllegalArgumentException("无效的 DES 模式");
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
    public AtomicReference<String> mode_DES = new AtomicReference<>("");
    public AtomicReference<String> padding_DES = new AtomicReference<>("");
    public AtomicReference<String> key_DES = new AtomicReference<>("");
    public AtomicReference<String> iv_DES = new AtomicReference<>("Null");
    public Set<String> keyArray_DES = new LinkedHashSet<>();
    //  50w字典爆破调用方法：
    //  DESUtils des=new DESUtils();
    //  String res = des.desWebShellDecode(encodeStr, null,true);
    /**
     *  DES解密尝试【兼容+Gzip】     放了3个常见key，两种常见iv,两个常见mode CBC/ECB，填充方式PKCS5Padding
     * @param conText       原始字符串
     * @param inputKeyStr   指定key的值，NULL时尝试webShell最常见的3个key
     * @param traverse      [可不传]调用50w字典进行爆破，将忽略inputKeyStr传入值
     * @param customPath    自定义字典路径
     * @return              解密后结果--最好返回byte[]数据，而非string，防止后续传输存在问题
     * @throws Exception
     */
    public byte[] desWebShellDecode(String conText, String inputKeyStr, String inputIv, List traverse, String customPath) {

        // 排除非DES加密格式字符串传入
        String desPattern = "^[A-Za-z0-9+/]+={0,2}$";
        Pattern pattern = Pattern.compile(desPattern);
        Matcher matcher = pattern.matcher(conText.replace("\n","").replace("\r","").replace("\t",""));

        if (!matcher.matches()) {
            return conText.getBytes(StandardCharsets.UTF_8);
        }

        byte[] res = null;
        Set<String> keyArray = new LinkedHashSet<>();


        if(traverse.contains("DES")){
            if(customPath != null && !customPath.equals("")){
                try{
                    BufferedReader reader = new BufferedReader(new FileReader(customPath));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        keyArray.add(line);
                    }
                    keyArray_DES = keyArray;
                } catch (Exception e) {
                    if(debugMode)e.printStackTrace();
                }
            }else {
                if(keyArray_DES.isEmpty()){ // 优先读取缓存数据
                    try(InputStream desKeyInputStream = getResourceStream("aesKey");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(desKeyInputStream, StandardCharsets.UTF_8))){

                        String line;
                        while ((line = reader.readLine()) != null) {
                            keyArray.add(line);
                        }
                        keyArray_DES = keyArray;
                    } catch (Exception e) {
                        if(debugMode)e.printStackTrace();
                    }
                }else {
                    keyArray = keyArray_DES;
                }
            }
        } else if( inputKeyStr == null ){
            //  webShell常见默认秘钥
            keyArray.add("e45e329feb5d925b");
            keyArray.add("3c6e0b8a9c15224a");
            keyArray.add("1a1dc91c907325c6");
            keyArray.add("ab645dd196197df7");
            keyArray.add("5f4dcc3b5aa765d6");
            keyArray.add("fd690c56512ce362");
            keyArray.add("changeit");
            keyArray.add("whir2014");
            keyArray.add("li_01010");
            keyArray.add("kmssAdminKey");
            keyArray.add("kmssPropertiesKey");
            keyArray.add("ilovethisgame");
            keyArray.add("1234567890123456");
        } else if( inputKeyStr != null ){
            keyArray.add(inputKeyStr);
            // 为符合部分加解密方案，强行指定key为16位
            if(inputKeyStr.length() < 16){
                byte[] keyBytes = new byte[16];
                byte[] originalKeyBytes = inputKeyStr.getBytes();
                System.arraycopy(originalKeyBytes, 0, keyBytes, 0, Math.min(originalKeyBytes.length, keyBytes.length));
                keyArray.add(new String(keyBytes));
            }else if (inputKeyStr.length() > 16){
                keyArray.add(inputKeyStr.substring(0, 16));
            }
        }

        String[] modeArray = {"CBC", "ECB"};    //  webShell常见两种模式
        String[] paddingArray = {PADDING_PKCS5_PADDING, PADDING_ZERO_PADDING}; // 数据加密常见的两种padding

        String poolName = ExecutorServiceManager.ExecutorPoolNames.DES_DECRYPT;
        ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName);
        List<CompletableFuture<byte[]>> futures = new ArrayList<>();

        for (String keyStr : keyArray) {
            for (String mode : modeArray) {
                for (String padding : paddingArray) {
                    for (int i = 0; i < 2; i++) {
                        if (i == 1 && mode.equals("ECB")) continue;

                        int finalI = i;
                        CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> {
                            try {
                                byte[] encryptData = StrUtils.base64Decode(conText.getBytes(StandardCharsets.UTF_8));
                                byte[] key = keyStr.getBytes(StandardCharsets.UTF_8);
                                byte[] iv = inputIv != null ? inputIv.getBytes(StandardCharsets.UTF_8) : finalI == 0 ? new byte[8] : key;

                                DESUtils des = new DESUtils();
                                byte[] result = des.decrypt(
                                        encryptData,
                                        key,
                                        iv,
                                        mode,
                                        padding
                                );
                                if (result != null && !result.equals("")) {
                                    mode_DES.set(mode);
                                    padding_DES.set(padding);
                                    key_DES.set(keyStr);
                                    iv_DES.set(!iv.equals("Null") ? (mode.equals("ECB") ? "Null" : new String(iv, StandardCharsets.UTF_8)) : "Null");
                                    classCode = des.classCode;
                                    serializeCode = des.serializeCode;

                                    // 停止所有线程
                                    ExecutorServiceManager.shutdownExecutor(poolName);
                                }

                                return result;
                            } catch (Exception e) {
                                if (debugMode) e.printStackTrace();
                                return null;
                            }
                        }, executor);

                        futures.add(future);
                    }
                }
            }
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
        ExecutorServiceManager.shutdownExecutor(poolName);

        return res==null ? conText.getBytes(StandardCharsets.UTF_8) : res;
    }

    public String finalshellDecode(String conText){

        try {
            byte[] decodedData = Base64.getDecoder().decode(conText);
            byte[] head = new byte[8];
            System.arraycopy(decodedData, 0, head, 0, head.length);

            byte[] encryptedData = new byte[decodedData.length - head.length];
            System.arraycopy(decodedData, head.length, encryptedData, 0, encryptedData.length);

            byte[] key = generateRandomKey(head);
            byte[] decryptedData = new DESUtils().decrypt(encryptedData, key, null, DES_MODE_ECB, PADDING_PKCS5_PADDING);
            mode_DES.set(DES_MODE_ECB);
            padding_DES.set(PADDING_PKCS5_PADDING);
            key_DES.set("(hex)"+ StrUtils.byteToHex(key));
            iv_DES.set("null");

            String result = new String(decryptedData, StandardCharsets.UTF_8);
            // 判断不存在乱码，防止误报
            boolean isReadable = ReadabilityChecker.assessReadability(result, 1, 0);
            if(isReadable){
                return result;
            }
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }
        return null;
    }

    static byte[] generateRandomKey(byte[] head) {
        long seed = 3680984568597093857L / (long) (new Random((long) head[5])).nextInt(127);
        Random random = new Random(seed);
        int t = head[0];

        for (int i = 0; i < t; ++i) {
            random.nextLong();
        }

        long n = random.nextLong();
        Random random2 = new Random(n);

        long[] keyData = {(long) head[4], random2.nextLong(), (long) head[7], (long) head[3], random2.nextLong(),
                (long) head[1], random.nextLong(), (long) head[2]};

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        for (long l : keyData) {
            try {
                dos.writeLong(l);
            } catch (IOException e) {
                if(debugMode)e.printStackTrace();
            }
        }

        try {
            dos.close();
        } catch (IOException e) {
            if(debugMode)e.printStackTrace();
        }

        byte[] key = bos.toByteArray();
        return md5Hash(key);
    }

    public static byte[] md5Hash(byte[] data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(data, 0, data.length);
            byte[] result = messageDigest.digest();
            return result;
        } catch (NoSuchAlgorithmException e) {
            if(debugMode)e.printStackTrace();
            return null;
        }
    }

    public static void main(String []args) {
        SecurityInitializer.initializeSecurityProvider();
        DESUtils des =new DESUtils();
        String res = des.finalshellDecode("Xg5GPCslNUdSsG1Tn3oR/g+3OAYFnCP3");
        System.out.println(res);
        System.out.println("Finalshell(DES\\"+des.mode_DES.get()+"\\"+des.padding_DES.get()+"<key:iv>"+des.key_DES.get()+":"+des.iv_DES.get());

    }

}

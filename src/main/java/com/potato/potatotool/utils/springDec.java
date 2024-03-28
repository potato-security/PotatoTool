package com.potato.potatotool.utils;


import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Potato
 * @date 2024/3/28 13:13
 */
public class springDec {

    public static String decode(String encryptedMessage, String cryptoPassword){

        if(cryptoPassword==null){
            cryptoPassword = "EbfYkitulv73I2p0mXI50JMXoaxZTKJ7";
        }

        try {
            String patternString = "\\((.*?)\\)";
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(encryptedMessage);
            String content = encryptedMessage;
            if (matcher.find()) {
                content = matcher.group(1);
            }
            StandardPBEStringEncryptor decryptor = new StandardPBEStringEncryptor();
            decryptor.setPassword(cryptoPassword);

            String res = decryptor.decrypt(content);

            return res;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String []args) {
        String res = springDec.decode("vpIBjT3RT8mf6pBjiKJuqA==",null);
        System.out.println(res);

    }

}

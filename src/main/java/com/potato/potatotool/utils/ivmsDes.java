package com.potato.potatotool.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * @author Potato
 * @date 2024/3/28 00:01
 */
public class ivmsDes {

    public static String decrypt(String str) {
        byte[] seed = strUtils.hexToByteArray("CAFEBABE0000003101F3037FFFFFFF048000000008000B08006F08007008007108007B0800850800990800B3010003203E2001000328294901001428294C6A6176612F6C616E672F537472696E673B01000328295601000328295A0100042844294A010004284629490100042849295601000528494929490100062849494929285B424949422956010008285B424949492949010008285B424949492956010007285B425B42295A010005285B432949010005285B432956010006285B43432949010006285B43432956010007285B4349295B43010007285B4349492956010008285B434949295B43010008285B434949432949010008285B43494943295601");

        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
            rnd.setSeed(seed);
            kgen.init(128, rnd);
            SecretKey secretKey = kgen.generateKey();
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(2, new SecretKeySpec(secretKey.getEncoded(), "AES"));
            byte[] result = cipher.doFinal(strUtils.hexToByteArray(str));
            return new String(result, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String []args) {

    }

}

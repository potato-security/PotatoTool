package com.potato.potatotool.utils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Potato
 * @date 2023/6/4 19:12
 */

//  启发式方法和规则 判断解密内容可读性
public class ReadabilityChecker {

    public static double calculatePrintableRatio(String decryptedString) {
        int totalCharacters = decryptedString.length();
        int printableCharacters = 0;

        for (int i = 0; i < totalCharacters; i++) {
            char currentChar = decryptedString.charAt(i);
            // Character.isLetterOrDigit(currentChar) 该函数存在bug:一些可打印的特殊符号会被识别通过，比如Ԅ
            if (isLetterOrDigit(currentChar) || Character.isWhitespace(currentChar) || isCommonSymbol(currentChar)) {
                printableCharacters++;
            }
        }

        return (double) printableCharacters / totalCharacters;
    }

    private static boolean isCommonSymbol(char c) {
        // 自定义常见符号集合
        String commonSymbols = "!@#$%^&*()-_=+[]{}\\|;:'\",.<>/?`~";
        return commonSymbols.indexOf(c) != -1;
    }

    private static boolean isLetterOrDigit(char c) {
        // 支持英文大小写、数字、中文
        return String.valueOf(c).matches("[\\p{Alnum}\\p{IsHan}]");
    }


    public static int detectGibberishPattern(String decryptedString) {
        Pattern gibberishPattern = Pattern.compile("[^\\x00-\\x7F\\u4E00-\\u9FFF]");  // "[^\\x00-\\x7F]"
        Matcher matcher = gibberishPattern.matcher(decryptedString);
        int gibberishCount = 0;

        while (matcher.find()) {
            gibberishCount++;
//            System.out.println("Gibberish 乱码字符如下: " + matcher.group());
        }

        return gibberishCount;
    }

    public static boolean assessReadability(String decryptedString,double... printableRatioSetAndgibberishCountSet) {
        if(decryptedString==null) return false;

        double printableRatio = calculatePrintableRatio(decryptedString);
        int gibberishCount = detectGibberishPattern(decryptedString);

//        System.out.println("原文内容:");
//        System.out.println(decryptedString);
//        System.out.println("可打印率:");
//        System.out.println(printableRatio);
//        System.out.println("乱码字符数量如下:");
//        System.out.println(gibberishCount);

        double printableRatioSet = 0.8;
        int gibberishCountSet = 5;

        if (decryptedString.length() < 20){
            // 短数据时，应提高可打印率
            printableRatioSet = 0.9;
        }

        if(printableRatioSetAndgibberishCountSet.length == 2){
            printableRatioSet = printableRatioSetAndgibberishCountSet[0];
            gibberishCountSet = (int) printableRatioSetAndgibberishCountSet[1];
        }

        // 自定义阈值，根据实际情况进行调整
        if (printableRatio >= printableRatioSet && gibberishCount <= gibberishCountSet) {
            return true;  // 可读性高
        } else {
            return false;  // 可读性低
        }
    }

    public static boolean assessReadability(byte[] decryptedByte,double... printableRatioSetAndgibberishCountSet) {
        return assessReadability( new String(decryptedByte, StandardCharsets.UTF_8), printableRatioSetAndgibberishCountSet);
    }

    public static void main(String[] args) {
        String decryptedString = "Decrypted result";

        boolean isReadable = assessReadability(decryptedString);
        System.out.println("Is readable? " + isReadable);
    }
}
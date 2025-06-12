package com.potato.potatotool.utils.misc;

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
            if (isLetterOrDigit(currentChar) || Character.isWhitespace(currentChar) || isCommonSymbol(currentChar)) { // 字母、数字||空白字符||中英文符号
                printableCharacters++;
            }
        }

        return (double) printableCharacters / totalCharacters;
    }

    private static boolean isCommonSymbol(char c) {
        // 自定义常见英文符号
        String commonSymbols = "!@#$%^&*()-_=+[]{}\\|;:'\",.<>/?`~";
        if (commonSymbols.indexOf(c) != -1) {
            return true;
        }

        // 检查中文标点符号的Unicode范围
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||   // CJK 符号和标点 的 Unicode 块
                block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ||    // 半角和全角形式 的 Unicode 块
                (c >= '\u3000' && c <= '\u303F') ||  // CJK标点符号范围
                (c >= '\uFF00' && c <= '\uFFEF');   // 全角符号范围
    }

    private static boolean isLetterOrDigit(char c) {
        // 支持英文大小写、数字、中文
        return String.valueOf(c).matches("[\\p{Alnum}\\p{IsHan}]");
    }


    public static int detectGibberishPattern(String decryptedString) {
        Pattern gibberishPattern = Pattern.compile(
                "[^\\x00-\\x7F" +       // ASCII
                "\\u4E00-\\u9FFF" +     // 中文字符
                "\\u3000-\\u303F" +     // 中文标点符号
                "\\uFF00-\\uFFEF]"      // 全角符号
        );
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

        if(printableRatioSetAndgibberishCountSet.length == 2 && (!(printableRatioSetAndgibberishCountSet[0]==0.5 && printableRatioSetAndgibberishCountSet[1]==0))){
            printableRatioSet = printableRatioSetAndgibberishCountSet[0];
            gibberishCountSet = (int) printableRatioSetAndgibberishCountSet[1];
        }

        // 自定义阈值，根据实际情况进行调整
        // 当设置可打印率=0.5，乱码字符数量=0，为js的base64加密(字符串utf-16编码，转二进制进行的base64加密，正常解密每个正常字符后面都会有不可见字符)
        if (printableRatio >= printableRatioSet && gibberishCount <= gibberishCountSet) {
            return true;  // 可读性高
        } else {

            if(printableRatioSetAndgibberishCountSet.length == 2 && printableRatioSetAndgibberishCountSet[0]==0.5 && printableRatioSetAndgibberishCountSet[1]==0){
                StringBuilder oddIndexChars = new StringBuilder(); // 存储单数索引字符
                StringBuilder evenIndexChars = new StringBuilder(); // 存储双数索引字符
                for (int i = 0; i < decryptedString.length(); i++) {
                    if ((i % 2) == 0) {
                        evenIndexChars.append(decryptedString.charAt(i));
                    } else {
                        oddIndexChars.append(decryptedString.charAt(i));
                    }
                }

                double even_printableRatio = calculatePrintableRatio(evenIndexChars.toString());
                int even_gibberishCount = detectGibberishPattern(evenIndexChars.toString());
                double odd_printableRatio = calculatePrintableRatio(oddIndexChars.toString());
                int odd_gibberishCount = detectGibberishPattern(oddIndexChars.toString());

                if(even_printableRatio==1 && even_gibberishCount==0 && odd_printableRatio==0 && odd_gibberishCount==0) return true;
            }

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
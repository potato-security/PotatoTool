package com.potato.potatotool.content.blueTeam.webshell.decoder.utils;


import java.nio.charset.StandardCharsets;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * @author Potato
 * @date 2023/4/19 18:23
 */

// 新华三IMC解密
public class ImcDecUtils {


    public static String decode(String et) {
        if (!et.startsWith("-")) {    //判断密文是否以“-”开头
            if(debugMode)System.out.println("Invalid encrypted text: "+et);
            return null;
        }
        String[] strs = et.substring(1).split("-");

        /**
         * 这里把密文以“-”为分隔符分割存放到数组strs[]里，就是只取出每一个“-”之间的整数，即
         * strs[0]=97
         * strs[1]=110
         * strs[2]=94
         */

        byte[] bytes = new byte[strs.length];
        for (int i = 0; i < strs.length; i++)
        {
            try {
                int b = Integer.parseInt(strs[i]) + i * 16 % 256;       //这里是算法的核心代码，数组的下标值(i)乘上16取余256再加上第i个数组的值。第一次循环就是0*16%256+97把结果赋给变量b，此时b=97，第二次b=126，第三次b=126
                if (b > 255) {                                          //整个判断的是如果b>255则把b-256在赋值给b，表达式表示为b=b-256
                    b -= 256;
                }
                bytes[i] = ((byte)b);    //把整形变量b转换成字节然后存放在上面创建的字节数组bytes[]里

            } catch (NumberFormatException nfe) {
                return null;
            }
        }

        String str = new String(bytes, StandardCharsets.UTF_8);    //这里把之前算出来的b，就是存放入bytes[]数组里的数据转换成字符，不知道怎么转换的，参考十进制的ASCII码表，转换之后把结果存放到变量str。举例的结果解密结果为“a~~”
        if (!str.endsWith("~~")) {   //最后检查是否以字符“~~”结尾
            System.out.println("Invalid encrypted text: "+et);
            return null;
        }
        return str.substring(0, str.length() - 2);    //把解密后的结果最后两位去掉，去掉“~~”之后的结果就是明文。举例的结果解密结果为“a”
    }


    public static void main(String []args) {
        String res = ImcDecUtils.decode("-115-97-76-67-49-28-30-14");
        System.out.println(res);

    }

}

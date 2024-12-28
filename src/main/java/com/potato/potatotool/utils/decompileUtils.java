package com.potato.potatotool.utils;

/**
 * @author Potato
 * @date 2023/4/21 17:13
 */


public class decompileUtils {

    /**
     * @param classPath     class文件路径
     * @param outfilePath   [选填]输出文件路径
     * @return
     */
    public static String Decompile(String classPath,String decompileMode, String... outfilePath){

        if(decompileMode.equals("idea")) {
            return ideaDecompileUtils.Decompile(classPath, outfilePath);
        }else if(decompileMode.equals("procyon")){
            return procyonDecompileUtils.Decompile(classPath, outfilePath);
        }else {
            System.out.println("decompileMode选择错误：" + decompileMode);
        }
        return null;
    }


    /**
     * @param classBytes     classBytes数据
     * @param outfilePath   [选填]输出文件路径
     * @return
     */
    public static String Decompile(byte[] classBytes,String decompileMode, String... outfilePath) throws Exception {

        if(decompileMode.equals("idea")) {
            return ideaDecompileUtils.Decompile(classBytes, outfilePath);
        }else if(decompileMode.equals("procyon")){
            return procyonDecompileUtils.Decompile(classBytes, outfilePath);
        }else {
            System.out.println("decompileMode选择错误：" + decompileMode);
        }

        return null;
    }

    public static void main(String[] args) {
        System.out.println(Decompile("/Users/a/Desktop/项目开发/PotatoTool/target/classes/com/potato/potatotool", "procyon"));
    }

}

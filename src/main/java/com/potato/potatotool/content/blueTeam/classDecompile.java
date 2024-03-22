package com.potato.potatotool.content.blueTeam;


import com.google.gson.JsonObject;
import com.potato.potatotool.utils.Constants;
import com.potato.potatotool.utils.strUtils;

import static com.potato.potatotool.utils.Constants.getResourceFileTmpPath;
import static com.potato.potatotool.utils.decompileUtils.Decompile;


public class classDecompile {

    public static void main(String[] args){

        //  初始化默认反编译模式配置
        JsonObject tmpJsonObj = (JsonObject) Constants.getOutsideConfig("Decompile");
        String decompileMode = tmpJsonObj.getAsJsonPrimitive("decompileMode").getAsString();

        String result = Decompile(getResourceFileTmpPath("classTestPath"), decompileMode);//可传入byte[]数据不传文件

        strUtils.createFile(result,"./12313.javaText");

        System.out.println(result);

    }

}

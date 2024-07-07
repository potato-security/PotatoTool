package com.potato.potatotool.content.blueTeam;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.ihxq.projects.pna.Attribution;
import me.ihxq.projects.pna.ISP;
import me.ihxq.projects.pna.PhoneNumberInfo;
import me.ihxq.projects.pna.PhoneNumberLookup;

import java.util.*;

import static com.potato.potatotool.utils.Constants.getResourceStreamByte;

/**
 * @author Potato
 * @date 2024/4/16 15:04
 */
public class phoneToRegionUtil {

    /**
     * 初始化归属地库
     */
    static PhoneNumberLookup phoneNumberLookup = new PhoneNumberLookup();

    public static JsonArray getPhoneInfo(String[] phoneList) {
        JsonArray jsonArray = new JsonArray();


        for (String phone : phoneList) {
            phone = phone.replace(" ","");

            PhoneNumberInfo nullData = new PhoneNumberInfo(phone, new Attribution("", "", "", ""), ISP.UNKNOWN);

            JsonObject temp_phone_search = new JsonObject();

            PhoneNumberInfo found = phoneNumberLookup.lookup(phone).orElse(nullData);
            phone = found.getNumber();
            String Province = found.getAttribution().getProvince();
            String City = found.getAttribution().getCity();
            String Operator = found.getIsp().getCnName();
            String AreaCode = found.getAttribution().getAreaCode();
            String PostalCode = found.getAttribution().getZipCode();

            if(Province.equals("") && City.equals("") && Operator.equals("未知")){
                Operator = "";
            }

            temp_phone_search.addProperty("手机号", phone);
            temp_phone_search.addProperty("省份", Province);
            temp_phone_search.addProperty("城市", City);
            temp_phone_search.addProperty("运营商", Operator);
            temp_phone_search.addProperty("区号", AreaCode);
            temp_phone_search.addProperty("邮编", PostalCode);

            jsonArray.add(temp_phone_search);
        }

        return jsonArray;
    }


    public static void main(String []args) {
        String[] phoneList = {"18666677777", "17391911111", "sds"};
        JsonArray result = getPhoneInfo(phoneList);
        for (JsonElement item : result) {
            System.out.println(item);
        }
    }

}

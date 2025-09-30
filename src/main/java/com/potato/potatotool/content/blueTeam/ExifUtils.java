package com.potato.potatotool.content.blueTeam;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.google.gson.JsonObject;
import com.potato.potatotool.utils.network.CustomHttpResponse;
import com.potato.potatotool.utils.network.RequestObj;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.getConfigInfo;
import static com.potato.potatotool.utils.network.RequestUtils.requests;

/**
 * @author Potato
 * @date 2024/4/17 15:12
 */
public class ExifUtils {

    public static Map<String, String> getExif(String filePath){

        File imageFile = new File(filePath);

        Map<String, String> metadataMap = new HashMap<>();

        Map<String, String> translationMap = new HashMap<>();
        translationMap.put("GPS Version ID", "GPS版本");
        translationMap.put("GPS Longitude", "GPS经度");
        translationMap.put("GPS Latitude", "GPS纬度");
        translationMap.put("Make", "厂商");
        translationMap.put("Model", "机型");
        translationMap.put("Orientation", "方向");
        translationMap.put("Software", "软件");
        translationMap.put("Date/Time", "修改时间");
        translationMap.put("Artist", "作者");
        translationMap.put("YCbCr Positioning", "YcbCr定位");
        translationMap.put("Exposure Time", "曝光时间");
        translationMap.put("F-Number", "光圈");
        translationMap.put("Exposure Program", "曝光程序");
        translationMap.put("ISO Speed Ratings", "ISO感光度");
        translationMap.put("Exif Version", "Exif版本");
        translationMap.put("Date/Time Original", "拍摄时间");
        translationMap.put("Date/Time Digitized", "数字化时间");
        translationMap.put("Components Configuration", "成分构成");
        translationMap.put("Shutter Speed Value", "快门速度");
        translationMap.put("Aperture Value", "光圈值");
        translationMap.put("Exposure Bias Value", "曝光补偿");
        translationMap.put("Max Aperture Value", "最大光圈");
        translationMap.put("Metering Mode", "测光模式");
        translationMap.put("Flash", "闪光");
        translationMap.put("Focal Length", "焦距");
        translationMap.put("User Comment", "用户注释");
        translationMap.put("Sub-Sec Time", "次秒（修改时间）");
        translationMap.put("Sub-Sec Time Original", "次秒（拍摄时间）");
        translationMap.put("Sub-Sec Time Digitized", "次秒（数字化时间）");
        translationMap.put("FlashPix Version", "FlashPix版本");
        translationMap.put("Color Space", "色彩空间");
        translationMap.put("Exif Image Width", "Exif图像宽度");
        translationMap.put("Exif Image Height", "Exif图像高度");
        translationMap.put("Focal Plane X Resolution", "焦平面水平分辨率");
        translationMap.put("Focal Plane Y Resolution", "焦平面垂直分辨率");
        translationMap.put("Focal Plane Resolution Unit", "焦平分辨率单位");
        translationMap.put("Custom Rendered", "自定义补偿");
        translationMap.put("Exposure Mode", "曝光模式");
        translationMap.put("White Balance Mode", "白平衡");
        translationMap.put("Scene Capture Type", "场景拍摄类型");
        translationMap.put("Interoperability Index", "可交换标准");
        translationMap.put("Interoperability Version", "可交换版本");
        translationMap.put("Thumbnail Compression", "压缩模式");
        translationMap.put("Thumbnail Offset", "JPEG缩略图起始位置");
        translationMap.put("Thumbnail Length", "JPEG缩略图数据长度");
        translationMap.put("X Resolution", "水平分辨率");
        translationMap.put("Y Resolution", "垂直分辨率");
        translationMap.put("Resolution Unit", "分辨率单位");
        translationMap.put("Lens Information", "镜头信息");
        translationMap.put("Lens", "镜头");
        translationMap.put("Compression", "压缩模式");
        translationMap.put("Number of Tables", "表的数量");
        translationMap.put("Compression Type", "压缩类型");
        translationMap.put("Brightness Value", "亮度值");
        translationMap.put("Focal Length 35", "35mm焦距");
        translationMap.put("Detected File Type Long Name", "检测到的文件类型长名称");
        translationMap.put("Detected MIME Type", "检测到的MIME类型");
        translationMap.put("Expected File Name Extension", "预期的文件名扩展名");
        translationMap.put("Detected File Type Name", "检测到的文件类型名称");
        translationMap.put("File Size", "文件大小");
        translationMap.put("File Modified Date", "文件修改日期");
        translationMap.put("File Name", "文件名称");


        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String key = tag.getTagName();  //标签名
                    String value = tag.getDescription(); //标签信息

                    key = translationMap.getOrDefault(key, key);
                    if(key.equals("GPS经度") || key.equals("GPS纬度")) {
                        value = pointToLatlong(value);
                    }

                    metadataMap.put(key,value);
                }
            }
        } catch (Exception e) {
            if(debugMode)e.printStackTrace();
        }

        return metadataMap;
    }


    // 经纬度转化格式
    public static String pointToLatlong (String point) {
        Double du = Double.parseDouble(point.substring(0, point.indexOf("°")).trim());
        Double fen = Double.parseDouble(point.substring(point.indexOf("°")+1, point.indexOf("'")).trim());
        Double miao = Double.parseDouble(point.substring(point.indexOf("'")+1, point.indexOf("\"")).trim());
        Double duStr = du + fen / 60 + miao / 60 / 60 ;
        return duStr.toString();
    }

    public static String blockUrl = getConfigInfo("blockchainUrl");
    public static HashMap<String, String> headers = new HashMap();
    static {
        headers.put("AuthToken", "MHg2ZCwweDcwLDB4NzMsMHg3OSwweDdhLDB4NDQsMHgzMywweDc0LDB4NmQsMHg0NiwweDc1LDB4MzIsMHg3NywweDQ4LDB4NzEsMHg2YywweDZmLDB4NzUsMHgzOCwweDc3LDB4NmMsMHg0NiwweDY4LDB4MmYsMHg0OSwweDQ4LDB4NTEsMHgzNywweDQ3LDB4MzksMHg0NywweDRiLDB4NDcsMHg0YiwweDcxLDB4MzYsMHg2MSwweDM1LDB4NDMsMHg3MiwweDY1LDB4NmUsMHg2MywweDU0LDB4NDQsMHgzMiwweDRiLDB4NTAsMHg2NywweDc4LDB4NWEsMHg0ZCwweDM5LDB4NjEsMHg0ZCwweDRjLDB4MzksMHg1YSwweDJiLDB4NGEsMHg0NCwweDM2LDB4NGIsMHg2ZCwweDVhLDB4NTIsMHg0YywweDcxLDB4NGQsMHgzNiwweDczLDB4NDIsMHg2NywweDM5LDB4NzMsMHg3NCwweDRkLDB4NjUsMHg0MiwweDY3LDB4NmQsMHgzOCwweDUyLDB4NzIsMHg0NSwweDUyLDB4NTcsMHg1OCwweDU4LDB4NzYsMHgzNywweDcwLDB4NGMsMHg3MiwweDc3LDB4NGYsMHg0NiwweDM3LDB4NzMsMHg0OSwweDMyLDB4NTcsMHgzNCwweDZiLDB4NzUsMHg1OSwweDRkLDB4M2Q=");
    }


    public static String getPosition(String lon,String lat){

        RequestObj obj = new RequestObj().setMethod("GET").setHeaders(headers).setUrl(blockUrl + "/geocoder?lon=" + lon +"&lat=" + lat);

        try (CustomHttpResponse con = requests(obj)) {
            JsonObject res = con.getJson().getAsJsonObject();
            return res.get("name").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    public static void main(String[] args) {

        String pos = getPosition("112.8902388888889", "28.24377777777778");
        System.out.println(pos);

        Map<String, String> metadataMap = getExif("123.jpg");
        System.out.println(metadataMap);

        if(metadataMap.containsKey("GPS经度") &&metadataMap.containsKey("GPS纬度")){
            String position = getPosition(metadataMap.get("GPS经度"), metadataMap.get("GPS纬度"));
            System.out.println(position);
        }

    }
}

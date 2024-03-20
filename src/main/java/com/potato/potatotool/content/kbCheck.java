package com.potato.potatotool.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Potato
 * @date 2023/4/12 14:21
 */
public class kbCheck {

    /**
     * 测试调用，接入时请模拟传参
     * @inputStr    用户传入进程列表 (String)
     */
    public static void main(String []args) {

        List<Map<String, String>> cves = init();  //TODO !软件初始化调用一次就行! !!!不要每次搜索都调用!!!

    }


    /**
     * 初始化命令集
     * !软件初始化调用一次就行!
     * !!!不要每次搜索都调用!!!
     * @return  json数据
     */
    public static List<Map<String, String>> init(){

        List<Map<String, String>> cves = new ArrayList<>();
//        String date = null;
//
//        Properties props = new Properties();
//
//        try {
//            props.load(new FileInputStream("config.properties"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        String path = props.getProperty("cvesKB");
//
//        try (ZipFile zipFile = new ZipFile(path)) {
//            Enumeration<? extends ZipEntry> entries = zipFile.entries();
//            while (entries.hasMoreElements()) {
//                ZipEntry entry = entries.nextElement();
//                if (entry.getName().endsWith(".csv")) {
//                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
//                        String data = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//                        try (Scanner scanner = new Scanner(data)) {
//                            scanner.useDelimiter(",|\\r\\n|\\n");
//                            String[] headers = scanner.nextLine().split(",");
//                            while (scanner.hasNextLine()) {
//                                String[] values = scanner.nextLine().split(",");
//                                Map<String, String> row = new HashMap<>();
//                                for (int i = 0; i < headers.length; i++) {
//                                    row.put(headers[i], values[i]);
//                                }
//                                cves.add(row);
//                            }
//                        }
//                    }
//                } else if (entry.getName().endsWith(".txt")) {
//                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
//                        String data = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//                        date = data.trim();
//                    }
//                }
//            }
//        }

        return cves;

    }


}

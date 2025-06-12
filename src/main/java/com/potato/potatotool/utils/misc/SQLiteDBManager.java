package com.potato.potatotool.utils.misc;

/**
 * @author Potato
 * @date 2024/6/29 16:45
 */
import com.potato.potatotool.content.blueTeam.webshell.decoder.utils.MD5Decrypt;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SQLiteDBManager {


    private static final String TMP_FOLDER = ".PotatoTool";
    private static final String md5DB_File = "md5_database.db";
    private static final Path configFolder = Paths.get(System.getProperty("user.home"), TMP_FOLDER);
    private static final Path md5DBFile = configFolder.resolve(md5DB_File);

    private static final String DB_URL = "jdbc:sqlite:" + md5DBFile.toString();

    public static void main(String[] args) {
        //生成处理代码，请勿删除！
//        // 创建表
//        createTable();
//
//        // 插入示例数据
//        try{
//            InputStream inputStream = getResourceStream("md5");
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String finalLine = line.trim();
//                insertMD5Mapping(finalLine);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        // 查询示例数据
        long startTime = System.nanoTime();
        Map hash = hashGetPlaintext("example_text");
        System.out.println("MD5 Hash: " + hash);
        long step1EndTime = System.nanoTime();
        System.out.println("Step 1 took " + (step1EndTime - startTime) + " ns");


        Map hash1 = hashGetPlaintext("ac59075b964b0715");
        System.out.println("MD5 Hash: " + hash1);
        long step2EndTime = System.nanoTime();
        System.out.println("Step 2 took " + (step2EndTime - step1EndTime) + " ns");

        Map hash11 = hashGetPlaintext("202cb962ac59075b964b07152d234b70");
        System.out.println("MD5 Hash: " + hash11);
        long step3EndTime = System.nanoTime();
        System.out.println("Step 3 took " + (step3EndTime - step2EndTime) + " ns");

////        数据库不变动的情况下再调用一次，不要乱调用
//        vacuumZipDB();
//        long step4EndTime = System.nanoTime();
//        System.out.println("Step 4 took " + (step4EndTime - step3EndTime) + " ns");

    }

    public static void createTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS md5_database ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "plaintext TEXT NOT NULL UNIQUE, "
                + "md5_32 TEXT NOT NULL, "
                + "md5_16 TEXT NOT NULL, "
                + "sha1 TEXT NOT NULL"
                + ");";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(createTableSQL)) {
            pstmt.executeUpdate();
            System.out.println("Table created.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void insertMD5Mapping(String plaintext) {
        String insertSQL = "INSERT INTO md5_database (plaintext, md5_32, md5_16, sha1) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, plaintext);
            pstmt.setString(2, MD5Decrypt.hashString(plaintext, "MD5"));
            pstmt.setString(3, MD5Decrypt.hashString(plaintext, "MD5").substring(8, 24));
            pstmt.setString(4, MD5Decrypt.hashString(plaintext, "SHA-1"));
            pstmt.executeUpdate();
//            System.out.println("Data inserted.");
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
//                System.out.println("Duplicate plaintext value: " + plaintext);
            } else {
                System.out.println(e.getMessage());
            }
        }
    }

    public static Map<String, String> hashGetPlaintext(String hash) {
        String querySQL = "SELECT plaintext, md5_32, md5_16, sha1 FROM md5_database WHERE md5_32 = ? OR md5_16 = ? OR sha1 = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {
            pstmt.setString(1, hash);
            pstmt.setString(2, hash);
            pstmt.setString(3, hash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, String> resultMap = new LinkedHashMap<>();
                resultMap.put("plaintext", rs.getString("plaintext"));
                resultMap.put("MD5", rs.getString("md5_32"));
                resultMap.put("MD5-16", rs.getString("md5_16"));
                resultMap.put("SHA1", rs.getString("sha1"));
                System.out.println(resultMap);
                return resultMap;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    // 数据库不变动的情况下再调用  优化结构，减小体积
    public static void vacuumZipDB(){
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("VACUUM;");
            System.out.println("Database vacuumed successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}
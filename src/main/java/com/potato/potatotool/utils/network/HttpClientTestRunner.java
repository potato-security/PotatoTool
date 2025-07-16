package com.potato.potatotool.utils.network;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.List;
import com.potato.potatotool.utils.okhttp.OkHttpRequestUtils;
import com.potato.potatotool.utils.okhttp.OkHttpRequestObj;
import com.potato.potatotool.utils.okhttp.OkHttpCustomResponse;
import okhttp3.OkHttpClient;
import okhttp3.ConnectionPool;
import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端性能测试主程序
 * 提供统一的入口来运行各种基准测试
 * @author Benchmark
 */
public class HttpClientTestRunner {
    
    // 共享的OkHttpClient实例，用于提高连接复用效率
    private static final OkHttpClient sharedClient = new OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
        .build();
    
    /**
     * 显示测试菜单
     */
    private static void showMenu() {
        System.out.println("=========================================");
        System.out.println("   HTTP客户端性能测试 - 请选择测试模式");
        System.out.println("=========================================");
        System.out.println("1. 运行基本性能测试套件（单线程、小规模测试）");
        System.out.println("2. 运行高并发性能测试（大规模网络扫描）");
        System.out.println("3. 运行所有测试");
        System.out.println("4. 检测网络连接状态");
        System.out.println("0. 退出");
        System.out.println("=========================================");
        System.out.print("请输入选项: ");
    }
    
    /**
     * 运行基本性能测试套件
     */
    private static void runBasicTests() {
        System.out.println("\n开始运行基本性能测试套件...");
        
        // 先测试网络连接
        if (!checkInternetConnection()) {
            System.out.println("\n网络连接测试失败，可能影响测试结果准确性。是否仍要继续？(y/n)");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim().toLowerCase();
            
            if (!input.equals("y") && !input.equals("yes")) {
                System.out.println("测试已取消");
                return;
            }
        }
        
        // 运行所有基本测试
        HttpClientBenchmark.runAllBenchmarks();
    }
    
    /**
     * 运行高并发性能测试
     */
    private static void runHighConcurrencyTests() {
        Scanner scanner = new Scanner(System.in);
        
        // 先测试网络连接
        if (!checkInternetConnection()) {
            System.out.println("\n网络连接测试失败，可能影响测试结果准确性。是否仍要继续？(y/n)");
            String input = scanner.nextLine().trim().toLowerCase();
            
            if (!input.equals("y") && !input.equals("yes")) {
                System.out.println("测试已取消");
                return;
            }
        }
        
        System.out.println("\n开始配置高并发性能测试...");
        System.out.print("请输入测试URL数量 (建议用于测试: 1000, 完整测试: 1000000): ");
        int urlCount = getIntInput(scanner, 1000);
        
        System.out.print("请输入并发线程数 (建议: 50-200): ");
        int concurrency = getIntInput(scanner, 50);
        
        // 生成测试URL并运行测试
        try {
            System.out.println("正在生成 " + urlCount + " 个测试URL...");
            java.util.List<String> testUrls = generateTestUrls(urlCount);
            
            System.out.println("开始运行高并发测试，并发线程数: " + concurrency);
            HighConcurrencyBenchmark.runMassiveScanTest(testUrls, concurrency);
        } catch (Exception e) {
            System.err.println("高并发测试出错: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\n测试中断，请查看错误信息");
        }
    }
    
    /**
     * 检测网络连接
     */
    private static boolean checkInternetConnection() {
        System.out.println("正在检测网络连接...");
        
        String[] testHosts = {
            "https://www.baidu.com",
            "https://www.qq.com",
            "https://www.example.com",
            "https://postman-echo.com/get"
        };
        
        // 先尝试使用OkHttp
        for (String host : testHosts) {
            try {
                OkHttpRequestObj requestObj = new OkHttpRequestObj()
                        .setUrl(host)
                        .setMethod("HEAD")
                        .setTimeOut(3);
                
                OkHttpCustomResponse response = OkHttpRequestUtils.requests(requestObj, sharedClient);
                int responseCode = response.getResponseCode();
                response.disconnect();
                
                if (responseCode >= 200 && responseCode < 400) {
                    System.out.println("网络连接正常: 成功连接到 " + host);
                    return true;
                }
            } catch (Exception e) {
                // 忽略OkHttp异常，尝试下一个或使用HttpURLConnection
                System.out.println("OkHttp连接测试失败: " + host + " - " + e.getMessage());
            }
        }
        
        // 如果OkHttp全部失败，尝试使用HttpURLConnection
        for (String host : testHosts) {
            try {
                URL url = new URL(host);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                
                if (responseCode >= 200 && responseCode < 400) {
                    System.out.println("网络连接正常: 成功连接到 " + host);
                    return true;
                }
            } catch (IOException e) {
                System.out.println("网络连接测试失败: " + host + " - " + e.getMessage());
            }
        }
        
        System.out.println("警告: 所有测试连接均失败，请检查网络状态");
        return false;
    }
    
    /**
     * 获取整数输入
     */
    private static int getIntInput(Scanner scanner, int defaultValue) {
        try {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultValue;
            }
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("输入无效，使用默认值: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * 生成测试URL列表
     */
    private static java.util.List<String> generateTestUrls(int count) {
        return HighConcurrencyBenchmark.generateTestUrls(count);
    }
    
    /**
     * 显示欢迎信息
     */
    private static void showWelcomeMessage() {
        System.out.println("\n=======================================");
        System.out.println("   HTTP客户端性能测试工具");
        System.out.println("=======================================");
        System.out.println("该工具用于比较OkHttp和HttpURLConnection的性能差异");
        System.out.println("支持测试：");
        System.out.println(" - 单线程请求性能（GET/POST/下载）");
        System.out.println(" - 连接池性能");
        System.out.println(" - 大规模并发请求性能");
        System.out.println("=======================================\n");
    }
    
    /**
     * 运行所有测试
     */
    private static void runAllTests() {
        // 先测试网络连接
        if (!checkInternetConnection()) {
            System.out.println("\n网络连接测试失败，可能影响测试结果准确性。是否仍要继续？(y/n)");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim().toLowerCase();
            
            if (!input.equals("y") && !input.equals("yes")) {
                System.out.println("测试已取消");
                return;
            }
        }
        
        System.out.println("\n开始运行所有性能测试...");
        
        try {
            // 先运行基本测试
            HttpClientBenchmark.runAllBenchmarks();
            
            // 再运行高并发测试（使用较小的数据集）
            System.out.println("\n现在运行高并发测试（使用默认参数）...");
            java.util.List<String> testUrls = generateTestUrls(1000);
            HighConcurrencyBenchmark.runMassiveScanTest(testUrls, 50);
        } catch (Exception e) {
            System.err.println("测试过程中出现错误: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\n测试中断，请查看错误信息");
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        try {
            showWelcomeMessage();
            
            Scanner scanner = new Scanner(System.in);
            boolean running = true;
            
            while (running) {
                showMenu();
                String choice = scanner.nextLine().trim();
                
                try {
                    switch (choice) {
                        case "1":
                            runBasicTests();
                            break;
                        case "2":
                            runHighConcurrencyTests();
                            break;
                        case "3":
                            runAllTests();
                            break;
                        case "4":
                            checkInternetConnection();
                            break;
                        case "0":
                            running = false;
                            System.out.println("感谢使用，再见！");
                            break;
                        default:
                            System.out.println("无效选项，请重新输入");
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("执行操作时出错: " + e.getMessage());
                    e.printStackTrace();
                }
                
                if (running) {
                    System.out.println("\n按Enter键继续...");
                    scanner.nextLine();
                }
            }
            
            scanner.close();
            
        } catch (Exception e) {
            System.err.println("测试运行过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 
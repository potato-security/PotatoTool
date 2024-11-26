package com.potato.potatotool.content.redTeam.infoGathering.subDomain;

import com.potato.potatotool.utils.ExecutorServiceManager;
import org.xbill.DNS.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2024/10/12 17:59
 */
public class SubdomainBruteForcer {
    private static final int THREAD_POOL_SIZE = ExecutorServiceManager.getOptimalThreadPoolSize() * 160; // 线程池大小，调整以适应资源
    private static final String[] DNS_SERVERS = {
            "8.8.8.8",
            "1.1.1.1",
            "114.114.114.114",
            "223.5.5.5",
            "119.29.29.29",
            "180.76.76.76",
            "1.2.4.8",
    }; // DNS服务器列表
    private static final Set<String> cache = Collections.newSetFromMap(new ConcurrentHashMap<>()); // 多线程时安全的缓存结果

    public static Set<String> getSubDomain(String domain){
        try {
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            long startTime = System.nanoTime();
            // 使用CompletableFuture异步处理子域名查询
            try (InputStream aesKeyInputStream = getResourceStream("subDomains");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(aesKeyInputStream, StandardCharsets.UTF_8))) {
                String subdomain;
                while ((subdomain = reader.readLine()) != null) {
                    String fullSubdomain = subdomain.trim() + "." + domain; // 组合完整的子域名
                    CompletableFuture.runAsync(() -> resolveSubdomain(fullSubdomain), executor);
                }
            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }

            // 关闭线程池，等待所有任务完成
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // 阻塞当前线程，等待所有任务完成
            long endTime = System.nanoTime();
            long duration = endTime - startTime;

            // 将纳米时间转换为秒
            double durationInSeconds = duration / 1_000_000_000.0;
            System.out.println("子域名爆破运行时长为：" + durationInSeconds + " 秒");

        }catch (Exception e){
            if (debugMode) e.printStackTrace();
        }

        return cache;
    }


    private static void resolveSubdomain(String subdomain) {
        try {
            // 执行DNS解析
            for (String dnsServer : DNS_SERVERS) {
                try {
                    Resolver resolver = new SimpleResolver(dnsServer);
                    resolver.setTimeout(Duration.ofSeconds(2));
                    resolver.setTCP(false);
                    Lookup lookup = new Lookup(subdomain, Type.A);
                    lookup.setResolver(resolver);
                    lookup.setCache(null); // 禁用缓存，确保每次请求都查询服务器
                    lookup.run();

                    if (lookup.getResult() == Lookup.SUCCESSFUL) {
                        cache.add(subdomain);
                        break; // 成功后跳出DNS服务器循环
                    }
                } catch (Exception e) {
                    if (debugMode) System.err.println("Failed on DNS server: " + dnsServer + " " + e.toString());
                }
            }
        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println(getSubDomain("potato.gold"));

    }
}

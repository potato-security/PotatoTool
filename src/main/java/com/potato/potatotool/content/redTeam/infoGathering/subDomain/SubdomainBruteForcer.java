package com.potato.potatotool.content.redTeam.infoGathering.subDomain;

import com.potato.potatotool.utils.core.ExecutorServiceManager;
import org.xbill.DNS.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

import static com.potato.potatotool.ToStart.debugMode;
import static com.potato.potatotool.utils.core.Constants.getResourceStream;

/**
 * @author Potato
 * @date 2024/10/12 17:59
 */
public class SubdomainBruteForcer {
    private static final int MAX_PARALLEL_QUERIES = 24;
    private static final int DNS_TIMEOUT_SECONDS = 1;
    private static final int WILDCARD_SAMPLE_COUNT = 3;
    private static final String[] DNS_SERVERS = {
            "8.8.8.8",
            "1.1.1.1",
            "114.114.114.114",
            "223.5.5.5",
            "119.29.29.29",
            "180.76.76.76",
            "1.2.4.8",
    }; // DNS服务器列表

    public static Set<String> getSubDomain(String domain){
        if (domain == null || domain.trim().isEmpty()) {
            return Collections.emptySet();
        }

        final Set<String> resolvedSubdomains = ConcurrentHashMap.newKeySet();
        final Set<String> wildcardAnswers = detectWildcardAnswers(domain.trim());
        if (!wildcardAnswers.isEmpty()) {
            if (debugMode) {
                System.out.println("检测到泛解析，跳过子域名字典爆破: " + domain + " -> " + wildcardAnswers);
            }
            return Collections.emptySet();
        }
        try {
            String poolName = ExecutorServiceManager.ExecutorPoolNames.SUBDOMAINBURTEFORCER_ASSET;
            ExecutorServiceManager.ExecutorConfig config = new ExecutorServiceManager.ExecutorConfig();
            int poolSize = Math.min(Math.max(Runtime.getRuntime().availableProcessors() * 2, 4), MAX_PARALLEL_QUERIES);
            config.setCorePoolSize(poolSize);
            config.setMaxPoolSize(poolSize);
            config.setQueueCapacity(4096);
            ExecutorService executor = ExecutorServiceManager.getOrCreateExecutor(poolName, config);
            List<CompletableFuture<?>> futures = new ArrayList<>();

            long startTime = System.nanoTime();
            // 使用CompletableFuture异步处理子域名查询
            try (InputStream aesKeyInputStream = getResourceStream("subDomains");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(aesKeyInputStream, StandardCharsets.UTF_8))) {
                String subdomain;
                while ((subdomain = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    String fullSubdomain = subdomain.trim() + "." + domain; // 组合完整的子域名
                    CompletableFuture<?> future = CompletableFuture.runAsync(
                            () -> resolveSubdomain(fullSubdomain, wildcardAnswers, resolvedSubdomains), executor);
                    futures.add(future);
                }
            } catch (Exception e) {
                if(debugMode)e.printStackTrace();
            }

            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get(); // 阻塞直到任务完成
                } catch (CancellationException ce) {
                } catch (Exception e) {
                    if (debugMode) e.printStackTrace();
                }
            }
            // 停止所有线程
            ExecutorServiceManager.shutdownExecutor(poolName);

            if (debugMode) {
                long endTime = System.nanoTime();
                long duration = endTime - startTime;
                double durationInSeconds = duration / 1_000_000_000.0;
                System.out.println("子域名爆破运行时长为：" + durationInSeconds + " 秒");
                if (!wildcardAnswers.isEmpty()) {
                    System.out.println("检测到泛解析，已过滤匹配泛解析响应的子域名: " + wildcardAnswers);
                }
            }

        }catch (Exception e){
            if (debugMode) e.printStackTrace();
        }

        return new LinkedHashSet<>(resolvedSubdomains);
    }


    private static void resolveSubdomain(String subdomain, Set<String> wildcardAnswers, Set<String> resolvedSubdomains) {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        try {
            DnsQueryResult queryResult = resolveARecords(subdomain);
            if (!queryResult.success || queryResult.answers.isEmpty()) {
                return;
            }

            if (!wildcardAnswers.isEmpty() && wildcardAnswers.equals(queryResult.answers)) {
                return;
            }

            resolvedSubdomains.add(subdomain);
        } catch (Exception e) {
            if (debugMode) e.printStackTrace();
        }
    }

    private static Set<String> detectWildcardAnswers(String domain) {
        AtomicInteger matchedSamples = new AtomicInteger(0);
        Set<String> wildcardAnswers = new LinkedHashSet<>();

        for (int i = 0; i < WILDCARD_SAMPLE_COUNT; i++) {
            String randomSubdomain = UUID.randomUUID().toString().replace("-", "") + "." + domain;
            DnsQueryResult queryResult = resolveARecords(randomSubdomain);
            if (!queryResult.success || queryResult.answers.isEmpty()) {
                continue;
            }

            if (wildcardAnswers.isEmpty()) {
                wildcardAnswers.addAll(queryResult.answers);
                matchedSamples.incrementAndGet();
            } else if (wildcardAnswers.equals(queryResult.answers)) {
                matchedSamples.incrementAndGet();
            }
        }

        return matchedSamples.get() >= 2 ? wildcardAnswers : Collections.<String>emptySet();
    }

    private static DnsQueryResult resolveARecords(String subdomain) {
        int startIndex = Math.abs(subdomain.hashCode()) % DNS_SERVERS.length;
        for (int i = 0; i < DNS_SERVERS.length; i++) {
            String dnsServer = DNS_SERVERS[(startIndex + i) % DNS_SERVERS.length];
            try {
                Resolver resolver = new SimpleResolver(dnsServer);
                resolver.setTimeout(Duration.ofSeconds(DNS_TIMEOUT_SECONDS));
                resolver.setTCP(false);

                Lookup lookup = new Lookup(subdomain, Type.A);
                lookup.setResolver(resolver);
                lookup.setCache(null); // 禁用缓存，确保每次请求都查询服务器
                Record[] records = lookup.run();

                if (lookup.getResult() == Lookup.SUCCESSFUL && records != null && records.length > 0) {
                    Set<String> answers = new LinkedHashSet<>();
                    for (Record record : records) {
                        if (record instanceof ARecord) {
                            answers.add(((ARecord) record).getAddress().getHostAddress());
                        } else if (record instanceof CNAMERecord) {
                            answers.add(((CNAMERecord) record).getTarget().toString(true));
                        } else {
                            answers.add(record.rdataToString());
                        }
                    }
                    return DnsQueryResult.success(answers);
                }

                if (lookup.getResult() == Lookup.HOST_NOT_FOUND || lookup.getResult() == Lookup.TYPE_NOT_FOUND) {
                    return DnsQueryResult.notFound();
                }
            } catch (Exception e) {
                if (debugMode) {
                    System.err.println("Failed on DNS server: " + dnsServer + " " + e.toString());
                }
            }
        }

        return DnsQueryResult.notFound();
    }

    private static class DnsQueryResult {
        private final boolean success;
        private final Set<String> answers;

        private DnsQueryResult(boolean success, Set<String> answers) {
            this.success = success;
            this.answers = answers;
        }

        private static DnsQueryResult success(Set<String> answers) {
            return new DnsQueryResult(true, answers);
        }

        private static DnsQueryResult notFound() {
            return new DnsQueryResult(false, Collections.<String>emptySet());
        }
    }

    public static void main(String[] args) {
        System.out.println(getSubDomain("potato.gold"));

    }
}

package com.potato.potatotool.utils.ai.transport;

import com.potato.potatotool.utils.network.RequestObj;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class AiGatewayClientFactory {

    private static final String DEFAULT_PKCS12_PASSWORD = "potato";
    private static final String CLASSPATH_CLIENT_P12 = "/certs/ai-gateway-client.p12";

    private static final ConnectionPool DEFAULT_CONNECTION_POOL =
            new ConnectionPool(64, 5, TimeUnit.MINUTES);
    private static final Dispatcher DEFAULT_DISPATCHER = createDefaultDispatcher();

    private static final Object SECURITY_BUNDLE_LOCK = new Object();
    private static volatile SecurityBundle cachedSecurityBundle;

    private AiGatewayClientFactory() {
    }

    public static boolean shouldUseSecureClient(RequestObj requestObj) {
        return requestObj != null && requestObj.isInternalAiRequest();
    }

    public static OkHttpClient createClient(RequestObj requestObj) {
        SecurityBundle securityBundle = getSecurityBundle();
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .sslSocketFactory(securityBundle.sslSocketFactory, securityBundle.trustManager)
                .retryOnConnectionFailure(false)
                .connectionPool(requestObj != null && requestObj.getConnectionPool() != null
                        ? requestObj.getConnectionPool() : DEFAULT_CONNECTION_POOL)
                .dispatcher(requestObj != null && requestObj.getDispatcher() != null
                        ? requestObj.getDispatcher() : DEFAULT_DISPATCHER);

        if (requestObj != null) {
            builder.followRedirects(requestObj.getFollowRedirects())
                    .connectTimeout(requestObj.getTimeOut(), TimeUnit.SECONDS)
                    .readTimeout(requestObj.getReadTimeout(), TimeUnit.SECONDS)
                    .writeTimeout(requestObj.getWriteTimeout(), TimeUnit.SECONDS)
                    .callTimeout(requestObj.getCallTimeout(), TimeUnit.SECONDS)
                    .proxy(resolveProxy(requestObj));
        }

        return builder.build();
    }

    private static Dispatcher createDefaultDispatcher() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(64);
        dispatcher.setMaxRequestsPerHost(16);
        return dispatcher;
    }

    private static SecurityBundle getSecurityBundle() {
        SecurityBundle local = cachedSecurityBundle;
        if (local != null) {
            return local;
        }
        synchronized (SECURITY_BUNDLE_LOCK) {
            if (cachedSecurityBundle == null) {
                cachedSecurityBundle = buildSecurityBundle();
            }
            return cachedSecurityBundle;
        }
    }

    private static SecurityBundle buildSecurityBundle() {
        char[] password = resolvePkcs12Password().toCharArray();
        try {
            KeyStore clientStore = KeyStore.getInstance("PKCS12");
            try (InputStream inputStream = openClientPkcs12()) {
                clientStore.load(inputStream, password);
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(clientStore, password);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            X509TrustManager trustManager = findX509TrustManager(trustManagerFactory.getTrustManagers());
            if (trustManager == null) {
                throw new IllegalStateException("未找到默认 X509TrustManager");
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[]{trustManager}, new SecureRandom());

            return new SecurityBundle(sslContext.getSocketFactory(), trustManager);
        } catch (Exception e) {
            throw new IllegalStateException("初始化内置 AI 网关 TLS 失败: " + e.getMessage(), e);
        } finally {
            clearPassword(password);
        }
    }

    private static InputStream openClientPkcs12() throws IOException {
        InputStream classpathStream = AiGatewayClientFactory.class.getResourceAsStream(CLASSPATH_CLIENT_P12);
        if (classpathStream != null) {
            return classpathStream;
        }

        throw new IOException("未找到内置 AI 网关客户端证书打包资源: " + CLASSPATH_CLIENT_P12);
    }

    private static Proxy resolveProxy(RequestObj requestObj) {
        if (requestObj == null) {
            return Proxy.NO_PROXY;
        }
        String proxyValue = requestObj.getProxies();
        if (proxyValue == null || proxyValue.trim().isEmpty()) {
            return Proxy.NO_PROXY;
        }

        String normalized = proxyValue.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        Proxy.Type proxyType = Proxy.Type.HTTP;
        if (lower.startsWith("socks://") || "SOCKS".equalsIgnoreCase(requestObj.getProxiesType())) {
            proxyType = Proxy.Type.SOCKS;
        }

        normalized = normalized.replaceFirst("(?i)^https?://", "").replaceFirst("(?i)^socks://", "");
        String[] parts = normalized.split(":");
        if (parts.length != 2) {
            return Proxy.NO_PROXY;
        }

        try {
            return new Proxy(proxyType, new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
        } catch (Exception ignored) {
            return Proxy.NO_PROXY;
        }
    }

    private static String resolvePkcs12Password() {
        String configuredPassword = readPropertyOrEnv("potato.ai.gateway.p12.password", "POTATO_AI_GATEWAY_P12_PASSWORD");
        if (configuredPassword == null || configuredPassword.trim().isEmpty()) {
            return DEFAULT_PKCS12_PASSWORD;
        }
        return configuredPassword.trim();
    }

    private static String readPropertyOrEnv(String systemPropertyKey, String envKey) {
        String value = System.getProperty(systemPropertyKey);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        value = System.getenv(envKey);
        return value == null ? "" : value.trim();
    }

    private static X509TrustManager findX509TrustManager(TrustManager[] trustManagers) {
        if (trustManagers == null) {
            return null;
        }
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
    }

    private static void clearPassword(char[] password) {
        if (password == null) {
            return;
        }
        for (int i = 0; i < password.length; i++) {
            password[i] = 0;
        }
    }

    private static final class SecurityBundle {
        private final javax.net.ssl.SSLSocketFactory sslSocketFactory;
        private final X509TrustManager trustManager;

        private SecurityBundle(javax.net.ssl.SSLSocketFactory sslSocketFactory,
                               X509TrustManager trustManager) {
            this.sslSocketFactory = sslSocketFactory;
            this.trustManager = trustManager;
        }
    }
}

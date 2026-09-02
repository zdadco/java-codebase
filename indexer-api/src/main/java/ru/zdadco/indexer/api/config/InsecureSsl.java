package ru.zdadco.indexer.api.config;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Disables TLS certificate and hostname verification for outbound HTTPS.
 * Intended for environments with SSL-inspecting proxies.
 */
public final class InsecureSsl {

    private static final Object LOCK = new Object();
    private static volatile boolean installed;
    private static SSLContext sslContext;

    private InsecureSsl() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        synchronized (LOCK) {
            if (installed) {
                return;
            }
            try {
                System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, trustAllManagers(), new SecureRandom());
                SSLContext.setDefault(context);
                HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
                sslContext = context;
                installed = true;
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to disable TLS certificate verification", ex);
            }
        }
    }

    public static SSLContext sslContext() {
        install();
        return sslContext;
    }

    private static TrustManager[] trustAllManagers() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
    }
}

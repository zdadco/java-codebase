package ru.zdadco.indexer.api.config;

import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InsecureSslTest {

    @Test
    void installDisablesCertificateAndHostnameChecks() throws Exception {
        InsecureSsl.install();

        assertThat(System.getProperty("jdk.internal.httpclient.disableHostnameVerification"))
                .isEqualTo("true");
        assertThat(SSLContext.getDefault()).isSameAs(InsecureSsl.sslContext());
        assertThat(HttpsURLConnection.getDefaultHostnameVerifier().verify("untrusted.example", null))
                .isTrue();
        assertThatCode(() -> InsecureSsl.sslContext().getSocketFactory().createSocket())
                .doesNotThrowAnyException();
    }
}

package com.potato.potatotool.content.redTeam.vulnScanner.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.security.auth.x500.X500Principal;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SslHandler 证书有效性判定测试")
class SslHandlerCertificateValidityTest {

    @Test
    @DisplayName("自签名且在有效期内的证书应标记为 valid 且 self-signed")
    void shouldMarkValidSelfSignedCertificate() throws Exception {
        Date now = new Date();
        FakeX509Certificate certificate = new FakeX509Certificate(
                new Date(now.getTime() - 60_000L),
                new Date(now.getTime() + 60_000L),
                "CN=PotatoTool",
                "CN=PotatoTool"
        );

        SslHandler.SslResponse response = inspectCertificate(certificate);

        assertTrue(response.isCertificateValid());
        assertFalse(response.isExpired());
        assertTrue(response.isSelfSigned());
    }

    @Test
    @DisplayName("过期证书应标记为 invalid 和 expired")
    void shouldMarkExpiredCertificate() throws Exception {
        Date now = new Date();
        FakeX509Certificate certificate = new FakeX509Certificate(
                new Date(now.getTime() - 120_000L),
                new Date(now.getTime() - 60_000L),
                "CN=expired.example",
                "CN=TestCA"
        );

        SslHandler.SslResponse response = inspectCertificate(certificate);

        assertFalse(response.isCertificateValid());
        assertTrue(response.isExpired());
        assertFalse(response.isSelfSigned());
    }

    @Test
    @DisplayName("尚未生效的证书也应标记为 invalid 和 expired")
    void shouldMarkNotYetValidCertificate() throws Exception {
        Date now = new Date();
        FakeX509Certificate certificate = new FakeX509Certificate(
                new Date(now.getTime() + 60_000L),
                new Date(now.getTime() + 120_000L),
                "CN=future.example",
                "CN=FutureCA"
        );

        SslHandler.SslResponse response = inspectCertificate(certificate);

        assertFalse(response.isCertificateValid());
        assertTrue(response.isExpired());
        assertFalse(response.isSelfSigned());
    }

    private SslHandler.SslResponse inspectCertificate(X509Certificate certificate) throws Exception {
        Method method = SslHandler.class.getDeclaredMethod(
                "checkCertificateValidity", X509Certificate.class, SslHandler.SslResponse.class);
        method.setAccessible(true);

        SslHandler.SslResponse response = new SslHandler.SslResponse();
        method.invoke(null, certificate, response);
        return response;
    }

    private static final class FakeX509Certificate extends X509Certificate {
        private final Date notBefore;
        private final Date notAfter;
        private final X500Principal subject;
        private final X500Principal issuer;

        private FakeX509Certificate(Date notBefore, Date notAfter, String subjectDn, String issuerDn) {
            this.notBefore = notBefore;
            this.notAfter = notAfter;
            this.subject = new X500Principal(subjectDn);
            this.issuer = new X500Principal(issuerDn);
        }

        @Override
        public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
            checkValidity(new Date());
        }

        @Override
        public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
            if (date.before(notBefore)) {
                throw new CertificateNotYetValidException("not yet valid");
            }
            if (date.after(notAfter)) {
                throw new CertificateExpiredException("expired");
            }
        }

        @Override
        public int getVersion() {
            return 3;
        }

        @Override
        public BigInteger getSerialNumber() {
            return BigInteger.ONE;
        }

        @Override
        public Principal getIssuerDN() {
            return issuer;
        }

        @Override
        public Principal getSubjectDN() {
            return subject;
        }

        @Override
        public Date getNotBefore() {
            return notBefore;
        }

        @Override
        public Date getNotAfter() {
            return notAfter;
        }

        @Override
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getSigAlgName() {
            return "SHA256withRSA";
        }

        @Override
        public String getSigAlgOID() {
            return "1.2.840.113549.1.1.11";
        }

        @Override
        public byte[] getSigAlgParams() {
            return new byte[0];
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return null;
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return null;
        }

        @Override
        public boolean[] getKeyUsage() {
            return null;
        }

        @Override
        public int getBasicConstraints() {
            return -1;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public void verify(PublicKey key) throws CertificateException {
        }

        @Override
        public void verify(PublicKey key, String sigProvider) throws CertificateException {
        }

        @Override
        public String toString() {
            return subject.getName();
        }

        @Override
        public PublicKey getPublicKey() {
            return new PublicKey() {
                @Override
                public String getAlgorithm() {
                    return "RSA";
                }

                @Override
                public String getFormat() {
                    return "X.509";
                }

                @Override
                public byte[] getEncoded() {
                    return new byte[0];
                }
            };
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return null;
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return null;
        }

        @Override
        public byte[] getExtensionValue(String oid) {
            return null;
        }

        @Override
        public X500Principal getSubjectX500Principal() {
            return subject;
        }

        @Override
        public X500Principal getIssuerX500Principal() {
            return issuer;
        }

        @Override
        public Collection<List<?>> getSubjectAlternativeNames() throws CertificateParsingException {
            return Collections.emptyList();
        }

        @Override
        public Collection<List<?>> getIssuerAlternativeNames() throws CertificateParsingException {
            return Collections.emptyList();
        }

        @Override
        public List<String> getExtendedKeyUsage() throws CertificateParsingException {
            return Collections.emptyList();
        }
    }
}

package org.givingledger.contracts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Component;

@Component
public final class PdfContractSigner {
    private final ContractConfig config;

    public PdfContractSigner(ContractConfig config) {
        this.config = config;
        Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] sign(byte[] unsignedPdf, String reason) throws Exception {
        X509Certificate certificate = readCertificate();
        PrivateKey key = readPrivateKey();
        try (PDDocument document = Loader.loadPDF(unsignedPdf);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(certificate.getSubjectX500Principal().getName());
            signature.setReason(reason);
            signature.setSignDate(Calendar.getInstance());
            document.addSignature(signature, content -> createCmsSignature(content, key, certificate));
            document.saveIncremental(output);
            return output.toByteArray();
        }
    }

    private static byte[] createCmsSignature(InputStream content, PrivateKey key, X509Certificate cert)
            throws IOException {
        try {
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            var signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(key);
            var digests = new JcaDigestCalculatorProviderBuilder().setProvider("BC").build();
            generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(digests).build(signer, cert));
            generator.addCertificates(new JcaCertStore(List.of(cert)));
            return generator.generate(new CMSProcessableByteArray(content.readAllBytes()), false).getEncoded();
        } catch (Exception e) {
            throw new IOException("Could not create contract signature", e);
        }
    }

    private X509Certificate readCertificate() throws Exception {
        try (InputStream input = Files.newInputStream(config.certificatePath())) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
        }
    }

    private PrivateKey readPrivateKey() throws Exception {
        String pem = Files.readString(config.privateKeyPath())
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}

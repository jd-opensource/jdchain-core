package com.jd.blockchain.tools.cli;

import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateUsage;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.crypto.*;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import picocli.CommandLine;
import utils.StringUtils;
import utils.io.FileUtils;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @description: JD Chain certificate management
 * @author: imuge
 * @date: 2021/9/1
 **/
@CommandLine.Command(name = "ca", mixinStandardHelpOptions = true, showDefaultValues = true, description = "List, create, update certificates.", subcommands = {CAShow.class, CACsr.class, CACrt.class, CARenew.class, CAPKCS12.class, CAGMPKCS12.class, CATest.class, CATestPlus.class, CommandLine.HelpCommand.class})

public class CA implements Runnable {
    static final String CA_HOME = "certs";
    static final String KEYS_HOME = "keys";
    static final String CA_SIGN_HOME = "certs/sign";
    static final String CA_TLS_HOME = "certs/tls";
    static final String CA_LIST_FORMAT = "%s\t%s\t%s\t%s%n";
    static Map<String, String> CA_ALGORITHM_MAP = new HashMap<>();

    static {
        CA_ALGORITHM_MAP.put("ED25519", "Ed25519");
        CA_ALGORITHM_MAP.put("SM2", "SM3WITHSM2");
        CA_ALGORITHM_MAP.put("RSA", "SHA256withRSA");
        CA_ALGORITHM_MAP.put("ECDSA", "SHA256WITHECDSA");
    }

    @CommandLine.ParentCommand
    JDChainCli jdChainCli;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }

    protected String getCaHome() {
        try {
            File caHome = new File(jdChainCli.path.getCanonicalPath() + File.separator + CA_HOME);
            if (!caHome.exists()) {
                caHome.mkdirs();
            }

            return caHome.getAbsolutePath();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected String getSignHome() {
        try {
            File caHome = new File(jdChainCli.path.getCanonicalPath() + File.separator + CA_SIGN_HOME);
            if (!caHome.exists()) {
                caHome.mkdirs();
            }

            return caHome.getAbsolutePath();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected String getTlsHome() {
        try {
            File caHome = new File(jdChainCli.path.getCanonicalPath() + File.separator + CA_TLS_HOME);
            if (!caHome.exists()) {
                caHome.mkdirs();
            }

            return caHome.getAbsolutePath();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected String getKeysHome() {
        try {
            File caHome = new File(jdChainCli.path.getCanonicalPath() + File.separator + KEYS_HOME);
            if (!caHome.exists()) {
                caHome.mkdirs();
            }

            return caHome.getAbsolutePath();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected String scanValue(String category) {
        System.out.println(String.format("input %s: ", category));
        return ScannerUtils.read();
    }

    protected String[] scanValues(String category, String[] values) {
        System.out.print("input " + category + " (");
        for (int i = 0; i < values.length; i++) {
            System.out.print(i + " for " + values[i]);
            if (i < values.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println(". multi values use ',' split): ");
        while (true) {
            try {
                String[] indexes = ScannerUtils.read().split(",");
                if (indexes.length == 0) {
                    throw new IllegalStateException();
                }
                String[] selected = new String[indexes.length];
                for (int i = 0; i < indexes.length; i++) {
                    selected[i] = values[Integer.parseInt(indexes[i])];
                }
                return selected;
            } catch (Exception e) {
                System.err.print("invalid value");
            }
        }
    }

    protected String scanValue(String category, String[] values) {
        System.out.print("input " + category + " (");
        for (int i = 0; i < values.length; i++) {
            System.out.print(i + " for " + values[i]);
            if (i < values.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println(" )");
        return values[Integer.parseInt(ScannerUtils.read())];
    }

    protected X500Name buildRDN(String organization, CertificateRole ou, String country, String province, String locality, String name, String email) {
        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.O, organization);
        nameBuilder.addRDN(BCStyle.OU, ou.name());
        nameBuilder.addRDN(BCStyle.C, country);
        nameBuilder.addRDN(BCStyle.ST, province);
        nameBuilder.addRDN(BCStyle.L, locality);
        nameBuilder.addRDN(BCStyle.CN, name);
        nameBuilder.addRDN(BCStyle.EmailAddress, email);
        return nameBuilder.build();
    }

    protected PKCS10CertificationRequest genCsr(CertificateUsage usage, String algorithm, String name, X500Name subject, CertificateRole ou, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        boolean isCa = ou.equals(CertificateRole.ROOT) || ou.equals(CertificateRole.CA);
        ContentSigner signGen = new JcaContentSignerBuilder(CA_ALGORITHM_MAP.get(algorithm)).build(privateKey);
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(subject, publicKey);
        if (isCa) {
            p10Builder.setAttribute(Extension.keyUsage, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        } else if (usage.equals(CertificateUsage.SIGN)) {
            p10Builder.setAttribute(Extension.keyUsage, new KeyUsage(KeyUsage.digitalSignature));
        } else if (usage.equals(CertificateUsage.TLS)) {
            p10Builder.setAttribute(Extension.keyUsage, new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.digitalSignature));
            p10Builder.setAttribute(Extension.extendedKeyUsage, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
            p10Builder.setAttribute(Extension.subjectAlternativeName, new GeneralNames(new GeneralName(GeneralName.iPAddress, name)));
        } else if (usage.equals(CertificateUsage.TLS_SIGN)) {
            p10Builder.setAttribute(Extension.keyUsage, new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.digitalSignature));
            p10Builder.setAttribute(Extension.extendedKeyUsage, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
            p10Builder.setAttribute(Extension.subjectAlternativeName, new GeneralNames(new GeneralName(GeneralName.iPAddress, name)));
        } else if (usage.equals(CertificateUsage.TLS_ENC)) {
            p10Builder.setAttribute(Extension.keyUsage, new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement));
            p10Builder.setAttribute(Extension.extendedKeyUsage, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
            p10Builder.setAttribute(Extension.subjectAlternativeName, new GeneralNames(new GeneralName(GeneralName.iPAddress, name)));
        }
        p10Builder.setAttribute(Extension.subjectKeyIdentifier, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey));
        p10Builder.setAttribute(Extension.basicConstraints, new BasicConstraints(isCa));

        return p10Builder.build(signGen);
    }

    protected X509Certificate genCert(CertificateUsage usage, String algorithm, String name, X500Name subject, CertificateRole ou, PublicKey publicKey, PrivateKey issuerPrivateKey, X509Certificate issuerCrt) throws Exception {
        boolean isCa = ou.equals(CertificateRole.ROOT) || ou.equals(CertificateRole.CA);
        X509v3CertificateBuilder certificateBuilder;
        if (null != issuerCrt) {
            certificateBuilder = new JcaX509v3CertificateBuilder(issuerCrt, BigInteger.valueOf(new Random().nextInt() & 0x7fffffff), new Date(), new Date(System.currentTimeMillis() + 3650 * 1000L * 24L * 60L * 60L), subject, publicKey);
        } else {
            certificateBuilder = new JcaX509v3CertificateBuilder(subject, BigInteger.valueOf(new Random().nextInt() & 0x7fffffff), new Date(), new Date(System.currentTimeMillis() + 3650 * 1000L * 24L * 60L * 60L), subject, publicKey);
        }
        if (isCa) {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        } else if (usage.equals(CertificateUsage.SIGN)) {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        } else if (usage.equals(CertificateUsage.TLS)) {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.digitalSignature));
            certificateBuilder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
            certificateBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.iPAddress, name)));
        } else if (usage.equals(CertificateUsage.TLS_SIGN)) {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.digitalSignature));
            certificateBuilder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
            certificateBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.iPAddress, name)));
        } else if (usage.equals(CertificateUsage.TLS_ENC)) {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement));
            certificateBuilder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
            certificateBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.iPAddress, name)));
        }
        certificateBuilder.addExtension(Extension.subjectKeyIdentifier, true, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey));
        certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCa));

        ContentSigner signer = new JcaContentSignerBuilder(CA_ALGORITHM_MAP.get(algorithm.toUpperCase())).build(issuerPrivateKey);
        X509CertificateHolder holder = certificateBuilder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    protected void doubleKeysStore(String alias, PrivateKey signKey, PrivateKey encKey, String password, X509Certificate signCert, X509Certificate encCert, X509Certificate ca) throws Exception {
        char[] phrase = password.toCharArray();

        KeyStore bothStore = KeyStore.getInstance("PKCS12");
        bothStore.load(null, phrase);
        bothStore.setCertificateEntry(alias + ".root", ca);
        bothStore.setCertificateEntry(alias + ".enc", encCert);
        bothStore.setCertificateEntry(alias + ".sig", signCert);
        bothStore.setKeyEntry(alias + ".enc", encKey, phrase, new X509Certificate[]{encCert, ca});
        bothStore.setKeyEntry(alias + ".sig", signKey, phrase, new X509Certificate[]{signCert, ca});

        OutputStream jksStream = new FileOutputStream(getTlsHome() + File.separator + alias + ".keystore");
        bothStore.store(jksStream, phrase);
        jksStream.close();
    }

    protected void trustStore(File trustStoreFile, String alias, String password, X509Certificate cert) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        if (trustStoreFile.exists()) {
            try (FileInputStream storeIn = new FileInputStream(trustStoreFile)) {
                trustStore.load(storeIn, password.toCharArray());
            }
        } else {
            trustStore.load(null, password.toCharArray());
        }
        trustStore.setCertificateEntry(alias, cert);
        try (FileOutputStream storeOut = new FileOutputStream(trustStoreFile)) {
            trustStore.store(storeOut, password.toCharArray());
        }
    }

    protected void keyStore(PrivateKey privateKey, String alias, String password, X509Certificate cert, X509Certificate ca) throws Exception {
        char[] phrase = password.toCharArray();
        X509Certificate[] outChain = {cert, ca};
        KeyStore p12Store = KeyStore.getInstance("PKCS12");
        p12Store.load(null, phrase);
        p12Store.setKeyEntry(alias, privateKey, phrase, outChain);

        KeyStore jksStore = KeyStore.getInstance("PKCS12");
        jksStore.load(null, phrase);
        jksStore.setKeyEntry(alias, p12Store.getKey(alias, phrase), phrase, outChain);
        OutputStream jksStream = new FileOutputStream(getTlsHome() + File.separator + alias + ".keystore");
        jksStore.store(jksStream, phrase);
        jksStream.close();
    }
}

@CommandLine.Command(name = "show", mixinStandardHelpOptions = true, header = "Show certificate.")
class CAShow implements Runnable {

    @CommandLine.Option(required = true, names = "--crt", description = "Path of the certificate file")
    String cert;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        Security.removeProvider("SunEC");
        X509Certificate certificate = CertificateUtils.parseCertificate(FileUtils.readText(new File(cert)));
        System.out.printf(caCli.CA_LIST_FORMAT, "ALGORITHM", "ROLE", "CN", "PUBKEY");
        PubKey pubKey = CertificateUtils.resolvePubKey(certificate);
        Set<String> ous = CertificateUtils.getSubject(certificate, BCStyle.OU);
        Set<String> cns = CertificateUtils.getSubject(certificate, BCStyle.CN);
        System.out.printf(caCli.CA_LIST_FORMAT, Crypto.getAlgorithm(pubKey.getAlgorithm()).name(), ous, cns, pubKey);
        System.out.println(certificate.toString());
    }
}

@CommandLine.Command(name = "csr", mixinStandardHelpOptions = true, header = "Create certificate signing request.")
class CACsr implements Runnable {

    @CommandLine.Option(names = "--priv", description = "Path of the private key file", required = true)
    String privPath;

    @CommandLine.Option(names = "--pub", description = "Path of the public key file", required = true)
    String pubPath;

    @CommandLine.Option(names = "--output", description = "Path of the certificate signing request file output", required = true)
    String output;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        try {
            Security.removeProvider("SunEC");
            String role = caCli.scanValue("certificate roles", Arrays.stream(CertificateRole.values()).map(Enum::name).toArray(String[]::new));
            String usage = caCli.scanValue("certificate usage", Arrays.stream(CertificateUsage.values()).map(Enum::name).toArray(String[]::new));
            String country = caCli.scanValue("country");
            String locality = caCli.scanValue("locality");
            String province = caCli.scanValue("province");
            String org = caCli.scanValue("organization name");
            String email = caCli.scanValue("email address");
            String cn = caCli.scanValue("common name");
            X500Name subject = caCli.buildRDN(org, CertificateRole.valueOf(role), country, province, locality, cn, email);

            String priv = FileUtils.readText(privPath);
            String pub = FileUtils.readText(pubPath);
            String password = caCli.scanValue("password of the key");
            PrivKey privKey = KeyGenUtils.decodePrivKeyWithRawPassword(priv, password);
            PubKey pubKey = KeyGenUtils.decodePubKey(pub);
            PublicKey publicKey = CertificateUtils.retrievePublicKey(pubKey);
            PrivateKey privateKey = CertificateUtils.retrievePrivateKey(privKey);
            String algorithm = Crypto.getAlgorithm(privKey.getAlgorithm()).name();

            PKCS10CertificationRequest csr = caCli.genCsr(CertificateUsage.valueOf(usage), algorithm, cn, subject, CertificateRole.valueOf(role), publicKey, privateKey);
            File outputFile = new File(output);
            if (!outputFile.exists()) {
                outputFile.getParentFile().mkdirs();
            }
            JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(new FileWriter(outputFile));
            jcaPEMWriter.writeObject(csr);
            jcaPEMWriter.close();
            System.out.println("create [" + output + "] success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

@CommandLine.Command(name = "crt", mixinStandardHelpOptions = true, header = "Create new certificate.")
class CACrt implements Runnable {

    @CommandLine.Option(names = "--csr", description = "Path of the certificate signing request file")
    String csrPath;

    @CommandLine.Option(names = "--days", required = true, description = "Days of certificate validity")
    int days;

    @CommandLine.Option(names = "--issuer-priv", required = true, description = "Path of the issuer private key file")
    String issuerPrivPath;

    @CommandLine.Option(names = "--issuer-crt", description = "Path of the issuer certificate file")
    String issuerCrtPath;

    @CommandLine.Option(names = "--output", required = true, description = "Path of the certificate file output")
    String output;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        try {
            Security.removeProvider("SunEC");
            PKCS10CertificationRequest csr = CertificateUtils.parseCertificationRequest(FileUtils.readText(csrPath));
            String issuerKey = FileUtils.readText(issuerPrivPath);
            String password = caCli.scanValue("password of the issuer");
            PrivKey issuerPrivKey = KeyGenUtils.decodePrivKeyWithRawPassword(issuerKey, password);
            PrivateKey issuerPrivateKey = CertificateUtils.retrievePrivateKey(issuerPrivKey);
            PubKey csrPubKey = CertificateUtils.resolvePubKey(csr);
            SignatureFunction signatureFunction = Crypto.getSignatureFunction(issuerPrivKey.getAlgorithm());
            byte[] testBytes = UUID.randomUUID().toString().getBytes();
            X509v3CertificateBuilder certificateBuilder;
            if (!signatureFunction.verify(signatureFunction.sign(issuerPrivKey, testBytes), csrPubKey, testBytes)) {
                String issuerCrt = FileUtils.readText(issuerCrtPath);
                X509Certificate signerCrt = CertificateUtils.parseCertificate(issuerCrt);
                CertificateUtils.checkCertificateRolesAny(signerCrt, CertificateRole.ROOT, CertificateRole.CA);
                certificateBuilder = new JcaX509v3CertificateBuilder(signerCrt, BigInteger.valueOf(new Random().nextInt() & 0x7fffffff), new Date(), new Date(System.currentTimeMillis() + days * 1000L * 24L * 60L * 60L), csr.getSubject(), CertificateUtils.retrievePublicKey(csrPubKey));
            } else {
                // 自签名证书
                certificateBuilder = new JcaX509v3CertificateBuilder(csr.getSubject(), BigInteger.valueOf(new Random().nextInt() & 0x7fffffff), new Date(), new Date(System.currentTimeMillis() + days * 1000L * 24L * 60L * 60L), csr.getSubject(), CertificateUtils.retrievePublicKey(csrPubKey));
            }

            Attribute[] attrs = csr.getAttributes();
            for (Attribute attr : attrs) {
                ASN1Encodable[] asn1Encodables = attr.getAttrValues().toArray();
                for (ASN1Encodable encodable : asn1Encodables) {
                    certificateBuilder.addExtension(Extension.create(attr.getAttrType(), true, encodable));
                }
            }

            String algorithm = Crypto.getAlgorithm(issuerPrivKey.getAlgorithm()).name();
            ContentSigner signer = new JcaContentSignerBuilder(caCli.CA_ALGORITHM_MAP.get(algorithm)).build(issuerPrivateKey);
            X509CertificateHolder holder = certificateBuilder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

            FileUtils.writeText(CertificateUtils.toPEMString(cert), new File(output));

            System.out.println("create [" + output + "] success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

@CommandLine.Command(name = "renew", mixinStandardHelpOptions = true, header = "Update validity period.")
class CARenew implements Runnable {

    @CommandLine.Option(names = "--crt", required = true, description = "File of the certificate")
    String crtPath;

    @CommandLine.Option(names = "--days", required = true, description = "Days of certificate validity")
    int days;

    @CommandLine.Option(names = "--issuer-priv", required = true, description = "Path of the issuer private key file")
    String issuerPrivPath;

    @CommandLine.Option(names = "--issuer-crt", required = true, description = "Path of the issuer certificate file")
    String issuerCrtPath;

    @CommandLine.Option(names = "--output", required = true, description = "Path of the new certificate file output")
    String output;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        try {
            Security.removeProvider("SunEC");
            X509Certificate originCrt = CertificateUtils.parseCertificate(FileUtils.readText(crtPath));
            X509CertificateHolder originHolder = new X509CertificateHolder(Certificate.getInstance(ASN1Primitive.fromByteArray(originCrt.getEncoded())));
            String issuerKey = FileUtils.readText(issuerPrivPath);
            String password = caCli.scanValue("password of the issuer");
            PrivKey issuerPrivKey = KeyGenUtils.decodePrivKeyWithRawPassword(issuerKey, password);
            PrivateKey issuerPrivateKey = CertificateUtils.retrievePrivateKey(issuerPrivKey);
            PubKey crtPubKey = CertificateUtils.resolvePubKey(originCrt);
            SignatureFunction signatureFunction = Crypto.getSignatureFunction(issuerPrivKey.getAlgorithm());
            byte[] testBytes = UUID.randomUUID().toString().getBytes();
            X509v3CertificateBuilder certificateBuilder;
            if (!signatureFunction.verify(signatureFunction.sign(issuerPrivKey, testBytes), crtPubKey, testBytes)) {
                String issuerCrt = FileUtils.readText(issuerCrtPath);
                X509Certificate signerCrt = CertificateUtils.parseCertificate(issuerCrt);
                CertificateUtils.checkCertificateRolesAny(signerCrt, CertificateRole.ROOT, CertificateRole.CA);
                certificateBuilder = new JcaX509v3CertificateBuilder(signerCrt, BigInteger.valueOf(new Random().nextInt() & 0x7fffffff), new Date(), new Date(System.currentTimeMillis() + days * 1000L * 24L * 60L * 60L), new X500Name(originCrt.getSubjectDN().getName()), CertificateUtils.retrievePublicKey(crtPubKey));
            } else {
                // 自签名证书
                certificateBuilder = new JcaX509v3CertificateBuilder(new X500Name(originCrt.getSubjectDN().getName()), BigInteger.valueOf(new Random().nextInt() & 0x7fffffff), new Date(), new Date(System.currentTimeMillis() + days * 1000L * 24L * 60L * 60L), new X500Name(originCrt.getSubjectDN().getName()), CertificateUtils.retrievePublicKey(crtPubKey));
            }

            Extensions extensions = originHolder.getExtensions();
            for (ASN1ObjectIdentifier id : extensions.getExtensionOIDs()) {
                certificateBuilder.addExtension(extensions.getExtension(id));
            }

            String algorithm = Crypto.getAlgorithm(issuerPrivKey.getAlgorithm()).name();
            ContentSigner signer = new JcaContentSignerBuilder(caCli.CA_ALGORITHM_MAP.get(algorithm)).build(issuerPrivateKey);
            X509CertificateHolder holder = certificateBuilder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

            FileUtils.writeText(CertificateUtils.toPEMString(cert), new File(output));

            System.out.println("renew [" + output + "] success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

@CommandLine.Command(name = "pkcs12", mixinStandardHelpOptions = true, header = "Output PKCS12 file.")
class CAPKCS12 implements Runnable {

    @CommandLine.Option(names = "--name", required = true, description = "Name for new keystore.")
    String name;

    @CommandLine.Option(names = "--crt", required = true, description = "File of the certificate")
    String crtPath;

    @CommandLine.Option(names = "--key", description = "Path of the private key file", required = true)
    String keyPath;

    @CommandLine.Option(names = "--issuer-crt", required = true, description = "Path of the issuer certificate file")
    String issuerCrtPath;

    @CommandLine.Option(names = "--output", required = true, description = "Path of the keystore file output")
    String output;

    @CommandLine.Option(names = "--trust", description = "Trust keystore file.")
    String trustKeyStore;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        try {
            Security.removeProvider("SunEC");
            String issuerCrt = FileUtils.readText(issuerCrtPath);
            X509Certificate signerCrt = CertificateUtils.parseCertificate(issuerCrt);
            String signerPub = CertificateUtils.resolvePubKey(signerCrt).toString();
            String crtPem = FileUtils.readText(crtPath);
            X509Certificate crt = CertificateUtils.parseCertificate(crtPem);
            PubKey pubKey = CertificateUtils.resolvePubKey(crt);
            String priv = FileUtils.readText(keyPath);
            PrivKey privKey = CertificateUtils.parsePrivKey(pubKey.getAlgorithm(), priv);
            PrivateKey privateKey = CertificateUtils.retrievePrivateKey(privKey, pubKey);

            String password = caCli.scanValue("password for generated keystore");
            caCli.keyStore(privateKey, name, password, crt, signerCrt);
            if (!StringUtils.isEmpty(trustKeyStore)) {
                password = caCli.scanValue("password for truststore");
                caCli.trustStore(new File(trustKeyStore), name, password, crt);
                caCli.trustStore(new File(trustKeyStore), signerPub, password, signerCrt);
            }
            System.out.println("create keystore [" + output + "] success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

@CommandLine.Command(name = "gm-pkcs12", mixinStandardHelpOptions = true, header = "Output GM PKCS12 file.")
class CAGMPKCS12 implements Runnable {

    @CommandLine.Option(names = "--name", required = true, description = "Name for new keystore.")
    String name;

    @CommandLine.Option(names = "--enc-crt", required = true, description = "File of the encrypt certificate")
    String encCrtPath;

    @CommandLine.Option(names = "--sign-crt", required = true, description = "File of the signature certificate")
    String signCrtPath;

    @CommandLine.Option(names = "--enc-key", description = "Path of the encrypt private key file", required = true)
    String encKeyPath;

    @CommandLine.Option(names = "--sign-key", description = "Path of the signature private key file", required = true)
    String signKeyPath;

    @CommandLine.Option(names = "--issuer-crt", required = true, description = "Path of the issuer certificate file")
    String issuerCrtPath;

    @CommandLine.Option(names = "--output", required = true, description = "Path of the keystore file output")
    String output;

    @CommandLine.Option(names = "--trust", description = "Trust keystore file.")
    String trustKeyStore;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        try {
            Security.removeProvider("SunEC");

            String issuerCrt = FileUtils.readText(issuerCrtPath);
            X509Certificate signerCrt = CertificateUtils.parseCertificate(issuerCrt);
            String signerPub = CertificateUtils.resolvePubKey(signerCrt).toString();

            String encCrtPem = FileUtils.readText(encCrtPath);
            X509Certificate enccrt = CertificateUtils.parseCertificate(encCrtPem);
            String encPriv = FileUtils.readText(encKeyPath);
            PubKey encPubKey = CertificateUtils.resolvePubKey(enccrt);
            PrivKey encPrivKey = CertificateUtils.parsePrivKey(encPubKey.getAlgorithm(), encPriv);
            PrivateKey encPrivateKey = CertificateUtils.retrievePrivateKey(encPrivKey, encPubKey);

            String signCrtPem = FileUtils.readText(signCrtPath);
            X509Certificate signcrt = CertificateUtils.parseCertificate(signCrtPem);
            String signPriv = FileUtils.readText(signKeyPath);
            PubKey signPubKey = CertificateUtils.resolvePubKey(signcrt);
            PrivKey signPrivKey = CertificateUtils.parsePrivKey(signPubKey.getAlgorithm(), signPriv);
            PrivateKey signPrivateKey = CertificateUtils.retrievePrivateKey(signPrivKey, signPubKey);

            String password = caCli.scanValue("password for generated keystore");

            caCli.keyStore(encPrivateKey, name + ".enc", password, enccrt, signerCrt);
            caCli.keyStore(signPrivateKey, name + ".sign", password, signcrt, signerCrt);

            caCli.doubleKeysStore(name, signPrivateKey, encPrivateKey, password, signcrt, enccrt, signerCrt);

            if (!StringUtils.isEmpty(trustKeyStore)) {
                password = caCli.scanValue("password for truststore");
                caCli.trustStore(new File(trustKeyStore), name + ".enc", password, enccrt);
                caCli.trustStore(new File(trustKeyStore), name + ".sign", password, signcrt);
                caCli.trustStore(new File(trustKeyStore), signerPub, password, signerCrt);
            }
            System.out.println("create keystore [" + output + "] success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

@CommandLine.Command(name = "test", mixinStandardHelpOptions = true, header = "Create certificates for a testnet.")
class CATest implements Runnable {

    @CommandLine.Option(names = {"-a", "--algorithm"}, required = true, description = "Crypto algorithm. defaultValue: ED25519.", defaultValue = "ED25519")
    String algorithm;

    @CommandLine.Option(names = "--nodes", required = true, description = "Node size", defaultValue = "4")
    int nodes;

    @CommandLine.Option(names = "--gws", required = true, description = "Gateway size", defaultValue = "1")
    int gws;

    @CommandLine.Option(names = "--users", description = "Available user size", defaultValue = "10")
    int users;

    @CommandLine.Option(names = {"-p", "--password"}, description = "Password of the key")
    String password;

    @CommandLine.Option(names = "--org", required = true, description = "Organization name")
    String organization;

    @CommandLine.Option(names = "--country", required = true, description = "Country")
    String country;

    @CommandLine.Option(names = "--locality", required = true, description = "Locality")
    String locality;

    @CommandLine.Option(names = "--province", required = true, description = "Province")
    String province;

    @CommandLine.Option(names = "--email", required = true, description = "Email address")
    String email;

    @CommandLine.Option(names = "--node-ips", split = ",", description = "node ip list for tls certificates", defaultValue = "127.0.0.1")
    String[] nodeIPs;

    @CommandLine.Option(names = "--gw-ips", split = ",", description = "gw ip list for tls certificates", defaultValue = "127.0.0.1")
    String[] gwIPs;

    @CommandLine.Option(names = "--user-ips", split = ",", description = "user ip list for tls certificates", defaultValue = "127.0.0.1")
    String[] userIPs;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        File caHome = new File(caCli.getCaHome());
        if (!caHome.exists()) {
            caHome.mkdirs();
        }

        try {
            Security.removeProvider("SunEC");

            if (StringUtils.isEmpty(password)) {
                password = caCli.scanValue("password for all private keys");
            }

            PrivKey issuerPrivKey = null;
            PrivateKey issuerPrivateKey = null;
            X509Certificate issuerCrt = null;
            File trustStoreFile = new File(caCli.getTlsHome() + File.separator + "trust.jks");
            for (int i = 0; i < nodes + users + gws + 1; i++) {
                String name;
                CertificateRole ou;
                if (i == 0) {
                    name = "root";
                    ou = CertificateRole.ROOT;
                } else if (i <= nodes) {
                    name = "peer" + (i - 1);
                    ou = CertificateRole.PEER;
                } else if (i <= nodes + gws) {
                    name = "gw" + (i - nodes - 1);
                    ou = CertificateRole.GW;
                } else {
                    name = "user" + (i - nodes - gws - 1);
                    ou = CertificateRole.USER;
                }
                algorithm = algorithm.toUpperCase();
                AsymmetricKeypair keypair = Crypto.getSignatureFunction(algorithm).generateKeypair();
                String pubkey = KeyGenUtils.encodePubKey(keypair.getPubKey());
                String base58pwd = KeyGenUtils.encodePasswordAsBase58(password);
                String privkey = KeyGenUtils.encodePrivKey(keypair.getPrivKey(), base58pwd);
                FileUtils.writeText(pubkey, new File(caCli.getKeysHome() + File.separator + name + ".pub"));
                FileUtils.writeText(privkey, new File(caCli.getKeysHome() + File.separator + name + ".priv"));
                FileUtils.writeText(base58pwd, new File(caCli.getKeysHome() + File.separator + name + ".pwd"));

                if (i == 0) {
                    issuerPrivKey = keypair.getPrivKey();
                    issuerPrivateKey = CertificateUtils.retrievePrivateKey(issuerPrivKey);
                }

                X500Name subject = caCli.buildRDN(organization, ou, country, province, locality, name, email);
                X509Certificate certificate = caCli.genCert(CertificateUsage.SIGN, algorithm, name, subject, ou, CertificateUtils.retrievePublicKey(keypair.getPubKey()), issuerPrivateKey, issuerCrt);
                if (i == 0) {
                    FileUtils.writeText(CertificateUtils.toPEMString(certificate), new File(caCli.getCaHome() + File.separator + name + ".crt"));
                    issuerCrt = certificate;
                    caCli.trustStore(trustStoreFile, name, password, certificate);
                } else {
                    FileUtils.writeText(CertificateUtils.toPEMString(certificate), new File(caCli.getSignHome() + File.separator + name + ".crt"));
                    String ip = "127.0.0.1";
                    switch (ou) {
                        case PEER:
                            if (nodeIPs.length >= i) {
                                ip = nodeIPs[i - 1];
                            }
                            break;
                        case GW:
                            if (gwIPs.length >= i - nodes) {
                                ip = gwIPs[i - nodes - 1];
                            }
                            break;
                        case USER:
                            if (gwIPs.length >= i - nodes - gws) {
                                ip = userIPs[i - nodes - gws - 1];
                            }
                            break;
                        default:
                            break;
                    }

                    PrivateKey privateKey = CertificateUtils.retrievePrivateKey(keypair.getPrivKey(), keypair.getPubKey());
                    FileUtils.writeText(CertificateUtils.toPEMString(algorithm, privateKey), new File(caCli.getKeysHome() + File.separator + name + ".key"));

                    if (!algorithm.equalsIgnoreCase("SM2")) {
                        subject = caCli.buildRDN(organization, ou, country, province, locality, ip, email);
                        X509Certificate tlsCertificate = caCli.genCert(CertificateUsage.TLS, algorithm, ip, subject, ou, CertificateUtils.retrievePublicKey(keypair.getPubKey()), issuerPrivateKey, issuerCrt);
                        FileUtils.writeText(CertificateUtils.toPEMString(tlsCertificate), new File(caCli.getTlsHome() + File.separator + name + ".crt"));
                        caCli.keyStore(privateKey, name, password, tlsCertificate, issuerCrt);

                        caCli.trustStore(trustStoreFile, name, password, tlsCertificate);
                    } else {
                        AsymmetricKeypair signKeypair = Crypto.getSignatureFunction(algorithm).generateKeypair();
                        subject = caCli.buildRDN(organization, ou, country, province, locality, ip, email);
                        X509Certificate signCertificate = caCli.genCert(CertificateUsage.TLS_SIGN, algorithm, ip, subject, ou, CertificateUtils.retrievePublicKey(signKeypair.getPubKey()), issuerPrivateKey, issuerCrt);
                        FileUtils.writeText(CertificateUtils.toPEMString(signCertificate), new File(caCli.getTlsHome() + File.separator + name + ".sign.crt"));
                        PrivateKey signPrivateKey = CertificateUtils.retrievePrivateKey(signKeypair.getPrivKey(), signKeypair.getPubKey());
                        FileUtils.writeText(CertificateUtils.toPEMString(signPrivateKey), new File(caCli.getKeysHome() + File.separator + name + ".sign.key"));
                        caCli.keyStore(signPrivateKey, name + ".sign", password, signCertificate, issuerCrt);

                        AsymmetricKeypair encKeypair = Crypto.getSignatureFunction(algorithm).generateKeypair();
                        X509Certificate encCertificate = caCli.genCert(CertificateUsage.TLS_ENC, algorithm, ip, subject, ou, CertificateUtils.retrievePublicKey(encKeypair.getPubKey()), issuerPrivateKey, issuerCrt);
                        FileUtils.writeText(CertificateUtils.toPEMString(encCertificate), new File(caCli.getTlsHome() + File.separator + name + ".enc.crt"));
                        PrivateKey encPrivateKey = CertificateUtils.retrievePrivateKey(encKeypair.getPrivKey(), encKeypair.getPubKey());
                        FileUtils.writeText(CertificateUtils.toPEMString(encPrivateKey), new File(caCli.getKeysHome() + File.separator + name + ".enc.key"));
                        caCli.keyStore(encPrivateKey, name + ".enc", password, encCertificate, issuerCrt);

                        caCli.doubleKeysStore(name, signPrivateKey, encPrivateKey, password, signCertificate, encCertificate, issuerCrt);
                        caCli.trustStore(trustStoreFile, name + ".sign", password, signCertificate);
                        caCli.trustStore(trustStoreFile, name + ".enc", password, encCertificate);
                    }
                }
            }

            System.out.println("create test certificates in [" + caCli.getCaHome() + "] success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

@CommandLine.Command(name = "test-plus", mixinStandardHelpOptions = true, header = "Create certificates for an existing testnet.")
class CATestPlus implements Runnable {
    @CommandLine.Option(names = "--name", required = true, description = "Name for new certificate.")
    String name;

    @CommandLine.Option(names = "--issuer-priv", required = true, description = "Path of the issuer private key file")
    String issuerPrivPath;

    @CommandLine.Option(names = "--issuer-crt", required = true, description = "Path of the issuer certificate file")
    String issuerCrtPath;

    @CommandLine.Option(names = "--issuer-password", required = true, description = "Password of issuer password")
    String issuerPassword;

    @CommandLine.Option(names = "--trust", required = true, description = "Trust keystore file.")
    String trustKeyStore;

    @CommandLine.Option(names = "--trust-password", required = true, description = "Trust keystore password.")
    String trustKeyStorePassword;

    @CommandLine.Option(names = {"-p", "--password"}, description = "Password of the key")
    String password;

    @CommandLine.Option(names = "--org", required = true, description = "Organization name")
    String organization;

    @CommandLine.Option(names = "--country", required = true, description = "Country")
    String country;

    @CommandLine.Option(names = "--locality", required = true, description = "Locality")
    String locality;

    @CommandLine.Option(names = "--province", required = true, description = "Province")
    String province;

    @CommandLine.Option(names = "--email", required = true, description = "Email address")
    String email;

    @CommandLine.Option(names = "--role", required = true, description = "Certificate Role: PEER/GW/USER")
    CertificateRole role;

    @CommandLine.Option(names = "--ip", description = "ip for tls certificates", defaultValue = "127.0.0.1")
    String ip;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        try {
            Security.removeProvider("SunEC");

            if (StringUtils.isEmpty(password)) {
                password = caCli.scanValue("password for all private keys");
            }

            String issuerCrt = FileUtils.readText(issuerCrtPath);
            X509Certificate signerCrt = CertificateUtils.parseCertificate(issuerCrt);
            String issuerKey = FileUtils.readText(issuerPrivPath);
            PrivKey issuerPrivKey = KeyGenUtils.decodePrivKeyWithRawPassword(issuerKey, issuerPassword);
            PrivateKey issuerPrivateKey = CertificateUtils.retrievePrivateKey(issuerPrivKey);
            String algorithm = Crypto.getAlgorithm(issuerPrivKey.getAlgorithm()).name();

            AsymmetricKeypair keypair = Crypto.getSignatureFunction(algorithm).generateKeypair();
            String pubkey = KeyGenUtils.encodePubKey(keypair.getPubKey());
            String base58pwd = KeyGenUtils.encodePasswordAsBase58(password);
            String privkey = KeyGenUtils.encodePrivKey(keypair.getPrivKey(), base58pwd);
            FileUtils.writeText(pubkey, new File(caCli.getKeysHome() + File.separator + name + ".pub"));
            FileUtils.writeText(privkey, new File(caCli.getKeysHome() + File.separator + name + ".priv"));
            FileUtils.writeText(base58pwd, new File(caCli.getKeysHome() + File.separator + name + ".pwd"));

            X500Name subject = caCli.buildRDN(organization, role, country, province, locality, name, email);
            X509Certificate certificate = caCli.genCert(CertificateUsage.SIGN, algorithm, name, subject, role, CertificateUtils.retrievePublicKey(keypair.getPubKey()), issuerPrivateKey, signerCrt);
            FileUtils.writeText(CertificateUtils.toPEMString(certificate), new File(caCli.getSignHome() + File.separator + name + ".crt"));

            PrivateKey privateKey = CertificateUtils.retrievePrivateKey(keypair.getPrivKey(), keypair.getPubKey());
            FileUtils.writeText(CertificateUtils.toPEMString(algorithm, privateKey), new File(caCli.getKeysHome() + File.separator + name + ".key"));

            File trustStoreFile = new File(trustKeyStore);
            if (!algorithm.equalsIgnoreCase("SM2")) {
                subject = caCli.buildRDN(organization, role, country, province, locality, ip, email);
                X509Certificate tlsCertificate = caCli.genCert(CertificateUsage.TLS, algorithm, ip, subject, role, CertificateUtils.retrievePublicKey(keypair.getPubKey()), issuerPrivateKey, signerCrt);
                FileUtils.writeText(CertificateUtils.toPEMString(tlsCertificate), new File(caCli.getTlsHome() + File.separator + name + ".crt"));
                caCli.keyStore(privateKey, name, password, tlsCertificate, signerCrt);

                caCli.trustStore(trustStoreFile, name, trustKeyStorePassword, tlsCertificate);
            } else {
                AsymmetricKeypair signKeypair = Crypto.getSignatureFunction(algorithm).generateKeypair();
                subject = caCli.buildRDN(organization, role, country, province, locality, ip, email);
                X509Certificate signCertificate = caCli.genCert(CertificateUsage.TLS_SIGN, algorithm, ip, subject, role, CertificateUtils.retrievePublicKey(signKeypair.getPubKey()), issuerPrivateKey, signerCrt);
                FileUtils.writeText(CertificateUtils.toPEMString(signCertificate), new File(caCli.getTlsHome() + File.separator + name + ".sign.crt"));
                PrivateKey signPrivateKey = CertificateUtils.retrievePrivateKey(signKeypair.getPrivKey(), signKeypair.getPubKey());
                FileUtils.writeText(CertificateUtils.toPEMString(signPrivateKey), new File(caCli.getKeysHome() + File.separator + name + ".sign.key"));
                caCli.keyStore(signPrivateKey, name + ".sign", password, signCertificate, signerCrt);

                AsymmetricKeypair encKeypair = Crypto.getSignatureFunction(algorithm).generateKeypair();
                X509Certificate encCertificate = caCli.genCert(CertificateUsage.TLS_ENC, algorithm, ip, subject, role, CertificateUtils.retrievePublicKey(encKeypair.getPubKey()), issuerPrivateKey, signerCrt);
                FileUtils.writeText(CertificateUtils.toPEMString(encCertificate), new File(caCli.getTlsHome() + File.separator + name + ".enc.crt"));
                PrivateKey encPrivateKey = CertificateUtils.retrievePrivateKey(encKeypair.getPrivKey(), encKeypair.getPubKey());
                FileUtils.writeText(CertificateUtils.toPEMString(encPrivateKey), new File(caCli.getKeysHome() + File.separator + name + ".enc.key"));
                caCli.keyStore(encPrivateKey, name + ".enc", password, encCertificate, signerCrt);

                caCli.doubleKeysStore(name, signPrivateKey, encPrivateKey, password, signCertificate, encCertificate, signerCrt);
                caCli.trustStore(trustStoreFile, name + ".sign", trustKeyStorePassword, signCertificate);
                caCli.trustStore(trustStoreFile, name + ".enc", trustKeyStorePassword, encCertificate);
            }

            System.out.println("create test certificates in [" + caCli.getCaHome() + "] success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
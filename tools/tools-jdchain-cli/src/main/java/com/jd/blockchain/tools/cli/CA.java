package com.jd.blockchain.tools.cli;

import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateUsage;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureFunction;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
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

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * @description: JD Chain certificate management
 * @author: imuge
 * @date: 2021/9/1
 **/
@CommandLine.Command(name = "ca",
        mixinStandardHelpOptions = true,
        showDefaultValues = true,
        description = "List, create, update certificates.",
        subcommands = {
                CAShow.class,
                CACsr.class,
                CACrt.class,
                CARenew.class,
                CATest.class,
                CommandLine.HelpCommand.class
        }
)

public class CA implements Runnable {
    static final String CA_HOME = "certs";
    static final String CA_SIGN_HOME = "certs/sign";
    static final String CA_KEYS_HOME = "certs/keys";
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
            File caHome = new File(jdChainCli.path.getCanonicalPath() + File.separator + CA_KEYS_HOME);
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
}

@CommandLine.Command(name = "show", mixinStandardHelpOptions = true, header = "Show certificate.")
class CAShow implements Runnable {

    @CommandLine.Option(required = true, names = "--cert", description = "Path of the certificate file")
    String cert;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
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
            String[] roles = caCli.scanValues("certificate roles", Arrays.stream(CertificateRole.values()).map(Enum::name).toArray(String[]::new));
            String usage = caCli.scanValue("certificate usage", Arrays.stream(CertificateUsage.values()).map(Enum::name).toArray(String[]::new));
            String country = caCli.scanValue("country");
            String locality = caCli.scanValue("locality");
            String province = caCli.scanValue("province");
            String org = caCli.scanValue("organization name");
            String email = caCli.scanValue("email address");
            String cn = caCli.scanValue("common name");

            X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
            nameBuilder.addRDN(BCStyle.O, org);
            boolean isCa = false;
            for (String ou : roles) {
                if (ou.equals(CertificateRole.ROOT.name()) || ou.equals(CertificateRole.CA.name())) {
                    isCa = true;
                }
                nameBuilder.addRDN(BCStyle.OU, ou);
            }
            nameBuilder.addRDN(BCStyle.C, country);
            nameBuilder.addRDN(BCStyle.ST, locality);
            nameBuilder.addRDN(BCStyle.L, province);
            nameBuilder.addRDN(BCStyle.CN, cn);
            nameBuilder.addRDN(BCStyle.EmailAddress, email);
            X500Name subject = nameBuilder.build();
            String priv = FileUtils.readText(privPath);
            String pub = FileUtils.readText(pubPath);
            String password = caCli.scanValue("password of the key");
            PrivKey privKey = KeyGenUtils.decodePrivKeyWithRawPassword(priv, password);
            PubKey pubKey = KeyGenUtils.decodePubKey(pub);
            PrivateKey privateKey = CertificateUtils.retrievePrivateKey(privKey);
            String algorithm = Crypto.getAlgorithm(privKey.getAlgorithm()).name();
            String keyFile = caCli.getKeysHome() + File.separator + output + ".key";
            FileUtils.writeText(CertificateUtils.toPEMString(algorithm, privateKey), new File(keyFile));
            ContentSigner signGen = new JcaContentSignerBuilder(caCli.CA_ALGORITHM_MAP.get(algorithm)).build(privateKey);
            PublicKey publicKey = CertificateUtils.retrievePublicKey(pubKey);
            PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(subject, publicKey);
            boolean forSign = CertificateUsage.SIGN.name().equals(usage);
            if (isCa) {
                p10Builder.setAttribute(Extension.keyUsage, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            } else if (forSign) {
                p10Builder.setAttribute(Extension.keyUsage, new KeyUsage(KeyUsage.digitalSignature));
            } else {
                p10Builder.setAttribute(Extension.keyUsage, new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.digitalSignature));
                p10Builder.setAttribute(Extension.extendedKeyUsage, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
            }
            p10Builder.setAttribute(Extension.basicConstraints, new BasicConstraints(isCa));
            p10Builder.setAttribute(Extension.subjectKeyIdentifier, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey));

            PKCS10CertificationRequest csr = p10Builder.build(signGen);
            JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(new FileWriter(output));
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

    @CommandLine.Option(names = "--issuer-crt", required = true, description = "Path of the issuer certificate file")
    String issuerCrtPath;

    @CommandLine.Option(names = "--output", required = true, description = "Path of the certificate file output")
    String output;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        try {
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
                certificateBuilder = new JcaX509v3CertificateBuilder(
                        signerCrt,
                        BigInteger.valueOf(new Random().nextInt() & 0x7fffffff),
                        new Date(),
                        new Date(System.currentTimeMillis() + days * 1000L * 24L * 60L * 60L),
                        csr.getSubject(),
                        CertificateUtils.retrievePublicKey(csrPubKey)
                );
            } else {
                // 自签名证书
                certificateBuilder = new JcaX509v3CertificateBuilder(
                        csr.getSubject(),
                        BigInteger.valueOf(new Random().nextInt() & 0x7fffffff),
                        new Date(),
                        new Date(System.currentTimeMillis() + days * 1000L * 24L * 60L * 60L),
                        csr.getSubject(),
                        CertificateUtils.retrievePublicKey(csrPubKey)
                );
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
                certificateBuilder = new JcaX509v3CertificateBuilder(
                        signerCrt,
                        BigInteger.valueOf(new Random().nextInt() & 0x7fffffff),
                        new Date(),
                        new Date(System.currentTimeMillis() + days * 1000L * 24L * 60L * 60L),
                        new X500Name(originCrt.getSubjectDN().getName()),
                        CertificateUtils.retrievePublicKey(crtPubKey)
                );
            } else {
                // 自签名证书
                certificateBuilder = new JcaX509v3CertificateBuilder(
                        new X500Name(originCrt.getSubjectDN().getName()),
                        BigInteger.valueOf(new Random().nextInt() & 0x7fffffff),
                        new Date(),
                        new Date(System.currentTimeMillis() + days * 1000L * 24L * 60L * 60L),
                        new X500Name(originCrt.getSubjectDN().getName()),
                        CertificateUtils.retrievePublicKey(crtPubKey)
                );
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

@CommandLine.Command(name = "test", mixinStandardHelpOptions = true, header = "Create certificates for a testnet.")
class CATest implements Runnable {

    @CommandLine.Option(names = {"-a", "--algorithm"}, required = true, description = "Crypto algorithm", defaultValue = "ED25519")
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

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        File caHome = new File(caCli.getCaHome());
        if (!caHome.exists()) {
            caHome.mkdirs();
        }

        try {
            if (StringUtils.isEmpty(password)) {
                password = caCli.scanValue("password for all private keys");
            }
            // 初始化公私钥对 root,peer[0~nodes-1],user[1~users]
            PrivKey issuerPrivKey = null;
            X509Certificate issuerCrt = null;
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
                    name = "gw" + (i - nodes);
                    ou = CertificateRole.GW;
                } else {
                    name = "user" + (i - nodes - gws);
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
                FileUtils.writeText(CertificateUtils.toPEMString(algorithm, CertificateUtils.retrievePrivateKey(keypair.getPrivKey(), keypair.getPubKey())), new File(caCli.getKeysHome() + File.separator + name + ".key"));

                if (i == 0) {
                    issuerPrivKey = keypair.getPrivKey();
                }
                X509Certificate certificate = genCert(CertificateUsage.SIGN, name, ou, CertificateUtils.retrievePublicKey(keypair.getPubKey()), CertificateUtils.retrievePrivateKey(issuerPrivKey), issuerCrt);
                FileUtils.writeText(CertificateUtils.toPEMString(certificate), new File(caCli.getSignHome() + File.separator + name + ".crt"));
                if (i == 0) {
                    issuerCrt = certificate;
                } else {
                    certificate = genCert(CertificateUsage.TLS, "127.0.0.1", ou, CertificateUtils.retrievePublicKey(keypair.getPubKey()), CertificateUtils.retrievePrivateKey(issuerPrivKey), issuerCrt);
                    FileUtils.writeText(CertificateUtils.toPEMString(certificate), new File(caCli.getTlsHome() + File.separator + name + ".crt"));
                }
            }

            System.out.println("create test certificates in [" + caCli.getCaHome() + "] success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private X509Certificate genCert(CertificateUsage usage, String name, CertificateRole ou, PublicKey publicKey, PrivateKey issuerPrivateKey, X509Certificate issuerCrt) throws Exception {
        boolean isCa = ou.equals(CertificateRole.ROOT) || ou.equals(CertificateRole.CA);
        boolean forSign = usage.equals(CertificateUsage.SIGN);
        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.O, organization);
        nameBuilder.addRDN(BCStyle.OU, ou.name());
        nameBuilder.addRDN(BCStyle.C, country);
        nameBuilder.addRDN(BCStyle.ST, province);
        nameBuilder.addRDN(BCStyle.L, locality);
        nameBuilder.addRDN(BCStyle.CN, name);
        nameBuilder.addRDN(BCStyle.EmailAddress, email);
        X500Name subject = nameBuilder.build();
        X509v3CertificateBuilder certificateBuilder;
        if (null != issuerCrt) {
            certificateBuilder = new JcaX509v3CertificateBuilder(
                    issuerCrt,
                    BigInteger.valueOf(new Random().nextInt() & 0x7fffffff),
                    new Date(),
                    new Date(System.currentTimeMillis() + 3650 * 1000L * 24L * 60L * 60L),
                    subject,
                    publicKey
            );
        } else {
            certificateBuilder = new JcaX509v3CertificateBuilder(
                    subject,
                    BigInteger.valueOf(new Random().nextInt() & 0x7fffffff),
                    new Date(),
                    new Date(System.currentTimeMillis() + 3650 * 1000L * 24L * 60L * 60L),
                    subject,
                    publicKey
            );
        }
        if (isCa) {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        } else if (forSign) {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        } else {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.keyAgreement | KeyUsage.digitalSignature));
            certificateBuilder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
        }
        certificateBuilder.addExtension(Extension.subjectKeyIdentifier, true, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(publicKey));
        certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCa));

        ContentSigner signer = new JcaContentSignerBuilder(caCli.CA_ALGORITHM_MAP.get(algorithm.toUpperCase())).build(issuerPrivateKey);
        X509CertificateHolder holder = certificateBuilder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }
}

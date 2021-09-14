package com.jd.blockchain.tools.cli;

import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureFunction;
import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
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
import java.util.Scanner;
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
                CAList.class,
                CAShow.class,
                CACsr.class,
                CACrt.class,
                CARenew.class,
                CATest.class,
                CommandLine.HelpCommand.class
        }
)

public class CA implements Runnable {
    static final String CA_HOME = "config/keys";
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
            return jdChainCli.path.getCanonicalPath() + File.separator + CA_HOME;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected String scanValue(String category) {
        Scanner scanner = new Scanner(System.in).useDelimiter("\n");
        System.out.println("input " + category + ": ");
        System.out.print("> ");
        return scanner.next();
    }

    protected String scanValue(String category, String[] values) {
        Scanner scanner = new Scanner(System.in).useDelimiter("\n");
        System.out.print("input " + category + " (");
        for (int i = 0; i < values.length; i++) {
            System.out.print(i + " for " + values[i]);
            if (i < values.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("): ");
        System.out.print("> ");
        while (true) {
            try {
                int index = Integer.parseInt(scanner.next());
                return values[index];
            } catch (Exception e) {
                System.err.print("invalid value");
            }
        }
    }

    protected String[] scanValues(String category, String[] values) {
        Scanner scanner = new Scanner(System.in).useDelimiter("\n");
        System.out.print("input " + category + " (");
        for (int i = 0; i < values.length; i++) {
            System.out.print(i + " for " + values[i]);
            if (i < values.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.println(". multi values use ',' split): ");
        System.out.print("> ");
        while (true) {
            try {
                String[] indexes = scanner.next().split(",");
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
}

@CommandLine.Command(name = "list", mixinStandardHelpOptions = true, header = "List all the certificates.")
class CAList implements Runnable {

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        File caHome = new File(caCli.getCaHome());
        if (!caHome.exists()) {
            caHome.mkdirs();
        }
        File[] certs = caHome.listFiles((dir, name) -> {
            if (name.endsWith(".crt")) {
                return true;
            }
            return false;
        });
        System.out.printf(caCli.CA_LIST_FORMAT, "NAME", "ALGORITHM", "ROLE", "PUBKEY");
        Arrays.stream(certs).forEach(cert -> {
            try {
                String name = FilenameUtils.removeExtension(cert.getName());
                X509Certificate certificate = CertificateUtils.parseCertificate(FileUtils.readText(cert));
                PubKey pubKey = CertificateUtils.resolvePubKey(certificate);
                Set<String> ous = CertificateUtils.getSubject(certificate, BCStyle.OU);
                System.out.printf(caCli.CA_LIST_FORMAT, name, Crypto.getAlgorithm(pubKey.getAlgorithm()).name(), ous, pubKey);
            } catch (Exception e) {
                System.err.print("error certificate: " + cert);
            }
        });
    }
}

@CommandLine.Command(name = "show", mixinStandardHelpOptions = true, header = "Show certificate.")
class CAShow implements Runnable {

    @CommandLine.Option(required = true, names = {"-n", "--name"}, description = "Name of the certificate")
    String name;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        File caHome = new File(caCli.getCaHome());
        if (!caHome.exists()) {
            caHome.mkdirs();
        }
        File[] crts = caHome.listFiles((dir, name) -> {
            if (name.equals(this.name + ".crt")) {
                return true;
            }
            return false;
        });
        if (null != crts && crts.length > 0) {
            X509Certificate certificate = CertificateUtils.parseCertificate(FileUtils.readText(new File(caHome + File.separator + name + ".crt")));
            System.out.printf(caCli.CA_LIST_FORMAT, "NAME", "ALGORITHM", "ROLE", "PUBKEY");
            PubKey pubKey = CertificateUtils.resolvePubKey(certificate);
            Set<String> ous = CertificateUtils.getSubject(certificate, BCStyle.OU);
            System.out.printf(caCli.CA_LIST_FORMAT, name, Crypto.getAlgorithm(pubKey.getAlgorithm()).name(), ous, pubKey);
            System.out.println(certificate.toString());
        } else {
            System.err.println("[" + name + "] not exists");
        }
    }
}

@CommandLine.Command(name = "csr", mixinStandardHelpOptions = true, header = "Create certificate signing request.")
class CACsr implements Runnable {

    @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the key")
    String name;

    @CommandLine.Option(names = "--priv", description = "Path of the private key file")
    String privPath;

    @CommandLine.Option(names = "--pub", description = "Path of the public key file")
    String pubPath;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        try {
            File caHome = new File(caCli.getCaHome());
            if (!caHome.exists()) {
                caHome.mkdirs();
            }
            if (StringUtils.isEmpty(name) && StringUtils.isEmpty(privPath) && StringUtils.isEmpty(pubPath)) {
                System.err.println("name and key cannot be empty at the same time");
            } else {
                String[] roles = caCli.scanValues("certificate roles", Arrays.stream(CertificateRole.values()).map(Enum::name).toArray(String[]::new));
                String country = caCli.scanValue("country");
                String locality = caCli.scanValue("locality");
                String province = caCli.scanValue("province");
                String org = caCli.scanValue("organization name");
                String email = caCli.scanValue("email address");

                X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
                nameBuilder.addRDN(BCStyle.O, org);
                for (String ou : roles) {
                    nameBuilder.addRDN(BCStyle.OU, ou);
                }
                nameBuilder.addRDN(BCStyle.C, country);
                nameBuilder.addRDN(BCStyle.ST, locality);
                nameBuilder.addRDN(BCStyle.L, province);
                nameBuilder.addRDN(BCStyle.CN, name);
                nameBuilder.addRDN(BCStyle.EmailAddress, email);
                X500Name subject = nameBuilder.build();
                String priv = !StringUtils.isEmpty(name) ? FileUtils.readText(caHome + File.separator + name + ".priv") : FileUtils.readText(privPath);
                String pub = !StringUtils.isEmpty(name) ? FileUtils.readText(caHome + File.separator + name + ".pub") : FileUtils.readText(pubPath);
                name = !StringUtils.isEmpty(name) ? name : FilenameUtils.removeExtension(new File(privPath).getName());
                String password = caCli.scanValue("password of the key");
                PrivKey privKey = KeyGenUtils.decodePrivKeyWithRawPassword(priv, password);
                PubKey pubKey = KeyGenUtils.decodePubKey(pub);
                PrivateKey privateKey = CertificateUtils.retrievePrivateKey(privKey);
                String algorithm = Crypto.getAlgorithm(privKey.getAlgorithm()).name();
                ContentSigner signGen = new JcaContentSignerBuilder(caCli.CA_ALGORITHM_MAP.get(algorithm)).build(privateKey);
                PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(subject, CertificateUtils.retrievePublicKey(pubKey));
                PKCS10CertificationRequest csr = p10Builder.build(signGen);
                String csrFile = caHome + File.separator + name + ".csr";
                JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(new FileWriter(csrFile));
                jcaPEMWriter.writeObject(csr);
                jcaPEMWriter.close();
                System.out.println("create [" + csrFile + "] success");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

@CommandLine.Command(name = "crt", mixinStandardHelpOptions = true, header = "Create new certificate.")
class CACrt implements Runnable {

    @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the certificate signing request file")
    String name;

    @CommandLine.Option(names = "--csr", description = "Path of the certificate signing request file")
    String csrPath;

    @CommandLine.Option(names = "--days", required = true, description = "Days of certificate validity")
    int days;

    @CommandLine.Option(names = "--issuer-name", description = "Name of the issuer key")
    String issuerName;

    @CommandLine.Option(names = "--issuer-priv", description = "Path of the issuer private key file")
    String issuerPrivPath;

    @CommandLine.Option(names = "--issuer-crt", description = "Path of the issuer certificate file")
    String issuerCrtPath;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        File caHome = new File(caCli.getCaHome());
        if (!caHome.exists()) {
            caHome.mkdirs();
        }
        if (StringUtils.isEmpty(name) && StringUtils.isEmpty(csrPath)) {
            System.err.println("csr name and csr path cannot be empty at the same time");
            return;
        }
        if (StringUtils.isEmpty(issuerName) && (StringUtils.isEmpty(issuerPrivPath) || StringUtils.isEmpty(issuerCrtPath))) {
            System.err.println("issuer name and issuer key cannot be empty at the same time");
            return;
        }
        try {
            PKCS10CertificationRequest csr = CertificateUtils.parseCertificationRequest(!StringUtils.isEmpty(name) ? FileUtils.readText(caHome + File.separator + name + ".csr") : FileUtils.readText(csrPath));
            String issuerKey = !StringUtils.isEmpty(issuerName) ? FileUtils.readText(caHome + File.separator + issuerName + ".priv") : FileUtils.readText(issuerPrivPath);
            issuerName = !StringUtils.isEmpty(issuerName) ? issuerName : FilenameUtils.removeExtension(new File(issuerPrivPath).getName());
            String password = caCli.scanValue("password of the issuer");
            PrivKey issuerPrivKey = KeyGenUtils.decodePrivKeyWithRawPassword(issuerKey, password);
            PrivateKey issuerPrivateKey = CertificateUtils.retrievePrivateKey(issuerPrivKey);
            PubKey csrPubKey = CertificateUtils.resolvePubKey(csr);
            SignatureFunction signatureFunction = Crypto.getSignatureFunction(issuerPrivKey.getAlgorithm());
            byte[] testBytes = UUID.randomUUID().toString().getBytes();
            X509v3CertificateBuilder certificateBuilder;
            if (!signatureFunction.verify(signatureFunction.sign(issuerPrivKey, testBytes), csrPubKey, testBytes)) {
                String issuerCrt = !StringUtils.isEmpty(issuerName) ? FileUtils.readText(caHome + File.separator + issuerName + ".crt") : FileUtils.readText(issuerCrtPath);
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
            certificateBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(CertificateUtils.checkCertificateRolesAnyNoException(csr, CertificateRole.ROOT, CertificateRole.CA)));

            String algorithm = Crypto.getAlgorithm(issuerPrivKey.getAlgorithm()).name();
            ContentSigner signer = new JcaContentSignerBuilder(caCli.CA_ALGORITHM_MAP.get(algorithm)).build(issuerPrivateKey);
            X509CertificateHolder holder = certificateBuilder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

            String crtName = !StringUtils.isEmpty(name) ? name : FilenameUtils.removeExtension(new File(csrPath).getName());
            String crtFile = caHome + File.separator + crtName + ".crt";
            FileUtils.writeText(CertificateUtils.toPEMString(cert), new File(crtFile));

            System.out.println("create [" + crtFile + "] success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

@CommandLine.Command(name = "renew", mixinStandardHelpOptions = true, header = "Update validity period.")
class CARenew implements Runnable {

    @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the certificate")
    String name;

    @CommandLine.Option(names = "--crt", description = "File of the certificate")
    String crtPath;

    @CommandLine.Option(names = "--days", required = true, description = "Days of certificate validity")
    int days;

    @CommandLine.Option(names = "--issuer-name", description = "Name of the issuer key")
    String issuerName;

    @CommandLine.Option(names = "--issuer-priv", description = "Path of the issuer private key file")
    String issuerPrivPath;

    @CommandLine.Option(names = "--issuer-crt", description = "Path of the issuer certificate file")
    String issuerCrtPath;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        File caHome = new File(caCli.getCaHome());
        if (!caHome.exists()) {
            caHome.mkdirs();
        }
        if (StringUtils.isEmpty(name) && StringUtils.isEmpty(crtPath)) {
            System.err.println("crt name and crt path cannot be empty at the same time");
            return;
        }
        if (StringUtils.isEmpty(issuerName) && (StringUtils.isEmpty(issuerPrivPath) || StringUtils.isEmpty(issuerCrtPath))) {
            System.err.println("issuer name and issuer key cannot be empty at the same time");
            return;
        }

        try {
            X509Certificate originCrt = CertificateUtils.parseCertificate(!StringUtils.isEmpty(name) ? FileUtils.readText(caHome + File.separator + name + ".crt") : FileUtils.readText(crtPath));
            String issuerKey = !StringUtils.isEmpty(issuerName) ? FileUtils.readText(caHome + File.separator + issuerName + ".priv") : FileUtils.readText(issuerPrivPath);
            issuerName = !StringUtils.isEmpty(issuerName) ? issuerName : FilenameUtils.removeExtension(new File(issuerPrivPath).getName());
            String password = caCli.scanValue("password of the issuer");
            PrivKey issuerPrivKey = KeyGenUtils.decodePrivKeyWithRawPassword(issuerKey, password);
            PrivateKey issuerPrivateKey = CertificateUtils.retrievePrivateKey(issuerPrivKey);
            PubKey crtPubKey = CertificateUtils.resolvePubKey(originCrt);
            SignatureFunction signatureFunction = Crypto.getSignatureFunction(issuerPrivKey.getAlgorithm());
            byte[] testBytes = UUID.randomUUID().toString().getBytes();
            X509v3CertificateBuilder certificateBuilder;
            if (!signatureFunction.verify(signatureFunction.sign(issuerPrivKey, testBytes), crtPubKey, testBytes)) {
                String issuerCrt = !StringUtils.isEmpty(issuerName) ? FileUtils.readText(caHome + File.separator + issuerName + ".crt") : FileUtils.readText(issuerCrtPath);
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
            certificateBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(CertificateUtils.checkCertificateRolesAnyNoException(originCrt, CertificateRole.ROOT, CertificateRole.CA)));

            String algorithm = Crypto.getAlgorithm(issuerPrivKey.getAlgorithm()).name();
            ContentSigner signer = new JcaContentSignerBuilder(caCli.CA_ALGORITHM_MAP.get(algorithm)).build(issuerPrivateKey);
            X509CertificateHolder holder = certificateBuilder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

            String crtName = !StringUtils.isEmpty(name) ? name : FilenameUtils.removeExtension(new File(crtPath).getName());
            String crtFile = caHome + File.separator + crtName + ".crt";
            FileUtils.writeText(CertificateUtils.toPEMString(cert), new File(crtFile));

            System.out.println("renew [" + crtFile + "] success");
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
                FileUtils.writeText(pubkey, new File(caCli.getCaHome() + File.separator + name + ".pub"));
                FileUtils.writeText(privkey, new File(caCli.getCaHome() + File.separator + name + ".priv"));
                FileUtils.writeText(base58pwd, new File(caCli.getCaHome() + File.separator + name + ".pwd"));
                FileUtils.writeText(CertificateUtils.toPEMString(algorithm, CertificateUtils.retrievePrivateKey(keypair.getPrivKey(), keypair.getPubKey())), new File(caCli.getCaHome() + File.separator + name + ".key"));

                if (i == 0) {
                    issuerPrivKey = keypair.getPrivKey();
                }
                X509Certificate certificate = genCert(name, ou, CertificateUtils.retrievePublicKey(keypair.getPubKey()), CertificateUtils.retrievePrivateKey(issuerPrivKey), issuerCrt);
                if (i == 0) {
                    issuerCrt = certificate;
                }
                FileUtils.writeText(CertificateUtils.toPEMString(certificate), new File(caCli.getCaHome() + File.separator + name + ".crt"));
            }

            System.out.println("create test certificates in [" + caCli.getCaHome() + "] success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private X509Certificate genCert(String name, CertificateRole ou, PublicKey publicKey, PrivateKey issuerPrivateKey, X509Certificate issuerCrt) throws Exception {
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
        certificateBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(ou.equals(CertificateRole.ROOT) || ou.equals(CertificateRole.CA)));

        ContentSigner signer = new JcaContentSignerBuilder(caCli.CA_ALGORITHM_MAP.get(algorithm.toUpperCase())).build(issuerPrivateKey);
        X509CertificateHolder holder = certificateBuilder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }
}

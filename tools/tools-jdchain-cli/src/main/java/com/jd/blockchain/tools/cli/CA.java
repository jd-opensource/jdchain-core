package com.jd.blockchain.tools.cli;

import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateType;
import com.jd.blockchain.ca.X509Utils;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
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
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

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
                CommandLine.HelpCommand.class
        }
)

public class CA implements Runnable {
    static final String CA_HOME = "config/keys";
    static final String CA_LIST_FORMAT = "%s\t%s\t%s\t%s\t%s%n";
    static final String[] SUPPORT_ALGORITHMS = new String[]{"ED25519", "SM2", "RSA", "ECDSA"};
    static Map<String, String> CA_ALGORITHM_MAP = new HashMap<String, String>();

    static {
        CA_ALGORITHM_MAP.put("ED25519", "ED25519");
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
        File caHome = new File(caCli.jdChainCli.path.getAbsolutePath() + File.separator + caCli.CA_HOME);
        if (!caHome.exists()) {
            caHome.mkdirs();
        }
        File[] certs = caHome.listFiles((dir, name) -> {
            if (name.endsWith(".crt")) {
                return true;
            }
            return false;
        });
        System.out.printf(caCli.CA_LIST_FORMAT, "NAME", "ALGORITHM", "TYPE", "ROLE", "PUBKEY");
        Arrays.stream(certs).forEach(cert -> {
            try {
                String name = FilenameUtils.removeExtension(cert.getName());
                X509Certificate certificate = X509Utils.resolveCertificate(FileUtils.readText(cert));
                PubKey pubKey = X509Utils.resolvePubKey(certificate);
                Set<String> ous = X509Utils.getSubject(certificate, BCStyle.OU);
                System.out.printf(caCli.CA_LIST_FORMAT, name, Crypto.getAlgorithm(pubKey.getAlgorithm()).name(), "ROLE-TODO", ous, pubKey);
            } catch (Exception e) {
                System.err.print("error certificate: " + cert);
            }
        });
    }
}

@CommandLine.Command(name = "show", mixinStandardHelpOptions = true, header = "Show certificate.")
class CAShow implements Runnable {

    @CommandLine.Option(required = true, names = {"-n", "--name"}, description = "Name of the key")
    String name;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        File caHome = new File(caCli.jdChainCli.path.getAbsolutePath() + File.separator + caCli.CA_HOME);
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
            X509Certificate certificate = X509Utils.resolveCertificate(FileUtils.readText(new File(caHome + File.separator + name + ".crt")));
            System.out.printf(caCli.CA_LIST_FORMAT, "NAME", "ALGORITHM", "TYPE", "ROLE", "PUBKEY");
            PubKey pubKey = X509Utils.resolvePubKey(certificate);
            Set<String> ous = X509Utils.getSubject(certificate, BCStyle.OU);
            System.out.printf(caCli.CA_LIST_FORMAT, name, Crypto.getAlgorithm(pubKey.getAlgorithm()).name(), "ROLE-TODO", ous, pubKey);
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
            File caHome = new File(caCli.jdChainCli.path.getAbsolutePath() + File.separator + caCli.CA_HOME);
            if (!caHome.exists()) {
                caHome.mkdirs();
            }
            if (StringUtils.isEmpty(name) && StringUtils.isEmpty(privPath) && StringUtils.isEmpty(pubPath)) {
                System.err.println("name and key cannot be empty at the same time");
            } else {
                String org = caCli.scanValue("organization name");
                String type = caCli.scanValue("certificate type", Arrays.stream(CertificateType.values()).map(Enum::name).toArray(String[]::new));
                String[] roles = caCli.scanValues("certificate roles", Arrays.stream(CertificateRole.values()).map(Enum::name).toArray(String[]::new));
                String country = caCli.scanValue("country");
                String locality = caCli.scanValue("locality");
                String province = caCli.scanValue("province");
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
                PrivateKey privateKey = X509Utils.resolvePrivateKey(privKey);
                String algorithm = Crypto.getAlgorithm(privKey.getAlgorithm()).name();
                ContentSigner signGen = new JcaContentSignerBuilder(caCli.CA_ALGORITHM_MAP.get(algorithm)).build(privateKey);
                PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(subject, X509Utils.resolvePublicKey(pubKey));
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

    @CommandLine.Option(names = "--csr", required = true, description = "Path of the certificate signing request file")
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
        File caHome = new File(caCli.jdChainCli.path.getAbsolutePath() + File.separator + caCli.CA_HOME);
        if (!caHome.exists()) {
            caHome.mkdirs();
        }
        if (StringUtils.isEmpty(issuerName) && (StringUtils.isEmpty(issuerPrivPath) || StringUtils.isEmpty(issuerCrtPath))) {
            System.err.println("issuer name and issuer key cannot be empty at the same time");
        } else {
            try {
                PKCS10CertificationRequest csr = X509Utils.resolveCertificationRequest(FileUtils.readText(csrPath));
                String issuerKey = !StringUtils.isEmpty(issuerName) ? FileUtils.readText(caHome + File.separator + issuerName + ".priv") : FileUtils.readText(issuerPrivPath);
                issuerName = !StringUtils.isEmpty(issuerName) ? issuerName : FilenameUtils.removeExtension(new File(issuerPrivPath).getName());
                String issuerCrt = !StringUtils.isEmpty(issuerName) ? FileUtils.readText(caHome + File.separator + issuerName + ".crt") : FileUtils.readText(issuerCrtPath);
                X509Certificate signerCrt = X509Utils.resolveCertificate(issuerCrt);
                String password = caCli.scanValue("password of the issuer");
                PrivKey issuerPrivKey = KeyGenUtils.decodePrivKeyWithRawPassword(issuerKey, password);
                PrivateKey issuerPrivateKey = X509Utils.resolvePrivateKey(issuerPrivKey);
                X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
                        new X500Name(signerCrt.getSubjectDN().getName()),
                        BigInteger.valueOf(new Random().nextInt() & 0x7fffffff),
                        new Date(),
                        new Date(System.currentTimeMillis() + days * 1000L * 24L * 60L * 60L),
                        csr.getSubject(),
                        csr.getSubjectPublicKeyInfo()
                );
                String algorithm = Crypto.getAlgorithm(issuerPrivKey.getAlgorithm()).name();
                ContentSigner signer = new JcaContentSignerBuilder(caCli.CA_ALGORITHM_MAP.get(algorithm)).build(issuerPrivateKey);
                X509CertificateHolder holder = certificateBuilder.build(signer);
                X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

                String crtFile = caHome + File.separator + FilenameUtils.removeExtension(new File(csrPath).getName()) + ".crt";
                FileUtils.writeText(X509Utils.toPEMString(cert), new File(crtFile));

                System.out.println("create [" + crtFile + "] success");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

@CommandLine.Command(name = "renew", mixinStandardHelpOptions = true, header = "Update validity period.")
class CARenew implements Runnable {

    @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the certificate")
    String name;

    @CommandLine.Option(names = "--crt", description = "File of the certificate")
    String crtPath;

    @CommandLine.Option(names = "--issuer-name", description = "Name of the issuer key")
    String issuerName;

    @CommandLine.Option(names = "--issuer-key", description = "Path of the issuer private key file")
    String issuerKeyPath;

    @CommandLine.ParentCommand
    private CA caCli;

    @Override
    public void run() {
        File caHome = new File(caCli.jdChainCli.path.getAbsolutePath() + File.separator + caCli.CA_HOME);
        if (!caHome.exists()) {
            caHome.mkdirs();
        }
        if (StringUtils.isEmpty(name) && StringUtils.isEmpty(crtPath)) {
            System.err.println("crt name and crt path cannot be empty at the same time");
            return;
        }
        if (StringUtils.isEmpty(issuerName) && StringUtils.isEmpty(issuerKeyPath)) {
            System.err.println("issuer name and issuer key cannot be empty at the same time");
            return;
        }
    }
}

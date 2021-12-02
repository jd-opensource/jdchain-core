package com.jd.blockchain.tools.cli;

import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainKeypair;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;
import utils.StringUtils;
import utils.codec.Base58Utils;
import utils.crypto.classic.SHA256Utils;
import utils.io.FileUtils;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * @description: JD Chain Keypair management
 * @author: imuge
 * @date: 2021/7/23
 **/
@CommandLine.Command(name = "keys",
        mixinStandardHelpOptions = true,
        showDefaultValues = true,
        description = "List, create, update or delete keypairs.",
        subcommands = {
                KeysList.class,
                KeysShow.class,
                KeysAdd.class,
                KeysUpdate.class,
                KeysDelete.class,
                KeysImport.class,
                CommandLine.HelpCommand.class
        }
)
public class Keys implements Runnable {

    static final String KEYS_HOME = "keys";
    static final String KEYS_PRINT_FORMAT = "%-15s%-15s\t%s\t%s%n";

    @CommandLine.ParentCommand
    JDChainCli jdChainCli;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    protected String getKeysHome() {
        try {
            return jdChainCli.path.getCanonicalPath() + File.separator + KEYS_HOME;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }
}

@CommandLine.Command(name = "list", mixinStandardHelpOptions = true, header = "List all the keypairs.")
class KeysList implements Runnable {

    @CommandLine.ParentCommand
    private Keys keys;

    @Override
    public void run() {
        File keysHome = new File(keys.getKeysHome());
        if (!keysHome.exists()) {
            keysHome.mkdirs();
        }
        File[] pubs = keysHome.listFiles((dir, name) -> {
            // TODO valid .priv exists
            if (name.endsWith(".pub")) {
                return true;
            }
            return false;
        });
        System.out.printf(Keys.KEYS_PRINT_FORMAT, "NAME", "ALGORITHM", "ADDRESS", "PUBKEY");
        Arrays.stream(pubs).forEach(priv -> {
            String key = FilenameUtils.removeExtension(priv.getName());
            String keyPath = FilenameUtils.removeExtension(priv.getAbsolutePath());
            String pubkey = FileUtils.readText(new File(keyPath + ".pub"));
            PubKey pk = KeyGenUtils.decodePubKey(pubkey);
            System.out.printf(Keys.KEYS_PRINT_FORMAT, key, Crypto.getAlgorithm(pk.getAlgorithm()).name(), AddressEncoding.generateAddress(pk), pubkey);
        });
    }
}

@CommandLine.Command(name = "show", mixinStandardHelpOptions = true, header = "Show keypair.")
class KeysShow implements Runnable {

    @CommandLine.Option(required = true, names = {"-n", "--name"}, description = "Name of the key")
    String name;

    @CommandLine.ParentCommand
    private Keys keys;

    @Override
    public void run() {
        File keysHome = new File(keys.getKeysHome());
        if (!keysHome.exists()) {
            keysHome.mkdirs();
        }
        File[] pubs = keysHome.listFiles((dir, name) -> {
            if (FilenameUtils.removeExtension(name).equals(this.name)) {
                return true;
            }
            return false;
        });
        if (null != pubs && pubs.length > 0) {
            System.out.println("input the password:");
            String password = ScannerUtils.read();
            String base58pwd = FileUtils.readText(new File(keysHome + File.separator + name + ".pwd"));
            if (!StringUtils.isEmpty(password) && base58pwd.equals(Base58Utils.encode(SHA256Utils.hash(password.getBytes())))) {
                String pubkey = FileUtils.readText(new File(keysHome + File.separator + name + ".pub"));
                PubKey pk = KeyGenUtils.decodePubKey(FileUtils.readText(new File(keysHome + File.separator + name + ".pub")));
                String privkey = FileUtils.readText(new File(keysHome + File.separator + name + ".priv"));
                System.out.printf("%-15s%-15s\t%s\t%s\t%s\t%s%n", "NAME", "ALGORITHM", "ADDRESS", "PUBKEY", "PRIVKEY", "PASSWORD");
                System.out.printf("%-15s%-15s\t%s\t%s\t%s\t%s%n", name, Crypto.getAlgorithm(pk.getAlgorithm()).name(), AddressEncoding.generateAddress(pk), pubkey, privkey, base58pwd);
            } else {
                System.err.print("password wrong");
            }
        } else {
            System.err.println("[" + name + "] not exists");
        }
    }
}

@CommandLine.Command(name = "add", mixinStandardHelpOptions = true, header = "Create a new keypair.")
class KeysAdd implements Runnable {

    @CommandLine.Option(required = true, names = {"-n", "--name"}, description = "Name of the key")
    String name;

    @CommandLine.Option(names = {"-a", "--algorithm"}, description = "Crypto algorithm")
    String algorithm;

    @CommandLine.Option(names = {"-p", "--password"}, description = "Password of the key")
    String password;

    @CommandLine.Option(names = "--seed", description = "Seed of the key, length must gte 32")
    String seed;

    @CommandLine.ParentCommand
    private Keys keys;

    @Override
    public void run() {
        File keysHome = new File(keys.getKeysHome());
        if (!keysHome.exists()) {
            keysHome.mkdirs();
        }
        String[] names = keysHome.list((dir, fileName) -> {
            if (FilenameUtils.removeExtension(fileName).equals(name)) {
                return true;
            }
            return false;
        });
        if (names.length > 0) {
            System.err.println("[" + name + "] already exists");
        } else {
            algorithm = null != algorithm ? algorithm.toUpperCase() : "ED25519";
            AsymmetricKeypair keypair;
            if (StringUtils.isEmpty(seed)) {
                keypair = Crypto.getSignatureFunction(algorithm.toUpperCase()).generateKeypair();
            } else {
                keypair = Crypto.getSignatureFunction(algorithm.toUpperCase()).generateKeypair(seed.getBytes());
            }
            if (StringUtils.isEmpty(password)) {
                System.out.println("input the password:");
                password = ScannerUtils.read();
            }
            if (!StringUtils.isEmpty(password)) {
                String pubkey = KeyGenUtils.encodePubKey(keypair.getPubKey());
                String base58pwd = KeyGenUtils.encodePasswordAsBase58(password);
                String privkey = KeyGenUtils.encodePrivKey(keypair.getPrivKey(), base58pwd);
                PrivateKey privateKey = CertificateUtils.retrievePrivateKey(keypair.getPrivKey());
                FileUtils.writeText(pubkey, new File(keysHome + File.separator + name + ".pub"));
                FileUtils.writeText(privkey, new File(keysHome + File.separator + name + ".priv"));
                FileUtils.writeText(base58pwd, new File(keysHome + File.separator + name + ".pwd"));
                FileUtils.writeText(CertificateUtils.toPEMString(algorithm, privateKey), new File(keysHome + File.separator + name + ".key"));
                System.out.printf(Keys.KEYS_PRINT_FORMAT, "NAME", "ALGORITHM", "ADDRESS", "PUBKEY");
                System.out.printf(Keys.KEYS_PRINT_FORMAT, name, Crypto.getAlgorithm(algorithm).name(), AddressEncoding.generateAddress(keypair.getPubKey()), keypair.getPubKey());
            } else {
                System.err.println("invalid password");
            }
        }
    }
}

@CommandLine.Command(name = "update", mixinStandardHelpOptions = true, header = "Update privkey password.")
class KeysUpdate implements Runnable {

    @CommandLine.Option(required = true, names = {"-n", "--name"}, description = "Name of the key")
    String name;

    @CommandLine.ParentCommand
    private Keys keys;

    @Override
    public void run() {
        File keysHome = new File(keys.getKeysHome());
        if (!keysHome.exists()) {
            keysHome.mkdirs();
        }
        String[] names = keysHome.list((dir, fileName) -> {
            if (FilenameUtils.removeExtension(fileName).equals(name)) {
                return true;
            }
            return false;
        });
        if (names.length == 0) {
            System.err.println("[" + name + "] not exists");
        } else {
            System.out.println("input the current password:");
            String password = ScannerUtils.read();
            String base58pwd = FileUtils.readText(new File(keysHome + File.separator + name + ".pwd"));
            if (!StringUtils.isEmpty(password) && base58pwd.equals(Base58Utils.encode(SHA256Utils.hash(password.getBytes())))) {
                System.out.println("input new password:");
                password = ScannerUtils.read();
                if (!StringUtils.isEmpty(password)) {
                    PrivKey privKey = KeyGenUtils.decodePrivKey(FileUtils.readText(new File(keysHome + File.separator + name + ".priv")), base58pwd);
                    base58pwd = KeyGenUtils.encodePasswordAsBase58(password);
                    FileUtils.writeText(KeyGenUtils.encodePrivKey(privKey, base58pwd), new File(keysHome + File.separator + name + ".priv"));
                    FileUtils.writeText(base58pwd, new File(keysHome + File.separator + name + ".pwd"));
                    PubKey pubkey = KeyGenUtils.decodePubKey(FileUtils.readText(new File(keysHome + File.separator + name + ".pub")));
                    System.out.printf(Keys.KEYS_PRINT_FORMAT, "NAME", "ALGORITHM", "ADDRESS", "PUBKEY");
                    System.out.printf(Keys.KEYS_PRINT_FORMAT, name, Crypto.getAlgorithm(pubkey.getAlgorithm()).name(), AddressEncoding.generateAddress(pubkey), pubkey);
                } else {
                    System.err.println("invalid password");
                }
            } else {
                System.err.print("password wrong");
            }
        }
    }
}

@CommandLine.Command(name = "delete", mixinStandardHelpOptions = true, header = "Delete keypair.")
class KeysDelete implements Runnable {

    @CommandLine.Option(required = true, names = {"-n", "--name"}, description = "Name of the key")
    String name;

    @CommandLine.ParentCommand
    private Keys keys;

    @Override
    public void run() {
        File keysHome = new File(keys.getKeysHome());
        if (!keysHome.exists()) {
            keysHome.mkdirs();
        }
        String[] names = keysHome.list((dir, fileName) -> {
            if (FilenameUtils.removeExtension(fileName).equals(name)) {
                return true;
            }
            return false;
        });
        if (names.length == 0) {
            System.err.println("[" + name + "] not exists");
        } else {
            System.out.println("input the current password:");
            String password = ScannerUtils.read();
            String base58pwd = FileUtils.readText(new File(keysHome + File.separator + name + ".pwd"));
            if (!StringUtils.isEmpty(password) && base58pwd.equals(KeyGenUtils.encodePasswordAsBase58(password))) {
                FileUtils.deleteFile(new File(keysHome + File.separator + name + ".pwd"));
                FileUtils.deleteFile(new File(keysHome + File.separator + name + ".priv"));
                FileUtils.deleteFile(new File(keysHome + File.separator + name + ".pub"));
                System.out.println("[" + name + "] deleted");
            } else {
                System.err.print("password wrong");
            }
        }
    }
}

@CommandLine.Command(name = "import", mixinStandardHelpOptions = true, header = "Import keypair from key and cert files.")
class KeysImport implements Runnable {

    @CommandLine.Option(required = true, names = {"-n", "--name"}, description = "Name of the key")
    String name;

    @CommandLine.Option(required = true, names = {"--crt"}, description = "File of the X509 certificate")
    String caPath;

    @CommandLine.Option(required = true, names = {"--key"}, description = "File of the PEM private key")
    String keyPath;

    @CommandLine.ParentCommand
    private Keys keys;

    @Override
    public void run() {
        File keysHome = new File(keys.getKeysHome());
        if (!keysHome.exists()) {
            keysHome.mkdirs();
        }
        String[] names = keysHome.list((dir, fileName) -> {
            if (FilenameUtils.removeExtension(fileName).equals(name)) {
                return true;
            }
            return false;
        });
        if (names.length != 0) {
            System.err.println("[" + name + "] already exists");
        } else {
            X509Certificate certificate = CertificateUtils.parseCertificate(new File(caPath));
            PubKey pubKey = CertificateUtils.resolvePubKey(certificate);
            PrivKey privKey = CertificateUtils.parsePrivKey(pubKey.getAlgorithm(), new File(keyPath));
            AsymmetricKeypair keypair = new BlockchainKeypair(pubKey, privKey);
            System.out.println("please input password:");
            String password = ScannerUtils.read();
            if (!StringUtils.isEmpty(password)) {
                String pubkey = KeyGenUtils.encodePubKey(keypair.getPubKey());
                String base58pwd = KeyGenUtils.encodePasswordAsBase58(password);
                String privkey = KeyGenUtils.encodePrivKey(keypair.getPrivKey(), base58pwd);
                FileUtils.writeText(pubkey, new File(keysHome + File.separator + name + ".pub"));
                FileUtils.writeText(privkey, new File(keysHome + File.separator + name + ".priv"));
                FileUtils.writeText(base58pwd, new File(keysHome + File.separator + name + ".pwd"));
                FileUtils.writeText(CertificateUtils.toPEMString(certificate), new File(keysHome + File.separator + name + ".crt"));
                System.out.printf(Keys.KEYS_PRINT_FORMAT, "NAME", "ALGORITHM", "ADDRESS", "PUBKEY");
                System.out.printf(Keys.KEYS_PRINT_FORMAT, name, Crypto.getAlgorithm(privKey.getAlgorithm()).name(), AddressEncoding.generateAddress(keypair.getPubKey()), keypair.getPubKey());
            } else {
                System.err.println("invalid password");
            }
        }
    }
}
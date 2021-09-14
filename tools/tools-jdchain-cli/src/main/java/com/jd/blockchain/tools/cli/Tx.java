package com.jd.blockchain.tools.cli;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BlockchainIdentityData;
import com.jd.blockchain.ledger.BlockchainKeyGenerator;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.ledger.BytesDataList;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.DigitalSignature;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.PreparedTransaction;
import com.jd.blockchain.ledger.RolesPolicy;
import com.jd.blockchain.ledger.TransactionPermission;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionTemplate;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.ledger.AccountState;
import com.jd.blockchain.sdk.client.GatewayBlockchainServiceProxy;
import com.jd.blockchain.sdk.client.GatewayServiceFactory;
import com.jd.blockchain.transaction.RolePrivilegeConfigurer;
import com.jd.blockchain.transaction.SignatureUtils;
import com.jd.blockchain.transaction.TransactionService;
import com.jd.blockchain.transaction.TxRequestMessage;
import com.jd.blockchain.transaction.UserRolesAuthorizer;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;
import utils.Bytes;
import utils.StringUtils;
import utils.codec.Base58Utils;
import utils.io.BytesUtils;
import utils.io.FileUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.security.cert.X509Certificate;
import java.util.Scanner;

/**
 * @description: traction operations
 * @author: imuge
 * @date: 2021/7/23
 **/
@CommandLine.Command(name = "tx",
        mixinStandardHelpOptions = true,
        showDefaultValues = true,
        description = "Build, sign or send transaction.",
        subcommands = {
                TxLedgerCAUpdate.class,
                TxUserRegister.class,
                TxUserCAUpdate.class,
                TxUserStateUpdate.class,
                TxRoleConfig.class,
                TxAuthorziationConfig.class,
                TxDataAccountRegister.class,
                TxKVSet.class,
                TxEventPublish.class,
                TxContractDeploy.class,
                TxContractCall.class,
                TxContractStateUpdate.class,
                TxEventAccountRegister.class,
                TxSign.class,
                TxSend.class,
                CommandLine.HelpCommand.class
        }
)
public class Tx implements Runnable {
    @CommandLine.ParentCommand
    JDChainCli jdChainCli;

    @CommandLine.Option(names = "--gw-host", defaultValue = "127.0.0.1", description = "Set the gateway host. Default: 127.0.0.1", scope = CommandLine.ScopeType.INHERIT)
    String gwHost;

    @CommandLine.Option(names = "--gw-port", defaultValue = "8080", description = "Set the gateway port. Default: 8080", scope = CommandLine.ScopeType.INHERIT)
    int gwPort;

    @CommandLine.Option(names = "--export", description = "Transaction export directory", scope = CommandLine.ScopeType.INHERIT)
    String export;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    GatewayBlockchainServiceProxy blockchainService;

    GatewayBlockchainServiceProxy getChainService() {
        if (null == blockchainService) {
            blockchainService = (GatewayBlockchainServiceProxy) GatewayServiceFactory.connect(gwHost, gwPort, false).getBlockchainService();
        }
        return blockchainService;
    }

    HashDigest selectLedger() {
        HashDigest[] ledgers = getChainService().getLedgerHashs();
        System.out.printf("select ledger, input the index: %n%-7s\t%s%n", "INDEX", "LEDGER");
        for (int i = 0; i < ledgers.length; i++) {
            System.out.printf("%-7s\t%s%n", i, ledgers[i]);
        }
        System.out.print("> ");
        Scanner scanner = new Scanner(System.in).useDelimiter("\n");
        String input = scanner.next().trim();
        return ledgers[Integer.parseInt(input)];
    }

    TransactionTemplate newTransaction() {
        return getChainService().newTransaction(selectLedger());
    }

    boolean sign(PreparedTransaction ptx) {
        DigitalSignature signature = sign(ptx.getTransactionHash());
        if (null != signature) {
            ptx.addSignature(signature);
            return true;
        } else {
            return false;
        }
    }

    boolean sign(TxRequestMessage tx) {
        DigitalSignature signature = sign(tx.getTransactionHash());
        if (null != signature) {
            tx.addEndpointSignatures(signature);
            return true;
        } else {
            return false;
        }
    }

    protected String getKeysHome() {
        try {
            return jdChainCli.path.getCanonicalPath() + File.separator + Keys.KEYS_HOME;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    DigitalSignature sign(HashDigest txHash) {
        File keysHome = new File(getKeysHome());
        File[] pubs = keysHome.listFiles((dir, name) -> {
            if (name.endsWith(".priv")) {
                return true;
            }
            return false;
        });
        if (null == pubs || pubs.length == 0) {
            System.err.printf("no signer in path [%s]%n", keysHome.getAbsolutePath());
            return null;
        } else {
            System.out.printf("select keypair to sign tx: %n%-7s\t%s\t%s%n", "INDEX", "KEY", "ADDRESS");
            BlockchainKeypair[] keypairs = new BlockchainKeypair[pubs.length];
            String[] passwords = new String[pubs.length];
            for (int i = 0; i < pubs.length; i++) {
                String key = FilenameUtils.removeExtension(pubs[i].getName());
                String keyPath = FilenameUtils.removeExtension(pubs[i].getAbsolutePath());
                String privkey = FileUtils.readText(new File(keyPath + ".priv"));
                String pwd = FileUtils.readText(new File(keyPath + ".pwd"));
                String pubkey = FileUtils.readText(new File(keyPath + ".pub"));
                keypairs[i] = new BlockchainKeypair(KeyGenUtils.decodePubKey(pubkey), KeyGenUtils.decodePrivKey(privkey, pwd));
                passwords[i] = pwd;
                System.out.printf("%-7s\t%s\t%s%n", i, key, keypairs[i].getAddress());
            }
            System.out.print("> ");
            Scanner scanner = new Scanner(System.in).useDelimiter("\n");
            int keyIndex = Integer.parseInt(scanner.next().trim());
            System.out.println("input password of the key: ");
            System.out.print("> ");
            String pwd = scanner.next();
            if (KeyGenUtils.encodePasswordAsBase58(pwd).equals(passwords[keyIndex])) {
                return SignatureUtils.sign(txHash, keypairs[keyIndex]);
            } else {
                System.err.println("password wrong");
                return null;
            }
        }
    }

    String export(PreparedTransaction ptx) {
        if (null != export) {
            File txPath = new File(export);
            txPath.mkdirs();
            File txFile = new File(txPath.getAbsolutePath() + File.separator + ptx.getTransactionHash());
            TxRequestMessage tx = new TxRequestMessage(ptx.getTransactionHash(), ptx.getTransactionContent());
            FileUtils.writeBytes(BinaryProtocol.encode(tx, TransactionRequest.class), txFile);
            return txFile.getAbsolutePath();
        } else {
            return null;
        }
    }

    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }
}

@CommandLine.Command(name = "ledger-ca-update", mixinStandardHelpOptions = true, header = "Update ledger certificates.")
class TxLedgerCAUpdate implements Runnable {

    @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the certificate")
    String name;

    @CommandLine.Option(names = "--crt", description = "File of the X509 certificate", scope = CommandLine.ScopeType.INHERIT)
    String caPath;

    @CommandLine.Option(names = "--operation", required = true, description = "Operation for this certificate. Optional values: ADD,UPDATE,REMOVE", scope = CommandLine.ScopeType.INHERIT)
    Operation operation;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        if (StringUtils.isEmpty(name) && StringUtils.isEmpty(caPath)) {
            System.err.println("crt name and crt path cannot be empty at the same time");
            return;
        }
        TransactionTemplate txTemp = txCommand.newTransaction();
        X509Certificate certificate = CertificateUtils.parseCertificate(!StringUtils.isEmpty(name) ? FileUtils.readText(txCommand.getKeysHome() + File.separator + name + ".crt") : FileUtils.readText(caPath));
        CertificateUtils.checkCertificateRolesAny(certificate, CertificateRole.ROOT, CertificateRole.CA);
        CertificateUtils.checkValidity(certificate);
        if (operation == Operation.ADD) {
            txTemp.metaInfo().ca().add(certificate);
        } else if (operation == Operation.UPDATE) {
            txTemp.metaInfo().ca().update(certificate);
        } else {
            txTemp.metaInfo().ca().remove(certificate);
        }
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.out.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                String pubkey = KeyGenUtils.encodePubKey(CertificateUtils.resolvePubKey(certificate));
                if (response.isSuccess()) {
                    System.out.printf("ledger ca: [%s](pubkey) updated%n", pubkey);
                } else {
                    System.err.printf("update ledger ca: [%s](pubkey) failed!%n", pubkey);
                }
            }
        }
    }

    private enum Operation {
        ADD,
        UPDATE,
        REMOVE
    }
}

@CommandLine.Command(name = "user-register", mixinStandardHelpOptions = true, header = "Register new user.")
class TxUserRegister implements Runnable {

    @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the key")
    String name;

    @CommandLine.Option(names = "--pubkey", description = "Pubkey of the user", scope = CommandLine.ScopeType.INHERIT)
    String pubkey;

    @CommandLine.Option(names = "--crt", description = "File of the X509 certificate", scope = CommandLine.ScopeType.INHERIT)
    String caPath;

    @CommandLine.Option(names = "--ca-mode", description = "Register with CA", scope = CommandLine.ScopeType.INHERIT)
    boolean caMode;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        File keysHome = new File(txCommand.getKeysHome());
        PubKey pubKey = null;
        X509Certificate certificate = null;
        if (!StringUtils.isEmpty(name)) {
            File[] pubs;
            if (caMode) {
                pubs = keysHome.listFiles((dir, name) -> {
                    if (name.endsWith(this.name + ".crt")) {
                        return true;
                    }
                    return false;
                });
                if (pubs.length != 1) {
                    System.err.printf("no [%s.crt] in path [%s]%n", name, keysHome.getAbsolutePath());
                    return;
                }
                certificate = CertificateUtils.parseCertificate(FileUtils.readText(pubs[0]));
            } else {
                pubs = keysHome.listFiles((dir, name) -> {
                    if (name.endsWith(this.name + ".pub")) {
                        return true;
                    }
                    return false;
                });
                if (pubs.length != 1) {
                    System.err.printf("no [%s.pub] in path [%s]%n", name, keysHome.getAbsolutePath());
                    return;
                }
                pubKey = Crypto.resolveAsPubKey(Base58Utils.decode(FileUtils.readText(pubs[0])));
            }
        } else if (!StringUtils.isEmpty(caPath)) {
            certificate = CertificateUtils.parseCertificate(FileUtils.readText(caPath));
        } else if (!StringUtils.isEmpty(pubkey) && !caMode) {
            pubKey = Crypto.resolveAsPubKey(Base58Utils.decode(pubkey));
        } else {
            System.err.println("key name, public key and certificate file can not be empty at the same time");
            return;
        }

        TransactionTemplate txTemp = txCommand.newTransaction();
        if (null != certificate) {
            CertificateUtils.checkCertificateRolesAny(certificate, CertificateRole.PEER, CertificateRole.GW, CertificateRole.USER);
            CertificateUtils.checkValidity(certificate);
            pubKey = CertificateUtils.resolvePubKey(certificate);
            txTemp.users().register(certificate);
        } else {
            txTemp.users().register(new BlockchainIdentityData(pubKey));
        }
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.out.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.printf("register user: [%s]%n", AddressEncoding.generateAddress(pubKey));
                } else {
                    System.err.println("register user failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "user-ca-update", mixinStandardHelpOptions = true, header = "Update user certificate.")
class TxUserCAUpdate implements Runnable {

    @CommandLine.Option(names = "--crt", description = "File of the X509 certificate", scope = CommandLine.ScopeType.INHERIT)
    String caPath;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        X509Certificate certificate;
        Bytes address;
        if (StringUtils.isEmpty(caPath)) {
            File keysHome = new File(txCommand.getKeysHome());
            File[] pubs = keysHome.listFiles((dir, name) -> {
                if (name.endsWith(".crt")) {
                    return true;
                }
                return false;
            });
            if (pubs.length == 0) {
                System.err.printf("no certificate in path [%s]%n", keysHome.getAbsolutePath());
                return;
            } else {
                System.out.printf("select user to update: %n%-7s\t%s\t%s%n", "INDEX", "KEY", "ADDRESS");
                Bytes[] addresses = new Bytes[pubs.length];
                String[] certs = new String[pubs.length];
                for (int i = 0; i < pubs.length; i++) {
                    String key = FilenameUtils.removeExtension(pubs[i].getName());
                    String keyPath = FilenameUtils.removeExtension(pubs[i].getAbsolutePath());
                    String pubkey = FileUtils.readText(new File(keyPath + ".pub"));
                    certs[i] = keyPath + ".crt";
                    addresses[i] = AddressEncoding.generateAddress(KeyGenUtils.decodePubKey(pubkey));
                    System.out.printf("%-7s\t%s\t%s%n", i, key, addresses[i]);
                }
                System.out.print("> ");
                Scanner scanner = new Scanner(System.in).useDelimiter("\n");
                int keyIndex = Integer.parseInt(scanner.next().trim());
                address = addresses[keyIndex];
                certificate = CertificateUtils.parseCertificate(FileUtils.readText(new File(certs[keyIndex])));
                if (!address.equals(address)) {
                    System.err.println("the key pair does not match the certificate");
                    return;
                }
            }
        } else {
            certificate = CertificateUtils.parseCertificate(new File(caPath));
            address = AddressEncoding.generateAddress(CertificateUtils.resolvePubKey(certificate));
        }
        CertificateUtils.checkCertificateRolesAny(certificate, CertificateRole.PEER, CertificateRole.GW, CertificateRole.USER);
        CertificateUtils.checkValidity(certificate);
        txTemp.user(address).ca(certificate);
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.out.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.printf("user: [%s] ca updated%n", address);
                } else {
                    System.err.println("update user ca failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "user-state-update", mixinStandardHelpOptions = true, header = "Update user(certificate) state.")
class TxUserStateUpdate implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "User address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--state", required = true, description = "User state，Optional values: FREEZE,NORMAL,REVOKE", scope = CommandLine.ScopeType.INHERIT)
    AccountState state;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        txTemp.user(address).state(state);
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.out.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.printf("change user: [%s] state to:[%s]%n", address, state);
                } else {
                    System.err.println("change user state failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "data-account-register", mixinStandardHelpOptions = true, header = "Register new data account.")
class TxDataAccountRegister implements Runnable {

    @CommandLine.Option(names = "--pubkey", description = "The pubkey of the exist data account", scope = CommandLine.ScopeType.INHERIT)
    String pubkey;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        BlockchainIdentity account;
        if (null == pubkey) {
            account = BlockchainKeyGenerator.getInstance().generate().getIdentity();
        } else {
            account = new BlockchainIdentityData(KeyGenUtils.decodePubKey(pubkey));
        }
        txTemp.dataAccounts().register(account);
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.printf("register data account: [%s]%n", account.getAddress());
                } else {
                    System.err.println("register data account failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "contract-deploy", mixinStandardHelpOptions = true, header = "Deploy or update contract.")
class TxContractDeploy implements Runnable {

    @CommandLine.Option(names = "--car", required = true, description = "The car file path", scope = CommandLine.ScopeType.INHERIT)
    File car;

    @CommandLine.Option(names = "--pubkey", description = "The pubkey of the exist contract", scope = CommandLine.ScopeType.INHERIT)
    String pubkey;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        BlockchainIdentity account;
        if (null == pubkey) {
            account = BlockchainKeyGenerator.getInstance().generate().getIdentity();
        } else {
            account = new BlockchainIdentityData(KeyGenUtils.decodePubKey(pubkey));
        }
        txTemp.contracts().deploy(account, FileUtils.readBytes(car));
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.printf("deploy contract: [%s]%n", account.getAddress());
                } else {
                    System.err.println("deploy contract failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "event-account-register", mixinStandardHelpOptions = true, header = "Register event account.")
class TxEventAccountRegister implements Runnable {

    @CommandLine.Option(names = "--pubkey", description = "The pubkey of the exist event account", scope = CommandLine.ScopeType.INHERIT)
    String pubkey;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        BlockchainIdentity account;
        if (null == pubkey) {
            account = BlockchainKeyGenerator.getInstance().generate().getIdentity();
        } else {
            account = new BlockchainIdentityData(KeyGenUtils.decodePubKey(pubkey));
        }
        txTemp.eventAccounts().register(account);
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.printf("register event account: [%s]%n", account.getAddress());
                } else {
                    System.err.println("register event account failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "kv", mixinStandardHelpOptions = true, header = "Set key-value.")
class TxKVSet implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Data account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--key", required = true, description = "Key to set", scope = CommandLine.ScopeType.INHERIT)
    String key;

    @CommandLine.Option(names = "--value", required = true, description = "Value to set", scope = CommandLine.ScopeType.INHERIT)
    String value;

    @CommandLine.Option(names = "--ver", defaultValue = "-1", description = "Version of the key-value", scope = CommandLine.ScopeType.INHERIT)
    long version;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        txTemp.dataAccount(address).setText(key, value, version);
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.println("set kv success");
                } else {
                    System.err.println("set kv failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "event", mixinStandardHelpOptions = true, header = "Publish event.")
class TxEventPublish implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Contract address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--name", required = true, description = "Event name", scope = CommandLine.ScopeType.INHERIT)
    String name;

    @CommandLine.Option(names = "--content", required = true, description = "Event content", scope = CommandLine.ScopeType.INHERIT)
    String value;

    @CommandLine.Option(names = "--sequence", defaultValue = "-1", description = "Sequence of the event", scope = CommandLine.ScopeType.INHERIT)
    long sequence;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        txTemp.eventAccount(address).publish(name, value, sequence);
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.println("event publish success");
                } else {
                    System.err.println("event publish failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "contract", mixinStandardHelpOptions = true, header = "Call contract method.")
class TxContractCall implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Contract address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--method", required = true, description = "Contract method", scope = CommandLine.ScopeType.INHERIT)
    String method;

    @CommandLine.Option(names = "--args", split = ",", description = "Method arguments", scope = CommandLine.ScopeType.INHERIT)
    String[] args;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        TypedValue[] tvs;
        if (null != args) {
            tvs = new TypedValue[args.length];
            for (int i = 0; i < args.length; i++) {
                tvs[i] = TypedValue.fromText(args[i]);
            }
        } else {
            tvs = new TypedValue[]{};
        }

        txTemp.contract().send(Bytes.fromBase58(address), method, new BytesDataList(tvs));
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.println("call contract success");
                    for (int i = 0; i < response.getOperationResults().length; i++) {
                        BytesValue content = response.getOperationResults()[i].getResult();
                        switch (content.getType()) {
                            case TEXT:
                                System.out.println("return string: " + content.getBytes().toUTF8String());
                                break;
                            case INT64:
                                System.out.println("return long: " + BytesUtils.toLong(content.getBytes().toBytes()));
                                break;
                            case BOOLEAN:
                                System.out.println("return boolean: " + BytesUtils.toBoolean(content.getBytes().toBytes()[0]));
                                break;
                            default: // byte[], Bytes
                                System.out.println("return bytes: " + content.getBytes().toBase58());
                                break;
                        }
                    }
                } else {
                    System.err.println("call contract failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "contract-state-update", mixinStandardHelpOptions = true, header = "Update contract state.")
class TxContractStateUpdate implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Contract address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--state", required = true, description = "Contract state，Optional values: FREEZE,NORMAL,REVOKE", scope = CommandLine.ScopeType.INHERIT)
    AccountState state;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        txTemp.contract(address).state(state);
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.out.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.printf("change contract: [%s] state to:[%s]%n", address, state);
                } else {
                    System.err.println("change contract state failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "role", mixinStandardHelpOptions = true, header = "Create or config role.")
class TxRoleConfig implements Runnable {

    @CommandLine.Option(names = "--name", required = true, description = "Role name", scope = CommandLine.ScopeType.INHERIT)
    String role;

    @CommandLine.Option(names = "--enable-ledger-perms", split = ",", description = "Enable ledger permissions", scope = CommandLine.ScopeType.INHERIT)
    LedgerPermission[] enableLedgerPerms;

    @CommandLine.Option(names = "--disable-ledger-perms", split = ",", description = "Disable ledger permissions", scope = CommandLine.ScopeType.INHERIT)
    LedgerPermission[] disableLedgerPerms;

    @CommandLine.Option(names = "--enable-transaction-perms", split = ",", description = "Enable transaction permissions", scope = CommandLine.ScopeType.INHERIT)
    TransactionPermission[] enableTransactionPerms;

    @CommandLine.Option(names = "--disable-transaction-perms", split = ",", description = "Disable transaction permissions", scope = CommandLine.ScopeType.INHERIT)
    TransactionPermission[] disableTransactionPerms;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        RolePrivilegeConfigurer configure = txTemp.security().roles().configure(role);
        if (null != enableLedgerPerms && enableLedgerPerms.length > 0) {
            configure.enable(enableLedgerPerms);
        }
        if (null != disableLedgerPerms && disableLedgerPerms.length > 0) {
            configure.disable(disableLedgerPerms);
        }
        if (null != enableTransactionPerms && enableTransactionPerms.length > 0) {
            configure.enable(enableTransactionPerms);
        }
        if (null != disableTransactionPerms && disableTransactionPerms.length > 0) {
            configure.disable(disableTransactionPerms);
        }
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.err.println("Role config success!");
                } else {
                    System.err.println("Role config failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "authorization", mixinStandardHelpOptions = true, header = "User role authorization.")
class TxAuthorziationConfig implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "User address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--authorize", split = ",", description = "Authorize roles", scope = CommandLine.ScopeType.INHERIT)
    String[] authorizeRoles;

    @CommandLine.Option(names = "--unauthorize", split = ",", description = "Unauthorize roles", scope = CommandLine.ScopeType.INHERIT)
    String[] unauthorizeRoles;

    @CommandLine.Option(names = "--policy", description = "Role policy", scope = CommandLine.ScopeType.INHERIT)
    RolesPolicy policy;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        TransactionTemplate txTemp = txCommand.newTransaction();
        UserRolesAuthorizer userRolesAuthorizer = txTemp.security().authorziations().forUser(Bytes.fromBase58(address));
        if (null != authorizeRoles && authorizeRoles.length > 0) {
            userRolesAuthorizer.authorize(authorizeRoles);
        }
        if (null != unauthorizeRoles && unauthorizeRoles.length > 0) {
            userRolesAuthorizer.unauthorize(unauthorizeRoles);
        }
        if (null == policy) {
            policy = RolesPolicy.UNION;
        }
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.err.println("Authorization config success!");
                } else {
                    System.err.println("Authorization config failed!");
                }
            }
        }
    }
}

@CommandLine.Command(name = "sign", mixinStandardHelpOptions = true, header = "Sign transaction.")
class TxSign implements Runnable {

    @CommandLine.Option(names = "--tx", description = "Local transaction file", scope = CommandLine.ScopeType.INHERIT)
    File txFile;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        if (!txFile.exists()) {
            System.err.println("Transaction file not exist!");
            return;
        }
        TxRequestMessage tx = new TxRequestMessage(BinaryProtocol.decode(FileUtils.readBytes(txFile), TransactionRequest.class));
        if (txCommand.sign(tx)) {
            FileUtils.writeBytes(BinaryProtocol.encode(tx), txFile);
            System.out.println("Sign transaction success!");
        } else {
            System.err.println("Sign transaction failed!");
        }
    }
}

@CommandLine.Command(name = "send", mixinStandardHelpOptions = true, header = "Send transaction.")
class TxSend implements Runnable {

    @CommandLine.Option(names = "--tx", description = "Local transaction file", scope = CommandLine.ScopeType.INHERIT)
    File txFile;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        if (!txFile.exists()) {
            System.err.println("Transaction file not exist!");
            return;
        }
        TxRequestMessage tx = new TxRequestMessage(BinaryProtocol.decode(FileUtils.readBytes(txFile), TransactionRequest.class));
        GatewayBlockchainServiceProxy chainService = txCommand.getChainService();
        try {
            Method method = GatewayBlockchainServiceProxy.class.getDeclaredMethod("getTransactionService", HashDigest.class);
            method.setAccessible(true);
            TransactionService txService = (TransactionService) method.invoke(chainService, txCommand.selectLedger());
            TransactionResponse response = txService.process(tx);
            if (response.isSuccess()) {
                System.out.println("Send transaction success: " + tx.getTransactionHash());
            } else {
                System.err.println("Send transaction failed!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
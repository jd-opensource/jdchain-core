package com.jd.blockchain.tools.cli;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.crypto.*;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.sdk.client.GatewayBlockchainServiceProxy;
import com.jd.blockchain.sdk.client.GatewayServiceFactory;
import com.jd.blockchain.transaction.*;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;
import utils.Bytes;
import utils.GmSSLProvider;
import utils.PropertiesUtils;
import utils.StringUtils;
import utils.codec.Base58Utils;
import utils.io.BytesUtils;
import utils.io.FileUtils;
import utils.net.SSLSecurity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

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
                TxDataAccountPermission.class,
                TxKVSet.class,
                TxEventAccountRegister.class,
                TxEventAccountPermission.class,
                TxEventPublish.class,
                TxEventSubscribe.class,
                TxContractDeploy.class,
                TxContractAccountPermission.class,
                TxContractCall.class,
                TxContractStateUpdate.class,
                TxSign.class,
                TxSend.class,
                TxTestKV.class,
                TxSwitchConsensus.class,
                TxSwitchHashAlgo.class,
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

    @CommandLine.Option(names = "--gw-secure", description = "Secure of the gateway service.", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean gwSecure;

    @CommandLine.Option(names = "--ssl.key-store", description = "Set ssl.key-store for SSL.", scope = CommandLine.ScopeType.INHERIT)
    String keyStore;

    @CommandLine.Option(names = "--ssl.key-store-type", description = "Set ssl.key-store-type for SSL.", scope = CommandLine.ScopeType.INHERIT)
    String keyStoreType;

    @CommandLine.Option(names = "--ssl.key-alias", description = "Set ssl.key-alias for SSL.", scope = CommandLine.ScopeType.INHERIT)
    String keyAlias;

    @CommandLine.Option(names = "--ssl.key-store-password", description = "Set ssl.key-store-password for SSL.", scope = CommandLine.ScopeType.INHERIT)
    String keyStorePassword;

    @CommandLine.Option(names = "--ssl.trust-store", description = "Set ssl.trust-store for SSL.", scope = CommandLine.ScopeType.INHERIT)
    String trustStore;

    @CommandLine.Option(names = "--ssl.trust-store-password", description = "Set trust-store-password for SSL.", scope = CommandLine.ScopeType.INHERIT)
    String trustStorePassword;

    @CommandLine.Option(names = "--ssl.trust-store-type", description = "Set ssl.trust-store-type for SSL.", scope = CommandLine.ScopeType.INHERIT)
    String trustStoreType;

    @CommandLine.Option(names = "--ssl.protocol", description = "Set ssl.protocol for SSL.", scope = CommandLine.ScopeType.INHERIT)
    String protocol;

    @CommandLine.Option(names = "--ssl.enabled-protocols", description = "Set ssl.enabled-protocols for SSL.", scope = CommandLine.ScopeType.INHERIT)
    String enabledProtocols;

    @CommandLine.Option(names = "--ssl.ciphers", description = "Set ssl.ciphers for SSL.", scope = CommandLine.ScopeType.INHERIT)
    String ciphers;

    @CommandLine.Option(names = "--ssl.host-verifier", defaultValue = "NO-OP",description = "Set host verifier for SSL. NO-OP or Default", scope = CommandLine.ScopeType.INHERIT)
    String hostNameVerifier;

    @CommandLine.Option(names = "--export", description = "Transaction export directory", scope = CommandLine.ScopeType.INHERIT)
    String export;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    GatewayBlockchainServiceProxy blockchainService;

    GatewayBlockchainServiceProxy getChainService() {
        if (null == blockchainService) {
            if (gwSecure) {
                GmSSLProvider.enableGMSupport(protocol);
                blockchainService = (GatewayBlockchainServiceProxy) GatewayServiceFactory.connect(gwHost, gwPort, gwSecure, new SSLSecurity(keyStoreType, keyStore, keyAlias, keyStorePassword,
                        trustStore, trustStorePassword, trustStoreType, protocol, enabledProtocols, ciphers, hostNameVerifier)).getBlockchainService();
            } else {
                blockchainService = (GatewayBlockchainServiceProxy) GatewayServiceFactory.connect(gwHost, gwPort, gwSecure).getBlockchainService();
            }
        }
        return blockchainService;
    }

    HashDigest selectLedger() {
        HashDigest[] ledgers = getChainService().getLedgerHashs();

        System.out.printf("select ledger, input the index: %n%-7s\t%s%n", "INDEX", "LEDGER");
        for (int i = 0; i < ledgers.length; i++) {
            System.out.printf("%-7s\t%s%n", i, ledgers[i]);
        }

        if(ledgers.length == 1){
            System.out.printf("> 0 (use default ledger)%n");
            return ledgers[0];
        }

        int index = ScannerUtils.readRangeInt(0, ledgers.length - 1);
        return ledgers[index];
    }

    TransactionTemplate newTransaction() {
        return getChainService().newTransaction(selectLedger());
    }

    TransactionTemplate newTransaction(HashDigest ledger) {
        return getChainService().newTransaction(ledger);
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
            return name.endsWith(".priv");
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
            int keyIndex = ScannerUtils.readRangeInt(0, pubs.length - 1);
            System.out.println("input password of the key: ");
            String pwd = ScannerUtils.read();
            if (KeyGenUtils.encodePasswordAsBase58(pwd).equals(passwords[keyIndex])) {
                return SignatureUtils.sign(txHash, keypairs[keyIndex]);
            } else {
                System.err.println("password wrong");
                return null;
            }
        }
    }

    BlockchainKeypair signer() {
        File keysHome = new File(getKeysHome());
        File[] pubs = keysHome.listFiles((dir, name) -> {
            return name.endsWith(".priv");
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
            int keyIndex = ScannerUtils.readRangeInt(0, pubs.length - 1);
            System.out.println("input password of the key: ");
            String pwd = ScannerUtils.read();
            if (KeyGenUtils.encodePasswordAsBase58(pwd).equals(passwords[keyIndex])) {
                return keypairs[keyIndex];
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

@CommandLine.Command(name = "root-ca", mixinStandardHelpOptions = true, header = "Update ledger root certificates.")
class TxLedgerCAUpdate implements Runnable {

    @CommandLine.Option(names = "--crt", description = "File of the X509 certificate", scope = CommandLine.ScopeType.INHERIT)
    String caPath;

    @CommandLine.Option(names = "--operation", required = true, description = "Operation for this certificate. Optional values: ADD,UPDATE,REMOVE", scope = CommandLine.ScopeType.INHERIT)
    Operation operation;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        if (StringUtils.isEmpty(caPath)) {
            System.err.println("crt path cannot be empty");
            return;
        }
        TransactionTemplate txTemp = txCommand.newTransaction();
        X509Certificate certificate = CertificateUtils.parseCertificate(FileUtils.readText(caPath));
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
                    System.err.printf("update ledger ca: [%s](pubkey) failed: [%s]!%n", pubkey, response.getExecutionState());
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

    @CommandLine.Option(names = "--pubkey", description = "Pubkey of the user", scope = CommandLine.ScopeType.INHERIT)
    String pubkey;

    @CommandLine.Option(names = "--crt", description = "File of the X509 certificate", scope = CommandLine.ScopeType.INHERIT)
    String caPath;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        PubKey pubKey = null;
        X509Certificate certificate = null;
        if (!StringUtils.isEmpty(caPath)) {
            certificate = CertificateUtils.parseCertificate(FileUtils.readText(caPath));
        } else if (!StringUtils.isEmpty(pubkey)) {
            pubKey = Crypto.resolveAsPubKey(Base58Utils.decode(pubkey));
        } else {
            System.err.println("public key and certificate file can not be empty at the same time");
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
                    System.err.printf("register user failed: [%s]!%n", response.getExecutionState());
                }
            }
        }
    }
}

@CommandLine.Command(name = "user-ca", mixinStandardHelpOptions = true, header = "Update user certificate.")
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
        if (!StringUtils.isEmpty(caPath)) {
            certificate = CertificateUtils.parseCertificate(new File(caPath));
            address = AddressEncoding.generateAddress(CertificateUtils.resolvePubKey(certificate));
        } else {
            System.err.println("certificate file can not be empty");
            return;
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
                    System.err.printf("update user failed: [%s]!%n", response.getExecutionState());
                }
            }
        }
    }
}

@CommandLine.Command(name = "user-state", mixinStandardHelpOptions = true, header = "Update user(certificate) state.")
class TxUserStateUpdate implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "User address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--state", required = true, description = "User state, Optional values: FREEZE,NORMAL,REVOKE", scope = CommandLine.ScopeType.INHERIT)
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
                    System.err.printf("change user state failed: [%s]!%n", response.getExecutionState());
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
                    System.err.printf("register data account failed: [%s]!%n", response.getExecutionState());
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
                    System.err.printf("deploy contract failed: [%s]!%n", response.getExecutionState());
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
                    System.err.printf("register event account failed: [%s]!%n", response.getExecutionState());
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
                    System.err.printf("set kv failed: [%s]!%n", response.getExecutionState());
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
                    System.err.printf("event publish failed: [%s]!%n", response.getExecutionState());
                }
            }
        }
    }
}

@CommandLine.Command(name = "event-listen", mixinStandardHelpOptions = true, header = "Subscribe event.")
class TxEventSubscribe implements Runnable {

    @CommandLine.Option(names = "--address", description = "Event address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--name", required = true, description = "Event name", scope = CommandLine.ScopeType.INHERIT)
    String name;

    @CommandLine.Option(names = "--sequence", defaultValue = "0", description = "Sequence of the event", scope = CommandLine.ScopeType.INHERIT)
    long sequence;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        // 事件监听会创建子线程，为阻止子线程被直接关闭，加入等待
        CountDownLatch cdl = new CountDownLatch(1);
        if (StringUtils.isEmpty(address)) {
            txCommand.getChainService().monitorSystemEvent(txCommand.selectLedger(),
                    SystemEvent.NEW_BLOCK_CREATED, sequence, (eventMessages, eventContext) -> {
                        for (Event eventMessage : eventMessages) {
                            // content中存放的是当前链上最新高度
                            System.out.println("New block:" + eventMessage.getSequence() + ":" + BytesUtils.toLong(eventMessage.getContent().getBytes().toBytes()));
                        }
                    });
        } else {
            txCommand.getChainService().monitorUserEvent(txCommand.selectLedger(), address, name, sequence, (eventMessage, eventContext) -> {

                BytesValue content = eventMessage.getContent();
                switch (content.getType()) {
                    case TEXT:
                    case XML:
                    case JSON:
                        System.out.println(eventMessage.getName() + ":" + eventMessage.getSequence() + ":" + content.getBytes().toUTF8String());
                        break;
                    case INT64:
                    case TIMESTAMP:
                        System.out.println(eventMessage.getName() + ":" + eventMessage.getSequence() + ":" + BytesUtils.toLong(content.getBytes().toBytes()));
                        break;
                    default: // byte[], Bytes
                        System.out.println(eventMessage.getName() + ":" + eventMessage.getSequence() + ":" + content.getBytes().toBase58());
                        break;
                }
            });
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
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

        txTemp.contract(Bytes.fromBase58(address)).invoke(method, new BytesDataList(tvs));
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

@CommandLine.Command(name = "contract-state", mixinStandardHelpOptions = true, header = "Update contract state.")
class TxContractStateUpdate implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Contract address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--state", required = true, description = "Contract state, Optional values: FREEZE,NORMAL,REVOKE", scope = CommandLine.ScopeType.INHERIT)
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
                    System.err.printf("change contract state failed: [%s]!%n", response.getExecutionState());
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
                    System.err.printf("Role config failed: [%s]!%n", response.getExecutionState());
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
                    System.err.printf("Authorization config failed: [%s]!%n", response.getExecutionState());
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
                System.err.printf("Send transaction failed: [%s]!%n", response.getExecutionState());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

@CommandLine.Command(name = "testkv", mixinStandardHelpOptions = true, header = "Send kv set transaction for testing.")
class TxTestKV implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Data account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--thread", required = true, description = "Thread number", defaultValue = "1", scope = CommandLine.ScopeType.INHERIT)
    int thread;

    @CommandLine.Option(names = "--interval", required = true, description = "Interval millisecond per single thread", defaultValue = "0", scope = CommandLine.ScopeType.INHERIT)
    int interval;

    @CommandLine.Option(names = "--silence", required = true, description = "Do not log tx detail", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean silence;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        HashDigest ledger = txCommand.selectLedger();
        BlockchainKeypair signer = txCommand.signer();
        CountDownLatch cdl = new CountDownLatch(1);
        AtomicLong count = new AtomicLong(0);
        final long startTime = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(startTime);
        for (int i = 0; i < thread; i++) {
            final int index = i + 1;
            new Thread(() -> {
                System.out.println("start thread " + index + " to set kv");
                while (true) {
                    TransactionTemplate txTemp = txCommand.newTransaction(ledger);
                    txTemp.dataAccount(address).setInt64(UUID.randomUUID().toString(), System.currentTimeMillis(), -1);
                    PreparedTransaction prepare = txTemp.prepare();
                    prepare.addSignature(SignatureUtils.sign(prepare.getTransactionHash(), signer));
                    try {
                        TransactionResponse response = prepare.commit();
                        if (!silence) {
                            System.out.println(prepare.getTransactionHash() + ": " + response.getExecutionState());
                        }
                        long l = count.incrementAndGet();
                        if (l % 1000 == 0) {
                            long t = System.currentTimeMillis();
                            date.setTime(t);
                            t -= startTime;
                            System.out.printf("current time: %s, total txs: %d, time: %d ms, tps: %d \n", sdf.format(date), l, t, l * 1000 / t);
                        }
                        if (interval > 0) {
                            try {
                                Thread.sleep(interval);
                            } catch (InterruptedException e) {
                            }
                        }
                    } catch (Exception e) {
                        if (!silence) {
                            System.out.println(prepare.getTransactionHash() + ": " + e.getMessage());
                        }
                    }
                }
            }).start();
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

@CommandLine.Command(name = "data-account-permission", mixinStandardHelpOptions = true, header = "Update data account permission.")
class TxDataAccountPermission implements Runnable {

    @CommandLine.Option(names = "--address", description = "Address of the data account", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--role", description = "Role of the data account", scope = CommandLine.ScopeType.INHERIT)
    String role;

    @CommandLine.Option(names = "--mode", description = "Mode value of the data account", defaultValue = "-1", scope = CommandLine.ScopeType.INHERIT)
    int mode;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        if (StringUtils.isEmpty(role) && mode == -1) {
            System.err.println("both role and mode are empty!");
            return;
        }
        TransactionTemplate txTemp = txCommand.newTransaction();
        AccountPermissionSetOperationBuilder builder = txTemp.dataAccount(address).permission();
        if (!StringUtils.isEmpty(role)) {
            builder.role(role);
        }
        if (mode > -1) {
            builder.mode(mode);
        }
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.printf("update data account: [%s] permission\n", address);
                } else {
                    System.err.printf("update data account permission failed: [%s]!%n", response.getExecutionState());
                }
            }
        }
    }
}

@CommandLine.Command(name = "event-account-permission", mixinStandardHelpOptions = true, header = "Update event account permission.")
class TxEventAccountPermission implements Runnable {

    @CommandLine.Option(names = "--address", description = "Address of the event account", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--role", description = "Role of the event account", scope = CommandLine.ScopeType.INHERIT)
    String role;

    @CommandLine.Option(names = "--mode", description = "Mode value of the event account", defaultValue = "-1", scope = CommandLine.ScopeType.INHERIT)
    int mode;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        if (StringUtils.isEmpty(role) && mode == -1) {
            System.err.println("both role and mode are empty!");
            return;
        }
        TransactionTemplate txTemp = txCommand.newTransaction();
        AccountPermissionSetOperationBuilder builder = txTemp.eventAccount(address).permission();
        if (!StringUtils.isEmpty(role)) {
            builder.role(role);
        }
        if (mode > -1) {
            builder.mode(mode);
        }
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.printf("update event account: [%s] permission\n", address);
                } else {
                    System.err.printf("update event account permission failed: [%s]!%n", response.getExecutionState());
                }
            }
        }
    }
}

@CommandLine.Command(name = "contract-permission", mixinStandardHelpOptions = true, header = "Update contract permission.")
class TxContractAccountPermission implements Runnable {

    @CommandLine.Option(names = "--address", description = "Address of the contract", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--role", description = "Role of the contract", scope = CommandLine.ScopeType.INHERIT)
    String role;

    @CommandLine.Option(names = "--mode", description = "Mode value of the contract", defaultValue = "-1", scope = CommandLine.ScopeType.INHERIT)
    int mode;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        if (StringUtils.isEmpty(role) && mode == -1) {
            System.err.println("both role and mode are empty!");
            return;
        }
        TransactionTemplate txTemp = txCommand.newTransaction();
        AccountPermissionSetOperationBuilder builder = txTemp.contract(address).permission();
        if (!StringUtils.isEmpty(role)) {
            builder.role(role);
        }
        if (mode > -1) {
            builder.mode(mode);
        }
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.printf("update contract: [%s] permission\n", address);
                } else {
                    System.err.printf("update contract permission failed: [%s]!%n", response.getExecutionState());
                }
            }
        }
    }
}

@CommandLine.Command(name = "switch-consensus", mixinStandardHelpOptions = true, header = "Switch consensus type.")
class TxSwitchConsensus implements Runnable {

    @CommandLine.Option(names = "--type", required = true, description = "New consensus type", scope = CommandLine.ScopeType.INHERIT)
    String type;

    @CommandLine.Option(names = "--file", required = true, description = "Set new consensus config file", scope = CommandLine.ScopeType.INHERIT)
    String config;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {
        Properties properties;
        String providerName = null;

        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(config)) {
            System.err.println("both type and config file path are empty!");
            return;
        }

        TransactionTemplate txTemp = txCommand.newTransaction();

        try (InputStream in = new FileInputStream(new File(config))) {
            properties = FileUtils.readProperties(in);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        if (type.equals("bft")) {
            providerName = "com.jd.blockchain.consensus.bftsmart.BftsmartConsensusProvider";
        } else if (type.equals("raft")) {
            providerName = "com.jd.blockchain.consensus.raft.RaftConsensusProvider";
        } else if (type.equals("mq")) {
            providerName = "com.jd.blockchain.consensus.mq.MsgQueueConsensusProvider";
        }
        txTemp.switchSettings().update(providerName, PropertiesUtils.getOrderedValues(properties));
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.println("switch consensus type success");
                } else {
                    System.err.printf("switch consensus type failed: [%s]!%n", response.getExecutionState());
                }
            }
        }
    }
}

@CommandLine.Command(name = "switch-hash-algo", mixinStandardHelpOptions = true, header = "Switch crypto hash algo.")
class TxSwitchHashAlgo implements Runnable {

    @CommandLine.Option(names = "--hash-algo", required = true, description = "New crypto hash algo", scope = CommandLine.ScopeType.INHERIT)
    String newHashAlgo;

    @CommandLine.ParentCommand
    private Tx txCommand;

    @Override
    public void run() {

        if (StringUtils.isEmpty(newHashAlgo)) {
            System.err.println("new hash algo is empty!");
            return;
        }

        TransactionTemplate txTemp = txCommand.newTransaction();
        txTemp.switchHashAlgo().update(newHashAlgo);
        PreparedTransaction ptx = txTemp.prepare();
        String txFile = txCommand.export(ptx);
        if (null != txFile) {
            System.err.println("export transaction success: " + txFile);
        } else {
            if (txCommand.sign(ptx)) {
                TransactionResponse response = ptx.commit();
                if (response.isSuccess()) {
                    System.out.println("switch new hash algo success");
                } else {
                    System.err.printf("switch new hash algo failed: [%s]!%n", response.getExecutionState());
                }
            }
        }
    }
}
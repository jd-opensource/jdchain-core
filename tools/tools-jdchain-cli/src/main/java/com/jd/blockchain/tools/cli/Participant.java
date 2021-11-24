package com.jd.blockchain.tools.cli;

import com.jd.blockchain.ca.CertificateRole;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainIdentityData;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.ledger.PreparedTransaction;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionTemplate;
import com.jd.blockchain.sdk.BlockchainService;
import com.jd.blockchain.sdk.client.GatewayServiceFactory;
import com.jd.httpservice.agent.ServiceConnectionManager;
import com.jd.httpservice.agent.ServiceEndpoint;
import com.jd.httpservice.converters.JsonResponseConverter;
import com.jd.httpservice.utils.web.WebResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import picocli.CommandLine;
import utils.StringUtils;
import utils.codec.Base58Utils;
import utils.io.FileUtils;
import utils.net.SSLSecurity;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: participant operations
 * @author: imuge
 * @date: 2021/7/26
 **/
@CommandLine.Command(name = "participant",
        mixinStandardHelpOptions = true,
        showDefaultValues = true,
        description = "Add, update or delete participant.",
        subcommands = {
                ParticipantRegister.class,
                ParticipantActive.class,
                ParticipantUpdate.class,
                ParticipantInactive.class,
                CommandLine.HelpCommand.class
        }
)
public class Participant implements Runnable {

    @CommandLine.Option(names = "--ssl.key-store", description = "Set ssl.key-store for TLS.", scope = CommandLine.ScopeType.INHERIT)
    String keyStore;

    @CommandLine.Option(names = "--ssl.key-store-type", description = "Set ssl.key-store-type for TLS.", scope = CommandLine.ScopeType.INHERIT)
    String keyStoreType;

    @CommandLine.Option(names = "--ssl.key-alias", description = "Set ssl.key-alias for TLS.", scope = CommandLine.ScopeType.INHERIT)
    String keyAlias;

    @CommandLine.Option(names = "--ssl.key-store-password", description = "Set ssl.key-store-password for TLS.", scope = CommandLine.ScopeType.INHERIT)
    String keyStorePassword;

    @CommandLine.Option(names = "--ssl.trust-store", description = "Set ssl.trust-store for TLS.", scope = CommandLine.ScopeType.INHERIT)
    String trustStore;

    @CommandLine.Option(names = "--ssl.trust-store-password", description = "Set trust-store-password for TLS.", scope = CommandLine.ScopeType.INHERIT)
    String trustStorePassword;

    @CommandLine.Option(names = "--ssl.trust-store-type", description = "Set ssl.trust-store-type for TLS.", scope = CommandLine.ScopeType.INHERIT)
    String trustStoreType;

    @CommandLine.ParentCommand
    JDChainCli jdChainCli;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }
}

@CommandLine.Command(name = "register", mixinStandardHelpOptions = true, header = "Register new participant.")
class ParticipantRegister implements Runnable {

    @CommandLine.Option(names = "--gw-host", defaultValue = "127.0.0.1", description = "Set the gateway host. Default: 127.0.0.1", scope = CommandLine.ScopeType.INHERIT)
    String gwHost;

    @CommandLine.Option(names = "--gw-port", defaultValue = "8080", description = "Set the gateway port. Default: 8080", scope = CommandLine.ScopeType.INHERIT)
    int gwPort;

    @CommandLine.Option(names = "--gw-secure", description = "Secure of the gateway service.", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean gwSecure;

    @CommandLine.Option(names = "--pubkey", description = "Pubkey of the user", scope = CommandLine.ScopeType.INHERIT)
    String pubkey;

    @CommandLine.Option(names = "--crt", description = "File of the X509 certificate", scope = CommandLine.ScopeType.INHERIT)
    String caPath;

    @CommandLine.Option(names = "--ca-mode", description = "Register with CA", scope = CommandLine.ScopeType.INHERIT)
    boolean caMode;

    @CommandLine.Option(required = true, names = "--participant-name", description = "Name of the participant")
    String participantName;

    BlockchainService blockchainService;
    @CommandLine.ParentCommand
    private Participant participant;

    BlockchainService getChainService() {
        if (null == blockchainService) {
            if (gwSecure) {
                blockchainService = GatewayServiceFactory.connect(gwHost, gwPort, gwSecure, new SSLSecurity(participant.keyStoreType, participant.keyStore, participant.keyAlias, participant.keyStorePassword,
                        participant.trustStore, participant.trustStorePassword, participant.trustStoreType)).getBlockchainService();
            } else {
                blockchainService = GatewayServiceFactory.connect(gwHost, gwPort, gwSecure).getBlockchainService();
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
        int selectedIndex = ScannerUtils.readRangeInt(0, ledgers.length - 1);
        return ledgers[selectedIndex];
    }

    TransactionTemplate newTransaction() {
        return getChainService().newTransaction(selectLedger());
    }

    boolean sign(PreparedTransaction ptx) {
        boolean ok = true;
        File keysHome = new File(getKeysHome());
        File[] pubs = keysHome.listFiles((dir, name) -> {
            if (name.endsWith(".priv")) {
                return true;
            }
            return false;
        });
        if (pubs.length == 0) {
            System.err.printf("no signer in path [%s]%n", keysHome.getAbsolutePath());
            ok = false;
        } else {
            System.out.println("select keypair to sign tx, input the index: ");
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
                System.out.printf("%-3s\t%s\t%s%n", i, key, keypairs[i].getAddress());
            }
            int keyIndex = ScannerUtils.readRangeInt(0, keypairs.length - 1);
            System.out.println("input password of the key: ");
            String pwd = ScannerUtils.read();
            if (KeyGenUtils.encodePasswordAsBase58(pwd).equals(passwords[keyIndex])) {
                ptx.sign(keypairs[keyIndex]);
            } else {
                System.err.println("password wrong");
                ok = false;
            }
        }

        return ok;
    }

    protected String getKeysHome() {
        try {
            return participant.jdChainCli.path.getCanonicalPath() + File.separator + Keys.KEYS_HOME;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void run() {
        PubKey pubKey = null;
        X509Certificate certificate = null;
        if (caMode) {
            if (!StringUtils.isEmpty(caPath)) {
                certificate = CertificateUtils.parseCertificate(FileUtils.readText(caPath));
            } else {
                System.err.println("certificate file can not be empty in ca mode");
                return;
            }
        } else if (!StringUtils.isEmpty(pubkey)) {
            pubKey = Crypto.resolveAsPubKey(Base58Utils.decode(pubkey));
        } else {
            System.err.println("public key can not be empty");
            return;
        }

        TransactionTemplate txTemp = newTransaction();
        if (null != certificate) {
            CertificateUtils.checkCertificateRolesAny(certificate, CertificateRole.PEER, CertificateRole.GW, CertificateRole.USER);
            CertificateUtils.checkValidity(certificate);
            pubKey = CertificateUtils.resolvePubKey(certificate);
            txTemp.participants().register(participantName, certificate);
        } else {
            txTemp.participants().register(participantName, new BlockchainIdentityData(pubKey));
        }
        PreparedTransaction ptx = txTemp.prepare();
        if (sign(ptx)) {
            TransactionResponse response = ptx.commit();
            if (response.isSuccess()) {
                System.out.printf("register participant: [%s]%n", AddressEncoding.generateAddress(pubKey));
            } else {
                System.err.println("register participant failed!");
            }
        }
    }
}

@CommandLine.Command(name = "active", mixinStandardHelpOptions = true, header = "Active participant.")
class ParticipantActive implements Runnable {

    @CommandLine.Option(names = "--ledger", required = true, description = "Set the ledger.", scope = CommandLine.ScopeType.INHERIT)
    String ledger;

    @CommandLine.Option(names = "--host", required = true, description = "Set the participant host.", scope = CommandLine.ScopeType.INHERIT)
    String host;

    @CommandLine.Option(names = "--port", required = true, description = "Set the participant service port.", scope = CommandLine.ScopeType.INHERIT)
    int port;

    @CommandLine.Option(names = "--secure", description = "Secure of participant service service.", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean secure;

    @CommandLine.Option(names = "--consensus-port", required = true, description = "Set the participant consensus port.", scope = CommandLine.ScopeType.INHERIT)
    int consensusPort;

    @CommandLine.Option(names = "--consensus-secure", description = "Whether to open the secure connection for consensus.", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean consensusSecure;

    @CommandLine.Option(names = "--syn-host", required = true, description = "Set synchronization participant host.", scope = CommandLine.ScopeType.INHERIT)
    String synHost;

    @CommandLine.Option(names = "--syn-port", required = true, description = "Set synchronization participant port.", scope = CommandLine.ScopeType.INHERIT)
    int synPort;

    @CommandLine.Option(names = "--syn-secure", description = "Whether the synchronization connection is secure.", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean synSecure;

    @CommandLine.Option(names = "--shutdown", description = "Restart the node server.", scope = CommandLine.ScopeType.INHERIT)
    boolean shutdown;

    @CommandLine.ParentCommand
    private Participant participant;

    @Override
    public void run() {
        // TODO valid params
        try {
            WebResponse webResponse = active();
            if (webResponse.isSuccess()) {
                System.out.println("participant activated");
            } else {
                System.err.println("active participant failed: " + webResponse.getError().getErrorMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("active participant error");
        }
    }

    public WebResponse active() throws Exception {
        String url = (secure ? "https://" : "http://") + host + ":" + port + "/management/delegate/activeparticipant";
        HttpPost httpPost = new HttpPost(url);
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("ledgerHash", ledger));
        params.add(new BasicNameValuePair("consensusHost", host));
        params.add(new BasicNameValuePair("consensusPort", consensusPort + ""));
        params.add(new BasicNameValuePair("consensusSecure", consensusSecure + ""));
        params.add(new BasicNameValuePair("remoteManageHost", synHost));
        params.add(new BasicNameValuePair("remoteManagePort", synPort + ""));
        params.add(new BasicNameValuePair("remoteManageSecure", synSecure + ""));
        params.add(new BasicNameValuePair("shutdown", shutdown + ""));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        ServiceEndpoint endpoint = new ServiceEndpoint(host, port, secure);
        if (secure) {
            endpoint.setSslSecurity(new SSLSecurity(participant.keyStoreType, participant.keyStore, participant.keyAlias, participant.keyStorePassword,
                    participant.trustStore, participant.trustStorePassword, participant.trustStoreType));
        } else {
            endpoint.setSslSecurity(new SSLSecurity());
        }
        HttpResponse response = ServiceConnectionManager.buildHttpClient(endpoint).execute(httpPost);
        return (WebResponse) new JsonResponseConverter(WebResponse.class).getResponse(null, response.getEntity().getContent(), null);
    }
}

@CommandLine.Command(name = "update", mixinStandardHelpOptions = true, header = "Update participant.")
class ParticipantUpdate implements Runnable {

    @CommandLine.Option(names = "--ledger", required = true, description = "Set the ledger.", scope = CommandLine.ScopeType.INHERIT)
    String ledger;

    @CommandLine.Option(names = "--host", required = true, description = "Set the participant host.", scope = CommandLine.ScopeType.INHERIT)
    String host;

    @CommandLine.Option(names = "--port", required = true, description = "Set the participant service port.", scope = CommandLine.ScopeType.INHERIT)
    int port;

    @CommandLine.Option(names = "--secure", description = "Secure of participant service service.", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean secure;

    @CommandLine.Option(names = "--consensus-port", required = true, description = "Set the participant consensus port.", scope = CommandLine.ScopeType.INHERIT)
    int consensusPort;

    @CommandLine.Option(names = "--consensus-secure", description = "Whether to open the secure connection for consensus.", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean consensusSecure;

    @CommandLine.Option(names = "--shutdown", description = "Restart the node server.", scope = CommandLine.ScopeType.INHERIT)
    boolean shutdown;

    @CommandLine.ParentCommand
    private Participant participant;

    @Override
    public void run() {
        // TODO valid params
        try {
            WebResponse webResponse = update();
            if (webResponse.isSuccess()) {
                System.out.println("participant updated");
            } else {
                System.err.println("update participant failed: " + webResponse.getError().getErrorMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("update participant error: " + e.getMessage());
        }
    }

    public WebResponse update() throws Exception {
        String url = (secure ? "https://" : "http//") + host + ":" + port + "/management/delegate/updateparticipant";
        HttpPost httpPost = new HttpPost(url);
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("ledgerHash", ledger));
        params.add(new BasicNameValuePair("consensusHost", host));
        params.add(new BasicNameValuePair("consensusPort", consensusPort + ""));
        params.add(new BasicNameValuePair("consensusSecure", consensusSecure + ""));
        params.add(new BasicNameValuePair("shutdown", shutdown + ""));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        ServiceEndpoint endpoint = new ServiceEndpoint(host, port, secure);
        if (secure) {
            endpoint.setSslSecurity(new SSLSecurity(participant.keyStoreType, participant.keyStore, participant.keyAlias, participant.keyStorePassword,
                    participant.trustStore, participant.trustStorePassword, participant.trustStoreType));
        } else {
            endpoint.setSslSecurity(new SSLSecurity());
        }
        HttpResponse response = ServiceConnectionManager.buildHttpClient(endpoint).execute(httpPost);
        return (WebResponse) new JsonResponseConverter(WebResponse.class).getResponse(null, response.getEntity().getContent(), null);
    }
}

@CommandLine.Command(name = "inactive", mixinStandardHelpOptions = true, header = "Inactive participant.")
class ParticipantInactive implements Runnable {

    @CommandLine.Option(names = "--ledger", required = true, description = "Set the ledger.", scope = CommandLine.ScopeType.INHERIT)
    String ledger;

    @CommandLine.Option(names = "--address", required = true, description = "Set the participant address.", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--host", required = true, description = "Set the participant host.", scope = CommandLine.ScopeType.INHERIT)
    String host;

    @CommandLine.Option(names = "--port", required = true, description = "Set the participant service port.", scope = CommandLine.ScopeType.INHERIT)
    int port;

    @CommandLine.Option(names = "--secure", description = "Secure of participant service service.", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean secure;

    @CommandLine.ParentCommand
    private Participant participant;

    @Override
    public void run() {
        // TODO valid params
        try {
            String url = (secure ? "https://" : "http//") + host + ":" + port + "/management/delegate/deactiveparticipant";
            HttpPost httpPost = new HttpPost(url);
            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("ledgerHash", ledger));
            params.add(new BasicNameValuePair("participantAddress", address));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            ServiceEndpoint endpoint = new ServiceEndpoint(host, port, secure);
            if (secure) {
                endpoint.setSslSecurity(new SSLSecurity(participant.keyStoreType, participant.keyStore, participant.keyAlias, participant.keyStorePassword,
                        participant.trustStore, participant.trustStorePassword, participant.trustStoreType));
            } else {
                endpoint.setSslSecurity(new SSLSecurity());
            }
            HttpResponse response = ServiceConnectionManager.buildHttpClient(endpoint).execute(httpPost);
            WebResponse webResponse = (WebResponse) new JsonResponseConverter(WebResponse.class).getResponse(null, response.getEntity().getContent(), null);
            if (webResponse.isSuccess()) {
                System.out.println("participant inactivated");
            } else {
                System.err.println("inactive participant failed: " + webResponse.getError().getErrorMessage());
            }
        } catch (Exception e) {
            System.out.println("active participant error: " + e.getMessage());
        }
    }
}
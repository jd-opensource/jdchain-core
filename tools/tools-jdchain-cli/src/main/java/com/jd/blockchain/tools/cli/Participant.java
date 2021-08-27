package com.jd.blockchain.tools.cli;

import com.jd.blockchain.ca.X509Utils;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.ledger.PreparedTransaction;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionTemplate;
import com.jd.blockchain.sdk.BlockchainService;
import com.jd.blockchain.sdk.client.GatewayServiceFactory;
import com.jd.httpservice.converters.JsonResponseConverter;
import com.jd.httpservice.utils.web.WebResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import picocli.CommandLine;
import utils.io.FileUtils;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

    @CommandLine.Option(names = "--ca-mode", description = "Register with CA", scope = CommandLine.ScopeType.INHERIT)
    boolean caMode;

    @CommandLine.Option(required = true, names = "--name", description = "Name of the participant")
    String name;
    BlockchainService blockchainService;
    @CommandLine.ParentCommand
    private Participant participant;

    BlockchainService getChainService() {
        if (null == blockchainService) {
            blockchainService = GatewayServiceFactory.connect(gwHost, gwPort, false).getBlockchainService();
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
        boolean ok = true;
        File keysHome = new File(participant.jdChainCli.path.getAbsolutePath() + File.separator + Keys.KEYS_HOME);
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
            Scanner scanner = new Scanner(System.in).useDelimiter("\n");
            int keyIndex = Integer.parseInt(scanner.next().trim());
            System.out.println("input password of the key: ");
            System.out.print("> ");
            String pwd = scanner.next();
            if (KeyGenUtils.encodePasswordAsBase58(pwd).equals(passwords[keyIndex])) {
                ptx.sign(keypairs[keyIndex]);
            } else {
                System.err.println("password wrong");
                ok = false;
            }
        }

        return ok;
    }

    @Override
    public void run() {
        TransactionTemplate txTemp = newTransaction();
        BlockchainKeypair keypair;
        File keysHome = new File(participant.jdChainCli.path.getAbsolutePath() + File.separator + Keys.KEYS_HOME);
        File[] pubs = keysHome.listFiles((dir, name) -> {
            if (name.endsWith(".priv")) {
                return true;
            }
            return false;
        });
        if (pubs.length == 0) {
            System.err.printf("no keypair in path [%s]%n", keysHome.getAbsolutePath());
            return;
        } else {
            System.out.println("select keypair to register, input the index: ");
            BlockchainKeypair[] keypairs = new BlockchainKeypair[pubs.length];
            String[] certs = new String[pubs.length];
            String[] passwords = new String[pubs.length];
            for (int i = 0; i < pubs.length; i++) {
                String key = FilenameUtils.removeExtension(pubs[i].getName());
                String keyPath = FilenameUtils.removeExtension(pubs[i].getAbsolutePath());
                String privkey = FileUtils.readText(new File(keyPath + ".priv"));
                String pwd = FileUtils.readText(new File(keyPath + ".pwd"));
                String pubkey = FileUtils.readText(new File(keyPath + ".pub"));
                certs[i] = keyPath + ".crt";
                keypairs[i] = new BlockchainKeypair(KeyGenUtils.decodePubKey(pubkey), KeyGenUtils.decodePrivKey(privkey, pwd));
                passwords[i] = pwd;
                System.out.printf("%-3s\t%s\t%s%n", i, key, keypairs[i].getAddress());
            }
            Scanner scanner = new Scanner(System.in).useDelimiter("\n");
            int keyIndex = Integer.parseInt(scanner.next().trim());
            System.out.println("input password of the key: ");
            System.out.print("> ");
            String pwd = scanner.next();
            if (KeyGenUtils.encodePasswordAsBase58(pwd).equals(passwords[keyIndex])) {
                // new participant keypair
                keypair = keypairs[keyIndex];
                if (caMode) {
                    X509Certificate cert = X509Utils.resolveCertificate(FileUtils.readText(new File(certs[keyIndex])));
                    txTemp.participants().register(name, cert);
                } else {
                    txTemp.participants().register(name, keypair.getIdentity());
                }
                PreparedTransaction ptx = txTemp.prepare();
                if (sign(ptx)) {
                    TransactionResponse response = ptx.commit();
                    if (response.isSuccess()) {
                        System.out.printf("register participant: [%s]%n", keypair.getAddress());
                    } else {
                        System.err.println("register participant failed!");
                    }
                }
            } else {
                System.err.println("password wrong");
                return;
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

    @CommandLine.Option(names = "--consensus-port", required = true, description = "Set the participant consensus port.", scope = CommandLine.ScopeType.INHERIT)
    int consensusPort;

    @CommandLine.Option(names = "--syn-host", required = true, description = "Set synchronization participant host.", scope = CommandLine.ScopeType.INHERIT)
    String synHost;

    @CommandLine.Option(names = "--syn-port", required = true, description = "Set synchronization participant port.", scope = CommandLine.ScopeType.INHERIT)
    int synPort;

    @CommandLine.Option(names = "--shutdown", description = "Restart the node server.", scope = CommandLine.ScopeType.INHERIT)
    boolean shutdown;

    @Override
    public void run() {
        // TODO valid params
        try {
            WebResponse webResponse = update();
            if (webResponse.isSuccess()) {
                System.out.println("participant activated");
            } else {
                System.err.println("active participant failed: " + webResponse.getError().getErrorMessage());
            }
        } catch (Exception e) {
            System.out.println("active participant error: " + e.getMessage());
        }
    }

    public WebResponse update() throws Exception {
        String url = "http://" + host + ":" + port + "/management/delegate/activeparticipant";
        HttpPost httpPost = new HttpPost(url);
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("ledgerHash", ledger));
        params.add(new BasicNameValuePair("consensusHost", host));
        params.add(new BasicNameValuePair("consensusPort", consensusPort + ""));
        params.add(new BasicNameValuePair("remoteManageHost", synHost));
        params.add(new BasicNameValuePair("remoteManagePort", synPort + ""));
        params.add(new BasicNameValuePair("shutdown", shutdown + ""));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(httpPost);
        return (WebResponse) new JsonResponseConverter(WebResponse.class).getResponse(null, response.getEntity().getContent(), null);
    }
}

@CommandLine.Command(name = "update", mixinStandardHelpOptions = true, header = "Update participant.")
class ParticipantUpdate extends ParticipantActive {
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

    @CommandLine.Option(names = "--syn-host", required = true, description = "Set synchronization participant host.", scope = CommandLine.ScopeType.INHERIT)
    String synHost;

    @CommandLine.Option(names = "--syn-port", required = true, description = "Set synchronization participant port.", scope = CommandLine.ScopeType.INHERIT)
    int synPort;

    @Override
    public void run() {
        // TODO valid params
        try {
            String url = "http://" + host + ":" + port + "/management/delegate/deactiveparticipant";
            HttpPost httpPost = new HttpPost(url);
            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("ledgerHash", ledger));
            params.add(new BasicNameValuePair("participantAddress", address));
            params.add(new BasicNameValuePair("remoteManageHost", synHost));
            params.add(new BasicNameValuePair("remoteManagePort", synPort + ""));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpClient httpClient = HttpClients.createDefault();
            HttpResponse response = httpClient.execute(httpPost);
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
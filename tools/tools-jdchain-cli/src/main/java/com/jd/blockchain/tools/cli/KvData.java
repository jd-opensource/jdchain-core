package com.jd.blockchain.tools.cli;

import com.jd.httpservice.agent.ServiceConnectionManager;
import com.jd.httpservice.agent.ServiceEndpoint;
import com.jd.httpservice.converters.JsonResponseConverter;
import com.jd.httpservice.utils.web.WebResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import picocli.CommandLine;
import utils.crypto.sm.GmSSLProvider;
import utils.net.SSLSecurity;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: kvdata operations
 * @Author: zhangshuang
 * @Date: 2022/6/6 2:08 PM
 * @version: Version 1.0
 */

@CommandLine.Command(name = "kvdata",
        mixinStandardHelpOptions = true,
        showDefaultValues = true,
        description = "Archive, recovery kvdata.",
        subcommands = {
                KvDataArchive.class,
                KvDataRecovery.class,
                CommandLine.HelpCommand.class
        }
)
public class KvData implements Runnable {

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

    @CommandLine.ParentCommand
    JDChainCli jdChainCli;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    SSLSecurity getSSLSecurity() {
        return new SSLSecurity(keyStoreType, keyStore, keyAlias, keyStorePassword, trustStore, trustStorePassword, trustStoreType, protocol, enabledProtocols, ciphers, hostNameVerifier);
    }

    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }
}


@CommandLine.Command(name = "archive", mixinStandardHelpOptions = true, header = "Archive kvdata.")
class KvDataArchive implements Runnable {

    @CommandLine.Option(names = "--ledger", required = true, description = "Set the ledger.", scope = CommandLine.ScopeType.INHERIT)
    String ledger;

    @CommandLine.Option(names = "--host", required = true, description = "Set the peer service host.", scope = CommandLine.ScopeType.INHERIT)
    String host;

    @CommandLine.Option(names = "--port", required = true, description = "Set the peer service port.", scope = CommandLine.ScopeType.INHERIT)
    int port;

    @CommandLine.Option(names = "--from", required = true, description = "Set the start block height of kvdata archive.", scope = CommandLine.ScopeType.INHERIT)
    int startHeight;

    @CommandLine.Option(names = "--to", required = true, description = "Set the end block height of kvdata archive.", scope = CommandLine.ScopeType.INHERIT)
    int endHeight;

    @CommandLine.Option(names = "--secure", description = "Secure of peer service.", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean secure;

    @CommandLine.ParentCommand
    private KvData kvData;

    @Override
    public void run() {
        try {
            String url = (secure ? "https://" : "http://") + host + ":" + port + "/management/delegate/kvdataarchive";
            HttpPost httpPost = new HttpPost(url);
            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("ledgerHash", ledger));
            params.add(new BasicNameValuePair("fromHeight", String.valueOf(startHeight)));
            params.add(new BasicNameValuePair("toHeight", String.valueOf(endHeight)));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            ServiceEndpoint endpoint = new ServiceEndpoint(host, port, secure);
            if (secure) {
                GmSSLProvider.enableGMSupport(kvData.getSSLSecurity().getProtocol());
                endpoint.setSslSecurity(kvData.getSSLSecurity());
            } else {
                endpoint.setSslSecurity(new SSLSecurity());
            }
            HttpResponse response = ServiceConnectionManager.buildHttpClient(endpoint).execute(httpPost);
            WebResponse webResponse = (WebResponse) new JsonResponseConverter(WebResponse.class).getResponse(null, response.getEntity().getContent(), null);
            if (webResponse.isSuccess()) {
                System.out.println("kvdata archive succ");
            } else {
                System.err.println("kvdata archive failed: " + webResponse.getError().getErrorMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("kvdata archive error");
        }
    }
}

@CommandLine.Command(name = "recovery", mixinStandardHelpOptions = true, header = "Recovery kvdata.")
class KvDataRecovery implements Runnable {

    @CommandLine.Option(names = "--ledger", required = true, description = "Set the ledger.", scope = CommandLine.ScopeType.INHERIT)
    String ledger;

    @CommandLine.Option(names = "--host", required = true, description = "Set the peer service host.", scope = CommandLine.ScopeType.INHERIT)
    String host;

    @CommandLine.Option(names = "--port", required = true, description = "Set the peer service port.", scope = CommandLine.ScopeType.INHERIT)
    int port;

    @CommandLine.Option(names = "--from", required = true, description = "Set the start block height of kvdata recovery.", scope = CommandLine.ScopeType.INHERIT)
    int startHeight;

    @CommandLine.Option(names = "--to", required = true, description = "Set the end block height of kvdata recovery.", scope = CommandLine.ScopeType.INHERIT)
    int endHeight;

    @CommandLine.Option(names = "--secure", description = "Secure of peer service.", defaultValue = "false", scope = CommandLine.ScopeType.INHERIT)
    boolean secure;

    @CommandLine.ParentCommand
    private KvData kvData;

    @Override
    public void run() {
        try {
            String url = (secure ? "https://" : "http://") + host + ":" + port + "/management/delegate/kvdatarecovery";
            HttpPost httpPost = new HttpPost(url);
            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("ledgerHash", ledger));
            params.add(new BasicNameValuePair("fromHeight", String.valueOf(startHeight)));
            params.add(new BasicNameValuePair("toHeight", String.valueOf(endHeight)));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            ServiceEndpoint endpoint = new ServiceEndpoint(host, port, secure);
            if (secure) {
                GmSSLProvider.enableGMSupport(kvData.getSSLSecurity().getProtocol());
                endpoint.setSslSecurity(kvData.getSSLSecurity());
            } else {
                endpoint.setSslSecurity(new SSLSecurity());
            }
            HttpResponse response = ServiceConnectionManager.buildHttpClient(endpoint).execute(httpPost);
            WebResponse webResponse = (WebResponse) new JsonResponseConverter(WebResponse.class).getResponse(null, response.getEntity().getContent(), null);
            if (webResponse.isSuccess()) {
                System.out.println("kvdata recovery succ");
            } else {
                System.err.println("kvdata recovery failed: " + webResponse.getError().getErrorMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("kvdata recovery error: " + e.getMessage());
        }
    }
}
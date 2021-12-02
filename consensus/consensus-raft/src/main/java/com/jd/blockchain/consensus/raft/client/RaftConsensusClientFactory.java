package com.jd.blockchain.consensus.raft.client;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consensus.client.ClientFactory;
import com.jd.blockchain.consensus.client.ClientSettings;
import com.jd.blockchain.consensus.client.ConsensusClient;
import com.jd.blockchain.consensus.manage.ConsensusManageClient;
import com.jd.blockchain.consensus.manage.ManageClientFactory;
import com.jd.blockchain.consensus.raft.config.RaftClientConfig;
import com.jd.blockchain.consensus.raft.settings.RaftClientIncomingSettings;
import com.jd.blockchain.consensus.raft.settings.RaftClientSettings;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.SignatureDigest;
import com.jd.blockchain.crypto.SignatureFunction;
import utils.net.SSLSecurity;

import java.security.cert.X509Certificate;
import java.util.Optional;

public class RaftConsensusClientFactory implements ClientFactory, ManageClientFactory {

    @Override
    public ClientCredential buildCredential(SessionCredential sessionCredential, AsymmetricKeypair clientKeyPair) {
        return buildCredential(sessionCredential, clientKeyPair, null);
    }


    @Override
    public ClientCredential buildCredential(SessionCredential sessionCredential, AsymmetricKeypair clientKeyPair, X509Certificate gatewayCertificate) {

        if (sessionCredential == null) {
            sessionCredential = RaftSessionCredentialConfig.createEmptyCredential();
        }

        if (!(sessionCredential instanceof RaftSessionCredential)) {
            throw new IllegalArgumentException(
                    "Illegal credential info type! Requrie [" + RaftSessionCredential.class.getName()
                            + "] but it is [" + sessionCredential.getClass().getName() + "]!");
        }

        SignatureFunction signatureFunction = Crypto.getSignatureFunction(clientKeyPair.getAlgorithm());

        byte[] credentialBytes = BinaryProtocol.encode(sessionCredential, RaftSessionCredential.class);
        SignatureDigest signatureDigest = signatureFunction.sign(clientKeyPair.getPrivKey(), credentialBytes);

        RaftClientCredential identification = new RaftClientCredential();

        identification.setIdentityInfo(sessionCredential);
        identification.setPubKey(clientKeyPair.getPubKey());
        identification.setSignature(signatureDigest);
        identification.setCertificate(Optional.ofNullable(gatewayCertificate).map(CertificateUtils::toPEMString).orElse(null));

        return identification;
    }


    @Override
    public ClientSettings buildClientSettings(ClientIncomingSettings incomingSettings) {

        if (!(incomingSettings instanceof RaftClientIncomingSettings)) {
            throw new IllegalStateException("incoming settings should be raft-client-incoming-settings type");
        }

        return new RaftClientConfig((RaftClientIncomingSettings) incomingSettings);
    }


    @Override
    public ClientSettings buildClientSettings(ClientIncomingSettings incomingSettings, SSLSecurity sslSecurity) {
        //todo
        return buildClientSettings(incomingSettings);
    }


    @Override
    public ConsensusClient setupClient(ClientSettings settings) {

        if (!(settings instanceof RaftClientSettings)) {
            throw new IllegalStateException("client settings should be raft-client-settings type");
        }

        return new RaftConsensusClient((RaftClientSettings) settings);
    }


    @Override
    public ConsensusClient setupClient(ClientSettings settings, String ledgerHash) {
        RaftConsensusClient raftConsensusClient = (RaftConsensusClient) this.setupClient(settings);
        raftConsensusClient.setLedgerHash(ledgerHash);
        return raftConsensusClient;
    }

    @Override
    public ConsensusManageClient setupManageClient(ClientSettings settings) {

        if (!(settings instanceof RaftClientSettings)) {
            throw new IllegalStateException("manager client settings should be raft-client-settings type");
        }

        return new RaftConsensusClient((RaftClientSettings) settings);
    }
}

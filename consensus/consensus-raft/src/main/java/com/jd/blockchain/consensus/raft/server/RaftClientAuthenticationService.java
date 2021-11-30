package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.closure.ReadIndexClosure;
import com.jd.binaryproto.BinaryProtocol;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.ConsensusSecurityException;
import com.jd.blockchain.consensus.raft.client.RaftSessionCredentialConfig;
import com.jd.blockchain.consensus.raft.client.RaftSessionCredential;
import com.jd.blockchain.consensus.raft.config.RaftClientIncomingConfig;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.SignatureFunction;
import utils.Bytes;
import utils.io.BytesUtils;

public class RaftClientAuthenticationService implements ClientAuthencationService {

    static {
        DataContractRegistry.register(ClientCredential.class);
        DataContractRegistry.register(RaftSessionCredential.class);
    }

    private RaftNodeServer raftNodeServer;

    public RaftClientAuthenticationService(RaftNodeServer raftNodeServer) {
        this.raftNodeServer = raftNodeServer;
    }

    @Override
    public ClientIncomingSettings authencateIncoming(ClientCredential credential) throws ConsensusSecurityException {

        if (!verify(credential)) {
            return null;
        }

        if(!raftNodeServer.isRunning()){
            throw new IllegalStateException("node server not start");
        }

        RaftClientIncomingConfig config = new RaftClientIncomingConfig();

        config.setPubKey(config.getPubKey());
        config.setClientId(0); //TODO
        config.setConsensusSettings(raftNodeServer.getServerSettings().getConsensusSettings());
        config.setSessionCredential(RaftSessionCredentialConfig.createEmptyCredential());
        config.setCurrentPeers(raftNodeServer.getCurrentPeerEndpoints());
        return config;
    }


    private boolean verify(ClientCredential credential) {
        byte[] credentialBytes = BinaryProtocol.encode(credential.getSessionCredential(),
                RaftSessionCredential.class);
        SignatureFunction signatureFunction = Crypto.getSignatureFunction(credential.getPubKey().getAlgorithm());

        return signatureFunction.verify(credential.getSignature(), credential.getPubKey(), credentialBytes);
    }

}

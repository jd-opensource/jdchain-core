package com.jd.blockchain.consensus.mq.server;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.ClientAuthencationService;
import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.consensus.mq.client.MQCredentialInfo;
import com.jd.blockchain.consensus.mq.config.MsgQueueClientIncomingConfig;
import com.jd.blockchain.consensus.mq.settings.MsgQueueClientIncomingSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.crypto.SignatureFunction;

public class MsgQueueConsensusManageService implements ClientAuthencationService {

    private MsgQueueConsensusSettings consensusSettings;

    public MsgQueueConsensusManageService setConsensusSettings(MsgQueueConsensusSettings consensusSettings) {
        this.consensusSettings = consensusSettings;
        return this;
    }

    @Override
    public MsgQueueClientIncomingSettings authencateIncoming(ClientCredential authId) {
        if (isLegal(authId)) {
            return new MsgQueueClientIncomingConfig().setPubKey(authId.getPubKey())
                    .setClientId(clientId(null)).setConsensusSettings(this.consensusSettings)
                    .setSessionCredential(authId.getSessionCredential());
        }
        return null;
    }

    private int clientId(byte[] identityInfo) {
        return 0;
    }

    public boolean isLegal(ClientCredential authId) {
        PubKey pubKey = authId.getPubKey();
        byte[] identityInfo = BinaryProtocol.encode(authId.getSessionCredential(), MQCredentialInfo.class);
        SignatureFunction signatureFunction = Crypto.getSignatureFunction(pubKey.getAlgorithm());
        return signatureFunction.verify(authId.getSignature(), pubKey, identityInfo);
    }
}
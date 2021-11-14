package com.jd.blockchain.consensus.bftsmart.service;

import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusViewSettings;
import utils.net.SSLSecurity;

public class BftsmartServerSettingConfig implements BftsmartServerSettings {
    private NodeSettings nodeSettings;
    private String realmName;
    private BftsmartConsensusViewSettings consensusSettings;
    private SSLSecurity sslSecurity;

    @Override
    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }


    @Override
    public NodeSettings getReplicaSettings() {
        return nodeSettings;
    }

    public void setReplicaSettings(NodeSettings nodeSettings) {
        this.nodeSettings = nodeSettings;
    }


    @Override
    public BftsmartConsensusViewSettings getConsensusSettings() {
        return consensusSettings;
    }

    public void setSslSecurity(SSLSecurity sslSecurity) {
        this.sslSecurity = sslSecurity;
    }

    @Override
    public SSLSecurity getSslSecurity() {
        return sslSecurity;
    }

    public void setConsensusSettings(BftsmartConsensusViewSettings consensusSettings) {
        this.consensusSettings = consensusSettings;
    }
}

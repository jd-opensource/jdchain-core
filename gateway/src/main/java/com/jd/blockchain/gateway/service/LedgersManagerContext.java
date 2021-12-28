package com.jd.blockchain.gateway.service;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.gateway.GatewayConfigProperties;
import com.jd.blockchain.gateway.service.topology.LedgerPeersTopologyStorage;
import com.jd.blockchain.sdk.service.ConsensusClientManager;
import com.jd.blockchain.sdk.service.SessionCredentialProvider;
import utils.net.SSLSecurity;

/**
 * 账本-共识节点连接管理上下文
 */
public class LedgersManagerContext {

    private AsymmetricKeypair keyPair;
    private GatewayConfigProperties configProperties;
    private ConsensusClientManager clientManager;
    private SessionCredentialProvider sessionCredentialProvider;
    private LedgerPeersTopologyStorage topologyStorage;

    // peer管理服务TLS配置信息
    private SSLSecurity manageSslSecurity;
    // peer共识服务TLS配置信息
    private SSLSecurity consensusSslSecurity;

    public LedgersManagerContext(AsymmetricKeypair keyPair, GatewayConfigProperties configProperties, ConsensusClientManager clientManager, SessionCredentialProvider sessionCredentialProvider,
                                 LedgerPeersTopologyStorage topologyStorage, SSLSecurity manageSslSecurity, SSLSecurity consensusSslSecurity) {
        this.keyPair = keyPair;
        this.configProperties = configProperties;
        this.clientManager = clientManager;
        this.sessionCredentialProvider = sessionCredentialProvider;
        this.topologyStorage = topologyStorage;
        this.manageSslSecurity = manageSslSecurity;
        this.consensusSslSecurity = consensusSslSecurity;
    }

    public ConsensusClientManager getClientManager() {
        return clientManager;
    }

    public void setClientManager(ConsensusClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public SessionCredentialProvider getSessionCredentialProvider() {
        return sessionCredentialProvider;
    }

    public void setSessionCredentialProvider(SessionCredentialProvider sessionCredentialProvider) {
        this.sessionCredentialProvider = sessionCredentialProvider;
    }

    public LedgerPeersTopologyStorage getTopologyStorage() {
        return topologyStorage;
    }

    public void setTopologyStorage(LedgerPeersTopologyStorage topologyStorage) {
        this.topologyStorage = topologyStorage;
    }

    public AsymmetricKeypair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(AsymmetricKeypair keyPair) {
        this.keyPair = keyPair;
    }

    public SSLSecurity getManageSslSecurity() {
        return manageSslSecurity;
    }

    public void setManageSslSecurity(SSLSecurity manageSslSecurity) {
        this.manageSslSecurity = manageSslSecurity;
    }

    public SSLSecurity getConsensusSslSecurity() {
        return consensusSslSecurity;
    }

    public void setConsensusSslSecurity(SSLSecurity consensusSslSecurity) {
        this.consensusSslSecurity = consensusSslSecurity;
    }

    public boolean isAwareTopology() {
        return this.configProperties.isAwareTopology();
    }

    public boolean isStoreTopology() {
        return this.configProperties.isStoreTopology();
    }

    public int getTopologyAwareInterval() {
        return this.configProperties.getAwareTopologyInterval();
    }

    public int getPeerConnectionPin() {
        return this.configProperties.getPeerConnectionPin();
    }

    public int getPeerConnectionAuth() {
        return this.configProperties.getPeerConnectionAuth();
    }
}

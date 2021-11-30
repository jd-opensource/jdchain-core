package com.jd.blockchain.consensus.raft;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.SettingsFactory;
import com.jd.blockchain.consensus.client.ClientFactory;
import com.jd.blockchain.consensus.manage.ManageClientFactory;
import com.jd.blockchain.consensus.raft.client.RaftConsensusClientFactory;
import com.jd.blockchain.consensus.raft.server.RaftNodeServerFactory;
import com.jd.blockchain.consensus.raft.settings.RaftSettingsFactory;
import com.jd.blockchain.consensus.service.NodeServerFactory;

public class RaftConsensusProvider implements ConsensusProvider {

    public static final String PROVIDER_NAME = RaftConsensusProvider.class.getName();

    private static final SettingsFactory SETTINGS_FACTORY = new RaftSettingsFactory();

    private static final RaftConsensusClientFactory CLIENT_FACTORY = new RaftConsensusClientFactory();

    private static final NodeServerFactory NODE_SERVER_FACTORY = new RaftNodeServerFactory();


    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public SettingsFactory getSettingsFactory() {
        return SETTINGS_FACTORY;
    }

    @Override
    public ClientFactory getClientFactory() {
        return CLIENT_FACTORY;
    }

    @Override
    public ManageClientFactory getManagerClientFactory() {
        return CLIENT_FACTORY;
    }

    @Override
    public NodeServerFactory getServerFactory() {
        return NODE_SERVER_FACTORY;
    }
}

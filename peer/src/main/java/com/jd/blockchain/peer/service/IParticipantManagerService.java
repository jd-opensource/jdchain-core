package com.jd.blockchain.peer.service;

import com.jd.blockchain.consensus.ConsensusProvider;
import com.jd.blockchain.consensus.ConsensusProviders;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.peer.web.ManagementController;
import com.jd.httpservice.utils.web.WebResponse;
import utils.Bytes;
import utils.Property;
import utils.net.NetworkAddress;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Properties;

public interface IParticipantManagerService {

    String RAFT_CONSENSUS_NODE_STORAGE = "RAFT_CONSENSUS_NODE_STORAGE";

    default boolean supportManagerParticipant() {
        return true;
    }

    default ConsensusViewSettings getConsensusSetting(ParticipantContext context) {
        ConsensusProvider provider = ConsensusProviders.getProvider(context.provider());
        Bytes csSettingBytes = context.ledgerAdminInfo().getSettings().getConsensusSetting();
        return provider.getSettingsFactory().getConsensusSettingsEncoder().decode(csSettingBytes.toBytes());
    }

    default String keyOfNode(String pattern, int id) {
        return String.format(pattern, id);
    }

    int minConsensusNodes();

    Properties getCustomProperties(ParticipantContext context);

    Property[] createActiveProperties(NetworkAddress address, PubKey activePubKey, int activeID, Properties customProperties);

    Property[] createUpdateProperties(NetworkAddress address, PubKey activePubKey, int activeID, Properties customProperties);

    Property[] createDeactiveProperties(PubKey deActivePubKey, int deActiveID, Properties customProperties);

    /**
     * 提交节点状态变更交易
     */
    TransactionResponse submitNodeStateChangeTx(ParticipantContext context, TransactionRequest txRequest, List<NodeSettings> origConsensusNodes);

    boolean startServerBeforeApplyNodeChange();

    /**
     * 使用共识原语管理共识节点
     * */
    WebResponse applyConsensusGroupNodeChange(ParticipantContext context,
                                              ParticipantNode node,
                                              @Nullable NetworkAddress changeNetworkAddress,
                                              List<NodeSettings> origConsensusNodes,
                                              ManagementController.ParticipantUpdateType type);
}

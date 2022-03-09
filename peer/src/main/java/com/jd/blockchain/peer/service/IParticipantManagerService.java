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

    /**
     * 共识最小节点数
     */
    int minConsensusNodes();

    /**
     * 获取共识自定义配置属性
     */
    Properties getCustomProperties(ParticipantContext context);

    /**
     * 组装新增共识节点属性
     *
     * @param address
     * @param activePubKey
     * @param activeID
     * @param customProperties
     */
    Property[] createActiveProperties(NetworkAddress address, PubKey activePubKey, int activeID, Properties customProperties);

    /**
     * 组装更新共识节点属性
     *
     * @param address
     * @param activePubKey
     * @param activeID
     * @param customProperties
     */
    Property[] createUpdateProperties(NetworkAddress address, PubKey activePubKey, int activeID, Properties customProperties);

    /**
     * 组装删除共识节点属性
     *
     * @param deActivePubKey
     * @param deActiveID
     * @param customProperties
     */
    Property[] createDeactiveProperties(PubKey deActivePubKey, int deActiveID, Properties customProperties);

    /**
     * 提交节点状态变更交易:
     * a. ParticipantStateUpdateOperation: participant state update
     * b. ConsensusSettingsUpdateOperation: consensus settings update
     */
    TransactionResponse submitNodeStateChangeTx(ParticipantContext context, int activeID, TransactionRequest txRequest, List<NodeSettings> origConsensusNodes);

    boolean startServerBeforeApplyNodeChange();

    /**
     * 使用共识原语管理共识节点
     */
    WebResponse applyConsensusGroupNodeChange(ParticipantContext context,
                                              ParticipantNode node,
                                              @Nullable NetworkAddress changeConsensusNodeAddress,
                                              List<NodeSettings> origConsensusNodes,
                                              ManagementController.ParticipantUpdateType type);
}

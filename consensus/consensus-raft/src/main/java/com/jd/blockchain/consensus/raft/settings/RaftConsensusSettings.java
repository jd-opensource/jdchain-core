
package com.jd.blockchain.consensus.raft.settings;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consts.DataCodes;


@DataContract(code = DataCodes.CONSENSUS_RAFT_VIEW_SETTINGS)
public interface RaftConsensusSettings extends ConsensusViewSettings {

    @DataField(order=1, primitiveType=PrimitiveType.INT32)
    int getMaxTxsPerBlock();

    @DataField(order=2, primitiveType=PrimitiveType.INT32)
    int getMaxBlockBytes();

    @DataField(order = 3, primitiveType = PrimitiveType.INT32)
    int getElectionTimeoutMs();

    @DataField(order = 4, primitiveType = PrimitiveType.INT32)
    int getSnapshotIntervalSec();

    @DataField(order = 5, primitiveType = PrimitiveType.INT32)
    int getRefreshConfigurationMs();

    @DataField(order = 6, refContract = true)
    RaftNetworkSettings getNetworkSettings();

    @DataField(order = 7, refContract = true)
    RaftSettings getRaftSettings();

}
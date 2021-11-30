package com.jd.blockchain.consensus.raft.settings;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.service.ServerSettings;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_RAFT_SERVER_SETTINGS_INFO)
public interface RaftServerSettings extends ServerSettings {

    @DataField(order = 3, refContract = true)
    RaftConsensusSettings getConsensusSettings();

    default RaftNetworkSettings getRaftNetworkSettings(){
        return getConsensusSettings().getNetworkSettings();
    }

    default RaftSettings getRaftSettings(){
        return getConsensusSettings().getRaftSettings();
    }

    default int getMaxTxsPerBlock(){
        return getConsensusSettings().getMaxTxsPerBlock();
    }

    default int getMaxBlockBytes(){
        return getConsensusSettings().getMaxBlockBytes();
    }

    default int getElectionTimeoutMs(){
        return getConsensusSettings().getElectionTimeoutMs();
    }

    default int getSnapshotIntervalSec(){
        return getConsensusSettings().getSnapshotIntervalSec();
    }

    default RaftNodeSettings getRaftNodeSettings(){
        return (RaftNodeSettings) getReplicaSettings();
    }
}


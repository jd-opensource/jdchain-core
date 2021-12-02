package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.Closure;
import com.jd.blockchain.consensus.raft.rpc.ParticipantNodeAddRequest;
import com.jd.blockchain.consensus.raft.rpc.ParticipantNodeRemoveRequest;
import com.jd.blockchain.consensus.raft.rpc.ParticipantNodeTransferRequest;
import com.jd.blockchain.consensus.raft.rpc.SubmitTxRequest;

public interface RaftNodeServerService {

    void handleSubmitTxRequest(SubmitTxRequest submitTxRequest, Closure done);

    void publishBlockEvent();

    RaftNodeServer getNodeServer();

    void addParticipantNode(ParticipantNodeAddRequest request, Closure done);

    void removeParticipantNode(ParticipantNodeRemoveRequest request, Closure done);

    void transferParticipantNode(ParticipantNodeTransferRequest request, Closure done);
}

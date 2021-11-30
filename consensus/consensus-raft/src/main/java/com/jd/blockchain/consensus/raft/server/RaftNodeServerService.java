package com.jd.blockchain.consensus.raft.server;

import com.alipay.sofa.jraft.Closure;
import com.jd.blockchain.consensus.raft.rpc.ParticipantNodeChangeRequest;
import com.jd.blockchain.consensus.raft.rpc.SubmitTxRequest;

public interface RaftNodeServerService {

    void handleSubmitTxRequest(SubmitTxRequest submitTxRequest, Closure done);

    void handleParticipantNodeChangeRequest(ParticipantNodeChangeRequest request, Closure done);

    void publishBlockEvent();

    RaftNodeServer getNodeServer();
}

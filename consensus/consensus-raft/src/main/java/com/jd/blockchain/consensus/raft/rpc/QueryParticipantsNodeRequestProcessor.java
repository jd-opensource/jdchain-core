package com.jd.blockchain.consensus.raft.rpc;

import com.alipay.sofa.jraft.Status;
import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.raft.server.RaftNodeServerService;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.core.LedgerRepository;
import com.jd.blockchain.peer.spring.LedgerManageUtils;
import utils.codec.Base58Utils;

import java.util.concurrent.Executor;

public class QueryParticipantsNodeRequestProcessor extends BaseRpcProcessor<QueryParticipantsNodeRequest> {


    public QueryParticipantsNodeRequestProcessor(RaftNodeServerService nodeServerService, Executor executor) {
        super(nodeServerService, executor);
    }

    @Override
    protected void processRequest(QueryParticipantsNodeRequest request, RpcResponseClosure done) {

        HashDigest hashDigest = Crypto.resolveAsHashDigest(Base58Utils.decode(request.getLedgerHash()));

        LedgerRepository ledgerRepository = LedgerManageUtils.getLedgerRepository(hashDigest);
        LedgerAdminInfo adminInfo = ledgerRepository.getAdminInfo(ledgerRepository.retrieveLatestBlock());
        ParticipantNode[] participants = adminInfo.getParticipants();

//        RaftServerSettings serverSettings = getNodeServerService().getNodeServer().getServerSettings();
//        PubKey pubKey = serverSettings.getRaftNodeSettings().getPubKey();
//
//        ParticipantNode currentNode = null;
//        for (ParticipantNode participantNode : participants) {
//            if (participantNode.getPubKey().equals(pubKey)) {
//                currentNode = participantNode;
//                break;
//            }
//        }
//
//        if (currentNode == null) {
//            done.run(new Status(RaftError.EEXISTS, "not found participant node"));
//            return;
//        }

        done.setResponse(RpcResponse.success(BinaryProtocol.encode(participants)));
        done.run(Status.OK());

    }
}

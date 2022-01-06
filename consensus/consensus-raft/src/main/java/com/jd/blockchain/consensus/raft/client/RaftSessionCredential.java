package com.jd.blockchain.consensus.raft.client;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_RAFT_CLIENT_CREDENTIAL_INFO)
public interface RaftSessionCredential extends SessionCredential {

	@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
	byte[] getInfo();

}

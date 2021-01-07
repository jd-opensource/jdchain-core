package com.jd.blockchain.consensus.mq.client;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.CONSENSUS_MSGQUEUE_CLIENT_CREDENTIAL_INFO)
public interface MQCredentialInfo extends SessionCredential {

	@DataField(order = 0, primitiveType = PrimitiveType.BYTES)
	byte[] getInfo();

}

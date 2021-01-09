package com.jd.blockchain.consensus.bftsmart;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.utils.net.NetworkAddress;

@DataContract(code = DataCodes.CONSENSUS_BFTSMART_NODE_SETTINGS)
public interface BftsmartNodeSettings extends NodeSettings {

	/**
	 * 节点的ID；
	 * 
	 * @return
	 */
	@DataField(order = 2, primitiveType = PrimitiveType.INT32)
	int getId();

	/**
	 * 共识协议的网络地址；
	 * 
	 * @return
	 */
	@DataField(order = 3, primitiveType = PrimitiveType.BYTES)
	NetworkAddress getNetworkAddress();

}

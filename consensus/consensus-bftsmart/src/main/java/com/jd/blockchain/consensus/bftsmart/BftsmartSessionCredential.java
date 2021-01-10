package com.jd.blockchain.consensus.bftsmart;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.SessionCredential;
import com.jd.blockchain.consts.DataCodes;

/**
 * Bftsmart 共识客户端的会话凭证信息；
 * <p>
 * 
 * @author huanghaiquan
 *
 */
@DataContract(code = DataCodes.CONSENSUS_BFTSMART_CLI_SESSION_CREDENTIAL)
public interface BftsmartSessionCredential extends SessionCredential {
	/**
	 * 客户端目前的 Id 起始编号；
	 * <p>
	 * 
	 * 如果从来没有，则返回 -1；
	 * 
	 * @return
	 */
	@DataField(order = 1, primitiveType = PrimitiveType.INT32)
	int getClientId();

	/**
	 * 客户端目前的 Id 长度范围；
	 * <p>
	 * 
	 * 如果客户端未有 Id 分配，则返回 0；
	 * 
	 * @return
	 */
	@DataField(order = 2, primitiveType = PrimitiveType.INT32)
	int getClientIdRange();

	/**
	 * 创建凭证的时间；
	 * 
	 * @return
	 */
	@DataField(order = 3, primitiveType = PrimitiveType.INT64)
	long getCreatedTime();
}

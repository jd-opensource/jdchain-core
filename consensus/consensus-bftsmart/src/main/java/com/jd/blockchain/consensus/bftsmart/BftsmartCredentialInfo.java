package com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.CredentialInfo;
import com.jd.blockchain.consts.DataCodes;

/**
 * Bftsmart 共识客户端 Id 分配请求信息；
 * <p>
 * 
 * 包含与客户端认证相关的参数信息；
 * 
 * @author huanghaiquan
 *
 */
@DataContract(code = DataCodes.CLIENT_CREDENTIAL_AUTH_REQUEST)
public interface BftsmartCredentialInfo extends CredentialInfo{
	/**
	 * 客户端目前的 Id 起始编号；
	 * 
	 * @return
	 */
	@DataField(order = 1, primitiveType = PrimitiveType.INT32)
	int getClientId();
	
	/**
	 * 客户端目前的 Id 长度范围；
	 * @return
	 */
	@DataField(order = 2, primitiveType = PrimitiveType.INT32)
	int getClientIdRange();
}

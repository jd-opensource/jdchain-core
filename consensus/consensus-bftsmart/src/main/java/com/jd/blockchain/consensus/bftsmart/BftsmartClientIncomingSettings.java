package com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.ClientCredential;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.bftsmart.service.BftsmartClientAuthencationService;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.PubKey;

/**
 * Bftsmart 客户端接入配置信息；
 * <p>
 * 
 * {@link BftsmartClientIncomingSettings}
 * 是客户端认证服务（{@link BftsmartClientAuthencationService}） 针对客户端认证信息
 * {@link ClientCredential} 进行验证生成的回复；
 * 
 * <p>
 * 参考方法
 * {@link BftsmartClientAuthencationService#authencateIncoming(ClientCredential)}；
 * 
 * @author huanghaiquan
 *
 */
@DataContract(code = DataCodes.CONSENSUS_BFTSMART_CLI_INCOMING_SETTINGS)
public interface BftsmartClientIncomingSettings extends ClientIncomingSettings {

	@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
	byte[] getTopology();

	@DataField(order = 2, primitiveType = PrimitiveType.BYTES)
	byte[] getTomConfig();

	@DataField(order = 3, primitiveType = PrimitiveType.BYTES)
	PubKey getPubKey();
}

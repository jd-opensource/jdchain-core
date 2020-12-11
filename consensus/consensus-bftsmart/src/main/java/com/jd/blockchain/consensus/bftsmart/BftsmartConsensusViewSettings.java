package com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.utils.Property;

@DataContract(code = DataCodes.CONSENSUS_BFTSMART_VIEW_SETTINGS)
public interface BftsmartConsensusViewSettings extends ConsensusViewSettings {

	@DataField(order = 1, primitiveType = PrimitiveType.BYTES, list=true)
	Property[] getSystemConfigs();

	@DataField(order = 2, primitiveType = PrimitiveType.INT32)
    int getViewId();

}

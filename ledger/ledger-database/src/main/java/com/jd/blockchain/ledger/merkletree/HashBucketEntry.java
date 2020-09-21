package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.consts.DataCodes;

@DataContract(code = DataCodes.MERKLE_HASH_SORTED_TREE_KEY_HASH_BUCKET)
public interface HashBucketEntry {

	/**
	 * 键的集合；
	 * 
	 * @return
	 */
	@DataField(order = 1, refContract = true, list = true)
	KeyIndex[] getKeySet();

}

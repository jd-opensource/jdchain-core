package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;

public interface ParticipantCollection extends MerkleProvable {

	HashDigest getRootHash();

	MerkleProof getProof(Bytes key);

	long getParticipantCount();

	boolean contains(Bytes address);

	/**
	 * 返回指定地址的参与方凭证；
	 * 
	 * <br>
	 * 如果不存在，则返回 null；
	 * 
	 * @param address
	 * @return
	 */
	ParticipantNode getParticipant(Bytes address);

	/**
	 * 废弃此方法，替代方法为 {@link #getAllParticipants()};
	 * <p>
	 * 
	 * 此方法在参与方数量很多的情况下会引发内存问题；
	 * <p>
	 * 
	 * @return
	 */
	@Deprecated
	ParticipantNode[] getParticipants();

	/**
	 * 返回所有的参与方；
	 * 
	 * @return
	 */
	SkippingIterator<ParticipantNode> getAllParticipants();

}
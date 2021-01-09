package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.ParticipantNode;

import utils.Bytes;
import utils.SkippingIterator;

/**
 * 参与方差异视图；
 * 
 * @author huanghaiquan
 *
 */
public interface ParticipantDiffView {

	Bytes getAddress();

	/**
	 * 参与方的版本差异；
	 * 
	 * @return 增加的版本列表；
	 */
	SkippingIterator<ParticipantNode> getVersionDiff();

}

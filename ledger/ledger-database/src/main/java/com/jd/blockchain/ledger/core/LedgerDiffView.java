package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerTransaction;

import utils.SkippingIterator;

/**
 * 账本的差异视图；
 * 
 * @author huanghaiquan
 *
 */
public interface LedgerDiffView extends DiffView {

	/**
	 * 账本哈希；
	 * 
	 * @return
	 */
	HashDigest getLedgerHash();

	/**
	 * 账本管理数据的差异;
	 * 
	 * @return
	 */
	AdminDataDiffView getAdminDataDiff();

	/**
	 * 交易的差异，即新增的交易列表；
	 * 
	 * @return
	 */
	SkippingIterator<LedgerTransaction> getTransactionDiff();

	/**
	 * 用户账户的差异；
	 * 
	 * @return
	 */
	SkippingIterator<UserAccountDiffView> getUserDiff();

	/**
	 * 数据账户的差异；
	 * 
	 * @return
	 */
	SkippingIterator<DataAccountDiffView> getDataDiff();

	/**
	 * 事件账户的差异；
	 * 
	 * @return
	 */
	SkippingIterator<EventAccountDiffView> getEventDiff();

	/**
	 * 事件账户的差异；
	 * 
	 * @return
	 */
	SkippingIterator<ContractAccountDiffView> getContractDiff();

}

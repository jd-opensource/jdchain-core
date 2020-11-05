package com.jd.blockchain.ledger.core;

/**
 * {@link LedgerDataset} 表示账本在某一个区块上的数据集合；
 * 
 * @author huanghaiquan
 *
 */
public interface LedgerDataQuery{
	
	LedgerAdminDataQuery getAdminDataset();

	UserAccountCollection getUserAccountSet();

	DataAccountCollection getDataAccountSet();

	ContractAccountCollection getContractAccountset();

}

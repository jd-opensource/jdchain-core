package com.jd.blockchain.ledger.core;

import java.io.Closeable;

import com.jd.blockchain.ledger.LedgerBlock;

public interface LedgerRepository extends Closeable, LedgerQuery {

	/**
	 * 创建新区块的编辑器；
	 * 
	 * @return
	 */
	LedgerEditor createNextBlock();

	/**
	 * 获取新区块的编辑器；
	 * <p>
	 * 
	 * 如果未创建新的区块，或者新区块已经提交或取消，则返回 null;
	 * 
	 * @return
	 */
	LedgerEditor getNextBlockEditor();

	/**
	 * 在最新区块下返回安全管理器；
	 * 
	 * @return
	 */
	LedgerSecurityManager getSecurityManager();

	/**
	 * 返回指定的新近区块和先前区块之间的账本差异视图；
	 * 
	 * @param recentBlock   新近区块；高度必须大于等于先前区块；
	 * @param previousBlock 先前区块；高度必须小于等于新近区块；
	 * @return
	 */
	LedgerDiffView getDiffView(LedgerBlock recentBlock, LedgerBlock previousBlock);

	/**
	 * 返回最新区块和指定的先前区块之间的账本差异视图；
	 * @param previousBlock 先前区块；高度必须小于等于最新区块；
	 * @return
	 */
	default LedgerDiffView getDiffView(LedgerBlock previousBlock) {
		return getDiffView(getLatestBlock(), previousBlock);
	}

	@Override
	void close();
}

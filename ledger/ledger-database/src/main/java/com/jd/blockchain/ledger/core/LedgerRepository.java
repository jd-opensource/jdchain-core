package com.jd.blockchain.ledger.core;

import java.io.Closeable;

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
	 * @return
	 */
	LedgerSecurityManager getSecurityManager();
	

	@Override
	void close();
}

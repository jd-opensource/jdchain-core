package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.utils.MathUtils;

/**
 * Degree Of Tree；
 * @author huanghaiquan
 *
 */
public enum TreeDegree {

	/**
	 * 度：2；最大深度：60；总节点数：2^60；
	 */
	D1(2, 60),

	/**
	 * 度：4；最大深度：30；总节点数： 4^30，即 2^60；
	 */
	D2(4, 30),

	/**
	 * 度：8；最大深度：20；总节点数： 8^20，即 2^60；
	 */
	D3(8, 20),

	/**
	 * 度：16；最大深度：15；总节点数： 16^15，即 2^60；
	 */
	D4(16, 15),

	/**
	 * 度：32；最大深度：12；总节点数： 32^12，即 2^60；
	 */
	D5(32, 12);

	public final int DEGREEE;
	
	public final int MAX_DEPTH;
	
	public final long MAX_COUNT;

	private TreeDegree(int degreee, int maxDepth) {
		this.DEGREEE = degreee;
		this.MAX_DEPTH = maxDepth;
		this.MAX_COUNT = MathUtils.power(degreee, maxDepth);
	}

}
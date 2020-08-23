package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.utils.MathUtils;

/**
 * Degree Of Tree；
 * @author huanghaiquan
 *
 */
public enum TreeDegree {

	D1(2, 60),

	D2(4, 30),

	D3(8, 20),

	D4(16, 15),

	D5(32, 12);

	public final int DEGREEE;
	public final int MAX_LEVEL;
	public final long MAX_COUNT;

	private TreeDegree(int degreee, int maxLevel) {
		this.DEGREEE = degreee;
		this.MAX_LEVEL = maxLevel;
		this.MAX_COUNT = MathUtils.power(degreee, maxLevel);
	}

}
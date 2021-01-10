package com.jd.blockchain.ledger.core;

import utils.Bytes;

/**
 * 序列号生成器；
 * 
 * @author huanghaiquan
 *
 */
public interface SNGenerator {

	long generate(Bytes key);

}

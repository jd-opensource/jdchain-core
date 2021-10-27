package com.jd.blockchain.ledger.core;

/**
 * 数据集类型枚举
 * 
 * @author zhangshuang
 *
 */
public enum SimpleDatasetType {

	/**
	 * 交易集,不同与其他数据集的处理
	 */
	TX,

	/**
	 * 用户账户数据集
	 */
	USERS,

	/**
	 * 数据账户数据集
	 */
	DATAS,

	/**
	 * 合约账户数据集
	 */
	CONTS,

	/**
	 * 事件账户数据集
	 */
	EVENTS,

	/**
	 * 用户属性信息数据集
	 */
	HDKVS,

	/**
	 * 用户数据信息数据集
	 */
	DTKVS,

	/**
	 * 参与方数据集
	 */
	PARTIS,

	/**
	 * 角色权限数据集
	 */
	ROLEPS,

	/**
	 * 用户角色数据集
	 */
	USERRS,

	/**
	 * 该类型暂不单独处理
	 */
	NONE
}

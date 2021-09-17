package com.jd.blockchain.ledger.core;

/**
 * 数据集类型枚举
 * 
 * @author zhangshuang
 *
 */
public enum SimpleDatasetType {

	/**
	 * 交易集
	 */
	TX,

	/**
	 * 用户集
	 */
	USERS,

	/**
	 * 用户集
	 */
	DATAS,

	/**
	 * 用户集
	 */
	CONTS,

	/**
	 * 用户集
	 */
	HDKVS,

	/**
	 * 用户集
	 */
	DTKVS,

	/**
	 * 参与方集
	 */
	PARTIS,

	/**
	 * 角色权限集
	 */
	ROLEPS,

	/**
	 * 角色权限集
	 */
	USERRS,

	/**
	 * 如果设置为NONE，则dataset单独处理，作为中间过渡使用
	 */
	NONE
}

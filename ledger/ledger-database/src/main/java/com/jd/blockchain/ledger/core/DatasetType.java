package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.EnumContract;
import com.jd.binaryproto.EnumField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;

/**
 * 数据集类型枚举
 * 
 * @author zhangshuang
 *
 */
@EnumContract(code = DataCodes.ENUM_TYPE_DATASET_TYPE)
public enum DatasetType {

	/**
	 * 交易集,不同与其他数据集的处理
	 */
	TX((byte) 0x01),

	/**
	 * 用户账户数据集
	 */
	USERS((byte) 0x02),

	/**
	 * 数据账户数据集
	 */
	DATAS((byte) 0x03),

	/**
	 * 合约账户数据集
	 */
	CONTS((byte) 0x04),

	/**
	 * 事件账户数据集
	 */
	EVENTS((byte) 0x05),

	/**
	 * 用户属性信息数据集
	 */
	HDKVS((byte) 0x06),

	/**
	 * 用户数据信息数据集
	 */
	DTKVS((byte) 0x07),

	/**
	 * 参与方数据集
	 */
	PARTIS((byte) 0x08),

	/**
	 * 角色权限数据集
	 */
	ROLEPS((byte) 0x09),

	/**
	 * 用户角色数据集
	 */
	USERRS((byte) 0x10),


	NONE((byte) 0x011);

	@EnumField(type = PrimitiveType.INT8)
	public final byte CODE;

	DatasetType(byte code) {
		this.CODE = code;
	}
}

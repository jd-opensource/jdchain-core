package com.jd.blockchain.ledger.merkletree;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.NumberEncoding;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;

/**
 * 默克尔数据索引；
 * 
 * <br>
 * 通过 {@link #getOffset()} 和 {@link #getStep()} 表示 1 个特定的位置区间;
 * 
 * @author huanghaiquan
 *
 */
@DataContract(code = DataCodes.MERKLE_SORTED_TREE_INDEX)
public interface MerkleIndex {

	/**
	 * 所有子项的起始ID； <br>
	 * 
	 * 即 {@link #getChildHashs()} 中第 0 个子项的 ID ；
	 * 
	 * @return
	 */
	@DataField(order = 0, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
	long getOffset();

	/**
	 * 子项的 ID 的递增步长；<br>
	 * 
	 * 即 {@link #getChildHashs()} 中任意子项的 ID 加上 {@link #getStep()} 为下一个子项的 ID；
	 * 
	 * @return
	 */
	@DataField(order = 1, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
	long getStep();

	/**
	 * 每个子项包含的数据项个数的列表；
	 * 
	 * @return
	 */
	@DataField(order = 2, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG, list = true)
	long[] getChildCounts();

	/**
	 * 子项的哈希的列表； <br>
	 * 
	 * 子项的个数总是固定的 {@value MerkleSortTree#DEGREE} ;
	 * 
	 * @return
	 */
	@DataField(order = 3, primitiveType = PrimitiveType.BYTES, list = true)
	HashDigest[] getChildHashs();
}
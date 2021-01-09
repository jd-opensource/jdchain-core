package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.ledger.core.MerkleProofException;

import utils.SkippingIterator;

/**
 * 默认的数据策略；
 * 
 * @author huanghaiquan
 *
 * @param <T>
 */
public class DefaultDataPolicy<T> implements DataPolicy<T> {

	/**
	 * 更新指定 id 的数据节点；
	 * 
	 * <p>
	 * 默认实现并不允许更新相同 id 的数据，并抛出 {@link MerkleProofException} 异常;
	 * 
	 * @param origValue 原数据；如果为 null，则表明是新增数据；
	 * @param newValue  新数据；
	 * @return 更新后的新节点的数据； 如果返回 null，则忽略此次操作；
	 */
	@Override
	public T updateData(long id, T origData, T newData) {
		if (origData != null) {
			throw new MerkleTreeKeyExistException("Unsupport updating datas with the same id!");
		}
		return newData;
	}

	/**
	 * 准备提交指定 id 的数据，保存至存储服务；<br>
	 * 
	 * 此方法在对指定数据节点进行序列化并进行哈希计算之前被调用；
	 * 
	 * @param id
	 * @param data
	 */
	@Override
	public T beforeCommitting(long id, T data) {
		return data;
	}

	@Override
	public long count(long id, T data) {
		return 1;
	}

	/**
	 * 已经取消指定 id 的数据；
	 * 
	 * @param id
	 * @param child
	 */
	@Override
	public void afterCanceled(long id, T data) {
	}

	@Override
	public SkippingIterator<MerkleValue<T>> iterator(long id, byte[] bytesData, long count,
			BytesConverter<T> converter) {
		return new MerkleDataIteratorWrapper<T>(id, bytesData, converter);
	}
}
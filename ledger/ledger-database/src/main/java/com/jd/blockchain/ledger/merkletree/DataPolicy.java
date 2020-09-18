package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.utils.SkippingIterator;

/**
 * 数据处理策略；
 * <p>
 * 
 * 定义默克尔树在对数据节点进行更新、提交、取消操作的策略；
 * <p>
 * 
 * 1. 当调用 {@link MerkleSortTree#set(long, Object)} 方法写入数据时，在被正式写入到叶子节点之前将触发
 * {@link DataPolicy#updateData(long, Object, Object)} 方法；
 * <p>
 * 
 * 2. 当调用 {@link MerkleSortTree#commit()} 方法提交新写入的数据时，如果
 * {@link MerkleSortTree#isUpdated()} 为true , 则每一条新写入的数据在被序列化之前都会作为参数先后调用方法
 * {@link #beforeCommitting(long, Object)} 和 {@link #count(long, Object)};
 * 
 * @author huanghaiquan
 *
 * @param <T>
 */
public interface DataPolicy<T> {

	/**
	 * 更新指定 id 的数据节点；
	 * 
	 * @param origValue 原数据；如果为 null，则表明是新增数据；
	 * @param newValue  新数据；
	 * @return 更新后的新节点的数据； 如果返回 null，则忽略此次操作，导致
	 *         {@link MerkleSortTree#set(long, Object)} 方法返回 false；
	 */
	T updateData(long id, T origData, T newData);

	/**
	 * 准备提交指定 id 的数据；
	 * <p>
	 * 
	 * 此方法在 {@link #count(long, Object)} 方法之前被调用；
	 * 
	 * @param id   数据的编码；
	 * @param data 新写入的数据对象，即 {@link #updateData(long, Object, Object)} 方法的返回值；
	 * @return 返回值是实际要提交的数据对象；
	 */
	T beforeCommitting(long id, T data);

	/**
	 * 在提交前对指定 id 的数据进行计数；返回值必须大于等于 0；<br>
	 * 
	 * 通常，一个 id 只表示一项数据的时候，返回计数 1 ；<br>
	 * 
	 * 如果扩展为表示其它的计数数值，需要对应地扩展 {@link #createIterator()}
	 * 方法返回的迭代器，实现对应数量的遍历，否则会影响整个默克尔树的遍历；
	 * <p>
	 * 
	 * 此方法在正式提交数据之前被调用；
	 * 
	 * @param id   数据的编码；
	 * @param data 数据对象；即 {@link #beforeCommitting(long, Object)} 方法的返回值；
	 * @return
	 */
	long count(long id, T data);

	/**
	 * 已经取消指定 id 的数据；
	 * 
	 * @param id
	 * @param child
	 */
	void afterCanceled(long id, T data);

	/**
	 * 创建指定的 id 的原始数据的迭代器；
	 * 
	 * @param id        数据的编码；
	 * @param bytesData 字节数组形式的数据；
	 * @param count     迭代器包含的数据记录的总数；
	 * @param converter 数据的转换器；
	 * @return
	 */
	SkippingIterator<MerkleValue<T>> iterator(long id, byte[] bytesData, long count, BytesConverter<T> converter);

}
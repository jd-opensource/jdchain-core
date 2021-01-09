package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;

import utils.Bytes;
import utils.SkippingIterator;
import utils.Transactional;

public class MerkleList<T> implements Transactional {

	private static DataPolicy<?> DATA_POLICY = new ListDataPolicy<>();

	@SuppressWarnings("unchecked")
	private static <T> DataPolicy<T> dataPolicy() {
		return (DataPolicy<T>) DATA_POLICY;
	}

	private MerkleSortTree<T> tree;

	public MerkleList(TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage, BytesConverter<T> converter) {
		tree = new MerkleSortTree<>(options, keyPrefix, kvStorage, converter, dataPolicy());
	}

	public MerkleList(HashDigest rootHash, TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter) {
		tree = new MerkleSortTree<T>(rootHash, options, keyPrefix, kvStorage, converter, dataPolicy());
	}

	public HashDigest getRootHash() {
		return tree.getRootHash();
	}

	public static MerkleList<byte[]> createByteArrayList(TreeOptions options, Bytes keyPrefix,
			ExPolicyKVStorage kvStorage) {
		return new MerkleList<byte[]>(options, keyPrefix, kvStorage, BytesToBytesConverter.INSTANCE);
	}

	public static MerkleList<byte[]> createByteArrayList(HashDigest rootHash, TreeOptions options, Bytes keyPrefix,
			ExPolicyKVStorage kvStorage) {
		return new MerkleList<byte[]>(rootHash, options, keyPrefix, kvStorage, BytesToBytesConverter.INSTANCE);
	}

	/**
	 * 总数；
	 * 
	 * @return
	 */
	public long size() {
		return tree.getCount();
	}

	/**
	 * 返回指定位置的数据；
	 * 
	 * @param index
	 * @return
	 */
	public T get(long index) {
		return tree.get(index);
	}

	/**
	 * 把指定的数据加到最后；
	 * 
	 * @param data
	 * @return
	 */
	public synchronized long add(T data) {
		long newId = tree.getMaxId() + 1;

		tree.set(newId, data);

		return newId;
	}

	public SkippingIterator<T> iterator() {
		return new ValuesIterator<T>(tree.iterator());
	}

	public void set(long index, T data) {
		tree.set(index, data);
	}

	@Override
	public boolean isUpdated() {
		return tree.isUpdated();
	}

	@Override
	public void commit() {
		tree.commit();
	}

	@Override
	public void cancel() {
		tree.cancel();
	}

	private static class ListDataPolicy<T> extends DefaultDataPolicy<T> {
		@Override
		public T updateData(long id, T origData, T newData) {
			// 允许更改数据；
			return newData;
		}
	}

	private static class ValuesIterator<T> implements SkippingIterator<T> {

		private SkippingIterator<MerkleValue<T>> iter;

		public ValuesIterator(SkippingIterator<MerkleValue<T>> iter) {
			this.iter = iter;
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public T next() {
			MerkleValue<T> mv = iter.next();
			return mv == null ? null : mv.getValue();
		}

		@Override
		public long getTotalCount() {
			return iter.getTotalCount();
		}

		@Override
		public long getCursor() {
			return iter.getCursor();
		}

		@Override
		public long skip(long count) {
			return iter.skip(count);
		}

	}

}

package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.proof.MerkleDataEntry;
import com.jd.blockchain.ledger.proof.MerkleHashTrie;
import com.jd.blockchain.ledger.proof.MerkleTree;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import com.jd.blockchain.storage.service.utils.BufferedKVStorage;
import com.jd.blockchain.storage.service.utils.VersioningKVData;
import com.jd.blockchain.utils.ArrayUtils;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.DataEntry;
import com.jd.blockchain.utils.DataIterator;
import com.jd.blockchain.utils.Dataset;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.Transactional;

/**
 * {@link MerkleHashDataset} 是对数据的键维护 {@link MerkleHashTrie} 索引的一种数据集结构； <br>
 *
 * 注：此实现不是线程安全的；
 *
 * @author huanghaiquan
 *
 */
public class MerkleHashDataset implements Transactional, MerkleProvable, Dataset<Bytes, byte[]> {

	/**
	 * 4 MB MaxSize of value;
	 */
	public static final int MAX_SIZE_OF_VALUE = 4 * 1024 * 1024;

//	public static final Bytes SN_PREFIX = Bytes.fromString("SN" + LedgerConsts.KEY_SEPERATOR);
	public static final Bytes DATA_PREFIX = Bytes.fromString("KV" + LedgerConsts.KEY_SEPERATOR);
	public static final Bytes MERKLE_TREE_PREFIX = Bytes.fromString("MKL" + LedgerConsts.KEY_SEPERATOR);

	@SuppressWarnings("unchecked")
	private static final DataEntry<Bytes, byte[]>[] EMPTY_ENTRIES = new DataEntry[0];
	
	
	private final HashFunction DEFAULT_HASH_FUNCTION;

	private final Bytes dataKeyPrefix;
	private final Bytes merkleKeyPrefix;

	private BufferedKVStorage bufferedStorage;

	private VersioningKVStorage valueStorage;

	private MerkleTree merkleTree;

	private boolean readonly;

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jd.blockchain.ledger.core.MerkleProvable#getRootHash()
	 */
	@Override
	public HashDigest getRootHash() {
		return merkleTree.getRootHash();
	}

	/**
	 * 创建一个新的 MerkleDataSet；
	 *
	 * @param setting           密码设置；
	 * @param exPolicyStorage   默克尔树的存储；
	 * @param versioningStorage 数据的存储；
	 */
	public MerkleHashDataset(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage exPolicyStorage,
			VersioningKVStorage versioningStorage) {
		this(setting, Bytes.fromString(keyPrefix), exPolicyStorage, versioningStorage);
	}

	/**
	 * 创建一个新的 MerkleDataSet；
	 *
	 * @param setting           密码设置；
	 * @param exPolicyStorage   默克尔树的存储；
	 * @param versioningStorage 数据的存储；
	 */
	public MerkleHashDataset(CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage exPolicyStorage,
			VersioningKVStorage versioningStorage) {
		// 缓冲对KV的写入；
		this.bufferedStorage = new BufferedKVStorage(exPolicyStorage, versioningStorage, false);

		// 把存储数据值、Merkle节点的 key 分别加入独立的前缀，避免针对 key 的注入攻击；
		dataKeyPrefix = keyPrefix.concat(DATA_PREFIX);
		this.valueStorage = bufferedStorage;
		
		this.DEFAULT_HASH_FUNCTION = Crypto.getHashFunction(setting.getHashAlgorithm());

		// MerkleTree 本身是可缓冲的；
		merkleKeyPrefix = keyPrefix.concat(MERKLE_TREE_PREFIX);
		ExPolicyKVStorage merkleTreeStorage = exPolicyStorage;
		this.merkleTree = new MerkleHashTrie(setting, merkleKeyPrefix, merkleTreeStorage);
	}

	/**
	 * 从指定的 Merkle 根构建的 MerkleDataSet；
	 *
	 * @param dataStorage
	 * @param defaultMerkleHashAlgorithm
	 * @param verifyMerkleHashOnLoad
	 * @param merkleTreeStorage
	 * @param snGenerator
	 */
	public MerkleHashDataset(HashDigest merkleRootHash, CryptoSetting setting, String keyPrefix,
			ExPolicyKVStorage exPolicyStorage, VersioningKVStorage versioningStorage, boolean readonly) {
		this(merkleRootHash, setting, Bytes.fromString(keyPrefix), exPolicyStorage, versioningStorage, readonly);
	}

	/**
	 * 从指定的 Merkle 根构建的 MerkleDataSet；
	 *
	 * @param dataStorage
	 * @param defaultMerkleHashAlgorithm
	 * @param verifyMerkleHashOnLoad
	 * @param merkleTreeStorage
	 * @param snGenerator
	 */
	public MerkleHashDataset(HashDigest merkleRootHash, CryptoSetting setting, Bytes keyPrefix,
			ExPolicyKVStorage exPolicyStorage, VersioningKVStorage versioningStorage, boolean readonly) {
		// 缓冲对KV的写入；
		this.bufferedStorage = new BufferedKVStorage(exPolicyStorage, versioningStorage, false);

		// 把存储数据值、Merkle节点的 key 分别加入独立的前缀，避免针对 key 的注入攻击；
		dataKeyPrefix = keyPrefix.concat(DATA_PREFIX);
		this.valueStorage = bufferedStorage;
		
		this.DEFAULT_HASH_FUNCTION = Crypto.getHashFunction(setting.getHashAlgorithm());

		// MerkleTree 本身是可缓冲的；
		merkleKeyPrefix = keyPrefix.concat(MERKLE_TREE_PREFIX);
		ExPolicyKVStorage merkleTreeStorage = exPolicyStorage;
		this.merkleTree = new MerkleHashTrie(merkleRootHash, setting, merkleKeyPrefix, merkleTreeStorage, readonly);

		this.readonly = readonly;
	}

	public boolean isReadonly() {
		return readonly;
	}

	void setReadonly() {
		this.readonly = true;
	}

	@Override
	public long getDataCount() {
		return merkleTree.getTotalKeys();
	}

	@Deprecated // 基于 MerkleHashTrie 的数据是无序固定排列的；
	public byte[][] getLatestValues(long fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}
		if (fromIndex < 0 || (fromIndex + count) > merkleTree.getTotalKeys()) {
			throw new IllegalArgumentException("The specified from-index and count are out of bound!");
		}
		byte[][] values = new byte[count][];
		SkippingIterator<MerkleDataEntry> iterator = merkleTree.iterator();
		iterator.skip(fromIndex);
		for (int i = 0; i < count && iterator.hasNext(); i++) {
			MerkleDataEntry dataNode = iterator.next();
			Bytes dataKey = encodeDataKey(dataNode.getKey());
			values[i] = valueStorage.get(dataKey, dataNode.getVersion());
		}
		return values;

	}

	@Deprecated // 基于 MerkleHashTrie 的数据是无序固定排列的；
	public DataEntry<Bytes, byte[]>[] getLatestDataEntries(long fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}
		if (fromIndex < 0 || (fromIndex + count) > merkleTree.getTotalKeys()) {
			throw new IllegalArgumentException("Index out of bound!");
		}
		if (count == 0) {
			return EMPTY_ENTRIES;
		}
		@SuppressWarnings("unchecked")
		DataEntry<Bytes, byte[]>[] values = new DataEntry[count];
		byte[] bytesValue;

		SkippingIterator<MerkleDataEntry> iterator = merkleTree.iterator();
		iterator.skip(fromIndex);
		for (int i = 0; i < count && iterator.hasNext(); i++) {
			MerkleDataEntry dataNode = iterator.next();
			Bytes dataKey = encodeDataKey(dataNode.getKey());
			bytesValue = valueStorage.get(dataKey, dataNode.getVersion());
			values[i] = new VersioningKVData<Bytes, byte[]>(dataNode.getKey(), dataNode.getVersion(), bytesValue);
		}
		return values;
	}

	@Deprecated // 基于 MerkleHashTrie 的数据是无序固定排列的；
	public DataEntry<Bytes, byte[]> getLatestDataEntry(long index) {
		if (index < 0 || index + 1 > merkleTree.getTotalKeys()) {
			throw new IllegalArgumentException("Index out of bound!");
		}
		byte[] bytesValue;
		SkippingIterator<MerkleDataEntry> iterator = merkleTree.iterator();
		iterator.skip(index);
		if (iterator.hasNext()) {
			MerkleDataEntry dataNode = iterator.next();
			Bytes dataKey = encodeDataKey(dataNode.getKey());
			bytesValue = valueStorage.get(dataKey, dataNode.getVersion());
			DataEntry<Bytes, byte[]> entry = new VersioningKVData<Bytes, byte[]>(dataNode.getKey(),
					dataNode.getVersion(), bytesValue);
			return entry;
		}
		return null;
	}

	/**
	 * get the data at the specific index;
	 *
	 * @param fromIndex
	 * @return
	 */
	@Deprecated // 基于 MerkleHashTrie 的数据是无序固定排列的；
	public byte[] getValuesAtIndex(int fromIndex) {
		SkippingIterator<MerkleDataEntry> iterator = merkleTree.iterator();
		iterator.skip(fromIndex);
		if (iterator.hasNext()) {
			MerkleDataEntry dataNode = iterator.next();
			Bytes dataKey = encodeDataKey(dataNode.getKey());
			return valueStorage.get(dataKey, dataNode.getVersion());
		}

		return null;
	}

	// 去掉不合理的接口定义； by huanghaiquan 2020-07-10;
//	// 获得两个默克尔数据集之间的数据节点差异
//	public byte[][] getDiffMerkleKeys(int fromIndex, int count, MerkleDataSet origMerkleDataSet) {
//		byte[][] values = new byte[count][];
//		LongSkippingIterator<MerkleData> diffIterator = merkleTree.getKeyDiffIterator(origMerkleDataSet.merkleTree);
//		diffIterator.skip(fromIndex);
//		for (int i = 0; i < count && diffIterator.hasNext(); i++) {
//			MerkleData merkleData = diffIterator.next();
//			Bytes dataKey = encodeDataKey(merkleData.getKey());
//			values[i] = valueStorage.get(dataKey, merkleData.getVersion());
//		}
//
//		return values;
//	}

//	public SkippingIterator<MerkleTrieData> getDiffMerkleKeys(MerkleHashDataset origMerkleDataSet) {
//		return merkleTree.getKeyDiffIterator(origMerkleDataSet.merkleTree);
//	}

	/**
	 * Create or update the value associated the specified key if the version
	 * checking is passed.<br>
	 *
	 * The value of the key will be updated only if it's latest version equals the
	 * specified version argument. <br>
	 * If the key doesn't exist, it will be created when the version arg was -1.
	 * <p>
	 * If updating is performed, the version of the key increase by 1. <br>
	 * If creating is performed, the version of the key initialize by 0. <br>
	 *
	 * @param key     The key of data;
	 * @param value   The value of data;
	 * @param version The expected latest version of the key.
	 * @return The new version of the key. <br>
	 *         If the key is new created success, then return 0; <br>
	 *         If the key is updated success, then return the new version;<br>
	 *         If this operation fail by version checking or other reason, then
	 *         return -1;
	 */
	@Override
	public long setValue(Bytes key, byte[] value, long version) {
		if (readonly) {
			throw new IllegalArgumentException("This merkle dataset is readonly!");
		}
		if (value.length > MAX_SIZE_OF_VALUE) {
			throw new IllegalArgumentException(
					"The size of value is great than the max size[" + MAX_SIZE_OF_VALUE + "]!");
		}
		Bytes dataKey = encodeDataKey(key);
		long latestVersion = valueStorage.getVersion(dataKey);
		if (version != latestVersion) {
			return -1;
		}

		// set into versioning kv storage before adding to merkle tree, in order to
		// check version confliction first;
		long newVersion;
		if (version < 0) {
			// creating ;
			newVersion = valueStorage.set(dataKey, value, -1);
			if (newVersion < 0) {
				return -1;
			}
		} else {
			// TODO: 未在当前实例的层面，实现对输入键-值的缓冲，而直接写入了存储，而 MerkleTree 在未调用 commit
			// 之前是缓冲的，这使得在存储层面的数据会不一致，而未来需要优化；
			newVersion = valueStorage.set(dataKey, value, version);
			if (newVersion < 0) {
				return -1;
			}

		}

		// TODO: 未在当前实例的层面，实现对输入键-值的缓冲，而直接写入了存储，而 MerkleTree 在未调用 commit
		// 之前是缓冲的，这使得在存储层面的数据会不一致，而未来需要优化；
		// update merkle tree;
		HashDigest valueHash = DEFAULT_HASH_FUNCTION.hash(value);
		merkleTree.setData(key, newVersion, valueHash);

		return newVersion;
	}

	private Bytes encodeDataKey(Bytes key) {
		return new Bytes(dataKeyPrefix, key);
	}

	private Bytes encodeDataKey(byte[] key) {
		return new Bytes(dataKeyPrefix, key);
	}

	/**
	 * 返回默克尔树中记录的指定键的版本，在由默克尔树表示的数据集的快照中，这是指定键的最新版本，<br>
	 * 但该版本有可能小于实际存储的最新版本（由于后续追加的新修改被之后生成的快照维护）；
	 *
	 * @param key
	 * @return 返回指定的键的版本；如果不存在，则返回 -1；
	 */
	private long getMerkleVersion(Bytes key) {
		MerkleDataEntry mdn = merkleTree.getData(key);
		if (mdn == null) {
			return -1;
		}
		return mdn.getVersion();
	}

	/**
	 * Return the specified version's value;<br>
	 *
	 * If the key with the specified version doesn't exist, then return null;<br>
	 * If the version is specified to -1, then return the latest version's value;
	 *
	 * @param key
	 * @param version
	 */
	@Override
	public byte[] getValue(Bytes key, long version) {
		long latestVersion = getMerkleVersion(key);
		if (latestVersion < 0 || version > latestVersion) {
			// key not exist, or the specified version is out of the latest version indexed
			// by the current merkletree;
			return null;
		}
		version = version < 0 ? latestVersion : version;
		Bytes dataKey = encodeDataKey(key);
		byte[] value = valueStorage.get(dataKey, version);
		if (value == null) {
			throw new MerkleProofException("Expected value does not exist!");
		}
		return value;
	}

	/**
	 * Return the latest version's value;
	 *
	 * @param key
	 * @return return null if not exist;
	 */
	@Override
	public byte[] getValue(Bytes key) {
		return getValue(key, -1);
	}

	/**
	 * Return the latest version entry associated the specified key; If the key
	 * doesn't exist, then return -1;
	 *
	 * @param key
	 * @return
	 */
	@Override
	public long getVersion(Bytes key) {
		return getMerkleVersion(key);
	}

	/**
	 *
	 * @param key
	 * @return Null if the key doesn't exist!
	 */
	@Override
	public DataEntry<Bytes, byte[]> getDataEntry(Bytes key) {
		return getDataEntry(key, -1);
	}

	@Override
	public DataEntry<Bytes, byte[]> getDataEntry(Bytes key, long version) {
		byte[] value = getValue(key, version);
		if (value == null) {
			return null;
		}
		return new VersioningKVData<Bytes, byte[]>(key, version, value);
	}

	@Override
	public DataIterator<Bytes, byte[]> iterator() {
		return new AscDataInterator(getDataCount());
	}

	@Override
	public DataIterator<Bytes, byte[]> iteratorDesc() {
		return new DescDataInterator(getDataCount());
	}

	public MerkleDataProof getDataProof(Bytes key, long version) {
		DataEntry<Bytes, byte[]> dataEntry = getDataEntry(key, version);
		if (dataEntry == null) {
			return null;
		}
		MerkleProof proof = getProof(key);
		return new MerkleDataEntryWrapper(dataEntry, proof);
	}

	public MerkleDataProof getDataProof(Bytes key) {
		DataEntry<Bytes, byte[]> dataEntry = getDataEntry(key);
		if (dataEntry == null) {
			return null;
		}
		MerkleProof proof = getProof(key);
		return new MerkleDataEntryWrapper(dataEntry, proof);
	}

	public MerkleProof getProof(String key) {
		return getProof(Bytes.fromString(key));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jd.blockchain.ledger.core.MerkleProvable#getProof(java.lang.String)
	 */
	@Override
	public MerkleProof getProof(Bytes key) {
		return merkleTree.getProof(key);
	}

	/**
	 * A wrapper for {@link DataEntry} and {@link MerkleProof};
	 *
	 * @author huanghaiquan
	 *
	 */
	private static class MerkleDataEntryWrapper implements MerkleDataProof {

		private DataEntry<Bytes, byte[]> data;
		private MerkleProof proof;

		public MerkleDataEntryWrapper(DataEntry<Bytes, byte[]> data, MerkleProof proof) {
			this.data = data;
			this.proof = proof;
		}

		@Override
		public DataEntry<Bytes, byte[]> getData() {
			return data;
		}

		@Override
		public MerkleProof getProof() {
			return proof;
		}

	}

	@Override
	public boolean isUpdated() {
		return bufferedStorage.isUpdated() || merkleTree.isUpdated();
	}

	@Override
	public void commit() {
		merkleTree.commit();
		bufferedStorage.commit();
	}

	@Override
	public void cancel() {
		merkleTree.cancel();
		bufferedStorage.cancel();
//		snGenerator = new MerkleSequenceSNGenerator(merkleTree);
	}

	// ----------------------------------------------------------

	private class AscDataInterator implements DataIterator<Bytes, byte[]> {

		private final long total;

		private long cursor = 0;

		public AscDataInterator(long total) {
			this.total = total;
		}

		@Override
		public void skip(long count) {
			cursor = nextCursor(count);
		}

		private long nextCursor(long skippingCount) {
			long c = cursor + skippingCount;
			return c > total ? total : c;
		}

		@Override
		public DataEntry<Bytes, byte[]> next() {
			if (hasNext()) {
				DataEntry<Bytes, byte[]> entry = getLatestDataEntry(cursor);
				cursor = nextCursor(1);
				return entry;
			}
			return null;
		}

		@Override
		public DataEntry<Bytes, byte[]>[] next(int count) {
			if (hasNext()) {
				long from = cursor;
				long nextCursor = nextCursor(count);
				long c = nextCursor - cursor;
				if (c > LedgerConsts.MAX_LIST_COUNT) {
					throw new IllegalArgumentException(
							"Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
				}
				DataEntry<Bytes, byte[]>[] entries = getLatestDataEntries(from, (int) c);
				cursor = nextCursor;
				return entries;
			}
			return EMPTY_ENTRIES;
		}

		@Override
		public boolean hasNext() {
			return cursor < total;
		}

	}

	private class DescDataInterator implements DataIterator<Bytes, byte[]> {

		private final long total;

		private long cursor;

		public DescDataInterator(long total) {
			this.total = total;
			this.cursor = total - 1;
		}

		@Override
		public void skip(long count) {
			cursor = nextCursor(count);
		}

		private long nextCursor(long skippingCount) {
			long c = cursor - skippingCount;
			return c < 0 ? -1 : c;
		}

		@Override
		public DataEntry<Bytes, byte[]> next() {
			if (hasNext()) {
				DataEntry<Bytes, byte[]> entry = getLatestDataEntry(cursor);
				cursor = nextCursor(1);
				return entry;
			}
			return null;
		}

		@Override
		public DataEntry<Bytes, byte[]>[] next(int count) {
			if (hasNext()) {
				long nextCursor = nextCursor(count);
				long from = nextCursor + 1;
				long c = cursor - nextCursor;
				if (c > LedgerConsts.MAX_LIST_COUNT) {
					throw new IllegalArgumentException(
							"Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
				}
				DataEntry<Bytes, byte[]>[] entries = getLatestDataEntries(from, (int) c);
				// reverse;
				ArrayUtils.reverse(entries);

				cursor = nextCursor;
				return entries;
			}
			return EMPTY_ENTRIES;
		}

		@Override
		public boolean hasNext() {
			return cursor < total;
		}

	}

}

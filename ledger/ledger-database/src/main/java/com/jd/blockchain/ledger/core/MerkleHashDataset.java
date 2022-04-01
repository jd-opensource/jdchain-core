package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.merkletree.KVEntry;
import com.jd.blockchain.ledger.merkletree.MerkleHashSortTree;
import com.jd.blockchain.ledger.merkletree.MerkleTree;
import com.jd.blockchain.ledger.merkletree.TreeOptions;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import com.jd.blockchain.storage.service.utils.BufferedKVStorage;
import com.jd.blockchain.storage.service.utils.VersioningKVData;

import utils.AbstractSkippingIterator;
import utils.Bytes;
import utils.DataEntry;
import utils.SkippingIterator;

/**
 * {@link MerkleHashDataset} 是基于默克尔树({@link MerkleHashSortTree})对数据的键维护一种数据集结构；
 * <br>
 *
 * 注：此实现不是线程安全的；
 *
 * @author huanghaiquan
 *
 */
public class MerkleHashDataset implements BaseDataset<Bytes, byte[]> {

	/**
	 * 4 MB MaxSize of value;
	 */
	public static final int MAX_SIZE_OF_VALUE = 8 * 1024 * 1024;

//	public static final Bytes SN_PREFIX = Bytes.fromString("SN" + LedgerConsts.KEY_SEPERATOR);
//	public static final Bytes DATA_PREFIX = Bytes.fromString("KV" + LedgerConsts.KEY_SEPERATOR);
	public static final Bytes MERKLE_TREE_PREFIX = Bytes.fromString("MKL" + LedgerConsts.KEY_SEPERATOR);

	@SuppressWarnings("unchecked")
	private static final DataEntry<Bytes, byte[]>[] EMPTY_ENTRIES = new DataEntry[0];

	private final HashFunction DEFAULT_HASH_FUNCTION;

	private final Bytes dataKeyPrefix;
	private final Bytes merkleKeyPrefix;

	private BufferedKVStorage valueStorage;

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
		this(null, setting, keyPrefix, exPolicyStorage, versioningStorage, false);
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
		// 把存储数据值、Merkle节点的 key 分别加入独立的前缀，避免针对 key 的注入攻击；
//		this.dataKeyPrefix = keyPrefix.concat(DATA_PREFIX);
		this.dataKeyPrefix = keyPrefix;
		// 缓冲对KV的写入；
		this.valueStorage = new BufferedKVStorage(Crypto.getHashFunction(setting.getHashAlgorithm()), exPolicyStorage, versioningStorage, false);

		this.DEFAULT_HASH_FUNCTION = Crypto.getHashFunction(setting.getHashAlgorithm());

		// MerkleTree 本身是可缓冲的；
		merkleKeyPrefix = keyPrefix.concat(MERKLE_TREE_PREFIX);
		TreeOptions options = TreeOptions.build().setDefaultHashAlgorithm(setting.getHashAlgorithm())
				.setVerifyHashOnLoad(setting.getAutoVerifyHash());
		if (merkleRootHash == null) {
			this.merkleTree = new MerkleHashSortTree(options, merkleKeyPrefix, exPolicyStorage);
		} else {
			this.merkleTree = new MerkleHashSortTree(merkleRootHash, options, merkleKeyPrefix, exPolicyStorage);
		}

		this.readonly = readonly;
	}

	@Override
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

	public byte[][] getValues(long fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}
		if (fromIndex < 0 || (fromIndex + count) > merkleTree.getTotalKeys()) {
			throw new IllegalArgumentException("The specified from-index and count are out of bound!");
		}
		byte[][] values = new byte[count][];
		SkippingIterator<KVEntry> iterator = merkleTree.iterator();
		iterator.skip(fromIndex);
		for (int i = 0; i < count && iterator.hasNext(); i++) {
			KVEntry dataNode = iterator.next();
			Bytes dataKey = encodeDataKey(dataNode.getKey());
			values[i] = valueStorage.get(dataKey, dataNode.getVersion());
		}
		return values;

	}

	@Override
	public DataEntry<Bytes, byte[]>[] getDataEntries(long fromIndex, int count) {
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

		SkippingIterator<KVEntry> iterator = merkleTree.iterator();
		iterator.skip(fromIndex);
		for (int i = 0; i < count && iterator.hasNext(); i++) {
			KVEntry dataNode = iterator.next();
			Bytes dataKey = encodeDataKey(dataNode.getKey());
			bytesValue = valueStorage.get(dataKey, dataNode.getVersion());
			values[i] = new VersioningKVData<Bytes, byte[]>(dataNode.getKey(), dataNode.getVersion(), bytesValue);
		}
		return values;
	}

	public DataEntry<Bytes, byte[]> getDataEntryAt(long index) {
		if (index < 0 || index + 1 > merkleTree.getTotalKeys()) {
			throw new IllegalArgumentException("Index out of bound!");
		}
		byte[] bytesValue;
		SkippingIterator<KVEntry> iterator = merkleTree.iterator();
		iterator.skip(index);
		if (iterator.hasNext()) {
			KVEntry dataNode = iterator.next();
			Bytes dataKey = encodeDataKey(dataNode.getKey());
			bytesValue = valueStorage.get(dataKey, dataNode.getVersion());
			DataEntry<Bytes, byte[]> entry = new VersioningKVData<Bytes, byte[]>(dataNode.getKey(),
					dataNode.getVersion(), bytesValue);
			return entry;
		}
		return null;
	}

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
		long newVersion = valueStorage.set(dataKey, value, version);
		if (newVersion < 0) {
			return -1;
		}

		// update merkle tree;
		HashDigest valueHash = DEFAULT_HASH_FUNCTION.hash(value);
		merkleTree.setData(key.toBytes(), newVersion, valueHash.toBytes());

		return newVersion;
	}

	@Override
	public long setValue(Bytes key, byte[] value) {
		if (readonly) {
			throw new IllegalArgumentException("This merkle dataset is readonly!");
		}
		if (value.length > MAX_SIZE_OF_VALUE) {
			throw new IllegalArgumentException(
					"The size of value is great than the max size[" + MAX_SIZE_OF_VALUE + "]!");
		}
		Bytes dataKey = encodeDataKey(key);
		long newVersion = valueStorage.set(dataKey, value, -1);
		if (newVersion < 0) {
			return -1;
		}

		// update merkle tree;
		HashDigest valueHash = DEFAULT_HASH_FUNCTION.hash(value);
		merkleTree.setData(key.toBytes(), newVersion, valueHash.toBytes());

		return newVersion;
	}

	private Bytes encodeDataKey(Bytes key) {
		return new Bytes(dataKeyPrefix, key);
	}

	@SuppressWarnings("unused")
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
		return merkleTree.getVersion(key.toBytes());
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
		return new VersioningKVData<Bytes, byte[]>(key, version, value);
	}

	@Override
	public SkippingIterator<DataEntry<Bytes, byte[]>> idIterator() {
		return new AscDataInterator(getDataCount());
	}

	@Override
	public SkippingIterator<DataEntry<Bytes, byte[]>> kvIterator() {
		return new AscDataInterator(getDataCount());
	}

	@Override
	public SkippingIterator<DataEntry<Bytes, byte[]>> idIteratorDesc() {
		return new DescDataInterator(getDataCount());
	}

	@Override
	public SkippingIterator<DataEntry<Bytes, byte[]>> kvIteratorDesc() {
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
		return merkleTree.getProof(key.toBytes());
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
		return valueStorage.isUpdated() || merkleTree.isUpdated();
	}

	@Override
	public void commit() {
		merkleTree.commit();
		valueStorage.commit();
	}

	@Override
	public void cancel() {
		merkleTree.cancel();
		valueStorage.cancel();
	}

	// ----------------------------------------------------------

	private class AscDataInterator extends AbstractSkippingIterator<DataEntry<Bytes, byte[]>> {

		private final long total;

		@Override
		public long getTotalCount() {
			return total;
		}

		public AscDataInterator(long total) {
			this.total = total;
		}

		@Override
		protected DataEntry<Bytes, byte[]> get(long cursor) {
			return getDataEntryAt(cursor);
		}

	}

	private class DescDataInterator extends AbstractSkippingIterator<DataEntry<Bytes, byte[]>> {

		private final long total;

		public DescDataInterator(long total) {
			this.total = total;
		}

		@Override
		public long getTotalCount() {
			return total;
		}

		@Override
		protected DataEntry<Bytes, byte[]> get(long cursor) {
			// 倒序的迭代器从后往前返回；
			return getDataEntryAt(total - cursor - 1);
		}
	}

}

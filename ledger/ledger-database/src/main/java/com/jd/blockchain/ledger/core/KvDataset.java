package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import com.jd.blockchain.storage.service.utils.BufferedKVStorage;
import com.jd.blockchain.storage.service.utils.VersioningKVData;
import utils.AbstractSkippingIterator;
import utils.Bytes;
import utils.DataEntry;
import utils.SkippingIterator;
import utils.io.BytesUtils;

import java.util.ArrayList;

/**
 * {@link KvDataset}
 * <br>
 *
 * 注：此实现不是线程安全的；
 *
 * @author
 *
 */
public class KvDataset implements BaseDataset<Bytes, byte[]> {

	/**
	 * 4 MB MaxSize of value;
	 */
	public static final int MAX_SIZE_OF_VALUE = 8 * 1024 * 1024;

//	public static final Bytes SN_PREFIX = Bytes.fromString("SN" + LedgerConsts.KEY_SEPERATOR);
//	private static final Bytes DATA_PREFIX = Bytes.fromString("KV" + LedgerConsts.KEY_SEPERATOR);
	private static final Bytes ACCOUNTSET_SEQUENCE_KEY_PREFIX = Bytes.fromString("SQ" + LedgerConsts.KEY_SEPERATOR);

	private static final DataEntry<Bytes, byte[]>[] EMPTY_ENTRIES = new DataEntry[0];

	@SuppressWarnings("unchecked")

	private final HashFunction DEFAULT_HASH_FUNCTION;

	private final Bytes dataKeyPrefix;

	private BufferedKVStorage valueStorage;

	private HashDigest rootHash;

	private HashDigest originHash;

	private long preBlockHeight;

	private boolean readonly;

	private DatasetType datasetType;

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jd.blockchain.ledger.core.MerkleProvable#getRootHash()
	 */
	@Override
	public HashDigest getRootHash() {
		return rootHash;
	}

	/**
	 * 创建一个新的 MerkleDataSet；
	 *
	 * @param setting           密码设置；
	 * @param exPolicyStorage   默克尔树的存储；
	 * @param versioningStorage 数据的存储；
	 */
	public KvDataset(DatasetType type, CryptoSetting setting, String keyPrefix, ExPolicyKVStorage exPolicyStorage,
					 VersioningKVStorage versioningStorage) {
		this(type, setting, Bytes.fromString(keyPrefix), exPolicyStorage, versioningStorage);
	}

	/**
	 * 创建一个新的 MerkleDataSet；
	 *
	 * @param setting           密码设置；
	 * @param exPolicyStorage   默克尔树的存储；
	 * @param versioningStorage 数据的存储；
	 */
	public KvDataset(DatasetType type, CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage exPolicyStorage,
					 VersioningKVStorage versioningStorage) {
		this(-1, null, type, setting, keyPrefix, exPolicyStorage, versioningStorage, false);
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
	public KvDataset(long preBlockHeight, HashDigest prevRootHash, DatasetType type, CryptoSetting setting, String keyPrefix,
					 ExPolicyKVStorage exPolicyStorage, VersioningKVStorage versioningStorage, boolean readonly) {
		this(preBlockHeight, prevRootHash, type, setting, Bytes.fromString(keyPrefix), exPolicyStorage, versioningStorage, readonly);
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
	public KvDataset(long preBlockHeight, HashDigest prevRootHash, DatasetType type, CryptoSetting setting, Bytes keyPrefix,
					 ExPolicyKVStorage exPolicyStorage, VersioningKVStorage versioningStorage, boolean readonly) {
		// 把存储数据值、Merkle节点的 key 分别加入独立的前缀，避免针对 key 的注入攻击；
//		this.dataKeyPrefix = keyPrefix.concat(DATA_PREFIX);
		this.dataKeyPrefix = keyPrefix;

		this.DEFAULT_HASH_FUNCTION = Crypto.getHashFunction(setting.getHashAlgorithm());

		// 缓冲对KV的写入；
		this.valueStorage = new BufferedKVStorage(this.DEFAULT_HASH_FUNCTION, exPolicyStorage, versioningStorage, false);

		this.preBlockHeight = preBlockHeight;

		this.rootHash = prevRootHash;

		this.datasetType = type;

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
		// prefix/T
		Bytes totalKey;

		if (datasetType == DatasetType.TX) {
			totalKey = dataKeyPrefix.concat(Bytes.fromString("T/").concat(Bytes.fromString(String.valueOf(preBlockHeight))));
		} else {
			totalKey = dataKeyPrefix.concat(Bytes.fromString("T"));
		}
		byte[] value = valueStorage.get(totalKey, -1);
		if (value == null) {
			return 0;
		}
		return BytesUtils.toLong(value);
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
		return setNoneTypeValue(dataKey, value, version);
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

//		long newVersion;
//		if (datasetType == DatasetType.TX) {
//			newVersion = setTxTypeValue(dataKey, value);
//		} else {
//			newVersion = setNoneTypeValue(dataKey, value, -1);
//		}

		return setNoneTypeValue(dataKey, value, -1);
	}

//	// 对于交易，只有一个版本，不再做多余的查询
//	private long setTxTypeValue(Bytes key, byte[] value) {
//		long newVersion = valueStorage.set(key, value, -1);
//		if (newVersion < 0) {
//			return -1;
//		}
//		return newVersion;
//	}


	private long setNoneTypeValue(Bytes key, byte[] value, long version) {
		// set into versioning kv storage before adding to merkle tree, in order to
		// check version confliction first;
		long newVersion;
		if (version < 0) {
			// creating ;
			newVersion = valueStorage.set(key, value, -1);
			if (newVersion < 0) {
				return -1;
			}
		} else {
			newVersion = valueStorage.set(key, value, version);
			if (newVersion < 0) {
				return -1;
			}

		}
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
		long latestVersion = getVersion(key);
		if (latestVersion < 0 || version > latestVersion) {
			// key not exist, or the specified version is out of the latest version indexed
			// by the current merkletree;
			return null;
		}
		version = version < 0 ? latestVersion : version;
		Bytes dataKey = encodeDataKey(key);
		byte[] value = valueStorage.get(dataKey, version);
		if (value == null) {
			throw new DataExistException("Expected value does not exist!");
		}
		return value;
	}

	// 根据索引编号找到存储数据的值,通过这个方法可以保证查询存储数据的顺序，维护账户或KV在集合中的顺序
	public byte[] getValueAt(long index) {
		Bytes key = ACCOUNTSET_SEQUENCE_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(index)));
		long latestVersion = getVersion(key);
		if (latestVersion != 0 ) {
			throw new DataExistException("Expected value version not exist!");
		}
		Bytes dataKey = encodeDataKey(key);
		byte[] value = valueStorage.get(dataKey, 0);
		if (value == null) {
			throw new DataExistException("Expected value does not exist!");
		}
		return value;
	}

	public DataEntry<Bytes, byte[]> getIdDataEntryAt(long index) {
		if (index < 0 || index + 1 > getDataCount()) {
			throw new IllegalArgumentException("Index out of bound!");
		}

		byte[] bytesValue = getValueAt(index);

		DataEntry<Bytes, byte[]> entry = new VersioningKVData<Bytes, byte[]>(Bytes.fromLong(index), 0, bytesValue);

		return entry;
	}

	public DataEntry<Bytes, byte[]> getKvDataEntryAt(long index) {
		if (index < 0 || index + 1 > getDataCount()) {
			throw new IllegalArgumentException("Index out of bound!");
		}

		byte[] bytesValue = getValueAt(index);

		Bytes key = new Bytes(bytesValue);
		long latestVersion = getVersion(key);
		if (latestVersion < 0 ) {
			throw new DataExistException("Expected value version not exist!");
		}
		Bytes dataKey = encodeDataKey(key);
		byte[] value = valueStorage.get(dataKey, latestVersion);
		if (value == null) {
			throw new DataExistException("Expected value does not exist!");
		}

		DataEntry<Bytes, byte[]> entry = new VersioningKVData<Bytes, byte[]>(key, latestVersion, value);

		return entry;
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
		// encdoe data key
		Bytes dataKey = encodeDataKey(key);
		return valueStorage.getVersion(dataKey);
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
		long latestVersion = getVersion(key);
		if (latestVersion < 0 || version > latestVersion) {
			// key not exist, or the specified version is out of the latest version indexed
			// by the current merkletree;
			return null;
		}
		version = version < 0 ? latestVersion : version;
		Bytes dataKey = encodeDataKey(key);
		byte[] value = valueStorage.get(dataKey, version);
		if (value == null) {
			throw new DataExistException("Expected value does not exist!");
		}
		return new VersioningKVData<Bytes, byte[]>(key, version, value);
	}

	@Override
	public DataEntry<Bytes, byte[]>[] getDataEntries(long fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}
		if (fromIndex < 0 || (fromIndex + count) > getDataCount()) {
			throw new IllegalArgumentException("Index out of bound!");
		}
		if (count == 0) {
			return EMPTY_ENTRIES;
		}
		@SuppressWarnings("unchecked")
		DataEntry<Bytes, byte[]>[] values = new DataEntry[count];
		byte[] bytesValue;

		for (int i = 0; i < count; i++) {
			byte[] key = getValueAt(fromIndex + i);
			DataEntry<Bytes, byte[]> entry = getDataEntry(new Bytes(key));
			values[i] = entry;
		}
		return values;
	}

	@Override
	public SkippingIterator<DataEntry<Bytes, byte[]>> idIterator() {
		return new AscIdDataInterator(getDataCount());
	}

	@Override
	public SkippingIterator<DataEntry<Bytes, byte[]>> kvIterator() {
		return new AscKvDataInterator(getDataCount());
	}

	@Override
	public SkippingIterator<DataEntry<Bytes, byte[]>> idIteratorDesc() {
		return new DescIdDataInterator(getDataCount());
	}

	@Override
	public SkippingIterator<DataEntry<Bytes, byte[]>> kvIteratorDesc() {
		return new DescKvDataInterator(getDataCount());
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
		return null;
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
		return valueStorage.isUpdated();
	}

	@Override
	public void commit() {
		// 保留roothash的中间执行状态，应对取消操作
		this.originHash = this.rootHash;
		this.rootHash = computeDataSetRootHash(originHash, valueStorage);
		valueStorage.commit();
	}


	@Override
	public void cancel() {
		valueStorage.cancel();
		rootHash = originHash;
	}


	private HashDigest computeDataSetRootHash(HashDigest originHash, BufferedKVStorage valueStorage) {

        ArrayList<HashDigest> merkleNodes = valueStorage.getCachedKvList();

		return  new KvTree(this.DEFAULT_HASH_FUNCTION, originHash, merkleNodes).root();
	}

	// 迭代身份集合
	private class AscIdDataInterator extends AbstractSkippingIterator<DataEntry<Bytes, byte[]>> {

		private final long total;

		@Override
		public long getTotalCount() {
			return total;
		}

		public AscIdDataInterator(long total) {
			this.total = total;
		}

		@Override
		protected DataEntry<Bytes, byte[]> get(long cursor) {
			return getIdDataEntryAt(cursor);
		}

	}

	// 迭代KV集合
	private class AscKvDataInterator extends AbstractSkippingIterator<DataEntry<Bytes, byte[]>> {

		private final long total;

		@Override
		public long getTotalCount() {
			return total;
		}

		public AscKvDataInterator(long total) {
			this.total = total;
		}

		@Override
		protected DataEntry<Bytes, byte[]> get(long cursor) {
			return getKvDataEntryAt(cursor);
		}

	}

	private class DescIdDataInterator extends AbstractSkippingIterator<DataEntry<Bytes, byte[]>> {

		private final long total;

		public DescIdDataInterator(long total) {
			this.total = total;
		}

		@Override
		public long getTotalCount() {
			return total;
		}

		@Override
		protected DataEntry<Bytes, byte[]> get(long cursor) {
			// 倒序的迭代器从后往前返回；
			return getIdDataEntryAt(total - cursor - 1);
		}
	}

	private class DescKvDataInterator extends AbstractSkippingIterator<DataEntry<Bytes, byte[]>> {

		private final long total;

		public DescKvDataInterator(long total) {
			this.total = total;
		}

		@Override
		public long getTotalCount() {
			return total;
		}

		@Override
		protected DataEntry<Bytes, byte[]> get(long cursor) {
			// 倒序的迭代器从后往前返回；
			return getKvDataEntryAt(total - cursor - 1);
		}
	}
}

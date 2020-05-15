package com.jd.blockchain.ledger.core;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.Transactional;
import com.jd.blockchain.utils.codec.Base58Utils;

public class TransactionSet implements Transactional, TransactionQuery {

	static {
		DataContractRegistry.register(LedgerTransaction.class);
	}

	private static final String TX_STATE_PREFIX = "STA" + LedgerConsts.KEY_SEPERATOR;

	private static final String TX_SET_PREFIX = "TXSET" + LedgerConsts.KEY_SEPERATOR;

	private final Bytes txStatePrefix;

	private final Bytes txSetPrefix;

	private MerkleDataSet txDataSet;

	private MerkleDataSet txStateSet;

    private HashDigest txSetRootHash;

	private HashFunction hashFunc;

	private ExPolicyKVStorage exPolicyKVStorage;

    private TransactionSetInfo transactionSetQuery = new TransactionSetInfo();

	@Override
	public LedgerTransaction[] getTxs(int fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}
		byte[][] results = getValuesByIndex(fromIndex, count);
		LedgerTransaction[] ledgerTransactions = new LedgerTransaction[results.length];

		for (int i = 0; i < results.length; i++) {
			ledgerTransactions[i] = deserializeTx(results[i]);
		}
		return ledgerTransactions;
	}

	@Override
	public LedgerTransaction[] getBlockTxs(int fromIndex, int count, TransactionQuery origTransactionSet) {
		//取创世区块包含的交易
		if (origTransactionSet == null)  {
			return getTxs(fromIndex, count);
		}

		//根据指定索引以及个数，取最新区块中的交易
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}
		byte[][] results = getValuesByDiff(fromIndex, count, origTransactionSet);
		LedgerTransaction[] ledgerTransactions = new LedgerTransaction[results.length];

		for (int i = 0; i < results.length; i++) {
			ledgerTransactions[i] = deserializeTx(results[i]);
		}
		return ledgerTransactions;

	}

	@Override
	public byte[][] getValuesByIndex(int fromIndex, int count) {
		byte[][] values = new byte[count][];
		for (int i = 0; i < count; i++) {
			values[i] = txDataSet.getValuesAtIndex(fromIndex);
			fromIndex++;
		}
		return values;
	}

	@Override
	public byte[][] getValuesByDiff(int fromIndex, int count, TransactionQuery origTransactionSet) {
		return txDataSet.getDiffMerkleKeys(fromIndex, count, ((TransactionSet)origTransactionSet).txDataSet);
	}

	@Override
	public HashDigest getRootHash() {
		return getTxSetRootHash();
	}

	public HashDigest getTxDataRootHash() {
		return getTxSetInfo(getTxSetRootHash()).getTxDataSetRootHash();
	}

	@Override
	public MerkleProof getProof(Bytes key) {
		return txDataSet.getProof(key);
	}

	@Override
	public long getTotalCount() {
		// 每写入一个交易，同时写入交易内容Hash与交易结果的索引，因此交易记录数为集合总记录数除以 2；
		return txDataSet.getDataCount();
	}

	/**
	 * Create a new TransactionSet which can be added transaction;
	 * 
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSet(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage merkleTreeStorage,
			VersioningKVStorage dataStorage) {
		this.txStatePrefix = Bytes.fromString(keyPrefix + TX_STATE_PREFIX);
		this.txSetPrefix = Bytes.fromString(keyPrefix + TX_SET_PREFIX);
		this.hashFunc = Crypto.getHashFunction(setting.getHashAlgorithm());
		this.exPolicyKVStorage = merkleTreeStorage;
		this.txDataSet = new MerkleDataSet(setting, keyPrefix, merkleTreeStorage, dataStorage);
		this.txStateSet = new MerkleDataSet(setting, keyPrefix, merkleTreeStorage, dataStorage);

	}

	/**
	 * Create TransactionSet which is readonly to the history transactions;
	 * 
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSet(HashDigest txRootHash, CryptoSetting setting, String keyPrefix,
			ExPolicyKVStorage merkleTreeStorage, VersioningKVStorage dataStorage, boolean readonly) {
		this.txStatePrefix = Bytes.fromString(keyPrefix + TX_STATE_PREFIX);
		this.txSetPrefix = Bytes.fromString(keyPrefix + TX_SET_PREFIX);
		this.exPolicyKVStorage = merkleTreeStorage;
		this.hashFunc = Crypto.getHashFunction(setting.getHashAlgorithm());
		this.txDataSet = new MerkleDataSet(getTxSetInfo(txRootHash).getTxDataSetRootHash(), setting, Bytes.fromString(keyPrefix), merkleTreeStorage, dataStorage,
				readonly);
		this.txStateSet = new MerkleDataSet(getTxSetInfo(txRootHash).getTxStateSetRootHash(), setting, Bytes.fromString(keyPrefix), merkleTreeStorage, dataStorage,
				readonly);

	}

	/**
	 * @param txRequest
	 * @param result
	 */
	public void add(LedgerTransaction tx) {
		// TODO: 优化对交易内存存储的优化，应对大数据量单交易，共享操作的“写集”与实际写入账户的KV版本；
		// 序列化交易内容；
		byte[] txBytes = serialize(tx);
		// 以交易内容的 hash 为 key；
		// String key = tx.getTransactionContent().getHash().toBase58();
		Bytes key = new Bytes(tx.getTransactionContent().getHash().toBytes());
		// 交易只有唯一的版本；
		long v = txDataSet.setValue(key, txBytes, -1);
		if (v < 0) {
			throw new LedgerException("Transaction is persisted repeatly! --[" + key + "]");
		}
		// 以交易内容的hash值为key，单独记录交易结果的索引，以便快速查询交易结果；
		Bytes resultKey = encodeTxStateKey(key);
		v = txStateSet.setValue(resultKey, new byte[] { tx.getExecutionState().CODE }, -1);
		if (v < 0) {
			throw new LedgerException("Transaction result is persisted repeatly! --[" + key + "]");
		}
	}

	private TransactionSetQuery getTxSetInfo(HashDigest txSetRootHash) {
		 return deserializeQuery(exPolicyKVStorage.get(encodeTxSetKey(txSetRootHash)));
	}

	public LedgerTransaction get(String base58Hash) {
		HashDigest hash = new HashDigest(Base58Utils.decode(base58Hash));
		return get(hash);
	}

	/**
	 * @param txContentHash Base58 编码的交易内容的哈希；
	 * @return
	 */
	@Override
	public LedgerTransaction get(HashDigest txContentHash) {
		// transaction has only one version;
		Bytes key = new Bytes(txContentHash.toBytes());
		// byte[] txBytes = txSet.getValue(txContentHash.toBase58(), 0);
		byte[] txBytes = txDataSet.getValue(key, 0);
		if (txBytes == null) {
			return null;
		}
		LedgerTransaction tx = deserializeTx(txBytes);
		return tx;
	}

	@Override
	public TransactionState getState(HashDigest txContentHash) {
		Bytes resultKey = encodeTxStateKey(txContentHash);
		// transaction has only one version;
		byte[] bytes = txStateSet.getValue(resultKey, 0);
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		return TransactionState.valueOf(bytes[0]);
	}

	private Bytes encodeTxStateKey(Bytes txContentHash) {
		return new Bytes(txStatePrefix, txContentHash);
	}

	private Bytes encodeTxSetKey(Bytes txSetRootHash) {
		return new Bytes(txSetPrefix, txSetRootHash);
	}

	private LedgerTransaction deserializeTx(byte[] txBytes) {
		return BinaryProtocol.decode(txBytes);
	}

	private TransactionSetQuery deserializeQuery(byte[] txBytes) {
		return BinaryProtocol.decode(txBytes);
	}

	//序列化交易内容
	private byte[] serialize(LedgerTransaction txRequest) {
		return BinaryProtocol.encode(txRequest, LedgerTransaction.class);
	}

	//序列化交易内容和状态的数据集根哈希
	private byte[] serialize(TransactionSetQuery transactionSetQuery) {
		return BinaryProtocol.encode(transactionSetQuery, TransactionSetQuery.class);
	}

	public boolean isReadonly() {
		return txDataSet.isReadonly() || txStateSet.isReadonly();
	}

	void setReadonly() {
		txDataSet.setReadonly();
		txStateSet.setReadonly();
	}

	private HashDigest getTxSetRootHash() {
		if (txStateSet.getRootHash() == null || txStateSet.getRootHash() == null)  {
			return null;
		}
		transactionSetQuery.setTxDataSetRootHash(txDataSet.getRootHash());
		transactionSetQuery.setTxStateSetRootHash(txStateSet.getRootHash());

		byte[] txSetRootHashBytes = serialize(transactionSetQuery);

		HashDigest rootHash = hashFunc.hash(txSetRootHashBytes);

		return rootHash;
	}

	private void setTxSetRootHash() {
		transactionSetQuery.setTxDataSetRootHash(txDataSet.getRootHash());
		transactionSetQuery.setTxStateSetRootHash(txStateSet.getRootHash());

		byte[] txSetRootHashBytes = serialize(transactionSetQuery);

		HashDigest rootHash = hashFunc.hash(txSetRootHashBytes);

		Bytes rootHashKey =  encodeTxSetKey(rootHash);

		exPolicyKVStorage.set(rootHashKey, txSetRootHashBytes, ExPolicyKVStorage.ExPolicy.NOT_EXISTING);

		txSetRootHash = rootHash;
	}

	@Override
	public boolean isUpdated() {
		return txDataSet.isUpdated() || txStateSet.isUpdated();
	}

	@Override
	public void commit() {
		txDataSet.commit();
		txStateSet.commit();

		//维护交易集根哈希的值
		setTxSetRootHash();
	}

	@Override
	public void cancel() {
		txDataSet.cancel();
		txStateSet.cancel();
	}

	@Override
	public MerkleProof getTxDataProof(Bytes key) {
		return getProof(key);
	}

	@Override
	public MerkleProof getTxStateProof(Bytes key) {
		return txStateSet.getProof(key);
	}

	public static class TransactionSetInfo implements TransactionSetQuery {
		private HashDigest txDataSetRootHash;
		private HashDigest txStateSetRootHash;


		public TransactionSetInfo() {

		}

		public TransactionSetInfo(TransactionSetQuery transactionSetInfo) {
			this.txDataSetRootHash = transactionSetInfo.getTxDataSetRootHash();
			this.txStateSetRootHash = transactionSetInfo.getTxStateSetRootHash();
		}

		@Override
		public HashDigest getTxDataSetRootHash() {
			return txDataSetRootHash;
		}

		@Override
		public HashDigest getTxStateSetRootHash() {
			return txStateSetRootHash;
		}

		public void setTxDataSetRootHash(HashDigest txDataSetRootHash) {
			this.txDataSetRootHash = txDataSetRootHash;
		}

		public void setTxStateSetRootHash(HashDigest txStateSetRootHash) {
			this.txStateSetRootHash = txStateSetRootHash;
		}
	}
}

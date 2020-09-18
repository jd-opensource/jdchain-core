package com.jd.blockchain.ledger.core;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.merkletree.BytesConverter;
import com.jd.blockchain.ledger.merkletree.MerkleList;
import com.jd.blockchain.ledger.merkletree.TreeOptions;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.DataEntry;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.Transactional;
import com.jd.blockchain.utils.codec.Base58Utils;

public class TransactionSet implements Transactional, TransactionQuery {

	static {
		DataContractRegistry.register(TransactionRequest.class);
		DataContractRegistry.register(LedgerTransaction.class);
	}

	private static final TransactionRequestConverter CONVERTER = new TransactionRequestConverter();

	private static final String TX_STATE_PREFIX = "STA" + LedgerConsts.KEY_SEPERATOR;

	private static final String TX_REQUEST_PREFIX = "REQ" + LedgerConsts.KEY_SEPERATOR;

	/**
	 * 交易请求列表的根哈希保存在交易状态集的 key ；
	 */
	private static final Bytes TX_REQUEST_ROOT_HASH = Bytes.fromString("TX-REQUEST-BLOCKS");

	/**
	 * 交易状态集合；用于记录交易执行结果；
	 */
	private MerkleHashDataset txStateSet;

	/**
	 * 交易请求列表；用于记录交易请求的顺序；
	 */
	private MerkleList<TransactionRequest> txRequests;

	/**
	 * 交易请求列表的根哈希在交易状态集合中的版本；(key 为 {@link #TX_REQUEST_ROOT_HASH} );
	 * 
	 */
	private volatile long txRequestBlockID;

	/**
	 * Create a new TransactionSet which can be added transaction;
	 * 
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSet(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage merkleTreeStorage,
			VersioningKVStorage dataStorage) {

		Bytes txStatePrefix = Bytes.fromString(keyPrefix + TX_STATE_PREFIX);
		this.txStateSet = new MerkleHashDataset(setting, txStatePrefix, merkleTreeStorage, dataStorage);

		this.txRequestBlockID = -1;

		Bytes txRequestPrefix = Bytes.fromString(keyPrefix + TX_REQUEST_PREFIX);
		TreeOptions options = TreeOptions.build().setDefaultHashAlgorithm(setting.getHashAlgorithm())
				.setVerifyHashOnLoad(setting.getAutoVerifyHash());
		this.txRequests = new MerkleList<>(options, txRequestPrefix, merkleTreeStorage, CONVERTER);

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

		Bytes txStatePrefix = Bytes.fromString(keyPrefix + TX_STATE_PREFIX);
		this.txStateSet = new MerkleHashDataset(txRootHash, setting, txStatePrefix, merkleTreeStorage, dataStorage,
				readonly);

		DataEntry<Bytes, byte[]> txReqRootHashData = this.txStateSet.getDataEntry(TX_REQUEST_ROOT_HASH);
		this.txRequestBlockID = txReqRootHashData.getVersion();
		HashDigest txRequestRootHash = new HashDigest(txReqRootHashData.getValue());

		Bytes txRequestPrefix = Bytes.fromString(keyPrefix + TX_REQUEST_PREFIX);
		TreeOptions options = TreeOptions.build().setDefaultHashAlgorithm(setting.getHashAlgorithm())
				.setVerifyHashOnLoad(setting.getAutoVerifyHash());
		this.txRequests = new MerkleList<>(txRequestRootHash, options, txRequestPrefix, merkleTreeStorage, CONVERTER);
	}

	@Override
	public LedgerTransaction[] getTransactions(int fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}

		SkippingIterator<TransactionRequest> txReqIterator = txRequests.iterator();

		txReqIterator.skip(fromIndex);

		int txCount = (int) Math.min(txReqIterator.getCount(), (long) count);
		LedgerTransaction[] ledgerTransactions = new LedgerTransaction[txCount];

		TransactionRequest txReq;
		for (int i = 0; i < txCount; i++) {
			txReq = txReqIterator.next();
			ledgerTransactions[i] = getTransaction(txReq.getTransactionHash());
		}
		return ledgerTransactions;
	}

//	@Override
//	public byte[][] getValuesByIndex(int fromIndex, int count) {
//		byte[][] values = new byte[count][];
//		for (int i = 0; i < count; i++) {
//			values[i] = txStateSet.getValuesAt(fromIndex * 2);
//			fromIndex++;
//		}
//		return values;
//	}

	@Override
	public HashDigest getRootHash() {
		return txStateSet.getRootHash();
	}

	@Override
	public MerkleProof getProof(Bytes key) {
		return txStateSet.getProof(key);
	}

	@Override
	public long getTotalCount() {
		return txRequests.size();
	}

	/**
	 * @param txRequest
	 * @param result
	 */
	public void addTransaction(TransactionRequest txRequest, LedgerTransaction txResult) {
		// TODO: 优化对交易内存存储的优化，应对大数据量单交易，共享操作的“写集”与实际写入账户的KV版本；
		txRequests.add(txRequest);

		// 序列化交易内容；
		byte[] txResultBytes = serialize(txResult);
		// 以交易内容的 hash 为 key；
		Bytes key = txResult.getTransactionHash();
		// 交易只有唯一的版本；
		long v = txStateSet.setValue(key, txResultBytes, -1);
		if (v < 0) {
			throw new LedgerException("Transaction is persisted repeatly! --[" + key + "]");
		}
		// 以交易内容的hash值为key，单独记录交易结果的索引，以便快速查询交易结果；
//		Bytes resultKey = encodeTxStateKey(key);
//		v = txStateSet.setValue(resultKey, new byte[] { txResult.getExecutionState().CODE }, -1);
//		if (v < 0) {
//			throw new LedgerException("Transaction result is persisted repeatly! --[" + key + "]");
//		}
	}

	public LedgerTransaction getTransaction(String base58Hash) {
		HashDigest hash = new HashDigest(Base58Utils.decode(base58Hash));
		return getTransaction(hash);
	}

	/**
	 * @param txContentHash Base58 编码的交易内容的哈希；
	 * @return
	 */
	@Override
	public LedgerTransaction getTransaction(HashDigest txContentHash) {
		// transaction has only one version;
		byte[] txBytes = txStateSet.getValue(txContentHash, 0);
		if (txBytes == null) {
			return null;
		}
		LedgerTransaction tx = deserialize(txBytes);
		return tx;
	}

	@Override
	public TransactionState getState(HashDigest txContentHash) {
		// TODO: 待优化性能；
		LedgerTransaction tx = getTransaction(txContentHash);
		if (tx == null) {
			throw new LedgerException("Transaction[" + txContentHash.toBase58() + "] doesn't exist!");
		}
		return tx.getExecutionState();
	}

	private LedgerTransaction deserialize(byte[] txBytes) {
		return BinaryProtocol.decode(txBytes);
	}

	private byte[] serialize(LedgerTransaction txRequest) {
		return BinaryProtocol.encode(txRequest, LedgerTransaction.class);
	}

	public boolean isReadonly() {
		return txStateSet.isReadonly();
	}

	void setReadonly() {
		txStateSet.setReadonly();
	}

	@Override
	public boolean isUpdated() {
		return txStateSet.isUpdated();
	}

	@Override
	public synchronized void commit() {
		txRequests.commit();
		HashDigest txReqRootHash = txRequests.getRootHash();
		long v = txStateSet.setValue(TX_REQUEST_ROOT_HASH, txReqRootHash.toBytes(), txRequestBlockID);
		if (v < 0) {
			throw new LedgerException("Fail to save the root hash of transaction request!");
		}
		txStateSet.commit();
		txRequestBlockID = v;
	}

	@Override
	public void cancel() {
		txRequests.cancel();
		txStateSet.cancel();
	}

	private static class TransactionRequestConverter implements BytesConverter<TransactionRequest> {

		@Override
		public byte[] toBytes(TransactionRequest value) {
			return BinaryProtocol.encode(value, TransactionRequest.class);
		}

		@Override
		public TransactionRequest fromBytes(byte[] bytes) {
			return BinaryProtocol.decode(bytes, TransactionRequest.class);
		}

	}

}

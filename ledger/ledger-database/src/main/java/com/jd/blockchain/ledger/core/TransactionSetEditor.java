package com.jd.blockchain.ledger.core;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResult;
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

public class TransactionSetEditor implements Transactional, TransactionSet {

	static {
		DataContractRegistry.register(TransactionRequest.class);
		DataContractRegistry.register(LedgerTransaction.class);
	}

//	private static final TransactionRequestConverter REQUEST_CONVERTER = new TransactionRequestConverter();
	private static final HashDigestBytesConverter HASH_DIGEST_CONVERTER = new HashDigestBytesConverter();

	private static final String TX_STATE_PREFIX = "STA" + LedgerConsts.KEY_SEPERATOR;

	private static final String TX_SEQUENCE_PREFIX = "SEQ" + LedgerConsts.KEY_SEPERATOR;

	/**
	 * 交易请求列表的根哈希保存在交易状态集的 key ；
	 */
	private static final Bytes TX_REQUEST_ROOT_HASH = Bytes.fromString("TX-REQUEST-BLOCKS");

	private static final Bytes TX_REQUEST_KEY_PREFIX = Bytes.fromString("REQ" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes TX_RESULT_KEY_PREFIX = Bytes.fromString("RST" + LedgerConsts.KEY_SEPERATOR);

	/**
	 * 交易状态集合；用于记录交易执行结果；
	 */
	private MerkleDataset<Bytes, byte[]> txStateSet;

	/**
	 * 交易请求列表；用于记录交易请求的顺序；
	 */
	private MerkleList<HashDigest> txSequence;

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
	public TransactionSetEditor(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage merkleTreeStorage,
			VersioningKVStorage dataStorage) {

		Bytes txStatePrefix = Bytes.fromString(keyPrefix + TX_STATE_PREFIX);
		this.txStateSet = new MerkleHashDataset(setting, txStatePrefix, merkleTreeStorage, dataStorage);

		this.txRequestBlockID = -1;

		Bytes txSequencePrefix = Bytes.fromString(keyPrefix + TX_SEQUENCE_PREFIX);
		TreeOptions options = TreeOptions.build().setDefaultHashAlgorithm(setting.getHashAlgorithm())
				.setVerifyHashOnLoad(setting.getAutoVerifyHash());
		this.txSequence = new MerkleList<>(options, txSequencePrefix, merkleTreeStorage, HASH_DIGEST_CONVERTER);

	}

	/**
	 * Create TransactionSet which is readonly to the history transactions;
	 * 
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSetEditor(HashDigest txRootHash, CryptoSetting setting, String keyPrefix,
			ExPolicyKVStorage merkleTreeStorage, VersioningKVStorage dataStorage, boolean readonly) {

		Bytes txStatePrefix = Bytes.fromString(keyPrefix + TX_STATE_PREFIX);
		this.txStateSet = new MerkleHashDataset(txRootHash, setting, txStatePrefix, merkleTreeStorage, dataStorage,
				readonly);

		DataEntry<Bytes, byte[]> txReqRootHashData = this.txStateSet.getDataEntry(TX_REQUEST_ROOT_HASH);
		this.txRequestBlockID = txReqRootHashData.getVersion();
		HashDigest txRequestRootHash = Crypto.resolveAsHashDigest(txReqRootHashData.getValue());

		Bytes txRequestPrefix = Bytes.fromString(keyPrefix + TX_SEQUENCE_PREFIX);
		TreeOptions options = TreeOptions.build().setDefaultHashAlgorithm(setting.getHashAlgorithm())
				.setVerifyHashOnLoad(setting.getAutoVerifyHash());
		this.txSequence = new MerkleList<>(txRequestRootHash, options, txRequestPrefix, merkleTreeStorage,
				HASH_DIGEST_CONVERTER);
	}

	@Override
	public LedgerTransaction[] getTransactions(int fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}

		SkippingIterator<HashDigest> txReqIterator = txSequence.iterator();

		txReqIterator.skip(fromIndex);

		int txCount = (int) Math.min(txReqIterator.getCount(), (long) count);
		LedgerTransaction[] ledgerTransactions = new LedgerTransaction[txCount];

		for (int i = 0; i < txCount; i++) {
			HashDigest txHash = txReqIterator.next();
			ledgerTransactions[i] = getTransaction(txHash);
		}
		return ledgerTransactions;
	}
	
	@Override
	public TransactionResult[] getTransactionResults(int fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}

		SkippingIterator<HashDigest> txReqIterator = txSequence.iterator();

		txReqIterator.skip(fromIndex);

		int txCount = (int) Math.min(txReqIterator.getCount(), (long) count);
		TransactionResult[] ledgerTransactions = new TransactionResult[txCount];

		for (int i = 0; i < txCount; i++) {
			HashDigest txHash = txReqIterator.next();
			ledgerTransactions[i] = loadResult(txHash);
		}
		return ledgerTransactions;
	}

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
		return txSequence.size();
	}

	/**
	 * @param txRequest
	 * @param result
	 */
	public void addTransaction(TransactionRequest txRequest, TransactionResult txResult) {
		// TODO: 优化对交易内存存储的优化，应对大数据量单交易，共享操作的“写集”与实际写入账户的KV版本；
		txSequence.add(txRequest.getTransactionHash());
		saveRequest(txRequest);
		saveResult(txResult);
	}

	public LedgerTransaction getTransaction(String base58Hash) {
		HashDigest hash = Crypto.resolveAsHashDigest(Base58Utils.decode(base58Hash));
		return getTransaction(hash);
	}

	/**
	 * @param txContentHash Base58 编码的交易内容的哈希；
	 * @return
	 */
	@Override
	public LedgerTransaction getTransaction(HashDigest txContentHash) {
		TransactionRequest txRequest = loadRequest(txContentHash);
		TransactionResult txResult = loadResult(txContentHash);

		return new LedgerTransactionData(txRequest, txResult);
	}
	
	@Override
	public TransactionRequest getTransactionRequest(HashDigest txContentHash) {
		return loadRequest(txContentHash);
	}
	
	@Override
	public TransactionResult getTransactionResult(HashDigest txContentHash) {
		return loadResult(txContentHash);
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

	private TransactionResult loadResult(HashDigest txContentHash) {
		// transaction has only one version;
		Bytes key = encodeResultKey(txContentHash);
		byte[] txBytes = txStateSet.getValue(key, 0);
		if (txBytes == null) {
			return null;
		}
		return BinaryProtocol.decode(txBytes, TransactionResult.class);
	}

	private void saveResult(TransactionResult txResult) {
		// 序列化交易内容；
		byte[] txResultBytes = BinaryProtocol.encode(txResult, TransactionResult.class);
		// 以交易内容的 hash 为 key；
		Bytes key = encodeResultKey(txResult.getTransactionHash());
		// 交易只有唯一的版本；
		long v = txStateSet.setValue(key, txResultBytes, -1);
		if (v < 0) {
			throw new LedgerException("Transaction Result is persisted repeatly! --[" + key + "]");
		}
	}
	
	private TransactionRequest loadRequest(HashDigest txContentHash) {
		// transaction has only one version;
		Bytes key = encodeRequestKey(txContentHash);
		byte[] txBytes = txStateSet.getValue(key, 0);
		if (txBytes == null) {
			return null;
		}
		return BinaryProtocol.decode(txBytes, TransactionRequest.class);
	}
	
	private void saveRequest(TransactionRequest txResult) {
		// 序列化交易内容；
		byte[] txResultBytes = BinaryProtocol.encode(txResult, TransactionRequest.class);
		// 以交易内容的 hash 为 key；
		Bytes key = encodeRequestKey(txResult.getTransactionHash());
		// 交易只有唯一的版本；
		long v = txStateSet.setValue(key, txResultBytes, -1);
		if (v < 0) {
			throw new LedgerException("Transaction Request is persisted repeatly! --[" + key + "]");
		}
	}

	private Bytes encodeResultKey(HashDigest txContentHash) {
		return TX_RESULT_KEY_PREFIX.concat(txContentHash);
	}
	
	private Bytes encodeRequestKey(HashDigest txContentHash) {
		return TX_REQUEST_KEY_PREFIX.concat(txContentHash);
	}

	public boolean isReadonly() {
		return txStateSet.isReadonly();
	}

	@Override
	public boolean isUpdated() {
		return txStateSet.isUpdated();
	}

	@Override
	public synchronized void commit() {
		txSequence.commit();
		HashDigest txReqRootHash = txSequence.getRootHash();
		long v = txStateSet.setValue(TX_REQUEST_ROOT_HASH, txReqRootHash.toBytes(), txRequestBlockID);
		if (v < 0) {
			throw new LedgerException("Fail to save the root hash of transaction request!");
		}
		txStateSet.commit();
		txRequestBlockID = v;
	}

	@Override
	public void cancel() {
		txSequence.cancel();
		txStateSet.cancel();
	}

//	private static class TransactionRequestConverter implements BytesConverter<TransactionRequest> {
//
//		@Override
//		public byte[] toBytes(TransactionRequest value) {
//			return BinaryProtocol.encode(value, TransactionRequest.class);
//		}
//
//		@Override
//		public TransactionRequest fromBytes(byte[] bytes) {
//			return BinaryProtocol.decode(bytes, TransactionRequest.class);
//		}
//
//	}

	private static class HashDigestBytesConverter implements BytesConverter<HashDigest> {

		@Override
		public byte[] toBytes(HashDigest value) {
			return value.toBytes();
		}

		@Override
		public HashDigest fromBytes(byte[] bytes) {
			return Crypto.resolveAsHashDigest(bytes);
		}

	}

}

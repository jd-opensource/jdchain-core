package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.IllegalTransactionException;
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
import utils.Bytes;
import utils.DataEntry;
import utils.SkippingIterator;
import utils.Transactional;
import utils.codec.Base58Utils;

public class TransactionSetEditorSimple implements Transactional, TransactionSet {

	static {
		DataContractRegistry.register(TransactionRequest.class);
		DataContractRegistry.register(LedgerTransaction.class);
	}

	private static final String TX_PREFIX = "TX" + LedgerConsts.KEY_SEPERATOR;

	private static final Bytes TX_REQUEST_KEY_PREFIX = Bytes.fromString("REQ" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes TX_RESULT_KEY_PREFIX = Bytes.fromString("RST" + LedgerConsts.KEY_SEPERATOR);

	/**
	 * 交易状态集合；用于记录交易执行结果；
	 */
	private SimpleDataset<Bytes, byte[]> txDataSet;

	/**
	 * Create a new TransactionSet which can be added transaction;
	 *
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSetEditorSimple(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage merkleTreeStorage,
                                      VersioningKVStorage dataStorage) {

		Bytes txPrefix = Bytes.fromString(keyPrefix + TX_PREFIX);
		this.txDataSet = new SimpleDatasetImpl(setting, txPrefix, merkleTreeStorage, dataStorage);
	}

	/**
	 * Create TransactionSet which is readonly to the history transactions;
	 *
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSetEditorSimple(HashDigest txRootHash, CryptoSetting setting, String keyPrefix,
                                      ExPolicyKVStorage merkleTreeStorage, VersioningKVStorage dataStorage, boolean readonly) {

		Bytes txPrefix = Bytes.fromString(keyPrefix + TX_PREFIX);
		this.txDataSet = new SimpleDatasetImpl(txRootHash, setting, txPrefix, merkleTreeStorage, dataStorage,
				readonly);
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
		return txDataSet.getRootHash();
	}

	@Override
	public MerkleProof getProof(Bytes key) {
		return txDataSet.getProof(key);
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
		if(null == txRequest) {
			return null;
		}
		TransactionResult txResult = loadResult(txContentHash);
		if(null == txResult) {
			return null;
		}
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
			return null;
		}
		return tx.getResult().getExecutionState();
	}

	private TransactionResult loadResult(HashDigest txContentHash) {
		// transaction has only one version;
		Bytes key = encodeResultKey(txContentHash);
		byte[] txBytes = txDataSet.getValue(key, 0);
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
		long v = txDataSet.setValue(key, txResultBytes, -1);
		if (v < 0) {
			throw new IllegalTransactionException("Repeated transaction request! --[" + key + "]");
		}
	}
	
	private TransactionRequest loadRequest(HashDigest txContentHash) {
		// transaction has only one version;
		Bytes key = encodeRequestKey(txContentHash);
		byte[] txBytes = txDataSet.getValue(key, 0);
		if (txBytes == null) {
			return null;
		}
		return BinaryProtocol.decode(txBytes, TransactionRequest.class);
	}
	
	private void saveRequest(TransactionRequest txRequest) {
		// 序列化交易内容；
		byte[] txResultBytes = BinaryProtocol.encode(txRequest, TransactionRequest.class);
		// 以交易内容的 hash 为 key；
		Bytes key = encodeRequestKey(txRequest.getTransactionHash());
		// 交易只有唯一的版本；
		long v = txDataSet.setValue(key, txResultBytes, -1);
		if (v < 0) {
			throw new IllegalTransactionException("Repeated transaction request! --[" + key + "]");
		}
	}

	private Bytes encodeResultKey(HashDigest txContentHash) {
		return TX_RESULT_KEY_PREFIX.concat(txContentHash);
	}
	
	private Bytes encodeRequestKey(HashDigest txContentHash) {
		return TX_REQUEST_KEY_PREFIX.concat(txContentHash);
	}

	public boolean isReadonly() {
		return txDataSet.isReadonly();
	}

	@Override
	public boolean isUpdated() {
		return txDataSet.isUpdated();
	}

	@Override
	public synchronized void commit() {
		txDataSet.commit();
	}

	@Override
	public void cancel() {
		txDataSet.cancel();
	}

}

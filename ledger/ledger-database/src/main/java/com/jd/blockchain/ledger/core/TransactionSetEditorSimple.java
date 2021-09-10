package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.IllegalTransactionException;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResult;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import utils.Bytes;
import utils.Transactional;
import utils.codec.Base58Utils;

public class TransactionSetEditorSimple implements Transactional, TransactionSet {

	static {
		DataContractRegistry.register(TransactionRequest.class);
		DataContractRegistry.register(LedgerTransaction.class);
	}

	private static final Bytes TX_REQUEST_KEY_PREFIX = Bytes.fromString("REQ" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes TX_RESULT_KEY_PREFIX = Bytes.fromString("RST" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes TX_SEQUENCE_KEY_PREFIX = Bytes.fromString("SEQ" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes TX_TOTOAL_KEY_PREFIX = Bytes.fromString("TOTAL" + LedgerConsts.KEY_SEPERATOR);

	/**
	 * 交易状态集合；用于记录交易执行结果；
	 */
	private SimpleDataset<Bytes, byte[]> txDataSet;

	private long preBlockHeight;

	private long txIndex = -1;

	/**
	 * Create a new TransactionSet which can be added transaction;
	 *
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSetEditorSimple(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage merkleTreeStorage,
                                      VersioningKVStorage dataStorage) {
		this.preBlockHeight = -1;
		this.txDataSet = new SimpleDatasetImpl(setting, keyPrefix, merkleTreeStorage, dataStorage);
	}

	/**
	 * Create TransactionSet which is readonly to the history transactions;
	 *
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSetEditorSimple(long preBlockHeight, HashDigest txsetHash, CryptoSetting setting, String keyPrefix,
                                      ExPolicyKVStorage merkleTreeStorage, VersioningKVStorage dataStorage, boolean readonly) {
		this.preBlockHeight = preBlockHeight;
		this.txDataSet = new SimpleDatasetImpl(preBlockHeight, txsetHash, setting, keyPrefix, merkleTreeStorage, dataStorage,
				readonly);
	}

	@Override
	public LedgerTransaction[] getTransactions(int fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}

		int txCount = (int) Math.min(getTotalCount(), (long) count);
		LedgerTransaction[] ledgerTransactions = new LedgerTransaction[txCount];

		for (int i = fromIndex; i < fromIndex + txCount; i++) {
			HashDigest txHash = loadReqHash(i);
			ledgerTransactions[i] = getTransaction(txHash);
		}
		return ledgerTransactions;
	}
	
	@Override
	public TransactionResult[] getTransactionResults(int fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}

		int txCount = (int) Math.min(getTotalCount(), (long) count);
		TransactionResult[] ledgerTransactions = new TransactionResult[txCount];

		for (int i = fromIndex; i < fromIndex + txCount; i++) {
			HashDigest txHash = loadReqHash(i);
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
		// key =  keyprefix + TOTAL/preBlockHeight
		// 暂时未包括缓存中的交易数
		Bytes key = encodeTotalNumKey(preBlockHeight);

		return txDataSet.getDataCount();
	}

	/**
	 * @param txRequest
	 * @param result
	 */
	public void addTransaction(TransactionRequest txRequest, TransactionResult txResult) {
		// TODO: 优化对交易内存存储的优化，应对大数据量单交易，共享操作的“写集”与实际写入账户的KV版本；
		saveRequest(txRequest);
		saveResult(txResult);
		saveSequence(txRequest);
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

	// 以区块高度为维度记录交易索引号
	private void saveSequenceByHeight(TransactionRequest txRequest) {
		txIndex++;
		Bytes blockHeightPrefix = Bytes.fromString(String.valueOf(preBlockHeight + 1) + LedgerConsts.KEY_SEPERATOR);

		// key =  keyprefix + SEQ/blockheight/txindex
		Bytes key = encodeSeqKeyByHeight(blockHeightPrefix, txIndex);
		// 交易序号只有唯一的版本；
		long v = txDataSet.setValue(key, txRequest.getTransactionHash().toBytes(), -1);
		if (v < 0) {
			throw new IllegalTransactionException("Repeated transaction request sequence! --[" + key + "]");
		}

	}

	// 以账本为维度记录交易索引号
	private void saveSequence(TransactionRequest txRequest) {
		txIndex++;
		// key = keyprefix + SEQ/txindex
		Bytes key = encodeSeqKey(getTotalCount() + txIndex);
		// 交易序号只有唯一的版本；
		long v = txDataSet.setValue(key, txRequest.getTransactionHash().toBytes(), -1);
		if (v < 0) {
			throw new IllegalTransactionException("Repeated transaction request sequence! --[" + key + "]");
		}
	}

	// 以区块为维度，加载指定交易索引的交易请求hash
	private HashDigest loadReqHash(long blockHeight, long txIndex) {
		// transaction sequence has only one version;
		Bytes blockHeightPrefix = Bytes.fromString(String.valueOf(blockHeight) + LedgerConsts.KEY_SEPERATOR);

		Bytes key = encodeSeqKeyByHeight(blockHeightPrefix, txIndex);
		byte[] txHashBytes = txDataSet.getValue(key, 0);
		if (txHashBytes == null) {
			return null;
		}
		return Crypto.resolveAsHashDigest(txHashBytes);
	}

	// 以账本为维度，加载指定交易索引的交易请求hash
	private HashDigest loadReqHash(long txIndex) {
		Bytes key = encodeSeqKey(txIndex);
		byte[] txHashBytes = txDataSet.getValue(key, 0);
		if (txHashBytes == null) {
			return null;
		}
		return Crypto.resolveAsHashDigest(txHashBytes);
	}

	// 需要仔细考虑，比如中间状态
//	private long loadTxTotalNum() {
//		// transaction sequence has only one version;
//		Bytes blockHeightPrefix = Bytes.fromString(String.valueOf(preBlockHeight + 1) + LedgerConsts.KEY_SEPERATOR);
//
//		Bytes key = encodeSeqKey(txIndex);
//
//		byte[] txHashBytes = txDataSet.getValue(key, preBlockHeight);
//		if (txHashBytes == null) {
//			return 0;
//		}
//		return 0;
//	}

	private Bytes encodeResultKey(HashDigest txContentHash) {
		return TX_RESULT_KEY_PREFIX.concat(txContentHash);
	}
	
	private Bytes encodeRequestKey(HashDigest txContentHash) {
		return TX_REQUEST_KEY_PREFIX.concat(txContentHash);
	}

	private Bytes encodeSeqKeyByHeight(Bytes blockHeightPrefix, long txIndex) {
		return TX_SEQUENCE_KEY_PREFIX.concat(blockHeightPrefix).concat(Bytes.fromString(String.valueOf(txIndex)));
	}

	private Bytes encodeSeqKey(long txIndex) {
		return TX_SEQUENCE_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(txIndex)));
	}

	private Bytes encodeTotalNumKey(long preBlockHeight) {
		return TX_TOTOAL_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(preBlockHeight)));
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

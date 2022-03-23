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
import utils.io.BytesUtils;

import java.util.ArrayList;

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
	private BaseDataset<Bytes, byte[]> txDataSet;

	private volatile long txIndex = 0;

	private volatile long origin_txIndex = 0;

	private HashDigest rootHash;

	private HashDigest origin_rootHash;

	private CryptoSetting setting;

	private long preBlockHeight;

	private ArrayList<HashDigest> transactions = new ArrayList<>();

	/**
	 * Create a new TransactionSet which can be added transaction;
	 *
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSetEditorSimple(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage merkleTreeStorage,
                                      VersioningKVStorage dataStorage) {
		this.rootHash = null;
		this.origin_rootHash = this.rootHash;
		this.setting = setting;
		this.preBlockHeight = -1;
		this.txDataSet = new KvDataset(DatasetType.TX, setting, keyPrefix, merkleTreeStorage, dataStorage);
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
		this.rootHash = txsetHash;
		this.origin_rootHash = this.rootHash;
		this.setting = setting;
		this.preBlockHeight = preBlockHeight;
		this.txDataSet = new KvDataset(preBlockHeight, txsetHash, DatasetType.TX, setting, keyPrefix, merkleTreeStorage, dataStorage,
				readonly);
	}

	@Override
	public LedgerTransaction[] getTransactions(int fromIndex, int count) {
		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}

		int txCount = (int) Math.min(getTotalCount(), (long) count);
		LedgerTransaction[] ledgerTransactions = new LedgerTransaction[txCount];

		for (int i = 0; i < txCount; i++) {
			HashDigest txHash = loadReqHash(fromIndex + i);
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

		for (int i = 0; i < txCount; i++) {
			HashDigest txHash = loadReqHash(fromIndex + i);
			ledgerTransactions[i] = loadResult(txHash);
		}
		return ledgerTransactions;
	}

	@Override
	public HashDigest getRootHash() {
		return rootHash;
	}

	@Override
	public MerkleProof getProof(Bytes key) {
		return txDataSet.getProof(key);
	}

	@Override
	public long getTotalCount() {
		// key =  keyprefix + TOTAL/preBlockHeight
		return txDataSet.getDataCount() + origin_txIndex;
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
		transactions.add(txRequest.getTransactionHash());
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
		long v = txDataSet.setValue(key, txResultBytes);
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
		long v = txDataSet.setValue(key, txResultBytes);
		if (v < 0) {
			throw new IllegalTransactionException("Repeated transaction request! --[" + key + "]");
		}
	}

	// 以区块高度为维度记录交易索引号
//	private void saveSequenceByHeight(TransactionRequest txRequest) {
//		txIndex++;
//		Bytes blockHeightPrefix = Bytes.fromString(String.valueOf(preBlockHeight + 1) + LedgerConsts.KEY_SEPERATOR);
//
//		// key =  keyprefix + SEQ/blockheight/txindex
//		Bytes key = encodeSeqKeyByHeight(blockHeightPrefix, txIndex);
//		// 交易序号只有唯一的版本；
//		long v = txDataSet.setValue(key, txRequest.getTransactionHash().toBytes(), -1);
//		if (v < 0) {
//			throw new IllegalTransactionException("Repeated transaction request sequence! --[" + key + "]");
//		}
//
//	}

	// 以账本为维度记录交易索引号
	private void saveSequence(TransactionRequest txRequest) {
		// key = keyprefix + SEQ/txindex
		Bytes key = encodeSeqKey(txDataSet.getDataCount() + txIndex);
		// 交易序号只有唯一的版本；
		long v = txDataSet.setValue(key, txRequest.getTransactionHash().toBytes());
		if (v < 0) {
			throw new IllegalTransactionException("Repeated transaction request sequence! --[" + key + "]");
		}
		txIndex++;
	}

	// 按照区块高度记录交易总数
	private void saveTotalByHeight(long blockHeight) {

		// key = keyprefix + TOTAL/blockheight
		Bytes key = encodeTotalNumKey(blockHeight);

		// 交易序号只有唯一的版本；
		long v = txDataSet.setValue(key, BytesUtils.toBytes(getTotalCount()));
		if (v < 0) {
			throw new IllegalTransactionException("Repeated transaction request sequence! --[" + key + "]");
		}
	}

	// 以区块为维度，加载指定交易索引的交易请求hash
//	private HashDigest loadReqHash(long blockHeight, long txIndex) {
//		// transaction sequence has only one version;
//		Bytes blockHeightPrefix = Bytes.fromString(String.valueOf(blockHeight) + LedgerConsts.KEY_SEPERATOR);
//
//		Bytes key = encodeSeqKeyByHeight(blockHeightPrefix, txIndex);
//		byte[] txHashBytes = txDataSet.getValue(key, 0);
//		if (txHashBytes == null) {
//			return null;
//		}
//		return Crypto.resolveAsHashDigest(txHashBytes);
//	}

	// 以账本为维度，加载指定交易索引的交易请求hash
	private HashDigest loadReqHash(long seq) {
		Bytes key = encodeSeqKey(seq);
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

//	private Bytes encodeSeqKeyByHeight(Bytes blockHeightPrefix, long txIndex) {
//		return TX_SEQUENCE_KEY_PREFIX.concat(blockHeightPrefix).concat(Bytes.fromString(String.valueOf(txIndex)));
//	}

	private Bytes encodeSeqKey(long seq) {
		return TX_SEQUENCE_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(seq)));
	}

	private Bytes encodeTotalNumKey(long blockHeight) {
		return TX_TOTOAL_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(blockHeight)));
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
		origin_txIndex = txIndex;
		origin_rootHash = rootHash;
		// 后续可以做默克尔证明；
		rootHash = computeTxsRootHash(rootHash, transactions);

		saveTotalByHeight(preBlockHeight + 1);
		txDataSet.commit();
	}


	@Override
	public void cancel() {
		txDataSet.cancel();

		// 恢复到上次的交易索引
		txIndex = origin_txIndex;
		rootHash = origin_rootHash;
	}

	private HashDigest computeTxsRootHash(HashDigest rootHash, ArrayList<HashDigest> transactions) {

		return new MerkleTreeSimple(Crypto.getHashFunction(setting.getHashAlgorithm()), rootHash, transactions).root();

	}

}

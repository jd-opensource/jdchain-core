package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.IllegalTransactionException;
import com.jd.blockchain.ledger.LedgerDataStructure;
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
import utils.io.BytesUtils;

import java.util.ArrayList;

public class TransactionSetEditor implements Transactional, TransactionSet {

	static {
		DataContractRegistry.register(TransactionRequest.class);
		DataContractRegistry.register(LedgerTransaction.class);
	}

//	private static final TransactionRequestConverter REQUEST_CONVERTER = new TransactionRequestConverter();
	private static final HashDigestBytesConverter HASH_DIGEST_CONVERTER = new HashDigestBytesConverter();

	private static final String TX_STATE_PREFIX = "ST" + LedgerConsts.KEY_SEPERATOR;

	/**
	 * 交易请求列表的根哈希保存在交易状态集的 key ；
	 */
	private static final Bytes TX_REQUEST_ROOT_HASH = Bytes.fromString("TX-REQUEST-BLOCKS");

	private static final Bytes TX_REQUEST_KEY_PREFIX = Bytes.fromString("RQ" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes TX_RESULT_KEY_PREFIX = Bytes.fromString("RT" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes TX_SEQUENCE_KEY_PREFIX = Bytes.fromString("SQ" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes TX_SET_KEY_PREFIX = Bytes.fromString("L:/" + Bytes.fromString("TS" + LedgerConsts.KEY_SEPERATOR));

	private static final Bytes TX_TOTOAL_KEY_PREFIX = Bytes.fromString("T");


	/**
	 * 交易状态集合；用于记录交易执行结果；
	 */
	private BaseDataset<Bytes, byte[]> txStateSet;

	/**
	 * 交易请求列表；用于记录交易请求的顺序；
	 */
	private MerkleList<HashDigest> txSequence;

	private LedgerDataStructure ledgerDataStructure;

	private ExPolicyKVStorage archiveExStorage;

	private VersioningKVStorage archiveDataStorage;

	/**
	 * 交易请求列表的根哈希在交易状态集合中的版本；(key 为 {@link #TX_REQUEST_ROOT_HASH} );
	 * 
	 */
	private volatile long txRequestBlockID;

	// start: only take effect in kv ledger structure
	private volatile long txIndex = 0;

	private volatile long origin_txIndex = 0;

	private long preBlockHeight;

	private ArrayList<HashDigest> transactions = new ArrayList<>();
    // end: only take effect in kv ledger structure


	/**
	 * Create a new TransactionSet which can be added transaction;
	 * 
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSetEditor(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage merkleTreeStorage,
			VersioningKVStorage dataStorage, LedgerDataStructure dataStructure) {

		ledgerDataStructure = dataStructure;

		if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			Bytes txStatePrefix = Bytes.fromString(keyPrefix + TX_STATE_PREFIX);
			this.txStateSet = new MerkleHashDataset(setting, txStatePrefix, merkleTreeStorage, dataStorage);

			this.txRequestBlockID = -1;

			Bytes txSequencePrefix = Bytes.fromString(keyPrefix).concat(TX_SEQUENCE_KEY_PREFIX);

			TreeOptions options = TreeOptions.build().setDefaultHashAlgorithm(setting.getHashAlgorithm())
					.setVerifyHashOnLoad(setting.getAutoVerifyHash());
			this.txSequence = new MerkleList<>(options, txSequencePrefix, merkleTreeStorage, HASH_DIGEST_CONVERTER);
		} else {
			this.preBlockHeight = -1;
			this.txStateSet = new KvDataset(DatasetType.TX, setting, keyPrefix, merkleTreeStorage, dataStorage);
		}
	}

	/**
	 * Create TransactionSet which is readonly to the history transactions;
	 *
	 * @param setting
	 * @param merkleTreeStorage
	 * @param dataStorage
	 */
	public TransactionSetEditor(long preBlockHeight, HashDigest txRootHash, CryptoSetting setting, String keyPrefix,
								ExPolicyKVStorage merkleTreeStorage, VersioningKVStorage dataStorage,
								ExPolicyKVStorage archiveExStorage, VersioningKVStorage archiveDataStorage,
								LedgerDataStructure dataStructure, boolean readonly) {
		ledgerDataStructure = dataStructure;

		// archive storage used in kv database mode
		this.archiveExStorage = archiveExStorage;
		this.archiveDataStorage = archiveDataStorage;

		if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			Bytes txStatePrefix = Bytes.fromString(keyPrefix + TX_STATE_PREFIX);
			this.txStateSet = new MerkleHashDataset(txRootHash, setting, txStatePrefix, merkleTreeStorage, dataStorage,
					readonly);

			DataEntry<Bytes, byte[]> txReqRootHashData = this.txStateSet.getDataEntry(TX_REQUEST_ROOT_HASH);
			this.txRequestBlockID = txReqRootHashData.getVersion();
			HashDigest txRequestRootHash = Crypto.resolveAsHashDigest(txReqRootHashData.getValue());

			Bytes txRequestPrefix = Bytes.fromString(keyPrefix).concat(TX_SEQUENCE_KEY_PREFIX);
			TreeOptions options = TreeOptions.build().setDefaultHashAlgorithm(setting.getHashAlgorithm())
					.setVerifyHashOnLoad(setting.getAutoVerifyHash());
			this.txSequence = new MerkleList<>(txRequestRootHash, options, txRequestPrefix, merkleTreeStorage,
					HASH_DIGEST_CONVERTER);

		} else {
			this.preBlockHeight = preBlockHeight;
			this.txStateSet = new KvDataset(preBlockHeight, txRootHash, DatasetType.TX, setting, keyPrefix, merkleTreeStorage, dataStorage,
					readonly);
		}
	}

	@Override
	public LedgerTransaction[] getTransactions(int fromIndex, int count) {
		LedgerTransaction[] ledgerTransactions;

		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}

		if (ledgerDataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			SkippingIterator<HashDigest> txReqIterator = txSequence.iterator();

			txReqIterator.skip(fromIndex);

			int txCount = (int) Math.min(txReqIterator.getCount(), (long) count);
			ledgerTransactions = new LedgerTransaction[txCount];

			for (int i = 0; i < txCount; i++) {
				HashDigest txHash = txReqIterator.next();
				ledgerTransactions[i] = getTransaction(txHash);
			}
		} else {
			int txCount = (int) Math.min(getTotalCount() - fromIndex, (long) count);
			ledgerTransactions = new LedgerTransaction[txCount];

			for (int i = 0; i < txCount; i++) {
//				HashDigest txHash = loadReqHash(fromIndex + i);
//				ledgerTransactions[i] = getTransaction(txHash);
				ledgerTransactions[i] = getTransaction(fromIndex + i);
			}
		}
		return ledgerTransactions;
	}

	@Override
	public TransactionResult[] getTransactionResults(int fromIndex, int count) {
		TransactionResult[] transactionResults;

		if (count > LedgerConsts.MAX_LIST_COUNT) {
			throw new IllegalArgumentException("Count exceed the upper limit[" + LedgerConsts.MAX_LIST_COUNT + "]!");
		}

		if (ledgerDataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			SkippingIterator<HashDigest> txReqIterator = txSequence.iterator();

			txReqIterator.skip(fromIndex);

			int txCount = (int) Math.min(txReqIterator.getCount(), (long) count);
			transactionResults = new TransactionResult[txCount];

			for (int i = 0; i < txCount; i++) {
				HashDigest txHash = txReqIterator.next();
				transactionResults[i] = loadResult(txHash);
			}
		} else {
			int txCount = (int) Math.min(getTotalCount()-fromIndex, (long) count);
			transactionResults = new TransactionResult[txCount];

			for (int i = 0; i < txCount; i++) {
//				HashDigest txHash = loadReqHash(fromIndex + i);
				transactionResults[i] = loadResultKv(fromIndex + i);
			}

		}
		return transactionResults;
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
		if (ledgerDataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			return txSequence.size();
		} else {
			return txStateSet.getDataCount() + txIndex;
		}

	}

	/**
	 * @param txRequest
	 * @param result
	 */
	public void addTransaction(TransactionRequest txRequest, TransactionResult txResult) {
		// TODO: 优化对交易内存存储的优化，应对大数据量单交易，共享操作的“写集”与实际写入账户的KV版本；
		if (ledgerDataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			txSequence.add(txRequest.getTransactionHash());
			saveRequest(txRequest);
			saveResult(txResult);
		} else {
			saveRequest(txRequest);
			saveResult(txResult);
			saveSequence(txRequest);
			transactions.add(txRequest.getTransactionHash());
		}
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

		if (ledgerDataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			TransactionRequest txRequest = loadRequest(txContentHash);
			if (null == txRequest) {
				return null;
			}
			TransactionResult txResult = loadResult(txContentHash);
			if (null == txResult) {
				return null;
			}
			return new LedgerTransactionData(txRequest, txResult);
		} else {

			long seq = loadReqSeq(txContentHash);
			return getTransaction(seq);
		}
	}

	private long loadReqSeq(HashDigest txContentHash) {
		Bytes key = encodeSeqKey(txContentHash);
		byte[] txReqIndex = txStateSet.getValue(key, 0);

		return BytesUtils.toLong(txReqIndex);
	}

	// 当底层存储模式为KV时才会调用该方法，可以根据交易序号获取交易内容
	@Override
	public LedgerTransaction getTransaction(long txSeq) {
		TransactionRequest txRequest = loadRequestKv(txSeq);
		if(null == txRequest) {
			return null;
		}
		TransactionResult txResult = loadResultKv(txSeq);
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
		byte[] txBytes = txStateSet.getValue(key, 0);
		if (txBytes == null) {
			return null;
		}
		return BinaryProtocol.decode(txBytes, TransactionResult.class);
	}

	private TransactionResult loadResultKv(long seq) {
		byte[] txResBytes;
		// transaction has only one version;
		Bytes key = encodeKvResultKey(seq);
		try {
			txResBytes = txStateSet.getValue(key, 0);
		} catch (DataExistException e) {
			// 增加对归档数据库的交易结果数据查询
			txResBytes = archiveGetTxRes(seq);
			if (txResBytes == null) {
				throw e;
			}
		}

		return BinaryProtocol.decode(txResBytes, TransactionResult.class);
	}

	private void saveResult(TransactionResult txResult) {
		// 序列化交易内容；
		byte[] txResultBytes = BinaryProtocol.encode(txResult, TransactionResult.class);
		Bytes key;

		if (ledgerDataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			// 以交易内容的 hash 为 key；
			key = encodeResultKey(txResult.getTransactionHash());
		} else {
			key = encodeKvResultKey(txStateSet.getDataCount() + txIndex);
		}
		// 交易只有唯一的版本；
		long v = txStateSet.setValue(key, txResultBytes);
		if (v < 0) {
			throw new IllegalTransactionException("Repeated transaction request! --[" + key + "]");
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

	// 以账本为维度，加载指定交易索引的交易请求hash
	private TransactionRequest loadRequestKv(long seq) {
		byte[] txReqBytes;

		Bytes key = encodeKvRequestKey(seq);
		try {
			txReqBytes = txStateSet.getValue(key, 0);
		} catch (DataExistException e) {
			// 增加对归档数据库的交易内容数据查询
			txReqBytes = archiveGetTxReq(seq);
			if (txReqBytes == null) {
				throw e;
			}
		}

		return BinaryProtocol.decode(txReqBytes, TransactionRequest.class);
	}

	// 从归档数据库查询交易结果
	private byte[] archiveGetTxRes(long txSeq) {
		Bytes txResKey = new Bytes(TX_SET_KEY_PREFIX, TX_RESULT_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(txSeq))));
		return archiveDataStorage.archiveGet(txResKey, 0);
	}

	// 从归档数据库查询交易内容
	private byte[] archiveGetTxReq(long txSeq) {
		Bytes txReqKey = new Bytes(TX_SET_KEY_PREFIX, TX_REQUEST_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(txSeq))));
		return archiveDataStorage.archiveGet(txReqKey, 0);
	}

	private void saveRequest(TransactionRequest txRequest) {
		// 序列化交易内容；
		byte[] txRequestBytes = BinaryProtocol.encode(txRequest, TransactionRequest.class);

		Bytes key;
		if (ledgerDataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			// 以交易内容的 hash 为 key；
			key = encodeRequestKey(txRequest.getTransactionHash());
		} else {
			key = encodeKvRequestKey(txStateSet.getDataCount() + txIndex);
		}
		// 交易只有唯一的版本；
		long v = txStateSet.setValue(key, txRequestBytes);
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

	private Bytes encodeKvRequestKey(long seq) {
		return TX_REQUEST_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(seq)));
	}

	private Bytes encodeKvResultKey(long seq) {
		return TX_RESULT_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(seq)));
	}

//	private Bytes encodeSeqKey(long seq) {
//		return TX_SEQUENCE_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(seq)));
//	}

	private Bytes encodeSeqKey(HashDigest txContentHash) {
		return TX_SEQUENCE_KEY_PREFIX.concat(txContentHash);
	}

	private Bytes encodeTotalNumKey() {
		return TX_TOTOAL_KEY_PREFIX;
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
		if (ledgerDataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			txSequence.commit();
			HashDigest txReqRootHash = txSequence.getRootHash();
			long v = txStateSet.setValue(TX_REQUEST_ROOT_HASH, txReqRootHash.toBytes(), txRequestBlockID);
			if (v < 0) {
				throw new LedgerException("Fail to save the root hash of transaction request!");
			}
			txStateSet.commit();
			txRequestBlockID = v;
		} else {
			origin_txIndex = txIndex;
			saveTotalByHeight();
			txStateSet.commit();
		}
	}

	@Override
	public void cancel() {
		if (ledgerDataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			txSequence.cancel();
			txStateSet.cancel();
		} else {
			txStateSet.cancel();
			// 恢复到上次的交易索引
			txIndex = origin_txIndex;
		}
	}

	// 以账本为维度记录交易哈希对应的索引号
	private void saveSequence(TransactionRequest txRequest) {
		// key = keyprefix/SQ/txHash
		Bytes key = encodeSeqKey(txRequest.getTransactionHash());

		// 交易序号只有唯一的版本；
		long v = txStateSet.setValue(key, BytesUtils.toBytes(txStateSet.getDataCount() + txIndex));
		if (v < 0) {
			throw new IllegalTransactionException("Repeated transaction request sequence! --[" + key + "]");
		}
		txIndex++;
	}

	// 按照区块高度记录交易总数
	private void saveTotalByHeight() {
		// key = keyprefix/T
		Bytes key = encodeTotalNumKey();

		// 交易序号只有唯一的版本；
		long v = txStateSet.setValue(key, BytesUtils.toBytes(getTotalCount()), preBlockHeight);
		if (v < 0) {
			throw new IllegalTransactionException("Repeated transaction request sequence! --[" + key + "]");
		}
	}

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

	public void clearCachedIndex() {
		txIndex = 0;
	}

	public void updatePreBlockHeight(long newBlockHeight) {
		txStateSet.updatePreBlockHeight(newBlockHeight);
	}

}

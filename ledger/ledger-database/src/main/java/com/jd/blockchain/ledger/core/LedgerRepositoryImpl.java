package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.BlockBody;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.LedgerAdminSettings;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerDataSnapshot;
import com.jd.blockchain.ledger.LedgerInitSetting;
import com.jd.blockchain.ledger.LedgerSettings;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;

import utils.Bytes;
import utils.codec.Base58Utils;

/**
 * 账本的存储结构： <br>
 * 
 * 1、账本数据以版本化KV存储({@link VersioningKVStorage})为基础； <br>
 * 
 * 2、以账本hash为 key，保存账本的每一个区块的hash，对应的版本序号恰好一致地表示了区块高度； <br>
 * 
 * 3、区块数据以区块 hash 加上特定前缀({@link #BLOCK_PREFIX}) 构成 key
 * 进行保存，每个区块只有唯一个版本，在存储时会进行版本唯一性校验； <br>
 * 
 * @author huanghaiquan
 *
 */
class LedgerRepositoryImpl implements LedgerRepository {

	private static final Bytes LEDGER_PREFIX = Bytes.fromString("IDX" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes BLOCK_PREFIX = Bytes.fromString("BLK" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes USER_SET_PREFIX = Bytes.fromString("USRS" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes DATA_SET_PREFIX = Bytes.fromString("DATS" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes CONTRACT_SET_PREFIX = Bytes.fromString("CTRS" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes TRANSACTION_SET_PREFIX = Bytes.fromString("TXS" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes SYSTEM_EVENT_SET_PREFIX = Bytes.fromString("SEVT" + LedgerConsts.KEY_SEPERATOR);

	private static final Bytes USER_EVENT_SET_PREFIX = Bytes.fromString("UEVT" + LedgerConsts.KEY_SEPERATOR);

	private static final AccountAccessPolicy DEFAULT_ACCESS_POLICY = new OpeningAccessPolicy();

	private HashDigest ledgerHash;

	private final String keyPrefix;

	private Bytes ledgerIndexKey;

	private VersioningKVStorage versioningStorage;

	private ExPolicyKVStorage exPolicyStorage;

	private volatile LedgerState latestState;

	private volatile LedgerEditor nextBlockEditor;

	private String anchorType;

	/**
	 * 账本结构版本号
	 *         默认为-1，需通过MetaData获取
	 */
	private volatile long ledgerStructureVersion = -1L;

	private volatile boolean closed = false;

	public LedgerRepositoryImpl(HashDigest ledgerHash, String keyPrefix, ExPolicyKVStorage exPolicyStorage,
			VersioningKVStorage versioningStorage, String anchorType) {
		this.keyPrefix = keyPrefix;

		this.ledgerHash = ledgerHash;
		this.versioningStorage = versioningStorage;
		this.exPolicyStorage = exPolicyStorage;
		this.ledgerIndexKey = encodeLedgerIndexKey(ledgerHash);
		this.anchorType = anchorType;

		if (getLatestBlockHeight() < 0) {
			throw new RuntimeException("Ledger doesn't exist!");
		}

		retrieveLatestState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jd.blockchain.ledger.core.LedgerRepository#getHash()
	 */
	@Override
	public HashDigest getHash() {
		return ledgerHash;
	}

	@Override
	public long getVersion() {
		return ledgerStructureVersion;
	}

	@Override
	public HashDigest getLatestBlockHash() {
		if (latestState == null) {
			return innerGetBlockHash(innerGetLatestBlockHeight());
		}
		return latestState.block.getHash();
	}

	@Override
	public long getLatestBlockHeight() {
		if (latestState == null) {
			return innerGetLatestBlockHeight();
		}
		return latestState.block.getHeight();
	}

	@Override
	public LedgerBlock getLatestBlock() {
		return latestState.block;
	}

	@Override
	public String getAnchorType() {
		return anchorType;
	}
	/**
	 * 重新检索加载最新的状态；
	 * 
	 * @return
	 */
	private LedgerState retrieveLatestState() {
		LedgerBlock latestBlock = innerGetBlock(innerGetLatestBlockHeight());
		LedgerDataSetEditor ledgerDataset = innerGetLedgerDataset(latestBlock);
		TransactionSet txSet;
		if (anchorType.equals("default")) {
			txSet = loadTransactionSet(latestBlock.getTransactionSetHash(),
					ledgerDataset.getAdminDataset().getSettings().getCryptoSetting(), keyPrefix, exPolicyStorage,
					versioningStorage, true);
		} else {
			txSet = loadTransactionSetSimple(latestBlock.getTransactionSetHash(),
					ledgerDataset.getAdminDataset().getSettings().getCryptoSetting(), keyPrefix, exPolicyStorage,
					versioningStorage, true);
		}
		LedgerEventSetEditor ledgerEventset = innerGetLedgerEventSet(latestBlock);
		this.latestState = new LedgerState(latestBlock, ledgerDataset, txSet, ledgerEventset);
		this.ledgerStructureVersion = ledgerDataset.getAdminDataset().getMetadata().getLedgerStructureVersion();
		return latestState;
	}

	@Override
	public LedgerBlock retrieveLatestBlock() {
		return retrieveLatestState().block;
	}

	@Override
	public HashDigest retrieveLatestBlockHash() {
		HashDigest latestBlockHash = innerGetBlockHash(innerGetLatestBlockHeight());
		if (latestState != null && !latestBlockHash.equals(latestState.block.getHash())) {
			latestState = null;
		}
		return latestBlockHash;
	}

	@Override
	public long retrieveLatestBlockHeight() {
		long latestBlockHeight = innerGetLatestBlockHeight();
		if (latestState != null && latestBlockHeight != latestState.block.getHeight()) {
			latestState = null;
		}
		return latestBlockHeight;
	}

	private long innerGetLatestBlockHeight() {
		return versioningStorage.getVersion(ledgerIndexKey);
	}

	@Override
	public HashDigest getBlockHash(long height) {
		LedgerBlock blk = latestState == null ? null : latestState.block;
		if (blk != null && height == blk.getHeight()) {
			return blk.getHash();
		}
		return innerGetBlockHash(height);
	}

	private HashDigest innerGetBlockHash(long height) {
		if (height < 0) {
			return null;
		}
		// get block hash by height;
		byte[] hashBytes = versioningStorage.get(ledgerIndexKey, height);
		if (hashBytes == null || hashBytes.length == 0) {
			return null;
		}
		return Crypto.resolveAsHashDigest(hashBytes);
	}

	@Override
	public LedgerBlock getBlock(long height) {
		LedgerBlock blk = latestState == null ? null : latestState.block;
		if (blk != null && height == blk.getHeight()) {
			return blk;
		}
		return innerGetBlock(height);
	}

	private LedgerBlock innerGetBlock(long height) {
		if (height < 0) {
			return null;
		}
		return innerGetBlock(innerGetBlockHash(height));
	}

	@Override
	public LedgerBlock getBlock(HashDigest blockHash) {
		LedgerBlock blk = latestState == null ? null : latestState.block;
		if (blk != null && blockHash.equals(blk.getHash())) {
			return blk;
		}
		return innerGetBlock(blockHash);
	}

	private LedgerBlock innerGetBlock(HashDigest blockHash) {
		if (blockHash == null) {
			return null;
		}
		Bytes key = encodeBlockStorageKey(blockHash);
		// Every one block has only one version;
		byte[] blockBytes = versioningStorage.get(key, 0);
		if(null == blockBytes) {
			return null;
		}
		LedgerBlockData block = new LedgerBlockData(deserialize(blockBytes));

		if (!blockHash.equals(block.getHash())) {
			throw new RuntimeException("Block hash not equals to it's storage key!");
		}

		// verify block hash;
		byte[] blockBodyBytes = null;
		if (block.getHeight() == 0) {
			// 计算创世区块的 hash 时，不包括 ledgerHash 字段；
			blockBodyBytes = BinaryProtocol.encode(block, BlockBody.class);
		} else {
			blockBodyBytes = BinaryProtocol.encode(block, BlockBody.class);
		}
		HashFunction hashFunc = Crypto.getHashFunction(blockHash.getAlgorithm());
		boolean pass = hashFunc.verify(blockHash, blockBodyBytes);
		if (!pass) {
			throw new RuntimeException("Block hash verification fail!");
		}

		// verify height;
		HashDigest indexedHash = getBlockHash(block.getHeight());
		if (indexedHash == null || !indexedHash.equals(blockHash)) {
			throw new RuntimeException(
					"Illegal ledger state in storage that ledger height index doesn't match it's block data in height["
							+ block.getHeight() + "] and block hash[" + Base58Utils.encode(blockHash.toBytes())
							+ "] !");
		}

		return block;
	}

	/**
	 * 获取最新区块的账本参数；
	 * 
	 * @return
	 */
	private LedgerSettings getLatestSettings() {
		return getAdminInfo().getSettings();
	}

	@Override
	public LedgerAdminInfo getAdminInfo() {
		return createAdminData(getLatestBlock());
	}

	private LedgerBlock deserialize(byte[] blockBytes) {
		return BinaryProtocol.decode(blockBytes);
	}

	@Override
	public TransactionSet getTransactionSet(LedgerBlock block) {
		long height = getLatestBlockHeight();
		if (height == block.getHeight()) {
			// 从缓存中返回最新区块的数据集；
			return latestState.getTransactionSet();
		}
		LedgerAdminInfo adminAccount = getAdminInfo(block);
		// All of existing block is readonly;
		return anchorType.equals("default")? loadTransactionSet(block.getTransactionSetHash(), adminAccount.getSettings().getCryptoSetting(),
				keyPrefix, exPolicyStorage, versioningStorage, true) : loadTransactionSetSimple(block.getTransactionSetHash(), adminAccount.getSettings().getCryptoSetting(),
				keyPrefix, exPolicyStorage, versioningStorage, true);
	}

	@Override
	public LedgerAdminInfo getAdminInfo(LedgerBlock block) {
		return createAdminData(block);
	}
	
	@Override
	public LedgerAdminSettings getAdminSettings() {
		return getAdminSettings(getLatestBlock());
	}
	
	@Override
	public LedgerAdminSettings getAdminSettings(LedgerBlock block) {
		long height = getLatestBlockHeight();
		if (height == block.getHeight()) {
			return (LedgerAdminSettings) latestState.getAdminDataset();
		}

		return anchorType.equals("default") ? createAdminDataset(block) : createAdminDatasetSimple(block);
	}
	
	@Override
	public LedgerDiffView getDiffView(LedgerBlock recentBlock, LedgerBlock previousBlock) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 生成LedgerAdminInfoData对象
	 *     该对象主要用于页面展示
	 *
	 * @param block
	 * @return
	 */
	private LedgerAdminInfoData createAdminData(LedgerBlock block) {
		return new LedgerAdminInfoData(anchorType.equals("default") ? createAdminDataset(block) : createAdminDatasetSimple(block));
	}

	/**
	 * 生成LedgerAdminDataset对象
	 *
	 * @param block
	 * @return
	 */
	private LedgerAdminDataSetEditor createAdminDataset(LedgerBlock block) {
		return new LedgerAdminDataSetEditor(block.getAdminAccountHash(), keyPrefix, exPolicyStorage, versioningStorage, true);
	}

	private LedgerAdminDataSetEditorSimple createAdminDatasetSimple(LedgerBlock block) {
		return new LedgerAdminDataSetEditorSimple(block.getAdminAccountHash(), keyPrefix, exPolicyStorage, versioningStorage, true);
	}

	@Override
	public UserAccountSet getUserAccountSet(LedgerBlock block) {
		long height = getLatestBlockHeight();
		if (height == block.getHeight()) {
			return latestState.getUserAccountSet();
		}
		LedgerAdminSettings adminAccount = getAdminSettings(block);
		return anchorType.equals("default") ? createUserAccountSet(block, adminAccount.getSettings().getCryptoSetting()) : createUserAccountSetSimple(block, adminAccount.getSettings().getCryptoSetting());
	}

	private UserAccountSetEditor createUserAccountSet(LedgerBlock block, CryptoSetting cryptoSetting) {
		return loadUserAccountSet(block.getUserAccountSetHash(), cryptoSetting, keyPrefix, exPolicyStorage,
				versioningStorage, true);
	}

	private UserAccountSetEditorSimple createUserAccountSetSimple(LedgerBlock block, CryptoSetting cryptoSetting) {
		return loadUserAccountSetSimple(block.getUserAccountSetHash(), cryptoSetting, keyPrefix, exPolicyStorage,
				versioningStorage, true);
	}

	@Override
	public DataAccountSet getDataAccountSet(LedgerBlock block) {
		long height = getLatestBlockHeight();
		if (height == block.getHeight()) {
			return latestState.getDataAccountSet();
		}

		LedgerAdminSettings adminAccount = getAdminSettings(block);
		return anchorType.equals("default") ? createDataAccountSet(block, adminAccount.getSettings().getCryptoSetting()) : createDataAccountSetSimple(block, adminAccount.getSettings().getCryptoSetting());
	}

	private DataAccountSetEditor createDataAccountSet(LedgerBlock block, CryptoSetting setting) {
		return loadDataAccountSet(block.getDataAccountSetHash(), setting, keyPrefix, exPolicyStorage, versioningStorage,
				true);
	}

	private DataAccountSetEditorSimple createDataAccountSetSimple(LedgerBlock block, CryptoSetting setting) {
		return loadDataAccountSetSimple(block.getDataAccountSetHash(), setting, keyPrefix, exPolicyStorage, versioningStorage,
				true);
	}

	@Override
	public ContractAccountSet getContractAccountSet(LedgerBlock block) {
		long height = getLatestBlockHeight();
		if (height == block.getHeight()) {
			return latestState.getContractAccountSet();
		}

		LedgerAdminSettings adminAccount = getAdminSettings(block);
		return anchorType.equals("default") ? createContractAccountSet(block, adminAccount.getSettings().getCryptoSetting()) : createContractAccountSetSimple(block, adminAccount.getSettings().getCryptoSetting());
	}

	@Override
	public EventGroup getSystemEventGroup(LedgerBlock block) {
		long height = getLatestBlockHeight();
		if (height == block.getHeight()) {
			return latestState.getLedgerEventSet().getSystemEventGroup();
		}

		LedgerAdminSettings adminAccount = getAdminSettings(block);
		return createSystemEventSet(block, adminAccount.getSettings().getCryptoSetting());
	}

	private MerkleEventGroupPublisher createSystemEventSet(LedgerBlock block, CryptoSetting cryptoSetting) {
		return loadSystemEventSet(block.getSystemEventSetHash(), cryptoSetting, keyPrefix, exPolicyStorage,
				versioningStorage, true);
	}

	private MerkleEventGroupPublisherSimple createSystemEventSetSimple(LedgerBlock block, CryptoSetting cryptoSetting) {
		return loadSystemEventSetSimple(block.getSystemEventSetHash(), cryptoSetting, keyPrefix, exPolicyStorage,
				versioningStorage, true);
	}

	@Override
	public EventAccountSet getEventAccountSet(LedgerBlock block) {
		long height = getLatestBlockHeight();
		if (height == block.getHeight()) {
			return latestState.getLedgerEventSet().getEventAccountSet();
		}

		LedgerAdminSettings adminAccount = getAdminSettings(block);
		return createUserEventSet(block, adminAccount.getSettings().getCryptoSetting());
	}

	private EventAccountSetEditor createUserEventSet(LedgerBlock block, CryptoSetting cryptoSetting) {
		return loadUserEventSet(block.getUserEventSetHash(), cryptoSetting, keyPrefix, exPolicyStorage,
				versioningStorage, true);
	}

	private EventAccountSetEditorSimple createUserEventSetSimple(LedgerBlock block, CryptoSetting cryptoSetting) {
		return loadUserEventSetSimple(block.getUserEventSetHash(), cryptoSetting, keyPrefix, exPolicyStorage,
				versioningStorage, true);
	}

	private ContractAccountSetEditor createContractAccountSet(LedgerBlock block, CryptoSetting cryptoSetting) {
		return loadContractAccountSet(block.getContractAccountSetHash(), cryptoSetting, keyPrefix, exPolicyStorage,
				versioningStorage, true);
	}

	private ContractAccountSetEditorSimple createContractAccountSetSimple(LedgerBlock block, CryptoSetting cryptoSetting) {
		return loadContractAccountSetSimple(block.getContractAccountSetHash(), cryptoSetting, keyPrefix, exPolicyStorage,
				versioningStorage, true);
	}

	@Override
	public LedgerDataSet getLedgerDataSet(LedgerBlock block) {
		long height = getLatestBlockHeight();
		if (height == block.getHeight()) {
			return latestState.getLedgerDataset();
		}

		// All of existing block is readonly;
		return anchorType.equals("default") ? innerGetLedgerDataset(block) : innerGetLedgerDatasetSimple(block);
	}

	@Override
	public LedgerEventSet getLedgerEventSet(LedgerBlock block) {
		long height = getLatestBlockHeight();
		if (height == block.getHeight()) {
			return latestState.getLedgerEventSet();
		}

		// All of existing block is readonly;
		return innerGetLedgerEventSet(block);
	}

	private LedgerDataSetEditor innerGetLedgerDataset(LedgerBlock block) {
		LedgerAdminDataSetEditor adminDataset = createAdminDataset(block);
		CryptoSetting cryptoSetting = adminDataset.getSettings().getCryptoSetting();

		UserAccountSetEditor userAccountSet = createUserAccountSet(block, cryptoSetting);
		DataAccountSetEditor dataAccountSet = createDataAccountSet(block, cryptoSetting);
		ContractAccountSetEditor contractAccountSet = createContractAccountSet(block, cryptoSetting);
		return new LedgerDataSetEditor(adminDataset, userAccountSet, dataAccountSet, contractAccountSet, true);
	}

	private LedgerDataSetEditorSimple innerGetLedgerDatasetSimple(LedgerBlock block) {
		LedgerAdminDataSetEditorSimple adminDataset = createAdminDatasetSimple(block);
		CryptoSetting cryptoSetting = adminDataset.getSettings().getCryptoSetting();

		UserAccountSetEditorSimple userAccountSet = createUserAccountSetSimple(block, cryptoSetting);
		DataAccountSetEditorSimple dataAccountSet = createDataAccountSetSimple(block, cryptoSetting);
		ContractAccountSetEditorSimple contractAccountSet = createContractAccountSetSimple(block, cryptoSetting);
		return new LedgerDataSetEditorSimple(adminDataset, userAccountSet, dataAccountSet, contractAccountSet, true);
	}

	private LedgerEventSetEditor innerGetLedgerEventSet(LedgerBlock block) {
		LedgerAdminDataSetEditor adminDataset = createAdminDataset(block);
		CryptoSetting cryptoSetting = adminDataset.getSettings().getCryptoSetting();

		MerkleEventGroupPublisher systemEventSet = createSystemEventSet(block, cryptoSetting);
		EventAccountSetEditor userEventSet = createUserEventSet(block, cryptoSetting);
		return new LedgerEventSetEditor(systemEventSet, userEventSet, true);
	}

	private LedgerEventSetEditorSimple innerGetLedgerEventSetSimple(LedgerBlock block) {
		LedgerAdminDataSetEditorSimple adminDataset = createAdminDatasetSimple(block);
		CryptoSetting cryptoSetting = adminDataset.getSettings().getCryptoSetting();

		MerkleEventGroupPublisherSimple systemEventSet = createSystemEventSetSimple(block, cryptoSetting);
		EventAccountSetEditorSimple userEventSet = createUserEventSetSimple(block, cryptoSetting);
		return new LedgerEventSetEditorSimple(systemEventSet, userEventSet, true);
	}

	public synchronized void resetNextBlockEditor() {
		this.nextBlockEditor = null;
	}

	@Override
	public synchronized LedgerEditor createNextBlock() {
		LedgerEditor editor;

		if (closed) {
			throw new RuntimeException("Ledger repository has been closed!");
		}
		if (this.nextBlockEditor != null) {
			throw new RuntimeException(
					"A new block is in process, cann't create another one until it finish by committing or canceling.");
		}
		LedgerBlock previousBlock = getLatestBlock();

		if (false) {//default simple
			editor = LedgerTransactionalEditorSimple.createEditor(previousBlock, getLatestSettings(),
				keyPrefix, exPolicyStorage, versioningStorage);
		} else {
			editor = LedgerTransactionalEditor.createEditor(previousBlock, getLatestSettings(),
					keyPrefix, exPolicyStorage, versioningStorage);
		}

		NewBlockCommittingMonitor committingMonitor = new NewBlockCommittingMonitor(editor, this);
		this.nextBlockEditor = committingMonitor;
		return committingMonitor;
	}

	@Override
	public LedgerEditor getNextBlockEditor() {
		return nextBlockEditor;
	}
	
	@Override
	public LedgerSecurityManager getSecurityManager() {
		LedgerBlock ledgerBlock = getLatestBlock();
		
		LedgerDataSet ledgerDataQuery = getLedgerDataSet(ledgerBlock);
		LedgerAdminDataSet previousAdminDataset = ledgerDataQuery.getAdminDataset();
		LedgerSecurityManager securityManager = new LedgerSecurityManagerImpl(previousAdminDataset.getAdminSettings().getRolePrivileges(),
				previousAdminDataset.getAdminSettings().getAuthorizations(), previousAdminDataset.getParticipantDataset(),
				ledgerDataQuery.getUserAccountSet());
		return securityManager;
	}

	@Override
	public synchronized void close() {
		if (closed) {
			return;
		}
		if (this.nextBlockEditor != null) {
			throw new RuntimeException("A new block is in process, cann't close the ledger repository!");
		}
		closed = true;
	}
	
	static Bytes encodeLedgerIndexKey(HashDigest ledgerHash) {
		return LEDGER_PREFIX.concat(ledgerHash);
	}

	static Bytes encodeBlockStorageKey(HashDigest blockHash) {
		return BLOCK_PREFIX.concat(blockHash);
	}

	static LedgerDataSetEditor newDataSet(LedgerInitSetting initSetting, String keyPrefix,
			ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage) {
		LedgerAdminDataSetEditor adminAccount = new LedgerAdminDataSetEditor(initSetting, keyPrefix, ledgerExStorage,
				ledgerVerStorage);

		String usersetKeyPrefix = keyPrefix + USER_SET_PREFIX;
		String datasetKeyPrefix = keyPrefix + DATA_SET_PREFIX;
		String contractsetKeyPrefix = keyPrefix + CONTRACT_SET_PREFIX;

		UserAccountSetEditor userAccountSet = new UserAccountSetEditor(adminAccount.getSettings().getCryptoSetting(),
				usersetKeyPrefix, ledgerExStorage, ledgerVerStorage, DEFAULT_ACCESS_POLICY);

		DataAccountSetEditor dataAccountSet = new DataAccountSetEditor(adminAccount.getSettings().getCryptoSetting(),
				datasetKeyPrefix, ledgerExStorage, ledgerVerStorage, DEFAULT_ACCESS_POLICY);

		ContractAccountSetEditor contractAccountSet = new ContractAccountSetEditor(adminAccount.getSettings().getCryptoSetting(),
				contractsetKeyPrefix, ledgerExStorage, ledgerVerStorage, DEFAULT_ACCESS_POLICY);

		LedgerDataSetEditor newDataSet = new LedgerDataSetEditor(adminAccount, userAccountSet, dataAccountSet,
				contractAccountSet, false);

		return newDataSet;
	}

	static LedgerDataSetEditorSimple newDataSetSimple(LedgerInitSetting initSetting, String keyPrefix,
										  ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage) {
		LedgerAdminDataSetEditorSimple adminAccount = new LedgerAdminDataSetEditorSimple(initSetting, keyPrefix, ledgerExStorage,
				ledgerVerStorage);

		String usersetKeyPrefix = keyPrefix + USER_SET_PREFIX;
		String datasetKeyPrefix = keyPrefix + DATA_SET_PREFIX;
		String contractsetKeyPrefix = keyPrefix + CONTRACT_SET_PREFIX;

		UserAccountSetEditorSimple userAccountSet = new UserAccountSetEditorSimple(adminAccount.getSettings().getCryptoSetting(),
				usersetKeyPrefix, ledgerExStorage, ledgerVerStorage, DEFAULT_ACCESS_POLICY);

		DataAccountSetEditorSimple dataAccountSet = new DataAccountSetEditorSimple(adminAccount.getSettings().getCryptoSetting(),
				datasetKeyPrefix, ledgerExStorage, ledgerVerStorage, DEFAULT_ACCESS_POLICY);

		ContractAccountSetEditorSimple contractAccountSet = new ContractAccountSetEditorSimple(adminAccount.getSettings().getCryptoSetting(),
				contractsetKeyPrefix, ledgerExStorage, ledgerVerStorage, DEFAULT_ACCESS_POLICY);

		LedgerDataSetEditorSimple newDataSet = new LedgerDataSetEditorSimple(adminAccount, userAccountSet, dataAccountSet,
				contractAccountSet, false);

		return newDataSet;
	}

	static LedgerEventSetEditor newEventSet(CryptoSetting cryptoSetting, String keyPrefix,
									ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage) {

		MerkleEventGroupPublisher systemEventSet = new MerkleEventGroupPublisher(cryptoSetting,
				keyPrefix + SYSTEM_EVENT_SET_PREFIX, ledgerExStorage, ledgerVerStorage);

		EventAccountSetEditor userEventSet = new EventAccountSetEditor(cryptoSetting,
				keyPrefix + USER_EVENT_SET_PREFIX, ledgerExStorage, ledgerVerStorage, DEFAULT_ACCESS_POLICY);

		LedgerEventSetEditor newEventSet = new LedgerEventSetEditor(systemEventSet, userEventSet, false);

		return newEventSet;
	}

	static LedgerEventSetEditorSimple newEventSetSimple(CryptoSetting cryptoSetting, String keyPrefix,
											ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage) {

		MerkleEventGroupPublisherSimple systemEventSet = new MerkleEventGroupPublisherSimple(cryptoSetting,
				keyPrefix + SYSTEM_EVENT_SET_PREFIX, ledgerExStorage, ledgerVerStorage);

		EventAccountSetEditorSimple userEventSet = new EventAccountSetEditorSimple(cryptoSetting,
				keyPrefix + USER_EVENT_SET_PREFIX, ledgerExStorage, ledgerVerStorage, DEFAULT_ACCESS_POLICY);

		LedgerEventSetEditorSimple newEventSet = new LedgerEventSetEditorSimple(systemEventSet, userEventSet, false);

		return newEventSet;
	}

	static TransactionSetEditor newTransactionSet(CryptoSetting cryptoSetting, String keyPrefix,
			ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage) {

		String txsetKeyPrefix = keyPrefix + TRANSACTION_SET_PREFIX;

		TransactionSetEditor transactionSet = new TransactionSetEditor(cryptoSetting, txsetKeyPrefix,
				ledgerExStorage, ledgerVerStorage);
		return transactionSet;
	}

	static TransactionSetEditorSimple newTransactionSetSimple(CryptoSetting cryptoSetting, String keyPrefix,
												  ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage) {

		String txsetKeyPrefix = keyPrefix + TRANSACTION_SET_PREFIX;

		TransactionSetEditorSimple transactionSet = new TransactionSetEditorSimple(cryptoSetting, txsetKeyPrefix,
				ledgerExStorage, ledgerVerStorage);
		return transactionSet;
	}

	static LedgerDataSetEditor loadDataSet(LedgerDataSnapshot dataSnapshot, CryptoSetting cryptoSetting, String keyPrefix,
			ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage, boolean readonly) {
		LedgerAdminDataSetEditor adminAccount = new LedgerAdminDataSetEditor(dataSnapshot.getAdminAccountHash(), keyPrefix,
				ledgerExStorage, ledgerVerStorage, readonly);

		UserAccountSetEditor userAccountSet = loadUserAccountSet(dataSnapshot.getUserAccountSetHash(), cryptoSetting,
				keyPrefix, ledgerExStorage, ledgerVerStorage, readonly);

		DataAccountSetEditor dataAccountSet = loadDataAccountSet(dataSnapshot.getDataAccountSetHash(), cryptoSetting,
				keyPrefix, ledgerExStorage, ledgerVerStorage, readonly);

		ContractAccountSetEditor contractAccountSet = loadContractAccountSet(dataSnapshot.getContractAccountSetHash(),
				cryptoSetting, keyPrefix, ledgerExStorage, ledgerVerStorage, readonly);

		LedgerDataSetEditor dataset = new LedgerDataSetEditor(adminAccount, userAccountSet, dataAccountSet,
				contractAccountSet, readonly);

		return dataset;
	}

	static LedgerDataSetEditorSimple loadDataSetSimple(LedgerDataSnapshot dataSnapshot, CryptoSetting cryptoSetting, String keyPrefix,
										   ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage, boolean readonly) {
		LedgerAdminDataSetEditorSimple adminAccount = new LedgerAdminDataSetEditorSimple(dataSnapshot.getAdminAccountHash(), keyPrefix,
				ledgerExStorage, ledgerVerStorage, readonly);

		UserAccountSetEditorSimple userAccountSet = loadUserAccountSetSimple(dataSnapshot.getUserAccountSetHash(), cryptoSetting,
				keyPrefix, ledgerExStorage, ledgerVerStorage, readonly);

		DataAccountSetEditorSimple dataAccountSet = loadDataAccountSetSimple(dataSnapshot.getDataAccountSetHash(), cryptoSetting,
				keyPrefix, ledgerExStorage, ledgerVerStorage, readonly);

		ContractAccountSetEditorSimple contractAccountSet = loadContractAccountSetSimple(dataSnapshot.getContractAccountSetHash(),
				cryptoSetting, keyPrefix, ledgerExStorage, ledgerVerStorage, readonly);

		LedgerDataSetEditorSimple dataset = new LedgerDataSetEditorSimple(adminAccount, userAccountSet, dataAccountSet,
				contractAccountSet, readonly);

		return dataset;
	}

	static LedgerEventSetEditor loadEventSet(LedgerDataSnapshot dataSnapshot, CryptoSetting cryptoSetting, String keyPrefix,
									   ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage, boolean readonly) {

		MerkleEventGroupPublisher systemEventSet = loadSystemEventSet(dataSnapshot.getSystemEventSetHash(), cryptoSetting,
				keyPrefix, ledgerExStorage, ledgerVerStorage, readonly);
		EventAccountSetEditor userEventSet = loadUserEventSet(dataSnapshot.getUserEventSetHash(), cryptoSetting,
				keyPrefix, ledgerExStorage, ledgerVerStorage, readonly);
		LedgerEventSetEditor newEventSet = new LedgerEventSetEditor(systemEventSet, userEventSet, false);

		return newEventSet;
	}

	static LedgerEventSetEditorSimple loadEventSetSimple(LedgerDataSnapshot dataSnapshot, CryptoSetting cryptoSetting, String keyPrefix,
											 ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage, boolean readonly) {

		MerkleEventGroupPublisherSimple systemEventSet = loadSystemEventSetSimple(dataSnapshot.getSystemEventSetHash(), cryptoSetting,
				keyPrefix, ledgerExStorage, ledgerVerStorage, readonly);
		EventAccountSetEditorSimple userEventSet = loadUserEventSetSimple(dataSnapshot.getUserEventSetHash(), cryptoSetting,
				keyPrefix, ledgerExStorage, ledgerVerStorage, readonly);
		LedgerEventSetEditorSimple newEventSet = new LedgerEventSetEditorSimple(systemEventSet, userEventSet, false);

		return newEventSet;
	}

	static UserAccountSetEditor loadUserAccountSet(HashDigest userAccountSetHash, CryptoSetting cryptoSetting,
			String keyPrefix, ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage,
			boolean readonly) {

		String usersetKeyPrefix = keyPrefix + USER_SET_PREFIX;
		return new UserAccountSetEditor(userAccountSetHash, cryptoSetting, usersetKeyPrefix, ledgerExStorage,
				ledgerVerStorage, readonly, DEFAULT_ACCESS_POLICY);
	}

	static UserAccountSetEditorSimple loadUserAccountSetSimple(HashDigest userAccountSetHash, CryptoSetting cryptoSetting,
												   String keyPrefix, ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage,
												   boolean readonly) {

		String usersetKeyPrefix = keyPrefix + USER_SET_PREFIX;
		return new UserAccountSetEditorSimple(userAccountSetHash, cryptoSetting, usersetKeyPrefix, ledgerExStorage,
				ledgerVerStorage, readonly, DEFAULT_ACCESS_POLICY);
	}

	static DataAccountSetEditor loadDataAccountSet(HashDigest dataAccountSetHash, CryptoSetting cryptoSetting,
			String keyPrefix, ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage,
			boolean readonly) {

		String datasetKeyPrefix = keyPrefix + DATA_SET_PREFIX;
		return new DataAccountSetEditor(dataAccountSetHash, cryptoSetting, datasetKeyPrefix, ledgerExStorage,
				ledgerVerStorage, readonly, DEFAULT_ACCESS_POLICY);
	}

	static DataAccountSetEditorSimple loadDataAccountSetSimple(HashDigest dataAccountSetHash, CryptoSetting cryptoSetting,
												   String keyPrefix, ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage,
												   boolean readonly) {

		String datasetKeyPrefix = keyPrefix + DATA_SET_PREFIX;
		return new DataAccountSetEditorSimple(dataAccountSetHash, cryptoSetting, datasetKeyPrefix, ledgerExStorage,
				ledgerVerStorage, readonly, DEFAULT_ACCESS_POLICY);
	}

	static ContractAccountSetEditor loadContractAccountSet(HashDigest contractAccountSetHash, CryptoSetting cryptoSetting,
			String keyPrefix, ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage,
			boolean readonly) {

		String contractsetKeyPrefix = keyPrefix + CONTRACT_SET_PREFIX;
		return new ContractAccountSetEditor(contractAccountSetHash, cryptoSetting, contractsetKeyPrefix, ledgerExStorage,
				ledgerVerStorage, readonly, DEFAULT_ACCESS_POLICY);
	}

	static ContractAccountSetEditorSimple loadContractAccountSetSimple(HashDigest contractAccountSetHash, CryptoSetting cryptoSetting,
														   String keyPrefix, ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage,
														   boolean readonly) {

		String contractsetKeyPrefix = keyPrefix + CONTRACT_SET_PREFIX;
		return new ContractAccountSetEditorSimple(contractAccountSetHash, cryptoSetting, contractsetKeyPrefix, ledgerExStorage,
				ledgerVerStorage, readonly, DEFAULT_ACCESS_POLICY);
	}

	static TransactionSetEditor loadTransactionSet(HashDigest txsetHash, CryptoSetting cryptoSetting, String keyPrefix,
			ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage, boolean readonly) {

		String txsetKeyPrefix = keyPrefix + TRANSACTION_SET_PREFIX;
		return new TransactionSetEditor(txsetHash, cryptoSetting, txsetKeyPrefix, ledgerExStorage, ledgerVerStorage,
				readonly);

	}

	static TransactionSetEditorSimple loadTransactionSetSimple(HashDigest txsetHash, CryptoSetting cryptoSetting, String keyPrefix,
												   ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage, boolean readonly) {

		String txsetKeyPrefix = keyPrefix + TRANSACTION_SET_PREFIX;
		return new TransactionSetEditorSimple(txsetHash, cryptoSetting, txsetKeyPrefix, ledgerExStorage, ledgerVerStorage,
				readonly);

	}

	static MerkleEventGroupPublisher loadSystemEventSet(HashDigest systemEventSetHash, CryptoSetting cryptoSetting,
											 String keyPrefix, ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage,
											 boolean readonly) {
		return new MerkleEventGroupPublisher(systemEventSetHash, cryptoSetting, keyPrefix+ SYSTEM_EVENT_SET_PREFIX, ledgerExStorage,
				ledgerVerStorage, readonly);
	}

	static MerkleEventGroupPublisherSimple loadSystemEventSetSimple(HashDigest systemEventSetHash, CryptoSetting cryptoSetting,
														String keyPrefix, ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage,
														boolean readonly) {
		return new MerkleEventGroupPublisherSimple(systemEventSetHash, cryptoSetting, keyPrefix+ SYSTEM_EVENT_SET_PREFIX, ledgerExStorage,
				ledgerVerStorage, readonly);
	}

	static EventAccountSetEditor loadUserEventSet(HashDigest eventAccountSetHash, CryptoSetting cryptoSetting,
											String keyPrefix, ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage,
											boolean readonly) {

		return new EventAccountSetEditor(eventAccountSetHash, cryptoSetting, keyPrefix + USER_EVENT_SET_PREFIX, ledgerExStorage,
				ledgerVerStorage, readonly, DEFAULT_ACCESS_POLICY);
	}

	static EventAccountSetEditorSimple loadUserEventSetSimple(HashDigest eventAccountSetHash, CryptoSetting cryptoSetting,
												  String keyPrefix, ExPolicyKVStorage ledgerExStorage, VersioningKVStorage ledgerVerStorage,
												  boolean readonly) {

		return new EventAccountSetEditorSimple(eventAccountSetHash, cryptoSetting, keyPrefix + USER_EVENT_SET_PREFIX, ledgerExStorage,
				ledgerVerStorage, readonly, DEFAULT_ACCESS_POLICY);
	}

	private static class NewBlockCommittingMonitor implements LedgerEditor {

		private LedgerEditor editor;

		private LedgerRepositoryImpl ledgerRepo;

		public NewBlockCommittingMonitor(LedgerEditor editor, LedgerRepositoryImpl ledgerRepo) {
			this.editor = editor;
			this.ledgerRepo = ledgerRepo;
		}

		@Override
		public HashDigest getLedgerHash() {
			return editor.getLedgerHash();
		}

		@Override
		public long getBlockHeight() {
			return editor.getBlockHeight();
		}

		@Override
		public LedgerBlock getCurrentBlock() {
			return editor.getCurrentBlock();
		}

		@Override
		public LedgerDataSetEditor getLedgerDataset() {
			return (LedgerDataSetEditor) editor.getLedgerDataset();
		}

		@Override
		public LedgerEventSetEditor getLedgerEventSet() {
			return (LedgerEventSetEditor) editor.getLedgerEventSet();
		}

		@Override
		public TransactionSetEditor getTransactionSet() {
			return (TransactionSetEditor) editor.getTransactionSet();
		}

		@Override
		public LedgerTransactionContext newTransaction(TransactionRequest txRequest) {
			return editor.newTransaction(txRequest);
		}

		@Override
		public LedgerBlock prepare() {
			return editor.prepare();
		}

		@Override
		public void commit() {
			try {
				editor.commit();
				LedgerBlock latestBlock = editor.getCurrentBlock();
				ledgerRepo.latestState = new LedgerState(latestBlock, editor.getLedgerDataset(),
						editor.getTransactionSet(), editor.getLedgerEventSet());
			} finally {
				ledgerRepo.nextBlockEditor = null;
			}
		}

		@Override
		public void cancel() {
			try {
				editor.cancel();
			} finally {
				ledgerRepo.nextBlockEditor = null;
			}
		}

	}

	/**
	 * 维护账本某个区块的数据状态的缓存结构；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class LedgerState {

		private final LedgerBlock block;

		private final TransactionSet transactionSet;

		private final LedgerDataSet ledgerDataset;

		private final LedgerEventSet ledgerEventSet;

		public LedgerState(LedgerBlock block, LedgerDataSet ledgerDataset, TransactionSet transactionSet, LedgerEventSet ledgerEventSet) {
			this.block = block;
			this.ledgerDataset = ledgerDataset;
			this.transactionSet = transactionSet;
			this.ledgerEventSet = ledgerEventSet;

		}

		public LedgerAdminDataSet getAdminDataset() {
			return ledgerDataset.getAdminDataset();
		}

		public LedgerDataSet getLedgerDataset() {
			return ledgerDataset;
		}

		public ContractAccountSet getContractAccountSet() {
			return ledgerDataset.getContractAccountSet();
		}

		public DataAccountSet getDataAccountSet() {
			return ledgerDataset.getDataAccountSet();
		}

		public UserAccountSet getUserAccountSet() {
			return ledgerDataset.getUserAccountSet();
		}

		public TransactionSet getTransactionSet() {
			return transactionSet;
		}

		public LedgerEventSet getLedgerEventSet() {
			return ledgerEventSet;
		}

	}

}

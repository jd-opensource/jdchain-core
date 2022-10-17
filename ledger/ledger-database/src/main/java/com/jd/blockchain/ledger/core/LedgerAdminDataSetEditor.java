package com.jd.blockchain.ledger.core;

import com.jd.blockchain.contract.jvm.JVMContractRuntimeConfig;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.cache.AdminCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.storage.service.ExPolicy;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;

import utils.Bytes;
import utils.Transactional;

public class LedgerAdminDataSetEditor implements Transactional, LedgerAdminDataSet, LedgerAdminSettings {

	static {
		DataContractRegistry.register(LedgerMetadata.class);
		DataContractRegistry.register(LedgerMetadata_V2.class);
	}

	private static Logger LOGGER = LoggerFactory.getLogger(LedgerAdminDataSetEditor.class);

	public static final String LEDGER_META_PREFIX = "MTA" + LedgerConsts.KEY_SEPERATOR;
	public static final String LEDGER_PARTICIPANT_PREFIX = "PAR" + LedgerConsts.KEY_SEPERATOR;
	public static final String LEDGER_SETTING_PREFIX = "SET" + LedgerConsts.KEY_SEPERATOR;
	public static final String ROLE_PRIVILEGE_PREFIX = "RPV" + LedgerConsts.KEY_SEPERATOR;
	public static final String USER_ROLE_PREFIX = "URO" + LedgerConsts.KEY_SEPERATOR;

	private final Bytes metaPrefix;
	private final Bytes settingPrefix;

	private LedgerMetadata_V2 origMetadata;

	private LedgerMetadataInfo metadata;

	private AdminCache cache;

	/**
	 * 原来的账本设置；
	 * 
	 * <br>
	 * 对 LedgerMetadata 修改的新配置不能立即生效，需要达成共识后，在下一次区块计算中才生效；
	 */
	private LedgerSettings previousSettings;

	private HashDigest previousSettingHash;

	/**
	 * 账本的参与节点；
	 */
	private ParticipantDataset participants;

	/**
	 * “角色-权限”数据集；
	 */
	private RolePrivilegeDataset rolePrivileges;

	/**
	 * “用户-角色”数据集；
	 */
	private UserRoleDatasetEditor userRoles;

	/**
	 * 账本参数配置；
	 */
	private LedgerSettings settings;

	private ExPolicyKVStorage storage;

	private HashDigest adminDataHash;

	private boolean readonly;

	private boolean updated;

	public HashDigest getHash() {
		return adminDataHash;
	}

	public boolean isReadonly() {
		return readonly;
	}

	void setReadonly() {
		this.readonly = true;
	}

	public LedgerSettings getPreviousSetting() {
		return previousSettings;
	}

	@Override
	public RolePrivilegeDataset getRolePrivileges() {
		return rolePrivileges;
	}

	@Override
	public UserRoleDatasetEditor getAuthorizations() {
		return userRoles;
	}

	@Override
	public LedgerAdminSettings getAdminSettings() {
		return this;
	}

	/**
	 * 初始化账本的管理账户；
	 * 
	 * <br>
	 * 
	 * 只在新建账本时调用此方法；
	 * 
	 * @param ledgerSeed
	 * @param settings
	 * @param partiList
	 * @param exPolicyStorage
	 * @param versioningStorage
	 */
	public LedgerAdminDataSetEditor(LedgerInitSetting initSetting, String keyPrefix, ExPolicyKVStorage exPolicyStorage,
			VersioningKVStorage versioningStorage, AdminCache cache) {
		this.metaPrefix = Bytes.fromString(keyPrefix + LEDGER_META_PREFIX);
		this.settingPrefix = Bytes.fromString(keyPrefix + LEDGER_SETTING_PREFIX);
		this.cache = cache;
		ParticipantNode[] parties = initSetting.getConsensusParticipants();
		if (parties.length == 0) {
			throw new LedgerException("No participant!");
		}

		// 初始化元数据；
		this.metadata = new LedgerMetadataInfo();
		this.metadata.setSeed(initSetting.getLedgerSeed());
		this.metadata.setIdentityMode(initSetting.getIdentityMode());
		this.metadata.setLedgerCertificates(initSetting.getLedgerCertificates());
		this.metadata.setLedgerStructureVersion(initSetting.getLedgerStructureVersion());
		this.metadata.setGenesisUsers(initSetting.getGenesisUsers());
		this.metadata.setContractRuntimeConfig(initSetting.getContractRuntimeConfig());
		// 新配置；
		this.settings = new LedgerConfiguration(initSetting.getConsensusProvider(), initSetting.getConsensusSettings(),
				initSetting.getCryptoSetting());
		this.previousSettings = new LedgerConfiguration(settings);
		this.previousSettingHash = null;
		this.adminDataHash = null;

		// 基于原配置初始化参与者列表；
		String partiPrefix = keyPrefix + LEDGER_PARTICIPANT_PREFIX;
		this.participants = new ParticipantDataset(previousSettings.getCryptoSetting(), partiPrefix, exPolicyStorage,
				versioningStorage, initSetting.getLedgerDataStructure(), cache);

		for (ParticipantNode p : parties) {
			this.participants.addConsensusParticipant(p);
		}

		String rolePrivilegePrefix = keyPrefix + ROLE_PRIVILEGE_PREFIX;
		this.rolePrivileges = new RolePrivilegeDataset(this.settings.getCryptoSetting(), rolePrivilegePrefix,
				exPolicyStorage, versioningStorage, initSetting.getLedgerDataStructure(), cache);

		String userRolePrefix = keyPrefix + USER_ROLE_PREFIX;
		this.userRoles = new UserRoleDatasetEditor(this.settings.getCryptoSetting(), userRolePrefix, exPolicyStorage,
				versioningStorage, initSetting.getLedgerDataStructure(), cache);

		// 初始化其它属性；
		this.storage = exPolicyStorage;
		this.readonly = false;
	}

	public LedgerAdminDataSetEditor(long preBlockHeight, HashDigest adminAccountHash, String keyPrefix, ExPolicyKVStorage kvStorage,
			VersioningKVStorage versioningKVStorage, LedgerDataStructure dataStructure, AdminCache cache, boolean readonly) {
		this.metaPrefix = Bytes.fromString(keyPrefix + LEDGER_META_PREFIX);
		this.settingPrefix = Bytes.fromString(keyPrefix + LEDGER_SETTING_PREFIX);
		this.storage = kvStorage;
		this.readonly = readonly;
		this.cache = cache;
		this.origMetadata = loadAndVerifyMetadata(adminAccountHash);
		this.metadata = new LedgerMetadataInfo(origMetadata);
		this.settings = loadAndVerifySettings(metadata.getSettingsHash());
		// 复制记录一份配置作为上一个区块的原始配置，该实例仅供读取，不做修改，也不会回写到存储；
		this.previousSettings = new LedgerConfiguration(settings);
		this.previousSettingHash = metadata.getSettingsHash();
		this.adminDataHash = adminAccountHash;

		String partiPrefix = keyPrefix + LEDGER_PARTICIPANT_PREFIX;
		this.participants = new ParticipantDataset(preBlockHeight, metadata.getParticipantsHash(), previousSettings.getCryptoSetting(),
				partiPrefix, kvStorage, versioningKVStorage, dataStructure, cache, readonly);

		String rolePrivilegePrefix = keyPrefix + ROLE_PRIVILEGE_PREFIX;
		this.rolePrivileges = new RolePrivilegeDataset(preBlockHeight, metadata.getRolePrivilegesHash(),
				previousSettings.getCryptoSetting(), rolePrivilegePrefix, kvStorage, versioningKVStorage, dataStructure, cache, readonly);

		String userRolePrefix = keyPrefix + USER_ROLE_PREFIX;
		this.userRoles = new UserRoleDatasetEditor(preBlockHeight, metadata.getUserRolesHash(), previousSettings.getCryptoSetting(),
				userRolePrefix, kvStorage, versioningKVStorage, dataStructure, cache, readonly);
	}

	private LedgerSettings loadAndVerifySettings(HashDigest settingsHash) {
		if (settingsHash == null) {
			return null;
		}
		Bytes key = encodeSettingsKey(settingsHash);
		LedgerSettings settings = cache.get(key, LedgerSettings.class);
		if (null == settings) {
			byte[] bytes = storage.get(key);
			HashFunction hashFunc = Crypto.getHashFunction(settingsHash.getAlgorithm());
			if (!hashFunc.verify(settingsHash, bytes)) {
				String errorMsg = "Verification of the hash for ledger setting failed! --[HASH=" + key + "]";
				LOGGER.error(errorMsg);
				throw new LedgerException(errorMsg);
			}
			settings = deserializeSettings(bytes);
			cache.set(key, settings);
		}

		return settings;
	}

	private LedgerSettings deserializeSettings(byte[] bytes) {
		return BinaryProtocol.decode(bytes);
	}

	private byte[] serializeSetting(LedgerSettings setting) {
		return BinaryProtocol.encode(setting, LedgerSettings.class);
	}

	private LedgerMetadata_V2 loadAndVerifyMetadata(HashDigest adminAccountHash) {
		Bytes key = encodeMetadataKey(adminAccountHash);
		LedgerMetadata_V2 metadata = cache.get(key, LedgerMetadata_V2.class);
		if(null == metadata) {
			byte[] bytes = storage.get(key);
			HashFunction hashFunc = Crypto.getHashFunction(adminAccountHash.getAlgorithm());
			if (!hashFunc.verify(adminAccountHash, bytes)) {
				String errorMsg = "Verification of the hash for ledger metadata failed! --[HASH=" + key + "]";
				LOGGER.error(errorMsg);
				throw new LedgerException(errorMsg);
			}
			metadata = deserializeMetadata(bytes);
			cache.set(key, metadata);
		}

		return metadata;
	}

	private Bytes encodeSettingsKey(HashDigest settingsHash) {
		return settingPrefix.concat(settingsHash);
	}

	private Bytes encodeMetadataKey(HashDigest metadataHash) {
		// return LEDGER_META_PREFIX + metadataHash;
		// return metaPrefix + metadataHash;
		return metaPrefix.concat(metadataHash);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jd.blockchain.ledger.core.LedgerAdministration#getMetadata()
	 */
	@Override
	public LedgerMetadata_V2 getMetadata() {
		return metadata;
	}

	/**
	 * 返回当前设置的账本配置；
	 * 
	 * @return
	 */

	@Override
	public LedgerSettings getSettings() {
		return settings;
	}

	/**
	 * 更新账本配置；
	 * 
	 * @param ledgerSetting
	 */
	public void setLedgerSetting(LedgerSettings ledgerSetting) {
		if (readonly) {
			throw new IllegalArgumentException("This merkle dataset is readonly!");
		}
		settings = ledgerSetting;
		updated = true;
	}

	@Override
	public long getParticipantCount() {
		return participants.getParticipantCount();
	}

	@Override
	public ParticipantNode[] getParticipants() {
		return participants.getParticipants();
	}

	@Override
	public ParticipantDataset getParticipantDataset() {
		return participants;
	}

	/**
	 * 加入新的参与方； 如果指定的参与方已经存在，则引发 LedgerException 异常；
	 * 
	 * @param participant
	 */
	public void addParticipant(ParticipantNode participant) {
		participants.addConsensusParticipant(participant);
	}

	/**
	 * 更新参与方的状态参数；
	 *
	 * @param participant
	 */
	public void updateParticipant(ParticipantNode participant) {
		participants.updateConsensusParticipant(participant);
	}

	@Override
	public boolean isUpdated() {
		return updated || participants.isUpdated() || rolePrivileges.isUpdated() || userRoles.isUpdated();
	}

	@Override
	public void commit() {
		if (!isUpdated()) {
			return;
		}
		// 计算并更新参与方集合的根哈希；
		participants.commit();
		metadata.setParticipantsHash(participants.getRootHash());

		// 计算并更新角色权限集合的根哈希；
		rolePrivileges.commit();
		metadata.setRolePrivilegesHash(rolePrivileges.getRootHash());

		// 计算并更新用户角色授权集合的根哈希；
		userRoles.commit();
		metadata.setUserRolesHash(userRoles.getRootHash());

		// 当前区块上下文的密码参数设置的哈希函数；
		HashFunction hashFunc = Crypto.getHashFunction(previousSettings.getCryptoSetting().getHashAlgorithm());

		// 计算并更新参数配置的哈希；
		if (settings == null) {
			throw new LedgerException("Missing ledger settings!");
		}
		byte[] settingsBytes = serializeSetting(settings);
		HashDigest settingsHash = hashFunc.hash(settingsBytes);
		metadata.setSettingsHash(settingsHash);
		if (previousSettingHash == null || !previousSettingHash.equals(settingsHash)) {
			Bytes settingsKey = encodeSettingsKey(settingsHash);
			if (!storage.exist(settingsKey)) {
				boolean nx = storage.set(settingsKey, settingsBytes, ExPolicy.NOT_EXISTING);
				if (!nx) {
					String base58MetadataHash = settingsHash.toBase58();
					// 有可能发生了并发写入冲突，不同的节点都向同一个存储服务器上写入数据；
					String errMsg = "Ledger metadata already exist! --[MetadataHash=" + base58MetadataHash + "]";
					LOGGER.warn(errMsg);
					throw new LedgerException(errMsg);
				}
			} else {
				//可能发生在共识切换的场景，共识又切换回以前的共识方式，那么key肯定是存在的，不需要重复设置到数据库
				LOGGER.info("Switch to old consensus, no need to set setting key repeatly!");
			}
		}

		// 基于之前的密码配置来计算元数据的哈希；
		byte[] metadataBytes = serializeMetadata(metadata);

		HashDigest metadataHash = hashFunc.hash(metadataBytes);
		if (adminDataHash == null || !adminDataHash.equals(metadataHash)) {
			// update modify;
			// String base58MetadataHash = metadataHash.toBase58();
			// String metadataKey = encodeMetadataKey(base58MetadataHash);
			Bytes metadataKey = encodeMetadataKey(metadataHash);

			if (!storage.exist(metadataKey)) {
				boolean nx = storage.set(metadataKey, metadataBytes, ExPolicy.NOT_EXISTING);
				if (!nx) {
					String base58MetadataHash = metadataHash.toBase58();
					// 有可能发生了并发写入冲突，不同的节点都向同一个存储服务器上写入数据；
					String errMsg = "Ledger metadata already exist! --[MetadataHash=" + base58MetadataHash + "]";
					LOGGER.warn(errMsg);
					throw new LedgerException(errMsg);
				}
			} else {
				//可能发生在共识切换的场景，共识又切换回以前的共识方式，那么key肯定是存在的，不需要重复设置到数据库
				LOGGER.info("Switch to old consensus, no need to set meta key repeatly!");
			}

			adminDataHash = metadataHash;
		}
		cache.clear();
		updated = false;
	}

	private LedgerMetadata_V2 deserializeMetadata(byte[] bytes) {
		return BinaryProtocol.decode(bytes);
	}

	private byte[] serializeMetadata(LedgerMetadataInfo config) {
		return BinaryProtocol.encode(config, LedgerMetadata_V2.class);
	}

	@Override
	public void cancel() {
		if (!isUpdated()) {
			return;
		}
		cache.clear();
		participants.cancel();
		metadata =origMetadata == null ? new LedgerMetadataInfo() :  new LedgerMetadataInfo(origMetadata);
	}

	public void updateLedgerCA(String[] certs) {
		metadata.setLedgerCertificates(certs);
		updated = true;
	}

	public static class LedgerMetadataInfo implements LedgerMetadata_V2 {

		private byte[] seed;

		private IdentityMode identityMode = IdentityMode.KEYPAIR;

		private String[] ledgerCertificates;

		private HashDigest participantsHash;

		private HashDigest settingsHash;

		private HashDigest rolePrivilegesHash;

		private HashDigest userRolesHash;

		private long ledgerStructureVersion = -1L;

		private GenesisUser[] genesisUsers;

		private ContractRuntimeConfig contractRuntimeConfig;

		public LedgerMetadataInfo() {
		}

		public LedgerMetadataInfo(LedgerMetadata_V2 metadata) {
			this.seed = metadata.getSeed();
			this.participantsHash = metadata.getParticipantsHash();
			this.settingsHash = metadata.getSettingsHash();
			this.rolePrivilegesHash = metadata.getRolePrivilegesHash();
			this.userRolesHash = metadata.getUserRolesHash();
			this.ledgerStructureVersion = metadata.getLedgerStructureVersion();
			if(null != metadata.getIdentityMode()) {
				this.identityMode = metadata.getIdentityMode();
			}
			this.ledgerCertificates = metadata.getLedgerCertificates();
			if(null != metadata.getGenesisUsers()) {
				GenesisUser[] users = metadata.getGenesisUsers();
				this.genesisUsers = new GenesisUserConfig[users.length];
				for (int i = 0; i < users.length; i++) {
					this.genesisUsers[i] = new GenesisUserConfig(users[i]);
				}
			}
			if(null != metadata.getContractRuntimeConfig()) {
				this.contractRuntimeConfig = new JVMContractRuntimeConfig(metadata.getContractRuntimeConfig());
			} else {
				this.contractRuntimeConfig = new JVMContractRuntimeConfig();
			}
		}

		@Override
		public byte[] getSeed() {
			return seed;
		}

		@Override
		public IdentityMode getIdentityMode() {
			return identityMode;
		}

		public void setIdentityMode(IdentityMode identityMode) {
			this.identityMode = identityMode;
		}

		@Override
		public String[] getLedgerCertificates() {
			return ledgerCertificates;
		}

		public void setGenesisUsers(GenesisUser[] genesisUsers) {
			this.genesisUsers = genesisUsers;
		}

		@Override
		public GenesisUser[] getGenesisUsers() {
			return genesisUsers;
		}

		@Override
		public ContractRuntimeConfig getContractRuntimeConfig() {
			return contractRuntimeConfig;
		}

		public void setLedgerCertificates(String[] ledgerCertificates) {
			this.ledgerCertificates = ledgerCertificates;
		}

		@Override
		public HashDigest getSettingsHash() {
			return settingsHash;
		}

		@Override
		public HashDigest getParticipantsHash() {
			return participantsHash;
		}

		@Override
		public HashDigest getRolePrivilegesHash() {
			return rolePrivilegesHash;
		}

		@Override
		public HashDigest getUserRolesHash() {
			return userRolesHash;
		}

		@Override
		public long getLedgerStructureVersion() {
			return ledgerStructureVersion;
		}

		public void setSeed(byte[] seed) {
			this.seed = seed;
		}

		public void setSettingsHash(HashDigest settingHash) {
			this.settingsHash = settingHash;
		}

		public void setParticipantsHash(HashDigest participantsHash) {
			this.participantsHash = participantsHash;
		}

		public void setRolePrivilegesHash(HashDigest rolePrivilegesHash) {
			this.rolePrivilegesHash = rolePrivilegesHash;
		}

		public void setUserRolesHash(HashDigest userRolesHash) {
			this.userRolesHash = userRolesHash;
		}

		public void setLedgerStructureVersion(long ledgerStructureVersion) {
			this.ledgerStructureVersion = ledgerStructureVersion;
		}

		public void setContractRuntimeConfig(ContractRuntimeConfig contractRuntimeConfig) {
			this.contractRuntimeConfig = contractRuntimeConfig;
		}
	}

}
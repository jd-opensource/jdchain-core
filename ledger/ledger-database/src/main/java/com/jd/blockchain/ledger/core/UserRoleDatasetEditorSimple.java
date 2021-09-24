package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.AuthorizationException;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.RoleSet;
import com.jd.blockchain.ledger.RolesPolicy;
import com.jd.blockchain.ledger.UserAuthorizationSettings;
import com.jd.blockchain.ledger.UserRoles;
import com.jd.blockchain.ledger.merkletree.KVEntry;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import com.jd.blockchain.storage.service.utils.VersioningKVData;
import utils.Bytes;
import utils.DataEntry;
import utils.Transactional;
import utils.io.BytesUtils;

import java.util.Collection;

/**
 * User-Role authorization data set;
 * 
 *
 *
 */
public class UserRoleDatasetEditorSimple implements Transactional, MerkleProvable<Bytes>, UserAuthorizationSettings {

	private SimpleDatasetImpl dataset;

	private volatile long userrole_index_in_block = 0;

	private volatile long origin_userrole_index_in_block  = 0;

	private static final Bytes USEERROLR_SEQUENCE_KEY_PREFIX = Bytes.fromString("SEQ" + LedgerConsts.KEY_SEPERATOR);

	public UserRoleDatasetEditorSimple(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exPolicyStorage,
                                       VersioningKVStorage verStorage) {
		dataset = new SimpleDatasetImpl(SimpleDatasetType.NONE, cryptoSetting, prefix, exPolicyStorage, verStorage);
	}

	public UserRoleDatasetEditorSimple(long preBlockHeight, HashDigest merkleRootHash, CryptoSetting cryptoSetting, String prefix,
                                       ExPolicyKVStorage exPolicyStorage, VersioningKVStorage verStorage, boolean readonly) {
		dataset = new SimpleDatasetImpl(preBlockHeight, merkleRootHash, SimpleDatasetType.NONE, cryptoSetting, prefix, exPolicyStorage, verStorage, readonly);
	}

	@Override
	public HashDigest getRootHash() {
		return dataset.getRootHash();
	}

	@Override
	public MerkleProof getProof(Bytes key) {
		return dataset.getProof(key);
	}

	@Override
	public boolean isUpdated() {
		return dataset.isUpdated();
	}

	@Override
	public void commit() {
		dataset.commit();
		origin_userrole_index_in_block = userrole_index_in_block;
	}

	@Override
	public void cancel() {
		dataset.cancel();
		userrole_index_in_block = origin_userrole_index_in_block;
	}

	@Override
	public long getUserCount() {
		return dataset.getDataCount() + userrole_index_in_block;
	}

	/**
	 * 加入新的用户角色授权； <br>
	 * 
	 * 如果该用户的授权已经存在，则引发 {@link LedgerException} 异常；
	 * 
	 * @param userAddress
	 * @param rolesPolicy
	 * @param roles
	 */
	public void addUserRoles(Bytes userAddress, RolesPolicy rolesPolicy, String... roles) {
		UserRoles roleAuth = new UserRoles(userAddress, -1, rolesPolicy);
		roleAuth.addRoles(roles);
		long nv = setUserRolesAuthorization(roleAuth);
		if (nv < 0) {
			throw new AuthorizationException("Roles authorization of User[" + userAddress + "] already exists!");
		}
	}

	/**
	 * 加入新的用户角色授权； <br>
	 * 
	 * 如果该用户的授权已经存在，则引发 {@link LedgerException} 异常；
	 * 
	 * @param userAddress
	 * @param rolesPolicy
	 * @param roles
	 */
	public void addUserRoles(Bytes userAddress, RolesPolicy rolesPolicy, Collection<String> roles) {
		UserRoles roleAuth = new UserRoles(userAddress, -1, rolesPolicy);
		roleAuth.addRoles(roles);
		long nv = setUserRolesAuthorization(roleAuth);
		if (nv < 0) {
			throw new AuthorizationException("Roles authorization of User[" + userAddress + "] already exists!");
		}

		nv = dataset.setValue(USEERROLR_SEQUENCE_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(dataset.getDataCount() + userrole_index_in_block))), userAddress.toBytes(), -1);

		if (nv < 0) {
			throw new AuthorizationException("Roles authorization seq of User[" + userAddress + "] already exists!");
		}

		userrole_index_in_block++;
	}

	/**
	 * 设置用户角色授权； <br>
	 * 如果版本校验不匹配，则返回 -1；
	 * 
	 * @param userRoles
	 * @return
	 */
	private long setUserRolesAuthorization(UserRoles userRoles) {
		if (userRoles.getRoleCount() > MAX_ROLES_PER_USER) {
			throw new AuthorizationException("The number of roles exceeds the maximum range!");
		}
		byte[] rolesetBytes = BinaryProtocol.encode(userRoles, RoleSet.class);
		return dataset.setValue(userRoles.getUserAddress(), rolesetBytes, userRoles.getVersion());
	}

	/**
	 * 更新用户角色授权； <br>
	 * 如果指定用户的授权不存在，或者版本不匹配，则引发 {@link LedgerException} 异常；
	 * 
	 * @param userRoles
	 */
	public void updateUserRoles(UserRoles userRoles) {
		long nv = setUserRolesAuthorization(userRoles);
		if (nv < 0) {
			throw new AuthorizationException("Update to roles of user[" + userRoles.getUserAddress()
					+ "] failed due to wrong version[" + userRoles.getVersion() + "] !");
		}
	}

	/**
	 * 设置用户的角色； <br>
	 * 如果用户的角色授权不存在，则创建新的授权；
	 * 
	 * @param userAddress 用户；
	 * @param policy      角色策略；
	 * @param roles       角色列表；
	 * @return
	 */
	public long setRoles(Bytes userAddress, RolesPolicy policy, String... roles) {
		UserRoles userRoles = getUserRoles(userAddress);
		if (userRoles == null) {
			userRoles = new UserRoles(userAddress, -1, policy);
		}
		userRoles.setPolicy(policy);
		userRoles.setRoles(roles);
		return setUserRolesAuthorization(userRoles);
	}

	/**
	 * 查询角色授权；
	 * 
	 * <br>
	 * 如果不存在，则返回 null；
	 * 
	 * @param address
	 * @return
	 */
	@Override
	public UserRoles getUserRoles(Bytes userAddress) {
		// 只返回最新版本；
		DataEntry<Bytes, byte[]> kv = dataset.getDataEntry(userAddress);
		if (kv == null) {
			return null;
		}
		RoleSet roleSet = BinaryProtocol.decode(kv.getValue());
		return new UserRoles(userAddress, kv.getVersion(), roleSet);
	}

	@Override
	public UserRoles[] getUserRoles() {
		DataEntry<Bytes, byte[]>[] kvEntries = dataset.getDataEntries(0, (int) dataset.getDataCount());
		UserRoles[] pns = new UserRoles[kvEntries.length];
		RoleSet roleset;
		for (int i = 0; i < pns.length; i++) {
			roleset = BinaryProtocol.decode(kvEntries[i].getValue());
			pns[i] = new UserRoles(kvEntries[i].getKey(), kvEntries[i].getVersion(), roleset);
		}
		return pns;
	}

	@Override
	public boolean isReadonly() {
		return dataset.isReadonly();
	}


	public boolean isAddNew() {
		return userrole_index_in_block != 0;
	}

	public void clearCachedIndex() {
		userrole_index_in_block = 0;
	}
}

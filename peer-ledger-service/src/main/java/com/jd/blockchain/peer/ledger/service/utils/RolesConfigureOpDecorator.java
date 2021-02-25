package com.jd.blockchain.peer.ledger.service.utils;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.ledger.*;

import java.util.*;

/**
 * 角色配置操作包装类
 *         用于将${@link RolesConfigureOperation} 对象转换为具体类对象，供JSON序列化
 *
 * @author shaozhuguang
 *
 */
public class RolesConfigureOpDecorator implements RolesConfigureOperation {

	static {
		DataContractRegistry.register(UserRegisterOperation.class);
		DataContractRegistry.register(RolesConfigureOperation.class);
		DataContractRegistry.register(RolePrivilegeEntry.class);
	}

	private Map<String, RolePrivilegeConfig> rolesMap = Collections
			.synchronizedMap(new LinkedHashMap<String, RolePrivilegeConfig>());

	public RolesConfigureOpDecorator() {
	}

	public boolean isEmpty() {
		return rolesMap.isEmpty();
	}

	@Override
	public RolePrivilegeEntry[] getRoles() {
		return rolesMap.values().toArray(new RolePrivilegeEntry[rolesMap.size()]);
	}

	public void configure(RolePrivilegeEntry entry) {
		String roleName = SecurityUtils.formatRoleName(entry.getRoleName());
		RolePrivilegeConfig roleConfig = rolesMap.get(roleName);
		if (roleConfig == null) {
			roleConfig = new RolePrivilegeConfig(roleName);
			roleConfig.setEnableLedgerPermissions(entry.getEnableLedgerPermissions());
			roleConfig.setEnableTxPermissions(entry.getEnableTransactionPermissions());
			roleConfig.setDisableLedgerPermissions(entry.getDisableLedgerPermissions());
			roleConfig.setDisableTxPermissions(entry.getDisableTransactionPermissions());
			rolesMap.put(roleName, roleConfig);
		}
	}

	private class RolePrivilegeConfig implements RolePrivilegeEntry {

		private String roleName;

		private LedgerPermission[] enableLedgerPermissions = null;
		private LedgerPermission[] disableLedgerPermissions = null;

		private TransactionPermission[] enableTxPermissions = null;
		private TransactionPermission[] disableTxPermissions = null;

		private RolePrivilegeConfig(String roleName) {
			this.roleName = roleName;
		}

		@Override
		public String getRoleName() {
			return roleName;
		}

		@Override
		public LedgerPermission[] getEnableLedgerPermissions() {
			return enableLedgerPermissions;
		}

		@Override
		public LedgerPermission[] getDisableLedgerPermissions() {
			return disableLedgerPermissions;
		}

		@Override
		public TransactionPermission[] getEnableTransactionPermissions() {
			return enableTxPermissions;
		}

		@Override
		public TransactionPermission[] getDisableTransactionPermissions() {
			return disableTxPermissions;
		}

		public void setRoleName(String roleName) {
			this.roleName = roleName;
		}

		public void setEnableLedgerPermissions(LedgerPermission[] enableLedgerPermissions) {
			this.enableLedgerPermissions = enableLedgerPermissions;
		}

		public void setDisableLedgerPermissions(LedgerPermission[] disableLedgerPermissions) {
			this.disableLedgerPermissions = disableLedgerPermissions;
		}

		public void setEnableTxPermissions(TransactionPermission[] enableTxPermissions) {
			this.enableTxPermissions = enableTxPermissions;
		}

		public void setDisableTxPermissions(TransactionPermission[] disableTxPermissions) {
			this.disableTxPermissions = disableTxPermissions;
		}
	}
}

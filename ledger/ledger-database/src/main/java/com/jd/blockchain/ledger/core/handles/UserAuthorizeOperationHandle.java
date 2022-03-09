package com.jd.blockchain.ledger.core.handles;

import java.util.Arrays;

import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.RoleDoesNotExistException;
import com.jd.blockchain.ledger.RolePrivilegeSettings;
import com.jd.blockchain.ledger.RolesPolicy;
import com.jd.blockchain.ledger.UserAuthorizationSettings;
import com.jd.blockchain.ledger.UserAuthorizeOperation;
import com.jd.blockchain.ledger.UserAuthorizeOperation.UserRolesEntry;
import com.jd.blockchain.ledger.UserRoles;
import com.jd.blockchain.ledger.core.EventManager;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.MultiIDsPolicy;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.ledger.SecurityContext;
import com.jd.blockchain.ledger.SecurityPolicy;
import com.jd.blockchain.ledger.core.TransactionRequestExtension;
import com.jd.blockchain.ledger.core.UserRoleDatasetEditor;

import com.jd.blockchain.ledger.core.UserRoleDatasetEditorSimple;
import utils.Bytes;

public class UserAuthorizeOperationHandle extends AbstractLedgerOperationHandle<UserAuthorizeOperation> {

	public UserAuthorizeOperationHandle() {
		super(UserAuthorizeOperation.class);
	}

	@Override
	protected void doProcess(UserAuthorizeOperation operation, LedgerTransactionContext transactionContext,
							 TransactionRequestExtension request, LedgerQuery ledger,
							 OperationHandleContext handleContext, EventManager manager) {
		// 权限校验；
		SecurityPolicy securityPolicy = SecurityContext.getContextUsersPolicy();
		securityPolicy.checkEndpointPermission(LedgerPermission.CONFIGURE_ROLES, MultiIDsPolicy.AT_LEAST_ONE);

		// 操作账本；
		UserRolesEntry[] urcfgs = operation.getUserRolesAuthorizations();

		UserAuthorizationSettings userRoleDataset = transactionContext.getDataset().getAdminDataset().getAdminSettings().getAuthorizations();
		RolePrivilegeSettings rolesSettings = transactionContext.getDataset().getAdminDataset().getAdminSettings().getRolePrivileges();
		if (urcfgs != null) {
			for (UserRolesEntry urcfg : urcfgs) {
				//
				String[] authRoles = urcfg.getAuthorizedRoles();
				Arrays.stream(authRoles).forEach(role -> {
					if(!rolesSettings.contains(role)) {
						throw new RoleDoesNotExistException(String.format("Role doesn't exist! --[Role=%s]", role));
					}
				});
				for (Bytes address : urcfg.getUserAddresses()) {
					UserRoles ur = userRoleDataset.getUserRoles(address);
					if (ur == null) {
						// 这是新的授权；
						RolesPolicy policy = urcfg.getPolicy();
						if (policy == null) {
							policy = RolesPolicy.UNION;
						}
						if (ledger.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
							((UserRoleDatasetEditor)userRoleDataset).addUserRoles(address, policy, authRoles);
						} else {
							((UserRoleDatasetEditorSimple)userRoleDataset).addUserRoles(address, policy, authRoles);
						}
					} else {
						// 更改之前的授权；
						ur.addRoles(authRoles);
						ur.removeRoles(urcfg.getUnauthorizedRoles());

						// 如果请求中设置了策略，才进行更新；
						RolesPolicy policy = urcfg.getPolicy();
						if (policy != null) {
							ur.setPolicy(policy);
						}
						if (ledger.getLedgerDataStructure().equals(LedgerDataStructure.MERKLE_TREE)) {
							((UserRoleDatasetEditor)userRoleDataset).updateUserRoles(ur);
						} else {
							((UserRoleDatasetEditorSimple)userRoleDataset).updateUserRoles(ur);
						}
					}
				}
			}
		}
	}

}

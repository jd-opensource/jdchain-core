package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.LedgerPrivilegeBitset;
import com.jd.blockchain.ledger.PrivilegeBitset;
import com.jd.blockchain.ledger.RolePrivileges;
import com.jd.blockchain.ledger.RolesPolicy;
import com.jd.blockchain.ledger.TransactionPrivilegeBitset;
import com.jd.blockchain.ledger.UserPrivilegeSet;
import com.jd.blockchain.utils.Bytes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link UserRolesPrivileges} 表示多角色用户的综合权限；
 *
 * @author huanghaiquan
 *
 */
public class UserRolesPrivileges implements UserPrivilegeSet {

	private Bytes userAddress;

	private Set<String> userRoles;

	private LedgerPrivilegeBitset ledgerPrivilegesBitset;

	private TransactionPrivilegeBitset transactionPrivilegesBitset;

	public UserRolesPrivileges(Bytes userAddress, RolesPolicy policy, Collection<RolePrivileges> privilegesList) {
		this.userAddress = userAddress;
		LedgerPrivilegeBitset[] ledgerPrivileges = privilegesList.stream().map(p -> p.getLedgerPrivilege())
				.toArray(LedgerPrivilegeBitset[]::new);
		TransactionPrivilegeBitset[] transactionPrivilegeBitsets = privilegesList.stream().map(p -> p.getTransactionPrivilege())
				.toArray(TransactionPrivilegeBitset[]::new);

		this.ledgerPrivilegesBitset = ledgerPrivileges[0].clone();
		this.transactionPrivilegesBitset = transactionPrivilegeBitsets[0].clone();
		if(userRoles == null){
			userRoles = new HashSet<>();
			for(RolePrivileges rolePrivileges : privilegesList){
				userRoles.add(rolePrivileges.getRoleName());
			}
		}

		if (policy == RolesPolicy.UNION) {
			this.ledgerPrivilegesBitset.union(ledgerPrivileges, 1, ledgerPrivileges.length - 1);
			this.transactionPrivilegesBitset.union(transactionPrivilegeBitsets, 1, transactionPrivilegeBitsets.length - 1);

		} else if (policy == RolesPolicy.INTERSECT) {
			this.ledgerPrivilegesBitset.intersect(ledgerPrivileges, 1, ledgerPrivileges.length - 1);
			this.transactionPrivilegesBitset.intersect(transactionPrivilegeBitsets, 1, transactionPrivilegeBitsets.length - 1);
		} else {
			throw new IllegalStateException("Unsupported roles policy[" + policy.toString() + "]!");
		}

	}

	public Bytes getUserAddress() {
		return userAddress;
	}

	public LedgerPrivilegeBitset getLedgerPrivilegesBitset() {
		return ledgerPrivilegesBitset;
	}

	public PrivilegeBitset getTransactionPrivilegesBitset() {
		return transactionPrivilegesBitset;
	}

	public Set<String> getUserRole() {
		return this.userRoles;
	}
}

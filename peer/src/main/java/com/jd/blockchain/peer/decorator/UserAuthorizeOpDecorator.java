package com.jd.blockchain.peer.decorator;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.transaction.UserAuthorizer;
import com.jd.blockchain.transaction.UserRolesAuthorizer;
import com.jd.blockchain.utils.ArrayUtils;
import com.jd.blockchain.utils.Bytes;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户权限操作包装类
 *         用于将${@link UserAuthorizeOperation} 对象转换为具体类对象，供JSON序列化
 *
 * @author shaozhuguang
 *
 */
public class UserAuthorizeOpDecorator implements UserAuthorizeOperation {

	static {
		DataContractRegistry.register(UserRegisterOperation.class);
		DataContractRegistry.register(UserAuthorizeOperation.class);
		DataContractRegistry.register(UserRolesEntry.class);
	}

	private Set<AuthorizationDataEntry> userAuthMap = Collections
			.synchronizedSet(new LinkedHashSet<AuthorizationDataEntry>());

	public UserAuthorizeOpDecorator() {
	}

	@Override
	public AuthorizationDataEntry[] getUserRolesAuthorizations() {
		return ArrayUtils.toArray(userAuthMap, AuthorizationDataEntry.class);
	}

	public void configure(UserRolesEntry entry) {
		AuthorizationDataEntry userRolesAuth = new AuthorizationDataEntry(entry.getUserAddresses());
		userRolesAuth.setPolicy(entry.getPolicy());
		userRolesAuth.setAuthRoles(entry.getAuthorizedRoles());
		userRolesAuth.setUnauthRoles(entry.getUnauthorizedRoles());
		userAuthMap.add(userRolesAuth);
	}

	private class AuthorizationDataEntry implements UserRolesEntry {

		private Bytes[] userAddress;

		private RolesPolicy policy = RolesPolicy.UNION;

		private String[] authRoles = null;
		private String[] unauthRoles = null;

		private AuthorizationDataEntry(Bytes[] userAddress) {
			this.userAddress = userAddress;
		}

		@Override
		public Bytes[] getUserAddresses() {
			return userAddress;
		}

		@Override
		public RolesPolicy getPolicy() {
			return policy;
		}

		@Override
		public String[] getAuthorizedRoles() {
			return authRoles;
		}

		@Override
		public String[] getUnauthorizedRoles() {
			return unauthRoles;
		}

		public void setUserAddress(Bytes[] userAddress) {
			this.userAddress = userAddress;
		}

		public void setPolicy(RolesPolicy policy) {
			this.policy = policy;
		}

		public void setAuthRoles(String[] authRoles) {
			this.authRoles = authRoles;
		}

		public void setUnauthRoles(String[] unauthRoles) {
			this.unauthRoles = unauthRoles;
		}
	}
}

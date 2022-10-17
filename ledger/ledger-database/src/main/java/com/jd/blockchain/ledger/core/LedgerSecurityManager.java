package com.jd.blockchain.ledger.core;

import java.security.cert.X509Certificate;
import java.util.Set;

import com.jd.blockchain.ledger.SecurityPolicy;
import utils.Bytes;

/**
 * 账本的安全管理器；
 * 
 * @author huanghaiquan
 *
 */
public interface LedgerSecurityManager {

	String DEFAULT_ROLE = "DEFAULT";

	/**
	 * 返回与指定的终端用户和节点参与方相关的安全策略；
	 * 
	 * @param endpoints 终端用户的地址列表；
	 * @param nodes     节点参与方的地址列表；
	 * @return 一项安全策略；
	 */
	SecurityPolicy getSecurityPolicy(Set<Bytes> endpoints, Set<Bytes> nodes);

	/**
	 * 返回指定用户的角色权限；
	 * 
	 * @param userAddress
	 * @return
	 */
	UserRolesPrivileges getUserRolesPrivilegs(Bytes userAddress);

}
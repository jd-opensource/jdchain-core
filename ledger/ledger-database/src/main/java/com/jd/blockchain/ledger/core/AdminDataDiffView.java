package com.jd.blockchain.ledger.core;

import utils.SkippingIterator;

/**
 * 账本管理数据的差异;
 * 
 * @author huanghaiquan
 *
 */
public interface AdminDataDiffView extends DiffView {

	/**
	 * 配置的差异；
	 * 
	 * @return
	 */
	AdminSettingDiffView getSettingDiff();

	/**
	 * 参与方的差异；
	 * 
	 * @return
	 */
	SkippingIterator<ParticipantDiffView> getParticipantDiff();

	/**
	 * “用户-角色” 配置的差异；
	 * 
	 * @return
	 */
	SkippingIterator<UserRolesDiffView> getUserRolesDiff();

	/**
	 * “角色-权限”授权的差异；
	 * 
	 * @return
	 */
	SkippingIterator<RolePrivilegesDiffView> getRolePrivilegesDiff();

}

package com.jd.blockchain.ledger.cache;

import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.RolePrivileges;
import com.jd.blockchain.ledger.UserRoles;
import utils.Bytes;

/** 管理配置相关数据 */
public interface AdminCache extends Clearable {

  ParticipantNode getParticipant(Bytes address);

  void setParticipant(Bytes address, ParticipantNode node);

  RolePrivileges getRolePrivileges(String role);

  void setRolePrivileges(String role, RolePrivileges rolePrivileges);

  UserRoles getUserRoles(Bytes address);

  void setUserRoles(Bytes address, UserRoles userRoles);

  <T> T get(Bytes key, Class<T> dataType);

  void set(Bytes key, Object obj);
}

package com.jd.blockchain.ledger.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.RolePrivileges;
import com.jd.blockchain.ledger.UserRoles;
import utils.Bytes;

public class AdminLRUCache implements AdminCache {

  private final Cache<Bytes, ParticipantNode> participantCache;
  private final Cache<String, RolePrivileges> rolePrivilegesCache;
  private final Cache<Bytes, UserRoles> userRolesCache;
  private final Cache<Bytes, Object> commonCache;

  public AdminLRUCache() {
    this.participantCache =
        CacheBuilder.newBuilder().initialCapacity(4).maximumSize(10).concurrencyLevel(1).build();
    this.rolePrivilegesCache =
        CacheBuilder.newBuilder().initialCapacity(1).maximumSize(20).concurrencyLevel(1).build();
    this.userRolesCache =
        CacheBuilder.newBuilder().initialCapacity(4).maximumSize(20).concurrencyLevel(1).build();
    this.commonCache =
        CacheBuilder.newBuilder().initialCapacity(2).maximumSize(10).concurrencyLevel(1).build();
  }

  @Override
  public void clear() {
    participantCache.invalidateAll();
    rolePrivilegesCache.invalidateAll();
    userRolesCache.invalidateAll();
    commonCache.invalidateAll();
  }

  @Override
  public ParticipantNode getParticipant(Bytes address) {
    return participantCache.getIfPresent(address);
  }

  @Override
  public void setParticipant(Bytes address, ParticipantNode node) {
    participantCache.put(address, node);
  }

  @Override
  public RolePrivileges getRolePrivileges(String role) {
    return rolePrivilegesCache.getIfPresent(role);
  }

  @Override
  public void setRolePrivileges(String role, RolePrivileges rolePrivileges) {
    rolePrivilegesCache.put(role, rolePrivileges);
  }

  @Override
  public UserRoles getUserRoles(Bytes address) {
    return userRolesCache.getIfPresent(address);
  }

  @Override
  public void setUserRoles(Bytes address, UserRoles userRoles) {
    userRolesCache.put(address, userRoles);
  }

  @Override
  public <T> T get(Bytes key, Class<T> dataType) {
    Object obj = commonCache.getIfPresent(key);
    if (null != obj) {
      return (T) obj;
    }

    return null;
  }

  @Override
  public void set(Bytes key, Object obj) {
    commonCache.put(key, obj);
  }
}

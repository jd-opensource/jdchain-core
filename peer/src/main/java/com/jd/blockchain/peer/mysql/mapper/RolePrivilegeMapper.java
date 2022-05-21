package com.jd.blockchain.peer.mysql.mapper;

import com.jd.blockchain.peer.mysql.entity.RolePrivilegeInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/13 11:31 AM
 * Version 1.0
 */
@Mapper
public interface RolePrivilegeMapper {

    @Insert("INSERT INTO jdchain_roles_privileges (`ledger`, `role`, `ledger_privileges`, `tx_privileges`, `block_height`, `tx_hash`) " +
            "VALUES (#{ledger}, #{role}, #{ledger_privileges}, #{tx_privileges}, #{block_height}, #{tx_hash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RolePrivilegeInfo info);

    @Select("SELECT * from jdchain_roles_privileges where `ledger` = #{ledger} and `role` = #{role}")
    RolePrivilegeInfo getRolePrivInfo(String ledger, String role);

    @Update("UPDATE jdchain_roles_privileges SET `ledger_privileges` = #{ledger_privileges}, `tx_privileges` = #{tx_privileges} where `ledger` = #{ledger} and `role` = #{role}")
    void updateRolePrivInfo(String ledger, String role, String ledger_privileges, String tx_privileges);

}

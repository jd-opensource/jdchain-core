package com.jd.blockchain.peer.mysql.mapper;

import com.jd.blockchain.peer.mysql.entity.UserInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 2:36 PM
 * Version 1.0
 */
@Mapper
public interface UserInfoMapper {
    @Insert("INSERT INTO jdchain_users (`ledger`, `user_address`, `user_pubkey`, `user_key_algorithm`, `user_certificate`, `user_state`, `user_roles`, `user_roles_policy`, `user_block_height`, `user_tx_hash`) " +
            "VALUES (#{ledger}, #{user_address}, #{user_pubkey}, #{user_key_algorithm}, #{user_certificate}, #{user_state}, #{user_roles}, #{user_roles_policy}, #{user_block_height}, #{user_tx_hash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserInfo info);

    @Update("UPDATE jdchain_users SET `user_state` = #{user_state} where `ledger` = #{ledger} and `user_address` = #{user_address}")
    void updateStatus(String ledger, String user_address, String user_state);

    @Select("SELECT * from jdchain_users where `ledger` = #{ledger} and `user_address` = #{user_address}")
    UserInfo getUserInfoByAddr(String ledger, String user_address);

    @Update("UPDATE jdchain_users SET `user_roles` = #{user_roles}, `user_roles_policy` = #{user_roles_policy} where `ledger` = #{ledger} and `user_address` = #{user_address}")
    void updateRolePolicy(String ledger, String user_address, String user_roles, String user_roles_policy);

}

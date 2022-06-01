package com.jd.blockchain.peer.mysql.mapper;

import com.jd.blockchain.peer.mysql.entity.DataInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 2:42 PM
 * Version 1.0
 */
@Mapper
public interface DataInfoMapper {
    @Insert("INSERT INTO jdchain_data_accounts (`ledger`, `data_account_address`, `data_account_pubkey`, `data_account_roles`, `data_account_privileges`, `data_account_creator`, `data_account_block_height`, `data_account_tx_hash`) " +
            "VALUES (#{ledger}, #{data_account_address}, #{data_account_pubkey}, #{data_account_roles}, #{data_account_privileges}, #{data_account_creator}, #{data_account_block_height}, #{data_account_tx_hash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(DataInfo info);

    @Update("UPDATE jdchain_data_accounts SET `data_account_roles` = #{data_account_roles}, `data_account_privileges` = #{data_account_privileges} where `ledger` = #{ledger} and `data_account_address` = #{data_account_address}")
    void updateDataPermission(String ledger, String data_account_address, String data_account_roles, int data_account_privileges);

}

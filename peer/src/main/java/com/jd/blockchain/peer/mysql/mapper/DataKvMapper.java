package com.jd.blockchain.peer.mysql.mapper;

import com.jd.blockchain.peer.mysql.entity.DataKv;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 2:55 PM
 * Version 1.0
 */
@Mapper
public interface DataKvMapper {
    @Insert("INSERT INTO jdchain_contracts (`ledger`, `data_account_address`, `data_account_key`, `data_account_value`, `data_account_type`, `data_account_version`, " +
            "`data_block_height`, `data_tx_hash`)" +
            "VALUES (#{ledger}, #{data_account_address}, #{data_account_key}, #{data_account_value}, #{data_account_type}, #{data_account_version}, " +
            "#{data_block_height}, #{data_tx_hash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(DataKv info);

//    void updateStatus(int id, int state);
//
//    DataKv getDataKvById(int id);

//    DataKv getDataKvByHeight(long height);
//
//    DataKv getDataKvByHash(String hash);

//    List<DataKv> getDataKvsByLimit(int limit);
//
//    List<DataKv> getAllDataKvs();
}

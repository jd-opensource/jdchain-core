package com.jd.blockchain.peer.mysql.mapper;

import com.jd.blockchain.peer.mysql.entity.BlockInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 3:05 PM
 * Version 1.0
 */
@Mapper
public interface BlockInfoMapper {

    @Insert("INSERT INTO jdchain_blocks (`ledger`, `block_height`, `block_hash`, `pre_block_hash`, `txs_set_hash`, `users_set_hash`, `contracts_set_hash`, " +
            "`configurations_set_hash`, `dataaccounts_set_hash`, `eventaccounts_set_hash`, `block_timestamp`) " +
            "VALUES (#{ledger}, #{block_height}, #{block_hash}, #{pre_block_hash}, #{txs_set_hash}, #{users_set_hash}, #{contracts_set_hash}," +
            " #{configurations_set_hash}, #{dataaccounts_set_hash}, #{eventaccounts_set_hash}, #{block_timestamp})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(BlockInfo info);

    @Select("SELECT COUNT(*) FROM jdchain_blocks where `ledger` = #{ledger}")
    long getBlockInfoTotal(String ledger);


//    void updateStatus(int id, int state);

//    BlockInfo getBlockInfoById(int id);

//    BlockInfo getBlockInfoByHeight(long height);

//    BlockInfo getBlockInfoByHash(String hash);

//    List<BlockInfo> getBlockInfosByLimit(int limit);

//    List<BlockInfo> getAllBlockInfos();
}

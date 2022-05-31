package com.jd.blockchain.peer.mysql.mapper;

import com.jd.blockchain.peer.mysql.entity.TxInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 2:43 PM
 * Version 1.0
 */
@Mapper
public interface TxInfoMapper {
    @Insert("INSERT INTO jdchain_txs (`ledger`, `tx_block_height`, `tx_index`, `tx_hash`, `tx_node_pubkeys`, `tx_endpoint_pubkeys`, `tx_contents`, " +
            "`tx_response_state`) VALUES (#{ledger}, #{tx_block_height}, #{tx_index}, #{tx_hash}, #{tx_node_pubkeys}, " +
            "#{tx_endpoint_pubkeys}, #{tx_contents}, #{tx_response_state})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TxInfo info);

}

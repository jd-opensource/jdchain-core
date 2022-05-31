package com.jd.blockchain.peer.mysql.mapper;

import com.jd.blockchain.peer.mysql.entity.EventKv;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 3:04 PM
 * Version 1.0
 */
@Mapper
public interface EventKvMapper {
    @Insert("INSERT INTO jdchain_event_account_events (`ledger`, `event_account_address`, `event_name`, `event_sequence`, `event_tx_hash`, `event_block_height`, " +
            "`event_type`, `event_value`, `event_contract_address`) " +
            "VALUES (#{ledger}, #{event_account_address}, #{event_name}, #{event_sequence}, #{event_tx_hash}, " +
            "#{event_block_height}, #{event_type}, #{event_value}, #{event_contract_address})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(EventKv info);
}

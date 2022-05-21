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
    @Insert("INSERT INTO jdchain_contracts (`ledger`, `event_account_address`, `event_name`, `event_sequence`, `event_tx_hash`, `event_block_height`, " +
            "`event_type`, `event_value`, `event_contract_address`) " +
            "VALUES (#{ledger}, #{event_account_address}, #{event_name}, #{event_sequence}, #{event_tx_hash}, " +
            "#{event_block_height}, #{event_type}, #{event_value}, #{event_contract_address})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(EventKv info);

//    void updateStatus(int id, int state);
//
//    EventKv getEventKvById(int id);

//    EventKv getEventKvByHeight(long height);
//
//    EventKv getEventKvByHash(String hash);

//    List<EventKv> getEventKvsByLimit(int limit);
//
//    List<EventKv> getAllEventKvs();
}

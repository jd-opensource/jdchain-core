package com.jd.blockchain.peer.mysql.mapper;

import com.jd.blockchain.peer.mysql.entity.EventInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 2:51 PM
 * Version 1.0
 */
@Mapper
public interface EventInfoMapper {
    @Insert("INSERT INTO jdchain_contracts (`ledger`, `event_account_address`, `event_account_pubkey`, `event_account_roles`, `event_account_rpriviledges`, `event_account_creator`, `event_account_block_height`, `event_account_tx_hash`) " +
            "VALUES (#{ledger}, #{event_account_address}, #{event_account_pubkey}, #{event_account_roles}, #{event_account_rpriviledges}, #{event_account_creator}, #{event_account_block_height}, #{event_account_tx_hash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(EventInfo info);

//    void updateStatus(int id, int state);
//
//    EventInfo getEventInfoById(int id);

//    EventInfo getEventInfoByHeight(long height);
//
//    EventInfo getEventInfoByHash(String hash);

//    List<EventInfo> getEventInfosByLimit(int limit);
//
//    List<EventInfo> getAllEventInfos();
}

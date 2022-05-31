package com.jd.blockchain.peer.mysql.thread;

import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.peer.mysql.service.MapperService;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/27 3:08 PM
 * Version 1.0
 */
public interface ScheduleWriteMysql {

    void init(LedgerQuery ledgerQuery, MapperService mapperService);

    void destroy();

}

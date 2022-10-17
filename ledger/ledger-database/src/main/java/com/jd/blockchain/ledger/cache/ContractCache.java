package com.jd.blockchain.ledger.cache;

import com.jd.blockchain.contract.engine.ContractCode;
import utils.Bytes;

/**
 * 合约相关缓存
 */
public interface ContractCache extends PubkeyCache, StateCache, PermissionCache, Clearable {

    ContractCode getContractCode(Bytes address, long version);

    void setContractCode(Bytes address, ContractCode contractCode);
}

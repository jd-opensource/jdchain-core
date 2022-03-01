package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.engine.ContractCode;
import utils.Bytes;

/**
 * 合约加载器
 */
public interface ContractCodeLoader {

    ContractCode loadContract(Bytes address, long version, byte[] codeBytes);

}

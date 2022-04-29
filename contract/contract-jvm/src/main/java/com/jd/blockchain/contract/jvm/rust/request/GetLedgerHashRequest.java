package com.jd.blockchain.contract.jvm.rust.request;

import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 获取账本哈希
 */
public class GetLedgerHashRequest extends Request {

    public GetLedgerHashRequest() {
        super(RequestType.GET_LEDGER_HASH.getCode());
    }
}

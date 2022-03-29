package com.jd.blockchain.contract.jvm.rust.request;

import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 获取交易哈希
 */
public class GetTxHashRequest extends Request {

    public GetTxHashRequest() {
        super(RequestType.GET_TX_HASH.getCode());
    }
}

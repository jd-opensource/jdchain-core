package com.jd.blockchain.contract.jvm.rust.request;

import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 获取交易时间
 */
public class GetTxTimeRequest extends Request {

    public GetTxTimeRequest() {
        super(RequestType.GET_TX_TIME.getCode());
    }
}

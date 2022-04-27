package com.jd.blockchain.contract.jvm.rust.request;

import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 合约前置操作
 */
public class BeforeEventRequest extends Request {

    public BeforeEventRequest() {
        super(RequestType.BEFORE_EVENT.getCode());
    }
}

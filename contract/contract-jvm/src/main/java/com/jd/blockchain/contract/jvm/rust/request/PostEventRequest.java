package com.jd.blockchain.contract.jvm.rust.request;

import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 合约后置操作
 */
public class PostEventRequest extends Request {

    public PostEventRequest() {
        super(RequestType.POST_EVENT.getCode());
    }
}

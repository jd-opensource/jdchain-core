package com.jd.blockchain.contract.jvm.rust.request;

import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 获取交易签名用户列表
 */
public class GetSignersRequest extends Request {

    public GetSignersRequest() {
        super(RequestType.GET_SIGNERS.getCode());
    }
}

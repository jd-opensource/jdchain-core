package com.jd.blockchain.contract.jvm.rust.request;

import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 获取合约地址
 */
public class GetContractAddressRequest extends Request {

    public GetContractAddressRequest() {
        super(RequestType.GET_CONTRACT_ADDRESS.getCode());
    }
}

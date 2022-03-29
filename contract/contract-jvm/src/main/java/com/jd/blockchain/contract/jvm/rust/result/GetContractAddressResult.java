package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * 获取合约地址
 */
public class GetContractAddressResult extends Result {

    @JSONField(name = "ca")
    private String contractAddress;

    public GetContractAddressResult(String contractAddress) {
        super(SUCCESS);
        this.contractAddress = contractAddress;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }
}

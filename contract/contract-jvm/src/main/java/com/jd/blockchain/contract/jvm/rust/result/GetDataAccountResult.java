package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * 获取数据账户返回信息
 */
public class GetDataAccountResult extends Result {

    @JSONField(name = "a")
    private String address;
    @JSONField(name = "pk")
    private String pubkey;

    public GetDataAccountResult(String address, String pubkey) {
        super(SUCCESS);
        this.address = address;
        this.pubkey = pubkey;
    }

    public GetDataAccountResult(String address) {
        this(address, null);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPubkey() {
        return pubkey;
    }

    public void setPubkey(String pubkey) {
        this.pubkey = pubkey;
    }
}

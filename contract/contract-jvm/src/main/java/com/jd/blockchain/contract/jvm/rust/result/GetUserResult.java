package com.jd.blockchain.contract.jvm.rust.result;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Result;

/**
 * 获取用户返回用户信息
 */
public class GetUserResult extends Result {

    @JSONField(name = "a")
    private String address;
    @JSONField(name = "pk")
    private String pubkey;

    public GetUserResult(String address, String pubkey) {
        super(SUCCESS);
        this.address = address;
        this.pubkey = pubkey;
    }

    public GetUserResult(String address) {
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

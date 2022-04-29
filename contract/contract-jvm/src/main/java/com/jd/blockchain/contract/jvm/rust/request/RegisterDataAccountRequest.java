package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 注册数据账户
 */
public class RegisterDataAccountRequest extends Request {

    @JSONField(name = "s")
    private String seed;
    @JSONField(name = "a")
    private String algorithm;

    public RegisterDataAccountRequest(String seed) {
        this(seed, "ed25519");
    }

    public RegisterDataAccountRequest(String seed, String algorithm) {
        super(RequestType.REGISTER_DATA_ACCOUNT.getCode());
        this.seed = seed;
        this.algorithm = algorithm;
    }

    public String getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}

package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 注册用户
 */
public class RegisterUserRequest extends Request {

    @JSONField(name = "s")
    private String seed;
    @JSONField(name = "a")
    private String algorithm;

    public RegisterUserRequest(String seed) {
        this(seed, "ed25519");
    }

    public RegisterUserRequest(String seed, String algorithm) {
        super(RequestType.REGISTER_USER.getCode());
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

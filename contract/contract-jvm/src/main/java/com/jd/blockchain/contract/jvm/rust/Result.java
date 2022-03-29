package com.jd.blockchain.contract.jvm.rust;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Wasm调用账本数据库请求返回
 */
public class Result {
    // 成功
    public static final int SUCCESS = 0;
    // 失败
    public static final int ERROR = 1;

    @JSONField(name = "rc")
    private int code;

    public Result(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public static Result success() {
        return new Result(SUCCESS);
    }

    public static Result error() {
        return new Result(ERROR);
    }
}

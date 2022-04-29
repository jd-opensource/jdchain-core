package com.jd.blockchain.contract.jvm.rust.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.jd.blockchain.contract.jvm.rust.Request;
import com.jd.blockchain.contract.jvm.rust.RequestType;

/**
 * 日志请求
 */
public class LogRequest extends Request {

    public static final int DEBUG = 1;
    public static final int INFO = 2;
    public static final int ERROR = 3;

    @JSONField(name = "l")
    private int level;// 1 debug; 2 info; 3 error
    @JSONField(name = "m")
    private String msg;

    public LogRequest(int level, String msg) {
        super(RequestType.LOG.getCode());
        this.level = level;
        this.msg = msg;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

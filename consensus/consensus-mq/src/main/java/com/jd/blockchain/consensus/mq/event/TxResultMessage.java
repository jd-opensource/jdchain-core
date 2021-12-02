/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: BlockEvent
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/11/20 上午11:32
 * Description:
 */
package com.jd.blockchain.consensus.mq.event;


/**
 * @author shaozhuguang
 * @create 2018/11/20
 * @since 1.0.0
 */

public class TxResultMessage {

    private String key;

    private byte[] response;

    public TxResultMessage(String key, byte[] response) {
        this.key = key;
        this.response = response;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getResponse() {
        return response;
    }

    public void setResponse(byte[] response) {
        this.response = response;
    }
}
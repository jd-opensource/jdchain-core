/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.peer.consensus.MessageEvent
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/11/23 上午11:45
 * Description:
 */
package com.jd.blockchain.consensus.mq.event;

/**
 * @author shaozhuguang
 * @create 2018/11/23
 * @since 1.0.0
 */

public class TxMessage {

    String key;

    byte[] message;

    public TxMessage(String key, byte[] message) {
        this.key = key;
        this.message = message;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }
}
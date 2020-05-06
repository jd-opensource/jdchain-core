package com.jd.blockchain.kvdb.protocol;

/**
 * 连接就绪回调，在客户端连接成功，上下文信息创建成功后调用
 */
public interface ConnectedCallback {

    void onConnected();
}

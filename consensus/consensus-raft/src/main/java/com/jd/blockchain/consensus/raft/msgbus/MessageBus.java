package com.jd.blockchain.consensus.raft.msgbus;

public interface MessageBus {

    String BLOCK_CATCH_UP_TOPIC = "BLOCK_CATCH_UP";

    void register(String topic, Subcriber subcriber);

    void deregister(String topic, Subcriber subcriber);

    void publish(String topic, byte[] data);

    void publishOrdered(String topic, byte[] data);

    void close();
}

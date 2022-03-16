package com.jd.blockchain.consensus.raft.config;

import com.jd.blockchain.consensus.NetworkReplica;
import com.jd.blockchain.crypto.PubKey;
import utils.Bytes;
import utils.net.NetworkAddress;

public class RaftReplica implements NetworkReplica {

    private int id;
    private Bytes address;
    private String name;
    private PubKey pubKey;
    private NetworkAddress networkAddress;

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public Bytes getAddress() {
        return address;
    }

    public void setAddress(Bytes address) {
        this.address = address;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public PubKey getPubKey() {
        return pubKey;
    }

    public void setPubKey(PubKey pubKey) {
        this.pubKey = pubKey;
    }

    @Override
    public NetworkAddress getNetworkAddress() {
        return networkAddress;
    }

    public void setNetworkAddress(NetworkAddress networkAddress) {
        this.networkAddress = networkAddress;
    }

    public String getPeerStr() {
        return String.format("%s:%d", this.getNetworkAddress().getHost(), this.getNetworkAddress().getPort());
    }

}

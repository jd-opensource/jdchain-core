package com.jd.blockchain.consensus.raft.config;

import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.raft.settings.RaftNodeSettings;
import com.jd.blockchain.crypto.PubKey;
import utils.PropertiesUtils;
import utils.net.NetworkAddress;

import java.util.Properties;

public class RaftNodeConfig extends PropertyConfig implements RaftNodeSettings {

    private int id;

    private String address;

    private PubKey pubKey;

    private String raftPath;

    private NetworkAddress networkAddress;

    public void init(Properties props, Replica replica) {
        this.setId(replica.getId());
        this.setAddress(replica.getAddress().toBase58());
        this.setPubKey(replica.getPubKey());

        boolean secure = PropertiesUtils.getBooleanOptional(props, String.format("system.server.%d.network.secure", replica.getId()), false);
        String host = PropertiesUtils.getProperty(props, String.format("system.server.%d.network.host", replica.getId()), true);
        int port = PropertiesUtils.getInt(props, String.format("system.server.%d.network.port", replica.getId()));
        String path = PropertiesUtils.getProperty(props, String.format("system.server.%d.raft.path", replica.getId()), true);

        this.setRaftPath(path);
        this.setNetworkAddress(new NetworkAddress(host, port, secure));
    }

    public Properties convert() {

        Properties properties = new Properties();

        setValue(properties, String.format("system.server.%d.network.host", this.getId()), this.getNetworkAddress().getHost());
        setValue(properties, String.format("system.server.%d.network.port", this.getId()), this.getNetworkAddress().getPort());
        setValue(properties, String.format("system.server.%d.network.secure", this.getId()), this.getNetworkAddress().isSecure());
        setValue(properties, String.format("system.server.%d.raft.path", this.getId()), this.getRaftPath());

        return properties;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public PubKey getPubKey() {
        return pubKey;
    }

    public void setPubKey(PubKey pubKey) {
        this.pubKey = pubKey;
    }

    @Override
    public String getRaftPath() {
        return raftPath;
    }

    public void setRaftPath(String raftPath) {
        this.raftPath = raftPath;
    }

    @Override
    public NetworkAddress getNetworkAddress() {
        return networkAddress;
    }

    public void setNetworkAddress(NetworkAddress networkAddress) {
        this.networkAddress = networkAddress;
    }


}
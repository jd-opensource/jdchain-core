package com.jd.blockchain.consensus.raft.config;

import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.consensus.raft.settings.RaftNodeSettings;
import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.PubKey;
import utils.PropertiesUtils;
import utils.codec.Base58Utils;
import utils.net.NetworkAddress;

import java.util.Properties;

public class RaftNodeConfig extends PropertyConfig implements RaftNodeSettings {

    public static final String SYSTEM_SERVER_D_NETWORK_SECURE = "system.server.%d.network.secure";
    public static final String SYSTEM_SERVER_D_NETWORK_HOST = "system.server.%d.network.host";
    public static final String SYSTEM_SERVER_D_NETWORK_PORT = "system.server.%d.network.port";
    public static final String SYSTEM_SERVER_D_RAFT_PATH = "system.server.%d.raft.path";
    public static final String SYSTEM_SERVER_D_PUBKEY = "system.server.%d.pubkey";
    private int id;

    private String address;

    private PubKey pubKey;

    private String raftPath;

    private NetworkAddress networkAddress;

    public RaftNodeConfig() {
    }

    public RaftNodeConfig(int id, String address, PubKey pubKey, String raftPath, NetworkAddress networkAddress) {
        this.id = id;
        this.address = address;
        this.pubKey = pubKey;
        this.raftPath = raftPath;
        this.networkAddress = networkAddress;
    }

    public RaftNodeConfig(RaftNodeSettings raftNodeSettings) {
        this(raftNodeSettings.getId(), raftNodeSettings.getAddress(), raftNodeSettings.getPubKey(), raftNodeSettings.getRaftPath(), raftNodeSettings.getNetworkAddress());
    }

    public void init(Properties props, Replica replica) {
        this.setId(replica.getId());
        this.setAddress(replica.getAddress().toBase58());
        this.setPubKey(replica.getPubKey());

        boolean secure = PropertiesUtils.getBooleanOptional(props, String.format(SYSTEM_SERVER_D_NETWORK_SECURE, replica.getId()), false);
        String host = PropertiesUtils.getProperty(props, String.format(SYSTEM_SERVER_D_NETWORK_HOST, replica.getId()), true);
        int port = PropertiesUtils.getInt(props, String.format(SYSTEM_SERVER_D_NETWORK_PORT, replica.getId()));
        String path = PropertiesUtils.getProperty(props, String.format(SYSTEM_SERVER_D_RAFT_PATH, replica.getId()), true);

        this.setRaftPath(path);
        this.setNetworkAddress(new NetworkAddress(host, port, secure));
    }

    public void init(Properties props, int id) {
        this.setId(id);
        String host = props.getProperty(String.format(SYSTEM_SERVER_D_NETWORK_HOST, id));
        int port = Integer.parseInt(props.getProperty(String.format(SYSTEM_SERVER_D_NETWORK_PORT, id)));
        boolean secure = Boolean.parseBoolean(props.getProperty(String.format(SYSTEM_SERVER_D_NETWORK_SECURE, id)));
        String raftPath = props.getProperty(String.format(SYSTEM_SERVER_D_RAFT_PATH, id));
        byte[] pubKeyBytes = Base58Utils.decode(props.getProperty(String.format(SYSTEM_SERVER_D_PUBKEY, id)));
        PubKey pubKey = Crypto.resolveAsPubKey(pubKeyBytes);

        this.setAddress(AddressEncoding.generateAddress(pubKey).toBase58());
        this.setPubKey(pubKey);
        this.setRaftPath(raftPath);
        this.setNetworkAddress(new NetworkAddress(host, port, secure));
    }

    public Properties convert() {

        Properties properties = new Properties();

        setValue(properties, String.format(SYSTEM_SERVER_D_NETWORK_HOST, this.getId()), this.getNetworkAddress().getHost());
        setValue(properties, String.format(SYSTEM_SERVER_D_NETWORK_PORT, this.getId()), this.getNetworkAddress().getPort());
        setValue(properties, String.format(SYSTEM_SERVER_D_NETWORK_SECURE, this.getId()), this.getNetworkAddress().isSecure());
        setValue(properties, String.format(SYSTEM_SERVER_D_RAFT_PATH, this.getId()), this.getRaftPath());

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
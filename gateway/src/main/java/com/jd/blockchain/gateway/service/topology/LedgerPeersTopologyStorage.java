package com.jd.blockchain.gateway.service.topology;

import com.jd.binaryproto.BinaryProtocol;
import utils.io.Storage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LedgerPeersTopologyStorage {

    private Storage topologyStorage;

    public LedgerPeersTopologyStorage(Storage runtimeStorage) {
        topologyStorage = runtimeStorage.getStorage("ledgers");
    }

    public Set<String> getLedgers() {
        return new HashSet<>(Arrays.asList(topologyStorage.getKeyNames()));
    }

    public LedgerPeersTopology getTopology(String ledger) {
        byte[] bytes = topologyStorage.readBytes(ledger);
        if (bytes == null) {
            return null;
        }
        return BinaryProtocol.decode(bytes);
    }

    public void setTopology(String ledger, LedgerPeersTopology topology) {
        byte[] bytes = BinaryProtocol.encode(topology);
        topologyStorage.writeBytes(ledger, bytes);
    }

}

package com.jd.blockchain.gateway.service.topology;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import utils.net.NetworkAddress;

import java.util.Set;

public class LedgerPeerApiServicesTopology implements LedgerPeersTopology {

    private HashDigest ledger;
    private Set<NetworkAddress> peerAddresses;
    private AsymmetricKeypair keypair;

    public LedgerPeerApiServicesTopology(HashDigest ledger, AsymmetricKeypair keypair, Set<NetworkAddress> peerAddresses) {
        this.ledger = ledger;
        this.peerAddresses = peerAddresses;
        this.keypair = keypair;
    }

    @Override
    public HashDigest getLedger() {
        return ledger;
    }

    @Override
    public PubKey getPubKey() {
        return keypair.getPubKey();
    }

    @Override
    public PrivKey getPrivKey() {
        return keypair.getPrivKey();
    }

    @Override
    public NetworkAddress[] getPeerAddresses() {
        return peerAddresses.toArray(new NetworkAddress[peerAddresses.size()]);
    }

    @Override
    public String toString() {
        return "ledger=" + ledger +
                ", peerAddresses=" + peerAddresses +
                ", keypair=" + keypair.getPubKey() +
                '}';
    }
}

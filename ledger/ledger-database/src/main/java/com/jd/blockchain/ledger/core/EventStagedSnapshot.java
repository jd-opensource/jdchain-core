package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerEventSnapshot;

public class EventStagedSnapshot implements LedgerEventSnapshot {
    private HashDigest systemEventSetHash;
    private HashDigest userEventSetHash;

    @Override
    public HashDigest getSystemEventSetHash() {
        return systemEventSetHash;
    }

    @Override
    public HashDigest getUserEventSetHash() {
        return userEventSetHash;
    }

    public void setSystemEventSetHash(HashDigest systemEventSetHash) {
        this.systemEventSetHash = systemEventSetHash;
    }

    public void setUserEventSetHash(HashDigest eventAccountSetHash) {
        this.userEventSetHash = eventAccountSetHash;
    }
}

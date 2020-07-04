package com.jd.blockchain.ledger.core;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.DataEntry;
import com.jd.blockchain.utils.DataIterator;
import com.jd.blockchain.utils.Transactional;

import java.util.ArrayList;
import java.util.List;

public class MerkleEventSet implements EventGroup, EventPublisher, Transactional {

    private MerkleDataSet events;

    public MerkleEventSet(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exStorage, VersioningKVStorage verStorage) {
        events = new MerkleDataSet(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage);
    }

    public MerkleEventSet(HashDigest dataRootHash, CryptoSetting cryptoSetting, String prefix,
                          ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, boolean readonly) {
        events = new MerkleDataSet(dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, readonly);
    }

    /**
     * 发布事件
     *
     * @param event
     * @return
     */
    @Override
    public long publish(Event event) {
        Bytes key = encodeKey(event.getName());
        long newSequence = events.setValue(key, BinaryProtocol.encode(event, Event.class), event.getSequence() - 1);

        if (newSequence < 0) {
            throw new LedgerException("Transaction is persisted repeatly! --[" + key + "]");
        }

        return newSequence;
    }

    @Override
    public Event[] getEvents(String eventName, long fromSequence, int maxCount) {
        List<Event> eventsList = new ArrayList<>();
        Bytes key = encodeKey(eventName);
        long maxVersion = events.getVersion(key);
        for (int i = 0; i < maxCount && i <= maxVersion; i++) {
            byte[] bs = events.getValue(key, fromSequence + i);
            if (null == bs) {
                break;
            }
            Event event = BinaryProtocol.decode(bs);
            eventsList.add(new EventInfo(event));
        }
        return eventsList.toArray(new Event[eventsList.size()]);
    }

    public HashDigest getRootHash() {
        return events.getRootHash();
    }

    private Bytes encodeKey(String eventName) {
        return Bytes.fromString(eventName);
    }

    private String decodeKey(Bytes key) {
        return key.toUTF8String();
    }

    @Override
    public boolean isUpdated() {
        return events.isUpdated();
    }

    @Override
    public void commit() {
        if (events.getDataCount() == 0) {
            return;
        }
        events.commit();
    }

    @Override
    public void cancel() {
        events.cancel();
    }

    public boolean isReadonly() {
        return events.isReadonly();
    }

    void setReadonly() {
        events.setReadonly();
    }

    @Override
    public String[] getEventNames(long fromIndex, int count) {
        DataIterator<Bytes, byte[]> iterator = events.iterator();
        iterator.skip(fromIndex);
        DataEntry<Bytes, byte[]>[] entries = iterator.next(count);
        String[] events = new String[entries.length];
        for (int i = 0; i < entries.length; i++) {
            events[i] = decodeKey(entries[i].getKey());
        }

        return events;
    }

    @Override
    public long totalEventNames() {
        return events.getDataCount();
    }

    @Override
    public long totalEvents(String eventName) {
        return events.getVersion(encodeKey(eventName)) + 1;
    }

    @Override
    public Event getLatest(String eventName) {
        byte[] bs = events.getValue(encodeKey(eventName));
        if (null == bs) {
            return null;
        }
        return new EventInfo(BinaryProtocol.decode(bs));
    }
}

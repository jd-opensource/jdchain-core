package com.jd.blockchain.ledger.core;

import java.util.ArrayList;
import java.util.List;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;

import utils.Bytes;
import utils.DataEntry;
import utils.Mapper;
import utils.SkippingIterator;
import utils.Transactional;

public class MerkleEventGroupPublisher implements EventGroup, EventPublisher, Transactional {

    private  MerkleDataset<Bytes, byte[]> events;

    public MerkleEventGroupPublisher(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exStorage, VersioningKVStorage verStorage) {
        events = new MerkleHashDataset(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage);
    }

    public MerkleEventGroupPublisher(HashDigest dataRootHash, CryptoSetting cryptoSetting, String prefix,
                          ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, boolean readonly) {
        events = new MerkleHashDataset(dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, readonly);
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

    @Override
    public String[] getEventNames(long fromIndex, int count) {
        SkippingIterator<DataEntry<Bytes, byte[]>> iterator = events.iterator();
        iterator.skip(fromIndex);
        
        String[] events = iterator.next(count, String.class, new Mapper<DataEntry<Bytes,byte[]>, String>() {
			@Override
			public String from(DataEntry<Bytes, byte[]> source) {
				return decodeKey(source.getKey());
			}
		});
        
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

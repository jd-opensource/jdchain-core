package com.jd.blockchain.ledger.core;

import java.util.ArrayList;
import java.util.List;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;

import utils.Bytes;
import utils.DataEntry;
import utils.Mapper;
import utils.SkippingIterator;
import utils.Transactional;

public class EventGroupPublisher implements EventGroup, EventPublisher, Transactional {

    private BaseDataset<Bytes, byte[]> events;

    private LedgerDataStructure ledgerDataStructure;

    // start: used only by kv ledger structure
    private volatile long event_index_in_block = 0;

    private volatile long origin_event_index_in_block  = 0;

    private static final Bytes SYSTEMEVENT_SEQUENCE_KEY_PREFIX = Bytes.fromString("SEQ" + LedgerConsts.KEY_SEPERATOR);
    // end: used only by kv ledger structure

    public EventGroupPublisher(CryptoSetting cryptoSetting, String prefix, ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, LedgerDataStructure dataStructure) {
        ledgerDataStructure = dataStructure;

        if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
            events = new MerkleHashDataset(cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage);
        } else {
            events = new KvDataset(DatasetType.NONE, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage);
        }
    }

    public EventGroupPublisher(long preBlockHeight, HashDigest dataRootHash, CryptoSetting cryptoSetting, String prefix,
                               ExPolicyKVStorage exStorage, VersioningKVStorage verStorage, LedgerDataStructure dataStructure, boolean readonly) {
        ledgerDataStructure = dataStructure;

        if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
            events = new MerkleHashDataset(dataRootHash, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, readonly);
        } else {
            events = new KvDataset(preBlockHeight, dataRootHash, DatasetType.NONE, cryptoSetting, Bytes.fromString(prefix), exStorage, verStorage, readonly);
        }
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
            throw new LedgerException("Event sequence conflict! --[" + key + "]");
        }

        // 属于新发布的事件名
        if (ledgerDataStructure.equals(LedgerDataStructure.KV)) {
            if (newSequence == 0) {
                long nv = events.setValue(SYSTEMEVENT_SEQUENCE_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(events.getDataCount() + event_index_in_block))), key.toBytes(), -1);

                if (nv < 0) {
                    throw new LedgerException("Event seq already exist! --[id=" + key + "]");
                }
                event_index_in_block++;
            }
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

        origin_event_index_in_block = event_index_in_block;
    }

    @Override
    public void cancel() {
        events.cancel();

        event_index_in_block = origin_event_index_in_block;
    }

    public boolean isReadonly() {
        return events.isReadonly();
    }

    @Override
    public String[] getEventNames(long fromIndex, int count) {
        SkippingIterator<DataEntry<Bytes, byte[]>> iterator = events.idIterator();
        iterator.skip(fromIndex);
        
        String[] events = iterator.next(count, String.class, new Mapper<DataEntry<Bytes,byte[]>, String>() {
			@Override
			public String from(DataEntry<Bytes, byte[]> source) {
			    if (ledgerDataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
                    return decodeKey(source.getKey());
                } else {
			        return decodeKey(new Bytes(source.getValue()));
                }
			}
		});
        
        return events;
    }

    @Override
    public long totalEventNames() {
        return events.getDataCount() + event_index_in_block;
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

    public boolean isAddNew() {
        return event_index_in_block != 0;
    }

    public void clearCachedIndex() {
        event_index_in_block = 0;
    }
}

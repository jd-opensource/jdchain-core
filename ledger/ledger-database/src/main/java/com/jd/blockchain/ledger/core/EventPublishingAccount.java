package com.jd.blockchain.ledger.core;

import java.util.ArrayList;
import java.util.List;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.TypedValue;

import utils.Bytes;
import utils.DataEntry;
import utils.Dataset;
import utils.Mapper;
import utils.SkippingIterator;
import utils.io.BytesUtils;

public class EventPublishingAccount implements EventAccount, EventPublisher {

    private CompositeAccount account;

    private static final Bytes ACCOUNTSET_SEQUENCE_KEY_PREFIX = Bytes.fromString("SEQ" + LedgerConsts.KEY_SEPERATOR);

    public EventPublishingAccount(CompositeAccount account) {
        this.account = account;
    }

    @Override
    public long publish(Event event) {
        return account.getDataset().setValue(event.getName(), TypedValue.fromBytes(BinaryProtocol.encode(event, Event.class)), event.getSequence() - 1);
    }

    @Override
    public Event[] getEvents(String eventName, long fromSequence, int count) {
        List<Event> events = new ArrayList<>();
        Dataset<String, TypedValue> ds = account.getDataset();
        long maxVersion = account.getDataset().getVersion(eventName) + 1;
        for (int i = 0; i < count && i <= maxVersion; i++) {
            TypedValue tv = ds.getValue(eventName, fromSequence + i);
            if (null == tv || tv.isNil()) {
                break;
            }
            Event event = BinaryProtocol.decode(tv.bytesValue());
            events.add(new EventInfo(event));

        }
        return events.toArray(new Event[events.size()]);
    }

    @Override
    public String[] getEventNames(long fromIndex, int count) {

        String[] eventNames = new String[count];

        for (int index = 0; index < count; index++) {
            byte[] indexKey = ((SimpleDatasetImpl)((ComplecatedSimpleAccount)account).getDataDataset()).getKeyByIndex(fromIndex + index);
            eventNames[index] = BytesUtils.toString(indexKey);
        }

        return eventNames;
    }

    @Override
    public long totalEventNames() {
        return account.getDataset().getDataCount();
    }

    @Override
    public long totalEvents(String eventName) {
        return account.getDataset().getVersion(eventName) + 1;
    }

    @Override
    public Event getLatest(String eventName) {
        TypedValue tv = account.getDataset().getValue(eventName);
        if (null == tv || tv.isNil()) {
            return null;
        }

        return new EventInfo(BinaryProtocol.decode(tv.bytesValue()));
    }

    @Override
    public BlockchainIdentity getID() {
        return account.getID();
    }
    
    
    @Override
    public Bytes getAddress() {
    	return account.getID().getAddress();
    }
    
    @Override
    public HashDigest getDataRootHash() {
    	return account.getDataRootHash();
    }
    
    @Override
    public HashDigest getHeaderRootHash() {
    	return account.getHeaderRootHash();
    }
    
    @Override
    public PubKey getPubKey() {
    	return account.getID().getPubKey();
    }
    
    

}

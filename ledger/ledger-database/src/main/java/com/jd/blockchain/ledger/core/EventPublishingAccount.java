package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.AccountType;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.TypedValue;
import utils.Bytes;
import utils.DataEntry;
import utils.Dataset;
import utils.Mapper;
import utils.SkippingIterator;

import java.util.ArrayList;
import java.util.List;

public class EventPublishingAccount extends PermissionAccountDecorator implements EventAccount, EventPublisher {

    public EventPublishingAccount(CompositeAccount account) {
        super(AccountType.EVENT, account);
    }

    @Override
    public long publish(Event event) {
        return mklAccount.getDataset().setValue(event.getName(), TypedValue.fromBytes(BinaryProtocol.encode(event, Event.class)), event.getSequence() - 1);
    }

    @Override
    public Event[] getEvents(String eventName, long fromSequence, int count) {
        List<Event> events = new ArrayList<>();
        Dataset<String, TypedValue> ds = mklAccount.getDataset();
        long maxVersion = mklAccount.getDataset().getVersion(eventName) + 1;
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
        SkippingIterator<DataEntry<String, TypedValue>> iterator = mklAccount.getDataset().iterator();
        iterator.skip(fromIndex);

        String[] eventNames = iterator.next(count, String.class, new Mapper<DataEntry<String, TypedValue>, String>() {
            @Override
            public String from(DataEntry<String, TypedValue> source) {
                return source.getKey();
            }
        });

        return eventNames;
    }

    @Override
    public long totalEventNames() {
        return mklAccount.getDataset().getDataCount();
    }

    @Override
    public long totalEvents(String eventName) {
        return mklAccount.getDataset().getVersion(eventName) + 1;
    }

    @Override
    public Event getLatest(String eventName) {
        TypedValue tv = mklAccount.getDataset().getValue(eventName);
        if (null == tv || tv.isNil()) {
            return null;
        }

        return new EventInfo(BinaryProtocol.decode(tv.bytesValue()));
    }

    @Override
    public BlockchainIdentity getID() {
        return mklAccount.getID();
    }

    @Override
    public Bytes getAddress() {
        return mklAccount.getID().getAddress();
    }

    @Override
    public HashDigest getDataRootHash() {
        return mklAccount.getDataRootHash();
    }

    @Override
    public HashDigest getHeaderRootHash() {
        return mklAccount.getHeaderRootHash();
    }

    @Override
    public PubKey getPubKey() {
        return mklAccount.getID().getPubKey();
    }

}

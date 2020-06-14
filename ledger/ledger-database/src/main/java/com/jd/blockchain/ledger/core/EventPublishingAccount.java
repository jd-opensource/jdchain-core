package com.jd.blockchain.ledger.core;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.ledger.Account;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.DataEntry;
import com.jd.blockchain.utils.DataIterator;
import com.jd.blockchain.utils.Dataset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EventPublishingAccount implements EventAccount, EventPublisher {

    private Account account;

    public EventPublishingAccount(Account account) {
        this.account = account;
    }

    @Override
    public long publish(Event event) {
        return account.getDataset().setValue(event.getName(), TypedValue.fromBytes(BinaryProtocol.encode(event, Event.class)), event.getSequence()-1);
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
    public Event[] getEventNames(long fromIndex, int count) {
        DataIterator<String, TypedValue> iterator = account.getDataset().iterator();
        iterator.skip(fromIndex);
        DataEntry<String, TypedValue>[] entries = iterator.next(count);
        Event[] events = new Event[entries.length];
        for (int i = 0; i < entries.length; i++) {
            events[i] = BinaryProtocol.decode(entries[i].getValue().bytesValue());
        }

        return events;
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
    public BlockchainIdentity getID() {
        return account.getID();
    }

}

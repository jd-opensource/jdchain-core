package com.jd.blockchain.ledger.core;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.ledger.Account;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.EventInfo;
import com.jd.blockchain.ledger.TypedValue;
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
        return account.getDataset().setValue(event.getName(), TypedValue.fromBytes(BinaryProtocol.encode(event, Event.class)), event.getSequence());
    }

    @Override
    public Iterator<Event> getEvents(String eventName, long fromSequence, int maxCount) {
        List<Event> events = new ArrayList<>();
        Dataset<String, TypedValue> ds = account.getDataset();
        long maxVersion = account.getDataset().getVersion(eventName);
        for (int i = 0; i < maxCount && i <= maxVersion; i++) {
            TypedValue tv = ds.getValue(eventName, fromSequence + i);
            if (null == tv || tv.isNil()) {
                break;
            }
            Event event = BinaryProtocol.decode(tv.bytesValue());
            events.add(new EventInfo(event));

        }
        return events.iterator();
    }

    @Override
    public BlockchainIdentity getID() {
        return account.getID();
    }

}

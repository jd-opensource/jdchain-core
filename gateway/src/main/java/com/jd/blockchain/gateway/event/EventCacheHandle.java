package com.jd.blockchain.gateway.event;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.sdk.EventPoint;
import com.jd.blockchain.sdk.UserEventPoint;
import org.apache.commons.collections4.map.LRUMap;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 事件缓存处理器
 *
 * @author shaozhuguang
 *
 */
public class EventCacheHandle implements EventCache {

    /**
     * 缓存中最大的用户事件数量
     */
    private static final int MAX_EVENTS = 1024;

    private final Lock heightLock = new ReentrantLock();

    private final Lock mapLock = new ReentrantLock();

    private volatile long maxHeight = -1;

    private final HashDigest ledgerHash;

    private final LRUMap<String, Events> eventsLruMap = new LRUMap<>(MAX_EVENTS);

    public EventCacheHandle(HashDigest ledgerHash) {
        this.ledgerHash = ledgerHash;
    }

    @Override
    public HashDigest getLedgerHash() {
        return ledgerHash;
    }

    @Override
    public long getMaxHeight() {
        return this.maxHeight;
    }

    @Override
    public void updateMaxHeight(long maxHeight) {
        heightLock.lock();
        try {
            setMaxHeight(Math.max(maxHeight, this.maxHeight));
        } finally {
            heightLock.unlock();
        }
    }

    @Override
    public long getMaxSequence(String key) {
        Events events = eventsLruMap.get(key);
        if (events != null) {
            return events.maxSequence();
        }
        return -1L;
    }

    @Override
    public long getMaxHeight(String key) {
        Events events = eventsLruMap.get(key);
        if (events != null) {
            return events.maxBlockHeight();
        }
        return -1L;
    }

    @Override
    public void addEvents(String key, Event... events) {
        if (events.length > 0) {
            mapLock.lock();
            try {
                Events eventList = eventsLruMap.get(key);
                if (eventList == null) {
                    eventList = new Events();
                    eventsLruMap.put(key, eventList);
                }
                for (Event event : events) {
                    eventList.put(event.getSequence(), event);
                    updateMaxHeight(event.getBlockHeight());
                }
            } finally {
                mapLock.unlock();
            }
        }
    }

    @Override
    public Event getEvent(String key, long sequence) {
        Events eventList = eventsLruMap.get(key);
        if (eventList != null) {
            return eventList.get(sequence);
        }
        return null;
    }

    private void setMaxHeight(long maxHeight) {
        this.maxHeight = maxHeight;
    }

    public static String eventKey(EventPoint eventPoint) {
        if (eventPoint instanceof UserEventPoint) {
            UserEventPoint userEventPoint = (UserEventPoint) eventPoint;
            return userEventPoint.getEventName() + "-" + userEventPoint.getEventAccount();
        }
        return eventPoint.getEventName();
    }

    private class Events {

        static final int MAX_EVENTS = 1024;

        private final Lock lock = new ReentrantLock();

        private volatile long maxSequence;

        private volatile long maxBlockHeight;

        private Map<Long, Event> events = new LRUMap<>(MAX_EVENTS);

        void maxBlockHeight(long maxBlockHeight) {
            lock.lock();
            try {
                this.maxBlockHeight = Math.max(this.maxBlockHeight, maxBlockHeight);
            } finally {
                lock.unlock();
            }
        }

        long maxBlockHeight() {
            return this.maxBlockHeight;
        }

        void maxSequence(long maxSequence) {
            lock.lock();
            try {
                this.maxSequence = Math.max(this.maxSequence, maxSequence);
            } finally {
                lock.unlock();
            }
        }

        long maxSequence() {
            return this.maxSequence;
        }

        void put(long sequence, Event event) {
            events.put(sequence, event);
            maxBlockHeight(event.getBlockHeight());
            maxSequence(sequence);
        }

        public Event get(long sequence) {
            return events.get(sequence);
        }
    }
}

package com.jd.blockchain.gateway.event;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.LedgerInfo;
import com.jd.blockchain.sdk.EventPoint;
import com.jd.blockchain.sdk.SystemEventPoint;
import com.jd.blockchain.sdk.UserEventPoint;
import com.jd.blockchain.transaction.BlockchainQueryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Pull方式的事件监听器
 *
 * @author shaozhuguang
 *
 */
public class PullEventListener implements EventListener {

    private static final int THREAD_CORE = 1;

    private static final int PERIOD_SECONDS = 5;

    private static final int MAX_EVENT_COUNT = 1000;
    /**
     * 用户事件缓存
     */
    private final Map<HashDigest, EventCacheHandle> userEventCaches = new ConcurrentHashMap<>();
    /**
     * 系统事件缓存
     */
    private final Map<HashDigest, EventCacheHandle> systemEventCaches = new ConcurrentHashMap<>();
    /**
     * 定时线程池
     */
    private final ScheduledThreadPoolExecutor pullExecutor;
    /**
     * queryService
     *         用于调用http从Peer查询数据
     */
    private BlockchainQueryService blockchainQueryService;

    public PullEventListener(BlockchainQueryService blockchainQueryService) {
        this.blockchainQueryService = blockchainQueryService;
        pullExecutor = scheduledThreadPoolExecutor();
        initCache();
    }

    @Override
    public void start() {
        // 有一个定时任务，定时更新所有的区块高度
        pullExecutor.scheduleAtFixedRate(new BlockPullRunner(), 0, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public Event[] getEvents(HashDigest ledgerHash, EventPoint eventPoint, long fromSequence, int maxCount) {
        maxCount = resetCount(maxCount);
        List<Event> events;
        if (eventPoint instanceof UserEventPoint) {
            events = getEvents(userEventCaches, ledgerHash, eventPoint, fromSequence, maxCount);
        } else {
            events = getEvents(systemEventCaches, ledgerHash, eventPoint, fromSequence, maxCount);
        }
        if (events != null) {
            return events.toArray(new Event[events.size()]);
        }
        return null;
    }

    @Override
    public Event[] getSystemEvents(HashDigest ledgerHash, String eventName, long fromSequence, int maxCount) {
        EventPoint eventPoint = new SystemEventPoint(eventName);
        return getEvents(ledgerHash, eventPoint, fromSequence, maxCount);
    }

    @Override
    public Event[] getUserEvents(HashDigest ledgerHash, String address, String eventName, long fromSequence, int count) {
        EventPoint eventPoint = new UserEventPoint(address, eventName);
        return getEvents(ledgerHash, eventPoint, fromSequence, count);
    }

    private void initCache() {
        HashDigest[] ledgerHashs = blockchainQueryService.getLedgerHashs();
        if (ledgerHashs != null) {
            for (HashDigest ledgerHash : ledgerHashs) {
                userEventCaches.put(ledgerHash, new EventCacheHandle(ledgerHash));
                systemEventCaches.put(ledgerHash, new EventCacheHandle(ledgerHash));
            }
        }
    }

    private List<Event> getEvents(Map<HashDigest, EventCacheHandle> eventCaches, HashDigest ledgerHash, EventPoint eventPoint, long fromSequence, int maxCount) {
        List<Event> events = new ArrayList<>();
        String key = EventCacheHandle.eventKey(eventPoint);
        // 首先判断已处理的最大高度
        EventCacheHandle eventCache = eventCaches.get(ledgerHash);
        if (eventCache == null) {
            Event[] eventsByQuery = getEventsByQuery(ledgerHash, eventPoint, fromSequence, maxCount);
            if (!empty(eventsByQuery)) {
                eventCache.addEvents(key, eventsByQuery);
                events.addAll(Arrays.asList(eventsByQuery));
            }
            return events;
        } else {
            // 有该缓存，则需要进行逻辑判断
            long maxBlockHeight = eventCache.getMaxHeight(), currKeyBlockHeight = eventCache.getMaxHeight(key);
            if (maxBlockHeight == -1L || currKeyBlockHeight == -1L) {
                Event[] eventsByQuery = getEventsByQuery(ledgerHash, eventPoint, fromSequence, maxCount);
                if (!empty(eventsByQuery)) {
                    eventCache.addEvents(key, eventsByQuery);
                    events.addAll(Arrays.asList(eventsByQuery));
                }
                return events;
            } else if (maxBlockHeight <= currKeyBlockHeight) {
                // 表示最近没有新区块生成，那么肯定没有新事件发生，事件仍停留在上次处理的最大sequence
                long maxSequence = eventCache.getMaxSequence(key);
                if (maxSequence < fromSequence) {
                    // 不处理，因为查询的范围不再处理范围之内
                    return events;
                } else {
                    // 只需要查询截止到maxSequence即可
                    long endSequence = maxSequence + 1;
                    getAndUpdateByCacheAndQuery(ledgerHash, eventCache, eventPoint, events, key, fromSequence, endSequence);
                }
            } else {
                // 已有新区块生成，但不一定有新事件，可能需要更新
                // 部分需要从缓存获取（也可能是全部需要）
                long endSequence = fromSequence + maxCount;
                getAndUpdateByCacheAndQuery(ledgerHash, eventCache, eventPoint, events, key, fromSequence, endSequence);
            }
        }

        return events;
    }

    private void getAndUpdateByCacheAndQuery(HashDigest ledgerHash, EventCache eventCache, EventPoint eventPoint,
                                    List<Event> events, String key, long start, long end) {
        List<Event> cachedEvents = new ArrayList<>();
        boolean needQuery = false;
        for (long l = start; l < end; l++) {
            Event event = eventCache.getEvent(key, l);
            if (event == null) {
                needQuery = true;
                break;
            }
            cachedEvents.add(event);
        }
        if(needQuery) {
            Event[] eventsByQuery = getEventsByQuery(ledgerHash, eventPoint, start, (int) (end-start));
            if (!empty(eventsByQuery)) {
                eventCache.addEvents(key, eventsByQuery);
                events.addAll(Arrays.asList(eventsByQuery));
            }
        } else {
            events.addAll(cachedEvents);
        }
    }

    private boolean empty(Event[] events) {
        return events == null || events.length == 0;
    }

    private int resetCount(int maxCount) {
        if (maxCount < 0) {
            return MAX_EVENT_COUNT;
        }
        return Math.min(maxCount, MAX_EVENT_COUNT);
    }

    private Event[] getEventsByQuery(HashDigest ledgerHash, EventPoint eventPoint, long fromSequence, int maxCount) {
        if (eventPoint instanceof UserEventPoint) {
            UserEventPoint userEventPoint = (UserEventPoint) eventPoint;
            return blockchainQueryService.getUserEvents(ledgerHash, userEventPoint.getEventAccount(),
                    userEventPoint.getEventName(), fromSequence, maxCount);
        }
        return blockchainQueryService.getSystemEvents(ledgerHash, eventPoint.getEventName(), fromSequence, maxCount);
    }

    private Event getEventByQuery(HashDigest ledgerHash, EventPoint eventPoint, long fromSequence) {
        Event[] events = getEventsByQuery(ledgerHash, eventPoint, fromSequence, 1);
        if (!empty(events)) {
            return events[0];
        }
        return null;
    }

    /**
     * 创建定时线程池
     * @return
     */
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("event-pull-%d").build();
        return new ScheduledThreadPoolExecutor(THREAD_CORE,
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    private class BlockPullRunner implements Runnable {

        @Override
        public void run() {
            HashDigest[] ledgerHashs = blockchainQueryService.getLedgerHashs();
            if (ledgerHashs != null && ledgerHashs.length > 0) {
                for (HashDigest ledgerHash : ledgerHashs) {
                    LedgerInfo ledgerInfo = blockchainQueryService.getLedger(ledgerHash);
                    flushCache(ledgerInfo);
                }
            }
        }

        private void flushCache(LedgerInfo ledgerInfo) {
            HashDigest ledgerHash = ledgerInfo.getHash();
            long maxBlockHeight = ledgerInfo.getLatestBlockHeight();
            flushCache(ledgerHash, maxBlockHeight);
        }

        private void flushCache(HashDigest ledgerHash, long maxBlockHeight) {
            flushCache(ledgerHash, maxBlockHeight, userEventCaches);
            flushCache(ledgerHash, maxBlockHeight, systemEventCaches);
        }

        private void flushCache(HashDigest ledgerHash, long maxBlockHeight,
                                    Map<HashDigest, EventCacheHandle> eventCaches) {
            EventCacheHandle eventCache = eventCaches.get(ledgerHash);
            if (eventCache != null) {
                eventCache.updateMaxHeight(maxBlockHeight);
            }
        }
    }
}

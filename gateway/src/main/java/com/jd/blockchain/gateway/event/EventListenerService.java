package com.jd.blockchain.gateway.event;

/**
 * 事件监听Service
 *
 * @author shaozhuguang
 *
 */
public interface EventListenerService {

    /**
     * 返回事件监听器
     *
     * @return
     */
    EventListener getEventListener();
}

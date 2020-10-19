package com.jd.blockchain.runtime;


import java.util.concurrent.atomic.AtomicInteger;

/**
 * 运行中的常量
 *
 */
public class RuntimeConstant {

    /**
     * 管理口常量
     *
     */
    private static final AtomicInteger MONITOR_PORT = new AtomicInteger(-1);

    public static void setMonitorPort(int monitorPort) {
        MONITOR_PORT.set(monitorPort);
    }

    public static int getMonitorPort() {
        return MONITOR_PORT.get();
    }
}

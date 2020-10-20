package com.jd.blockchain.runtime;


import java.util.concurrent.atomic.AtomicInteger;

/**
 * 运行中的常量
 *
 */
public class RuntimeConstant {

    private static final ThreadLocal<Integer> MONITOR_PORT_LOCAL = new ThreadLocal<>();

    /**
     * 管理口常量
     *
     */
    private static final AtomicInteger MONITOR_PORT = new AtomicInteger(-1);

    public static void setMonitorPort(int monitorPort) {
        MONITOR_PORT_LOCAL.set(monitorPort);
        MONITOR_PORT.set(monitorPort);
    }

    public static int getMonitorPort() {
        Integer monitorPort = MONITOR_PORT_LOCAL.get();
        if (monitorPort == null) {
            return MONITOR_PORT.get();
        }
        return monitorPort;
    }
}

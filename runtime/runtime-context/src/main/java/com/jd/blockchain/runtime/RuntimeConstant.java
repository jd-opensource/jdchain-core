package com.jd.blockchain.runtime;


import java.util.concurrent.atomic.AtomicInteger;

/**
 * 运行中的常量
 *
 */
public class RuntimeConstant {

    private static final ThreadLocal<Integer> MONITOR_PORT_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> MONITOR_SECURE_LOCAL = new ThreadLocal<>();

    /**
     * 管理口常量
     *
     */
    private static final AtomicInteger MONITOR_PORT = new AtomicInteger(-1);

    public static void setMonitorProperties(int monitorPort, boolean monitorSecure) {
        MONITOR_PORT_LOCAL.set(monitorPort);
        MONITOR_PORT.set(monitorPort);
        MONITOR_SECURE_LOCAL.set(monitorSecure);
    }

    public static int getMonitorPort() {
        Integer monitorPort = MONITOR_PORT_LOCAL.get();
        if (monitorPort == null) {
            return MONITOR_PORT.get();
        }
        return monitorPort;
    }

    public static boolean isMonitorSecure() {
        Boolean monitorSecure = MONITOR_SECURE_LOCAL.get();
        if (monitorSecure == null) {
            return false;
        }
        return monitorSecure;
    }
}

package com.jd.blockchain.runtime;


/**
 * 运行中的常量
 */
public class RuntimeConstant {

    private static int MONITOR_PORT = -1;
    private static boolean MONITOR_SECURE = false;

    public static void setMonitorProperties(int monitorPort, boolean monitorSecure) {
        MONITOR_PORT = monitorPort;
        MONITOR_SECURE = monitorSecure;
    }

    public static int getMonitorPort() {
        return MONITOR_PORT;
    }

    public static boolean isMonitorSecure() {
        return MONITOR_SECURE;
    }
}

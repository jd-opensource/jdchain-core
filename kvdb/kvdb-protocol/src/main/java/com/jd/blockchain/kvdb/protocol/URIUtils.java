package com.jd.blockchain.kvdb.protocol;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class URIUtils {

    /**
     * 是否本机地址
     *
     * @param host
     * @return
     */
    public static boolean isLocalhost(String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
                return true;
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (Exception e) {
            return false;
        }
    }
}

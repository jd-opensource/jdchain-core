package com.jd.blockchain.kvdb.protocol;

import com.jd.blockchain.kvdb.protocol.exception.KVDBException;

import java.net.URI;

public class KVDBURI {

    private static final String SCHEME = "kvdb";
    private String origin;
    private URI uri;

    public KVDBURI(String origin) {
        this.origin = origin;
        this.uri = URI.create(origin);
        if(!SCHEME.equals(uri.getScheme())) {
            throw new KVDBException("un support url");
        }
    }

    public String getOrigin() {
        return origin;
    }

    public String getHost() {
        return uri.getHost();
    }

    public int getPort() {
        return uri.getPort();
    }

    /**
     * Get database from URI
     *
     * @return
     */
    public String getDatabase() {
        return uri.getPath().replace("/", "");
    }

    /**
     * 是否是本地地址
     *
     * @return
     */
    public boolean isLocalhost() {
        return URIUtils.isLocalhost(getHost());
    }
}

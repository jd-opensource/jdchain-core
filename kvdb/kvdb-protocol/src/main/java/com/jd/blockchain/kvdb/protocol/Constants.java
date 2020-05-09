package com.jd.blockchain.kvdb.protocol;

public class Constants {

    /**
     * codes for server response
     */
    public static final int SUCCESS = 1;
    public static final int ERROR = 0;

    /**
     * Codes for data protocol
     */
    public static final int MESSAGE = 1;
    public static final int MESSAGE_CONTENT = 11;
    public static final int COMMAND = 2;
    public static final int RESPONSE = 3;
    public static final int DATABASE_INFO = 4;
    public static final int CLUSTER_INFO = 5;
    public static final int CLUSTER_ITEM = 6;
    public static final int DATABASE_BASE_INFO = 7;
    public static final int DATABASE_BASE_INFO_LIST = 8;
}

package com.jd.blockchain.contract.jvm.rust;

/**
 * Wasm数据交互请求类型
 */
public enum RequestType {

    LOG(0),
    BEFORE_EVENT(1),
    POST_EVENT(2),
    GET_LEDGER_HASH(3),
    GET_CONTRACT_ADDRESS(4),
    GET_TX_HASH(5),
    GET_TX_TIME(6),
    GET_SIGNERS(7),
    REGISTER_USER(8),
    GET_USER(9),
    REGISTER_DATA_ACCOUNT(10),
    GET_DATA_ACCOUNT(11),
    SET_TEXT(12),
    SET_TEXT_WITH_VERSION(13),
    SET_INT64(14),
    SET_INT64_WITH_VERSION(15),
    GET_VALUE_VERSION(16),
    GET_VALUE(17),
    UNKNOWN(-1);

    private int code;

    RequestType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RequestType valueOf(int code) {
        switch (code) {
            case 0:
                return LOG;
            case 1:
                return BEFORE_EVENT;
            case 2:
                return POST_EVENT;
            case 3:
                return GET_LEDGER_HASH;
            case 4:
                return GET_CONTRACT_ADDRESS;
            case 5:
                return GET_TX_HASH;
            case 6:
                return GET_TX_TIME;
            case 7:
                return GET_SIGNERS;
            case 8:
                return REGISTER_USER;
            case 9:
                return GET_USER;
            case 10:
                return REGISTER_DATA_ACCOUNT;
            case 11:
                return GET_DATA_ACCOUNT;
            case 12:
                return SET_TEXT;
            case 13:
                return SET_TEXT_WITH_VERSION;
            case 14:
                return SET_INT64;
            case 15:
                return SET_INT64_WITH_VERSION;
            case 16:
                return GET_VALUE_VERSION;
            case 17:
                return GET_VALUE;
            default:
                return UNKNOWN;
        }
    }

}

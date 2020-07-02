package com.jd.blockchain.gateway.exception;

/**
 * 区块不存在异常
 *
 * @author shaozhuguang
 *
 */
public class BlockNonExistentException extends RuntimeException {

    public BlockNonExistentException() {
    }

    public BlockNonExistentException(String message) {
        super(message);
    }

    public BlockNonExistentException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlockNonExistentException(Throwable cause) {
        super(cause);
    }

    public BlockNonExistentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

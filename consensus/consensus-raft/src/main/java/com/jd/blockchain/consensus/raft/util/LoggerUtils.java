package com.jd.blockchain.consensus.raft.util;

import org.slf4j.Logger;

public final class LoggerUtils {

    private LoggerUtils() {
    }

    public static void debugIfEnabled(Logger logger, String fmt, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(fmt, args);
        }
        //todo
        logger.error(fmt, args);
    }

    public static void infoIfEnabled(Logger logger, String fmt, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(fmt, args);
        }
    }

    public static void errorIfEnabled(Logger logger, String fmt, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(fmt, args);
        }
    }

    public static void errorIfEnabled(Logger logger, String fmt, Throwable e, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(fmt, args, e);
        }
    }

    public static void errorIfEnabled(Logger logger, String msg, Throwable e) {
        if (logger.isErrorEnabled()) {
            logger.error(msg, e);
        }
    }


}

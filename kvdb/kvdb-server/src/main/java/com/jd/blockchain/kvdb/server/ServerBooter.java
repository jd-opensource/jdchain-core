package com.jd.blockchain.kvdb.server;

import com.jd.blockchain.kvdb.server.config.ServerConfig;
import com.jd.blockchain.utils.ArgumentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerBooter {

    private static final Logger LOGGER = LoggerFactory.getLogger(KVDB.class);

    private static final String HOME_DIR = "-home";

    public static void main(String[] args) {
        try {
            ArgumentSet arguments = ArgumentSet.resolve(args, ArgumentSet.setting().prefix(HOME_DIR));
            ArgumentSet.ArgEntry homeArg = arguments.getArg(HOME_DIR);
            String home = null;
            if (null != homeArg) {
                home = homeArg.getValue();
            }

            new KVDBServer(new KVDBServerContext(new ServerConfig(home))).start();
        } catch (Exception e) {
            LOGGER.error("server start failed!", e);
        }
    }

}

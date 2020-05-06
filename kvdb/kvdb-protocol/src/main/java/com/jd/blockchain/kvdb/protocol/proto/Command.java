package com.jd.blockchain.kvdb.protocol.proto;

import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.kvdb.protocol.Constants;
import com.jd.blockchain.utils.Bytes;

/**
 * Command from client
 */
@DataContract(code = Constants.COMMAND)
public interface Command extends MessageContent {

    enum CommandType {
        CREATE_DATABASE("create database", false),
        DISABLE_DATABASE("disable database", false),
        ENABLE_DATABASE("enable database", false),
        DROP_DATABASE("drop database", false),
        CLUSTER_INFO("cluster", true),
        SHOW_DATABASES("show databases", false),
        USE("use", true),
        PUT("put", true),
        GET("get", true),
        EXISTS("exists", true),
        BATCH_BEGIN("batch begin", true),
        BATCH_ABORT("batch abort", true),
        BATCH_COMMIT("batch commit", true),
        UNKNOWN("unknown", true);

        // 操作名称
        String command;
        // 是否所有端口开放，false表示仅对本地管理工具开放
        boolean open;

        CommandType(String command, boolean open) {
            this.command = command;
            this.open = open;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public boolean isOpen() {
            return open;
        }

        public static CommandType getCommand(String command) {
            for (CommandType ct : CommandType.values()) {
                if (ct.command.equals(command)) {
                    return ct;
                }
            }

            return UNKNOWN;
        }
    }

    /**
     * @return 命令名称
     */
    @DataField(order = 0, primitiveType = PrimitiveType.TEXT)
    String getName();

    /**
     * @return 参数列表
     */
    @DataField(order = 1, list = true, primitiveType = PrimitiveType.BYTES)
    Bytes[] getParameters();

}

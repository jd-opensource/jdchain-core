package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.kvdb.KVDBInstance;
import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.server.Request;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.io.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 存在性查询
 */
public class ExistsExecutor implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistsExecutor.class);

    @Override
    public Message execute(Request request) {

        try {
            KVDBInstance db = request.getSession().getDBInstance();
            if (null == db) {
                return KVDBMessage.error(request.getId(), "no database selected");
            }
            boolean batch = request.getSession().batchMode();
            Bytes[] keys = request.getCommand().getParameters();
            Bytes[] values = new Bytes[keys.length];
            for (int i = 0; i < keys.length; i++) {
                final Bytes key = keys[i];
                byte[] value;
                if (!batch) {
                    // 因mayExists不一定准确，此处使用rocksdb的get方法判断
                    value = db.get(key.toBytes());
                } else {
                    value = request.getSession().readInBatch((wb) -> wb.get(key));
                    if (null == value) {
                        value = db.get(key.toBytes());
                    }
                }
                LOGGER.debug("execute exists, key:{}, value:{}", BytesUtils.toString(keys[i].toBytes()), value);
                values[i] = null != value ? Bytes.fromInt(1) : Bytes.fromInt(0);
            }
            return KVDBMessage.success(request.getId(), values);
        } catch (Exception e) {
            LOGGER.error("execute exists error", e);
            return KVDBMessage.error(request.getId(), e.toString());
        }
    }
}

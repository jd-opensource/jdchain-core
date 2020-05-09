package com.jd.blockchain.kvdb.server.executor;

import com.jd.blockchain.kvdb.KVDBInstance;
import com.jd.blockchain.kvdb.KVWriteBatch;
import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.kvdb.server.Request;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.io.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 设置键值对
 */
public class PutExecutor implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutExecutor.class);

    @Override
    public Message execute(Request request) {

        KVDBInstance db = request.getSession().getDBInstance();
        if (null == db) {
            return KVDBMessage.error(request.getId(), "no database selected");
        }

        boolean batch = request.getSession().batchMode();
        Bytes[] kvs = request.getCommand().getParameters();
        if (kvs.length % 2 != 0) {
            return KVDBMessage.error(request.getId(), "keys and values must in pairs");
        }

        if (batch) {
            // 批处理模式
            try {
                for (int i = 0; i < kvs.length; ) {
                    LOGGER.debug("execute put in batch, key:{}, value:{}", BytesUtils.toString(kvs[i].toBytes()), kvs[i + 1].toBytes());
                    final Bytes key = kvs[i];
                    final Bytes value = kvs[i + 1];
                    request.getSession().writeInBatch((wb) -> wb.put(key, value.toBytes()));
                    i = i + 2;
                }
            } catch (Exception e) {
                LOGGER.debug("execute put error", e);
                return KVDBMessage.error(request.getId(), e.toString());
            }
        } else {
            if (kvs.length == 2) {
                // 单个键值对
                try {
                    LOGGER.debug("execute put, key:{}, value:{}", BytesUtils.toString(kvs[0].toBytes()), kvs[1].toBytes());
                    db.set(kvs[0].toBytes(), kvs[1].toBytes());
                } catch (Exception e) {
                    LOGGER.debug("execute put error", e);
                    return KVDBMessage.error(request.getId(), e.toString());
                }
            } else {
                // 多个键值对
                KVWriteBatch wb = db.beginBatch();
                try {
                    for (int i = 0; i < kvs.length; ) {
                        LOGGER.debug("execute put, key:{}, value:{}", BytesUtils.toString(kvs[i].toBytes()), kvs[i + 1].toBytes());
                        wb.set(kvs[i].toBytes(), kvs[i + 1].toBytes());
                        i = i + 2;
                    }
                    wb.commit();
                } catch (Exception e) {
                    LOGGER.debug("execute put error", e);
                    return KVDBMessage.error(request.getId(), e.toString());
                }
            }
        }

        return KVDBMessage.success(request.getId());
    }
}

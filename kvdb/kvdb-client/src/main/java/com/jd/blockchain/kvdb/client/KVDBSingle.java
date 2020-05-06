package com.jd.blockchain.kvdb.client;

import com.jd.blockchain.kvdb.protocol.Constants;
import com.jd.blockchain.kvdb.protocol.client.NettyClient;
import com.jd.blockchain.kvdb.protocol.exception.KVDBException;
import com.jd.blockchain.kvdb.protocol.exception.KVDBTimeoutException;
import com.jd.blockchain.kvdb.protocol.proto.Message;
import com.jd.blockchain.kvdb.protocol.proto.Response;
import com.jd.blockchain.kvdb.protocol.proto.impl.KVDBMessage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.io.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单实例数据库数据操作
 */
public class KVDBSingle implements KVDBOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KVDBSingle.class);

    // 当前数据库连接
    private final NettyClient client;

    public KVDBSingle(NettyClient client) {
        this.client = client;
    }

    private Response send(Message message) throws KVDBException {
        return client.send(message);
    }

    @Override
    public boolean exists(Bytes key) throws KVDBException {
        Response response = send(KVDBMessage.exists(key));
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return BytesUtils.toInt(response.getResult()[0].toBytes()) == 1;
    }

    @Override
    public boolean[] exists(Bytes... keys) throws KVDBException {
        Response response = send(KVDBMessage.exists(keys));
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        boolean[] results = new boolean[keys.length];
        for (int i = 0; i < keys.length; i++) {
            results[i] = BytesUtils.toInt(response.getResult()[i].toBytes()) == 1;
        }

        return results;
    }

    @Override
    public Bytes get(Bytes key) throws KVDBException {
        Response response = send(KVDBMessage.get(key));
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return response.getResult()[0];
    }

    @Override
    public Bytes[] get(Bytes... keys) throws KVDBException {
        Response response = send(KVDBMessage.get(keys));
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return response.getResult();
    }

    @Override
    public boolean put(Bytes key, Bytes value) throws KVDBException {
        Response response = send(KVDBMessage.put(key, value));
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return true;
    }

    @Override
    public boolean batchBegin() throws KVDBException {
        Response response = send(KVDBMessage.batchBegin());
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return true;
    }

    @Override
    public boolean batchAbort() throws KVDBException {
        Response response = send(KVDBMessage.batchAbort());
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return true;
    }

    @Override
    public boolean batchCommit() throws KVDBException {
        Response response = send(KVDBMessage.batchCommit());
        if (null == response) {
            throw new KVDBTimeoutException("time out");
        } else if (response.getCode() == Constants.ERROR) {
            throw new KVDBException(BytesUtils.toString(response.getResult()[0].toBytes()));
        }

        return true;
    }
}

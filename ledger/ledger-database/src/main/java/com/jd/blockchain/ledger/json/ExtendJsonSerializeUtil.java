package com.jd.blockchain.ledger.json;

import com.jd.blockchain.web.serializes.ByteArrayJsonDeserializer;
import com.jd.blockchain.web.serializes.ByteArrayJsonSerializer;
import com.jd.blockchain.web.serializes.ExtendJsonDeserializer;
import com.jd.blockchain.web.serializes.ExtendJsonSerializer;

import utils.serialize.json.JSONSerializeUtils;

/**
 * 扩展JSON序列化/反序列化工具
 *
 * @author shaozhuguang
 *
 */
public class ExtendJsonSerializeUtil {

    /**
     * 初始化class对应的序列化/反序列化处理机制
     *
     * @param clazz
     * @param jsonSerializer
     * @param jsonDeserializer
     */
    public static void init(Class<?> clazz, ExtendJsonSerializer jsonSerializer, ExtendJsonDeserializer jsonDeserializer) {
        JSONSerializeUtils.configSerialization(clazz,
                ByteArrayJsonSerializer.create(clazz, jsonSerializer),
                ByteArrayJsonDeserializer.create(clazz, jsonDeserializer));
    }
}

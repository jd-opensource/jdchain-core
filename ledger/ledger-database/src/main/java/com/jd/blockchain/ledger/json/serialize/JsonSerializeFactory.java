package com.jd.blockchain.ledger.json.serialize;

import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.web.serializes.ExtendJsonDeserializer;
import com.jd.blockchain.web.serializes.ExtendJsonSerializer;

/**
 * JSON序列化工厂
 *
 * @author shaozhuguang
 *
 */
public class JsonSerializeFactory {

    /**
     * 创建指定类对应的扩展序列化处理接口
     *
     * @param clazz
     * @return
     */
    public static ExtendJsonSerializer createSerializer(Class<?> clazz) {
        if (clazz == CryptoSetting.class) {
            return new CryptoSettingJsonSerializer();
        }
        return null;
    }

    /**
     * 创建指定类对应的扩展反序列化接口
     *
     * @param clazz
     * @return
     */
    public static ExtendJsonDeserializer createDeserializer(Class<?> clazz) {
        if (clazz == CryptoSetting.class) {
            return new CryptoSettingJsonDeserializer();
        }
        return null;
    }
}

package com.jd.blockchain.ledger.json.serialize;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.json.CryptoConfigInfo;
import com.jd.blockchain.web.serializes.ExtendJsonSerializer;

import java.lang.reflect.Type;

/**
 * CryptoSetting序列化处理类
 *
 * @author shaozhuguang
 *
 */
public class CryptoSettingJsonSerializer implements ExtendJsonSerializer {

    @Override
    public void write(Class<?> clazz, JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) {
        if (object instanceof CryptoSetting) {
            serializer.write(convert((CryptoSetting) object));
        }
    }

    /**
     * 将cryptoSetting转换为CryptoConfigInfo
     *
     * @param cryptoSetting
     * @return
     */
    private CryptoConfigInfo convert(CryptoSetting cryptoSetting) {
        return new CryptoConfigInfo(cryptoSetting);
    }
}

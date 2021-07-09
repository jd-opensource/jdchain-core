package com.jd.blockchain.ledger.json;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.JavaBeanSerializer;
import com.jd.blockchain.ledger.CryptoSetting;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * CryptoSetting 序列化
 */
public class CryptoSettingSerializer extends JavaBeanSerializer {

    public static final CryptoSettingSerializer INSTANCE = new CryptoSettingSerializer(CryptoSetting.class);

    public CryptoSettingSerializer(Class<?> beanType) {
        super(beanType);
    }

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        if (null != object) {
            serializer.write(new CryptoConfigInfo((CryptoSetting) object));
        } else {
            serializer.writeNull();
        }
    }

}

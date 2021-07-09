package com.jd.blockchain.ledger.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.JavaBeanDeserializer;
import com.jd.blockchain.ledger.CryptoSetting;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * CryptoSetting 反序列化
 */
public class CryptoSettingDeserializer extends JavaBeanDeserializer {

    public static final CryptoSettingDeserializer INSTANCE = new CryptoSettingDeserializer(CryptoSetting.class);

    public CryptoSettingDeserializer(Class<?> clazz) {
        super(ParserConfig.global, clazz);
    }

    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        String parseText = parser.parseObject(String.class);
        // 将JSON转换为具体的对象
        return (T) JSON.parseObject(parseText, CryptoConfigInfo.class);
    }

    @Override
    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }

    @Override
    public Object createInstance(Map<String, Object> map, ParserConfig config) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                return JSON.parseObject((String) value, CryptoConfigInfo.class);
            }
        }
        return null;
    }
}

package com.jd.blockchain.ledger.json.serialize;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.ParserConfig;
import com.jd.blockchain.ledger.json.CryptoConfigInfo;
import com.jd.blockchain.web.serializes.ExtendJsonDeserializer;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * CryptoSetting反序列化处理类
 *
 * @author shaozhuguang
 *
 */
public class CryptoSettingJsonDeserializer implements ExtendJsonDeserializer {

    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        // 将JSON字符串转换为具体的对象
        String parseText = parser.parseObject(String.class);
        // 将JSON转换为具体的对象
        return (T) convert(parseText);
    }

    @Override
    public Object createInstance(Map<String, Object> map, ParserConfig config) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                return convert(value.toString());
            }
        }
        return null;
    }

    /**
     * 字符串转换为CryptoSetting具体实现类
     *
     * @param parseText
     * @return
     */
    private CryptoConfigInfo convert(String parseText) {
        return JSON.parseObject(parseText, CryptoConfigInfo.class);
    }
}
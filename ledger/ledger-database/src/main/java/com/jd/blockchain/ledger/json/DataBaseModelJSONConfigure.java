package com.jd.blockchain.ledger.json;

import com.jd.blockchain.ledger.CryptoSetting;
import utils.serialize.json.JSONAutoConfigure;
import utils.serialize.json.JSONConfigurator;

public class DataBaseModelJSONConfigure implements JSONAutoConfigure {

    @Override
    public void configure(JSONConfigurator configurator) {
        // CryptoSetting
        configurator.configSerializer(CryptoSetting.class, CryptoSettingSerializer.INSTANCE);
        configurator.configDeserializer(CryptoSetting.class, CryptoSettingDeserializer.INSTANCE);
    }

}

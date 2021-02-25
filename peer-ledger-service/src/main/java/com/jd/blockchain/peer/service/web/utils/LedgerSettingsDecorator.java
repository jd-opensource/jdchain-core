package com.jd.blockchain.peer.service.web.utils;

import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.LedgerSettings;
import com.jd.blockchain.ledger.json.CryptoConfigInfo;

import utils.Bytes;

public class LedgerSettingsDecorator implements LedgerSettings {

    private String consensusProvider;

    private Bytes consensusSetting;

    private CryptoSetting cryptoSetting;

    public LedgerSettingsDecorator(LedgerSettings ledgerSettings) {
        this.consensusProvider = ledgerSettings.getConsensusProvider();
        this.consensusSetting = ledgerSettings.getConsensusSetting();
        this.cryptoSetting = new CryptoConfigInfo(ledgerSettings.getCryptoSetting());
    }

    @Override
    public String getConsensusProvider() {
        return consensusProvider;
    }

    @Override
    public Bytes getConsensusSetting() {
        return consensusSetting;
    }

    @Override
    public CryptoSetting getCryptoSetting() {
        return cryptoSetting;
    }
}

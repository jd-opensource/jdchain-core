package com.jd.blockchain.ledger.json;

import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.CryptoProvider;
import com.jd.blockchain.ledger.CryptoSetting;

/**
 * CryptoSetting实现类
 * 用于对该类进行序列化/反序列化操作
 *
 * @author shaozhuguang
 *
 */
public class CryptoConfigInfo implements CryptoSetting {

    private CryptoProviderInfo[] supportedProviders;

    private short hashAlgorithm;

    private boolean autoVerifyHash;

    public CryptoConfigInfo() {
    }

    public CryptoConfigInfo(CryptoSetting setting) {
        copyCryptoProviders(setting.getSupportedProviders());
        this.hashAlgorithm = setting.getHashAlgorithm();
        this.autoVerifyHash = setting.getAutoVerifyHash();
    }

    private void copyCryptoProviders(CryptoProvider[] cps) {
        if (cps == null || cps.length == 0) {
            throw new IllegalArgumentException("CryptoProviders is empty !!!");
        }
        supportedProviders = new CryptoProviderInfo[cps.length];
        for (int i = 0; i < cps.length; i++) {
            // 生成
            CryptoProvider cp = cps[i];
            CryptoAlgorithm[] cpAlgorithms = cp.getAlgorithms();
            if (cpAlgorithms == null || cpAlgorithms.length == 0) {
                throw new IllegalArgumentException("CryptoAlgorithm is empty !!!");
            }
            CryptoAlgorithmInfo[] cryptoAlgorithmInfos = new CryptoAlgorithmInfo[cpAlgorithms.length];
            for (int j = 0; j < cpAlgorithms.length; j++) {
                cryptoAlgorithmInfos[i] = new CryptoAlgorithmInfo(cpAlgorithms[i]);
            }
            CryptoProviderInfo cryptoProviderInfo = new CryptoProviderInfo(cp.getName(), cryptoAlgorithmInfos);
            supportedProviders[i] = cryptoProviderInfo;
        }
    }

    @Override
    public CryptoProvider[] getSupportedProviders() {
        return supportedProviders == null ? null : supportedProviders.clone();
    }

    @Override
    public short getHashAlgorithm() {
        return hashAlgorithm;
    }

    @Override
    public boolean getAutoVerifyHash() {
        return autoVerifyHash;
    }

    public void setSupportedProviders(CryptoProviderInfo[] supportedProviders) {
        this.supportedProviders = supportedProviders;
    }

    public void setHashAlgorithm(short hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public boolean isAutoVerifyHash() {
        return autoVerifyHash;
    }

    public void setAutoVerifyHash(boolean autoVerifyHash) {
        this.autoVerifyHash = autoVerifyHash;
    }

    public static class CryptoProviderInfo implements CryptoProvider {

        private String name;

        private CryptoAlgorithmInfo[] algorithms;

        public CryptoProviderInfo() {
        }

        public CryptoProviderInfo(String name, CryptoAlgorithmInfo[] algorithms) {
            this.name = name;
            this.algorithms = algorithms;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAlgorithms(CryptoAlgorithmInfo[] algorithms) {
            this.algorithms = algorithms;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public CryptoAlgorithm[] getAlgorithms() {
            return algorithms.clone();
        }

    }

    public static class CryptoAlgorithmInfo implements CryptoAlgorithm {

        private short code;

        private String name;

        public CryptoAlgorithmInfo() {
        }

        public CryptoAlgorithmInfo(CryptoAlgorithm cryptoAlgorithm) {
            this.code = cryptoAlgorithm.code();
            this.name = cryptoAlgorithm.name();
        }

        @Override
        public short code() {
            return this.code;
        }

        @Override
        public String name() {
            return name;
        }

        public short getCode() {
            return code;
        }

        public void setCode(short code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

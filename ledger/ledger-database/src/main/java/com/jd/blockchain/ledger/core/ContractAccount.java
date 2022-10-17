package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.cache.ContractCache;
import utils.Bytes;
import utils.io.BytesUtils;

public class ContractAccount extends PermissionAccountDecorator implements ContractInfo {

    private static final String CONTRACT_INFO_PREFIX = "INFO" + LedgerConsts.KEY_SEPERATOR;

    private static final String CHAIN_CODE_KEY = "C-CODE";
    private static final String DATA_STATE = "D-ST";
    private static final String DATA_LANG = "D-LG";

    private AccountState state;
    private ContractLang lang;

    public ContractAccount(CompositeAccount mklAccount, ContractCache cache) {
        super(AccountType.CONTRACT, mklAccount, cache);
    }

    @Override
    public Bytes getAddress() {
        return getID().getAddress();
    }

    @Override
    public PubKey getPubKey() {
        return getID().getPubKey();
    }

    public long setChaincode(byte[] chaincode, long version) {
        TypedValue bytesValue = TypedValue.fromBytes(chaincode);
        return getHeaders().setValue(CHAIN_CODE_KEY, bytesValue, version);
    }

    public byte[] getChainCode() {
        return getHeaders().getValue(CHAIN_CODE_KEY).getBytes().toBytes();
    }

    public byte[] getChainCode(long version) {
        return getHeaders().getValue(CHAIN_CODE_KEY, version).getBytes().toBytes();
    }

    public long getChainCodeVersion() {
        return getHeaders().getVersion(CHAIN_CODE_KEY);
    }

    public void setState(AccountState state) {
        long version = getHeaders().getVersion(DATA_STATE);
        getHeaders().setValue(DATA_STATE, TypedValue.fromText(state.name()), version);
        this.state = state;
    }

    public void setLang(ContractLang lang) {
        if (null == lang) {
            lang = ContractLang.Java;
        }
        long version = getHeaders().getVersion(DATA_LANG);
        getHeaders().setValue(DATA_LANG, TypedValue.fromText(lang.name()), version);
        this.lang = lang;
    }

    @Override
    public AccountState getState() {
        if (state == null) {
            BytesValue rbs = getHeaders().getValue(DATA_STATE);
            if (rbs == null) {
                state = AccountState.NORMAL;
            } else {
                state = AccountState.valueOf(BytesUtils.toString(rbs.getBytes().toBytes()));
            }
        }
        return state;
    }

    @Override
    public ContractLang getLang() {
        if (lang == null) {
            BytesValue rbs = getHeaders().getValue(DATA_LANG);
            if (rbs == null) {
                lang = ContractLang.Java;
            } else {
                lang = ContractLang.valueOf(BytesUtils.toString(rbs.getBytes().toBytes()));
            }
        }
        return lang;
    }

    public long setProperty(String key, String value, long version) {
        TypedValue bytesValue = TypedValue.fromText(value);
        return getHeaders().setValue(encodePropertyKey(key), bytesValue, version);
    }

    public String getProperty(String key) {
        BytesValue bytesValue = getHeaders().getValue(encodePropertyKey(key));
        return TypedValue.wrap(bytesValue).stringValue();
    }

    public String getProperty(String key, long version) {
        BytesValue bytesValue = getHeaders().getValue(encodePropertyKey(key), version);
        return TypedValue.wrap(bytesValue).stringValue();
    }

    private String encodePropertyKey(String key) {
        return CONTRACT_INFO_PREFIX.concat(key);
    }

}
package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.AccountType;
import com.jd.blockchain.ledger.DataAccountInfo;
import com.jd.blockchain.ledger.cache.DataAccountCache;
import utils.Bytes;

public class DataAccount extends PermissionAccountDecorator implements DataAccountInfo {

    public DataAccount(CompositeAccount mklAccount) {
        super(AccountType.DATA, mklAccount);
    }

    public DataAccount(CompositeAccount mklAccount, DataAccountCache cache) {
        super(AccountType.DATA, mklAccount, cache);
    }

    @Override
    public Bytes getAddress() {
        return getID().getAddress();
    }

    @Override
    public PubKey getPubKey() {
        return getID().getPubKey();
    }

}
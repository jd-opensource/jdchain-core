package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.AccountType;
import com.jd.blockchain.ledger.DataAccountInfo;
import utils.Bytes;

public class DataAccount extends PermissionAccountDecorator implements DataAccountInfo {

    public DataAccount(CompositeAccount mklAccount) {
        super(AccountType.DATA, mklAccount);
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
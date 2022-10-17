package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.ledger.AccountDataPermission;
import com.jd.blockchain.ledger.AccountModeBits;
import com.jd.blockchain.ledger.AccountType;
import com.jd.blockchain.ledger.DataPermission;
import com.jd.blockchain.ledger.PermissionAccount;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.ledger.cache.PermissionCache;
import utils.Bytes;

public class PermissionAccountDecorator extends AccountDecorator implements PermissionAccount {

    private static final String KEY_PERMISSION = "PERMISSION";
    private AccountType accountType;
    private PermissionCache cache;

    public PermissionAccountDecorator(AccountType accountType, CompositeAccount mklAccount) {
        super(mklAccount);
        this.accountType = accountType;
    }

    public PermissionAccountDecorator(AccountType accountType, CompositeAccount mklAccount, PermissionCache cache) {
        super(mklAccount);
        this.accountType = accountType;
        this.cache = cache;
    }

    @Override
    public DataPermission getPermission() {
        DataPermission permission = null;
        if(null != cache) {
            permission = cache.getPermission(getID().getAddress());
        }
        if (null == permission) {
            TypedValue ptv = getHeaders().getValue(KEY_PERMISSION);
            if (null != ptv && !ptv.isNil()) {
                DataPermission dp = BinaryProtocol.decode(ptv.bytesValue());
                permission = new AccountDataPermission(dp.getModeBits(), dp.getOwners(), dp.getRole());
            } else {
                permission = new AccountDataPermission(accountType, new Bytes[]{});
            }
            if(null != cache) {
                cache.setPermission(getID().getAddress(), permission);
            }
        }

        return permission;
    }

    @Override
    public void setPermission(DataPermission permission) {
        long version = getHeaders().getVersion(KEY_PERMISSION);
        getHeaders().setValue(KEY_PERMISSION, TypedValue.fromBytes(BinaryProtocol.encode(permission)), version);
        if(null != cache) {
            cache.setPermission(getID().getAddress(), permission);
        }
    }

    @Override
    public void setModeBits(AccountModeBits modeBits) {
        DataPermission permission = getPermission();
        if (null == permission) {
            permission = new AccountDataPermission(modeBits, null, null);
        } else {
            permission = new AccountDataPermission(modeBits, permission.getOwners(), permission.getRole());
        }
        setPermission(permission);
    }

    @Override
    public void setRole(String role) {
        DataPermission permission = getPermission();
        if (null == permission) {
            permission = new AccountDataPermission(new AccountModeBits(accountType), null, role);
        } else {
            permission = new AccountDataPermission(new AccountModeBits(accountType), permission.getOwners(), role);
        }
        setPermission(permission);
    }
}

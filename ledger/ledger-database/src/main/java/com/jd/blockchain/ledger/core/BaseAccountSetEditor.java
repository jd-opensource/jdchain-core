package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.PubKey;
import utils.Bytes;
import utils.Transactional;

import java.util.Map;

/**
 * @Author: zhangshuang
 * @Date: 2022/3/28 8:03 PM
 * Version 1.0
 */
public interface BaseAccountSetEditor extends Transactional, BaseAccountSet<CompositeAccount> {
    boolean isReadonly();

    CompositeAccount register(Bytes address, PubKey pubKey);

    long getVersion(Bytes address);

    boolean isAddNew(); // used only by kv ledger structure

    void clearCachedIndex(); // used only by kv ledger structure

    Map<Bytes, Long> getKvNumCache(); // used only by kv ledger structure

    void updatePreBlockHeight(long newBlockHeight); // used only by kv ledger structure

}

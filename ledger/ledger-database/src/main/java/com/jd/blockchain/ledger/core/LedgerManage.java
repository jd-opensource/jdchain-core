package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.storage.service.KVStorageService;

/**
 * 账本管理器；
 * 
 * @author huanghaiquan
 *
 */
public interface LedgerManage extends LedgerService {
	
	static final String LEDGER_PREFIX = "LDG://";
	
	LedgerQuery register(HashDigest ledgerHash, KVStorageService storageService, String anchorType);
	
	void unregister(HashDigest ledgerHash);


}
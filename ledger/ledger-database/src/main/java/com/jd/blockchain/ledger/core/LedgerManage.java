package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.storage.service.KVStorageService;

/**
 * 账本管理器；
 * 
 * @author huanghaiquan
 *
 */
public interface LedgerManage extends LedgerService {
	
	static final String LEDGER_PREFIX = "L:/";
	
	LedgerQuery register(HashDigest ledgerHash, KVStorageService storageService, KVStorageService archiveStorageService, LedgerDataStructure dataStructure);
	
	void unregister(HashDigest ledgerHash);


}
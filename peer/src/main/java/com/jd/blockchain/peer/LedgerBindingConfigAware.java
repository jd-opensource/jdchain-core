package com.jd.blockchain.peer;

import com.jd.blockchain.consensus.service.NodeServer;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.tools.initializer.LedgerBindingConfig;
import com.jd.blockchain.tools.initializer.LedgerBindingConfig.BindingConfig;

public interface LedgerBindingConfigAware {
	
	void setConfig(LedgerBindingConfig config);

	NodeServer setConfig(BindingConfig bindingConfig, HashDigest ledgerHash);
}

package com.jd.blockchain.peer.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jd.blockchain.ledger.core.DefaultOperationHandleRegisteration;
import com.jd.blockchain.ledger.core.LedgerManager;
import com.jd.blockchain.ledger.core.OperationHandleRegisteration;
import com.jd.blockchain.ledger.core.TransactionEngineImpl;
import com.jd.blockchain.runtime.RuntimeContext;
import com.jd.blockchain.service.TransactionEngine;

import utils.io.Storage;

@Configuration
public class ComponentsConfigurer {

	@ConditionalOnMissingBean
	@Bean
	public LedgerManager ledgerManager() {
		return new LedgerManager();
	}
	
	@Bean
	public TransactionEngine transactionEngine() {
		return new TransactionEngineImpl();
	}
	
	@Bean
	public OperationHandleRegisteration operationHandleRegisteration() {
		return new DefaultOperationHandleRegisteration();
	}
	
	@Bean
	public Storage runtimeStorage() {
		return RuntimeContext.get().getEnvironment().getRuntimeStorage();
	}
}

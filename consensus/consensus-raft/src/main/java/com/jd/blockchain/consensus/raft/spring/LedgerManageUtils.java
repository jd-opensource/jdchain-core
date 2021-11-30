package com.jd.blockchain.consensus.raft.spring;

import com.jd.blockchain.ledger.core.LedgerManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class LedgerManageUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static LedgerManager getLedgerManager() {
        return applicationContext.getBean(LedgerManager.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}

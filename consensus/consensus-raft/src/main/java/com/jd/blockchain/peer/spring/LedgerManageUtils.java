package com.jd.blockchain.peer.spring;


import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.core.LedgerManager;
import com.jd.blockchain.ledger.core.LedgerRepository;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LedgerManageUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static LedgerManager getLedgerManager() {
        return applicationContext.getBean(LedgerManager.class);
    }

    public static LedgerRepository getLedgerRepository(HashDigest ledgerHash) {
        return getLedgerManager().getLedger(ledgerHash);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}

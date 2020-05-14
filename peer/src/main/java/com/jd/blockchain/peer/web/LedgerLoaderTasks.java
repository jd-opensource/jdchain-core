/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.peer.web.ScheduledTasks
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2019/1/7 上午11:12
 * Description:
 */
package com.jd.blockchain.peer.web;

import com.jd.blockchain.consensus.service.NodeServer;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.core.LedgerManage;
import com.jd.blockchain.peer.ConsensusManage;
import com.jd.blockchain.peer.LedgerBindingConfigAware;
import com.jd.blockchain.peer.PeerServerBooter;
import com.jd.blockchain.tools.initializer.LedgerBindingConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import static com.jd.blockchain.peer.PeerServerBooter.LEDGER_BIND_CONFIG_NAME;

/**
 *
 * @author shaozhuguang
 * @create 2019/1/7
 * @since 1.0.0
 */
@Component
@EnableScheduling
public class LedgerLoaderTasks implements ApplicationContextAware {

    private static Logger LOGGER = LoggerFactory.getLogger(LedgerLoaderTasks.class);

    private ApplicationContext applicationContext;

    @Autowired
    private LedgerManage ledgerManager;

    //每1分钟执行一次
    @Scheduled(cron = "0 */5 * * * * ")
    public void updateLedger(){

        LOGGER.debug("--- Ledger loader tasks start... ");
        try {
            LedgerBindingConfig ledgerBindingConfig = loadLedgerBindingConfig();
            if (ledgerBindingConfig == null) {
                // print debug
                LOGGER.error("Can not load any ledgerBindingConfigs !!!");
                return;
            }

            HashDigest[] totalLedgerHashs = ledgerBindingConfig.getLedgerHashs();
            HashDigest[] existingLedgerHashs = ledgerManager.getLedgerHashs();

            Set<HashDigest> newAddHashs = new HashSet<>();
            for (HashDigest ledgerHash : totalLedgerHashs) {
                boolean isExist = false;
                for (HashDigest exist : existingLedgerHashs) {
                    if (ledgerHash.equals(exist)) {
                        isExist = true;
                        break;
                    }
                }
                if (!isExist) {
                    newAddHashs.add(ledgerHash);
                }
            }
            if (!newAddHashs.isEmpty()) {
                // 建立共识网络；
                Map<String, LedgerBindingConfigAware> bindingConfigAwares = applicationContext.getBeansOfType(LedgerBindingConfigAware.class);
                List<NodeServer> nodeServers = new ArrayList<>();
                for (HashDigest ledgerHash : newAddHashs) {

                    LOGGER.info("--- New ledger [{}] need to be init... ", ledgerHash.toBase58());
                    for (LedgerBindingConfigAware aware : bindingConfigAwares.values()) {
                        nodeServers.add(aware.setConfig(ledgerBindingConfig.getLedger(ledgerHash), ledgerHash));
                    }
                }
                // 启动指定NodeServer节点
                ConsensusManage consensusManage = applicationContext.getBean(ConsensusManage.class);
                for (NodeServer nodeServer : nodeServers) {
                    consensusManage.runRealm(nodeServer);
                }
            } else {
                LOGGER.debug("All ledgers is newest!!!");
            }
        } catch (Exception e) {
            LOGGER.error("--- Ledger loader execute error !!!", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private LedgerBindingConfig loadLedgerBindingConfig() throws Exception {
        LedgerBindingConfig ledgerBindingConfig = null;
        String ledgerBindConfigFile = PeerServerBooter.ledgerBindConfigFile;
        LOGGER.debug("--- Load " + LEDGER_BIND_CONFIG_NAME + " path = {}",
                ledgerBindConfigFile == null ? "Default" : ledgerBindConfigFile);
        if (ledgerBindConfigFile == null) {
            ClassPathResource configResource = new ClassPathResource(LEDGER_BIND_CONFIG_NAME);
            if (configResource.exists()) {
                try (InputStream in = configResource.getInputStream()) {
                    ledgerBindingConfig = LedgerBindingConfig.resolve(in);
                } catch (Exception e) {
                    throw e;
                }
            }
        } else {
            File file = new File(ledgerBindConfigFile);
            ledgerBindingConfig = LedgerBindingConfig.resolve(file);
        }
        return ledgerBindingConfig;
    }
}
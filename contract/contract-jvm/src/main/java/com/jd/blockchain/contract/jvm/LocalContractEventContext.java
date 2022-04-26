package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.ContractEventContext;
import com.jd.blockchain.contract.LedgerContext;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.runtime.RuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Bytes;
import utils.serialize.json.JSONSerializeUtils;

import java.util.Set;

/**
 * @Author zhaogw
 * @Date 2018/9/5 17:43
 */
public class LocalContractEventContext implements ContractEventContext, Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(LocalContractEventContext.class);
    private HashDigest ledgeHash;
    private Bytes contractAddress;
    private String event;
    private BytesValueList args;
    private TransactionRequest transactionRequest;
    private Set<BlockchainIdentity> txSigners;
    private LedgerContext ledgerContext;
    private long version;
    // 包含未提交区块数据账本查询
    private LedgerQueryService uncommittedLedgerQuery;
    private ContractRuntimeConfig runtimeConfig;

    public LocalContractEventContext(HashDigest ledgeHash, ContractRuntimeConfig runtimeConfig, Bytes contractAddress, String event) {
        this.ledgeHash = ledgeHash;
        this.runtimeConfig = runtimeConfig;
        this.contractAddress = contractAddress;
        this.event = event;
    }

    @Override
    public HashDigest getCurrentLedgerHash() {
        return ledgeHash;
    }

    @Override
    public TransactionRequest getTransactionRequest() {
        return transactionRequest;
    }

    @Override
    public Set<BlockchainIdentity> getTxSigners() {
        return txSigners;
    }

    @Override
    public String getEvent() {
        return event;
    }

    @Override
    public BytesValueList getArgs() {
        return args;
    }

    @Override
    public LedgerContext getLedger() {
        return ledgerContext;
    }

    @Override
    public Set<BlockchainIdentity> getContractOwners() {
        return null;
    }

    public LocalContractEventContext setLedgerHash(HashDigest ledgeHash) {
        this.ledgeHash = ledgeHash;
        return this;
    }

    public LocalContractEventContext setEvent(String event) {
        this.event = event;
        return this;
    }

    public LocalContractEventContext setTransactionRequest(TransactionRequest transactionRequest) {
        this.transactionRequest = transactionRequest;
        return this;
    }

    public LocalContractEventContext setTxSigners(Set<BlockchainIdentity> txSigners) {
        this.txSigners = txSigners;
        return this;
    }

    public LocalContractEventContext setLedgerContext(LedgerContext ledgerContext) {
        this.ledgerContext = ledgerContext;
        return this;
    }

    public LocalContractEventContext setUncommittedLedgerContext(LedgerQueryService uncommittedLedger) {
        this.uncommittedLedgerQuery = uncommittedLedger;
        return this;
    }

    public LocalContractEventContext setArgs(BytesValueList args) {
        this.args = args;
        return this;
    }

    public LocalContractEventContext setVersion(long version) {
        this.version = version;
        return this;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public LedgerQueryService getUncommittedLedger() {
        return uncommittedLedgerQuery;
    }

    @Override
    public Bytes getCurrentContractAddress() {
        return contractAddress;
    }

    @Override
    public ContractRuntimeConfig getContractRuntimeConfig() {
        return runtimeConfig;
    }

    @Override
    public String jsonSerialize(Object obj) {
        boolean enabled = null != RuntimeContext.get().getSecurityManager() && RuntimeContext.get().getSecurityManager().isEnabled();
        if (enabled) {
            try {
                RuntimeContext.disableSecurityManager();
                return JSONSerializeUtils.serializeToJSON(obj);
            } finally {
                RuntimeContext.enableSecurityManager();
            }
        } else {
            return JSONSerializeUtils.serializeToJSON(obj);
        }
    }

    @Override
    public <T> T jsonDeserialize(String json, Class<T> dataClazz) {
        boolean enabled = null != RuntimeContext.get().getSecurityManager() && RuntimeContext.get().getSecurityManager().isEnabled();
        if (enabled) {
            try {
                RuntimeContext.disableSecurityManager();
                return JSONSerializeUtils.deserializeFromJSON(json, dataClazz);
            } finally {
                RuntimeContext.enableSecurityManager();
            }
        } else {
            return JSONSerializeUtils.deserializeFromJSON(json, dataClazz);
        }
    }

    @Override
    public void logInfo(String var1, Object... var2) {
        if (null != RuntimeContext.get().getSecurityManager() && RuntimeContext.get().getSecurityManager().isEnabled()) {
            try {
                RuntimeContext.disableSecurityManager();
                logger.info(var1, var2);
            } finally {
                RuntimeContext.enableSecurityManager();
            }
        }
    }

    @Override
    public void logInfo(String var1, Throwable var2) {
        if (null != RuntimeContext.get().getSecurityManager() && RuntimeContext.get().getSecurityManager().isEnabled()) {
            try {
                RuntimeContext.disableSecurityManager();
                logger.info(var1, var2);
            } finally {
                RuntimeContext.enableSecurityManager();
            }
        }
    }

    @Override
    public void logDebug(String var1, Object... var2) {
        if (null != RuntimeContext.get().getSecurityManager() && RuntimeContext.get().getSecurityManager().isEnabled()) {
            try {
                RuntimeContext.disableSecurityManager();
                logger.debug(var1, var2);
            } finally {
                RuntimeContext.enableSecurityManager();
            }
        }
    }

    @Override
    public void logDebug(String var1, Throwable var2) {
        if (null != RuntimeContext.get().getSecurityManager() && RuntimeContext.get().getSecurityManager().isEnabled()) {
            try {
                RuntimeContext.disableSecurityManager();
                logger.debug(var1, var2);
            } finally {
                RuntimeContext.enableSecurityManager();
            }
        }
    }

    @Override
    public void logError(String var1, Object... var2) {
        if (null != RuntimeContext.get().getSecurityManager() && RuntimeContext.get().getSecurityManager().isEnabled()) {
            try {
                RuntimeContext.disableSecurityManager();
                logger.error(var1, var2);
            } finally {
                RuntimeContext.enableSecurityManager();
            }
        }
    }

    @Override
    public void logError(String var1, Throwable var2) {
        if (null != RuntimeContext.get().getSecurityManager() && RuntimeContext.get().getSecurityManager().isEnabled()) {
            try {
                RuntimeContext.disableSecurityManager();
                logger.error(var1, var2);
            } finally {
                RuntimeContext.enableSecurityManager();
            }
        }
    }

}

package com.jd.blockchain.contract.jvm;

import com.jd.blockchain.contract.ContractEventContext;
import com.jd.blockchain.contract.engine.ContractCode;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.ContractExecuteException;
import com.jd.blockchain.ledger.LedgerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Bytes;

/**
 * @author huanghaiquan
 */
public abstract class AbstractContractCode implements ContractCode {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractContractCode.class);
    private Bytes address;
    private long version;

    public AbstractContractCode(Bytes address, long version) {
        this.address = address;
        this.version = version;
    }

    @Override
    public Bytes getAddress() {
        return address;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public BytesValue processEvent(ContractEventContext eventContext) {
        Object retn = null;
        LedgerException error = null;
        Object contractInstance = null;
        try {
            contractInstance = getContractInstance();
            // 执行预处理;
            beforeEvent(contractInstance, eventContext);

            // 合约方法执行
            retn = doProcessEvent(contractInstance, eventContext);

        } catch (LedgerException e) {
            error = e;
        } catch (Throwable e) {
            String errorMessage = String.format("Error occurred while processing event[%s] of contract[%s]! --%s",
                    eventContext.getEvent(), address.toString(), e.getMessage());
            error = new ContractExecuteException(errorMessage, e);
        }

        try {
            postEvent(contractInstance, eventContext, error);
        } catch (Throwable e) {
            throw new ContractExecuteException("Error occurred while posting contract event!", e);
        }
        if (error != null) {
            throw error;
        }
        return (BytesValue) retn;
    }

    protected abstract Object getContractInstance();

    protected abstract void beforeEvent(Object contractInstance, ContractEventContext eventContext);

    protected abstract BytesValue doProcessEvent(Object contractInstance, ContractEventContext eventContext);

    protected abstract void postEvent(Object contractInstance, ContractEventContext eventContext, LedgerException error);
}

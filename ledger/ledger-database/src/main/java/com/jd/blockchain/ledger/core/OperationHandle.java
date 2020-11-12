package com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.Operation;

public interface OperationHandle {

    /**
     * 是否支持指定类型的操作；
     *
     * @return
     */
    Class<?> getOperationType();

    /**
     * 同步解析和执行操作；
     *
     * @param op              操作实例；
     * @param newBlockDataset 需要修改的新区块的数据集；
     * @param requestContext  交易请求上下文；
     * @param handleContext   操作上下文；
     * @return
     */
    BytesValue process(Operation op, LedgerDataSetEditor newBlockDataset, TransactionRequestExtension requestContext,
                       LedgerQuery ledger, OperationHandleContext handleContext, EventManager manager);

}

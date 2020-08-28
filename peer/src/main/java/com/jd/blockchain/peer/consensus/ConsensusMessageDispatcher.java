package com.jd.blockchain.peer.consensus;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.BlockStateSnapshot;
import com.jd.blockchain.consensus.service.ConsensusContext;
import com.jd.blockchain.consensus.service.ConsensusMessageContext;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.OperationResult;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.core.LedgerEditor;
import com.jd.blockchain.ledger.core.TransactionBatchProcessor;
import com.jd.blockchain.ledger.core.TransactionEngineImpl;
import com.jd.blockchain.service.TransactionBatchProcess;
import com.jd.blockchain.service.TransactionBatchResultHandle;
import com.jd.blockchain.service.TransactionEngine;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.concurrent.AsyncFuture;
import com.jd.blockchain.utils.concurrent.CompletableAsyncFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.StateSnapshot;
import com.jd.blockchain.crypto.HashDigest;

import javax.swing.plaf.nimbus.State;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author huanghaiquan
 *
 */
@Component
public class ConsensusMessageDispatcher implements MessageHandle {

	@Autowired
	private TransactionEngine txEngine;

	// todo 可能存在内存溢出的问题
	private final Map<String, RealmProcessor> realmProcessorMap = new ConcurrentHashMap<>();

	private final ReentrantLock beginLock = new ReentrantLock();

	//Used by mocked integration test example
	public void setTxEngine(TransactionEngine txEngine) {
		this.txEngine = txEngine;
	}

	@Override
	public String beginBatch(ConsensusContext consensusContext) {
		String realmName = realmName(consensusContext);
		RealmProcessor realmProcessor = realmProcessorMap.get(realmName);
		if (realmProcessor == null) {
			beginLock.lock();
			try {
				realmProcessor = realmProcessorMap.get(realmName);
				if (realmProcessor == null) {
					realmProcessor = initRealmProcessor(realmName);
					realmProcessorMap.put(realmName, realmProcessor);
				}
			} finally {
				beginLock.unlock();
			}
		}
		return realmProcessor.newBatchId();
	}

	@Override
	public StateSnapshot getStateSnapshot(ConsensusContext consensusContext) {
		RealmProcessor realmProcessor = realmProcessorMap.get(realmName(consensusContext));
		if (realmProcessor == null) {
			throw new IllegalArgumentException("RealmName is not init!");
		}
		return realmProcessor.getStateSnapshot();
	}

	@Override
	public StateSnapshot getGenesisStateSnapshot(ConsensusContext consensusContext) {
		RealmProcessor realmProcessor = realmProcessorMap.get(realmName(consensusContext));
		if (realmProcessor == null) {
			throw new IllegalArgumentException("RealmName is not init!");
		}
		return realmProcessor.getGenesisStateSnapshot();
	}

	@Override
	public AsyncFuture<byte[]> processOrdered(int messageId, byte[] message, ConsensusMessageContext context) {
		// TODO 要求messageId在同一个批次不重复，但目前暂不验证
		RealmProcessor realmProcessor = realmProcessorMap.get(realmName(context));
		if (realmProcessor == null) {
			throw new IllegalArgumentException("RealmName is not init!");
		}
		if (!realmProcessor.getCurrBatchId().equalsIgnoreCase(batchId(context))) {
			throw new IllegalArgumentException("BatchId is not begin!");
		}
		TransactionRequest txRequest = BinaryProtocol.decode(message);
		return realmProcessor.schedule(txRequest);
	}

	@Override
	public StateSnapshot completeBatch(ConsensusMessageContext context) {
		RealmProcessor realmProcessor = realmProcessorMap.get(realmName(context));
		if (realmProcessor == null) {
			throw new IllegalArgumentException("RealmName is not init!");
		}
		if (!realmProcessor.getCurrBatchId().equalsIgnoreCase(batchId(context))) {
			throw new IllegalArgumentException("BatchId is not begin!");
		}
		return realmProcessor.complete(context.getTimestamp());
	}

	@Override
	public void commitBatch(ConsensusMessageContext context) {
		RealmProcessor realmProcessor = realmProcessorMap.get(realmName(context));
		if (realmProcessor == null) {
			throw new IllegalArgumentException("RealmName is not init!");
		}
		if (!realmProcessor.getCurrBatchId().equalsIgnoreCase(batchId(context))) {
			throw new IllegalArgumentException("BatchId is not begin!");
		}

		realmProcessor.commit();
	}

	@Override
	public void rollbackBatch(int reasonCode, ConsensusMessageContext context) {
		RealmProcessor realmProcessor = realmProcessorMap.get(realmName(context));
		if (realmProcessor == null) {
			throw new IllegalArgumentException("RealmName is not init!");
		}
		if (!realmProcessor.getCurrBatchId().equalsIgnoreCase(batchId(context))) {
			throw new IllegalArgumentException("BatchId is not begin!");
		}
		realmProcessor.rollback(reasonCode);
	}

	@Override
	public AsyncFuture<byte[]> processUnordered(byte[] message) {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("Not implemented!");
	}

	private RealmProcessor initRealmProcessor(String realmName) {
		RealmProcessor realmProcessor = new RealmProcessor();
		byte[] hashBytes = Base58Utils.decode(realmName);
		HashDigest ledgerHash = new HashDigest(hashBytes);
		realmProcessor.realmName = realmName;
		realmProcessor.ledgerHash = ledgerHash;
		return realmProcessor;
	}

	private String realmName(ConsensusContext consensusContext) {
		return consensusContext.getRealmName();
	}

	private String batchId(ConsensusMessageContext context) {
	    return context.getBatchId();
    }

	private final class RealmProcessor {

		private final Lock realmLock = new ReentrantLock();

		private String currBatchId;

		// todo 暂不处理队列溢出导致的OOM问题
		private final ExecutorService asyncBlExecutor = Executors.newSingleThreadExecutor();

		private Map<TransactionResponse, CompletableAsyncFuture<byte[]>> txResponseMap;

		private TransactionBatchResultHandle batchResultHandle;

		private final AtomicLong batchIdIndex = new AtomicLong();

		private TransactionBatchProcess txBatchProcess;

		HashDigest ledgerHash;

		String realmName;

		public String getRealmName() {
			return realmName;
		}

		public TransactionBatchProcess getTxBatchProcess() {
			return txBatchProcess;
		}

		public AtomicLong getBatchIdIndex() {
			return batchIdIndex;
		}

		public HashDigest getLedgerHash() {
			return ledgerHash;
		}

		public String getCurrBatchId() {
			return currBatchId;
		}

		public String newBatchId() {
			realmLock.lock();
			try {
				if (currBatchId == null) {
					currBatchId = getRealmName() + "-" + getBatchIdIndex().getAndIncrement();
				}
				if (txResponseMap == null) {
					txResponseMap = new ConcurrentHashMap<>();
				}
				if (txBatchProcess == null) {
					txBatchProcess = txEngine.createNextBatch(ledgerHash);
				}
			} finally {
				realmLock.unlock();
			}
			return currBatchId;
		}

		public StateSnapshot getStateSnapshot() {
			TransactionBatchProcess txBatchProcess = getTxBatchProcess();
			if (txBatchProcess instanceof TransactionBatchProcessor) {
				LedgerBlock block = ((TransactionBatchProcessor) txBatchProcess).getLatestBlock();
				return new BlockStateSnapshot(block.getHeight(), block.getTimestamp(), block.getHash());
			} else {
				throw new IllegalStateException("Tx batch process is not instance of TransactionBatchProcessor !!!");
			}
		}

		public StateSnapshot getGenesisStateSnapshot() {
			TransactionBatchProcess txBatchProcess = getTxBatchProcess();
			if (txBatchProcess instanceof TransactionBatchProcessor) {
				LedgerBlock block = ((TransactionBatchProcessor) txBatchProcess).getGenesisBlock();
				return new BlockStateSnapshot(block.getHeight(), block.getTimestamp(), block.getHash());
			} else {
				throw new IllegalStateException("Tx batch process is not instance of TransactionBatchProcessor !!!");
			}
		}

		public AsyncFuture<byte[]> schedule(TransactionRequest txRequest) {
			CompletableAsyncFuture<byte[]> asyncTxResult = new CompletableAsyncFuture<>();
			TransactionResponse resp = getTxBatchProcess().schedule(txRequest);
			txResponseMap.put(resp, asyncTxResult);
			return asyncTxResult;
		}

		public StateSnapshot complete(long timestamp) {
			LedgerEditor.TIMESTAMP_HOLDER.set(timestamp);
			batchResultHandle = getTxBatchProcess().prepare();
			LedgerBlock currBlock = batchResultHandle.getBlock();
			long blockHeight = currBlock.getHeight();
			HashDigest blockHash = currBlock.getHash();
			asyncBlExecute(new HashMap<>(txResponseMap), blockHeight, blockHash);
			return new BlockStateSnapshot(blockHeight, currBlock.getTimestamp(), blockHash);
		}

		public void commit() {
			realmLock.lock();
			try {
				if (batchResultHandle == null) {
					throw new IllegalArgumentException("BatchResultHandle is null, complete() is not execute !");
				}
				batchResultHandle.commit();
				currBatchId = null;
				txResponseMap = null;
				txBatchProcess = null;
				batchResultHandle = null;
			} finally {
				realmLock.unlock();
				LedgerEditor.TIMESTAMP_HOLDER.remove();
			}
		}

		public void rollback(int reasonCode) {
			realmLock.lock();
			try {
				if (batchResultHandle != null) {
					batchResultHandle.cancel(TransactionState.valueOf((byte) reasonCode));
				}
				currBatchId = null;
				txResponseMap = null;
				txBatchProcess = null;
				batchResultHandle = null;
				if (txEngine != null && txEngine instanceof TransactionEngineImpl) {
					((TransactionEngineImpl) txEngine).freeBatch(ledgerHash);
					((TransactionEngineImpl) txEngine).resetNewBlockEditor(ledgerHash);
				} else {
					if (txEngine == null) {
						throw new IllegalStateException("You should init txEngine first !!!");
					} else {
						throw new IllegalStateException("TxEngine is not instance of TransactionEngineImpl !!!");
					}
				}
			} finally {
				realmLock.unlock();
				LedgerEditor.TIMESTAMP_HOLDER.remove();
			}
		}

		private void asyncBlExecute(Map<TransactionResponse, CompletableAsyncFuture<byte[]>> asyncMap,
									long blockHeight, HashDigest blockHash) {
			asyncBlExecutor.execute(() -> {
				// 填充应答结果
				for (Map.Entry<TransactionResponse, CompletableAsyncFuture<byte[]>> entry : asyncMap.entrySet()) {
					CompletableAsyncFuture<byte[]> asyncResult = entry.getValue();
					TxResponse txResponse = new TxResponse(entry.getKey());
					txResponse.setBlockHeight(blockHeight);
					txResponse.setBlockHash(blockHash);
					asyncResult.complete(BinaryProtocol.encode(txResponse, TransactionResponse.class));
				}
			});
		}

		private final class TxResponse implements TransactionResponse {

			private long blockHeight;

			private HashDigest blockHash;

			private TransactionResponse txResp;

			public TxResponse(TransactionResponse txResp) {
				this.txResp = txResp;
			}

			public void setBlockHeight(long blockHeight) {
				this.blockHeight = blockHeight;
			}

			public void setBlockHash(HashDigest blockHash) {
				this.blockHash = blockHash;
			}

			@Override
			public HashDigest getContentHash() {
				return this.txResp.getContentHash();
			}

			@Override
			public TransactionState getExecutionState() {
				return this.txResp.getExecutionState();
			}

			@Override
			public HashDigest getBlockHash() {
				return this.blockHash;
			}

			@Override
			public long getBlockHeight() {
				return this.blockHeight;
			}

			@Override
			public boolean isSuccess() {
				return this.txResp.isSuccess();
			}

			@Override
			public OperationResult[] getOperationResults() {
				return txResp.getOperationResults();
			}
		}
	}
}

package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.service.TransactionBatchProcess;
import com.jd.blockchain.service.TransactionEngine;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.jd.blockchain.metrics.LedgerMetrics;

public class TransactionEngineImpl implements TransactionEngine {

	@Autowired
	private LedgerService ledgerService;

	@Autowired
	private OperationHandleRegisteration opHdlRegs;

	private Map<HashDigest, TransactionBatchProcessor> batchs = new ConcurrentHashMap<>();

	private LedgerMetrics metrics;

	public TransactionEngineImpl() {
	}

	public TransactionEngineImpl(LedgerService ledgerService, OperationHandleRegisteration opHdlRegs) {
		this.ledgerService = ledgerService;
		this.opHdlRegs = opHdlRegs;
	}

	public int getTxsNumByHeight(HashDigest ledgerHash, int height) {

		LedgerRepository ledgerRepo = ledgerService.getLedger(ledgerHash);

		if (height == 0) {
			return (int)ledgerRepo.getTransactionSet(ledgerRepo.getBlock(height)).getTotalCount();
		} else if (height > 0) {
			return  (int)ledgerRepo.getTransactionSet(ledgerRepo.getBlock(height)).getTotalCount() - (int)ledgerRepo.getTransactionSet(ledgerRepo.getBlock(height - 1)).getTotalCount();
		} else {
			throw new IllegalArgumentException("[TransactionEngineImpl] getTxsNumByHeight height exception!");
		}
	}

	public byte[][] getTxsByHeight(HashDigest ledgerHash, int height, int currHeightCommandsNum) {

		int lastHeightCommandsTotal = -1;

		byte[][] commands = new byte[currHeightCommandsNum][];

		LedgerRepository ledgerRepo = ledgerService.getLedger(ledgerHash);

		if (height == 0) {
			lastHeightCommandsTotal = 0;
		} else if (height > 0) {
			lastHeightCommandsTotal = (int) ledgerRepo.getTransactionSet(ledgerRepo.getBlock(height - 1)).getTotalCount();
		} else {
			throw new IllegalArgumentException("[TransactionEngineImpl] getTxsByHeight height exception!");
		}

		for (int i = 0; i < currHeightCommandsNum; i++) {

			LedgerTransaction[] ledgerTransactions = ledgerRepo.getTransactionSet(ledgerRepo.getBlock(height)).getTransactions(lastHeightCommandsTotal + i , 1);
			commands[i] = BinaryProtocol.encode(ledgerTransactions[0].getRequest(), TransactionRequest.class);

		}

		return commands;

	}

	public byte[] getSnapshotByHeight(HashDigest ledgerHash, int height) {

		LedgerRepository ledgerRepo = ledgerService.getLedger(ledgerHash);

		return ledgerRepo.getBlock(height).getHash().toBytes();

	}

	public long getTimestampByHeight(HashDigest ledgerHash, int height) {

		LedgerRepository ledgerRepo = ledgerService.getLedger(ledgerHash);

		return ledgerRepo.getBlock(height).getTimestamp();

	}

	@Override
	public synchronized TransactionBatchProcess createNextBatch(HashDigest ledgerHash) {
		return createNextBatch(ledgerHash, null);
	}

	@Override
	public TransactionBatchProcess createNextBatch(HashDigest ledgerHash, LedgerMetrics metrics) {
		this.metrics = metrics;
		TransactionBatchProcessor batch = batchs.get(ledgerHash);
		if (batch != null) {
			throw new IllegalStateException(
					"The transaction batch process of ledger already exist! Cann't create another one!");
		}

		LedgerRepository ledgerRepo = ledgerService.getLedger(ledgerHash);

		batch = new InnerTransactionBatchProcessor(ledgerRepo,
				opHdlRegs);
		batchs.put(ledgerHash, batch);
		return batch;
	}

	@Override
	public TransactionBatchProcess getBatch(HashDigest ledgerHash) {
		return batchs.get(ledgerHash);
	}

	public void freeBatch(HashDigest ledgerHash) {
		finishBatch(ledgerHash);
	}

	public void resetNewBlockEditor(HashDigest ledgerHash) {

		LedgerRepository ledgerRepo = ledgerService.getLedger(ledgerHash);
		((LedgerRepositoryImpl)ledgerRepo).resetNextBlockEditor();
	}

	private void finishBatch(HashDigest ledgerHash) {
		batchs.remove(ledgerHash);
	}

	private class InnerTransactionBatchProcessor extends TransactionBatchProcessor {

//		private HashDigest ledgerHash;

		/**
		 * 创建交易批处理器；
		 * 
		 * @param ledgerRepo           账本；
		 * @param handlesRegisteration 操作处理对象注册表；
		 *
		 */
		public InnerTransactionBatchProcessor(LedgerRepository ledgerRepo,
				OperationHandleRegisteration handlesRegisteration) {
			super(ledgerRepo, handlesRegisteration);
//			ledgerHash = ledgerRepo.getHash();
		}

		@Override
		protected void onCommitted() {
			super.onCommitted();
			finishBatch(getLedgerHash());
			if (null != metrics) {
				metrics.block(getLatestBlock().getHeight());
			}
		}

		@Override
		protected void onCanceled() {
			super.onCanceled();
			finishBatch(getLedgerHash());
		}

	}
}

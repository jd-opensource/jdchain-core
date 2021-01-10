package com.jd.blockchain.peer.web;

import java.util.ArrayList;
import java.util.List;

import com.jd.blockchain.ledger.LedgerTransactions;
import com.jd.blockchain.web.converters.BinaryMessageConverter;

import utils.Bytes;
import utils.DataEntry;
import utils.Mapper;
import utils.SkippingIterator;
import utils.query.QueryArgs;
import utils.query.QueryUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.jd.blockchain.contract.ContractException;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.DataAccountInfo;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.KVDataVO;
import com.jd.blockchain.ledger.KVInfoVO;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.ledger.LedgerInfo;
import com.jd.blockchain.ledger.LedgerMetadata;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.PrivilegeSet;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.TypedKVData;
import com.jd.blockchain.ledger.TypedKVEntry;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.ledger.UserInfo;
import com.jd.blockchain.ledger.UserPrivilegeSet;
import com.jd.blockchain.ledger.core.ContractAccountSet;
import com.jd.blockchain.ledger.core.DataAccount;
import com.jd.blockchain.ledger.core.DataAccountSet;
import com.jd.blockchain.ledger.core.EventAccountSet;
import com.jd.blockchain.ledger.core.EventGroup;
import com.jd.blockchain.ledger.core.EventPublishingAccount;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.LedgerRepository;
import com.jd.blockchain.ledger.core.LedgerService;
import com.jd.blockchain.ledger.core.ParticipantCertData;
import com.jd.blockchain.ledger.core.TransactionSet;
import com.jd.blockchain.ledger.core.UserAccountSet;
import com.jd.blockchain.peer.decorator.LedgerAdminInfoDecorator;
import com.jd.blockchain.peer.decorator.TransactionDecorator;
import com.jd.blockchain.transaction.BlockchainQueryService;

@RestController
@RequestMapping(path = "/")
public class LedgerQueryController implements BlockchainQueryService {

	@Autowired
	private LedgerService ledgerService;

	@RequestMapping(method = RequestMethod.GET, path = "ledgers")
	@Override
	public HashDigest[] getLedgerHashs() {
		return ledgerService.getLedgerHashs();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}")
	@Override
	public LedgerInfo getLedger(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		// TODO: 需要配置返回值的 spring MsgQueueMessageDispatcher
		// ，对返回对象仅仅序列化声明的返回值类型的属性，而不是整个对象本身；
		LedgerInfo ledgerInfo = new LedgerInfo();
		ledgerInfo.setHash(ledgerHash);
		ledgerInfo.setLatestBlockHash(ledger.getLatestBlockHash());
		ledgerInfo.setLatestBlockHeight(ledger.getLatestBlockHeight());
		return ledgerInfo;
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/participants")
	@Override
	public ParticipantNode[] getConsensusParticipants(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerAdminInfo ledgerAdministration = ledger.getAdminInfo();
		long participantCount = ledgerAdministration.getParticipantCount();
		if (participantCount <= 0) {
			return null;
		}
		ParticipantNode[] participantNodes = ledgerAdministration.getParticipants();
		// 重新封装，处理Proxy的问题
		if (participantNodes != null && participantNodes.length > 0) {
			ParticipantNode[] convertNodes = new ParticipantNode[participantNodes.length];
			for (int i = 0, length = participantNodes.length; i < length; i++) {
				convertNodes[i] = new ParticipantCertData(participantNodes[i]);
			}
			return convertNodes;
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/admininfo")
	@Override
	public LedgerAdminInfo getLedgerAdminInfo(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerAdminInfo ledgerAdministration = ledger.getAdminInfo();
		return ledgerAdminInfoDecorator(ledgerAdministration);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/metadata")
	@Override
	public LedgerMetadata getLedgerMetadata(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerAdminInfo ledgerAdministration = getLedgerAdminInfo(ledgerHash);
		if (ledgerAdministration != null) {
			return ledgerAdministration.getMetadata();
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}")
	@Override
	public LedgerBlock getBlock(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		// TODO: 需要配置返回值的 spring MsgQueueMessageDispatcher
		// ，对返回对象仅仅序列化声明的返回值类型的属性，而不是整个对象本身；
		return ledger.getBlock(blockHeight);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}")
	@Override
	public LedgerBlock getBlock(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		// TODO: 需要配置返回值的 spring MsgQueueMessageDispatcher
		// ，对返回对象仅仅序列化声明的返回值类型的属性，而不是整个对象本身；
		return ledger.getBlock(blockHash);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/txs/count")
	@Override
	public long getTransactionCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getBlock(blockHeight);
		if(null == block) {
			return 0;
		}
		TransactionSet txSet = ledger.getTransactionSet(block);
		return txSet.getTotalCount();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/txs/count")
	@Override
	public long getTransactionCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getBlock(blockHash);
		if(null == block) {
			return 0;
		}
		TransactionSet txSet = ledger.getTransactionSet(block);
		return txSet.getTotalCount();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/txs/count")
	@Override
	public long getTransactionTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		TransactionSet txSet = ledger.getTransactionSet(block);
		return txSet.getTotalCount();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/accounts/count")
	@Override
	public long getDataAccountCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long height) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getBlock(height);
		if(null == block) {
			return 0;
		}
		DataAccountSet dataAccountSet = ledger.getDataAccountSet(block);
		return dataAccountSet.getTotal();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/accounts/count")
	@Override
	public long getDataAccountCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getBlock(blockHash);
		if(null == block) {
			return 0;
		}
		DataAccountSet dataAccountSet = ledger.getDataAccountSet(block);
		return dataAccountSet.getTotal();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/accounts/count")
	@Override
	public long getDataAccountTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		DataAccountSet dataAccountSet = ledger.getDataAccountSet(block);
		return dataAccountSet.getTotal();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/users/count")
	@Override
	public long getUserCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long height) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getBlock(height);
		if(null == block) {
			return 0;
		}
		UserAccountSet userAccountSet = ledger.getUserAccountSet(block);
		return userAccountSet.getTotal();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/users/count")
	@Override
	public long getUserCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getBlock(blockHash);
		if(null == block) {
			return 0;
		}
		UserAccountSet userAccountSet = ledger.getUserAccountSet(block);
		return userAccountSet.getTotal();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/users/count")
	@Override
	public long getUserTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		UserAccountSet userAccountSet = ledger.getUserAccountSet(block);
		return userAccountSet.getTotal();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/contracts/count")
	@Override
	public long getContractCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long height) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getBlock(height);
		if(null == block) {
			return 0;
		}
		ContractAccountSet contractAccountSet = ledger.getContractAccountSet(block);
		return contractAccountSet.getTotal();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/contracts/count")
	@Override
	public long getContractCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getBlock(blockHash);
		if(null == block) {
			return 0;
		}
		ContractAccountSet contractAccountSet = ledger.getContractAccountSet(block);
		return contractAccountSet.getTotal();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/contracts/count")
	@Override
	public long getContractTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		ContractAccountSet contractAccountSet = ledger.getContractAccountSet(block);
		return contractAccountSet.getTotal();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/txs")
	@Override
	public LedgerTransaction[] getTransactions(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {

		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock ledgerBlock = ledger.getBlock(blockHeight);
		if(null == ledgerBlock) {
			return null;
		}
		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count,
				(int) ledger.getTransactionSet(ledgerBlock).getTotalCount());

		LedgerTransaction[] txs = ledger.getTransactionSet(ledgerBlock).getTransactions(queryArgs.getFrom(),
				queryArgs.getCount());

		return txsDecorator(txs);

//		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
//		LedgerBlock ledgerBlock = ledger.getBlock(blockHeight);
//		TransactionQuery transactionSet = ledger.getTransactionSet(ledgerBlock);
//		TransactionQuery origTransactionSet = null;
//
//		int lastHeightTxTotalNums = 0;
//
//		if (blockHeight > 0) {
//			origTransactionSet = ledger.getTransactionSet(ledger.getBlock(blockHeight - 1));
//			lastHeightTxTotalNums = (int) origTransactionSet.getTotalCount();
//		}
//
//		int currentHeightTxTotalNums = (int) ledger.getTransactionSet(ledger.getBlock(blockHeight)).getTotalCount();
//		// 取当前高度的增量交易数，在增量交易里进行查找
//		int currentHeightTxNums = currentHeightTxTotalNums - lastHeightTxTotalNums;
//
//		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count, currentHeightTxNums);
//		LedgerTransaction[] txs = transactionSet.getBlockTxs(queryArgs.getFrom(), queryArgs.getCount(), origTransactionSet);
//		return txsDecorator(txs);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/txs")
	@Override
	public LedgerTransaction[] getTransactions(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {

		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock ledgerBlock = ledger.getBlock(blockHash);
		if(null == ledgerBlock) {
			return null;
		}
		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count,
				(int) ledger.getTransactionSet(ledgerBlock).getTotalCount());

		LedgerTransaction[] txs = ledger.getTransactionSet(ledgerBlock).getTransactions(queryArgs.getFrom(),
				queryArgs.getCount());

		return txsDecorator(txs);

//		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
//		LedgerBlock ledgerBlock = ledger.getBlock(blockHash);
//		long height = ledgerBlock.getHeight();
//		TransactionQuery transactionSet = ledger.getTransactionSet(ledgerBlock);
//		TransactionQuery origTransactionSet = null;
//		int lastHeightTxTotalNums = 0;
//
//		if (height > 0) {
//			origTransactionSet = ledger.getTransactionSet(ledger.getBlock(height - 1));
//			lastHeightTxTotalNums = (int) origTransactionSet.getTotalCount();
//		}
//
//		int currentHeightTxTotalNums = (int) ledger.getTransactionSet(ledger.getBlock(height)).getTotalCount();
//		// 取当前块hash的增量交易数，在增量交易里进行查找
//		int currentHeightTxNums = currentHeightTxTotalNums - lastHeightTxTotalNums;
//
//		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count, currentHeightTxNums);
//		LedgerTransaction[] txs = transactionSet.getBlockTxs(queryArgs.getFrom(), queryArgs.getCount(), origTransactionSet);
//		return txsDecorator(txs);
	}

	@RequestMapping(method = RequestMethod.POST, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/txs/additional-txs/binary" , produces = BinaryMessageConverter.CONTENT_TYPE_VALUE)
	public @ResponseBody LedgerTransactions getAdditionalTransactionsInBinary(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
														 @PathVariable(name = "blockHeight") long blockHeight,
														 @RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
														 @RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		LedgerTransaction[] ledgerTransactions = getAdditionalTransactions(ledgerHash, blockHeight, fromIndex, count);
		if (ledgerTransactions != null) {
			return new LedgerTransactionsDecorator(ledgerTransactions);
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/txs/additional-txs")
	@Override
	public LedgerTransaction[] getAdditionalTransactions(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {

		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock ledgerBlock = ledger.getBlock(blockHeight);
		if(null == ledgerBlock) {
			return null;
		}
		TransactionSet currTransactionSet = ledger.getTransactionSet(ledgerBlock);
		TransactionSet lastTransactionSet = null;

		int lastHeightTxTotalNums = 0;

		if (blockHeight > 0) {
			lastTransactionSet = ledger.getTransactionSet(ledger.getBlock(blockHeight - 1));
			lastHeightTxTotalNums = (int) lastTransactionSet.getTotalCount();
		}

		int currentHeightTxTotalNums = (int) ledger.getTransactionSet(ledger.getBlock(blockHeight)).getTotalCount();
		// 取当前高度的增量交易数，在增量交易里进行查找
		int currentHeightTxNums = currentHeightTxTotalNums - lastHeightTxTotalNums;

		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count, currentHeightTxNums);
		LedgerTransaction[] txs = currTransactionSet.getTransactions(lastHeightTxTotalNums + queryArgs.getFrom(),
				queryArgs.getCount());
		return txsDecorator(txs);

	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/txs/additional-txs")
	@Override
	public LedgerTransaction[] getAdditionalTransactions(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock ledgerBlock = ledger.getBlock(blockHash);
		if(null == ledgerBlock) {
			return null;
		}
		long height = ledgerBlock.getHeight();
		TransactionSet currTransactionSet = ledger.getTransactionSet(ledgerBlock);
		TransactionSet lastTransactionSet = null;
		int lastHeightTxTotalNums = 0;

		if (height > 0) {
			lastTransactionSet = ledger.getTransactionSet(ledger.getBlock(height - 1));
			lastHeightTxTotalNums = (int) lastTransactionSet.getTotalCount();
		}

		int currentHeightTxTotalNums = (int) ledger.getTransactionSet(ledger.getBlock(height)).getTotalCount();
		// 取当前块hash的增量交易数，在增量交易里进行查找
		int currentHeightTxNums = currentHeightTxTotalNums - lastHeightTxTotalNums;

		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count, currentHeightTxNums);
		LedgerTransaction[] txs = currTransactionSet.getTransactions(lastHeightTxTotalNums + queryArgs.getFrom(),
				queryArgs.getCount());
		return txsDecorator(txs);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/txs/hash/{contentHash}")
	@Override
	public LedgerTransaction getTransactionByContentHash(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "contentHash") HashDigest contentHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		TransactionSet txset = ledger.getTransactionSet(block);
		LedgerTransaction transaction = txset.getTransaction(contentHash);

		// TODO: 去掉包装类，通过修正针对代理对象的 JSON 序列化来解决； by huanghaiquan at 2020-09-21;
		return txDecorator(transaction);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/txs/state/{contentHash}")
	@Override
	public TransactionState getTransactionStateByContentHash(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "contentHash") HashDigest contentHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		TransactionSet txset = ledger.getTransactionSet(block);
		return txset.getState(contentHash);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/users/address/{address}")
	@Override
	public UserInfo getUser(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		UserAccountSet userAccountSet = ledger.getUserAccountSet(block);
		return userAccountSet.getAccount(address);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/accounts/address/{address}")
	@Override
	public DataAccountInfo getDataAccount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		DataAccountSet dataAccountSet = ledger.getDataAccountSet(block);
		return dataAccountSet.getAccount(Bytes.fromBase58(address));
	}

	@RequestMapping(method = { RequestMethod.GET,
			RequestMethod.POST }, path = "ledgers/{ledgerHash}/accounts/{address}/entries")
	@Override
	public TypedKVEntry[] getDataEntries(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address, @RequestParam("keys") String... keys) {
		if (keys == null || keys.length == 0) {
			return null;
		}
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		DataAccountSet dataAccountSet = ledger.getDataAccountSet(block);
		DataAccount dataAccount = dataAccountSet.getAccount(Bytes.fromBase58(address));
		if (dataAccount == null) {
			return null;
		}

		TypedKVEntry[] entries = new TypedKVEntry[keys.length];
		long ver;
		for (int i = 0; i < entries.length; i++) {
			ver = dataAccount.getDataset().getVersion(keys[i]);
			if (ver < 0) {
				entries[i] = new TypedKVData(keys[i], -1, null);
			} else {
				BytesValue value = dataAccount.getDataset().getValue(keys[i], ver);
				entries[i] = new TypedKVData(keys[i], ver, value);
			}
		}

		return entries;
	}

	@RequestMapping(method = { RequestMethod.GET,
			RequestMethod.POST }, path = "ledgers/{ledgerHash}/accounts/{address}/entries-version")
	@Override
	public TypedKVEntry[] getDataEntries(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address, @RequestBody KVInfoVO kvInfoVO) {
		// parse kvInfoVO;
		List<String> keyList = new ArrayList<>();
		List<Long> versionList = new ArrayList<>();
		if (kvInfoVO != null) {
			for (KVDataVO kvDataVO : kvInfoVO.getData()) {
				for (Long version : kvDataVO.getVersion()) {
					keyList.add(kvDataVO.getKey());
					versionList.add(version);
				}
			}
		}
		String[] keys = keyList.toArray(new String[keyList.size()]);
		Long[] versions = versionList.toArray(new Long[versionList.size()]);

		if (keys == null || keys.length == 0) {
			return null;
		}
		if (versions == null || versions.length == 0) {
			return null;
		}
		if (keys.length != versions.length) {
			throw new ContractException("keys.length!=versions.length!");
		}

		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		DataAccountSet dataAccountSet = ledger.getDataAccountSet(block);
		DataAccount dataAccount = dataAccountSet.getAccount(Bytes.fromBase58(address));
		if (dataAccount == null) {
			return null;
		}

		TypedKVEntry[] entries = new TypedKVEntry[keys.length];
		long ver = -1;
		for (int i = 0; i < entries.length; i++) {
//			ver = dataAccount.getDataVersion(Bytes.fromString(keys[i]));
			ver = versions[i];
			if (ver < 0) {
				entries[i] = new TypedKVData(keys[i], -1, null);
			} else {
				if (dataAccount.getDataset().getDataCount() == 0
						|| dataAccount.getDataset().getValue(keys[i], ver) == null) {
					// is the address is not exist; the result is null;
					entries[i] = new TypedKVData(keys[i], -1, null);
				} else {
					BytesValue value = dataAccount.getDataset().getValue(keys[i], ver);
					entries[i] = new TypedKVData(keys[i], ver, value);
				}
			}
		}

		return entries;
	}

	@RequestMapping(method = { RequestMethod.GET,
			RequestMethod.POST }, path = "ledgers/{ledgerHash}/accounts/address/{address}/entries")
	@Override
	public TypedKVEntry[] getDataEntries(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {

		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		DataAccountSet dataAccountSet = ledger.getDataAccountSet(block);
		DataAccount dataAccount = dataAccountSet.getAccount(Bytes.fromBase58(address));
		if (dataAccount == null) {
			return null;
		}

		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count,
				(int) dataAccount.getDataset().getDataCount());
		fromIndex = queryArgs.getFrom();
		count = queryArgs.getCount();

		SkippingIterator<DataEntry<String, TypedValue>> iterator = dataAccount.getDataset().iterator();
		iterator.skip(fromIndex);
		TypedKVEntry[] typedKVEntries = iterator.next(count, TypedKVEntry.class, new Mapper<DataEntry<String,TypedValue>, TypedKVEntry>() {
			@Override
			public TypedKVEntry from(DataEntry<String, TypedValue> entry) {
				return  entry == null ? null : new TypedKVData(entry.getKey(), entry.getVersion(), entry.getValue());
			}
		});
		return typedKVEntries;
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/accounts/address/{address}/entries/count")
	@Override
	public long getDataEntriesTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		DataAccountSet dataAccountSet = ledger.getDataAccountSet(block);
		DataAccount dataAccount = dataAccountSet.getAccount(Bytes.fromBase58(address));
		if (dataAccount == null) {
			return 0;
		}

		return dataAccount.getDataset().getDataCount();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/contracts/address/{address}")
	@Override
	public ContractInfo getContract(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		ContractAccountSet contractAccountSet = ledger.getContractAccountSet(block);
		return contractAccountSet.getAccount(Bytes.fromBase58(address));
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/system/names/{eventName}")
	@Override
	public Event[] getSystemEvents(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "eventName") String eventName,
			@RequestParam(name = "fromSequence", required = false, defaultValue = "0") long fromSequence,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {

		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		EventGroup systemEvents = ledger.getSystemEventGroup(block);
		return systemEvents.getEvents(eventName, fromSequence, count);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/system/names/count")
	@Override
	public long getSystemEventNameTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		EventGroup systemEvents = ledger.getSystemEventGroup(block);
		return systemEvents.totalEventNames();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/system/names")
	@Override
	public String[] getSystemEventNames(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "maxCount", required = false, defaultValue = "-1") int count) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		EventGroup systemEvents = ledger.getSystemEventGroup(block);
		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count, (int) systemEvents.totalEventNames());
		return systemEvents.getEventNames(queryArgs.getFrom(), queryArgs.getCount());
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/system/names/{eventName}/latest")
	@Override
	public Event getLatestEvent(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "eventName") String eventName) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		EventGroup systemEvents = ledger.getSystemEventGroup(block);
		return systemEvents.getLatest(eventName);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/system/names/{eventName}/count")
	@Override
	public long getSystemEventsTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "eventName") String eventName) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		EventGroup systemEvents = ledger.getSystemEventGroup(block);
		return systemEvents.totalEvents(eventName);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts")
	@Override
	public BlockchainIdentity[] getUserEventAccounts(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		EventAccountSet eventAccountSet = ledger.getEventAccountSet(ledger.getLatestBlock());
		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count, (int) eventAccountSet.getTotal());

		SkippingIterator<BlockchainIdentity> it = eventAccountSet.identityIterator();
		it.skip(queryArgs.getFrom());
		return it.next(queryArgs.getCount(), BlockchainIdentity.class);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}")
	@Override
	public BlockchainIdentity getUserEventAccount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		EventAccountSet eventAccountSet = ledger.getEventAccountSet(ledger.getLatestBlock());
		EventPublishingAccount account = eventAccountSet.getAccount(address);
		if (null == account) {
			return null;
		}
		return account.getID();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/count")
	@Override
	public long getUserEventAccountTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		EventAccountSet eventAccountSet = ledger.getEventAccountSet(ledger.getLatestBlock());
		return eventAccountSet.getTotal();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}/names/count")
	@Override
	public long getUserEventNameTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		EventAccountSet eventAccountSet = ledger.getEventAccountSet(ledger.getLatestBlock());
		EventPublishingAccount account = eventAccountSet.getAccount(address);
		if (null == account) {
			return 0;
		}
		return account.totalEventNames();
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}/names")
	@Override
	public String[] getUserEventNames(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		EventPublishingAccount account = ledger.getEventAccountSet(block).getAccount(address);
		if (null == account) {
			return null;
		}
		QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count, (int) account.totalEventNames());
		return account.getEventNames(queryArgs.getFrom(), queryArgs.getCount());
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}/names/{eventName}/latest")
	@Override
	public Event getLatestEvent(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address, @PathVariable(name = "eventName") String eventName) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		EventPublishingAccount account = ledger.getEventAccountSet(ledger.getLatestBlock()).getAccount(address);
		if (null == account) {
			return null;
		}
		return account.getLatest(eventName);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}/names/{eventName}/count")
	@Override
	public long getUserEventsTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address, @PathVariable(name = "eventName") String eventName) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		EventPublishingAccount account = ledger.getEventAccountSet(ledger.getLatestBlock()).getAccount(address);
		if (null == account) {
			return 0;
		}
		return account.totalEvents(eventName);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}/names/{eventName}")
	@Override
	public Event[] getUserEvents(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address, @PathVariable(name = "eventName") String eventName,
			@RequestParam(name = "fromSequence", required = false, defaultValue = "0") long fromSequence,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		EventPublishingAccount account = ledger.getEventAccountSet(block).getAccount(address);
		if (null == account) {
			return null;
		}
		return account.getEvents(eventName, fromSequence, count);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/contracts/address/{address}/version/{version}")
	@Override
	public ContractInfo getContract(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address, @PathVariable(name = "version") long version) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		ContractAccountSet contractAccountSet = ledger.getContractAccountSet(block);
		return contractAccountSet.getAccount(Bytes.fromBase58(address), version);
	}

	/**
	 * get more users by fromIndex and count;
	 *
	 * @param ledgerHash
	 * @param fromIndex
	 * @param count
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/users")
	@Override
	public BlockchainIdentity[] getUsers(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		UserAccountSet userAccountSet = ledger.getUserAccountSet(block);
		QueryArgs queryArgs = QueryUtils.calFromIndexAndCountDescend(fromIndex, count, (int) userAccountSet.getTotal());

		SkippingIterator<BlockchainIdentity> it = userAccountSet.identityIterator();
		it.skip(queryArgs.getFrom());
		return it.next(queryArgs.getCount(), BlockchainIdentity.class);
	}

	/**
	 * get more dataAccounts by fromIndex and count;
	 *
	 * @param ledgerHash
	 * @param fromIndex
	 * @param count
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/accounts")
	@Override
	public BlockchainIdentity[] getDataAccounts(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		DataAccountSet dataAccountSet = ledger.getDataAccountSet(block);
		QueryArgs queryArgs = QueryUtils.calFromIndexAndCountDescend(fromIndex, count, (int) dataAccountSet.getTotal());

		SkippingIterator<BlockchainIdentity> it = dataAccountSet.identityIterator();
		it.skip(queryArgs.getFrom());
		return it.next(queryArgs.getCount(), BlockchainIdentity.class);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/contracts")
	@Override
	public BlockchainIdentity[] getContractAccounts(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		LedgerBlock block = ledger.getLatestBlock();
		ContractAccountSet contractAccountSet = ledger.getContractAccountSet(block);
		QueryArgs queryArgs = QueryUtils.calFromIndexAndCountDescend(fromIndex, count,
				(int) contractAccountSet.getTotal());

		SkippingIterator<BlockchainIdentity> it = contractAccountSet.identityIterator();
		it.skip(queryArgs.getFrom());
		return it.next(queryArgs.getCount(), BlockchainIdentity.class);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/authorization/role/{roleName}")
	@Override
	public PrivilegeSet getRolePrivileges(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "roleName") String roleName) {
		LedgerQuery ledger = ledgerService.getLedger(ledgerHash);
		return ledger.getAdminSettings().getRolePrivileges().getRolePrivilege(roleName);
	}

	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/authorization/user/{userAddress}")
	@Override
	public UserPrivilegeSet getUserPrivileges(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "userAddress") String userAddress) {
		LedgerRepository ledger = ledgerService.getLedger(ledgerHash);
		return ledger.getSecurityManager().getUserRolesPrivilegs(Bytes.fromBase58(userAddress));
	}

//	private LedgerTransaction txDecorator(LedgerTransaction ledgerTransaction) {
//		if (ledgerTransaction == null) {
//			return null;
//		}
//		return new TransactionDecorator(ledgerTransaction);
//	}

	private LedgerAdminInfo ledgerAdminInfoDecorator(LedgerAdminInfo ledgerAdministration) {
		if (ledgerAdministration == null) {
			return null;
		}
		return new LedgerAdminInfoDecorator(ledgerAdministration);
	}

	private LedgerTransaction txDecorator(LedgerTransaction ledgerTransaction) {
		if (ledgerTransaction == null) {
			return null;
		}
		return new TransactionDecorator(ledgerTransaction);
	}

	private LedgerTransaction[] txsDecorator(LedgerTransaction[] ledgerTransactions) {
		if (ledgerTransactions == null || ledgerTransactions.length == 0) {
			return ledgerTransactions;
		}
		LedgerTransaction[] transactionDecorators = new LedgerTransaction[ledgerTransactions.length];
		for (int i = 0; i < ledgerTransactions.length; i++) {
			transactionDecorators[i] = txDecorator(ledgerTransactions[i]);
		}
		return transactionDecorators;
	}

	class LedgerTransactionsDecorator implements LedgerTransactions {

		LedgerTransaction[] ledgerTransactions;

		public LedgerTransactionsDecorator(LedgerTransaction[] ledgerTransactions) {
			this.ledgerTransactions = ledgerTransactions;
		}

		@Override
		public LedgerTransaction[] getLedgerTransactions() {
			return ledgerTransactions;
		}
	}
}

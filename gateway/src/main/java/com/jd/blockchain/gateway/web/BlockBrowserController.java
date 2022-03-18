package com.jd.blockchain.gateway.web;

import java.util.ArrayList;
import java.util.List;

import com.jd.blockchain.ledger.EventAccountInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jd.blockchain.contract.ContractProcessor;
import com.jd.blockchain.contract.OnLineContractProcessor;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.gateway.service.GatewayQueryService;
import com.jd.blockchain.gateway.service.LedgersManager;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.DataAccountInfo;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.KVInfoVO;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerInfo;
import com.jd.blockchain.ledger.LedgerMetadata;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.PrivilegeSet;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.TypedKVEntry;
import com.jd.blockchain.ledger.UserInfo;
import com.jd.blockchain.ledger.UserPrivilegeSet;
import com.jd.blockchain.sdk.BlockchainBrowserService;
import com.jd.blockchain.sdk.DecompliedContractInfo;
import com.jd.blockchain.sdk.LedgerInitAttributes;

@RestController
@RequestMapping(path = "/")
public class BlockBrowserController implements BlockchainBrowserService {

	private static final ContractProcessor CONTRACT_PROCESSOR = OnLineContractProcessor.getInstance();

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private LedgersManager peerService;

	@Autowired
	private GatewayQueryService gatewayQueryService;

	private static final int BLOCK_MAX_DISPLAY = 3;

	private static final long GENESIS_BLOCK_HEIGHT = 0L;

	@RequestMapping(method = RequestMethod.GET, path = GET_LEGDER_HASH_LIST)
	@Override
	public HashDigest[] getLedgerHashs() {
		return peerService.getLedgerHashs();
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}")
	@RequestMapping(method = RequestMethod.GET, path = GET_LEDGER)
	@Override
	public LedgerInfo getLedger(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getLedger(ledgerHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/admininfo")
	@RequestMapping(method = RequestMethod.GET, path = GET_LEDGER_ADMIN_INFO)
	@Override
	public LedgerAdminInfo getLedgerAdminInfo(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getLedgerAdminInfo(ledgerHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/participants")
	@RequestMapping(method = RequestMethod.GET, path = GET_CONSENSUS_PARTICIPANTS)
	@Override
	public ParticipantNode[] getConsensusParticipants(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getConsensusParticipants(ledgerHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/metadata")
	@RequestMapping(method = RequestMethod.GET, path = GET_LEDGER_METADATA)
	@Override
	public LedgerMetadata getLedgerMetadata(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getLedgerMetadata(ledgerHash);
	}

	@RequestMapping(method = RequestMethod.GET, path = GET_LEDGER_INIT_SETTINGS)
	@Override
	public LedgerInitAttributes getLedgerInitSettings(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return gatewayQueryService.getLedgerBaseSettings(ledgerHash);
	}

	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_LATEST_BLOCK_LIST)
	@Override
	public LedgerBlock[] getLatestBlocks(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "numOfBlocks", required = false, defaultValue = "3") int numOfBlocks) {
		if (numOfBlocks <= 0 || numOfBlocks > BLOCK_MAX_DISPLAY) {
			numOfBlocks = BLOCK_MAX_DISPLAY;
		}
		LedgerInfo ledgerInfo = peerService.getQueryService(ledgerHash).getLedger(ledgerHash);
		long maxBlockHeight = ledgerInfo.getLatestBlockHeight();
		List<LedgerBlock> ledgerBlocks = new ArrayList<>();
		for (long blockHeight = maxBlockHeight; blockHeight > GENESIS_BLOCK_HEIGHT; blockHeight--) {
			LedgerBlock ledgerBlock = peerService.getQueryService(ledgerHash).getBlock(ledgerHash, blockHeight);
			ledgerBlocks.add(0, ledgerBlock);
			if (ledgerBlocks.size() == numOfBlocks) {
				break;
			}
		}
		// 最后增加创世区块
		LedgerBlock genesisBlock = peerService.getQueryService(ledgerHash).getBlock(ledgerHash, GENESIS_BLOCK_HEIGHT);
		ledgerBlocks.add(0, genesisBlock);
		LedgerBlock[] blocks = new LedgerBlock[ledgerBlocks.size()];
		ledgerBlocks.toArray(blocks);
		return blocks;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}")
	@RequestMapping(method = RequestMethod.GET, path = GET_BLOCK_WITH_HEIGHT)
	@Override
	public LedgerBlock getBlock(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		// 获取最新区块高度
		LedgerBlock latestBlock = getLatestBlock(ledgerHash);
		if (blockHeight >= latestBlock.getHeight() || blockHeight < 0) {
			return latestBlock;
		} else {
			return peerService.getQueryService(ledgerHash).getBlock(ledgerHash, blockHeight);
		}
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}")
	@RequestMapping(method = RequestMethod.GET, path = GET_BLOCK_WITH_HASH)
	@Override
	public LedgerBlock getBlock(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		return peerService.getQueryService(ledgerHash).getBlock(ledgerHash, blockHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/txs/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_TRANSACTION_COUNT_ON_BLOCK_HEIGHT)
	@Override
	public long getTransactionCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		return peerService.getQueryService(ledgerHash).getTransactionCount(ledgerHash, blockHeight);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/txs/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_TRANSACTION_COUNT_ON_BLOCK_HASH)
	@Override
	public long getTransactionCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		return peerService.getQueryService(ledgerHash).getTransactionCount(ledgerHash, blockHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/txs/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_TOTAL_TRANSACTION_COUNT)
	@Override
	public long getTransactionTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getTransactionTotalCount(ledgerHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/accounts/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_DATA_ACCOUNT_COUNT_ON_BLOCK_HEIGHT)
	@Override
	public long getDataAccountCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		return peerService.getQueryService(ledgerHash).getDataAccountCount(ledgerHash, blockHeight);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/accounts/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_DATA_ACCOUNT_COUNT_ON_BLOCK_HASH)
	@Override
	public long getDataAccountCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		return peerService.getQueryService(ledgerHash).getDataAccountCount(ledgerHash, blockHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/accounts/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_TOTAL_DATA_ACCOUNT_COUNT)
	@Override
	public long getDataAccountTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getDataAccountTotalCount(ledgerHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/users/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_USER_COUNT_ON_BLOCK_HEIGHT)
	@Override
	public long getUserCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		return peerService.getQueryService(ledgerHash).getUserCount(ledgerHash, blockHeight);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/users/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_USER_COUNT_ON_BLOCK_HASH)
	@Override
	public long getUserCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		return peerService.getQueryService(ledgerHash).getUserCount(ledgerHash, blockHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/users/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_TOTAL_USER_COUNT)
	@Override
	public long getUserTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getUserTotalCount(ledgerHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/contracts/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_CONTRACT_COUNT_ON_BLOCK_HEIGHT)
	@Override
	public long getContractCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		return peerService.getQueryService(ledgerHash).getContractCount(ledgerHash, blockHeight);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/contracts/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_CONTRACT_COUNT_ON_BLOCK_HASH)
	@Override
	public long getContractCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		return peerService.getQueryService(ledgerHash).getContractCount(ledgerHash, blockHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/contracts/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_TOTAL_CONTRACT_COUNT)
	@Override
	public long getContractTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getContractTotalCount(ledgerHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/txs")
	@RequestMapping(method = RequestMethod.GET, path = GET_TRANSACTIONS_ON_BLOCK_HEIGHT)
	@Override
	public LedgerTransaction[] getTransactions(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return peerService.getQueryService(ledgerHash).getTransactions(ledgerHash, blockHeight, fromIndex, count);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/txs")
	@RequestMapping(method = RequestMethod.GET, path = GET_TRANSACTIONS_ON_BLOCK_HASH)
	@Override
	public LedgerTransaction[] getTransactions(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return peerService.getQueryService(ledgerHash).getTransactions(ledgerHash, blockHash, fromIndex, count);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/txs/additional-txs")
	@RequestMapping(method = RequestMethod.GET, path = GET_TRANSACTIONS_IN_BLOCK_HEIGHT)
	@Override
	public LedgerTransaction[] getAdditionalTransactions(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return peerService.getQueryService(ledgerHash).getAdditionalTransactions(ledgerHash, blockHeight, fromIndex,
				count);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/txs/additional-txs")
	@RequestMapping(method = RequestMethod.GET, path = GET_TRANSACTIONS_IN_BLOCK_HASH)
	@Override
	public LedgerTransaction[] getAdditionalTransactions(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return peerService.getQueryService(ledgerHash).getAdditionalTransactions(ledgerHash, blockHash, fromIndex,
				count);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/txs/hash/{contentHash}")
	@RequestMapping(method = RequestMethod.GET, path = GET_TRANSACTION)
	@Override
	public LedgerTransaction getTransactionByContentHash(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "contentHash") HashDigest contentHash) {
		return peerService.getQueryService(ledgerHash).getTransactionByContentHash(ledgerHash, contentHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/txs/state/{contentHash}")
	@RequestMapping(method = RequestMethod.GET, path = GET_TRANSACTION_STATE)
	@Override
	public TransactionState getTransactionStateByContentHash(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "contentHash") HashDigest contentHash) {
		return peerService.getQueryService(ledgerHash).getTransactionStateByContentHash(ledgerHash, contentHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/users/address/{address}")
	@RequestMapping(method = RequestMethod.GET, path = GET_USER)
	@Override
	public UserInfo getUser(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {
		return peerService.getQueryService(ledgerHash).getUser(ledgerHash, address);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/accounts/address/{address}")
	@RequestMapping(method = RequestMethod.GET, path = GET_DATA_ACCOUNT)
	@Override
	public DataAccountInfo getDataAccount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {

		return peerService.getQueryService(ledgerHash).getDataAccount(ledgerHash, address);
	}

//	@RequestMapping(method = { RequestMethod.GET,
//			RequestMethod.POST }, path = "ledgers/{ledgerHash}/accounts/{address}/entries")
	@RequestMapping(method = { RequestMethod.GET, RequestMethod.POST }, path = GET_LATEST_KV_LIST)
	@Override
	public TypedKVEntry[] getDataEntries(@PathVariable("ledgerHash") HashDigest ledgerHash,
			@PathVariable("address") String address, @RequestParam("keys") String... keys) {
		return peerService.getQueryService(ledgerHash).getDataEntries(ledgerHash, address, keys);
	}

//	@RequestMapping(method = { RequestMethod.GET,
//			RequestMethod.POST }, path = "ledgers/{ledgerHash}/accounts/{address}/entries-version")
	@RequestMapping(method = { RequestMethod.GET, RequestMethod.POST }, path = GET_KV_VERSION_LIST)
	@Override
	public TypedKVEntry[] getDataEntries(@PathVariable("ledgerHash") HashDigest ledgerHash,
			@PathVariable("address") String address, @RequestBody KVInfoVO kvInfoVO) {
		return peerService.getQueryService(ledgerHash).getDataEntries(ledgerHash, address, kvInfoVO);
	}

//	@RequestMapping(method = { RequestMethod.GET,
//			RequestMethod.POST }, path = "ledgers/{ledgerHash}/accounts/address/{address}/entries")
	@RequestMapping(method = { RequestMethod.GET, RequestMethod.POST }, path = GET_LATEST_KV_SEQUENCE)
	@Override
	public TypedKVEntry[] getDataEntries(@PathVariable("ledgerHash") HashDigest ledgerHash,
			@PathVariable("address") String address,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return peerService.getQueryService(ledgerHash).getDataEntries(ledgerHash, address, fromIndex, count);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/accounts/address/{address}/entries/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_KV_COUNT)
	@Override
	public long getDataEntriesTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {
		return peerService.getQueryService(ledgerHash).getDataEntriesTotalCount(ledgerHash, address);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/contracts/address/{address}")
	@RequestMapping(method = RequestMethod.GET, path = GET_LATEST_DECOMPILED_CONTRACT)
	@Override
	public DecompliedContractInfo getDecompiledContract(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {
		ContractInfo contractInfo = peerService.getQueryService(ledgerHash).getContract(ledgerHash, address);
		return decompileContract(contractInfo);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/contracts/address/{address}/version/{version}")
	@RequestMapping(method = RequestMethod.GET, path = GET_DECOMPILED_CONTRACT)
	@Override
	public DecompliedContractInfo getDecompiledContractByVersion(
			@PathVariable(name = "ledgerHash") HashDigest ledgerHash, @PathVariable(name = "address") String address,
			@PathVariable(name = "version") long version) {
		ContractInfo contractInfo = peerService.getQueryService(ledgerHash).getContract(ledgerHash, address, version);
		return decompileContract(contractInfo);
	}

	/**
	 * 反编译合约源码；
	 * 
	 * @param contractInfo
	 * @return
	 */
	private DecompliedContractInfo decompileContract(ContractInfo contractInfo) {
		if (null == contractInfo) {
			return null;
		}
		DecompliedContractInfo contractSettings = new DecompliedContractInfo(contractInfo);
		byte[] chainCodeBytes = contractInfo.getChainCode();
		try {
			// 将反编译chainCode
			String mainClassJava = CONTRACT_PROCESSOR.decompileEntranceClass(chainCodeBytes, contractInfo.getLang());
			contractSettings.setChainCode(mainClassJava);
		} catch (Exception e) {
			// 打印日志
			logger.error(String.format("Decompile contract[%s] error !!!", contractInfo.getAddress().toBase58()), e);
		}
		return contractSettings;
	}

//    @RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/contracts/address/{address}")
	@RequestMapping(method = RequestMethod.GET, path = GET_LATEST_COMPILED_CONTRACT)
	@Override
	public ContractInfo getContract(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
									@PathVariable(name = "address") String address) {
		return peerService.getQueryService(ledgerHash).getContract(ledgerHash, address);
	}

	@RequestMapping(method = RequestMethod.GET, path = GET_COMPILED_CONTRACT)
	@Override
	public ContractInfo getContract(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
									@PathVariable(name = "address") String address,
									@PathVariable(name = "version") long version) {
		return peerService.getQueryService(ledgerHash).getContract(ledgerHash, address, version);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/system/names/{eventName}")
	@RequestMapping(method = RequestMethod.GET, path = GET_SYSTEM_EVENT_SEQUENCE)
	@Override
	public Event[] getSystemEvents(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "eventName") String eventName,
			@RequestParam(name = "fromSequence", required = false, defaultValue = "0") long fromSequence,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return peerService.getEventListener().getSystemEvents(ledgerHash, eventName, fromSequence, count);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/system/names/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_SYSTEM_EVENT_SUBJECT_COUNT)
	@Override
	public long getSystemEventNameTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getSystemEventNameTotalCount(ledgerHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/system/names")
	@RequestMapping(method = RequestMethod.GET, path = GET_SYSTEM_EVENT_SUBJECTS)
	@Override
	public String[] getSystemEventNames(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "maxCount", required = false, defaultValue = "-1") int count) {
		return peerService.getQueryService(ledgerHash).getSystemEventNames(ledgerHash, fromIndex, count);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/system/names/{eventName}/latest")
	@RequestMapping(method = RequestMethod.GET, path = GET_LATEST_SYSTEM_EVENT)
	@Override
	public Event getLatestSystemEvent(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "eventName") String eventName) {
		return peerService.getQueryService(ledgerHash).getLatestSystemEvent(ledgerHash, eventName);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/system/names/{eventName}/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_SYSTEM_EVENT_COUNT)
	@Override
	public long getSystemEventsTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "eventName") String eventName) {
		return peerService.getQueryService(ledgerHash).getSystemEventsTotalCount(ledgerHash, eventName);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts")
	@RequestMapping(method = RequestMethod.GET, path = GET_EVENT_ACCOUNT_SEQUENCE)
	@Override
	public BlockchainIdentity[] getUserEventAccounts(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return peerService.getQueryService(ledgerHash).getUserEventAccounts(ledgerHash, fromIndex, count);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}")
	@RequestMapping(method = RequestMethod.GET, path = GET_EVENT_ACCOUNT)
	@Override
	public EventAccountInfo getUserEventAccount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
												@PathVariable(name = "address") String address) {
		return peerService.getQueryService(ledgerHash).getUserEventAccount(ledgerHash, address);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_TOTAL_EVENT_ACCOUNT_COUNT)
	@Override
	public long getUserEventAccountTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getUserEventAccountTotalCount(ledgerHash);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}/names/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_EVENT_SUBJECT_COUNT)
	@Override
	public long getUserEventNameTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address) {
		return peerService.getQueryService(ledgerHash).getUserEventNameTotalCount(ledgerHash, address);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}/names")
	@RequestMapping(method = RequestMethod.GET, path = GET_EVENT_SUBJECTS)
	@Override
	public String[] getUserEventNames(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return peerService.getQueryService(ledgerHash).getUserEventNames(ledgerHash, address, fromIndex, count);
	}
	
	@Deprecated
	@Override
	public Event getLatestEvent(HashDigest ledgerHash,
			String address, String eventName) {
		return getLatestUserEvent(ledgerHash, address, eventName);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}/names/{eventName}/latest")
	@RequestMapping(method = RequestMethod.GET, path = GET_LATEST_EVENT)
	@Override
	public Event getLatestUserEvent(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address, @PathVariable(name = "eventName") String eventName) {
		return peerService.getQueryService(ledgerHash).getLatestUserEvent(ledgerHash, address, eventName);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}/names/{eventName}/count")
	@RequestMapping(method = RequestMethod.GET, path = GET_EVENT_COUNT)
	@Override
	public long getUserEventsTotalCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address, @PathVariable(name = "eventName") String eventName) {
		return peerService.getQueryService(ledgerHash).getUserEventsTotalCount(ledgerHash, address, eventName);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/events/user/accounts/{address}/names/{eventName}")
	@RequestMapping(method = RequestMethod.GET, path = GET_EVENT_SEQUENCE)
	@Override
	public Event[] getUserEvents(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "address") String address, @PathVariable(name = "eventName") String eventName,
			@RequestParam(name = "fromSequence", required = false, defaultValue = "0") long fromSequence,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return peerService.getEventListener().getUserEvents(ledgerHash, address, eventName, fromSequence, count);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/latest")
	@RequestMapping(method = RequestMethod.GET, path = GET_LATEST_BLOCK)
	@Override
	public LedgerBlock getLatestBlock(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		long latestBlockHeight = peerService.getQueryService(ledgerHash).getLedger(ledgerHash).getLatestBlockHeight();
		return peerService.getQueryService(ledgerHash).getBlock(ledgerHash, latestBlockHeight);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/txs/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_TRANSACTION_COUNT_IN_BLOCK_HEIGHT)
	@Override
	public long getAdditionalTransactionCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		// 获取某个区块的交易总数
		long currentBlockTxCount = peerService.getQueryService(ledgerHash).getTransactionCount(ledgerHash, blockHeight);
		if (blockHeight == GENESIS_BLOCK_HEIGHT) {
			return currentBlockTxCount;
		}
		if(currentBlockTxCount == 0) {
			return 0;
		}
		long lastBlockHeight = blockHeight - 1;
		long lastBlockTxCount = peerService.getQueryService(ledgerHash).getTransactionCount(ledgerHash,
				lastBlockHeight);
		// 当前区块交易数减上个区块交易数
		return currentBlockTxCount - lastBlockTxCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/txs/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_TRANSACTION_COUNT_IN_BLOCK_HASH)
	@Override
	public long getAdditionalTransactionCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		LedgerBlock currentBlock = peerService.getQueryService(ledgerHash).getBlock(ledgerHash, blockHash);
		long currentBlockTxCount = peerService.getQueryService(ledgerHash).getTransactionCount(ledgerHash, blockHash);
		if (currentBlock.getHeight() == GENESIS_BLOCK_HEIGHT) {
			return currentBlockTxCount;
		}
		if(currentBlockTxCount == 0) {
			return 0;
		}
		HashDigest previousHash = currentBlock.getPreviousHash();
		long lastBlockTxCount = peerService.getQueryService(ledgerHash).getTransactionCount(ledgerHash, previousHash);
		// 当前区块交易数减上个区块交易数
		return currentBlockTxCount - lastBlockTxCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/txs/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_ADDITIONAL_TRANSACTION_COUNT)
	@Override
	public long getAdditionalTransactionCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerInfo ledgerInfo = peerService.getQueryService(ledgerHash).getLedger(ledgerHash);
		long maxBlockHeight = ledgerInfo.getLatestBlockHeight();
		long totalCount = peerService.getQueryService(ledgerHash).getTransactionTotalCount(ledgerHash);
		if (maxBlockHeight == GENESIS_BLOCK_HEIGHT) { // 只有一个创世区块
			return totalCount;
		}
		if(totalCount == 0) {
			return 0;
		}
		long lastTotalCount = peerService.getQueryService(ledgerHash).getTransactionCount(ledgerHash,
				maxBlockHeight - 1);
		return totalCount - lastTotalCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/accounts/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_ADDITIONAL_DATA_ACCOUNT_COUNT_IN_BLOCK_HEIGHT)
	@Override
	public long getAdditionalDataAccountCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		long currentDaCount = peerService.getQueryService(ledgerHash).getDataAccountCount(ledgerHash, blockHeight);
		if (blockHeight == GENESIS_BLOCK_HEIGHT) {
			return currentDaCount;
		}
		if(currentDaCount == 0) {
			return 0;
		}
		long lastBlockHeight = blockHeight - 1;
		long lastDaCount = peerService.getQueryService(ledgerHash).getDataAccountCount(ledgerHash, lastBlockHeight);
		return currentDaCount - lastDaCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/accounts/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_ADDITIONAL_DATA_ACCOUNT_COUNT_IN_BLOCK_HASH)
	@Override
	public long getAdditionalDataAccountCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		LedgerBlock currentBlock = peerService.getQueryService(ledgerHash).getBlock(ledgerHash, blockHash);
		long currentBlockDaCount = peerService.getQueryService(ledgerHash).getDataAccountCount(ledgerHash, blockHash);
		if (currentBlock.getHeight() == GENESIS_BLOCK_HEIGHT) {
			return currentBlockDaCount;
		}
		if(currentBlockDaCount == 0) {
			return 0;
		}
		HashDigest previousHash = currentBlock.getPreviousHash();
		long lastBlockDaCount = peerService.getQueryService(ledgerHash).getDataAccountCount(ledgerHash, previousHash);
		// 当前区块数据账户数量减上个区块数据账户数量
		return currentBlockDaCount - lastBlockDaCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/accounts/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_ADDITIONAL_DATA_ACCOUNT_COUNT)
	@Override
	public long getAdditionalDataAccountCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerInfo ledgerInfo = peerService.getQueryService(ledgerHash).getLedger(ledgerHash);
		long maxBlockHeight = ledgerInfo.getLatestBlockHeight();
		long totalCount = peerService.getQueryService(ledgerHash).getDataAccountTotalCount(ledgerHash);
		if (maxBlockHeight == GENESIS_BLOCK_HEIGHT) { // 只有一个创世区块
			return totalCount;
		}
		if(totalCount == 0) {
			return 0;
		}
		long lastTotalCount = peerService.getQueryService(ledgerHash).getDataAccountCount(ledgerHash,
				maxBlockHeight - 1);
		return totalCount - lastTotalCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/users/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_ADDITIONAL_USER_COUNT_IN_BLOCK_HEIGHT)
	@Override
	public long getAdditionalUserCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		long currentUserCount = peerService.getQueryService(ledgerHash).getUserCount(ledgerHash, blockHeight);
		if (blockHeight == GENESIS_BLOCK_HEIGHT) {
			return currentUserCount;
		}
		if(currentUserCount == 0) {
			return 0;
		}
		long lastBlockHeight = blockHeight - 1;
		long lastUserCount = peerService.getQueryService(ledgerHash).getUserCount(ledgerHash, lastBlockHeight);
		return currentUserCount - lastUserCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/users/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_ADDITIONAL_USER_COUNT_IN_BLOCK_HASH)
	@Override
	public long getAdditionalUserCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		LedgerBlock currentBlock = peerService.getQueryService(ledgerHash).getBlock(ledgerHash, blockHash);
		long currentBlockUserCount = peerService.getQueryService(ledgerHash).getUserCount(ledgerHash, blockHash);
		if (currentBlock.getHeight() == GENESIS_BLOCK_HEIGHT) {
			return currentBlockUserCount;
		}
		if(currentBlockUserCount == 0) {
			return 0;
		}
		HashDigest previousHash = currentBlock.getPreviousHash();
		long lastBlockUserCount = peerService.getQueryService(ledgerHash).getUserCount(ledgerHash, previousHash);
		// 当前区块用户数量减上个区块用户数量
		return currentBlockUserCount - lastBlockUserCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/users/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_ADDITIONAL_USER_COUNT)
	@Override
	public long getAdditionalUserCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerInfo ledgerInfo = peerService.getQueryService(ledgerHash).getLedger(ledgerHash);
		long maxBlockHeight = ledgerInfo.getLatestBlockHeight();
		long totalCount = peerService.getQueryService(ledgerHash).getUserTotalCount(ledgerHash);
		if (maxBlockHeight == GENESIS_BLOCK_HEIGHT) { // 只有一个创世区块
			return totalCount;
		}
		if(totalCount == 0) {
			return 0;
		}
		long lastTotalCount = peerService.getQueryService(ledgerHash).getUserCount(ledgerHash, maxBlockHeight - 1);
		return totalCount - lastTotalCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/height/{blockHeight}/contracts/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_ADDITIONAL_CONTRACT_COUNT_IN_BLOCK_HEIGHT)
	@Override
	public long getAdditionalContractCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHeight") long blockHeight) {
		long currentContractCount = peerService.getQueryService(ledgerHash).getContractCount(ledgerHash, blockHeight);
		if (blockHeight == GENESIS_BLOCK_HEIGHT) {
			return currentContractCount;
		}
		if(currentContractCount == 0) {
			return 0;
		}
		long lastBlockHeight = blockHeight - 1;
		long lastContractCount = peerService.getQueryService(ledgerHash).getContractCount(ledgerHash, lastBlockHeight);
		return currentContractCount - lastContractCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/blocks/hash/{blockHash}/contracts/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_ADDITIONAL_CONTRACT_COUNT_IN_BLOCK_HASH)
	@Override
	public long getAdditionalContractCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "blockHash") HashDigest blockHash) {
		LedgerBlock currentBlock = peerService.getQueryService(ledgerHash).getBlock(ledgerHash, blockHash);
		long currentBlockContractCount = peerService.getQueryService(ledgerHash).getContractCount(ledgerHash,
				blockHash);
		if (currentBlock.getHeight() == GENESIS_BLOCK_HEIGHT) {
			return currentBlockContractCount;
		}
		if(currentBlockContractCount == 0) {
			return 0;
		}
		HashDigest previousHash = currentBlock.getPreviousHash();
		long lastBlockContractCount = peerService.getQueryService(ledgerHash).getContractCount(ledgerHash,
				previousHash);
		// 当前区块合约数量减上个区块合约数量
		return currentBlockContractCount - lastBlockContractCount;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/contracts/additional-count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_ADDITIONAL_CONTRACT_COUNT)
	@Override
	public long getAdditionalContractCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		LedgerInfo ledgerInfo = peerService.getQueryService(ledgerHash).getLedger(ledgerHash);
		long maxBlockHeight = ledgerInfo.getLatestBlockHeight();
		long totalCount = peerService.getQueryService(ledgerHash).getContractTotalCount(ledgerHash);
		if (maxBlockHeight == GENESIS_BLOCK_HEIGHT) { // 只有一个创世区块
			return totalCount;
		}
		if(totalCount == 0) {
			return 0;
		}
		long lastTotalCount = peerService.getQueryService(ledgerHash).getContractCount(ledgerHash, maxBlockHeight - 1);
		return totalCount - lastTotalCount;
	}

	/**
	 * get all ledgers count;
	 */
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_LEGDER_COUNT)
	@Override
	public int getLedgersCount() {
		return peerService.getLedgerHashs().length;
	}

	// 注： 账本的数量不会很多，不需要分页；
//	/**
//	 * get all ledgers hashs;
//	 */
//	@RequestMapping(method = RequestMethod.GET, path = "ledgers")
//	public HashDigest[] getLedgersHash(
//			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
//			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
//		return gatewayQueryService.getLedgersHash(fromIndex, count);
//	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/participants/count")
	@RequestMapping(method = RequestMethod.GET, path = BlockchainBrowserService.GET_CONSENSUS_PARTICIPANT_COUNT)
	@Override
	public int getConsensusParticipantCount(@PathVariable(name = "ledgerHash") HashDigest ledgerHash) {
		return peerService.getQueryService(ledgerHash).getConsensusParticipants(ledgerHash).length;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/participants")
//	public ParticipantNode[] getConsensusParticipants(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
//			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
//			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
//
//		ParticipantNode participantNode[] = peerService.getQueryService().getConsensusParticipants(ledgerHash);
//		int indexAndCount[] = QueryUtil.calFromIndexAndCount(fromIndex, count, participantNode.length);
//		ParticipantNode participantNodesNew[] = Arrays.copyOfRange(participantNode, indexAndCount[0],
//				indexAndCount[0] + indexAndCount[1]);
//		return participantNodesNew;
//	}

	/**
	 * get more users by fromIndex and count;
	 *
	 * @param ledgerHash
	 * @param fromIndex
	 * @param count
	 * @return
	 */
//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/users")
	@RequestMapping(method = RequestMethod.GET, path = GET_USER_SEQUENCE)
	@Override
	public BlockchainIdentity[] getUsers(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return revertAccountHeader(peerService.getQueryService(ledgerHash).getUsers(ledgerHash, fromIndex, count));
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/accounts")
	@RequestMapping(method = RequestMethod.GET, path = GET_DATA_ACCOUNT_SEQUENCE)
	@Override
	public BlockchainIdentity[] getDataAccounts(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return revertAccountHeader(
				peerService.getQueryService(ledgerHash).getDataAccounts(ledgerHash, fromIndex, count));
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/contracts")
	@RequestMapping(method = RequestMethod.GET, path = GET_CONTRACT_ACCOUNT_SEQUENCE)
	@Override
	public BlockchainIdentity[] getContractAccounts(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@RequestParam(name = "fromIndex", required = false, defaultValue = "0") int fromIndex,
			@RequestParam(name = "count", required = false, defaultValue = "-1") int count) {
		return revertAccountHeader(
				peerService.getQueryService(ledgerHash).getContractAccounts(ledgerHash, fromIndex, count));
	}

	/**
	 * reverse the BlockchainIdentity[] content; the latest record show first;
	 * 
	 * @return
	 */
	private BlockchainIdentity[] revertAccountHeader(BlockchainIdentity[] accountHeaders) {
		BlockchainIdentity[] accounts = new BlockchainIdentity[accountHeaders.length];
		if (accountHeaders != null && accountHeaders.length > 0) {
			for (int i = 0; i < accountHeaders.length; i++) {
				accounts[accountHeaders.length - 1 - i] = accountHeaders[i];
			}
		}
		return accounts;
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/authorization/role/{roleName}")
	@RequestMapping(method = RequestMethod.GET, path = GET_ROLE_PRIVILEGES)
	@Override
	public PrivilegeSet getRolePrivileges(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "roleName") String roleName) {

		return peerService.getQueryService(ledgerHash).getRolePrivileges(ledgerHash, roleName);
	}

//	@RequestMapping(method = RequestMethod.GET, path = "ledgers/{ledgerHash}/authorization/user/{userAddress}")
	@RequestMapping(method = RequestMethod.GET, path = GET_USER_PRIVILEGES)
	@Override
	public UserPrivilegeSet getUserPrivileges(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
			@PathVariable(name = "userAddress") String userAddress) {
		return peerService.getQueryService(ledgerHash).getUserPrivileges(ledgerHash, userAddress);
	}

}

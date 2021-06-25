package com.jd.blockchain.gateway.service;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.gateway.service.search.Transaction;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.DataAccountInfo;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import utils.codec.Base58Utils;

import java.util.HashMap;
import java.util.Map;

@Service
public class DataSearchServiceHandler implements DataSearchService {

    @Autowired
    private LedgersService peerService;

    @Override
    public Map<String, Object> searchAll(HashDigest ledgerHash, String keyword) {
        Map<String, Object> data = new HashMap<>();
        data.put("combine", true);
        // 区块
        LedgerBlock block = searchBlock(ledgerHash, keyword);
        if (null != block) {
            data.put("blocks", new LedgerBlock[]{block});
        }
        // 交易
        Transaction tx = searchTransaction(ledgerHash, keyword);
        if (null != tx) {
            data.put("txs", new Transaction[]{tx});
        }
        // 数据账户
        DataAccountInfo dataAccount = searchDataAccount(ledgerHash, keyword);
        if (null != dataAccount) {
            data.put("accounts", new DataAccountInfo[]{dataAccount});
        }
        // 事件账户
        BlockchainIdentity eventAccount = searchEventAccount(ledgerHash, keyword);
        if (null != eventAccount) {
            data.put("event_accounts", new BlockchainIdentity[]{eventAccount});
        }
        // 合约
        ContractInfo contract = searchContractAccount(ledgerHash, keyword);
        if (null != contract) {
            data.put("contracts", new ContractInfo[]{contract});
        }
        // 用户
        UserInfo user = searchUser(ledgerHash, keyword);
        if (null != user) {
            data.put("users", new UserInfo[]{user});
        }
        return data;
    }

    @Override
    public int searchDataAccountCount(HashDigest ledgerHash, String address) {
        return null != peerService.getQueryService(ledgerHash).getDataAccount(ledgerHash, address) ? 1 : 0;
    }

    @Override
    public DataAccountInfo searchDataAccount(HashDigest ledgerHash, String address) {
        return peerService.getQueryService(ledgerHash).getDataAccount(ledgerHash, address);
    }

    @Override
    public int searchEventAccountCount(HashDigest ledgerHash, String address) {
        return null != peerService.getQueryService(ledgerHash).getUserEventAccount(ledgerHash, address) ? 1 : 0;
    }

    @Override
    public BlockchainIdentity searchEventAccount(HashDigest ledgerHash, String address) {
        return peerService.getQueryService(ledgerHash).getUserEventAccount(ledgerHash, address);
    }

    @Override
    public int searchContractAccountCount(HashDigest ledgerHash, String address) {
        return null != peerService.getQueryService(ledgerHash).getContract(ledgerHash, address) ? 1 : 0;
    }

    @Override
    public ContractInfo searchContractAccount(HashDigest ledgerHash, String address) {
        return peerService.getQueryService(ledgerHash).getContract(ledgerHash, address);
    }

    @Override
    public int searchUserCount(HashDigest ledgerHash, String address) {
        return null != peerService.getQueryService(ledgerHash).getUser(ledgerHash, address) ? 1 : 0;
    }

    @Override
    public UserInfo searchUser(HashDigest ledgerHash, String address) {
        return peerService.getQueryService(ledgerHash).getUser(ledgerHash, address);
    }

    @Override
    public LedgerBlock searchBlock(HashDigest ledgerHash, String blockHash) {
        try {
            return peerService.getQueryService(ledgerHash).getBlock(ledgerHash, Crypto.resolveAsHashDigest(Base58Utils.decode(blockHash)));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Transaction searchTransaction(HashDigest ledgerHash, String txHash) {
        try {
            LedgerTransaction tx = peerService.getQueryService(ledgerHash).getTransactionByContentHash(ledgerHash, Crypto.resolveAsHashDigest(Base58Utils.decode(txHash)));
            if(null != tx) {
                return new Transaction(tx.getRequest().getTransactionHash(), tx.getResult().getBlockHeight(), tx.getResult().getExecutionState());
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}

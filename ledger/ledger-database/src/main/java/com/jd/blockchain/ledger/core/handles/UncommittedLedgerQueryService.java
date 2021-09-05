package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.DataAccountInfo;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.KVDataVO;
import com.jd.blockchain.ledger.KVInfoVO;
import com.jd.blockchain.ledger.LedgerAdminInfo;
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
import com.jd.blockchain.ledger.core.EventPublishingAccount;
import com.jd.blockchain.ledger.core.LedgerAdminDataSet;
import com.jd.blockchain.ledger.core.LedgerDataSet;
import com.jd.blockchain.ledger.core.LedgerSecurityManager;
import com.jd.blockchain.ledger.core.LedgerSecurityManagerImpl;
import com.jd.blockchain.ledger.core.LedgerTransactionContext;
import com.jd.blockchain.ledger.core.UserAccountSet;
import com.jd.blockchain.ledger.LedgerQueryService;
import utils.Bytes;
import utils.DataEntry;
import utils.SkippingIterator;
import utils.query.QueryArgs;
import utils.query.QueryUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 包含未提交区块数据查询
 */
public class UncommittedLedgerQueryService implements LedgerQueryService {

    // 最新待提交区块数据
    private LedgerTransactionContext transactionContext;

    public UncommittedLedgerQueryService(LedgerTransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    @Override
    public LedgerAdminInfo getLedgerAdminInfo() {
        return (LedgerAdminInfo) transactionContext.getDataset().getAdminDataset();
    }

    @Override
    public ParticipantNode[] getConsensusParticipants() {
        return transactionContext.getDataset().getAdminDataset().getAdminSettings().getParticipants();
    }

    @Override
    public LedgerMetadata getLedgerMetadata() {
        return transactionContext.getDataset().getAdminDataset().getAdminSettings().getMetadata();
    }

    @Override
    public long getTransactionTotalCount() {
        return transactionContext.getTransactionSet().getTotalCount();
    }

    @Override
    public long getDataAccountTotalCount() {
        return transactionContext.getDataset().getDataAccountSet().getTotal();
    }

    @Override
    public long getUserTotalCount() {
        return transactionContext.getDataset().getUserAccountSet().getTotal();
    }

    @Override
    public long getContractTotalCount() {
        return transactionContext.getDataset().getContractAccountSet().getTotal();
    }

    @Override
    public LedgerTransaction getTransactionByContentHash(HashDigest contentHash) {
        return transactionContext.getTransactionSet().getTransaction(contentHash);
    }

    @Override
    public TransactionState getTransactionStateByContentHash(HashDigest contentHash) {
        return transactionContext.getTransactionSet().getState(contentHash);
    }

    @Override
    public UserInfo getUser(String address) {
        return transactionContext.getDataset().getUserAccountSet().getAccount(address);
    }

    @Override
    public DataAccountInfo getDataAccount(String address) {
        return transactionContext.getDataset().getDataAccountSet().getAccount(address);
    }

    @Override
    public TypedKVEntry[] getDataEntries(String address, String... keys) {
        DataAccount account = transactionContext.getDataset().getDataAccountSet().getAccount(address);
        TypedKVEntry[] entries = new TypedKVEntry[keys.length];
        long ver;
        for (int i = 0; i < entries.length; i++) {
            final String currKey = keys[i];

            ver = account == null ? -1 : account.getDataset().getVersion(currKey);

            if (ver < 0) {
                entries[i] = new TypedKVData(currKey, -1, null);
            } else {
                BytesValue value = account.getDataset().getValue(currKey, ver);
                entries[i] = new TypedKVData(currKey, ver, value);
            }
        }

        return entries;
    }

    @Override
    public TypedKVEntry[] getDataEntries(String address, KVInfoVO kvInfoVO) {
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

        if (keys.length == 0) {
            return null;
        }
        if (versions.length == 0) {
            return null;
        }
        if (keys.length != versions.length) {
            throw null;
        }

        DataAccount dataAccount = transactionContext.getDataset().getDataAccountSet().getAccount(address);

        TypedKVEntry[] entries = new TypedKVEntry[keys.length];
        long ver = -1L;
        for (int i = 0; i < entries.length; i++) {
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

    @Override
    public long getDataEntriesTotalCount(String address) {
        DataAccount account = transactionContext.getDataset().getDataAccountSet().getAccount(address);
        return null != account ? account.getDataset().getDataCount() : 0;
    }

    @Override
    public TypedKVEntry[] getDataEntries(String address, int fromIndex, int count) {
        DataAccountSet dataAccountSet = transactionContext.getDataset().getDataAccountSet();
        DataAccount dataAccount = dataAccountSet.getAccount(Bytes.fromBase58(address));

        QueryArgs queryArgs = QueryUtils.calFromIndexAndCount(fromIndex, count, (int) dataAccount.getDataset().getDataCount());
        SkippingIterator<DataEntry<String, TypedValue>> iterator = dataAccount.getDataset().iterator();
        iterator.skip(queryArgs.getFrom());

        TypedKVEntry[] typedKVEntries = iterator.next(queryArgs.getCount(), TypedKVEntry.class, entry -> new TypedKVData(entry.getKey(), entry.getVersion(), entry.getValue()));

        return typedKVEntries;
    }

    @Override
    public ContractInfo getContract(String address) {
        return transactionContext.getDataset().getContractAccountSet().getAccount(address);
    }

    @Override
    public Event[] getSystemEvents(String eventName, long fromSequence, int count) {
        return transactionContext.getEventSet().getSystemEventGroup().getEvents(eventName, fromSequence, count);
    }

    @Override
    public long getSystemEventNameTotalCount() {
        return transactionContext.getEventSet().getSystemEventGroup().totalEventNames();
    }

    @Override
    public String[] getSystemEventNames(int fromIndex, int count) {
        return transactionContext.getEventSet().getSystemEventGroup().getEventNames(fromIndex, count);
    }

    @Override
    public Event getLatestEvent(String eventName) {
        return transactionContext.getEventSet().getSystemEventGroup().getLatest(eventName);
    }

    @Override
    public long getSystemEventsTotalCount(String eventName) {
        return transactionContext.getEventSet().getSystemEventGroup().totalEvents(eventName);
    }

    @Override
    public BlockchainIdentity[] getUserEventAccounts(int fromIndex, int count) {
        EventAccountSet eventAccountSet = transactionContext.getEventSet().getEventAccountSet();
        QueryArgs queryArgs = QueryUtils.calFromIndexAndCountDescend(fromIndex, count, (int) eventAccountSet.getTotal());

        SkippingIterator<BlockchainIdentity> it = eventAccountSet.identityIterator();
        it.skip(queryArgs.getFrom());
        return it.next(queryArgs.getCount(), BlockchainIdentity.class);
    }

    @Override
    public BlockchainIdentity getUserEventAccount(String address) {
        return transactionContext.getEventSet().getEventAccountSet().getAccount(address);
    }

    @Override
    public long getUserEventAccountTotalCount() {
        return transactionContext.getEventSet().getEventAccountSet().getTotal();
    }

    @Override
    public long getUserEventNameTotalCount(String address) {
        EventPublishingAccount account = transactionContext.getEventSet().getEventAccountSet().getAccount(address);
        return null != account ? account.totalEventNames() : 0;
    }

    @Override
    public String[] getUserEventNames(String address, int fromIndex, int count) {
        EventPublishingAccount account = transactionContext.getEventSet().getEventAccountSet().getAccount(address);
        return null != account ? account.getEventNames(fromIndex, count) : null;
    }

    @Override
    public Event getLatestEvent(String address, String eventName) {
        EventPublishingAccount account = transactionContext.getEventSet().getEventAccountSet().getAccount(address);
        return null != account ? account.getLatest(eventName) : null;
    }

    @Override
    public long getUserEventsTotalCount(String address, String eventName) {
        EventPublishingAccount account = transactionContext.getEventSet().getEventAccountSet().getAccount(address);
        return null != account ? account.totalEvents(eventName) : 0;
    }

    @Override
    public Event[] getUserEvents(String address, String eventName, long fromSequence, int count) {
        EventPublishingAccount account = transactionContext.getEventSet().getEventAccountSet().getAccount(address);
        return null != account ? account.getEvents(eventName, fromSequence, count) : null;
    }

    @Override
    public ContractInfo getContract(String address, long version) {
        return transactionContext.getDataset().getContractAccountSet().getAccount(Bytes.fromBase58(address), version);
    }

    @Override
    public BlockchainIdentity[] getUsers(int fromIndex, int count) {
        UserAccountSet userAccountSet = transactionContext.getDataset().getUserAccountSet();
        QueryArgs queryArgs = QueryUtils.calFromIndexAndCountDescend(fromIndex, count, (int) userAccountSet.getTotal());

        SkippingIterator<BlockchainIdentity> it = userAccountSet.identityIterator();
        it.skip(queryArgs.getFrom());
        return it.next(queryArgs.getCount(), BlockchainIdentity.class);
    }

    @Override
    public BlockchainIdentity[] getDataAccounts(int fromIndex, int count) {
        DataAccountSet dataAccountSet = transactionContext.getDataset().getDataAccountSet();
        QueryArgs queryArgs = QueryUtils.calFromIndexAndCountDescend(fromIndex, count, (int) dataAccountSet.getTotal());

        SkippingIterator<BlockchainIdentity> it = dataAccountSet.identityIterator();
        it.skip(queryArgs.getFrom());
        return it.next(queryArgs.getCount(), BlockchainIdentity.class);
    }

    @Override
    public BlockchainIdentity[] getContractAccounts(int fromIndex, int count) {
        ContractAccountSet contractAccountSet = transactionContext.getDataset().getContractAccountSet();
        QueryArgs queryArgs = QueryUtils.calFromIndexAndCountDescend(fromIndex, count, (int) contractAccountSet.getTotal());

        SkippingIterator<BlockchainIdentity> it = contractAccountSet.identityIterator();
        it.skip(queryArgs.getFrom());
        return it.next(queryArgs.getCount(), BlockchainIdentity.class);
    }

    @Override
    public PrivilegeSet getRolePrivileges(String roleName) {
        return transactionContext.getDataset().getAdminDataset().getAdminSettings().getRolePrivileges().getRolePrivilege(roleName);
    }

    @Override
    public UserPrivilegeSet getUserPrivileges(String userAddress) {
        LedgerDataSet ledgerDataQuery = transactionContext.getDataset();
        LedgerAdminDataSet previousAdminDataset = ledgerDataQuery.getAdminDataset();
        LedgerSecurityManager securityManager = new LedgerSecurityManagerImpl(previousAdminDataset.getAdminSettings().getRolePrivileges(),
                previousAdminDataset.getAdminSettings().getAuthorizations(), previousAdminDataset.getParticipantDataset(),
                ledgerDataQuery.getUserAccountSet());
        UserPrivilegeSet userPrivilegeSet = securityManager.getUserRolesPrivilegs(Bytes.fromBase58(userAddress));
        return userPrivilegeSet;
    }

}

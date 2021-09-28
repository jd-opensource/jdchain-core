package com.jd.blockchain.ledger.core.handles;

import com.jd.blockchain.ca.CertificateUtils;
import com.jd.blockchain.contract.LedgerContext;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.AccountPermissionSetOperation;
import com.jd.blockchain.ledger.AccountState;
import com.jd.blockchain.ledger.AccountType;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.BlockchainIdentityData;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.BytesValueList;
import com.jd.blockchain.ledger.ContractCodeDeployOperation;
import com.jd.blockchain.ledger.ContractEventSendOperation;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.ContractStateUpdateOperation;
import com.jd.blockchain.ledger.DataAccountInfo;
import com.jd.blockchain.ledger.DataAccountKVSetOperation;
import com.jd.blockchain.ledger.DataAccountRegisterOperation;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.EventAccountInfo;
import com.jd.blockchain.ledger.EventAccountRegisterOperation;
import com.jd.blockchain.ledger.EventPublishOperation;
import com.jd.blockchain.ledger.KVInfoVO;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerInfo;
import com.jd.blockchain.ledger.LedgerMetadata;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.LedgerQueryService;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.PrivilegeSet;
import com.jd.blockchain.ledger.RolesConfigureOperation;
import com.jd.blockchain.ledger.RolesPolicy;
import com.jd.blockchain.ledger.RootCAUpdateOperationBuilder;
import com.jd.blockchain.ledger.TransactionPermission;
import com.jd.blockchain.ledger.TransactionState;
import com.jd.blockchain.ledger.TypedKVEntry;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.ledger.UserAuthorizeOperation;
import com.jd.blockchain.ledger.UserCAUpdateOperation;
import com.jd.blockchain.ledger.UserInfo;
import com.jd.blockchain.ledger.UserPrivilegeSet;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.UserStateUpdateOperation;
import com.jd.blockchain.ledger.core.OperationHandleContext;
import com.jd.blockchain.transaction.AccountPermissionSetOpTemplate;
import com.jd.blockchain.transaction.AccountPermissionSetOperationBuilder;
import com.jd.blockchain.transaction.BlockchainQueryService;
import com.jd.blockchain.transaction.ContractCodeDeployOpTemplate;
import com.jd.blockchain.transaction.ContractCodeDeployOperationBuilder;
import com.jd.blockchain.transaction.ContractEventSendOpTemplate;
import com.jd.blockchain.transaction.ContractOperationBuilder;
import com.jd.blockchain.transaction.ContractStateUpdateOpTemplate;
import com.jd.blockchain.transaction.DataAccountKVSetOperationBuilder;
import com.jd.blockchain.transaction.DataAccountOperationBuilder;
import com.jd.blockchain.transaction.DataAccountRegisterOperationBuilder;
import com.jd.blockchain.transaction.DataAccountRegisterOperationBuilderImpl;
import com.jd.blockchain.transaction.EventAccountRegisterOperationBuilder;
import com.jd.blockchain.transaction.EventAccountRegisterOperationBuilderImpl;
import com.jd.blockchain.transaction.EventData;
import com.jd.blockchain.transaction.EventOperationBuilder;
import com.jd.blockchain.transaction.EventPublishOperationBuilder;
import com.jd.blockchain.transaction.KVData;
import com.jd.blockchain.transaction.MetaInfoUpdateOperationBuilder;
import com.jd.blockchain.transaction.RootCAUpdateOpTemplate;
import com.jd.blockchain.transaction.SimpleSecurityOperationBuilder;
import com.jd.blockchain.transaction.UserCAUpdateOpTemplate;
import com.jd.blockchain.transaction.UserRegisterOperationBuilder;
import com.jd.blockchain.transaction.UserRegisterOperationBuilderImpl;
import com.jd.blockchain.transaction.UserStateUpdateOpTemplate;
import com.jd.blockchain.transaction.UserUpdateOperationBuilder;
import utils.ArrayUtils;
import utils.Bytes;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 合约内账本上下文
 */
public class ContractLedgerContext implements LedgerContext {

    private LedgerQueryService innerQueryService;
    private BlockchainQueryService multiLedgerQueryService;

    private OperationHandleContext opHandleContext;

    private List<Operation> generatedOpList = new ArrayList<>();

    public ContractLedgerContext(OperationHandleContext opHandleContext, LedgerQueryService innerQueryService, BlockchainQueryService multiLedgerQueryService) {
        this.opHandleContext = opHandleContext;
        this.innerQueryService = innerQueryService;
        this.multiLedgerQueryService = multiLedgerQueryService;
    }

    @Override
    public LedgerAdminInfo getLedgerAdminInfo() {
        return innerQueryService.getLedgerAdminInfo();
    }

    @Override
    public ParticipantNode[] getConsensusParticipants() {
        return innerQueryService.getConsensusParticipants();
    }

    @Override
    public LedgerMetadata getLedgerMetadata() {
        return innerQueryService.getLedgerMetadata();
    }

    @Override
    public long getTransactionTotalCount() {
        return innerQueryService.getTransactionTotalCount();
    }

    @Override
    public long getDataAccountTotalCount() {
        return innerQueryService.getDataAccountTotalCount();
    }

    @Override
    public long getUserTotalCount() {
        return innerQueryService.getUserTotalCount();
    }

    @Override
    public long getContractTotalCount() {
        return innerQueryService.getContractTotalCount();
    }

    @Override
    public LedgerTransaction getTransactionByContentHash(HashDigest contentHash) {
        return innerQueryService.getTransactionByContentHash(contentHash);
    }

    @Override
    public TransactionState getTransactionStateByContentHash(HashDigest contentHash) {
        return innerQueryService.getTransactionStateByContentHash(contentHash);
    }

    @Override
    public UserInfo getUser(String address) {
        return innerQueryService.getUser(address);
    }

    @Override
    public DataAccountInfo getDataAccount(String address) {
        return innerQueryService.getDataAccount(address);
    }

    @Override
    public TypedKVEntry[] getDataEntries(String address, String... keys) {
        return innerQueryService.getDataEntries(address, keys);
    }

    @Override
    public TypedKVEntry[] getDataEntries(String address, KVInfoVO kvInfoVO) {
        return innerQueryService.getDataEntries(address, kvInfoVO);
    }

    @Override
    public long getDataEntriesTotalCount(String address) {
        return innerQueryService.getDataEntriesTotalCount(address);
    }

    @Override
    public TypedKVEntry[] getDataEntries(String address, int fromIndex, int count) {
        return innerQueryService.getDataEntries(address, fromIndex, count);
    }

    @Override
    public ContractInfo getContract(String address) {
        return innerQueryService.getContract(address);
    }

    @Override
    public Event[] getSystemEvents(String eventName, long fromSequence, int count) {
        return innerQueryService.getSystemEvents(eventName, fromSequence, count);
    }

    @Override
    public long getSystemEventNameTotalCount() {
        return innerQueryService.getSystemEventNameTotalCount();
    }

    @Override
    public String[] getSystemEventNames(int fromIndex, int count) {
        return innerQueryService.getSystemEventNames(fromIndex, count);
    }

    @Override
    public Event getLatestEvent(String eventName) {
        return innerQueryService.getLatestEvent(eventName);
    }

    @Override
    public long getSystemEventsTotalCount(String eventName) {
        return innerQueryService.getSystemEventsTotalCount(eventName);
    }

    @Override
    public BlockchainIdentity[] getUserEventAccounts(int fromIndex, int count) {
        return innerQueryService.getUserEventAccounts(fromIndex, count);
    }

    @Override
    public EventAccountInfo getUserEventAccount(String address) {
        return innerQueryService.getUserEventAccount(address);
    }

    @Override
    public long getUserEventAccountTotalCount() {
        return innerQueryService.getUserEventAccountTotalCount();
    }

    @Override
    public long getUserEventNameTotalCount(String address) {
        return innerQueryService.getUserEventNameTotalCount(address);
    }

    @Override
    public String[] getUserEventNames(String address, int fromIndex, int count) {
        return innerQueryService.getUserEventNames(address, fromIndex, count);
    }

    @Override
    public Event getLatestEvent(String address, String eventName) {
        return innerQueryService.getLatestEvent(address, eventName);
    }

    @Override
    public long getUserEventsTotalCount(String address, String eventName) {
        return innerQueryService.getUserEventsTotalCount(address, eventName);
    }

    @Override
    public Event[] getUserEvents(String address, String eventName, long fromSequence, int count) {
        return innerQueryService.getUserEvents(address, eventName, fromSequence, count);
    }

    @Override
    public ContractInfo getContract(String address, long version) {
        return innerQueryService.getContract(address, version);
    }

    @Override
    public BlockchainIdentity[] getUsers(int fromIndex, int count) {
        return innerQueryService.getUsers(fromIndex, count);
    }

    @Override
    public BlockchainIdentity[] getDataAccounts(int fromIndex, int count) {
        return innerQueryService.getDataAccounts(fromIndex, count);
    }

    @Override
    public BlockchainIdentity[] getContractAccounts(int fromIndex, int count) {
        return innerQueryService.getContractAccounts(fromIndex, count);
    }

    @Override
    public PrivilegeSet getRolePrivileges(String roleName) {
        return innerQueryService.getRolePrivileges(roleName);
    }

    @Override
    public UserPrivilegeSet getUserPrivileges(String userAddress) {
        return innerQueryService.getUserPrivileges(userAddress);
    }

    @Override
    public UserRegisterOperationBuilder users() {
        return new UserRegisterOperationBuilder1();
    }

    @Override
    public UserUpdateOperationBuilder user(String address) {
        return user(Bytes.fromBase58(address));
    }

    @Override
    public UserUpdateOperationBuilder user(Bytes address) {
        return new UserUpdateOperationBuilder1(address);
    }

    @Override
    public DataAccountRegisterOperationBuilder dataAccounts() {
        return new DataAccountRegisterOperationBuilder1();
    }

    @Override
    public DataAccountOperationBuilder dataAccount(String accountAddress) {
        return new DataAccountOperationExecBuilder(Bytes.fromBase58(accountAddress));
    }

    @Override
    public DataAccountOperationBuilder dataAccount(Bytes accountAddress) {
        return new DataAccountOperationExecBuilder(accountAddress);
    }

    @Override
    public EventAccountRegisterOperationBuilder eventAccounts() {
        return new EventAccountRegisterOperationBuilder1();
    }

    @Override
    public EventOperationBuilder eventAccount(String accountAddress) {
        return new EventOperationBuilder1(Bytes.fromBase58(accountAddress));
    }

    @Override
    public EventOperationBuilder eventAccount(Bytes accountAddress) {
        return new EventOperationBuilder1(accountAddress);
    }

    @Override
    public MetaInfoUpdateOperationBuilder metaInfo() {
        return new MetaInfoUpdateOperationBuilder1();
    }

    @Override
    public ContractCodeDeployOperationBuilder contracts() {
        return new ContractCodeDeployOperationBuilder1();
    }

    @Override
    public ContractOperationBuilder contract(Bytes address) {
        return new ContractOperationBuilder1(address);
    }

    @Override
    public ContractOperationBuilder contract(String address) {
        return contract(Bytes.fromBase58(address));
    }

    @Override
    @Deprecated
    public HashDigest[] getLedgerHashs() {
        return multiLedgerQueryService.getLedgerHashs();
    }

    @Override
    @Deprecated
    public LedgerInfo getLedger(HashDigest ledgerHash) {
        return multiLedgerQueryService.getLedger(ledgerHash);
    }

    @Override
    @Deprecated
    public LedgerAdminInfo getLedgerAdminInfo(HashDigest ledgerHash) {
        return multiLedgerQueryService.getLedgerAdminInfo(ledgerHash);
    }

    @Override
    @Deprecated
    public ParticipantNode[] getConsensusParticipants(HashDigest ledgerHash) {
        return multiLedgerQueryService.getConsensusParticipants(ledgerHash);
    }

    @Override
    @Deprecated
    public LedgerMetadata getLedgerMetadata(HashDigest ledgerHash) {
        return multiLedgerQueryService.getLedgerMetadata(ledgerHash);
    }

    @Override
    @Deprecated
    public LedgerBlock getBlock(HashDigest ledgerHash, long height) {
        return multiLedgerQueryService.getBlock(ledgerHash, height);
    }

    @Override
    @Deprecated
    public LedgerBlock getBlock(HashDigest ledgerHash, HashDigest blockHash) {
        return multiLedgerQueryService.getBlock(ledgerHash, blockHash);
    }

    @Override
    @Deprecated
    public long getTransactionCount(HashDigest ledgerHash, long height) {
        return multiLedgerQueryService.getTransactionCount(ledgerHash, height);
    }

    @Override
    @Deprecated
    public long getTransactionCount(HashDigest ledgerHash, HashDigest blockHash) {
        return multiLedgerQueryService.getTransactionCount(ledgerHash, blockHash);
    }

    @Override
    @Deprecated
    public long getTransactionTotalCount(HashDigest ledgerHash) {
        return multiLedgerQueryService.getTransactionTotalCount(ledgerHash);
    }

    // 以下查询为了兼容旧版本

    @Override
    @Deprecated
    public long getDataAccountCount(HashDigest ledgerHash, long height) {
        return multiLedgerQueryService.getDataAccountCount(ledgerHash, height);
    }

    @Override
    @Deprecated
    public long getDataAccountCount(HashDigest ledgerHash, HashDigest blockHash) {
        return multiLedgerQueryService.getDataAccountCount(ledgerHash, blockHash);
    }

    @Override
    @Deprecated
    public long getDataAccountTotalCount(HashDigest ledgerHash) {
        return multiLedgerQueryService.getDataAccountTotalCount(ledgerHash);
    }

    @Override
    @Deprecated
    public long getUserCount(HashDigest ledgerHash, long height) {
        return multiLedgerQueryService.getUserCount(ledgerHash, height);
    }

    @Override
    @Deprecated
    public long getUserCount(HashDigest ledgerHash, HashDigest blockHash) {
        return multiLedgerQueryService.getUserCount(ledgerHash, blockHash);
    }

    @Override
    @Deprecated
    public long getUserTotalCount(HashDigest ledgerHash) {
        return multiLedgerQueryService.getUserTotalCount(ledgerHash);
    }

    @Override
    @Deprecated
    public long getContractCount(HashDigest ledgerHash, long height) {
        return multiLedgerQueryService.getContractCount(ledgerHash, height);
    }

    @Override
    @Deprecated
    public long getContractCount(HashDigest ledgerHash, HashDigest blockHash) {
        return multiLedgerQueryService.getContractCount(ledgerHash, blockHash);
    }

    @Override
    @Deprecated
    public long getContractTotalCount(HashDigest ledgerHash) {
        return multiLedgerQueryService.getContractTotalCount(ledgerHash);
    }

    @Override
    @Deprecated
    public LedgerTransaction[] getTransactions(HashDigest ledgerHash, long height, int fromIndex, int count) {
        return multiLedgerQueryService.getTransactions(ledgerHash, height, fromIndex, count);
    }

    @Override
    @Deprecated
    public LedgerTransaction[] getTransactions(HashDigest ledgerHash, HashDigest blockHash, int fromIndex, int count) {
        return multiLedgerQueryService.getTransactions(ledgerHash, blockHash, fromIndex, count);
    }

    @Override
    @Deprecated
    public LedgerTransaction[] getAdditionalTransactions(HashDigest ledgerHash, long height, int fromIndex, int count) {
        return multiLedgerQueryService.getAdditionalTransactions(ledgerHash, height, fromIndex, count);
    }

    @Override
    @Deprecated
    public LedgerTransaction[] getAdditionalTransactions(HashDigest ledgerHash, HashDigest blockHash, int fromIndex, int count) {
        return multiLedgerQueryService.getAdditionalTransactions(ledgerHash, blockHash, fromIndex, count);
    }

    @Override
    @Deprecated
    public LedgerTransaction getTransactionByContentHash(HashDigest ledgerHash, HashDigest contentHash) {
        return multiLedgerQueryService.getTransactionByContentHash(ledgerHash, contentHash);
    }

    @Override
    @Deprecated
    public TransactionState getTransactionStateByContentHash(HashDigest ledgerHash, HashDigest contentHash) {
        return multiLedgerQueryService.getTransactionStateByContentHash(ledgerHash, contentHash);
    }

    @Override
    @Deprecated
    public UserInfo getUser(HashDigest ledgerHash, String address) {
        return multiLedgerQueryService.getUser(ledgerHash, address);
    }

    @Override
    @Deprecated
    public DataAccountInfo getDataAccount(HashDigest ledgerHash, String address) {
        return multiLedgerQueryService.getDataAccount(ledgerHash, address);
    }

    @Override
    @Deprecated
    public TypedKVEntry[] getDataEntries(HashDigest ledgerHash, String address, String... keys) {
        return multiLedgerQueryService.getDataEntries(ledgerHash, address, keys);
    }

    @Override
    @Deprecated
    public TypedKVEntry[] getDataEntries(HashDigest ledgerHash, String address, KVInfoVO kvInfoVO) {
        return multiLedgerQueryService.getDataEntries(ledgerHash, address, kvInfoVO);
    }

    @Override
    @Deprecated
    public TypedKVEntry[] getDataEntries(HashDigest ledgerHash, String address, int fromIndex, int count) {
        return multiLedgerQueryService.getDataEntries(ledgerHash, address, fromIndex, count);
    }

    @Override
    @Deprecated
    public long getDataEntriesTotalCount(HashDigest ledgerHash, String address) {
        return multiLedgerQueryService.getDataEntriesTotalCount(ledgerHash, address);
    }

    @Override
    @Deprecated
    public ContractInfo getContract(HashDigest ledgerHash, String address) {
        return multiLedgerQueryService.getContract(ledgerHash, address);
    }

    @Override
    @Deprecated
    public Event[] getSystemEvents(HashDigest ledgerHash, String eventName, long fromSequence, int maxCount) {
        return multiLedgerQueryService.getSystemEvents(ledgerHash, eventName, fromSequence, maxCount);
    }

    @Override
    @Deprecated
    public long getSystemEventNameTotalCount(HashDigest ledgerHash) {
        return multiLedgerQueryService.getSystemEventNameTotalCount(ledgerHash);
    }

    @Override
    @Deprecated
    public String[] getSystemEventNames(HashDigest ledgerHash, int fromIndex, int count) {
        return multiLedgerQueryService.getSystemEventNames(ledgerHash, fromIndex, count);
    }

    @Override
    @Deprecated
    public Event getLatestSystemEvent(HashDigest ledgerHash, String eventName) {
        return multiLedgerQueryService.getLatestSystemEvent(ledgerHash, eventName);
    }

    @Override
    @Deprecated
    public long getSystemEventsTotalCount(HashDigest ledgerHash, String eventName) {
        return multiLedgerQueryService.getSystemEventsTotalCount(ledgerHash, eventName);
    }

    @Override
    @Deprecated
    public BlockchainIdentity[] getUserEventAccounts(HashDigest ledgerHash, int fromIndex, int count) {
        return multiLedgerQueryService.getUserEventAccounts(ledgerHash, fromIndex, count);
    }

    @Override
    @Deprecated
    public EventAccountInfo getUserEventAccount(HashDigest ledgerHash, String address) {
        return multiLedgerQueryService.getUserEventAccount(ledgerHash, address);
    }

    @Override
    @Deprecated
    public long getUserEventAccountTotalCount(HashDigest ledgerHash) {
        return multiLedgerQueryService.getUserEventAccountTotalCount(ledgerHash);
    }

    @Override
    @Deprecated
    public long getUserEventNameTotalCount(HashDigest ledgerHash, String address) {
        return multiLedgerQueryService.getUserEventNameTotalCount(ledgerHash, address);
    }

    @Override
    @Deprecated
    public String[] getUserEventNames(HashDigest ledgerHash, String address, int fromSequence, int count) {
        return multiLedgerQueryService.getUserEventNames(ledgerHash, address, fromSequence, count);
    }

    @Deprecated
    @Override
    public Event getLatestEvent(HashDigest ledgerHash, String address, String eventName) {
        return getLatestUserEvent(ledgerHash, address, eventName);
    }

    @Override
    @Deprecated
    public Event getLatestUserEvent(HashDigest ledgerHash, String address, String eventName) {
        return multiLedgerQueryService.getLatestUserEvent(ledgerHash, address, eventName);
    }

    @Override
    @Deprecated
    public long getUserEventsTotalCount(HashDigest ledgerHash, String address, String eventName) {
        return multiLedgerQueryService.getUserEventsTotalCount(ledgerHash, address, eventName);
    }

    @Override
    @Deprecated
    public Event[] getUserEvents(HashDigest ledgerHash, String address, String eventName, long fromSequence, int count) {
        return multiLedgerQueryService.getUserEvents(ledgerHash, address, eventName, fromSequence, count);
    }

    @Override
    @Deprecated
    public ContractInfo getContract(HashDigest ledgerHash, String address, long version) {
        return multiLedgerQueryService.getContract(ledgerHash, address, version);
    }

    @Override
    @Deprecated
    public BlockchainIdentity[] getUsers(HashDigest ledgerHash, int fromIndex, int count) {
        return multiLedgerQueryService.getUsers(ledgerHash, fromIndex, count);
    }

    @Override
    @Deprecated
    public BlockchainIdentity[] getDataAccounts(HashDigest ledgerHash, int fromIndex, int count) {
        return multiLedgerQueryService.getDataAccounts(ledgerHash, fromIndex, count);
    }

    @Override
    @Deprecated
    public BlockchainIdentity[] getContractAccounts(HashDigest ledgerHash, int fromIndex, int count) {
        return multiLedgerQueryService.getContractAccounts(ledgerHash, fromIndex, count);
    }

    @Override
    @Deprecated
    public PrivilegeSet getRolePrivileges(HashDigest ledgerHash, String roleName) {
        return multiLedgerQueryService.getRolePrivileges(ledgerHash, roleName);
    }

    @Override
    @Deprecated
    public UserPrivilegeSet getUserPrivileges(HashDigest ledgerHash, String userAddress) {
        return multiLedgerQueryService.getUserPrivileges(ledgerHash, userAddress);
    }

    @Override
    public SimpleSecurityOperationBuilder security() {
        return new SecurityOperationBuilder1();
    }

    private class ContractCodeDeployOperationBuilder1 implements ContractCodeDeployOperationBuilder {

        @Override
        public ContractCodeDeployOperation deploy(BlockchainIdentity id, byte[] chainCode) {
            ContractCodeDeployOperation op = new ContractCodeDeployOpTemplate(id, chainCode);
            opHandleContext.handle(op);
            return op;
        }

        @Override
        public ContractCodeDeployOperation deploy(BlockchainIdentity id, byte[] chainCode, long version) {
            ContractCodeDeployOperation op = new ContractCodeDeployOpTemplate(id, chainCode, version);
            opHandleContext.handle(op);
            return op;
        }
    }

    private class ContractOperationBuilder1 implements ContractOperationBuilder {

        private Bytes address;

        ContractOperationBuilder1(Bytes address) {
            this.address = address;
        }

        @Override
        public ContractStateUpdateOperation revoke() {
            ContractStateUpdateOperation op = new ContractStateUpdateOpTemplate(address, AccountState.REVOKE);
            opHandleContext.handle(op);
            return op;
        }

        @Override
        public ContractStateUpdateOperation freeze() {
            ContractStateUpdateOperation op = new ContractStateUpdateOpTemplate(address, AccountState.FREEZE);
            opHandleContext.handle(op);
            return op;
        }

        @Override
        public ContractStateUpdateOperation restore() {
            ContractStateUpdateOperation op = new ContractStateUpdateOpTemplate(address, AccountState.NORMAL);
            opHandleContext.handle(op);
            return op;
        }

        @Override
        public ContractStateUpdateOperation state(AccountState state) {
            ContractStateUpdateOperation op = new ContractStateUpdateOpTemplate(address, state);
            opHandleContext.handle(op);
            return op;
        }

        @Override
        public AccountPermissionSetOperationBuilder permission() {
            AccountPermissionSetOperationBuilder builder = new AccountPermissionSetOperationBuilder1(address, AccountType.CONTRACT);
            return builder;
        }

        @Override
        public ContractEventSendOperation invoke(String event, BytesValueList args) {
            ContractEventSendOperation op = new ContractEventSendOpTemplate(address, event, args);
            opHandleContext.handle(op);
            return op;
        }
    }

    private class AccountPermissionSetOperationBuilder1 implements AccountPermissionSetOperationBuilder {

        private Bytes address;
        private AccountType accountType;
        private AccountPermissionSetOpTemplate op;

        public AccountPermissionSetOperationBuilder1(Bytes accountAddress, AccountType accountType) {
            this.address = accountAddress;
            this.accountType = accountType;
        }

        @Override
        public AccountPermissionSetOperation getOperation() {
            return op;
        }

        @Override
        public AccountPermissionSetOperationBuilder mode(int mode) {
            op = new AccountPermissionSetOpTemplate(address, accountType);
            op.setMode(mode);
            opHandleContext.handle(op);
            return this;
        }

        @Override
        public AccountPermissionSetOperationBuilder role(String role) {
            op = new AccountPermissionSetOpTemplate(address, accountType);
            op.setRole(role);
            opHandleContext.handle(op);
            return this;
        }
    }

    private class MetaInfoUpdateOperationBuilder1 implements MetaInfoUpdateOperationBuilder {

        @Override
        public RootCAUpdateOperationBuilder ca() {
            return new RootCAUpdateOperationBuilder1();
        }
    }

    private class RootCAUpdateOperationBuilder1 implements RootCAUpdateOperationBuilder {

        @Override
        public RootCAUpdateOperationBuilder add(String certificate) {
            RootCAUpdateOpTemplate op = new RootCAUpdateOpTemplate();
            op.addCertificate(certificate);
            opHandleContext.handle(op);
            return this;
        }

        @Override
        public RootCAUpdateOperationBuilder add(X509Certificate certificate) {
            RootCAUpdateOpTemplate op = new RootCAUpdateOpTemplate();
            op.addCertificate(CertificateUtils.toPEMString(certificate));
            opHandleContext.handle(op);
            return this;
        }

        @Override
        public RootCAUpdateOperationBuilder update(String certificate) {
            RootCAUpdateOpTemplate op = new RootCAUpdateOpTemplate();
            op.updateCertificate(certificate);
            opHandleContext.handle(op);
            return this;
        }

        @Override
        public RootCAUpdateOperationBuilder update(X509Certificate certificate) {
            RootCAUpdateOpTemplate op = new RootCAUpdateOpTemplate();
            op.updateCertificate(CertificateUtils.toPEMString(certificate));
            opHandleContext.handle(op);
            return this;
        }

        @Override
        public RootCAUpdateOperationBuilder remove(String certificate) {
            RootCAUpdateOpTemplate op = new RootCAUpdateOpTemplate();
            op.removeCertificate(certificate);
            opHandleContext.handle(op);
            return this;
        }

        @Override
        public RootCAUpdateOperationBuilder remove(X509Certificate certificate) {
            RootCAUpdateOpTemplate op = new RootCAUpdateOpTemplate();
            op.removeCertificate(CertificateUtils.toPEMString(certificate));
            opHandleContext.handle(op);
            return this;
        }
    }

    private class DataAccountRegisterOperationBuilder1 implements DataAccountRegisterOperationBuilder {
        @Override
        public DataAccountRegisterOperation register(BlockchainIdentity accountID) {
            final DataAccountRegisterOperationBuilderImpl DATA_ACC_REG_OP_BUILDER = new DataAccountRegisterOperationBuilderImpl();
            DataAccountRegisterOperation op = DATA_ACC_REG_OP_BUILDER.register(accountID);
            generatedOpList.add(op);
            opHandleContext.handle(op);
            return op;
        }
    }

    private class UserRegisterOperationBuilder1 implements UserRegisterOperationBuilder {
        private final UserRegisterOperationBuilderImpl USER_REG_OP_BUILDER = new UserRegisterOperationBuilderImpl();

        @Override
        public UserRegisterOperation register(BlockchainIdentity userID) {
            UserRegisterOperation op = USER_REG_OP_BUILDER.register(userID);
            generatedOpList.add(op);
            opHandleContext.handle(op);
            return op;
        }

        @Override
        public UserRegisterOperation register(X509Certificate certificate) {
            return register(new BlockchainIdentityData(CertificateUtils.resolvePubKey(certificate)));
        }
    }

    private class UserUpdateOperationBuilder1 implements UserUpdateOperationBuilder {

        private Bytes address;

        public UserUpdateOperationBuilder1(Bytes address) {
            this.address = address;
        }

        @Override
        public UserStateUpdateOperation revoke() {
            UserStateUpdateOperation op = new UserStateUpdateOpTemplate(address, AccountState.REVOKE);
            generatedOpList.add(op);
            opHandleContext.handle(op);
            return op;
        }

        @Override
        public UserStateUpdateOperation freeze() {
            UserStateUpdateOperation op = new UserStateUpdateOpTemplate(address, AccountState.FREEZE);
            generatedOpList.add(op);
            opHandleContext.handle(op);
            return op;
        }

        @Override
        public UserStateUpdateOperation restore() {
            UserStateUpdateOperation op = new UserStateUpdateOpTemplate(address, AccountState.NORMAL);
            generatedOpList.add(op);
            opHandleContext.handle(op);
            return op;
        }

        @Override
        public UserStateUpdateOperation state(AccountState state) {
            UserStateUpdateOperation op = new UserStateUpdateOpTemplate(address, state);
            generatedOpList.add(op);
            opHandleContext.handle(op);
            return op;
        }

        @Override
        public UserCAUpdateOperation ca(X509Certificate cert) {
            UserCAUpdateOperation op = new UserCAUpdateOpTemplate(address, cert);
            generatedOpList.add(op);
            opHandleContext.handle(op);
            return op;
        }
    }

    private class EventAccountRegisterOperationBuilder1 implements EventAccountRegisterOperationBuilder {
        @Override
        public EventAccountRegisterOperation register(BlockchainIdentity accountID) {
            final EventAccountRegisterOperationBuilderImpl EVENT_ACC_REG_OP_BUILDER = new EventAccountRegisterOperationBuilderImpl();
            EventAccountRegisterOperation op = EVENT_ACC_REG_OP_BUILDER.register(accountID);
            generatedOpList.add(op);
            opHandleContext.handle(op);
            return op;
        }
    }

    private class DataAccountOperationExecBuilder implements DataAccountOperationBuilder {

        private Bytes accountAddress;

        private SingleKVSetOpTemplate op;

        public DataAccountOperationExecBuilder(Bytes accountAddress) {
            this.accountAddress = accountAddress;
        }

        @Override
        public DataAccountKVSetOperation getOperation() {
            return op;
        }

        @Override
        public DataAccountKVSetOperationBuilder set(String key, BytesValue value, long expVersion) {
            this.op = new SingleKVSetOpTemplate(key, value, expVersion);
            handle(op);
            return this;
        }

        @Override
        public DataAccountKVSetOperationBuilder setText(String key, String value, long expVersion) {
            BytesValue bytesValue = TypedValue.fromText(value);
            this.op = new SingleKVSetOpTemplate(key, bytesValue, expVersion);
            handle(op);
            return this;
        }

        @Override
        public DataAccountKVSetOperationBuilder setBytes(String key, Bytes value, long expVersion) {
            BytesValue bytesValue = TypedValue.fromBytes(value);
            this.op = new SingleKVSetOpTemplate(key, bytesValue, expVersion);
            handle(op);
            return this;
        }

        @Override
        public DataAccountKVSetOperationBuilder setInt64(String key, long value, long expVersion) {
            BytesValue bytesValue = TypedValue.fromInt64(value);
            this.op = new SingleKVSetOpTemplate(key, bytesValue, expVersion);
            handle(op);
            return this;
        }

        @Override
        public DataAccountKVSetOperationBuilder setJSON(String key, String value, long expVersion) {
            BytesValue bytesValue = TypedValue.fromJSON(value);
            this.op = new SingleKVSetOpTemplate(key, bytesValue, expVersion);
            handle(op);
            return this;
        }

        @Override
        public DataAccountKVSetOperationBuilder setXML(String key, String value, long expVersion) {
            BytesValue bytesValue = TypedValue.fromXML(value);
            this.op = new SingleKVSetOpTemplate(key, bytesValue, expVersion);
            handle(op);
            return this;
        }

        @Override
        public DataAccountKVSetOperationBuilder setBytes(String key, byte[] value, long expVersion) {
            BytesValue bytesValue = TypedValue.fromBytes(value);
            this.op = new SingleKVSetOpTemplate(key, bytesValue, expVersion);
            handle(op);
            return this;
        }

        @Override
        public DataAccountKVSetOperationBuilder setImage(String key, byte[] value, long expVersion) {
            BytesValue bytesValue = TypedValue.fromImage(value);
            this.op = new SingleKVSetOpTemplate(key, bytesValue, expVersion);
            handle(op);
            return this;
        }

        @Override
        public DataAccountKVSetOperationBuilder setTimestamp(String key, long value, long expVersion) {
            BytesValue bytesValue = TypedValue.fromTimestamp(value);
            this.op = new SingleKVSetOpTemplate(key, bytesValue, expVersion);
            handle(op);
            return this;
        }

        private void handle(Operation op) {
            generatedOpList.add(op);
            opHandleContext.handle(op);
        }

        @Override
        public AccountPermissionSetOperationBuilder permission() {
            return new AccountPermissionSetOperationBuilder1(accountAddress, AccountType.DATA);
        }

        /**
         * 单个KV写入操作；
         *
         * @author huanghaiquan
         */
        private class SingleKVSetOpTemplate implements DataAccountKVSetOperation {

            private KVWriteEntry[] writeset = new KVWriteEntry[1];

            private SingleKVSetOpTemplate(String key, BytesValue value, long expVersion) {
                writeset[0] = new KVData(key, value, expVersion);
            }

            @Override
            public Bytes getAccountAddress() {
                return accountAddress;
            }

            @Override
            public KVWriteEntry[] getWriteSet() {
                return writeset;
            }

        }
    }

    private class EventOperationBuilder1 implements EventOperationBuilder {

        private Bytes eventAddress;

        private SingleEventPublishOpTemplate op;

        public EventOperationBuilder1(Bytes eventAddress) {
            this.eventAddress = eventAddress;
        }

        private void handle(Operation op) {
            generatedOpList.add(op);
            opHandleContext.handle(op);
        }

        @Override
        public EventPublishOperation getOperation() {
            return op;
        }

        @Override
        public EventPublishOperationBuilder publish(String name, byte[] content, long sequence) {
            BytesValue bytesValue = TypedValue.fromBytes(content);
            this.op = new SingleEventPublishOpTemplate(name, bytesValue, sequence);
            handle(op);
            return this;
        }

        @Override
        public EventPublishOperationBuilder publish(String name, Bytes content, long sequence) {
            BytesValue bytesValue = TypedValue.fromBytes(content);
            this.op = new SingleEventPublishOpTemplate(name, bytesValue, sequence);
            handle(op);
            return this;
        }

        @Override
        public EventPublishOperationBuilder publish(String name, String content, long sequence) {
            BytesValue bytesValue = TypedValue.fromText(content);
            this.op = new SingleEventPublishOpTemplate(name, bytesValue, sequence);
            handle(op);
            return this;
        }

        @Override
        public EventPublishOperationBuilder publish(String name, long content, long sequence) {
            BytesValue bytesValue = TypedValue.fromInt64(content);
            this.op = new SingleEventPublishOpTemplate(name, bytesValue, sequence);
            handle(op);
            return this;
        }

        @Override
        public EventPublishOperationBuilder publishTimestamp(String name, long content, long sequence) {
            BytesValue bytesValue = TypedValue.fromTimestamp(content);
            this.op = new SingleEventPublishOpTemplate(name, bytesValue, sequence);
            handle(op);
            return this;
        }

        @Override
        public EventPublishOperationBuilder publishImage(String name, byte[] content, long sequence) {
            BytesValue bytesValue = TypedValue.fromImage(content);
            this.op = new SingleEventPublishOpTemplate(name, bytesValue, sequence);
            handle(op);
            return this;
        }

        @Override
        public EventPublishOperationBuilder publishJSON(String name, String content, long sequence) {
            BytesValue bytesValue = TypedValue.fromJSON(content);
            this.op = new SingleEventPublishOpTemplate(name, bytesValue, sequence);
            handle(op);
            return this;
        }

        @Override
        public EventPublishOperationBuilder publishXML(String name, String content, long sequence) {
            BytesValue bytesValue = TypedValue.fromXML(content);
            this.op = new SingleEventPublishOpTemplate(name, bytesValue, sequence);
            handle(op);
            return this;
        }

        @Override
        public AccountPermissionSetOperationBuilder permission() {
            return new AccountPermissionSetOperationBuilder1(eventAddress, AccountType.EVENT);
        }

        /**
         * 单个事件发布操作
         */
        private class SingleEventPublishOpTemplate implements EventPublishOperation {

            private EventEntry[] writeset = new EventEntry[1];

            private SingleEventPublishOpTemplate(String key, BytesValue value, long expVersion) {
                writeset[0] = new EventData(key, value, expVersion);
            }

            @Override
            public Bytes getEventAddress() {
                return eventAddress;
            }

            @Override
            public EventEntry[] getEvents() {
                return writeset;
            }
        }
    }

    private class SecurityOperationBuilder1 implements SimpleSecurityOperationBuilder {

        @Override
        public SimpleRoleConfigurer role(String role) {
            return new RolesConfigureOpTemplate(role);
        }

        @Override
        public SimpleUserAuthorizer authorziation(Bytes user) {
            return new UserAuthorizeOpTemplate(user);
        }

        private class RolesConfigureOpTemplate implements SimpleRoleConfigurer, RolesConfigureOperation {

            private String role;
            private RolePrivilegeConfig config;

            public RolesConfigureOpTemplate(String role) {
                this.role = role;
            }

            @Override
            public RolePrivilegeEntry[] getRoles() {
                return new RolePrivilegeEntry[]{config};
            }

            @Override
            public void enable(LedgerPermission... enableLedgerPermissions) {
                configure(enableLedgerPermissions, null, null, null);
            }

            @Override
            public void enable(TransactionPermission... enableTransactionPermissions) {
                configure(null, enableTransactionPermissions, null, null);
            }

            @Override
            public void enable(LedgerPermission[] enableLedgerPermissions, TransactionPermission[] enableTransactionPermissions) {
                configure(enableLedgerPermissions, enableTransactionPermissions, null, null);
            }

            @Override
            public void disable(LedgerPermission... disableLedgerPermissions) {
                configure(null, null, disableLedgerPermissions, null);
            }

            @Override
            public void disable(TransactionPermission... disableTransactionPermissions) {
                configure(null, null, null, disableTransactionPermissions);
            }

            @Override
            public void disable(LedgerPermission[] disableLedgerPermissions, TransactionPermission[] disableTransactionPermissions) {
                configure(null, null, disableLedgerPermissions, disableTransactionPermissions);
            }

            @Override
            public void configure(LedgerPermission[] enableLedgerPermissions, TransactionPermission[] enableTransactionPermissions, LedgerPermission[] disableLedgerPermissions, TransactionPermission[] disableTransactionPermissions) {
                this.config = new RolePrivilegeConfig(role, enableLedgerPermissions, enableTransactionPermissions, disableLedgerPermissions, disableTransactionPermissions);
                generatedOpList.add(this);
                opHandleContext.handle(this);
            }

            private class RolePrivilegeConfig implements RolePrivilegeEntry {

                private String roleName;

                private Set<LedgerPermission> enableLedgerPermissions = new LinkedHashSet<>();
                private Set<LedgerPermission> disableLedgerPermissions = new LinkedHashSet<>();

                private Set<TransactionPermission> enableTxPermissions = new LinkedHashSet<>();
                private Set<TransactionPermission> disableTxPermissions = new LinkedHashSet<>();

                public RolePrivilegeConfig(String role, LedgerPermission[] enableLedgerPermissions, TransactionPermission[] enableTransactionPermissions, LedgerPermission[] disableLedgerPermissions, TransactionPermission[] disableTransactionPermissions) {
                    this.roleName = role;
                    if (null != enableLedgerPermissions) {
                        Arrays.stream(enableLedgerPermissions).forEach(permission -> this.enableLedgerPermissions.add(permission));
                    }
                    if (null != enableTransactionPermissions) {
                        Arrays.stream(enableTransactionPermissions).forEach(permission -> this.enableTxPermissions.add(permission));
                    }
                    if (null != disableLedgerPermissions) {
                        Arrays.stream(disableLedgerPermissions).forEach(permission -> this.disableLedgerPermissions.add(permission));
                    }
                    if (null != disableTransactionPermissions) {
                        Arrays.stream(disableTransactionPermissions).forEach(permission -> this.disableTxPermissions.add(permission));
                    }
                }

                @Override
                public String getRoleName() {
                    return roleName;
                }

                @Override
                public LedgerPermission[] getEnableLedgerPermissions() {
                    return ArrayUtils.toArray(enableLedgerPermissions, LedgerPermission.class);
                }

                @Override
                public LedgerPermission[] getDisableLedgerPermissions() {
                    return ArrayUtils.toArray(disableLedgerPermissions, LedgerPermission.class);
                }

                @Override
                public TransactionPermission[] getEnableTransactionPermissions() {
                    return ArrayUtils.toArray(enableTxPermissions, TransactionPermission.class);
                }

                @Override
                public TransactionPermission[] getDisableTransactionPermissions() {
                    return ArrayUtils.toArray(disableTxPermissions, TransactionPermission.class);
                }

            }
        }

        private class UserAuthorizeOpTemplate implements SimpleUserAuthorizer, UserAuthorizeOperation {

            private Bytes user;
            private AuthorizationDataEntry authorizationDataEntry;

            public UserAuthorizeOpTemplate(Bytes user) {
                this.user = user;
            }

            @Override
            public void authorize(String... roles) {
                authorizationDataEntry = new AuthorizationDataEntry(user, roles, null, RolesPolicy.UNION);
                generatedOpList.add(this);
                opHandleContext.handle(this);
            }

            @Override
            public void setPolicy(RolesPolicy rolePolicy) {
                authorizationDataEntry = new AuthorizationDataEntry(user, null, null, rolePolicy);
                generatedOpList.add(this);
                opHandleContext.handle(this);
            }

            @Override
            public void unauthorize(String... roles) {
                authorizationDataEntry = new AuthorizationDataEntry(user, null, roles, RolesPolicy.UNION);
                generatedOpList.add(this);
                opHandleContext.handle(this);
            }

            @Override
            public AuthorizationDataEntry[] getUserRolesAuthorizations() {
                return new AuthorizationDataEntry[]{authorizationDataEntry};
            }

            private class AuthorizationDataEntry implements UserRolesEntry {

                private Bytes[] userAddress;
                private RolesPolicy policy = RolesPolicy.UNION;

                private Set<String> authRoles = new LinkedHashSet<String>();
                private Set<String> unauthRoles = new LinkedHashSet<String>();

                public AuthorizationDataEntry(Bytes user, String[] authRoles, String[] unauthRoles, RolesPolicy policy) {
                    this.userAddress = new Bytes[]{user};
                    if (null != authRoles) {
                        Arrays.stream(authRoles).forEach(role -> this.authRoles.add(role));
                    }
                    if (null != unauthRoles) {
                        Arrays.stream(unauthRoles).forEach(role -> this.unauthRoles.add(role));
                    }
                    if (null != policy) {
                        this.policy = policy;
                    }
                }

                @Override
                public Bytes[] getUserAddresses() {
                    return userAddress;
                }

                @Override
                public RolesPolicy getPolicy() {
                    return policy;
                }

                @Override
                public String[] getAuthorizedRoles() {
                    return ArrayUtils.toArray(authRoles, String.class);
                }

                @Override
                public String[] getUnauthorizedRoles() {
                    return ArrayUtils.toArray(unauthRoles, String.class);
                }
            }
        }
    }

}

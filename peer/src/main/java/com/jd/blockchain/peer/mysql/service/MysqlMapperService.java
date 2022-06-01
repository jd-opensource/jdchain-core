package com.jd.blockchain.peer.mysql.service;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.AccountPermissionSetOperation;
import com.jd.blockchain.ledger.AccountType;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.ContractCodeDeployOperation;
import com.jd.blockchain.ledger.ContractCrossEventSendOperation;
import com.jd.blockchain.ledger.ContractEventSendOperation;
import com.jd.blockchain.ledger.ContractStateUpdateOperation;
import com.jd.blockchain.ledger.DataAccountKVSetOperation;
import com.jd.blockchain.ledger.DataAccountRegisterOperation;
import com.jd.blockchain.ledger.EventAccountRegisterOperation;
import com.jd.blockchain.ledger.EventPublishOperation;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.RolesConfigureOperation;
import com.jd.blockchain.ledger.RolesPolicy;
import com.jd.blockchain.ledger.TransactionContent;
import com.jd.blockchain.ledger.TransactionPermission;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResult;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.ledger.UserAuthorizeOperation;
import com.jd.blockchain.ledger.UserRegisterOperation;
import com.jd.blockchain.ledger.UserStateUpdateOperation;
import com.jd.blockchain.ledger.core.LedgerQuery;
import com.jd.blockchain.ledger.core.TransactionSet;
import com.jd.blockchain.peer.ledger.service.utils.TransactionDecorator;
import com.jd.blockchain.peer.mysql.entity.BlockInfo;
import com.jd.blockchain.peer.mysql.entity.ContractInfo;
import com.jd.blockchain.peer.mysql.entity.DataInfo;
import com.jd.blockchain.peer.mysql.entity.DataKv;
import com.jd.blockchain.peer.mysql.entity.EventInfo;
import com.jd.blockchain.peer.mysql.entity.EventKv;
import com.jd.blockchain.peer.mysql.entity.RolePrivilegeInfo;
import com.jd.blockchain.peer.mysql.entity.TxInfo;
import com.jd.blockchain.peer.mysql.entity.UserInfo;
import com.jd.blockchain.peer.mysql.mapper.BlockInfoMapper;
import com.jd.blockchain.peer.mysql.mapper.ContractInfoMapper;
import com.jd.blockchain.peer.mysql.mapper.DataInfoMapper;
import com.jd.blockchain.peer.mysql.mapper.DataKvMapper;
import com.jd.blockchain.peer.mysql.mapper.EventInfoMapper;
import com.jd.blockchain.peer.mysql.mapper.EventKvMapper;
import com.jd.blockchain.peer.mysql.mapper.RolePrivilegeMapper;
import com.jd.blockchain.peer.mysql.mapper.TxInfoMapper;
import com.jd.blockchain.peer.mysql.mapper.UserInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utils.Bytes;
import utils.io.BytesUtils;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/14 5:59 PM
 * Version 1.0
 */
@Service
public class MysqlMapperService implements MapperService {
    @Autowired
    BlockInfoMapper blockInfoMapper;
    @Autowired
    TxInfoMapper txInfoMapper;
    @Autowired
    UserInfoMapper userInfoMapper;
    @Autowired
    DataInfoMapper dataInfoMapper;
    @Autowired
    ContractInfoMapper contractInfoMapper;
    @Autowired
    EventInfoMapper eventInfoMapper;
    @Autowired
    DataKvMapper dataKvMapper;
    @Autowired
    EventKvMapper eventKvMapper;
    @Autowired
    RolePrivilegeMapper rolePrivilegeMapper;

    private static final Logger logger = LoggerFactory.getLogger(MysqlMapperService.class);


    @Override
    public long getBlockTotal(HashDigest ledgerHash) {
        return blockInfoMapper.getBlockInfoTotal(ledgerHash.toBase58()) - 1;
    }

    // write all kinds of ledger app datas to mysql
    @Transactional(rollbackFor = {Exception.class, Error.class})
    @Override
    public void writeAppToMysql(LedgerQuery ledgerQuery, long blockHeight) {
        try {
            LedgerBlock ledgerBlock = ledgerQuery.getBlock(blockHeight);
            String ledger = ledgerQuery.getHash().toBase58();

            LedgerTransaction[] ledgerTransactions = getTransactionsInBlock(ledgerQuery, ledgerBlock);

            for (int i = 0; i < ledgerTransactions.length; i++) {
                String tx_node_pubkeys = null;
                String tx_endpoint_pubkeys = null;
                TransactionRequest transactionRequest = ledgerTransactions[i].getRequest();
                TransactionResult transactionResult = ledgerTransactions[i].getResult();
                Operation[] txDerivedOps = transactionResult.getDerivedOperations();
                String txHashBase58 = transactionRequest.getTransactionHash().toBase58();
                byte[] tx_contents = BinaryProtocol.encode(transactionRequest.getTransactionContent(), TransactionContent.class);

                if (transactionRequest.getNodeSignatures() != null) {
                    for (int j = 0; j < transactionRequest.getNodeSignatures().length; j++) {
                        String nodePubKey = transactionRequest.getNodeSignatures()[j].getPubKey().toBase58();
                        if (tx_node_pubkeys == null) {
                            tx_node_pubkeys = nodePubKey + ",";
                        } else {
                            tx_node_pubkeys = tx_node_pubkeys + nodePubKey + ",";
                        }
                    }
                }

                if (transactionRequest.getEndpointSignatures() != null) {
                    for (int j = 0; j < transactionRequest.getEndpointSignatures().length; j++) {
                        String endpointPubKey = transactionRequest.getEndpointSignatures()[j].getPubKey().toBase58();
                        if (tx_endpoint_pubkeys == null) {
                            tx_endpoint_pubkeys = endpointPubKey + ",";
                        } else {
                            tx_endpoint_pubkeys = tx_endpoint_pubkeys + endpointPubKey + ",";
                        }
                    }
                }

                tx_endpoint_pubkeys = tx_endpoint_pubkeys == null? null : (tx_endpoint_pubkeys.substring(0, tx_endpoint_pubkeys.length() - 1));
                tx_node_pubkeys = tx_node_pubkeys == null? null : (tx_node_pubkeys.substring(0, tx_node_pubkeys.length() - 1));

                for (Operation op : transactionRequest.getTransactionContent().getOperations()) {
                    // contract event op and cross contract invoke op process
                    if (op instanceof ContractEventSendOperation || op instanceof ContractCrossEventSendOperation) {
                        for(Operation operation : txDerivedOps) {
                            // todo:txDerivedOps 处理不完善，不适应复杂场景, 暂不处理
                            nativeOpHandler(ledger, blockHeight, txHashBase58, tx_endpoint_pubkeys, operation);
                        }
                    } else {
                        // normal op process
                        nativeOpHandler(ledger, blockHeight, txHashBase58, tx_endpoint_pubkeys, op);
                    }
                }

                insertTxInfo(ledger, blockHeight, txHashBase58, i, tx_node_pubkeys, tx_endpoint_pubkeys, transactionResult.getExecutionState().CODE, tx_contents);
            }

            insertBlockInfo(ledger, ledgerBlock);
        } catch (Throwable e) {
            throw new RuntimeException("writeAppToMysql exception occur, error = !" + e.getMessage());
        }

    }

    private void nativeOpHandler(String ledger, long blockHeight, String txHashBase58, String endpointPubKey, Operation op) {
        if (op instanceof UserRegisterOperation) {
            insertUserInfo(ledger, blockHeight, txHashBase58, (UserRegisterOperation)op);
        } else if (op instanceof DataAccountRegisterOperation) {
            insertDataInfo(ledger, blockHeight, txHashBase58, (DataAccountRegisterOperation)op, endpointPubKey);
        } else if (op instanceof ContractCodeDeployOperation) {
            insertContractInfo(ledger, blockHeight, txHashBase58, (ContractCodeDeployOperation)op, endpointPubKey);
        } else if (op instanceof EventAccountRegisterOperation) {
            insertEventInfo(ledger, blockHeight, txHashBase58, (EventAccountRegisterOperation)op, endpointPubKey);
        } else if (op instanceof DataAccountKVSetOperation) {
            insertDataKvInfo(ledger, blockHeight, txHashBase58, (DataAccountKVSetOperation)op);
        } else if (op instanceof EventPublishOperation) {
            insertEventKvInfo(ledger, blockHeight, txHashBase58, (EventPublishOperation)op);
        } else if (op instanceof UserStateUpdateOperation) {
            updateUserState(ledger, (UserStateUpdateOperation)op);
        } else if (op instanceof ContractStateUpdateOperation) {
            updateContratState(ledger, (ContractStateUpdateOperation)op);
        } else if (op instanceof UserAuthorizeOperation) {
            updateUserAuthorize(ledger, (UserAuthorizeOperation)op);
        } else if (op instanceof AccountPermissionSetOperation) {
            updateDataPermission(ledger, (AccountPermissionSetOperation)op);
        } else if (op instanceof RolesConfigureOperation) {
            updateRolePrivilege(ledger, blockHeight, txHashBase58, (RolesConfigureOperation)op);
        }
    }

    private void insertUserInfo(String ledger, long blockHeight, String txHashBase58, UserRegisterOperation userRegisterOperation) {
        String user_address = userRegisterOperation.getUserID().getAddress().toBase58();
        String user_pubkey = userRegisterOperation.getUserID().getPubKey().toBase58();
        String user_key_algorithm =  Crypto.getAlgorithm(userRegisterOperation.getUserID().getPubKey().getAlgorithm()).name();
        String user_certificate = userRegisterOperation.getCertificate();
        UserInfo userInfo = new UserInfo(ledger, user_address, user_pubkey, user_key_algorithm, user_certificate, "NORMAL", "DEFAULT", "UNION", txHashBase58, blockHeight);
        userInfoMapper.insert(userInfo);
    }

    private void insertDataInfo(String ledger, long blockHeight, String txHashBase58, DataAccountRegisterOperation dataAccountRegisterOperation, String endpointPubKey) {
        String data_account_address = dataAccountRegisterOperation.getAccountID().getAddress().toBase58();
        String data_account_pubkey = dataAccountRegisterOperation.getAccountID().getPubKey().toBase58();
        DataInfo dataInfo = new DataInfo(ledger, data_account_address, data_account_pubkey, null, 777, endpointPubKey, txHashBase58, blockHeight);
        dataInfoMapper.insert(dataInfo);
    }

    private void insertContractInfo(String ledger, long blockHeight, String txHashBase58, ContractCodeDeployOperation contractCodeDeployOperation, String endpointPubKey) {
        String contract_address = contractCodeDeployOperation.getContractID().getAddress().toBase58();
        String contract_pubkey = contractCodeDeployOperation.getContractID().getPubKey().toBase58();
        String contract_lang = contractCodeDeployOperation.getLang().name();
        long contract_version = contractCodeDeployOperation.getChainCodeVersion();
        byte[] contract_content = contractCodeDeployOperation.getChainCode();

        ContractInfo contractInfo = new ContractInfo(ledger, contract_address, contract_pubkey, "NORMAL", null, 777, endpointPubKey, contract_lang, contract_version, BytesUtils.toString(contract_content), txHashBase58, blockHeight);
        contractInfoMapper.insert(contractInfo);
    }

    private void insertEventInfo(String ledger, long blockHeight, String txHashBase58, EventAccountRegisterOperation eventAccountRegisterOperation, String endpointPubKey) {
        String event_account_address = eventAccountRegisterOperation.getEventAccountID().getAddress().toBase58();
        String event_account_pubkey = eventAccountRegisterOperation.getEventAccountID().getPubKey().toBase58();
        EventInfo eventInfo = new EventInfo(ledger, event_account_address, event_account_pubkey, null, 777, endpointPubKey, txHashBase58, blockHeight);
        eventInfoMapper.insert(eventInfo);
    }

    private void insertDataKvInfo(String ledger, long blockHeight, String txHashBase58, DataAccountKVSetOperation dataAccountKVSetOperation) {
        String data_account_address = dataAccountKVSetOperation.getAccountAddress().toBase58();
        for (DataAccountKVSetOperation.KVWriteEntry kvw : dataAccountKVSetOperation.getWriteSet()) {
            String data_key = kvw.getKey();
            String data_type = kvw.getValue().getType().name();
            byte[] data_value = BinaryProtocol.encode(TypedValue.wrap(kvw.getValue()), BytesValue.class);
            long data_version = kvw.getExpectedVersion() + 1;
            DataKv dataKv = new DataKv(ledger, data_account_address, data_key, data_value, data_version, data_type, txHashBase58, blockHeight);
            dataKvMapper.insert(dataKv);
        }
    }

    private void insertEventKvInfo(String ledger, long blockHeight, String txHashBase58, EventPublishOperation eventPublishOperation) {
        String event_account_address = eventPublishOperation.getEventAddress().toBase58();
        for (EventPublishOperation.EventEntry eventEntry : eventPublishOperation.getEvents()) {
            String event_name = eventEntry.getName();
            String event_type = eventEntry.getContent().getType().name();
            String event_value = BinaryProtocol.encode(TypedValue.wrap(eventEntry.getContent()), BytesValue.class).toString();
            long event_sequence = eventEntry.getSequence() + 1;
            EventKv eventKv = new EventKv(ledger, event_account_address, event_name, event_sequence, event_type, event_value, txHashBase58, blockHeight);
            eventKvMapper.insert(eventKv);
        }
    }

    private void insertTxInfo(String ledger, long blockHeight, String txHashBase58, int txIndex, String ndoePubKey, String endpointPubKey, int exeState, byte[] txContent) {
        TxInfo txInfo = new TxInfo(ledger, blockHeight, txHashBase58, txIndex, ndoePubKey, endpointPubKey, exeState, BytesUtils.toString(txContent));
        txInfoMapper.insert(txInfo);
    }

    private void insertBlockInfo(String ledger, LedgerBlock ledgerBlock) {
        String previousHash = ledgerBlock.getPreviousHash() == null? null : ledgerBlock.getPreviousHash().toBase58();
        String dataAccountSetHash = ledgerBlock.getDataAccountSetHash() == null? null : ledgerBlock.getDataAccountSetHash().toBase58();
        String contractAccountSetHash = ledgerBlock.getContractAccountSetHash() == null? null : ledgerBlock.getContractAccountSetHash().toBase58();
        String userEventSetHash = ledgerBlock.getUserEventSetHash() == null? null : ledgerBlock.getUserEventSetHash().toBase58();

        BlockInfo blockInfo = new BlockInfo(ledger, ledgerBlock.getHeight(), ledgerBlock.getHash().toBase58(),
                previousHash,  ledgerBlock.getTransactionSetHash().toBase58(), ledgerBlock.getUserAccountSetHash().toBase58(),
                dataAccountSetHash, contractAccountSetHash, userEventSetHash, ledgerBlock.getAdminAccountHash().toBase58(), new Date(ledgerBlock.getTimestamp()));

        blockInfoMapper.insert(blockInfo);
    }

    private void updateUserState(String ledger, UserStateUpdateOperation userStateUpdateOperation) {
        userInfoMapper.updateStatus(ledger, userStateUpdateOperation.getUserAddress().toBase58(), userStateUpdateOperation.getState().name());
    }

    private void updateContratState(String ledger, ContractStateUpdateOperation contractStateUpdateOperation) {
        contractInfoMapper.updateStatus(ledger, contractStateUpdateOperation.getContractAddress().toBase58(), contractStateUpdateOperation.getState().name());
    }

    // update data/event/contract permission info
    private void updateDataPermission(String ledger, AccountPermissionSetOperation op) {

        AccountType accountType = op.getAccountType();
        String accountAddress = op.getAddress().toBase58();

        if (rolePrivilegeMapper.getRolePrivInfo(ledger, op.getRole()) == null) {
            logger.info("updateDataPermission, role info is not exist in rolePrivilege table!");
            return;
        }

        if (accountType.name().equals("DATA")) {
            dataInfoMapper.updateDataPermission(ledger, accountAddress, op.getRole(), op.getMode());
        } else if (accountType.name().equals("EVENT")) {
            eventInfoMapper.updateDataPermission(ledger, accountAddress, op.getRole(), op.getMode());
        } else if (accountType.name().equals("CONTRACT")) {
            contractInfoMapper.updateDataPermission(ledger, accountAddress, op.getRole(), op.getMode());
        }
    }

    // update user's role group and role group policy
    private void updateUserAuthorize(String ledger, UserAuthorizeOperation userAuthorizeOperation) {
        for (UserAuthorizeOperation.UserRolesEntry userRolesEntry: userAuthorizeOperation.getUserRolesAuthorizations()) {
            for (Bytes userAddr : userRolesEntry.getUserAddresses()) {
                String userBase58 = userAddr.toBase58();
                String[] authorizedRoles = userRolesEntry.getAuthorizedRoles();
                String[] unauthorizedRoles = userRolesEntry.getUnauthorizedRoles();
                String rolesPolicy = (userRolesEntry.getPolicy() == null ? RolesPolicy.UNION.name() : userRolesEntry.getPolicy().name());
                UserInfo userInfo = userInfoMapper.getUserInfoByAddr(ledger, userBase58);
                if (userInfo == null) {
                    logger.error("[MysqlMapperService] updateRolePolicy error, user does not exist!");
                    return;
                }

                String[] existRoles = null;
                Set<String>  updateRolesSet = new HashSet<String>();
                String updateRolesString = null;
                if (userInfo.getUser_roles().contains(",")) {
                    existRoles = userInfo.getUser_roles().split(",");
                    for (String existRole : existRoles) {
                        updateRolesSet.add(existRole);
                    }
                } else {
                    updateRolesSet.add(userInfo.getUser_roles());
                }

                for (String authorizedRole : authorizedRoles) {
                    updateRolesSet.add(authorizedRole);
                }

                for (String unauthorizedRole : unauthorizedRoles) {
                    updateRolesSet.remove(unauthorizedRole);
                }

                for (String updateRole : updateRolesSet) {
                    if (updateRolesString == null) {
                        updateRolesString = updateRole + ",";
                    } else {
                        updateRolesString = updateRolesString + updateRole + ",";
                    }
                }

                updateRolesString = updateRolesString == null? null : (updateRolesString.substring(0, updateRolesString.length() - 1));

                userInfoMapper.updateRolePolicy(ledger, userBase58, updateRolesString, rolesPolicy);
            }
        }
    }

    private void updateRolePrivilege(String ledger, long blockHeight, String txHashBase58, RolesConfigureOperation rolesConfigureOperation) {
        for (RolesConfigureOperation.RolePrivilegeEntry rolePrivilegeEntry : rolesConfigureOperation.getRoles()) {

            String role = rolePrivilegeEntry.getRoleName();
            LedgerPermission[] enableLedgerPermissions = rolePrivilegeEntry.getEnableLedgerPermissions();
            LedgerPermission[] unEnableLedgerPermissions = rolePrivilegeEntry.getDisableLedgerPermissions();
            TransactionPermission[] enableTransactionPermissions = rolePrivilegeEntry.getEnableTransactionPermissions();
            TransactionPermission[] unEnableTransactionPermissions = rolePrivilegeEntry.getDisableTransactionPermissions();

            RolePrivilegeInfo rolePrivilegeInfo = rolePrivilegeMapper.getRolePrivInfo(ledger, role);

            if (rolePrivilegeInfo == null) {
                // 新添加的角色
                String insert_ledger_permissions = null;
                String insert_tx_permissions = null;

                for (LedgerPermission ledgerPermission : enableLedgerPermissions) {
                    if (insert_ledger_permissions == null) {
                        insert_ledger_permissions = ledgerPermission.name() + ",";
                    } else {
                        insert_ledger_permissions = insert_ledger_permissions + ledgerPermission.name() + ",";
                    }
                }

                for (TransactionPermission transactionPermission : enableTransactionPermissions) {
                    if (insert_tx_permissions == null) {
                        insert_tx_permissions = transactionPermission.name() + ",";
                    } else {
                        insert_tx_permissions = insert_tx_permissions + transactionPermission.name() + ",";
                    }
                }

                insert_ledger_permissions = insert_ledger_permissions == null? null : (insert_ledger_permissions.substring(0, insert_ledger_permissions.length() - 1));
                insert_tx_permissions = insert_tx_permissions == null? null : (insert_tx_permissions.substring(0, insert_tx_permissions.length() - 1));

                rolePrivilegeMapper.insert(new RolePrivilegeInfo(ledger, role, insert_ledger_permissions, insert_tx_permissions, blockHeight, txHashBase58));
            } else {
                // 已存在的角色
                String[] existLedgerPermissions = rolePrivilegeInfo.getLedger_privileges().split(",");
                String[] existTransactionPermissions = rolePrivilegeInfo.getTx_privileges().split(",");

                Set<String> updateLedgerPermissions = new HashSet<String>();
                Set<String> updateTransactionPermissions = new HashSet<String>();

                String update_ledger_privileges = null;
                String update_tx_privileges = null;

                for (String ledgerPermission : existLedgerPermissions) {
                    updateLedgerPermissions.add(ledgerPermission);
                }

                for (LedgerPermission ledgerPermission : enableLedgerPermissions) {
                    updateLedgerPermissions.add(ledgerPermission.name());
                }

                for (LedgerPermission ledgerPermission : unEnableLedgerPermissions) {
                    updateLedgerPermissions.remove(ledgerPermission.name());
                }

                for(String transactionPermission: existTransactionPermissions) {
                    updateTransactionPermissions.add(transactionPermission);
                }

                for(TransactionPermission transactionPermission: enableTransactionPermissions) {
                    updateTransactionPermissions.add(transactionPermission.name());
                }

                for(TransactionPermission transactionPermission: unEnableTransactionPermissions) {
                    updateTransactionPermissions.remove(transactionPermission.name());
                }

                for (String ledgerPermission : updateLedgerPermissions) {
                    if (update_ledger_privileges == null) {
                        update_ledger_privileges = ledgerPermission + ",";
                    } else {
                        update_ledger_privileges = update_ledger_privileges + ledgerPermission + ",";
                    }
                }

                for (String transactionPermission : updateTransactionPermissions) {
                    if (update_tx_privileges == null) {
                        update_tx_privileges = transactionPermission + ",";
                    } else {
                        update_tx_privileges = update_tx_privileges + transactionPermission + ",";
                    }
                }

                update_ledger_privileges = update_ledger_privileges == null? null : (update_ledger_privileges.substring(0, update_ledger_privileges.length() - 1));
                update_tx_privileges = update_tx_privileges == null? null : (update_tx_privileges.substring(0, update_tx_privileges.length() - 1));

                rolePrivilegeMapper.updateRolePrivInfo(ledger, role, update_ledger_privileges, update_tx_privileges);
            }
        }
    }

    private LedgerTransaction[] getTransactionsInBlock(LedgerQuery ledgerQuery, LedgerBlock ledgerBlock) {
        TransactionSet currTransactionSet = ledgerQuery.getTransactionSet(ledgerBlock);
        TransactionSet lastTransactionSet = null;
        long currBlockHeight = ledgerBlock.getHeight();

        int lastBlockTxTotalNums = 0;

        if (currBlockHeight > 0) {
            lastTransactionSet = ledgerQuery.getTransactionSet(ledgerQuery.getBlock(currBlockHeight - 1));
            lastBlockTxTotalNums = (int) lastTransactionSet.getTotalCount();
        }

        int currBlockTxTotalNums = (int) ledgerQuery.getTransactionSet(ledgerBlock).getTotalCount();

        // 获取当前高度的增量交易数
        int currBlockTxNums = currBlockTxTotalNums - lastBlockTxTotalNums;

        LedgerTransaction[] currTxs = new LedgerTransaction[currBlockTxNums];

        for (int i = 0; i < currBlockTxNums; i++) {

            LedgerTransaction[] ledgerTransactions = currTransactionSet.getTransactions(lastBlockTxTotalNums + i , 1);
            currTxs[i] = ledgerTransactions[0];
        }

        return txsDecorator(currTxs);
    }

    private LedgerTransaction[] txsDecorator(LedgerTransaction[] ledgerTransactions) {
        LedgerTransaction[] transactionDecorators = new LedgerTransaction[ledgerTransactions.length];
        for (int i = 0; i < ledgerTransactions.length; i++) {
            transactionDecorators[i] = new TransactionDecorator(ledgerTransactions[i]);
        }
        return transactionDecorators;
    }
}

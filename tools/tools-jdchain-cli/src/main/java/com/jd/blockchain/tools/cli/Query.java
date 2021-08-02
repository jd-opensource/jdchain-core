package com.jd.blockchain.tools.cli;

import com.jd.blockchain.contract.OnLineContractProcessor;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.DataAccountInfo;
import com.jd.blockchain.ledger.Event;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.LedgerInfo;
import com.jd.blockchain.ledger.LedgerTransaction;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.ledger.PrivilegeSet;
import com.jd.blockchain.ledger.TypedKVEntry;
import com.jd.blockchain.ledger.UserInfo;
import com.jd.blockchain.ledger.UserPrivilegeSet;
import com.jd.blockchain.sdk.BlockchainService;
import com.jd.blockchain.sdk.client.GatewayServiceFactory;
import picocli.CommandLine;
import utils.StringUtils;
import utils.codec.Base58Utils;
import utils.serialize.json.JSONSerializeUtils;

import java.util.Scanner;

/**
 * @description: query commands
 * @author: imuge
 * @date: 2021/7/26
 **/
@CommandLine.Command(name = "query",
        mixinStandardHelpOptions = true,
        showDefaultValues = true,
        description = "Query commands.",
        subcommands = {
                Ledgers.class,
                Ledger.class,
                Participants.class,
                Block.class,
                TransactionsCount.class,
                Transactions.class,
                Transaction.class,
                Users.class,
                UsersCount.class,
                User.class,
                RolePrivileges.class,
                UserPrivileges.class,
                DataAccountsCount.class,
                DataAccounts.class,
                DataAccount.class,
                KVsCount.class,
                KVs.class,
                KV.class,
                UserEventAccountsCount.class,
                UserEventAccounts.class,
                UserEventAccount.class,
                UserEventNamesCount.class,
                UserEventNames.class,
                UserEventsCount.class,
                UserEvents.class,
                UserEventLatest.class,
                ContractsCount.class,
                Contracts.class,
                Contract.class,
                CommandLine.HelpCommand.class
        }
)
public class Query implements Runnable {

    @CommandLine.Option(names = "--gw-host", defaultValue = "127.0.0.1", description = "Set the gateway host. Default: 127.0.0.1", scope = CommandLine.ScopeType.INHERIT)
    String gwHost;

    @CommandLine.Option(names = "--gw-port", defaultValue = "8080", description = "Set the gateway port. Default: 8080", scope = CommandLine.ScopeType.INHERIT)
    int gwPort;

    @CommandLine.ParentCommand
    JDChainCli jdChainCli;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    BlockchainService blockchainService;

    BlockchainService getChainService() {
        if (null == blockchainService) {
            blockchainService = GatewayServiceFactory.connect(gwHost, gwPort, false).getBlockchainService();
        }
        return blockchainService;
    }

    HashDigest[] getLedgers() {
        return getChainService().getLedgerHashs();
    }

    HashDigest selectLedger() {
        HashDigest[] ledgers = getChainService().getLedgerHashs();
        System.out.printf("select ledger, input the index: %n%-7s\t%s%n", "INDEX", "LEDGER");
        for (int i = 0; i < ledgers.length; i++) {
            System.out.printf("%-7s\t%s%n", i, ledgers[i]);
        }
        System.out.print("> ");
        Scanner scanner = new Scanner(System.in).useDelimiter("\n");
        String input = scanner.next().trim();
        return ledgers[Integer.parseInt(input)];
    }

    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }
}

@CommandLine.Command(name = "ledgers", mixinStandardHelpOptions = true, header = "Query ledgers.")
class Ledgers implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        HashDigest[] ledgers = query.getLedgers();
        for (int i = 0; i < ledgers.length; i++) {
            System.out.println(ledgers[i]);
        }
    }
}

@CommandLine.Command(name = "ledger", mixinStandardHelpOptions = true, header = "Query ledger.")
class Ledger implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        LedgerInfo ledger = query.getChainService().getLedger(query.selectLedger());
        if (null != ledger) {
            System.out.println(JSONSerializeUtils.serializeToJSON(ledger, query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "participants", mixinStandardHelpOptions = true, header = "Query participants.")
class Participants implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        ParticipantNode[] participants = query.getChainService().getConsensusParticipants(query.selectLedger());
        System.out.println(JSONSerializeUtils.serializeToJSON(participants, query.jdChainCli.pretty));
    }
}

@CommandLine.Command(name = "block", mixinStandardHelpOptions = true, header = "Query block.")
class Block implements Runnable {

    @CommandLine.Option(names = "--height", defaultValue = "-1", description = "Block height.", scope = CommandLine.ScopeType.INHERIT)
    int height;

    @CommandLine.Option(names = "--hash", description = "Block hash", scope = CommandLine.ScopeType.INHERIT)
    String hash;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        LedgerBlock block;
        if (!StringUtils.isEmpty(hash)) {
            block = query.getChainService().getBlock(query.selectLedger(), Crypto.resolveAsHashDigest(Base58Utils.decode(hash)));
        } else {
            block = query.getChainService().getBlock(query.selectLedger(), height);
        }
        if (null != block) {
            System.out.println(JSONSerializeUtils.serializeToJSON(block, query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "txs-count", mixinStandardHelpOptions = true, header = "Query transactions count.")
class TransactionsCount implements Runnable {

    @CommandLine.Option(names = "--height", required = true, description = "Block height.", scope = CommandLine.ScopeType.INHERIT)
    int height;

    @CommandLine.Option(names = "--in-block", description = "In the given block.", scope = CommandLine.ScopeType.INHERIT)
    boolean inBlock;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        long count;
        if (!inBlock) {
            count = query.getChainService().getTransactionCount(query.selectLedger(), height);
        } else {
            if (height < 0) {
                System.err.println("Height can not be negative");
                return;
            }
            HashDigest ledger = query.selectLedger();
            if (height > 0) {
                count = query.getChainService().getTransactionCount(ledger, height) -
                        query.getChainService().getTransactionCount(ledger, height - 1);
            } else {
                count = query.getChainService().getTransactionCount(ledger, height);
            }
        }
        System.out.println(count);
    }
}

@CommandLine.Command(name = "txs", mixinStandardHelpOptions = true, header = "Query transactions.")
class Transactions implements Runnable {

    @CommandLine.Option(names = "--height", required = true, defaultValue = "-1", description = "Block height.", scope = CommandLine.ScopeType.INHERIT)
    int height;

    @CommandLine.Option(names = "--in-block", description = "In the given block.", scope = CommandLine.ScopeType.INHERIT)
    boolean inBlock;

    @CommandLine.Option(names = "--index", required = true, description = "Transaction item index", scope = CommandLine.ScopeType.INHERIT)
    int index;

    @CommandLine.Option(names = "--count", required = true, description = "Transaction item count", scope = CommandLine.ScopeType.INHERIT)
    int count;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        LedgerTransaction[] txs;
        if (!inBlock) {
            txs = query.getChainService().getTransactions(query.selectLedger(), height, index, count);
        } else {
            txs = query.getChainService().getAdditionalTransactions(query.selectLedger(), height, index, count);
        }
        System.out.println(JSONSerializeUtils.serializeToJSON(txs, query.jdChainCli.pretty));
    }
}

@CommandLine.Command(name = "tx", mixinStandardHelpOptions = true, header = "Query transaction.")
class Transaction implements Runnable {

    @CommandLine.Option(names = "--hash", description = "Transaction hash", scope = CommandLine.ScopeType.INHERIT)
    String hash;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        LedgerTransaction tx = query.getChainService().getTransactionByContentHash(query.selectLedger(), Crypto.resolveAsHashDigest(Base58Utils.decode(hash)));
        if (null != tx) {
            System.out.println(JSONSerializeUtils.serializeToJSON(tx, query.jdChainCli.pretty));
        } else {
            System.err.printf("Transaction [%s] not exists", hash);
        }
    }
}

@CommandLine.Command(name = "users", mixinStandardHelpOptions = true, header = "Query users.")
class Users implements Runnable {

    @CommandLine.Option(names = "--index", required = true, description = "User item index", scope = CommandLine.ScopeType.INHERIT)
    int index;

    @CommandLine.Option(names = "--count", required = true, description = "User item count", scope = CommandLine.ScopeType.INHERIT)
    int count;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        BlockchainIdentity[] users = query.getChainService().getUsers(query.selectLedger(), index, count);
        System.out.printf("%s\t%s%n", "ADDRESS", "PUBKEY");
        for (BlockchainIdentity user : users) {
            System.out.printf("%s\t%s%n", user.getAddress(), user.getPubKey());
        }
    }
}

@CommandLine.Command(name = "users-count", mixinStandardHelpOptions = true, header = "Query users count.")
class UsersCount implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        long count = query.getChainService().getUserTotalCount(query.selectLedger());
        System.out.println(count);
    }
}

@CommandLine.Command(name = "user", mixinStandardHelpOptions = true, header = "Query user.")
class User implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "User address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        UserInfo user = query.getChainService().getUser(query.selectLedger(), address);
        if (null != user) {
            System.out.println(JSONSerializeUtils.serializeToJSON(user, query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "role-privileges", mixinStandardHelpOptions = true, header = "Query role privileges.")
class RolePrivileges implements Runnable {

    @CommandLine.Option(names = "--role", required = true, description = "Role name", scope = CommandLine.ScopeType.INHERIT)
    String role;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        PrivilegeSet privileges = query.getChainService().getRolePrivileges(query.selectLedger(), role);
        if (null != privileges) {
            System.out.println(JSONSerializeUtils.serializeToJSON(privileges, query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "user-privileges", mixinStandardHelpOptions = true, header = "Query user privileges.")
class UserPrivileges implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "User address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        UserPrivilegeSet privileges = query.getChainService().getUserPrivileges(query.selectLedger(), address);
        if (null != privileges) {
            System.out.println(JSONSerializeUtils.serializeToJSON(privileges, query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "data-accounts", mixinStandardHelpOptions = true, header = "Query data accounts.")
class DataAccounts implements Runnable {

    @CommandLine.Option(names = "--index", required = true, description = "Data account item index", scope = CommandLine.ScopeType.INHERIT)
    int index;

    @CommandLine.Option(names = "--count", required = true, description = "Data account item count", scope = CommandLine.ScopeType.INHERIT)
    int count;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        BlockchainIdentity[] users = query.getChainService().getDataAccounts(query.selectLedger(), index, count);
        System.out.printf("%s\t%s%n", "ADDRESS", "PUBKEY");
        for (BlockchainIdentity user : users) {
            System.out.printf("%s\t%s%n", user.getAddress(), user.getPubKey());
        }
    }
}

@CommandLine.Command(name = "data-accounts-count", mixinStandardHelpOptions = true, header = "Query data accounts count.")
class DataAccountsCount implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        long count = query.getChainService().getDataAccountTotalCount(query.selectLedger());
        System.out.println(count);
    }
}

@CommandLine.Command(name = "data-account", mixinStandardHelpOptions = true, header = "Query data account.")
class DataAccount implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Data account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        DataAccountInfo account = query.getChainService().getDataAccount(query.selectLedger(), address);
        if (null != account) {
            System.out.println(JSONSerializeUtils.serializeToJSON(account, query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "kvs-count", mixinStandardHelpOptions = true, header = "Query key-values count.")
class KVsCount implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Data account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        long count = query.getChainService().getDataEntriesTotalCount(query.selectLedger(), address);
        System.out.println(count);
    }
}

@CommandLine.Command(name = "kvs", mixinStandardHelpOptions = true, header = "Query kvs.")
class KVs implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Data account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--index", required = true, description = "KV item index", scope = CommandLine.ScopeType.INHERIT)
    int index;

    @CommandLine.Option(names = "--count", required = true, description = "KV item count", scope = CommandLine.ScopeType.INHERIT)
    int count;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        TypedKVEntry[] kvs = query.getChainService().getDataEntries(query.selectLedger(), address, index, count);
        for (TypedKVEntry kv : kvs) {
            System.out.println(JSONSerializeUtils.serializeToJSON(kv, query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "kv", mixinStandardHelpOptions = true, header = "Query kv.")
class KV implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Data account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--key", required = true, description = "Key", scope = CommandLine.ScopeType.INHERIT)
    String key;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        TypedKVEntry[] kvs = query.getChainService().getDataEntries(query.selectLedger(), address, key);
        if (null != kvs && kvs.length > 0 && kvs[0].getVersion() > -1) {
            System.out.println(JSONSerializeUtils.serializeToJSON(kvs[0], query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "user-event-accounts", mixinStandardHelpOptions = true, header = "Query user event accounts.")
class UserEventAccounts implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @CommandLine.Option(names = "--index", required = true, description = "Event account item index", scope = CommandLine.ScopeType.INHERIT)
    int index;

    @CommandLine.Option(names = "--count", required = true, description = "Event account item count", scope = CommandLine.ScopeType.INHERIT)
    int count;

    @Override
    public void run() {
        BlockchainIdentity[] accouts = query.getChainService().getUserEventAccounts(query.selectLedger(), index, count);
        System.out.printf("%s\t%s%n", "ADDRESS", "PUBKEY");
        for (BlockchainIdentity account : accouts) {
            System.out.printf("%s\t%s%n", account.getAddress(), account.getPubKey());
        }
    }
}

@CommandLine.Command(name = "user-event-accounts-count", mixinStandardHelpOptions = true, header = "Query user event accounts count.")
class UserEventAccountsCount implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        long count = query.getChainService().getUserEventAccountTotalCount(query.selectLedger());
        System.out.println(count);
    }
}

@CommandLine.Command(name = "user-event-account", mixinStandardHelpOptions = true, header = "Query user event account.")
class UserEventAccount implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @CommandLine.Option(names = "--address", required = true, description = "Event account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @Override
    public void run() {
        BlockchainIdentity accout = query.getChainService().getUserEventAccount(query.selectLedger(), address);
        if (null != accout) {
            System.out.println(JSONSerializeUtils.serializeToJSON(accout, query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "user-event-names-count", mixinStandardHelpOptions = true, header = "Query user event names count.")
class UserEventNamesCount implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Event account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        long count = query.getChainService().getUserEventNameTotalCount(query.selectLedger(), address);
        System.out.println(count);
    }
}

@CommandLine.Command(name = "user-event-names", mixinStandardHelpOptions = true, header = "Query user event names.")
class UserEventNames implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @CommandLine.Option(names = "--address", required = true, description = "Event account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--index", required = true, description = "Event name item index", scope = CommandLine.ScopeType.INHERIT)
    int index;

    @CommandLine.Option(names = "--count", required = true, description = "Event name item count", scope = CommandLine.ScopeType.INHERIT)
    int count;

    @Override
    public void run() {
        String[] names = query.getChainService().getUserEventNames(query.selectLedger(), address, index, count);
        for (String name : names) {
            System.out.println(name);
        }
    }
}

@CommandLine.Command(name = "user-events-count", mixinStandardHelpOptions = true, header = "Query user events count.")
class UserEventsCount implements Runnable {

    @CommandLine.Option(names = "--address", required = true, description = "Event account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--name", required = true, description = "Event name", scope = CommandLine.ScopeType.INHERIT)
    String name;

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        long count = query.getChainService().getUserEventsTotalCount(query.selectLedger(), address, name);
        System.out.println(count);
    }
}

@CommandLine.Command(name = "user-events", mixinStandardHelpOptions = true, header = "Query user events.")
class UserEvents implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @CommandLine.Option(names = "--address", required = true, description = "Event account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--name", required = true, description = "Event name", scope = CommandLine.ScopeType.INHERIT)
    String name;

    @CommandLine.Option(names = "--index", required = true, description = "Event name item index", scope = CommandLine.ScopeType.INHERIT)
    int index;

    @CommandLine.Option(names = "--count", required = true, description = "Event name item count", scope = CommandLine.ScopeType.INHERIT)
    int count;

    @Override
    public void run() {
        Event[] events = query.getChainService().getUserEvents(query.selectLedger(), address, name, index, count);
        for (Event event : events) {
            System.out.println(JSONSerializeUtils.serializeToJSON(event, query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "latest-user-event", mixinStandardHelpOptions = true, header = "Query latest user event.")
class UserEventLatest implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @CommandLine.Option(names = "--address", required = true, description = "Event account address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @CommandLine.Option(names = "--name", required = true, description = "Event name", scope = CommandLine.ScopeType.INHERIT)
    String name;

    @Override
    public void run() {
        Event event = query.getChainService().getLatestEvent(query.selectLedger(), address, name);
        if (null != event) {
            System.out.println(JSONSerializeUtils.serializeToJSON(event, query.jdChainCli.pretty));
        }
    }
}

@CommandLine.Command(name = "contracts", mixinStandardHelpOptions = true, header = "Query contracts.")
class Contracts implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @CommandLine.Option(names = "--index", required = true, description = "Contract item index", scope = CommandLine.ScopeType.INHERIT)
    int index;

    @CommandLine.Option(names = "--count", required = true, description = "Contract item count", scope = CommandLine.ScopeType.INHERIT)
    int count;

    @Override
    public void run() {
        BlockchainIdentity[] accounts = query.getChainService().getContractAccounts(query.selectLedger(), index, count);
        System.out.printf("%s\t%s%n", "ADDRESS", "PUBKEY");
        for (BlockchainIdentity account : accounts) {
            System.out.printf("%s\t%s%n", account.getAddress(), account.getPubKey());
        }
    }
}

@CommandLine.Command(name = "contracts-count", mixinStandardHelpOptions = true, header = "Query contracts count.")
class ContractsCount implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @Override
    public void run() {
        long count = query.getChainService().getContractTotalCount(query.selectLedger());
        System.out.println(count);
    }
}

@CommandLine.Command(name = "contract", mixinStandardHelpOptions = true, header = "Query contract.")
class Contract implements Runnable {

    @CommandLine.ParentCommand
    Query query;

    @CommandLine.Option(names = "--address", required = true, description = "Contract address", scope = CommandLine.ScopeType.INHERIT)
    String address;

    @Override
    public void run() {
        ContractInfo contract = query.getChainService().getContract(query.selectLedger(), address);
        if (null != contract) {
            System.out.println(OnLineContractProcessor.getInstance().decompileEntranceClass(contract.getChainCode()));
        }
    }
}
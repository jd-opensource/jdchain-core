package com.jd.blockchain.tools.cli;

import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.ledger.ConsensusTypeEnum;
import com.jd.blockchain.ledger.LedgerDataStructure;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import picocli.CommandLine;
import utils.io.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * @description: 测试网络配置生成工具
 * @author: imuge
 * @date: 2021/11/19
 **/
@CommandLine.Command(name = "testnet",
        mixinStandardHelpOptions = true,
        showDefaultValues = true,
        description = "Tools for testnet",
        subcommands = {
                InitConfig.class,
                CommandLine.HelpCommand.class
        }
)
public class TestNet implements Runnable {

    @CommandLine.ParentCommand
    JDChainCli jdChainCli;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }
}

@CommandLine.Command(name = "config", mixinStandardHelpOptions = true, header = "Generate testnet init configs.")
class InitConfig implements Runnable {

    @CommandLine.Option(names = {"-c", "--consensus"}, description = "Consensus, options: BFTSMART, RAFT, MQ", defaultValue = "BFTSMART")
    ConsensusTypeEnum consensus;

    @CommandLine.Option(names = {"-a", "--algorithm"}, description = "Crypto algorithm", defaultValue = "ECDSA")
    String algorithm;

    @CommandLine.Option(names = "--rabbit", description = "RabbitMQ Server address for MQ consensus")
    String rabbit;

    @CommandLine.Option(names = "--data-structure", description = "Ledger Data Struct: MERKLE_TREE,KV.", defaultValue = "MERKLE_TREE")
    LedgerDataStructure dataStructure;

    @CommandLine.Option(names = "--peer-size", description = "Size of peers", defaultValue = "4")
    int peerSize;

    @CommandLine.Option(names = "--init-hosts", description = "Hosts for initialization, input one (all the peers use the same host) or peer-size(comma division)",
            defaultValue = "127.0.0.1", split = ",")
    String[] initHosts;

    @CommandLine.Option(names = "--init-ports", description = "Ports for initialization, input one (all the peers use the same manage port) or peer-size(comma division)",
            defaultValue = "8800", split = ",")
    int[] initPorts;

    @CommandLine.Option(names = "--peer-hosts", description = "Hosts for nodes, input one (all the peers use the same host) or peer-size(comma division)",
            defaultValue = "127.0.0.1", split = ",")
    String[] peerHosts;

    @CommandLine.Option(names = "--peer-manage-ports", description = "Ports for node manage server, input one (all the peers use the same manage port) or peer-size(comma division)",
            defaultValue = "7080", split = ",")
    int[] peerManagePorts;

    @CommandLine.Option(names = "--peer-consensus-ports", description = "Ports for node consensus server, input one (all the peers use the same consensus port) or peer-size(comma division)",
            defaultValue = "10080", split = ",")
    int[] peerConsensusPorts;

    @CommandLine.Option(names = "--gw-port", description = "Port for gateway server", defaultValue = "8080")
    int gwPort;

    @CommandLine.Option(names = "--peer-zip", description = "Zip file of jdchain-peer", required = true)
    String peerZip;

    @CommandLine.Option(names = "--gw-zip", description = "Zip file of jdchain-gateway", required = true)
    String gwZip;

    @CommandLine.Option(names = "--output", description = "Output directory for initialized files", required = true)
    String output;

    @CommandLine.Option(names = "--ledger-name", description = "Ledger name", required = true)
    String ledgerName;

    @CommandLine.Option(names = "--password", description = "Raw password for keypair", required = true)
    String password;

    @CommandLine.ParentCommand
    private TestNet testnet;

    @Override
    public void run() {
        try {
            if (consensus.getMinimalNodeSize() > peerSize) {
                System.err.printf("consensus %s minimal peer size is %d\n", consensus.name(), consensus.getMinimalNodeSize());
                return;
            }
            File out = new File(output);
            if (!out.exists()) {
                out.mkdirs();
            }
            String[] hostsForInit = null;
            if (initHosts.length == peerSize) {
                hostsForInit = initHosts;
            } else if (initHosts.length == 1) {
                hostsForInit = new String[peerSize];
                for (int i = 0; i < peerSize; i++) {
                    hostsForInit[i] = initHosts[0];
                }
            } else {
                System.err.println("error init-hosts value");
            }
            int[] portsForInit = null;
            if (initPorts.length == peerSize) {
                portsForInit = initPorts;
            } else if (initPorts.length == 1) {
                portsForInit = new int[peerSize];
                for (int i = 0; i < peerSize; i++) {
                    portsForInit[i] = initPorts[0];
                }
            } else {
                System.err.println("error init-ports value");
            }
            String[] hostsForPeer = null;
            if (peerHosts.length == peerSize) {
                hostsForPeer = peerHosts;
            } else if (peerHosts.length == 1) {
                hostsForPeer = new String[peerSize];
                for (int i = 0; i < peerSize; i++) {
                    hostsForPeer[i] = peerHosts[0];
                }
            } else {
                System.err.println("error peer-hosts value");
            }
            int[] portsForManage = null;
            if (peerManagePorts.length == peerSize) {
                portsForManage = peerManagePorts;
            } else if (peerManagePorts.length == 1) {
                portsForManage = new int[peerSize];
                for (int i = 0; i < peerSize; i++) {
                    portsForManage[i] = peerManagePorts[0];
                }
            } else {
                System.err.println("error peer-manage-ports value");
            }
            int[] portsForConsensus = null;
            if (peerConsensusPorts.length == peerSize) {
                portsForConsensus = peerConsensusPorts;
            } else if (peerConsensusPorts.length == 1) {
                portsForConsensus = new int[peerSize];
                for (int i = 0; i < peerSize; i++) {
                    portsForConsensus[i] = peerConsensusPorts[0];
                }
            } else {
                System.err.println("error peer-consensus-ports value");
            }
            // 解压并配置peer节点
            String[] pubkeys = new String[peerSize];
            String[] privkeys = new String[peerSize];
            String[] peerDirs = new String[peerSize];
            String[] raftDirs = new String[peerSize];
            String base58pwd = KeyGenUtils.encodePasswordAsBase58(password);
            for (int i = 0; i < peerSize; i++) {
                peerDirs[i] = out.getAbsolutePath() + File.separator + "peer" + i;
                raftDirs[i] = peerDirs[i] + File.separator + "raft";
                // 解压
                unzipFile(new File(peerZip), peerDirs[i]);
                // 生成公私钥
                AsymmetricKeypair keypair = Crypto.getSignatureFunction(algorithm.toUpperCase()).generateKeypair();
                String pubkey = KeyGenUtils.encodePubKey(keypair.getPubKey());
                pubkeys[i] = pubkey;
                String privkey = KeyGenUtils.encodePrivKey(keypair.getPrivKey(), base58pwd);
                privkeys[i] = privkey;
                String keysDir = peerDirs[i] + File.separator + "config" + File.separator + "keys";
                File keys = new File(keysDir);
                if (!keys.exists()) {
                    keys.mkdirs();
                }
                FileUtils.writeText(pubkey, new File(keysDir + File.separator + i + ".pub"));
                FileUtils.writeText(privkey, new File(keysDir + File.separator + i + ".priv"));
                FileUtils.writeText(base58pwd, new File(keysDir + File.separator + i + ".pwd"));

            }
            for (int i = 0; i < peerSize; i++) {
                switch (consensus) {
                    case RAFT:
                        // 配置 raft.config
                        configRaft(peerDirs[i], hostsForPeer, portsForConsensus);
                        break;
                    case MQ:
                        // 配置 mq.config
                        configMQ(peerDirs[i], rabbit, hostsForPeer, pubkeys);
                        break;
                    case BFTSMART:
                        // 配置 bftsmart.config
                        configBftsmart(peerDirs[i], hostsForPeer, portsForConsensus);
                        break;
                    default:
                        System.err.println("invalid consensus type");
                        return;
                }
                // 配置 local.conf
                configLocal(peerDirs[i], i, pubkeys[i], privkeys[i], base58pwd);
                // 配置 peer-startup.sh
                configPeerStartup(peerDirs[i] + File.separator + "bin" + File.separator + "peer-startup.sh", portsForManage[i]);
            }
            String ledgerSeed = UUID.randomUUID().toString() + UUID.randomUUID().toString();
            String ledgerTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(new Date());
            for (int i = 0; i < peerSize; i++) {
                String peerDir = out.getAbsolutePath() + File.separator + "peer" + i;
                // 配置 ledger.init
                configLedgerInit(peerDir, ledgerSeed, ledgerName, ledgerTime, pubkeys, hostsForInit, portsForInit);
            }

            String gwDir = out.getAbsolutePath() + File.separator + "gw";
            // 解压网关节点
            unzipFile(new File(gwZip), gwDir);
            // 配置网关节点 gateway.conf
            configGateway(gwDir, gwPort, hostsForPeer[0], portsForManage[0], pubkeys[0], privkeys[0], base58pwd);

            System.out.println("INIT CONFIGS FOR LEDGER INITIALIZATION SUCCESS: \n");
            String initializerAddresses = "";
            String peerAddresses = "";
            String consensusAddresses = "";
            for (int i = 0; i < peerSize; i++) {
                initializerAddresses += hostsForInit[i] + ":" + portsForInit[i] + " ";
                peerAddresses += hostsForPeer[i] + ":" + portsForManage[i] + " ";
                consensusAddresses += hostsForPeer[i] + ":" + portsForConsensus[i] + " ";
                consensusAddresses += hostsForPeer[i] + ":" + (portsForConsensus[i] + 1) + " ";
            }
            System.out.println("initializer addresses: " + initializerAddresses);
            System.out.println("peer addresses: " + peerAddresses);
            System.out.println("consensus addresses: " + consensusAddresses);
            System.out.println("gateway port: " + gwPort + "\n");
            System.out.println("more details, see " + output);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("test net init failed");
        }
    }

    /**
     * 文件解压
     *
     * @param source
     * @param output
     */
    private void unzipFile(File source, String output) throws ZipException {
        FileUtils.makeDirectory(output);
        ZipFile zipFile = new ZipFile(source);
        zipFile.extractAll(output);
    }

    private void configBftsmart(String peerDir, String[] peerHosts, int[] peerPorts) {
        String file = peerDir + File.separator + "config" + File.separator + "init" + File.separator + "bftsmart.config";
        StringBuilder sb = new StringBuilder();
        sb.append("############################################\n" +
                "###### #Consensus Participants ######\n" +
                "############################################\n");
        String views = "";
        for (int i = 0; i < peerHosts.length; i++) {
            sb.append("#Consensus Participant" + i + "\n");
            sb.append("system.server." + i + ".network.host=" + peerHosts[i] + "\n");
            sb.append("system.server." + i + ".network.port=" + peerPorts[i] + "\n");
            sb.append("\n");
            if (i == 0) {
                views += i;
            } else {
                views += "," + i;
            }
        }
        sb.append("############################################\n" +
                "####### Communication Configurations #######\n" +
                "############################################\n" +
                "\n" +
                "#HMAC algorithm used to authenticate messages between processes (HmacMD5 is the default value)\n" +
                "#This parameter is not currently being used\n" +
                "#system.authentication.hmacAlgorithm = HmacSHA1\n" +
                "\n" +
                "#Specify if the communication system should use a thread to send data (true or false)\n" +
                "#system.communication.useSenderThread = true  //unused property;\n" +
                "\n" +
                "#Force all processes to use the same public/private keys pair and secret key. This is useful when deploying experiments\n" +
                "#and benchmarks, but must not be used in production systems.\n" +
                "system.communication.defaultkeys = true\n" +
                "\n" +
                "############################################\n" +
                "### Replication Algorithm Configurations ###\n" +
                "############################################\n" +
                "\n" +
                "#Number of servers in the group\n" +
                "system.servers.num = " + peerHosts.length + "\n" +
                "\n" +
                "#Maximum number of faulty replicas\n" +
                "system.servers.f = " + (peerHosts.length - 1) / 3 + "\n" +
                "\n" +
                "#Timeout to asking for a client request\n" +
                "#system.totalordermulticast.timeout = 60000\n" +
                "\n" +
                "#Allowable time tolerance range(millisecond)\n" +
                "system.totalordermulticast.timeTolerance = 3000000\n" +
                "\n" +
                "#Maximum batch size (in number of messages)\n" +
                "system.totalordermulticast.maxbatchsize = 2000\n" +
                "\n" +
                "#Number of nonces (for non-determinism actions) generated\n" +
                "system.totalordermulticast.nonces = 10\n" +
                "\n" +
                "#if verification of leader-generated timestamps are increasing\n" +
                "#it can only be used on systems in which the network clocks\n" +
                "#are synchronized\n" +
                "system.totalordermulticast.verifyTimestamps = false\n" +
                "\n" +
                "#Quantity of messages that can be stored in the receive queue of the communication system\n" +
                "system.communication.inQueueSize = 500000\n" +
                "\n" +
                "# Quantity of messages that can be stored in the send queue of each replica\n" +
                "system.communication.outQueueSize = 500000\n" +
                "\n" +
                "#The time interval for retrying to send message after connection failure.  In milliseconds;\n" +
                "system.communication.send.retryInterval=2000\n" +
                "\n" +
                "#The number of retries to send message after connection failure.\n" +
                "system.communication.send.retryCount=100\n" +
                "\n" +
                "#Set to 1 if SMaRt should use signatures, set to 0 if otherwise\n" +
                "system.communication.useSignatures = 0\n" +
                "\n" +
                "#Set to 1 if SMaRt should use MAC's, set to 0 if otherwise\n" +
                "system.communication.useMACs = 1\n" +
                "\n" +
                "#Set to 1 if SMaRt should use the standard output to display debug messages, set to 0 if otherwise\n" +
                "system.debug = 0\n" +
                "\n" +
                "#Print information about the replica when it is shutdown\n" +
                "system.shutdownhook = true\n" +
                "\n" +
                "############################################\n" +
                "###### State Transfer Configurations #######\n" +
                "############################################\n" +
                "\n" +
                "#Activate the state transfer protocol ('true' to activate, 'false' to de-activate)\n" +
                "system.totalordermulticast.state_transfer = true\n" +
                "\n" +
                "#Maximum ahead-of-time message not discarded\n" +
                "system.totalordermulticast.highMark = 10000\n" +
                "\n" +
                "#Maximum ahead-of-time message not discarded when the replica is still on EID 0 (after which the state transfer is triggered)\n" +
                "system.totalordermulticast.revival_highMark = 10\n" +
                "\n" +
                "#Number of ahead-of-time messages necessary to trigger the state transfer after a request timeout occurs\n" +
                "system.totalordermulticast.timeout_highMark = 200\n" +
                "\n" +
                "############################################\n" +
                "###### Log and Checkpoint Configurations ###\n" +
                "############################################\n" +
                "\n" +
                "system.totalordermulticast.log = true\n" +
                "system.totalordermulticast.log_parallel = false\n" +
                "system.totalordermulticast.log_to_disk = true\n" +
                "system.totalordermulticast.sync_log = false\n" +
                "\n" +
                "#Period at which BFT-SMaRt requests the state to the application (for the state transfer state protocol)\n" +
                "system.totalordermulticast.checkpoint_period = 1000\n" +
                "system.totalordermulticast.global_checkpoint_period = 120000\n" +
                "\n" +
                "system.totalordermulticast.checkpoint_to_disk = false\n" +
                "system.totalordermulticast.sync_ckp = false\n" +
                "\n" +
                "\n" +
                "############################################\n" +
                "###### Reconfiguration Configurations ######\n" +
                "############################################\n" +
                "\n" +
                "#Replicas ID for the initial view, separated by a comma.\n" +
                "# The number of replicas in this parameter should be equal to that specified in 'system.servers.num'\n" +
                "system.initial.view = " + views + "\n" +
                "\n" +
                "#The ID of the trust third party (TTP)\n" +
                "system.ttp.id = 2001\n" +
                "\n" +
                "#This sets if the system will function in Byzantine or crash-only mode. Set to \"true\" to support Byzantine faults\n" +
                "system.bft = true\n" +
                "\n" +
                "#Custom View Storage;\n" +
                "#view.storage.handler=bftsmart.reconfiguration.views.DefaultViewStorage");
        FileUtils.deleteFile(file);
        FileUtils.writeText(sb.toString(), new File(file));
    }

    private void configRaft(String peerDir, String[] peerHosts, int[] portsForConsensus) {
        String file = peerDir + File.separator + "config" + File.separator + "init" + File.separator + "raft.config";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < peerHosts.length; i++) {
            sb.append("system.server." + i + ".network.host=" + peerHosts[i] + "\n" +
                    "system.server." + i + ".network.port=" + portsForConsensus[i] + "\n" +
                    "system.server." + i + ".network.secure=false\n");
        }
        sb.append("\nsystem.server.block.max.num=100\n" +
                "system.server.block.max.bytes=4194304\n" +
                "\n" +
                "system.server.election.timeout=5000\n" +
                "system.server.snapshot.interval=1800\n" +
                "\n" +
                "system.client.configuration.refresh.interval=60000\n" +
                "\n" +
                "system.server.rpc.connect.timeout=10000\n" +
                "system.server.rpc.default.timeout=10000\n" +
                "system.server.rpc.snapshot.timeout=300000\n" +
                "system.server.rpc.request.timeout=120000\n" +
                "\n" +
                "system.raft.maxByteCountPerRpc=131072\n" +
                "system.raft.maxEntriesSize=1024\n" +
                "system.raft.maxBodySize=524288\n" +
                "system.raft.maxAppendBufferSize=262144\n" +
                "system.raft.maxElectionDelayMs=1000\n" +
                "system.raft.electionHeartbeatFactor=5\n" +
                "system.raft.applyBatch=32\n" +
                "system.raft.sync=true\n" +
                "system.raft.syncMeta=false\n" +
                "system.raft.disruptorBufferSize=16384\n" +
                "system.raft.replicatorPipeline=true\n" +
                "system.raft.maxReplicatorInflightMsgs=256\n");
        FileUtils.deleteFile(file);
        FileUtils.writeText(sb.toString(), new File(file));
    }

    private void configMQ(String peerDir, String rabbit, String[] peerHosts, String[] peerPubs) {
        String file = peerDir + File.separator + "config" + File.separator + "init" + File.separator + "mq.config";
        StringBuilder sb = new StringBuilder();
        sb.append("# MQ连接地址，格式：{MQ类型}://{IP}:{PORT}\n" +
                "system.msg.queue.server=amqp://" + rabbit + "\n" +
                "\n" +
                "# 当前账本交易发送队列主题（不同账本需不同主题）\n" +
                "system.msg.queue.topic.tx=tx\n" +
                "\n" +
                "# 当前账本结块消息应答队列主题\n" +
                "system.msg.queue.topic.tx-result=tx-result\n" +
                "\n" +
                "# 当前账本普通消息主题\n" +
                "system.msg.queue.topic.msg=msg\n" +
                "\n" +
                "# 当前账本普通消息主题\n" +
                "system.msg.queue.topic.msg-result=msg-result\n" +
                "\n" +
                "# 当前账本区块信息主题\n" +
                "system.msg.queue.topic.block=block\n" +
                "\n" +
                "# 当前账本结块最大交易数\n" +
                "system.msg.queue.block.txsize=1000\n" +
                "\n" +
                "# 当前账本结块最大时长（单位：毫秒）\n" +
                "system.msg.queue.block.maxdelay=10\n" +
                "\n" +
                "# 当前账本节点总数\n" +
                "system.servers.num=" + peerHosts.length + "\n" +
                "\n" +
                "# 当前账本对应节点的公钥信息列表\n");
        for (int i = 0; i < peerHosts.length; i++) {
            sb.append("system.server." + i + ".pubkey=" + peerPubs[i] + "\n");
        }
        FileUtils.deleteFile(file);
        FileUtils.writeText(sb.toString(), new File(file));
    }

    private void configLedgerInit(String peerDir, String ledgerSeed, String ledgerName, String ledgerTime, String[] pubkeys, String[] initHosts, int[] initPorts) {
        String file = peerDir + File.separator + "config" + File.separator + "init" + File.separator + "ledger.init";
        StringBuilder sb = new StringBuilder("#账本的种子；一段16进制字符，最长可以包含64个字符；可以用字符“-”分隔，以便更容易读取；\n" +
                "ledger.seed=" + ledgerSeed + "\n" +
                "\n" +
                "#账本的描述名称；此属性不参与共识，仅仅在当前参与方的本地节点用于描述用途；\n" +
                "ledger.name=" + ledgerName + "\n" +
                "\n" +
                "#身份认证模式：KEYPAIR/CA，默认KEYPAIR即公私钥对模式\n" +
                "identity-mode=KEYPAIR\n" +
                "\n" +
                "#账本根证书路径，identity-mode 为 CA 时，此选项不能为空，支持多个，半角逗号相隔\n" +
                "root-ca-path=\n" +
                "\n" +
                "#声明的账本创建时间；格式为 “yyyy-MM-dd HH:mm:ss.SSSZ”，表示”年-月-日 时:分:秒:毫秒时区“；例如：“2019-08-01 14:26:58.069+0800”，其中，+0800 表示时区是东8区\n" +
                "created-time=" + ledgerTime + "\n" +
                "\n" +
                "#账本数据底层结构，分为：MERKLE_TREE, KV两种，默认MERKLE_TREE\n" +
                "ledger.data.structure=" + dataStructure + "\n" +
                "\n" +
                "#-----------------------------------------------\n" +
                "# 初始的角色名称列表；可选项；\n" +
                "# 角色名称不区分大小写，最长不超过20个字符；多个角色名称之间用半角的逗点“,”分隔；\n" +
                "# 系统会预置一个默认角色“DEFAULT”，所有未指定角色的用户都以赋予该角色的权限；若初始化时未配置默认角色的权限，则为默认角色分配所有权限；\n" +
                "#\n" +
                "# 注：如果声明了角色，但未声明角色对应的权限清单，这会忽略该角色的初始化；\n" +
                "#\n" +
                "#security.roles=DEFAULT, ADMIN, MANAGER, GUEST\n" +
                "\n" +
                "# 赋予角色的账本权限清单；可选项；\n" +
                "# 可选的权限如下；\n" +
                "# AUTHORIZE_ROLES, SET_CONSENSUS, SET_CRYPTO, REGISTER_PARTICIPANT,\n" +
                "# REGISTER_USER, REGISTER_DATA_ACCOUNT, REGISTER_CONTRACT, UPGRADE_CONTRACT, \n" +
                "# SET_USER_ATTRIBUTES, WRITE_DATA_ACCOUNT, \n" +
                "# APPROVE_TX, CONSENSUS_TX\n" +
                "# 多项权限之间用逗点“,”分隔；\n" +
                "# \n" +
                "#security.role.DEFAULT.ledger-privileges=REGISTER_USER, REGISTER_DATA_ACCOUNT\n" +
                "\n" +
                "# 赋予角色的交易权限清单；可选项；\n" +
                "# 可选的权限如下；\n" +
                "# DIRECT_OPERATION, CONTRACT_OPERATION\n" +
                "# 多项权限之间用逗点“,”分隔；\n" +
                "#\n" +
                "#security.role.DEFAULT.tx-privileges=DIRECT_OPERATION, CONTRACT_OPERATION\n" +
                "\n" +
                "# 其它角色的配置示例；\n" +
                "# 系统管理员角色：只能操作全局性的参数配置和用户注册，只能执行直接操作指令；\n" +
                "#security.role.ADMIN.ledger-privileges=CONFIGURE_ROLES, AUTHORIZE_USER_ROLES, SET_CONSENSUS, SET_CRYPTO, REGISTER_PARTICIPANT, REGISTER_USER\n" +
                "#security.role.ADMIN.tx-privileges=DIRECT_OPERATION\n" +
                "\n" +
                "# 业务主管角色：只能够执行账本数据相关的操作，包括注册用户、注册数据账户、注册合约、升级合约、写入数据等；能够执行直接操作指令和调用合约；\n" +
                "#security.role.MANAGER.ledger-privileges=CONFIGURE_ROLES, AUTHORIZE_USER_ROLES, REGISTER_USER, REGISTER_DATA_ACCOUNT, REGISTER_CONTRACT, UPGRADE_CONTRACT, SET_USER_ATTRIBUTES, WRITE_DATA_ACCOUNT, \n" +
                "#security.role.MANAGER.tx-privileges=DIRECT_OPERATION, CONTRACT_OPERATION\n" +
                "\n" +
                "# 访客角色：不具备任何的账本权限，只有数据读取的操作；也只能够通过调用合约来读取数据；\n" +
                "#security.role.GUEST.ledger-privileges=\n" +
                "#security.role.GUEST.tx-privileges=CONTRACT_OPERATION\n" +
                "\n" +
                "#-----------------------------------------------\n" +
                "#共识服务提供者；必须；\n" +
                "consensus.service-provider=" + consensus.getProvider() +
                "\n" +
                "#共识服务的参数配置；推荐使用绝对路径；必须；\n" +
                "consensus.conf=" + new File(file).getParentFile().getAbsolutePath() + File.separator +
                (consensus.equals(ConsensusTypeEnum.MQ) ? "mq.config" :
                        (consensus.equals(ConsensusTypeEnum.RAFT) ? "raft.config" :
                                "bftsmart.config")
                ) +
                "\n" +
                "#密码服务提供者列表，以英文逗点“,”分隔；必须；\n" +
                "crypto.service-providers=com.jd.blockchain.crypto.service.classic.ClassicCryptoService, \\\n" +
                "com.jd.blockchain.crypto.service.sm.SMCryptoService\n" +
                "\n" +
                "#从存储中加载账本数据时，是否校验哈希；可选；\n" +
                "crypto.verify-hash=true\n" +
                "\n" +
                "#哈希算法；\n" +
                "crypto.hash-algorithm=SHA256\n" +
                "\n" +
                "\n" +
                "#参与方的个数，后续以 cons_parti.id 分别标识每一个参与方的配置；\n" +
                "cons_parti.count=" + pubkeys.length + "\n\n");
        for (int i = 0; i < pubkeys.length; i++) {
            sb.append("#---------------------\n" +
                    "#第" + i + "个参与方的名称；\n" +
                    "cons_parti." + i + ".name=" + i + "\n" +
                    "#第" + i + "个参与方的公钥文件路径；\n" +
                    "cons_parti." + i + ".pubkey-path=\n" +
                    "#第" + i + "个参与方的公钥内容（由keygen工具生成）；此参数优先于 pubkey-path 参数；\n" +
                    "cons_parti." + i + ".pubkey=" + pubkeys[i] + "\n" +
                    "#第" + i + "个参与方的证书路径，identity-mode 为 CA 时，此选项不能为空\n" +
                    "cons_parti." + i + ".ca-path=\n" +
                    "\n" +
                    "#第" + i + "个参与方的角色清单；可选项；\n" +
                    "#cons_parti." + i + ".roles=ADMIN, MANAGER\n" +
                    "#第" + i + "个参与方的角色权限策略，可选值有：UNION（并集），INTERSECT（交集）；可选项；\n" +
                    "#cons_parti." + i + ".roles-policy=UNION\n" +
                    "\n" +
                    "#第" + i + "个参与方的账本初始服务的主机；\n" +
                    "cons_parti." + i + ".initializer.host=" + initHosts[i] + "\n" +
                    "#第" + i + "个参与方的账本初始服务的端口；\n" +
                    "cons_parti." + i + ".initializer.port=" + initPorts[i] + "\n" +
                    "#第" + i + "个参与方的账本初始服务是否开启安全连接；\n" +
                    "cons_parti." + i + ".initializer.secure=false\n\n");
            FileUtils.deleteFile(file);
            FileUtils.writeText(sb.toString(), new File(file));
        }
    }

    private void configLocal(String peerDir, int i, String pubkey, String privkey, String pwd) {
        String file = peerDir + File.separator + "config" + File.separator + "init" + File.separator + "local.conf";
        FileUtils.deleteFile(file);
        FileUtils.writeText("#当前参与方的 id，与ledger.init文件中cons_parti.id一致，默认从0开始\n" +
                        "local.parti.id=" + i + "\n" +
                        "\n" +
                        "#当前参与方的公钥，用于非证书模式\n" +
                        "local.parti.pubkey=" + pubkey + "\n" +
                        "#当前参与方的证书信息，用于证书模式\n" +
                        "local.parti.ca-path=\n" +
                        "\n" +
                        "#当前参与方的私钥（密文编码）\n" +
                        "local.parti.privkey=" + privkey + "\n" +
                        "#当前参与方的私钥文件，PEM格式,用于证书模式\n" +
                        "local.parti.privkey-path=\n" +
                        "\n" +
                        "#当前参与方的私钥解密密钥(原始口令的一次哈希，Base58格式)，如果不设置，则启动过程中需要从控制台输入;\n" +
                        "local.parti.pwd=" + pwd + "\n" +
                        "\n" +
                        "#账本初始化完成后生成的\"账本绑定配置文件\"的输出目录\n" +
                        "#推荐使用绝对路径，相对路径以当前文件(local.conf）所在目录为基准\n" +
                        "ledger.binding.out=../\n" +
                        "\n" +
                        "#账本数据库的连接字符\n" +
                        "#rocksdb数据库连接格式：rocksdb://{path}，例如：rocksdb:///export/App08/peer/rocks.db/rocksdb0.db\n" +
                        "#redis数据库连接格式：redis://{ip}:{prot}/{db}，例如：redis://127.0.0.1:6379/0\n" +
                        "#kvdb数据库连接格式：kvdb://{ip}:{prot}/{db}，例如：kvdb://127.0.0.1:7078/test\n" +
                        "ledger.db.uri=" + ("rocksdb://" + peerDir + File.separator + ledgerName + "-db?bloom=100000000,0.01&lru=5000,5000") + "\n" +
                        "\n" +
                        "#账本数据库的连接口令\n" +
                        "ledger.db.pwd=\n" +
                        "\n" +
                        (consensus.equals(ConsensusTypeEnum.RAFT) ?
                                ("#Raft运行时数据路径\n" + "extra.properties.raft.path=" + peerDir + File.separator + "raft") : ""),
                new File(file));
    }

    private void configPeerStartup(String file, int peerManagePort) {
        FileUtils.deleteFile(file);
        FileUtils.writeText("#!/bin/bash\n" +
                "\n" +
                "#设置Java命令\n" +
                "JAVA_BIN=java\n" +
                "\n" +
                "#定义程序启动的Jar包前缀\n" +
                "APP_JAR_PREFIX=deploy-peer-\n" +
                "\n" +
                "#Peer节点Web端口\n" +
                "#请运维根据实际环境进行调整或通过-p参数传入\n" +
                "WEB_PORT=" + peerManagePort + "\n" +
                "#端口配置参数\n" +
                "IS_CONFIG=false\n" +
                "for i in \"$@\"; do\n" +
                "    if [ $i = \"-p\" ];then\n" +
                "        IS_CONFIG=true\n" +
                "    fi\n" +
                "done\n" +
                "\n" +
                "#检查Java环境变量\n" +
                "if [ ! -n \"$JAVA_HOME\" ]; then\n" +
                "  echo \"UnFound environment variable[JAVA_HOME], will use command[java]...\"\n" +
                "else\n" +
                "  JAVA_BIN=$JAVA_HOME/bin/java\n" +
                "fi\n" +
                "\n" +
                "#获取当前的根目录\n" +
                "APP_HOME=$(cd `dirname $0`;cd ../; pwd)\n" +
                "\n" +
                "#System目录\n" +
                "APP_SYSTEM_PATH=$APP_HOME/system\n" +
                "\n" +
                "#nohup输出日志路径\n" +
                "LOG_OUT=$APP_HOME/bin/peer.out\n" +
                "\n" +
                "#获取Peer节点的启动Jar包\n" +
                "APP_JAR=$(ls $APP_SYSTEM_PATH | grep $APP_JAR_PREFIX)\n" +
                "\n" +
                "#Config配置路径\n" +
                "CONFIG_PATH=$APP_HOME/config\n" +
                "\n" +
                "#ledger-binding.conf完整路径\n" +
                "LEDGER_BINDING_CONFIG=$CONFIG_PATH/ledger-binding.conf\n" +
                "\n" +
                "#application-peer.properties完整路径\n" +
                "SPRING_CONFIG=$CONFIG_PATH/application-peer.properties\n" +
                "\n" +
                "JDK_VERSION=$(java -version 2>&1 | sed '1!d' | sed -e 's/\"//g' | awk '{print $3}')\n" +
                "if [[ $JDK_VERSION == 1.8.* ]]; then\n" +
                "  opens=\"\"\n" +
                "else\n" +
                "  opens=\"--add-opens java.base/java.lang=ALL-UNNAMED\"\n" +
                "  opens=$opens\" --add-opens java.base/java.util=ALL-UNNAMED\"\n" +
                "  opens=$opens\" --add-opens java.base/java.net=ALL-UNNAMED\"\n" +
                "  opens=$opens\" --add-opens java.base/sun.security.x509=ALL-UNNAMED\"\n" +
                "  opens=$opens\" --add-opens java.base/sun.security.util=ALL-UNNAMED\"\n" +
                "fi\n" +
                "\n" +
                "#定义程序启动的参数\n" +
                "JAVA_OPTS=\"-jar -server -Xms2048m -Xmx2048m $opens -Djdchain.log=$APP_HOME/logs -Dlog4j.configurationFile=file:$APP_HOME/config/log4j2-peer.xml\"\n" +
                "\n" +
                "#APP具体相关命令\n" +
                "APP_CMD=$APP_SYSTEM_PATH/$APP_JAR\" -home=\"$APP_HOME\" -c \"$LEDGER_BINDING_CONFIG\" -p \"$WEB_PORT\" -sp \"$SPRING_CONFIG\n" +
                "if [ $IS_CONFIG = true ];then\n" +
                "    APP_CMD=$APP_SYSTEM_PATH/$APP_JAR\" -home=\"$APP_HOME\" -c \"$LEDGER_BINDING_CONFIG\" -sp \"$SPRING_CONFIG\n" +
                "fi\n" +
                "\n" +
                "#APP_JAR的具体路径\n" +
                "APP_JAR_PATH=$APP_SYSTEM_PATH/$APP_JAR\n" +
                "\n" +
                "#JAVA_CMD具体命令\n" +
                "JAVA_CMD=\"$JAVA_BIN $JAVA_OPTS $APP_CMD\"\n" +
                "\n" +
                "###################################\n" +
                "#(函数)判断程序是否已启动\n" +
                "#\n" +
                "#说明：\n" +
                "#使用awk，分割出pid ($1部分)，及Java程序名称($2部分)\n" +
                "###################################\n" +
                "#初始化psid变量（全局）\n" +
                "psid=0\n" +
                "\n" +
                "checkpid() {\n" +
                "  javaps=`ps -ef | grep $APP_JAR_PATH | grep -v grep | awk '{print $2}'`\n" +
                "\n" +
                "  if [[ -n \"$javaps\" ]]; then\n" +
                "    psid=$javaps\n" +
                "  else\n" +
                "    psid=0\n" +
                "  fi\n" +
                "}\n" +
                "\n" +
                "###################################\n" +
                "#(函数)打印系统环境参数\n" +
                "###################################\n" +
                "info() {\n" +
                "  echo \"System Information:\"\n" +
                "  echo \"****************************\"\n" +
                "  echo `uname -a`\n" +
                "  echo\n" +
                "  echo `$JAVA_BIN -version`\n" +
                "  echo\n" +
                "  echo \"APP_HOME=$APP_HOME\"\n" +
                "  echo \"APP_JAR=$APP_JAR\"\n" +
                "  echo \"CONFIG_PATH=$CONFIG_PATH\"\n" +
                "  echo \"APP_JAR_PATH=$APP_JAR_PATH\"\n" +
                "  echo\n" +
                "  echo \"JAVA_CMD=$JAVA_CMD\"\n" +
                "  echo \"****************************\"\n" +
                "}\n" +
                "\n" +
                "#真正启动的处理流程\n" +
                "checkpid\n" +
                "\n" +
                "if [[ $psid -ne 0 ]]; then\n" +
                "  echo \"================================\"\n" +
                "  echo \"warn: Peer already started! (pid=$psid)\"\n" +
                "  echo \"================================\"\n" +
                "else\n" +
                "  echo \"Starting Peer ......\"\n" +
                "  nohup $JAVA_BIN $JAVA_OPTS $APP_CMD $* >$LOG_OUT 2>&1 &\n" +
                "  JAVA_CMD=\"$JAVA_BIN $JAVA_OPTS $APP_CMD $*\"\n" +
                "  sleep 1\n" +
                "  checkpid\n" +
                "  if [[ $psid -ne 0 ]]; then\n" +
                "    echo \"(pid=$psid) [OK]\"\n" +
                "    info\n" +
                "  else\n" +
                "    echo \"[Failed]\"\n" +
                "  fi\n" +
                "fi", new File(file));
    }

    private void configGateway(String gwDir, int gwPort, String peerHost, int peerPort, String pubkey, String privkey, String pwd) {
        String file = gwDir + File.separator + "config" + File.separator + "gateway.conf";
        FileUtils.deleteFile(file);
        FileUtils.writeText("#网关的HTTP服务地址；\n" +
                "http.host=0.0.0.0\n" +
                "#网关的HTTP服务端口；\n" +
                "http.port=" + gwPort + "\n" +
                "#网关服务是否启用安全证书；\n" +
                "http.secure=false\n" +
                "#网关服务SSL客户端认证模式\n" +
                "https.client-auth=none\n" +
                "#网关的HTTP服务上下文路径，可选；\n" +
                "#http.context-path=\n" +
                "\n" +
                "#共识节点的管理服务地址（与该网关节点连接的Peer节点的IP地址）；\n" +
                "peer.host=" + peerHost + "\n" +
                "#共识节点的管理服务端口（与该网关节点连接的Peer节点的端口，即在Peer节点的peer-startup.sh中定义的端口）；\n" +
                "peer.port=" + peerPort + "\n" +
                "#共识节点的管理服务是否启用安全证书；\n" +
                "peer.secure=false\n" +
                "\n" +
                "#共识节点的共识服务是否启用安全证书；\n" +
                "peer.consensus.secure=false\n" +
                "\n" +
                "#账本节点拓扑信息落盘，默认false\n" +
                "topology.store=false\n" +
                "\n" +
                "#是否开启共识节点自动感知，默认true. MQ不支持动态感知\n" +
                "topology.aware=" + (consensus.equals(ConsensusTypeEnum.MQ) ? "false" : "true") + "\n" +
                "#共识节点自动感知间隔（毫秒），0及负值表示仅感知一次。对于不存在节点变更的场景可只感知一次\n" +
                "topology.aware.interval=0\n" +
                "\n" +
                "# 节点连接心跳（毫秒），及时感知连接有效性，0及负值表示关闭\n" +
                "peer.connection.ping=3000\n" +
                "# 节点连接认证（毫秒），及时感知连接合法性，0及负值表示关闭。对于不存在权限变更的场景可关闭\n" +
                "peer.connection.auth=0" +
                "\n" +
                "#共识节点的服务提供解析器\n" +
                "peer.providers=" + consensus.getProvider() +
                "\n" +
                "#数据检索服务对应URL，格式：http://{ip}:{port}，例如：http://127.0.0.1:10001\n" +
                "#若该值不配置或配置不正确，则浏览器模糊查询部分无法正常显示\n" +
                "data.retrieval.url=\n" +
                "schema.retrieval.url=\n" +
                "\n" +
                "#默认公钥的内容（Base58编码数据），非CA模式下必填；\n" +
                "keys.default.pubkey=" + pubkey + "\n" +
                "#默认网关证书路径（X509,PEM），CA模式下必填；\n" +
                "keys.default.ca-path=\n" +
                "#默认私钥的路径；在 pk-path 和 pk 之间必须设置其一；\n" +
                "keys.default.privkey-path=\n" +
                "#默认私钥的内容；在 pk-path 和 pk 之间必须设置其一；\n" +
                "keys.default.privkey=" + privkey + "\n" +
                "#默认私钥的解码密码；\n" +
                "keys.default.privkey-password=" + pwd + "\n", new File(file));
    }
}

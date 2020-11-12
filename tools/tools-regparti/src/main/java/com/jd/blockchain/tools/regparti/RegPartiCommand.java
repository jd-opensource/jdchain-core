package com.jd.blockchain.tools.regparti;

import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.ledger.BlockchainKeypair;
import com.jd.blockchain.ledger.ConsensusSettingsUpdateOperation;
import com.jd.blockchain.ledger.ParticipantRegisterOperation;
import com.jd.blockchain.ledger.ParticipantStateUpdateOperation;
import com.jd.blockchain.ledger.PreparedTransaction;
import com.jd.blockchain.ledger.TransactionContent;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TransactionTemplate;
import com.jd.blockchain.sdk.BlockchainService;
import com.jd.blockchain.sdk.client.GatewayServiceFactory;
import com.jd.blockchain.utils.ArgumentSet;
import com.jd.blockchain.utils.ArgumentSet.Setting;
import com.jd.blockchain.utils.ConsoleUtils;
import com.jd.blockchain.utils.codec.Base58Utils;

/**
 * @Author: zhangshuang
 * @Date: 2020/10/22 5:56 PM
 * Version 1.0
 */
public class RegPartiCommand {

    private static final String NEW_PARTI_LEDGER_ARG = "-ledger";

    private static final String NEW_PARTI_PUBKEY_ARG = "-pub";

    private static final String NEW_PARTI_PRIVKEY_ARG = "-priv";

    private static final String NEW_PARTI_PRIVKEY_PASS_ARG = "-pass";

    private static final String EXIST_USER_PUBKEY_ARG = "-existpub";

    private static final String EXIST_USER_PRIVKEY_ARG = "-existpriv";

    private static final String EXIST_USER_PRIVKEY_PASS_ARG = "-existpass";

    private static final String NEW_PARTI_NAME_ARG = "-name";

    private static final String GATEWAY_HOST_ARG = "-host";

    private static final String GATEWAY_PORT_ARG = "-port";

    private static final String GATEWAY_SECURE_MODE_ARG = "-secure";

    // 是否输出调试信息；
    private static final String OPT_DEBUG = "-debug";

    /**
     * 入口；
     *
     * @param args
     */
    public static void main(String[] args) {
        Setting setting = ArgumentSet.setting().prefix(NEW_PARTI_LEDGER_ARG, NEW_PARTI_PUBKEY_ARG, NEW_PARTI_PRIVKEY_ARG, NEW_PARTI_PRIVKEY_PASS_ARG, EXIST_USER_PUBKEY_ARG, EXIST_USER_PRIVKEY_ARG, EXIST_USER_PRIVKEY_PASS_ARG, NEW_PARTI_NAME_ARG, GATEWAY_HOST_ARG, GATEWAY_PORT_ARG)
                .option(GATEWAY_SECURE_MODE_ARG, OPT_DEBUG);
        ArgumentSet argSet = ArgumentSet.resolve(args, setting);
        try {
            ArgumentSet.ArgEntry[] argEntries = argSet.getArgs();
            if (argEntries.length == 0) {
                ConsoleUtils.info("Miss argument!\r\n"
                        + "-ledger : New participant register ledger info.\r\n"
                        + "-pub : New participant pubkey info.\r\n"
                        + "-priv : New participant privkey info.\r\n"
                        + "-pass : New participant password info.\r\n"
                        + "-name : Name of new participant.\r\n"
                        + "-existpub : Ledger exist user pubkey info.\r\n"
                        + "-existpriv : Ledger exist user privkey info.\r\n"
                        + "-existpass : Exist user password info.\r\n"
                        + "-host : Gateway host ip.\r\n"
                        + "-port : Gateway host port.\r\n"
                        + "-secure : Gateway connect secure moode, optional, defalt false\r\n"
                        + "-debug : Debug mode, optional.\r\n");
                return;
            }

            if (argSet.getArg(NEW_PARTI_LEDGER_ARG) == null) {
                ConsoleUtils.info("Miss ledger info of new participant!");
                return;
            }

            if (argSet.getArg(NEW_PARTI_PUBKEY_ARG) == null) {
                ConsoleUtils.info("Miss pubkey of new participant!");
                return;
            }

            if (argSet.getArg(NEW_PARTI_PRIVKEY_ARG) == null) {
                ConsoleUtils.info("Miss privkey of new participant!");
                return;
            }

            if (argSet.getArg(NEW_PARTI_PRIVKEY_PASS_ARG) == null) {
                ConsoleUtils.info("Miss password of new participant privkey!");
                return;
            }

            if (argSet.getArg(NEW_PARTI_NAME_ARG) == null) {
                ConsoleUtils.info("Miss name of new participant!");
                return;
            }

            if (argSet.getArg(EXIST_USER_PUBKEY_ARG) == null) {
                ConsoleUtils.info("Miss exist user pubkey!");
                return;
            }

            if (argSet.getArg(EXIST_USER_PRIVKEY_ARG) == null) {
                ConsoleUtils.info("Miss exist user privkey!");
                return;
            }

            if (argSet.getArg(EXIST_USER_PRIVKEY_PASS_ARG) == null) {
                ConsoleUtils.info("Miss password of exist user privkey!");
                return;
            }

            if (argSet.getArg(GATEWAY_HOST_ARG) == null) {
                ConsoleUtils.info("Miss connect host of gateway!");
                return;
            }

            if (argSet.getArg(GATEWAY_PORT_ARG) == null) {
                ConsoleUtils.info("Miss connect port of gateway!");
                return;
            }

            DataContractRegistry.register(TransactionContent.class);
            DataContractRegistry.register(TransactionRequest.class);
            DataContractRegistry.register(TransactionResponse.class);
            DataContractRegistry.register(ParticipantRegisterOperation.class);
            DataContractRegistry.register(ParticipantStateUpdateOperation.class);
            DataContractRegistry.register(ConsensusSettingsUpdateOperation.class);

            HashDigest ledgerHash = new HashDigest(Base58Utils.decode(argSet.getArg(NEW_PARTI_LEDGER_ARG).getValue()));

            String pubkey = argSet.getArg(NEW_PARTI_PUBKEY_ARG).getValue();

            String privkey = argSet.getArg(NEW_PARTI_PRIVKEY_ARG).getValue();

            String privKey_pass = argSet.getArg(NEW_PARTI_PRIVKEY_PASS_ARG).getValue();

            String exist_pubkey = argSet.getArg(EXIST_USER_PUBKEY_ARG).getValue();

            String exist_privkey = argSet.getArg(EXIST_USER_PRIVKEY_ARG).getValue();

            String exist_privkey_pass = argSet.getArg(EXIST_USER_PRIVKEY_PASS_ARG).getValue();

            String name = argSet.getArg(NEW_PARTI_NAME_ARG).getValue();

            String gw_host = argSet.getArg(GATEWAY_HOST_ARG).getValue();

            String gw_port = argSet.getArg(GATEWAY_PORT_ARG).getValue();

            String gw_secure = "false";

            if (argSet.getArg(GATEWAY_SECURE_MODE_ARG) != null) {

                gw_secure = argSet.getArg(GATEWAY_SECURE_MODE_ARG).getValue();

            }

            //existed signer
            AsymmetricKeypair keyPair = new BlockchainKeypair(KeyGenUtils.decodePubKey(exist_pubkey), KeyGenUtils.decodePrivKey(exist_privkey, exist_privkey_pass));

            GatewayServiceFactory serviceFactory = GatewayServiceFactory.connect(gw_host, Integer.getInteger(gw_port),false, (BlockchainKeypair) keyPair);

            BlockchainService service = serviceFactory.getBlockchainService();

            // 在本地定义注册账号的 TX；
            TransactionTemplate txTemp = service.newTransaction(ledgerHash);

            BlockchainKeypair user = new BlockchainKeypair(KeyGenUtils.decodePubKey(pubkey), KeyGenUtils.decodePrivKey(privkey, privKey_pass));

            ConsoleUtils.info("Register new participant address = %s" + AddressEncoding.generateAddress(KeyGenUtils.decodePubKey(pubkey)));

            // 注册参与方
            txTemp.participants().register(name, user.getIdentity());

            // TX 准备就绪；
            PreparedTransaction prepTx = txTemp.prepare();

            // 使用私钥进行签名；
            prepTx.sign(keyPair);

            // 提交交易；
            TransactionResponse transactionResponse = prepTx.commit();

        } catch (Exception e) {
            e.printStackTrace();
            ConsoleUtils.info("Error!!! %s", e.getMessage());
            if (argSet.hasOption(OPT_DEBUG)) {
                e.printStackTrace();
            }
        }

    }

}
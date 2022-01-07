package com.jd.blockchain.tools.cli;


import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.ledger.*;
import com.jd.blockchain.sdk.client.GatewayBlockchainServiceProxy;
import com.jd.blockchain.sdk.client.GatewayServiceFactory;
import com.jd.blockchain.transaction.SignatureUtils;

class TxDataAccountRegisterTest {

    public static  HashDigest ledgerHash = null;
    public static  BlockchainKeypair blockchainKeypair = null;
    public static  GatewayBlockchainServiceProxy blockchainService = null;


    public static void init(){

        blockchainService =  (GatewayBlockchainServiceProxy) GatewayServiceFactory.connect("127.0.0.1", 8080, false).getBlockchainService();
        HashDigest[] ledgerHashs = blockchainService.getLedgerHashs();
        ledgerHash = ledgerHashs[0];


        blockchainKeypair = new BlockchainKeypair(KeyGenUtils.decodePubKey("7VeRMiSy9vJ3ipmgXYnDUKz2z9ayx2dPdsnyHtdHiWKQWssb"),
                KeyGenUtils.decodePrivKey("177gjzM3VFsWcJ6bHu51T7GJYgdWjVzvZe1Y8BovktL1jkRmS8Ehj9yfznE1JehKRYMHuh5", "AXhhKihAa2LaRwY5mftnngSPKDF4N9JignnQ4skynY8y"));

    }


    public static void sendRegisterDataAccount(){

        BlockchainIdentity account = BlockchainKeyGenerator.getInstance().generate().getIdentity();
        TransactionTemplate transaction = blockchainService.newTransaction(ledgerHash);

        transaction.dataAccounts().register(account);
        PreparedTransaction ptx = transaction.prepare();

        DigitalSignature digitalSignature = SignatureUtils.sign(ptx.getTransactionHash(), blockchainKeypair);

        ptx.addSignature(digitalSignature);
        TransactionResponse commit = ptx.commit();

        System.out.printf("height: %d, success: %s, address: %s%n", commit.getBlockHeight(), commit.isSuccess(), account.getAddress());

    }



    public static void main(String[] args) throws InterruptedException {

        init();

        for(int i=0; i<=5000; i++){
            sendRegisterDataAccount();
            Thread.sleep(10L);
        }



    }



}
package com.jd.blockchain.gateway.service.topology;

import com.jd.binaryproto.DataContract;
import com.jd.binaryproto.DataField;
import com.jd.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.PrivKey;
import com.jd.blockchain.crypto.PubKey;
import utils.net.NetworkAddress;

@DataContract(code = DataCodes.LEDGER_PEERS_TOPOLOGY)
public interface LedgerPeersTopology {

    @DataField(order = 0, primitiveType = PrimitiveType.BYTES)
    HashDigest getLedger();

    @DataField(order = 1, primitiveType = PrimitiveType.BYTES)
    PubKey getPubKey();

    @DataField(order = 2, primitiveType = PrimitiveType.BYTES)
    PrivKey getPrivKey();

    @DataField(order = 3, primitiveType = PrimitiveType.BYTES, list = true)
    NetworkAddress[] getPeerAddresses();

}

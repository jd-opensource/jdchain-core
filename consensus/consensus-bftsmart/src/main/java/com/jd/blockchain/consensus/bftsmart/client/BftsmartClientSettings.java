package com.jd.blockchain.consensus.bftsmart.client;

import com.jd.blockchain.consensus.bftsmart.BftsmartSessionCredential;
import com.jd.blockchain.consensus.client.ClientSettings;
import utils.net.SSLSecurity;


public interface BftsmartClientSettings extends ClientSettings {

    byte[] getTopology();

    byte[] getTomConfig();
    
    @Override
    BftsmartSessionCredential getSessionCredential();

    SSLSecurity getSSLSecurity();

}

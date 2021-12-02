package com.jd.blockchain.consensus.bftsmart.service;

import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusViewSettings;
import com.jd.blockchain.consensus.service.ServerSettings;
import utils.net.SSLSecurity;

public interface BftsmartServerSettings extends ServerSettings {

    BftsmartConsensusViewSettings getConsensusSettings();

    SSLSecurity getSslSecurity();

}

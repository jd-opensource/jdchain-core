package com.jd.blockchain.consensus.bftsmart.service;

import com.jd.blockchain.consensus.bftsmart.BftsmartConsensusViewSettings;
import com.jd.blockchain.consensus.service.ServerSettings;

public interface BftsmartServerSettings extends ServerSettings {

    BftsmartConsensusViewSettings getConsensusSettings();

}

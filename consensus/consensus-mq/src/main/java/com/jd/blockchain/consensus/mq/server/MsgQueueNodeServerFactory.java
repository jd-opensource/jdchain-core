/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.mq.server.MsgQueueNodeServerFactory
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/12/12 上午11:30
 * Description:
 */
package com.jd.blockchain.consensus.mq.server;

import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.mq.config.MsgQueueNodeConfig;
import com.jd.blockchain.consensus.mq.config.MsgQueueServerConfig;
import com.jd.blockchain.consensus.mq.settings.MsgQueueConsensusSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueNodeSettings;
import com.jd.blockchain.consensus.mq.settings.MsgQueueServerSettings;
import com.jd.blockchain.consensus.service.MessageHandle;
import com.jd.blockchain.consensus.service.NodeServerFactory;
import com.jd.blockchain.consensus.service.ServerSettings;
import com.jd.blockchain.consensus.service.StateMachineReplicate;

import utils.io.Storage;

/**
 *
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */

public class MsgQueueNodeServerFactory implements NodeServerFactory {

	@Override
	public MsgQueueServerSettings buildServerSettings(String realmName, ConsensusViewSettings consensusSetting,
			String nodeAddress) {

		if (!(consensusSetting instanceof MsgQueueConsensusSettings)) {
			throw new IllegalArgumentException(
					"ConsensusSettings data isn't supported! Accept MsgQueueConsensusSettings only!");
		}

		MsgQueueNodeSettings nodeSettings = new MsgQueueNodeConfig().setAddress(nodeAddress);

		MsgQueueServerSettings serverSettings = new MsgQueueServerConfig().setRealmName(realmName)
				.setNodeSettings(nodeSettings).setConsensusSettings((MsgQueueConsensusSettings) consensusSetting);
		return serverSettings;

	}

	@Override
	public MsgQueueNodeServer setupServer(ServerSettings serverSettings, MessageHandle messageHandler,
			StateMachineReplicate stateMachineReplicator, Storage storage) {
		if (!(serverSettings instanceof MsgQueueServerSettings)) {
			throw new IllegalArgumentException(
					"ServerSettings data isn't supported! Accept MsgQueueServerSettings only!");
		}

		MsgQueueNodeServer nodeServer = new MsgQueueNodeServer()
				.setServerSettings((MsgQueueServerSettings) serverSettings).setMessageHandle(messageHandler)
				.setStateMachineReplicator(stateMachineReplicator).init();
		return nodeServer;
	}
}
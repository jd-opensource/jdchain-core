/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved FileName:
 * com.jd.blockchain.mq.config.MsgQueueSettingsFactory Author: shaozhuguang Department: 区块链研发部 Date:
 * 2018/12/12 上午11:49 Description:
 */
package com.jd.blockchain.consensus.mq.config;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.NodeSettings;
import com.jd.blockchain.consensus.SettingsFactory;
import com.jd.blockchain.consensus.mq.MQConsensusSettingsBuilder;
import com.jd.blockchain.consensus.mq.settings.*;
import utils.io.BytesEncoder;

/**
 * @author shaozhuguang
 * @create 2018/12/12
 * @since 1.0.0
 */
public class MQSettingsFactory implements SettingsFactory {

    private static final MQConsensusSettingsEncoder MQCS_ENCODER =
            new MQConsensusSettingsEncoder();
    private static final MQClientIncomingSettingsEncoder MQCIS_ENCODER =
            new MQClientIncomingSettingsEncoder();
    private static final MQConsensusSettingsBuilder BUILDER =
            new MQConsensusSettingsBuilder();

    static {
        DataContractRegistry.register(NodeSettings.class);

        DataContractRegistry.register(MQNodeSettings.class);

        DataContractRegistry.register(ConsensusViewSettings.class);

        DataContractRegistry.register(MQConsensusSettings.class);

        DataContractRegistry.register(MQNetworkSettings.class);

        DataContractRegistry.register(MQBlockSettings.class);

        DataContractRegistry.register(MQClientIncomingSettings.class);

        DataContractRegistry.register(ClientIncomingSettings.class);
    }

    @Override
    public MQConsensusSettingsBuilder getConsensusSettingsBuilder() {
        return BUILDER;
    }

    @Override
    public BytesEncoder<ConsensusViewSettings> getConsensusSettingsEncoder() {
        return MQCS_ENCODER;
    }

    @Override
    public BytesEncoder<ClientIncomingSettings> getIncomingSettingsEncoder() {
        return MQCIS_ENCODER;
    }

    private static class MQConsensusSettingsEncoder
            implements BytesEncoder<ConsensusViewSettings> {

        @Override
        public byte[] encode(ConsensusViewSettings data) {
            if (data instanceof MQConsensusSettings) {
                return BinaryProtocol.encode(data, MQConsensusSettings.class);
            }
            throw new IllegalArgumentException(
                    "Settings data isn't supported! Accept MsgQueueConsensusSettings only!");
        }

        @Override
        public MQConsensusSettings decode(byte[] bytes) {
            return BinaryProtocol.decodeAs(bytes, MQConsensusSettings.class);
        }
    }

    private static class MQClientIncomingSettingsEncoder
            implements BytesEncoder<ClientIncomingSettings> {

        @Override
        public byte[] encode(ClientIncomingSettings data) {
            if (data instanceof MQClientIncomingSettings) {
                return BinaryProtocol.encode(data, MQClientIncomingSettings.class);
            }
            throw new IllegalArgumentException(
                    "Settings data isn't supported! Accept MsgQueueClientIncomingSettings only!");
        }

        @Override
        public MQClientIncomingSettings decode(byte[] bytes) {
            return BinaryProtocol.decodeAs(bytes, MQClientIncomingSettings.class);
        }
    }
}

package com.jd.blockchain.consensus.raft.settings;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.consensus.ClientIncomingSettings;
import com.jd.blockchain.consensus.ConsensusSettingsBuilder;
import com.jd.blockchain.consensus.ConsensusViewSettings;
import com.jd.blockchain.consensus.SettingsFactory;
import com.jd.blockchain.consensus.raft.RaftConsensusSettingsBuilder;
import com.jd.blockchain.consensus.raft.settings.RaftClientIncomingSettings;
import com.jd.blockchain.consensus.raft.settings.RaftConsensusSettings;
import utils.io.BytesEncoder;

public class RaftSettingsFactory implements SettingsFactory {

    private static ConsensusSettingsEncoder CS_ENCODER = new ConsensusSettingsEncoder();

    private static ClientIncomingSettingsEncoder CI_ENCODER = new ClientIncomingSettingsEncoder();

    @Override
    public ConsensusSettingsBuilder getConsensusSettingsBuilder() {
        return new RaftConsensusSettingsBuilder();
    }

    @Override
    public BytesEncoder<ConsensusViewSettings> getConsensusSettingsEncoder() {
        return CS_ENCODER;
    }

    @Override
    public BytesEncoder<ClientIncomingSettings> getIncomingSettingsEncoder() {
        return CI_ENCODER;
    }


    private static class ConsensusSettingsEncoder implements BytesEncoder<ConsensusViewSettings>{

        @Override
        public byte[] encode(ConsensusViewSettings data) {
            if (data instanceof RaftConsensusSettings) {
                return BinaryProtocol.encode(data, RaftConsensusSettings.class);
            }
            throw new IllegalArgumentException("Settings data isn't supported! Accept RaftConsensusViewSettings only!");
        }

        @Override
        public ConsensusViewSettings decode(byte[] bytes) {
            return BinaryProtocol.decode(bytes, RaftConsensusSettings.class);
        }
    }

    private static class ClientIncomingSettingsEncoder implements BytesEncoder<ClientIncomingSettings> {

        @Override
        public byte[] encode(ClientIncomingSettings data) {
            if (data instanceof RaftClientIncomingSettings) {
                return BinaryProtocol.encode(data, RaftClientIncomingSettings.class);
            }
            throw new IllegalArgumentException("Settings data isn't supported! Accept RaftClientIncomingSettings only!");
        }

        @Override
        public ClientIncomingSettings decode(byte[] bytes) {
            return BinaryProtocol.decode(bytes, RaftClientIncomingSettings.class);
        }
    }
}

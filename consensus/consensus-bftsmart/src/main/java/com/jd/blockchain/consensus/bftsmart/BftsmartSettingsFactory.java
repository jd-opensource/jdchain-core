package com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.*;
import com.jd.blockchain.utils.io.BytesEncoder;

public class BftsmartSettingsFactory implements SettingsFactory {
	
	private static ConsensusSettingsEncoder CS_ENCODER = new ConsensusSettingsEncoder();
	
	private static ClientIncomingSettingsEncoder CI_ENCODER = new ClientIncomingSettingsEncoder();

	@Override
	public BftsmartConsensusSettingsBuilder getConsensusSettingsBuilder() {
		return new BftsmartConsensusSettingsBuilder();
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
			if (data instanceof BftsmartConsensusViewSettings) {
				return BinaryProtocol.encode(data, BftsmartConsensusViewSettings.class);
			}
			throw new IllegalArgumentException("Settings data isn't supported! Accept BftsmartConsensusSettings only!");
		}

		@Override
		public ConsensusViewSettings decode(byte[] bytes) {
			return BinaryProtocol.decode(bytes, BftsmartConsensusViewSettings.class);
		}
	}
	
	private static class ClientIncomingSettingsEncoder implements BytesEncoder<ClientIncomingSettings> {

		@Override
		public byte[] encode(ClientIncomingSettings data) {
			if (data instanceof BftsmartClientIncomingSettings) {
				return BinaryProtocol.encode(data, BftsmartClientIncomingSettings.class);
			}
			throw new IllegalArgumentException("Settings data isn't supported! Accept BftsmartClientIncomingSettings only!");
		}

		@Override
		public ClientIncomingSettings decode(byte[] bytes) {
			return BinaryProtocol.decode(bytes, BftsmartClientIncomingSettings.class);
		}
	}
}

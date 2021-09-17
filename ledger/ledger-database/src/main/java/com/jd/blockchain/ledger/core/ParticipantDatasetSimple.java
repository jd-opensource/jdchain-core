package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.ParticipantNode;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.VersioningKVStorage;
import utils.Bytes;
import utils.DataEntry;
import utils.Mapper;
import utils.SkippingIterator;
import utils.Transactional;

public class ParticipantDatasetSimple implements Transactional, ParticipantCollection {

	static {
		DataContractRegistry.register(ParticipantNode.class);
	}

	private SimpleDataset<Bytes, byte[]> dataset;

	private volatile long parti_index_in_block = 0;

	private volatile long origin_parti_index_in_block  = 0;

	private static final Bytes PARTISET_SEQUENCE_KEY_PREFIX = Bytes.fromString("SEQ" + LedgerConsts.KEY_SEPERATOR);

	public ParticipantDatasetSimple(CryptoSetting cryptoSetting, String keyPrefix, ExPolicyKVStorage exPolicyStorage,
                                    VersioningKVStorage verStorage) {
		dataset = new SimpleDatasetImpl(SimpleDatasetType.PARTIS, cryptoSetting, Bytes.fromString(keyPrefix), exPolicyStorage, verStorage);
	}

	public ParticipantDatasetSimple(long preBlockHeight, HashDigest merkleRootHash, CryptoSetting cryptoSetting, String keyPrefix,
                                    ExPolicyKVStorage exPolicyStorage, VersioningKVStorage verStorage, boolean readonly) {
		dataset = new SimpleDatasetImpl(preBlockHeight, merkleRootHash, SimpleDatasetType.PARTIS, cryptoSetting, Bytes.fromString(keyPrefix), exPolicyStorage,
				verStorage, readonly);
	}

	@Override
	public HashDigest getRootHash() {
		return dataset.getRootHash();
	}

	@Override
	public MerkleProof getProof(Bytes key) {
		return dataset.getProof(key);
	}

	@Override
	public boolean isUpdated() {
		return dataset.isUpdated();
	}

	@Override
	public void commit() {
		if (parti_index_in_block == 0) {
			return;
		}
		dataset.commit();
		origin_parti_index_in_block = parti_index_in_block;
	}

	@Override
	public void cancel() {
		if (parti_index_in_block == 0) {
			return;
		}
		dataset.cancel();
		parti_index_in_block = origin_parti_index_in_block;
	}

	@Override
	public long getParticipantCount() {
		return dataset.getDataCount() + parti_index_in_block;
	}

	/**
	 * 加入新的共识参与方； <br>
	 * 如果指定的共识参与方已经存在，则引发 {@link LedgerException} 异常；
	 * 
	 * @param participant
	 */
	public void addConsensusParticipant(ParticipantNode participant) {
		Bytes key = encodeKey(participant.getAddress());
		byte[] participantBytes = BinaryProtocol.encode(participant, ParticipantNode.class);
		long nv = dataset.setValue(key, participantBytes, -1);
		if (nv < 0) {
			throw new LedgerException("Participant already exist! --[id=" + key + "]");
		}

		nv = dataset.setValue(PARTISET_SEQUENCE_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(dataset.getDataCount() + parti_index_in_block))), key.toBytes(), -1);

		if (nv < 0) {
			throw new LedgerException("Participant seq already exist! --[id=" + key + "]");
		}

		parti_index_in_block++;
	}

	/**
	 * 更新共识参与方的状态信息； <br>
	 *
	 * @param participant
	 */
	public void updateConsensusParticipant(ParticipantNode participant) {
		Bytes key = encodeKey(participant.getAddress());
		byte[] participantBytes = BinaryProtocol.encode(participant, ParticipantNode.class);
		long version = dataset.getVersion(key);
		if (version < 0) {
			throw new LedgerException("Participant not exist, update failed!");
		}

		long nv = dataset.setValue(key, participantBytes, version);
		if (nv < 0) {
			throw new LedgerException("Participant update failed!");
		}
	}

	private Bytes encodeKey(Bytes address) {
		return address;
	}

	public boolean isAddNew() {
		return parti_index_in_block != 0;
	}

	@Override
	public boolean contains(Bytes address) {
		Bytes key = encodeKey(address);
		long latestVersion = dataset.getVersion(key);
		return latestVersion > -1;
	}

	/**
	 * 返回指定地址的参与方凭证；
	 * 
	 * <br>
	 * 如果不存在，则返回 null；
	 * 
	 * @param address
	 * @return
	 */
	@Override
	public ParticipantNode getParticipant(Bytes address) {
		Bytes key = encodeKey(address);
		byte[] bytes = dataset.getValue(key);
		if (bytes == null) {
			return null;
		}
		return BinaryProtocol.decode(bytes);
	}

	@Deprecated
	@Override
	public ParticipantNode[] getParticipants() {

		int total = (int) dataset.getDataCount();
		ParticipantNode[] nodes = new ParticipantNode[total];

		for (int index = 0; index < total; index++) {
			byte[] indexKey = dataset.getValue(PARTISET_SEQUENCE_KEY_PREFIX.concat(Bytes.fromString(String.valueOf((long)index))));
			byte[] parti = dataset.getValue(new Bytes(indexKey));
			nodes[index] = BinaryProtocol.decode(parti);
		}

		return nodes;
	}

	@Override
	public SkippingIterator<ParticipantNode> getAllParticipants() {
		// not used in simple ledger database
		return null;
	}

}

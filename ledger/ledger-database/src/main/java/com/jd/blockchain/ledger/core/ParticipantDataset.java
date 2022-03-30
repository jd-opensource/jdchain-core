package com.jd.blockchain.ledger.core;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.LedgerDataStructure;
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

public class ParticipantDataset implements Transactional, ParticipantCollection {

	static {
		DataContractRegistry.register(ParticipantNode.class);
	}

	private BaseDataset<Bytes, byte[]> dataset;

	private LedgerDataStructure ledgerDataStructure;

	// start: used only by kv ledger structure
	private volatile long parti_index_in_block = 0;

	private volatile long origin_parti_index_in_block  = 0;

	private static final Bytes PARTISET_SEQUENCE_KEY_PREFIX = Bytes.fromString("SEQ" + LedgerConsts.KEY_SEPERATOR);
	// end: used only by kv ledger structure


	public ParticipantDataset(CryptoSetting cryptoSetting, String keyPrefix, ExPolicyKVStorage exPolicyStorage,
							  VersioningKVStorage verStorage, LedgerDataStructure dataStructure) {
		ledgerDataStructure = dataStructure;
		if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			dataset = new MerkleHashDataset(cryptoSetting, Bytes.fromString(keyPrefix), exPolicyStorage, verStorage);
		} else {
			dataset = new KvDataset(DatasetType.PARTIS, cryptoSetting, Bytes.fromString(keyPrefix), exPolicyStorage, verStorage);
		}
	}

	public ParticipantDataset(long preBlockHeight, HashDigest merkleRootHash, CryptoSetting cryptoSetting, String keyPrefix,
									ExPolicyKVStorage exPolicyStorage, VersioningKVStorage verStorage, LedgerDataStructure dataStructure, boolean readonly) {
		ledgerDataStructure = dataStructure;
		if (dataStructure.equals(LedgerDataStructure.MERKLE_TREE)) {
			dataset = new MerkleHashDataset(merkleRootHash, cryptoSetting, Bytes.fromString(keyPrefix), exPolicyStorage,
					verStorage, readonly);
		} else {
			dataset = new KvDataset(preBlockHeight, merkleRootHash, DatasetType.PARTIS, cryptoSetting, Bytes.fromString(keyPrefix), exPolicyStorage,
					verStorage, readonly);
		}
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
		dataset.commit();
		origin_parti_index_in_block = parti_index_in_block;
	}

	@Override
	public void cancel() {
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

		if (ledgerDataStructure.equals(LedgerDataStructure.KV)) {
			// 为参与方维护添加的顺序
			Bytes index = PARTISET_SEQUENCE_KEY_PREFIX.concat(Bytes.fromString(String.valueOf(dataset.getDataCount() + parti_index_in_block)));
			nv = dataset.setValue(index, key.toBytes(), -1);

			if (nv < 0) {
				throw new LedgerException("Participant seq already exist! --[id=" + key + "]");
			}

			parti_index_in_block++;
		}
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
		SkippingIterator<ParticipantNode> nodesIterator = getAllParticipants();
		ParticipantNode[] nodes = new ParticipantNode[(int) nodesIterator.getCount()];
		nodesIterator.next(nodes);

		return nodes;
	}

	@Override
	public SkippingIterator<ParticipantNode> getAllParticipants() {
		SkippingIterator<DataEntry<Bytes, byte[]>> dataIterator = dataset.idIterator();
		return dataIterator.iterateAs(new Mapper<DataEntry<Bytes, byte[]>, ParticipantNode>() {

			@Override
			public ParticipantNode from(DataEntry<Bytes, byte[]> source) {
				return source == null ? null : BinaryProtocol.decode(source.getValue());
			}
		});
	}

	public boolean isAddNew() {
		return parti_index_in_block != 0;
	}

	public void clearCachedIndex() {
		parti_index_in_block = 0;
	}

}

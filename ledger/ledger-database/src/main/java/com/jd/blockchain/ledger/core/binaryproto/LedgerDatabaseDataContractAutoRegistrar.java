package com.jd.blockchain.ledger.core.binaryproto;

import com.jd.binaryproto.DataContractAutoRegistrar;
import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.CryptoProvider;
import com.jd.blockchain.ledger.core.LedgerInitDecision;
import com.jd.blockchain.ledger.core.LedgerInitProposal;
import com.jd.blockchain.ledger.merkletree.HashBucketEntry;
import com.jd.blockchain.ledger.merkletree.KeyIndex;
import com.jd.blockchain.ledger.merkletree.MerkleIndex;
import com.jd.blockchain.ledger.proof.MerkleKey;
import com.jd.blockchain.ledger.proof.MerkleLeaf;
import com.jd.blockchain.ledger.proof.MerklePath;
import com.jd.blockchain.ledger.proof.MerkleTrieData;

public class LedgerDatabaseDataContractAutoRegistrar implements DataContractAutoRegistrar{

	@Override
	public void initContext(DataContractRegistry registry) {
		DataContractRegistry.register(MerkleTrieData.class);
		DataContractRegistry.register(MerkleLeaf.class);
		DataContractRegistry.register(MerklePath.class);
		DataContractRegistry.register(MerkleKey.class);
		DataContractRegistry.register(MerkleIndex.class, MerkleIndex::newArray);
		DataContractRegistry.register(KeyIndex.class);
		DataContractRegistry.register(HashBucketEntry.class, HashBucketEntry::newArray);
		DataContractRegistry.register(LedgerInitProposal.class);
		DataContractRegistry.register(LedgerInitDecision.class);
		DataContractRegistry.register(CryptoProvider.class);
//		DataContractRegistry.register(CryptoAlgorithm.class);
	}
}

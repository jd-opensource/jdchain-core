package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.AccountSnapshot;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.TypedValue;

import utils.Bytes;
import utils.Dataset;

public class AccountDecorator implements Account, HashProvable, AccountSnapshot{
	
	protected CompositeAccount mklAccount;
	
	public AccountDecorator(CompositeAccount mklAccount) {
		this.mklAccount = mklAccount;
	}
	
	protected Dataset<String, TypedValue> getHeaders() {
		return mklAccount.getHeaders();
	}

//	@Override
//	public HashDigest getRootHash() {
//		return mklAccount.getRootHash();
//	}

	@Override
	public MerkleProof getProof(Bytes key) {
		return mklAccount.getProof(key);
	}

	@Override
	public BlockchainIdentity getID() {
		return mklAccount.getID();
	}

	@Override
	public MerkleDataset<String, TypedValue> getDataset() {
		return mklAccount.getDataset();
	}

	@Override
	public HashDigest getHeaderRootHash() {
		return mklAccount.getHeaderRootHash();
	}

	@Override
	public HashDigest getDataRootHash() {
		return mklAccount.getDataRootHash();
	}

}

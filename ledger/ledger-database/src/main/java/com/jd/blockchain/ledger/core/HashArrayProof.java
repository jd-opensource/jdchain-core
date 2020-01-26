package com.jd.blockchain.ledger.core;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.MerkleProof;

/**
 * 
 * @author huanghaiquan
 *
 */
public class HashArrayProof implements MerkleProof {

	private HashDigest[] hashPaths;

	public HashArrayProof(HashDigest... hashPaths) {
		this.hashPaths = hashPaths;
	}

	public HashArrayProof(List<HashDigest> hashPaths) {
		this.hashPaths = hashPaths.toArray(new HashDigest[hashPaths.size()]);
	}

	public HashArrayProof(MerkleProof proof) {
		this.hashPaths = proof.getHashPaths();
	}

	public HashArrayProof(MerkleProof proof1, MerkleProof proof2) {
		HashDigest[] path1 = proof1.getHashPaths();
		HashDigest[] path2 = proof2.getHashPaths();
		this.hashPaths = new HashDigest[path1.length + path2.length];
		System.arraycopy(path1, 0, hashPaths, 0, path1.length);
		System.arraycopy(path2, 0, hashPaths, path1.length, path2.length);
	}

	@Override
	public Iterator<HashDigest> iterator() {
		return new HashNodeIterator();
	}

	@Override
	public HashDigest getRootHash() {
		return hashPaths[0];
	}

	@Override
	public HashDigest getDataHash() {
		return hashPaths[hashPaths.length - 1];
	}

	@Override
	public HashDigest[] getHashPaths() {
		return hashPaths.clone();
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(hashPaths);
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof MerkleProof)) {
			return false;
		}
		HashDigest[] path1 = ((MerkleProof)obj).getHashPaths();
		if (hashPaths.length != path1.length) {
			return false;
		}
		for (int i = 0; i < path1.length; i++) {
			if (!hashPaths[i].equals(path1[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder strPath = new StringBuilder();
		for (int i = 0; i < hashPaths.length; i++) {
			if (i > 0) {
				strPath.append("/");
			}
			strPath.append(hashPaths[i].toBase58());
		}
		return strPath.toString();
	}

	private class HashNodeIterator implements Iterator<HashDigest> {

		private int index = 0;

		@Override
		public boolean hasNext() {
			return index < hashPaths.length;
		}

		@Override
		public HashDigest next() {
			return hashPaths[index++];
		}

	}
}

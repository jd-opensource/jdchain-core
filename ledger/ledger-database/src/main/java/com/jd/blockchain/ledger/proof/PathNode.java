package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.MerkleNode;

/**
 * 路径节点；
 * 
 * @author huanghaiquan
 *
 */
class PathNode extends AbstractMerkleNode implements MerklePath {

	private long[] childKeys;
	private long[] childRecords;
	private HashDigest[] childHashs;

	private transient AbstractMerkleNode[] childNodes;

	@Override
	public byte getType() {
		return MerkleElement.PATH_NODE;
	}

	public PathNode(int degree) {
		childKeys = new long[degree];
		childRecords = new long[degree];
		childHashs = new HashDigest[degree];
		setModified();
	}

	private PathNode(HashDigest nodeHash, MerklePath path) {
		this.nodeHash = nodeHash;

		this.childKeys = path.getChildKeys();
		this.childRecords = path.getChildRecords();
		this.childHashs = path.getChildHashs();
	}

	@Override
	public byte[] toBytes() {
		return BinaryProtocol.encode(this, MerklePath.class);
	}

	public static PathNode resolve(HashDigest nodeHash, byte[] nodeBytes) {
		MerklePath path = BinaryProtocol.decodeAs(nodeBytes, MerklePath.class);
		return new PathNode(nodeHash, path);
	}

	public static PathNode create(HashDigest nodeHash, MerklePath path) {
		return new PathNode(nodeHash, path);
	}

	public void setChildNode(byte index, AbstractMerkleNode childPath) {
		if (childNodes == null) {
			childNodes = new AbstractMerkleNode[childHashs.length];
		}
		childNodes[index] = childPath;
		childPath.parent = this;
		if (childPath.isModified()) {
			setModified();
		}
	}

//	public MerkleNode getChildNode(byte keyIndex) {
//		if (childNodes == null) {
//			return null;
//		}
//		return childNodes[keyIndex];
//	}

	@Override
	public long getTotalKeys() {
		long sum = 0;
		for (long s : childKeys) {
			sum += s;
		}
		return sum;
	}

	@Override
	public long getTotalRecords() {
		long sum = 0;
		for (long s : childRecords) {
			sum += s;
		}
		return sum;
	}

	public HashDigest getChildHash(byte keyIndex) {
		return childHashs[keyIndex];
	}

	public boolean containChild(byte keyIndex) {
		return childHashs[keyIndex] != null;
	}

	@Override
	public int getLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	public MerkleNode[] getChildNodes() {
		return childNodes;
	}

	public HashDigest[] getChildHashs() {
		return childHashs;
	}

	@Override
	public long[] getChildKeys() {
		return childKeys;
	}

	@Override
	public long[] getChildRecords() {
		return childRecords;
	}

	@Override
	public void update(HashFunction hashFunc, NodeUpdatedListener updatedListener) {
		if (!isModified()) {
			return;
		}

		if (childNodes != null) {
			// update child nodes;
			for (int i = 0; i < childNodes.length; i++) {
				if (childNodes[i] != null) {
					if (childNodes[i].isModified()) {
						childNodes[i].update(hashFunc, updatedListener);
						childHashs[i] = childNodes[i].nodeHash;
						childKeys[i] = childNodes[i].getTotalKeys();
						childRecords[i] = childNodes[i].getTotalRecords();
					}
				}
			}
		}

		byte[] nodeBytes = BinaryProtocol.encode(this, MerklePath.class);
		HashDigest nodeHash = hashFunc.hash(nodeBytes);
		this.nodeHash = nodeHash;

		updatedListener.onUpdated(nodeHash, this, nodeBytes);

		clearModified();
	}

}
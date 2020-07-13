package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;

/**
 * 路径节点；
 * 
 * @author huanghaiquan
 *
 */
public class PathNode extends MerkleTreeNode implements MerklePath {

	private long[] childKeys;
	private long[] childRecords;
	private HashDigest[] childHashs;

	private transient MerkleTreeNode[] childNodes;

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

	public static PathNode resolve(HashDigest nodeHash, byte[] nodeBytes) {
		MerklePath path = BinaryProtocol.decodeAs(nodeBytes, MerklePath.class);
		return new PathNode(nodeHash, path);
	}

	public static PathNode create(HashDigest nodeHash, MerklePath path) {
		return new PathNode(nodeHash, path);
	}

	public void setChildNode(byte index, HashDigest childHash, MerkleTreeNode childNode) {
		if (childNodes == null) {
			childNodes = new MerkleTreeNode[childHashs.length];
		}
		childHashs[index] = childHash;
		childNodes[index] = childNode;
		if (childNode != null) {
			childKeys[index] = childNode.getTotalKeys();
			childRecords[index] = childNode.getTotalRecords();
			childNode.setParent(this);
			if (childNode.isModified()) {
				setModified();
			}
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
		return childHashs[keyIndex] != null || (childNodes != null && childNodes[keyIndex] != null);
	}

	public MerkleTreeNode getChildNode(byte index) {
		return childNodes == null ? null : childNodes[index];
	}

	public MerkleTreeNode[] getChildNodes() {
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
	protected void update(HashFunction hashFunc, NodeUpdatedListener updatedListener) {
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

	@Override
	protected void cancel() {
		if (!isModified()) {
			return;
		}
		if (childNodes != null) {
			// update child nodes;
			for (int i = 0; i < childNodes.length; i++) {
				if (childNodes[i] != null) {
					if (childNodes[i].isModified()) {
						childNodes[i].cancel();
						if (childHashs[i] == null) {
							childNodes[i] = null;
						}
					}
				}
			}
		}
		clearModified();
	}

}
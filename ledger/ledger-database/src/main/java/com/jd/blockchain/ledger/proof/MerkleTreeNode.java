package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;

/**
 * Abstract node of merkle tree;
 * 
 * @author huanghaiquan
 *
 */
abstract class MerkleTreeNode implements MerkleElement {

	protected HashDigest nodeHash;

	protected MerkleTreeNode parent;

	private boolean modified;

	public boolean isModified() {
		return modified;
	}

	protected void setModified() {
		if (!modified) {
			modified = true;
			if (parent != null) {
				parent.setModified();
			}
		}
	}
	
	protected void clearModified() {
		modified = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jd.blockchain.ledger.core.MerkleNode#getNodeHash()
	 */
	public HashDigest getNodeHash() {
		return nodeHash;
	}

	/**
	 * 根据修改重新计算节点哈希，并重置节点的修改状态({@link #isModified()})为false；
	 * <p>
	 * 
	 * 如果节点没有修改，则直接返回；
	 */
	public abstract void update(HashFunction hashFunc, NodeUpdatedListener updatedListener);

	public abstract long getTotalKeys();

	public abstract long getTotalRecords();
}
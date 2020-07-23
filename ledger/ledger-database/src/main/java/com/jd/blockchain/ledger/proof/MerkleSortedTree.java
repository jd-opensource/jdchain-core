package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContract;
import com.jd.blockchain.binaryproto.DataField;
import com.jd.blockchain.binaryproto.NumberEncoding;
import com.jd.blockchain.binaryproto.PrimitiveType;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.core.MerkleProofException;
import com.jd.blockchain.storage.service.ExPolicy;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.Transactional;

/**
 * 默克尔树；
 * <p>
 * 树的level是按照倒置的方式计算，而不是以根节点的距离衡量，即叶子节点的 level 是 0； <br>
 * 所有的数据的哈希索引都以叶子节点进行记录; <br>
 * 每一个数据节点都以标记一个序列号（Sequence Number, 缩写为 SN），并按照序列号的大小统一地在 level 0
 * 上排列，并填充从根节点到数据节点的所有路径节点； <br>
 * 随着数据节点的增加，整棵树以倒置方式向上增长（根节点在上，叶子节点在下），此设计带来显著特性是已有节点的信息都可以不必修改；
 * 
 * <p>
 * <strong>注：此实现不是线程安全的；</strong><br>
 * 但由于对单个账本中的写入过程被设计为同步写入，因而非线程安全的设计并不会影响在此场景下的使用，而且由于省去了线程间同步操作，反而提升了性能；
 * 
 * @author huanghaiquan
 *
 *
 * @param <T>
 */
public class MerkleSortedTree implements Transactional {

	public static final int TREE_DEGREE = 4;

	public static final int MAX_LEVEL = 32;

	// 正好是 2 的 56 次方(7字节），将 SN 8个字节中的首个字节预留作为 DataNode 的编码格式版本标记；
	public static final long MAX_DATACOUNT = power(TREE_DEGREE, MAX_LEVEL);

	public static final long MAX_SN = MAX_DATACOUNT - 1;

	private final Bytes keyPrefix;

	private CryptoSetting setting;

	private HashFunction hashFunc;

	private ExPolicyKVStorage kvStorage;

	private HashDigest rootHash;

	private AbstractMerklePath root;

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortedTree(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage kvStorage) {
		this(null, setting, Bytes.fromString(keyPrefix), kvStorage);
	}

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortedTree(CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
		this(null, setting, keyPrefix, kvStorage);
	}

	/**
	 * 创建 Merkle 树；
	 * 
	 * @param rootHash     节点的根Hash; 如果指定为 null，则实际上创建一个空的 Merkle Tree；
	 * @param verifyOnLoad 从外部存储加载节点时是否校验节点的哈希；
	 * @param kvStorage    保存 Merkle 节点的存储服务；
	 * @param readonly     是否只读；
	 */
	public MerkleSortedTree(HashDigest rootHash, CryptoSetting setting, String keyPrefix, ExPolicyKVStorage kvStorage) {
		this(rootHash, setting, Bytes.fromString(keyPrefix), kvStorage);
	}

	/**
	 * 创建 Merkle 树；
	 * 
	 * @param rootHash     节点的根Hash; 如果指定为 null，则实际上创建一个空的 Merkle Tree；
	 * @param verifyOnLoad 从外部存储加载节点时是否校验节点的哈希；
	 * @param kvStorage    保存 Merkle 节点的存储服务；
	 * @param readonly     是否只读；
	 */
	public MerkleSortedTree(HashDigest rootHash, CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
		this.setting = setting;
		this.keyPrefix = keyPrefix;
		this.kvStorage = kvStorage;
		this.hashFunc = Crypto.getHashFunction(setting.getHashAlgorithm());

		if (rootHash != null) {
			loadNodeBytes(rootHash);
		}
	}

	private AbstractMerklePath loadPathNode(HashDigest nodeHash) {
		byte[] nodeBytes = loadNodeBytes(nodeHash);
		MerkleIndex idx = BinaryProtocol.decode(nodeBytes, MerkleIndex.class);
		if (idx.getStep() == 1) {
			return new MerkleLeafNode(nodeHash, idx.getOffset(), idx.getChildHashs());
		} else if (idx.getStep() > 1) {
			return new MerklePathNode(nodeHash, idx);
		}
		throw new MerkleProofException(
				"Illegal step value[" + idx.getStep() + "] of MerkleIndex which is loaded from storage!");
	}

	/**
	 * 计算 value 的 x 次方；
	 * <p>
	 * 注：此方法不处理溢出；调用者需要自行规避；
	 * 
	 * @param value
	 * @param x     大于等于 0 的整数；
	 * @return
	 */
	private static long power(long value, int x) {
		if (x == 0) {
			return 1;
		}
		long r = value;
		for (int i = 1; i < x; i++) {
			r *= value;
		}
		return r;
	}

	private static long getLeafNodeOffset(long id) {
		return id - id % TREE_DEGREE;
	}

	public boolean set(long id, byte[] data, ExPolicy ex) {
		if (id < 0) {
			throw new IllegalArgumentException("The argument 'id' is negative!");
		}
		if (root == null) {
			long offset = getLeafNodeOffset(id);
			MerkleLeafNode<T> leaf = new MerkleLeafNode<T>(offset, codec);
			int index = (int) (id - offset);
			leaf.setData(index, data);
			root = leaf;

			return true;
		}

		if (id < root.getOffset()) {
			// 要插入的节点在根节点的左侧的子树；

		} else if (id >= root.getOffset() + root.getStep() * TREE_DEGREE) {
			// 要插入的节点在根节点的右侧的子树；

		} else {
			// 要插入的节点在根节点当前的子树；

		}
	}

	public HashDigest getRootHash() {
		// TODO Auto-generated method stub
		return null;
	}

	public MerkleProof getProof(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isUpdated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void commit() {
		// TODO Auto-generated method stub

	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub

	}
	
	private Bytes encodePathNodeKey(HashDigest nodeHash) {
		// TODO:
				throw new IllegalStateException("Not implemented!");
	}
	
	private Bytes encodeDataNodeKey(long id) {
		// TODO:
		throw new IllegalStateException("Not implemented!");
	}

	/**
	 * 加载指定节点的内容，如果不存在，则抛出异常；
	 * 
	 * @param nodeHash
	 * @return
	 */
	private byte[] loadNodeBytes(HashDigest nodeHash) {
		// TODO:
		throw new IllegalStateException("Not implemented!");
	}

	private void saveNodeBytes(HashDigest nodeHash, byte[] nodeBytes) {
		// TODO:
		throw new IllegalStateException("Not implemented!");
	}

	/**
	 * 表示 {@link MerkleSortedTree} 维护的数据项；
	 * 
	 * @author huanghaiquan
	 *
	 */
	@DataContract(code = DataCodes.MERKLE_SORTED_TREE_DATA)
	public static interface MerkleData {

		/**
		 * 数据哈希；
		 * 
		 * @return
		 */
		@DataField(order = 0, primitiveType = PrimitiveType.BYTES)
		HashDigest getHash();

		/**
		 * 数据字节；
		 * 
		 * @return
		 */
		@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
		byte[] getBytes();

	}

	/**
	 * 默克尔数据索引；
	 * 
	 * @author huanghaiquan
	 *
	 */
	@DataContract(code = DataCodes.MERKLE_SORTED_TREE_INDEX)
	public static interface MerkleIndex {

		/**
		 * 所有子项的起始ID； <br>
		 * 
		 * 即 {@link #getChildHashs()} 中第 0 个子项的 ID ；
		 * 
		 * @return
		 */
		@DataField(order = 0, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
		long getOffset();

		/**
		 * 子项的 ID 的递增步长；<br>
		 * 
		 * 即 {@link #getChildHashs()} 中任意子项的 ID 加上 {@link #getStep()} 为下一个子项的 ID；
		 * 
		 * @return
		 */
		@DataField(order = 1, primitiveType = PrimitiveType.INT8)
		byte getStep();

		/**
		 * 子项的哈希的列表； <br>
		 * 
		 * 子项的个数总是固定的 {@value MerkleSortedTree#TREE_DEGREE} ;
		 * 
		 * @return
		 */
		@DataField(order = 2, primitiveType = PrimitiveType.BYTES, list = true)
		HashDigest[] getChildHashs();
	}

	/**
	 * 默克尔节点；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static interface MerkleNode {

	}

	/**
	 * 默克尔数据节点；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class MerkleDataNode implements MerkleData, MerkleNode {

		private HashDigest hash;

		private byte[] bytes;

		/**
		 * 创建默克尔数据节点；
		 * 
		 * @param hash  数据参数的哈希值；
		 * @param bytes 数据；
		 */
		public MerkleDataNode(HashDigest hash, byte[] bytes) {
			this.hash = hash;
			this.bytes = bytes;
		}

		@Override
		public HashDigest getHash() {
			return hash;
		}

		@Override
		public byte[] getBytes() {
			return bytes;
		}

	}

	/**
	 * 默克尔路径的抽象实现；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private abstract class AbstractMerklePath implements MerkleIndex, MerkleNode {
		
		
		private final long MAX_ID;

		private HashDigest nodeHash;

		private long offset;

		private byte step;

		private HashDigest[] origChildHashs;

		private HashDigest[] childHashs;

		private MerkleNode[] children;

		private boolean modified;

		public AbstractMerklePath(long offset, byte step) {
			MAX_ID = offset + step * TREE_DEGREE;
			
			this.offset = offset;
			this.step = step;
			this.childHashs = new HashDigest[TREE_DEGREE];
			this.origChildHashs = childHashs.clone();
			this.modified = true;
		}

		public AbstractMerklePath(HashDigest nodeHash, MerkleIndex index) {
			this(nodeHash, index.getOffset(), index.getStep(), index.getChildHashs());
		}
		
		protected AbstractMerklePath(HashDigest nodeHash, long offset, byte step, HashDigest[] childHashs) {
			MAX_ID = offset + step * TREE_DEGREE;
			
			this.nodeHash = nodeHash;
			
			this.offset = offset;
			this.step = step;
			this.childHashs = childHashs;
			this.origChildHashs = childHashs.clone();
			this.modified = false;
			
			assert childHashs.length == TREE_DEGREE;
		}

		public boolean isModified() {
			return modified;
		}

		@Override
		public long getOffset() {
			return offset;
		}

		@Override
		public byte getStep() {
			return step;
		}

		@Override
		public HashDigest[] getChildHashs() {
			return childHashs;
		}

		/**
		 * 返回子节点；
		 * 
		 * @param id 子节点的id, 如果子节点不属于当前节点的存储空间。则抛出异常；
		 */
		public MerkleNode getChildNode(long id) {
			assert id >= offset && id < MAX_ID;
			int index = (int) (id - MAX_ID);
			MerkleNode childNode = children[index];
			if (childNode != null) {
				return childNode;
			}
			HashDigest childHash = childHashs[index];
			if (childHash == null) {
				return null;
			}
			childNode = loadChild(id, childHash);
			children[index] = childNode;
			return childNode;
		}

		/**
		 * @param child
		 */
		protected void setChild(int index, MerkleNode child) {
			children[index] = child;
			childHashs[index] = null;
		}

		public HashDigest commit() {
			if (!modified) {
				return nodeHash;
			}
			// save the modified childNodes;
			for (int i = 0; i < TREE_DEGREE; i++) {
				if (children[i] != null && childHashs[i] == null) {
					HashDigest childHash = saveChild(children[i]);
					childHashs[i] = childHash;
				}
			}

			// save;
			byte[] nodeBytes = BinaryProtocol.encode(this, MerkleIndex.class);
			HashDigest hash = hashFunc.hash(nodeBytes);
			saveNodeBytes(hash, nodeBytes);

			this.nodeHash = hash;

			modified = false;

			return hash;
		}

		abstract HashDigest saveChild(MerkleNode childNode);

		abstract MerkleNode loadChild(long id, HashDigest childHash);

	}

	private class MerklePathNode extends AbstractMerklePath {

		public MerklePathNode(long offset, byte step) {
			super(offset, step);

			assert step > 1;
		}

		public MerklePathNode(HashDigest nodeHash, MerkleIndex index) {
			super(nodeHash, index);

			assert index.getStep() > 1;
		}

		@SuppressWarnings("unchecked")
		@Override
		HashDigest saveChildNode(Object node) {
			return ((AbstractMerklePath) node).commit();
		}
		
		@Override
		MerkleNode loadChild(long id, HashDigest childHash) {
			return loadPathNode(childHash);
		}
	}

	private class MerkleLeafNode extends AbstractMerklePath {

		public MerkleLeafNode(long offset) {
			super(offset, (byte) 1);
		}

		public MerkleLeafNode(HashDigest nodeHash, MerkleIndex index) {
			super(nodeHash, index.getOffset(), (byte) 1, index.getChildHashs());
			assert index.getStep() == 1;
		}

		@SuppressWarnings("unchecked")
		@Override
		HashDigest saveChild(MerkleNode data) {
			byte[] bytes = BinaryProtocol.encode(data, MerkleData.class);
			HashDigest hash = hashFunc.hash(bytes);
			saveNodeBytes(hash, bytes);
			return hash;
		}

		@Override
		MerkleNode loadChild(long id, HashDigest childHash) {
			byte[] nodeBytes = loadNodeBytes(childHash);
			
			return dataCodec.decode(nodeBytes);
		}

		@Override
		MerkleNode loadChild(long id, HashDigest childHash) {
			// TODO Auto-generated method stub
			return null;
		}

	}
}

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
public class MerkleSortedTree<T> implements Transactional {

	public static final int TREE_DEGREE = 4;

	public static final int MAX_LEVEL = 32;

	// 正好是 2 的 56 次方(7字节），将 SN 8个字节中的首个字节预留作为 DataNode 的编码格式版本标记；
	public static final long MAX_DATACOUNT = power(TREE_DEGREE, MAX_LEVEL);

	public static final long MAX_SN = MAX_DATACOUNT - 1;

	private final Bytes keyPrefix;

	private CryptoSetting setting;

	private HashFunction hashFunc;

	private ExPolicyKVStorage kvStorage;

	private DataCodec<T> codec;

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
	 * 数据项的编解码器；
	 * 
	 * @author huanghaiquan
	 *
	 * @param <T>
	 */
	public static interface DataCodec<T> {

		byte[] encode(T data);

		T decode(byte[] bytes);

	}

	/**
	 * 默克尔数据索引；
	 * 
	 * @author huanghaiquan
	 *
	 */
	@DataContract(code = DataCodes.MERKLE_INDEX)
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

	private abstract class MerkleNode<E> implements MerkleIndex {

		private HashDigest nodeHash;

		private long offset;

		private byte step;

		private HashDigest[] origChildHashs;

		private HashDigest[] childHashs;

		private E[] childNodeCaches;

		private boolean modified;

		public MerkleNode(long offset, byte step) {
			this.offset = offset;
			this.step = step;
			this.childHashs = new HashDigest[TREE_DEGREE];
			this.origChildHashs = childHashs.clone();
			this.modified = true;
		}

		public MerkleNode(HashDigest nodeHash, long offset, byte step, HashDigest[] childHashs) {
			this.nodeHash = nodeHash;
			this.offset = offset;
			this.step = step;
			this.childHashs = childHashs;
			this.origChildHashs = childHashs.clone();
			this.modified = false;
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
		 * @param index 子节点在哈希列表 {@link #getChildHashs()} 中的位置；
		 */
		public E getChildNode(int index) {
			E childNode = childNodeCaches[index];
			if (childNode != null) {
				return childNode;
			}
			HashDigest nodeHash = childHashs[index];
			if (nodeHash == null) {
				return null;
			}
			childNode = loadChildNode(nodeHash);
			childNodeCaches[index] = childNode;
			return childNode;
		}

		/**
		 * @param childNode
		 */
		public void updateChildNode(E childNode, int index) {
			childNodeCaches[index] = childNode;
			childHashs[index] = null;
		}

		public HashDigest commit() {
			if (!modified) {
				return nodeHash;
			}
			//save the modified childNodes;
			for (int i = 0; i < TREE_DEGREE; i++) {
				if (childNodeCaches[i] != null && childHashs[i] == null) {
					HashDigest childHash = saveChildNode(childNodeCaches[i]);
					childHashs[i] = childHash;
				}
			}

			//save;
			byte[] nodeBytes = BinaryProtocol.encode(this, MerkleIndex.class);
			HashDigest hash = hashFunc.hash(nodeBytes);
			saveNodeBytes(hash, nodeBytes);
			
			this.nodeHash = hash;
			
			modified = false;

			return hash;
		}

		abstract HashDigest saveChildNode(E node);

		abstract E loadChildNode(HashDigest nodeHash);

	}

	private class MerklePathNode extends MerkleNode<MerkleNode<?>> {

		public MerklePathNode(long offset, byte step) {
			super(offset, step);
			assert step > 1;
		}
		
		public MerklePathNode(HashDigest nodeHash, long offset, byte step, HashDigest[] childHashs) {
			super(nodeHash, offset, step, childHashs);
			assert step > 1;
		}

		@Override
		HashDigest saveChildNode(MerkleNode<?> node) {
			return node.commit();
		}

		@Override
		MerkleNode<?> loadChildNode(HashDigest nodeHash) {
			byte[] nodeBytes = loadNodeBytes(nodeHash);
			MerkleIndex idx = BinaryProtocol.decode(nodeBytes, MerkleIndex.class);
			if (idx.getStep() == 1) {
				return new MerkleLeafNode(nodeHash, idx.getOffset(), idx.getChildHashs());
			}else if(idx.getStep() > 1) {
				return new MerklePathNode(nodeHash, idx.getOffset(), idx.getStep(), idx.getChildHashs());
			}
			throw new MerkleProofException("Illegal step value["+idx.getStep()+"] of MerkleIndex which is loaded from storage!");
		}
	}

	private class MerkleLeafNode extends MerkleNode<T> {

		public MerkleLeafNode(long offset) {
			super(offset, (byte) 1);
		}
		
		public MerkleLeafNode(HashDigest nodeHash,long offset, HashDigest[] childHashs) {
			super(nodeHash, offset, (byte) 1, childHashs);
		}

		@Override
		HashDigest saveChildNode(T data) {
			byte[] bytes = codec.encode(data);
			HashDigest hash = hashFunc.hash(bytes);
			saveNodeBytes(hash, bytes);
			return hash;
		}

		@Override
		T loadChildNode(HashDigest nodeHash) {
			byte[] nodeBytes = loadNodeBytes(nodeHash);
			return codec.decode(nodeBytes);
		}

	}
}

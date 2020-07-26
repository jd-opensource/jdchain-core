package com.jd.blockchain.ledger.proof;

import java.util.LinkedList;
import java.util.List;

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
import com.jd.blockchain.ledger.core.HashPathProof;
import com.jd.blockchain.ledger.core.MerkleProofException;
import com.jd.blockchain.storage.service.ExPolicy;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.Transactional;
import com.jd.blockchain.utils.io.BytesUtils;

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

	public static final int MAX_LEVEL = 30;

	// 正好是 2 的 60 次方，足以覆盖 long 类型的正整数，且为避免溢出预留了区间；
	public static final long MAX_COUNT = power(TREE_DEGREE, MAX_LEVEL);

	private final Bytes keyPrefix;

	private CryptoSetting setting;

	private HashFunction hashFunc;

	private ExPolicyKVStorage kvStorage;

	private HashDigest rootHash;

	private MerkleIndex root;

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

	public void set(long id, byte[] data) {
		if (id < 0) {
			throw new IllegalArgumentException("The argument 'id' is negative!");
		}
		if (root == null) {
			long offset = calculateLeafOffset(id);
			MerklePath leaf = createLeafPath(offset);
			leaf.setData(id, data);
			root = leaf;
			return;
		}

		MerkleData dataNode = new MerkleDataNode(id, data);
		root = mergeChildren(dataNode, rootHash, root);
	}

	public HashDigest getRootHash() {
		return rootHash;
	}

	/**
	 * 返回指定编码数据的默克尔证明；
	 * 
	 * @param id
	 * @return
	 */
	public MerkleProof getProof(long id) {
		LinkedList<HashDigest> paths = new LinkedList<HashDigest>();
		MerkleData data = seekData(root, id, paths);
		if (data == null) {
			return null;
		}
		paths.add(data.getHash());
		return new HashPathProof(paths);
	}

	private MerkleData seekData(MerkleIndex node, long id, List<HashDigest> paths) {
		
	}

	@Override
	public boolean isUpdated() {
		if (root == null) {
			return true;
		}
		if (root instanceof MerklePath) {
			return ((MerklePath) root).isModified();
		}
		return false;
	}

	@Override
	public void commit() {
		if (root == null) {
			throw new IllegalStateException("Nothing to commit!");
		}
		if (root instanceof MerklePath) {
			rootHash = ((MerklePath) root).commit();
		}
	}

	@Override
	public void cancel() {
		if (root == null) {
			return;
		}
		if (root instanceof MerklePath) {
			((MerklePath) root).cancel();
		}
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

	/**
	 * 计算指定 id 在叶子节点上的偏移量；
	 * 
	 * @param id
	 * @return
	 */
	private static long calculateLeafOffset(long id) {
		// 叶子节点的 level 为 0 ；
		return calculateOffset(id, 0);
	}

	/**
	 * 计算指定 id 在指定 level 的子树根节点的偏移量； <br>
	 * level 大于等于 0 ，直接包含数据项的叶子节点的 level 为 0； <br>
	 * 默克尔索引节点的步长 {@link MerkleIndex#getStep()} step 等于 {@link #TREE_DEGREE} 的 level
	 * 次方；
	 * 
	 * @param id    要计算的编号；
	 * @param level
	 * @return
	 */
	private static long calculateOffset(long id, int level) {
		// 该层节点数；
		long step = power(TREE_DEGREE, level);
		return calculateOffset(id, step);
	}

	/**
	 * 计算指定 id 在指定 step 的子树根节点的偏移量；
	 * 
	 * @param id
	 * @param step
	 * @return
	 */
	private static long calculateOffset(long id, long step) {
		long count = step * TREE_DEGREE;
		return id - id % count;
	}

	/**
	 * 上一级的步长；
	 * 
	 * @param step
	 * @return
	 */
	private static long upStep(long step) {
		return step * TREE_DEGREE;
	}

	private static long nextOffset(long offset, long step) {
		return offset + step * TREE_DEGREE;
	}

//	private static int index(long id, MerkleIndex merkleIndex) {
//		return index(id, merkleIndex.getOffset(), merkleIndex.getStep());
//	}
//
//	private static int index(long id, long offset, long step) {
//		if (id < offset) {
//			return -1;
//		}
//		long nextOffset = nextOffset(offset, step);
//		if (id >= nextOffset) {
//			return -1;
//		}
//		long p = id - offset;
//		long m = p % step;
//		return (int) ((p - m) / step);
//	}

	/**
	 * 合并子节点，返回共同的父节点；
	 * 
	 * @param data         数据项；
	 * @param pathNodeHash 路径节点的哈希；
	 * @param pathNode     路径节点；
	 * @return
	 */
	private MerklePath mergeChildren(MerkleData data, HashDigest pathNodeHash, MerkleIndex pathNode) {
		long id1 = data.getId();
		long pathOffset = pathNode.getOffset();
		long id2 = pathOffset;

		long step = pathNode.getStep();
		long offset1 = calculateOffset(id1, step);
		long offset2 = pathOffset;

		while (offset1 != offset2) {
			step = upStep(step);
			if (step >= MAX_COUNT) {
				throw new IllegalStateException("The 'step' overlows!");
			}
			offset1 = calculateOffset(id1, step);
			offset2 = calculateOffset(id2, step);
		}

		// offset1 == offset2;
		int index;
		MerklePath childRoot = null;

		if (offset2 == pathOffset) {
			// 数据节点属于 pathNode 路径节点；
			// 把数据节点合并到 pathNode 路径节点；
			if (pathNode instanceof MerklePath) {
				childRoot = (MerklePath) pathNode;
			} else {
				childRoot = new MerklePath(pathNodeHash, pathNode);
			}

			index = childRoot.index(id1);
			childRoot.setChild(index, null, data);
		} else {
			// 数据节点不属于 pathNode 路径节点；
			// 创建共同的父节点；
			childRoot = new MerklePath(offset2, step);

			int index1 = childRoot.index(id1);
			childRoot.setChild(index1, null, data);

			int index2 = childRoot.index(id2);
			childRoot.setChild(index2, pathNodeHash, pathNode);
		}

		return childRoot;
	}

	/**
	 * 合并指定的两个编号的数据项到他们共同的父节点；
	 * 
	 * @param data1
	 * @param data2
	 * @return
	 */
	private MerklePath mergeChildren(MerkleData data1, MerkleData data2) {
		long id1 = data1.getId();
		long id2 = data2.getId();
		long offset1 = -1;
		long offset2 = -1;
		int level;

		// 查找共同的父节点；
		for (level = 0; level < MAX_LEVEL; level++) {
			offset1 = MerkleSortedTree.calculateOffset(id1, level);
			offset2 = MerkleSortedTree.calculateOffset(id2, level);
			if (offset1 == offset2) {
				break;
			}
		}
		if (offset1 == -1) {
			//
			throw new IllegalStateException(
					String.format("Cann't find the \"offset\" of common parent node!  -- id1=%s, id2=!", id1, id2));
		}
		long step = power(TREE_DEGREE, level);
		MerklePath path = new MerklePath(offset1, step);

		path.setChild(null, data1);
		path.setChild(null, data2);

		return path;
	}

	private MerkleIndex loadPathNode(HashDigest nodeHash) {
		byte[] nodeBytes = loadNodeBytes(nodeHash);
		MerkleIndex idx = BinaryProtocol.decode(nodeBytes, MerkleIndex.class);
		if (setting.getAutoVerifyHash()) {
			HashDigest hash = hashFunc.hash(nodeBytes);
			if (!hash.equals(nodeHash)) {
				throw new MerkleProofException("Merkle hash verification fail! -- NodeHash=" + nodeHash.toBase58());
			}
		}
		return idx;
	}

	private MerkleData loadData(long id, HashDigest nodeHash) {
		byte[] nodeBytes = loadNodeBytes(BytesUtils.toBytes(id));
		MerkleData merkleData = BinaryProtocol.decode(nodeBytes, MerkleData.class);
		if (setting.getAutoVerifyHash()) {
			HashDigest hash = hashFunc.hash(nodeBytes);
			if (!hash.equals(nodeHash)) {
				throw new MerkleProofException(
						String.format("Merkle hash verification fail! --ID=%s; NodeHash=%s", id, nodeHash.toBase58()));
			}
		}
		return merkleData;
	}

	/**
	 * 生成存储节点数据的key；
	 * 
	 * @param key 节点逻辑key；
	 * @return 节点的存储key；
	 */
	private Bytes encodeStorageKey(byte[] key) {
		return keyPrefix.concat(key);
	}

	/**
	 * 生成存储节点数据的key；
	 * 
	 * @param key 节点逻辑key；
	 * @return 节点的存储key；
	 */
	private Bytes encodeStorageKey(Bytes key) {
		return keyPrefix.concat(key);
	}

	/**
	 * 加载指定节点的内容，如果不存在，则抛出异常；
	 * 
	 * @param nodeHash
	 * @return
	 */
	private byte[] loadNodeBytes(byte[] key) {
		Bytes storageKey = encodeStorageKey(key);
		byte[] nodeBytes = kvStorage.get(storageKey);
		if (nodeBytes == null) {
			throw new MerkleProofException("Merkle node does not exist! -- key=" + storageKey.toBase58());
		}
		return nodeBytes;
	}

	private byte[] loadNodeBytes(Bytes key) {
		Bytes storageKey = encodeStorageKey(key);
		byte[] nodeBytes = kvStorage.get(storageKey);
		if (nodeBytes == null) {
			throw new MerkleProofException("Merkle node does not exist! -- key=" + storageKey.toBase58());
		}
		return nodeBytes;
	}

	@SuppressWarnings("unused")
	private void saveNodeBytes(byte[] key, byte[] nodeBytes) {
		Bytes storageKey = encodeStorageKey(key);
		boolean success = kvStorage.set(storageKey, nodeBytes, ExPolicy.NOT_EXISTING);
		if (!success) {
			throw new MerkleProofException("Merkle node already exist! -- key=" + storageKey.toBase58());
		}
	}

	private void saveNodeBytes(Bytes key, byte[] nodeBytes) {
		Bytes storageKey = encodeStorageKey(key);
		boolean success = kvStorage.set(storageKey, nodeBytes, ExPolicy.NOT_EXISTING);
		if (!success) {
			throw new MerkleProofException("Merkle node already exist! -- key=" + storageKey.toBase58());
		}
	}

	/**
	 * 默克尔节点；
	 * 
	 * @author huanghaiquan
	 *
	 */
	public static interface MerkleEntry {

	}

	/**
	 * 表示 {@link MerkleSortedTree} 维护的数据项；
	 * 
	 * @author huanghaiquan
	 *
	 */
	@DataContract(code = DataCodes.MERKLE_SORTED_TREE_DATA)
	public static interface MerkleData extends MerkleEntry {

		@DataField(order = 0, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
		long getId();

		/**
		 * 数据({@link #getBytes()})的哈希；
		 * 
		 * @return
		 */
		@DataField(order = 1, primitiveType = PrimitiveType.BYTES)
		HashDigest getHash();

		/**
		 * 数据字节；
		 * 
		 * @return
		 */
		@DataField(order = 2, primitiveType = PrimitiveType.BYTES)
		byte[] getBytes();

	}

	/**
	 * 默克尔数据索引；
	 * 
	 * @author huanghaiquan
	 *
	 */
	@DataContract(code = DataCodes.MERKLE_SORTED_TREE_INDEX)
	public static interface MerkleIndex extends MerkleEntry {

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
		@DataField(order = 1, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
		long getStep();

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
	 * 默克尔数据节点；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private class MerkleDataNode implements MerkleData {

		private long id;

		private HashDigest hash;

		private byte[] bytes;

		/**
		 * 创建默克尔数据节点；
		 * 
		 * @param hash  数据参数的哈希值；
		 * @param bytes 数据；
		 */
		public MerkleDataNode(long id, byte[] bytes) {
			this.id = id;
			this.bytes = bytes;

			this.hash = hashFunc.hash(bytes);
		}

		@Override
		public long getId() {
			return id;
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

	private MerklePath createLeafPath(long offset) {
		return new MerklePath(offset, 1L);
	}

	/**
	 * 默克尔路径的抽象实现；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private class MerklePath implements MerkleIndex {

		/**
		 * 与当前子树相邻的右侧兄弟子树的偏移量；
		 */
		private final long NEXT_OFFSET;

		private HashDigest nodeHash;

		private long offset;

		private long step;

		private HashDigest[] origChildHashs;

		private HashDigest[] childHashs;

		private MerkleEntry[] children;

		protected MerklePath(long offset, long step) {
			this(null, offset, step, new HashDigest[TREE_DEGREE]);
		}

		protected MerklePath(HashDigest nodeHash, MerkleIndex index) {
			this(nodeHash, index.getOffset(), index.getStep(), index.getChildHashs());
		}

		protected MerklePath(HashDigest nodeHash, long offset, long step, HashDigest[] childHashs) {
			assert step > 0;
			NEXT_OFFSET = nextOffset(offset, step);

			this.nodeHash = nodeHash;

			this.offset = offset;
			this.step = step;
			this.childHashs = childHashs;
			this.origChildHashs = childHashs.clone();
			this.children = new MerkleEntry[TREE_DEGREE];

			assert childHashs.length == TREE_DEGREE;
		}

		public boolean isModified() {
			for (int i = 0; i < TREE_DEGREE; i++) {
				if (childHashs[i] == null && children[i] != null) {
					return true;
				}
			}
			return false;
		}

		@Override
		public long getOffset() {
			return offset;
		}

		@Override
		public long getStep() {
			return step;
		}

		@Override
		public HashDigest[] getChildHashs() {
			return childHashs;
		}

		/**
		 * 返回指定 ID 在当前节点表示的子树的偏移位置；
		 * 
		 * <br>
		 * 
		 * 如果不属于当前节点，则返回 -1；
		 * 
		 * @param id
		 * @return
		 */
		public int index(long id) {
			if (id < offset || id >= NEXT_OFFSET) {
				return -1;
			}
			long p = id - offset;
			long m = p % step;
			return (int) ((p - m) / step);
		}

		/**
		 * 返回指定 id 的数据所在的直接子节点；
		 * 
		 * @param id 子节点的id, 如果子节点不属于当前节点的存储空间。则抛出异常；
		 */
		public MerkleEntry getChildPath(long id) {
			int index = index(id);
			assert index > -1;
			MerkleEntry child = children[index];
			if (child != null) {
				return child;
			}
			HashDigest childHash = childHashs[index];
			if (childHash == null) {
				return null;
			}

			child = loadChild(id, childHash);
			children[index] = child;
			return child;
		}

		private MerkleEntry loadChild(long id, HashDigest childHash) {
			MerkleEntry child;
			if (step == 1) {
				// 叶子节点；
				child = loadData(id, childHash);
			} else {
				// step > 1， 非叶子节点； 注：构造器对输入参数的处理保证 step > 0;
				child = loadPathNode(childHash);
			}
			return child;
		}

		/**
		 * 设置数据；<br>
		 * 
		 * 如果指定编号的数据已经存在，则抛出 {@link MerkleProofException} 异常；
		 * 
		 * @param id        数据的唯一编号；
		 * @param dataBytes 数据；
		 * @return 返回该编号的数据写入的子树的位置; <br>
		 *         如果指定编号不属于该子树，则返回值大于等于 0 且小于 {@value MerkleSortedTree#TREE_DEGREE};
		 *         <br>
		 *         如果指定编号不属于该子树，则返回 -1；
		 */
		public int setData(long id, byte[] dataBytes) {
			int index = index(id);
			if (index < 0) {
				return index;
			}

			MerkleData newData = new MerkleDataNode(id, dataBytes);

			HashDigest childHash = childHashs[index];
			MerkleEntry child = children[index];

			if (child == null) {
				if (childHash == null) {
					// 完全没有子树时，直接附加数据节点；
					// 当新节点在此子树中没有其它兄弟节点时，不建立从当前节点到叶子节点之间完整的路径节点，目的是缩减空间，优化处理少量数据节点的情形；
					setChild(index, null, newData);
					return index;
				}

				child = loadChild(id, childHash);
			}

			if (child instanceof MerkleData) {
				// 已经有子节点存在，检查 id 是否冲突，如果不冲突，则合并两个数据节点到同样一棵子树；
				MerkleData childData = (MerkleData) child;
				if (id == childData.getId()) {
					// TODO: 出现 id 冲突；同一个 id 不能设置两次；
					throw new MerkleProofException("The data entry with the same id[" + id + "] already exist!");
				}

				child = mergeChildren(newData, childData);
			} else {
				// 已经有子树存在，检查要加入的节点属于该子树，还是与其是兄弟子树，合并这两个节点；
				MerkleIndex merkleIndex = (MerkleIndex) child;

				child = mergeChildren(newData, childHash, merkleIndex);
			}

			setChild(index, null, child);
			return index;
		}

		/**
		 * 设置子节点；
		 * 
		 * @param id        子节点的编号；
		 * @param childHash
		 * @param child
		 */
		private void setChild(HashDigest childHash, MerkleData data) {
			int index = index(data.getId());
			assert index > -1;
			setChild(index, childHash, data);
		}

		/**
		 * 设置子节点；
		 * 
		 * @param index     子节点的位置；
		 * @param childHash 子节点的哈希；如果为 null，则在 commit 时计算哈希；
		 * @param child     子节点；
		 */
		private void setChild(int index, HashDigest childHash, MerkleEntry child) {
			childHashs[index] = childHash;
			children[index] = child;
		}

		public HashDigest commit() {
			// save the modified childNodes;
			boolean modified = false;
			for (int i = 0; i < TREE_DEGREE; i++) {
				if (childHashs[i] == null && children[i] != null) {
					modified = true;

					MerkleEntry child = children[i];
					// 需要先保存子节点，获得子节点的哈希；
					if (step == 1) {
						// 当前已经是叶子节点，子项是数据项；
						long id = offset + i * step;
						childHashs[i] = saveData(id, (MerkleData) child);
					} else {
						// step > 1， 非叶子节点； 注：构造器对输入参数的处理保证 step > 0;
						if (child instanceof MerklePath) {
							childHashs[i] = ((MerklePath) child).commit();
						} else {
							// 注：上下文逻辑应确保不可能进入此分支，即一个新加入的尚未生成哈希的子节点，却不是 MerklePathNode 实例；
							// 对于附加已存在的节点的情况，已存在的节点已经生成子节点哈希，并且其实例是 MerkleIndex 的动态代理；
							throw new IllegalStateException(
									"Illegal child node which has no hash and is not instance of MerklePathNode!");
						}
					}
				}
			}

			if (!modified) {
				return nodeHash;
			}

			// save;
			byte[] nodeBytes = BinaryProtocol.encode(this, MerkleIndex.class);
			HashDigest hash = hashFunc.hash(nodeBytes);
			Bytes storageKey = encodeStorageKey(hash);
			saveNodeBytes(storageKey, nodeBytes);

			// update hash;
			for (int i = 0; i < TREE_DEGREE; i++) {
				origChildHashs[i] = childHashs[i];
			}
			this.nodeHash = hash;

			return hash;
		}

		public void cancel() {
			MerkleEntry child;
			for (int i = 0; i < TREE_DEGREE; i++) {
				if (childHashs[i] == null || origChildHashs[i] == null || (!childHashs[i].equals(origChildHashs[i]))) {
					child = children[i];
					children[i] = null;
					// 清理字节点以便优化大对象的垃圾回收效率；
					if (child != null && child instanceof MerklePath) {
						((MerklePath) child).cancel();
					}
				}
				childHashs[i] = origChildHashs[i];
			}
			// 注：不需要处理 nodeHash 的回滚，因为 nodeHash 是 commit 操作的最后确认标志；
		}

		HashDigest saveData(long id, MerkleData data) {
			byte[] dataNodeBytes = BinaryProtocol.encode(data, MerkleData.class);

			// 以 id 建议存储key ，便于根据 id 直接快速查询检索，无需展开默克尔树；
			Bytes storageKey = encodeStorageKey(BytesUtils.toBytes(id));
			saveNodeBytes(storageKey, dataNodeBytes);

			HashDigest dataEntryHash = hashFunc.hash(dataNodeBytes);
			return dataEntryHash;
		}

	}

}

package com.jd.blockchain.ledger.proof;

import java.util.ArrayList;
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
import com.jd.blockchain.utils.AbstractSkippingIterator;
import com.jd.blockchain.utils.ArrayUtils;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.MathUtils;
import com.jd.blockchain.utils.SkippingIterator;
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
 */
public class MerkleSortedTree implements Transactional {

	public static final int DEFAULT_DEGREE = TreeDegree.D3.DEGREEE;

	public static final int DEFAULT_MAX_LEVEL = TreeDegree.D3.MAX_DEPTH;

	public static final long DEFAULT_MAX_COUNT = TreeDegree.D3.MAX_COUNT;

	protected final int DEGREE;

	protected final int MAX_LEVEL;

	// 正好是 2 的 60 次方，足以覆盖 long 类型的正整数，且为避免溢出预留了区间；
	protected final long MAX_COUNT;
	
	protected final HashFunction DEFAULT_HASH_FUNCTION;

	protected final CryptoSetting setting;
	
	private final Bytes KEY_PREFIX;

	private ExPolicyKVStorage kvStorage;
	
	private MerklePathNode root;

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortedTree(CryptoSetting setting, String keyPrefix, ExPolicyKVStorage kvStorage) {
		this(TreeDegree.D3, setting, Bytes.fromString(keyPrefix), kvStorage);
	}

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortedTree(TreeDegree mode, CryptoSetting setting, String keyPrefix, ExPolicyKVStorage kvStorage) {
		this(mode, setting, Bytes.fromString(keyPrefix), kvStorage);
	}

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortedTree(CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
		this(TreeDegree.D3, setting, keyPrefix, kvStorage);
	}

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortedTree(TreeDegree mode, CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
		this.DEGREE = mode.DEGREEE;
		this.MAX_LEVEL = mode.MAX_DEPTH;
		this.MAX_COUNT = MathUtils.power(DEGREE, MAX_LEVEL);

		this.setting = setting;
		this.KEY_PREFIX = keyPrefix;
		this.kvStorage = kvStorage;
		this.DEFAULT_HASH_FUNCTION = Crypto.getHashFunction(setting.getHashAlgorithm());

		this.root = createTopRoot();
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
		this.KEY_PREFIX = keyPrefix;
		this.setting = setting;
		this.kvStorage = kvStorage;
		this.DEFAULT_HASH_FUNCTION = Crypto.getHashFunction(setting.getHashAlgorithm());

		IndexEntry merkleIndex = loadMerkleIndex(rootHash);
		int subtreeCount = merkleIndex.getChildCounts().length;
		TreeDegree degree = null;
		for (TreeDegree td : TreeDegree.values()) {
			if (td.DEGREEE == subtreeCount) {
				degree = td;
			}
		}
		if (degree == null) {
			throw new MerkleProofException("The root node with hash[" + rootHash.toBase58() + "] has wrong degree!");
		}

		this.DEGREE = degree.DEGREEE;
		this.MAX_LEVEL = degree.MAX_DEPTH;
		this.MAX_COUNT = MathUtils.power(DEGREE, MAX_LEVEL);

		this.root = new MerklePathNode(rootHash, merkleIndex, this);
	}

	/**
	 * 创建顶级根节点；
	 * 
	 * @return
	 */
	protected MerklePathNode createTopRoot() {
		long step = MAX_COUNT / DEGREE;
		return new MerklePathNode(0, step, this);
	}

	public HashDigest getRootHash() {
		return root.getNodeHash();
	}

	public long getCount() {
		return count(root);
	}
	
	private void checkId(long id) {
		if (id < 0) {
			throw new IllegalArgumentException("'id' is negative!");
		}
		if (id >= MAX_COUNT) {
			throw new IllegalArgumentException("'id' is greater than or equal to the MAX_COUNT[" + MAX_COUNT + "]!");
		}
	}

	public void set(long id, byte[] data) {
		checkId(id);;
		
		ValueEntry dataNode = new MerkleDataNode(id, data);

		root = mergeChildren(null, dataNode, root.getNodeHash(), root);
	}
	
	public void set(ValueEntry data) {
		checkId(data.getId());
		
		root = mergeChildren(null, data, root.getNodeHash(), root);
	}
	
	public ValueEntry get(long id) {
		return seekData(root, id, NullSelector.INSTANCE);
	}

	public SkippingIterator<ValueEntry> iterator() {
		// 克隆根节点的数据，避免根节点的更新影响了迭代器；
		return new MerklePathIterator(root.getOffset(), root.getStep(), root.getChildHashs().clone(),
				root.getChildCounts().clone());
	}

	/**
	 * 返回指定编码数据的默克尔证明；
	 * 
	 * @param id
	 * @return
	 */
	public MerkleProof getProof(long id) {
		MerkleProofSelector proofSelector = new MerkleProofSelector();
		proofSelector.accept(root.getNodeHash(), root);
		ValueEntry data = seekData(root, id, proofSelector);
		if (data == null) {
			return null;
		}
//		proofSelector.addPath(data.getHash());
		return proofSelector.getProof();
	}

	/**
	 * 从指定的默克尔索引开始，搜索指定 id 的数据，并记录搜索经过的节点的哈希；如果数据不存在，则返回 null；
	 * 
	 * @param merkleIndex 开始搜索的默克尔索引节点；
	 * @param id          要查找的数据的 id；
	 * @param paths       哈希列表，记录搜索经过的节点；（注：不包含 merkleIndex 参数指定的节点的哈希）
	 * @return
	 */
	public ValueEntry seekData(IndexEntry merkleIndex, long id, MerkleEntrySelector pathSelector) {
		int idx = index(id, merkleIndex);
		if (idx < 0) {
			return null;
		}
		MerkleEntry child;
		if (merkleIndex instanceof MerklePathNode) {
			MerklePathNode path = (MerklePathNode) merkleIndex;
			child = path.getChildAtIndex(idx);
			if (child == null) {
				return null;
			}
			HashDigest childHash = path.getChildHashs()[idx];
			pathSelector.accept(childHash, child);
		} else {
			HashDigest[] childHashs = merkleIndex.getChildHashs();
			HashDigest childHash = childHashs[idx];
			if (childHash == null) {
				return null;
			}
			child = loadMerkleEntry(childHash);
			pathSelector.accept(childHash, child);
		}
		if (child instanceof ValueEntry) {
			return (ValueEntry) child;
		}
		return seekData((IndexEntry) child, id, pathSelector);
	}

	@Override
	public boolean isUpdated() {
		if (root == null) {
			return true;
		}
		if (root instanceof MerklePathNode) {
			return ((MerklePathNode) root).isModified();
		}
		return false;
	}

	@Override
	public void commit() {
		root.commit();
	}

	@Override
	public void cancel() {
		root.cancel();
	}

	/**
	 * 计算指定 id 在指定 level 的子树根节点的偏移量； <br>
	 * level 大于等于 0 ，直接包含数据项的叶子节点的 level 为 0； <br>
	 * 默克尔索引节点的步长 {@link IndexEntry#getStep()} step 等于 {@link #DEGREE} 的 level 次方；
	 * 
	 * @param id    要计算的编号；
	 * @param level
	 * @return
	 */
	private long calculateOffset(long id, int level) {
		// 该层节点数；
		long step = MathUtils.power(DEGREE, level);
		return calculateOffset(id, step);
	}

	/**
	 * 计算指定 id 在指定 step 的子树根节点的偏移量；
	 * 
	 * @param id
	 * @param step
	 * @return
	 */
	private long calculateOffset(long id, long step) {
		long count = step * DEGREE;
		return id - id % count;
	}

	/**
	 * 上一级的步长；
	 * 
	 * @param step
	 * @return
	 */
	private long upStep(long step) {
		return step * DEGREE;
	}

	private long nextOffset(long offset, long step) {
		return offset + step * DEGREE;
	}

	/**
	 * 计算指定 id 在 {@link IndexEntry} 中的偏移位置；<br>
	 * 如果 id 不属于 {@link IndexEntry} 的地址区间，则返回 -1；
	 * 
	 * @param id          编号；
	 * @param merkleIndex 默克尔索引，表示1个特定的位置区间；
	 * @return
	 */
	private int index(long id, IndexEntry merkleIndex) {
		return index(id, merkleIndex.getOffset(), merkleIndex.getStep());
	}

	/**
	 * 计算指定 id 在指定地址区间中的偏移位置；<br>
	 * 
	 * @param id     编号；
	 * @param offset 初始偏移位置；
	 * @param step   步长，1个位置包含的节点数；由“步长 * {@link #DEGREE} ”构成参与计算的位置区间范围；
	 * @return
	 */
	private int index(long id, long offset, long step) {
		if (id < offset) {
			return -1;
		}
		long nextOffset = nextOffset(offset, step);
		if (id >= nextOffset) {
			return -1;
		}
		long p = id - offset;
		long m = p % step;
		return (int) ((p - m) / step);
	}

	private static long count(IndexEntry merkleIndex) {
		long[] childCounts = merkleIndex.getChildCounts();
		// 使用此方法的上下文逻辑已经能够约束每一项的数字大小范围，不需要考虑溢出；
		return ArrayUtils.sum(childCounts);
	}

	/**
	 * 合并子节点，返回共同的父节点；
	 * 
	 * @param dataNode     数据项；
	 * @param pathNodeHash 路径节点的哈希；
	 * @param pathNode     路径节点；
	 * @return
	 */
	private MerklePathNode mergeChildren(HashDigest dataNodeHash, ValueEntry dataNode, HashDigest pathNodeHash,
			IndexEntry pathNode) {
		final long PATH_OFFSET = pathNode.getOffset();
		final long PATH_STEP = pathNode.getStep();

		long dataId = dataNode.getId();
		long pathId = PATH_OFFSET;

		long dataOffset = calculateOffset(dataId, PATH_STEP);
		long pathOffset = PATH_OFFSET;

		long step = PATH_STEP;
		while (dataOffset != pathOffset) {
			step = upStep(step);
			if (step >= MAX_COUNT) {
				throw new IllegalStateException("The 'step' overlows!");
			}
			dataOffset = calculateOffset(dataId, step);
			pathOffset = calculateOffset(pathId, step);
		}

		// offset1 == offset2;
		int index;
		MerklePathNode childRoot = null;

		if (step == PATH_STEP && pathOffset == PATH_OFFSET) {
			// 数据节点属于 pathNode 路径节点；
			// 把数据节点合并到 pathNode 路径节点；
			if (pathNode instanceof MerklePathNode) {
				childRoot = (MerklePathNode) pathNode;
			} else {
				childRoot = new MerklePathNode(pathNodeHash, pathNode, this);
			}

			index = childRoot.index(dataId);

			updateChild(childRoot, index, dataNodeHash, dataNode);
		} else {
			// 数据节点不属于 pathNode 路径节点；
			// 创建共同的父节点；
			childRoot = new MerklePathNode(pathOffset, step, this);

			int dataChildIndex = childRoot.index(dataId);
			updateChild(childRoot, dataChildIndex, dataNodeHash, dataNode);

			int pathChildIndex = childRoot.index(pathId);
			updateChild(childRoot, pathChildIndex, pathNodeHash, pathNode);
		}

		return childRoot;
	}

	/**
	 * 合并子节点，返回共同的父节点；
	 * 
	 * @param pathNode1     数据项；
	 * @param pathNodeHash2 路径节点的哈希；
	 * @param pathNode2     路径节点；
	 * @return
	 */
	private MerklePathNode mergeChildren(HashDigest pathNodeHash1, IndexEntry pathNode1, HashDigest pathNodeHash2,
			IndexEntry pathNode2) {
		final long PATH_OFFSET1 = pathNode1.getOffset();
		final long PATH_STEP1 = pathNode1.getStep();
		final long PATH_OFFSET2 = pathNode2.getOffset();
		final long PATH_STEP2 = pathNode2.getStep();
		if (PATH_OFFSET1 == PATH_OFFSET2 && PATH_STEP1 == PATH_STEP2) {
			throw new IllegalStateException("Can not merge two path nodes with the same index!");
		}

		long id1 = PATH_OFFSET1;
		long id2 = PATH_OFFSET2;

		long offset1 = PATH_OFFSET1;
		long offset2 = PATH_OFFSET2;

		long step = Math.max(PATH_STEP1, PATH_STEP2);
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
		MerklePathNode childRoot = null;
		if (step == PATH_STEP1 && offset2 == PATH_OFFSET1) {
			// pathNode2 是 pathNode1 的子节点；
			if (pathNode1 instanceof MerklePathNode) {
				childRoot = (MerklePathNode) pathNode1;
			} else {
				childRoot = new MerklePathNode(pathNodeHash1, pathNode1, this);
			}

			index = childRoot.index(id2);

			updateChild(childRoot, index, pathNodeHash2, pathNode2);
		} else if (step == PATH_STEP2 && offset2 == PATH_OFFSET2) {
			// pathNode1 是 pathNode2 的子节点；
			if (pathNode2 instanceof MerklePathNode) {
				childRoot = (MerklePathNode) pathNode2;
			} else {
				childRoot = new MerklePathNode(pathNodeHash2, pathNode2, this);
			}

			index = childRoot.index(id1);

			updateChild(childRoot, index, pathNodeHash1, pathNode1);
		} else {
			// 数据节点不属于 pathNode 路径节点；
			// 创建共同的父节点；
			childRoot = new MerklePathNode(offset2, step, this);

			int childIndex1 = childRoot.index(id1);
			updateChild(childRoot, childIndex1, pathNodeHash1, pathNode1);

			int childIndex2 = childRoot.index(id2);
			updateChild(childRoot, childIndex2, pathNodeHash2, pathNode2);
		}

		return childRoot;
	}

	/**
	 * 合并指定的两个编号的数据项到他们共同的父节点；
	 * 
	 * @param dataNode1
	 * @param dataNode2
	 * @return
	 */
	private MerklePathNode mergeChildren(HashDigest dataNodeHash1, ValueEntry dataNode1, HashDigest dataNodeHash2,
			ValueEntry dataNode2) {
		long id1 = dataNode1.getId();
		long id2 = dataNode2.getId();
		if (id1 == id2) {
			throw new IllegalArgumentException("Cannot merge two merke data nodes with the same id!");
		}
		long offset1 = -1;
		long offset2 = -1;
		int level;

		// 查找共同的父节点；
		for (level = 0; level < MAX_LEVEL; level++) {
			offset1 = calculateOffset(id1, level);
			offset2 = calculateOffset(id2, level);
			if (offset1 == offset2) {
				break;
			}
		}
		if (offset1 == -1) {
			//
			throw new IllegalStateException(
					String.format("Cann't find the \"offset\" of common parent node!  -- id1=%s, id2=!", id1, id2));
		}
		long step = MathUtils.power(DEGREE, level);
		MerklePathNode path = new MerklePathNode(offset1, step, this);

		int childIndex1 = index(id1, offset1, step);
		int childIndex2 = index(id2, offset1, step);
		path.setChildAtIndex(childIndex1, dataNodeHash1, dataNode1);
		path.setChildAtIndex(childIndex2, dataNodeHash2, dataNode2);

		return path;
	}

	private void updateChild(MerklePathNode parent, int index, HashDigest childHash, MerkleEntry child) {
		// 检查是否有子项，如果有，则需要合并子项；
		MerkleEntry origChild = parent.getChildAtIndex(index);
		if (origChild == null) {
			parent.setChildAtIndex(index, childHash, child);
			return;
		}
		// 合并；
		HashDigest origChildHash = parent.getChildHashAtIndex(index);
		MerkleEntry newChild;
		if (origChild instanceof ValueEntry) {
			ValueEntry origData = (ValueEntry) origChild;
			if (child instanceof ValueEntry) {
				ValueEntry newData = (ValueEntry) child;
				if (origData.getId() == newData.getId()) {
					newChild = updateData(origData, newData);
				} else {
					newChild = mergeChildren(origChildHash, origData, childHash, (ValueEntry) child);
				}
			} else {
				newChild = mergeChildren(origChildHash, origData, childHash, (IndexEntry) child);
			}
		} else {
			IndexEntry origPath = (IndexEntry) origChild;
			if (child instanceof ValueEntry) {
				newChild = mergeChildren(childHash, (ValueEntry) child, origChildHash, origPath);
			} else {
				newChild = mergeChildren(childHash, (IndexEntry) child, origChildHash, origPath);
			}
		}

		parent.setChildAtIndex(index, null, newChild);
	}

	/** 
	 * 更新同一个 id 的数据节点；
	 * 
	 * <p>
	 * 这是模板方法，默认实现并不允许更新相同 id 的数据，并抛出 {@link MerkleProofException} 异常;
	 * 
	 * @param origValue 原来的数据；
	 * @param newValue  具有相同 id 的新数据；
	 * @return 更新后的新节点的数据；
	 */
	protected ValueEntry updateData(ValueEntry origValue, ValueEntry newValue) {
		throw new MerkleTreeKeyExistException("Unsupport updating datas with the same id!");
	}

	private MerkleEntry loadMerkleEntry(HashDigest nodeHash) {
		byte[] nodeBytes = loadNodeBytes(nodeHash);
		MerkleEntry merkleEntry = BinaryProtocol.decode(nodeBytes);
		if (setting.getAutoVerifyHash()) {
			verifyHash(nodeHash, nodeBytes, "Merkle hash verification fail! -- NodeHash=" + nodeHash.toBase58());
		}
		return merkleEntry;
	}
	
	
	private void verifyHash(HashDigest hash, byte[] bytes, String errMessage, Object...errorArgs) {
		if (!Crypto.getHashFunction(hash.getAlgorithm()).verify(hash, bytes)) {
			throw new MerkleProofException(String.format(errMessage, errorArgs));
		}
	}

	private IndexEntry loadMerkleIndex(HashDigest nodeHash) {
		return (IndexEntry) loadMerkleEntry(nodeHash);
	}

	private HashDigest saveData(ValueEntry data) {
		byte[] dataNodeBytes = BinaryProtocol.encode(data, ValueEntry.class);
		HashDigest dataEntryHash = DEFAULT_HASH_FUNCTION.hash(dataNodeBytes);

		saveNodeBytes(dataEntryHash, dataNodeBytes);

		return dataEntryHash;
	}

	/**
	 * 生成存储节点数据的key；
	 * 
	 * @param key 节点逻辑key；
	 * @return 节点的存储key；
	 */
	private Bytes encodeStorageKey(byte[] key) {
		return KEY_PREFIX.concat(key);
	}

	/**
	 * 生成存储节点数据的key；
	 * 
	 * @param key 节点逻辑key；
	 * @return 节点的存储key；
	 */
	private Bytes encodeStorageKey(Bytes key) {
		return KEY_PREFIX.concat(key);
	}

	/**
	 * 加载指定节点的内容，如果不存在，则抛出异常；
	 * 
	 * @param nodeHash
	 * @return
	 */
	@SuppressWarnings("unused")
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
	
	
	//----------------------------- inner types --------------------------
	
	
	
	
	public static interface MerkleEntrySelector {

		void accept(HashDigest nodeHash, MerkleEntry nodePath);

	}

	public static class NullSelector implements MerkleEntrySelector {

		public static final MerkleEntrySelector INSTANCE = new NullSelector();

		private NullSelector() {
		}

		@Override
		public void accept(HashDigest nodeHash, MerkleEntry nodePath) {
		}

	}

	public static class MerkleProofSelector implements MerkleEntrySelector {

		private List<HashDigest> paths = new ArrayList<HashDigest>();

		public MerkleProof getProof() {
			return new HashPathProof(paths);
		}

		@Override
		public void accept(HashDigest nodeHash, MerkleEntry nodePath) {
			paths.add(nodeHash);
		}

		public void addPath(HashDigest hashPath) {
			paths.add(hashPath);
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
	public static interface ValueEntry extends MerkleEntry {

		@DataField(order = 0, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG)
		long getId();

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
	 * <br>
	 * 通过 {@link #getOffset()} 和 {@link #getStep()} 表示 1 个特定的位置区间;
	 * 
	 * @author huanghaiquan
	 *
	 */
	@DataContract(code = DataCodes.MERKLE_SORTED_TREE_INDEX)
	public static interface IndexEntry extends MerkleEntry {

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
		 * 每个子项包含的数据项个数的列表；
		 * 
		 * @return
		 */
		@DataField(order = 2, primitiveType = PrimitiveType.INT64, numberEncoding = NumberEncoding.LONG, list = true)
		long[] getChildCounts();

		/**
		 * 子项的哈希的列表； <br>
		 * 
		 * 子项的个数总是固定的 {@value MerkleSortedTree#DEGREE} ;
		 * 
		 * @return
		 */
		@DataField(order = 3, primitiveType = PrimitiveType.BYTES, list = true)
		HashDigest[] getChildHashs();
	}

	/**
	 * 默克尔数据节点；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class MerkleDataNode implements ValueEntry {

		private long id;

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
		}
		
		@Override
		public long getId() {
			return id;
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
	private static class MerklePathNode implements IndexEntry {

		/**
		 * 与当前子树相邻的右侧兄弟子树的偏移量；
		 */
		private final long NEXT_OFFSET;
		
		private MerkleSortedTree tree;

		private HashDigest nodeHash;

		private long offset;

		private long step;

		private long[] childCounts;

		private HashDigest[] origChildHashs;

		private HashDigest[] childHashs;

		private MerkleEntry[] children;

		private boolean modified;

		protected MerklePathNode(long offset, long step, MerkleSortedTree tree) {
			this(null, offset, step, new long[tree.DEGREE], new HashDigest[tree.DEGREE], tree);
		}

		protected MerklePathNode(HashDigest nodeHash, IndexEntry index, MerkleSortedTree tree) {
			this(nodeHash, index.getOffset(), index.getStep(), index.getChildCounts(), index.getChildHashs(), tree);
		}

		private MerklePathNode(HashDigest nodeHash, long offset, long step, long[] childCounts,
				HashDigest[] childHashs, MerkleSortedTree tree) {
			assert step > 0;
			this.tree = tree;
			NEXT_OFFSET = tree.nextOffset(offset, step);

			this.nodeHash = nodeHash;
			this.modified = (nodeHash == null);

			this.offset = offset;
			this.step = step;
			this.childCounts = childCounts;
			this.childHashs = childHashs;
			this.origChildHashs = childHashs.clone();
			this.children = new MerkleEntry[tree.DEGREE];

			assert childHashs.length == tree.DEGREE;
		}

		public HashDigest getNodeHash() {
			return nodeHash;
		}

		public boolean isModified() {
			return modified;
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
		public long[] getChildCounts() {
			return childCounts;
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

		public HashDigest getChildHashAtIndex(int index) {
			return childHashs[index];
		}

		public MerkleEntry getChildAtIndex(int index) {
			MerkleEntry child = children[index];
			if (child != null) {
				return child;
			}
			HashDigest childHash = childHashs[index];
			if (childHash == null) {
				return null;
			}
			child = tree.loadMerkleEntry(childHash);
			children[index] = child;
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
		 *         如果指定编号不属于该子树，则返回值大于等于 0 且小于 {@value MerkleSortedTree#DEGREE}; <br>
		 *         如果指定编号不属于该子树，则返回 -1；
		 */
		@SuppressWarnings("unused")
		public int setData(long id, byte[] dataBytes) {
			int index = index(id);
			if (index < 0) {
				return index;
			}

			ValueEntry newData = new MerkleDataNode(id, dataBytes);

			HashDigest childHash = childHashs[index];
			MerkleEntry child = children[index];

			if (child == null) {
				if (childHash == null) {
					// 完全没有子树时，直接附加数据节点；
					// 当新节点在此子树中没有其它兄弟节点时，不建立从当前节点到叶子节点之间完整的路径节点，目的是缩减空间，优化处理少量数据节点的情形；
					setChildAtIndex(index, null, newData);
					return index;
				}

				child = tree.loadMerkleEntry(childHash);
			}

			IndexEntry newChild;
			if (child instanceof ValueEntry) {
				// 已经有子节点存在，检查 id 是否冲突，如果不冲突，则合并两个数据节点到同样一棵子树；
				ValueEntry childData = (ValueEntry) child;
				if (id == childData.getId()) {
					// TODO: 出现 id 冲突；同一个 id 不能设置两次；
					throw new MerkleProofException("The data entry with the same id[" + id + "] already exist!");
				}

				newChild = tree.mergeChildren(null, newData, childHash, childData);
			} else {
				// 已经有子树存在，检查要加入的节点属于该子树，还是与其是兄弟子树，合并这两个节点；
				IndexEntry merkleIndex = (IndexEntry) child;

				newChild = tree.mergeChildren(null, newData, childHash, merkleIndex);
			}

			setChildAtIndex(index, null, newChild);
			return index;
		}

		/**
		 * 设置子节点；
		 * 
		 * @param index     子节点的位置；
		 * @param childHash 子节点的哈希；如果为 null，则在 commit 时计算哈希；
		 * @param child     子节点；
		 */
		private void setChildAtIndex(int index, HashDigest childHash, MerkleEntry child) {
			if (child instanceof IndexEntry) {
				IndexEntry childPath = (IndexEntry) child;
				if (childPath.getStep() >= step || childPath.getOffset() < offset
						|| childPath.getOffset() >= NEXT_OFFSET) {
					throw new IllegalArgumentException("The specified child not belong to this node!");
				}
			}

			childHashs[index] = childHash;
			children[index] = child;
			modified = true;
		}

		public HashDigest commit() {
			if (!modified) {
				return nodeHash;
			}

			// save the modified childNodes;
			for (int i = 0; i < tree.DEGREE; i++) {
				if (children[i] != null) {
					MerkleEntry child = children[i];
					// 需要先保存子节点，获得子节点的哈希；
					if (child instanceof MerklePathNode) {
						childHashs[i] = ((MerklePathNode) child).commit();
						childCounts[i] = count((MerklePathNode) child);
					} else if (child instanceof ValueEntry) {
						// 保存新创建的子节点；
						if (childHashs[i] == null) {
							ValueEntry dataChild = (ValueEntry) child;
							childHashs[i] = tree.saveData(dataChild);
						}
						childCounts[i] = 1;
					} else {
						childCounts[i] = count((IndexEntry) child);

//						// 注：上下文逻辑应确保不可能进入此分支，即一个新加入的尚未生成哈希的子节点，却不是 MerklePathNode 实例；
//						// 对于附加已存在的节点的情况，已存在的节点已经生成子节点哈希，并且其实例是 MerkleIndex 的动态代理；
//						throw new IllegalStateException(
//								"Illegal child node which has no hash and is not instance of MerklePathNode!");
					}
				}
			}

			// save;
			byte[] nodeBytes = BinaryProtocol.encode(this, IndexEntry.class);
			HashDigest hash = tree.DEFAULT_HASH_FUNCTION.hash(nodeBytes);
			tree.saveNodeBytes(hash, nodeBytes);

			// update hash;
			for (int i = 0; i < tree.DEGREE; i++) {
				origChildHashs[i] = childHashs[i];
			}
			this.nodeHash = hash;
			this.modified = false;

			return hash;
		}

		public void cancel() {
			MerkleEntry child;
			for (int i = 0; i < tree.DEGREE; i++) {
				if (childHashs[i] == null || origChildHashs[i] == null || (!childHashs[i].equals(origChildHashs[i]))) {
					child = children[i];
					children[i] = null;
					// 清理字节点以便优化大对象的垃圾回收效率；
					if (child != null && child instanceof MerklePathNode) {
						((MerklePathNode) child).cancel();
					}
				}
				childHashs[i] = origChildHashs[i];
			}
			// 注：不需要处理 nodeHash 的回滚，因为 nodeHash 是 commit 操作的最后确认标志；
		}

	}

	/**
	 * 数据迭代器；
	 * <p>
	 * 注：未考虑迭代过程中新写入数据引起的变化；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private class MerklePathIterator implements SkippingIterator<ValueEntry> {

		private final long totalCount;

		@SuppressWarnings("unused")
		private final long offset;

		@SuppressWarnings("unused")
		private final long step;

		// 子节点的游标边界；
		private long[] childCounts;

		private HashDigest[] childHashs;

		private int childIndex;

		private long cursor = -1;

		private SkippingIterator<ValueEntry> childIterator;

		public MerklePathIterator(long offset, long step, HashDigest[] childHashs, long[] childCounts) {
			this.offset = offset;
			this.step = step;
			this.childHashs = childHashs;
			this.childCounts = childCounts;
			// 使用此方法的上下文逻辑已经能够约束每一项的数字大小范围，不需要考虑溢出；
			this.totalCount = ArrayUtils.sum(childCounts);
		}

		@Override
		public long getTotalCount() {
			return totalCount;
		}

		@Override
		public boolean hasNext() {
			return cursor + 1 < totalCount;
		}

		@Override
		public long skip(long count) {
			if (count < 0) {
				throw new IllegalArgumentException("The specified count is out of bound!");
			}
			if (count == 0) {
				return 0;
			}
			if (childIndex >= DEGREE) {
				return 0;
			}

			long s = ArrayUtils.sum(childCounts, 0, childIndex + 1);
			long skipped;// 实际可略过的数量；
			long currLeft = s - cursor - 1;
			if (count < currLeft) {
				// 实际略过的数量在 index 指示的当前子节点的范围内；
				if (childIterator == null) {
					childIterator = createChildIterator(childIndex);
				}
				skipped = count;
				long sk = childIterator.skip(skipped);
				assert sk == skipped;
			} else {
				// 已经超过 index 指示的当前子节点的剩余数量，直接忽略当前子节点；
				childIterator = null;
				skipped = currLeft;
				childIndex++;
				while (childIndex < DEGREE && skipped + childCounts[childIndex] <= count) {
					skipped += childCounts[childIndex];
					childIndex++;
				}
				if (childIndex < DEGREE) {
					// 未超出子节点的范围；
					long c = count - skipped;
					childIterator = createChildIterator(childIndex);
					long sk = childIterator.skip(c);
					assert sk == c;

					skipped = count;
				}
			}
			cursor = cursor + skipped;
			return skipped;
		}

		private SkippingIterator<ValueEntry> createChildIterator(int childIndex) {
			HashDigest childHash = childHashs[childIndex];
			if (childHash == null) {
				// 正常情况下不应该进入此逻辑分支，因为空的子节点的数量表为 0，迭代器的迭代处理逻辑理应过滤掉此位置的子节点；
				throw new IllegalStateException();
			}
			MerkleEntry child = loadMerkleEntry(childHash);

			if (child instanceof ValueEntry) {
				return new MerkleDataIteratorWrapper((ValueEntry) child);
			}
			IndexEntry indexNode = (IndexEntry) child;
			return new MerklePathIterator(indexNode.getOffset(), indexNode.getStep(), indexNode.getChildHashs(),
					indexNode.getChildCounts());
		}

		@Override
		public ValueEntry next() {
			if (!hasNext()) {
				return null;
			}

			long s = ArrayUtils.sum(childCounts, 0, childIndex + 1);

			while (cursor + 1 >= s && childIndex < DEGREE - 1) {
				childIndex++;
				childIterator = null;
				s += childCounts[childIndex];
			}

			if (childIterator == null) {
				childIterator = createChildIterator(childIndex);
			}
			cursor++;
			return childIterator.next();
		}

		@Override
		public long getCursor() {
			return cursor;
		}

	}

	private static class MerkleDataIteratorWrapper extends AbstractSkippingIterator<ValueEntry> {

		private ValueEntry data;

		public MerkleDataIteratorWrapper(ValueEntry data) {
			this.data = data;
		}

		@Override
		public long getTotalCount() {
			return 1;
		}

		@Override
		public ValueEntry next() {
			if (hasNext()) {
				cursor++;
				return data;
			}
			return null;
		}

	}
}

package com.jd.blockchain.ledger.merkletree;

import java.util.ArrayList;
import java.util.List;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.MerkleProof;
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
 * 默克尔排序树；
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
 * @param <T> 默克尔树的数据节点的类型；
 */
public class MerkleSortTree<T> implements Transactional {

	public static final BytesConverter<byte[]> BYTES_TO_BYTES_CONVERTER = new BytesToBytesConverter();

	@SuppressWarnings("rawtypes")
	private static final DataPolicy DEFAULT_DATA_POLICY = new DefaultDataPolicy<>();

	@SuppressWarnings("unchecked")
	private static <T> DataPolicy<T> defaultDataPolicy() {
		return (DataPolicy<T>) DEFAULT_DATA_POLICY;
	}

	public static final int DEFAULT_DEGREE = TreeDegree.D3.DEGREEE;

	public static final int DEFAULT_MAX_LEVEL = TreeDegree.D3.MAX_DEPTH;

	public static final long DEFAULT_MAX_COUNT = TreeDegree.D3.MAX_COUNT;

	protected final int DEGREE;

	protected final int MAX_LEVEL;

	// 正好是 2 的 60 次方，足以覆盖 long 类型的正整数，且为避免溢出预留了区间；
	protected final long MAX_COUNT;

	protected final BytesConverter<T> CONVERTER;

	protected final HashFunction DEFAULT_HASH_FUNCTION;

	protected final TreeOptions OPTIONS;

	private final Bytes KEY_PREFIX;

	private final DataPolicy<T> DATA_POLICY;

	private final ExPolicyKVStorage KV_STORAGE;

	private MerkleNode root;

	private Long maxId;

	/**
	 * 构建空的树；
	 * 
	 * @param setting   密码参数；
	 * @param keyPrefix 键的前缀；
	 * @param kvStorage 节点的存储；
	 * @param converter 数据的转换器；
	 */
	public MerkleSortTree(TreeOptions options, String keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter) {
		this(TreeDegree.D3, options, Bytes.fromString(keyPrefix), kvStorage, converter);
	}

	/**
	 * 构建空的树；
	 * 
	 * @param setting   密码参数；
	 * @param keyPrefix 键的前缀；
	 * @param kvStorage 节点的存储；
	 * @param converter 数据的转换器；
	 */
	public MerkleSortTree(TreeOptions options, String keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter, DataPolicy<T> dataPolicy) {
		this(TreeDegree.D3, options, Bytes.fromString(keyPrefix), kvStorage, converter, dataPolicy);
	}

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortTree(TreeDegree degree, TreeOptions options, String keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter) {
		this(degree, options, Bytes.fromString(keyPrefix), kvStorage, converter);
	}

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortTree(TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter) {
		this(TreeDegree.D3, options, keyPrefix, kvStorage, converter);
	}

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortTree(TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter, DataPolicy<T> dataPolicy) {
		this(TreeDegree.D3, options, keyPrefix, kvStorage, converter, dataPolicy);
	}

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortTree(TreeDegree degree, TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter) {
		this(degree, options, keyPrefix, kvStorage, converter, defaultDataPolicy());
	}

	/**
	 * 构建空的树；
	 * 
	 * @param kvStorage
	 */
	public MerkleSortTree(TreeDegree degree, TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter, DataPolicy<T> dataPolicy) {
		this.DEGREE = degree.DEGREEE;
		this.MAX_LEVEL = degree.MAX_DEPTH;
		this.MAX_COUNT = MathUtils.power(DEGREE, MAX_LEVEL);
		this.CONVERTER = converter;

		this.OPTIONS = options;
		this.KEY_PREFIX = keyPrefix;
		this.KV_STORAGE = kvStorage;
		this.DEFAULT_HASH_FUNCTION = Crypto.getHashFunction(options.getDefaultHashAlgorithm());

		this.DATA_POLICY = dataPolicy;

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
	public MerkleSortTree(HashDigest rootHash, TreeOptions options, String keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter) {
		this(rootHash, options, Bytes.fromString(keyPrefix), kvStorage, converter);
	}

	/**
	 * 创建 Merkle 树；
	 * 
	 * @param rootHash     节点的根Hash; 如果指定为 null，则实际上创建一个空的 Merkle Tree；
	 * @param verifyOnLoad 从外部存储加载节点时是否校验节点的哈希；
	 * @param kvStorage    保存 Merkle 节点的存储服务；
	 * @param readonly     是否只读；
	 */
	public MerkleSortTree(HashDigest rootHash, TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter) {
		this(rootHash, options, keyPrefix, kvStorage, converter, defaultDataPolicy());
	}

	/**
	 * 创建 Merkle 树；
	 * 
	 * @param rootHash   节点的根Hash; 如果指定为 null，则实际上创建一个空的 Merkle Tree；
	 * @param options    树的配置选项；
	 * @param keyPrefix  存储数据的 key 的前缀；
	 * @param kvStorage  保存 Merkle 节点的存储服务；
	 * @param converter  默克尔树的转换器；
	 * @param dataPolicy 数据节点的处理策略；
	 */
	public MerkleSortTree(HashDigest rootHash, TreeOptions options, Bytes keyPrefix, ExPolicyKVStorage kvStorage,
			BytesConverter<T> converter, DataPolicy<T> dataPolicy) {
		this.KEY_PREFIX = keyPrefix;
		this.OPTIONS = options;
		this.KV_STORAGE = kvStorage;
		this.DEFAULT_HASH_FUNCTION = Crypto.getHashFunction(options.getDefaultHashAlgorithm());
		this.CONVERTER = converter;

		this.DATA_POLICY = dataPolicy;

		MerkleIndex merkleIndex = loadMerkleEntry(rootHash);
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

		this.root = new PathNode(rootHash, merkleIndex, this);

		refreshMaxId();
	}

	/**
	 * 创建数据类型为字节数组的空的默克尔排序树；
	 * 
	 * @param setting
	 * @param keyPrefix
	 * @param kvStorage
	 * @return
	 */
	public static MerkleSortTree<byte[]> createBytesTree(TreeOptions options, String keyPrefix,
			ExPolicyKVStorage kvStorage) {
		return new MerkleSortTree<byte[]>(options, keyPrefix, kvStorage, BYTES_TO_BYTES_CONVERTER);
	}

	/**
	 * 创建数据类型为字节数组的空的默克尔排序树；
	 * 
	 * @param setting
	 * @param keyPrefix
	 * @param kvStorage
	 * @return
	 */
	public static MerkleSortTree<byte[]> createBytesTree(TreeOptions options, String keyPrefix,
			ExPolicyKVStorage kvStorage, DataPolicy<byte[]> dataPolicy) {
		return new MerkleSortTree<byte[]>(options, keyPrefix, kvStorage, BYTES_TO_BYTES_CONVERTER, dataPolicy);
	}

	/**
	 * 创建数据类型为字节数组的空的默克尔排序树；
	 * 
	 * @param setting
	 * @param keyPrefix
	 * @param kvStorage
	 * @return
	 */
	public static MerkleSortTree<byte[]> createBytesTree(TreeOptions options, Bytes keyPrefix,
			ExPolicyKVStorage kvStorage) {
		return new MerkleSortTree<byte[]>(options, keyPrefix, kvStorage, BYTES_TO_BYTES_CONVERTER);
	}

	/**
	 * 创建数据类型为字节数组的空的默克尔排序树；
	 * 
	 * @param setting
	 * @param keyPrefix
	 * @param kvStorage
	 * @return
	 */
	public static MerkleSortTree<byte[]> createBytesTree(TreeDegree degree, TreeOptions options, String keyPrefix,
			ExPolicyKVStorage kvStorage) {
		return new MerkleSortTree<byte[]>(degree, options, keyPrefix, kvStorage, BYTES_TO_BYTES_CONVERTER);
	}

	/**
	 * 创建数据类型为字节数组的空的默克尔排序树；
	 * 
	 * @param setting
	 * @param keyPrefix
	 * @param kvStorage
	 * @return
	 */
	public static MerkleSortTree<byte[]> createBytesTree(TreeDegree degree, TreeOptions options, Bytes keyPrefix,
			ExPolicyKVStorage kvStorage) {
		return new MerkleSortTree<byte[]>(degree, options, keyPrefix, kvStorage, BYTES_TO_BYTES_CONVERTER);
	}

	/**
	 * 以指定哈希的节点为根节点创建数据类型为字节数组的默克尔排序树；
	 * 
	 * @param setting
	 * @param keyPrefix
	 * @param kvStorage
	 * @return
	 */
	public static MerkleSortTree<byte[]> createBytesTree(HashDigest rootHash, TreeOptions options, String keyPrefix,
			ExPolicyKVStorage kvStorage) {
		return new MerkleSortTree<byte[]>(rootHash, options, keyPrefix, kvStorage, BYTES_TO_BYTES_CONVERTER);
	}

	/**
	 * 以指定哈希的节点为根节点创建数据类型为字节数组的默克尔排序树；
	 * 
	 * @param setting
	 * @param keyPrefix
	 * @param kvStorage
	 * @return
	 */
	public static MerkleSortTree<byte[]> createBytesTree(HashDigest rootHash, TreeOptions options, Bytes keyPrefix,
			ExPolicyKVStorage kvStorage) {
		return new MerkleSortTree<byte[]>(rootHash, options, keyPrefix, kvStorage, BYTES_TO_BYTES_CONVERTER);
	}

	/**
	 * 根哈希；
	 * <p>
	 * 
	 * 注：在未提交写入的新数据之前，不更新根哈希；
	 * 
	 * @return
	 */
	public HashDigest getRootHash() {
		return root.getNodeHash();
	}

	/**
	 * 数据节点的总数；
	 * 
	 * <br>
	 * 
	 * 注：注：在未提交写入的新数据之前，不更节点总数；
	 * 
	 * @return
	 */
	public long getCount() {
		return count(root);
	}

	/**
	 * 设置指定 id 的数据；
	 * 
	 * @param id   数据的编号；
	 * @param data 数据；
	 * @return true 表示数据已设置； false 表示数据未设置；当内部的数据写入策略有可能阻止设置数据，此时将返回 false；
	 */
	public boolean set(long id, T data) {
		checkId(id);
		MerkleNode newRoot = mergeChildren(root.getNodeHash(), root, id, data);
		if (newRoot == null) {
			return false;
		}
		root = newRoot;
		// 更新 maxId；
		if (maxId == null || id > maxId.longValue()) {
			maxId = Long.valueOf(id);
		}
		return true;
	}

	/**
	 * 最大的编码；<br>
	 * 如果默克尔没有数据，则返回 null；
	 * <p>
	 * 
	 * 该属性会实时反映默克尔树的状态，包括未提交的状态，有新的数据写入之后立即更新该属性；<br>
	 * 
	 * 如果未提交的更改已经取消，该属性也同时恢复到上一次提交后的结果；
	 * 
	 * @return
	 */
	public Long getMaxId() {
		return maxId;
	}

	public T get(long id) {
		return seekData(root, id, NullSelector.INSTANCE);
	}

	/**
	 * 包含所有已提交的数据对象的迭代器；
	 * 
	 * @return
	 */
	public SkippingIterator<MerkleValue<T>> iterator() {
		// 克隆根节点的数据，避免根节点的更新影响了迭代器；
		return new MerklePathIterator<T>(root.getOffset(), root.getStep(), root.getOrigChildHashs(),
				root.getChildCounts().clone(), DATA_POLICY, CONVERTER, this);
	}

	/**
	 * 包含所有已提交的数据字节的迭代器；<br>
	 * 
	 * @return
	 */
	public SkippingIterator<MerkleValue<byte[]>> bytesIterator() {
		// 克隆根节点的数据，避免根节点的更新影响了迭代器；
		return new MerklePathIterator<byte[]>(root.getOffset(), root.getStep(), root.getOrigChildHashs(),
				root.getChildCounts().clone(), defaultDataPolicy(), BYTES_TO_BYTES_CONVERTER, this);
	}

	/**
	 * 返回指定编码数据的默克尔证明；
	 * 
	 * @param id
	 * @return
	 */
	public MerkleProof getProof(long id) {
		MerkleProofSelector proofSelector = new MerkleProofSelector();
		T data = seekData(id, proofSelector);
		if (data == null) {
			return null;
		}
		return proofSelector.getProof();
	}

	/**
	 * 从默克尔树的根开始，搜索指定 id 的数据，并记录搜索经过的节点的哈希；如果数据不存在，则返回 null；
	 * 
	 * @param id           要查找的数据的 id；
	 * @param pathSelector
	 * @return
	 */
	public T seekData(long id, MerkleEntrySelector pathSelector) {
		pathSelector.accept(root.getNodeHash(), root);
		return seekData(root, id, pathSelector);
	}

	/**
	 * 检查指定的 id 是否在合法范围；
	 * 
	 * @param id
	 */
	private void checkId(long id) {
		if (id < 0) {
			throw new IllegalArgumentException("'id' is negative!");
		}
		if (id >= MAX_COUNT) {
			throw new IllegalArgumentException("'id' is greater than or equal to the MAX_COUNT[" + MAX_COUNT + "]!");
		}
	}

	private Long refreshMaxId() {
		if (getCount() == 0) {
			// 空的默克尔树；
			maxId = null;
		} else {
			// 默克尔树不为空 ；
			SkippingIterator<MerkleValue<T>> itr = iterator();
			itr.skip(itr.getTotalCount() - 1);
			MerkleValue<T> value = itr.next();
			maxId = Long.valueOf(value.getId());
		}
		return maxId;
	}

	/**
	 * 创建顶级根节点；
	 * 
	 * @return
	 */
	protected PathNode createTopRoot() {
		long step = MAX_COUNT / DEGREE;
		return new PathNode(0, step, this);
	}

	/**
	 * 从指定的默克尔索引开始，搜索指定 id 的数据，并记录搜索经过的节点的哈希；如果数据不存在，则返回 null；
	 * 
	 * @param merkleIndex  开始搜索的默克尔索引节点；
	 * @param id           要查找的数据的 id；
	 * @param pathSelector 路径选择器，记录搜索过程经过的默克尔节点；
	 * @return 搜索到的指定 ID 的数据；
	 */
	private T seekData(MerkleIndex merkleIndex, long id, MerkleEntrySelector pathSelector) {
		int idx = index(id, merkleIndex);
		if (idx < 0) {
			return null;
		}
		if (merkleIndex.getStep() > 1) {
			MerkleIndex child;
			if (merkleIndex instanceof PathNode) {
				PathNode path = (PathNode) merkleIndex;
				child = path.getChild(idx);
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

			return seekData((MerkleIndex) child, id, pathSelector);
		}
		// leaf node;
		T child;
		if (merkleIndex instanceof LeafNode) {
			@SuppressWarnings("unchecked")
			LeafNode<T> path = (LeafNode<T>) merkleIndex;
			child = path.getChild(idx);
		} else {
			HashDigest[] childHashs = merkleIndex.getChildHashs();
			HashDigest childHash = childHashs[idx];
			if (childHash == null) {
				return null;
			}

			child = CONVERTER.fromBytes(loadNodeBytes(childHash));
		}
		return child;
	}

	/**
	 * 是否发生了变更；
	 * <p>
	 * 
	 * 自上一次调用 commit() 或 cancel() 之后，对象的状态如果发生了变更，则返回 true，否则返回 false；
	 * 
	 * <p>
	 * 
	 * 对于新创建的空的 {@link MerkleSortTree} 实例，此属性返回 true；
	 */
	@Override
	public boolean isUpdated() {
		if (root == null) {
			return true;
		}
		if (root instanceof PathNode) {
			return ((PathNode) root).isModified();
		}
		return false;
	}

	@Override
	public final void commit() {
		root.commit();
	}

	@Override
	public final void cancel() {
		root.cancel();
		refreshMaxId();
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
	 * 计算指定 id 在 {@link MerkleIndex} 中的偏移位置；<br>
	 * 如果 id 不属于 {@link MerkleIndex} 的地址区间，则返回 -1；
	 * 
	 * @param id          编号；
	 * @param merkleIndex 默克尔索引，表示1个特定的位置区间；
	 * @return
	 */
	private int index(long id, MerkleIndex merkleIndex) {
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

	private static long count(MerkleIndex merkleIndex) {
		long[] childCounts = merkleIndex.getChildCounts();
		// 使用此方法的上下文逻辑已经能够约束每一项的数字大小范围，不需要考虑溢出；
		return ArrayUtils.sum(childCounts);
	}

	/**
	 * 合并指定的索引节点和数据节点；
	 * 
	 * @param indexNodeHash 索引节点的哈希；
	 * @param indexNode     索引节点；
	 * @param dataId        数据节点的 id；
	 * @param data          数据；
	 * @return 返回共同的父节点；如果未更新数据，则返回 null；
	 */
	@SuppressWarnings("unchecked")
	private MerkleNode mergeChildren(HashDigest indexNodeHash, MerkleIndex indexNode, long dataId, T data) {
		final long PATH_OFFSET = indexNode.getOffset();
		final long PATH_STEP = indexNode.getStep();

		long pathId = PATH_OFFSET;

		long dataOffset = calculateOffset(dataId, PATH_STEP);
		long pathOffset = PATH_OFFSET;
		long step = PATH_STEP;
		while (dataOffset != pathOffset) {
			step = upStep(step);
			if (step >= MAX_COUNT) {
				throw new IllegalStateException("The 'step' overflows!");
			}
			dataOffset = calculateOffset(dataId, step);
			pathOffset = calculateOffset(pathId, step);
		}

		// 判断参数指定的索引节点和数据节点是否具有从属关系，还是并列关系；
		if (step == PATH_STEP && pathOffset == PATH_OFFSET) {
			// 数据节点属于 pathNode 路径节点；
			// 把数据节点合并到 pathNode 路径节点；
			int index = index(dataId, pathOffset, step);
			if (PATH_STEP > 1) {
				PathNode parentNode;
				if (indexNode instanceof PathNode) {
					parentNode = (PathNode) indexNode;
				} else {
					parentNode = new PathNode(indexNodeHash, indexNode, this);
				}

				boolean ok = updateChildAtIndex(parentNode, index, dataId, data);

				return ok ? parentNode : null;
			} else {
				// while PATH_STEP == 1, this index node is leaf;
				LeafNode<T> parentNode;
				if (indexNode instanceof LeafNode) {
					parentNode = (LeafNode<T>) indexNode;
				} else {
					parentNode = new LeafNode<T>(indexNodeHash, indexNode, this);
				}
				boolean ok = updateChildAtIndex(parentNode, index, dataId, data);
				return ok ? parentNode : null;
			}
		} else {
			// 数据节点不从属于 pathNode 路径节点，它们有共同的父节点；
			// 创建共同的父节点；
			PathNode parentPathNode = new PathNode(pathOffset, step, this);

			int dataChildIndex = parentPathNode.index(dataId);
			boolean ok = updateChildAtIndex(parentPathNode, dataChildIndex, dataId, data);
			if (!ok) {
				return null;
			}

			int pathChildIndex = parentPathNode.index(pathId);
			updateChildAtIndex(parentPathNode, pathChildIndex, indexNodeHash, indexNode);

			return parentPathNode;
		}
	}

	/**
	 * 设置指定位置的子节点为指定的数据节点；
	 * 
	 * @param parentNode 作为父节点的默克尔索引节点；
	 * @param index      要设置的数据节点在父节点中的位置；
	 * @param dataId     要设置的数据节点的 id；
	 * @param data       要设置的字节数据；
	 * @return true 表示更新成功； false 表示未更新；
	 */
	private boolean updateChildAtIndex(LeafNode<T> parentNode, int index, long dataId, T data) {
		T origChild = parentNode.getChild(index);

		T newChild = DATA_POLICY.updateData(dataId, origChild, data);
		if (newChild != null) {
			parentNode.setChild(index, null, newChild);
			return true;
		}
		return false;
	}

	/**
	 * 设置指定位置的子节点为指定的子索引节点；
	 * 
	 * @param parentNode 作为父节点的默克尔索引节点；
	 * @param index      要设置的子索引节点在父节点中的位置；
	 * @param childHash  子索引节点的哈希；
	 * @param childEntry 子索引节点；
	 * @return
	 */
	private void updateChildAtIndex(PathNode parentNode, int index, HashDigest childHash, MerkleIndex childEntry) {
		MerkleIndex origChild = parentNode.getChild(index);
		if (origChild == null) {
			parentNode.setChild(index, childHash, childEntry);
			return;
		}

		// 合并两个子节点；
		HashDigest origChildHash = parentNode.getChildHashAtIndex(index);
		PathNode newChild = mergeChildren(origChildHash, origChild, childHash, childEntry);
		parentNode.setChild(index, null, newChild);
	}

	/**
	 * 设置指定位置的子节点为指定的数据节点；
	 * 
	 * @param parentNode 作为父节点的默克尔索引节点；
	 * @param index      要设置的数据节点在父节点中的位置；
	 * @param dataId     要设置的数据节点的 id；
	 * @param dataBytes  要设置的字节数据；
	 * @return
	 */
	private boolean updateChildAtIndex(PathNode parentNode, int index, long dataId, T dataBytes) {
		MerkleIndex origChild = parentNode.getChild(index);
		if (origChild == null) {
			long offset = calculateOffset(dataId, 1L);
			LeafNode<T> leafNode = new LeafNode<T>(offset, this);
			boolean ok = updateChildAtIndex(leafNode, leafNode.index(dataId), dataId, dataBytes);
			if (ok) {
				parentNode.setChild(index, null, leafNode);
			}

			return ok;
		}

		// 合并两个子节点；
		HashDigest origChildHash = parentNode.getChildHashAtIndex(index);
		MerkleIndex newChild = mergeChildren(origChildHash, origChild, dataId, dataBytes);
		if (newChild == null) {
			return false;
		}
		parentNode.setChild(index, null, newChild);
		return true;
	}

	/**
	 * 合并子节点，返回共同的父节点；
	 * 
	 * @param pathNode1     数据项；
	 * @param pathNodeHash2 路径节点的哈希；
	 * @param pathNode2     路径节点；
	 * @return
	 */
	private PathNode mergeChildren(HashDigest pathNodeHash1, MerkleIndex pathNode1, HashDigest pathNodeHash2,
			MerkleIndex pathNode2) {
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

		// 判断参数指定的两个索引节点是否具有从属关系，还是并列关系；
		if (step == PATH_STEP1 && offset2 == PATH_OFFSET1) {
			// pathNode1 是父节点，pathNode2 是子节点；
			PathNode parentNode;
			if (pathNode1 instanceof PathNode) {
				parentNode = (PathNode) pathNode1;
			} else {
				parentNode = new PathNode(pathNodeHash1, pathNode1, this);
			}
			int index = parentNode.index(id2);
			updateChildAtIndex(parentNode, index, pathNodeHash2, pathNode2);

			return parentNode;
		} else if (step == PATH_STEP2 && offset2 == PATH_OFFSET2) {
			// pathNode2 是父节点，pathNode1 是子节点；
			PathNode parentNode;
			if (pathNode2 instanceof PathNode) {
				parentNode = (PathNode) pathNode2;
			} else {
				parentNode = new PathNode(pathNodeHash2, pathNode2, this);
			}
			int index = parentNode.index(id1);
			updateChildAtIndex(parentNode, index, pathNodeHash1, pathNode1);

			return parentNode;
		} else {
			// 数据节点不属于 pathNode 路径节点；
			// 创建共同的父节点；
			PathNode parentNode = new PathNode(offset2, step, this);

			int childIndex1 = parentNode.index(id1);
			updateChildAtIndex(parentNode, childIndex1, pathNodeHash1, pathNode1);

			int childIndex2 = parentNode.index(id2);
			updateChildAtIndex(parentNode, childIndex2, pathNodeHash2, pathNode2);

			return parentNode;
		}
	}

	/**
	 * 加载指定哈希的默克尔节点的字节；
	 * 
	 * @param nodeHash
	 * @return
	 */
	private byte[] loadNodeBytes(HashDigest nodeHash) {
		byte[] nodeBytes = loadBytes(nodeHash);
		if (OPTIONS.isVerifyHashOnLoad()) {
			verifyHash(nodeHash, nodeBytes, "Merkle hash verification fail! -- NodeHash=" + nodeHash.toBase58());
		}
		return nodeBytes;
	}

	private HashDigest saveNodeBytes(byte[] nodeBytes, boolean reportKeyStorageConfliction) {
		HashDigest nodeHash = DEFAULT_HASH_FUNCTION.hash(nodeBytes);
		saveBytes(nodeHash, nodeBytes, reportKeyStorageConfliction);
		return nodeHash;
	}

	private MerkleIndex loadMerkleEntry(HashDigest nodeHash) {
		byte[] nodeBytes = loadNodeBytes(nodeHash);

		MerkleIndex merkleEntry = BinaryProtocol.decode(nodeBytes);
		return merkleEntry;
	}

	private void verifyHash(HashDigest hash, byte[] bytes, String errMessage, Object... errorArgs) {
		if (!Crypto.getHashFunction(hash.getAlgorithm()).verify(hash, bytes)) {
			throw new MerkleProofException(String.format(errMessage, errorArgs));
		}
	}

	private HashDigest saveIndex(MerkleIndex indexEntry) {
		byte[] nodeBytes = BinaryProtocol.encode(indexEntry, MerkleIndex.class);
		return saveNodeBytes(nodeBytes, true);
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
	private byte[] loadBytes(byte[] key) {
		Bytes storageKey = encodeStorageKey(key);
		byte[] nodeBytes = KV_STORAGE.get(storageKey);
		if (nodeBytes == null) {
			throw new MerkleProofException("Merkle node does not exist! -- key=" + storageKey.toBase58());
		}
		return nodeBytes;
	}

	private byte[] loadBytes(Bytes key) {
		Bytes storageKey = encodeStorageKey(key);
		byte[] nodeBytes = KV_STORAGE.get(storageKey);
		if (nodeBytes == null) {
			throw new MerkleProofException("Merkle node does not exist! -- key=" + storageKey.toBase58());
		}
		return nodeBytes;
	}

	private void saveBytes(Bytes key, byte[] nodeBytes, boolean reportKeyStorageConfliction) {
		Bytes storageKey = encodeStorageKey(key);
		boolean success = KV_STORAGE.set(storageKey, nodeBytes, ExPolicy.NOT_EXISTING);
		if ((!success) && reportKeyStorageConfliction) {
			throw new MerkleKeyStorageConflictException("Merkle node already exist! -- key=" + storageKey.toBase58());
		}
	}

	// ----------------------------- inner types --------------------------

	/**
	 * 默认的数据策略；
	 * 
	 * @author huanghaiquan
	 *
	 * @param <T>
	 */
	public static class DefaultDataPolicy<T> implements DataPolicy<T> {

		/**
		 * 更新指定 id 的数据节点；
		 * 
		 * <p>
		 * 默认实现并不允许更新相同 id 的数据，并抛出 {@link MerkleProofException} 异常;
		 * 
		 * @param origValue 原数据；如果为 null，则表明是新增数据；
		 * @param newValue  新数据；
		 * @return 更新后的新节点的数据； 如果返回 null，则忽略此次操作；
		 */
		@Override
		public T updateData(long id, T origData, T newData) {
			if (origData != null) {
				throw new MerkleTreeKeyExistException("Unsupport updating datas with the same id!");
			}
			return newData;
		}

		/**
		 * 准备提交指定 id 的数据，保存至存储服务；<br>
		 * 
		 * 此方法在对指定数据节点进行序列化并进行哈希计算之前被调用；
		 * 
		 * @param id
		 * @param data
		 */
		@Override
		public T beforeCommitting(long id, T data) {
			return data;
		}

		@Override
		public long count(long id, T data) {
			return 1;
		}

		/**
		 * 已经取消指定 id 的数据；
		 * 
		 * @param id
		 * @param child
		 */
		@Override
		public void afterCanceled(long id, T data) {
		}

		@Override
		public SkippingIterator<MerkleValue<T>> iterator(long id, byte[] bytesData, long count,
				BytesConverter<T> converter) {
			return new MerkleDataIteratorWrapper<T>(id, bytesData, converter);
		}
	}

	private static class NullSelector implements MerkleEntrySelector {

		public static final MerkleEntrySelector INSTANCE = new NullSelector();

		private NullSelector() {
		}

		@Override
		public void accept(HashDigest nodeHash, MerkleIndex nodePath) {
		}

		@Override
		public void accept(HashDigest nodeHash, long id, byte[] bytesValue) {
		}

	}

	private static class MerkleProofSelector implements MerkleEntrySelector {

		private List<HashDigest> paths = new ArrayList<HashDigest>();

		private boolean success;

		public MerkleProof getProof() {
			throw new IllegalStateException("Not implemented");
		}

		@Override
		public void accept(HashDigest nodeHash, MerkleIndex nodePath) {
			paths.add(nodeHash);
		}

		@Override
		public void accept(HashDigest nodeHash, long id, byte[] bytesValue) {
			// 已经查找到对应的数据节点；
			this.success = true;
		}

		@SuppressWarnings("unused")
		public boolean isSuccess() {
			return success;
		}
	}

	/**
	 * 默克尔树的节点；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static abstract class MerkleNode implements MerkleIndex {

		protected final MerkleSortTree<?> TREE;

		/**
		 * 与当前子树相邻的右侧兄弟子树的偏移量；
		 */
		protected final long NEXT_OFFSET;

		protected final long OFFSET;

		protected final long STEP;

		private final HashDigest[] ORIG_CHILD_HASHS;

		private HashDigest nodeHash;

		private long[] childCounts;

		private HashDigest[] childHashs;

		private boolean modified;

		private Object[] children;

		protected MerkleNode(HashDigest nodeHash, long offset, long step, long[] childCounts, HashDigest[] childHashs,
				MerkleSortTree<?> tree) {
			this.TREE = tree;
			NEXT_OFFSET = tree.nextOffset(offset, step);

			this.nodeHash = nodeHash;
			this.modified = (nodeHash == null);

			this.OFFSET = offset;
			this.STEP = step;
			this.childCounts = childCounts;
			this.childHashs = childHashs;
			this.ORIG_CHILD_HASHS = childHashs.clone();
			this.children = new Object[tree.DEGREE];

			assert childHashs.length == tree.DEGREE;
		}

		public HashDigest[] getOrigChildHashs() {
			return ORIG_CHILD_HASHS;
		}

		public HashDigest getNodeHash() {
			return nodeHash;
		}

		public boolean isModified() {
			return modified;
		}

		@Override
		public long getOffset() {
			return OFFSET;
		}

		@Override
		public long getStep() {
			return STEP;
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
			if (id < OFFSET || id >= NEXT_OFFSET) {
				return -1;
			}
			long p = id - OFFSET;
			long m = p % STEP;
			return (int) ((p - m) / STEP);
		}

		/**
		 * 是否包含指定 id 的节点；
		 * 
		 * @param id
		 * @return
		 */
		@SuppressWarnings("unused")
		public boolean contain(long id) {
			return id >= OFFSET && id < NEXT_OFFSET;
		}

		public HashDigest getChildHashAtIndex(int index) {
			return childHashs[index];
		}

		/**
		 * @param index
		 * @return
		 */
		protected Object getChildObject(int index) {
			Object child = children[index];
			if (child != null) {
				return child;
			}
			HashDigest childHash = childHashs[index];
			if (childHash == null) {
				return null;
			}
			child = loadChild(childHash);
			children[index] = child;
			return child;
		}

		private Object loadChild(HashDigest childHash) {
			byte[] childBytes = TREE.loadNodeBytes(childHash);
			return deserializeChild(childBytes);
		}

		protected abstract Object deserializeChild(byte[] childBytes);

		protected abstract byte[] serializeChild(Object child);

		/**
		 * 设置子节点；
		 * 
		 * @param index     子节点的位置；
		 * @param childHash 子节点的哈希；如果为 null，则在 commit 时计算哈希；
		 * @param child     子节点；
		 */
		protected void setChildObject(int index, HashDigest childHash, Object child) {
			childHashs[index] = childHash;
			children[index] = child;
			modified = true;
		}

		/**
		 * 提交当前节点，把节点数据写入存储；
		 * 
		 * @return
		 */
		public HashDigest commit() {
			if (!modified) {
				return nodeHash;
			}

			commitChildren(childCounts, childHashs, children);

			// save;
			HashDigest hash = TREE.saveIndex(this);

			// update hash;
			for (int i = 0; i < TREE.DEGREE; i++) {
				ORIG_CHILD_HASHS[i] = childHashs[i];
			}
			this.nodeHash = hash;
			this.modified = false;

			return hash;
		}

		/**
		 * 提交子节点；在提交当前节点之前执行；
		 * 
		 * @param childCounts
		 * @param childHashs
		 * @param children
		 */
		protected abstract void commitChildren(long[] childCounts, HashDigest[] childHashs, Object[] children);

		protected void cancelChild(long id, Object child) {
		}

		/**
		 * 取消当前节点的更改；
		 */
		public void cancel() {
			Object child;
			for (int i = 0; i < TREE.DEGREE; i++) {
				if (children[i] != null && (childHashs[i] == null || ORIG_CHILD_HASHS[i] == null
						|| (!childHashs[i].equals(ORIG_CHILD_HASHS[i])))) {
					child = children[i];
					children[i] = null;
					// 清理字节点以便优化大对象的垃圾回收效率；
					if (STEP == 1) {
						long id = OFFSET + i;
						cancelChild(id, child);
					} else if (child instanceof MerkleNode) {
						((MerkleNode) child).cancel();
					}
				}
				childHashs[i] = ORIG_CHILD_HASHS[i];
			}
			// 注：不需要处理 nodeHash 的回滚，因为 nodeHash 是 commit 操作的最后确认标志；
		}
	}

	/**
	 * 默克尔树的叶子节点；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class LeafNode<T> extends MerkleNode {

		private final BytesConverter<T> CONVERTER;

		public LeafNode(long offset, MerkleSortTree<T> tree) {
			this(null, offset, 1L, new long[tree.DEGREE], new HashDigest[tree.DEGREE], tree);
		}

		public LeafNode(HashDigest nodeHash, MerkleIndex index, MerkleSortTree<T> tree) {
			this(nodeHash, index.getOffset(), index.getStep(), index.getChildCounts(), index.getChildHashs(), tree);
		}

		protected LeafNode(HashDigest nodeHash, long offset, long step, long[] childCounts, HashDigest[] childHashs,
				MerkleSortTree<T> tree) {
			super(nodeHash, offset, step, childCounts, childHashs, tree);
			assert step == 1;

			CONVERTER = tree.CONVERTER;
		}

		@SuppressWarnings("unchecked")
		private MerkleSortTree<T> tree() {
			return (MerkleSortTree<T>) TREE;
		}

		protected void setChild(int index, HashDigest childHash, T child) {
			super.setChildObject(index, childHash, child);
		}

		@SuppressWarnings("unchecked")
		public T getChild(int index) {
			return (T) super.getChildObject(index);
		}

		@Override
		protected T deserializeChild(byte[] childBytes) {
			return CONVERTER.fromBytes(childBytes);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected byte[] serializeChild(Object child) {
			return CONVERTER.toBytes((T) child);
		}

		@Override
		protected void commitChildren(long[] childCounts, HashDigest[] childHashs, Object[] children) {
			for (int i = 0; i < TREE.DEGREE; i++) {
				// 保存新创建的子节点；
				if (childHashs[i] == null && children[i] != null) {
					@SuppressWarnings("unchecked")
					T child = (T) children[i];
					long id = OFFSET + i;

					child = tree().DATA_POLICY.beforeCommitting(id, child);
					long count = tree().DATA_POLICY.count(id, child);

					byte[] childBytes = CONVERTER.toBytes(child);
					childHashs[i] = tree().saveNodeBytes(childBytes, TREE.OPTIONS.isReportKeyStorageConfliction());
					childCounts[i] = count;
					children[i] = child;
				}
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void cancelChild(long id, Object child) {
			tree().DATA_POLICY.afterCanceled(id, (T) child);
		}

	}

	/**
	 * 默克尔树的路径实现（非叶子节点）；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class PathNode extends MerkleNode {

		protected PathNode(long offset, long step, MerkleSortTree<?> tree) {
			this(null, offset, step, new long[tree.DEGREE], new HashDigest[tree.DEGREE], tree);
		}

		protected PathNode(HashDigest nodeHash, MerkleIndex index, MerkleSortTree<?> tree) {
			this(nodeHash, index.getOffset(), index.getStep(), index.getChildCounts(), index.getChildHashs(), tree);
		}

		protected PathNode(HashDigest nodeHash, long offset, long step, long[] childCounts, HashDigest[] childHashs,
				MerkleSortTree<?> tree) {
			super(nodeHash, offset, step, childCounts, childHashs, tree);
			assert step > 1;
		}

		/**
		 * 设置子节点；
		 * 
		 * @param index     子节点的位置；
		 * @param childHash 子节点的哈希；如果为 null，则在 commit 时计算哈希；
		 * @param child     子节点；
		 */
		protected void setChild(int index, HashDigest childHash, MerkleIndex child) {
			if (child.getStep() >= STEP || child.getOffset() < OFFSET || child.getOffset() >= NEXT_OFFSET) {
				throw new IllegalArgumentException("The specified child not belong to this node!");
			}
			super.setChildObject(index, childHash, child);
		}

		public MerkleIndex getChild(int index) {
			return (MerkleIndex) super.getChildObject(index);
		}

		@Override
		protected MerkleIndex deserializeChild(byte[] childBytes) {
			return BinaryProtocol.decode(childBytes, MerkleIndex.class);
		}

		@Override
		protected byte[] serializeChild(Object child) {
			return BinaryProtocol.encode(child, MerkleIndex.class);
		}

		@Override
		protected void commitChildren(long[] childCounts, HashDigest[] childHashs, Object[] children) {
			// save the modified childNodes;
			for (int i = 0; i < TREE.DEGREE; i++) {
				if (children[i] != null) {
					MerkleIndex child = (MerkleIndex) children[i];
					// 需要先保存子节点，获得子节点的哈希；
					if (child instanceof MerkleNode) {
						childHashs[i] = ((MerkleNode) child).commit();
					}
					childCounts[i] = count(child);
				}
			}
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
	private static class MerklePathIterator<T> implements SkippingIterator<MerkleValue<T>> {

		private final MerkleSortTree<?> TREE;

		private final BytesConverter<T> CONVERTER;

		private final DataPolicy<T> DATA_POLICY;

		private final long totalCount;

		private final long offset;

		private final long step;

		// 子节点的游标边界；
		private long[] childCounts;

		private HashDigest[] childHashs;

		private int childIndex;

		private long cursor = -1;

		private SkippingIterator<MerkleValue<T>> childIterator;

		public MerklePathIterator(long offset, long step, HashDigest[] childHashs, long[] childCounts,
				DataPolicy<T> dataPolicy, BytesConverter<T> converter, MerkleSortTree<?> tree) {
			this.offset = offset;
			this.step = step;
			this.childHashs = childHashs;
			this.childCounts = childCounts;
			// 使用此方法的上下文逻辑已经能够约束每一项的数字大小范围，不需要考虑溢出；
			this.totalCount = ArrayUtils.sum(childCounts);

			this.DATA_POLICY = dataPolicy;
			this.CONVERTER = converter;
			this.TREE = tree;
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
			if (childIndex >= TREE.DEGREE) {
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
				while (childIndex < TREE.DEGREE && skipped + childCounts[childIndex] <= count) {
					skipped += childCounts[childIndex];
					childIndex++;
				}
				if (childIndex < TREE.DEGREE) {
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

		private SkippingIterator<MerkleValue<T>> createChildIterator(int childIndex) {
			HashDigest childHash = childHashs[childIndex];
			if (childHash == null) {
				// 正常情况下不应该进入此逻辑分支，因为空的子节点的数量表为 0，迭代器的迭代处理逻辑理应过滤掉此位置的子节点；
				throw new IllegalStateException("The child hash is null !");
			}

			if (step > 1) {
				MerkleIndex child = TREE.loadMerkleEntry(childHash);
				return new MerklePathIterator<T>(child.getOffset(), child.getStep(), child.getChildHashs(),
						child.getChildCounts(), DATA_POLICY, CONVERTER, TREE);
			}
			byte[] childBytes = TREE.loadNodeBytes(childHash);
			long id = offset + childIndex;
			long count = childCounts[childIndex];
			return DATA_POLICY.iterator(id, childBytes, count, CONVERTER);
		}

		@Override
		public MerkleValue<T> next() {
			if (!hasNext()) {
				return null;
			}

			long s = ArrayUtils.sum(childCounts, 0, childIndex + 1);

			while (cursor + 1 >= s && childIndex < TREE.DEGREE - 1) {
				childIndex++;
				childIterator = null;
				s += childCounts[childIndex];
			}

			if (childIterator == null) {
				childIterator = createChildIterator(childIndex);
			}

			MerkleValue<T> v = childIterator.next();
			cursor++;
			return v;
		}

		@Override
		public long getCursor() {
			return cursor;
		}

	}

	private static class MerkleDataIteratorWrapper<T> extends AbstractSkippingIterator<MerkleValue<T>> {

		private long id;

		private byte[] valueBytes;

		private BytesConverter<T> converter;

		public MerkleDataIteratorWrapper(long id, byte[] valueBytes, BytesConverter<T> converter) {
			this.id = id;
			this.valueBytes = valueBytes;
			this.converter = converter;
		}

		@Override
		public long getTotalCount() {
			return 1;
		}

		@Override
		public MerkleValue<T> next() {
			if (hasNext()) {
				cursor++;
				T value = converter.fromBytes(valueBytes);
				return new IDValue<T>(id, value);
			}
			return null;
		}

	}

	private static class BytesToBytesConverter implements BytesConverter<byte[]> {

		@Override
		public byte[] toBytes(byte[] value) {
			return value;
		}

		@Override
		public byte[] fromBytes(byte[] bytes) {
			return bytes;
		}
	}

}

package com.jd.blockchain.ledger.proof;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.core.HashArrayProof;
import com.jd.blockchain.ledger.core.MerkleProofException;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.ExPolicyKVStorage.ExPolicy;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.LongSkippingIterator;
import com.jd.blockchain.utils.Transactional;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.io.BytesUtils;

public class HashSortingMerkleTree implements Transactional, Iterable<MerkleData> {

	private static final LongSkippingIterator<MerkleData> NULL_DATA_ITERATOR = LongSkippingIterator.empty();

	public static final int TREE_DEGREE = 16;

	public static final int MAX_LEVEL = 14;

	private static final Acceptor NULL_SELECTOR = new FullAcceptor();

	private HashFunction hashFunc;

	private final Bytes keyPrefix;

	private CryptoSetting setting;

	private ExPolicyKVStorage storage;

	private boolean readonly;

	private HashDigest rootHash;

	private PathNode root;

	/**
	 * 创建 Merkle 树；
	 * 
	 * @param rootHash     节点的根Hash; 如果指定为 null，则实际上创建一个空的 Merkle Tree；
	 * @param verifyOnLoad 从外部存储加载节点时是否校验节点的哈希；
	 * @param kvStorage    保存 Merkle 节点的存储服务；
	 * @param readonly     是否只读；
	 */
	public HashSortingMerkleTree(CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
		this(null, setting, keyPrefix, kvStorage, false);
	}

	/**
	 * 创建 Merkle 树；
	 * 
	 * @param rootHash     节点的根Hash; 如果指定为 null，则实际上创建一个空的 Merkle Tree；
	 * @param verifyOnLoad 从外部存储加载节点时是否校验节点的哈希；
	 * @param kvStorage    保存 Merkle 节点的存储服务；
	 * @param readonly     是否只读；
	 */
	public HashSortingMerkleTree(HashDigest rootHash, CryptoSetting setting, Bytes keyPrefix,
			ExPolicyKVStorage kvStorage, boolean readonly) {
		this.setting = setting;
		this.keyPrefix = keyPrefix;
		this.storage = kvStorage;
		this.readonly = readonly;
		this.hashFunc = Crypto.getHashFunction(setting.getHashAlgorithm());
		if (rootHash == null) {
			root = new PathNode(TREE_DEGREE);
		} else {
			PathNode rootNode = loadPathNode(rootHash, setting.getAutoVerifyHash());
			if (rootNode == null) {
				throw new IllegalStateException(
						"The root path node[" + Base58Utils.encode(rootHash.toBytes()) + "] not exist!");
			}
			this.rootHash = rootHash;
			this.root = rootNode;
		}
	}

	private PathNode loadPathNode(HashDigest rootHash, boolean autoVerifyHash) {
		return (PathNode) loadMerkleNode(rootHash);
	}

	public HashDigest getRootHash() {
		return rootHash;
	}

	@Deprecated
	public long getDataCount() {
		return getTotalKeys();
	}

	public long getTotalKeys() {
		return root.getTotalKeys();
	}

	public long getTotalRecords() {
		return root.getTotalRecords();
	}

	/**
	 * 返回指定 key 最新版本的默克尔证明；
	 * <p>
	 * 默克尔证明的根哈希为当前默克尔树的根哈希；<br>
	 * 默克尔证明的数据哈希为指定 key 的最新版本的值的哈希；
	 * <p>
	 * 
	 * 默克尔证明至少有 4 个哈希路径，包括：根节点哈希 + （0-N)个路径节点哈希 + 叶子节点哈希 + 数据项哈希(Key, Version,
	 * Value) + 数据值哈希；
	 * 
	 * @param key
	 * @return 默克尔证明
	 */
	public MerkleProof getProof(String key) {
		return seekProof(BytesUtils.toBytes(key));
	}

	/**
	 * 返回指定 key 指定版本的默克尔证明；
	 * <p>
	 * 默克尔证明的根哈希为当前默克尔树的根哈希；<br>
	 * 默克尔证明的数据哈希为指定 key 的最新版本的值的哈希；
	 * <p>
	 * 
	 * 默克尔证明至少有 4 个哈希路径，包括：根节点哈希 + （0-N)个路径节点哈希 + 叶子节点哈希 + 数据项哈希(Key, Version,
	 * Value) + 数据值哈希；
	 * 
	 * @param key
	 * @return 默克尔证明
	 */
	public MerkleProof getProof(String key, long version) {
		return seekProof(BytesUtils.toBytes(key), version);
	}

	/**
	 * 返回指定 key 最新版本的默克尔证明；
	 * <p>
	 * 默克尔证明的根哈希为当前默克尔树的根哈希；<br>
	 * 默克尔证明的数据哈希为指定 key 的最新版本的值的哈希；
	 * <p>
	 * 
	 * 默克尔证明至少有 4 个哈希路径，包括：根节点哈希 + （0-N)个路径节点哈希 + 叶子节点哈希 + 数据项哈希(Key, Version,
	 * Value) + 数据值哈希；
	 * 
	 * @param key
	 * @return 默克尔证明
	 */
	public MerkleProof getProof(Bytes key) {
		return seekProof(key.toBytes());
	}

	/**
	 * 返回指定 key 最新版本的默克尔证明；
	 * <p>
	 * 默克尔证明的根哈希为当前默克尔树的根哈希；<br>
	 * 默克尔证明的数据哈希为指定 key 的最新版本的值的哈希；
	 * <p>
	 * 
	 * 默克尔证明至少有 4 个哈希路径，包括：根节点哈希 + （0-N)个路径节点哈希 + 叶子节点哈希 + 数据项哈希(Key, Version,
	 * Value) + 数据值哈希；
	 * 
	 * @param key
	 * @return 默克尔证明
	 */
	public MerkleProof getProof(byte[] key) {
		return seekProof(key);
	}

	/**
	 * 返回指定 key 指定版本的默克尔证明；
	 * <p>
	 * 默克尔证明的根哈希为当前默克尔树的根哈希；<br>
	 * 默克尔证明的数据哈希为指定 key 的最新版本的值的哈希；
	 * <p>
	 * 
	 * 默克尔证明至少有 4 个哈希路径，包括：根节点哈希 + （0-N)个路径节点哈希 + 叶子节点哈希 + 数据项哈希(Key, Version,
	 * Value) + 数据值哈希；
	 * 
	 * @param key
	 * @return 默克尔证明
	 */
	public MerkleProof getProof(byte[] key, long version) {
		return seekProof(key, version);
	}

	private MerkleProof seekProof(byte[] key) {
		return seekProof(key, -1);
	}

	private MerkleProof seekProof(byte[] key, long version) {
		if (rootHash == null) {
			return null;
		}

		long keyHash = KeyIndexer.hash(key);

		ProofAcceptor selector = new ProofAcceptor(rootHash);

		MerkleData dataEntry = seekDataEntry(key, version, keyHash, root, 0, selector);
		if (dataEntry == null) {
			return null;
		}
		selector.addProof(dataEntry.getValueHash());
		return selector.getProof();
	}

	public MerkleData getData(String key) {
		return getData(key, -1);
	}

	public MerkleData getData(String key, long version) {
//		if (root.getNodeHash() == null) {
//			return null;
//		}
		byte[] keyBytes = BytesUtils.toBytes(key);
		long keyHash = KeyIndexer.hash(keyBytes);
		MerkleData dataEntry = seekDataEntry(keyBytes, version, keyHash, root, 0, NULL_SELECTOR);
		return dataEntry;
	}

	public MerkleData getData(byte[] key) {
//		if (root.getNodeHash() == null) {
//			return null;
//		}
		long keyHash = KeyIndexer.hash(key);
		MerkleData dataEntry = seekDataEntry(key, -1, keyHash, root, 0, NULL_SELECTOR);
		return dataEntry;
	}

	public MerkleData getData(byte[] key, long version) {
//		if (root.getNodeHash() == null) {
//			return null;
//		}
		long keyHash = KeyIndexer.hash(key);
		MerkleData dataEntry = seekDataEntry(key, version, keyHash, root, 0, NULL_SELECTOR);
		return dataEntry;
	}

	public MerkleData getData(Bytes key) {
//		if (root.getNodeHash() == null) {
//			return null;
//		}
		byte[] keyBytes = key.toBytes();
		long keyHash = KeyIndexer.hash(keyBytes);

		MerkleData dataEntry = seekDataEntry(keyBytes, -1, keyHash, root, 0, NULL_SELECTOR);
		return dataEntry;
	}

	/**
	 * 迭代器包含所有最新版本的数据项；
	 */
	@Override
	public MerkleDataIterator iterator() {
		return new MerkleDataIterator(root);
	}

	/**
	 * 迭代器包含所有基准树与原始树之间差异的数据项
	 */
	public PathKeysDiffIterator keysDiffIterator(HashDigest baseHash, HashDigest origHash) {
		return new PathKeysDiffIterator((PathNode) loadMerkleNode(baseHash), (PathNode) loadMerkleNode(origHash), 0);
	}

	/**
	 * 查找指定版本的键对应的数据项；
	 * 
	 * @param key
	 * @param version
	 * @param keyHash
	 * @param path
	 * @param level
	 * @param acceptor
	 * @return
	 */
	private MerkleData seekDataEntry(byte[] key, long version, long keyHash, MerklePath path, int level,
			Acceptor acceptor) {
		HashDigest[] childHashs = path.getChildHashs();
		byte keyIndex = KeyIndexer.index(keyHash, level);

		HashDigest childHash = childHashs == null ? null : childHashs[keyIndex];

		final int childLevel = level + 1;
		MerkleElement child = null;
		if (path instanceof PathNode) {
			// 从内存中加载；
			child = ((PathNode) path).getChildNode(keyIndex);
		}
		if (child == null) {
			if (childHash == null) {
				return null;
			}
			// 从存储中加载；
			child = loadMerkleEntry(childHash);
		}

		if (!acceptor.accept(childHash, child, childLevel)) {
			return null;
		}

		if (child instanceof MerklePath) {
			// Path;
			return seekDataEntry(key, version, keyHash, (MerklePath) child, childLevel, acceptor);
		}

		// Leaf；
		MerkleLeaf leaf = (MerkleLeaf) child;

		MerkleData[] merkleKeys = leaf.getDataEntries();
		for (MerkleData dataEntry : merkleKeys) {
			if (BytesUtils.equals(dataEntry.getKey(), key)) {
				if (version > dataEntry.getVersion()) {
					// 指定的版本超出最大版本；
					return null;
				}

				if (version < 0 || version == dataEntry.getVersion()) {
					return dataEntry;
				}

				return seekPreviousData(dataEntry, version, childLevel, acceptor);
			}
		}
		return null;
	}

	private MerkleData seekPreviousData(MerkleData data, long version, int level, Acceptor acceptor) {
		HashDigest previousHash = data.getPreviousEntryHash();

		MerkleData previousEntry = null;
		if (data instanceof MerkleDataEntry) {
			// 从内存中加载；
			previousEntry = ((MerkleDataEntry) data).getPreviousEntry();
		}
		if (previousEntry == null) {
			if (previousHash == null) {
				return null;
			}
			// 从存储中加载；
			previousEntry = loadDataEntry(previousHash);
		}

		if (!acceptor.accept(previousHash, previousEntry, level)) {
			return null;
		}

		if (previousEntry.getVersion() == version) {
			return previousEntry;
		}
		if (version > previousEntry.getVersion()) {
			// 未知异常；前向的数据链的版本本应该是顺序递减 1 直至版本 0 ，发生此错误表明数据链的版本存在错误；
			throw new IllegalStateException("Version is illegal in the data entry chain!");
		}

		return seekPreviousData(previousEntry, version, level, acceptor);
	}

	private MerkleData loadDataEntry(HashDigest dataEntryHash) {
		Bytes key = encodeNodeKey(dataEntryHash);
		byte[] bytes = storage.get(key);
		MerkleData dataEntry = BinaryProtocol.decode(bytes);
		return dataEntry;
	}

	@Override
	public boolean isUpdated() {
		return root.isModified();
	}

	@Override
	public void commit() {
		commit(root);
		rootHash = root.getNodeHash();
	}

	@Override
	public void cancel() {
		root.cancel();
	}

	public void print() {
		Map<Integer, List<String>> nodes = new HashMap<Integer, List<String>>();
		collectNodes(root, 0, nodes);

		for (Integer level : nodes.keySet()) {
			System.out.printf("--------- LEVE-%s ---------\r\n", level);
			List<String> nodeInfos = nodes.get(level);
			for (String nf : nodeInfos) {
				System.out.printf("%s, ", nf);
			}
			System.out.printf("\r\n");
		}
	}

	public void printDatas() {
		MerkleDataIterator iterator = iterator();
		System.out.println("\r\n\rn-------- HASH-SORTING-MERKLE-TREE -------");
		System.out.printf("total-size=%s\r\n", iterator.getCount());
		int i = 0;
		while (iterator.hasNext()) {
			MerkleData data = iterator.next();
			System.out.printf("[%s] - KEY=%s; VERSION=%s;\r\n", i, Base58Utils.encode(data.getKey()),
					data.getVersion());
		}
		System.out.printf("\r\n------------------\r\n", iterator.getCount());
	}

	private void collectNodes(PathNode node, int level, Map<Integer, List<String>> nodes) {
		Integer k = Integer.valueOf(level);
		List<String> lnodes = nodes.get(k);
		if (lnodes == null) {
			lnodes = new LinkedList<String>();
			nodes.put(k, lnodes);
		}
		MerkleTreeNode[] childNodes = node.getChildNodes();
		if (childNodes == null) {
			childNodes = new MerkleTreeNode[0];
		}
		StringBuilder nodeInfo = new StringBuilder("[P::");
		for (int i = 0; i < childNodes.length; i++) {
			if (childNodes[i] != null) {
				nodeInfo.append(i);
			}
			if (i < childNodes.length - 1) {
				nodeInfo.append(",");
			}
		}
		nodeInfo.append("]");

		lnodes.add(nodeInfo.toString());

		for (int i = 0; i < childNodes.length; i++) {
			if (childNodes[i] != null) {
				if (childNodes[i] instanceof PathNode) {
					collectNodes((PathNode) childNodes[i], level + 1, nodes);
				} else {
					collectNodes((LeafNode) childNodes[i], level + 1, nodes);
				}
			}
		}
	}

	private void collectNodes(LeafNode leafNode, int level, Map<Integer, List<String>> nodes) {
		Integer k = Integer.valueOf(level);
		List<String> lnodes = nodes.get(k);
		if (lnodes == null) {
			lnodes = new LinkedList<String>();
			nodes.put(k, lnodes);
		}
		MerkleData[] keys = leafNode.getDataEntries();
		StringBuilder nodeInfo = new StringBuilder(
				String.format("[L-%s-(k:%s;r=%s)-::", leafNode.getKeyHash(), keys.length, leafNode.getTotalRecords()));
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null) {
				nodeInfo.append(BytesUtils.toString(keys[i].getKey()));
			}
			if (i < keys.length - 1) {
				nodeInfo.append(";");
			}
		}
		nodeInfo.append("]");

		lnodes.add(nodeInfo.toString());
	}

	public void setData(String key, long version, byte[] data) {
		HashDigest dataHash = hashFunc.hash(data);
		setData(key, version, dataHash);
	}

	public void setData(Bytes key, long version, byte[] data) {
		HashDigest dataHash = hashFunc.hash(data);
		setData(key, version, dataHash);
	}

	public void setData(String key, long version, HashDigest dataHash) {
		setData(BytesUtils.toBytes(key), version, dataHash);
	}

	public void setData(Bytes key, long version, HashDigest dataHash) {
		setData(key.toBytes(), version, dataHash);
	}

	public void setData(byte[] key, long version, byte[] data) {
		HashDigest dataHash = hashFunc.hash(data);
		setData(key, version, dataHash);
	}

	public void setData(byte[] key, long version, HashDigest dataHash) {
		MerkleDataEntry data = new MerkleDataEntry(key, version, dataHash);
		long keyHash = KeyIndexer.hash(data.getKey());
		setDataEntry(keyHash, data);
	}

	private void setDataEntry(long keyHash, MerkleDataEntry dataEntry) {
		setDataEntry(keyHash, dataEntry, root, 0);
	}

	private void setDataEntry(long keyHash, MerkleDataEntry dataEntry, PathNode parentNode, int level) {
		byte index = KeyIndexer.index(keyHash, level);

		boolean hasChild = parentNode.containChild(index);
		if (hasChild) {
			// 存在子节点；
			MerkleTreeNode childNode = parentNode.getChildNode(index);
			HashDigest childHash = parentNode.getChildHash(index);
			if (childNode == null) {
				// 子节点为null，同时由于 PathNode#containChild 为 true，故此逻辑分支下 childHash 必然不为 null；
				childNode = loadMerkleNode(childHash);
				parentNode.setChildNode(index, childHash, childNode);
			}

			if (childNode instanceof LeafNode) {
				LeafNode leafNode = (LeafNode) childNode;
				if (keyHash == leafNode.getKeyHash()) {
					// key哈希冲突，追加新key；
					leafNode.addKeyNode(dataEntry);
				} else {
					// 延伸路径节点；
					PathNode newPath = new PathNode(TREE_DEGREE);
					parentNode.setChildNode(index, null, newPath);

					// 加入已有的数据节点；
					byte idx = KeyIndexer.index(leafNode.getKeyHash(), level + 1);
					newPath.setChildNode(idx, childHash, leafNode);

					// 递归: 加入新的key；
					setDataEntry(keyHash, dataEntry, newPath, level + 1);
				}
			} else if (childNode instanceof PathNode) {
				PathNode pathNode = (PathNode) childNode;
				// 递归: 加入新的key；
				setDataEntry(keyHash, dataEntry, pathNode, level + 1);
			} else {
				throw new IllegalStateException(
						"Unsupported merkle entry type[" + childNode.getClass().getName() + "]!");
			}
		} else {
			// 直接追加新节点；
			LeafNode leafNode = new LeafNode(keyHash);
			leafNode.addKeyNode(dataEntry);
			parentNode.setChildNode(index, null, leafNode);
		}
	}

	private MerkleTreeNode loadMerkleNode(HashDigest nodeHash) {
		MerkleElement entry = loadMerkleEntry(nodeHash);
		if (entry instanceof MerkleLeaf) {
			return LeafNode.create(nodeHash, (MerkleLeaf) entry);
		} else if (entry instanceof MerklePath) {
			return PathNode.create(nodeHash, (MerklePath) entry);
		} else {
			throw new IllegalStateException("Unsupported merkle entry type[" + entry.getClass().getName() + "]!");
		}
	}

	private MerkleElement loadMerkleEntry(HashDigest nodeHash) {
		Bytes key = encodeNodeKey(nodeHash);
		byte[] bytes = storage.get(key);
		if (bytes == null) {
			throw new MerkleProofException("Merkle node[" + nodeHash.toBase58() + "] does not exist!");
		}
		MerkleElement entry = BinaryProtocol.decode(bytes);
		return entry;
	}

	private void commit(PathNode pathNode) {
		if (!pathNode.isModified()) {
			return;
		}

		pathNode.update(hashFunc, new NodeUpdatedListener() {

			@Override
			public void onUpdated(HashDigest nodeHash, MerkleElement nodeEntry, byte[] nodeBytes) {
				Bytes key = encodeNodeKey(nodeHash);
				boolean success = storage.set(key, nodeBytes, ExPolicy.NOT_EXISTING);
				if (!success) {
					throw new MerkleProofException("Merkle node already exist!");
				}
			}
		});
	}

	private Bytes encodeNodeKey(HashDigest hashBytes) {
		return new Bytes(keyPrefix, hashBytes.toBytes());
	}

	/**
	 * 默克尔树的节点选择器；<br>
	 * 
	 * 用于在树节点的遍历中收集节点信息；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static interface Acceptor {
		boolean accept(HashDigest hash, MerkleElement element, int level);
	}

	private static class FullAcceptor implements Acceptor {
		@Override
		public boolean accept(HashDigest hash, MerkleElement element, int level) {
			return true;
		}

	}

	private static class ProofAcceptor implements Acceptor {

		private List<HashDigest> hashPaths = new ArrayList<HashDigest>();

		ProofAcceptor(HashDigest rootHash) {
			hashPaths.add(rootHash);
		}

		void addProof(HashDigest hashPath) {
			hashPaths.add(hashPath);
		}

		@Override
		public boolean accept(HashDigest hash, MerkleElement element, int level) {
			if (hash == null) {
				return false;
			}
			hashPaths.add(hash);
			return true;
		}

		MerkleProof getProof() {
			return new HashArrayProof(hashPaths);
		}
	}

	/**
	 * DiffIterator 提供对两个默克尔节点表示的默克尔树的新增节点的遍历；
	 * 
	 * <p>
	 * 
	 * 遍历的结果是得到一个数据差集：包含在新数据默克尔树中，但不包含在比较基准默克尔树中的数据集合；
	 * 
	 * @author huanghaiquan
	 *
	 */
	public static abstract class AbstractMerkleDataIterator implements LongSkippingIterator<MerkleData> {

		protected long cursor = -1;

		public long getCursor() {
			return cursor;
		}

		@Override
		public boolean hasNext() {
			return cursor + 1 < getCount();
		}

		public long skip(long skippingCount) {
			if (skippingCount < 0) {
				throw new IllegalArgumentException("The specified count is out of bound!");
			}
			if (skippingCount == 0) {
				return 0;
			}
			long count = getCount();
			if (cursor + skippingCount < count) {
				cursor += skippingCount;
				return skippingCount;
			}
			long skipped = count - cursor - 1;
			cursor = count - 1;
			return skipped;
		}

	}

	public class MerkleDataIterator extends AbstractMerkleDataIterator {

		private int childCursor = 0;

		private long totalSize;

		private PathNode root;


		private LongSkippingIterator<MerkleData> childIterator;

		private MerkleDataIterator(HashDigest rootHash, MerklePath rootNode) {
			this(PathNode.create(rootHash, rootNode));
		}

		private MerkleDataIterator(PathNode rootNode) {
			this.root = rootNode;
			totalSize = Arrays.stream(root.getChildKeys()).sum();
		}


		public long getCount() {
			return totalSize;
		}

		@Override
		public MerkleData next() {
			if (!hasNext()) {
				return null;
			}

			long childCount = getChildCount(0, childCursor + 1);

			while (cursor + 1 >= childCount) {
				childCursor++;
				childIterator = null;
				childCount = getChildCount(0, childCursor + 1);
			}

			cursor++;
			long childDiffOffset = cursor - getChildCount(0, childCursor);
			MerkleTreeNode childNode = getChildNode(root, (byte) childCursor);

			LongSkippingIterator<MerkleData> childIterator = getOrCreateDiffIterator(childNode);
			long nextChildCursor = childIterator.getCursor() + 1;

			childIterator.skip(childDiffOffset - nextChildCursor);

			return childIterator.next();
		}

		private LongSkippingIterator<MerkleData> getOrCreateDiffIterator(MerkleTreeNode childNode) {
			if (childIterator == null) {
				childIterator = createDiffIterator(childNode);
			}
			return childIterator;
		}

		private LongSkippingIterator<MerkleData> createDiffIterator(MerkleTreeNode childNode) {
			if (childNode == null) {
				return NULL_DATA_ITERATOR;
			}
			if (childNode instanceof PathNode) {
				return new MerkleDataIterator((PathNode) childNode);
			}
			if (childNode instanceof LeafNode) {
				return new LeafNodeIterator((LeafNode) childNode);
			}
			throw new IllegalStateException("Illegal type of MerkleTreeNode[" + childNode.getClass().getName() + "]");
		}

		private MerkleTreeNode getChildNode(PathNode pathNode, byte childIndex) {
			HashDigest childHash = pathNode.getChildHash(childIndex);
			MerkleTreeNode childNode = pathNode.getChildNode(childIndex);
			if (childNode == null && childHash != null) {
				childNode = loadMerkleNode(childHash);
				pathNode.setChildNode(childIndex, childHash, childNode);
			}
			return childNode;
		}

		private long getChildCount(int from, int to) {
			long sum = 0;
			long[] keys = root.getChildKeys();
			for (int i = from; i < to; i++) {
				sum += keys[i];
			}
			return sum;
		}

	}

	private static class LeafNodeIterator implements LongSkippingIterator<MerkleData> {

		private MerkleData[] dataEntries;

		private int cursor = -1;

		public LeafNodeIterator(MerkleLeaf leaf) {
			this.dataEntries = leaf.getDataEntries();
		}

		public long getCursor() {
			return cursor;
		}

		public long getCount() {
			return dataEntries.length;
		}

		/**
		 * 略过指定数量的数据项；
		 * 
		 * @param count 要略过的数据项的数量；
		 * @return 实际略过的数量；如果指定的数量超出剩余的数量范围，则返回实际略过的数量；否则返回参数指定的数量；
		 */
		public long skip(long count) {
			if (count < 0) {
				throw new IllegalArgumentException("The specified count is out of bound!");
			}
			if ((cursor + count + 1) >= dataEntries.length) {
				// 直接结束；
				long skipped = dataEntries.length - 1 - cursor;
				cursor = dataEntries.length - 1;
				return skipped;
			}

			cursor += count;
			return count;
		}

		@Override
		public boolean hasNext() {
			return cursor + 1 < dataEntries.length;
		}

		@Override
		public MerkleData next() {
			if (hasNext()) {
				return dataEntries[++cursor];
			}
			return null;
		}

	}

	/**
	 * DiffIterator 提供对两个默克尔节点表示的默克尔树的新增节点的遍历；
	 * 
	 * <p>
	 * 
	 * 遍历的结果是得到一个数据差集：包含在新数据默克尔树中，但不包含在比较基准默克尔树中的数据集合；
	 * 
	 * @author huanghaiquan
	 *
	 */
	public static abstract class DiffIterator extends AbstractMerkleDataIterator {

		/**
		 * 新增
		 */
		protected MerkleTreeNode root1;

		protected MerkleTreeNode root2;

		/**
		 * 创建一个差异遍历器；
		 * 
		 * 
		 * @param root1 包含新数据的默克尔树的根节点;
		 * @param root2 作为比较基准的默克尔树的根节点;
		 */
		public DiffIterator(MerkleTreeNode root1, MerkleTreeNode root2) {
			this.root1 = root1;
			this.root2 = root2;
		}

		@Override
		public long getCount() {
			return getDiffCount(root1, root2);
		}

		private long getDiffCount(MerkleTreeNode node1, MerkleTreeNode node2) {
			return getCount(node1) - getCount(node2);
		}

		protected abstract long getCount(MerkleTreeNode node);

	}

	/**
	 * DiffIterator 提供对两个默克尔节点表示的默克尔树的新增节点的遍历；
	 * 
	 * <p>
	 * 
	 * 遍历的结果是得到一个数据差集：包含在新数据默克尔树中，但不包含在比较基准默克尔树中的数据集合；
	 * 
	 * @author huanghaiquan
	 *
	 */
	public abstract class PathDiffIterator extends DiffIterator {

		private byte childCursor = 0;

		private LongSkippingIterator<MerkleData> childDiffIterator;

		/**
		 * 创建一个差异遍历器；
		 * 
		 * 
		 * @param root1 包含新数据的默克尔树的根节点;
		 * @param root2 作为比较基准的默克尔树的根节点;
		 */
		public PathDiffIterator(PathNode root1, PathNode root2) {
			super(root1, root2);
		}

		public long getCount() {
			return getDiffCount(root1, root2);
		}

		@Override
		public MerkleData next() {
			if (!hasNext()) {
				return null;
			}

			long diffChildCount = getDiffChildCount(0, childCursor + 1);

			while (cursor + 1 >= diffChildCount) {
				childCursor++;
				childDiffIterator = null;
				diffChildCount = getDiffChildCount(0, childCursor + 1);
			}

			cursor++;
			long childDiffOffset = cursor - getDiffChildCount(0, childCursor);
			MerkleTreeNode childNode1 = getChildNode((PathNode) root1, childCursor);
			MerkleTreeNode childNode2 = getChildNode((PathNode) root2, childCursor);

			LongSkippingIterator<MerkleData> childDiffIterator = getOrCreateDiffIterator(childNode1, childNode2);
			long nextChildDiffCursor = childDiffIterator.getCursor() + 1;

			childDiffIterator.skip(childDiffOffset - nextChildDiffCursor);

			return childDiffIterator.next();
		}

		private MerkleTreeNode getChildNode(PathNode pathNode, byte childIndex) {
			HashDigest childHash = pathNode.getChildHash(childIndex);
			MerkleTreeNode childNode = pathNode.getChildNode(childIndex);
			if (childNode == null && childHash != null) {
				childNode = loadMerkleNode(childHash);
				pathNode.setChildNode(childIndex, childHash, childNode);
			}
			return childNode;
		}

		private long getDiffChildCount(int fromIndex, int toIndex) {
			long diffChildCount = getDiffChildCount(root1, root2, fromIndex, toIndex);

			assert diffChildCount >= 0 : "The diffChildCount is negative!";
			return diffChildCount;
		}

		/**
		 * 返回
		 * 
		 * @param node1
		 * @param node2
		 * @param fromIndex
		 * @param toIndex
		 * @return
		 */
		private long getDiffChildCount(MerkleTreeNode node1, MerkleTreeNode node2, int fromIndex, int toIndex) {
			return getChildCount(node1, fromIndex, toIndex) - getChildCount(node2, fromIndex, toIndex);
		}

		private long getDiffCount(MerkleTreeNode node1, MerkleTreeNode node2) {
			return getCount(node1) - getCount(node2);
		}

		/**
		 * get the total count between the specified child index range;
		 * 
		 * @param node
		 * @param fromIndex from child index(included);
		 * @param toIndex   to child index(excluded);
		 * @return
		 */
		private long getChildCount(MerkleTreeNode node, int fromIndex, int toIndex) {
			assert fromIndex <= toIndex : "from-index larger than to-index";

			if (fromIndex == toIndex) {
				return 0;
			}

			long s1 = 0;
			for (int i = fromIndex; i < toIndex; i++) {
				s1 += getChildCount(node, i);
			}
			return s1;
		}

		protected LongSkippingIterator<MerkleData> getOrCreateDiffIterator(MerkleTreeNode rootNode1,
				MerkleTreeNode rootNode2) {
			if (childDiffIterator == null) {
				childDiffIterator = createDiffIterator(rootNode1, rootNode2);
			}
			return childDiffIterator;
		}

		protected abstract long getCount(MerkleTreeNode node);

		protected abstract long getChildCount(MerkleTreeNode node, int childIndex);

		protected abstract LongSkippingIterator<MerkleData> createDiffIterator(MerkleTreeNode rootNode1,
				MerkleTreeNode rootNode2);

	}

	/**
	 * 对两个默克尔树表示的键集合的差集遍历器；
	 * 
	 * @author huanghaiquan
	 *
	 */
	public class PathKeysDiffIterator extends PathDiffIterator {

		private int level;

		public PathKeysDiffIterator(PathNode root1, PathNode root2, int level) {
			super(root1, root2);
			this.level = level;
		}

		@Override
		protected long getCount(MerkleTreeNode node) {
			return node.getTotalKeys();
		}

		@Override
		protected long getChildCount(MerkleTreeNode node, int childIndex) {
			return ((PathNode) node).getChildKeys()[childIndex];
		}

		@Override
		protected LongSkippingIterator<MerkleData> createDiffIterator(MerkleTreeNode node1, MerkleTreeNode node2) {
			if (node2 == null && node1 instanceof LeafNode) {
				return new LeafNodeIterator((LeafNode) node1);
			}
			if (node2 == null && node1 instanceof PathNode) {
				return new MerkleDataIterator((PathNode) node1);
			}
			if (node1 instanceof PathNode && node2 instanceof PathNode) {
				return new PathKeysDiffIterator((PathNode) node1, (PathNode) node2, level + 1);
			}
			if (node1 instanceof PathNode && node2 instanceof LeafNode) {
				return new NewPathKeysDiffIterator((PathNode) node1, (LeafNode) node2, level + 1);
			}
			if (node1 instanceof LeafNode && node2 instanceof LeafNode) {
				return new LeafKeysDiffIterator((LeafNode) node1, (LeafNode) node2);
			}
			throw new IllegalStateException("Both nodes type exception!");
		}

	}

	/**
	 * 对两个默克尔树表示的记录集合的差集遍历器；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private class PathRecordsDiffIterator extends PathDiffIterator {

		public PathRecordsDiffIterator(PathNode root1, PathNode root2) {
			super(root1, root2);
		}

		@Override
		protected long getCount(MerkleTreeNode node) {
			return ((PathNode) node).getTotalRecords();
		}

		@Override
		protected long getChildCount(MerkleTreeNode node, int childIndex) {
			return ((PathNode) node).getChildRecords()[childIndex];
		}

		@Override
		protected DiffIterator createDiffIterator(MerkleTreeNode rootNode1, MerkleTreeNode rootNode2) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	/**
	 * 对两个默克尔树叶子节点包含的键集合的差集遍历器；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private class LeafKeysDiffIterator extends DiffIterator {
		private LinkedList<MerkleData> origDataEntries;
		private LinkedList<MerkleData> baseDataEntries;
		private LinkedList<MerkleData> diffDataEntries;

		public LeafKeysDiffIterator(LeafNode root1, LeafNode orignalLeafNode) {
			super(root1, orignalLeafNode);
			origDataEntries = createDataEntries(orignalLeafNode);
			baseDataEntries = createDataEntries(root1);
			diffDataEntries = createDiffDataEntries(baseDataEntries, origDataEntries);
		}

		@Override
		protected long getCount(MerkleTreeNode node) {
			return ((LeafNode) node).getTotalKeys();
		}

		@Override
		public MerkleData next() {
			System.out.println("LeafKeysDiffIterator " + diffDataEntries.getFirst());

			return diffDataEntries.removeFirst();
		}

		// 获取叶子节点对应的所有数据入口集
		private LinkedList<MerkleData> createDataEntries(LeafNode leafNode) {
			LinkedList<MerkleData> dataEntries = new LinkedList<>();
			for (int i = 0; i < leafNode.getTotalKeys(); i++) {
				dataEntries.add(leafNode.getDataEntries()[i]);
			}
			return dataEntries;
		}

		// 获取两个叶子节点数据入口的差异集
		private LinkedList<MerkleData> createDiffDataEntries(LinkedList<MerkleData> baseDataEntries,
				LinkedList<MerkleData> origDataEntries) {
			LinkedList<MerkleData> diffDataEntries = new LinkedList<>();
			boolean found = false;

			for (int baseindex = 0; baseindex < baseDataEntries.size(); baseindex++) {
				for (int origindex = 0; origindex < origDataEntries.size(); origindex++) {
					if (Arrays.equals(origDataEntries.get(origindex).getKey(),
							baseDataEntries.get(baseindex).getKey())) {
						found = true;
						break;
					}
				}
				if (!found) {
					diffDataEntries.add(baseDataEntries.get(baseindex));
					found = false;
				}
			}
			return baseDataEntries;
		}
	}

	/**
	 * 对两个默克尔树叶子节点包含的记录集合的差集遍历器；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private class LeafRecordsDiffIterator extends DiffIterator {

		public LeafRecordsDiffIterator(LeafNode root1, LeafNode root2) {
			super(root1, root2);
		}

		@Override
		protected long getCount(MerkleTreeNode node) {
			return ((LeafNode) node).getTotalRecords();
		}

		@Override
		public MerkleData next() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * 对默克尔树新的叶子节点和默克尔树空子节点包含的记录集合的差集遍历器；
	 *
	 * @author huanghaiquan
	 *
	 */
	private class NewLeafRecordsDiffIterator extends DiffIterator {

		public NewLeafRecordsDiffIterator(MerkleTreeNode root1, MerkleTreeNode root2) {
			super(root1, root2);
		}

		@Override
		protected long getCount(MerkleTreeNode node) {
			return 0;
		}

		@Override
		public MerkleData next() {
			return null;
		}
	}

	/**
	 * 对默克尔树路径节点和默克尔树空子节点包含的记录集合的差集遍历器；
	 *
	 * @author huanghaiquan
	 *
	 */
	private class NewPathRecordsDiffIterator1 extends DiffIterator {

		public NewPathRecordsDiffIterator1(MerkleTreeNode root1, MerkleTreeNode root2) {
			super(root1, root2);
		}

		@Override
		protected long getCount(MerkleTreeNode node) {
			return 0;
		}

		@Override
		public MerkleData next() {
			return null;
		}
	}

	/**
	 * 对默克尔树路径节点和默克尔树叶子节点包含的键集合的差集遍历器；
	 * 
	 * @author huanghaiquan
	 *
	 */
	public class NewPathKeysDiffIterator extends DiffIterator {

		private Set<byte[]> origKeys;

		private Set<Long> origKeyIndexes;

		private MerkleDataIterator iterator1;

		public NewPathKeysDiffIterator(PathNode root1, LeafNode orignalLeafNode, int level) {
			super(root1, orignalLeafNode);
			iterator1 = new MerkleDataIterator(root1);
			origKeys = createKeySet(orignalLeafNode);
			origKeyIndexes = seekKeyIndexes(origKeys, root1, level);
		}

		// 不包括toIndex对应子孩子的key总数
		private long getTotalChildKeys(byte fromIndex, byte toIndex, PathNode pathNode) {
			long total = 0;

			for (byte i = fromIndex; i < toIndex; i++) {
				total += pathNode.getChildKeys()[i];
			}
			return total;
		}

		/**
		 * Compare this key and specified key;
		 *
		 * @param otherKey
		 * @return Values: -1, 0, 1. <br>
		 *         Return -1 means that the current key is less than the specified
		 *         key;<br>
		 *         Return 0 means that the current key is equal to the specified
		 *         key;<br>
		 *         Return 1 means that the current key is great than the specified key;
		 */
		public int compare(byte[] key1, byte[] key2) {
			int len = Math.min(key1.length, key2.length);
			for (int i = 0; i < len; i++) {
				if (key1[i] == key2[i]) {
					continue;
				}
				return key1[i] < key2[i] ? -1 : 1;
			}
			if (key1.length == key2.length) {
				return 0;
			}

			return key1.length < key2.length ? -1 : 1;
		}

		/**
		 * 寻找指定key在指定的子树中的线性位置；
		 * 
		 * @param merkleDataKey 要查找的key；
		 * @param pathNode      key所在的子树的根节点；
		 * @param level         子树根节点的深度；
		 * @return 返回 key 在子树中的线性位置，值大于等于 0；如果不存在，-1；
		 */
		private long seekKeyIndex(byte[] merkleDataKey, PathNode pathNode, int level) {
			// 1：计算 key 在当前路径节点中的子节点位置；
			// 2：计算 key 所在子节点之前的所有key的数量，作为 key 最终线性位置基准；
			// 3：计算 key 在子节点表示的线性位置；有两种情况：(1)子节点为路径节点；(2)子节点为叶子节点；
			// 3.1：子节点为路径节点时：递归调用当前方法计算；
			// 3.2：子节点为叶子节点时：在叶子节点中的数据项列表查找指定 key 的位置；
			// 4：基准位置和当前偏移位置相加，得到 key 的最终线性位置，该位置与左序遍历得到的位置一致；

			// 1
			long keyHash = KeyIndexer.hash(merkleDataKey);
			byte index = KeyIndexer.index(keyHash, level);

			// 2
			long childCounts = getTotalChildKeys((byte) 0, index, pathNode);

			// 3
			MerkleTreeNode childNode = pathNode.getChildNode(index);
			HashDigest childHash = pathNode.getChildHash(index);
			if (childNode == null) {
				// 子节点为null，同时由于 PathNode#containChild 为 true，故此逻辑分支下 childHash 必然不为 null；
				childNode = loadMerkleNode(childHash);
				pathNode.setChildNode(index, childHash, childNode);
			}

			// 3.1
			long keyIndex = -1;
			if (childNode instanceof PathNode) {
				keyIndex = seekKeyIndex(merkleDataKey, (PathNode) childNode, level + 1);
			} else {
				// 3.2 childNode instanceof LeafNode
				LeafNode leafNode = (LeafNode) childNode;
				MerkleData[] dataEntries = leafNode.getDataEntries();
				for (int i = 0; i < dataEntries.length; i++) {
					if (compare(merkleDataKey, dataEntries[i].getKey()) == 0) {
						keyIndex = i;
						break;
					}
				}
			}
			if (keyIndex == -1) {
				throw new IllegalStateException(
						"Unexpected error! -- key doesnot contains in the specified merkle tree!");
			}

			return childCounts + keyIndex;
		}

		private Set<Long> seekKeyIndexes(Set<byte[]> origKeys, PathNode root1, int level) {
			Set<Long> origKeyIndexes = new HashSet<Long>();

			for (byte[] key : origKeys) {
				origKeyIndexes.add(seekKeyIndex(key, root1, level));
			}
			return origKeyIndexes;
		}

		private Set<byte[]> createKeySet(LeafNode orignalLeafNode) {
			Set<byte[]> origKeys = new HashSet<byte[]>();

			for (int i = 0; i < orignalLeafNode.getTotalKeys(); i++) {
				origKeys.add(orignalLeafNode.getDataEntries()[i].getKey());
			}
			return origKeys;
		}

		@Override
		protected long getCount(MerkleTreeNode node) {
			if (node instanceof PathNode) {
				return ((PathNode) node).getTotalKeys();
			}
			return ((LeafNode) node).getTotalKeys();
		}

		@Override
		public long getCount() {
			return iterator1.getCount() - origKeys.size();
		}

		@Override
		public long skip(long count) {
			long s = 0;
			long k = 0;
			for (long i = 0; i < count; i++) {
				k = iterator1.skip(1);
				if (k < 1) {
					break;
				}
				s += k;
				while (origKeyIndexes.contains(new Long(iterator1.cursor))) {
					k = iterator1.skip(1);
					if (k < 1) {
						break;
					}
				}
			}
			return s;
		}

		// 判断原始叶子键值集中是否包含指定的键值
		private boolean contains(Set<byte[]> origKeys, byte[] key) {
			for (byte[] origKey : origKeys) {
				if (Arrays.equals(origKey, key)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public MerkleData next() {
			if (iterator1.hasNext()) {
				MerkleData nextKey = iterator1.next();
				while (contains(origKeys, nextKey.getKey())) {
					if (iterator1.hasNext()) {
						nextKey = iterator1.next();
					} else {
						return null;
					}
				}

				return nextKey;
			}
//			System.out.println("NewPathKeysDiffIterator " + new String(nextKey.getKey()));
			return null;
		}

	}

	/**
	 * 对默克尔树路径节点和默克尔树叶子节点包含的记录集合的差集遍历器；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private class NewPathRecordsDiffIterator extends DiffIterator {

		public NewPathRecordsDiffIterator(PathNode root1, LeafNode root2) {
			super(root1, root2);
		}

		@Override
		protected long getCount(MerkleTreeNode node) {
			if (node instanceof PathNode) {
				return ((PathNode) node).getTotalRecords();
			}
			return ((LeafNode) node).getTotalRecords();
		}

		@Override
		public MerkleData next() {
			// TODO Auto-generated method stub
			return null;
		}

	}

}

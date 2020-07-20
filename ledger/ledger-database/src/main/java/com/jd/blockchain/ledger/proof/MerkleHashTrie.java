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
import com.jd.blockchain.ledger.core.HashPathProof;
import com.jd.blockchain.ledger.core.MerkleProofException;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.ExPolicyKVStorage.ExPolicy;
import com.jd.blockchain.utils.AbstractSkippingIterator;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.Transactional;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.io.BytesUtils;

/**
 * {@link MerkleHashTrie} 默克尔哈希前缀树；
 * <p>
 * 
 * 结合了默克尔树 (MerkleTree) 和哈希前缀树(Hash Trie)；
 * <p>
 * 哈希前缀树(Hash Trie)是一种特殊的前缀树（Trie），对输入的 KEY 进行哈希计算之后，基于得到的哈希值进行前缀排序的有序树；<br>
 * 通过哈希计算使树节点的分布更均匀；
 * <p>
 * {@link MerkleHashTrie} 不保证输入 KEY
 * 的自然顺序和写入的先后顺序，但是保证集合顺序的确定性，称为不变性（immutability）<br>
 * 即对于相同的数据集合组成的 {@link MerkleHashTrie} 都具有相同的根哈希和完全一致的树节点分布，与每一项数据写入的先后顺序无关；
 * 
 * @author huanghaiquan
 *
 */
public class MerkleHashTrie implements Transactional, Iterable<MerkleData> {

	private static final SkippingIterator<MerkleData> NULL_DATA_ITERATOR = SkippingIterator.empty();

	public static final int TREE_DEGREE = 16;

	public static final int MAX_LEVEL = 14;

	private static final SeekingSelector NULL_SELECTOR = new FullSelector();

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
	public MerkleHashTrie(CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage kvStorage) {
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
	public MerkleHashTrie(HashDigest rootHash, CryptoSetting setting, Bytes keyPrefix, ExPolicyKVStorage kvStorage,
			boolean readonly) {
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

		ProofPathsSelector selector = new ProofPathsSelector(rootHash);

		MerkleData dataEntry = seekDataEntry(new Bytes(key), version, keyHash, root, 0, selector);
		if (dataEntry == null) {
			return null;
		}
		selector.addProof(dataEntry.getValueHash());
		return selector.createProof();
	}

	public MerkleData getData(String key) {
		return getData(key, -1);
	}

	public MerkleData getData(String key, long version) {
		return getData(Bytes.fromString(key), version);
	}

	public MerkleData getData(byte[] key) {
		return getData(new Bytes(key));
	}

	public MerkleData getData(byte[] key, long version) {
		return getData(new Bytes(key), version);
	}

	public MerkleData getData(Bytes key) {
		return getData(key, -1);
	}

	public MerkleData getData(Bytes key, long version) {
		long keyHash = KeyIndexer.hash(key);
		MerkleData dataEntry = seekDataEntry(key, version, keyHash, root, 0, NULL_SELECTOR);
		return dataEntry;
	}

	/**
	 * 返回所有键的最新版本数据；
	 */
	@Override
	public SkippingIterator<MerkleData> iterator() {
		return new MerkleDataIterator(root, this);
	}

	/**
	 * 返回指定键的所有版本数据；
	 * 
	 * @param key
	 * @return
	 */
	public SkippingIterator<MerkleData> iterator(byte[] key) {
		return iterator(key, -1);
	}

	/**
	 * 返回指定键的指定版本之前的所有数据（含指定版本）；
	 * 
	 * @param key
	 * @param version
	 * @return
	 */
	public SkippingIterator<MerkleData> iterator(byte[] key, long version) {
		//TODO;
		return null;
	}


	/**
	 * 迭代器包含所有基准树与原始树之间差异的数据项
	 */
	public SkippingIterator<MerkleData> getKeyDiffIterator(MerkleHashTrie origTree) {
		return new PathKeysDiffIterator(root, this, origTree.root, origTree, 0);
	}

	/**
	 * 查找指定版本的键对应的数据项；
	 * 
	 * @param key
	 * @param version
	 * @param keyHash
	 * @param path
	 * @param level
	 * @param selector
	 * @return
	 */
	private MerkleData seekDataEntry(Bytes key, long version, long keyHash, MerklePath path, int level,
			SeekingSelector selector) {
		HashDigest[] childHashs = path.getChildHashs();
		byte keyIndex = KeyIndexer.index(keyHash, level);

		HashDigest childHash = childHashs == null ? null : childHashs[keyIndex];

		final int childLevel = level + 1;
		MerkleTrieEntry child = null;
		if (path instanceof PathNode) {
			// 从内存中加载；
			child = ((PathNode) path).getChildNode(keyIndex);
		}
		if (child == null) {
			if (childHash == null) {
				return null;
			}
			// 从存储中加载；
			child = loadMerkleTrieEntry(childHash);
		}

		if (!selector.accept(childHash, child, childLevel)) {
			// 对于路径节点，如果选择器不接受，则直接终止搜索；
			return null;
		}

		if (child instanceof MerklePath) {
			// Path;
			return seekDataEntry(key, version, keyHash, (MerklePath) child, childLevel, selector);
		}

		// Leaf；
		MerkleLeaf leaf = (MerkleLeaf) child;

		MerkleKey[] merkleKeys = leaf.getKeys();

		for (int i = 0; i < merkleKeys.length; i++) {
			MerkleKey merkleKey = merkleKeys[i];

			if (merkleKey.getKey().equals(key)) {
				long latestVersion = merkleKey.getVersion();
				if (version > latestVersion) {
					// 指定的版本超出最大版本；
					return null;
				}

				HashDigest dataEntryHash = merkleKey.getDataEntryHash();
				MerkleData data = null;
				if (leaf instanceof LeafNode) {
					// LeafNode 是新增或者新修改的；
					MerkleData[] datas = ((LeafNode) leaf).getDataEntries();
					data = datas[i];
				}
				if (data == null) {
					// 注：不应同时出现 data == null and merkleKey.getDataEntryHash() == null 的情形；
					// 因为只有新增节点尚未提交时，才存在 merkleKey.getDataEntryHash() == null 的情形；
					if (dataEntryHash == null) {
						throw new IllegalStateException(
								"Illegal state that a merkle key has a null hash of data-entry and has no uncommitted data-entry! ");
					}

					data = loadDataEntry(dataEntryHash);
					if (data == null) {
						// 丢失数据；只有数据库存储层面被恶意或者非恶意地破坏数据完整性才可能出现此情况；
						throw new IllegalStateException("Miss MerkleData with hash[" + dataEntryHash + "]!");
					}
				}
				if (data.getVersion() != latestVersion) {
					throw new IllegalStateException(
							"The version of MerkleData doesn't match the expected version of MerkleKey!");
				}

				if (setting.getAutoVerifyHash()) {
					// TODO: 验证哈希；
				}

				int dataEntryLevel = childLevel + 1;

				if (version < 0 || version == latestVersion) {
					// 如果指定的 version 小于零，则等同于查询最新版本；

					// 匹配到最终版本的数据项；
					if (!selector.accept(dataEntryHash, data, dataEntryLevel)) {
						// 如果选择器不接受最终的版本匹配节点，则返回 null，结束继续向前搜索；
						return null;
					}
					return data;
				} else {
					// 查询的版本仍然小于前一个数据项，仍然需要继续往前搜索；

					// 对于数据节点，此处忽略选择器的判断，是因为需要继续向前搜索至目标版本的数据节点；
					// 这样设计的考虑是：数据节点的不同版本之间形成的是链表结构，链表的头部是最新加入的版本，有可能新加入的版本是尚未提交的数据；
					// 通过采用不间断地继续往前搜索的策略，可以实现即使存在未提交的新版本，依然可以正常地检索到已提交版本的数据；
					selector.accept(dataEntryHash, data, dataEntryLevel);
					return seekPreviousData(data, version, dataEntryLevel, selector);
				}
			}
		}
		return null;
	}

	/**
	 * 查询指定数据节点的早期版本；
	 * 
	 * @param data     要查询的数据节点；
	 * @param version  要查询的版本；
	 * @param level    所属叶子节点的深度；
	 * @param selector 节点选择器；
	 * @return
	 */
	private MerkleData seekPreviousData(MerkleData data, long version, int level, SeekingSelector selector) {
		assert version < data.getVersion();

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

		if (version > previousEntry.getVersion()) {
			// 查询的版本大于前一个数据项版本，如果出现这种状态，可能的原因包括：a、从存储加载的数据节点存在错误；b、属于外部调用的逻辑错误，正常情况不应该出现此条件分支；
			// 因为前向的数据链的版本总是顺序递减 1 直至版本 0 的，出现此条件表明继续向前搜索是没有意义的；
			throw new IllegalStateException("Version is illegal in the data entry chain!");
		}

		int previousDataLevel = level + 1;

		if (previousEntry.getVersion() == version) {
			// 匹配到最终版本的数据项；
			if (!selector.accept(previousHash, previousEntry, previousDataLevel)) {
				// 如果选择器不接受最终的版本匹配节点，则返回 null，结束继续向前搜索；
				return null;
			}
			return previousEntry;
		} else {
			// 查询的版本仍然小于前一个数据项，仍然需要继续往前搜索；

			// 对于数据节点，此处忽略选择器的判断，是因为需要继续向前搜索至目标版本的数据节点；
			// 这样设计的考虑是：数据节点的不同版本之间形成的是链表结构，链表的头部是最新加入的版本，有可能新加入的版本是尚未提交的数据；
			// 通过采用不间断地继续往前搜索的策略，可以实现即使存在未提交的新版本，依然可以正常地检索到已提交版本的数据；
			selector.accept(previousHash, previousEntry, previousDataLevel);

			return seekPreviousData(previousEntry, version, previousDataLevel, selector);
		}
	}

	private MerkleData loadDataEntry(HashDigest dataEntryHash) {
//		Bytes key = encodeEntryKey(dataEntryHash);
//		byte[] bytes = storage.get(key);
//		MerkleData dataEntry = BinaryProtocol.decode(bytes);
//		return dataEntry;

		return (MerkleData) loadMerkleTrieEntry(dataEntryHash);
	}

	private MerkleData[] loadDataEntries(MerkleLeaf leafNode) {
		MerkleKey[] keys = leafNode.getKeys();
		MerkleData[] datas;
		if (leafNode instanceof LeafNode) {
			datas = ((LeafNode) leafNode).getDataEntries();
		} else {
			datas = new MerkleData[keys.length];
		}
		for (int i = 0; i < datas.length; i++) {
			if (datas[i] == null) {
				datas[i] = loadDataEntry(keys[i].getDataEntryHash());
			}
		}
		return datas;
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
		SkippingIterator<MerkleData> iterator = iterator();
		System.out.println("\r\n\rn-------- HASH-SORTING-MERKLE-TREE -------");
		System.out.printf("total-size=%s\r\n", iterator.getCount());
		int i = 0;
		while (iterator.hasNext()) {
			MerkleData data = iterator.next();
			System.out.printf("[%s] - KEY=%s; VERSION=%s;\r\n", i, data.getKey().toBase58(), data.getVersion());
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
		MerkleKey[] keys = leafNode.getKeys();
		StringBuilder nodeInfo = new StringBuilder(
				String.format("[L-%s-(k:%s;r=%s)-::", leafNode.getKeyHash(), keys.length, leafNode.getTotalRecords()));
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != null) {
				nodeInfo.append(keys[i].getKey());
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
		MerkleTrieEntry entry = loadMerkleTrieEntry(nodeHash);
		if (entry instanceof MerkleLeaf) {
			return LeafNode.create(nodeHash, (MerkleLeaf) entry);
		} else if (entry instanceof MerklePath) {
			return PathNode.create(nodeHash, (MerklePath) entry);
		} else {
			throw new IllegalStateException("Unsupported merkle entry type[" + entry.getClass().getName() + "]!");
		}
	}

	private MerkleTrieEntry loadMerkleTrieEntry(HashDigest entryHash) {
		Bytes key = encodeEntryKey(entryHash);
		byte[] bytes = storage.get(key);
		if (bytes == null) {
			throw new MerkleProofException(
					"The merkle trie entry with hash [" + entryHash.toBase58() + "] does not exist!");
		}
		MerkleTrieEntry entry = BinaryProtocol.decode(bytes);
		return entry;
	}

	private void commit(PathNode pathNode) {
		if (!pathNode.isModified()) {
			return;
		}

		pathNode.update(hashFunc, new NodeUpdatedListener() {

			@Override
			public void onUpdated(HashDigest nodeHash, MerkleTrieEntry nodeEntry, byte[] nodeBytes) {
				Bytes key = encodeEntryKey(nodeHash);
				boolean success = storage.set(key, nodeBytes, ExPolicy.NOT_EXISTING);
				if (!success) {
					throw new MerkleProofException("Merkle node already exist!");
				}
			}
		});
	}

	private Bytes encodeEntryKey(HashDigest hashBytes) {
		return new Bytes(keyPrefix, hashBytes.toBytes());
	}

	/**
	 * 默克尔树的节点遍历选择器；<br>
	 * 
	 * 用于在树节点的遍历中收集节点信息；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static interface SeekingSelector {

		/**
		 * 检查接收节点；
		 * 
		 * @param hash    节点哈希；
		 * @param element 节点；
		 * @param level   深度；
		 * @return 是否继续；
		 */
		boolean accept(HashDigest hash, MerkleTrieEntry element, int level);
	}

	private static class FullSelector implements SeekingSelector {
		@Override
		public boolean accept(HashDigest hash, MerkleTrieEntry element, int level) {
			return true;
		}

	}

	private static class ProofPathsSelector implements SeekingSelector {

		private List<HashDigest> hashPaths = new ArrayList<HashDigest>();

		ProofPathsSelector(HashDigest rootHash) {
			hashPaths.add(rootHash);
		}

		void addProof(HashDigest hashPath) {
			hashPaths.add(hashPath);
		}

		@Override
		public boolean accept(HashDigest hash, MerkleTrieEntry element, int level) {
			// 默克尔证明收集器需要忽略哈希为 null 的默克尔前缀树节点；
			// 哈希为 null 的情形包括：
			// 1：未提交的路径节点；
			// 2：未提交的数据节点；
			// 对于调用者来说，在此方法返回 false 时，仍然会继续向前搜索，以便继续遍历排列在未提交节点之后的已提交节点；
			if (hash == null) {
				return false;
			}

			// TODO: 需要改进可能的超长的证明路径；当一个 key 的数据节点产生了许多版本之后，查历史版本可能会出现很深的路径；
			// 通过参数 level 可以判断这一点；
			// 当出现很深的路径时，可以采用对中间路径进行压缩计算成一个哈希证明；
			hashPaths.add(hash);
			return true;
		}

		MerkleProof createProof() {
			return new HashPathProof(hashPaths);
		}
	}
	
	
	public static class MerkleKeyVersionIterator extends AbstractSkippingIterator<MerkleData>{
		
		private MerkleHashTrie tree;
		
		private MerkleKey key;
		

		@Override
		public long getCount() {
			// TODO Auto-generated method stub
			return key.getVersion()+1;
		}

		@Override
		public MerkleData next() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	public static class MerkleDataIterator extends AbstractSkippingIterator<MerkleData> {

		private MerkleHashTrie tree;

		private int childCursor = 0;

		private long totalSize;

		private PathNode root;

		private SkippingIterator<MerkleData> childIterator;

		public MerkleDataIterator(PathNode rootNode, MerkleHashTrie tree) {
			this.root = rootNode;
			this.tree = tree;
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

			SkippingIterator<MerkleData> childIterator = getOrCreateDiffIterator(childNode);
			long nextChildCursor = childIterator.getCursor() + 1;

			childIterator.skip(childDiffOffset - nextChildCursor);

			return childIterator.next();
		}

		private SkippingIterator<MerkleData> getOrCreateDiffIterator(MerkleTreeNode childNode) {
			if (childIterator == null) {
				childIterator = createDiffIterator(childNode);
			}
			return childIterator;
		}

		private SkippingIterator<MerkleData> createDiffIterator(MerkleTreeNode childNode) {
			if (childNode == null) {
				return NULL_DATA_ITERATOR;
			}
			if (childNode instanceof PathNode) {
				return new MerkleDataIterator((PathNode) childNode, tree);
			}
			if (childNode instanceof LeafNode) {
				return new MerkleLeafDataIterator((LeafNode) childNode, tree);
			}
			throw new IllegalStateException("Illegal type of MerkleTreeNode[" + childNode.getClass().getName() + "]");
		}

		private MerkleTreeNode getChildNode(PathNode pathNode, byte childIndex) {
			HashDigest childHash = pathNode.getChildHash(childIndex);
			MerkleTreeNode childNode = pathNode.getChildNode(childIndex);
			if (childNode == null && childHash != null) {
				childNode = tree.loadMerkleNode(childHash);
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

	/**
	 * 对叶子节点最新版本数据项的迭代器；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class MerkleLeafDataIterator implements SkippingIterator<MerkleData> {

		private MerkleHashTrie tree;

		private MerkleKey[] keys;

		private MerkleData[] dataEntries;

		private int cursor = -1;

		public MerkleLeafDataIterator(MerkleLeaf leaf, MerkleHashTrie tree) {
			this.keys = leaf.getKeys();
			if (leaf instanceof LeafNode) {
				this.dataEntries = ((LeafNode) leaf).getDataEntries();
			}

			this.tree = tree;
		}

		public long getCursor() {
			return cursor;
		}

		public long getCount() {
			return keys.length;
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
			if ((cursor + count + 1) >= keys.length) {
				// 直接结束；
				long skipped = keys.length - 1 - cursor;
				cursor = keys.length - 1;
				return skipped;
			}

			cursor += count;
			return count;
		}

		@Override
		public boolean hasNext() {
			return cursor + 1 < keys.length;
		}

		@Override
		public MerkleData next() {
			if (hasNext()) {
				int nextIndex = cursor + 1;
				MerkleData data = dataEntries[nextIndex];
				if (data == null) {
					data = tree.loadDataEntry(keys[nextIndex].getDataEntryHash());
				}
				return data;
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
	public static abstract class DiffDataIterator extends AbstractSkippingIterator<MerkleData> {

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
		public DiffDataIterator(MerkleTreeNode root1, MerkleTreeNode root2) {
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
	public static abstract class PathDiffIterator extends DiffDataIterator {

		protected MerkleHashTrie tree1;

		protected MerkleHashTrie tree2;

		private byte childCursor = 0;

		private SkippingIterator<MerkleData> childDiffIterator;

		/**
		 * 创建一个差异遍历器；
		 * 
		 * 
		 * @param root1 包含新数据的默克尔树的根节点;
		 * @param root2 作为比较基准的默克尔树的根节点;
		 */
		public PathDiffIterator(MerkleHashTrie tree1, PathNode root1, MerkleHashTrie tree2, PathNode root2) {
			super(root1, root2);
			this.tree1 = tree1;
			this.tree2 = tree2;
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
			MerkleTreeNode childNode1 = getChildNode(tree1, (PathNode) root1, childCursor);
			MerkleTreeNode childNode2 = getChildNode(tree2, (PathNode) root2, childCursor);

			SkippingIterator<MerkleData> childDiffIterator = getOrCreateDiffIterator(childNode1, childNode2);
			long nextChildDiffCursor = childDiffIterator.getCursor() + 1;

			childDiffIterator.skip(childDiffOffset - nextChildDiffCursor);

			return childDiffIterator.next();
		}

		private MerkleTreeNode getChildNode(MerkleHashTrie tree, PathNode pathNode, byte childIndex) {
			HashDigest childHash = pathNode.getChildHash(childIndex);
			MerkleTreeNode childNode = pathNode.getChildNode(childIndex);
			if (childNode == null && childHash != null) {
				childNode = tree.loadMerkleNode(childHash);
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

		protected SkippingIterator<MerkleData> getOrCreateDiffIterator(MerkleTreeNode rootNode1,
				MerkleTreeNode rootNode2) {
			if (childDiffIterator == null) {
				childDiffIterator = createDiffIterator(rootNode1, rootNode2);
			}
			return childDiffIterator;
		}

		protected abstract long getCount(MerkleTreeNode node);

		protected abstract long getChildCount(MerkleTreeNode node, int childIndex);

		protected abstract SkippingIterator<MerkleData> createDiffIterator(MerkleTreeNode rootNode1,
				MerkleTreeNode rootNode2);

	}

	/**
	 * 对两个默克尔树表示的键集合的差集遍历器；
	 * 
	 * @author huanghaiquan
	 *
	 */
	public static class PathKeysDiffIterator extends PathDiffIterator {

		private int level;

		public PathKeysDiffIterator(PathNode root1, MerkleHashTrie tree1, PathNode root2, MerkleHashTrie tree2,
				int level) {
			super(tree1, root1, tree2, root2);
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
		protected SkippingIterator<MerkleData> createDiffIterator(MerkleTreeNode node1, MerkleTreeNode node2) {
			if (node2 == null && node1 instanceof LeafNode) {
				return new MerkleLeafDataIterator((LeafNode) node1, tree1);
			}
			if (node2 == null && node1 instanceof PathNode) {
				return new MerkleDataIterator((PathNode) node1, tree1);
			}
			if (node1 instanceof PathNode && node2 instanceof PathNode) {
				return new PathKeysDiffIterator((PathNode) node1, tree1, (PathNode) node2, tree2, level + 1);
			}
			if (node1 instanceof PathNode && node2 instanceof LeafNode) {
				return new NewPathKeysDiffIterator((PathNode) node1, tree1, (LeafNode) node2, tree2, level + 1);
			}
			if (node1 instanceof LeafNode && node2 instanceof LeafNode) {
				return new LeafKeysDiffIterator((LeafNode) node1, tree1, (LeafNode) node2, tree2);
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

		public PathRecordsDiffIterator(MerkleHashTrie tree1, PathNode root1, MerkleHashTrie tree2, PathNode root2) {
			super(tree1, root1, tree2, root2);
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
		protected DiffDataIterator createDiffIterator(MerkleTreeNode rootNode1, MerkleTreeNode rootNode2) {
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
	private static class LeafKeysDiffIterator extends AbstractSkippingIterator<MerkleData> {
		private MerkleData[] diffDataEntries;

		public LeafKeysDiffIterator(LeafNode leaf1, MerkleHashTrie tree1, LeafNode leaf2, MerkleHashTrie tree2) {
			MerkleData[] dataEntries1 = tree1.loadDataEntries(leaf1);
			MerkleData[] dataEntries2 = tree2.loadDataEntries(leaf2);
			diffDataEntries = selectDiffDataEntries(dataEntries1, dataEntries2);
		}

		@Override
		public long getCount() {
			return diffDataEntries.length;
		}

		@Override
		public MerkleData next() {
			return diffDataEntries[(int) cursor];
		}

		/**
		 * 获取数据集合的差集；即包含在集合 dataEntries1 中且不包含在集合 dataEntries2 中的数据项；
		 * 
		 * @param dataEntries1
		 * @param dataEntries2
		 * @return
		 */
		private MerkleData[] selectDiffDataEntries(MerkleData[] dataEntries1, MerkleData[] dataEntries2) {
			// MerkleHashTrie 的 Leaf 节点由于哈希冲突概率极小，同一个 LeafNode 中有 1 条以上的概率极小；故优化 ArrayList
			// 初始化容量为 2；
			List<MerkleData> diffDataEntries = new ArrayList<MerkleData>(2);
			boolean found = false;
			Set<Bytes> keys2 = new HashSet<Bytes>();
			for (int i = 0; i < dataEntries2.length; i++) {
				keys2.add(dataEntries2[i].getKey());
			}
			for (int i = 0; i < dataEntries1.length; i++) {
				if (keys2.contains(dataEntries1[i].getKey())) {
					continue;
				}
				diffDataEntries.add(dataEntries1[i]);
			}
			return diffDataEntries.toArray(new MerkleData[diffDataEntries.size()]);
		}
	}

	/**
	 * 对两个默克尔树叶子节点包含的记录集合的差集遍历器；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private class LeafRecordsDiffIterator extends DiffDataIterator {

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
	private class NewLeafRecordsDiffIterator extends DiffDataIterator {

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
	private class NewPathRecordsDiffIterator1 extends DiffDataIterator {

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
	public static class NewPathKeysDiffIterator extends DiffDataIterator {

		private MerkleHashTrie tree1;

		private MerkleKey[] origKeys;

		private Set<Long> origKeyIndexes;

		private MerkleDataIterator iterator1;

		public NewPathKeysDiffIterator(PathNode node1, MerkleHashTrie tree1, LeafNode origNode, MerkleHashTrie origTree,
				int level) {
			super(node1, origNode);
			this.tree1 = tree1;
			this.iterator1 = new MerkleDataIterator(node1, tree1);
			this.origKeys = origNode.getKeys();
			this.origKeyIndexes = seekKeyIndexes(this.origKeys, node1, level);
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
		 * 寻找指定key在指定的子树中的线性位置；
		 * 
		 * @param key      要查找的key；
		 * @param pathNode key所在的子树的根节点；
		 * @param level    子树根节点的深度；
		 * @return 返回 key 在子树中的线性位置，值大于等于 0；如果不存在，-1；
		 */
		private long seekKeyIndex(Bytes key, PathNode pathNode, int level) {
			// 1：计算 key 在当前路径节点中的子节点位置；
			// 2：计算 key 所在子节点之前的所有key的数量，作为 key 最终线性位置基准；
			// 3：计算 key 在子节点表示的线性位置；有两种情况：(1)子节点为路径节点；(2)子节点为叶子节点；
			// 3.1：子节点为路径节点时：递归调用当前方法计算；
			// 3.2：子节点为叶子节点时：在叶子节点中的数据项列表查找指定 key 的位置；
			// 4：基准位置和当前偏移位置相加，得到 key 的最终线性位置，该位置与左序遍历得到的位置一致；

			// 1
			long keyHash = KeyIndexer.hash(key);
			byte index = KeyIndexer.index(keyHash, level);

			// 2
			long childCounts = getTotalChildKeys((byte) 0, index, pathNode);

			// 3
			MerkleTreeNode childNode = pathNode.getChildNode(index);
			HashDigest childHash = pathNode.getChildHash(index);
			if (childNode == null) {
				// 子节点为null，同时由于 PathNode#containChild 为 true，故此逻辑分支下 childHash 必然不为 null；
				childNode = tree1.loadMerkleNode(childHash);
				pathNode.setChildNode(index, childHash, childNode);
			}

			// 3.1
			long keyIndex = -1;
			if (childNode instanceof PathNode) {
				keyIndex = seekKeyIndex(key, (PathNode) childNode, level + 1);
			} else {
				// 3.2 childNode instanceof LeafNode
				LeafNode leafNode = (LeafNode) childNode;
				MerkleKey[] dataEntries = leafNode.getKeys();
				for (int i = 0; i < dataEntries.length; i++) {
					if (key.equals(dataEntries[i].getKey())) {
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

		private Set<Long> seekKeyIndexes(MerkleKey[] keys, PathNode node, int level) {
			Set<Long> origKeyIndexes = new HashSet<Long>();

			for (MerkleKey key : keys) {
				origKeyIndexes.add(seekKeyIndex(key.getKey(), node, level));
			}
			return origKeyIndexes;
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
			return iterator1.getCount() - origKeys.length;
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
				while (origKeyIndexes.contains(new Long(iterator1.getCursor()))) {
					k = iterator1.skip(1);
					if (k < 1) {
						return s;
					}
				}
				s++;
				cursor++;
			}
			return s;
		}

		// 判断原始叶子键值集中是否包含指定的键值
		private boolean contains(MerkleKey[] origKeys, Bytes key) {
			for (MerkleKey origKey : origKeys) {
				if (origKey.getKey().equals(key)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public MerkleData next() {
			MerkleData nextKey = null;

			while (iterator1.hasNext()) {
				nextKey = iterator1.next();
				if (!contains(origKeys, nextKey.getKey())) {
					cursor++;
					return nextKey;
				}
			}
			return null;
		}
	}

	/**
	 * 对默克尔树路径节点和默克尔树叶子节点包含的记录集合的差集遍历器；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private class NewPathRecordsDiffIterator extends DiffDataIterator {

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

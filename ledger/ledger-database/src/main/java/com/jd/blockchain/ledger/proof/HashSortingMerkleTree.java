package com.jd.blockchain.ledger.proof;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import com.jd.blockchain.utils.Transactional;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.io.BytesUtils;

public class HashSortingMerkleTree implements Transactional {

	public static final int TREE_DEGREE = 16;

	public static final int MAX_LEVEL = 14;

	private HashFunction hashFunc;

	private final Bytes keyPrefix;

	private CryptoSetting setting;

	private ExPolicyKVStorage storage;

	private boolean readonly;

	private PathNode root;

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
			this.root = rootNode;
		}
	}

	private PathNode loadPathNode(HashDigest rootHash, boolean autoVerifyHash) {
		return (PathNode) loadMerkleNode(rootHash);
	}

	public HashDigest getRootHash() {
		return root.getNodeHash();
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
		if (root.getNodeHash() == null) {
			return null;
		}
		byte[] keyBytes = BytesUtils.toBytes(key);
		long keyHash = KeyIndexer.hash(keyBytes);

		List<HashDigest> hashPaths = new LinkedList<HashDigest>();
		hashPaths.add(root.getNodeHash());

		boolean success = seekHashPaths(keyBytes, keyHash, root, 0, hashPaths);
		if (success) {
			return new HashArrayProof(hashPaths);
		}
		return null;
	}

	private boolean seekHashPaths(byte[] key, long keyHash, MerklePath path, int level, List<HashDigest> hashPaths) {
		HashDigest[] childHashs = path.getChildHashs();
		byte keyIndex = KeyIndexer.index(keyHash, level);

		HashDigest childHash = childHashs == null ? null : childHashs[keyIndex];
		if (childHash == null) {
			return false;
		}

		hashPaths.add(childHash);

		MerkleElement child = null;
		if (path instanceof PathNode) {
			child = ((PathNode) path).getChildNode(keyIndex);
		}
		if (child == null) {
			child = loadMerkleEntry(childHash);
		}

		if (child instanceof MerklePath) {
			// Path;
			return seekHashPaths(key, keyHash, (MerklePath) child, level + 1, hashPaths);
		}

		// Leaf；
		MerkleLeaf leaf = (MerkleLeaf) child;

		MerkleKey[] merkleKeys = leaf.getKeys();
		for (MerkleKey mkey : merkleKeys) {
			if (BytesUtils.equals(mkey.getKey(), key)) {
				HashDigest dataEntryHash = mkey.getDataEntryHash();
				hashPaths.add(dataEntryHash);

				MerkleData dataEntry = null;
				if (mkey instanceof KeyEntry) {
					dataEntry = ((KeyEntry) mkey).getDataNode();
				}
				if (dataEntry == null) {
					dataEntry = loadDataEntry(dataEntryHash);
				}
				hashPaths.add(dataEntry.getValueHash());
				return true;
			}
		}
		return false;
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
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub

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
//				nodeInfo.append(" ");
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
		StringBuilder nodeInfo = new StringBuilder(String.format("[L-%s-%s::", leafNode.getKeyHash(), keys.length));
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

	public void setData(String key, long version, HashDigest dataHash) {
		MerkleDataEntry data = new MerkleDataEntry(BytesUtils.toBytes(key), version, dataHash);
		long keyHash = KeyIndexer.hash(data.getKey());
		addKeyNode(keyHash, data);
	}

	private void addKeyNode(long keyHash, MerkleDataEntry keyNode) {
		addKeyNode(keyHash, keyNode, root, 0);
	}

	private void addKeyNode(long keyHash, MerkleDataEntry dataEntry, PathNode parentNode, int level) {
		byte index = KeyIndexer.index(keyHash, level);

		boolean hasChild = parentNode.containChild(index);
		if (hasChild) {
			// 存在子节点；
			MerkleTreeNode childNode = parentNode.getChildNode(index);
			if (childNode == null) {
				// 子节点尚未加载； 注：由于 PathNode#containChild 为 true，故此分支下 childHash 必然不为 null；
				HashDigest childHash = parentNode.getChildHash(index);
				childNode = loadMerkleNode(childHash);
			}

			if (childNode instanceof LeafNode) {
				LeafNode leafNode = (LeafNode) childNode;
				if (keyHash == leafNode.getKeyHash()) {
					// key哈希冲突，追加新key；
					leafNode.addKeyNode(dataEntry);
				} else {
					// 延伸路径节点；
					PathNode newPath = new PathNode(TREE_DEGREE);
					parentNode.setChildNode(index, newPath);

					// 加入已有的数据节点；
					byte idx = KeyIndexer.index(leafNode.getKeyHash(), level + 1);
					newPath.setChildNode(idx, leafNode);

					// 递归: 加入新的key；
					addKeyNode(keyHash, dataEntry, newPath, level + 1);
				}
			} else if (childNode instanceof PathNode) {
				PathNode pathNode = (PathNode) childNode;
				// 递归: 加入新的key；
				addKeyNode(keyHash, dataEntry, pathNode, level + 1);
			} else {
				throw new IllegalStateException(
						"Unsupported merkle entry type[" + childNode.getClass().getName() + "]!");
			}
		} else {
			// 直接追加新节点；
			LeafNode leafNode = new LeafNode(keyHash);
			leafNode.addKeyNode(dataEntry);
			parentNode.setChildNode(index, leafNode);
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
}

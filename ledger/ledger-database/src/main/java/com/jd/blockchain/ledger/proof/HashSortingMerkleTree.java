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
		// TODO Auto-generated method stub
		return null;
	}

	public HashDigest getRootHash() {
		return root.getNodeHash();
	}

	public long getDataCount() {
		throw new IllegalStateException("Not implement");
	}

	public MerkleProof getProof(String key, long version) {
		throw new IllegalStateException("Not implement");
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

	public void setData(String key, long version, byte[] data, long ts) {
		HashDigest dataHash = hashFunc.hash(data);
		setData(key, version, dataHash, ts);
	}

	public void setData(String key, long version, HashDigest dataHash, long ts) {
		MerkleDataEntry data = new MerkleDataEntry(BytesUtils.toBytes(key), version, dataHash, ts);
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
				MerkleElement entry = loadMerkleEntry(childHash);
				if (entry instanceof MerkleLeaf) {
					childNode = LeafNode.create(childHash, (MerkleLeaf) entry);
				} else if (entry instanceof MerklePath) {
					childNode = PathNode.create(childHash, (MerklePath) entry);
				} else {
					throw new IllegalStateException(
							"Unsupported merkle entry type[" + entry.getClass().getName() + "]!");
				}
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

	private MerkleElement loadMerkleEntry(HashDigest childHash) {
		Bytes key = encodeNodeKey(childHash);
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

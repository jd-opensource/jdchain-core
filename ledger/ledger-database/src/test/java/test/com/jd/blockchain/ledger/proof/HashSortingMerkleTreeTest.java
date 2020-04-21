package test.com.jd.blockchain.ledger.proof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.mockito.Mockito;

import com.jd.blockchain.binaryproto.BinaryProtocol;
import com.jd.blockchain.binaryproto.DataContractEncoder;
import com.jd.blockchain.binaryproto.DataSpecification;
import com.jd.blockchain.binaryproto.impl.DataContractContext;
import com.jd.blockchain.consts.DataCodes;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.proof.HashSortingMerkleTree;
import com.jd.blockchain.ledger.proof.HashSortingMerkleTree.MerkleDataIterator;
import com.jd.blockchain.ledger.proof.MerkleData;
import com.jd.blockchain.ledger.proof.MerkleDataEntry;
import com.jd.blockchain.ledger.proof.MerklePath;
import com.jd.blockchain.ledger.proof.PathNode;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.utils.MemoryKVStorage;
import com.jd.blockchain.storage.service.utils.VersioningKVData;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.hash.MurmurHash3;
import com.jd.blockchain.utils.io.BytesUtils;
import com.jd.blockchain.utils.io.NumberMask;

import test.com.jd.blockchain.ledger.core.LedgerTestUtils;

public class HashSortingMerkleTreeTest {

	private static final Bytes KEY_PREFIX = Bytes.fromString("/MerkleTree");

	private static final HashFunction SHA256_HASH_FUNC = Crypto.getHashFunction(ClassicAlgorithm.SHA256);

	/**
	 * 测试 Murmur3 哈希算法的不变性，即同样的输入得到完全一致的输出；
	 */
	@Test
	public void testMurmurHashInvariance() {
		int seed = 102400;
		byte[] data = BytesUtils.toBytes("TEST_DATA_" + System.currentTimeMillis());
		long h1;
		long h2;
		long[] hashs = new long[2];
		MurmurHash3.murmurhash3_x64_128(data, 0, data.length, seed, hashs);
		h1 = hashs[0];
		h2 = hashs[1];

		for (int i = 0; i < 10240000; i++) {
			MurmurHash3.murmurhash3_x64_128(data, 0, data.length, seed, hashs);
			assertEquals(h1, hashs[0]);
			assertEquals(h2, hashs[1]);
		}
	}

	/**
	 * 测试数据序列化；
	 */
	@Test
	public void testDataSerialize() {
		byte[] key = BytesUtils.toBytes("KEY-1");
		HashDigest hashDigest = SHA256_HASH_FUNC.hash(key);
		MerkleData merkleData = new MerkleDataEntry(key, 0, hashDigest);
		testMerkleDataSerialize(merkleData);
		merkleData = new MerkleDataEntry(key, 1024, hashDigest);
		testMerkleDataSerialize(merkleData);
		merkleData = new MerkleDataEntry(key, NumberMask.LONG.MAX_BOUNDARY_SIZE - 1, hashDigest);
		testMerkleDataSerialize(merkleData);

		// 数据大小；
		System.out.println("------ Merkle Data Serialize ------");
		byte[] dataBytes = BinaryProtocol.encode(merkleData, MerkleData.class);
		System.out.printf("DataBytes= %s B\r\n", dataBytes.length);

		System.out.println("------ Merkle Path Serialize ------");
		int degree = 8;
		PathNode pathNode = new PathNode(degree);

		dataBytes = BinaryProtocol.encode(pathNode, MerklePath.class);
		System.out.printf("childs=%s; bytes=%s B\r\n", 0, dataBytes.length);
		for (int i = 0; i < degree; i++) {
			HashDigest childHash = SHA256_HASH_FUNC.hash(BytesUtils.toBytes(1024 + i));
			pathNode.setChildNode((byte) i, childHash, null);

			dataBytes = BinaryProtocol.encode(pathNode, MerklePath.class);
			System.out.printf("childs=%s; bytes=%s B\r\n", i + 1, dataBytes.length);
		}

	}

	private void testMerkleDataSerialize(MerkleData data) {
		byte[] dataBytes = BinaryProtocol.encode(data, MerkleData.class);
		int offset = 0;
		int code = BytesUtils.toInt(dataBytes, offset);
		offset += 12;
		assertEquals(DataCodes.MERKLE_DATA, code);

		byte[] valueHashBytes = data.getValueHash().toBytes();

		int expectedSize = 12 + NumberMask.NORMAL.getMaskLength(data.getKey().length) + data.getKey().length
				+ NumberMask.LONG.getMaskLength(data.getVersion())
				+ NumberMask.NORMAL.getMaskLength(valueHashBytes.length) + valueHashBytes.length
				+ NumberMask.NORMAL.getMaskLength(0);

		assertEquals(expectedSize, dataBytes.length);

		DataContractEncoder dataContractEncoder = DataContractContext.resolve(MerkleData.class);
		DataSpecification dataSpec = dataContractEncoder.getSepcification();
		assertEquals(4, dataSpec.getFields().size());
		assertEquals(5, dataSpec.getSlices().size());

		System.out.println(dataSpec.toString());

		MerkleData dataDes = BinaryProtocol.decode(dataBytes);

		assertTrue(BytesUtils.equals(data.getKey(), dataDes.getKey()));
		assertEquals(data.getVersion(), dataDes.getVersion());
		assertEquals(data.getValueHash(), dataDes.getValueHash());
		assertEquals(data.getPreviousEntryHash(), dataDes.getPreviousEntryHash());
	}

	@Test
	public void testReadUncommitting() {
		// 数据集合长度为 1024 时也能正常生成；
		int count = 500;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		HashSortingMerkleTree merkleTree = buildMerkleTree(datas, false);

		// 未提交之前查不到信息；
		assertNull(merkleTree.getRootHash());
		assertEquals(0, merkleTree.getTotalKeys());
		assertEquals(0, merkleTree.getTotalRecords());

		MerkleData dt = merkleTree.getData("KEY-69");
		assertNotNull(dt);
		assertEquals(0, dt.getVersion());
		dt = merkleTree.getData("KEY-69", 0);
		assertNotNull(dt);
		assertEquals(0, dt.getVersion());

		dt = merkleTree.getData("KEY-69", 1);
		assertNull(dt);

		VersioningKVData<String, byte[]> data69 = new VersioningKVData<String, byte[]>("KEY-69", 1,
				BytesUtils.toBytes("NEW-VALUE-VERSION-1"));
		merkleTree.setData(data69.getKey(), data69.getVersion(), data69.getValue());

		dt = merkleTree.getData("KEY-69", 1);
		assertNotNull(dt);
		assertEquals(1, dt.getVersion());

		merkleTree.commit();

		HashDigest rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(count, merkleTree.getTotalKeys());
		assertEquals(count + 1, merkleTree.getTotalRecords()); // 由于 KEY-69 写入了 2 个版本的记录；
		dt = merkleTree.getData("KEY-69");
		assertNotNull(dt);
		assertEquals(1, dt.getVersion());
		dt = merkleTree.getData("KEY-69", 0);
		assertNotNull(dt);
		assertEquals(0, dt.getVersion());
		dt = merkleTree.getData("KEY-69", 1);
		assertNotNull(dt);
		assertEquals(1, dt.getVersion());

//		merkleTree.print();
		for (VersioningKVData<String, byte[]> data : datas) {
			testMerkleProof(data, merkleTree, -1);
		}
	}

	@Test
	public void testCancel() {
		// 数据集合长度为 1024 时也能正常生成；
		int count = 1024;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		HashSortingMerkleTree merkleTree = buildMerkleTree(datas, false);
		assertTrue(merkleTree.isUpdated());

		// 未提交之前查不到信息；
		assertNull(merkleTree.getRootHash());
		assertEquals(0, merkleTree.getTotalKeys());
		assertEquals(0, merkleTree.getTotalRecords());

		MerkleData dt = merkleTree.getData("KEY-69");
		assertNotNull(dt);
		assertEquals(0, dt.getVersion());
		dt = merkleTree.getData("KEY-69", 0);
		assertNotNull(dt);
		assertEquals(0, dt.getVersion());

		dt = merkleTree.getData("KEY-69", 1);
		assertNull(dt);

		VersioningKVData<String, byte[]> data69 = new VersioningKVData<String, byte[]>("KEY-69", 1,
				BytesUtils.toBytes("NEW-VALUE-VERSION-1"));
		merkleTree.setData(data69.getKey(), data69.getVersion(), data69.getValue());

		dt = merkleTree.getData("KEY-69", 1);
		assertNotNull(dt);
		assertEquals(1, dt.getVersion());

		assertTrue(merkleTree.isUpdated());

		// 回滚全部数据；
		merkleTree.cancel();

		HashDigest rootHash = merkleTree.getRootHash();
		assertNull(rootHash);
		assertEquals(0, merkleTree.getTotalKeys());
		assertEquals(0, merkleTree.getTotalRecords()); // 由于 KEY-69 写入了 2 个版本的记录；
		dt = merkleTree.getData("KEY-69");
		assertNull(dt);
		dt = merkleTree.getData("KEY-69", 0);
		assertNull(dt);
		dt = merkleTree.getData("KEY-69", 1);
		assertNull(dt);

		assertFalse(merkleTree.isUpdated());
	}

	/**
	 * 测试树的创建的正确性；
	 */
	@Test
	public void testCreation() {
		// 数据集合长度为 0 时也能正常生成；
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(0);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);
		HashSortingMerkleTree merkleTree = buildMerkleTree(datas);
		HashDigest rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(0, merkleTree.getTotalKeys());
		assertEquals(0, merkleTree.getTotalRecords());

		MerkleProof proof = merkleTree.getProof("KEY_NOT_EXIST");
		assertNull(proof);

		// 数据集合长度为 1 时也能正常生成；
		dataList = generateDatas(1);
		datas = toArray(dataList);
		merkleTree = buildMerkleTree(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(1, merkleTree.getTotalKeys());
		assertEquals(1, merkleTree.getTotalRecords());

		testMerkleProof(datas[0], merkleTree, 3);

		// 数据集合长度为 2 时也能正常生成；
		dataList = generateDatas(2);
		datas = toArray(dataList);
		merkleTree = buildMerkleTree(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(2, merkleTree.getTotalKeys());
		assertEquals(2, merkleTree.getTotalRecords());

		testMerkleProof(datas[0], merkleTree, 3);
		testMerkleProof(datas[1], merkleTree, 3);

		// 数据集合长度为 100 时也能正常生成；
		dataList = generateDatas(100);
		datas = toArray(dataList);
		merkleTree = buildMerkleTree(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(100, merkleTree.getTotalKeys());
		assertEquals(100, merkleTree.getTotalRecords());

		// 数据集合长度为 1024 时也能正常生成；
		int count = 1024;
		dataList = generateDatas(count);
		datas = toArray(dataList);
		merkleTree = buildMerkleTree(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(count, merkleTree.getTotalKeys());
		assertEquals(count, merkleTree.getTotalRecords());

//		merkleTree.print();
		for (VersioningKVData<String, byte[]> data : datas) {
			testMerkleProof(data, merkleTree, -1);
		}
		testMerkleProof1024(datas, merkleTree);

		// 数据集合长度为 20000 时也能正常生成；
		count = 20000;
		dataList = generateDatas(count);
		datas = toArray(dataList);
		merkleTree = buildMerkleTree(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(count, merkleTree.getTotalKeys());
		assertEquals(count, merkleTree.getTotalRecords());

//		merkleTree.print();
		for (VersioningKVData<String, byte[]> data : datas) {
			testMerkleProof(data, merkleTree, -1);
		}
	}

	/**
	 * 测试浏览默克尔树的所有数据节点；
	 */
	@Test
	public void testDataIterator() {
		int count = 1024;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);
		HashSortingMerkleTree merkleTree = buildMerkleTree(datas);
		HashDigest rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(count, merkleTree.getTotalKeys());
		assertEquals(count, merkleTree.getTotalRecords());

		Map<String, VersioningKVData<String, byte[]>> dataMap = new HashMap<String, VersioningKVData<String, byte[]>>();
		for (VersioningKVData<String, byte[]> data : datas) {
			dataMap.put(data.getKey(), data);
		}

		Iterator<MerkleData> dataIterator = merkleTree.iterator();
		String[] dataKeys = new String[count];
		int index = 0;
		while (dataIterator.hasNext()) {
			MerkleData data = dataIterator.next();
			assertNotNull(data);
			String key = BytesUtils.toString(data.getKey());
			assertTrue(dataMap.containsKey(key));
			dataMap.remove(key);
			dataKeys[index] = key;
			index++;
		}
		assertEquals(0, dataMap.size());
		assertEquals(count, index);

		MerkleDataIterator skippingIterator = merkleTree.iterator();
		testDataIteratorSkipping(dataKeys, skippingIterator, 0);

		skippingIterator = merkleTree.iterator();
		testDataIteratorSkipping(dataKeys, skippingIterator, 1);

		skippingIterator = merkleTree.iterator();
		testDataIteratorSkipping(dataKeys, skippingIterator, 2);

		skippingIterator = merkleTree.iterator();
		testDataIteratorSkipping(dataKeys, skippingIterator, 16);

		skippingIterator = merkleTree.iterator();
		testDataIteratorSkipping(dataKeys, skippingIterator, 128);

		skippingIterator = merkleTree.iterator();
		testDataIteratorSkipping(dataKeys, skippingIterator, 1023);

		skippingIterator = merkleTree.iterator();
		testDataIteratorSkipping(dataKeys, skippingIterator, 1024);
	}

	/**
	 * 测试在已经持久化的默克尔树上追加新节点时，扩展已有路径节点的正确性；
	 */
	@Test
	public void testExtendPersistedPathNodes() {
		int count = 10240;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		CryptoSetting cryptoSetting = createCryptoSetting();
		MemoryKVStorage storage = new MemoryKVStorage();

		Bytes prefix = Bytes.fromString(LedgerTestUtils.LEDGER_KEY_PREFIX);

		HashSortingMerkleTree merkleTree = new HashSortingMerkleTree(cryptoSetting, prefix, storage);

		int firstBatch = 500;
		for (int i = 0; i < firstBatch; i++) {
			merkleTree.setData(datas[i].getKey(), datas[i].getVersion(), datas[i].getValue());
		}
		// 先提交；
		merkleTree.commit();

		// 加载默克尔树到新实例；
		HashDigest rootHash = merkleTree.getRootHash();
		HashSortingMerkleTree merkleTree1 = new HashSortingMerkleTree(rootHash, cryptoSetting, prefix, storage, false);
		for (int i = firstBatch; i < datas.length; i++) {
			merkleTree1.setData(datas[i].getKey(), datas[i].getVersion(), datas[i].getValue());
		}
		merkleTree1.commit();

		// 重新加载；未正确扩展路径节点时，部分已持久化的叶子节点有可能丢失，在重新加载默克尔树并进行检索时将发现此错误；
		rootHash = merkleTree1.getRootHash();
		HashSortingMerkleTree merkleTree2 = new HashSortingMerkleTree(rootHash, cryptoSetting, prefix, storage, false);

		for (int i = 0; i < datas.length; i++) {
			MerkleData data = merkleTree2.getData(datas[i].getKey());
			assertNotNull(data);
		}
	}

	@Test
	public void testSpecialUseCase_1() {
		CryptoSetting cryptoSetting = createCryptoSetting();
		MemoryKVStorage storage = new MemoryKVStorage();

		HashSortingMerkleTree merkleTree = new HashSortingMerkleTree(cryptoSetting, KEY_PREFIX, storage);

		byte[] key = Base58Utils.decode("j5sXmpcomtM2QMUNWeQWsF8bNFFnyeXoCjVAekEeLSscgY");
		byte[] value = BytesUtils.toBytes("Special Use-Case VALUE");
		long version = 0;

		merkleTree.setData(key, version, value);

		MerkleData mkdata = merkleTree.getData(key);

		assertNotNull(mkdata);

		merkleTree.commit();

		mkdata = merkleTree.getData(key);
		assertNotNull(mkdata);

		HashSortingMerkleTree merkleTreeReload = new HashSortingMerkleTree(merkleTree.getRootHash(), cryptoSetting,
				KEY_PREFIX, storage, false);

		mkdata = merkleTreeReload.getData(key);
		assertNotNull(mkdata);
	}

	private void testDataIteratorSkipping(String[] expectedKeys, MerkleDataIterator iterator, int skip) {
		int count = expectedKeys.length;
		int index = skip;
		iterator.skip(index);
		if (skip < count) {
			assertTrue(iterator.hasNext());
		} else {
			assertFalse(iterator.hasNext());
		}
		while (iterator.hasNext()) {
			MerkleData data = iterator.next();
			assertNotNull(data);
			String key = BytesUtils.toString(data.getKey());
			assertEquals(expectedKeys[index], key);
			index++;
		}
		assertEquals(count, index);
	}

	private void testMerkleProof1024(VersioningKVData<String, byte[]>[] datas, HashSortingMerkleTree merkleTree) {
		testMerkleProof(datas[28], merkleTree, 4);
		testMerkleProof(datas[103], merkleTree, 4);
		testMerkleProof(datas[637], merkleTree, 4);
		testMerkleProof(datas[505], merkleTree, 4);
		testMerkleProof(datas[93], merkleTree, 4);
		testMerkleProof(datas[773], merkleTree, 4);
		testMerkleProof(datas[163], merkleTree, 4);
		testMerkleProof(datas[5], merkleTree, 4);
		testMerkleProof(datas[815], merkleTree, 4);
		testMerkleProof(datas[89], merkleTree, 4);
		testMerkleProof(datas[854], merkleTree, 5);
		testMerkleProof(datas[146], merkleTree, 5);
		testMerkleProof(datas[606], merkleTree, 5);
		testMerkleProof(datas[1003], merkleTree, 5);
		testMerkleProof(datas[156], merkleTree, 5);
		testMerkleProof(datas[861], merkleTree, 5);
		testMerkleProof(datas[1018], merkleTree, 6);
		testMerkleProof(datas[260], merkleTree, 6);
		testMerkleProof(datas[770], merkleTree, 6);
		testMerkleProof(datas[626], merkleTree, 6);
		testMerkleProof(datas[182], merkleTree, 6);
		testMerkleProof(datas[200], merkleTree, 6);
		testMerkleProof(datas[995], merkleTree, 7);
		testMerkleProof(datas[583], merkleTree, 7);
		testMerkleProof(datas[898], merkleTree, 7);
		testMerkleProof(datas[244], merkleTree, 7);
		testMerkleProof(datas[275], merkleTree, 7);
		testMerkleProof(datas[69], merkleTree, 8);
		testMerkleProof(datas[560], merkleTree, 8);
	}

	private void testMerkleProof(VersioningKVData<String, byte[]> data, HashSortingMerkleTree merkleTree,
			int expectProofPathCount) {
		MerkleProof proof = merkleTree.getProof("KEY_NOT_EXIST");
		assertNull(proof);

		proof = merkleTree.getProof(data.getKey(), data.getVersion());
		assertNotNull(proof);
		HashDigest[] hashPaths = proof.getHashPaths();
		if (expectProofPathCount > 0) {
			assertEquals(expectProofPathCount, hashPaths.length);
		}
		HashDigest dataHash = SHA256_HASH_FUNC.hash(data.getValue());
		assertEquals(dataHash, proof.getDataHash());
		assertEquals(dataHash, hashPaths[hashPaths.length - 1]);

		HashDigest rootHash = merkleTree.getRootHash();
		assertEquals(rootHash, proof.getRootHash());
		assertEquals(rootHash, hashPaths[0]);
	}


	/**
	 * 对已存在的树进行重载，增加新的数据节点，通过重载树验证新节点是否添加成功，total keys 与total records 是否符合预期，新添加的数据节点Key随机产生
	 *
	 */
	@Test
	public void testReloadTreeAddRandomNewDataNode() {

		Random random = new Random();
		byte[] bytes = new byte[200];
		random.nextBytes(bytes);
		String newDataKey = bytes.toString();

		CryptoSetting cryptoSetting = createCryptoSetting();
		MemoryKVStorage storage = new MemoryKVStorage();

		int count = 1024;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		HashSortingMerkleTree merkleTree = newMerkleTree(datas, cryptoSetting, storage);
		HashDigest rootHash0 = merkleTree.getRootHash();
		assertNotNull(rootHash0);
		assertEquals(count, merkleTree.getTotalKeys());
		assertEquals(count, merkleTree.getTotalRecords());

		// reload and add one data item;
		HashSortingMerkleTree merkleTree_reload = new HashSortingMerkleTree(rootHash0, cryptoSetting, KEY_PREFIX,
				storage, false);
		assertEquals(count, merkleTree_reload.getTotalKeys());
		assertEquals(count, merkleTree_reload.getTotalRecords());
		assertEquals(rootHash0, merkleTree_reload.getRootHash());

		VersioningKVData<String, byte[]> data1025 = new VersioningKVData<String, byte[]>(newDataKey, 0,
				BytesUtils.toBytes("NEW-VALUE-1025-VERSION-0"));

		merkleTree_reload.setData(data1025.getKey(), data1025.getVersion(), data1025.getValue());
		merkleTree_reload.commit();
		HashDigest rootHash1 = merkleTree_reload.getRootHash();
		assertNotNull(rootHash1);
		assertNotEquals(rootHash0, rootHash1);

		MerkleData data1025_reload_0 = merkleTree_reload.getData(data1025.getKey(), 0);
		assertNotNull(data1025_reload_0);
		assertNull(data1025_reload_0.getPreviousEntryHash());

		MerkleData data0_reload_0 = merkleTree_reload.getData("KEY-0", 0);
		assertNotNull(data0_reload_0);
		assertNull(data0_reload_0.getPreviousEntryHash());

		System.out.println("mkl reload total keys = " + merkleTree_reload.getTotalKeys());
		assertEquals(count + 1, merkleTree_reload.getTotalKeys());

		HashSortingMerkleTree merkleTree_reload_1 = new HashSortingMerkleTree(rootHash1, cryptoSetting, KEY_PREFIX,
				storage, false);
		assertEquals(count + 1, merkleTree_reload_1.getTotalKeys());
		assertEquals(count + 1, merkleTree_reload_1.getTotalRecords());
		assertEquals(rootHash1, merkleTree_reload_1.getRootHash());

		HashDigest rootHash2 = merkleTree_reload_1.getRootHash();
		assertNotNull(rootHash2);
		assertNotEquals(rootHash0, rootHash2);

		MerkleData data1025_reload_1 = merkleTree_reload_1.getData(data1025.getKey(), 0);
		assertNotNull(data1025_reload_1);
		assertNull(data1025_reload_1.getPreviousEntryHash());

		MerkleData data0_reload_1 = merkleTree_reload_1.getData("KEY-0", 0);
		assertNotNull(data0_reload_1);
		assertNull(data0_reload_1.getPreviousEntryHash());

		System.out.println("mkl reload total keys = " + merkleTree_reload_1.getTotalKeys());
		assertEquals(count + 1, merkleTree_reload_1.getTotalKeys());
	}

	/**
	 * 对已存在的树进行重载，增加新的数据节点，通过重载树验证新节点是否添加成功，total keys 与total records 是否符合预期，新添加的数据节点Key具有一定的规律
	 *
	 */
	@Test
	public void testReloadTreeAddNewDataNode() {

		CryptoSetting cryptoSetting = createCryptoSetting();
		MemoryKVStorage storage = new MemoryKVStorage();

		int count = 1024;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		HashSortingMerkleTree merkleTree = newMerkleTree(datas, cryptoSetting, storage);
		HashDigest rootHash0 = merkleTree.getRootHash();
		assertNotNull(rootHash0);
		assertEquals(count, merkleTree.getTotalKeys());
		assertEquals(count, merkleTree.getTotalRecords());

		// reload and add one data item;
		HashSortingMerkleTree merkleTree_reload = new HashSortingMerkleTree(rootHash0, cryptoSetting, KEY_PREFIX,
				storage, false);
		assertEquals(count, merkleTree_reload.getTotalKeys());
		assertEquals(count, merkleTree_reload.getTotalRecords());
		assertEquals(rootHash0, merkleTree_reload.getRootHash());

		VersioningKVData<String, byte[]> data1025 = new VersioningKVData<String, byte[]>("KEY-1025", 0,
				BytesUtils.toBytes("NEW-VALUE-1025-VERSION-0"));

		merkleTree_reload.setData(data1025.getKey(), data1025.getVersion(), data1025.getValue());
		merkleTree_reload.commit();
		HashDigest rootHash1 = merkleTree_reload.getRootHash();
		assertNotNull(rootHash1);
		assertNotEquals(rootHash0, rootHash1);

		MerkleData data1025_reload_0 = merkleTree_reload.getData(data1025.getKey(), 0);
		assertNotNull(data1025_reload_0);
		assertNull(data1025_reload_0.getPreviousEntryHash());

		MerkleData data0_reload_0 = merkleTree_reload.getData("KEY-0", 0);
		assertNotNull(data0_reload_0);
		assertNull(data0_reload_0.getPreviousEntryHash());

		System.out.println("mkl reload total keys = " + merkleTree_reload.getTotalKeys());
		assertEquals(count + 1, merkleTree_reload.getTotalKeys());

		HashSortingMerkleTree merkleTree_reload_1 = new HashSortingMerkleTree(rootHash1, cryptoSetting, KEY_PREFIX,
				storage, false);
		assertEquals(count + 1, merkleTree_reload_1.getTotalKeys());
		assertEquals(count + 1, merkleTree_reload_1.getTotalRecords());
		assertEquals(rootHash1, merkleTree_reload_1.getRootHash());

		HashDigest rootHash2 = merkleTree_reload_1.getRootHash();
		assertNotNull(rootHash2);
		assertNotEquals(rootHash0, rootHash2);

		MerkleData data1025_reload_1 = merkleTree_reload_1.getData(data1025.getKey(), 0);
		assertNotNull(data1025_reload_1);
		assertNull(data1025_reload_1.getPreviousEntryHash());

		MerkleData data0_reload_1 = merkleTree_reload_1.getData("KEY-0", 0);
		assertNotNull(data0_reload_1);
		assertNull(data0_reload_1.getPreviousEntryHash());

		System.out.println("mkl reload total keys = " + merkleTree_reload_1.getTotalKeys());
		assertEquals(count + 1, merkleTree_reload_1.getTotalKeys());
	}

	/**
	 * 测试树的加载读取；
	 */
	@Test
	public void testReloading() {
		CryptoSetting cryptoSetting = createCryptoSetting();
		MemoryKVStorage storage = new MemoryKVStorage();

		// 数据集合长度为 100 时也能正常生成；
		int count = 1024;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		HashSortingMerkleTree merkleTree = newMerkleTree(datas, cryptoSetting, storage);
		HashDigest rootHash0 = merkleTree.getRootHash();
		assertNotNull(rootHash0);

		assertEquals(count, merkleTree.getTotalKeys());
		assertEquals(count, merkleTree.getTotalRecords());

		for (VersioningKVData<String, byte[]> data : datas) {
			testMerkleProof(data, merkleTree, -1);
		}
		testMerkleProof1024(datas, merkleTree);

		HashSortingMerkleTree merkleTree_reload = new HashSortingMerkleTree(rootHash0, cryptoSetting, KEY_PREFIX,
				storage, false);
		assertEquals(count, merkleTree_reload.getTotalKeys());
		assertEquals(count, merkleTree_reload.getTotalRecords());

		testMerkleProof1024(datas, merkleTree_reload);

		VersioningKVData<String, byte[]> data28 = new VersioningKVData<String, byte[]>("KEY-28", 1,
				BytesUtils.toBytes("NEW-VALUE-VERSION-1"));
		VersioningKVData<String, byte[]> data606 = new VersioningKVData<String, byte[]>("KEY-606", 1,
				BytesUtils.toBytes("NEW-VALUE-VERSION-1"));
		VersioningKVData<String, byte[]> data770 = new VersioningKVData<String, byte[]>("KEY-770", 1,
				BytesUtils.toBytes("NEW-VALUE-VERSION-1"));
		VersioningKVData<String, byte[]> data898 = new VersioningKVData<String, byte[]>("KEY-898", 1,
				BytesUtils.toBytes("NEW-VALUE-VERSION-1"));
		VersioningKVData<String, byte[]> data69 = new VersioningKVData<String, byte[]>("KEY-69", 1,
				BytesUtils.toBytes("NEW-VALUE-VERSION-1"));

		merkleTree_reload.setData(data28.getKey(), data28.getVersion(), data28.getValue());
		merkleTree_reload.setData(data606.getKey(), data606.getVersion(), data606.getValue());
		merkleTree_reload.setData(data770.getKey(), data770.getVersion(), data770.getValue());
		merkleTree_reload.setData(data898.getKey(), data898.getVersion(), data898.getValue());
		merkleTree_reload.setData(data69.getKey(), data69.getVersion(), data69.getValue());

		merkleTree_reload.commit();
		HashDigest rootHash1 = merkleTree_reload.getRootHash();
		assertNotNull(rootHash1);
		assertNotEquals(rootHash0, rootHash1);

		MerkleProof proof = merkleTree_reload.getProof(data28.getKey(), 1);
		HashDigest[] hashPaths = proof.getHashPaths();
		assertEquals(4, hashPaths.length);
		proof = merkleTree_reload.getProof(data28.getKey(), 0);
		hashPaths = proof.getHashPaths();
		assertEquals(5, hashPaths.length);

		MerkleData data28_reload_0 = merkleTree_reload.getData(data28.getKey(), 0);
		assertNotNull(data28_reload_0);
		assertNull(data28_reload_0.getPreviousEntryHash());
		assertEquals(data28.getKey(), BytesUtils.toString(data28_reload_0.getKey()));
		assertEquals(datas[28].getVersion(), data28_reload_0.getVersion());
		assertEquals(SHA256_HASH_FUNC.hash(datas[28].getValue()), data28_reload_0.getValueHash());

		MerkleData data28_reload_1 = merkleTree_reload.getData(data28.getKey(), 1);
		assertNotNull(data28_reload_1);
		assertNotNull(data28_reload_1.getPreviousEntryHash());
		assertEquals(data28.getKey(), BytesUtils.toString(data28_reload_1.getKey()));
		assertEquals(data28.getVersion(), data28_reload_1.getVersion());
		assertEquals(SHA256_HASH_FUNC.hash(data28.getValue()), data28_reload_1.getValueHash());

//		merkleTree_reload.print();

		// 测试不同根哈希加载的默克尔树能够检索的最新版本；
		HashSortingMerkleTree merkleTree_0 = new HashSortingMerkleTree(rootHash0, cryptoSetting, KEY_PREFIX, storage,
				false);
		HashSortingMerkleTree merkleTree_1 = new HashSortingMerkleTree(rootHash1, cryptoSetting, KEY_PREFIX, storage,
				false);
		MerkleData data28_reload = merkleTree_0.getData(data28.getKey());
		assertEquals(0, data28_reload.getVersion());
		data28_reload = merkleTree_1.getData(data28.getKey());
		assertEquals(1, data28_reload.getVersion());

		// 测试在修改状态下检索默克尔证明的正确性；
		VersioningKVData<String, byte[]> data28_2 = new VersioningKVData<String, byte[]>("KEY-28", 2,
				BytesUtils.toBytes("NEW-VALUE-VERSION-2"));

		MerkleProof proof28_1 = merkleTree_1.getProof("KEY-28", 1);
		MerkleProof proof606_1 = merkleTree_1.getProof("KEY-606", 1);
		assertNotNull(proof28_1);
		assertNotNull(proof606_1);

		// 针对编号为 28 的数据加入一条新版本记录；
		merkleTree_1.setData(data28_2.getKey(), data28_2.getVersion(), data28_2.getValue());

		// 对于修改中的数据项，其默克尔证明为 null；此外，可以获得未修改的数据项的默克尔证明；
		MerkleProof proof28_2 = merkleTree_1.getProof("KEY-28", 1);
		MerkleProof proof606_2 = merkleTree_1.getProof("KEY-606", 1);
		assertNull(proof28_2);
		assertNotNull(proof606_2);
		assertEquals(proof606_1, proof606_2);

		// 当默克尔树提交修改之后，可以重新查找数据项； 
		merkleTree_1.commit();
		MerkleProof proof28_3 = merkleTree_1.getProof("KEY-28", 1);
		MerkleProof proof606_3 = merkleTree_1.getProof("KEY-606", 1);
		assertNotNull(proof28_3);
		assertNotNull(proof606_3);
		// 由于默克尔树发生了修改，所有默克尔证明发生了改变；
		assertFalse(proof28_1.equals(proof28_3));
		assertFalse(proof606_1.equals(proof606_3));

		// 验证默克尔证明的长度增长；
		MerkleProof proof28_5 = merkleTree_1.getProof("KEY-28", 0);
		assertNotNull(proof28_5);
		hashPaths = proof28_5.getHashPaths();
		assertEquals(6, hashPaths.length);

		// 重新加载默克尔树，默克尔证明是一致的；
		HashSortingMerkleTree merkleTree_1_1 = new HashSortingMerkleTree(rootHash1, cryptoSetting, KEY_PREFIX, storage,
				false);
		MerkleProof proof28_4 = merkleTree_1_1.getProof("KEY-28", 1);
		assertNotNull(proof28_4);
		assertEquals(proof28_1, proof28_4);

//		merkleTree_1.print();
	}

	/**
	 * 测试 Merkle 根哈希的不变性，即相同的数据集合得到相同的默克尔树根哈希，与数据加入的先后顺序无关；
	 */
	@Test
	public void testInvariance() {
		// 长度为 0 的情况；
		int count = 0;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);
		HashDigest rootHash0_1 = buildMerkleRootHash(datas);
		HashDigest rootHash0_2 = buildMerkleRootHash(datas);
		assertNotNull(rootHash0_1);
		assertNotNull(rootHash0_2);
		assertEquals(rootHash0_1, rootHash0_2);

		// 长度为 1 的情况；
		count = 1;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);
		HashDigest rootHash1_1 = buildMerkleRootHash(datas);
		HashDigest rootHash1_2 = buildMerkleRootHash(datas);
		assertNotNull(rootHash1_1);
		assertNotNull(rootHash1_2);
		assertEquals(rootHash1_1, rootHash1_2);

		// 长度为 2 的情况；
		count = 2;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);
		VersioningKVData<String, byte[]>[] datas1 = toArray(dataList);
		datas1[0] = datas[1];
		datas1[1] = datas[0];

		HashDigest rootHash2_1 = buildMerkleRootHash(datas);
		HashDigest rootHash2_2 = buildMerkleRootHash(datas1);
		assertNotNull(rootHash2_1);
		assertNotNull(rootHash2_2);
		assertEquals(rootHash2_1, rootHash2_2);

		// 长度为 8 的情况；
		count = 8;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);
		datas1 = getRandomSortingCopy(datas);

		HashDigest rootHash_N1 = buildMerkleRootHash(datas);
		HashDigest rootHash_N2 = buildMerkleRootHash(datas1);
		assertNotNull(rootHash_N1);
		assertNotNull(rootHash_N2);
		assertEquals(rootHash_N1, rootHash_N2);

		// 长度为 16 的情况；
		count = 16;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);
		datas1 = getRandomSortingCopy(datas);

		rootHash_N1 = buildMerkleRootHash(datas);
		rootHash_N2 = buildMerkleRootHash(datas1);
		assertNotNull(rootHash_N1);
		assertNotNull(rootHash_N2);
		assertEquals(rootHash_N1, rootHash_N2);

		// 长度为 32 的情况；
		count = 32;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);
		datas1 = getRandomSortingCopy(datas);

		rootHash_N1 = buildMerkleRootHash(datas);
		rootHash_N2 = buildMerkleRootHash(datas1);
		assertNotNull(rootHash_N1);
		assertNotNull(rootHash_N2);
		assertEquals(rootHash_N1, rootHash_N2);

		// 长度为 1025 的情况；
		count = 1025;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);
		datas1 = getRandomSortingCopy(datas);

		rootHash_N1 = buildMerkleRootHash(datas);
		rootHash_N2 = buildMerkleRootHash(datas1);
		assertNotNull(rootHash_N1);
		assertNotNull(rootHash_N2);
		assertEquals(rootHash_N1, rootHash_N2);
	}

	private List<VersioningKVData<String, byte[]>> generateDatas(int count) {
		List<VersioningKVData<String, byte[]>> dataList = new ArrayList<VersioningKVData<String, byte[]>>();
		for (int i = 0; i < count; i++) {
			VersioningKVData<String, byte[]> data = new VersioningKVData<String, byte[]>("KEY-" + i, 0L,
					BytesUtils.concat(BytesUtils.toBytes(i), BytesUtils.toBytes("VALUE")));
			dataList.add(data);
		}
		return dataList;
	}

	private static VersioningKVData<String, byte[]>[] toArray(List<VersioningKVData<String, byte[]>> dataList) {
		@SuppressWarnings("unchecked")
		VersioningKVData<String, byte[]>[] datas = (VersioningKVData<String, byte[]>[]) Array
				.newInstance(VersioningKVData.class, dataList.size());
		dataList.toArray(datas);
		return datas;
	}

	private VersioningKVData<String, byte[]>[] getRandomSortingCopy(VersioningKVData<String, byte[]>[] origDatas) {
		VersioningKVData<String, byte[]>[] datas = Arrays.copyOf(origDatas, origDatas.length);

		Random rand = new Random();
		VersioningKVData<String, byte[]> t;
		int c = datas.length * 2;
		for (int i = 0; i < c; i++) {
			int x = rand.nextInt(datas.length);
			int y = rand.nextInt(datas.length);
			t = datas[x];
			datas[x] = datas[y];
			datas[y] = t;
		}

//		System.out.printf("[");
//		for (int i = 0; i < datas.length; i++) {
//			System.out.printf("%s, ", datas[i].getKey());
//		}
//		System.out.printf("]\r\n");
		return datas;
	}

	private CryptoSetting createCryptoSetting() {
		CryptoSetting cryptoSetting = Mockito.mock(CryptoSetting.class);
		when(cryptoSetting.getAutoVerifyHash()).thenReturn(true);
		when(cryptoSetting.getHashAlgorithm()).thenReturn(ClassicAlgorithm.SHA256.code());
		return cryptoSetting;
	}

	private HashSortingMerkleTree buildMerkleTree(VersioningKVData<String, byte[]>[] datas) {
		CryptoSetting cryptoSetting = createCryptoSetting();
		MemoryKVStorage storage = new MemoryKVStorage();

		return newMerkleTree(datas, cryptoSetting, storage, true);
	}

	private HashSortingMerkleTree buildMerkleTree(VersioningKVData<String, byte[]>[] datas, boolean commit) {
		CryptoSetting cryptoSetting = createCryptoSetting();
		MemoryKVStorage storage = new MemoryKVStorage();

		return newMerkleTree(datas, cryptoSetting, storage, commit);
	}

	private HashSortingMerkleTree newMerkleTree(VersioningKVData<String, byte[]>[] datas, CryptoSetting cryptoSetting,
			ExPolicyKVStorage storage) {
		return newMerkleTree(datas, cryptoSetting, storage, true);
	}

	private HashSortingMerkleTree newMerkleTree(VersioningKVData<String, byte[]>[] datas, CryptoSetting cryptoSetting,
			ExPolicyKVStorage storage, boolean commit) {
		HashSortingMerkleTree merkleTree = new HashSortingMerkleTree(null, cryptoSetting, KEY_PREFIX, storage, false);
		assertTrue(merkleTree.isUpdated());

		for (int i = 0; i < datas.length; i++) {
			merkleTree.setData(datas[i].getKey(), datas[i].getVersion(), datas[i].getValue());
		}

		assertTrue(merkleTree.isUpdated());

		if (commit) {
			merkleTree.commit();
			assertFalse(merkleTree.isUpdated());
		}

//		merkleTree.print();
//		System.out.println("---- end tree ----\r\n");
		return merkleTree;
	}

	private HashDigest buildMerkleRootHash(VersioningKVData<String, byte[]>[] datas) {
		HashSortingMerkleTree merkleTree = buildMerkleTree(datas);
		HashDigest rootHash = merkleTree.getRootHash();

		return rootHash;
	}

}

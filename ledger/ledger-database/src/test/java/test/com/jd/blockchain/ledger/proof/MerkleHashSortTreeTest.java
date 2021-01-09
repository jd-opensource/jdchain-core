package test.com.jd.blockchain.ledger.proof;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Array;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.MerkleProofLevel;
import com.jd.blockchain.ledger.MerkleProofVerifier;
import com.jd.blockchain.ledger.merkletree.BytesKeyValue;
import com.jd.blockchain.ledger.merkletree.HashBucketEntry;
import com.jd.blockchain.ledger.merkletree.HashEntry;
import com.jd.blockchain.ledger.merkletree.KVEntry;
import com.jd.blockchain.ledger.merkletree.MerkleHashBucket;
import com.jd.blockchain.ledger.merkletree.MerkleHashSortTree;
import com.jd.blockchain.ledger.merkletree.MerkleTree;
import com.jd.blockchain.ledger.merkletree.MerkleValue;
import com.jd.blockchain.ledger.merkletree.TreeDegree;
import com.jd.blockchain.ledger.merkletree.TreeOptions;
import com.jd.blockchain.ledger.proof.KeyIndexer;
import com.jd.blockchain.ledger.proof.MerkleTrieData;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.utils.MemoryKVStorage;
import com.jd.blockchain.storage.service.utils.VersioningKVData;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.SkippingIterator;
import com.jd.blockchain.utils.codec.Base58Utils;
import com.jd.blockchain.utils.io.BytesUtils;
import com.jd.blockchain.utils.security.RandomUtils;

import test.com.jd.blockchain.ledger.core.LedgerTestUtils;

public class MerkleHashSortTreeTest {

	private static final Bytes KEY_PREFIX = Bytes.fromString("/MerkleTree");

	private static final HashFunction SHA256_HASH_FUNC = Crypto.getHashFunction(ClassicAlgorithm.SHA256);

	/**
	 * 测试加入存在哈希冲突(基于 {@link MerkleHashSortTree#MURMUR3_HASH_POLICY}
	 * 哈希策略)的数据时是否能够正确处理；<br>
	 * 
	 */
	@Test
	public void testAddDataWithHashConfliction() {
		// 8 位哈希最多有 256 个值，设置数据的总数大于 256 必然产生哈希冲突，可以验证哈希桶处理多个 key 的情况；
		int count = 300;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		TreeOptions treeOption = createTreeOptions();
		MemoryKVStorage storage = new MemoryKVStorage();
		MerkleHashSortTree merkleTree = new HashTreeIn8Bits(treeOption, KEY_PREFIX, storage);

		setDatas(merkleTree, datas);

		assertDataExist(merkleTree, datas);

		// 未提交之前查不到信息；
		assertNull(merkleTree.getRootHash());
		assertEquals(0, merkleTree.getTotalKeys());

		KVEntry dt = merkleTree.getData("KEY-69");
		assertNotNull(dt);
		assertEquals(0, dt.getVersion());
		dt = merkleTree.getData("KEY-69", 0);
		assertNotNull(dt);
		assertEquals(0, dt.getVersion());

		dt = merkleTree.getData("KEY-69", 1);
		assertNull(dt);

		// 提交；
		merkleTree.commit();

		assertEquals(count, merkleTree.getTotalKeys());

		// 重新加载；
		HashDigest rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);

		merkleTree = new HashTreeIn8Bits(rootHash, treeOption, KEY_PREFIX, storage);

		assertEquals(count, merkleTree.getTotalKeys());
		assertDataExist(merkleTree, datas);

		// 验证迭代器；
		SkippingIterator<KVEntry> kvIter = merkleTree.iterator();
		assertDataEquals(datas, kvIter);

		// 验证增加不同版本；
		{
			VersioningKVData<String, byte[]> newData = createNewVersion(datas, 2);
			VersioningKVData<String, byte[]> newData1 = createNewVersion(datas, 26);
			VersioningKVData<String, byte[]> newData2 = createNewVersion(datas, 8);
			VersioningKVData<String, byte[]> newData3 = createNewVersion(datas, 2);

			merkleTree.setData(newData.getKey(), newData.getVersion(), newData.getValue());
			merkleTree.setData(newData1.getKey(), newData1.getVersion(), newData1.getValue());
			merkleTree.setData(newData2.getKey(), newData2.getVersion(), newData2.getValue());
			merkleTree.setData(newData3.getKey(), newData3.getVersion(), newData3.getValue());

			// 验证最新数据；
			assertDataExist(merkleTree, datas);

			// 迭代器返回不包含未提交的数据；
			// kvIter = merkleTree.iterator();
			// assertDataEquals(datas, kvIter);

			merkleTree.commit();

			// 验证提交之后的数据；
			kvIter = merkleTree.iterator();
			assertDataEquals(datas, kvIter);
		}

	}

	private VersioningKVData<String, byte[]> createNewVersion(VersioningKVData<String, byte[]>[] datas, int newIndex) {
		Random rand = new Random();
		byte[] bytes = new byte[16];
		rand.nextBytes(bytes);
		VersioningKVData<String, byte[]> newData = new VersioningKVData<String, byte[]>(datas[newIndex].getKey(),
				datas[newIndex].getVersion() + 1, bytes);

		datas[newIndex] = newData;
		return newData;
	}

	private void assertDataEquals(VersioningKVData<String, byte[]>[] datas, SkippingIterator<KVEntry> kvIter) {
		Map<String, VersioningKVData<String, byte[]>> dataMap = new HashMap<>();
		for (VersioningKVData<String, byte[]> dt : datas) {
			dataMap.put(dt.getKey(), dt);
		}

		assertEquals(datas.length, kvIter.getTotalCount());

		Set<String> keyReadSet = new HashSet<String>();
		while (kvIter.hasNext()) {
			KVEntry kvEntry = (KVEntry) kvIter.next();

			String strKey = kvEntry.getKey().toUTF8String();

			assertFalse(keyReadSet.contains(strKey));
			assertTrue(dataMap.containsKey(strKey));

			VersioningKVData<String, byte[]> dt = dataMap.get(strKey);
			assertEquals(dt.getVersion(), kvEntry.getVersion());
			assertArrayEquals(dt.getValue(), kvEntry.getValue().toBytes());

			keyReadSet.add(strKey);
		}

	}

	/**
	 * 采用 8 位的哈希值获得更多的哈希冲突，验证在哈希冲突的情况下默克尔树是否正常处理；
	 * 
	 * @author huanghaiquan
	 *
	 */
	private static class HashTreeIn8Bits extends MerkleHashSortTree {

		public HashTreeIn8Bits(TreeOptions options, Bytes prefix, ExPolicyKVStorage kvStorage) {
			super(options, prefix, kvStorage);
		}

		public HashTreeIn8Bits(HashDigest rootHash, TreeOptions options, Bytes prefix, ExPolicyKVStorage kvStorage) {
			super(rootHash, options, prefix, kvStorage);
		}

		@Override
		protected long hashKey(byte[] key) {
			return super.hashKey(key) & 0xFFL;
		}
	}

	/**
	 * 
	 */
	@Test
	public void testHashBucket() {
		byte[][] keys = new byte[4][];
		byte[][] values = new byte[4][];

		for (int i = 0; i < keys.length; i++) {
			keys[i] = BytesUtils.toBytes("KEY-" + i);
			values[i] = RandomUtils.generateRandomBytes(16);
		}

		TreeOptions treeOptions = TreeOptions.build().setDefaultHashAlgorithm(ClassicAlgorithm.SHA256.code());
		Bytes bucketPrefix = Bytes.fromString("BUCKET");
		MemoryKVStorage kvStorage = new MemoryKVStorage();
		MerkleHashBucket hashBucket = new MerkleHashBucket(100, keys[0], values[0], TreeDegree.D3, treeOptions,
				bucketPrefix, kvStorage);

		// 验证初始化之后的数据是否正确；
		assertEquals(1, hashBucket.getKeysCount());

		MerkleValue<byte[]> value = hashBucket.getValue(keys[0]);
		assertNotNull(value);
		assertEquals(0, value.getId());
		assertArrayEquals(values[0], value.getValue());
		assertEquals(0, hashBucket.getVersion(keys[0]));

		MerkleValue<byte[]> value_v1 = hashBucket.getValue(keys[0], 1);
		assertNull(value_v1);

		MerkleValue<byte[]> value1_v1 = hashBucket.getValue(keys[1], 0);
		assertNull(value1_v1);

		// 提交数据；
		hashBucket.commit();

		// 模拟对默尔克哈希桶的存储；
		byte[] bucketBytes = BinaryProtocol.encode(hashBucket, HashBucketEntry.class);
		HashBucketEntry bucketEntry = BinaryProtocol.decode(bucketBytes);

		// 重新加载；
		hashBucket = new MerkleHashBucket(100, bucketEntry.getKeySet(), TreeDegree.D3, treeOptions, bucketPrefix,
				kvStorage);
		// 验证重新加载之后的数据正确性；
		value = hashBucket.getValue(keys[0]);
		assertNotNull(value);
		assertEquals(0, value.getId());
		assertArrayEquals(values[0], value.getValue());
		assertEquals(0, hashBucket.getVersion(keys[0]));

		assertEquals(1, hashBucket.getKeysCount());

		SkippingIterator<MerkleValue<HashEntry>> keysIterator = hashBucket.iterator();
		assertEquals(1, keysIterator.getTotalCount());
		assertTrue(keysIterator.hasNext());

		MerkleValue<HashEntry> entry = keysIterator.next();
		assertNotNull(entry);
		assertTrue(entry.getValue() instanceof BytesKeyValue);
		BytesKeyValue kv = (BytesKeyValue) entry.getValue();
		assertArrayEquals(keys[0], kv.getKey().toBytes());
		assertArrayEquals(values[0], kv.getValue().toBytes());

		// 验证加入新的键；
		for (int i = 1; i < keys.length; i++) {
			hashBucket.setValue(keys[i], 0, values[i]);
		}

		assertEquals(keys.length, hashBucket.getKeysCount());
		for (int i = 0; i < keys.length; i++) {
			value = hashBucket.getValue(keys[i]);
			assertNotNull(value);
			// id 即版本；
			assertEquals(0, value.getId());
			assertArrayEquals(values[i], value.getValue());
			assertEquals(0, hashBucket.getVersion(keys[i]));
		}

		hashBucket.commit();

		// 重新加载并验证数据；
		bucketBytes = BinaryProtocol.encode(hashBucket, HashBucketEntry.class);
		bucketEntry = BinaryProtocol.decode(bucketBytes);

		hashBucket = new MerkleHashBucket(100, bucketEntry.getKeySet(), TreeDegree.D3, treeOptions, bucketPrefix,
				kvStorage);
		assertEquals(keys.length, hashBucket.getKeysCount());
		for (int i = 0; i < keys.length; i++) {
			value = hashBucket.getValue(keys[i]);
			assertNotNull(value);
			// id 即版本；
			assertEquals(0, value.getId());
			assertArrayEquals(values[i], value.getValue());
			assertEquals(0, hashBucket.getVersion(keys[i]));
		}
	}

	/**
	 * 验证 HashSortingMerkleTree 在未提交之前的总数和根哈希维持不变的特性，新增的数据记录可读，但是具有临时性，一旦回滚则被清除；
	 */
	@Test
	public void testReadUncommitting() {
		// 数据集合长度为 1024 时也能正常生成；
		int count = 100;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		TreeOptions treeOption = createTreeOptions();
		MemoryKVStorage storage = new MemoryKVStorage();
		MerkleHashSortTree merkleTree = new MerkleHashSortTree(treeOption, KEY_PREFIX, storage);

		setDatas(merkleTree, datas);

		assertDataExist(merkleTree, datas);

		// 未提交之前查不到信息；
		assertNull(merkleTree.getRootHash());
		assertEquals(0, merkleTree.getTotalKeys());

		KVEntry dt = merkleTree.getData("KEY-69");
		assertNotNull(dt);
		assertEquals(0, dt.getVersion());
		dt = merkleTree.getData("KEY-69", 0);
		assertNotNull(dt);
		assertEquals(0, dt.getVersion());

		dt = merkleTree.getData("KEY-69", 1);
		assertNull(dt);

		// 提交；
		merkleTree.commit();

		// 重新加载；
		HashDigest rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);

		merkleTree = new MerkleHashSortTree(rootHash, treeOption, KEY_PREFIX, storage);

		// 测试写入数据的多版本；
		VersioningKVData<String, byte[]> data69 = new VersioningKVData<String, byte[]>("KEY-69", 1,
				BytesUtils.toBytes("NEW-VALUE-VERSION-1"));
		merkleTree.setData(data69.getKey(), data69.getVersion(), data69.getValue());

		dt = merkleTree.getData("KEY-69", 1);
		assertNotNull(dt);
		assertEquals(1, dt.getVersion());

		merkleTree.commit();

		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		// 预期键的总数不变；
		assertEquals(count, merkleTree.getTotalKeys());

		dt = merkleTree.getData("KEY-69");
		assertNotNull(dt);
		assertEquals(1, dt.getVersion());
		dt = merkleTree.getData("KEY-69", 0);
		assertNotNull(dt);
		assertEquals(0, dt.getVersion());
		dt = merkleTree.getData("KEY-69", 1);
		assertNotNull(dt);
		assertEquals(1, dt.getVersion());

		// 整体地验证数据的存在性；
		datas[69] = data69;
		assertDataExist(merkleTree, datas);
	}

	private void assertDataExist(MerkleHashSortTree merkleTree, VersioningKVData<String, byte[]>[] datas) {
		for (int i = 0; i < datas.length; i++) {
			KVEntry kv = merkleTree.getData(datas[i].getKey());
			assertNotNull(kv);
			assertEquals(datas[i].getKey(), kv.getKey().toString("UTF-8"));
			assertEquals(datas[i].getVersion(), kv.getVersion());
			assertArrayEquals(datas[i].getValue(), kv.getValue().toBytes());
		}
	}

	/**
	 * 断言指定的默克尔树不存在指定键的数据，或者不存在指定键的指定版本的数据；
	 * 
	 * @param merkleTree
	 * @param datas
	 */
	private void assertDataNotExist(MerkleHashSortTree merkleTree, VersioningKVData<String, byte[]>[] datas) {
		for (int i = 0; i < datas.length; i++) {
			KVEntry kv = merkleTree.getData(datas[i].getKey());
			// 数据不存在，或者指定版本的数据不存在；
			assertTrue(kv == null || kv.getVersion() < datas[i].getVersion());
		}
	}

	/**
	 * 验证 HashSortingMerkleTree 在未提交之前，新增的数据记录可读和可回滚特性；
	 */
	@Test
	public void testCancel() {
		// 数据集合长度为 1024 时也能正常生成；
		int count = 1024;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		MerkleHashSortTree merkleTree = newMerkleTree(datas);
		assertTrue(merkleTree.isUpdated());

		// 未提交之前查不到信息；
		assertNull(merkleTree.getRootHash());
		assertEquals(0, merkleTree.getTotalKeys());

		KVEntry dt = merkleTree.getData("KEY-69");
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
		dt = merkleTree.getData("KEY-69");
		assertNull(dt);
		dt = merkleTree.getData("KEY-69", 0);
		assertNull(dt);
		dt = merkleTree.getData("KEY-69", 1);
		assertNull(dt);

		// 对于新创建的空的默克尔树的 updated 属性为 true，回滚之后仍然是空的，此时预期 updated 属性为 true；
		assertTrue(merkleTree.isUpdated());

		// 加入数据，提交后继续加入数据，验证能够正常回滚到上一次提交；
		setDatas(merkleTree, datas);

		merkleTree.commit();

		assertDataExist(merkleTree, datas);

		int count2 = 200;
		List<VersioningKVData<String, byte[]>> dataList2 = generateDatasFrom(count, count2);
		VersioningKVData<String, byte[]>[] datas2 = toArray(dataList2);

		setDatas(merkleTree, datas2);

		// 在回滚之前，验证全部的数据都存在；
		assertDataExist(merkleTree, datas);
		assertDataExist(merkleTree, datas2);

		// 回滚未提交的第二批数据；
		merkleTree.cancel();

		// 验证仅剩第一批数据；
		assertDataExist(merkleTree, datas);
		assertDataNotExist(merkleTree, datas2);
	}

	/**
	 * 测试树的创建的正确性；
	 */
	@Test
	public void testCreation() {
		// 数据集合长度为 0 时也能正常生成；
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(0);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);
		MerkleHashSortTree merkleTree = newMerkleTree_with_committed(datas);
		HashDigest rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(0, merkleTree.getTotalKeys());

		// TODO: 暂时注释掉默克尔证明相关的内容；
//		MerkleProof proof = merkleTree.getProof("KEY_NOT_EXIST");
//		assertNull(proof);

		// 数据集合长度为 1 时也能正常生成；
		dataList = generateDatas(1);
		datas = toArray(dataList);
		merkleTree = newMerkleTree_with_committed(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(1, merkleTree.getTotalKeys());

		// TODO: 暂时注释掉默克尔证明相关的内容；
		// 默克尔证明路径的长度至少为 4 ——包括：根节点/叶子节点/数据节点/值哈希；
//		assertMerkleProofAndProofLength(datas[0], merkleTree, 4);

		// 数据集合长度为 2 时也能正常生成；
		dataList = generateDatas(2);
		datas = toArray(dataList);
		merkleTree = newMerkleTree_with_committed(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(2, merkleTree.getTotalKeys());

		//TODO: 暂时注释掉默克尔证明相关的内容；
//		assertMerkleProofAndProofLength(datas[0], merkleTree, 4);
//		assertMerkleProofAndProofLength(datas[1], merkleTree, 4);

		// 数据集合长度为 100 时也能正常生成；
		dataList = generateDatas(100);
		datas = toArray(dataList);
		merkleTree = newMerkleTree_with_committed(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(100, merkleTree.getTotalKeys());

		// 数据集合长度为 1024 时也能正常生成；
		int count = 1024;
		dataList = generateDatas(count);
		datas = toArray(dataList);
		merkleTree = newMerkleTree_with_committed(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(count, merkleTree.getTotalKeys());

		//TODO: 暂时注释掉默克尔证明相关的内容；
//		merkleTree.print();
//		for (VersioningKVData<String, byte[]> data : datas) {
//			assertMerkleProof(data, merkleTree);
//		}
//		testMerkleProof1024(datas, merkleTree);

		// 数据集合长度为 20000 时也能正常生成；
		count = 20000;
		dataList = generateDatas(count);
		datas = toArray(dataList);
		merkleTree = newMerkleTree_with_committed(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(count, merkleTree.getTotalKeys());

		//TODO: 暂时注释掉默克尔证明相关的内容；
//		merkleTree.print();
//		for (VersioningKVData<String, byte[]> data : datas) {
//			assertMerkleProof(data, merkleTree);
//		}
	}

	/**
	 * 测试浏览默克尔树的所有数据节点；
	 */
	@Test
	public void testDataIterator() {
		int count = 1024;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);
		MerkleHashSortTree merkleTree = newMerkleTree_with_committed(datas);
		HashDigest rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		assertEquals(count, merkleTree.getTotalKeys());

		Map<String, VersioningKVData<String, byte[]>> dataMap = new HashMap<String, VersioningKVData<String, byte[]>>();
		for (VersioningKVData<String, byte[]> data : datas) {
			dataMap.put(data.getKey(), data);
		}

		Iterator<KVEntry> dataIterator = merkleTree.iterator();
		String[] dataKeys = new String[count];
		int index = 0;
		while (dataIterator.hasNext()) {
			KVEntry data = dataIterator.next();
			assertNotNull(data);
			String key = data.getKey().toUTF8String();
			assertTrue(dataMap.containsKey(key));
			dataMap.remove(key);
			dataKeys[index] = key;
			index++;
		}
		assertEquals(0, dataMap.size());
		assertEquals(count, index);

		SkippingIterator<KVEntry> skippingIterator = merkleTree.iterator();
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

		TreeOptions treeOption = createTreeOptions();
		MemoryKVStorage storage = new MemoryKVStorage();

		Bytes prefix = Bytes.fromString(LedgerTestUtils.LEDGER_KEY_PREFIX);

		MerkleHashSortTree merkleTree = new MerkleHashSortTree(treeOption, prefix, storage);

		int firstBatch = 500;
		for (int i = 0; i < firstBatch; i++) {
			merkleTree.setData(datas[i].getKey(), datas[i].getVersion(), datas[i].getValue());
		}
		// 先提交；
		merkleTree.commit();

		// 加载默克尔树到新实例；
		HashDigest rootHash = merkleTree.getRootHash();
		MerkleHashSortTree merkleTree1 = new MerkleHashSortTree(rootHash, treeOption, prefix, storage);
		for (int i = firstBatch; i < datas.length; i++) {
			merkleTree1.setData(datas[i].getKey(), datas[i].getVersion(), datas[i].getValue());
		}
		merkleTree1.commit();

		// 重新加载；未正确扩展路径节点时，部分已持久化的叶子节点有可能丢失，在重新加载默克尔树并进行检索时将发现此错误；
		rootHash = merkleTree1.getRootHash();
		MerkleHashSortTree merkleTree2 = new MerkleHashSortTree(rootHash, treeOption, prefix, storage);

		for (int i = 0; i < datas.length; i++) {
			KVEntry data = merkleTree2.getData(datas[i].getKey());
			assertNotNull(data);
		}
	}

	@Test
	public void testSpecialUseCase_1() {
		TreeOptions treeOptions = createTreeOptions();
		MemoryKVStorage storage = new MemoryKVStorage();

		MerkleHashSortTree merkleTree = new MerkleHashSortTree(treeOptions, KEY_PREFIX, storage);

		byte[] key = Base58Utils.decode("j5sXmpcomtM2QMUNWeQWsF8bNFFnyeXoCjVAekEeLSscgY");
		byte[] value = BytesUtils.toBytes("Special Use-Case VALUE");
		long version = 0;

		merkleTree.setData(key, version, value);

		KVEntry mkdata = merkleTree.getData(key);

		assertNotNull(mkdata);

		merkleTree.commit();

		mkdata = merkleTree.getData(key);
		assertNotNull(mkdata);

		MerkleTree merkleTreeReload = new MerkleHashSortTree(merkleTree.getRootHash(), treeOptions, KEY_PREFIX,
				storage);

		mkdata = merkleTreeReload.getData(key);
		assertNotNull(mkdata);
	}

	private void testDataIteratorSkipping(String[] expectedKeys, SkippingIterator<KVEntry> iterator, int skip) {
		int count = expectedKeys.length;
		int index = skip;
		iterator.skip(index);
		if (skip < count) {
			assertTrue(iterator.hasNext());
		} else {
			assertFalse(iterator.hasNext());
		}
		while (iterator.hasNext()) {
			KVEntry data = iterator.next();
			assertNotNull(data);
			String key = data.getKey().toUTF8String();
			assertEquals(expectedKeys[index], key);
			index++;
		}
		assertEquals(count, index);
	}

	/**
	 * 这是根据经过实测验证过的1024条固定的数据记录生成的校验代码；
	 * 
	 * @param datas
	 * @param merkleTree
	 */
	private void testMerkleProof1024(VersioningKVData<String, byte[]>[] datas, MerkleTree merkleTree) {
		assertMerkleProofAndProofLength(datas[28], merkleTree, 5);
		assertMerkleProofAndProofLength(datas[103], merkleTree, 5);
		assertMerkleProofAndProofLength(datas[637], merkleTree, 5);
		assertMerkleProofAndProofLength(datas[505], merkleTree, 5);
		assertMerkleProofAndProofLength(datas[93], merkleTree, 5);
		assertMerkleProofAndProofLength(datas[773], merkleTree, 5);
		assertMerkleProofAndProofLength(datas[163], merkleTree, 5);
		assertMerkleProofAndProofLength(datas[5], merkleTree, 5);
		assertMerkleProofAndProofLength(datas[815], merkleTree, 5);
		assertMerkleProofAndProofLength(datas[89], merkleTree, 5);
		assertMerkleProofAndProofLength(datas[854], merkleTree, 6);
		assertMerkleProofAndProofLength(datas[146], merkleTree, 6);
		assertMerkleProofAndProofLength(datas[606], merkleTree, 6);
		assertMerkleProofAndProofLength(datas[1003], merkleTree, 6);
		assertMerkleProofAndProofLength(datas[156], merkleTree, 6);
		assertMerkleProofAndProofLength(datas[861], merkleTree, 6);
		assertMerkleProofAndProofLength(datas[1018], merkleTree, 7);
		assertMerkleProofAndProofLength(datas[260], merkleTree, 7);
		assertMerkleProofAndProofLength(datas[770], merkleTree, 7);
		assertMerkleProofAndProofLength(datas[626], merkleTree, 7);
		assertMerkleProofAndProofLength(datas[182], merkleTree, 7);
		assertMerkleProofAndProofLength(datas[200], merkleTree, 7);
		assertMerkleProofAndProofLength(datas[995], merkleTree, 8);
		assertMerkleProofAndProofLength(datas[583], merkleTree, 8);
		assertMerkleProofAndProofLength(datas[898], merkleTree, 8);
		assertMerkleProofAndProofLength(datas[244], merkleTree, 8);
		assertMerkleProofAndProofLength(datas[275], merkleTree, 8);
		assertMerkleProofAndProofLength(datas[69], merkleTree, 9);
		assertMerkleProofAndProofLength(datas[560], merkleTree, 9);
	}

	private MerkleProof assertMerkleProof(VersioningKVData<String, byte[]> data, MerkleTree merkleTree) {
		MerkleProof proof_nx = merkleTree.getProof(BytesUtils.toBytes("KEY_NOT_EXIST"));
		assertNull(proof_nx);

		MerkleProof proof = merkleTree.getProof(BytesUtils.toBytes(data.getKey()), data.getVersion());
		assertNotNull(proof);

		HashDigest dataHash = SHA256_HASH_FUNC.hash(data.getValue());
		assertEquals(dataHash, proof.getDataHash());

		HashDigest rootHash = merkleTree.getRootHash();
		assertEquals(rootHash, proof.getRootHash());

		assertTrue(MerkleProofVerifier.verify(proof));

		return proof;
	}

	private void assertMerkleProofAndProofLength(VersioningKVData<String, byte[]> data, MerkleTree merkleTree,
			int expectProofPathCount) {
		MerkleProof proof = assertMerkleProof(data, merkleTree);

		assertEquals(expectProofPathCount, proof.getProofLevels().length);
	}

	/**
	 * 对已存在的树进行重载，增加新的数据节点，通过重载树验证新节点是否添加成功，total keys 与total records
	 * 是否符合预期，新添加的数据节点Key随机产生
	 *
	 */
	@Test
	public void testReloadTreeAddRandomNewDataNode() {

		Random random = new Random();
		byte[] bytes = new byte[200];
		random.nextBytes(bytes);
		String newDataKey = bytes.toString();

		TreeOptions treeOptions = createTreeOptions();
		MemoryKVStorage storage = new MemoryKVStorage();

		int count = 1024;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		MerkleHashSortTree merkleTree = newMerkleTree_with_committed(datas, treeOptions, storage);
		HashDigest rootHash0 = merkleTree.getRootHash();
		assertNotNull(rootHash0);
		assertEquals(count, merkleTree.getTotalKeys());

		// reload and add one data item;
		MerkleHashSortTree merkleTree_reload = new MerkleHashSortTree(rootHash0, treeOptions, KEY_PREFIX, storage);
		assertEquals(count, merkleTree_reload.getTotalKeys());
		assertEquals(rootHash0, merkleTree_reload.getRootHash());

		VersioningKVData<String, byte[]> data1025 = new VersioningKVData<String, byte[]>(newDataKey, 0,
				BytesUtils.toBytes("NEW-VALUE-1025-VERSION-0"));

		merkleTree_reload.setData(data1025.getKey(), data1025.getVersion(), data1025.getValue());
		merkleTree_reload.commit();
		HashDigest rootHash1 = merkleTree_reload.getRootHash();
		assertNotNull(rootHash1);
		assertNotEquals(rootHash0, rootHash1);

		KVEntry data1025_reload_0 = merkleTree_reload.getData(data1025.getKey(), 0);
		assertNotNull(data1025_reload_0);

		KVEntry data0_reload_0 = merkleTree_reload.getData("KEY-0", 0);
		assertNotNull(data0_reload_0);

		System.out.println("mkl reload total keys = " + merkleTree_reload.getTotalKeys());
		assertEquals(count + 1, merkleTree_reload.getTotalKeys());

		MerkleHashSortTree merkleTree_reload_1 = new MerkleHashSortTree(rootHash1, treeOptions, KEY_PREFIX, storage);
		assertEquals(count + 1, merkleTree_reload_1.getTotalKeys());
		assertEquals(rootHash1, merkleTree_reload_1.getRootHash());

		HashDigest rootHash2 = merkleTree_reload_1.getRootHash();
		assertNotNull(rootHash2);
		assertNotEquals(rootHash0, rootHash2);

		KVEntry data1025_reload_1 = merkleTree_reload_1.getData(data1025.getKey(), 0);
		assertNotNull(data1025_reload_1);

		KVEntry data0_reload_1 = merkleTree_reload_1.getData("KEY-0", 0);
		assertNotNull(data0_reload_1);

		System.out.println("mkl reload total keys = " + merkleTree_reload_1.getTotalKeys());
		assertEquals(count + 1, merkleTree_reload_1.getTotalKeys());
	}

	/**
	 * 对已存在的树进行重载，增加新的数据节点，通过重载树验证新节点是否添加成功，total keys 与total records
	 * 是否符合预期，新添加的数据节点Key具有一定的规律
	 *
	 */
	@Test
	public void testReloadTreeAddNewDataNode() {

		TreeOptions cryptoSetting = createTreeOptions();
		MemoryKVStorage storage = new MemoryKVStorage();

		int count = 1024;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		MerkleHashSortTree merkleTree = newMerkleTree_with_committed(datas, cryptoSetting, storage);
		HashDigest rootHash0 = merkleTree.getRootHash();
		assertNotNull(rootHash0);
		assertEquals(count, merkleTree.getTotalKeys());

		// reload and add one data item;
		MerkleHashSortTree merkleTree_reload = new MerkleHashSortTree(rootHash0, cryptoSetting, KEY_PREFIX, storage);
		assertEquals(count, merkleTree_reload.getTotalKeys());
		assertEquals(rootHash0, merkleTree_reload.getRootHash());

		VersioningKVData<String, byte[]> data1025 = new VersioningKVData<String, byte[]>("KEY-1025", 0,
				BytesUtils.toBytes("NEW-VALUE-1025-VERSION-0"));

		merkleTree_reload.setData(data1025.getKey(), data1025.getVersion(), data1025.getValue());
		merkleTree_reload.commit();
		HashDigest rootHash1 = merkleTree_reload.getRootHash();
		assertNotNull(rootHash1);
		assertNotEquals(rootHash0, rootHash1);

		KVEntry data1025_reload_0 = merkleTree_reload.getData(data1025.getKey(), 0);
		assertNotNull(data1025_reload_0);

		KVEntry data0_reload_0 = merkleTree_reload.getData("KEY-0", 0);
		assertNotNull(data0_reload_0);

		System.out.println("mkl reload total keys = " + merkleTree_reload.getTotalKeys());
		assertEquals(count + 1, merkleTree_reload.getTotalKeys());

		MerkleHashSortTree merkleTree_reload_1 = new MerkleHashSortTree(rootHash1, cryptoSetting, KEY_PREFIX, storage);
		assertEquals(count + 1, merkleTree_reload_1.getTotalKeys());
		assertEquals(rootHash1, merkleTree_reload_1.getRootHash());

		HashDigest rootHash2 = merkleTree_reload_1.getRootHash();
		assertNotNull(rootHash2);
		assertNotEquals(rootHash0, rootHash2);

		KVEntry data1025_reload_1 = merkleTree_reload_1.getData(data1025.getKey(), 0);
		assertNotNull(data1025_reload_1);

		KVEntry data0_reload_1 = merkleTree_reload_1.getData("KEY-0", 0);
		assertNotNull(data0_reload_1);

		System.out.println("mkl reload total keys = " + merkleTree_reload_1.getTotalKeys());
		assertEquals(count + 1, merkleTree_reload_1.getTotalKeys());
	}

	@Test
	public void findKeyWithIndexEqual3() {
		int count = 200;

		List<VersioningKVData<String, byte[]>> dataList = generateRandomKeyDatas(count);
		dataList.get(0).getKey().getBytes();

		for (int i = 0; i < count; i++) {
			long keyHash = KeyIndexer.hash(dataList.get(i).getKey().getBytes());
			byte index = KeyIndexer.index(keyHash, 0);
			if (index == 3) {
				System.out.println("Key = " + dataList.get(i).getKey());
			}
		}
	}

	@Test
	public void findKeyWithEqualKeyHash() {
		int count = 200000;

		long keyHash = KeyIndexer.hash(new String("KEY-155749221633494971").getBytes());

		List<VersioningKVData<String, byte[]>> dataList = generateRandomKeyDatas(count);

		for (int i = 0; i < count; i++) {
			long keyHash1 = KeyIndexer.hash(dataList.get(i).getKey().getBytes());
			byte index = KeyIndexer.index(keyHash, 0);
			if (index == 3 && keyHash1 == keyHash) {
				System.out.println("Key = " + dataList.get(i).getKey());
			}
		}
	}

	/**
	 * 测试树的加载读取；
	 */
	@Test
	public void testReloading() {
		TreeOptions cryptoSetting = createTreeOptions();
		MemoryKVStorage storage = new MemoryKVStorage();

		// 数据集合长度为 100 时也能正常生成；
		int count = 1024;
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);

		MerkleHashSortTree merkleTree = newMerkleTree_with_committed(datas, cryptoSetting, storage);
		HashDigest rootHash0 = merkleTree.getRootHash();
		assertNotNull(rootHash0);

		assertEquals(count, merkleTree.getTotalKeys());

		//TODO: 暂时注释掉默克尔证明相关的内容；
//		for (VersioningKVData<String, byte[]> data : datas) {
//			assertMerkleProof(data, merkleTree);
//		}
//		testMerkleProof1024(datas, merkleTree);

		MerkleHashSortTree merkleTree_reload = new MerkleHashSortTree(rootHash0, cryptoSetting, KEY_PREFIX, storage);
		assertEquals(count, merkleTree_reload.getTotalKeys());

		//TODO: 暂时注释掉默克尔证明相关的内容；
//		testMerkleProof1024(datas, merkleTree_reload);

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

		//TODO: 暂时注释掉默克尔证明相关的内容；
//		MerkleProof proof = merkleTree_reload.getProof(data28.getKey(), 1);
//		assertNotNull(proof);
//		MerkleProofLevel[] hashPaths = proof.getProofLevels();
//		assertEquals(5, hashPaths.length);
//		proof = merkleTree_reload.getProof(data28.getKey(), 0);
//		assertNotNull(proof);
//		hashPaths = proof.getProofLevels();
//		assertEquals(6, hashPaths.length);

		KVEntry data28_reload_0 = merkleTree_reload.getData(data28.getKey(), 0);
		assertNotNull(data28_reload_0);
		assertEquals(data28.getKey(), data28_reload_0.getKey().toUTF8String());
		assertEquals(datas[28].getVersion(), data28_reload_0.getVersion());
		assertArrayEquals(datas[28].getValue(), data28_reload_0.getValue().toBytes());

		KVEntry data28_reload_1 = merkleTree_reload.getData(data28.getKey(), 1);
		assertNotNull(data28_reload_1);
		assertEquals(data28.getKey(), data28_reload_1.getKey().toUTF8String());
		assertEquals(data28.getVersion(), data28_reload_1.getVersion());
		assertArrayEquals(data28.getValue(), data28_reload_1.getValue().toBytes());

//		merkleTree_reload.print();

		// 测试不同根哈希加载的默克尔树能够检索的最新版本；
		MerkleHashSortTree merkleTree_0 = new MerkleHashSortTree(rootHash0, cryptoSetting, KEY_PREFIX, storage);
		MerkleHashSortTree merkleTree_1 = new MerkleHashSortTree(rootHash1, cryptoSetting, KEY_PREFIX, storage);
		KVEntry data28_reload = merkleTree_0.getData(data28.getKey());
		assertEquals(0, data28_reload.getVersion());
		data28_reload = merkleTree_1.getData(data28.getKey());
		assertEquals(1, data28_reload.getVersion());

		// 测试在修改状态下检索默克尔证明的正确性；
		VersioningKVData<String, byte[]> data28_2 = new VersioningKVData<String, byte[]>("KEY-28", 2,
				BytesUtils.toBytes("NEW-VALUE-VERSION-2"));

		//TODO: 暂时注释掉默克尔证明相关的内容；
//		MerkleProof proof28_1 = merkleTree_1.getProof("KEY-28", 1);
//		MerkleProof proof606_1 = merkleTree_1.getProof("KEY-606", 1);
//		assertNotNull(proof28_1);
//		assertNotNull(proof606_1);

		// 针对编号为 28 的数据加入一条新版本记录；
		merkleTree_1.setData(data28_2.getKey(), data28_2.getVersion(), data28_2.getValue());

		
		
		//TODO: 暂时注释掉默克尔证明相关的内容；
		// 对于修改中的数据项，查询未提交的最新版本数据的默克尔证明为 null，但是其已提交版本的证明不受影响；
		// 此外，其它未修改的数据项的默克尔证明也不受影响；
//		MerkleProof proof28_1_1 = merkleTree_1.getProof("KEY-28", 1);
//		MerkleProof proof28_2 = merkleTree_1.getProof("KEY-28", 2);
//		MerkleProof proof606_1_1 = merkleTree_1.getProof("KEY-606", 1);
//		assertNotNull(proof28_1_1);
//		assertNotNull(proof606_1_1);
//		assertNull(proof28_2);
//		assertEquals(proof28_1, proof28_1_1);
//		assertEquals(proof606_1, proof606_1_1);

		// 当提交修改之后，可以获取到修改数据项的最新版本的证明，同时其旧版本的证明也刷新了中间路径（加入了新版本数据节点）； 
		merkleTree_1.commit();
		
		//TODO: 暂时注释掉默克尔证明相关的内容；
//		MerkleProof proof28_1_2 = merkleTree_1.getProof("KEY-28", 1);
//		MerkleProof proof28_2_1 = merkleTree_1.getProof("KEY-28", 2);
//		MerkleProof proof606_1_2 = merkleTree_1.getProof("KEY-606", 1);
//		assertNotNull(proof28_1_2);
//		assertNotNull(proof28_2_1);
//		assertNotNull(proof606_1_2);
		
		//TODO: 暂时注释掉默克尔证明相关的内容；
		// 由于默克尔树发生了修改，所有默克尔证明发生了改变；
//		assertFalse(proof28_1.equals(proof28_1_2));
//		assertFalse(proof606_1.equals(proof606_1_2));
		// 同一个key的数据项的最新版本的默克尔证明路径节点中数据节点部分（倒数第2项），出现在其前一个版本的更新后的数据证明中倒数第3项；

		// 验证默克尔证明的长度增长；
//		MerkleProof proof28_5 = merkleTree_1.getProof("KEY-28", 0);
//		assertNotNull(proof28_5);
//		hashPaths = proof28_5.getProofLevels();
//		assertEquals(7, hashPaths.length);

		//TODO: 暂时注释掉默克尔证明相关的内容；
		// 重新加载默克尔树，默克尔证明是一致的；
//		MerkleHashSortTree merkleTree_1_1 = new MerkleHashSortTree(rootHash1, cryptoSetting, KEY_PREFIX, storage);
//		MerkleProof proof28_4 = merkleTree_1_1.getProof("KEY-28", 1);
//		assertNotNull(proof28_4);
//		assertEquals(proof28_1, proof28_4);

//		merkleTree_1.print();
	}

	/**
	 * 测试 Merkle 证明的正确性；
	 */
	//TODO: 暂时注释掉默克尔证明相关的内容；
//	@Test
	public void testMerkleProofCorrectness() {
		// 长度为 0 的情况；
		int count = 0;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		List<VersioningKVData<String, byte[]>> dataList = generateDatas(count);
		VersioningKVData<String, byte[]>[] datas = toArray(dataList);
		MerkleHashSortTree merkleTree = newMerkleTree_with_committed(datas);
		HashDigest rootHash0 = merkleTree.getRootHash();
		assertNotNull(rootHash0);

		// 预期空的默克尔树中查询任何数据的证明都获得 null 返回；
		MerkleProof proof = merkleTree.getProof("KEY-0");
		assertNull(proof);

		// 长度为 1 的情况；
		count = 1;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);
		merkleTree = newMerkleTree_with_committed(datas);
		HashDigest rootHash1 = merkleTree.getRootHash();
		assertNotNull(rootHash1);

		// 预期在只有 1 条数据的情况下可以正常得到该数据的默克尔证明；
		MerkleProof proof1_0 = merkleTree.getProof("KEY-0");
		assertNotNull(proof1_0);
		// 依照设计，预期任何默克尔证明都至少有 4 条路径；
		assertMerkleProofPath(proof1_0, merkleTree.getRootHash(), merkleTree.getData("KEY-0"));

		// 长度为 2 的情况；
		count = 2;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);

		merkleTree = newMerkleTree_with_committed(datas);
		HashDigest rootHash2 = merkleTree.getRootHash();
		assertNotNull(rootHash2);

		MerkleProof proof2_0 = merkleTree.getProof("KEY-0");
		assertNotNull(proof2_0);
		// 依照设计，预期任何默克尔证明都至少有 4 条路径；
		assertMerkleProofPath(proof2_0, merkleTree.getRootHash(), merkleTree.getData("KEY-0"));

		// 长度为 16 的情况；
		count = 16;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);
		merkleTree = newMerkleTree_with_committed(datas);
		HashDigest rootHash16 = merkleTree.getRootHash();
		assertNotNull(rootHash16);

		MerkleProof proof16_0 = merkleTree.getProof("KEY-0");
		assertNotNull(proof16_0);
		// 依照设计，预期任何默克尔证明都至少有 4 条路径；
		assertMerkleProofPath(proof16_0, merkleTree.getRootHash(), merkleTree.getData("KEY-0"));

		// 长度为 32 的情况；
		count = 32;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);
		merkleTree = newMerkleTree_with_committed(datas);
		HashDigest rootHash32 = merkleTree.getRootHash();
		assertNotNull(rootHash32);

		MerkleProof proof32_0 = merkleTree.getProof("KEY-0");
		assertNotNull(proof32_0);
		// 依照设计，预期任何默克尔证明都至少有 4 条路径；
		assertMerkleProofPath(proof32_0, merkleTree.getRootHash(), merkleTree.getData("KEY-0"));

		// 长度为 1025 的情况；
		count = 1025;
//		System.out.printf("\r\n\r\n================= %s 个节点 =================\r\n\r\n", count);
		dataList = generateDatas(count);
		datas = toArray(dataList);
		merkleTree = newMerkleTree_with_committed(datas);
		HashDigest rootHash1025 = merkleTree.getRootHash();
		assertNotNull(rootHash1025);

		MerkleProof proof1025 = merkleTree.getProof("KEY-0");
		assertNotNull(proof1025);
		// 依照设计，预期任何默克尔证明都至少有 4 条路径；
		assertMerkleProofPath(proof1025, merkleTree.getRootHash(), merkleTree.getData("KEY-0"));
	}

	/**
	 * 预期任何默克尔证明都至少有 4 条路径；路径的构成参考 {@link MerkleHashSortTree#getProof(String)}
	 * 
	 * @param proof1_0
	 * @param rootHash
	 * @param data
	 */
	private void assertMerkleProofPath(MerkleProof proof, HashDigest rootHash, KVEntry data) {
		MerkleProofLevel[] path = proof.getProofLevels();
		assertTrue(path.length >= 4);
		assertEquals(path[0], rootHash);

		HashFunction hashFunc = Crypto.getHashFunction(proof.getDataHash().getAlgorithm());
		byte[] nodeBytes = BinaryProtocol.encode(data, MerkleTrieData.class);
		HashDigest entryHash = hashFunc.hash(nodeBytes);

		assertEquals(entryHash, path[path.length - 2]);

		// TODO: 暂时注释掉，待完成了默克尔证明之后再恢复；
//		assertEquals(data.getValue(), path[path.length - 1]);
	}

	/**
	 * 测试 Merkle 根哈希的不变性，即相同的数据集合得到相同的默克尔树根哈希，与数据加入的先后顺序无关；
	 */
	@Test
	public void testImmutability() {
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

		MerkleHashSortTree merkleTree = newMerkleTree_with_committed(datas);
		MerkleHashSortTree merkleTree1 = newMerkleTree_with_committed(datas1);

		assertEquals(rootHash_N1, rootHash_N2);

		assertEqualsTrie(merkleTree, merkleTree1);
	}

	private void assertEqualsTrie(MerkleHashSortTree merkleTree, MerkleHashSortTree merkleTree1) {
		HashDigest rootHash_N1 = merkleTree.getRootHash();
		HashDigest rootHash_N2 = merkleTree1.getRootHash();
		assertNotNull(rootHash_N1);
		assertNotNull(rootHash_N2);

		SkippingIterator<KVEntry> iterator = merkleTree.iterator();
		SkippingIterator<KVEntry> iterator1 = merkleTree1.iterator();
		KVEntry dt;
		KVEntry dt1;
		assertEquals(iterator.getTotalCount(), iterator1.getTotalCount());
		for (int i = 0; i < iterator.getTotalCount(); i++) {
			assertTrue(iterator.hasNext());
			assertTrue(iterator1.hasNext());
			dt = iterator.next();
			dt1 = iterator1.next();
			assertTrue(dt.getKey().equals(dt1.getKey()));
			assertEquals(dt.getValue(), dt1.getValue());
			assertEquals(dt.getVersion(), dt1.getVersion());

			//TODO: 暂时注释掉默克尔证明相关的内容；
//			MerkleProof proof = merkleTree.getProof(dt.getKey().toBytes());
//			MerkleProof proof1 = merkleTree1.getProof(dt.getKey().toBytes());
//			assertMerkleProofEquals(dt, proof, proof1);
		}
	}

	private void assertMerkleProofEquals(KVEntry data, MerkleProof proof, MerkleProof proof1) {
		MerkleProofLevel[] path = proof.getProofLevels();
		MerkleProofLevel[] path1 = proof1.getProofLevels();
		assertEquals(path.length, path1.length);
		for (int i = 0; i < path.length; i++) {
			assertEquals(path[i], path1[i]);
		}

		HashFunction hashFunc = Crypto.getHashFunction(proof.getDataHash().getAlgorithm());
		byte[] nodeBytes = BinaryProtocol.encode(data, MerkleTrieData.class);
		HashDigest entryHash = hashFunc.hash(nodeBytes);

		// 默克尔证明的哈希路径中，最后的两个哈希值分别是 MerkleData 节点的哈希值，和 MerkleData 节点表示的数据的哈希值；
		assertEquals(entryHash, path[path.length - 2]);
		assertEquals(entryHash, path1[path1.length - 2]);

		// TODO: 暂时注释掉这两个
//		assertEquals(data.getValue(), proof.getDataHash());
//		assertEquals(data.getValue(), proof1.getDataHash());
	}

	private List<VersioningKVData<String, byte[]>> generateRandomKeyDatas(int count) {
		List<VersioningKVData<String, byte[]>> dataList = new ArrayList<VersioningKVData<String, byte[]>>();
		for (int i = 0; i < count; i++) {
			VersioningKVData<String, byte[]> data = new VersioningKVData<String, byte[]>("KEY-" + i + System.nanoTime(),
					0L, BytesUtils.concat(BytesUtils.toBytes(i), BytesUtils.toBytes("VALUE")));
			dataList.add(data);
		}
		return dataList;
	}

	// generate key data with index 3 in merkle tree child
	private List<VersioningKVData<String, byte[]>> generateSpecKeyDatas(List<String> dataListString, int count) {
		List<VersioningKVData<String, byte[]>> dataList = new ArrayList<VersioningKVData<String, byte[]>>();

		VersioningKVData<String, byte[]> data0 = new VersioningKVData<String, byte[]>("KEY-19745261600024463", 0L,
				BytesUtils.concat(BytesUtils.toBytes(0), BytesUtils.toBytes("VALUE")));
		dataList.add(data0);
		dataListString.add(data0.getKey());

		return dataList;
	}

	private List<String> generateNewDatasString(int origCount, int newCount) {
		List<String> newDataListString = new ArrayList<String>();
		for (int i = 0 + origCount; i < newCount + origCount; i++) {
			newDataListString.add("KEY-" + i);
		}
		return newDataListString;
	}

	/**
	 * 生成指定数量的 KV 测试数据集合； KEY 以 “KEY-%s” 格式生成，对应的值为 “VALUE-%s”
	 * 
	 * @param count
	 * @return
	 */
	private List<VersioningKVData<String, byte[]>> generateDatas(int count) {
		return generateDatas(count, 0L);
	}

	/**
	 * 生成指定数量的 KV 测试数据集合； KEY 以 “KEY-%s” 格式生成，对应的值为 “VALUE-%s”
	 * 
	 * @param count
	 * @return
	 */
	private List<VersioningKVData<String, byte[]>> generateDatasFrom(int fromId, int count) {
		return generateDatasFrom(fromId, count, 0);
	}

	/**
	 * 生成指定数量的 KV 测试数据集合； KEY 以 “KEY-%s” 格式生成，对应的值为 “VALUE-%s”
	 * 
	 * @param count
	 * @return
	 */
	private List<VersioningKVData<String, byte[]>> generateDatas(int count, long version) {
		return generateDatasFrom(0, count, version);
	}

	/**
	 * 生成指定数量的 KV 测试数据集合； KEY 以 “KEY-%s” 格式生成，对应的值为 “VALUE-%s”
	 * 
	 * @param count
	 * @return
	 */
	private List<VersioningKVData<String, byte[]>> generateDatasFrom(int fromId, int count, long version) {
		List<VersioningKVData<String, byte[]>> dataList = new ArrayList<VersioningKVData<String, byte[]>>();
		for (int i = 0; i < count; i++) {
			VersioningKVData<String, byte[]> data = new VersioningKVData<String, byte[]>("KEY-" + (i + fromId), version,
					BytesUtils.concat(BytesUtils.toBytes(i), BytesUtils.toBytes("VALUE" + i)));
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

		SecureRandom rand = new SecureRandom();
		VersioningKVData<String, byte[]> t;
		int c = datas.length * 2;
		for (int i = 0; i < c; i++) {
			int x = rand.nextInt(datas.length);
			int y = rand.nextInt(datas.length);
			t = datas[x];
			datas[x] = datas[y];
			datas[y] = t;
		}

		return datas;
	}

	private TreeOptions createTreeOptions() {
		return TreeOptions.build().setDefaultHashAlgorithm(ClassicAlgorithm.SHA256.code());
	}

	private MerkleHashSortTree newMerkleTree_with_committed(VersioningKVData<String, byte[]>[] datas) {
		TreeOptions treeOption = createTreeOptions();
		MemoryKVStorage storage = new MemoryKVStorage();

		return newMerkleTree(datas, treeOption, storage, true);
	}

	private MerkleHashSortTree newMerkleTree(VersioningKVData<String, byte[]>[] datas) {
		TreeOptions treeOption = createTreeOptions();
		MemoryKVStorage storage = new MemoryKVStorage();

		return newMerkleTree(datas, treeOption, storage, false);
	}

	private MerkleHashSortTree newMerkleTree_with_committed(VersioningKVData<String, byte[]>[] datas,
			TreeOptions treeOption, ExPolicyKVStorage storage) {
		return newMerkleTree(datas, treeOption, storage, true);
	}

	private MerkleHashSortTree newMerkleTree(VersioningKVData<String, byte[]>[] datas, TreeOptions treeOption,
			ExPolicyKVStorage storage, boolean commit) {

		MerkleHashSortTree merkleTree = new MerkleHashSortTree(treeOption, KEY_PREFIX, storage);
		assertTrue(merkleTree.isUpdated());

		setDatas(merkleTree, datas);

		if (commit) {
			merkleTree.commit();
			assertFalse(merkleTree.isUpdated());
		}

		return merkleTree;
	}

	private MerkleHashSortTree setDatasAndCommit(MerkleHashSortTree merkleTree,
			VersioningKVData<String, byte[]>[] datas) {
		setDatas(merkleTree, datas);

		merkleTree.commit();
		assertFalse(merkleTree.isUpdated());

		return merkleTree;
	}

	private MerkleHashSortTree setDatas(MerkleHashSortTree merkleTree, VersioningKVData<String, byte[]>[] datas) {
		for (int i = 0; i < datas.length; i++) {
			merkleTree.setData(datas[i].getKey(), datas[i].getVersion(), datas[i].getValue());
		}

		assertTrue(merkleTree.isUpdated());
		return merkleTree;
	}

	private HashDigest buildMerkleRootHash(VersioningKVData<String, byte[]>[] datas) {
		MerkleTree merkleTree = newMerkleTree_with_committed(datas);
		HashDigest rootHash = merkleTree.getRootHash();

		return rootHash;
	}

}

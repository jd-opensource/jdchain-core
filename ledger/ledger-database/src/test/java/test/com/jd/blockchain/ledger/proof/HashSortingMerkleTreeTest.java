package test.com.jd.blockchain.ledger.proof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.mockito.Mockito;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.MerkleProof;
import com.jd.blockchain.ledger.proof.HashSortingMerkleTree;
import com.jd.blockchain.storage.service.ExPolicyKVStorage;
import com.jd.blockchain.storage.service.utils.MemoryKVStorage;
import com.jd.blockchain.storage.service.utils.VersioningKVData;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.hash.MurmurHash3;
import com.jd.blockchain.utils.io.BytesUtils;

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

		MerkleProof proof = merkleTree.getProof("KEY_NOT_EXIST");
		assertNull(proof);

		// 数据集合长度为 1 时也能正常生成；
		dataList = generateDatas(1);
		datas = toArray(dataList);
		merkleTree = buildMerkleTree(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		testMerkleProof(datas[0], merkleTree, 4);

		// 数据集合长度为 2 时也能正常生成；
		dataList = generateDatas(2);
		datas = toArray(dataList);
		merkleTree = buildMerkleTree(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		testMerkleProof(datas[0], merkleTree, 4);
		testMerkleProof(datas[1], merkleTree, 4);

		// 数据集合长度为 100 时也能正常生成；
		dataList = generateDatas(100);
		datas = toArray(dataList);
		merkleTree = buildMerkleTree(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);

		// 数据集合长度为 1024 时也能正常生成；
		dataList = generateDatas(1024);
		datas = toArray(dataList);
		merkleTree = buildMerkleTree(datas);
		rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);
		merkleTree.print();
		for (VersioningKVData<String, byte[]> data : datas) {
			testMerkleProof(data, merkleTree, -1);
		}
		testMerkleProof1024(datas, merkleTree);
	}
	
	private void testMerkleProof1024(VersioningKVData<String, byte[]>[] datas, HashSortingMerkleTree merkleTree) {
		testMerkleProof(datas[28], merkleTree, 5);
		testMerkleProof(datas[103], merkleTree, 5);
		testMerkleProof(datas[637], merkleTree, 5);
		testMerkleProof(datas[505], merkleTree, 5);
		testMerkleProof(datas[93], merkleTree, 5);
		testMerkleProof(datas[773], merkleTree, 5);
		testMerkleProof(datas[163], merkleTree, 5);
		testMerkleProof(datas[5], merkleTree, 5);
		testMerkleProof(datas[815], merkleTree, 5);
		testMerkleProof(datas[89], merkleTree, 5);
		testMerkleProof(datas[854], merkleTree, 6);
		testMerkleProof(datas[146], merkleTree, 6);
		testMerkleProof(datas[606], merkleTree, 6);
		testMerkleProof(datas[1003], merkleTree, 6);
		testMerkleProof(datas[156], merkleTree, 6);
		testMerkleProof(datas[861], merkleTree, 6);
		testMerkleProof(datas[1018], merkleTree, 7);
		testMerkleProof(datas[260], merkleTree, 7);
		testMerkleProof(datas[770], merkleTree, 7);
		testMerkleProof(datas[626], merkleTree, 7);
		testMerkleProof(datas[182], merkleTree, 7);
		testMerkleProof(datas[200], merkleTree, 7);
		testMerkleProof(datas[898], merkleTree, 8);
		testMerkleProof(datas[244], merkleTree, 8);
		testMerkleProof(datas[275], merkleTree, 8);
		testMerkleProof(datas[69], merkleTree, 9);
		testMerkleProof(datas[560], merkleTree, 9);
	}
	
	private void testMerkleProof(VersioningKVData<String, byte[]> data, HashSortingMerkleTree merkleTree, int expectProofPathCount) {
		MerkleProof proof = merkleTree.getProof("KEY_NOT_EXIST");
		assertNull(proof);

		proof = merkleTree.getProof(data.getKey());
		assertNotNull(proof);
		HashDigest[] hashPaths = proof.getHashPaths();
		if (expectProofPathCount > 0) {
			assertEquals(expectProofPathCount, hashPaths.length);
		}
		HashDigest dataHash = SHA256_HASH_FUNC.hash(data.getValue());
		assertEquals(dataHash, proof.getDataHash());
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
		HashDigest rootHash = merkleTree.getRootHash();
		assertNotNull(rootHash);

		assertEquals(count, merkleTree.getTotalKeys());
		assertEquals(count, merkleTree.getTotalRecords());

		for (VersioningKVData<String, byte[]> data : datas) {
			testMerkleProof(data, merkleTree, -1);
		}
		testMerkleProof1024(datas, merkleTree);

		HashSortingMerkleTree merkleTree_reload = new HashSortingMerkleTree(rootHash, cryptoSetting, KEY_PREFIX,
				storage, false);
		assertEquals(count, merkleTree_reload.getTotalKeys());
		assertEquals(count, merkleTree_reload.getTotalRecords());
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

		return newMerkleTree(datas, cryptoSetting, storage);
	}

	private HashSortingMerkleTree newMerkleTree(VersioningKVData<String, byte[]>[] datas, CryptoSetting cryptoSetting,
			ExPolicyKVStorage storage) {
		HashSortingMerkleTree merkleTree = new HashSortingMerkleTree(null, cryptoSetting, KEY_PREFIX, storage, false);
		assertTrue(merkleTree.isUpdated());

		for (int i = 0; i < datas.length; i++) {
			merkleTree.setData(datas[i].getKey(), datas[i].getVersion(), datas[i].getValue());
		}

		assertTrue(merkleTree.isUpdated());

		merkleTree.commit();

		assertFalse(merkleTree.isUpdated());

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

package test.com.jd.blockchain.ledger.proof;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;

import org.junit.Test;
import org.mockito.Mockito;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.CryptoAlgorithm;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import com.jd.blockchain.crypto.service.classic.ClassicAlgorithm;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.proof.MerkleSortedTree;
import com.jd.blockchain.ledger.proof.MerkleSortedTree.MerkleData;
import com.jd.blockchain.storage.service.utils.MemoryKVStorage;

public class MerkleSortedTreeTest {

	private static final String DEFAULT_MKL_KEY_PREFIX = "";
	
	private static final CryptoAlgorithm HASH_ALGORITHM = ClassicAlgorithm.SHA256;
	
	private static final HashFunction HASH_FUNCTION = Crypto.getHashFunction(HASH_ALGORITHM);

	/**
	 * 测试顺序加入数据，是否能够得到
	 */
	@Test
	public void testSequenceAdd() {
		int count = 1;
		byte[][] datas = generateRandomData(count);
		testWithSequenceIDs(datas, count);
		
		count = MerkleSortedTree.TREE_DEGREE;
		datas = generateRandomData(count);
		testWithSequenceIDs(datas, count);
		
		count = (int) power(MerkleSortedTree.TREE_DEGREE, 2);
		datas = generateRandomData(count);
		testWithSequenceIDs(datas, count);
		
		count = (int) power(MerkleSortedTree.TREE_DEGREE, 3);
		datas = generateRandomData(count);
		testWithSequenceIDs(datas, count);
		
		count = count+1;
		datas = generateRandomData(count);
		testWithSequenceIDs(datas, count);
		
		count = count-2;
		datas = generateRandomData(count);
		testWithSequenceIDs(datas, count);
		
		count = 10010;
		datas = generateRandomData(count);
		testWithSequenceIDs(datas, count);
	}
	
	/**
	 * 测试顺序加入数据，是否能够得到
	 */
	@Test
	public void testRandomAdd() {
		int count = 1;
		byte[][] datas = generateRandomData(count);
		testWithRandomIDs(datas, count);
		
		count = MerkleSortedTree.TREE_DEGREE;
		datas = generateRandomData(count);
		testWithRandomIDs(datas, count);
		
		count = (int) power(MerkleSortedTree.TREE_DEGREE, 2);
		datas = generateRandomData(count);
		testWithRandomIDs(datas, count);
		
		count = (int) power(MerkleSortedTree.TREE_DEGREE, 3);
		datas = generateRandomData(count);
		testWithRandomIDs(datas, count);
		
		count = count+1;
		datas = generateRandomData(count);
		testWithRandomIDs(datas, count);
		
		count = count-2;
		datas = generateRandomData(count);
		testWithRandomIDs(datas, count);
		
		count = 10010;
		datas = generateRandomData(count);
		testWithRandomIDs(datas, count);
	}
	
	private static void testWithRandomIDs(byte[][] datas, int count) {
		long[] ids = new long[count];
		HashSet<Long> set =new HashSet<Long>();
		SecureRandom random = new SecureRandom();
		long id = -1;
		set.add(Long.valueOf(id));
		for (int i = 0; i < count; i++) {
			while (id< 0 || id >= MerkleSortedTree.MAX_COUNT || set.contains(Long.valueOf(id))) {
				id = random.nextLong();
			}
			set.add(Long.valueOf(id));
			ids[i] = id;
		}
		testAddingAndAssertingEquals(ids, datas);
	}
	
	private static void testWithSequenceIDs(byte[][] datas, int count) {
		long[] ids = new long[count];
		for (int i = 0; i < count; i++) {
			ids[i] = i;
		}
		testAddingAndAssertingEquals(ids, datas);
	}
	
	private static void testAddingAndAssertingEquals(long[] ids, byte[][] datas) {
		CryptoSetting cryptoSetting = createCryptoSetting();
		MemoryKVStorage storage = new MemoryKVStorage();
		MerkleSortedTree mst = new MerkleSortedTree(cryptoSetting, DEFAULT_MKL_KEY_PREFIX, storage);
		for (int i = 0; i < ids.length; i++) {
			mst.set(ids[i], datas[i]);
		}
		mst.commit();
		
		HashDigest rootHash = mst.getRootHash();
		assertNotNull(rootHash);
		
		assertDataEquals(ids, datas, mst);
		
		// reload merkle tree from storage;
		MerkleSortedTree mst1 = new MerkleSortedTree(rootHash, cryptoSetting, DEFAULT_MKL_KEY_PREFIX, storage);
		assertDataEquals(ids, datas, mst1);
	}
	
	private static void assertDataEquals(long[] ids, byte[][] datas, MerkleSortedTree mst) {
		int i ;
		for (i = 0; i < ids.length; i++) {
			long id = ids[i];
			MerkleData mdata = mst.get(id);
			assertNotNull(mdata);
			assertEquals(id, mdata.getId());
			
			HashDigest dataHash = HASH_FUNCTION.hash(datas[i]);
			assertEquals(dataHash, mdata.getHash());
			assertArrayEquals(datas[i], mdata.getBytes());
		}
	}
	
	private static byte[][] generateRandomData(int count){
		Random random = new Random();
		byte[][] datas = new byte[count][];
		for (int i = 0; i < count; i++) {
			datas[i] = new byte[8];
			random.nextBytes(datas[i]);
		}
		return datas;
	}
	


	private static CryptoSetting createCryptoSetting() {
		CryptoSetting cryptoSetting = Mockito.mock(CryptoSetting.class);
		when(cryptoSetting.getAutoVerifyHash()).thenReturn(true);
		when(cryptoSetting.getHashAlgorithm()).thenReturn(HASH_ALGORITHM.code());
		return cryptoSetting;
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
}

package test.com.jd.blockchain.peer.service;

import org.junit.Test;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.sdk.AccessSpecification;

import utils.security.RandomUtils;
import utils.serialize.json.JSONSerializeUtils;

public class AccessSpecificationSerializeTest {

	@Test
	public void test() {
		byte[] randomBytes = RandomUtils.generateRandomBytes(20);
		HashDigest[] ledgers = new HashDigest[10];
		String[] providers = new String[ledgers.length];
		for (int i = 0; i < ledgers.length; i++) {
			ledgers[i] = Crypto.getHashFunction("SHA256").hash(randomBytes);
		}
		
		AccessSpecification accessSepc = new AccessSpecification(ledgers, providers);
		
		String json = JSONSerializeUtils.serializeToJSON(accessSepc);
		
		AccessSpecification accessSepc_des = JSONSerializeUtils.deserializeAs(json, AccessSpecification.class);
	}

}

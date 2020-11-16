package test.com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.utils.Bytes;

public class ReplicaInfo implements Replica {

	private int id;
	private Bytes address;
	private String name;
	private PubKey pubKey;

	public ReplicaInfo(int id) {
		this.id = id;
	}
	
	@Override
	public int getId() {
		return id;
	}

	@Override
	public Bytes getAddress() {
		return address;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public PubKey getPubKey() {
		return pubKey;
	}
	
	
	public void setAddress(Bytes address) {
		this.address = address;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setPubKey(PubKey pubKey) {
		this.pubKey = pubKey;
	}

}
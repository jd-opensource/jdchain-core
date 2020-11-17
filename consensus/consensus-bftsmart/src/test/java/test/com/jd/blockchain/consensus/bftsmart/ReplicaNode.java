package test.com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.consensus.Replica;
import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.AsymmetricKeypair;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.utils.Bytes;

public class ReplicaNode implements Replica {

	private int id;
	private Bytes address;
	private String name;
	private AsymmetricKeypair keypair;

	public ReplicaNode(int id) {
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
		return keypair.getPubKey();
	}
	
	
	public void setAddress(Bytes address) {
		this.address = address;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setKey(AsymmetricKeypair keypair) {
		this.keypair = keypair;
		this.address = AddressEncoding.generateAddress(keypair.getPubKey());
	}
	
	@Override
	public String toString() {
		return String.format("Replica[%s : %s]", id, address.toBase58());
	}

}
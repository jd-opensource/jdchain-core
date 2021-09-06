package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.ledger.UserInfo;

import com.jd.blockchain.ledger.UserState;
import utils.Bytes;
import utils.io.BytesUtils;

/**
 * 用户账户；
 * 
 * @author huanghaiquan
 *
 */
public class UserAccount extends AccountDecorator implements UserInfo { // implements UserInfo {

	private static final String USER_INFO_PREFIX = "PROP" + LedgerConsts.KEY_SEPERATOR;

	private static final String DATA_PUB_KEY = "DATA-PUBKEY";
	private static final String DATA_CA = "DATA-CA";
	private static final String DATA_STATE = "DATA-STATE";

	public UserAccount(CompositeAccount baseAccount) {
		super(baseAccount);
	}

	private PubKey dataPubKey;
	private String certificate;
	private UserState state;

	@Override
	public Bytes getAddress() {
		return getID().getAddress();
	}

	@Override
	public PubKey getPubKey() {
		return getID().getPubKey();
	}
	
	@Override
	public PubKey getDataPubKey() {
		if (dataPubKey == null) {
			BytesValue pkBytes = getHeaders().getValue(DATA_PUB_KEY);
			if (pkBytes == null) {
				return null;
			}
			dataPubKey = Crypto.resolveAsPubKey(pkBytes.getBytes().toBytes());
		}
		return dataPubKey;
	}

	public void setDataPubKey(PubKey pubKey) {
		long version = getHeaders().getVersion(DATA_PUB_KEY);
		setDataPubKey(pubKey, version);
	}

	public void setDataPubKey(PubKey pubKey, long version) {
		TypedValue value = TypedValue.fromPubKey(dataPubKey);
		long newVersion = getHeaders().setValue(DATA_PUB_KEY, value, version);
		if (newVersion > -1) {
			dataPubKey = pubKey;
		} else {
			throw new LedgerException("Data public key was updated failed!");
		}
	}

	@Override
	public UserState getState() {
		if(state == null) {
			BytesValue rbs = getHeaders().getValue(DATA_STATE);
			if (rbs == null) {
				state = UserState.NORMAL;
			} else {
				state = UserState.valueOf(BytesUtils.toString(rbs.getBytes().toBytes()));
			}
		}
		return state;
	}

	public void setState(UserState state) {
		long version = getHeaders().getVersion(DATA_STATE);
		getHeaders().setValue(DATA_STATE, TypedValue.fromText(state.name()), version);
		this.state = state;
	}

	@Override
	public String getCertificate() {
		if(certificate == null) {
			BytesValue crt = getHeaders().getValue(DATA_CA);
			if (crt != null) {
				certificate = BytesUtils.toString(crt.getBytes().toBytes());
			}
		}
		return certificate;
	}

	public void setCertificate(String certificate) {
		long version = getHeaders().getVersion(DATA_CA);
		getHeaders().setValue(DATA_CA, TypedValue.fromText(certificate), version);
	}

	public long setProperty(String key, String value, long version) {
		return getHeaders().setValue(encodePropertyKey(key), TypedValue.fromText(value), version);
	}

	public String getProperty(String key) {
		BytesValue value = getHeaders().getValue(encodePropertyKey(key));
		return value == null ? null : value.getBytes().toUTF8String();
	}

	public String getProperty(String key, long version) {
		BytesValue value = getHeaders().getValue(encodePropertyKey(key), version);
		return value == null ? null : value.getBytes().toUTF8String();
	}

	private String encodePropertyKey(String key) {
		return USER_INFO_PREFIX+key;
	}


}
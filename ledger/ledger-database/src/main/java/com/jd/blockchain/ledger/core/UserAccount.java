package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.LedgerException;
import com.jd.blockchain.ledger.TypedValue;
import com.jd.blockchain.ledger.UserInfo;

import com.jd.blockchain.ledger.AccountState;
import com.jd.blockchain.ledger.cache.UserCache;
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

	private static final String DATA_PUB_KEY = "D-PK";
	private static final String DATA_CA = "D-CA";
	private static final String DATA_STATE = "D-ST";
	private UserCache cache;

	public UserAccount(CompositeAccount baseAccount) {
		super(baseAccount);
	}

	public UserAccount(CompositeAccount baseAccount, UserCache cache) {
		super(baseAccount);
		this.cache = cache;
	}

	private PubKey dataPubKey;
	private String certificate;
	private AccountState state;

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
	public AccountState getState() {
		if(state == null) {
			if(null != cache) {
				state = cache.getState(getAddress());
			}
			if (null == state) {
				BytesValue rbs = getHeaders().getValue(DATA_STATE);
				if (rbs == null) {
					state = AccountState.NORMAL;
				} else {
					state = AccountState.valueOf(BytesUtils.toString(rbs.getBytes().toBytes()));
				}
			}
			if(null != cache) {
				cache.setState(getAddress(), this.state);
			}
		}
		return state;
	}

	public void setState(AccountState state) {
		long version = getHeaders().getVersion(DATA_STATE);
		getHeaders().setValue(DATA_STATE, TypedValue.fromText(state.name()), version);
		this.state = state;
		if(null != cache) {
			cache.setState(getAddress(), this.state);
		}
	}

	@Override
	public String getCertificate() {
		if(certificate == null) {
			if(null != cache) {
				certificate = cache.getCertificate(getAddress());
			}
			if (null == certificate) {
				BytesValue crt = getHeaders().getValue(DATA_CA);
				if (crt != null) {
					certificate = BytesUtils.toString(crt.getBytes().toBytes());
					if(null != cache) {
						cache.setCertificate(getAddress(), certificate);
					}
				}
			}
		}
		return certificate;
	}

	public void setCertificate(String certificate) {
		long version = getHeaders().getVersion(DATA_CA);
		getHeaders().setValue(DATA_CA, TypedValue.fromText(certificate), version);
		if(null != cache) {
			cache.setCertificate(getAddress(), certificate);
		}
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
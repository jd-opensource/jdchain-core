package com.jd.blockchain.tools.initializer;

public class DBConnectionConfig {

	private String connectionUri;

	private String password;

	private String anchor;
	
	public DBConnectionConfig() {
	}
	public DBConnectionConfig(String uri) {
		this.connectionUri = uri;
	}

	public String getUri() {
		return connectionUri;
	}

	public void setConnectionUri(String connectionUri) {
		this.connectionUri = connectionUri;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAnchor() {
		return anchor;
	}

	public void setAnchor(String anchor) {
		this.anchor = anchor;
	}
}
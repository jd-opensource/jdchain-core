package com.jd.blockchain.tools.initializer;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import utils.PathUtils;
import utils.PropertiesUtils;
import utils.StringUtils;
import utils.io.FileUtils;

public class LocalConfig {

	// 当前参与方的 公钥；
	public static final String LOCAL_PARTI_PUBKEY = "local.parti.pubkey";
	// 当前参与方的 证书；
	public static final String LOCAL_PARTI_CA_PATH = "local.parti.ca-path";

	// 当前参与方的私钥（密文编码）；
	public static final String LOCAL_PARTI_PRIVKEY = "local.parti.privkey";
	// 当前参与方的私钥文件；
	public static final String LOCAL_PARTI_PRIVKEY_PATH = "local.parti.privkey-path";

	// 当前参与方的私钥解密密钥(原始口令的一次哈希，Base58格式)，如果不设置，则启动过程中需要从控制台输入；
	public static final String LOCAL_PARTI_PWD = "local.parti.pwd";

	// TLS相关配置
	public static final String LOCAL_PARTI_SSL_KEY_STORE = "local.parti.ssl.key-store";
	public static final String LOCAL_PARTI_SSL_KEY_STORE_TYPE = "local.parti.ssl.key-store-type";
	public static final String LOCAL_PARTI_SSL_KEY_ALIAS = "local.parti.ssl.key-alias";
	public static final String LOCAL_PARTI_SSL_KEY_STORE_PASSWORD = "local.parti.ssl.key-store-password";
	public static final String LOCAL_PARTI_SSL_TRUST_STORE = "local.parti.ssl.trust-store";
	public static final String LOCAL_PARTI_SSL_TRUST_STORE_PASSWORD = "local.parti.ssl.trust-store-password";
	public static final String LOCAL_PARTI_SSL_TRUST_STORE_TYPE = "local.parti.ssl.trust-store-type";

	// 账本初始化完成后生成的"账本绑定配置文件"的输出目录；
	public static final String LEDGER_BINDING_OUT = "ledger.binding.out";

	// 账本数据库的连接字符串；
	public static final String LEDGER_DB_URI = "ledger.db.uri";

	// 账本数据库的连接口令；
	public static final String LEDGER_DB_PWD = "ledger.db.pwd";

	private LocalParticipantConfig local = new LocalParticipantConfig();

	private String bindingOutDir;

	private DBConnectionConfig storagedDb = new DBConnectionConfig();

	public LocalParticipantConfig getLocal() {
		return local;
	}

	public void setLocal(LocalParticipantConfig local) {
		this.local = local;
	}

	public String getBindingOutDir() {
		return bindingOutDir;
	}

	public void setBindingOutDir(String bindingOutDir) {
		this.bindingOutDir = bindingOutDir;
	}

	public DBConnectionConfig getStoragedDb() {
		return storagedDb;
	}

	public void setStoragedDb(DBConnectionConfig storagedDb) {
		this.storagedDb = storagedDb;
	}

	public static LocalConfig resolve(String initSettingFile) {
		Properties props = FileUtils.readProperties(initSettingFile, "UTF-8");
		return resolve(props, initSettingFile);
	}

	public static LocalConfig resolve(InputStream in) {
		Properties props = FileUtils.readProperties(in, "UTF-8");
		return resolve(props, null);
	}

	public static LocalConfig resolve(Properties props, String initSettingFile) {

		LocalConfig conf = new LocalConfig();

		conf.local.pubKeyString = PropertiesUtils.getOptionalProperty(props, LOCAL_PARTI_PUBKEY);
		conf.local.caPath = PropertiesUtils.getOptionalProperty(props, LOCAL_PARTI_CA_PATH);
		if(StringUtils.isEmpty(conf.local.pubKeyString) && StringUtils.isEmpty(conf.local.caPath)) {
			throw new IllegalArgumentException("Property[" + LOCAL_PARTI_PUBKEY + "] and ["+ LOCAL_PARTI_CA_PATH +"] cannot be empty at the same time!");
		}

		conf.local.privKeyString = PropertiesUtils.getOptionalProperty(props, LOCAL_PARTI_PRIVKEY);
		conf.local.privKeyPath = PropertiesUtils.getOptionalProperty(props, LOCAL_PARTI_PRIVKEY_PATH);
		if(StringUtils.isEmpty(conf.local.privKeyString) && StringUtils.isEmpty(conf.local.privKeyPath)) {
			throw new IllegalArgumentException("Property[" + LOCAL_PARTI_PRIVKEY + "] and ["+ LOCAL_PARTI_PRIVKEY_PATH +"] cannot be empty at the same time!");
		}
		conf.local.password = PropertiesUtils.getProperty(props, LOCAL_PARTI_PWD, false);

		conf.storagedDb.setConnectionUri(PropertiesUtils.getRequiredProperty(props, LEDGER_DB_URI));
		conf.storagedDb.setPassword(PropertiesUtils.getProperty(props, LEDGER_DB_PWD, false));

		if (initSettingFile == null) {
			conf.bindingOutDir = PropertiesUtils.getRequiredProperty(props, LEDGER_BINDING_OUT);
		} else {
			String bindingOutDir = PropertiesUtils.getRequiredProperty(props, LEDGER_BINDING_OUT);
			String initSettingDir = PathUtils.concatPaths(initSettingFile, "../");
			conf.bindingOutDir = absolutePath(initSettingDir, bindingOutDir);
		}

		conf.local.sslKeyStore = PropertiesUtils.getProperty(props, LOCAL_PARTI_SSL_KEY_STORE, false);
		conf.local.sslKeyStoreType = PropertiesUtils.getProperty(props, LOCAL_PARTI_SSL_KEY_STORE_TYPE, false);
		conf.local.sslKeyAlias = PropertiesUtils.getProperty(props, LOCAL_PARTI_SSL_KEY_ALIAS, false);
		conf.local.sslKeyStorePassword = PropertiesUtils.getProperty(props, LOCAL_PARTI_SSL_KEY_STORE_PASSWORD, false);
		conf.local.sslTrustStore = PropertiesUtils.getProperty(props, LOCAL_PARTI_SSL_TRUST_STORE, false);
		conf.local.sslTrustStorePassword = PropertiesUtils.getProperty(props, LOCAL_PARTI_SSL_TRUST_STORE_PASSWORD, false);
		conf.local.sslTrustStoreType = PropertiesUtils.getProperty(props, LOCAL_PARTI_SSL_TRUST_STORE_TYPE, false);

		return conf;
	}

	private static String absolutePath(String currPath, String settingPath) {
		String absolutePath = settingPath;
		File settingFile = new File(settingPath);
		if (!settingFile.isAbsolute()) {
            absolutePath = PathUtils.concatPaths(currPath, settingPath);
		}
		return absolutePath;
	}

	/**
	 * 当前参与方的本地配置信息；
	 * 
	 * @author huanghaiquan
	 *
	 */
	public static class LocalParticipantConfig {
		private String pubKeyString;
		private String caPath;
		private String privKeyString;
		private String privKeyPath;
		private String password;
		private String sslKeyStore;
		private String sslKeyStoreType;
		private String sslKeyAlias;
		private String sslKeyStorePassword;
		private String sslTrustStore;
		private String sslTrustStorePassword;
		private String sslTrustStoreType;

		public String getPubKeyString() {
			return pubKeyString;
		}

		public void setId(String pubKeyString) {
			this.pubKeyString = pubKeyString;
		}

		public String getPrivKeyString() {
			return privKeyString;
		}

		public void setPrivKeyString(String privKeyString) {
			this.privKeyString = privKeyString;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public void setPubKeyString(String pubKeyString) {
			this.pubKeyString = pubKeyString;
		}

		public String getCaPath() {
			return caPath;
		}

		public void setCaPath(String caPath) {
			this.caPath = caPath;
		}

		public String getPrivKeyPath() {
			return privKeyPath;
		}

		public void setPrivKeyPath(String privKeyPath) {
			this.privKeyPath = privKeyPath;
		}

		public String getSslKeyStore() {
			return sslKeyStore;
		}

		public void setSslKeyStore(String sslKeyStore) {
			this.sslKeyStore = sslKeyStore;
		}

		public String getSslKeyStoreType() {
			return sslKeyStoreType;
		}

		public void setSslKeyStoreType(String sslKeyStoreType) {
			this.sslKeyStoreType = sslKeyStoreType;
		}

		public String getSslKeyAlias() {
			return sslKeyAlias;
		}

		public void setSslKeyAlias(String sslKeyAlias) {
			this.sslKeyAlias = sslKeyAlias;
		}

		public String getSslKeyStorePassword() {
			return sslKeyStorePassword;
		}

		public void setSslKeyStorePassword(String sslKeyStorePassword) {
			this.sslKeyStorePassword = sslKeyStorePassword;
		}

		public String getSslTrustStore() {
			return sslTrustStore;
		}

		public void setSslTrustStore(String sslTrustStore) {
			this.sslTrustStore = sslTrustStore;
		}

		public String getSslTrustStorePassword() {
			return sslTrustStorePassword;
		}

		public void setSslTrustStorePassword(String sslTrustStorePassword) {
			this.sslTrustStorePassword = sslTrustStorePassword;
		}

		public String getSslTrustStoreType() {
			return sslTrustStoreType;
		}

		public void setSslTrustStoreType(String sslTrustStoreType) {
			this.sslTrustStoreType = sslTrustStoreType;
		}
	}

}

package com.jd.blockchain.tools.initializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerDataStructure;
import com.jd.blockchain.tools.initializer.web.LedgerBindingConfigException;

import utils.StringUtils;
import utils.codec.Base58Utils;
import utils.io.FileUtils;
import utils.io.RuntimeIOException;
import utils.net.SSLSecurity;

public class LedgerBindingConfig {

	public static final String CHARSET = "UTF-8";

	public static final String LEDGER_HASH_SEPERATOR = ",";

	public static final String ATTR_SEPERATOR = ".";

	// Binding List;
	public static final String LEDGER_BINDINS = "ledger.bindings";

	// Binding Config Key Prefix;
	public static final String BINDING_PREFIX = "binding";

	// Participant Config Key Prefix;
	public static final String PARTI_PREFIX = "parti.";

	// Participant Attribute Key;
	public static final String PARTI_ADDRESS = PARTI_PREFIX + "address";
	// 参与方名称
	public static final String PARTI_NAME = PARTI_PREFIX + "name";
	public static final String PARTI_PK_PATH = PARTI_PREFIX + "pk-path";
	public static final String PARTI_PK = PARTI_PREFIX + "pk";
	public static final String PARTI_PASSWORD = PARTI_PREFIX + "pwd";
	// TLS配置
	public static final String PARTI_SSL_KEY_STORE = "ssl.key-store";
	public static final String PARTI_SSL_KEY_STORE_TYPE = "ssl.key-store-type";
	public static final String PARTI_SSL_KEY_ALIAS = "ssl.key-alias";
	public static final String PARTI_SSL_KEY_STORE_PASSWORD = "ssl.key-store-password";
	public static final String PARTI_SSL_TRUST_STORE = "ssl.trust-store";
	public static final String PARTI_SSL_TRUST_STORE_PASSWORD = "ssl.trust-store-password";
	public static final String PARTI_SSL_TRUST_STORE_TYPE = "ssl.trust-store-type";
	public static final String PARTI_SSL_PROTOCOL = "ssl.protocol";
	public static final String PARTI_SSL_ENABLED_PROTOCOLS = "ssl.enabled-protocols";
	public static final String PARTI_SSL_CIPHERS = "ssl.ciphers";

	// DB Connection Config Key Prefix;
	public static final String DB_PREFIX = "db.";

	// DB Connction Attribute Key;
	public static final String DB_CONN = DB_PREFIX + "uri";
	public static final String DB_PASSWORD = DB_PREFIX + "pwd";

	// 账本名字
	public static final String LEDGER_NAME = "name";
	public static final String LEDGER_DATA_STRUCTURE = "data.structure";

	// ------------------------------

	private Map<HashDigest, BindingConfig> bindings = new LinkedHashMap<>();

	public HashDigest[] getLedgerHashs() {
		return bindings.keySet().toArray(new HashDigest[bindings.size()]);
	}

	public BindingConfig getLedger(HashDigest hash) {
		return bindings.get(hash);
	}

	public void addLedgerBinding(HashDigest ledgerHash, BindingConfig binding) {
		bindings.put(ledgerHash, binding);
	}

	public void removeLedgerBinding(HashDigest ledgerHash) {
		bindings.remove(ledgerHash);
	}

	public void store(File file) {
		try (FileOutputStream out = new FileOutputStream(file, false)) {
			store(out);
		} catch (IOException e) {
			throw new RuntimeIOException(e.getMessage(), e);
		}
	}

	public void store(OutputStream out) {
		FileUtils.writeText(toPropertiesString(), out, CHARSET);
	}

	private String toPropertiesString() {
		StringBuilder builder = new StringBuilder();

		HashDigest[] hashs = this.getLedgerHashs();
		writeLedgerHashs(builder, hashs);
		writeLine(builder);

		for (int i = 0; i < hashs.length; i++) {
			writeLine(builder, "#第 %s 个账本[%s]的配置；", i + 1, hashs[i].toBase58());
			BindingConfig binding = getLedger(hashs[i]);
			writeLedger(builder, hashs[i], binding);
			writeParticipant(builder, hashs[i], binding);
			writeDB(builder, hashs[i], binding);
			writeLine(builder);
		}
		return builder.toString();
	}

	private void writeLedgerHashs(StringBuilder builder, HashDigest[] hashs) {
		writeLine(builder, "#绑定的账本的hash列表；以逗号分隔；");

		String[] base58Hashs = new String[hashs.length];
		for (int i = 0; i < base58Hashs.length; i++) {
			base58Hashs[i] = hashs[i].toBase58();
		}
		String base58HashList = String.join(", \\\r\n", base58Hashs);
		writeLine(builder, "%s=%s", LEDGER_BINDINS, base58HashList);
		writeLine(builder);
	}

	private void writeParticipant(StringBuilder builder, HashDigest ledgerHash, BindingConfig binding) {
		String ledgerPrefix = String.join(ATTR_SEPERATOR, BINDING_PREFIX, ledgerHash.toBase58());
		// 参与方配置；
		String partiAddressKey = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_ADDRESS);
		String partiPkPathKey = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_PK_PATH);
		String partiNameKey = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_NAME);
		String partiPKKey = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_PK);
		String partiPwdKey = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_PASSWORD);
		String partiSslKeyStore = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_KEY_STORE);
		String partiSslKeyStoreType = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_KEY_STORE_TYPE);
		String partiSslKeyAlias = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_KEY_ALIAS);
		String partiSslKeyStorePassword = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_KEY_STORE_PASSWORD);
		String partiSslTrustStore = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_TRUST_STORE);
		String partiSslTrustStorePassword = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_TRUST_STORE_PASSWORD);
		String partiSslTrustStoreType = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_TRUST_STORE_TYPE);
		String partiSslProtocol = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_PROTOCOL);
		String partiSslEnabledProtocols = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_ENABLED_PROTOCOLS);
		String partiSslCiphers = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_CIPHERS);

		writeLine(builder, "#账本的当前共识参与方的节点地址 Address；");
		writeLine(builder, "%s=%s", partiAddressKey, stringOf(binding.getParticipant().getAddress()));
		writeLine(builder, "#账本的当前共识参与方的节点名称 NodeName；");
		writeLine(builder, "%s=%s", partiNameKey, stringOf(binding.getParticipant().getName()));
		writeLine(builder, "#账本的当前共识参与方的私钥文件的保存路径；CA模式此属性必填，忽略 pk 属性");
		writeLine(builder, "%s=%s", partiPkPathKey, stringOf(binding.getParticipant().getPkPath()));
		writeLine(builder, "#账本的当前共识参与方的私钥内容；如果指定了，优先选用此属性，其次是 pk-path 属性；");
		writeLine(builder, "%s=%s", partiPKKey, stringOf(binding.getParticipant().getPk()));
		writeLine(builder, "#账本的当前共识参与方的私钥文件的读取口令；可为空；如果为空时，节点的启动过程中需要手动从控制台输入；");
		writeLine(builder, "%s=%s", partiPwdKey, stringOf(binding.getParticipant().getPassword()));
		writeLine(builder, "#账本的当前共识参与方的共识服务TLS相关配置；");
		writeLine(builder, "%s=%s", partiSslKeyStore, escapeWinPath(stringOf(binding.getParticipant().getSslKeyStore())));
		writeLine(builder, "%s=%s", partiSslKeyStoreType, stringOf(binding.getParticipant().getSslKeyStoreType()));
		writeLine(builder, "%s=%s", partiSslKeyAlias, stringOf(binding.getParticipant().getSslKeyAlias()));
		writeLine(builder, "%s=%s", partiSslKeyStorePassword, stringOf(binding.getParticipant().getSslKeyStorePassword()));
		writeLine(builder, "%s=%s", partiSslTrustStore, escapeWinPath(stringOf(binding.getParticipant().getSslTrustStore())));
		writeLine(builder, "%s=%s", partiSslTrustStorePassword, stringOf(binding.getParticipant().getSslTrustStorePassword()));
		writeLine(builder, "%s=%s", partiSslTrustStoreType, stringOf(binding.getParticipant().getSslTrustStoreType()));
		writeLine(builder, "%s=%s", partiSslProtocol, stringOf(binding.getParticipant().getProtocol()));
		writeLine(builder, "%s=%s", partiSslEnabledProtocols, stringOf(binding.getParticipant().getEnabledProtocols()));
		writeLine(builder, "%s=%s", partiSslCiphers, stringOf(binding.getParticipant().getCiphers()));
		writeLine(builder);
	}

	private String  escapeWinPath(String path){

		if(path == null || "".equals(path.trim())){
			return path;
		}

		String os = System.getProperty("os.name");
		if(!os.toLowerCase().startsWith("win")){
			return path;
		}

		String[] split = path.split("\\\\");
		return Arrays.stream(split).filter(x -> x != null && !"".equals(x.trim())).collect(Collectors.joining("\\\\"));
	}

	private void writeDB(StringBuilder builder, HashDigest ledgerHash, BindingConfig binding) {
		String ledgerPrefix = String.join(ATTR_SEPERATOR, BINDING_PREFIX, ledgerHash.toBase58());
		// 数据库存储配置；
		String dbConnKey = String.join(ATTR_SEPERATOR, ledgerPrefix, DB_CONN);
		String dbPwdKey = String.join(ATTR_SEPERATOR, ledgerPrefix, DB_PASSWORD);

		writeLine(builder, "#账本的存储数据库的连接字符串；");
		writeLine(builder, "%s=%s", dbConnKey, stringOf(binding.getDbConnection().getUri()));
		writeLine(builder, "#账本的存储数据库的连接口令；");
		writeLine(builder, "%s=%s", dbPwdKey, stringOf(binding.getDbConnection().getPassword()));
		writeLine(builder);
	}

	private void writeLedger(StringBuilder builder, HashDigest ledgerHash, BindingConfig binding) {
		String ledgerPrefix = String.join(ATTR_SEPERATOR, BINDING_PREFIX, ledgerHash.toBase58());
		// 账本相关信息配置；
		String ledgerNameKey = String.join(ATTR_SEPERATOR, ledgerPrefix, LEDGER_NAME);
		String ledgerDataStructure = String.join(ATTR_SEPERATOR, ledgerPrefix, LEDGER_DATA_STRUCTURE);
		writeLine(builder, "#账本的名称；");
		writeLine(builder, "%s=%s", ledgerNameKey, stringOf(binding.getLedgerName()));
		writeLine(builder, "#账本的存储数据库的锚定类型；");
		writeLine(builder, "%s=%s", ledgerDataStructure, stringOf(binding.getDataStructure()));
		writeLine(builder);
	}

	private static String stringOf(Object obj) {
		if (obj == null) {
			return "";
		}
		return obj.toString();
	}

	private static void writeLine(StringBuilder content, String format, Object... args) {
		content.append(String.format(format, args));
		content.append("\r\n");
	}

	private static void writeLine(StringBuilder content) {
		content.append("\r\n");
	}

	/**
	 * 解析配置；
	 * 
	 * @param file
	 * @return
	 */
	public static LedgerBindingConfig resolve(File file) {
		Properties props = FileUtils.readProperties(file, CHARSET);
		if (props == null || props.isEmpty()) {
			throw new LedgerBindingConfigException("--- ledger-binding.config content is empty !!!");
		}
		return resolve(props);
	}

	/**
	 * 解析配置；
	 * 
	 * @param in
	 * @return
	 */
	public static LedgerBindingConfig resolve(InputStream in) {
		Properties props = FileUtils.readProperties(in, CHARSET);
		if (props == null || props.isEmpty()) {
			throw new LedgerBindingConfigException("--- ledger-binding.config content is empty !!!");
		}
		return resolve(props);
	}

	/**
	 * 解析配置；
	 * 
	 * @param props
	 * @return
	 */
	public static LedgerBindingConfig resolve(Properties props) {
		LedgerBindingConfig conf = new LedgerBindingConfig();

		// 解析哈希列表；
		String ledgerHashListString = getProperty(props, LEDGER_BINDINS, true);
		String[] base58Hashs = split(ledgerHashListString, LEDGER_HASH_SEPERATOR);
		if (base58Hashs.length == 0) {
			return conf;
		}
		HashDigest[] hashs = new HashDigest[base58Hashs.length];
		for (int i = 0; i < base58Hashs.length; i++) {
			byte[] hashBytes = Base58Utils.decode(base58Hashs[i]);
			hashs[i] = Crypto.resolveAsHashDigest(hashBytes);

			BindingConfig bindingConf = resolveBinding(props, base58Hashs[i]);
			conf.bindings.put(hashs[i], bindingConf);
		}

		return conf;
	}

	/**
	 * 解析 Binding 配置；
	 * 
	 * @param props
	 * @param ledgerHash
	 * @return
	 */
	private static BindingConfig resolveBinding(Properties props, String ledgerHash) {
		BindingConfig binding = new BindingConfig();

		String ledgerPrefix = String.join(ATTR_SEPERATOR, BINDING_PREFIX, ledgerHash);
		// 参与方配置；
		String partiAddrKey = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_ADDRESS);
		String partiPkPathKey = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_PK_PATH);
		String partiNameKey = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_NAME);
		String partiPKKey = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_PK);
		String partiPwdKey = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_PASSWORD);
		String partiSslKeyStore = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_KEY_STORE);
		String partiSslKeyStoreType = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_KEY_STORE_TYPE);
		String partiSslKeyAlias = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_KEY_ALIAS);
		String partiSslKeyStorePassword = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_KEY_STORE_PASSWORD);
		String partiSslTrustStore = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_TRUST_STORE);
		String partiSslTrustStorePassword = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_TRUST_STORE_PASSWORD);
		String partiSslTrustStoreType = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_TRUST_STORE_TYPE);
		String partiSslProtocol = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_PROTOCOL);
		String partiSslEnabledProtocols = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_ENABLED_PROTOCOLS);
		String partiSslCiphers = String.join(ATTR_SEPERATOR, ledgerPrefix, PARTI_SSL_CIPHERS);

		binding.participant.address = getProperty(props, partiAddrKey, true);
		binding.participant.name = getProperty(props, partiNameKey, true);
		binding.participant.pkPath = getProperty(props, partiPkPathKey, false);
		binding.participant.pk = getProperty(props, partiPKKey, false);
		binding.participant.password = getProperty(props, partiPwdKey, false);

		binding.participant.sslKeyStore = getProperty(props, partiSslKeyStore, false);
		binding.participant.sslKeyStoreType = getProperty(props, partiSslKeyStoreType, false);
		binding.participant.sslKeyAlias = getProperty(props, partiSslKeyAlias, false);
		binding.participant.sslKeyStorePassword = getProperty(props, partiSslKeyStorePassword, false);
		binding.participant.sslTrustStore = getProperty(props, partiSslTrustStore, false);
		binding.participant.sslTrustStorePassword = getProperty(props, partiSslTrustStorePassword, false);
		binding.participant.sslTrustStoreType = getProperty(props, partiSslTrustStoreType, false);
		binding.participant.protocol = getProperty(props, partiSslProtocol, false);
		binding.participant.enabledProtocols = getProperty(props, partiSslEnabledProtocols, false);
		binding.participant.ciphers = getProperty(props, partiSslCiphers, false);
		binding.setSslSecurity(new SSLSecurity(binding.participant.sslKeyStoreType, binding.participant.sslKeyStore, binding.participant.sslKeyAlias, binding.participant.sslKeyStorePassword,
				binding.participant.sslTrustStore, binding.participant.sslTrustStorePassword, binding.participant.sslTrustStoreType,
				binding.participant.protocol, binding.participant.enabledProtocols, binding.participant.ciphers));

		if (binding.participant.pkPath == null && binding.participant.pk == null) {
			throw new IllegalArgumentException(
					String.format("No priv key config of participant of ledger binding[%s]!", ledgerHash));
		}

		// 数据库存储配置；
		String dbConnKey = String.join(ATTR_SEPERATOR, ledgerPrefix, DB_CONN);
		String dbPwdKey = String.join(ATTR_SEPERATOR, ledgerPrefix, DB_PASSWORD);

		binding.dbConnection.setConnectionUri(getProperty(props, dbConnKey, true));
		binding.dbConnection.setPassword(getProperty(props, dbPwdKey, false));
		if (binding.dbConnection.getUri() == null) {
			throw new IllegalArgumentException(
					String.format("No db connection config of participant of ledger binding[%s]!", ledgerHash));
		}

		// 设置账本名称
		String ledgerNameKey = String.join(ATTR_SEPERATOR, ledgerPrefix, LEDGER_NAME);
		binding.ledgerName = getProperty(props, ledgerNameKey, true);
		String ledgerDataStructure = String.join(ATTR_SEPERATOR, ledgerPrefix, LEDGER_DATA_STRUCTURE);
		String structure = getProperty(props, ledgerDataStructure, false);
		binding.dataStructure = StringUtils.isEmpty(structure) ? LedgerDataStructure.MERKLE_TREE : LedgerDataStructure.valueOf(structure);

		return binding;
	}

	private static String[] split(String str, String seperator) {
		String[] items = str.split(seperator);
		List<String> validItems = new ArrayList<>();
		for (int i = 0; i < items.length; i++) {
			items[i] = items[i].trim();
			if (items[i].length() > 0) {
				validItems.add(items[i]);
			}
		}
		return validItems.toArray(new String[validItems.size()]);
	}

	/**
	 * 返回指定属性的值；
	 * 
	 * <br>
	 * 当值不存在时，如果是必需参数，则抛出异常 {@link IllegalArgumentException}，否则返回 null；
	 * 
	 * @param props
	 *            属性表；
	 * @param key
	 *            属性的键；
	 * @param required
	 *            是否为必需参数；
	 * @return 长度大于 0 的字符串，或者 null；
	 */
	private static String getProperty(Properties props, String key, boolean required) {
		String value = props.getProperty(key);
		if (value == null) {
			if (required) {
				throw new IllegalArgumentException("Miss property[" + key + "]!");
			}
			return null;
		}
		value = value.trim();
		if (value.length() == 0) {
			if (required) {
				throw new IllegalArgumentException("Miss property[" + key + "]!");
			}
			return null;
		}
		return value;
	}

	public static class BindingConfig {

		private String ledgerName;

		private LedgerDataStructure dataStructure = LedgerDataStructure.MERKLE_TREE;

		private SSLSecurity sslSecurity;

		// 账本名字
		private ParticipantBindingConfig participant = new ParticipantBindingConfig();

		private DBConnectionConfig dbConnection = new DBConnectionConfig();

		public ParticipantBindingConfig getParticipant() {
			return participant;
		}

		public DBConnectionConfig getDbConnection() {
			return dbConnection;
		}

		public void setLedgerName(String ledgerName) {
			this.ledgerName = ledgerName;
		}

		public String getLedgerName() {
			return ledgerName;
		}

		public LedgerDataStructure getDataStructure() {
			return dataStructure;
		}

		public void setDataStructure(LedgerDataStructure dataStructure) {
			this.dataStructure = dataStructure;
		}

		public SSLSecurity getSslSecurity() {
			return sslSecurity;
		}

		public void setSslSecurity(SSLSecurity sslSecurity) {
			this.sslSecurity = sslSecurity;
		}
	}

	public static class ParticipantBindingConfig {

		private String address;
		private String name;
		private String pkPath;
		private String pk;
		private String password;
		private String sslKeyStore;
		private String sslKeyStoreType;
		private String sslKeyAlias;
		private String sslKeyStorePassword;
		private String sslTrustStore;
		private String sslTrustStorePassword;
		private String sslTrustStoreType;
		private String protocol;
		private String enabledProtocols;
		private String ciphers;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getPkPath() {
			return pkPath;
		}

		public void setPkPath(String pkPath) {
			this.pkPath = pkPath;
		}

		public String getPk() {
			return pk;
		}

		public void setPk(String pk) {
			this.pk = pk;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
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

		public String getProtocol() {
			return protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public String getEnabledProtocols() {
			return enabledProtocols;
		}

		public void setEnabledProtocols(String enabledProtocols) {
			this.enabledProtocols = enabledProtocols;
		}

		public String getCiphers() {
			return ciphers;
		}

		public void setCiphers(String ciphers) {
			this.ciphers = ciphers;
		}
	}

}

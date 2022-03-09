package com.jd.blockchain.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.jd.blockchain.contract.ContractEntrance;
import com.jd.blockchain.contract.ContractProcessor;
import com.jd.blockchain.contract.OnLineContractProcessor;

import utils.StringUtils;
import utils.io.FileSystemStorage;
import utils.io.FileUtils;
import utils.io.RuntimeIOException;
import utils.io.Storage;

public abstract class RuntimeContext {

	public static interface Environment {

		boolean isProductMode();

		Storage getRuntimeStorage();

	}

	private static final Object mutex = new Object();

	private static volatile RuntimeContext runtimeContext;

	private static final ContractProcessor CONTRACT_PROCESSOR = OnLineContractProcessor.getInstance();

	public static RuntimeContext get() {
		if (runtimeContext == null) {
			synchronized (mutex) {
				if (runtimeContext == null) {
					runtimeContext = new DefaultRuntimeContext();
				}
			}
		}
		return runtimeContext;
	}

	protected static void set(RuntimeContext runtimeContext) {
		if (RuntimeContext.runtimeContext != null) {
			throw new IllegalStateException("RuntimeContext has been setted!");
		}
		RuntimeContext.runtimeContext = runtimeContext;
	}

	public RuntimeContext() {
	}

	private File getDynamicModuleJarFile(String name) {
		name = name + ".mdl";
		String parent = getRuntimeDir();
		File parentDir = new File(parent);
		synchronized (RuntimeContext.class) {
			if (!parentDir.exists()) {
				try {
					org.apache.commons.io.FileUtils.forceMkdir(parentDir);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}

		return new File(parent, name);
	}

	public Module createDynamicModule(String name, byte[] jarBytes) {
		// Save File to Disk;
		File jarFile = getDynamicModuleJarFile(name);
		synchronized (RuntimeContext.class) {
			if (!jarFile.exists()) {
				FileUtils.writeBytes(jarBytes, jarFile);
			}
		}

		try {
			URL jarURL = jarFile.toURI().toURL();
			ClassLoader moduleClassLoader = createDynamicModuleClassLoader(jarURL);
			ContractEntrance entrance = contractEntrance(jarFile);
			String contractMainClass = entrance.getImpl();

			return new DefaultModule(name, moduleClassLoader, contractMainClass);
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private ContractEntrance contractEntrance(File jarFile) throws Exception {
		return CONTRACT_PROCESSOR.analyse(jarFile);
	}

	public abstract Environment getEnvironment();

	public abstract RuntimeSecurityManager getSecurityManager();

	protected abstract String getRuntimeDir();

	protected abstract URLClassLoader createDynamicModuleClassLoader(URL jarURL);

	// ------------------------- inner types --------------------------

	private static class EnvSettings implements Environment {

		private boolean productMode;
		private Storage runtimeStorage;

		@Override
		public boolean isProductMode() {
			return productMode;
		}

		public void setProductMode(boolean productMode) {
			this.productMode = productMode;
		}

		@Override
		public Storage getRuntimeStorage() {
			return runtimeStorage;
		}

		public void setRuntimeDir(String runtimeDir) {
			try {
				this.runtimeStorage = new FileSystemStorage(runtimeDir);
			} catch (IOException e) {
				throw new RuntimeIOException(e.getMessage(), e);
			}
		}

	}

	private static class DefaultModule extends AbstractModule {

		private String name;

		private ClassLoader moduleClassLoader;

		private String mainClass;

		public DefaultModule(String name, ClassLoader cl, String mainClass) {
			this.name = name;
			this.moduleClassLoader = cl;
			this.mainClass = mainClass;
		}

		@Override
		public String getMainClass() {
			return mainClass;
		}

		@Override
		public String getName() {
			return name;
		}

//		@Override
//		public Module getParent() {
//			return null;
//		}

		@Override
		protected ClassLoader getModuleClassLoader() {
			return moduleClassLoader;
		}

	}

	/**
	 * Default RuntimeContext is a context of that:<br>
	 * all modules are running in a single class loader;
	 * 
	 * @author huanghaiquan
	 *
	 */
	static class DefaultRuntimeContext extends RuntimeContext {

		protected String homeDir;

		protected String runtimeDir;

		protected EnvSettings environment;

		protected RuntimeSecurityManager securityManager;

		public DefaultRuntimeContext() {

			this.environment = new EnvSettings();
			this.environment.setProductMode(true);

			try {
				this.homeDir = new File("./").getCanonicalPath();
				this.runtimeDir = new File(homeDir, "runtime").getAbsolutePath();
				this.environment.setRuntimeDir(runtimeDir);
				String securityPolicy = System.getProperty("java.security.policy");
				if(!StringUtils.isEmpty(securityPolicy)) {
					securityManager = new RuntimeSecurityManager(false);
					System.setSecurityManager(securityManager);
				}
			} catch (IOException e) {
				throw new RuntimeIOException(e.getMessage(), e);
			}
		}

		@Override
		public Environment getEnvironment() {
			return environment;
		}

		@Override
		public RuntimeSecurityManager getSecurityManager() {
			return securityManager;
		}

		@Override
		protected String getRuntimeDir() {
			return runtimeDir;
		}

		@Override
		protected URLClassLoader createDynamicModuleClassLoader(URL jarURL) {
			return new ContractURLClassLoader(jarURL, RuntimeContext.class.getClassLoader());
		}

	}

	static class ContractURLClassLoader extends URLClassLoader {

		private static final String BLACK_CONFIG = "black.config";

		private static final Set<String> BLACK_CLASSES = new HashSet<>();

		private static final Set<String> BLACK_PACKAGES = new HashSet<>();

		static {
			initBlacks();
		}

		public ContractURLClassLoader(URL contractJarURL, ClassLoader parent) {
			super(new URL[] { contractJarURL }, parent);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (BLACK_CLASSES.contains(name)) {
				throw new IllegalStateException(String.format("Contract cannot use Class [%s]", name));
			} else {
				// 判断该包是否是黑名单
				String trimName = name.trim();
				String packageName = trimName.substring(0, trimName.length() - 2);
				if (BLACK_PACKAGES.contains(packageName)) {
					throw new IllegalStateException(String.format("Contract cannot use Class [%s]", name));
				}
			}
			return super.loadClass(name);
		}

		private static void initBlacks() {
			try {
				InputStream inputStream = ContractURLClassLoader.class.getResourceAsStream("/" + BLACK_CONFIG);
				String text = FileUtils.readText(inputStream);
				String[] textArray = text.split("\n");
				for (String setting : textArray) {
					// 支持按照逗号分隔
					if (setting == null || setting.length() == 0) {
						continue;
					}
					String[] settingArray = setting.split(",");
					for (String set : settingArray) {
						String totalClass = set.trim();
						if (totalClass.endsWith("*")) {
							// 说明是包，获取具体包名
							String packageName = totalClass.substring(0, totalClass.length() - 2);
							BLACK_PACKAGES.add(packageName);
						} else {
							// 具体的类名，直接放入集合
							BLACK_CLASSES.add(totalClass);
						}
					}
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		public static void main(String[] args) {
			for (String s : BLACK_CLASSES) {
				System.out.println(s);
			}

			for (String s : BLACK_PACKAGES) {
				System.out.println(s);
			}
		}
	}
}
